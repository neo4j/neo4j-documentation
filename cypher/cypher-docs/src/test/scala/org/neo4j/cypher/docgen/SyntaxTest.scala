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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.neo4j.cypher.internal.compiler.v3_0.executionplan.InternalExecutionResult

class SyntaxTest extends DocumentingTestBase {
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

  def section = "syntax"

  val common_arguments = List(
    "list" -> "An expression that returns a list",
    "variable" -> "This is the variable that can be used from the predicate.",
    "predicate" -> "A predicate that is tested against all items in the list."
  )



  @Test def simple_case() {
    testThis(
      title = "Simple CASE",
      syntax = """CASE test
WHEN value THEN result
[WHEN ...]
[ELSE default]
END""",

      arguments = List(
        "test" -> "A valid expression.",
        "value" -> "An expression whose result will be compared to the `test` expression.",
        "result" -> "This is the result expression used if the value expression matches the `test` expression.",
        "default" -> "The expression to use if no match is found."
      ),
      text = "The expression is calculated, and compared in order with the `WHEN` clauses until a match is found. " +
        "If no match is found the expression in the `ELSE` clause is used, or `null`, if no `ELSE` case exists.",
      queryText =
        """MATCH (n) RETURN CASE n.eyes
    WHEN 'blue'  THEN 1
    WHEN 'brown' THEN 2
                 ELSE 3
END AS result""",
      returns = "",
      assertions = (p) => assert(Set(Map("result" -> 2), Map("result" -> 1), Map("result" -> 2), Map("result" -> 1), Map("result" -> 3)) === p.toSet)
    )
  }

  @Test def generic_case() {
    testThis(
      title = "Generic CASE",
      syntax = """CASE
WHEN predicate THEN result
[WHEN ...]
[ELSE default]
END""",

      arguments = List(
        "predicate" -> "A predicate that is tested to find a valid alternative.",
        "result" -> "This is the result expression used if the predicate matches.",
        "default" -> "The expression to use if no match is found."
      ),
      text = "The predicates are evaluated in order until a true value is found, and the result value is used. " +
        "If no match is found the expression in the `ELSE` clause is used, or `null`, if no `ELSE` case exists.",
      queryText =
        """MATCH (n) RETURN CASE
    WHEN n.eyes = 'blue'  THEN 1
    WHEN n.age < 40       THEN 2
                          ELSE 3
END AS result""",
      returns = "",
      assertions = (p) => assert(Set(Map("result" -> 3), Map("result" -> 1), Map("result" -> 2), Map("result" -> 1), Map("result" -> 3)) === p.toSet)
    )
  }

  @Test def distinct_operator() {
    testThis(
      title = "Using the DISTINCT operator",
      syntax = "",
      arguments = List.empty,
      text = "Retrieve the unique eye colors from `Person` nodes.",
      queryText =
        """CREATE (a:Person {name: 'Anne', eyeColor: 'blue'}),
          |             (b:Person {name: 'Bill', eyeColor: 'brown'}),
          |             (c:Person {name: 'Carol', eyeColor: 'blue'})
          |WITH a, b, c
          |MATCH (p:Person)
          |RETURN DISTINCT p.eyeColor""".stripMargin,
      returns = "Even though both *'Anne'* and *'Carol'* have blue eyes, *'blue'* is only returned once.",
      assertions = (p) => assert(Set(Map("p.eyeColor" -> "blue"), Map("p.eyeColor" -> "brown")) === p.toSet)
    )
  }

