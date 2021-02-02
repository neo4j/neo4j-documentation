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

class CallSubqueryTest extends RefcardTest with QueryStatisticsTestSupport {

  val graphDescription = List("A:Person:Child FRIEND_OF B:Person", "A CHILD_OF C:Person:Parent")
  val title = "CALL subquery"
  override val linkId = "clauses/call-subquery"

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice"),
    "B" -> Map("name" -> "Bob"),
    "C" -> Map("name" -> "Chuck")
    )

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "itWorks" =>
        assert(result.toList === Seq(Map("p.name" -> "Alice", "count(other)" -> 2)))
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=arg" => Map("input" ->"foo")
      case "" => Map.empty
    }

  def text = """
### assertion=itWorks
//

CALL {
  MATCH (p:Person)-[:FRIEND_OF]->(other:Person) RETURN p, other
  UNION
  MATCH (p:Child)-[:CHILD_OF]->(other:Parent) RETURN p, other
}

RETURN DISTINCT p.name, count(other)
###

This calls a subquery with two union parts.
The result of the subquery can afterwards be post-processed.
"""
}

