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

class ListPredicatesTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A KNOWS B", "B KNOWS C", "C KNOWS ROOT")
  val title = "List predicates"
  override val linkId = "functions/predicate"

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

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> "Andy"),
    "B" -> Map("property" -> "Timothy"),
    "C" -> Map("property" -> "Chris"))

  def text = """
###assertion=returns-one
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
WITH nodes(path) AS coll, n, m
WHERE

all(x IN coll WHERE x.property IS NOT NULL)

RETURN n,m###

Returns `true` if the predicate is `true` for all elements in the list.

###assertion=returns-one
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
WITH nodes(path) AS coll, n, m
WHERE

any(x IN coll WHERE x.property IS NOT NULL)

RETURN n, m###

Returns `true` if the predicate is `true` for at least one element in the list.

###assertion=returns-none
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
WITH nodes(path) AS coll, n, m
WHERE

none(x IN coll WHERE x.property IS NOT NULL)

RETURN n, m###

Returns `true` if the predicate is `false` for all elements in the list.

###assertion=returns-none
MATCH path = (n)-->(m)
WHERE id(n) = %A% AND id(m) = %B%
WITH nodes(path) AS coll, n, m
WHERE

single(x IN coll WHERE x.property IS NOT NULL)

RETURN n, m###

Returns `true` if the predicate is `true` for exactly one element in the list.
             """
}
