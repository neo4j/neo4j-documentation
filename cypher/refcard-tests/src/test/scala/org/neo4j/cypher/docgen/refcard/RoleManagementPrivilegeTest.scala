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

class RoleManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Role management privileges"
  override val linkId = "administration/security/administration/#administration-security-administration-dbms-privileges-role-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("DENY SHOW ROLE ON DBMS TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT CREATE ROLE ON DBMS TO my_role
###

Grant the privilege to create roles to a role.

###assertion=update-one
//

GRANT DROP ROLE ON DBMS TO my_role
###

Grant the privilege to delete roles to a role.

###assertion=update-one
//

DENY ASSIGN ROLE ON DBMS TO my_role
###

Deny the privilege to assign roles to users to a role.

###assertion=update-one
//

DENY REMOVE ROLE ON DBMS TO my_role
###

Deny the privilege to remove roles from users to a role.

###assertion=update-one
//

REVOKE DENY SHOW ROLE ON DBMS FROM my_role
###

Revoke the denied privilege to show roles from a role.

###assertion=update-one
//

GRANT ROLE MANAGEMENT ON DBMS TO my_role
###

Grant all privileges to manage roles to a role.

"""
  }
}
