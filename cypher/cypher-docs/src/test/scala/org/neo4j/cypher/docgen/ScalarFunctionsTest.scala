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
import org.neo4j.graphdb.{Node}

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
    p(
      """* <<functions-coalesce,coalesce()>>
        |* <<functions-endnode,endNode()>>
        |* <<functions-head,head()>>
        |* <<functions-id,id()>>
        |* <<functions-last,last()>>
        |* <<functions-length,length()>>
        |* <<functions-properties,properties()>>
        |* <<functions-size,size()>>
        |* <<functions-size-of-pattern-expression,Size of pattern expression>>
        |* <<functions-size-of-string,Size of string>>
        |* <<functions-startnode,startNode()>>
        |* <<functions-timestamp,timestamp()>>
        |* <<functions-tofloat,toFloat()>>
        |* <<functions-toint,toInt()>>
        |* <<functions-type,type()>>""")
    graphViz()
    section("coalesce()", "functions-coalesce") {
      p(
        """Returns the first non-`null` value in the list of expressions passed to it.
          |In case all arguments are `null`, `null` will be returned.""".stripMargin)
      function("coalesce(expression [, expression]*)", ("expression", "The expression that may return `null`."))
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
      function("endNode(relationship)", ("relationship", "An expression that returns a relationship."))
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
      function("head(expression)", ("expression", "This expression should return a list of some kind."))
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
        """Returns the id of the relationship or node.""".stripMargin)
      function("id(expression)", ("expression", "An expression that returns a node or a relationship."))
      query(
        """MATCH (a)
          |RETURN id(a)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("id(a)" -> 0), Map("id(a)" -> 1), Map("id(a)" -> 2), Map("id(a)" -> 3), Map("id(a)" -> 4)))
        })) {
        p("This returns the node id for all the nodes.")
        resultTable()
      }
    }
    section("last()", "functions-last") {
      p(
        """`last()` returns the last element in a list.""".stripMargin)
      function("last(expression)", ("expression", "This expression should return a list of some kind."))
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
        """To return or filter on the length of a path, use the `length()` function.""".stripMargin)
      function("length(path)", ("path", "An expression that returns a path."))
      query(
        """MATCH p = (a)-->(b)-->(c)
          |WHERE a.name = 'Alice'
          |RETURN length(p)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("length(p)" -> 2), Map("length(p)" -> 2), Map("length(p)" -> 2)))
        })) {
        p("The length of the path `p` is returned by the query.")
        resultTable()
      }
    }
    section("properties()", "functions-properties") {
      p(
        """`properties()` converts the arguments to a map of its properties.
          |If the argument is a node or a relationship, the returned map is a map of its properties.
          |If the argument is already a map, it is returned unchanged.""".stripMargin)
      function("properties(expression)", ("expression", "An expression that returns a node, a relationship, or a map."))
      query(
        """CREATE (p:Person {name: 'Stefan', city: 'Berlin'})
          |RETURN properties(p)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("properties(p)" -> Map("name" -> "Stefan", "city" -> "Berlin"))))
        })) {
        resultTable()
      }
    }
    section("size()", "functions-size") {
      p(
        """To return or filter on the size of a list, use the `size()` function.""".stripMargin)
      function("size(list)", ("list", "An expression that returns a list."))
      query(
        """RETURN size(['Alice', 'Bob'])""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("size(['Alice', 'Bob'])" -> 2)))
        })) {
        resultTable()
        p("The number of items in the list is returned by the query.")
      }
    }
    section("Size of pattern expression", "functions-size-of-pattern-expression") {
      p(
        """This is the same `size()` method described before, but instead of passing in a list directly, you provide a pattern expression that can be used in a match query to provide a new set of results.
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
        p("The number of sub-graphs matching the pattern expression is returned by the query.")
      }
    }
    section("Size of string", "functions-size-of-string") {
      p(
        """To return or filter on the size of a string, use the `size()` function.""".stripMargin)
      function("size(string)", ("string", "An expression that returns a string."))
      query(
        """MATCH (a)
          |WHERE size(a.name) > 6
          |RETURN size(a.name)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("size(a.name)" -> 7)))
        })) {
        resultTable()
        p("The size of the name *'Charlie'* is returned by the query.")
      }
    }
    section("startNode()", "functions-startnode") {
      p(
        """`startNode()` returns the starting node of a relationship.""".stripMargin)
      function("startNode(relationship)", ("relationship", "An expression that returns a relationship."))
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
        """`timestamp()` returns the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
          |It will return the same value during the whole one query, even if the query is a long running one.""".stripMargin)
      function("timestamp()")
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
    section("toFloat()", "functions-tofloat") {
      p(
        """`toFloat()` converts the argument to a float.
          |A string is parsed as if it was an floating point number.
          |If the parsing fails, `null` will be returned.
          |An integer will be cast to a floating point number.""".stripMargin)
      function("toFloat(expression)", ("expression", "An expression that returns anything."))
      query(
        """RETURN toFloat('11.5'), toFloat('not a number')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toFloat('11.5')" -> 11.5, "toFloat('not a number')" -> null)))
        })) {
        resultTable()
      }
    }
    section("toInt()", "functions-toint") {
      p(
        """`toInt()` converts the argument to an integer.
          |A string is parsed as if it was an integer number.
          |If the parsing fails, `null` will be returned.
          |A floating point number will be cast into an integer.""".stripMargin)
      function("toInt(expression)", ("expression", "An expression that returns anything."))
      query(
        """RETURN toInt('42'), toInt('not a number')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toInt('42')" -> 42, "toInt('not a number')" -> null)))
        })) {
        resultTable()
      }
    }
    section("type()", "functions-type") {
      p(
        """Returns a string representation of the relationship type.""".stripMargin)
      function("type(relationship)", ("relationship", "A relationship."))
      query(
        """MATCH (n)-[r]->()
          |WHERE n.name = 'Alice'
          |RETURN type(r)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("type(r)" -> "KNOWS"), Map("type(r)" -> "KNOWS")))
        })) {
        p("The relationship type of `r` is returned by the query.")
        resultTable()
      }
    }
  }.build()
}
