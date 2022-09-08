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

CREATE DATABASE `topology-example` IF NOT EXISTS TOPOLOGY 1 PRIMARY 0 SECONDARIES
###

(★) Create a database named `topology-example`, in a cluster environment, with 1 primary servers and 0 secondary servers.

###assertion=update-one
//

CREATE COMPOSITE DATABASE myCompositeDatabase IF NOT EXISTS
###

(★) Create a composite database named `myCompositeDatabase`. If any database with that name exists, then no new database will be created.

###assertion=update-one
//

ALTER DATABASE myDatabase SET ACCESS READ ONLY
###

(★) Modify a database named `myDatabase` to be read-only.

###assertion=update-one
//

ALTER DATABASE `topology-example` SET TOPOLOGY 1 PRIMARY 0 SECONDARIES
###

(★) Modify a database named `topology-example`, in a cluster environment, to use 1 primary servers and 0 secondary servers.

###assertion=update-one
//

ALTER DATABASE `topology-example` SET TOPOLOGY 1 PRIMARY SET ACCESS READ ONLY
###

(★) Modify a database named `topology-example`, in a cluster environment, to use 1 primary server and set access mode to read-only.

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

###assertion=show-five
//

SHOW DATABASES
###

List all databases in the system and information about them.

###assertion=show-two
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

###assertion=update-one
//

DROP COMPOSITE DATABASE myCompositeDatabase
###

(★) Delete the composite database `myCompositeDatabase`, throw error if it doesn't exist or isn't composite.

"""
}
