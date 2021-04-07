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

import java.util.UUID

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Node

class ScalarFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Scalar functions", "query-functions-scalar")
    initQueries(
      """CREATE
        #  (alice:Developer {name:'Alice', age: 38, eyes: 'brown'}),
        #  (bob {name: 'Bob', age: 25, eyes: 'blue'}),
        #  (charlie {name: 'Charlie', age: 53, eyes: 'green'}),
        #  (daniel {name: 'Daniel', age: 54, eyes: 'brown'}),
        #  (eskil {name: 'Eskil', age: 41, eyes: 'blue', liked_colors: ['pink', 'yellow', 'black']}),
        #  (alice)-[:KNOWS]->(bob),
        #  (alice)-[:KNOWS]->(charlie),
        #  (bob)-[:KNOWS]->(daniel),
        #  (charlie)-[:KNOWS]->(daniel),
        #  (bob)-[:MARRIED]->(eskil)""".stripMargin('#'))
    synopsis("Scalar functions return a single value.")
    p("""Functions:
        #
        #* <<functions-coalesce, coalesce()>>
        #* <<functions-endnode, endNode()>>
        #* <<functions-head, head()>>
        #* <<functions-id, id()>>
        #* <<functions-last, last()>>
        #* <<functions-length, length()>>
        #* <<functions-properties, properties()>>
        #* <<functions-randomuuid, randomUUID()>>
        #* <<functions-size, size()>>
        #* <<functions-size-of-pattern-expression, Size of pattern expression>>
        #* <<functions-size-of-string, Size of string>>
        #* <<functions-startnode, startNode()>>
        #* <<functions-timestamp, timestamp()>>
        #* <<functions-toboolean, toBoolean()>>
        #* <<functions-tobooleanornull, toBooleanOrNull()>>
        #* <<functions-tofloat, toFloat()>>
        #* <<functions-tofloatornull, toFloatOrNull()>>
        #* <<functions-tointeger, toInteger()>>
        #* <<functions-tointegerornull, toIntegerOrNull()>>
        #* <<functions-type, type()>>""".stripMargin('#'))
    important {
      p("""The `length()` and `size()` functions are quite similar, and so it is important to take note of the difference.
          #
          #Function `length()`:: Only works for <<functions-length, paths>>.
          #Function `size()`:: Only works for the three types: <<functions-size-of-string, strings>>, <<functions-size, lists>>, and <<functions-size-of-pattern-expression, pattern expressions>>.""".stripMargin('#'))
    }
    graphViz()
    section("coalesce()", "functions-coalesce") {
      p("The function `coalesce()` returns the first non-`null` value in the given list of expressions.")
      function("coalesce(expression [, expression]*)",
        "The type of the value returned will be that of the first non-`null` expression.",
        ("expression", "An expression that may return `null`."))
      considerations("`null` will be returned if all the arguments are `null`.")
      query("""MATCH (a)
              #WHERE a.name = 'Alice'
              #RETURN coalesce(a.hairColor, a.eyes)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("coalesce(a.hairColor, a.eyes)" -> "brown")))
        })) {
        resultTable()
      }
    }
    section("endNode()", "functions-endnode") {
      p("The function `endNode()` returns the end node of a relationship.")
      function("endNode(relationship)",
        "A Node.",
        ("relationship", "An expression that returns a relationship."))
      considerations("`endNode(null)` returns `null`.")
      query("""MATCH (x:Developer)-[r]-()
              #RETURN endNode(r)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[Node]("endNode(r)").toList.map(_.getId) should equal(Array(2, 1))
        })) {
        resultTable()
      }
    }
    section("head()", "functions-head") {
      p("The function `head()` returns the first element in a list.")
      function("head(expression)",
        "The type of the value returned will be that of the first element of the list.",
        ("expression", "An expression that returns a list."))
      considerations(
        "`head(null)` returns `null`.",
        "`head([])` returns `null`.",
        "If the first element in `list` is `null`, `head(list)` will return `null`.")
      query("""MATCH (a)
              #WHERE a.name = 'Eskil'
              #RETURN a.liked_colors, head(a.liked_colors)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[String]("head(a.liked_colors)").toList.head should equal("pink")
        })) {
        p("The first element in the list is returned.")
        resultTable()
      }
    }
    section("id()", "functions-id") {
      p("The function `id()` returns an identifier; the function can be utilized for a relationship or a node.")
      note {
        //The note has been approved by kernel team.
        p("""Neo4j implements the id so that:
            #
            #Node::
            #Every node in a database has an identifier.
            #The identifier for a node is guaranteed to be unique among other nodes' identifiers in the same database, within the scope of a single transaction.
            #
            #Relationship::
            #Every relationship in a database has an identifier.
            #The identifier for a relationship is guaranteed to be unique among other relationships' identifiers in the same database, within the scope of a single transaction.""".stripMargin('#'))
      }
      function("id(expression)",
        "An Integer.",
        ("expression", "An expression that returns a node or a relationship."))
      considerations("`id(null)` returns `null`.")
      //Fix this example to show that ids between nodes and relationships can have the same id.
      query("""MATCH (a)
              #RETURN id(a)""".stripMargin('#'), ResultAssertions((r) => {
          r.toList should equal(List(
            Map("id(a)" -> 0),
            Map("id(a)" -> 1),
            Map("id(a)" -> 2),
            Map("id(a)" -> 3),
            Map("id(a)" -> 4)))
        })) {
        p("The node identifier for each of the nodes is returned.")
        resultTable()
      }
    }
    section("last()", "functions-last") {
      p("The function `last()` returns the last element in a list.")
      function("last(expression)",
        "The type of the value returned will be that of the last element of the list.",
        ("expression", "An expression that returns a list."))
      considerations(
        "`last(null)` returns `null`.",
        "`last([])` returns `null`.",
        "If the last element in `list` is `null`, `last(list)` will return `null`.")
      query("""MATCH (a)
              #WHERE a.name = 'Eskil'
              #RETURN a.liked_colors, last(a.liked_colors)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[String]("last(a.liked_colors)").toList.head should equal("black")
        })) {
        p("The last element in the list is returned.")
        resultTable()
      }
    }
    section("length()", "functions-length") {
      p("The function `length()` returns the length of a path.")
      function("length(path)",
        "An Integer.",
        ("path", "An expression that returns a path."))
      considerations("`length(null)` returns `null`.")
      query("""MATCH p = (a)-->(b)-->(c)
              #WHERE a.name = 'Alice'
              #RETURN length(p)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(
            Map("length(p)" -> 2),
            Map("length(p)" -> 2),
            Map("length(p)" -> 2)))
        })) {
        p("The length of the path `p` is returned.")
        resultTable()
      }
    }
    section("properties()", "functions-properties") {
      p("""The function `properties()` returns a map containing all the properties; the function can be utilized for a relationship or a node.
          #If the argument is already a map, it is returned unchanged.""".stripMargin('#'))
      function("properties(expression)",
        "A Map.",
        ("expression", "An expression that returns a relationship, a node, or a map."))
      considerations("`properties(null)` returns `null`.")
      query("""CREATE (p:Person {name: 'Stefan', city: 'Berlin'})
              #RETURN properties(p)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("properties(p)" -> Map("name" -> "Stefan", "city" -> "Berlin"))))
        })) {
        resultTable()
      }
    }
    section("randomUUID()", "functions-randomuuid") {
      p("""The function `randomUUID()` returns a randomly-generated Universally Unique Identifier (UUID), also known as a Globally Unique Identifier (GUID).
          #This is a 128-bit value with strong guarantees of uniqueness.""".stripMargin('#'))
      function("randomUUID()",
        "A String.")
      query("RETURN randomUUID() AS uuid",
      ResultAssertions((r) => {
          val uuid = r.columnAs[String]("uuid").next()
          UUID.fromString(uuid) should be(a[UUID])
        })) {
        resultTable()
        p("A randomly-generated UUID is returned.")
      }
    }
    section("size()", "functions-size") {
      p("The function `size()` returns the number of elements in a list.")
      function("size(list)",
        "An Integer.",
        ("list", "An expression that returns a list."))
      considerations("`size(null)` returns `null`.")
      query("RETURN size(['Alice', 'Bob'])",
      ResultAssertions((r) => {
          r.toList should equal(List(Map("size(['Alice', 'Bob'])" -> 2)))
        })) {
        resultTable()
        p("The number of elements in the list is returned.")
      }
    }
    section("size() applied to pattern expression", "functions-size-of-pattern-expression") {
      p("""This is the same function `size()` as described above, but you pass in a pattern expression, instead of a list.
          #The function size will then calculate on a _list_ of paths.""".stripMargin('#'))
      function("size(pattern expression)",
        ("pattern expression", "A pattern expression that returns a list."))
      query("""MATCH (a)
              #WHERE a.name = 'Alice'
              #RETURN size((a)-->()-->()) AS fof""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("fof" -> 3)))
        })) {
        resultTable()
        p("The number of paths matching the pattern expression is returned. (The size of the list of paths).")
      }
    }
    section("size() applied to string", "functions-size-of-string") {
      p("The function `size()` returns the number of Unicode characters in a string.")
      function("size(string)",
        "An Integer.",
        ("string", "An expression that returns a string value."))
      considerations("`size(null)` returns `null`.")
      query("""MATCH (a)
              #WHERE size(a.name) > 6
              #RETURN size(a.name)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("size(a.name)" -> 7)))
        })) {
        resultTable()
        p("The number of characters in the string `'Charlie'` is returned.")
      }
    }
    section("startNode()", "functions-startnode") {
      p("The function `startNode()` returns the start node of a relationship.")
      function("startNode(relationship)",
        "A Node.",
        ("relationship", "An expression that returns a relationship."))
      considerations("`startNode(null)` returns `null`.")
      query("""MATCH (x:Developer)-[r]-()
              #RETURN startNode(r)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[Node]("startNode(r)").toList.map(_.getId) should equal(Array(0, 0))
        })) {
        resultTable()
      }
    }
    section("timestamp()", "functions-timestamp") {
      p("The function `timestamp()` returns the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.")
      note {
        p("It is the equivalent of `datetime().epochMillis`.")
      }
      function("timestamp()",
        "An Integer.")
      considerations("`timestamp()` will return the same value during one entire query, even for long-running queries.")
      query("RETURN timestamp()",
      ResultAssertions((r) => {
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
      p("The function `toBoolean()` converts a string, integer or boolean value to a boolean value.")
      function("toBoolean(expression)",
        "A Boolean.",
        ("expression", "An expression that returns a boolean, string or integer value."))
      considerations(
        "`toBoolean(null)` returns `null`.",
        "If `expression` is a boolean value, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "If `expression` is the integer value `0`, `false` will be returned. For any other integer value `true` will be returned.",
        "This function will return an error if provided with an expression that is not a string, integer or boolean value.")
      query("RETURN toBoolean('true'), toBoolean('not a boolean'), toBoolean(0)",
      ResultAssertions((r) => {
          r.toList should equal(List(Map("toBoolean('true')" -> true, "toBoolean('not a boolean')" -> null, "toBoolean(0)" -> false)))
        })) {
        resultTable()
      }
    }
    section("toBooleanOrNull()", "functions-tobooleanornull") {
      p("The function `toBooleanOrNull()` converts a string, integer or boolean value to a boolean value. For any other input value, `null` will be returned.")
      function("toBooleanOrNull(expression)",
        "A Boolean or `null`.",
        ("expression", "Any expression that returns a value."))
      considerations(
        "`toBooleanOrNull(null)` returns `null`.",
        "If `expression` is a boolean value, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "If `expression` is the integer value `0`, `false` will be returned. For any other integer value `true` will be returned.",
        "If the `expression` is not a string, integer or boolean value, `null` will be returned.")
      query("RETURN toBooleanOrNull('true'), toBooleanOrNull('not a boolean'), toBooleanOrNull(0), toBooleanOrNull(1.5)",
        ResultAssertions((r) => {
          r.toList should equal(List(Map("toBooleanOrNull('true')" -> true, "toBooleanOrNull('not a boolean')" -> null, "toBooleanOrNull(0)" -> false, "toBooleanOrNull(1.5)" -> null)))
        })) {
        resultTable()
      }
    }
    section("toFloat()", "functions-tofloat") {
      p("The function `toFloat()` converts an integer or a string value to a floating point number.")
      function("toFloat(expression)",
        "A Float.",
        ("expression", "An expression that returns a numeric or a string value."))
      considerations(
        "`toFloat(null)` returns `null`.",
        "If `expression` is a floating point number, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "This function will return an error if provided with an expression that is not an integer, floating point or a string value.")
      query("RETURN toFloat('11.5'), toFloat('not a number')",
      ResultAssertions((r) => {
          r.toList should equal(List(Map("toFloat('11.5')" -> 11.5, "toFloat('not a number')" -> null)))
        })) {
        resultTable()
      }
    }
    section("toFloatOrNull()", "functions-tofloatornull") {
      p("The function `toFloatOrNull()` converts an integer or a string value to a floating point number. For any other input value, `null` will be returned.")
      function("toFloatOrNull(expression)",
        "A Float or `null`.",
        ("expression", "Any expression that returns a value."))
      considerations(
        "`toFloatOrNull(null)` returns `null`.",
        "If `expression` is a floating point number, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "If the `expression` is not an integer, floating point or a string value, `null` will be returned.")
      query("RETURN toFloatOrNull('11.5'), toFloatOrNull('not a number'), toFloatOrNull(true)",
        ResultAssertions((r) => {
          r.toList should equal(List(Map("toFloatOrNull('11.5')" -> 11.5, "toFloatOrNull('not a number')" -> null, "toFloatOrNull(true)" -> null)))
        })) {
        resultTable()
      }
    }
    section("toInteger()", "functions-tointeger") {
      p("The function `toInteger()` converts a boolean, floating point or a string value to an integer value.")
      function("toInteger(expression)",
        "An Integer.",
        ("expression", "An expression that returns a boolean, numeric or a string value."))
      considerations(
        "`toInteger(null)` returns `null`.",
        "If `expression` is an integer value, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "If `expression` is the boolean value `false`, `0` will be returned. If `expression` is the boolean value `true`, `1` will be returned.",
        "This function will return an error if provided with an expression that is not a boolean, floating point, integer or a string value.")
      query("RETURN toInteger('42'), toInteger('not a number'), toInteger(true)",
      ResultAssertions((r) => {
          r.toList should equal(List(Map("toInteger('42')" -> 42, "toInteger('not a number')" -> null, "toInteger(true)" -> 1)))
        })) {
        resultTable()
      }
    }
    section("toIntegerOrNull()", "functions-tointegerornull") {
      p("The function `toIntegerOrNull()` converts a boolean, floating point or a string value to an integer value. For any other input value, `null` will be returned.")
      function("toIntegerOrNull(expression)",
        "An Integer or `null`.",
        ("expression", "Any expression that returns a value."))
      considerations(
        "`toIntegerOrNull(null)` returns `null`.",
        "If `expression` is an integer value, it will be returned unchanged.",
        "If the parsing fails, `null` will be returned.",
        "If `expression` is the boolean value `false`, `0` will be returned. If `expression` is the boolean value `true`, `1` will be returned.",
        "If the `expression` is not a boolean, floating point, integer or a string value, `null` will be returned.")
      query("RETURN toIntegerOrNull('42'), toIntegerOrNull('not a number'), toIntegerOrNull(true), toIntegerOrNull(['A', 'B', 'C'])",
        ResultAssertions((r) => {
          r.toList should equal(List(Map("toIntegerOrNull('42')" -> 42, "toIntegerOrNull('not a number')" -> null, "toIntegerOrNull(true)" -> 1, "toIntegerOrNull(['A', 'B', 'C'])" -> null)))
        })) {
        resultTable()
      }
    }
    section("type()", "functions-type") {
      p("The function `type()` returns the string representation of the relationship type.")
      function("type(relationship)",
        "A String.",
        ("relationship", "An expression that returns a relationship."))
      considerations("`type(null)` returns `null`.")
      query("""MATCH (n)-[r]->()
              #WHERE n.name = 'Alice'
              #RETURN type(r)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("type(r)" -> "KNOWS"), Map("type(r)" -> "KNOWS")))
        })) {
        p("The relationship type of `r` is returned.")
        resultTable()
      }
    }
  }.build()
}
