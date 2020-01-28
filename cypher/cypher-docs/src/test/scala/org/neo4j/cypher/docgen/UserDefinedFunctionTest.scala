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
package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class UserDefinedFunctionTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("User-defined scalar functions", "query-functions-udf")

    registerUserDefinedFunctions(classOf[org.neo4j.function.example.JoinFunction])

    initQueries(
      """UNWIND ["John", "Paul", "George", "Ringo"] as name CREATE (:Member {name: name})""")

    p("""
        |For each incoming row the function takes parameters and returns a single result.""")

    p("""
        |This example shows how you invoke a user-defined function called `join` from Cypher.""")

    section("Call a user-defined function") {
      p("This calls the user-defined function `org.neo4j.procedure.example.join()`.")

      query("MATCH (n:Member) RETURN org.neo4j.function.example.join(collect(n.name)) AS members", ResultAssertions((r) =>
        assert(r.toList === List(Map("members" -> "John,Paul,George,Ringo"))))) {
        resultTable()
      }
    }
    p("""
        |For developing and deploying user-defined functions in Neo4j, see <<user-defined-functions, Extending Neo4j -> User-defined functions>>.""")

  }.build()
}
