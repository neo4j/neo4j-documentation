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

import java.util.UUID

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Node

class ScalarFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Scalar functions", "query-functions-scalar")
    initQueries(
      """CREATE (alice:Developer {name:'Alice', age: 38, eyes: 'brown'}),
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
    synopsis("Scalar functions return a single value.")
    important {
      p(
        """The `length()` and `size()` functions are quite similar, and so it is important to take note of the difference.
     Owing to backwards compatibility, `length()` currently works on four types: strings, paths, lists and pattern expressions.
     However, it is recommended to use `length()` only for paths, and the `size()` function for strings, lists and pattern expressions.
     `length()` on those types may be deprecated in future.""")
    }
    note{
      p("""The `timestamp()` function returns the equivalent value of `datetime().epochMillis`.""")
    }
    note {
      p("The function `toInt()` has been superseded by `toInteger()`, and will be removed in a future release.")
    }
    p(
      """Functions:
        |
        |* <<functions-coalesce, coalesce()>>
        |* <<functions-endnode, endNode()>>
        |* <<functions-head, head()>>
        |* <<functions-id, id()>>
        |* <<functions-last, last()>>
        |* <<functions-length, length()>>
        |* <<functions-properties, properties()>>
        |* <<functions-randomuuid, randomUUID()>>
        |* <<functions-size, size()>>
        |* <<functions-size-of-pattern-expression, Size of pattern expression>>
        |* <<functions-size-of-string, Size of string>>
        |* <<functions-startnode, startNode()>>
        |* <<functions-timestamp, timestamp()>>
        |* <<functions-toboolean, toBoolean()>>
        |* <<functions-tofloat, toFloat()>>
        |* <<functions-tointeger, toInteger()>>
        |* <<functions-type, type()>>""")
    graphViz()
    section("coalesce()", "functions-coalesce") {
      p(
        "`coalesce()` returns the first non-`null` value in the given list of expressions.")
      function("coalesce(expression [, expression]*)", "The type of the value returned will be that of the first non-`null` expression.", ("expression", "An expression which may return `null`."))
      considerations("`null` will be returned if all the arguments are `null`.")
      query(
        """MATCH (a)
          |WHERE a.name = 'Alice'
          |RETURN coalesce(a.hairColor, a.eyes)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("coalesce(a.hairColor, a.eyes)" -> "brown")))
        })) {
        resultTable()
      }
    }
    section("endNode()", "functions-endnode") {
      p(
        """`endNode()` returns the end node of a relationship.""".stripMargin)
      function("endNode(relationship)", "A Node.", ("relationship", "An expression that returns a relationship."))
      considerations("`endNode(null)` returns `null`.")
      query(
        """MATCH (x:Developer)-[r]-()
          |RETURN endNode(r)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Node]("endNode(r)").toList.map(_.getId) should equal(Array(2, 1))
        })) {
        resultTable()
      }
    }
    section("head()", "functions-head") {
      p(
        """`head()` returns the first element in a list.""".stripMargin)
      function("head(list)", "The type of the value returned will be that of the first element of `list`.", ("list", "An expression that returns a list."))
      considerations("`head(null)` returns `null`.", "If the first element in `list` is `null`, `head(list)` will return `null`.")
      query(
        """MATCH (a)
          |WHERE a.name = 'Eskil'
          |RETURN a.array, head(a.array)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[String]("head(a.array)").toList.head should equal("one")
        })) {
        p("The first element in the list is returned.")
        resultTable()
      }
    }
    section("id()", "functions-id") {
      p(
        """`id()` returns the id of a relationship or node.""".stripMargin)
      function("id(expression)", "An Integer.", ("expression", "An expression that returns a node or a relationship."))
      considerations("`id(null)` returns `null`.")
      query(
        """MATCH (a)
          |RETURN id(a)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("id(a)" -> 0), Map("id(a)" -> 1), Map("id(a)" -> 2), Map("id(a)" -> 3), Map("id(a)" -> 4)))
        })) {
        p("The node id for each of the nodes is returned.")
        resultTable()
      }
    }
    section("last()", "functions-last") {
      p(
        """`last()` returns the last element in a list.""".stripMargin)
      function("last(expression)", "The type of the value returned will be that of the last element of `list`.", ("list", "An expression that returns a list."))
      considerations("`last(null)` returns `null`.", "If the last element in `list` is `null`, `last(list)` will return `null`.")
      query(
        """MATCH (a)
          |WHERE a.name = 'Eskil'
          |RETURN a.array, last(a.array)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[String]("last(a.array)").toList.head should equal("three")
        })) {
        p("The last element in the list is returned.")
        resultTable()
      }
    }
    section("length()", "functions-length") {
      p(
        """`length()` returns the length of a path.""".stripMargin)
      function("length(path)", "An Integer.", ("path", "An expression that returns a path."))
      considerations("`length(null)` returns `null`.")
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice'
          |RETURN length(p)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("length(p)" -> 2), Map("length(p)" -> 2), Map("length(p)" -> 2)))
        })) {
        p("The length of the path `p` is returned.")
        resultTable()
      }
    }
    section("properties()", "functions-properties") {
      p(
        """`properties()` returns a map containing all the properties of a node or relationship.
          |If the argument is already a map, it is returned unchanged.""".stripMargin)
      function("properties(expression)", "A Map.", ("expression", "An expression that returns a node, a relationship, or a map."))
      considerations("`properties(null)` returns `null`.")
      query(
        """CREATE (p:Person {name: 'Stefan', city: 'Berlin'})
          |RETURN properties(p)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("properties(p)" -> Map("name" -> "Stefan", "city" -> "Berlin"))))
        })) {
        resultTable()
      }
    }
    section("randomUUID()", "functions-randomuuid") {
      p(
        """`randomUUID()` returns a randomly-generated Universally Unique Identifier (UUID), also known as a Globally Unique Identifier (GUID).
          |This is a 128-bit value with strong guarantees of uniqueness.
        """.stripMargin)
      function("randomUUID()", "A String.")
      query(
        """RETURN randomUUID() AS uuid""".stripMargin, ResultAssertions((r) => {
          val uuid = r.columnAs[String]("uuid").next()
          UUID.fromString(uuid) should be(a[UUID])
        })) {
        resultTable()
        p("A randomly-generated UUID is returned.")
      }
    }
    section("size()", "functions-size") {
      p(
        """`size()` returns the number of elements in a list.""".stripMargin)
      function("size(list)", "An Integer.", ("list", "An expression that returns a list."))
      considerations("`size(null)` returns `null`.")
      query(
        """RETURN size(['Alice', 'Bob'])""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("size(['Alice', 'Bob'])" -> 2)))
        })) {
        resultTable()
        p("The number of elements in the list is returned.")
      }
    }
    section("size() applied to pattern expression", "functions-size-of-pattern-expression") {
      p(
        """This is the same `size()` method as described above, but instead of passing in a list directly, a pattern expression can be provided that can be used in a match query to provide a new set of results.
          |These results are a _list_ of paths.
          |The size of the result is calculated, not the length of the expression itself.""".stripMargin)
      function("size(pattern expression)", ("pattern expression", "A pattern expression that returns a list."))
      query(
        """MATCH (a)
          |WHERE a.name = 'Alice'
          |RETURN size((a)-->()-->()) AS fof""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("fof" -> 3)))
        })) {
        resultTable()
        p("The number of sub-graphs matching the pattern expression is returned.")
      }
    }
    section("size() applied to string", "functions-size-of-string") {
      p(
        """`size()` returns the number of Unicode characters in a string.""".stripMargin)
      function("size(string)", "An Integer.", ("string", "An expression that returns a string value."))
      considerations("`size(null)` returns `null`.")
      query(
        """MATCH (a)
          |WHERE size(a.name) > 6
          |RETURN size(a.name)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("size(a.name)" -> 7)))
        })) {
        resultTable()
        p("The number of characters in the string *'Charlie'* is returned.")
      }
    }
    section("startNode()", "functions-startnode") {
      p(
        """`startNode()` returns the start node of a relationship.""".stripMargin)
      function("startNode(relationship)", "A Node.", ("relationship", "An expression that returns a relationship."))
      considerations("`startNode(null)` returns `null`.")
      query(
        """MATCH (x:Developer)-[r]-()
          |RETURN startNode(r)""".stripMargin, ResultAssertions((r) => {
          r.columnAs[Node]("startNode(r)").toList.map(_.getId) should equal(Array(0, 0))
        })) {
        resultTable()
      }
    }
    section("timestamp()", "functions-timestamp") {
      p(
        """`timestamp()` returns the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.""".stripMargin)
      function("timestamp()", "An Integer.")
      considerations("`timestamp()` will return the same value during one entire query, even for long-running queries.")
      query(
        """RETURN timestamp()""".stripMargin, ResultAssertions((r) => {
          r.toList.head("timestamp()") match {
            // this should pass unless your machine is really slow
            case x: Long => System.currentTimeMillis - x < 100000
            case _ => false
          }
        })) {
        p("The time in milliseconds is returned.")
        resultTable()
      }
    }
    section("toBoolean()", "functions-toboolean") {
      p(
        "`toBoolean()` converts a string value to a boolean value.")
      function("toBoolean(expression)", "A Boolean.", ("expression", "An expression that returns a boolean or string value."))
      considerations("`toBoolean(null)` returns `null`.", "If `expression` is a boolean value, it will be returned unchanged.", "If the parsing fails, `null` will be returned.")
      query(
        """RETURN toBoolean('TRUE'), toBoolean('not a boolean')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toBoolean('TRUE')" -> true, "toBoolean('not a boolean')" -> null)))
        })) {
        resultTable()
      }
    }
    section("toFloat()", "functions-tofloat") {
      p(
        "`toFloat()` converts an integer or string value to a floating point number.".stripMargin)
      function("toFloat(expression)", "A Float.", ("expression", "An expression that returns a numeric or string value."))
      considerations("`toFloat(null)` returns `null`.", "If `expression` is a floating point number, it will be returned unchanged.", "If the parsing fails, `null` will be returned.")
      query(
        """RETURN toFloat('11.5'), toFloat('not a number')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toFloat('11.5')" -> 11.5, "toFloat('not a number')" -> null)))
        })) {
        resultTable()
      }
    }
    section("toInteger()", "functions-tointeger") {
      p(
        """`toInteger()` converts a floating point or string value to an integer value.""".stripMargin)
      function("toInteger(expression)", "An Integer.", ("expression", "An expression that returns a numeric or string value."))
      considerations("`toInteger(null)` returns `null`.", "If `expression` is an integer value, it will be returned unchanged.", "If the parsing fails, `null` will be returned.")
      query(
        """RETURN toInteger('42'), toInteger('not a number')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toInteger('42')" -> 42, "toInteger('not a number')" -> null)))
        })) {
        resultTable()
      }
    }
    section("type()", "functions-type") {
      p(
        """`type()` returns the string representation of the relationship type.""".stripMargin)
      function("type(relationship)", "A String.", ("relationship", "An expression that returns a relationship."))
      considerations("`type(null)` returns `null`.")
      query(
        """MATCH (n)-[r]->()
          |WHERE n.name = 'Alice'
          |RETURN type(r)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("type(r)" -> "KNOWS"), Map("type(r)" -> "KNOWS")))
        })) {
        p("The relationship type of `r` is returned.")
        resultTable()
      }
    }
  }.build()
}
