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

class ForeachTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("FOREACH", "query-foreach")
    initQueries("""CREATE
                  #  (a:Person {name: 'A'}),
                  #  (b:Person {name: 'B'}),
                  #  (c:Person {name: 'C'}),
                  #  (d:Person {name: 'D'}),
                  #  (a)-[:KNOWS]->(b),
                  #  (b)-[:KNOWS]->(c),
                  #  (c)-[:KNOWS]->(d)""".stripMargin('#'))
    synopsis("The `FOREACH` clause is used to update data within a collection whether components of a path, or result of aggregation.")
    section("Introduction", "query-foreach-introduction") {
      p("""Lists and paths are key concepts in Cypher.
          #The `FOREACH` clause can be used to update data, such as executing update commands on elements in a path, or on a list created by aggregation.""".stripMargin('#'))
      p("""The variable context within the `FOREACH` parenthesis is separate from the one outside it.
          #This means that if you `CREATE` a node variable within a `FOREACH`, you will _not_ be able to use it outside of the foreach statement, unless you match to find it.""".stripMargin('#'))
      p("Within the `FOREACH` parentheses, you can do any of the updating commands -- `SET`, `REMOVE`, `CREATE`, `MERGE`, `DELETE`, and `FOREACH`.")
      p("If you want to execute an additional `MATCH` for each element in a list then the <<query-unwind,`UNWIND`>> clause would be a more appropriate command.")
      graphViz()
    }
    section("Mark all nodes along a path", "foreach-mark-all-nodes-along-a-path") {
      p("This query will set the property `marked` to `true` on all nodes along a path.")
      query("""MATCH p=(start)-[*]->(finish)
              #WHERE start.name = 'A' AND finish.name = 'D'
              #FOREACH (n IN nodes(p) | SET n.marked = true)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList.length should equal(0);  assertStats(r, propertiesWritten = 4)
        })) {
        resultTable()
        //statsOnlyResultTable()
      }
    }
  }.build()

}
