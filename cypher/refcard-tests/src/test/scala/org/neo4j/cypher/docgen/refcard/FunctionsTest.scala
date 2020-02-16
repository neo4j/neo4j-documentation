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

class FunctionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A", "A KNOWS B", "B KNOWS C", "C KNOWS ROOT")
  val title = "Functions"
  override val linkId = "functions"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
      case "toInteger" =>
        assert(result.toList === List(Map("toInteger($expr)" -> 10)))
      case "toFloat" =>
        assert(result.toList === List(Map("toFloat($expr)" -> 10.1)))
      case "toBoolean" =>
        assert(result.toList === List(Map("toBoolean($expr)" -> true)))
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=default" =>
        Map("defaultValue" -> "Bob")
      case "parameters=toInteger" =>
        Map("expr" -> "10")
      case "parameters=toFloat" =>
        Map("expr" -> "10.1")
      case "parameters=toBoolean" =>
        Map("expr" -> "TRUE")
      case "parameters=map" =>
        Map("expr" -> Map("name" -> "Bob"))
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("property" -> "Andy"),
    "B" -> Map("property" -> "Timothy"),
    "C" -> Map("property" -> "Chris"))

  def text = """
###assertion=returns-one parameters=default
MATCH (n)
WHERE id(n) = %A%
RETURN

coalesce(n.property, $defaultValue)###

The first non-`null` expression.

###assertion=returns-one
RETURN

timestamp()###

Milliseconds since midnight, January 1, 1970 UTC.

###assertion=returns-one
MATCH (n)-[nodeOrRelationship]->(m)
WHERE id(n) = %A% AND id(m) = %B%
RETURN

id(nodeOrRelationship)###

The internal id of the relationship or node.

###assertion=toInteger parameters=toInteger
RETURN

toInteger($expr)###

Converts the given input into an integer if possible; otherwise it returns `null`.

###assertion=toFloat parameters=toFloat
RETURN

toFloat($expr)###

Converts the given input into a floating point number if possible; otherwise it returns `null`.

###assertion=toBoolean parameters=toBoolean
RETURN

toBoolean($expr)###

Converts the given input into a boolean if possible; otherwise it returns `null`.

###assertion=returns-one parameters=map
RETURN

keys($expr)###

Returns a list of string representations for the property names of a node, relationship, or map.

###assertion=returns-one parameters=map
RETURN

properties($expr)###

Returns a map containing all the properties of a node or relationship."""
}
