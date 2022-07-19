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

import java.util

import com.neo4j.configuration.EnterpriseEditionInternalSettings
import org.neo4j.graphdb.config.Setting

class DatabaseManagementTest extends AdministrationCommandTestBase {
  val title = "Database management"
  override val linkId = "administration/databases"

  def text: String =
    """
###assertion=update-one
//

CREATE OR REPLACE DATABASE myDatabase
###

(★) Create a database named `myDatabase`. If a database with that name exists, then the existing database is deleted and a new one created.

###assertion=update-one
//

ALTER DATABASE myDatabase SET ACCESS READ ONLY
###

(★) Modify a database named `myDatabase` to be read-only.

###assertion=update-one
//

STOP DATABASE myDatabase
###

(★) Stop the database `myDatabase`.

###assertion=update-one
//

START DATABASE myDatabase
###

(★) Start the database `myDatabase`.

###assertion=update-one
//

CREATE ALIAS myAlias FOR DATABASE myDatabase
###

(★) Create an alias `myAlias` for the database with name `myDatabase`.

###assertion=update-one
//

ALTER ALIAS myAlias SET DATABASE TARGET myDatabase
###

(★) Alter the alias `myAlias` to target the database with name `myDatabase`.

###assertion=update-one
//

DROP ALIAS myAlias FOR DATABASE
###

(★) Drop the database alias `myAlias`.

###assertion=show-three
//

SHOW DATABASES
###

List all databases in the system and information about them.

###assertion=show-one
//

SHOW DATABASES
YIELD name, currentStatus
WHERE name CONTAINS 'my' AND currentStatus = 'online'
###

List information about databases, filtered by name and online status and further refined by conditions on these.

###assertion=show-one
//

SHOW DATABASE myDatabase
###

List information about the database `myDatabase`.

###assertion=show-one
//

SHOW DEFAULT DATABASE
###

List information about the default database.

###assertion=show-one
//

SHOW HOME DATABASE
###

List information about the current users home database.

###assertion=update-one
//

DROP DATABASE myDatabase IF EXISTS
###

(★) Delete the database `myDatabase`, if it exists.

"""

  // there is no way around this at this moment the script about is doing DDL operations:
  //  they can be made synchronous either by this hack, or with `WAIT`.
  //  `WAIT` fails 'update-one' assertion - so this remains
  override protected def databaseConfig(): util.Map[Setting[_], Object] = {
    val config = new util.HashMap[Setting[_], Object](super.databaseConfig());
    config.put(EnterpriseEditionInternalSettings.replication_enabled, java.lang.Boolean.FALSE);
    return util.Map.copyOf(config);
  }
}
