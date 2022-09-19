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

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.graphdb.Transaction

class ServerManagementTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List()
  val title = "(★) Server management"
  override val linkId = "administration/servers"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {}

  def text = """
// Can't be run since there is no cluster available.
###dontrun

ENABLE SERVER 'serverId'
###

Make the server with the id 'serverId' an active member of the cluster

###dontrun

RENAME SERVER 'oldName' TO 'newName'
###

Rename the server 'oldName' to 'newName'

###dontrun

ALTER SERVER 'name' SET OPTIONS {modeConstraint: 'PRIMARY'}
###

Only allow the server 'name' to host databases in primary mode.

###dontrun

REALLOCATE DATABASES
###

Makes the system re-balance databases among the servers.

###dontrun

DEALLOCATE DATABASES FROM SERVERS 'name'
###

Remove all databases from the server 'name', adding them to other servers as needed, and the server is not allowed to host any new databases.

###dontrun

DROP SERVER 'name'

###

Removes the server 'name' from the cluster.

"""
}
