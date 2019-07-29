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

import org.neo4j.cypher.docgen.tooling._

class PredicateFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Predicate functions", "query-functions-predicate")
    initQueries(
      """CREATE (alice {name:'Alice', age: 38, eyes: 'brown'}),
        |       (bob {name: 'Bob', age: 25, eyes: 'blue'}),
        |       (charlie {name: 'Charlie', age: 53, eyes: 'green'}),
        |       (daniel {name: 'Daniel', age: 54, eyes: 'brown'}),
        |       (eskil {name: 'Eskil', age: 41, eyes: 'blue', array: ['one', 'two', 'three']}),
        |
        |       (alice)-[:KNOWS]->(bob),
        |       (alice)-[:KNOWS]->(charlie),
        |       (bob)-[:KNOWS]->(daniel),
        |       (charlie)-[:KNOWS]->(daniel),
        |       (bob)-[:MARRIED]->(eskil)""")
    synopsis(
      """Predicates are boolean functions that return true or false for a given set of input.
        |They are most commonly used to filter out subgraphs in the `WHERE` part of a query.""".stripMargin)
    p(
      """* <<functions-all,all()>>
        |* <<functions-any,any()>>
        |* <<functions-exists,exists()>>
        |* <<functions-none,none()>>
        |* <<functions-single,single()>>""")
    graphViz()
    section("all()", "functions-all") {
      p("Tests whether a predicate holds for all elements of this list.")
      function("all(variable IN list WHERE predicate)", ("list", "An expression that returns a list"), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all items in the list."))
      query(
        """MATCH p = (a)-[*1..3]->(b)
          |WHERE a.name = 'Alice' AND b.name = 'Daniel'
          |AND all(x IN nodes(p) WHERE x.age > 30)
          |RETURN p""".stripMargin, ResultAssertions((r) => {
          r.toList.length should equal(1)
        })) {
        p("All nodes in the returned paths will have an `age` property of at least *'30'*.")
        resultTable()
      }
    }
    section("any()", "functions-any") {
      p("Tests whether a predicate holds for at least one element in the list.")
      function("any(variable IN list WHERE predicate)", ("list", "An expression that returns a list"), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all items in the list."))
      query(
        """MATCH (a)
          |WHERE a.name = 'Eskil'
          |AND any(x IN a.array WHERE x = 'one')
          |RETURN a.name, a.array""".stripMargin, ResultAssertions((r) => {
          r.toList.length should equal(1)
          r.columnAs[String]("a.name").toList.head should equal("Eskil")
        })) {
        p("All nodes in the returned paths have at least one *'one'* value set in the array property named `array`.")
        resultTable()
      }
    }
    section("exists()", "functions-exists") {
      p("Returns true if a match for the pattern exists in the graph, or the property exists in the node, relationship or map.")
      function("exists(pattern-or-property)", ("pattern-or-property", "A pattern or a property (in the form 'variable.prop')."))
      query(
        """MATCH (n)
          |WHERE exists(n.name)
          |RETURN n.name AS name, exists((n)-[:MARRIED]->()) AS is_married""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice", "is_married" -> false), Map("name" -> "Bob", "is_married" -> true), Map("name" -> "Charlie", "is_married" -> false), Map("name" -> "Daniel", "is_married" -> false), Map("name" -> "Eskil", "is_married" -> false)))
        })) {
        p("This query returns the names of all nodes with a name property along with a boolean `true` / `false` indicating if they are married.")
        resultTable()
      }
    }
    section("none()", "functions-none") {
      p("Returns true if the predicate holds for no element in the list.")
      function("none(variable IN list WHERE predicate)", ("list", "An expression that returns a list"), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all items in the list."))
      query(
        """MATCH p = (n)-[*1..3]->(b)
          |WHERE n.name = 'Alice'
          |AND none(x IN nodes(p) WHERE x.age = 25) RETURN p""".stripMargin, ResultAssertions((r) => {
          r.toList.length should equal(2)
        })) {
        p("No nodes in the returned paths has an `age` property set to *'25'*.")
        resultTable()
      }
    }
    section("single()", "functions-single") {
      p("Returns true if the predicate holds for exactly one of the elements in the list.")
      function("single(variable IN list WHERE predicate)", ("list", "An expression that returns a list"), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all items in the list."))
      query(
        """MATCH p = (n)-->(b)
          |WHERE n.name = 'Alice'
          |AND single(var IN nodes(p) WHERE var.eyes = 'blue')
          |RETURN p""".stripMargin, ResultAssertions((r) => {
          r.toList.length should equal(1)
        })) {
        p("Exactly one node in every returned path will have the `eyes` property set to *'blue'*.")
        resultTable()
      }
    }
  }.build()
}


