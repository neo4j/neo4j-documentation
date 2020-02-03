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
package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class WithTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("WITH", "query-with")
    initQueries(
      """CREATE (a {name: 'Anders'}),
                (b {name: 'Bossman'}),
                (c {name: 'Ceasar'}),
                (d {name: 'David'}),
                (e {name: 'George'}),

                (a)-[:KNOWS]->(b),
                (a)-[:BLOCKS]->(c),
                (d)-[:KNOWS]->(a),
                (b)-[:KNOWS]->(e),
                (c)-[:KNOWS]->(e),
                (b)-[:BLOCKS]->(d)
      """.stripMargin)
    synopsis("The `WITH` clause allows query parts to be chained together, piping the results from one to be used as starting points or criteria in the next.")
    p(
      """* <<with-introduction, Introduction>>
        |* <<with-filter-on-aggregate-function-results, Filter on aggregate function results>>
        |* <<with-sort-results-before-using-collect-on-them, Sort results before using collect on them>>
        |* <<with-limit-branching-of-path-search, Limit branching of a path search>>
      """.stripMargin)
    section("Introduction", "with-introduction") {
      p(
        """Using `WITH`, you can manipulate the output before it is passed on to the following query parts.
          |The manipulations can be of the shape and/or number of entries in the result set.""".stripMargin)
      p(
        """One common usage of `WITH` is to limit the number of entries that are then passed on to other `MATCH` clauses.
          |By combining `ORDER BY` and `LIMIT`, it's possible to get the top X entries by some criteria, and then bring in additional data from the graph.""".stripMargin)
      p(
        """Another use is to filter on aggregated values.
          |`WITH` is used to introduce aggregates which can then be used in predicates in `WHERE`.
          |These aggregate expressions create new bindings in the results.
          |`WITH` can also, like `RETURN`, alias expressions that are introduced into the results using the aliases as the binding name.""".stripMargin)
      p(
        """`WITH` is also used to separate reading from updating of the graph.
          |Every part of a query must be either read-only or write-only.
          |When going from a writing part to a reading part, the switch must be done with a `WITH` clause.""".stripMargin)
      graphViz()
    }
    section("Filter on aggregate function results", "with-filter-on-aggregate-function-results") {
      p(
        """Aggregated results have to pass through a `WITH` clause to be able to filter on.""".stripMargin)
      query(
        """MATCH (david {name: 'David'})--(otherPerson)-->()
          |WITH otherPerson, count(*) AS foaf
          |WHERE foaf > 1
          |RETURN otherPerson.name""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("otherPerson.name" -> "Anders")))
        })) {
        p("The name of the person connected to *'David'* with the at least more than one outgoing relationship will be returned by the query.")
        resultTable()
      }
    }
    section("Sort results before using collect on them", "with-sort-results-before-using-collect-on-them") {
      p(
        """You can sort your results before passing them to collect, thus sorting the resulting list.""".stripMargin)
      query(
        """MATCH (n)
          |WITH n
          |ORDER BY n.name DESC
          |LIMIT 3
          |RETURN collect(n.name)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("collect(n.name)" -> List("George", "David", "Ceasar"))))
        })) {
        p("A list of the names of people in reverse order, limited to 3, is returned in a list.")
        resultTable()
      }
    }
    section("Limit branching of a path search", "with-limit-branching-of-path-search") {
      p(
        """You can match paths, limit to a certain number, and then match again using those paths as a base, as well as any number of similar limited searches.""".stripMargin)
      query(
        """MATCH (n {name: 'Anders'})--(m)
          |WITH m
          |ORDER BY m.name DESC
          |LIMIT 1
          |MATCH (m)--(o)
          |RETURN o.name""".stripMargin, ResultAssertions((r) => {
          r.toSet should equal(Set(Map("o.name" -> "Bossman"), Map("o.name" -> "Anders")))
        })) {
        p("Starting at *'Anders'*, find all matching nodes, order by name descending and get the top result, then find all the nodes connected to that top result, and return their names.")
        resultTable()
      }
    }
  }.build()
}
