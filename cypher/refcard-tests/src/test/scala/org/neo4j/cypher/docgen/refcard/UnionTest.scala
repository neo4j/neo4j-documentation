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

class UnionTest extends RefcardTest with QueryStatisticsTestSupport {

  def graphDescription = List(
    "A KNOWS B",
    "A LOVES B"
  )
  val title = "UNION"
  override val linkId = "clauses/union"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "one" =>
        assertStats(result)
        val list = result.toList
        assert(list === List(Map("b.name" -> "Beth")))
        assert(list.size === 1)
      case "two" =>
        assertStats(result)
        val list = result.toList
        assert(list.size === 2)
        assert(list === List(Map("b.name" -> "Beth"), Map("b.name" -> "Beth")))
    }
  }

  override val properties = Map(
    "A" -> Map("name" -> "Alice"),
    "B" -> Map("name" -> "Beth")
  )

  def text = """
###assertion=one
//

MATCH (a)-[:KNOWS]->(b)
RETURN b.name
UNION
MATCH (a)-[:LOVES]->(b)
RETURN b.name

###

Returns the distinct union of all query results.
Result column types and names have to match.

###assertion=two
//

MATCH (a)-[:KNOWS]->(b)
RETURN b.name
UNION ALL
MATCH (a)-[:LOVES]->(b)
RETURN b.name

###

Returns the union of all query results, including duplicated rows.
"""
}
