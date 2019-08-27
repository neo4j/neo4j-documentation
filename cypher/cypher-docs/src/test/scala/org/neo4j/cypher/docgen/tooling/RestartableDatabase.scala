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

import java.io.File

import org.apache.commons.io.FileUtils
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.cypher.docgen.ExecutionEngineFactory
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, ResultSubscriber}
import org.neo4j.cypher.{ExecutionEngineHelper, GraphIcing}
import org.neo4j.dbms.api.{DatabaseManagementService, DatabaseManagementServiceBuilder}
import org.neo4j.kernel.api.procedure.GlobalProcedures

import scala.util.Try

/* I exist so my users can have a restartable database that is lazily created */
class RestartableDatabase(init: RunnableInitialization )
 extends GraphIcing with ExecutionEngineHelper {

  var managementService: DatabaseManagementService = _
  var dbFolder: File = _
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
      dbFolder = new File("target/example-db" + System.nanoTime())
      managementService = new DatabaseManagementServiceBuilder(dbFolder).build()
      val db = managementService.database(DEFAULT_DATABASE_NAME)
      graph = new GraphDatabaseCypherService(db)
      eengine = ExecutionEngineFactory.createCommunityEngineFromDb(db)
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

  def executeWithParams(q: String, params: (String, Any)*): DocsExecutionResult = {
    createAndStartIfNecessary()
    val executionResult: DocsExecutionResult = try {
      graph.inTx({ tx =>
        val txContext = graph.transactionalContext(tx, query = q -> params.toMap)
        val subscriber = new ResultSubscriber(txContext)
        val execution = eengine.execute(q,
          ExecutionEngineHelper.asMapValue(params.toMap),
          txContext,
          profile = false,
          prePopulate = false,
          subscriber)
        subscriber.init(execution)
        DocsExecutionResult(subscriber, txContext)
      })
    } catch {
      case e: Throwable => _markedForRestart = true; throw e
    }
    _markedForRestart = executionResult.queryStatistics().containsUpdates
    executionResult
  }

  private def restart() {
    if (graph == null) return
    managementService.shutdown()
    FileUtils.deleteQuietly(dbFolder)
    graph = null
    _markedForRestart = false
  }

  private def initialize(init: RunnableInitialization, graph: GraphDatabaseCypherService): Seq[QueryRunResult] = {
    // Register procedures and functions
    val procedureRegistry = graph.getDependencyResolver.resolveDependency(classOf[GlobalProcedures])
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
