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

class ListExpressionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A:Person KNOWS B:Person", "B KNOWS C:Person", "C KNOWS ROOT")
  val title = "List expressions"
  override val linkId = "functions/list"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-three" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 3)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
      case "foreach" =>
        assertStats(result, nodesCreated = 3, labelsAdded = 3, propertiesWritten = 3)
        assert(result.toList.size === 0)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=list" =>
        Map("list" -> List(1, 2, 3))
      case "parameters=value" =>
        Map("value" -> "Bob")
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("prop" -> "Andy"),
    "B" -> Map("prop" -> "Timothy"),
    "C" -> Map("prop" -> "Chris")
  )

  def text = """
###assertion=returns-one parameters=list
RETURN

size($list)
###

Number of elements in the list.

###assertion=returns-one parameters=list
RETURN

reverse($list)
###

Reverse the order of the elements in the list.

###assertion=returns-one parameters=list
RETURN

head($list)
###

Get the first element of the list.
Return `null` for an empty list.
Eqivalent to the list indexing `$list[0]`.

###assertion=returns-one parameters=list
RETURN

last($list)
###

Get the last element of the list.
Return `null` for an empty list.
Eqivalent to the list indexing `$list[-1]`.

###assertion=returns-one parameters=list
RETURN

tail($list)
###

Get all elements except for the first element.
Return `[]` for an empty list.
Eqivalent to the list slice `$list[1..]`.
Out-of-bound slices are truncated to an empty list `[]`.

###assertion=returns-one
MATCH (n)
WHERE id(n) = %A%
WITH [n] AS list
RETURN

[x IN list | x.prop]
###

A list of the value of the expression for each element in the original list.

###assertion=returns-one parameters=value
MATCH (n)
WHERE id(n) = %A%
WITH [n] AS list
RETURN

[x IN list WHERE x.prop <> $value]
###

A filtered list of the elements where the predicate is `true`.

###assertion=returns-one parameters=value
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
WITH nodes(path) AS list
RETURN

[x IN list WHERE x.prop <> $value | x.prop]
###

A list comprehension that filters a list and extracts the value of the expression for each element in that list.

###assertion=returns-one
MATCH (n)
WHERE id(n) = %A%
WITH [n] AS list
RETURN

reduce(s = "", x IN list | s + x.prop)
###

Evaluate expression for each element in the list, accumulate the results.
             """
}
