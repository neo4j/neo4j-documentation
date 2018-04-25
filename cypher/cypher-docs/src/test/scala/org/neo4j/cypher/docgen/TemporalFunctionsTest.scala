/*
 * Copyright (c) 2002-2018 "Neo Technology,"
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

import java.time._

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class TemporalFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Temporal functions", "query-functions-temporal")
    synopsis(
      """Cypher provides functions allowing for the creation of values for each temporal type -- Date, Time, LocalTime, DateTime, LocalDateTime and Duration.""".stripMargin)
    p(
      """Each function bears the same name as the type, and construct the type they correspond to in one of four ways:
        |
        |* Capturing the current time
        |* Composing the components of the type
        |* Parsing a string representation of the temporal value
        |* Selecting and composing components from another temporal value by
        | ** either combining temporal values (such as combining a _Date_ with a _Time_ to create a _DateTime_), or
        | ** selecting parts from a temporal value (such as selecting the _Date_ from a _DateTime_).""".stripMargin)
    note {
      p("""See also <<cypher-temporal>> and <<query-operators-temporal>>.""")
    }
    p(
      """
        |
        |[options="header"]
        ||===
        || Function                   | Date | Time | LocalTime | DateTime | LocalDateTime
        || Getting the current value  | <<functions-date-current, X>> | <<functions-time-current, X>> | <<functions-localtime-current, X>> | <<functions-datetime-current, X>> | <<functions-localdatetime-current, X>>
        || Creating a calendar (Year-Month-Day) value | <<functions-date-calendar, X>> | | | <<functions-datetime-calendar, X>> | <<functions-localdatetime-calendar, X>>
        || Creating a week (Year-Week-Day) value | <<functions-date-week, X>> | | | <<functions-datetime-week, X>> | <<functions-localdatetime-week, X>>
        || Creating a quarter (Year-Quarter-Day) value | <<functions-date-quarter, X>> | | | <<functions-datetime-quarter, X>> | <<functions-localdatetime-quarter, X>>
        || Creating an ordinal (Year-Day) value | <<functions-date-ordinal, X>> | | | <<functions-datetime-ordinal, X>> | <<functions-localdatetime-ordinal, X>>
        || Creating a value from time components |  | <<functions-time-create, X>> | <<functions-localtime-create, X>> | |
        || Creating a value from a string | <<functions-date-string, X>> | <<functions-time-string, X>> | <<functions-localtime-string, X>> | <<functions-datetime-string, X>> | <<functions-localdatetime-string, X>>
        || Creating a value from a timestamp | | | | <<functions-datetime-timestamp, X>> |
        ||===
        |
        |""")
    section("date(): getting the current _Date_", "functions-date-current") {
      p(
        """`date()` returns the current _Date_ value.""".stripMargin)
      function("date()", "A Date.")
      query(
        """RETURN date() AS currentDate""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDate]("currentDate").next()
          now should be(a[LocalDate])
        })) {
        p("""The current date is returned.""")
        resultTable()
      }
    }
    section("date(): creating a calendar (Year-Month-Day) _Date_", "functions-date-calendar") {
      p(
        """`date()` returns a _Date_ value with the specified _year_, _month_ and _day_ component values.""".stripMargin)
      function("date({year [, month, day]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."))
      considerations("The _day of the month_ component will default to `1` if `day` is omitted.", "The _month_ component will default to `1` if `month` is omitted.", "If `month` is omitted, `day` must also be omitted.")
      query(
        """UNWIND [date({year:1984, month:10, day:11}),
          | date({year:1984, month:10}),
          | date({year:1984})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 10, 11)), Map("theDate" -> LocalDate.of(1984, 10, 1)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
        })) {
        resultTable()
      }
    }
    section("date(): creating a week (Year-Week-Day) _Date_", "functions-date-week") {
      p(
        """`date()` returns a _Date_ value with the specified _year_, _week_ and _dayOfWeek_ component values.""".stripMargin)
      function("date({year [, week, dayOfWeek]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."))
      considerations("The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.", "The _week_ component will default to `1` if `week` is omitted.", "If `week` is omitted, `dayOfWeek` must also be omitted.")
      query(
        """UNWIND [date({year:1984, week:10, dayOfWeek:3}),
          | date({year:1984, week:10}),
          | date({year:1984})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 3, 7)), Map("theDate" -> LocalDate.of(1984, 3, 5)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
        })) {
        resultTable()
      }
    }
    section("date(): creating a quarter (Year-Quarter-Day) _Date_", "functions-date-quarter") {
      p(
        """`date()` returns a _Date_ value with the specified _year_, _quarter_ and _dayOfQuarter_ component values.""".stripMargin)
      function("date({year [, quarter, dayOfQuarter]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."))
      considerations("The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.", "The _quarter_ component will default to `1` if `quarter` is omitted.", "If `quarter` is omitted, `dayOfQuarter` must also be omitted.")
      query(
        """UNWIND [date({year:1984, quarter:3, dayOfQuarter: 45}),
          | date({year:1984, quarter:3}),
          | date({year:1984})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 8, 14)), Map("theDate" -> LocalDate.of(1984, 7, 1)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
        })) {
        resultTable()
      }
    }
    section("date(): creating an ordinal (Year-Day) _Date_", "functions-date-ordinal") {
      p(
        """`date()` returns a _Date_ value with the specified _year_ and _ordinalDay_ component values.""".stripMargin)
      function("date({year [, ordinalDay]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."))
      considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.")
      query(
        """UNWIND [date({year:1984, ordinalDay:202}),
          | date({year:1984})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 7, 20)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
        })) {
        p("""The date corresponding to `11 February 1984` is returned.""")
        resultTable()
      }
    }
    section("date(): creating a _Date_ from a string", "functions-date-string") {
      p(
        """`date()` returns the _Date_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
      function("date(temporalValue)", "A Date.", ("temporalValue", "A string representing a temporal value."))
      considerations("`temporalValue` must comply with XXLINK.", "`date(null)` returns the current date.", "`temporalValue` must denote a valid date; i.e. a `temporalValue` denoting `30 February 2001` is invalid.")
      query(
        """UNWIND [date('2015-07-21'),
          |   date('2015-07'),
          |   date('201507'),
          |   date('2015-W30-2'),
          |   date('2015202'),
          |   date('2015')] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("datetime(): getting the current _DateTime_", "functions-datetime-current") {
      p(
        """`datetime()` returns the current _DateTime_ value.
          |If no time zone parameter is specified, the local time zone will be used.
        """.stripMargin)
      function("datetime([ {timezone} ])", "A DateTime.", ("A single map consisting of the following:", ""), ("timezone", "An expression that represents the time zone"))
      considerations("If no parameters are provided, `datetime()` should be invoked (`datetime({})` is invalid).")
      query(
        """RETURN datetime() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[ZonedDateTime]("currentDateTime").next()
          now should be(a[ZonedDateTime])
        })) {
        p("""The current date and time using the local time zone is returned.""")
        resultTable()
      }
      query(
        """RETURN datetime( {timezone: "America/Los Angeles"} ) AS currentDateTimeInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[ZonedDateTime]("currentDateTimeInLA").next()
          now should be(a[ZonedDateTime])
        })) {
        p("""The current date and time of day in California is returned.""")
        resultTable()
      }
    }
    section("datetime(): creating a calendar (Year-Month-Day) _DateTime_", "functions-datetime-calendar") {
      p(
        """`datetime()` returns a _DateTime_ value with the specified _year_, _month_, _day_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
      function("datetime({year [, month, day, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("The _month_ component will default to `1` if `month` is omitted.", "The _day of the month_ component will default to `1` if `day` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "The _timezone_ component will default to the `UTC` time zone if `timezone` is omitted.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `month`, `day`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `month` and `day`, but specifying `year`, `month`, `day` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 123, microsecond: 456, nanosecond: 789}),
          |   datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 645, timezone: '+01:00'}),
          |   datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, timezone: '+01:00'}),
          |   datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14}),
          |   datetime({year:1984, month:10, day:11, hour:12, minute:31, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'}),
          |   datetime({year:1984, month:10, day:11, timezone: 'Europe/Stockholm'})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("datetime(): creating a week (Year-Week-Day) _DateTime_", "functions-datetime-week") {
      p(
        """`datetime()` returns a _DateTime_ value with the specified _year_, _week_, _dayOfWeek_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
      function("datetime({year [, week, dayOfWeek, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("The _week_ component will default to `1` if `week` is omitted.", "The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "The _timezone_ component will default to the `UTC` time zone if `timezone` is omitted.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `week`, `dayOfWeek`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `week` and `dayOfWeek`, but specifying `year`, `week`, `dayOfWeek` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14}),
          |   datetime({year:1984, week:10, dayOfWeek:3, hour:12, timezone: '+01:00'}),
          |   datetime({year:1984, week:10, dayOfWeek:3, timezone: 'Europe/Stockholm'})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
          // starting off...r.toList should equal(List(Map("theDate" -> ZonedDateTime.of(1984, 10, 11, 12, 31, 14, 123456789, ZoneId.of("Z")))), Map("theDate" -> ZonedDateTime.of(1984, 10, 11, 12, 31, 14, 645, ZoneId.ofOffset("UTC", ZoneOffset.ofHours(1)))))
        })) {
        resultTable()
      }
    }
    section("datetime(): creating a quarter (Year-Quarter-Day) _DateTime_", "functions-datetime-quarter") {
      p(
        """`datetime()` returns a _DateTime_ value with the specified _year_, _quarter_, _dayOfQuarter_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
      function("datetime({year [, quarter, dayOfQuarter, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("The _quarter_ component will default to `1` if `quarter` is omitted.", "The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "The _timezone_ component will default to the `UTC` time zone if `timezone` is omitted.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `quarter`, `dayOfQuarter`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `quarter` and `dayOfQuarter`, but specifying `year`, `quarter`, `dayOfQuarter` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, microsecond: 645876}),
          |   datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, timezone: '+01:00'}),
          |   datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, quarter:3, dayOfQuarter: 45})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("datetime(): creating an ordinal (Year-Day) _DateTime_", "functions-datetime-ordinal") {
      p(
        """`datetime()` returns a _DateTime_ value with the specified _year_, _ordinalDay_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
      function("datetime({year [, ordinalDay, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "The _timezone_ component will default to the `UTC` time zone if `timezone` is omitted.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `ordinalDay`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year` and `ordinalDay`, but specifying `year`, `ordinalDay` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [datetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, timezone: '+01:00'}),
          |   datetime({year:1984, ordinalDay:202, timezone: 'Europe/Stockholm'}),
          |   datetime({year:1984, ordinalDay:202})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("datetime(): creating a _DateTime_ from a string", "functions-datetime-string") {
      p(
        """`datetime()` returns the _DateTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
      function("datetime(temporalValue)", "A DateTime.", ("temporalValue", "A string representing a temporal value."))
      considerations("`temporalValue` must comply with XXLINK.", "`datetime(null)` returns the current date and time.", "`temporalValue` must denote a valid date and time; i.e. a `temporalValue` denoting `30 February 2001` is invalid.")
      query(
        """UNWIND [datetime('2015-07-21T21:40:32.142+0100'),
          |   datetime('2015-W30-2T214032.142Z'),
          |   datetime('2015T214032-0100'),
          |   datetime('20150721T21:40-01:30'),
          |   datetime('2015-W30T2140-02'),
          |   datetime('2015202T21+18:00'),
          |   datetime('2015-07-21T21:40:32.142[Europe/London]'),
          |   datetime('2015-07-21T21:40:32.142-04[America/New_York]')] AS theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("datetime(): creating a _DateTime_ from a timestamp", "functions-datetime-timestamp") {
      p(
        """`datetime()` returns the _DateTime_ value at the specified number of _seconds_ or _milliseconds_ from the UNIX epoch in the UTC time zone.""".stripMargin)
      p("Conversions to other temporal instant types from UNIX epoch representations can be achieved by transforming a _DateTime_ value to one of these types.")
      p("Information about how to convert _DateTime_ values to UNIX epoch representations may be found <<XXX, here>>.")
      function("datetime({ epochSeconds | epochMillis })", "A DateTime.", ("epochSeconds", "A numeric value representing the number of seconds from the UNIX epoch in the UTC time zone."), ("epochMillis", "A numeric value representing the number of milliseconds from the UNIX epoch in the UTC time zone."))
      considerations("`epochSeconds`/`epochMillis` must denote a valid date and time; i.e. a value for either of these denoting a date of `30 February 2001` is invalid.", "`epochSeconds`/`epochMillis` may be used in conjunction with `nanosecond`")
      query(
        """RETURN datetime({epochSeconds:timestamp() / 1000, nanosecond: 23}) AS theDate""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      query(
        """RETURN datetime({epochMillis: 424797300000}) AS theDate""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localdatetime(): getting the current _LocalDateTime_", "functions-localdatetime-current") {
      p(
        """`localdatetime()` returns the current _LocalDateTime_ value.""".stripMargin)
      function("localdatetime()", "A LocalDateTime.")
      query(
        """RETURN localdatetime() AS now""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDateTime]("now").next()
          now should be(a[LocalDateTime])
        })) {
        p("""The current local date and time (i.e. in the local time zone) is returned.""")
        resultTable()
      }
    }
    section("localdatetime(): creating a calendar (Year-Month-Day) _LocalDateTime_", "functions-localdatetime-calendar") {
      p(
        """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _month_, _day_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
      function("localdatetime({year [, month, day, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("The _month_ component will default to `1` if `month` is omitted.", "The _day of the month_ component will default to `1` if `day` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `month`, `day`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `month` and `day`, but specifying `year`, `month`, `day` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [localdatetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, nanosecond: 789, millisecond: 123, microsecond: 456}),
          |   localdatetime({year:1984, month:10, day:11, hour:12, minute:31})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localdatetime(): creating a week (Year-Week-Day) _LocalDateTime_", "functions-localdatetime-week") {
      p(
        """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _week_, _dayOfWeek_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
      function("localdatetime({year [, week, dayOfWeek, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("The _week_ component will default to `1` if `week` is omitted.", "The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `week`, `dayOfWeek`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `week` and `dayOfWeek`, but specifying `year`, `week`, `dayOfWeek` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localdatetime(): creating a quarter (Year-Quarter-Day) _DateTime_", "functions-localdatetime-quarter") {
      p(
        """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _quarter_, _dayOfQuarter_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
      function("localdatetime({year [, quarter, dayOfQuarter, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("The _quarter_ component will default to `1` if `quarter` is omitted.", "The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `quarter`, `dayOfQuarter`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `quarter` and `dayOfQuarter`, but specifying `year`, `quarter`, `dayOfQuarter` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [localdatetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   localdatetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localdatetime(): creating an ordinal (Year-Day) _LocalDateTime_", "functions-localdatetime-ordinal") {
      p(
        """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _ordinalDay_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
      function("localdatetime({year [, ordinalDay, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `ordinalDay`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year` and `ordinalDay`, but specifying `year`, `ordinalDay` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [localdatetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, microsecond: 645876}),
          |   localdatetime({year:1984, ordinalDay:202, hour:12})] as theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localdatetime(): creating a _LocalDateTime_ from a string", "functions-localdatetime-string") {
      p(
        """`localdatetime()` returns the _LocalDateTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
      function("localdatetime(temporalValue)", "A LocalDateTime.", ("temporalValue", "A string representing a temporal value."))
      considerations("`temporalValue` must comply with XXLINK.", "`localdatetime(null)` returns the current date and time.", "`temporalValue` must denote a valid date and time; i.e. a `temporalValue` denoting `30 February 2001` is invalid.")
      query(
        """UNWIND [localdatetime('2015-07-21T21:40:32.142'),
          |   localdatetime('2015-W30-2T214032.142'),
          |   localdatetime('2015-202T21:40:32'),
          |   localdatetime('2015202T21')] AS theDate
          |RETURN theDate""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localtime(): getting the current _LocalTime_", "functions-localtime-current") {
      p(
        """`localtime()` returns the current _LocalTime_ value.""".stripMargin)
      function("localtime()", "A LocalTime.")
      query(
        """RETURN localtime() AS now""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalTime]("now").next()
          now should be(a[LocalTime])
        })) {
        p("""The current local time (i.e. in the local time zone) is returned.""")
        resultTable()
      }
    }
    section("localtime(): creating a _LocalTime_", "functions-localtime-create") {
      p(
        """`localtime()` returns a _LocalDateTime_ value with the specified _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
      function("localtime({hour [, minute, second, millisecond, microsecond, nanosecond]})", "A LocalTime.", ("A single map consisting of the following:", ""), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `hour` and `minute`, but specifying `hour` and `second` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 789, millisecond: 123, microsecond: 456}),
          |   localtime({hour:12, minute:31, second:14}),
          |   localtime({hour:12})] as theTime
          |RETURN theTime""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localtime(): creating a _LocalTime_ from a string", "functions-localtime-string") {
      p(
        """`localtime()` returns the _LocalTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
      function("localtime(temporalValue)", "A LocalTime.", ("temporalValue", "A string representing a temporal value."))
      considerations("`temporalValue` must comply with XXLINK.", "`localtime(null)` returns the current time.", "`temporalValue` must denote a valid time; i.e. a `temporalValue` denoting `13:46:64` is invalid.")
      query(
        """UNWIND [localtime('21:40:32.142'),
          |   localtime('214032.142'),
          |   localtime('21:40'),
          |   localtime('21')] AS theTime
          |RETURN theTime""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("time(): getting the current _Time_", "functions-time-current") {
      p(
        """`time()` returns the current _Time_ value.
          |If no time zone parameter is specified, the local time zone will be used.""".stripMargin)
      function("time([ {timezone} ])", "A Time.", ("A single map consisting of the following:", ""), ("timezone", "An expression that represents the time zone"))
      considerations("If no parameters are provided, `time()` should be invoked (`time({})` is invalid).")
      query(
        """RETURN time() AS currentTime""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[OffsetTime]("currentTime").next()
          now should be(a[OffsetTime])
        })) {
        p("""The current time of day using the local time zone is returned.""")
        resultTable()
      }
      query(
        """RETURN time( {timezone: "America/Los Angeles"} ) AS currentTimeInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[OffsetTime]("currentTimeInLA").next()
          now should be(a[OffsetTime])
        })) {
        p("""The current time of day in California is returned.""")
        resultTable()
      }
    }
    section("time(): creating a _Time_", "functions-time-create") {
      p(
        """`time()` returns a _Time_ value with the specified _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
      function("time({hour [, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A Time.", ("A single map consisting of the following:", ""), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "The _timezone_ component will default to the `UTC` time zone if `timezone` is omitted.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `hour` and `minute`, but specifying `hour` and `second` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
      query(
        """UNWIND [time({hour:12, minute:31, second:14, nanosecond: 789, millisecond: 123, microsecond: 456}),
          |   time({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   time({hour:12, minute:31, timezone: '+01:00'}),
          |   time({hour:12, timezone: '+01:00'})] AS theTime
          |RETURN theTime""".stripMargin, ResultAssertions((r) => {
          // CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("time(): creating a _Time_ from a string", "functions-time-string") {
      p(
        """`time()` returns the _Time_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
      function("time(temporalValue)", "A Time.", ("temporalValue", "A string representing a temporal value."))
      considerations("`temporalValue` must comply with XXLINK.", "`time(null)` returns the current time.", "`temporalValue` must denote a valid time; i.e. a `temporalValue` denoting `15:67` is invalid.")
      query(
        """UNWIND [time('21:40:32.142+0100'),
          |   time('214032.142Z'),
          |   time('21:40:32+01:00'),
          |   time('214032-0100'),
          |   time('21:40-01:30'),
          |   time('2140-00:00'),
          |   time('2140-02'),
          |   time('22+18:00')] AS theTime
          |RETURN theTime""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
  }.build()
}


//  for (s <- Seq("date", "localtime", "time", "localdatetime", "datetime")) {
//  shouldReturnSomething(s"$s.transaction()")
//  shouldReturnSomething(s"$s.statement()")
//  shouldReturnSomething(s"$s.realtime()")
//  shouldReturnSomething(s"$s.transaction('America/Los_Angeles')")
//  shouldReturnSomething(s"$s.statement('America/Los_Angeles')")
//  shouldReturnSomething(s"$s.realtime('America/Los_Angeles')")
//  shouldReturnSomething(s"$s({timezone: '+01:00'})")

//}

//  test("should get current 'realtime' datetime") {
//    val result = executeWith(supported, "RETURN datetime.realtime() as now")
//
//    val now = single(result.columnAs[ZonedDateTime]("now"))
//
//    now shouldBe a[ZonedDateTime]
//  }
