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

class MapsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A KNOWS B")
  val title = "Maps"
  override val linkId = "syntax/maps"

  override def assert(name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-two" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 2)
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-one-merge" =>
        assertStats(result, nodesCreated = 1, propertiesWritten = 3, labelsAdded = 1)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=name" =>
        Map("value" -> "Bob")
      case "parameters=map" =>
        Map("map" -> Map("name" -> "Alice", "age" -> 38))
      case "" =>
        Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice", "coll" -> Array(1, 2, 3)),
    "B" -> Map("name" -> "Bob", "coll" -> Array(1, 2, 3)))

  def text = """
###assertion=returns-one
RETURN

{name: 'Alice', age: 38,
 address: {city: 'London', residential: true}}

###

Literal maps are declared in curly braces much like property maps.
Lists are supported.

###assertion=returns-one
//

WITH {person: {name: 'Anne', age: 25}} AS p
RETURN p.person.name

###

Access the property of a nested map.

###assertion=returns-one-merge parameters=map
//

MERGE (p:Person {name: $map.name})
  ON CREATE SET p = $map

RETURN p
###

Maps can be passed in as parameters and used either as a map or by accessing keys.

###assertion=returns-one
//

MATCH (matchedNode:Person)
RETURN matchedNode

###

Nodes and relationships are returned as maps of their data.

###assertion=returns-one
WITH {name: 'Alice', age: 38, children: ['John', 'Max']} AS map
RETURN

map.name, map.age, map.children[0]

###

Map entries can be accessed by their keys.
Invalid keys result in an error.
"""
  /*
WITH {name:'Alice', age:38, address:{city:'London', residential:true}, children:['John','Max']} as data
RETURN

data.name, data.address.city, data.children[0]

 */
}
