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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class StringFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("String functions", "query-functions-string")
    synopsis(
      """These functions all operate on string expressions only, and will return an error if used on any other values.
        |The exception to this rule is `toString()`, which also accepts numbers and booleans.""".stripMargin)
    note {
      p("""The functions `lower()` and `upper()` have been superseded by `toLower()` and `toUpper()`, respectively, and will be deprecated in a future release.""")
    }
    p("""See also <<query-operators-string>>.""")
    p(
      """* <<functions-left,left()>>
        |* <<functions-ltrim,lTrim()>>
        |* <<functions-replace,replace()>>
        |* <<functions-reverse,reverse()>>
        |* <<functions-right,right()>>
        |* <<functions-rtrim,rTrim()>>
        |* <<functions-split,split()>>
        |* <<functions-substring,substring()>>
        |* <<functions-tolower,toLower()>>
        |* <<functions-tostring,toString()>>
        |* <<functions-toupper,toUpper()>>
        |* <<functions-trim,trim()>>""")
    section("left()", "functions-left") {
      p("`left()` returns a string containing the left n characters of the original string.")
      function("left(original, length)", ("original", "An expression that returns a string"), ("n", "An expression that returns a positive number."))
      query(
        """RETURN left('hello', 3)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("left('hello', 3)" -> "hel")))
        })) {
        resultTable()
      }
    }
    section("ltrim()", "functions-ltrim") {
      p("`lTrim()` returns the original string with whitespace removed from the left side.")
      function("lTrim(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN lTrim('   hello')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("lTrim('   hello')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("replace()", "functions-replace") {
      p("`replace()` returns a string with the search string replaced by the replace string. It replaces all occurrences.")
      function("replace(original, search, replace)", ("original", "An expression that returns a string"), ("search", "An expression that returns a string to search for"), ("replace", "An expression that returns the string to replace the search string with"))
      query(
        """RETURN replace("hello", "l", "w")""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("replace(\"hello\", \"l\", \"w\")" -> "hewwo")))
        })) {
        resultTable()
      }
    }
    section("reverse()", "functions-reverse") {
      p("`reverse()` returns the original string reversed.")
      function("reverse(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN reverse('anagram')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("reverse('anagram')" -> "margana")))
        })) {
        resultTable()
      }
    }
    section("right()", "functions-right") {
      p("`right()` returns a string containing the right n characters of the original string.")
      function("right(original, length)", ("original", "An expression that returns a string"), ("n", "An expression that returns a positive number."))
      query(
        """RETURN right('hello', 3)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("right('hello', 3)" -> "llo")))
        })) {
        resultTable()
      }
    }
    section("rtrim()", "functions-rtrim") {
      p("`rTrim()` returns the original string with whitespace removed from the right side.")
      function("rTrim(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN rTrim('hello   ')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("rTrim('hello   ')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("split()", "functions-split") {
      p("`split()` returns the sequence of strings which are delimited by split patterns.")
      function("split(original, splitPattern)", ("original", "An expression that returns a string"), ("splitPattern", "The string to split the original string with"))
      query(
        """RETURN split('one,two', ',')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("split('one,two', ',')" -> List("one", "two"))))
        })) {
        resultTable()
      }
    }
    section("substring()", "functions-substring") {
      p(
        """`substring()` returns a substring of the original, with a 0-based index start and length.
          |If length is omitted, it returns a substring from start until the end of the string.""".stripMargin)
      function("substring(original, start [, length])", ("original", "An expression that returns a string"), ("start", "An expression that returns a positive number"), ("length", "An expression that returns a positive number"))
      query(
        """RETURN substring('hello', 1, 3), substring('hello', 2)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("substring('hello', 1, 3)" -> "ell", "substring('hello', 2)" -> "llo")))
        })) {
        resultTable()
      }
    }
    section("toLower()", "functions-tolower") {
      p(
        """`toLower()` returns the original string in lowercase.""".stripMargin)
      function("toLower(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN toLower('HELLO')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toLower('HELLO')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("toString()", "functions-tostring") {
      p(
        """`toString()` converts the argument to a string.
          |It converts integral and floating point numbers and booleans to strings, and if called with a string will leave it unchanged.""".stripMargin)
      function("toString(expression)", ("expression", "An expression that returns a number, a boolean, or a string."))
      query(
        """RETURN toString(11.5), toString('already a string'), toString(true)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toString(11.5)" -> "11.5", "toString('already a string')" -> "already a string", "toString(TRUE )" -> "true")))
        })) {
        resultTable()
      }
    }
    section("toUpper()", "functions-toupper") {
      p(
        """`toUpper()` returns the original string in uppercase.""".stripMargin)
      function("toUpper(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN toUpper('hello')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toUpper('hello')" -> "HELLO")))
        })) {
        resultTable()
      }
    }
    section("trim()", "functions-trim") {
      p(
        """`trim()` returns the original string with whitespace removed from both sides.""".stripMargin)
      function("trim(original)", ("original", "An expression that returns a string"))
      query(
        """RETURN trim('   hello   ')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("trim('   hello   ')" -> "hello")))
        })) {
        resultTable()
      }
    }
  }.build()

}
