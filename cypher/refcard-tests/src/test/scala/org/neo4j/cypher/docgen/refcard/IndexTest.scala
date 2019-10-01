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

import java.util.concurrent.TimeUnit

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction

class IndexTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A:Person KNOWS B:Person")
  val title = "INDEX"
  override val linkId = "schema/index"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "create-index" =>
        assert(result.toList.size === 0)
        tx.schema().awaitIndexesOnline(10, TimeUnit.SECONDS)
      case "drop-index" =>
        // assertStats(result, indexDeleted = 1)
        assert(result.toList.size === 0)
      case "match" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
    }
  }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice", "age" -> 18 ),
    "B" -> Map("name" -> "Timothy", "age" -> 22 ))

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Alice")
      case "parameters=nameandage" =>
        Map("value" -> "Alice" , "value2" -> 18 )
      case _ =>
        Map()
    }

  def text = """
###assertion=create-index
//

CREATE INDEX ON :Person(name)
###

Create an index on the label `Person` and property `name`.

###assertion=create-index
//

CREATE INDEX ON :Person(name, age)
###

Create a composite index on the label `Person` and the properties `name` and `age`.

###assertion=match parameters=aname
//

MATCH (n:Person) WHERE n.name = $value

RETURN n
###

An index can be automatically used for the equality comparison.
Note that for example `toLower(n.name) = $value` will not use an index.

###assertion=match parameters=aname
//

MATCH (n:Person)
WHERE n.name IN [$value]

RETURN n
###

An index can automatically be used for the `IN` list checks.

###assertion=match parameters=nameandage
//

MATCH (n:Person)
WHERE n.name = $value and n.age = $value2

RETURN n
###

A composite index can be automatically used for equality comparison of both properties.
Note that there needs to be predicates on all properties of the composite index for it to be used.

###assertion=match parameters=aname
//

MATCH (n:Person)
USING INDEX n:Person(name)
WHERE n.name = $value

RETURN n
###

Index usage can be enforced when Cypher uses a suboptimal index, or
more than one index should be used.

###assertion=drop-index
//

DROP INDEX ON :Person(name)
###

Drop the index on the label `Person` and property `name`.
"""
}
