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
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction

class SubqueryExpressionsTest extends RefcardTest with QueryStatisticsTestSupport {
  def graphDescription = List(
    "A KNOWS B")
  val title = "Subquery Expressions"
  override val linkId = "syntax/Subquery expressions"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result)
        assert(result.toList.size === 1)
    }
  }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> "Andy", "age" -> 39),
    "B" -> Map("property" -> "Timothy", "age" -> 39)
  )

  def text = """
###assertion=returns-one
MATCH (n) WHERE

EXISTS {
  MATCH (n)-->(m) WHERE n.age = m.age
}

RETURN n###

Use an EXISTS subquery to test for existence.

###assertion=returns-one
MATCH (n) WHERE

COUNT {
  MATCH (n)-[:KNOWS]->(m) WHERE n.age = m.age
}

= 1
RETURN n###

Use a `COUNT` subquery expression to count the number of results of a subquery.
"""
}
