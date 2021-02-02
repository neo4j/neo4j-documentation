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

class GraphPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) GRAPH PRIVILEGES"
  override val linkId = "administration/security/subgraph"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("GRANT WRITE ON GRAPH * TO my_role")
    tx.execute("CREATE DATABASE foo")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT TRAVERSE ON GRAPH * NODES * TO my_role
###

Grant `traverse` privilege on all nodes and all graphs to a role.

###assertion=update-one
//

DENY READ {prop} ON GRAPH foo RELATIONSHIP Type TO my_role
###

Deny `read` privilege on a specified property, on all relationships with a specified type in a specified graph, to a role.

###assertion=update-four
//

GRANT MATCH {*} ON GRAPH foo ELEMENTS Label TO my_role
###

Grant `read` privilege on all properties and `traverse` privilege to a role.
Here, both privileges apply to all nodes with a specified label in the graph.

###assertion=update-two
//

REVOKE WRITE ON GRAPH * FROM my_role
###

Revoke `write` privilege on all graphs from a role.

"""
  }
}
