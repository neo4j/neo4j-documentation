/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.neo4j.cypher.docgen

import org.junit.Assert._
import org.junit.Test
import org.neo4j.visualization.graphviz.{AsciiDocSimpleStyle, GraphStyle}

class AggregationTest extends DocumentingTestBase with SoftReset {
  override def graphDescription = List("A:Person KNOWS B:Person", "A KNOWS C:Person", "A KNOWS D:Person")

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> 13),
    "B" -> Map("property" -> 33, "eyes" -> "blue"),
    "C" -> Map("property" -> 44, "eyes" -> "blue"),
    "D" -> Map("eyes" -> "brown"))

  override protected def getGraphvizStyle: GraphStyle =
    AsciiDocSimpleStyle.withAutomaticRelationshipTypeColors()

  def section = "Distinct - temp"

  @Test def count_distinct() {
    testQuery(
      title = "DISTINCT",
      text = """All aggregation functions also take the `DISTINCT` modifier, which removes duplicates from the values.
So, to count the number of unique eye colors from nodes related to `a`, this query can be used: """,
      queryText = "MATCH (a:Person {name: 'A'})-->(b) RETURN count(DISTINCT b.eyes)",
      optionalResultExplanation = "Returns the number of eye colors.",
      assertions = p => assertEquals(Map("count(DISTINCT b.eyes)" -> 2), p.toList.head))
  }

  @Test def intro() {
    prepareAndTestQuery(
      title = "Introduction",
      text = """To calculate aggregated data, Cypher offers aggregation, much like SQL's `GROUP BY`.

Aggregate functions take multiple input values and calculate an aggregated value from them.
Examples are `avg` that calculates the average of multiple numeric values, or `min` that finds the smallest numeric value in a set of values.

Aggregation can be done over all the matching subgraphs, or it can be further divided by introducing key values.
These are non-aggregate expressions, that are used to group the values going into the aggregate functions.
""",
      prepare = _ => executePreparationQueries(List("""
MATCH (b:Person {name: 'B'}), (c:Person {name: 'C'})
CREATE (d:Person {name: 'D'}), (b)-[:KNOWS]->(d), (c)-[:KNOWS]->(d)
""")),
      queryText = "MATCH (me:Person)-->(friend:Person)-->(friend_of_friend:Person) " +
        "WHERE me.name = 'A'" +
        "RETURN count(distinct friend_of_friend), count(friend_of_friend)",
      assertions = p => assertEquals(Map("count(distinct friend_of_friend)" -> 1, "count(friend_of_friend)" -> 2), p.toList.head))
  }

}
