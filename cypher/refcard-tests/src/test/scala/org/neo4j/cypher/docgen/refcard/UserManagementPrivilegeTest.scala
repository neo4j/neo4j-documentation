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

class UserManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) User management privileges"
  override val linkId = "administration/security/administration/#administration-security-administration-dbms-privileges-user-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("GRANT SET PASSWORDS ON DBMS TO my_role")
    tx.execute("DENY SET PASSWORDS ON DBMS TO my_role")
    tx.execute("GRANT SET USER STATUS ON DBMS TO my_role")
    tx.execute("DENY SHOW USER ON DBMS TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT CREATE USER ON DBMS TO my_role
###

Grant the privilege to create users to a role.

###assertion=update-one
//

GRANT DROP USER ON DBMS TO my_role
###

Grant the privilege to delete users to a role.

###assertion=update-one
//

DENY ALTER USER ON DBMS TO my_role
###

Deny the privilege to alter users to a role.

###assertion=update-two
//

REVOKE SET PASSWORDS ON DBMS FROM my_role
###

Revoke the granted and denied privileges to alter users' passwords from a role.

###assertion=update-one
//

REVOKE GRANT SET USER STATUS ON DBMS FROM my_role
###

Revoke the granted privilege to alter the account status of users from a role.

###assertion=update-one
//

GRANT SET USER HOME DATABASE ON DBMS TO my_role
###

Grant the privilege alter the home database of users to a role.

###assertion=update-one
//

REVOKE DENY SHOW USER ON DBMS FROM my_role
###

Revoke the denied privilege to show users from a role.

###assertion=update-one
//

GRANT USER MANAGEMENT ON DBMS TO my_role
###

Grant all privileges to manage users to a role.

"""
  }
}
