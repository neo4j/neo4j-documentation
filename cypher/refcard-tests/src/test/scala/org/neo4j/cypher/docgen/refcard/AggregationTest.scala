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

class AggregationTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A KNOWS B", "B KNOWS C", "C KNOWS ROOT")
  val title = "Aggregating functions"
  override val linkId = "functions/aggregating"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=value" =>
        Map("value" -> "Bob")
      case "parameters=percentile" =>
        Map("percentile" -> 0.5)
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> 10),
    "B" -> Map("property" -> 20),
    "C" -> Map("property" -> 30))

  def text = """
###assertion=returns-one
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
RETURN NODES(path),

count(*)
###

The number of matching rows.

###assertion=returns-one
MATCH path = (variable)-->(m)
WHERE id(variable) = %A% AND id(m) = %B%
RETURN nodes(path),

count(variable)
###

The number of non-`null` values.

###assertion=returns-one
MATCH path = (variable)-->(m)
WHERE id(variable) = %A% AND id(m) = %B%
RETURN nodes(path),

count(DISTINCT variable)
###

All aggregating functions also take the `DISTINCT` operator,
which removes duplicates from the values.

###assertion=returns-one
MATCH (n)
WHERE id(n) IN [%A%, %B%, %C%]
RETURN

collect(n.property)
###

List from the values, ignores `null`.

###assertion=returns-one
MATCH (n)
WHERE id(n) IN [%A%, %B%, %C%]
RETURN

sum(n.property)

,avg(n.property), min(n.property), max(n.property)
###

Sum numerical values. Similar functions are `avg()`, `min()`, `max()`.

###assertion=returns-one parameters=percentile
MATCH (n)
WHERE id(n) IN [%A%, %B%, %C%]
RETURN

percentileDisc(n.property, $percentile)

,percentileCont(n.property, $percentile)
###

Discrete percentile. Continuous percentile is `percentileCont()`.
The `percentile` argument is from `0.0` to `1.0`.

###assertion=returns-one parameters=percentile
MATCH (n)
WHERE id(n) IN [%A%, %B%, %C%]
RETURN

stDev(n.property)

, stDevP(n.property)
###

Standard deviation for a sample of a population.
For an entire population use `stDevP()`.
"""
}
