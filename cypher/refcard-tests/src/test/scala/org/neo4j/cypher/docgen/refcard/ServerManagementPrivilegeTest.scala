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

class ServerManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Server management privileges"
  override val linkId = "administration/security/administration/#access-control-dbms-administration-server-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT SERVER MANAGEMENT ON DBMS TO my_role
###

Grant the privilege to manage servers.

###assertion=update-one
//

DENY SHOW SERVERS ON DBMS TO my_role
###

Deny the privilege to show information about the servers.

"""
  }
}
