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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class StringFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("String functions", "query-functions-string")
    synopsis(
      """These functions all operate on string expressions only, and will return an error if used on any other values.
        #The exception to this rule is `toString()`, which also accepts numbers, booleans and temporal values (i.e. _Date_, _Time_. _LocalTime_, _DateTime_, _LocalDateTime_  or _Duration_ values).""".stripMargin('#'))
    p("Functions taking a string as input all operate on _Unicode characters_ rather than on a standard `char[]`. For example, the `size()` function applied to any _Unicode character_ will return *1*, even if the character does not fit in the 16 bits of one `char`.")
    note {
      p("""When `toString()` is applied to a temporal value, it returns a string representation suitable for parsing by the corresponding <<query-functions-temporal-instant-types, temporal functions>>.
          #This string will therefore be formatted according to the https://en.wikipedia.org/wiki/ISO_8601[ISO 8601] format.""".stripMargin('#'))
    }
    p("""See also <<query-operators-string>>.""")
    p("""Functions:
        #
        #* <<functions-left,left()>>
        #* <<functions-ltrim,lTrim()>>
        #* <<functions-replace,replace()>>
        #* <<functions-reverse,reverse()>>
        #* <<functions-right,right()>>
        #* <<functions-rtrim,rTrim()>>
        #* <<functions-split,split()>>
        #* <<functions-substring,substring()>>
        #* <<functions-tolower,toLower()>>
        #* <<functions-tostring,toString()>>
        #* <<functions-toupper,toUpper()>>
        #* <<functions-trim,trim()>>""".stripMargin('#'))
    section("left()", "functions-left") {
      p("`left()` returns a string containing the specified number of leftmost characters of the original string.")
      function("left(original, length)", "A String.", ("original", "An expression that returns a string."), ("n", "An expression that returns a positive integer."))
      considerations("`left(null, length)` and `left(null, null)` both return `null`", "`left(original, null)` will raise an error.", "If `length` is not a positive integer, an error is raised.", "If `length` exceeds the size of `original`, `original` is returned.")
      query(
        """RETURN left('hello', 3)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("left('hello', 3)" -> "hel")))
        })) {
        resultTable()
      }
    }
    section("ltrim()", "functions-ltrim") {
      p("`lTrim()` returns the original string with leading whitespace removed.")
      function("lTrim(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`lTrim(null)` returns `null`")
      query(
        """RETURN lTrim('   hello')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("lTrim('   hello')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("replace()", "functions-replace") {
      p("`replace()` returns a string in which all occurrences of a specified string in the original string have been replaced by another (specified) string.")
      function("replace(original, search, replace)", "A String.", ("original", "An expression that returns a string."), ("search", "An expression that specifies the string to be replaced in `original`."), ("replace", "An expression that specifies the replacement string."))
      considerations("If any argument is `null`, `null` will be returned.", "If `search` is not found in `original`, `original` will be returned.")
      query(
        """RETURN replace("hello", "l", "w")""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("replace(\"hello\", \"l\", \"w\")" -> "hewwo")))
        })) {
        resultTable()
      }
    }
    section("reverse()", "functions-reverse") {
      p("`reverse()` returns a string in which the order of all characters in the original string have been reversed.")
      function("reverse(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`reverse(null)` returns `null`.")
      query(
        """RETURN reverse('anagram')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("reverse('anagram')" -> "margana")))
        })) {
        resultTable()
      }
    }
    section("right()", "functions-right") {
      p("`right()` returns a string containing the specified number of rightmost characters of the original string.")
      function("right(original, length)", "A String.", ("original", "An expression that returns a string."), ("n", "An expression that returns a positive integer."))
      considerations("`right(null, length)` and `right(null, null)` both return `null`", "`right(original, null)` will raise an error.", "If `length` is not a positive integer, an error is raised.", "If `length` exceeds the size of `original`, `original` is returned.")
      query(
        """RETURN right('hello', 3)""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("right('hello', 3)" -> "llo")))
        })) {
        resultTable()
      }
    }
    section("rtrim()", "functions-rtrim") {
      p("`rTrim()` returns the original string with trailing whitespace removed.")
      function("rTrim(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`rTrim(null)` returns `null`")
      query(
        """RETURN rTrim('hello   ')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("rTrim('hello   ')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("split()", "functions-split") {
      p("`split()` returns a list of strings resulting from the splitting of the original string around matches of the given delimiter.")
      function("split(original, splitDelimiter)", "A list of Strings.", ("original", "An expression that returns a string."), ("splitDelimiter", "The string with which to split `original`."))
      considerations("`split(null, splitDelimiter)` and `split(original, null)` both return `null`")
      query(
        """RETURN split('one,two', ',')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("split('one,two', ',')" -> List("one", "two"))))
        })) {
        resultTable()
      }
    }
    section("substring()", "functions-substring") {
      p(
        """`substring()` returns a substring of the original string, beginning  with a 0-based index start and length.""".stripMargin)
      function("substring(original, start [, length])", "A String.", ("original", "An expression that returns a string."), ("start", "An expression that returns a positive integer, denoting the position at which the substring will begin."), ("length", "An expression that returns a positive integer, denoting how many characters of `original` will be returned."))
      considerations("`start` uses a zero-based index.", "If `length` is omitted, the function returns the substring starting at the position given by `start` and extending to the end of `original`.", "If `original` is `null`, `null` is returned.", "If either `start` or `length` is `null` or a negative integer, an error is raised.", "If `start` is `0`, the substring will start at the beginning of `original`.", "If `length` is `0`, the empty string will be returned.")
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
      function("toLower(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`toLower(null)` returns `null`")
      query(
        """RETURN toLower('HELLO')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toLower('HELLO')" -> "hello")))
        })) {
        resultTable()
      }
    }
    section("toString()", "functions-tostring") {
      p(
        """`toString()` converts an integer, float or boolean value to a string.""".stripMargin)
      function("toString(expression)", "A String.", ("expression", "An expression that returns a number, a boolean, or a string."))
      considerations("`toString(null)` returns `null`", "If `expression` is a string, it will be returned unchanged.")
      query(
        """RETURN toString(11.5),
          #toString('already a string'),
          #toString(true),
          #toString(date({year:1984, month:10, day:11})) AS dateString,
          #toString(datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 341, timezone: 'Europe/Stockholm'})) AS datetimeString,
          #toString(duration({minutes: 12, seconds: -60})) AS durationString""".stripMargin('#'), ResultAssertions((r) => {
          r.toList should equal(List(Map(
            "toString(11.5)" -> "11.5",
            "toString('already a string')" -> "already a string",
            "toString(true)" -> "true",
            "dateString" -> "1984-10-11",
            "datetimeString" -> "1984-10-11T12:31:14.341+01:00[Europe/Stockholm]",
            "durationString" -> "PT11M")
          ))
        })) {
        resultTable()
      }
    }
    section("toUpper()", "functions-toupper") {
      p(
        """`toUpper()` returns the original string in uppercase.""".stripMargin)
      function("toUpper(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`toUpper(null)` returns `null`")
      query(
        """RETURN toUpper('hello')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("toUpper('hello')" -> "HELLO")))
        })) {
        resultTable()
      }
    }
    section("trim()", "functions-trim") {
      p(
        """`trim()` returns the original string with leading and trailing whitespace removed.""".stripMargin)
      function("trim(original)", "A String.", ("original", "An expression that returns a string."))
      considerations("`trim(null)` returns `null`")
      query(
        """RETURN trim('   hello   ')""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("trim('   hello   ')" -> "hello")))
        })) {
        resultTable()
      }
    }
  }.build()

}
