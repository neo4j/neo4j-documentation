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
        |* <<functions-keys,keys()>>
        |* <<functions-labels,labels()>>
        |* <<functions-nodes,nodes()>>
        |* <<functions-range,range()>>
        |* <<functions-reduce,reduce()>>
        |* <<functions-relationships,relationships()>>
        |* <<functions-reverse-list, reverse()>>
        |* <<functions-tail,tail()>>
        |* <<functions-tobooleanlist,toBooleanList()>>
        |* <<functions-tofloatlist,toFloatList()>>
        |* <<functions-tointegerlist,toIntegerList()>>
        |* <<functions-tostringlist,toStringList()>>
        |""")
    graphViz()


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
          |To create ranges with decreasing integer values, use a negative value `step`.
          |The range is inclusive for non-empty ranges, and the arithmetic progression will therefore always contain `start` and -- depending on the values of `start`, `step` and `end` -- `end`.
          |The only exception where the range does not contain `start` are empty ranges.
          |An empty range will be returned if the value `step` is negative and `start - end` is positive, or vice versa, e.g. `range(0, 5, -1)`.
          |""".stripMargin)
      function("range(start, end [, step])", "A list of Integer elements.", ("start", "An expression that returns an integer value."), ("end", "An expression that returns an integer value."), ("step", "A numeric expression defining the difference between any two consecutive values, with a default of `1`."))
      query(
        """RETURN range(0, 10), range(2, 18, 3), range(0, 5, -1)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("range(0, 10)" -> List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), "range(2, 18, 3)" -> List(2, 5, 8, 11, 14, 17), "range(0, 5, -1)" -> List())))
        })) {
        p("Three lists of numbers in the given ranges are returned.")
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
    section("toBooleanList()", "functions-tobooleanlist") {
      p("""`toBooleanList()` converts a list of values and returns a list of boolean values. If any values are not convertible to boolean they will be null in the list returned.""")
      function("toBooleanList(list)", "A list containing the converted elements; depending on the input value a converted value is either a boolean value or `null`.", ("list", "An expression that returns a list."))
      considerations("Any `null` element in `list` is preserved.",
        "Any boolean value in `list` is preserved.",
        "If the `list` is `null`, `null` will be returned.",
        "If the `list` is not a list, an error will be returned.",
        "The conversion for each value in `original` is done according to the <<functions-tobooleanornull,`toBooleanOrNull()` function>>.")
      query(
        """RETURN toBooleanList(null) as noList,
          |toBooleanList([null, null]) as nullsInList,
          |toBooleanList(['a string', true, 'false', null, ['A','B']]) as mixedList""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("noList").toList.head should equal(null)
          r.columnAs[Iterable[_]]("nullsInList").toList.head should equal(Array(null, null))
          r.columnAs[Iterable[_]]("mixedList").toList.head should equal(Array(null, true, false, null, null))
        })) {
        resultTable()
      }
    }
    section("toFloatList()", "functions-tofloatlist") {
      p("""`toFloatList()` converts a list of values and returns a list of floating point values. If any values are not convertible to floating point they will be `null` in the list returned.""")
      function("toFloatList(list)", "A list containing the converted elements; depending on the input value a converted value is either a floating point value or `null`.", ("list", "An expression that returns a list."))
      considerations("Any `null` element in `list` is preserved.",
        "Any floating point value in `list` is preserved.",
        "If the `list` is `null`, `null` will be returned.",
        "If the `list` is not a list, an error will be returned.",
        "The conversion for each value in `original` is done according to the <<functions-tofloatornull,`toFloatOrNull()` function>>.")
      query(
        """RETURN toFloatList(null) as noList,
          |toFloatList([null, null]) as nullsInList,
          |toFloatList(['a string', 2.5, '3.14159', null, ['A','B']]) as mixedList""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("noList").toList.head should equal(null)
          r.columnAs[Iterable[_]]("nullsInList").toList.head should equal(Array(null, null))
          r.columnAs[Iterable[_]]("mixedList").toList.head should equal(Array(null, 2.5, 3.14159, null, null))
        })) {
        resultTable()
      }
    }
    section("toIntegerList()", "functions-tointegerlist") {
      p("""`toIntegerList()` converts a list of values and returns a list of integer values. If any values are not convertible to integer they will be `null` in the list returned.""")
      function("toIntegerList(list)", "A list containing the converted elements; depending on the input value a converted value is either a integer value or `null`.", ("list", "An expression that returns a list."))
      considerations("Any `null` element in `list` is preserved.",
        "Any integer value in `list` is preserved.",
        "If the `list` is `null`, `null` will be returned.",
        "If the `list` is not a list, an error will be returned.",
        "The conversion for each value in `original` is done according to the <<functions-tointegerornull,`toIntegerOrNull()` function>>.")
      query(
        """RETURN toIntegerList(null) as noList,
          |toIntegerList([null, null]) as nullsInList,
          |toIntegerList(['a string', 2, '5', null, ['A','B']]) as mixedList""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("noList").toList.head should equal(null)
          r.columnAs[Iterable[_]]("nullsInList").toList.head should equal(Array(null, null))
          r.columnAs[Iterable[_]]("mixedList").toList.head should equal(Array(null, 2, 5, null, null))
        })) {
        resultTable()
      }
    }
    section("toStringList()", "functions-tostringlist") {
      p("""`toStringList()` converts a list of values and returns a list of string values. If any values are not convertible to string they will be `null` in the list returned.""")
      function("toStringList(list)", "A list containing the converted elements; depending on the input value a converted value is either a string value or `null`.", ("list", "An expression that returns a list."))
      considerations("Any `null` element in `list` is preserved.",
        "Any string value in `list` is preserved.",
        "If the `list` is `null`, `null` will be returned.",
        "If the `list` is not a list, an error will be returned.",
        "The conversion for each value in `original` is done according to the <<functions-tostringornull,`toStringOrNull()` function>>.")
      query(
        """RETURN toStringList(null) as noList,
          |toStringList([null, null]) as nullsInList,
          |toStringList(['already a string', 2, date({year:1955, month:11, day:5}), null, ['A','B']]) as mixedList""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Iterable[_]]("noList").toList.head should equal(null)
          r.columnAs[Iterable[_]]("nullsInList").toList.head should equal(Array(null, null))
          r.columnAs[Iterable[_]]("mixedList").toList.head should equal(Array("already a string", "2", "1955-11-05", null, null))
        })) {
        resultTable()
      }
    }
  }.build()
}
