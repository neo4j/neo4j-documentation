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

import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.{CommunityCompatibilityFactory, EnterpriseCompatibilityFactory, ExecutionEngine}
import org.neo4j.doc.test.TestEnterpriseGraphDatabaseFactory
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.impl.logging.LogService
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.kernel.monitoring.Monitors

object ExecutionEngineFactory {
  def createEnterpriseDbAndEngine(): (GraphDatabaseService, ExecutionEngine) = {
    val fs = new EphemeralFileSystemAbstraction
    val graph: GraphDatabaseService = new TestEnterpriseGraphDatabaseFactory().setFileSystem(fs).newImpermanentDatabase

    (graph, createEnterpriseEngineFromDb(graph))
  }

  def createEnterpriseEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    val (database, queryService, monitors, logProvider) = prepare(graph)

    val inner = new CommunityCompatibilityFactory(queryService, monitors, logProvider)
    val compatibilityFactory = new EnterpriseCompatibilityFactory(inner, queryService, monitors, logProvider)
    new ExecutionEngine(database, logProvider, compatibilityFactory)
  }

  def createCommunityEngineFromDb(graph: GraphDatabaseService): ExecutionEngine = {
    val (database, queryService, monitors, logProvider) = prepare(graph)
    val compatibilityFactory = new CommunityCompatibilityFactory(queryService, monitors, logProvider)
    new ExecutionEngine(database, logProvider, compatibilityFactory)
  }

  private def prepare(graph: GraphDatabaseService) = {
    val database = new GraphDatabaseCypherService(graph)
    val queryService = new GraphDatabaseCypherService(graph)
    val graphAPI = graph.asInstanceOf[GraphDatabaseAPI]
    val resolver = graphAPI.getDependencyResolver
    val logService = resolver.resolveDependency(classOf[LogService])
    val monitors = resolver.resolveDependency(classOf[Monitors])
    val logProvider = logService.getInternalLogProvider
    (database, queryService, monitors, logProvider)
  }
}
