/*
 * Copyright (c) 2002-2017 "Neo Technology,"
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

import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.internal.compiler.v3_0.executionplan.InternalExecutionResult
import org.neo4j.graphdb.Node
import org.neo4j.visualization.graphviz.{AsciiDocSimpleStyle, GraphStyle}

class FunctionsTest extends DocumentingTestBase {
  override def graphDescription = List(
    "A:foo:bar KNOWS B",
    "A KNOWS C",
    "B KNOWS D",
    "C KNOWS D",
    "B MARRIED E:Spouse")

  override val properties = Map(
    "A" -> Map[String, Any]("name" -> "Alice", "age" -> 38, "eyes" -> "brown"),
    "B" -> Map[String, Any]("name" -> "Bob", "age" -> 25, "eyes" -> "blue"),
    "C" -> Map[String, Any]("name" -> "Charlie", "age" -> 53, "eyes" -> "green"),
    "D" -> Map[String, Any]("name" -> "Daniel", "age" -> 54, "eyes" -> "brown"),
    "E" -> Map[String, Any]("name" -> "Eskil", "age" -> 41, "eyes" -> "blue", "array" -> Array("one", "two", "three"))
  )

  override protected def getGraphvizStyle: GraphStyle =
    AsciiDocSimpleStyle.withAutomaticRelationshipTypeColors()

  def section = "functions"

  val common_arguments = List(
    "list" -> "An expression that returns a list",
    "variable" -> "This is the variable that can be used from the predicate.",
    "predicate" -> "A predicate that is tested against all items in the list."
  )

  @Test def relationship_type() {
    testThis(
      title = "`type()`",
      syntax = "type( relationship )",
      arguments = List("relationship" -> "A relationship."),
      text = """Returns a string representation of the relationship type.""",
      queryText = """MATCH (n)-[r]->() WHERE n.name = 'Alice' RETURN type(r)""",
      returns = """The relationship type of `r` is returned by the query.""",
      assertions = (p) => assertEquals("KNOWS", p.columnAs[String]("type(r)").toList.head))
  }

  @Test def size() {
    testThis(
      title = "`size()`",
      syntax = "size( list )",
      arguments = List("list" -> "An expression that returns a list"),
      text = """To return or filter on the size of a list, use the `size()` function.""",
      queryText = """RETURN size(['Alice', 'Bob']) AS col""",
      returns = """The number of items in the list is returned by the query.""",
      assertions = (col) => assertEquals(2, col.columnAs[Long]("col").toList.head))
  }

  @Test def size2() {
    testThis(
      title = "Size of pattern expression",
      syntax = "size( pattern expression )",
      arguments = List("pattern expression" -> "A pattern expression that returns a list"),
      text = """
               |This is the same `size()` method described before,
               |but instead of passing in a list directly, you provide a pattern expression
               |that can be used in a match query to provide a new set of results.
               |These results are a _list_ of paths.
               |The size of the result is calculated, not the length of the expression itself.
               |""".stripMargin,
      queryText = """MATCH (a) WHERE a.name = 'Alice' RETURN size((a)-->()-->()) AS fof""",
      returns = """The number of sub-graphs matching the pattern expression is returned by the query.""",
      assertions = (p) => assertEquals(3, p.columnAs[Long]("fof").toList.head))
  }

  @Test def length() {
    testThis(
      title = "`length()`",
      syntax = "length( path )",
      arguments = List("path" -> "An expression that returns a path"),
      text = """To return or filter on the length of a path, use the `length()` function.""",
      queryText = """MATCH p = (a)-->(b)-->(c) where a.name = 'Alice' RETURN length(p)""",
      returns = """The length of the path `p` is returned by the query.""",
      assertions = (p) => assertEquals(2, p.columnAs[Long]("length(p)").toList.head))
  }

  @Test def sizeString() {
    testThis(
      title = "Size of string",
      syntax = "size( string )",
      arguments = List("string" -> "An expression that returns a string"),
      text = """To return or filter on the size of a string, use the `size()` function.""",
      queryText = """MATCH (a) WHERE size(a.name) > 6 RETURN size(a.name)""",
      returns = """The size of the name *'Charlie'* is returned by the query.""",
      assertions = (p) => assertEquals(7, p.columnAs[Long]("size(a.name)").toList.head))
  }

  @Test def head() {
    testThis(
      title = "`head()`",
      syntax = "head( expression )",
      arguments = List(
        "expression" -> "This expression should return a list of some kind."
      ),
      text = "`head()` returns the first element in a list.",
      queryText = """MATCH (a) WHERE a.name = 'Eskil' RETURN a.array, head(a.array)""",
      returns = "The first node in the path is returned.",
      assertions = (p) => assertEquals(List("one"), p.columnAs[List[_]]("head(a.array)").toList))
  }

  @Test def last() {
    testThis(
      title = "`last()`",
      syntax = "last( expression )",
      arguments = List(
        "expression" -> "This expression should return a list of some kind."
      ),
      text = "`last()` returns the last element in a list.",
      queryText = """MATCH (a) WHERE a.name = 'Eskil' RETURN a.array, last(a.array)""",
      returns = "The last node in the path is returned.",
      assertions = (p) => assertEquals(List("three"), p.columnAs[List[_]]("last(a.array)").toList))
  }


  @Test def id() {
    testThis(
      title = "`id()`",
      syntax = "id(  expression )",
      arguments = List("expression" -> "An expression that returns a node or a relationship."),
      text = """Returns the id of the relationship or node.""",
      queryText = """MATCH (a) RETURN id(a)""",
      returns = """This returns the node id for three nodes.""",
      assertions = (p) => assert(Seq(0,1,2,3,4) === p.columnAs[Long]("id(a)").toSeq)
    )
  }

  @Test def coalesce() {
    testThis(
      title = "`coalesce()`",
      syntax = "coalesce( expression [, expression]* )",
      arguments = List("expression" -> "The expression that might return `null`."),
      text = """Returns the first non-`null` value in the list of expressions passed to it.
In case all arguments are `null`, `null` will be returned.""",
      queryText = """MATCH (a) WHERE a.name = 'Alice' RETURN coalesce(a.hairColor, a.eyes)""",
      returns = """""",
      assertions = (p) => assert(Seq("brown") === p.columnAs[String]("coalesce(a.hairColor, a.eyes)").toSeq)
    )
  }

  @Test def toInt() {
    testThis(
      title = "`toInt()`",
      syntax = "toInt( expression )",
      arguments = List("expression" -> "An expression that returns anything"),
      text = "`toInt()` converts the argument to an integer. A string is parsed as if it was an integer number. If the " +
        "parsing fails, `null` will be returned. A floating point number will be cast into an integer.",
      queryText = "RETURN toInt('42'), toInt('not a number')",
      returns = "",
      assertions = (p) => assert(List(Map("toInt('42')" -> 42, "toInt('not a number')" -> null)) === p.toList)
    )
  }

  @Test def toFloat() {
    testThis(
      title = "`toFloat()`",
      syntax = "toFloat( expression )",
      arguments = List("expression" -> "An expression that returns anything"),
      text = "`toFloat()` converts the argument to a float. A string is parsed as if it was an floating point number. " +
        "If the parsing fails, `null` will be returned. An integer will be cast to a floating point number.",
      queryText = "RETURN toFloat('11.5'), toFloat('not a number')",
      returns = "",
      assertions = (p) => assert(List(Map("toFloat('11.5')" -> 11.5, "toFloat('not a number')" -> null)) === p.toList)
    )
  }

  @Test def propertiesFunc() {
    testThis(
      title = "`properties()`",
      syntax = "properties( expression )",
      arguments = List("expression" -> "An expression that returns a node, a relationship, or a map"),
      text = "`properties()` converts the arguments to a map of its properties. " +
        "If the argument is a node or a relationship, the returned map is a map of its properties. " +
        "If the argument is already a map, it is returned unchanged.",
      queryText = "CREATE (p:Person {name: 'Stefan', city: 'Berlin'}) RETURN properties(p)",
      returns = "",
      assertions = (p) => assert(List(
        Map("properties(p)" -> Map("name" -> "Stefan", "city" -> "Berlin"))) === p.toList)
    )
  }

  @Test def now() {
    testThis(
      title = "`timestamp()`",
      syntax = "timestamp()",
      arguments = List.empty,
      text = "`timestamp()` returns the difference, measured in milliseconds, between the current time and midnight, " +
        "January 1, 1970 UTC. It will return the same value during the whole one query, even if the query is a long " +
        "running one.",
      queryText = "RETURN timestamp()",
      returns = "The time in milliseconds is returned.",
      assertions = (p) => assert(
        p.toList.head("timestamp()") match {
          // this should pass unless your machine is really slow
          case x: Long => System.currentTimeMillis - x < 100000
          case _       => false
        })
    )
  }

  @Test def startNode() {
    testThis(
      title = "`startNode()`",
      syntax = "startNode( relationship )",
      arguments = List("relationship" -> "An expression that returns a relationship"),
      text = "`startNode()` returns the starting node of a relationship",
      queryText = "MATCH (x:foo)-[r]-() RETURN startNode(r)",
      returns = "",
      assertions = (p) => assert(p.toList.head("startNode(r)") === node("A")))
  }

  @Test def endNode() {
    testThis(
      title = "`endNode()`",
      syntax = "endNode( relationship )",
      arguments = List("relationship" -> "An expression that returns a relationship"),
      text = "`endNode()` returns the end node of a relationship",
      queryText = "MATCH (x:foo)-[r]-() RETURN endNode(r)",
      returns = "",
      assertions = (p) => assert(p.toList.head("endNode(r)") === node("C")))
  }

  private def testThis(title: String, syntax: String, arguments: List[(String, String)], text: String, queryText: String, returns: String, assertions: (InternalExecutionResult => Unit)) {
    val args = arguments.map(x => "| `" + x._1 + "` | " + x._2).mkString("", "\n", "")
    val formattedArguments = if (!arguments.isEmpty) Array("*Arguments:*", "[options=\"header\"]", "|===", "| Name | Description", args, "|===").mkString("\n", "\n", "") else ""

    val fullText = String.format(
      """%s
         |
         |*Syntax:* `%s`
         |%s
      """.stripMargin, text, syntax, formattedArguments)

    testQuery(title, fullText, queryText, returns, assertions = assertions)
  }
}
