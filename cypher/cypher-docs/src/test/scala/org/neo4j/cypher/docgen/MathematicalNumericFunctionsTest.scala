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

class MathematicalNumericFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Mathematical functions - numeric", "query-functions-mathematical-numeric")
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
    synopsis("These functions all operate on numerical expressions only, and will return an error if used on any other values. See also <<query-operators-mathematical>>.")
    p(
      """
        |* <<functions-abs, abs()>>
        |* <<functions-ceil, ceil()>>
        |* <<functions-floor, floor()>>
        |* <<functions-rand, rand()>>
        |* <<functions-round, round()>>
        |* <<functions-sign, sign()>>
      """.stripMargin)
    p("The following graph is used for the examples below:")
    graphViz()
    section("abs()", "functions-abs") {
      p("`abs()` returns the absolute value of a number.")
      function("abs(expression)", ("expression", "A numeric expression."))
      query("MATCH (a), (e) WHERE a.name = 'Alice' AND e.name = 'Eskil' RETURN a.age, e.age, abs(a.age - e.age)", ResultAssertions((r) => {
        r.toList should equal(List(Map("a.age" -> 38L, "e.age" -> 41L, "abs(a.age - e.age)" -> 3L)))
      })) {
        p("The absolute value of the age difference is returned.")
        resultTable()
      }
    }
    section("ceil()", "functions-ceil") {
      p("`ceil()` returns the smallest integer greater than or equal to the argument.")
      function("ceil(expression)", ("expression", "A numeric expression."))
      query("RETURN ceil(0.1)", ResultAssertions((r) => {
        r.toList.head("ceil(0.1)") should equal(1.0)
      })) {
        p("The ceil of `0.1`.")
        resultTable()
      }
    }
    section("floor()", "functions-floor") {
      p("`floor()` returns the greatest integer less than or equal to the expression.")
      function("floor(expression)", ("expression", "A numeric expression."))
      query("RETURN floor(0.9)", ResultAssertions((r) => {
        r.toList.head("floor(0.9)") should equal(0.0)
      })) {
        p("The floor of `0.9` is returned.")
        resultTable()
      }
    }
    section("rand()", "functions-rand") {
      p("`rand()` returns a random number in the range from 0 (inclusive) to 1 (exclusive), [0,1). The numbers returned follow an approximate uniform distribution.")
      function("rand()")
      query("RETURN rand()", ResultAssertions((r) => {
        r.toList.head("rand()").asInstanceOf[Double] should be >= 0.0
        r.toList.head("rand()").asInstanceOf[Double] should be < 1.0
      })) {
        p("A random number is returned.")
        resultTable()
      }
    }
    section("round()", "functions-round") {
      p("`round()` returns the numerical expression, rounded to the nearest integer.")
      function("round(expression)", ("expression", "A numeric expression that represents the angle in radians."))
      query("RETURN round(3.141592)", ResultAssertions((r) => {
        r.toList.head("round(3.141592)") should equal(3.0)
      })) {
        p("`3.0` is returned.")
        resultTable()
      }
    }
    section("sign()", "functions-sign") {
      p("`sign()` returns the signum of a number -- zero if the expression is zero, `-1` for any negative number, and `1` for any positive number.")
      function("sign(expression)", ("expression", "A numeric expression."))
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
