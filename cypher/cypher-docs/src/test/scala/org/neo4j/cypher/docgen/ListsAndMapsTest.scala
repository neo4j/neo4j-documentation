/*
 * Copyright (c) 2002-2019 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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


class ListsAndMapsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Lists", "cypher-lists")
    initQueries(
      """CREATE (charlie:Person {name: 'Charlie Sheen',  realName: 'Carlos Irwin Estévez'}),
                |(martin:Person {name: 'Martin Sheen'}),
                |(wallstreet:Movie {title: 'Wall Street', year: 1987}),
                |(reddawn:Movie {title: 'Red Dawn', year: 1984}),
                |(apocalypsenow:Movie {title: 'Apocalypse Now', year: 1979}),
                |
                |(charlie)-[:ACTED_IN]->(wallstreet),
                |(charlie)-[:ACTED_IN]->(reddawn),
                |(charlie)-[:ACTED_IN]->(apocalypsenow),
                |(martin)-[:ACTED_IN]->(wallstreet),
                |(martin)-[:ACTED_IN]->(apocalypsenow)

      """.stripMargin)
    synopsis("Cypher has comprehensive support for lists.")
    p(
      """* <<cypher-lists-general,Lists in general>>
        |* <<cypher-list-comprehension,List comprehension>>
        |* <<cypher-literal-maps,Literal maps>>""")
    section("Lists in general", "cypher-lists-general") {
      p(
        """A literal list is created by using brackets and separating the elements in the list with commas.""".stripMargin)
      query(
        """RETURN [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] AS list""", ResultAssertions((r) => {
          r.toList should equal(List(Map("list" -> List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))))
        })) {
        resultTable()
      }
      p(
        """In our examples, we'll use the <<functions-range,`range`>> function.
          |It gives you a list containing all numbers between given start and end numbers.
          |Range is inclusive in both ends.""".stripMargin)
      p("""To access individual elements in the list, we use the square brackets again.
          |This will extract from the start index and up to but not including the end index.""")
      query(
        """RETURN range(0, 10)[3]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[3]" -> 3)))
        })) {
        resultTable()
      }
      p("You can also use negative numbers, to start from the end of the list instead.")
      query(
        """RETURN range(0, 10)[-3]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[-3]" -> 8)))
        })) {
        resultTable()
      }
      p("Finally, you can use ranges inside the brackets to return ranges of the list.")
      query(
        """RETURN range(0, 10)[0..3]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[0..3]" -> List(0, 1, 2))))
        })) {
        resultTable()
      }
      query(
        """RETURN range(0, 10)[0..-5]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[0..-5]" -> List(0, 1, 2, 3, 4, 5))))
        })) {
        resultTable()
      }
      query(
        """RETURN range(0, 10)[-5..]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[-5..]" -> List(6, 7, 8, 9, 10))))
        })) {
        resultTable()
      }
      query(
        """RETURN range(0, 10)[..4]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[..4]" -> List(0, 1, 2, 3))))
        })) {
        resultTable()
      }
      note {
        p("Out-of-bound slices are simply truncated, but out-of-bound single elements return `null`.")
      }
      query(
        """RETURN range(0, 10)[15]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[15]" -> null)))
        })) {
        resultTable()
      }
      query(
        """RETURN range(0, 10)[5..15]""", ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)[5..15]" -> List(5, 6, 7, 8, 9, 10))))
        })) {
        resultTable()
      }
      p("You can get the <<functions-size,`size`>> of a list as follows:")
      query(
        """RETURN size(range(0, 10)[0..3])""", ResultAssertions((r) => {
          r.toList should equal(List(Map("size(range(0, 10)[0..3])" -> 3)))
        })) {
        resultTable()
      }
    }
    section("List comprehension", "cypher-list-comprehension") {
      p(
        """List comprehension is a syntactic construct available in Cypher for creating a list based on existing lists.
          |It follows the form of the mathematical set-builder notation (set comprehension) instead of the use of map and filter functions.""".stripMargin)
      query(
        """RETURN [x IN range(0,10) WHERE x % 2 = 0 | x^3 ] AS result""", ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> List(0.0,8.0,64.0,216.0,512.0,1000.0))))
        })) {
        resultTable()
      }
      p("Either the `WHERE` part, or the expression, can be omitted, if you only want to filter or map respectively.")
      query(
        """RETURN [x IN range(0,10) WHERE x % 2 = 0 ] AS result""", ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> List(0, 2, 4, 6, 8, 10))))
        })) {
        resultTable()
      }
      query(
        """RETURN [x IN range(0,10) | x^3 ] AS result""", ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> List(0.0,1.0,8.0,27.0,64.0,125.0,216.0,343.0,512.0,729.0,1000.0))))
        })) {
        resultTable()
      }
    }


    section("Literal maps", "cypher-literal-maps") {
      p(
        """From Cypher, you can also construct maps.
          |Through REST you will get JSON objects; in Java they will be `java.util.Map<String,Object>`.""".stripMargin)
      query(
        """RETURN {key: 'Value', listKey: [{inner: 'Map1'}, {inner: 'Map2'}]} AS mapLiteral""", ResultAssertions((r) => {
          r.toList should equal(List(Map("mapLiteral" -> Map("key" -> "Value", "listKey" -> List(Map("inner" -> "Map1"), Map("inner" -> "Map2"))))))
        })) {
        resultTable()
      }
    }
  }.build()
}
