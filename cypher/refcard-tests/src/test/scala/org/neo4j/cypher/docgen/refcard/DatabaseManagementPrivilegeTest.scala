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

class DatabaseManagementPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) Database management privileges"
  override val linkId = "administration/security/administration/#access-control-dbms-administration-database-management"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("DENY DROP DATABASE ON DBMS TO my_role")
    tx.execute("GRANT ALTER DATABASE ON DBMS TO my_role")
    tx.execute("GRANT DROP COMPOSITE DATABASE ON DBMS TO my_role")
    tx.execute("DENY DROP COMPOSITE DATABASE ON DBMS TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT CREATE DATABASE ON DBMS TO my_role
###

Grant the privilege to create databases and aliases to a role.

###assertion=update-one
//

REVOKE DENY DROP DATABASE ON DBMS FROM my_role
###

Revoke the denied privilege to delete databases and aliases from a role.

###assertion=update-one
//

REVOKE GRANT ALTER DATABASE ON DBMS FROM my_role
###

Revoke the granted privilege to alter databases and aliases from a role.

###assertion=update-one
//

GRANT SET DATABASE ACCESS ON DBMS TO my_role
###

Granted privilege to set database access mode to a role.

###assertion=update-one
//

DENY CREATE COMPOSITE DATABASE ON DBMS TO my_role
###

Deny the privilege to create composite databases to a role.

###assertion=update-two
//

REVOKE DROP COMPOSITE DATABASE ON DBMS FROM my_role
###

Revoke the granted and denied privileges to delete composite databases from a role.

###assertion=update-one
//

GRANT COMPOSITE DATABASE MANAGEMENT ON DBMS TO my_role
###

Grant all privileges to manage composite databases to a role.

###assertion=update-one
//

DENY DATABASE MANAGEMENT ON DBMS TO my_role
###

Deny all privileges to manage databases and aliases to a role.

"""
  }
}
