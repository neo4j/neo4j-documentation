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

class AliasManagementTest extends AdministrationCommandTestBase {
  val title = "(★) Alias management"
  override val linkId = "administration/aliases"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE DATABASE myDatabase")
  }

  def text: String = {
    setup()
  """
###assertion=update-one
//

CREATE ALIAS myAlias FOR DATABASE myDatabase
###

Create a local alias `myAlias` for the database with name `myDatabase`.

###assertion=update-one
//

CREATE ALIAS myRemote FOR DATABASE myDatabase AT "neo4j+s://location:7687"
USER alice PASSWORD "password"
DRIVER { connection_timeout : duration({ minutes: 1 }) }
###

Create a remote alias `myRemote` for the database with name `myDatabase`.

###assertion=update-one
//

ALTER ALIAS myAlias SET DATABASE TARGET myDatabase
###

Alter the local alias `myAlias` to target the database with name `myDatabase`.

###assertion=update-one
//

ALTER ALIAS myRemote SET
DATABASE TARGET myDatabase AT "neo4j+s://location:7687"
USER bob PASSWORD "password"
DRIVER { connection_timeout : duration({ minutes: 1 }) }
###

Alter the remote alias `myRemote` with possible subclauses.

###assertion=show-two
//

SHOW ALIASES FOR DATABASE
###

List all database aliases.

###assertion=update-nothing
//

DROP ALIAS myAlias IF EXISTS FOR DATABASE
###

Drop the database alias `myAlias`.

"""
  }
}
