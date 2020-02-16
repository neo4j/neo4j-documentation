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

import org.neo4j.cypher.docgen.tooling._

class MapsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Maps", "cypher-maps")
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
    synopsis("This section describes how to use maps in Cyphers.")
    p(
      """* <<cypher-literal-maps, Literal maps>>
        |* <<cypher-map-projection, Map projection>>
        |** <<cypher-map-projection-examples, Examples of map projection>>""")
    p("The following graph is used for the examples below:")
    graphViz()
    note{
      p(
        """Information regarding property access operators such as `.` and `[]` can be found <<query-operators-map, here>>.
          |The behavior of the `[]` operator with respect to `null` is detailed <<cypher-null-bracket-operator, here>>.""".stripMargin)
    }
    section("Literal maps", "cypher-literal-maps") {
      p(
        """Cypher supports construction of maps.
          |The key names in a map must be of type `String`.
          |If returned through an <<http-api#http-api, HTTP API call>>, a JSON object will be returned.
          |If returned in Java, an object of type `java.util.Map<String,Object>` will be returned.
          |""".stripMargin)
      query(
        """RETURN { key: 'Value', listKey: [{ inner: 'Map1' }, { inner: 'Map2' }]}""", ResultAssertions((r) => {
          r.toList should equal(List(Map("{ key: 'Value', listKey: [{ inner: 'Map1' }, { inner: 'Map2' }]}" -> Map("key" -> "Value", "listKey" -> List(Map("inner" -> "Map1"), Map("inner" -> "Map2"))))))
        })) {
        resultTable()
      }
    }
    section("Map projection", "cypher-map-projection") {
      p(
        """Cypher supports a concept called "map projections".
          |It allows for easily constructing map projections from nodes, relationships and other map values.""")
      p(
        """A map projection begins with the variable bound to the graph entity to be projected from, and contains a body of comma-separated map elements, enclosed by `{` and  `}`.
        """)
      p("`map_variable {map_element, [, ...n]}`")
      p(
        """A map element projects one or more key-value pairs to the map projection.
          |There exist four different types of map projection elements:
          |
          |* Property selector - Projects the property name as the key, and the value from the `map_variable` as the value for the projection.
          |* Literal entry - This is a key-value pair, with the value being arbitrary expression `key: <expression>`.
          |* Variable selector - Projects a variable, with the variable name as the key, and the value the variable is pointing to as the value of the projection. Its syntax is just the variable.
          |* All-properties selector - projects all key-value pairs from the `map_variable` value.
          |""".stripMargin)
      p("""The following conditions apply:
          |
          |* If the `map_variable` points to a `null` value, the whole map projection will evaluate to `null`.
          |* The key names in a map must be of type `String`.
          |""".stripMargin)
      section("Examples of map projections", "cypher-map-projection-examples") {
        p(
          """Find *'Charlie Sheen'* and return data about him and the movies he has acted in.
            |This example shows an example of map projection with a literal entry, which in turn also uses map projection inside the aggregating `collect()`.""")
        query("""MATCH (actor:Person {name: 'Charlie Sheen'})-[:ACTED_IN]->(movie:Movie)
            |RETURN actor{ .name, .realName, movies: collect(movie{ .title, .year })}""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("actor" -> Map("name" -> "Charlie Sheen", "realName" -> "Carlos Irwin Estévez", "movies" -> List(Map("title" -> "Apocalypse Now", "year" -> 1979), Map("title" -> "Red Dawn", "year" -> 1984), Map("title" -> "Wall Street", "year" -> 1987))))))
          })) {
          resultTable()
        }
        p(
          """Find all persons that have acted in movies, and show number for each.
            |This example introduces an variable with the count, and uses a variable selector to project the value.""")
        query("""MATCH (actor:Person)-[:ACTED_IN]->(movie:Movie)
            |WITH actor, count(movie) as nrOfMovies
            |RETURN actor{ .name, nrOfMovies}""".stripMargin, ResultAssertions((r) => {
            r.toSet should equal(Set(Map("actor" -> Map("name" -> "Charlie Sheen", "nrOfMovies" -> 3)), Map("actor" -> Map("name" -> "Martin Sheen", "nrOfMovies" -> 2))))
          })) {
          resultTable()
        }
        p(
          """Again, focusing on *'Charlie Sheen'*, this time returning all properties from the node.
            |Here we use an all-properties selector to project all the node properties, and additionally, explicitly project the property `age`.
            |Since this property does not exist on the node, a `null` value is projected instead.""")
        query(
          """MATCH (actor:Person {name: 'Charlie Sheen'})
            |RETURN actor{.*, .age}""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("actor" -> Map("name" -> "Charlie Sheen", "realName" -> "Carlos Irwin Estévez", "age" -> null))))
          })) {
          resultTable()
        }
      }
    }

  }.build()
}


