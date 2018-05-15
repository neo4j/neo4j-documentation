/*
 * Copyright (c) 2002-2018 "Neo4j,"
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

import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, MonitoringCacheTracer}
import org.neo4j.cypher.internal._
import org.neo4j.cypher.internal.tracing.TimingCompilationTracer
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.configuration.Config
import org.neo4j.kernel.impl.logging.LogService
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.kernel.monitoring.Monitors
import org.neo4j.logging.LogProvider
import org.neo4j.test.TestEnterpriseGraphDatabaseFactory

object ExecutionEngineFactory {
  def createEnterpriseDbAndEngine(): (GraphDatabaseService, ExecutionEngine) = {
    val fs = new EphemeralFileSystemAbstraction
    val graph: GraphDatabaseService = new TestEnterpriseGraphDatabaseFactory().setFileSystem(fs).newImpermanentDatabase

    (graph, createEnterpriseEngineFromDb(graph))
  }

  def createEnterpriseEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    createEngineFromDb(graph,
      (queryService, monitors, logProvider) => {
        val inner = new CommunityCompatibilityFactory(queryService, monitors, logProvider)
        new EnterpriseCompatibilityFactory(inner, queryService, monitors, logProvider)
      })
  }

  def createCommunityEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    createEngineFromDb(graph,
      (queryService, monitors, logProvider) => {
        new CommunityCompatibilityFactory(queryService, monitors, logProvider)
      })
  }

  private def createEngineFromDb(graph: GraphDatabaseService,
                                 newCompatibilityFactory: (GraphDatabaseCypherService, Monitors, LogProvider) => CompatibilityFactory
                                ): ExecutionEngine = {
    val queryService = new GraphDatabaseCypherService(graph)
    val graphAPI = graph.asInstanceOf[GraphDatabaseAPI]
    val resolver = graphAPI.getDependencyResolver
    val logService = resolver.resolveDependency(classOf[LogService])
    val monitors = resolver.resolveDependency(classOf[Monitors])
    val logProvider = logService.getInternalLogProvider

    val compatibilityFactory = newCompatibilityFactory(queryService, monitors, logProvider)

    val cacheTracer = new MonitoringCacheTracer(monitors.newMonitor(classOf[StringCacheMonitor]))
    val cypherConfiguration = CypherConfiguration.fromConfig(resolver.resolveDependency(classOf[Config]))
    val tracer = new TimingCompilationTracer(monitors.newMonitor(classOf[TimingCompilationTracer.EventListener]))
    new ExecutionEngine(queryService, monitors, tracer, cacheTracer, cypherConfiguration, compatibilityFactory, logProvider)
  }
}
