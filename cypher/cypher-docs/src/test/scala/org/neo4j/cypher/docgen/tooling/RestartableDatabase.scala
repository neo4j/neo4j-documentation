/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.neo4j.cypher.docgen.ExecutionEngineFactory
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.runtime.InternalExecutionResult
import org.neo4j.cypher.{ExecutionEngineHelper, GraphIcing}
import org.neo4j.doc.test.TestEnterpriseGraphDatabaseFactory
import org.neo4j.kernel.impl.proc.Procedures

import scala.util.Try

/* I exist so my users can have a restartable database that is lazily created */
class RestartableDatabase(init: RunnableInitialization, factory: TestEnterpriseGraphDatabaseFactory = new TestEnterpriseGraphDatabaseFactory())
 extends GraphIcing with ExecutionEngineHelper {

  var graph: GraphDatabaseCypherService = null
  var eengine: ExecutionEngine = null
  private var _failures: Seq[QueryRunResult] = null
  private var _markedForRestart = false

  /*
  This is the public way of controlling when it's safe to restart the database
   */
  def nowIsASafePointToRestartDatabase() = if(_markedForRestart) restart()

  private def createAndStartIfNecessary() {
    if (graph == null) {
      val db = factory.newImpermanentDatabase()
      graph = new GraphDatabaseCypherService(db)
      eengine = ExecutionEngineFactory.createEnterpriseEngineFromDb(db)
      _failures = initialize(init, graph)
    }
  }

  def failures = {
    createAndStartIfNecessary()
    _failures
  }

  def getInnerDb = {
    createAndStartIfNecessary()
    graph
  }

  def shutdown() {
    restart()
  }

  def executeWithParams(q: String, params: (String, Any)*): InternalExecutionResult = {
    createAndStartIfNecessary()
    val executionResult: InternalExecutionResult = try {
      execute(q, params:_*)
    } catch {
      case e: Throwable => _markedForRestart = true; throw e
    }
    _markedForRestart = executionResult.queryStatistics().containsUpdates
    executionResult
  }

  private def restart() {
    if (graph == null) return
    graph.getGraphDatabaseService.shutdown()
    graph = null
    _markedForRestart = false
  }

  private def initialize(init: RunnableInitialization, graph: GraphDatabaseCypherService): Seq[QueryRunResult] = {
    // Register procedures and functions
    val procedureRegistry = graph.getDependencyResolver.resolveDependency(classOf[Procedures])
    init.procedures.foreach(procedureRegistry.registerProcedure)
    init.userDefinedFunctions.foreach(procedureRegistry.registerFunction)
    init.userDefinedAggregationFunctions.foreach(procedureRegistry.registerAggregationFunction)

    // Execute custom initialization code
    init.initCode.foreach(_.apply(graph))

    // Execute queries
    init.initQueries.flatMap { q =>
      val result = Try(execute(q, Seq.empty: _*))
      result.failed.toOption.map((e: Throwable) => QueryRunResult(q, new ErrorPlaceHolder(), Left(e)))
    }
  }
}
