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

class ListPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) SHOW PRIVILEGES"
  override val linkId = "administration/security/subgraph/#administration-security-subgraph-show"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("CREATE ROLE my_second_role")
    tx.execute("GRANT ACCESS ON DATABASE * TO my_role")
    tx.execute("GRANT TRAVERSE ON GRAPH * NODES * TO my_second_role")
    tx.execute("CREATE USER alice SET PASSWORD 'secret'")
    tx.execute("GRANT ROLE my_role TO alice")
  }

  def text: String = {
    setup()
    """
###assertion=show-two
//

SHOW PRIVILEGES
###

List all privileges in the system, and the roles that they are assigned to.

###assertion=show-one
//

SHOW PRIVILEGES
YIELD role, action, access
WHERE role = 'my_role'
###

List information about privileges, filtered by role, action and access and further refined by the name of the role.

###assertion=show-two
//

SHOW PRIVILEGES AS COMMANDS
###

List all privileges in the system as Cypher commands.

###assertion=show-one
//

SHOW ROLE my_role PRIVILEGES AS COMMANDS
###

List all privileges assigned to a role as Cypher commands.

###assertion=show-two
//

SHOW ROLE my_role, my_second_role PRIVILEGES AS COMMANDS
###

List all privileges assigned to each of the multiple roles as Cypher commands.

###assertion=show-one
//

SHOW USER alice PRIVILEGES AS COMMANDS
###

List all privileges of a user, and the role that they are assigned to as Cypher commands.

###assertion=show-nothing
//

SHOW USER PRIVILEGES AS COMMANDS
###

Lists all privileges of the currently logged in user, and the role that they are assigned to as Cypher commands.
"""
  }
}
