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
import org.neo4j.graphdb.{Relationship, Node}

class ListFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("List functions", "query-functions-list")
    initQueries(
      """CREATE (alice:Person:Developer {name:'Alice', age: 38, eyes: 'brown'}),
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
    synopsis("List functions return lists of things -- nodes in a path, and so on.")
    p("Further details and examples of lists may be found in <<cypher-lists>> and <<query-operators-list>>.")
    p(
      """* <<functions-extract,extract()>>
        |* <<functions-filter,filter()>>
        |* <<functions-keys,keys()>>
        |* <<functions-labels,labels()>>
        |* <<functions-nodes,nodes()>>
        |* <<functions-range,range()>>
        |* <<functions-reduce,reduce()>>
        |* <<functions-relationships,relationships()>>
        |* <<functions-tail,tail()>>""")
    graphViz()
    section("extract()", "functions-extract") {
      p(
        """To return a single property, or the value of a function from a list of nodes or relationships, you can use `extract()`.
     It will go through a list, run an expression on every element, and return the results in a list with these values.
     It works like the `map` method in functional languages such as Lisp and Scala.""")
      function("extract(variable IN list | expression)", ("list", "An expression that returns a list"), ("variable", "The closure will have a variable introduced in its context. Here you decide which variable to use."), ("expression", "This expression will run once per value in the list, and produces the result list."))
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND b.name = 'Bob' AND c.name = 'Daniel'
          |RETURN extract(n IN nodes(p) | n.age) AS extracted""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("extracted" -> List(38, 25, 54))))
        })) {
        p("The `age` property of all nodes in the path are returned.")
        resultTable()
      }
    }
    section("filter()", "functions-filter") {
      p("""`filter()` returns all the elements in a list that comply to a predicate.""")
      function("filter(variable IN list WHERE predicate)", ("list", "An expression that returns a list"), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all items in the list."))
      query(
        """MATCH (a)
          |WHERE a.name = 'Eskil'
          |RETURN a.array, filter(x IN a.array WHERE size(x)= 3)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("filter(x IN a.array WHERE size(x)= 3)").toList.head should equal(Array("one", "two"))
        })) {
        p("This returns the property named `array` and a list of values in it, which have size *'3'*.")
        resultTable()
      }
    }
    section("keys()", "functions-keys") {
      p("""Returns a list of string representations for the property names of a node, relationship, or map.""")
      function("keys(expression)", ("expression", "An expression that returns a node, a relationship, or a map."))
      query(
        """MATCH (a) WHERE a.name = 'Alice'
          |RETURN keys(a)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("keys(a)").toList.head should equal(Array("name", "age", "eyes"))
        })) {
        p("The name of the properties of `n` is returned by the query.")
        resultTable()
      }
    }
    section("labels()", "functions-labels") {
      p("""Returns a list of string representations for the labels attached to a node.""")
      function("labels(node)", ("node", "Any expression that returns a single node."))
      query(
        """MATCH (a) WHERE a.name = 'Alice'
          |RETURN labels(a)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("labels(a)").toList.head should equal(Array("Person", "Developer"))
        })) {
        p("The labels of `n` are returned by the query.")
        resultTable()
      }
    }
    section("nodes()", "functions-nodes") {
      p("""Returns all nodes in a path.""")
      function("nodes(path)", ("path", "A path."))
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND c.name = 'Eskil'
          |RETURN nodes(p)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Seq[Node]]("nodes(p)").toList.head.map(_.getId) should equal(Array(0, 1, 4))
        })) {
        p("All the nodes in the path `p` are returned by the example query.")
        resultTable()
      }
    }
    section("range()", "functions-range") {
      p(
        """`range()` returns numerical values in a range. The default distance between values in the range is `1`.
          |The range is inclusive in both ends.""".stripMargin)
      function("range(start, end [, step])", ("start", "A numerical expression."), ("end", "A numerical expression."), ("step", "A numerical expression."))
      query(
        """RETURN range(0, 10), range(2, 18, 3)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)" -> List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), "range(2, 18, 3)" -> List(2, 5, 8, 11, 14, 17))))
        })) {
        p("Two lists of numbers in the given ranges are returned.")
        resultTable()
      }
    }
    section("reduce()", "functions-reduce") {
      p(
        """To run an expression against individual elements of a list, and store the result of the expression in an accumulator, you can use `reduce()`.
     It will go through a list, run an expression on every element, storing the partial result in the accumulator.
     It works like the `fold` or `reduce` method in functional languages such as Lisp and Scala.""")
      function("reduce(accumulator = initial, variable IN list | expression)", ("accumulator", "A variable that will hold the result and the partial results as the list is iterated."), ("initial", "An expression that runs once to give a starting value to the accumulator."), ("list", "An expression that returns a list."), ("variable", "The closure will have a variable introduced in its context. Here you decide which variable to use."), ("expression", "This expression will run once per value in the list, and produces the result value."))
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND b.name = 'Bob' AND c.name = 'Daniel'
          |RETURN reduce(totalAge = 0, n IN nodes(p) | totalAge + n.age) AS reduction""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("reduction" -> 117)))
        })) {
        p("The `age` property of all nodes in the path are summed and returned as a single value.")
        resultTable()
      }
    }
    section("relationships()", "functions-relationships") {
      p("""Returns all relationships in a path.""")
      function("relationships(path)", ("path", "A path."))
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND c.name = 'Eskil'
          |RETURN relationships(p)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Seq[Relationship]]("relationships(p)").toList.head.map(_.getId) should equal(Array(0, 4))
        })) {
        p("All the relationships in the path `p` are returned.")
        resultTable()
      }
    }
    section("tail()", "functions-tail") {
      p("""`tail()` returns all but the first element in a list.""")
      function("tail(expression)", ("expression", "This expression should return a list of some kind."))
      query(
        """MATCH (a) WHERE a.name = 'Eskil'
          |RETURN a.array, tail(a.array)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("tail(a.array)").toList.head should equal(Array("two", "three"))
        })) {
        p("This returns the property named `array` and all elements of that property except the first one.")
        resultTable()
      }
    }
  }.build()
}
