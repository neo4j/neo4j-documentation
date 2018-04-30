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
    note {
      p("""See also <<cypher-temporal>> and <<query-operators-temporal>>.""")
    }
    p(
      """
        |* <<functions-temporal-instant-type, Temporal instant types (_Date_, _Time_, _LocalTime_, _DateTime_ and _LocalDateTime_)>>
        |* <<functions-duration, _Duration_>>
        | ** <<functions-duration-create-components, Creating a _Duration_ from duration components>>
        | ** <<functions-duration-create-string, Creating a _Duration_ from a string>>
        | ** <<functions-duration-computing, Computing the _Duration_ between two temporal instants>>
        |  *** <<functions-duration-between, duration.between()>>
        |  *** <<functions-duration-inmonths, duration.inMonths()>>
        |  *** <<functions-duration-indays, duration.inDays()>>
        |  *** <<functions-duration-inseconds, duration.inSeconds()>>
      """.stripMargin)
    section("Temporal instant types (_Date_, _Time_, _LocalTime_, _DateTime_ and _LocalDateTime_)", "functions-temporal-instant-type") {
      p(
        """Each function bears the same name as the type, and construct the type they correspond to in one of four ways:
          |
          |* Capturing the current time
          |* Composing the components of the type
          |* Parsing a string representation of the temporal value
          |* Selecting and composing components from another temporal value by
          | ** either combining temporal values (such as combining a _Date_ with a _Time_ to create a _DateTime_), or
          | ** selecting parts from a temporal value (such as selecting the _Date_ from a _DateTime_); the _extractors_ -- groups of components which can be selected -- are:
          |  *** `date` -- contains all components for a _Date_ (conceptually _year_, _month_ and _day_).
          |  *** `time` -- contains all components for a _Time_ (_hour_, _minute_, _second_, and sub-seconds; namely _millisecond_, _microsecond_ and _nanosecond_). footnoteref:[TimezoneInfo,If the type being created xxxlink and the type from which the time component being selected from both contain `timezone` (and a `timezone` is not explicitly specified) the `timezone` is also selected.]
          |  *** `datetime` -- selects all components, and is useful for overriding specific components. footnoteref:[TimezoneInfo]
          | ** In effect, this allows for the _conversion_ between different temporal types, and allowing for 'missing' components to be specified.""".stripMargin)
      p(
        """
          |.Temporal instant type creation functions
          |[options="header"]
          ||===
          || Function                   | Date | Time | LocalTime | DateTime | LocalDateTime
          || Getting the current value  | <<functions-date-current, X>> | <<functions-time-current, X>> | <<functions-localtime-current, X>> | <<functions-datetime-current, X>> | <<functions-localdatetime-current, X>>
          || Creating a calendar (Year-Month-Day) value | <<functions-date-calendar, X>> | | | <<functions-datetime-calendar, X>> | <<functions-localdatetime-calendar, X>>
          || Creating a week (Year-Week-Day) value | <<functions-date-week, X>> | | | <<functions-datetime-week, X>> | <<functions-localdatetime-week, X>>
          || Creating a quarter (Year-Quarter-Day) value | <<functions-date-quarter, X>> | | | <<functions-datetime-quarter, X>> | <<functions-localdatetime-quarter, X>>
          || Creating an ordinal (Year-Day) value | <<functions-date-ordinal, X>> | | | <<functions-datetime-ordinal, X>> | <<functions-localdatetime-ordinal, X>>
          || Creating a value from time components |  | <<functions-time-create, X>> | <<functions-localtime-create, X>> | |
          || Creating a value from other temporal values using extractors (i.e. converting between different types) | <<functions-date-temporal, X>> | <<functions-time-temporal, X>> | <<functions-localtime-temporal, X>> | <<functions-datetime-temporal, X>> | <<functions-localdatetime-temporal, X>>
          || Creating a value from a string | <<functions-date-create-string, X>> | <<functions-time-create-string, X>> | <<functions-localtime-create-string, X>> | <<functions-datetime-create-string, X>> | <<functions-localdatetime-create-string, X>>
          || Creating a value from a timestamp | | | | <<functions-datetime-timestamp, X>> |
          ||===
          |
          |""")
      note {
        p(
          """All the temporal instant types -- including those that do not contain time zone information support such as _Date_, _LocalTime_ and _DateTime_ -- allow for a time zone to specified for the functions that retrieve the current instant.
     This allows for the retrieval of the current instant in the specified time zone.
          |""")
      }
      p(
        """The functions which create temporal instant values based on the current instant use the _default_ clock as standard.
          |**CYPHER_TODO: what is the default clock???**
          |However, there are three different clocks available for more fine-grained control:
          |
          |* `transaction`: The same instant is produced for each invocation within the same transaction.
          |A different time may be produced for different transactions.
          |* `statement`: The same instant is produced for each invocation within the same statement.
          |A different time may be produced for different statements within the same transaction.
          |* `realtime`: The instant produced will be the live clock of the system.
          |
      """.stripMargin)
      p(
        """
          |The following table lists the different sub-functions for specifying the clock to be used when creating the current temporal instant value:
          |
          |[options="header"]
          ||===
          || Type                   | default | transaction | statement | realtime
          || Date  | <<functions-date-current, date()>> | <<functions-date-current-transaction, date.transaction()>>  | <<functions-date-current-statement, date.statement()>> | <<functions-date-current-realtime, date.realtime()>>
          || Time | <<functions-time-current, time()>> | <<functions-time-current-transaction, time.transaction()>> | <<functions-time-current-statement, time.statement()>> | <<functions-time-current-realtime, time.realtime()>>
          || LocalTime | <<functions-localtime-current, localtime()>> | <<functions-localtime-current-transaction, localtime.transaction()>> | <<functions-localtime-current-statement, localtime.statement()>> | <<functions-localtime-current-realtime, localtime.realtime()>>
          || DateTime | <<functions-datetime-current, datetime()>> | <<functions-datetime-current-transaction, datetime.transaction()>> | <<functions-datetime-current-statement, datetime.statement()>> | <<functions-datetime-current-realtime, datetime.realtime()>>
          || LocalDateTime | <<functions-localdatetime-current, localdatetime()>> | <<functions-localdatetime-current-transaction, localdatetime.transaction()>> | <<functions-localdatetime-current-statement, localdatetime.statement()>> | <<functions-localdatetime-current-realtime, localdatetime.realtime()>>
          ||===
          |
          |""")
    }
    section("_Duration_", "functions-duration") {
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
        query(
          """UNWIND [duration({days: 14, hours:16, minutes: 12}),
            |   duration({months: 5, days: 1.5}),
            |   duration({months: 0.75}),
            |   duration({weeks: 2.5}),
            |   duration({years: 12, months:5, days: 14, hours:16, minutes: 12, seconds: 70}),
            |   duration({days: 14, seconds: 70, milliseconds: 1}),
            |   duration({minutes: 1.5, seconds: 1})] AS d
            |RETURN d""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("Creating a _Duration_ from a string", "functions-duration-create-string") {
        p(
          """`duration()` returns the _Duration_ value obtained by parsing a string representation of a temporal amount.""".stripMargin)
        function("duration(temporalAmount)", "A Duration.", ("temporalAmount", "A string representing a temporal amount."))
        considerations("`temporalAmount` must comply with XXLINK.")
        query(
          """UNWIND [duration("P14DT16H12M"),
            |   duration("P5M1.5D"),
            |   duration("P0.75M"),
            |   duration("PT0.75M"),
            |   duration("P2.5W"),
            |   duration("P12Y5M14DT16H12M70S"),
            |   duration("P2012-02-02T14:37:21.545")] AS d
            |RETURN d""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("Computing the _Duration_ between two temporal instants", "functions-duration-computing") {
        p(
          """`duration()` has sub-functions which compute the _logical difference_ (in days, months, etc) between two temporal instant values:
            |
            |* `duration.between(a, b)`: Computes the difference in multiple components between instant `a` and instant `b`.
            |* `duration.inMonths(a, b)`: Computes the difference in whole months (or quarters or years) between instant `a` and instant `b`.
            |* `duration.inDays(a, b)`: Computes the difference in whole days (or weeks) between instant `a` and instant `b`.
            |* `duration.inSeconds(a, b)`: Computes the difference in seconds (and fractions of seconds, or minutes or hours) between instant `a` and instant `b`.
            |""".stripMargin)
        section("duration.between()", "functions-duration-between") {
          p(
            """`duration.between()` returns the _Duration_ value equal to the difference between the two given instants.""".stripMargin)
          function("duration.between(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
          considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.")
          query(
            """UNWIND [duration.between(date("1984-10-11"), date("2015-06-24")),
              |   duration.between(date("2015-06-24"), date("1984-10-11")),
              |   duration.between(date("1984-10-11"), datetime("2015-07-21T21:40:32.142+0100")),
              |   duration.between(localtime("14:30"), date("2015-06-24")),
              |   duration.between(time("14:30"), time("16:30+0100")),
              |   duration.between(localdatetime("2015-07-21T21:40:32.142"), localdatetime("2016-07-21T21:45:22.142")),
              |   duration.between(datetime({year: 2017, month: 10, day: 29, hour: 0, timezone: 'Europe/Stockholm'}), localdatetime({year: 2017, month: 10, day: 29, hour: 4}))] AS d
              |RETURN d""".stripMargin, ResultAssertions((r) => {
              //CYPHER_TODO
            })) {
            resultTable()
          }
        }
        section("duration.inMonths()", "functions-duration-inmonths") {
          p(
            """`duration.inMonths()` returns the _Duration_ value equal to the difference in whole months, quarters or years between the two given instants.""".stripMargin)
          function("duration.inMonths(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
          considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.")
          query(
            """UNWIND [duration.inMonths(date("1984-10-11"), date("2015-06-24")),
              |   duration.inMonths(date("2015-06-24"), date("1984-10-11")),
              |   duration.inMonths(date("1984-10-11"), localdatetime("2016-07-21T21:45:22.142")),
              |   duration.inMonths(date("1984-10-11"), datetime("2015-07-21T21:40:32.142+0100")),
              |   duration.inMonths(time("14:30"), date("2015-06-24")),
              |   duration.inMonths(datetime("2014-07-21T21:40:36.143+0200"), datetime("2015-07-21T21:40:32.142+0100"))] AS d
              |RETURN d""".stripMargin, ResultAssertions((r) => {
              //CYPHER_TODO
            })) {
            resultTable()
          }
        }
        section("duration.inDays()", "functions-duration-indays") {
          p(
            """`duration.inDays()` returns the _Duration_ value equal to the difference in whole days or weeks between the two given instants.""".stripMargin)
          function("duration.inDays(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
          considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.")
          query(
            """UNWIND [duration.inDays(date("1984-10-11"), date("2015-06-24")),
              |   duration.inDays(date("1984-10-11"), date("2015-06-24")),
              |   duration.inDays(date("1984-10-11"), time("16:30+0100")),
              |   duration.inDays(datetime("2014-07-21T21:40:36.143+0200"), date("2015-06-24")),
              |   duration.inDays(datetime("2014-07-21T21:40:36.143+0200"), localdatetime("2016-07-21T21:45:22.142"))] AS d
              |RETURN d""".stripMargin, ResultAssertions((r) => {
              //CYPHER_TODO
            })) {
            resultTable()
          }
        }
        section("duration.inSeconds()", "functions-duration-inseconds") {
          p(
            """`duration.inSeconds()` returns the _Duration_ value equal to the difference in seconds and fractions of seconds, or minutes or hours, between the two given instants.""".stripMargin)
          function("duration.inSeconds(instant~1~, instant~2~)", "A Duration.", ("instant~1~", "An expression returning any temporal instant type (_Date_ etc) that represents the starting instant."), ("instant~2~", "An expression returning any temporal instant type (_Date_ etc) that represents the ending instant."))
          considerations("If `instant~2~` occurs earlier than `instant~1~`, the resulting _Duration_ will be negative.")
          query(
            """UNWIND [duration.inSeconds(date("1984-10-11"), date("2015-06-24")),
              |   duration.inSeconds(localtime("14:30"), localtime("16:30")),
              |   duration.inSeconds(time("14:30"), date("2015-06-24")),
              |   duration.inSeconds(datetime("2014-07-21T21:40:36.143+0200"), datetime("2015-07-21T21:40:32.142+0100")),
              |   duration.inSeconds(datetime("2015-07-21T21:40:32.142+0100"), datetime("2014-07-21T21:40:36.143+0200"))] AS d
              |RETURN d""".stripMargin, ResultAssertions((r) => {
              //CYPHER_TODO
            })) {
            resultTable()
          }
        }
      }
    }
    section("date(): getting the current _Date_", "functions-date-current") {
      p(
        """`date()` returns the current _Date_ value.
          |If no time zone parameter is specified, the local time zone will be used.
        """.stripMargin)
      function("date([ {timezone} ])", "A Date.", ("A single map consisting of the following:", ""), ("timezone", "An expression that represents the time zone"))
      considerations("If no parameters are provided, `date()` must be invoked (`date({})` is invalid).")
      query(
        """RETURN date() AS currentDate""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDate]("currentDate").next()
          now should be(a[LocalDate])
        })) {
        p("""The current date is returned.""")
        resultTable()
      }
      query(
        """RETURN date( {timezone: 'America/Los Angeles'} ) AS currentDateInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDate]("currentDateInLA").next()
          now should be(a[LocalDate])
        })) {
        p("""The current date in California is returned.""")
        resultTable()
      }
      section("date.transaction()", "functions-date-current-transaction") {
        p(
          """`date.transaction()` returns the current _Date_ value using the `transaction` clock.
            |This value will be the same for each invocation within the same transaction.
            |However, a different value may be produced for different transactions.
          """.stripMargin)
        function("date.transaction([ timezone ])", "A Date.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN date.transaction() AS currentDate""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("date.statement()", "functions-date-current-statement") {
        p(
          """`date.statement()` returns the current _Date_ value using the `statement` clock.
            |This value will be the same for each invocation within the same statement.
            |However, a different value may be produced for different statements within the same transaction.
          """.stripMargin)
        function("date.statement([ timezone ])", "A Date.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN date.statement() AS currentDate""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("date.realtime()", "functions-date-current-realtime") {
        p(
          """`date.realtime()` returns the current _Date_ value using the `realtime` clock.
            |This value will be the live clock of the system.
          """.stripMargin)
        function("date.realtime([ timezone ])", "A Date.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN date.realtime() AS currentDate""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
        query(
          """RETURN date.realtime('America/Los Angeles') AS currentDateInLA""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
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
    section("date(): creating a _Date_ from a string", "functions-date-create-string") {
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
    section("date(): creating a _Date_ using other temporal values as components", "functions-date-temporal") {
      p(
        """`date()` returns the _Date_ value obtained by selecting and composing components from another temporal value.
          |In essence, this allows a _DateTime_ or _LocalDateTime_ value to be converted to a _Date_, and for "missing" components to be provided.
        """.stripMargin)
      function("date({date [, year, month, day, week, dayOfWeek, quarter, dayOfQuarter, ordinalDay]})", "A Date.", ("date", "A _Date_ value."), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."))
      considerations("If any of the optional parameters are provided, these will override the corresponding components of `date`.")
      query(
        """UNWIND [date({year:1984, month:11, day:11}),
          |   localdatetime({year:1984, month:11, day:11, hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   datetime({year:1984, month:11, day:11, hour:12, timezone: '+01:00'})] as dd
          |RETURN date(dd) AS d1,
          |   date({date: dd}) AS d2,
          |   date({date: dd, year: 28}) AS d3,
          |   date({date: dd, day: 28}) AS d4,
          |   date({date: dd, week: 1}) AS d5,
          |   date({date: dd, ordinalDay: 28}) AS d6,
          |   date({date: dd, quarter: 3}) AS d7""".stripMargin, ResultAssertions((r) => {
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
      considerations("If no parameters are provided, `datetime()` must be invoked (`datetime({})` is invalid).")
      query(
        """RETURN datetime() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[ZonedDateTime]("currentDateTime").next()
          now should be(a[ZonedDateTime])
        })) {
        p("""The current date and time using the local time zone is returned.""")
        resultTable()
      }
      query(
        """RETURN datetime( {timezone: 'America/Los Angeles'} ) AS currentDateTimeInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[ZonedDateTime]("currentDateTimeInLA").next()
          now should be(a[ZonedDateTime])
        })) {
        p("""The current date and time of day in California is returned.""")
        resultTable()
      }
      section("datetime.transaction()", "functions-datetime-current-transaction") {
        p(
          """`datetime.transaction()` returns the current _DateTime_ value using the `transaction` clock.
            |This value will be the same for each invocation within the same transaction.
            |However, a different value may be produced for different transactions.
          """.stripMargin)
        function("datetime.transaction([ timezone ])", "A DateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN datetime.transaction() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
        query(
          """RETURN datetime.transaction('America/Los Angeles') AS currentDateTimeInLA""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("datetime.statement()", "functions-datetime-current-statement") {
        p(
          """`datetime.statement()` returns the current _DateTime_ value using the `statement` clock.
            |This value will be the same for each invocation within the same statement.
            |However, a different value may be produced for different statements within the same transaction.
          """.stripMargin)
        function("datetime.statement([ timezone ])", "A DateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN datetime.statement() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("datetime.realtime()", "functions-datetime-current-realtime") {
        p(
          """`datetime.realtime()` returns the current _DateTime_ value using the `realtime` clock.
            |This value will be the live clock of the system.
          """.stripMargin)
        function("datetime.realtime([ timezone ])", "A DateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN datetime.realtime() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
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
    section("datetime(): creating a _DateTime_ from a string", "functions-datetime-create-string") {
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
    section("datetime(): creating a _DateTime_ using other temporal values as components", "functions-datetime-temporal") {
      p(
        """`datetime()` returns the _DateTime_ value obtained by selecting and composing components from another temporal value.
          |In essence, this allows a _Date_, _LocalDateTime_, _Time_ or _LocalTime_ value to be converted to a _DateTime_, and for "missing" components to be provided.
        """.stripMargin)
      function("datetime({datetime [, year, ..., timezone]}) | datetime({date [, year, ..., timezone]}) | datetime({time [, year, ..., timezone]}) | datetime({date, time [, year, ..., timezone]})", "A DateTime.", ("datetime", "A _DateTime_ value."), ("date", "A _Date_ value."), ("time", "A _Time_ value."), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("If any of the optional parameters are provided, these will override the corresponding components of `datetime`, `date` and/or `time`.")
      p("""The following query shows the various usages of `datetime({date [, year, ..., timezone]})`""")
      query(
        """UNWIND [date({year:1984, month:10, day:11}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS dd
          |RETURN datetime({date:dd, hour: 10, minute: 10, second: 10}) AS d1,
          |   datetime({date:dd, hour: 10, minute: 10, second: 10, timezone:'+05:00'}) AS d2,
          |   datetime({date:dd, day: 28, hour: 10, minute: 10, second: 10}) AS d3,
          |   datetime({date:dd, day: 28, hour: 10, minute: 10, second: 10, timezone:'Pacific/Honolulu'}) AS d4""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `datetime({time [, year, ..., timezone]})`""")
      query(
        """UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: 'Europe/Stockholm'})] AS tt
          |RETURN datetime({year:1984, month:10, day:11, time:tt}) AS d1,
          |   datetime({year:1984, month:10, day:11, time:tt, timezone:'+05:00'}) AS d2,
          |   datetime({year:1984, month:10, day:11, time:tt, second: 42}) AS d3,
          |   datetime({year:1984, month:10, day:11, time:tt, second: 42, timezone:'Pacific/Honolulu'}) AS d4""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `datetime({date, time [, year, ..., timezone]})`; i.e. combining a _Date_ and a _Time_ value to create a single _DateTime_ value:""")
      query(
        """UNWIND [date({year:1984, month:10, day:11}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS dd
          |UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'})] AS tt
          |RETURN datetime({date:dd, time:tt}) as d1,
          |   datetime({date:dd, time:tt, timezone:'+05:00'}) AS d2,
          |   datetime({date:dd, time:tt, day: 28, second: 42}) AS d3,
          |   datetime({date:dd, time:tt, day: 28, second: 42, timezone:'Pacific/Honolulu'}) AS d4""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `datetime({datetime [, year, ..., timezone]})`""")
      query(
        """UNWIND [localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: 'Europe/Stockholm'})] AS dd
          |RETURN datetime(dd) AS d1,
          |   datetime({datetime:dd}) AS d2,
          |   datetime({datetime:dd, timezone:'+05:00'}) AS d3,
          |   datetime({datetime:dd, day: 28, second: 42}) AS d4,
          |   datetime({datetime:dd, day: 28, second: 42, timezone:'Pacific/Honolulu'}) AS d5""".stripMargin, ResultAssertions((r) => {
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
        """`localdatetime()` returns the current _LocalDateTime_ value.
          |If no time zone parameter is specified, the local time zone will be used.
        """.stripMargin)
      function("localdatetime([ {timezone} ])", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("timezone", "An expression that represents the time zone"))
      considerations("If no parameters are provided, `localdatetime()` must be invoked (`localdatetime({})` is invalid).")
      query(
        """RETURN localdatetime() AS now""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDateTime]("now").next()
          now should be(a[LocalDateTime])
        })) {
        p("""The current local date and time (i.e. in the local time zone) is returned.""")
        resultTable()
      }
      query(
        """RETURN localdatetime({timezone: 'America/Los Angeles'}) AS now""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalDateTime]("now").next()
          now should be(a[LocalDateTime])
        })) {
        p("""The current local date and time in California is returned.""")
        resultTable()
      }
      section("localdatetime.transaction()", "functions-localdatetime-current-transaction") {
        p(
          """`localdatetime.transaction()` returns the current _LocalDateTime_ value using the `transaction` clock.
            |This value will be the same for each invocation within the same transaction.
            |However, a different value may be produced for different transactions.
          """.stripMargin)
        function("localdatetime.transaction([ timezone ])", "A LocalDateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localdatetime.transaction() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("localdatetime.statement()", "functions-localdatetime-current-statement") {
        p(
          """`localdatetime.statement()` returns the current _LocalDateTime_ value using the `statement` clock.
            |This value will be the same for each invocation within the same statement.
            |However, a different value may be produced for different statements within the same transaction.
          """.stripMargin)
        function("localdatetime.statement([ timezone ])", "A LocalDateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localdatetime.statement() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("localdatetime.realtime()", "functions-localdatetime-current-realtime") {
        p(
          """`localdatetime.realtime()` returns the current _LocalDateTime_ value using the `realtime` clock.
            |This value will be the live clock of the system.
          """.stripMargin)
        function("localdatetime.realtime([ timezone ])", "A LocalDateTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localdatetime.realtime() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
        query(
          """RETURN localdatetime.realtime('America/Los Angeles') AS nowInLA""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
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
    section("localdatetime(): creating a _LocalDateTime_ from a string", "functions-localdatetime-create-string") {
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
    section("localdatetime(): creating a _LocalDateTime_ using other temporal values as components", "functions-localdatetime-temporal") {
      p(
        """`localdatetime()` returns the _LocalDateTime_ value obtained by selecting and composing components from another temporal value.
          |In essence, this allows a _Date_, _DateTime_, _Time_ or _LocalTime_ value to be converted to a _LocalDateTime_, and for "missing" components to be provided.
        """.stripMargin)
      function("localdatetime({datetime [, year, ..., nanosecond]}) | localdatetime({date [, year, ..., nanosecond]}) | localdatetime({time [, year, ..., nanosecond]}) | localdatetime({date, time [, year, ..., nanosecond]})", "A LocalDateTime.", ("datetime", "A _DateTime_ value."), ("date", "A _Date_ value."), ("time", "A _Time_ value."), ("year", "An expression consisting of at least four digits that specifies the year TODOLINK."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("If any of the optional parameters are provided, these will override the corresponding components of `datetime`, `date` and/or `time`.")
      p("""The following query shows the various usages of `localdatetime({date [, year, ..., nanosecond]})`""")
      query(
        """UNWIND [date({year:1984, month:10, day:11}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS dd
          |RETURN localdatetime({date:dd, hour: 10, minute: 10, second: 10}) AS d1,
          |   localdatetime({date:dd, day: 28, hour: 10, minute: 10, second: 10}) AS d2""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `localdatetime({time [, year, ..., nanosecond]})`""")
      query(
        """UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS tt
          |RETURN localdatetime({year:1984, month:10, day:11, time:tt}) AS d1,
          |   localdatetime({year:1984, month:10, day:11, time:tt, second: 42}) AS d2""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `localdatetime({date, time [, year, ..., nanosecond]})`; i.e. combining a _Date_ and a _Time_ value to create a single _LocalDateTime_ value:""")
      query(
        """UNWIND [date({year:1984, month:10, day:11}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS dd
          |UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'})] AS tt
          |RETURN localdatetime({date:dd, time:tt}) AS d1,
          |   localdatetime({date:dd, time:tt, day: 28, second: 42}) AS d2""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
      p("""The following query shows the various usages of `localdatetime({datetime [, year, ..., nanosecond]})`""")
      query(
        """UNWIND [localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] as dd
          |RETURN localdatetime(dd) as d1,
          |   localdatetime({datetime:dd}) as d2,
          |   localdatetime({datetime:dd, day: 28, second: 42}) as d3""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
    section("localtime(): getting the current _LocalTime_", "functions-localtime-current") {
      p(
        """`localtime()` returns the current _LocalTime_ value.
          |If no time zone parameter is specified, the local time zone will be used.
        """.stripMargin)
      function("localtime([ {timezone} ])", "A LocalTime.", ("A single map consisting of the following:", ""), ("timezone", "An expression that represents the time zone"))
      considerations("If no parameters are provided, `localtime()` must be invoked (`localtime({})` is invalid).")
      query(
        """RETURN localtime() AS now""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalTime]("now").next()
          now should be(a[LocalTime])
        })) {
        p("""The current local time (i.e. in the local time zone) is returned.""")
        resultTable()
      }
      query(
        """RETURN localtime( {timezone: 'America/Los Angeles'} ) AS nowInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[LocalTime]("nowInLA").next()
          now should be(a[LocalTime])
        })) {
        p("""The current local time in California is returned.""")
        resultTable()
      }
      section("localtime.transaction()", "functions-localtime-current-transaction") {
        p(
          """`localtime.transaction()` returns the current _LocalTime_ value using the `transaction` clock.
            |This value will be the same for each invocation within the same transaction.
            |However, a different value may be produced for different transactions.
          """.stripMargin)
        function("localtime.transaction([ timezone ])", "A LocalTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localtime.transaction() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("localtime.statement()", "functions-localtime-current-statement") {
        p(
          """`localtime.statement()` returns the current _LocalTime_ value using the `statement` clock.
            |This value will be the same for each invocation within the same statement.
            |However, a different value may be produced for different statements within the same transaction.
          """.stripMargin)
        function("localtime.statement([ timezone ])", "A LocalTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localtime.statement() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
        query(
          """RETURN localtime.statement('America/Los Angeles') AS nowInLA""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("localtime.realtime()", "functions-localtime-current-realtime") {
        p(
          """`localtime.realtime([ timezone ])` returns the current _LocalTime_ value using the `realtime` clock.
            |This value will be the live clock of the system.
          """.stripMargin)
        function("localtime.realtime()", "A LocalTime.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN localtime.realtime() AS now""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
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
    section("localtime(): creating a _LocalTime_ from a string", "functions-localtime-create-string") {
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
    section("localtime(): creating a _LocalTime_ using other temporal values as components", "functions-localtime-temporal") {
      p(
        """`localtime()` returns the _LocalTime_ value obtained by selecting and composing components from another temporal value.
          |In essence, this allows a _DateTime_, _LocalDateTime_ or _Time_ value to be converted to a _LocalTime_, and for "missing" components to be provided.
        """.stripMargin)
      function("localtime({time [, hour, ..., nanosecond]})", "A LocalTime.", ("time", "A _Time_ value."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
      considerations("If any of the optional parameters are provided, these will override the corresponding components of `time`.")
      query(
        """UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'})] AS dd
          |RETURN localtime(dd) AS d1,
          |   localtime({time:dd}) AS d2,
          |   localtime({time:dd, second: 42}) AS d3""".stripMargin, ResultAssertions((r) => {
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
      considerations("If no parameters are provided, `time()` must be invoked (`time({})` is invalid).")
      query(
        """RETURN time() AS currentTime""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[OffsetTime]("currentTime").next()
          now should be(a[OffsetTime])
        })) {
        p("""The current time of day using the local time zone is returned.""")
        resultTable()
      }
      query(
        """RETURN time( {timezone: 'America/Los Angeles'} ) AS currentTimeInLA""".stripMargin, ResultAssertions((r) => {
          val now = r.columnAs[OffsetTime]("currentTimeInLA").next()
          now should be(a[OffsetTime])
        })) {
        p("""The current time of day in California is returned.""")
        resultTable()
      }
      section("time.transaction(): getting the current _Time_ using the `transaction` clock", "functions-time-current-transaction") {
        p(
          """`time.transaction()` returns the current _Time_ value.
            |This value will be the same for each invocation within the same transaction.
            |However, a different value may be produced for different transactions.
          """.stripMargin)
        function("time.transaction([ timezone ])", "A Time.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN time.transaction() AS currentTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("time.statement(): getting the current _Time_ using the `statement` clock", "functions-time-current-statement") {
        p(
          """`time.statement()` returns the current _Time_ value.
            |This value will be the same for each invocation within the same statement.
            |However, a different value may be produced for different statements within the same transaction.
          """.stripMargin)
        function("time.statement([ timezone ])", "A Time.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN time.statement() AS currentTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
        query(
          """RETURN time.statement('America/Los Angeles') AS currentTimeInLA""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
      }
      section("time.realtime(): getting the current _Time_ using the `realtime` clock", "functions-time-current-realtime") {
        p(
          """`time.realtime()` returns the current _Time_ value.
            |This value will be the live clock of the system.
          """.stripMargin)
        function("time.realtime([ timezone ])", "A Time.", ("timezone", "An expression that represents the time zone"))
        query(
          """RETURN time.realtime() AS currentTime""".stripMargin, ResultAssertions((r) => {
            //CYPHER_TODO
          })) {
          resultTable()
        }
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
    section("time(): creating a _Time_ from a string", "functions-time-create-string") {
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
    section("time(): creating a _Time_ using other temporal values as components", "functions-time-temporal") {
      p(
        """`time()` returns the _Time_ value obtained by selecting and composing components from another temporal value.
          |In essence, this allows a _DateTime_, _LocalDateTime_ or _LocalTime_ value to be converted to a _Time_, and for "missing" components to be provided.
        """.stripMargin)
      function("time({time [, hour, ..., timezone]})", "A Time.", ("time", "A _Time_ value."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
      considerations("If any of the optional parameters are provided, these will override the corresponding components of `time`.")
      query(
        """UNWIND [localtime({hour:12, minute:31, second:14, nanosecond: 645876123}),
          |   time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
          |   localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
          |   datetime({year:1984, month:10, day:11, hour:12, timezone: 'Europe/Stockholm'})] AS dd
          |RETURN time(dd) AS d1,
          |   time({time:dd}) AS d2,
          |   time({time:dd, timezone:'+05:00'}) AS d3,
          |   time({time:dd, second: 42}) AS d4,
          |   time({time:dd, second: 42, timezone:'+05:00'}) AS d5""".stripMargin, ResultAssertions((r) => {
          //CYPHER_TODO
        })) {
        resultTable()
      }
    }
  }.build()
}