  @Test def property_access_operator() {
    testThis(
      title = "Accessing the property of a nested literal map",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH {person: {name: 'Anne', age: 25}} AS p
          |RETURN  p.person.name""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("p.person.name" -> "Anne")) === p.toSet)
    )
  }

  @Test def mathematical_operator_exponentiation() {
    testThis(
      title = "Using the exponentiation operator",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH 2 AS number, 3 AS exponent
           RETURN number ^ exponent AS result""",
      returns = "",
      assertions = (p) => assert(Set(Map("result" -> 8L)) === p.toSet)
    )
  }

  @Test def mathematical_operator_unary_minus() {
    testThis(
      title = "Using the unary minus operator",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH -3 AS a, 4 AS b
          |return b - a AS result""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("result" -> 7L)) === p.toSet)
    )
  }

  @Test def comparison_operator() {
    testThis(
      title = "Comparing two numbers",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH 4 AS one, 3 AS two
           RETURN one > two AS result""",
      returns = "",
      assertions = (p) => assert(Set(Map("result" -> true)) === p.toSet)
    )
  }

  @Test def starts_with_comparison_operator() {
    testThis(
      title = "Using STARTS WITH to filter names",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH ['John', 'Mark', 'Jonathan', 'Bill'] AS somenames
          |UNWIND somenames AS names
          |WITH names AS candidate
          |WHERE candidate STARTS WITH 'Jo'
          |RETURN candidate""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("candidate" -> "John"), Map("candidate" -> "Jonathan")) === p.toSet)
    )
  }

  @Test def boolean_operator() {
    testThis(
      title = "Using boolean operators to filter numbers",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH [2, 4, 7, 9, 12] AS numberlist
          |UNWIND numberlist AS number
          |WITH number
          |WHERE number = 4 OR (number > 6 AND number < 10)
          |RETURN number""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("number" -> 4L), Map("number" -> 7L), Map("number" -> 9L)) === p.toSet)
    )
  }

  @Test def regex_string_operator() {
    testThis(
      title = "Using a regular expression to filter words",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH ['mouse', 'chair', 'door', 'house'] AS wordlist
          |UNWIND wordlist AS word
          |WITH word
          |WHERE word =~ '.*ous.*'
          |RETURN word""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("word" -> "mouse"), Map("word" -> "house")) === p.toSet)
    )
  }

  @Test def in_list_operator() {
    testThis(
      title = "Using `IN` to check if a number is in a list",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH [2, 3, 4, 5] AS numberlist
          |UNWIND numberlist AS number
          |WITH number
          |WHERE number IN [2, 3, 8]
          |RETURN number""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("number" -> 2L), Map("number" -> 3L)) === p.toSet)
    )
  }

  @Test def concatenation_list_operator() {
    testThis(
      title = "Concatenating two lists",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """RETURN [1,2,3,4,5] + [6,7] AS myList""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("myList" -> List(1L, 2L, 3L, 4L, 5L, 6L, 7L))) === p.toSet)
    )
  }

  @Test def accessing_element_list_operator() {
    testThis(
      title = "Accessing elements in a list",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """WITH ['Anne', 'John', 'Bill', 'Diane', 'Eve'] AS names
          |RETURN names[1..3] AS result""".stripMargin,
      returns = "The square brackets will extract the elements from the start index `1`, and up to (but excluding) the end index `3`.",
      assertions = (p) => assert(Set(Map("result" -> List("John", "Bill"))) === p.toSet)
    )
  }

  @Test def dynamic_property_access() {
    testThis(
      title = "Filtering on a dynamically-computed property key",
      syntax = "",
      arguments = List.empty,
      text = "",
      queryText =
        """CREATE (a:Restaurant {name: 'Hungry Jo', rating_hygiene: 10, rating_food: 7}),
          | (b:Restaurant {name: 'Buttercup Tea Rooms', rating_hygiene: 5, rating_food: 6}),
          | (c1:Category {name: 'hygiene'}),
          | (c2:Category {name: 'food'})
          |WITH a, b, c1, c2
          |MATCH (restaurant:Restaurant), (category:Category)
          |WHERE restaurant["rating_" + category.name] > 6
          |RETURN DISTINCT restaurant.name""".stripMargin,
      returns = "",
      assertions = (p) => assert(Set(Map("restaurant.name" -> "Hungry Jo")) === p.toSet)
    )
  }

  private def testThis(title: String, syntax: String, arguments: List[(String, String)], text: String, queryText: String, returns: String, assertions: InternalExecutionResult => Unit) {
    val formattedSyntax = if (!syntax.isEmpty) Array("*Syntax:*", "[source, cypher]", syntax).mkString("\n", "\n", "") else ""

    val args = arguments.map(x => "| `" + x._1 + "` | " + x._2).mkString("", "\n", "")
    val formattedArguments = if (!arguments.isEmpty) Array("*Arguments:*", "[options=\"header\"]", "|===", "| Name | Description", args, "|===").mkString("\n", "\n", "") else ""
    val fullText = String.format(
      """%s
         |%s
         |%s
      """.stripMargin, text, formattedSyntax, formattedArguments)

    testQuery(title, fullText, queryText, returns, assertions = assertions)
  }
}
