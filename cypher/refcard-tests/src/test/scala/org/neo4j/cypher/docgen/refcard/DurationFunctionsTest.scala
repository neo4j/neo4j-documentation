/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import java.time._

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction
import org.neo4j.values.storable.DurationValue

class DurationFunctionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A")
  val title = "Duration Functions"
  override val linkId = "functions/temporal/duration/"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "returns-duration-5min" =>
        assertStats(result, nodesCreated = 0)
        val results = result.toList
        assert(results.size === 1)
        assert(results.head.values.head === DurationValue.parse("PT5M"))
      case "returns-date" =>
        assertStats(result, nodesCreated = 0)
        val results = result.toList
        assert(results.size === 1)
        assert(results.head.values.head === LocalDate.parse("2016-02-02"))
      case "returns-duration-accessors1" =>
        assertStats(result, nodesCreated = 0)
        val results = result.toList
        assert(results.size === 1)
        assert(results.head === Map("d.years" -> 1, "d.months" -> 14, "d.days" -> 10, "d.hours" -> 12, "d.minutes" -> 765))
      case "returns-duration-accessors2" =>
        assertStats(result, nodesCreated = 0)
        val results = result.toList
        assert(results.size === 1)
        assert(results.head === Map("d.years" -> 1, "d.monthsOfYear" -> 2, "d.days" -> 10, "d.hours" -> 12, "d.minutesOfHour" -> 45))

    }
  }

  override def parameters(name: String): Map[String, Any] = {
    val date1 = LocalDate.parse("2015-02-02")
    val date2 = LocalDate.parse("2018-04-05")
    name match {
      case "parameters=between" =>
        Map("date1" -> date1, "date2" -> date2)
      case "" =>
        Map()
    }
  }

  def text = """
###assertion=returns-one
RETURN

duration("P1Y2M10DT12H45M30.25S")
###

Returns a duration of 1 year, 2 months, 10 days, 12 hours, 45 minutes and 30.25 seconds.

###assertion=returns-one parameters=between
RETURN

duration.between($date1,$date2)
###

Returns a duration between two temporal instances.

###assertion=returns-duration-accessors1


WITH duration("P1Y2M10DT12H45M") AS d
RETURN d.years, d.months, d.days, d.hours, d.minutes
###

Returns 1 year, 14 months, 10 days, 12 hours and 765 minutes.

###assertion=returns-duration-accessors2


WITH duration("P1Y2M10DT12H45M") AS d
RETURN d.years, d.monthsOfYear, d.days, d.hours, d.minutesOfHour
###

Returns 1 year, 2 months, 10 days, 12 hours and 45 minutes.

###assertion=returns-date
RETURN

date("2015-01-01") + duration("P1Y1M1D")
###

Returns a date of `2016-02-02`. It is also possible to subtract durations from temporal instances.

###assertion=returns-duration-5min
RETURN

duration("PT30S") * 10
###

Returns a duration of 5 minutes. It is also possible to divide a duration by a number.
"""
}
