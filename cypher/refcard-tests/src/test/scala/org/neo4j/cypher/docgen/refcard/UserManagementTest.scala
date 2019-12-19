/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
  val title = "USER MANAGEMENT"
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

Create a user with the username `alice` that needs to change her password on first login.

###assertion=update-one parameters=update
//

ALTER USER alice SET PASSWORD $password CHANGE NOT REQUIRED
###

(★) Change the password for the user `alice` and update so that she is not required to change password on next login.

###assertion=update-one parameters=update
//

ALTER USER alice SET STATUS SUSPENDED
###

(★) Change the status for the user `alice` so that she is suspended. Use `SET STATUS ACTIVE` to reactivate `alice`.

###dontrun
// Can't be run since we can't log in as a user, and have auth disabled

ALTER CURRENT USER SET PASSWORD FROM $old TO $new
###

Change the password for the logged in user and update so that a password change is not required on next login.

###assertion=show-one
//

SHOW USERS
###

List all users in the system, their status, roles and if they need to change their password.\n
(★) Status and roles are enterprise only.

###assertion=update-one
//

DROP USER alice
###

Delete the user `alice`.

"""
}
