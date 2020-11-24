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
package org.neo4j.cypher.docgen

import java.io.File

import org.neo4j.configuration.Config
import org.neo4j.configuration.GraphDatabaseSettings.{DEFAULT_DATABASE_NAME, SYSTEM_DATABASE_NAME}
import org.neo4j.cypher.internal.cache.ExecutorBasedCaffeineCacheFactory
import org.neo4j.cypher.internal.compiler.CypherPlannerConfiguration
import org.neo4j.cypher.internal.config.CypherConfiguration
import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, MonitoringCacheTracer}
import org.neo4j.cypher.internal.tracing.TimingCompilationTracer
import org.neo4j.cypher.internal.{ExecutionEngine, _}
import org.neo4j.dbms.api.{DatabaseManagementService, DatabaseManagementServiceBuilder}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.api.Kernel
import org.neo4j.kernel.database.Database
import org.neo4j.kernel.impl.query.QueryEngineProvider
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.logging.internal.LogService
import org.neo4j.monitoring.Monitors
import org.neo4j.scheduler.JobScheduler

object ExecutionEngineFactory {
  def createDbAndCommunityEngine(): (DatabaseManagementService, GraphDatabaseService, ExecutionEngine) = {
    val managementService: DatabaseManagementService = new DatabaseManagementServiceBuilder(new File("target/example-db")).build()
    val graph: GraphDatabaseService = managementService.database(DEFAULT_DATABASE_NAME)

    (managementService, graph, createExecutionEngineFromDb(graph))
  }

  def createCommunityEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    val spi = new DocsKernelSPI(graph.asInstanceOf[GraphDatabaseAPI])
    val cacheFactory = new ExecutorBasedCaffeineCacheFactory((_:Runnable).run())
    createEngineFromDb(spi, (queryService, plannerConfig, runtimeConfig) => new CommunityCompilerFactory(queryService, spi.monitors, cacheFactory,
      spi.logProvider, plannerConfig, runtimeConfig))
  }

  def createExecutionEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    val spi = new DocsKernelSPI(graph.asInstanceOf[GraphDatabaseAPI])
    createEngineFromDb(spi, (queryService, plannerConfig, runtimeConfig) => new EnterpriseCompilerFactory(queryService, spi, plannerConfig, runtimeConfig))
  }

  private def createEngineFromDb(spi: DocsKernelSPI,
                                 newCompatibilityFactory: (GraphDatabaseCypherService, CypherPlannerConfiguration, CypherRuntimeConfiguration) => CompilerFactory
                                ): ExecutionEngine = {
    val isSystemDatabase = spi.graphAPI.databaseName().equals(SYSTEM_DATABASE_NAME)
    val queryService = new GraphDatabaseCypherService(spi.graphAPI)
    val cypherConfig = CypherConfiguration.fromConfig(spi.config)
    val plannerConfig = CypherPlannerConfiguration.fromCypherConfiguration(cypherConfig, spi.config, isSystemDatabase)
    val runtimeConfig = CypherRuntimeConfiguration.fromCypherConfiguration(cypherConfig)
    val compilerFactory = newCompatibilityFactory(queryService, plannerConfig, runtimeConfig)
    val cacheFactory = new ExecutorBasedCaffeineCacheFactory((_:Runnable).run())
    val cacheTracer = new MonitoringCacheTracer(spi.monitors.newMonitor(classOf[ExecutionEngineQueryCacheMonitor]))
    val tracer = new TimingCompilationTracer(spi.monitors.newMonitor(classOf[TimingCompilationTracer.EventListener]))
    if (isSystemDatabase) {
      val innerPlannerConfig: CypherPlannerConfiguration = CypherPlannerConfiguration.fromCypherConfiguration(cypherConfig, spi.config, planSystemCommands = false)
      val innerCompilerFactory: CompilerFactory = newCompatibilityFactory(queryService, innerPlannerConfig, runtimeConfig)
      // The following lines are only needed for the ContextSwitchingSystemGraphQueryExecutor, which is only needed for some specific cases
//      val inner: JavaExecutionEngine = new JavaExecutionEngine(queryService, spi.logProvider, innerCompilerFactory)
//      val innerEngine: SystemDatabaseInnerEngine = (query, parameters, context, subscriber) => inner.executeQuery(query, parameters, context, false, subscriber)
//      val innerAccessor: SystemDatabaseInnerAccessor = new SystemDatabaseInnerAccessor(spi.graphAPI, innerEngine)
//      spi.resolver.asInstanceOf[Dependencies].satisfyDependency(innerAccessor)
      val innerExecutionEngine = new ExecutionEngine(queryService, spi.monitors, tracer, cacheTracer, cypherConfig,
        new CompilerLibrary(innerCompilerFactory, () => null), cacheFactory, spi.logProvider)
      new ExecutionEngine(queryService, spi.monitors, tracer, cacheTracer, cypherConfig, new CompilerLibrary(compilerFactory, () => innerExecutionEngine), cacheFactory, spi.logProvider)
    } else {
      new ExecutionEngine(queryService, spi.monitors, tracer, cacheTracer, cypherConfig, new CompilerLibrary(compilerFactory, () => null), cacheFactory, spi.logProvider)
    }
  }

  class DocsKernelSPI(val graphAPI: GraphDatabaseAPI) extends QueryEngineProvider.SPI {
    val resolver = graphAPI.getDependencyResolver
    val logService = resolver.resolveDependency(classOf[LogService])
    val logProvider = logService.getInternalLogProvider
    val monitors = resolver.resolveDependency(classOf[Monitors])
    val config = resolver.resolveDependency(classOf[Config])
    val jobScheduler = resolver.resolveDependency(classOf[JobScheduler])
    val kernel = resolver.resolveDependency(classOf[Kernel])
    val database = resolver.resolveDependency(classOf[Database])
    val lifeSupport = database.getLife
  }

}
