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

import java.time._

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Transaction

class TemporalFunctionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A")
  val title = "Temporal Functions"
  override val linkId = "functions/temporal"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
    }
  }

  override def parameters(name: String): Map[String, Any] = {
    val date = LocalDate.parse("2018-04-05")
    val time = OffsetTime.of(12, 45, 30, 250000000, ZoneOffset.ofHours(1))
    val localtime = LocalTime.of(12, 45, 30, 250000000)
    val datetime = java.time.ZonedDateTime.of(date, localtime, ZoneId.of("+01:00"))
    name match {
      case "parameters=date" =>
        Map("year" -> 2018, "month" -> 5, "day" -> 8)
      case "parameters=datetime" =>
        Map("date" -> date, "time" -> time)
      case "parameters=dateselection" =>
        Map("datetime" -> datetime)
      case "" =>
        Map()
    }
  }

  def text = """
###assertion=returns-one
RETURN

date("2018-04-05")
###
Returns a date parsed from a string.

###assertion=returns-one
RETURN

localtime("12:45:30.25")
###

Returns a time with no time zone.

###assertion=returns-one
RETURN

time("12:45:30.25+01:00")
###

Returns a time in a specified time zone.

###assertion=returns-one
RETURN

localdatetime("2018-04-05T12:34:00")
###

Returns a datetime with no time zone.

###assertion=returns-one
RETURN

datetime("2018-04-05T12:34:00[Europe/Berlin]")
###

Returns a datetime in the specified time zone.

###assertion=returns-one
RETURN

datetime({epochMillis: 3360000})
###
Transforms 3360000 as a UNIX Epoch time into a normal datetime.

###assertion=returns-one parameters=date
RETURN

date({year: $year, month: $month, day: $day})
###

All of the temporal functions can also be called with a map of named components.
This example returns a date from `year`, `month` and `day` components.
Each function supports a different set of possible components.

###assertion=returns-one parameters=datetime
RETURN

datetime({date: $date, time: $time})
###

Temporal types can be created by combining other types. This example creates a `datetime` from a `date` and a `time`.

###assertion=returns-one parameters=dateselection
RETURN

date({date: $datetime, day: 5})
###

Temporal types can be created by selecting from more complex types, as well as overriding individual components.
This example creates a `date` by selecting from a `datetime`, as well as overriding the `day` component.

###assertion=returns-one parameters=dateselection


WITH date("2018-04-05") AS d
RETURN d.year, d.month, d.day, d.week, d.dayOfWeek
###

Accessors allow extracting components of temporal types.

"""
}
