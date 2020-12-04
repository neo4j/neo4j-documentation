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

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction

class UseTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A:Person", "A KNOWS B:Person", "B KNOWS C:Person", "C KNOWS ROOT")
  val title = "USE"
  override val linkId = "clauses/use"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {}



  def text = """
###dontrun
//

USE myDatabase
###

Select `myDatabase` to execute query or query part against.

###dontrun
//

USE neo4j
MATCH (n:Person)-[:KNOWS]->(m:Person)
WHERE n.name = 'Alice'

RETURN n, m###

`MATCH` query executed against `neo4j` database.

"""
}
