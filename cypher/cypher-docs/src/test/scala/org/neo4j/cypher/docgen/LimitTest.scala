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

class LimitTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("LIMIT", "query-limit")
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
    synopsis("`LIMIT` constrains the number of returned rows.")
    p("""* <<limit-introduction, Introduction>>
        #* <<limit-subset-rows, Return a subset of the rows>>
        #* <<limit-subset-rows-using-expression, Using an expression with `LIMIT` to return a subset of the rows>>""".stripMargin('#'))
    section("Introduction", "limit-introduction") {
      p("""`LIMIT` accepts any expression that evaluates to a positive integer -- however the expression cannot refer to nodes or relationships.""")
      graphViz()
    }
    section("Return a limited subset of the rows", "limit-subset-rows") {
      p("""To return a limited subset of the rows, use this syntax:""")
      query("""MATCH (n)
              #RETURN n.name
              #ORDER BY n.name
              #LIMIT 3""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList should equal(List(Map("n.name" -> "A"), Map("n.name" -> "B"), Map("n.name" -> "C")))
      })) {
        p("Limit to 3 rows by the example query.")
        resultTable()
      }
    }
    section("Using an expression with `LIMIT` to return a subset of the rows", "limit-subset-rows-using-expression") {
      p("""Limit accepts any expression that evaluates to a positive integer as long as it is not referring to any external variables:""")
      query("""MATCH (n)
              #RETURN n.name
              #ORDER BY n.name
              #LIMIT 1 + toInteger(3 * rand())""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toSet should contain(Map("n.name" -> "A"))
        })) {
        p("Limit 1 row plus randomly 0, 1, or 2. So randomly limit to 1, 2, or 3 rows.")
        resultTable()
      }
    }
  }.build()
}

