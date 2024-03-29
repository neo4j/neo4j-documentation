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

class MatchTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A:Person", "A KNOWS B:Person", "B KNOWS C:Person", "C KNOWS ROOT")
  val title = "MATCH"
  override val linkId = "clauses/match"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "related" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "none" =>
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice"),
    "B" -> Map("name" -> "Bob"),
    "C" -> Map("name" -> "Chuck")
  )

  def text = """
###assertion=related
//

MATCH (n:Person)-[:KNOWS]->(m:Person)
WHERE n.name = 'Alice'

RETURN n, m###

Node patterns can contain labels and properties.

###assertion=related
//

MATCH (n)-->(m)

WHERE id(n) = %A% AND id(m) = %B%
RETURN n, m###

Any pattern can be used in `MATCH`.

###assertion=related
//

MATCH (n {name: 'Alice'})-->(m)

RETURN n, m###

Patterns with node properties.

###assertion=related
//

MATCH p = (n)-->(m)

WHERE id(n) = %A% AND id(m) = %B%
RETURN p###

Assign a path to `p`.

###assertion=related
MATCH (n), (m)
WHERE id(n) = %A% AND id(m) = %B%

OPTIONAL MATCH (n)-[r]->(m)

RETURN r###

Optional pattern: ``null``s will be used for missing parts.

"""
}
