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
package org.neo4j.cypher.docgen.refcard

import org.neo4j.configuration.GraphDatabaseSettings
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.{GraphDatabaseService, Transaction}
import org.neo4j.internal.kernel.api.security.LoginContext
import org.neo4j.kernel.api.KernelTransaction

abstract class AdministrationCommandTestBase extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List()

  override protected def getGraph: GraphDatabaseService = managementService.database(GraphDatabaseSettings.SYSTEM_DATABASE_NAME)

  override protected def cleanGraph(): Unit = {
    val tx = db.beginTransaction(KernelTransaction.Type.EXPLICIT, LoginContext.AUTH_DISABLED)
    try {
      // delete all roles
      tx.findNodes(Label.label("Role")).forEachRemaining(role => {
        role.getRelationships.forEach(rel => rel.delete())
        role.delete()
      })
      // delete all users
      tx.findNodes(Label.label("User")).forEachRemaining(user => {
        user.getRelationships.forEach(rel => rel.delete())
        user.delete()
      })
      tx.commit()
    } finally {
      if (tx != null) tx.close()
    }
  }

  //noinspection RedundantDefaultArgument
  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "update-one" =>
        assertStats(result, systemUpdates = 1)
        assert(result.toList.size === 0)
      case "update-two" =>
        assertStats(result, systemUpdates = 2)
        assert(result.toList.size === 0)
      case "update-nothing" =>
        assertStats(result, systemUpdates = 0)
        assert(result.toList.size === 0)
      case "show-one" =>
        assertStats(result, systemUpdates = 0)
        assert(result.toList.size === 1) // there are no default roles or users
      case "show-two" =>
        assertStats(result, systemUpdates = 0)
        assert(result.toList.size === 2) // there are no default users
      case "show-three" =>
        assertStats(result, systemUpdates = 0)
        assert(result.toList.size === 3) // default and system database are shown
      case "show-nothing" =>
        assertStats(result, systemUpdates = 0)
        assert(result.toList.size === 0)
    }
  }
}
