/*
 * Copyright (c) "Neo4j"
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

import com.neo4j.configuration.OnlineBackupSettings
import com.neo4j.kernel.enterprise.api.security.EnterpriseAuthManager
import org.neo4j.configuration.GraphDatabaseSettings
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.cypher.ExecutionEngineHelper
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.TestEnterpriseDatabaseManagementServiceBuilder
import org.neo4j.cypher.docgen.ExecutionEngineFactory
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.javacompat.ResultSubscriber
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.graphdb.config.Setting
import org.neo4j.internal.kernel.api.security.SecurityContext.AUTH_DISABLED
import org.neo4j.io.fs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.procedure.GlobalProcedures
import org.neo4j.kernel.api.security.AuthToken
import org.neo4j.kernel.impl.coreapi.InternalTransaction
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.test.rule.TestDirectory

import java.io.File
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

/* I exist so my users can have a restartable database that is lazily created */
class RestartableDatabase(init: RunnableInitialization)
  extends GraphIcing with ExecutionEngineHelper {

  var managementService: DatabaseManagementService = _
  var authManager: EnterpriseAuthManager = _
  var dbFolder: File = _
  var graph: GraphDatabaseCypherService = null
  val graphs: mutable.Map[String, MetaData] = mutable.Map.empty
  var eengine: ExecutionEngine = null
  private var _failures: Seq[QueryRunResult] = null
  private var _markedForRestart = false
  private var _login: Option[(String, String)] = None

  /*
  This is the public way of controlling when it's safe to restart the database
   */
  def nowIsASafePointToRestartDatabase(): Unit = if (_markedForRestart) restart()

  private def createAndStartIfNecessary() {
    if (graph == null) {
      dbFolder = new File("target/example-db" + System.nanoTime())
      val fs = new EphemeralFileSystemAbstraction()
      val td = TestDirectory.testDirectory(this.getClass, fs)
      dbFolder = td.prepareDirectoryForTest("target/example-db" + System.nanoTime())
      val config: Map[Setting[_], Object] = Map(
        GraphDatabaseSettings.auth_enabled -> TRUE,
        OnlineBackupSettings.online_backup_listen_address -> new SocketAddress("127.0.0.1", 0),
        OnlineBackupSettings.online_backup_enabled ->  FALSE
      )
      managementService = new TestEnterpriseDatabaseManagementServiceBuilder(dbFolder).setFileSystem(fs).setConfig(config.asJava).build()

      //    managementService = graphDatabaseFactory(Files.createTempDirectory("test").getParent.toFile).impermanent().setConfig(config.asJava).setInternalLogProvider(logProvider).build()
      managementService.listDatabases().toArray().foreach { name =>
        graphs(name.toString) = new MetaData(name.toString)
      }
      selectDatabase(Some(DEFAULT_DATABASE_NAME))
      authManager = graph.getDependencyResolver.resolveDependency(classOf[EnterpriseAuthManager])
    }
  }

  private def selectDatabase(database: Option[String]): Unit = {
    if (database.isDefined) {
      val meta = graphs(database.get)
      graph = meta.graph
      eengine = meta.eengine
      _failures = meta.failures
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

  def login(login: Option[(String, String)]): Unit = {
    _login = login
  }

  def beginTx(database: Option[String]): InternalTransaction = {
    createAndStartIfNecessary()
    selectDatabase(database)
    val loginContext = _login match {
      case Some((name, password)) => authManager.login(AuthToken.newBasicAuthToken(name, password))
      case _ => AUTH_DISABLED
    }
    graph.beginTransaction(Type.IMPLICIT, loginContext)
  }

  def executeWithParams(tx: InternalTransaction, q: String, params: (String, Any)*): DocsExecutionResult = {
    val executionResult: DocsExecutionResult = try {
      val txContext = graph.transactionalContext(tx, query = q -> params.toMap)
      val subscriber = new ResultSubscriber(txContext)
      val execution = eengine.execute(q,
        ValueUtils.asParameterMapValue(ExecutionEngineHelper.asJavaMapDeep(params.toMap)),
        txContext,
        profile = false,
        prePopulate = false,
        subscriber)
      subscriber.init(execution)
      DocsExecutionResult(subscriber, txContext)
    } catch {
      case e: Throwable => _markedForRestart = true; throw e
    }
    _markedForRestart = executionResult.queryStatistics().containsUpdates
    executionResult
  }

  def executeWithParams(query: DatabaseQuery, params: (String, Any)*): DocsExecutionResult = {
    val tx = beginTx(query.database)
    try {
      val executionResult = executeWithParams(tx, query.runnable, params: _*)
      tx.commit()
      executionResult
    } finally {
      tx.close()
    }
  }

  private def restart() {
    if (graph == null) return
    managementService.shutdown()
    graphs.clear()
    graph = null
    eengine = null
    _failures = null
    _markedForRestart = false
  }

  class MetaData(database: String) extends GraphIcing with ExecutionEngineHelper {
    val db = managementService.database(database)
    val graph = new GraphDatabaseCypherService(db)
    val eengine = ExecutionEngineFactory.createExecutionEngineFromDb(db)
    val failures: Seq[QueryRunResult] = initialize(init)

    private def initialize(init: RunnableInitialization): Seq[QueryRunResult] = {
      // Register procedures and functions
      if (database == DEFAULT_DATABASE_NAME) {
        val procedureRegistry = graph.getDependencyResolver.resolveDependency(classOf[GlobalProcedures])
        init.procedures.foreach(procedureRegistry.registerProcedure)
        init.userDefinedFunctions.foreach(procedureRegistry.registerFunction)
        init.userDefinedAggregationFunctions.foreach(procedureRegistry.registerAggregationFunction)
      }

      // Execute custom initialization code
      init.initCode.foreach(_.apply(graph))

      // Execute queries
      init.initQueries.filter(x => x.database.isEmpty || x.database.get == database).flatMap { query =>
        //TODO: Consider supporting login for initQueries
        val q = query.prettified
        val result = Try(execute(q, Seq.empty: _*))
        result.failed.toOption.map((e: Throwable) => QueryRunResult(q, new ErrorPlaceHolder(), Left(e)))
      }
    }
  }

}
