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
package org.neo4j.cypher.docgen

import org.neo4j.cypher.internal._
import org.neo4j.cypher.internal.javacompat.ExecutionEngineGetter
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.io.fs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.internal.GraphDatabaseAPI
import org.neo4j.test.TestDatabaseManagementServiceBuilder
import org.neo4j.test.utils.TestDirectory

object ExecutionEngineFactory {

  def createCommunityDbms(): DatabaseManagementService = {
    val fs = new EphemeralFileSystemAbstraction()
    val td = TestDirectory.testDirectory(this.getClass, fs)
    val dbFolder = td.prepareDirectoryForTest("target/example-db" + System.nanoTime()).toFile
    new TestDatabaseManagementServiceBuilder(dbFolder.toPath).setFileSystem(fs).build()
  }

  def getExecutionEngine(graph: GraphDatabaseService): ExecutionEngine = {
    val resolver = graph.asInstanceOf[GraphDatabaseAPI].getDependencyResolver
    val ee = resolver.resolveDependency(classOf[org.neo4j.cypher.internal.javacompat.ExecutionEngine])
    ExecutionEngineGetter.getCypherExecutionEngine(ee)
  }

}
