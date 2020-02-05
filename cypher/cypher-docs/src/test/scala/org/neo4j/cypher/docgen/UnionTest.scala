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

class UnionTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("UNION", "query-union")
    initQueries("""CREATE (ah:Actor {name: 'Anthony Hopkins'}),
                  |       (hm:Actor {name: 'Helen Mirren'}),
                  |       (hitchcock:Actor {name: 'Hitchcock'}),
                  |       (hitchcockMovie:Movie {title: 'Hitchcock'}),
                  |       (ah)-[:KNOWS]->(hm),
                  |       (ah)-[:ACTS_IN]->(hitchcockMovie),
                  |       (hm)-[:ACTS_IN]->(hitchcockMovie)""")
    synopsis("The `UNION` clause is used to combine the result of multiple queries.")
    p(
      """* <<union-introduction, Introduction>>
        |* <<union-combine-queries-retain-duplicates, Combine two queries and retain duplicates>>
        |* <<union-combine-queries-remove-duplicates, Combine two queries and remove duplicates>>
      """.stripMargin)
    section("Introduction", "union-introduction") {
      p("`UNION` combines the results of two or more queries into a single result set that includes all the rows that belong to all queries in the union.")
      p("""The number and the names of the columns must be identical in all queries combined by using `UNION`.""")
      p(
        """To keep all the result rows, use `UNION ALL`.
          |Using just `UNION` will combine and remove duplicates from the result set.""".stripMargin)
      graphViz()
    }
    section("Combine two queries and retain duplicates", "union-combine-queries-retain-duplicates") {
      p(
        """Combining the results from two queries is done using `UNION ALL`.""".stripMargin)
      query(
        """MATCH (n:Actor)
          |RETURN n.name AS name
          |UNION ALL
          |MATCH (n:Movie)
          |RETURN n.title AS name""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Anthony Hopkins"), Map("name" -> "Helen Mirren"), Map("name" -> "Hitchcock"), Map("name" -> "Hitchcock")))
        })) {
        p("The combined result is returned, including duplicates.")
        resultTable()
      }
    }

    section("Combine two queries and remove duplicates", "union-combine-queries-remove-duplicates") {
      p(
        """By not including `ALL` in the `UNION`, duplicates are removed from the combined result set""".stripMargin)
      query(
        """MATCH (n:Actor)
          |RETURN n.name AS name
          |UNION
          |MATCH (n:Movie)
          |RETURN n.title AS name""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Anthony Hopkins"), Map("name" -> "Helen Mirren"), Map("name" -> "Hitchcock")))
        })) {
        p("The combined result is returned, without duplicates.")
        resultTable()
      }
    }
  }.build()

}
