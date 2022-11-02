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

import org.neo4j.cypher.docgen.tooling.DocBuilder
import org.neo4j.cypher.docgen.tooling.DocumentingTest
import org.neo4j.cypher.docgen.tooling.ResultAssertions

class OptionalMatchTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("OPTIONAL MATCH", "query-optional-match")

    initQueries(
      """CREATE
        #  (charlie:Person {name: 'Charlie Sheen'}),
        #  (martin:Person {name: 'Martin Sheen'}),
        #  (michael:Person {name: 'Michael Douglas'}),
        #  (oliver:Person {name: 'Oliver Stone'}),
        #  (rob:Person {name: 'Rob Reiner'}),
        #  (wallStreet:Movie {title: 'Wall Street'}),
        #  (charlie)-[:ACTED_IN]->(wallStreet),
        #  (martin)-[:ACTED_IN]->(wallStreet),
        #  (michael)-[:ACTED_IN]->(wallStreet),
        #  (oliver)-[:DIRECTED]->(wallStreet),
        #  (thePresident:Movie {title: 'The American President'}),
        #  (martin)-[:ACTED_IN]->(thePresident),
        #  (michael)-[:ACTED_IN]->(thePresident),
        #  (rob)-[:DIRECTED]->(thePresident),
        #  (charlie)-[:FATHER]->(martin)""".stripMargin('#')
    )

    synopsis(
      "The `OPTIONAL MATCH` clause is used to search for the pattern described in it, while using nulls for missing parts of the pattern."
    )

    p("""* <<optional-match-introduction, Introduction>>
        #* <<optional-relationships, Optional relationships>>
        #* <<properties-on-optional-elements, Properties on optional elements>>
        #* <<optional-typed-named-relationship, Optional typed and named relationship>>""".stripMargin('#'))

    section("Introduction", "optional-match-introduction") {
      p("""`OPTIONAL MATCH` matches patterns against your graph database, just like `MATCH` does.
          #The difference is that if no matches are found, `OPTIONAL MATCH` will use a `null` for missing parts of the pattern.
          #`OPTIONAL MATCH` could be considered the Cypher equivalent of the outer join in SQL.""".stripMargin('#'))
      p("""Either the whole pattern is matched, or nothing is matched.
          #Remember that `WHERE` is part of the pattern description, and the predicates will be considered while looking for matches, not after.
          #This matters especially in the case of multiple (`OPTIONAL`) `MATCH` clauses, where it is crucial to put `WHERE` together with the `MATCH` it belongs to.""".stripMargin(
        '#'
      ))
      tip {
        p("""To understand the patterns used in the `OPTIONAL MATCH` clause, read <<cypher-patterns>>.""")
      }
      p("""The following graph is used for the examples below:""")
      graphViz()
    }

    section("Optional relationships", "optional-relationships") {
      p("""If a relationship is optional, use the `OPTIONAL MATCH` clause.
          #This is similar to how a SQL outer join works.
          #If the relationship is there, it is returned.
          #If it's not, `null` is returned in its place.""".stripMargin('#'))
      query(
        """MATCH (a:Movie {title: 'Wall Street'})
          #OPTIONAL MATCH (a)-->(x)
          #RETURN x""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("x" -> null)))
        })
      ) {
        p("""Returns `null`, since the node has no outgoing relationships.""")
        resultTable()
      }
    }

    section("Properties on optional elements", "properties-on-optional-elements") {
      p("""Returning a property from an optional element that is `null` will also return `null`.""")
      query(
        """MATCH (a:Movie {title: 'Wall Street'})
          #OPTIONAL MATCH (a)-->(x)
          #RETURN x, x.name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("x" -> null, "x.name" -> null)))
        })
      ) {
        p("""Returns the element x (`null` in this query), and `null` as its name.""")
        resultTable()
      }
    }

    section("Optional typed and named relationship", "optional-typed-named-relationship") {
      p(
        "Just as with a normal relationship, you can decide which variable it goes into, and what relationship type you need."
      )
      query(
        """MATCH (a:Movie {title: 'Wall Street'})
          #OPTIONAL MATCH (a)-[r:ACTS_IN]->()
          #RETURN a.title, r""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("a.title" -> "Wall Street", "r" -> null)))
        })
      ) {
        p(
          """This returns the title of the node, *'Wall Street'*, and, since the node has no outgoing `ACTS_IN` relationships, `null` is returned for the relationship denoted by `r`."""
        )
        resultTable()
      }
    }
  }.build()
}
