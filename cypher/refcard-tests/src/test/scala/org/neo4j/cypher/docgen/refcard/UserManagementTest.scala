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

class UserManagementTest extends AdministrationCommandTestBase {
  val title = "User management"
  override val linkId = "administration/security/users-and-roles/#administration-security-users"

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=create" =>
        Map("password" -> "secret")
      case "parameters=update" =>
        Map("password" -> "new_secret")
      case _ =>
        Map.empty
    }

  def text: String =
    """
###assertion=update-one parameters=create
//

CREATE USER alice SET PASSWORD $password
###

Create a new user and a password. This password must be changed on the first login.

###assertion=update-one parameters=update
//

ALTER USER alice SET PASSWORD $password CHANGE NOT REQUIRED
###

Set a new password for a user. This user will not be required to change this password on the next login.

###assertion=update-one parameters=update
//

ALTER USER alice IF EXISTS SET PASSWORD CHANGE REQUIRED
###

If the specified user exists, force this user to change their password on the next login.

###assertion=update-one parameters=update
//

ALTER USER alice SET STATUS SUSPENDED
###

(★) Change the user status to suspended. Use `SET STATUS ACTIVE` to reactivate the user.

###dontrun
//

ALTER USER alice SET HOME DATABASE otherDb
###

(★) Change the home database of user to otherDb. Use `REMOVE HOME DATABASE` to have the default database be home for the user again.

###dontrun
// Can't be run since we can't log in as a user, and have auth disabled

ALTER CURRENT USER SET PASSWORD FROM $old TO $new
###

Change the password of the logged-in user. The user will not be required to change this password on the next login.

###assertion=show-nothing
// shows nothing since we can't log in as a user, and have auth disabled

SHOW CURRENT USER
###

List the currently logged-in user, their status, roles and whether they need to change their password. +
(★) Status and roles are Enterprise Edition only.

###assertion=show-one
//

SHOW USERS
###

List all users in the system, their status, roles and if they need to change their password. +
(★) Status and roles are Enterprise Edition only.

###assertion=show-one
//

SHOW USERS
YIELD user, suspended
WHERE suspended = true
###

List users in the system, filtered by their name and status and further refined by whether they are suspended. +
(★) Status is Enterprise Edition only.

###assertion=update-one
//

DROP USER alice
###

Delete the user.

"""
}
