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

class DatabasePrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) DATABASE PRIVILEGES"
  override val linkId = "administration/security/administration/#administration-security-administration-database-privileges"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
    tx.execute("CREATE DATABASE foo")
    tx.execute("CREATE DATABASE bar")
    tx.execute("CREATE DATABASE baz")
    tx.execute("GRANT CREATE NEW PROPERTY NAMES ON DATABASE bar TO my_role")
    tx.execute("GRANT CONSTRAINT ON DATABASE * TO my_role")
    tx.execute("DENY CONSTRAINT ON DATABASE * TO my_role")
    tx.execute("GRANT TRANSACTION MANAGEMENT ON DEFAULT DATABASE TO my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT ACCESS ON DATABASE * TO my_role
###

Grant privilege to access and run queries against all databases to a role.

###assertion=update-one
//

GRANT START ON DATABASE * TO my_role
###

Grant privilege to start all databases to a role.

###assertion=update-one
//

GRANT STOP ON DATABASE * TO my_role
###

Grant privilege to stop all databases to a role.

###assertion=update-one
//

GRANT CREATE INDEX ON DATABASE foo TO my_role
###

Grant privilege to create indexes on a specified database to a role.

###assertion=update-one
//

GRANT DROP INDEX ON DATABASE foo TO my_role
###

Grant privilege to drop indexes on a specified database to a role.

###assertion=update-one
//

DENY INDEX MANAGEMENT ON DATABASE bar TO my_role
###

Deny privilege to create and drop indexes on a specified database to a role.

###assertion=update-one
//

GRANT CREATE CONSTRAINT ON DATABASE * TO my_role
###

Grant privilege to create constraints on all databases to a role.

###assertion=update-one
//

DENY DROP CONSTRAINT ON DATABASE * TO my_role
###

Deny privilege to drop constraints on all databases to a role.

###assertion=update-two
//

REVOKE CONSTRAINT ON DATABASE * FROM my_role
###

Revoke granted and denied privileges to create and drop constraints on all databases from a role.

###assertion=update-one
//

GRANT CREATE NEW LABELS ON DATABASE * TO my_role
###

Grant privilege to create new labels on all databases to a role.

###assertion=update-one
//

DENY CREATE NEW TYPES ON DATABASE foo TO my_role
###

Deny privilege to create new relationship types on a specified database to a role.

###assertion=update-one
//

REVOKE GRANT CREATE NEW PROPERTY NAMES ON DATABASE bar FROM my_role
###

Revoke the grant privilege to create new property names on a specified database from a role.

###assertion=update-one
//

GRANT NAME MANAGEMENT ON DEFAULT DATABASE TO my_role
###

Grant privilege to create labels, relationship types, and property names on default database to a role.

###assertion=update-one
//

GRANT ALL ON DATABASE baz TO my_role
###

Grant privilege to access, create and drop indexes and constraints, create new labels, types and property names on a specified database to a role.

###assertion=update-one
//

GRANT SHOW TRANSACTION (*) ON DATABASE foo TO my_role
###

Grant privilege to list transactions and queries from all users on a specified database to a role.

###assertion=update-two
//

DENY TERMINATE TRANSACTION (user1, user2) ON DATABASES * TO my_role
###

Deny privilege to kill transactions and queries from user1 and user2 on all databases to a role.

###assertion=update-one
//

REVOKE GRANT TRANSACTION MANAGEMENT ON DEFAULT DATABASE FROM my_role
###

Revoke the granted privilege to list and kill transactions and queries from all users on the default database from a role.

"""
  }
}
