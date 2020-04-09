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

class PrivilegeManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Privilege management privileges"
  override val linkId = "administration/security/administration/#administration-security-administration-dbms-privileges-privilege-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("GRANT REMOVE PRIVILEGE ON DBMS TO my_role")
    tx.execute("GRANT PRIVILEGE MANAGEMENT ON DBMS TO my_role")
    tx.execute("DENY PRIVILEGE MANAGEMENT ON DBMS TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT SHOW PRIVILEGE ON DBMS TO my_role
###

Grant the privilege to show privileges to a role.

###assertion=update-one
//

DENY ASSIGN PRIVILEGE ON DBMS TO my_role
###

Deny the privilege to assign privileges to roles to a role.

###assertion=update-one
//

REVOKE GRANT REMOVE PRIVILEGE ON DBMS FROM my_role
###

Revoke the granted privilege to remove privileges from roles from a role.

###assertion=update-two
//

REVOKE PRIVILEGE MANAGEMENT ON DBMS FROM my_role
###

Revoke all granted and denied privileges for manage privileges from a role.

"""
  }
}
