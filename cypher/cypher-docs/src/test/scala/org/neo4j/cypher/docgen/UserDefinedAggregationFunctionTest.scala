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
package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class UserDefinedAggregationFunctionTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("User-defined aggregation functions", "query-functions-user-defined-aggregation")
    registerUserDefinedAggregationFunctions(classOf[org.neo4j.function.example.LongestString])
    initQueries(
      """UNWIND ['John', 'Paul', 'George', 'Ringe'] AS beatle
        |CREATE (:Member {name: beatle})""".stripMargin)
    p("Aggregating functions consume many rows and produces a single aggregated result.")
    p("This example shows how you invoke a user-defined aggregation function called `longestString` from Cypher.")

    section("Call a user-defined aggregation function", "functions-call-a-user-defined-aggregation-function") {
      p("This calls the user-defined function `org.neo4j.function.example.longestString()`.")
      query(
        """MATCH (n:Member)
          |RETURN org.neo4j.function.example.longestString(n.name) AS member""".stripMargin,
        ResultAssertions((r) => {
          assert(r.toList === List(Map("member" -> "George")))
        })) {
        resultTable()
      }
    }
  }.build()

}
