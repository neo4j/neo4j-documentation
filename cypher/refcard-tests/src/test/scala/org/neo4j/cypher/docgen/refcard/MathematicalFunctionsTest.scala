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
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.graphdb.Transaction

class MathematicalFunctionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A")
  val title = "Mathematical functions"
  override val linkId = "functions"

  override def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-none" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 0)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=value" =>
        Map("value" -> "Bob")
      case "parameters=expression" =>
        Map("expr" -> .5)
      case "" =>
        Map()
    }

  def text = """
###assertion=returns-one parameters=expression
RETURN

abs($expr)
###

The absolute value.

###assertion=returns-one parameters=expression
RETURN

isNaN($expr)
###

Returns whether a number is `NaN`, a special floating point number defined in the Floating-Point Standard IEEE 754.

###assertion=returns-one parameters=expression
RETURN

rand()
###

Returns a random number in the range from 0 (inclusive) to 1 (exclusive), `[0,1)`.
Returns a new value for each call.
Also useful for selecting a subset or random ordering.

###assertion=returns-one parameters=expression
RETURN

round($expr)

, floor($expr), ceil($expr)
###

Round to the nearest integer; `ceil()` and `floor()` find the next integer up or down.

###assertion=returns-one parameters=expression
RETURN

sqrt($expr)
###

The square root.

###assertion=returns-one parameters=expression
RETURN

sign($expr)
###

`0` if zero, `-1` if negative, `1` if positive.

###assertion=returns-one parameters=expression
RETURN

sin($expr)

,cos($expr), tan($expr), cot($expr), asin($expr), acos($expr), atan($expr), atan2($expr, $expr), haversin($expr)
###

Trigonometric functions also include `cos()`, `tan()`, `cot()`, `asin()`, `acos()`, `atan()`, `atan2()`, and `haversin()`.
All arguments for the trigonometric functions should be in radians, if not otherwise specified.

###assertion=returns-one parameters=expression
RETURN

degrees($expr), radians($expr), pi()
###

Converts radians into degrees; use `radians()` for the reverse, and `pi()` for π.

###assertion=returns-one parameters=expression
RETURN

log10($expr), log($expr), exp($expr), e()
###

Logarithm base 10, natural logarithm, `e` to the power of the parameter, and the value of `e`.
             """
}
