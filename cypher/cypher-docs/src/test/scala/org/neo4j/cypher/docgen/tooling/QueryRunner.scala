/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen.tooling

import org.neo4j.cypher.GraphIcing
import org.neo4j.exceptions.InternalException
import org.neo4j.internal.kernel.api.security.SecurityContext.AUTH_DISABLED
import org.neo4j.kernel.GraphDatabaseQueryService
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.impl.coreapi.InternalTransaction

import scala.collection.immutable.Iterable
import scala.util.{Failure, Success, Try}

/**
 * QueryRunner is used to actually run queries and produce either errors or
 * Content containing the runSingleQuery of the execution
 *
 * It works by grouping queries and graph-vizualisations by the initiation they need, and running all queries with the same
 * init queries together. After running the query, we check if it updated the graph. If a query updates the graph,
 * we drop the database and create a new one. This way we can make sure that two queries don't affect each other more than
 * necessary.
 */
class QueryRunner(formatter: (GraphDatabaseQueryService, InternalTransaction) => DocsExecutionResult => Content,
                  statsOnly: (GraphDatabaseQueryService, InternalTransaction) => DocsExecutionResult => Content) extends GraphIcing {

  def runQueries(contentsWithInit: Seq[ContentWithInit], title: String): TestRunResult = {

    val groupedByInits: Map[RunnableInitialization, Seq[(DatabaseQuery, QueryResultPlaceHolder)]] =
      contentsWithInit.groupBy(_.initKey).mapValues(_.map(cwi => cwi.queryToPresent -> cwi.queryResultPlaceHolder))
    var graphVizCounter = 0

    val results: Iterable[RunResult] = groupedByInits.flatMap {
      case (init, placeHolders) =>

        val dbms = new RestartableDatabase(init)
        try {
          if (dbms.failures.nonEmpty) dbms.failures
          else {
            val result = placeHolders.map { queryAndPlaceHolder =>
              try {
                queryAndPlaceHolder match {
                  case (query: DatabaseQuery, tb: TablePlaceHolder) =>
                    runSingleQuery(dbms, query, tb.assertions, tb)

                  case (query: DatabaseQuery, gv: GraphVizPlaceHolder) =>
                    graphVizCounter = graphVizCounter + 1
                    Try(dbms.executeWithParams(query)) match {
                      case Success(inner) =>
                        GraphVizRunResult(gv, captureStateAsGraphViz(dbms.getInnerDb, title, graphVizCounter, gv.options))
                      case Failure(error) =>
                        QueryRunResult(query.prettified, gv, Left(error))
                    }

                  case (query: DatabaseQuery, placeHolder: ExecutionPlanPlaceHolder) =>
                    explainSingleQuery(dbms, query, placeHolder.assertions, placeHolder)

                  case (query: DatabaseQuery, placeHolder: ProfileExecutionPlanPlaceHolder) =>
                    profileSingleQuery(dbms, query, placeHolder.assertions, placeHolder)

                  case _ =>
                    ???
                }
              } finally {
                dbms.nowIsASafePointToRestartDatabase()
              }
            }
            result
          }
        } finally dbms.shutdown()
    }

    TestRunResult(results.toSeq)
  }

  private def runSingleQuery(dbms: RestartableDatabase, query: DatabaseQuery, assertions: QueryAssertions, content: TablePlaceHolder): QueryRunResult = {
    val format: (InternalTransaction) => (DocsExecutionResult) => Content = (tx: InternalTransaction) => content match {
      case _: StatsOnlyTablePlaceHolder => statsOnly(dbms.getInnerDb, tx)(_)
      case _ => formatter(dbms.getInnerDb, tx)(_)
    }

    val result: Either[Throwable, InternalTransaction => Content] =
      try {
        val resultTry = Try(dbms.executeWithParams(query, content.params:_*))
        (assertions, resultTry) match {
          // *** Success conditions

          case (ResultAssertions(f), Success(r)) =>
            f(r)
            Right(format(_)(r))

          case (ResultAndDbAssertions(f), Success(r)) =>
            f(r, dbms.getInnerDb)
            Right(format(_)(r))

          case (NoAssertions, Success(r)) =>
            Right(format(_)(r))

          // *** Error conditions
          case (_, Failure(exception: Throwable)) =>
            Left(exception)

          case x =>
            throw new InternalException(s"Did not see this one coming $x")
        }
      } catch {
        case e: Throwable =>
          Left(e)
      }

    val formattedResult: Either[Throwable, Content] = {
      val tx = dbms.getInnerDb.beginTransaction(Type.`implicit`, AUTH_DISABLED)
      try {
        val r = result.right.map(contentBuilder => contentBuilder(tx))
        tx.commit()
        r
      } finally {
        tx.close()
      }
    }

    QueryRunResult(query.prettified, content, formattedResult)
  }

  private def explainSingleQuery(database: RestartableDatabase,
                                 query: DatabaseQuery,
                                 assertions: QueryAssertions,
                                 placeHolder: QueryResultPlaceHolder): RunResult = {
    val result: Either[Throwable, ExecutionPlan] =
      try {
        (assertions, Try(database.executeWithParams(query.explain))) match {
          case (ResultAssertions(f), Success(inner)) =>
            f(inner)
            Right(ExecutionPlan(inner.executionPlanDescription().toString))

          case (ResultAndDbAssertions(f), Success(inner)) =>
            f(inner, database.getInnerDb)
            Right(ExecutionPlan(inner.executionPlanDescription().toString))

          case (NoAssertions, Success(inner)) =>
            Right(ExecutionPlan(inner.executionPlanDescription().toString))

          case x =>
            throw new InternalException(s"Did not see this one coming $x")
        }
      } catch {
        case e: Throwable =>
          Left(e)
      }
    ExecutionPlanRunResult(query.prettified, placeHolder, result)
  }

  private def profileSingleQuery(database: RestartableDatabase,
                                 query: DatabaseQuery,
                                 assertions: QueryAssertions,
                                 placeHolder: QueryResultPlaceHolder) = {
    val profilingAttempt = Try(database.executeWithParams(query.profile))
    val planString = (assertions, profilingAttempt) match {
      case (ResultAssertions(f), Success(result)) =>
        f(result)
        result.executionPlanDescription().toString

      case (ResultAndDbAssertions(f), Success(result)) =>
        f(result, database.getInnerDb)
        result.executionPlanDescription().toString

      case (NoAssertions, Success(inner)) =>
        inner.executionPlanDescription().toString

      case x =>
        throw new InternalException(s"Did not see this one coming $x")
    }
    ExecutionPlanRunResult(query.prettified, placeHolder, Right(ExecutionPlan(planString)))
  }
}

sealed trait RunResult {
  def success: Boolean
  def original: QueryResultPlaceHolder
  def newContent: Option[Content]
  def newFailure: Option[Throwable]
}

case class QueryRunResult(queryText: String, original: QueryResultPlaceHolder, testResult: Either[Throwable, Content]) extends RunResult {
  override def success = testResult.isRight

  override def newContent: Option[Content] = testResult.right.toOption

  override def newFailure: Option[Throwable] = testResult.left.toOption
}

case class GraphVizRunResult(original: GraphVizPlaceHolder, graphViz: GraphViz) extends RunResult {
  override def success = true
  override def newContent = Some(graphViz)
  override def newFailure = None
}

case class ExecutionPlanRunResult(queryText: String, original: QueryResultPlaceHolder, testResult: Either[Throwable, ExecutionPlan]) extends RunResult {

  override def success: Boolean = testResult.isRight

  override def newContent: Option[Content] = testResult.right.toOption

  override def newFailure: Option[Throwable] = testResult.left.toOption
}

case class TestRunResult(queryResults: Seq[RunResult]) {
  def success = queryResults.forall(_.success)

  def foreach[U](f: RunResult => U) = queryResults.foreach(f)
}

class ExpectedExceptionNotFound(m: String) extends Exception(m)
