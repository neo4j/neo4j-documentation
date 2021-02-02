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

class MathematicalTrigonometricFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Mathematical functions - trigonometric", "query-functions-mathematical-trigonometric")
    synopsis("These functions all operate on numeric expressions only, and will return an error if used on any other values. See also <<query-operators-mathematical>>.")
    p(
      """Functions:
        |
        |* <<functions-acos, acos()>>
        |* <<functions-asin, asin()>>
        |* <<functions-atan, atan()>>
        |* <<functions-atan2, atan2()>>
        |* <<functions-cos, cos()>>
        |* <<functions-cot, cot()>>
        |* <<functions-degrees, degrees()>>
        |* <<functions-haversin, haversin()>>
        |* <<functions-spherical-distance-using-haversin, Spherical distance using the `haversin()` function>>
        |* <<functions-pi, pi()>>
        |* <<functions-radians, radians()>>
        |* <<functions-sin, sin()>>
        |* <<functions-tan, tan()>>
      """.stripMargin)
    section("acos()", "functions-acos") {
      p("`acos()` returns the arccosine of a number in radians.")
      function("acos(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`acos(null)` returns `null`.", "If (`expression` < -1) or (`expression` > 1), then (`acos(expression)`) returns `null`.")
      query("RETURN acos(0.5)", ResultAssertions((r) => {
        r.toList.head("acos(0.5)") should equal(1.0471975511965979)
      })) {
        p("The arccosine of `0.5` is returned.")
        resultTable()
      }
    }
    section("asin()", "functions-asin") {
      p("`asin()` returns the arcsine of a number in radians.")
      function("asin(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`asin(null)` returns `null`.", "If (`expression` < -1) or (`expression` > 1), then (`asin(expression)`) returns `null`.")
      query("RETURN asin(0.5)", ResultAssertions((r) => {
        r.toList.head("asin(0.5)") should equal(0.5235987755982989)
      })) {
        p("The arcsine of `0.5` is returned.")
        resultTable()
      }
    }
    section("atan()", "functions-atan") {
      p("`atan()` returns the arctangent of a number in radians.")
      function("atan(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`atan(null)` returns `null`.")
      query("RETURN atan(0.5)", ResultAssertions((r) => {
        r.toList.head("atan(0.5)") should equal(0.4636476090008061)
      })) {
        p("The arctangent of `0.5` is returned.")
        resultTable()
      }
    }
    section("atan2()", "functions-atan2") {
      p("`atan2()` returns the arctangent2 of a set of coordinates in radians.")
      function("atan2(expression1, expression2)", "A Float.", ("expression1", "A numeric expression for y that represents the angle in radians."), ("expression2", "A numeric expression for x that represents the angle in radians."))
      considerations("`atan2(null, null)`, `atan2(null, expression2)` and `atan(expression1, null)` all return `null`.")
      query("RETURN atan2(0.5, 0.6)", ResultAssertions((r) => {
        r.toList.head("atan2(0.5, 0.6)") should equal(0.6947382761967033)
      })) {
        p("The arctangent2 of `0.5` and `0.6` is returned.")
        resultTable()
      }
    }
    section("cos()", "functions-cos") {
      p("`cos()` returns the cosine of a number.")
      function("cos(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`cos(null)` returns `null`.")
      query("RETURN cos(0.5)", ResultAssertions((r) => {
        r.toList.head("cos(0.5)") should equal(0.8775825618903728)
      })) {
        p("The cosine of `0.5` is returned.")
        resultTable()
      }
    }
    section("cot()", "functions-cot") {
      p("`cot()` returns the cotangent of a number.")
      function("cot(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`cot(null)` returns `null`.", "`cot(0)` returns `null`.")
      query("RETURN cot(0.5)", ResultAssertions((r) => {
        r.toList.head("cot(0.5)") should equal(1.830487721712452)
      })) {
        p("The cotangent of `0.5` is returned.")
        resultTable()
      }
    }
    section("degrees()", "functions-degrees") {
      p("`degrees()` converts radians to degrees.")
      function("degrees(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`degrees(null)` returns `null`.")
      query("RETURN degrees(3.14159)", ResultAssertions((r) => {
        r.toList.head("degrees(3.14159)").asInstanceOf[Double] should equal(180.0 +- 0.001)
      })) {
        p("The number of degrees in something close to _pi_ is returned.")
        resultTable()
      }
    }
    section("haversin()", "functions-haversin") {
      p("`haversin()` returns half the versine of a number.")
      function("haversin(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`haversin(null)` returns `null`.")
      query("RETURN haversin(0.5)", ResultAssertions((r) => {
        r.toList.head("haversin(0.5)") should equal(0.06120871905481362)
      })) {
        p("The haversine of `0.5` is returned.")
        resultTable()
      }
    }
    section("Spherical distance using the `haversin()` function", "functions-spherical-distance-using-haversin") {
      p(
        """The `haversin()` function may be used to compute the distance on the surface of a sphere between two
          |points (each given by their latitude and longitude). In this example the spherical distance (in km)
          |between Berlin in Germany (at lat 52.5, lon 13.4) and San Mateo in California (at lat 37.5, lon -122.3)
          |is calculated using an average earth radius of 6371 km.""")
      query(
        """CREATE (ber:City {lat: 52.5, lon: 13.4}), (sm:City {lat: 37.5, lon: -122.3})
          |RETURN 2 * 6371 * asin(sqrt(haversin(radians( sm.lat - ber.lat ))
          |       + cos(radians( sm.lat )) * cos(radians( ber.lat )) *
          |       haversin(radians( sm.lon - ber.lon )))) AS dist""".stripMargin, ResultAssertions((r) => {
          r.toList.head("dist").asInstanceOf[Double] should equal(9129.0 +- 1)
        })) {
        p("The estimated distance between *'Berlin'* and *'San Mateo'* is returned.")
        resultTable()
      }
    }
    section("pi()", "functions-pi") {
      p("`pi()` returns the mathematical constant _pi_.")
      function("pi()", "A Float.")
      query("RETURN pi()", ResultAssertions((r) => {
        r.toList.head("pi()") should equal(3.141592653589793)
      })) {
        p("The constant _pi_ is returned.")
        resultTable()
      }
    }
    section("radians()", "functions-radians") {
      p("`radians()` converts degrees to radians.")
      function("radians(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in degrees."))
      considerations("`radians(null)` returns `null`.")
      query("RETURN radians(180)", ResultAssertions((r) => {
        r.toList.head("radians(180)") should equal(3.141592653589793)
      })) {
        p("The number of radians in `180` degrees is returned (pi).")
        resultTable()
      }
    }
    section("sin()", "functions-sin") {
      p("`sin()` returns the sine of a number.")
      function("sin(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`sin(null)` returns `null`.")
      query("RETURN sin(0.5)", ResultAssertions((r) => {
        r.toList.head("sin(0.5)") should equal(0.479425538604203)
      })) {
        p("The sine of `0.5` is returned.")
        resultTable()
      }
    }
    section("tan()", "functions-tan") {
      p("`tan()` returns the tangent of a number.")
      function("tan(expression)", "A Float.", ("expression", "A numeric expression that represents the angle in radians."))
      considerations("`tan(null)` returns `null`.")
      query("RETURN tan(0.5)", ResultAssertions((r) => {
        r.toList.head("tan(0.5)") should equal(0.5463024898437905)
      })) {
        p("The tangent of `0.5` is returned.")
        resultTable()
      }
    }
  }.build()
}
