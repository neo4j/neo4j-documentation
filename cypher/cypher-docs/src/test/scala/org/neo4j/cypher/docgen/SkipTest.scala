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

class SkipTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("SKIP", "query-skip")
    initQueries("""CREATE
                  #  (a {name: 'A'}),
                  #  (b {name: 'B'}),
                  #  (c {name: 'C'}),
                  #  (d {name: 'D'}),
                  #  (e {name: 'E'}),
                  #  (a)-[:KNOWS]->(b),
                  #  (a)-[:KNOWS]->(c),
                  #  (a)-[:KNOWS]->(d),
                  #  (a)-[:KNOWS]->(e)""".stripMargin('#'))
    synopsis("`SKIP` defines from which row to start including the rows in the output.")
    p("""* <<skip-introduction, Introduction>>
        #* <<skip-first-three-rows, Skip first three rows>>
        #* <<skip-return-middle-rows, Return middle two rows>>
        #* <<skip-using-expression, Using an expression with `SKIP` to return a subset of the rows>>""".stripMargin('#'))
    section("Introduction", "skip-introduction") {
      p("""By using `SKIP`, the result set will get trimmed from the top.
          #Please note that no guarantees are made on the order of the result unless the query specifies the `ORDER BY` clause.
          #`SKIP` accepts any expression that evaluates to a positive integer -- however the expression cannot refer to nodes or relationships.""".stripMargin('#'))
      graphViz()
    }
    section("Skip first three rows", "skip-first-three-rows") {
      p("""To return a subset of the result, starting from the fourth result, use the following syntax:""")
      query("""MATCH (n)
              #RETURN n.name
              #ORDER BY n.name
              #SKIP 3""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList should equal(List(Map("n.name" -> "D"), Map("n.name" -> "E")))
      })) {
        p("The first three nodes are skipped, and only the last two are returned in the result.")
        resultTable()
      }
    }
    section("Return middle two rows", "skip-return-middle-rows") {
      p("""To return a subset of the result, starting from somewhere in the middle, use this syntax:""")
      query("""MATCH (n)
              #RETURN n.name
              #ORDER BY n.name
              #SKIP 1
              #LIMIT 2""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "B"), Map("n.name" -> "C")))
        })) {
        p("Two nodes from the middle are returned.")
        resultTable()
      }
    }
    section("Using an expression with `SKIP` to return a subset of the rows", "skip-using-expression") {
      p("""Skip accepts any expression that evaluates to a positive integer as long as it is not referring to any external variables:""")
      query("""MATCH (n)
              #RETURN n.name
              #ORDER BY n.name
              #SKIP 1 + toInteger(3*rand())""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toSet should contain(Map("n.name" -> "E"))
        })) {
        p("Skip the firs row plus randomly 0, 1, or 2. So randomly skip 1, 2, or 3 rows.")
        resultTable()
      }
    }
  }.build()

}


