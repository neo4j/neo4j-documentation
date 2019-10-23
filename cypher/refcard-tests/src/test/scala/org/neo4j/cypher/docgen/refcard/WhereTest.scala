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

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction

class WhereTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT FRIEND A", "A FRIEND B", "B FRIEND C", "C FRIEND ROOT")
  val title = "WHERE"
  override val linkId = "clauses/where"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Bob")
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> "Andy", "age" -> 39),
    "B" -> Map("property" -> "Timothy", "age" -> 39),
    "C" -> Map("property" -> "Chris", "age" -> 22))

  def text = """
###assertion=returns-one parameters=aname
MATCH (n)-->(m)

WHERE n.property <> $value

AND id(n) = %A% AND id(m) = %B%
RETURN n, m
###

Use a predicate to filter.
Note that `WHERE` is always part of a  `MATCH`, `OPTIONAL MATCH` or `WITH` clause.
Putting it after a different clause in a query will alter what it does.

###assertion=returns-one
MATCH (n)

WHERE EXISTS {
  MATCH (n)-->(m) WHERE n.age = m.age
}

RETURN n, m
###

Use an existential subquery to filter.
"""
}
