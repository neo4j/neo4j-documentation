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

class DbmsPrivilegeTest extends AdministrationCommandTestBase {
  val title = "(★) DBMS privileges"
  override val linkId = "administration/security/administration/#administration-security-administration-dbms-privileges"

  private def setup() = graph.withTx { tx =>
    tx.execute("CREATE ROLE my_role")
  }

  def text: String = {
    setup()
    """
###assertion=update-one
//

GRANT ALL ON DBMS TO my_role
###

Grant privilege to perform all role, user, database, alias, privilege, procedure, function and impersonation management to a role.

###assertion=update-one
//

DENY IMPERSONATE (alice) ON DBMS TO my_role
###

Deny privilege to impersonate the specified user to a role.

"""
  }
}
