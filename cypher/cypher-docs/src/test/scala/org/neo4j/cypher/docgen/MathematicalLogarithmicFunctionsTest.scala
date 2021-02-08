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

class MathematicalLogarithmicFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Mathematical functions - logarithmic", "query-functions-logarithmic")
    synopsis("These functions all operate on numeric expressions only, and will return an error if used on any other values. See also <<query-operators-mathematical>>.")
    p(
      """Functions:
        |
        |* <<functions-e, e()>>
        |* <<functions-exp, exp()>>
        |* <<functions-log, log()>>
        |* <<functions-log10, log10()>>
        |* <<functions-sqrt, sqrt()>>
      """.stripMargin)
    section("e()", "functions-e") {
      p("`e()` returns the base of the natural logarithm, `e`.")
      function("e()", "A Float.")
      query("RETURN e()", ResultAssertions((r) => {
        r.toList.head("e()") should equal(Math.E)
      })) {
        p("The base of the natural logarithm, `e`, is returned.")
        resultTable()
      }
    }
    section("exp()", "functions-exp") {
      p("`exp()` returns `e^n`, where `e` is the base of the natural logarithm, and `n` is the value of the argument expression.")
      function("e(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`exp(null)` returns `null`.")
      query("RETURN exp(2)", ResultAssertions((r) => {
        r.toList.head("exp(2)").asInstanceOf[Double] should equal(Math.E * Math.E +- 0.00000001)
      })) {
        p("`e` to the power of `2` is returned.")
        resultTable()
      }
    }
    section("log()", "functions-log") {
      p("`log()` returns the natural logarithm of a number.")
      function("log(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`log(null)` returns `null`.", "`log(0)` returns `null`.")
      query("RETURN log(27)", ResultAssertions((r) => {
        r.toList.head("log(27)") should equal(3.295836866004329)
      })) {
        p("The natural logarithm of `27` is returned.")
        resultTable()
      }
    }
    section("log10()", "functions-log10") {
      p("`log10()` returns the common logarithm (base 10) of a number.")
      function("log10(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`log10(null)` returns `null`.", "`log10(0)` returns `null`.")
      query("RETURN log10(27)", ResultAssertions((r) => {
        r.toList.head("log10(27)") should equal(1.4313637641589874)
      })) {
        p("The common logarithm of `27` is returned.")
        resultTable()
      }
    }
    section("sqrt()", "functions-sqrt") {
      p("`sqrt()` returns the square root of a number.")
      function("sqrt(expression)", "A Float.", ("expression", "A numeric expression."))
      considerations("`sqrt(null)` returns `null`.", "`sqrt(<any negative number>)` returns `null`")
      query("RETURN sqrt(256)", ResultAssertions((r) => {
        r.toList.head("sqrt(256)") should equal(16.0)
      })) {
        p("The square root of `256` is returned.")
        resultTable()
      }
    }
  }.build()
}
