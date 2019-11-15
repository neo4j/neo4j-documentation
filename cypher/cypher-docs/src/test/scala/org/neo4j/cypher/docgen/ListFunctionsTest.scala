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

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.{Node, Relationship}

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
      """Functions:
        |
        |* <<functions-extract,extract()>>
        |* <<functions-filter,filter()>>
        |* <<functions-keys,keys()>>
        |* <<functions-labels,labels()>>
        |* <<functions-nodes,nodes()>>
        |* <<functions-range,range()>>
        |* <<functions-reduce,reduce()>>
        |* <<functions-relationships,relationships()>>
        |* <<functions-reverse-list, reverse()>>
        |* <<functions-tail,tail()>>""")
    graphViz()

    p("[role=deprecated]")
    section("extract()", "functions-extract") {
      p(
        """`extract()` returns a list `l~result~` containing the values resulting from an expression which has been applied to each element in a list `list`.
          |This function is analogous to the `map` method in functional languages such as Lisp and Scala. Note that this function has been deprecated, consider using a <<cypher-list-comprehension, list comprehension>> (e.g. `[variable IN list | expression]`) instead.""".stripMargin)
      function("extract(variable IN list | expression)", "A list containing heterogeneous elements; the types of the elements are determined by `expression`.", ("list", "An expression that returns a list."), ("variable", "The closure will have a variable introduced in its context. We decide here which variable to use."), ("expression", "This expression will run once per value in `list`, and add it to the list which is returned by `extract()`."))
      considerations("Any `null` values in `list` are preserved.")
      p(
        """
          |Common usages of `extract()` include:
          |
          |* Returning a property from a list of nodes or relationships; for example, `expression` = `n.prop` and `list` = `nodes(<some-path>)`.
          |* Returning the result of the application of a function on each element in a list; for example, `expression` = `toUpper(x)` and `variable` = `x`.""".stripMargin)
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND b.name = 'Bob' AND c.name = 'Daniel'
          |RETURN [n IN nodes(p) | n.age] AS extracted""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("extracted" -> List(38, 25, 54))))
        })) {
        p("The `age` property of all nodes in path `p` are returned.")
        resultTable()
      }
    }
    p("[role=deprecated]")
    section("filter()", "functions-filter") {
      p("""`filter()` returns a list `l~result~` containing all the elements from a list `list` that comply with the given predicate. Note that this function has been deprecated, consider using a <<cypher-list-comprehension, list comprehension>> (e.g. `[variable IN list WHERE predicate]`) instead.""")
      function("filter(variable IN list WHERE predicate)", "A list containing heterogeneous elements; the types of the elements are determined by the elements in `list`.", ("list", "An expression that returns a list."), ("variable", "This is the variable that can be used from the predicate."), ("predicate", "A predicate that is tested against all elements in `list`."))
      query(
        """MATCH (a)
          |WHERE a.name = 'Eskil'
          |RETURN a.array, [x IN a.array WHERE size(x)= 3]""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("[x IN a.array WHERE size(x)= 3]").toList.head should equal(Array("one", "two"))
        })) {
        p("The property named `array` and a list of all values having size *'3'* are returned.")
        resultTable()
      }
    }
    section("keys()", "functions-keys") {
      p("""`keys` returns a list containing the string representations for all the property names of a node, relationship, or map.""")
      function("keys(expression)", "A list containing String elements.", ("expression", "An expression that returns a node, a relationship, or a map."))
      considerations("`keys(null)` returns `null`.")
      query(
        """MATCH (a) WHERE a.name = 'Alice'
          |RETURN keys(a)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("keys(a)").toList.head should contain theSameElementsAs Array("name", "age", "eyes")
        })) {
        p("A list containing the names of all the properties on the node bound to `a` is returned.")
        resultTable()
      }
    }
    section("labels()", "functions-labels") {
      p("""`labels` returns a list containing the string representations for all the labels of a node.""")
      function("labels(node)", "A list containing String elements.", ("node", "An expression that returns a single node."))
      considerations("`labels(null)` returns `null`.")
      query(
        """MATCH (a) WHERE a.name = 'Alice'
          |RETURN labels(a)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("labels(a)").toList.head should equal(Array("Person", "Developer"))
        })) {
        p("A list containing all the labels of the node bound to `a` is returned.")
        resultTable()
      }
    }
    section("nodes()", "functions-nodes") {
      p("`nodes()` returns a list containing all the nodes in a path.")
      function("nodes(path)", "A list containing Node elements.", ("path", "An expression that returns a path."))
      considerations("`nodes(null)` returns `null`.")
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND c.name = 'Eskil'
          |RETURN nodes(p)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Seq[Node]]("nodes(p)").toList.head.map(_.getId) should equal(Array(0, 1, 4))
        })) {
        p("A list containing all the nodes in the path `p` is returned.")
        resultTable()
      }
    }
    section("range()", "functions-range") {
      p(
        """`range()` returns a list comprising all integer values within a range bounded by a start value `start` and end value `end`, where the difference `step` between any two consecutive values is constant; i.e. an arithmetic progression.
          |The range is inclusive, and the arithmetic progression will therefore always contain `start` and -- depending on the values of `start`, `step` and `end` -- `end`.""".stripMargin)
      function("range(start, end [, step])", "A list of Integer elements.", ("start", "An expression that returns an integer value."), ("end", "An expression that returns an integer value."), ("step", "A numeric expression defining the difference between any two consecutive values, with a default of `1`."))
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
        """`reduce()` returns the value resulting from the application of an expression on each successive element in a list in conjunction with the result of the computation thus far.
           This function will iterate through each element `e` in the given list, run the expression on `e` -- taking into account the current partial result -- and store the new partial result in the accumulator.
           This function is analogous to the `fold` or `reduce` method in functional languages such as Lisp and Scala.""")
      function("reduce(accumulator = initial, variable IN list | expression)", "The type of the value returned depends on the arguments provided, along with the semantics of `expression`.", ("accumulator", "A variable that will hold the result and the partial results as the list is iterated."), ("initial", "An expression that runs once to give a starting value to the accumulator."), ("list", "An expression that returns a list."), ("variable", "The closure will have a variable introduced in its context. We decide here which variable to use."), ("expression", "This expression will run once per value in the list, and produce the result value."))
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
      p("""`relationships()` returns a list containing all the relationships in a path.""")
      function("relationships(path)", "A list containing Relationship elements.", ("path", "An expression that returns a path."))
      considerations("`relationships(null)` returns `null`.")
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice' AND c.name = 'Eskil'
          |RETURN relationships(p)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Seq[Relationship]]("relationships(p)").toList.head.map(_.getId) should equal(Array(0, 4))
        })) {
        p("A list containing all the relationships in the path `p` is returned.")
        resultTable()
      }
    }
    section("reverse()", "functions-reverse-list") {
      p("""`reverse()` returns a list in which the order of all elements in the original list have been reversed.""")
      function("reverse(original)", "A list containing homogeneous or heterogeneous elements; the types of the elements are determined by the elements within `original`.", ("original", "An expression that returns a list."))
      considerations("Any `null` element in `original` is preserved.")
      query(
        """WITH [4923,'abc',521, null, 487] AS ids
          |RETURN reverse(ids)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("reverse(ids)" -> Vector(487, null, 521, "abc", 4923))))
        })) {
        resultTable()
      }
    }
    section("tail()", "functions-tail") {
      p("""`tail()` returns a list `l~result~` containing all the elements, excluding the first one, from a list `list`.""")
      function("tail(list)", "A list containing heterogeneous elements; the types of the elements are determined by the elements in `list`.", ("list", "An expression that returns a list."))
      query(
        """MATCH (a) WHERE a.name = 'Eskil'
          |RETURN a.array, tail(a.array)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("tail(a.array)").toList.head should equal(Array("two", "three"))
        })) {
        p("The property named `array` and a list comprising all but the first element of the `array` property are returned.")
        resultTable()
      }
    }
  }.build()
}
