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

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.internal.runtime.InternalExecutionResult

class PredicatesTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A:Person KNOWS B", "B KNOWS C", "C KNOWS ROOT")
  val title = "Predicates"
  override val linkId = "clauses/where"

  override def assert(name: String, result: InternalExecutionResult) {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
      case "returns-two" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 2)
      case "returns-three" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 3)
      case "returns-four" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 4)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Bob")
      case "parameters=anothername" =>
        Map("value" -> "Stefan")
      case "parameters=regex" =>
        Map("regex" -> "Tim.*")
      case "parameters=names" =>
        Map("value1" -> "Peter", "value2" -> "Timothy")
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> "Stefan", "number" -> 5),
    "B" -> Map("property" -> "Timothy"),
    "C" -> Map("property" -> "Chris"))

  def text = """
###assertion=returns-one parameters=aname
MATCH (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
AND

n.property <> $value

RETURN n, m###

Use comparison operators.

###assertion=returns-three
MATCH (n)
WHERE

exists(n.property)

RETURN n###

Use functions.

###assertion=returns-one
MATCH (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B% AND

n.number >= 1 AND n.number <= 10

RETURN n, m###

Use boolean operators to combine predicates.

###assertion=returns-one
MATCH (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B% AND

1 <= n.number <= 10

RETURN n, m###

Use chained operators to combine predicates.

###assertion=returns-one
MATCH (n:Person)
WHERE

n:Person

RETURN n###

Check for node labels.

###assertion=returns-one
MATCH (n), (m)
WHERE id(n) = %A% AND id(m) = %B%
OPTIONAL MATCH (n)-[variable]->(m)
WHERE

variable IS NULL

RETURN n, m###

Check if something is `null`.

###assertion=returns-one parameters=aname
MATCH (n)
WHERE

NOT exists(n.property) OR n.property = $value

RETURN n###

Either the property does not exist or the predicate is `true`.

###assertion=returns-none parameters=aname
MATCH (n)
WHERE

n.property = $value

RETURN n###

Non-existing property returns `null`, which is not equal to anything.

###assertion=returns-none parameters=aname
MATCH (n)
WHERE

n["property"] = $value

RETURN n###

Properties may also be accessed using a dynamically computed property name.

###assertion=returns-two
MATCH (n)
WHERE

n.property STARTS WITH 'Tim' OR
n.property ENDS WITH 'n' OR
n.property CONTAINS 'goodie'

RETURN n###

String matching.

###assertion=returns-one parameters=regex
MATCH (n)
WHERE exists(n.property) AND

n.property =~ 'Tim.*'

RETURN n###

String regular expression matching.

###assertion=returns-four
MATCH (n), (m)
WHERE

(n)-[:KNOWS]->(m)

RETURN n###

Ensure the pattern has at least one match.

###assertion=returns-none
MATCH (n), (m)
WHERE id(n) = %A% AND id(m) = %B% AND

NOT (n)-[:KNOWS]->(m)

RETURN n###

Exclude matches to `(n)-[:KNOWS]->(m)` from the result.

###assertion=returns-one parameters=names
MATCH (n)
WHERE exists(n.property) AND

n.property IN [$value1, $value2]

RETURN n###

Check if an element exists in a list.
"""
}
