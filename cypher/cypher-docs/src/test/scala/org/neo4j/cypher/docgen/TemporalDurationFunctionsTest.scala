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

import java.time._
import java.util.function.Supplier

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}
import org.neo4j.values.storable._

class TemporalDurationsFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    val defaultZoneSupplier: Supplier[ZoneId] = new Supplier[ZoneId] {
      override def get(): ZoneId = ZoneOffset.UTC
    }

    doc("Temporal functions - duration", "functions-duration")
    synopsis(
      """Cypher provides functions allowing for the creation and manipulation of values for a _Duration_ temporal type.""".stripMargin)
    note {
      p("""See also <<cypher-temporal>> and <<query-operators-temporal>>.""")
    }
      p(
        """
        |duration():
        |
        |* <<functions-duration-create-components, Creating a _Duration_ from duration components>>
        |* <<functions-duration-create-string, Creating a _Duration_ from a string>>
        |* <<functions-duration-computing, Computing the _Duration_ between two temporal instants>>
      """.stripMargin)
      p(
        """Information regarding specifying and accessing components of a _Duration_ value can be found <<cypher-temporal-durations, here>>.""".stripMargin)
    section("Creating a _Duration_ from duration components", "functions-duration-create-components") {
      p(
        """`duration()` can construct a _Duration_ from a map of its components in the same way as the temporal instant types.
          |
          |* `years`
          |* `quarters`
          |* `months`
          |* `weeks`
          |* `days`
          |* `hours`
          |* `minutes`
          |* `seconds`
          |* `milliseconds`
          |* `microseconds`
          |* `nanoseconds`
          |
      """.stripMargin)
      function("duration([ {years, quarters, months, weeks, days, hours, minutes, seconds, milliseconds, microseconds, nanoseconds} ])", "A Duration.", ("A single map consisting of the following:", ""), ("years", "A numeric expression."), ("quarters", "A numeric expression."), ("months", "A numeric expression."), ("weeks", "A numeric expression."), ("days", "A numeric expression."), ("hours", "A numeric expression."), ("minutes", "A numeric expression."), ("seconds", "A numeric expression."), ("milliseconds", "A numeric expression."), ("microseconds", "A numeric expression."), ("nanoseconds", "A numeric expression."))
      considerations("At least one parameter must be provided (`duration()` and `duration({})` are invalid).", "There is no constraint on how many of the parameters are provided.", "It is possible to have a _Duration_ where the amount of a smaller unit (e.g. `seconds`) exceeds the threshold of a larger unit (e.g. `days`).", "The values of the parameters may be expressed as decimal fractions.", "The values of the parameters may be arbitrarily large.", "The values of the parameters may be negative.")
      preformattedQuery(
        """UNWIND [
          |  duration({days: 14, hours:16, minutes: 12}),
          |  duration({months: 5, days: 1.5}),
          |  duration({months: 0.75}),
          |  duration({weeks: 2.5}),
          |  duration({minutes: 1.5, seconds: 1, milliseconds: 123, microseconds: 456, nanoseconds: 789}),
          |  duration({minutes: 1.5, seconds: 1, nanoseconds: 123456789})
          |] AS aDuration
          |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(
            Map("aDuration" -> DurationValue.parse("P14DT16H12M")),
            Map("aDuration" -> DurationValue.parse("P5M1DT12H")),
            Map("aDuration" -> DurationValue.parse("P22DT19H51M49.5S")),
            Map("aDuration" -> DurationValue.parse("P17DT12H")),
            Map("aDuration" -> DurationValue.parse("PT1M31.123456789S")),
            Map("aDuration" -> DurationValue.parse("PT1M31.123456789S"))
          ))
        })) {
        resultTable()
      }
    }
    section("Creating a _Duration_ from a string", "functions-duration-create-string") {
      p(
        """`duration()` returns the _Duration_ value obtained by parsing a string representation of a temporal amount.""".stripMargin)
      function("duration(temporalAmount)", "A Duration.", ("temporalAmount", "A string representing a temporal amount."))
      considerations("`temporalAmount` must comply with either the <<cypher-temporal-specifying-durations, unit based form or date-and-time based form defined for _Durations_>>.")
      preformattedQuery(
        """UNWIND [
          |  duration("P14DT16H12M"),
          |  duration("P5M1.5D"),
          |  duration("P0.75M"),
          |  duration("PT0.75M"),
          |  duration("P2012-02-02T14:37:21.545")
          |] AS aDuration
          |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(
            Map("aDuration" -> DurationValue.parse("P14DT16H12M")),
            Map("aDuration" -> DurationValue.parse("P5M1DT12H")),
            Map("aDuration" -> DurationValue.parse("P22DT19H51M49.5S")),
            Map("aDuration" -> DurationValue.parse("PT45S")),
            Map("aDuration" -> DurationValue.parse("P2012Y2M2DT14H37M21.545S"))
          ))
        })) {
        resultTable()
      }
    }
    section("Computing the _Duration_ between two temporal instants", "functions-duration-computing") {
      p(
        """`duration()` has sub-functions which compute the _logical difference_ (in days, months, etc) between two temporal instant values:
          |
          |* `duration.between(a, b)`: Computes the difference in multiple components between instant `a` and instant `b`. This captures month, days, seconds and sub-seconds differences separately.
          |* `duration.inMonths(a, b)`: Computes the difference in whole months (or quarters or years) between instant `a` and instant `b`. This captures the difference as the total number of months. Any difference smaller than a whole month is disregarded.
          |* `duration.inDays(a, b)`: Computes the difference in whole days (or weeks) between instant `a` and instant `b`. This captures the difference as the total number of days.  Any difference smaller than a whole day is disregarded.
          |* `duration.inSeconds(a, b)`: Computes the difference in seconds (and fractions of seconds, or minutes or hours) between instant `a` and instant `b`. This captures the difference as the total number of seconds.
          |""".stripMargin)
      section("duration.between()", "functions-duration-between") {
        p(
          """`duration.between()` returns the _Duration_ value equal to the difference between the two given instants.""".stripMargin)
        function("duration.between(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
        considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.",
          "If `instant~1~` has a time component and `instant~2~` does not, the time component of `instant~2~` is assumed to be midnight, and vice versa.",
          "If `instant~1~` has a time zone component and `instant~2~` does not, the time zone component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "If `instant~1~` has a date component and `instant~2~` does not, the date component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.")
        preformattedQuery(
          """UNWIND [
            |  duration.between(date("1984-10-11"), date("1985-11-25")),
            |  duration.between(date("1985-11-25"), date("1984-10-11")),
            |  duration.between(date("1984-10-11"), datetime("1984-10-12T21:40:32.142+0100")),
            |  duration.between(date("2015-06-24"), localtime("14:30")),
            |  duration.between(localtime("14:30"), time("16:30+0100")),
            |  duration.between(localdatetime("2015-07-21T21:40:32.142"), localdatetime("2016-07-21T21:45:22.142")),
            |  duration.between(datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/Stockholm'}), datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/London'}))
            |] AS aDuration
            |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("aDuration" -> DurationValue.parse("P1Y1M14D")),
              Map("aDuration" -> DurationValue.parse("P-1Y-1M-14D")),
              Map("aDuration" -> DurationValue.parse("P1DT21H40M32.142S")),
              Map("aDuration" -> DurationValue.parse("PT14H30M")),
              Map("aDuration" -> DurationValue.parse("PT2H")),
              Map("aDuration" -> DurationValue.parse("P1YT4M50S")),
              Map("aDuration" -> DurationValue.parse("PT1H"))
            ))
          })) {
          resultTable()
        }
      }
      section("duration.inMonths()", "functions-duration-inmonths") {
        p(
          """`duration.inMonths()` returns the _Duration_ value equal to the difference in whole months, quarters or years between the two given instants.""".stripMargin)
        function("duration.inMonths(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
        considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.",
          "If `instant~1~` has a time component and `instant~2~` does not, the time component of `instant~2~` is assumed to be midnight, and vice versa.",
          "If `instant~1~` has a time zone component and `instant~2~` does not, the time zone component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "If `instant~1~` has a date component and `instant~2~` does not, the date component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "Any difference smaller than a whole month is disregarded.")

        preformattedQuery(
          """UNWIND [
            |  duration.inMonths(date("1984-10-11"), date("1985-11-25")),
            |  duration.inMonths(date("1985-11-25"), date("1984-10-11")),
            |  duration.inMonths(date("1984-10-11"), datetime("1984-10-12T21:40:32.142+0100")),
            |  duration.inMonths(date("2015-06-24"), localtime("14:30")),
            |  duration.inMonths(localdatetime("2015-07-21T21:40:32.142"), localdatetime("2016-07-21T21:45:22.142")),
            |  duration.inMonths(datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/Stockholm'}), datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/London'}))
            |] AS aDuration
            |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("aDuration" -> DurationValue.parse("P1Y1M")),
              Map("aDuration" -> DurationValue.parse("P-1Y-1M")),
              Map("aDuration" -> DurationValue.parse("PT0S")),
              Map("aDuration" -> DurationValue.parse("PT0S")),
              Map("aDuration" -> DurationValue.parse("P1Y")),
              Map("aDuration" -> DurationValue.parse("PT0S"))
            ))
          })) {
          resultTable()
        }
      }
      section("duration.inDays()", "functions-duration-indays") {
        p(
          """`duration.inDays()` returns the _Duration_ value equal to the difference in whole days or weeks between the two given instants.""".stripMargin)
        function("duration.inDays(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
        considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.",
          "If `instant~1~` has a time component and `instant~2~` does not, the time component of `instant~2~` is assumed to be midnight, and vice versa.",
          "If `instant~1~` has a time zone component and `instant~2~` does not, the time zone component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "If `instant~1~` has a date component and `instant~2~` does not, the date component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "Any difference smaller than a whole day is disregarded.")
        preformattedQuery(
          """UNWIND [
            |  duration.inDays(date("1984-10-11"), date("1985-11-25")),
            |  duration.inDays(date("1985-11-25"), date("1984-10-11")),
            |  duration.inDays(date("1984-10-11"), datetime("1984-10-12T21:40:32.142+0100")),
            |  duration.inDays(date("2015-06-24"), localtime("14:30")),
            |  duration.inDays(localdatetime("2015-07-21T21:40:32.142"), localdatetime("2016-07-21T21:45:22.142")),
            |  duration.inDays(datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/Stockholm'}), datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/London'}))
            |] AS aDuration
            |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("aDuration" -> DurationValue.parse("P410D")),
              Map("aDuration" -> DurationValue.parse("P-410D")),
              Map("aDuration" -> DurationValue.parse("P1D")),
              Map("aDuration" -> DurationValue.parse("PT0S")),
              Map("aDuration" -> DurationValue.parse("P366D")),
              Map("aDuration" -> DurationValue.parse("PT0S"))
            ))
          })) {
          resultTable()
        }
      }
      section("duration.inSeconds()", "functions-duration-inseconds") {
        p(
          """`duration.inSeconds()` returns the _Duration_ value equal to the difference in seconds and fractions of seconds, or minutes or hours, between the two given instants.""".stripMargin)
        function("duration.inSeconds(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
        considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.",
          "If `instant~1~` has a time component and `instant~2~` does not, the time component of `instant~2~` is assumed to be midnight, and vice versa.",
          "If `instant~1~` has a time zone component and `instant~2~` does not, the time zone component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.",
          "If `instant~1~` has a date component and `instant~2~` does not, the date component of `instant~2~` is assumed to be the same as that of `instant~1~`, and vice versa.")
        preformattedQuery(
          """UNWIND [
            |  duration.inSeconds(date("1984-10-11"), date("1984-10-12")),
            |  duration.inSeconds(date("1984-10-12"), date("1984-10-11")),
            |  duration.inSeconds(date("1984-10-11"), datetime("1984-10-12T01:00:32.142+0100")),
            |  duration.inSeconds(date("2015-06-24"), localtime("14:30")),
            |  duration.inSeconds(datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/Stockholm'}), datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/London'}))
            |] AS aDuration
            |RETURN aDuration""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("aDuration" -> DurationValue.parse("PT24H")),
              Map("aDuration" -> DurationValue.parse("PT-24H")),
              Map("aDuration" -> DurationValue.parse("PT25H32.142S")),
              Map("aDuration" -> DurationValue.parse("PT14H30M")),
              Map("aDuration" -> DurationValue.parse("PT1H"))
            ))
          })) {
          resultTable()
        }
      }
    }
  }.build()
}
