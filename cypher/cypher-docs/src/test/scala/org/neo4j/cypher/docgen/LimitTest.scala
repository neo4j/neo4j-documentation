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

import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class LimitTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("LIMIT", "query-limit")
    initQueries(
      """CREATE (a {name: 'A'}),
                (b {name: 'B'}),
                (c {name: 'C'}),
                (d {name: 'D'}),
                (e {name: 'E'}),

                (a)-[:KNOWS]->(b),
                (a)-[:KNOWS]->(c),
                (a)-[:KNOWS]->(d),
                (a)-[:KNOWS]->(e)
      """.stripMargin)
    synopsis("`LIMIT` constrains the number of rows in the output.")
    p(
      """* <<limit-introduction, Introduction>>
        |* <<limit-subset-rows, Return a subset of the rows>>
        |* <<limit-subset-rows-using-expression, Using an expression with `LIMIT` to return a subset of the rows>>
        |* <<limit-will-not-stop-side-effects, `LIMIT` will not stop side effects>>
      """.stripMargin)
    section("Introduction", "limit-introduction") {
      p("""`LIMIT` accepts any expression that evaluates to a positive integer -- however the expression cannot refer to nodes or relationships.""")
      graphViz()
    }
    section("Return a subset of the rows", "limit-subset-rows") {
      p(
        """To return a subset of the result, starting from the top, use this syntax:""".stripMargin)
      query(
        """MATCH (n)
          |RETURN n.name
          |ORDER BY n.name
          |LIMIT 3""".stripMargin, ResultAssertions((r) => {
        r.toList should equal(List(Map("n.name" -> "A"), Map("n.name" -> "B"), Map("n.name" -> "C")))
      })) {
        p("The top three items are returned by the example query.")
        resultTable()
      }
    }
    section("Using an expression with `LIMIT` to return a subset of the rows", "limit-subset-rows-using-expression") {
      p(
        """Limit accepts any expression that evaluates to a positive integer as long as it is not referring to any external variables:""".stripMargin)
      query(
        """MATCH (n)
          |RETURN n.name
          |ORDER BY n.name
          |LIMIT toInteger(3 * rand()) + 1""".stripMargin, ResultAssertions((r) => {
          r.toSet should contain(Map("n.name" -> "A"))
        })) {
        p("Returns one to three top items.")
        resultTable()
      }
    }
    section("`LIMIT` will not stop side effects", "limit-will-not-stop-side-effects") {
      p(
        """The use of `LIMIT` in a query will not stop side effects, like `CREATE`, `DELETE` or `SET`, from happening if
          |the limit is in the same query part as the side effect. This behaviour was undefined in versions before
          |4.3.""".stripMargin
      )
      query(
        """CREATE (n)
          |RETURN n
          |LIMIT 0""".stripMargin,
        ResultAssertions((r) => {
          r.size shouldBe 0
          assertStats(r, nodesCreated = 1)
        })
      ) {
        p("This query returns nothing, but creates one node:")
        resultTable()
      }
      query(
        """MATCH (n {name: 'A'})
          |SET n.age = 60
          |RETURN n
          |LIMIT 0""".stripMargin,
        ResultAssertions((r) => {
          r.size shouldBe 0
          assertStats(r, propertiesWritten = 1)
        })
      ) {
        p("This query returns nothing, but writes one property:")
        resultTable()
      }
      p(
        "If we want to limit the number of updates we can split the query using the `WITH` clause:"
      )
      query(
        """MATCH (n)
          |WITH n LIMIT 1
          |SET n.locked = true
          |RETURN n""".stripMargin,
        ResultAssertions((r) => {
          r.size shouldBe 1
          assertStats(r, propertiesWritten = 1)
        })
      ) {
        p("Writes `locked` property on one node and return that node:")
        resultTable()
      }
    }
  }.build()
}
