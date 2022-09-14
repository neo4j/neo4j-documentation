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

class AliasManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Alias management privileges"
  override val linkId = "administration/security/administration/#access-control-dbms-administration-alias-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("DENY DROP ALIAS ON DBMS TO my_role")
    tx.execute("GRANT ALTER ALIAS ON DBMS TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT CREATE ALIAS ON DBMS TO my_role
###

Grant the privilege to create aliases to a role.

###assertion=update-one
//

REVOKE DENY DROP ALIAS ON DBMS FROM my_role
###

Revoke the denied privilege to delete aliases from a role.

###assertion=update-one
//

REVOKE GRANT ALTER ALIAS ON DBMS FROM my_role
###

Revoke the granted privilege to alter aliases from a role.

###assertion=update-one
//

GRANT SHOW ALIAS ON DBMS TO my_role
###

Granted privilege to list aliases to a role.

###assertion=update-one
//

DENY ALIAS MANAGEMENT ON DBMS TO my_role
###

Deny all privileges to manage aliases to a role.

"""
  }
}
