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

class SetTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT LINK A")
  val title = "SET"
  override val linkId = "clauses/set"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "set" =>
        assertStats(result, propertiesWritten = 2)
        assert(result.head("n.property1") == "a value")
      case "set-label" =>
        assertStats(result, nodesCreated = 1, labelsAdded = 1)
        assert(result.head("labels(n)") == List("Person"))
      case "map" =>
        assertStats(result, nodesCreated = 1, propertiesWritten = 1)
        assert(result.head("n.property") == "a value")
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=set" => Map("value1" -> "a value", "value2" -> "another value")
      case "parameters=map" => Map("map" -> Map("property" -> "a value"))
      case ""               => Map()
    }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("value" -> 10)
  )

  def text = """
###assertion=set parameters=set
MATCH (n) WHERE id(n) = %A%

SET n.property1 = $value1,
    n.property2 = $value2

RETURN n.property1###

Update or create a property.

###assertion=map parameters=map
CREATE (n)

SET n = $map

RETURN n.property###

Set all properties.
This will remove any existing properties.

###assertion=map parameters=map
CREATE (n)

SET n += $map

RETURN n.property###

Add and update properties, while keeping existing ones.

###assertion=set-label
CREATE (n)

SET n:Person

RETURN labels(n)###

Adds a label `Person` to a node.
"""
}
