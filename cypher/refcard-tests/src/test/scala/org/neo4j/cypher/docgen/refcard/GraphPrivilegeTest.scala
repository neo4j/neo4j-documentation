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

Grant `traverse` privilege on all nodes and all graphs to the role `my_role`.

###assertion=update-one
//

DENY READ {prop} ON GRAPH foo NODES Label TO my_role
###

Deny `read` privilege on property `prop` on all nodes with label `Label` on the graph `foo` to the role `my_role`.

###assertion=update-two
//

GRANT MATCH {*} ON GRAPH foo NODES Label TO my_role
###

Grant `read` and `traverse` privileges to the role `my_role`.
Here the `read` privilege is on all properties on all nodes with label `Label` on the graph `foo` and
the `traverse` privilege on all nodes with label `Label` on the graph `foo`.

###assertion=update-two
//

REVOKE WRITE ON GRAPH * FROM my_role
###

Revoke `write` privilege on all graphs from the role `my_role`.

"""
  }
}
