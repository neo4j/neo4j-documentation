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

class MathematicalNumericFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Mathematical functions - numeric", "query-functions-numeric")
    initQueries(
      """CREATE (alice:A {name:'Alice', age: 38, eyes: 'brown'}),
        |       (bob:B {name: 'Bob', age: 25, eyes: 'blue'}),
        |       (charlie:C {name: 'Charlie', age: 53, eyes: 'green'}),
        |       (daniel:D {name: 'Daniel', age: 54, eyes: 'brown'}),
        |       (eskil:E {name: 'Eskil', age: 41, eyes: 'blue', array: ['one', 'two', 'three']}),
        |
        |       (alice)-[:KNOWS]->(bob),
        |       (alice)-[:KNOWS]->(charlie),
        |       (bob)-[:KNOWS]->(daniel),
        |       (charlie)-[:KNOWS]->(daniel),
        |       (bob)-[:MARRIED]->(eskil)""")
    synopsis("These functions all operate on numeric expressions only, and will return an error if used on any other values. See also <<query-operators-mathematical>>.")
    p(
      """Functions:
        |
        |* <<functions-abs, abs()>>
        |* <<functions-ceil, ceil()>>
        |* <<functions-floor, floor()>>
        |* <<functions-isnan, isNaN()>>
        |* <<functions-rand, rand()>>
        |* <<functions-round, round()>>
        |* <<functions-round2, round(), with precision>>
        |* <<functions-round3, round(), with precision and rounding mode>>
        |* <<functions-sign, sign()>>
      """.stripMargin)
    p("The following graph is used for the examples below:")
    graphViz()
    section("abs()", "functions-abs") {
      p("`abs()` returns the absolute value of the given number.")
      function("abs(expression)", "The type of the value returned will be that of `expression`.", ("expression", "A numeric expression."))
      considerations("`abs(null)` returns `null`.", "If `expression` is negative, `-(expression)` (i.e. the _negation_ of `expression`) is returned.")
      query("MATCH (a), (e) WHERE a.name = 'Alice' AND e.name = 'Eskil' RETURN a.age, e.age, abs(a.age - e.age)", ResultAssertions((r) => {
        r.toList should equal(List(Map("a.age" -> 38L, "e.age" -> 41L, "abs(a.age - e.age)" -> 3L)))
      })) {
        p("The absolute value of the age difference is returned.")
        resultTable()
      }
    }
    section("ceil()", "functions-ceil") {
      p("`ceil()` returns the smallest floating point number that is greater than or equal to the given number and equal to a mathematical integer.")
      function("ceil(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`ceil(null)` returns `null`.")
      query("RETURN ceil(0.1)", ResultAssertions((r) => {
        r.toList.head("ceil(0.1)") should equal(1.0)
      })) {
        p("The ceil of `0.1` is returned.")
        resultTable()
      }
    }
    section("floor()", "functions-floor") {
      p("`floor()` returns the largest floating point number that is less than or equal to the given number and equal to a mathematical integer.")
      function("floor(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`floor(null)` returns `null`.")
      query("RETURN floor(0.9)", ResultAssertions((r) => {
        r.toList.head("floor(0.9)") should equal(0.0)
      })) {
        p("The floor of `0.9` is returned.")
        resultTable()
      }
    }
    section("isNaN()", "functions-isnan") {
      p("`isNaN()` returns whether the given number is `NaN`.")
      function("isNaN(expression)", "A Boolean.", ("expression", "A numeric expression."))
      considerations("`isNaN(null)` returns `null`.")
      query("RETURN isNaN(0/0.0)", ResultAssertions((r) => {
        r.toList.head("isNaN(0/0.0)") should equal(true)
      })) {
        p("`true` is returned since the value is `NaN`.")
        resultTable()
      }
    }
    section("rand()", "functions-rand") {
      p("`rand()` returns a random floating point number in the range from 0 (inclusive) to 1 (exclusive); i.e. `[0,1)`. " +
        "The numbers returned follow an approximate uniform distribution.")
      function("rand()", "A Float.")
      query("RETURN rand()", ResultAssertions((r) => {
        r.toList.head("rand()").asInstanceOf[Double] should be >= 0.0
        r.toList.head("rand()").asInstanceOf[Double] should be < 1.0
      })) {
        p("A random number is returned.")
        resultTable()
      }
    }
    section("round()", "functions-round") {
      p("`round()` returns the value of the given number rounded to the nearest integer, with half-way values always rounded up.")
      function("round(expression)", "A Float.", ("expression", "A numeric expression to be rounded."))
      considerations("`round(null)` returns `null`.")
      query("RETURN round(3.141592)", ResultAssertions((r) => {
        r.toList.head("round(3.141592)") should equal(3.0)
      })) {
        p("`3.0` is returned.")
        resultTable()
      }
    }
    section("round(), with precision", "functions-round2") {
      p("`round()` returns the value of the given number rounded with the specified precision, with half-values always being rounded up.")
      function("round(expression, precision)", "A Float.",
        ("expression", "A numeric expression to be rounded."),
        ("precision", "A numeric expression specifying precision."))
      considerations("`round(null)` returns `null`.")
      query("RETURN round(3.141592, 3)", ResultAssertions((r) => {
        r.toList.head("round(3.141592, 3)") should equal(3.142)
      })) {
        p("`3.142` is returned.")
        resultTable()
      }
    }
    section("round(), with precision and rounding mode", "functions-round3") {
      p("`round()` returns the value of the given number rounded with the specified precision and the specified rounding mode.")
      function("round(expression, precision, mode)", "A Float.",
        ("expression", "A numeric expression to be rounded."),
        ("precision", "A numeric expression specifying precision."),
        ("mode", "A string expression specifying rounding mode."))
      enumTable("Modes",
        ("Mode", "Description"),
        ("CEILING", "Round towards positive infinity."),
        ("DOWN", "Round towards zero."),
        ("FLOOR", "Round towards zero."),
        ("HALF_DOWN", "Round towards closest value of given precision, with half-values always being rounded down."),
        ("HALF_EVEN", "Round towards closest value of given precision, with half-values always being rounded to the even neighbor."),
        ("HALF_UP", "Round towards closest value of given precision, with half-values always being rounded up."),
        ("UP", "Round away from zero.")
      )
      considerations("`round(null)` returns `null`.")
      query("RETURN round(3.141592, 2, 'CEILING')", ResultAssertions((r) => {
        r.toList.head("round(3.141592, 2, 'CEILING')") should equal(3.15)
      })) {
        p("`3.15` is returned.")
        resultTable()
      }
    }
    section("sign()", "functions-sign") {
      p("`sign()` returns the signum of the given number: `0` if the number is `0`, `-1` for any negative number, and `1` for any positive number.")
      function("sign(expression)", "An Integer.", ("expression", "A numeric expression."))
      considerations("`sign(null)` returns `null`.")
      query("RETURN sign(-17), sign(0.1)", ResultAssertions((r) => {
        r.toList.head("sign(-17)") should equal(-1L)
        r.toList.head("sign(0.1)") should equal(1L)
      })) {
        p("The signs of `-17` and `0.1` are returned.")
        resultTable()
      }
    }
  }.build()
}
