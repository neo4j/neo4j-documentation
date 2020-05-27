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
package org.neo4j.cypher.docgen.refcard

class GraphWritePrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Graph write privileges"
  override val linkId = "administration/security/writes"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("GRANT SET LABEL Label ON GRAPH * TO my_role")
    tx.execute("GRANT WRITE ON GRAPH * TO my_role")
    tx.execute("CREATE DATABASE foo")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT CREATE ON GRAPH * NODES Label TO my_role
###

Grant `create` privilege on all nodes with a specified label in all graphs to a role.

###assertion=update-two
//

DENY DELETE ON GRAPH neo4j TO my_role
###

Deny `delete` privilege on all nodes and relationships in a specified graph to a role.

###assertion=update-one
//

REVOKE SET LABEL Label ON GRAPH * FROM my_role
###

Revoke `set label` privilege for the specified label on all graphs to a role.

###assertion=update-one
//

GRANT REMOVE LABEL * ON GRAPH foo TO my_role
###

Grant `remove label` privilege for all labels on a specified graph to a role.

###assertion=update-one
//

DENY SET PROPERTY {prop} ON GRAPH foo RELATIONSHIPS Type TO my_role
###

Deny `set property` privilege on a specified property, on all relationships with a specified type in a specified graph, to a role.

###assertion=update-one
//

GRANT MERGE {*} ON GRAPH * NODES Label TO my_role
###

Grant `merge` privilege on all properties, on all nodes with a specified label in all graphs, to a role.

###assertion=update-two
//

REVOKE WRITE ON GRAPH * FROM my_role
###

Revoke `write` privilege on all graphs from a role.

###assertion=update-one
//

DENY ALL GRAPH PRIVILEGES ON GRAPH foo TO my_role
###

Deny `all graph privileges` privilege on a specified graph to a role.

"""
  }
}
