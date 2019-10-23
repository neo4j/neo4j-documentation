/*
 * Copyright (c) 2002-2019 "Neo4j,"
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

class TemporalFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    val defaultZoneSupplier: Supplier[ZoneId] = new Supplier[ZoneId] {
      override def get(): ZoneId = ZoneOffset.UTC
    }

    doc("Temporal functions - instant types", "query-functions-temporal")
    synopsis(
      """Cypher provides functions allowing for the creation and manipulation of values for each temporal type -- _Date_, _Time_, _LocalTime_, _DateTime_, and _LocalDateTime_.""".stripMargin)
    note {
      p("""See also <<cypher-temporal>> and <<query-operators-temporal>>.""")
    }
    p(
      """
        |<<functions-temporal-instant-type, Temporal instant types>>
        |
        |* <<functions-temporal-create-overview, An overview of temporal instant type creation>>
        |* <<functions-temporal-clock-overview, Controlling which clock to use>>
        |* <<functions-temporal-truncate-overview, Truncating temporal values>>
      """.stripMargin)
      p(
        """
        |Functions:
        |
        |* <<functions-date, date()>>
        |** <<functions-date-current, Getting the current _Date_>>
        |** <<functions-date-calendar, Creating a calendar (Year-Month-Day) _Date_>>
        |** <<functions-date-week, Creating a week (Year-Week-Day) _Date_>>
        |** <<functions-date-quarter, Creating a quarter (Year-Quarter-Day) _Date_>>
        |** <<functions-date-ordinal, Creating an ordinal (Year-Day) _Date_>>
        |** <<functions-date-create-string, Creating a _Date_ from a string>>
        |** <<functions-date-temporal, Creating a _Date_ using other temporal values as components>>
        |** <<functions-date-truncate, Truncating a _Date_>>
        |* <<functions-datetime, datetime()>>
        |** <<functions-datetime-current, Getting the current _DateTime_>>
        |** <<functions-datetime-calendar, Creating a calendar (Year-Month-Day) _DateTime_>>
        |** <<functions-datetime-week, Creating a week (Year-Week-Day) _DateTime_>>
        |** <<functions-datetime-quarter, Creating a quarter (Year-Quarter-Day) _DateTime_>>
        |** <<functions-datetime-ordinal, Creating an ordinal (Year-Day) _DateTime_>>
        |** <<functions-datetime-create-string, Creating a _DateTime_ from a string>>
        |** <<functions-datetime-temporal, Creating a _DateTime_ using other temporal values as components>>
        |** <<functions-datetime-timestamp, Creating a _DateTime_ from a timestamp>>
        |** <<functions-datetime-truncate, Truncating a _DateTime_>>
        |* <<functions-localdatetime, localdatetime()>>
        |** <<functions-localdatetime-current, Getting the current _LocalDateTime_>>
        |** <<functions-localdatetime-calendar, Creating a calendar (Year-Month-Day) _LocalDateTime_>>
        |** <<functions-localdatetime-week, Creating a week (Year-Week-Day) _LocalDateTime_>>
        |** <<functions-localdatetime-quarter, Creating a quarter (Year-Quarter-Day) _DateTime_>>
        |** <<functions-localdatetime-ordinal, Creating an ordinal (Year-Day) _LocalDateTime_>>
        |** <<functions-localdatetime-create-string, Creating a _LocalDateTime_ from a string>>
        |** <<functions-localdatetime-temporal, Creating a _LocalDateTime_ using other temporal values as components>>
        |** <<functions-localdatetime-truncate, Truncating a _LocalDateTime_>>
        |* <<functions-localtime, localtime()>>
        |** <<functions-localtime-current, Getting the current _LocalTime_>>
        |** <<functions-localtime-create, Creating a _LocalTime_>>
        |** <<functions-localtime-create-string, Creating a _LocalTime_ from a string>>
        |** <<functions-localtime-temporal, Creating a _LocalTime_ using other temporal values as components>>
        |** <<functions-localtime-truncate, Truncating a _LocalTime_>>
        |* <<functions-time, time()>>
        |** <<functions-time-current, Getting the current _Time_>>
        |** <<functions-time-create, Creating a _Time_>>
        |** <<functions-time-create-string, Creating a _Time_ from a string>>
        |** <<functions-time-temporal, Creating a _Time_ using other temporal values as components>>
        |** <<functions-time-truncate, Truncating a _Time_>>
      """.stripMargin)
    section("Temporal instant types", "functions-temporal-instant-type") {
      synopsis("""An introduction to temporal instant types, including descriptions of creation functions, clocks, and truncation.""".stripMargin)
      section("An overview of temporal instant type creation", "functions-temporal-create-overview") {
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
            |  *** `time` -- contains all components for a _Time_ (_hour_, _minute_, _second_, and sub-seconds; namely _millisecond_, _microsecond_ and _nanosecond_).
            |  If the type being created and the type from which the time component is being selected both contain `timezone` (and a `timezone` is not explicitly specified) the `timezone` is also selected.
            |  *** `datetime` -- selects all components, and is useful for overriding specific components.
            |  Analogously to `time`, if the type being created and the type from which the time component is being selected both contain `timezone` (and a `timezone` is not explicitly specified) the `timezone` is also selected.
            | ** In effect, this allows for the _conversion_ between different temporal types, and allowing for 'missing' components to be specified.""".stripMargin)
        p(
          """
            |.Temporal instant type creation functions
            |[options="header"]
            ||===
            || Function                   | Date | Time | LocalTime | DateTime | LocalDateTime
            || Getting the current value  | <<functions-date-current, X>> | <<functions-time-current, X>> | <<functions-localtime-current, X>> | <<functions-datetime-current, X>> | <<functions-localdatetime-current, X>>
            || Creating a calendar-based (Year-Month-Day) value | <<functions-date-calendar, X>> | | | <<functions-datetime-calendar, X>> | <<functions-localdatetime-calendar, X>>
            || Creating a week-based (Year-Week-Day) value | <<functions-date-week, X>> | | | <<functions-datetime-week, X>> | <<functions-localdatetime-week, X>>
            || Creating a quarter-based (Year-Quarter-Day) value | <<functions-date-quarter, X>> | | | <<functions-datetime-quarter, X>> | <<functions-localdatetime-quarter, X>>
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
      }
      section("Controlling which clock to use", "functions-temporal-clock-overview") {
        p(
          """The functions which create temporal instant values based on the current instant use the `statement` clock as default.
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
      section("Truncating temporal values", "functions-temporal-truncate-overview") {
        p(
          """A temporal instant value can be created by truncating another temporal instant value at the nearest preceding point in time at a specified component boundary (namely, a _truncation unit_).
            |A temporal instant value created in this way will have all components which are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of overriding the default values which would otherwise have been set for these less significant components.
            |
            |The following truncation units are supported:
            |
            |* `millennium`: Select the temporal instant corresponding to the _millenium_ of the given instant.
            |* `century`: Select the temporal instant corresponding to the _century_ of the given instant.
            |* `decade`: Select the temporal instant corresponding to the _decade_ of the given instant.
            |* `year`: Select the temporal instant corresponding to the _year_ of the given instant.
            |* `weekYear`: Select the temporal instant corresponding to the first day of the first week of the _week-year_ of the given instant.
            |* `quarter`: Select the temporal instant corresponding to the _quarter of the year_ of the given instant.
            |* `month`: Select the temporal instant corresponding to the _month_ of the given instant.
            |* `week`: Select the temporal instant corresponding to the _week_ of the given instant.
            |* `day`: Select the temporal instant corresponding to the _month_ of the given instant.
            |* `hour`: Select the temporal instant corresponding to the _hour_ of the given instant.
            |* `minute`: Select the temporal instant corresponding to the _minute_ of the given instant.
            |* `second`: Select the temporal instant corresponding to the _second_ of the given instant.
            |* `millisecond`: Select the temporal instant corresponding to the _millisecond_ of the given instant.
            |* `microsecond`: Select the temporal instant corresponding to the _microsecond_ of the given instant.
            |
      """.stripMargin)
        p(
          """
            |The following table lists the supported truncation units and the corresponding sub-functions:
            |
            |[options="header"]
            ||===
            || Truncation unit                   | Date | Time | LocalTime | DateTime | LocalDateTime
            || `millennium`  | <<functions-date-truncate, date.truncate('millennium', input)>> | | | <<functions-datetime-truncate, datetime.truncate('millennium', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('millennium', input)>>
            || `century`  | <<functions-date-truncate, date.truncate('century', input)>> | | | <<functions-datetime-truncate, datetime.truncate('century', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('century', input)>>
            || `decade`  | <<functions-date-truncate, date.truncate('decade', input)>> | | | <<functions-datetime-truncate, datetime.truncate('decade', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('decade', input)>>
            || `year`  | <<functions-date-truncate, date.truncate('year', input)>> | | | <<functions-datetime-truncate, datetime.truncate('year', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('year', input)>>
            || `weekYear`  | <<functions-date-truncate, date.truncate('weekYear', input)>> | | | <<functions-datetime-truncate, datetime.truncate('weekYear', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('weekYear', input)>>
            || `quarter`  | <<functions-date-truncate, date.truncate('quarter', input)>> | | | <<functions-datetime-truncate, datetime.truncate('quarter', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('quarter', input)>>
            || `month`  | <<functions-date-truncate, date.truncate('month', input)>> | | | <<functions-datetime-truncate, datetime.truncate('month', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('month', input)>>
            || `week`  | <<functions-date-truncate, date.truncate('week', input)>> | | | <<functions-datetime-truncate, datetime.truncate('week', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('week', input)>>
            || `day`  | <<functions-date-truncate, date.truncate('day', input)>> | <<functions-time-truncate, time.truncate('day', input)>> | <<functions-localtime-truncate, localtime.truncate('day', input)>> | <<functions-datetime-truncate, datetime.truncate('day', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('day', input)>>
            || `hour`  | | <<functions-time-truncate, time.truncate('hour', input)>> | <<functions-localtime-truncate, localtime.truncate('hour', input)>> | <<functions-datetime-truncate, datetime.truncate('hour', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('hour',input)>>
            || `minute`  | | <<functions-time-truncate, time.truncate('minute', input)>> | <<functions-localtime-truncate, localtime.truncate('minute', input)>> | <<functions-datetime-truncate, datetime.truncate('minute', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('minute', input)>>
            || `second`  | | <<functions-time-truncate, time.truncate('second', input)>> | <<functions-localtime-truncate, localtime.truncate('second', input)>> | <<functions-datetime-truncate, datetime.truncate('second', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('second', input)>>
            || `millisecond`  |  | <<functions-time-truncate, time.truncate('millisecond', input)>> | <<functions-localtime-truncate, localtime.truncate('millisecond', input)>> | <<functions-datetime-truncate, datetime.truncate('millisecond', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('millisecond', input)>>
            || `microsecond`  | | <<functions-time-truncate, time.truncate('microsecond', input)>> | <<functions-localtime-truncate, localtime.truncate('microsecond', input)>> | <<functions-datetime-truncate, datetime.truncate('microsecond', input)>> | <<functions-localdatetime-truncate, localdatetime.truncate('microsecond', input)>>
            ||===
            |
            |""")
      }
    }
    section("Date: `date()`", "functions-date") {
      synopsis("""Details for using the `date()` function.""".stripMargin)
      p(
        """
          |* <<functions-date-current, Getting the current _Date_>>
          |* <<functions-date-calendar, Creating a calendar (Year-Month-Day) _Date_>>
          |* <<functions-date-week, Creating a week (Year-Week-Day) _Date_>>
          |* <<functions-date-quarter, Creating a quarter (Year-Quarter-Day) _Date_>>
          |* <<functions-date-ordinal, Creating an ordinal (Year-Day) _Date_>>
          |* <<functions-date-create-string, Creating a _Date_ from a string>>
          |* <<functions-date-temporal, Creating a _Date_ using other temporal values as components>>
          |* <<functions-date-truncate, Truncating a _Date_>>
        """.stripMargin)
      section("Getting the current _Date_", "functions-date-current") {
        p(
          """`date()` returns the current _Date_ value.
            |If no time zone parameter is specified, the local time zone will be used.
          """.stripMargin)
        function("date([ +{timezone}+ ])", "A Date.", ("A single map consisting of the following:", ""), ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
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
          function("date.transaction([ +{timezone}+ ])", "A Date.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN date.transaction() AS currentDate""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDate]("currentDate").next()
              now should be(a[LocalDate])
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
          function("date.statement([ +{timezone}+ ])", "A Date.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN date.statement() AS currentDate""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDate]("currentDate").next()
              now should be(a[LocalDate])
            })) {
            resultTable()
          }
        }
        section("date.realtime()", "functions-date-current-realtime") {
          p(
            """`date.realtime()` returns the current _Date_ value using the `realtime` clock.
              |This value will be the live clock of the system.
            """.stripMargin)
          function("date.realtime([ +{timezone}+ ])", "A Date.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN date.realtime() AS currentDate""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDate]("currentDate").next()
              now should be(a[LocalDate])
            })) {
            resultTable()
          }
          query(
            """RETURN date.realtime('America/Los Angeles') AS currentDateInLA""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDate]("currentDateInLA").next()
              now should be(a[LocalDate])
            })) {
            resultTable()
          }
        }
      }
      section("Creating a calendar (Year-Month-Day) _Date_", "functions-date-calendar") {
        p(
          """`date()` returns a _Date_ value with the specified _year_, _month_ and _day_ component values.""".stripMargin)
        function("date({year [, month, day]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."))
        considerations("The _day of the month_ component will default to `1` if `day` is omitted.", "The _month_ component will default to `1` if `month` is omitted.", "If `month` is omitted, `day` must also be omitted.")
        preformattedQuery(
          """UNWIND [
            |  date({year:1984, month:10, day:11}),
            |  date({year:1984, month:10}),
            |  date({year:1984})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 10, 11)), Map("theDate" -> LocalDate.of(1984, 10, 1)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
          })) {
          resultTable()
        }
      }
      section("Creating a week (Year-Week-Day) _Date_", "functions-date-week") {
        p(
          """`date()` returns a _Date_ value with the specified _year_, _week_ and _dayOfWeek_ component values.""".stripMargin)
        function("date({year [, week, dayOfWeek]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."))
        considerations("The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.", "The _week_ component will default to `1` if `week` is omitted.", "If `week` is omitted, `dayOfWeek` must also be omitted.")
        preformattedQuery(
          """UNWIND [
            |  date({year:1984, week:10, dayOfWeek:3}),
            |  date({year:1984, week:10}),
            |  date({year:1984})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 3, 7)), Map("theDate" -> LocalDate.of(1984, 3, 5)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
          })) {
          resultTable()
        }
      }
      section("Creating a quarter (Year-Quarter-Day) _Date_", "functions-date-quarter") {
        p(
          """`date()` returns a _Date_ value with the specified _year_, _quarter_ and _dayOfQuarter_ component values.""".stripMargin)
        function("date({year [, quarter, dayOfQuarter]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."))
        considerations("The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.", "The _quarter_ component will default to `1` if `quarter` is omitted.", "If `quarter` is omitted, `dayOfQuarter` must also be omitted.")
        preformattedQuery(
          """UNWIND [
            |  date({year:1984, quarter:3, dayOfQuarter: 45}),
            |  date({year:1984, quarter:3}),
            |  date({year:1984})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 8, 14)), Map("theDate" -> LocalDate.of(1984, 7, 1)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
          })) {
          resultTable()
        }
      }
      section("Creating an ordinal (Year-Day) _Date_", "functions-date-ordinal") {
        p(
          """`date()` returns a _Date_ value with the specified _year_ and _ordinalDay_ component values.""".stripMargin)
        function("date({year [, ordinalDay]})", "A Date.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."))
        considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.")
        preformattedQuery(
          """UNWIND [
            |  date({year:1984, ordinalDay:202}),
            |  date({year:1984})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDate.of(1984, 7, 20)), Map("theDate" -> LocalDate.of(1984, 1, 1))))
          })) {
          p("""The date corresponding to `11 February 1984` is returned.""")
          resultTable()
        }
      }
      section("Creating a _Date_ from a string", "functions-date-create-string") {
        p(
          """`date()` returns the _Date_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
        function("date(temporalValue)", "A Date.", ("temporalValue", "A string representing a temporal value."))
        considerations("`temporalValue` must comply with the format defined for <<cypher-temporal-specify-date, dates>>.",
          "`temporalValue` must denote a valid date; i.e. a `temporalValue` denoting `30 February 2001` is invalid.",
          "`date(null)` returns null.")
        preformattedQuery(
          """UNWIND [
            |  date('2015-07-21'),
            |  date('2015-07'),
            |  date('201507'),
            |  date('2015-W30-2'),
            |  date('2015202'),
            |  date('2015')
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateValue.parse("2015-07-21").asObjectCopy()),
              Map("theDate" -> DateValue.parse("2015-07").asObjectCopy()),
              Map("theDate" -> DateValue.parse("201507").asObjectCopy()),
              Map("theDate" -> DateValue.parse("2015-W30-2").asObjectCopy()),
              Map("theDate" -> DateValue.parse("2015202").asObjectCopy()),
              Map("theDate" -> DateValue.parse("2015").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _Date_ using other temporal values as components", "functions-date-temporal") {
        p(
          """`date()` returns the _Date_ value obtained by selecting and composing components from another temporal value.
            |In essence, this allows a _DateTime_ or _LocalDateTime_ value to be converted to a _Date_, and for "missing" components to be provided.
          """.stripMargin)
        function("date({date [, year, month, day, week, dayOfWeek, quarter, dayOfQuarter, ordinalDay]})", "A Date.", ("A single map consisting of the following:", ""), ("date", "A _Date_ value."), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."))
        considerations("If any of the optional parameters are provided, these will override the corresponding components of `date`.",
          "`date(dd)` may be written instead of `date({date: dd})`.")
        preformattedQuery(
          """UNWIND [
            |  date({year:1984, month:11, day:11}),
            |  localdatetime({year:1984, month:11, day:11, hour:12, minute:31, second:14}),
            |  datetime({year:1984, month:11, day:11, hour:12, timezone: '+01:00'})
            |] as dd
            |RETURN date({date: dd}) AS dateOnly,
            |   date({date: dd, day: 28}) AS dateDay""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("dateOnly" -> DateValue.parse("1984-11-11").asObjectCopy(), "dateDay" -> DateValue.parse("1984-11-28").asObjectCopy()),
              Map("dateOnly" -> DateValue.parse("1984-11-11").asObjectCopy(), "dateDay" -> DateValue.parse("1984-11-28").asObjectCopy()),
              Map("dateOnly" -> DateValue.parse("1984-11-11").asObjectCopy(), "dateDay" -> DateValue.parse("1984-11-28").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Truncating a _Date_", "functions-date-truncate") {
        p(
          """`date.truncate()` returns the _Date_ value obtained by truncating a specified temporal instant value at the nearest preceding point in time at the specified component boundary (which is denoted by the truncation unit passed as a parameter to the function).
            |In other words, the _Date_ returned will have all components that are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of _overriding_ the default values which would otherwise have been set for these less significant components.
            |For example, `day` -- with some value `x` -- may be provided when the truncation unit is `year` in order to ensure the returned value has the _day_ set to `x` instead of the default _day_ (which is `1`).
          """.stripMargin)
        function("date.truncate(unit [, temporalInstantValue [, mapOfComponents ] ])", "A Date.", ("unit", "A string expression evaluating to one of the following: {`millennium`, `century`, `decade`, `year`, `weekYear`, `quarter`, `month`, `week`, `day`}."), ("temporalInstantValue", "An expression of one of the following types: {_DateTime_, _LocalDateTime_, _Date_}."), ("mapOfComponents", "An expression evaluating to a map containing components less significant than `unit`."))
        considerations("Any component that is provided in `mapOfComponents` must be less significant than `unit`; i.e. if `unit` is 'day', " +
          "`mapOfComponents` cannot contain information pertaining to a _month_.",
          "Any component that is not contained in `mapOfComponents` and which is less significant than `unit` will be set to its <<cypher-temporal-accessing-components-temporal-instants, minimal value>>.",
          "If `mapOfComponents` is not provided, all components of the returned value which are less significant than `unit` will be set to their default values.",
          "If `temporalInstantValue` is not provided, it will be set to the current date, i.e. `date.truncate(unit)` is equivalent of `date.truncate(unit, date())`.")
        preformattedQuery(
          """WITH datetime({year:2017, month:11, day:11, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: '+01:00'}) AS d
            |RETURN date.truncate('millennium', d) AS truncMillenium,
            |       date.truncate('century', d) AS truncCentury,
            |       date.truncate('decade', d) AS truncDecade,
            |       date.truncate('year', d, {day:5}) AS truncYear,
            |       date.truncate('weekYear', d) AS truncWeekYear,
            |       date.truncate('quarter', d) AS truncQuarter,
            |       date.truncate('month', d) AS truncMonth,
            |       date.truncate('week', d, {dayOfWeek:2}) AS truncWeek,
            |       date.truncate('day', d) AS truncDay""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "truncMillenium" -> DateValue.parse("2000-01-01").asObjectCopy(),
              "truncCentury" -> DateValue.parse("2000-01-01").asObjectCopy(),
              "truncDecade" -> DateValue.parse("2010-01-01").asObjectCopy(),
              "truncYear" -> DateValue.parse("2017-01-05").asObjectCopy(),
              "truncWeekYear" -> DateValue.parse("2017-01-02").asObjectCopy(),
              "truncQuarter" -> DateValue.parse("2017-10-01").asObjectCopy(),
              "truncMonth" -> DateValue.parse("2017-11-01").asObjectCopy(),
              "truncWeek" -> DateValue.parse("2017-11-07").asObjectCopy(),
              "truncDay" -> DateValue.parse("2017-11-11").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
    }
    section("DateTime: `datetime()`", "functions-datetime") {
      synopsis("""Details for using the `datetime()` function.""".stripMargin)
      p(
        """
          |* <<functions-datetime-current, Getting the current _DateTime_>>
          |* <<functions-datetime-calendar, Creating a calendar (Year-Month-Day) _DateTime_>>
          |* <<functions-datetime-week, Creating a week (Year-Week-Day) _DateTime_>>
          |* <<functions-datetime-quarter, Creating a quarter (Year-Quarter-Day) _DateTime_>>
          |* <<functions-datetime-ordinal, Creating an ordinal (Year-Day) _DateTime_>>
          |* <<functions-datetime-create-string, Creating a _DateTime_ from a string>>
          |* <<functions-datetime-temporal, Creating a _DateTime_ using other temporal values as components>>
          |* <<functions-datetime-timestamp, Creating a _DateTime_ from a timestamp>>
          |* <<functions-datetime-truncate, Truncating a _DateTime_>>
        """.stripMargin)
      section("Getting the current _DateTime_", "functions-datetime-current") {
        p(
          """`datetime()` returns the current _DateTime_ value.
            |If no time zone parameter is specified, the default time zone will be used.
          """.stripMargin)
        function("datetime([ +{timezone}+ ])", "A DateTime.", ("A single map consisting of the following:", ""), ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
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
          function("datetime.transaction([ +{timezone}+ ])", "A DateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN datetime.transaction() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[ZonedDateTime]("currentDateTime").next()
              now should be(a[ZonedDateTime])
            })) {
            resultTable()
          }
          query(
            """RETURN datetime.transaction('America/Los Angeles') AS currentDateTimeInLA""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[ZonedDateTime]("currentDateTimeInLA").next()
              now should be(a[ZonedDateTime])
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
          function("datetime.statement([ +{timezone}+ ])", "A DateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN datetime.statement() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[ZonedDateTime]("currentDateTime").next()
              now should be(a[ZonedDateTime])
            })) {
            resultTable()
          }
        }
        section("datetime.realtime()", "functions-datetime-current-realtime") {
          p(
            """`datetime.realtime()` returns the current _DateTime_ value using the `realtime` clock.
              |This value will be the live clock of the system.
            """.stripMargin)
          function("datetime.realtime([ +{timezone}+ ])", "A DateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN datetime.realtime() AS currentDateTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[ZonedDateTime]("currentDateTime").next()
              now should be(a[ZonedDateTime])
            })) {
            resultTable()
          }
        }
      }
      section("Creating a calendar (Year-Month-Day) _DateTime_", "functions-datetime-calendar") {
        p(
          """`datetime()` returns a _DateTime_ value with the specified _year_, _month_, _day_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
        function("datetime({year [, month, day, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("The _month_ component will default to `1` if `month` is omitted.",
          "The _day of the month_ component will default to `1` if `day` is omitted.",
          "The _hour_ component will default to `0` if `hour` is omitted.",
          "The _minute_ component will default to `0` if `minute` is omitted.",
          "The _second_ component will default to `0` if `second` is omitted.",
          "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.",
          "The _timezone_ component will default to the configured default time zone if `timezone` is omitted.",
          "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.",
          "The least significant components in the set `year`, `month`, `day`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `month` and `day`, but specifying `year`, `month`, `day` and `minute` is not permitted.",
          "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 123, microsecond: 456, nanosecond: 789}),
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 645, timezone: '+01:00'}),
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, timezone: '+01:00'}),
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, second:14}),
            |  datetime({year:1984, month:10, day:11, hour:12, minute:31, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'}),
            |  datetime({year:1984, month:10, day:11, timezone: 'Europe/Stockholm'})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31:14.123456789Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31:14.645+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31:14.645876123+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31:14+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31:14Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:31+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T12:00+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-10-11T00:00+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a week (Year-Week-Day) _DateTime_", "functions-datetime-week") {
        p(
          """`datetime()` returns a _DateTime_ value with the specified _year_, _week_, _dayOfWeek_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
        function("datetime({year [, week, dayOfWeek, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("The _week_ component will default to `1` if `week` is omitted.",
          "The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.",
          "The _hour_ component will default to `0` if `hour` is omitted.",
          "The _minute_ component will default to `0` if `minute` is omitted.",
          "The _second_ component will default to `0` if `second` is omitted.",
          "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.",
          "The _timezone_ component will default to the configured default time zone if `timezone` is omitted.",
          "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.",
          "The least significant components in the set `year`, `week`, `dayOfWeek`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `week` and `dayOfWeek`, but specifying `year`, `week`, `dayOfWeek` and `minute` is not permitted.",
          "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}),
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14}),
            |  datetime({year:1984, week:10, dayOfWeek:3, hour:12, timezone: '+01:00'}),
            |  datetime({year:1984, week:10, dayOfWeek:3, timezone: 'Europe/Stockholm'})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:31:14.645Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:31:14.645876+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:31:14.645876123+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:31:14+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:31:14Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T12:00+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-03-07T00:00+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a quarter (Year-Quarter-Day) _DateTime_", "functions-datetime-quarter") {
        p(
          """`datetime()` returns a _DateTime_ value with the specified _year_, _quarter_, _dayOfQuarter_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
        function("datetime({year [, quarter, dayOfQuarter, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("The _quarter_ component will default to `1` if `quarter` is omitted.",
          "The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.",
          "The _hour_ component will default to `0` if `hour` is omitted.",
          "The _minute_ component will default to `0` if `minute` is omitted.",
          "The _second_ component will default to `0` if `second` is omitted.",
          "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.",
          "The _timezone_ component will default to the configured default time zone if `timezone` is omitted.",
          "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.",
          "The least significant components in the set `year`, `quarter`, `dayOfQuarter`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `quarter` and `dayOfQuarter`, but specifying `year`, `quarter`, `dayOfQuarter` and `minute` is not permitted.",
          "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, microsecond: 645876}),
            |  datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, timezone: '+01:00'}),
            |  datetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, quarter:3, dayOfQuarter: 45})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateTimeValue.parse("1984-08-14T12:31:14.645876Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-08-14T12:31:14+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-08-14T12:00+02:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-08-14T00:00Z", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating an ordinal (Year-Day) _DateTime_", "functions-datetime-ordinal") {
        p(
          """`datetime()` returns a _DateTime_ value with the specified _year_, _ordinalDay_, _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
        function("datetime({year [, ordinalDay, hour, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.",
          "The _hour_ component will default to `0` if `hour` is omitted.",
          "The _minute_ component will default to `0` if `minute` is omitted.",
          "The _second_ component will default to `0` if `second` is omitted.",
          "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.",
          "The _timezone_ component will default to the configured default time zone if `timezone` is omitted.",
          "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.",
          "The least significant components in the set `year`, `ordinalDay`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year` and `ordinalDay`, but specifying `year`, `ordinalDay` and `minute` is not permitted.",
          "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  datetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, millisecond: 645}),
            |  datetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, timezone: '+01:00'}),
            |  datetime({year:1984, ordinalDay:202, timezone: 'Europe/Stockholm'}),
            |  datetime({year:1984, ordinalDay:202})
            |] as theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateTimeValue.parse("1984-07-20T12:31:14.645Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-07-20T12:31:14+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-07-20T00:00+02:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("1984-07-20T00:00Z", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _DateTime_ from a string", "functions-datetime-create-string") {
        p(
          """`datetime()` returns the _DateTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
        function("datetime(temporalValue)", "A DateTime.", ("temporalValue", "A string representing a temporal value."))
        considerations("`temporalValue` must comply with the format defined for <<cypher-temporal-specify-date, dates>>, <<cypher-temporal-specify-time, times>> and <<cypher-temporal-specify-time-zone, time zones>>.",
          "The _timezone_ component will default to the configured default time zone if it is omitted.",
          "`temporalValue` must denote a valid date and time; i.e. a `temporalValue` denoting `30 February 2001` is invalid.",
          "`datetime(null)` returns null.")
        preformattedQuery(
          """UNWIND [
            |  datetime('2015-07-21T21:40:32.142+0100'),
            |  datetime('2015-W30-2T214032.142Z'),
            |  datetime('2015T214032-0100'),
            |  datetime('20150721T21:40-01:30'),
            |  datetime('2015-W30T2140-02'),
            |  datetime('2015202T21+18:00'),
            |  datetime('2015-07-21T21:40:32.142[Europe/London]'),
            |  datetime('2015-07-21T21:40:32.142-04[America/New_York]')
            |] AS theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> DateTimeValue.parse("2015-07-21T21:40:32.142+0100", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015-W30-2T214032.142Z", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015T214032-0100", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("20150721T21:40-01:30", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015-W30T2140-02", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015202T21+18:00", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015-07-21T21:40:32.142[Europe/London]", defaultZoneSupplier).asObjectCopy()),
              Map("theDate" -> DateTimeValue.parse("2015-07-21T21:40:32.142-04[America/New_York]", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _DateTime_ using other temporal values as components", "functions-datetime-temporal") {
        p(
          """`datetime()` returns the _DateTime_ value obtained by selecting and composing components from another temporal value.
            |In essence, this allows a _Date_, _LocalDateTime_, _Time_ or _LocalTime_ value to be converted to a _DateTime_, and for "missing" components to be provided.
          """.stripMargin)
        function("datetime({datetime [, year, ..., timezone]}) | datetime({date [, year, ..., timezone]}) | datetime({time [, year, ..., timezone]}) | datetime({date, time [, year, ..., timezone]})", "A DateTime.", ("A single map consisting of the following:", ""), ("datetime", "A _DateTime_ value."), ("date", "A _Date_ value."), ("time", "A _Time_ value."), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("If any of the optional parameters are provided, these will override the corresponding components of `datetime`, `date` and/or `time`.",
          "`datetime(dd)` may be written instead of `datetime({datetime: dd})`.",
          "Selecting a _Time_ or _DateTime_ value as the `time` component also selects its time zone. If a _LocalTime_ or _LocalDateTime_ is selected instead, the default time zone is used. In any case, the time zone can be overridden explicitly.",
          "Selecting a _DateTime_ as the `datetime` component and overwriting the time zone will adjust the local time to keep the same point in time.",
          "Selecting a _DateTime_ or _Time_ as the `time` component and overwriting the time zone will adjust the local time to keep the same point in time.")
        p("""The following query shows the various usages of `datetime({date [, year, ..., timezone]})`""")
        preformattedQuery(
          """WITH date({year:1984, month:10, day:11}) AS dd
            |RETURN datetime({date:dd, hour: 10, minute: 10, second: 10}) AS dateHHMMSS,
            |       datetime({date:dd, hour: 10, minute: 10, second: 10, timezone:'+05:00'}) AS dateHHMMSSTimezone,
            |       datetime({date:dd, day: 28, hour: 10, minute: 10, second: 10}) AS dateDDHHMMSS,
            |       datetime({date:dd, day: 28, hour: 10, minute: 10, second: 10, timezone:'Pacific/Honolulu'}) AS dateDDHHMMSSTimezone""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateHHMMSS" -> DateTimeValue.parse("1984-10-11T10:10:10Z", defaultZoneSupplier).asObjectCopy(),
              "dateHHMMSSTimezone" -> DateTimeValue.parse("1984-10-11T10:10:10+05:00", defaultZoneSupplier).asObjectCopy(),
              "dateDDHHMMSS" -> DateTimeValue.parse("1984-10-28T10:10:10Z", defaultZoneSupplier).asObjectCopy(),
              "dateDDHHMMSSTimezone" -> DateTimeValue.parse("1984-10-28T10:10:10[Pacific/Honolulu]", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `datetime({time [, year, ..., timezone]})`""")
        preformattedQuery(
          """WITH time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}) AS tt
            |RETURN datetime({year:1984, month:10, day:11, time:tt}) AS YYYYMMDDTime,
            |       datetime({year:1984, month:10, day:11, time:tt, timezone:'+05:00'}) AS YYYYMMDDTimeTimezone,
            |       datetime({year:1984, month:10, day:11, time:tt, second: 42}) AS YYYYMMDDTimeSS,
            |       datetime({year:1984, month:10, day:11, time:tt, second: 42, timezone:'Pacific/Honolulu'}) AS YYYYMMDDTimeSSTimezone""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "YYYYMMDDTime" -> DateTimeValue.parse("1984-10-11T12:31:14.645876+01:00", defaultZoneSupplier).asObjectCopy(),
              "YYYYMMDDTimeTimezone" -> DateTimeValue.parse("1984-10-11T16:31:14.645876+05:00", defaultZoneSupplier).asObjectCopy(),
              "YYYYMMDDTimeSS" -> DateTimeValue.parse("1984-10-11T12:31:42.645876+01:00", defaultZoneSupplier).asObjectCopy(),
              "YYYYMMDDTimeSSTimezone" -> DateTimeValue.parse("1984-10-11T01:31:42.645876-10:00[Pacific/Honolulu]", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `datetime({date, time [, year, ..., timezone]})`; i.e. combining a _Date_ and a _Time_ value to create a single _DateTime_ value:""")
        preformattedQuery(
          """WITH date({year:1984, month:10, day:11}) AS dd,
            |     localtime({hour:12, minute:31, second:14, millisecond: 645}) AS tt
            |RETURN datetime({date:dd, time:tt}) as dateTime,
            |      datetime({date:dd, time:tt, timezone:'+05:00'}) AS dateTimeTimezone,
            |      datetime({date:dd, time:tt, day: 28, second: 42}) AS dateTimeDDSS,
            |      datetime({date:dd, time:tt, day: 28, second: 42, timezone:'Pacific/Honolulu'}) AS dateTimeDDSSTimezone""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateTime" -> DateTimeValue.parse("1984-10-11T12:31:14.645Z", defaultZoneSupplier).asObjectCopy(),
              "dateTimeTimezone" -> DateTimeValue.parse("1984-10-11T12:31:14.645+05:00", defaultZoneSupplier).asObjectCopy(),
              "dateTimeDDSS" -> DateTimeValue.parse("1984-10-28T12:31:42.645Z", defaultZoneSupplier).asObjectCopy(),
              "dateTimeDDSSTimezone" -> DateTimeValue.parse("1984-10-28T12:31:42.645-10:00[Pacific/Honolulu]", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `datetime({datetime [, year, ..., timezone]})`""")
        preformattedQuery(
          """WITH datetime({year:1984, month:10, day:11, hour:12, timezone: 'Europe/Stockholm'}) AS dd
            |RETURN datetime({datetime:dd}) AS dateTime,
            |       datetime({datetime:dd, timezone:'+05:00'}) AS dateTimeTimezone,
            |       datetime({datetime:dd, day: 28, second: 42}) AS dateTimeDDSS,
            |       datetime({datetime:dd, day: 28, second: 42, timezone:'Pacific/Honolulu'}) AS dateTimeDDSSTimezone""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateTime" -> DateTimeValue.parse("1984-10-11T12:00+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy(),
              "dateTimeTimezone" -> DateTimeValue.parse("1984-10-11T16:00+05:00", defaultZoneSupplier).asObjectCopy(),
              "dateTimeDDSS" -> DateTimeValue.parse("1984-10-28T12:00:42+01:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy(),
              "dateTimeDDSSTimezone" -> DateTimeValue.parse("1984-10-28T01:00:42-10:00[Pacific/Honolulu]", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
      section("Creating a _DateTime_ from a timestamp", "functions-datetime-timestamp") {
        p(
          """`datetime()` returns the _DateTime_ value at the specified number of _seconds_ or _milliseconds_ from the UNIX epoch in the UTC time zone.""".stripMargin)
        p("Conversions to other temporal instant types from UNIX epoch representations can be achieved by transforming a _DateTime_ value to one of these types.")
        function("datetime({ epochSeconds | epochMillis })", "A DateTime.", ("A single map consisting of the following:", ""), ("epochSeconds", "A numeric value representing the number of seconds from the UNIX epoch in the UTC time zone."), ("epochMillis", "A numeric value representing the number of milliseconds from the UNIX epoch in the UTC time zone."))
        considerations("`epochSeconds`/`epochMillis` may be used in conjunction with `nanosecond`")
        query(
          """RETURN datetime({epochSeconds:timestamp() / 1000, nanosecond: 23}) AS theDate""".stripMargin, ResultAssertions((r) => {
            val now = r.columnAs[ZonedDateTime]("theDate").next()
            now should be(a[ZonedDateTime])
          })) {
          resultTable()
        }
        query(
          """RETURN datetime({epochMillis: 424797300000}) AS theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> DateTimeValue.ofEpochMillis(Values.longValue(424797300000L)).asObjectCopy() )))
          })) {
          resultTable()
        }
      }
      section("Truncating a _DateTime_", "functions-datetime-truncate") {
        p(
          """`datetime.truncate()` returns the _DateTime_ value obtained by truncating a specified temporal instant value at the nearest preceding point in time at the specified component boundary (which is denoted by the truncation unit passed as a parameter to the function).
            |In other words, the _DateTime_ returned will have all components that are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of _overriding_ the default values which would otherwise have been set for these less significant components.
            |For example, `day` -- with some value `x` -- may be provided when the truncation unit is `year` in order to ensure the returned value has the _day_ set to `x` instead of the default _day_ (which is `1`).
          """.stripMargin)
        function("datetime.truncate(unit [, temporalInstantValue [, mapOfComponents ] ])", "A DateTime.", ("unit", "A string expression evaluating to one of the following: {`millennium`, `century`, `decade`, `year`, `weekYear`, `quarter`, `month`, `week`, `day`, `hour`, `minute`, `second`, `millisecond`, `microsecond`}."), ("temporalInstantValue", "An expression of one of the following types: {_DateTime_, _LocalDateTime_, _Date_}."), ("mapOfComponents", "An expression evaluating to a map containing components less significant than `unit`. During truncation, a time zone can be attached or overridden using the key `timezone`."))
        considerations("`temporalInstantValue` cannot be a _Date_ value if `unit` is one of {`hour`, `minute`, `second`, `millisecond`, `microsecond`}.",
          "The time zone of `temporalInstantValue` may be overridden; for example, `datetime.truncate('minute', input, {timezone:'+0200'})`. ",
          "If `temporalInstantValue` is one of {_Time_, _DateTime_} -- a value with a time zone -- and the time zone is overridden, no time conversion occurs.",
          "If `temporalInstantValue` is one of {_LocalDateTime_, _Date_} -- a value without a time zone -- and the time zone is not overridden, the configured default time zone will be used.",
          "Any component that is provided in `mapOfComponents` must be less significant than `unit`; i.e. if `unit` is 'day', `mapOfComponents` cannot contain information pertaining to a _month_.",
          "Any component that is not contained in `mapOfComponents` and which is less significant than `unit` will be set to its <<cypher-temporal-accessing-components-temporal-instants, minimal value>>.",
          "If `mapOfComponents` is not provided, all components of the returned value which are less significant than `unit` will be set to their default values.",
          "If `temporalInstantValue` is not provided, it will be set to the current date, time and timezone, i.e. `datetime.truncate(unit)` is equivalent of `datetime.truncate(unit, datetime())`.")
        preformattedQuery(
          """WITH datetime({year:2017, month:11, day:11, hour:12, minute:31, second:14, nanosecond: 645876123, timezone: '+03:00'}) AS d
            |RETURN datetime.truncate('millennium', d, {timezone:'Europe/Stockholm'}) AS truncMillenium,
            |       datetime.truncate('year', d, {day:5}) AS truncYear,
            |       datetime.truncate('month', d) AS truncMonth,
            |       datetime.truncate('day', d, {millisecond:2}) AS truncDay,
            |       datetime.truncate('hour', d) AS truncHour,
            |       datetime.truncate('second', d) AS truncSecond""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "truncMillenium" -> DateTimeValue.parse("2000-01-01T00:00[Europe/Stockholm]", defaultZoneSupplier).asObjectCopy(),
              "truncYear" -> DateTimeValue.parse("2017-01-05T00:00+03:00", defaultZoneSupplier).asObjectCopy(),
              "truncMonth" -> DateTimeValue.parse("2017-11-01T00:00+03:00", defaultZoneSupplier).asObjectCopy(),
              "truncDay" -> DateTimeValue.parse("2017-11-11T00:00:00.002+03:00", defaultZoneSupplier).asObjectCopy(),
              "truncHour" -> DateTimeValue.parse("2017-11-11T12:00+03:00", defaultZoneSupplier).asObjectCopy(),
              "truncSecond" -> DateTimeValue.parse("2017-11-11T12:31:14+03:00", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
    }
    section("LocalDateTime: `localdatetime()`", "functions-localdatetime") {
      synopsis("""Details for using the `localdatetime()` function.""".stripMargin)
      p(
        """
          |* <<functions-localdatetime-current, Getting the current _LocalDateTime_>>
          |* <<functions-localdatetime-calendar, Creating a calendar (Year-Month-Day) _LocalDateTime_>>
          |* <<functions-localdatetime-week, Creating a week (Year-Week-Day) _LocalDateTime_>>
          |* <<functions-localdatetime-quarter, Creating a quarter (Year-Quarter-Day) _LocalDateTime_>>
          |* <<functions-localdatetime-ordinal, Creating an ordinal (Year-Day) _LocalDateTime_>>
          |* <<functions-localdatetime-create-string, Creating a _LocalDateTime_ from a string>>
          |* <<functions-localdatetime-temporal, Creating a _LocalDateTime_ using other temporal values as components>>
          |* <<functions-localdatetime-truncate, Truncating a _LocalDateTime_>>
        """.stripMargin)
      section("Getting the current _LocalDateTime_", "functions-localdatetime-current") {
        p(
          """`localdatetime()` returns the current _LocalDateTime_ value.
            |If no time zone parameter is specified, the local time zone will be used.
          """.stripMargin)
        function("localdatetime([ +{timezone}+ ])", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
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
          function("localdatetime.transaction([ +{timezone}+ ])", "A LocalDateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localdatetime.transaction() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDateTime]("now").next()
              now should be(a[LocalDateTime])
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
          function("localdatetime.statement([ +{timezone}+ ])", "A LocalDateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localdatetime.statement() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDateTime]("now").next()
              now should be(a[LocalDateTime])
            })) {
            resultTable()
          }
        }
        section("localdatetime.realtime()", "functions-localdatetime-current-realtime") {
          p(
            """`localdatetime.realtime()` returns the current _LocalDateTime_ value using the `realtime` clock.
              |This value will be the live clock of the system.
            """.stripMargin)
          function("localdatetime.realtime([ +{timezone}+ ])", "A LocalDateTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localdatetime.realtime() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDateTime]("now").next()
              now should be(a[LocalDateTime])
            })) {
            resultTable()
          }
          query(
            """RETURN localdatetime.realtime('America/Los Angeles') AS nowInLA""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalDateTime]("nowInLA").next()
              now should be(a[LocalDateTime])
            })) {
            resultTable()
          }
        }
      }
      section("Creating a calendar (Year-Month-Day) _LocalDateTime_", "functions-localdatetime-calendar") {
        p(
          """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _month_, _day_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
        function("localdatetime({year [, month, day, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("The _month_ component will default to `1` if `month` is omitted.", "The _day of the month_ component will default to `1` if `day` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `month`, `day`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `month` and `day`, but specifying `year`, `month`, `day` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        query(
          """RETURN localdatetime({year:1984, month:10, day:11, hour:12, minute:31, second:14, millisecond: 123, microsecond: 456, nanosecond: 789}) AS theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDateTimeValue.parse("1984-10-11T12:31:14.123456789").asObjectCopy())))
          })) {
          resultTable()
        }
      }
      section("Creating a week (Year-Week-Day) _LocalDateTime_", "functions-localdatetime-week") {
        p(
          """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _week_, _dayOfWeek_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
        function("localdatetime({year [, week, dayOfWeek, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("The _week_ component will default to `1` if `week` is omitted.", "The _day of the week_ component will default to `1` if `dayOfWeek` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `week`, `dayOfWeek`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `week` and `dayOfWeek`, but specifying `year`, `week`, `dayOfWeek` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        query(
          """RETURN localdatetime({year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645}) AS theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDateTimeValue.parse("1984-03-07T12:31:14.645").asObjectCopy())))
          })) {
          resultTable()
        }
      }
      section("Creating a quarter (Year-Quarter-Day) _DateTime_", "functions-localdatetime-quarter") {
        p(
          """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _quarter_, _dayOfQuarter_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
        function("localdatetime({year [, quarter, dayOfQuarter, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("The _quarter_ component will default to `1` if `quarter` is omitted.", "The _day of the quarter_ component will default to `1` if `dayOfQuarter` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `quarter`, `dayOfQuarter`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year`, `quarter` and `dayOfQuarter`, but specifying `year`, `quarter`, `dayOfQuarter` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        query(
          """RETURN localdatetime({year:1984, quarter:3, dayOfQuarter: 45, hour:12, minute:31, second:14, nanosecond: 645876123}) AS theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDateTimeValue.parse("1984-08-14T12:31:14.645876123").asObjectCopy())))
          })) {
          resultTable()
        }
      }
      section("Creating an ordinal (Year-Day) _LocalDateTime_", "functions-localdatetime-ordinal") {
        p(
          """`localdatetime()` returns a _LocalDateTime_ value with the specified _year_, _ordinalDay_, _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
        function("localdatetime({year [, ordinalDay, hour, minute, second, millisecond, microsecond, nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("The _ordinal day of the year_ component will default to `1` if `ordinalDay` is omitted.", "The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `year`, `ordinalDay`, `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `year` and `ordinalDay`, but specifying `year`, `ordinalDay` and `minute` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        query(
          """RETURN localdatetime({year:1984, ordinalDay:202, hour:12, minute:31, second:14, microsecond: 645876}) AS theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("theDate" -> LocalDateTimeValue.parse("1984-07-20T12:31:14.645876").asObjectCopy())))
          })) {
          resultTable()
        }
      }
      section("Creating a _LocalDateTime_ from a string", "functions-localdatetime-create-string") {
        p(
          """`localdatetime()` returns the _LocalDateTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
        function("localdatetime(temporalValue)", "A LocalDateTime.", ("temporalValue", "A string representing a temporal value."))
        considerations("`temporalValue` must comply with the format defined for <<cypher-temporal-specify-date, dates>> and <<cypher-temporal-specify-time, times>>.",
          "`temporalValue` must denote a valid date and time; i.e. a `temporalValue` denoting `30 February 2001` is invalid.",
          "`localdatetime(null)` returns null.")
        preformattedQuery(
          """UNWIND [
            |  localdatetime('2015-07-21T21:40:32.142'),
            |  localdatetime('2015-W30-2T214032.142'),
            |  localdatetime('2015-202T21:40:32'),
            |  localdatetime('2015202T21')
            |] AS theDate
            |RETURN theDate""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theDate" -> LocalDateTimeValue.parse("2015-07-21T21:40:32.142").asObjectCopy()),
              Map("theDate" -> LocalDateTimeValue.parse("2015-W30-2T214032.142").asObjectCopy()),
              Map("theDate" -> LocalDateTimeValue.parse("2015-202T21:40:32").asObjectCopy()),
              Map("theDate" -> LocalDateTimeValue.parse("2015202T21").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _LocalDateTime_ using other temporal values as components", "functions-localdatetime-temporal") {
        p(
          """`localdatetime()` returns the _LocalDateTime_ value obtained by selecting and composing components from another temporal value.
            |In essence, this allows a _Date_, _DateTime_, _Time_ or _LocalTime_ value to be converted to a _LocalDateTime_, and for "missing" components to be provided.
          """.stripMargin)
        function("localdatetime({datetime [, year, ..., nanosecond]}) | localdatetime({date [, year, ..., nanosecond]}) | localdatetime({time [, year, ..., nanosecond]}) | localdatetime({date, time [, year, ..., nanosecond]})", "A LocalDateTime.", ("A single map consisting of the following:", ""), ("datetime", "A _DateTime_ value."), ("date", "A _Date_ value."), ("time", "A _Time_ value."), ("year", "An expression consisting of at <<cypher-temporal-year, least four digits>> that specifies the year."), ("month", "An integer between `1` and `12` that specifies the month."), ("day", "An integer between `1` and `31` that specifies the day of the month."), ("week", "An integer between `1` and `53` that specifies the week."), ("dayOfWeek", "An integer between `1` and `7` that specifies the day of the week."), ("quarter", "An integer between `1` and `4` that specifies the quarter."), ("dayOfQuarter", "An integer between `1` and `92` that specifies the day of the quarter."), ("ordinalDay", "An integer between `1` and `366` that specifies the ordinal day of the year."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("If any of the optional parameters are provided, these will override the corresponding components of `datetime`, `date` and/or `time`.",
          "`localdatetime(dd)` may be written instead of `localdatetime({datetime: dd})`.")
        p("""The following query shows the various usages of `localdatetime({date [, year, ..., nanosecond]})`""")
        preformattedQuery(
          """WITH date({year:1984, month:10, day:11}) AS dd
            |RETURN localdatetime({date:dd, hour: 10, minute: 10, second: 10}) AS dateHHMMSS,
            |       localdatetime({date:dd, day: 28, hour: 10, minute: 10, second: 10}) AS dateDDHHMMSS""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateHHMMSS" -> LocalDateTimeValue.parse("1984-10-11T10:10:10").asObjectCopy(),
              "dateDDHHMMSS" -> LocalDateTimeValue.parse("1984-10-28T10:10:10").asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `localdatetime({time [, year, ..., nanosecond]})`""")
        preformattedQuery(
          """WITH time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}) AS tt
            |RETURN localdatetime({year:1984, month:10, day:11, time:tt}) AS YYYYMMDDTime,
            |       localdatetime({year:1984, month:10, day:11, time:tt, second: 42}) AS YYYYMMDDTimeSS""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "YYYYMMDDTime" -> LocalDateTimeValue.parse("1984-10-11T12:31:14.645876").asObjectCopy(),
              "YYYYMMDDTimeSS" -> LocalDateTimeValue.parse("1984-10-11T12:31:42.645876").asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `localdatetime({date, time [, year, ..., nanosecond]})`; i.e. combining a _Date_ and a _Time_ value to create a single _LocalDateTime_ value:""")
        preformattedQuery(
          """WITH date({year:1984, month:10, day:11}) AS dd,
            |     time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}) AS tt
            |RETURN localdatetime({date:dd, time:tt}) AS dateTime,
            |       localdatetime({date:dd, time:tt, day: 28, second: 42}) AS dateTimeDDSS""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateTime" -> LocalDateTimeValue.parse("1984-10-11T12:31:14.645876").asObjectCopy(),
              "dateTimeDDSS" -> LocalDateTimeValue.parse("1984-10-28T12:31:42.645876").asObjectCopy()
            )))
          })) {
          resultTable()
        }
        p("""The following query shows the various usages of `localdatetime({datetime [, year, ..., nanosecond]})`""")
        preformattedQuery(
          """WITH datetime({year:1984, month:10, day:11, hour:12, timezone: '+01:00'}) as dd
            |RETURN localdatetime({datetime:dd}) as dateTime,
            |       localdatetime({datetime:dd, day: 28, second: 42}) as dateTimeDDSS""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "dateTime" -> LocalDateTimeValue.parse("1984-10-11T12:00").asObjectCopy(),
              "dateTimeDDSS" -> LocalDateTimeValue.parse("1984-10-28T12:00:42").asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
      section("Truncating a _LocalDateTime_", "functions-localdatetime-truncate") {
        p(
          """`localdatetime.truncate()` returns the _LocalDateTime_ value obtained by truncating a specified temporal instant value at the nearest preceding point in time at the specified component boundary (which is denoted by the truncation unit passed as a parameter to the function).
            |In other words, the _LocalDateTime_ returned will have all components that are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of _overriding_ the default values which would otherwise have been set for these less significant components.
            |For example, `day` -- with some value `x` -- may be provided when the truncation unit is `year` in order to ensure the returned value has the _day_ set to `x` instead of the default _day_ (which is `1`).
          """.stripMargin)
        function("localdatetime.truncate(unit [, temporalInstantValue [, mapOfComponents ] ])", "A LocalDateTime.", ("unit", "A string expression evaluating to one of the following: {`millennium`, `century`, `decade`, `year`, `weekYear`, `quarter`, `month`, `week`, `day`, `hour`, `minute`, `second`, `millisecond`, `microsecond`}."), ("temporalInstantValue", "An expression of one of the following types: {_DateTime_, _LocalDateTime_, _Date_}."), ("mapOfComponents", "An expression evaluating to a map containing components less significant than `unit`."))
        considerations("`temporalInstantValue` cannot be a _Date_ value if `unit` is one of {`hour`, `minute`, `second`, `millisecond`, `microsecond`}.",
          "Any component that is provided in `mapOfComponents` must be less significant than `unit`; i.e. if `unit` is 'day', `mapOfComponents` cannot contain information pertaining to a _month_.",
          "Any component that is not contained in `mapOfComponents` and which is less significant than `unit` will be set to its <<cypher-temporal-accessing-components-temporal-instants, minimal value>>.",
          "If `mapOfComponents` is not provided, all components of the returned value which are less significant than `unit` will be set to their default values.",
          "If `temporalInstantValue` is not provided, it will be set to the current date and time, i.e. `localdatetime.truncate(unit)` is equivalent of `localdatetime.truncate(unit, localdatetime())`.")
        preformattedQuery(
          """WITH localdatetime({year:2017, month:11, day:11, hour:12, minute:31, second:14, nanosecond: 645876123}) AS d
            |RETURN localdatetime.truncate('millennium', d) AS truncMillenium,
            |       localdatetime.truncate('year', d, {day:2}) AS truncYear,
            |       localdatetime.truncate('month', d) AS truncMonth,
            |       localdatetime.truncate('day', d) AS truncDay,
            |       localdatetime.truncate('hour', d, {nanosecond:2}) AS truncHour,
            |       localdatetime.truncate('second', d) AS truncSecond""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "truncMillenium" -> LocalDateTimeValue.parse("2000-01-01T00:00").asObjectCopy(),
              "truncYear" -> LocalDateTimeValue.parse("2017-01-02T00:00").asObjectCopy(),
              "truncMonth" -> LocalDateTimeValue.parse("2017-11-01T00:00").asObjectCopy(),
              "truncDay" -> LocalDateTimeValue.parse("2017-11-11T00:00").asObjectCopy(),
              "truncHour" -> LocalDateTimeValue.parse("2017-11-11T12:00:00.000000002").asObjectCopy(),
              "truncSecond" -> LocalDateTimeValue.parse("2017-11-11T12:31:14").asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
    }
    section("LocalTime: `localtime()`", "functions-localtime") {
      synopsis("""Details for using the `localtime()` function.""".stripMargin)
      p(
        """
          |* <<functions-localtime-current, Getting the current _LocalTime_>>
          |* <<functions-localtime-create, Creating a _LocalTime_>>
          |* <<functions-localtime-create-string, Creating a _LocalTime_ from a string>>
          |* <<functions-localtime-temporal, Creating a _LocalTime_ using other temporal values as components>>
          |* <<functions-localtime-truncate, Truncating a _LocalTime_>>
        """.stripMargin)
      section("Getting the current _LocalTime_", "functions-localtime-current") {
        p(
          """`localtime()` returns the current _LocalTime_ value.
            |If no time zone parameter is specified, the local time zone will be used.
          """.stripMargin)
        function("localtime([ +{timezone}+ ])", "A LocalTime.", ("A single map consisting of the following:", ""), ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
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
          function("localtime.transaction([ +{timezone}+ ])", "A LocalTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localtime.transaction() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalTime]("now").next()
              now should be(a[LocalTime])
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
          function("localtime.statement([ +{timezone}+ ])", "A LocalTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localtime.statement() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalTime]("now").next()
              now should be(a[LocalTime])
            })) {
            resultTable()
          }
          query(
            """RETURN localtime.statement('America/Los Angeles') AS nowInLA""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalTime]("nowInLA").next()
              now should be(a[LocalTime])
            })) {
            resultTable()
          }
        }
        section("localtime.realtime()", "functions-localtime-current-realtime") {
          p(
            """`localtime.realtime()` returns the current _LocalTime_ value using the `realtime` clock.
              |This value will be the live clock of the system.
            """.stripMargin)
          function("localtime.realtime([ +{timezone}+ ])", "A LocalTime.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN localtime.realtime() AS now""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[LocalTime]("now").next()
              now should be(a[LocalTime])
            })) {
            resultTable()
          }
        }
      }
      section("Creating a _LocalTime_", "functions-localtime-create") {
        p(
          """`localtime()` returns a _LocalTime_ value with the specified _hour_, _minute_, _second_, _millisecond_, _microsecond_ and _nanosecond_ component values.""".stripMargin)
        function("localtime({hour [, minute, second, millisecond, microsecond, nanosecond]})", "A LocalTime.", ("A single map consisting of the following:", ""), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.", "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.", "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.", "The least significant components in the set `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `hour` and `minute`, but specifying `hour` and `second` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  localtime({hour:12, minute:31, second:14, nanosecond: 789, millisecond: 123, microsecond: 456}),
            |  localtime({hour:12, minute:31, second:14}),
            |  localtime({hour:12})
            |] as theTime
            |RETURN theTime""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theTime" -> LocalTimeValue.parse("12:31:14.123456789").asObjectCopy()),
              Map("theTime" -> LocalTimeValue.parse("12:31:14").asObjectCopy()),
              Map("theTime" -> LocalTimeValue.parse("12:00").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _LocalTime_ from a string", "functions-localtime-create-string") {
        p(
          """`localtime()` returns the _LocalTime_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
        function("localtime(temporalValue)", "A LocalTime.", ("temporalValue", "A string representing a temporal value."))
        considerations("`temporalValue` must comply with the format defined for <<cypher-temporal-specify-time, times>>.",
          "`temporalValue` must denote a valid time; i.e. a `temporalValue` denoting `13:46:64` is invalid.",
          "`localtime(null)` returns null.")
        preformattedQuery(
          """UNWIND [
            |  localtime('21:40:32.142'),
            |  localtime('214032.142'),
            |  localtime('21:40'),
            |  localtime('21')
            |] AS theTime
            |RETURN theTime""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theTime" -> LocalTimeValue.parse("21:40:32.142").asObjectCopy()),
              Map("theTime" -> LocalTimeValue.parse("214032.142").asObjectCopy()),
              Map("theTime" -> LocalTimeValue.parse("21:40").asObjectCopy()),
              Map("theTime" -> LocalTimeValue.parse("21").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _LocalTime_ using other temporal values as components", "functions-localtime-temporal") {
        p(
          """`localtime()` returns the _LocalTime_ value obtained by selecting and composing components from another temporal value.
            |In essence, this allows a _DateTime_, _LocalDateTime_ or _Time_ value to be converted to a _LocalTime_, and for "missing" components to be provided.
          """.stripMargin)
        function("localtime({time [, hour, ..., nanosecond]})", "A LocalTime.", ("A single map consisting of the following:", ""), ("time", "A _Time_ value."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."))
        considerations("If any of the optional parameters are provided, these will override the corresponding components of `time`.",
          "`localtime(tt)` may be written instead of `localtime({time: tt})`.")
        preformattedQuery(
          """WITH time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}) AS tt
            |RETURN localtime({time:tt}) AS timeOnly,
            |       localtime({time:tt, second: 42}) AS timeSS""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "timeOnly" -> LocalTimeValue.parse("12:31:14.645876").asObjectCopy(),
              "timeSS" -> LocalTimeValue.parse("12:31:42.645876").asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
      section("Truncating a _LocalTime_", "functions-localtime-truncate") {
        p(
          """`localtime.truncate()` returns the _LocalTime_ value obtained by truncating a specified temporal instant value at the nearest preceding point in time at the specified component boundary (which is denoted by the truncation unit passed as a parameter to the function).
            |In other words, the _LocalTime_ returned will have all components that are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of _overriding_ the default values which would otherwise have been set for these less significant components.
            |For example, `minute` -- with some value `x` -- may be provided when the truncation unit is `hour` in order to ensure the returned value has the _minute_ set to `x` instead of the default _minute_ (which is `1`).
          """.stripMargin)
        function("localtime.truncate(unit [, temporalInstantValue [, mapOfComponents ] ])", "A LocalTime.", ("unit", "A string expression evaluating to one of the following: {`day`, `hour`, `minute`, `second`, `millisecond`, `microsecond`}."), ("temporalInstantValue", "An expression of one of the following types: {_DateTime_, _LocalDateTime_, _Time_, _LocalTime_}."), ("mapOfComponents", "An expression evaluating to a map containing components less significant than `unit`."))
        considerations("Truncating time to day -- i.e. `unit` is 'day'  -- is supported, and yields midnight at the start of the day (`00:00`), regardless of the value of `temporalInstantValue`. However, the time zone of `temporalInstantValue` is retained.",
          "Any component that is provided in `mapOfComponents` must be less significant than `unit`; i.e. if `unit` is 'second', `mapOfComponents` cannot contain information pertaining to a _minute_.", "Any component that is not contained in `mapOfComponents` and which is less significant than `unit` will be set to its <<cypher-temporal-accessing-components-temporal-instants, minimal value>>.",
          "If `mapOfComponents` is not provided, all components of the returned value which are less significant than `unit` will be set to their default values.",
          "If `temporalInstantValue` is not provided, it will be set to the current time, i.e. `localtime.truncate(unit)` is equivalent of `localtime.truncate(unit, localtime())`.")
        preformattedQuery(
          """WITH time({hour:12, minute:31, second:14, nanosecond: 645876123, timezone: '-01:00'}) AS t
            |RETURN localtime.truncate('day', t) AS truncDay,
            |       localtime.truncate('hour', t) AS truncHour,
            |       localtime.truncate('minute', t, {millisecond:2}) AS truncMinute,
            |       localtime.truncate('second', t) AS truncSecond,
            |       localtime.truncate('millisecond', t) AS truncMillisecond,
            |       localtime.truncate('microsecond', t) AS truncMicrosecond""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "truncDay" -> LocalTimeValue.parse("00:00").asObjectCopy(),
              "truncHour" -> LocalTimeValue.parse("12:00").asObjectCopy(),
              "truncMinute" -> LocalTimeValue.parse("12:31:00.002").asObjectCopy(),
              "truncSecond" -> LocalTimeValue.parse("12:31:14").asObjectCopy(),
              "truncMillisecond" -> LocalTimeValue.parse("12:31:14.645").asObjectCopy(),
              "truncMicrosecond" -> LocalTimeValue.parse("12:31:14.645876").asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
    }
    section("Time: `time()`", "functions-time") {
      synopsis("""Details for using the `time()` function.""".stripMargin)
      p(
        """
          |* <<functions-time-current, Getting the current _Time_>>
          |* <<functions-time-create, Creating a _Time_>>
          |* <<functions-time-create-string, Creating a _Time_ from a string>>
          |* <<functions-time-temporal, Creating a _Time_ using other temporal values as components>>
          |* <<functions-time-truncate, Truncating a _Time_>>
        """.stripMargin)
      section("Getting the current _Time_", "functions-time-current") {
        p(
          """`time()` returns the current _Time_ value.
            |If no time zone parameter is specified, the local time zone will be used.""".stripMargin)
        function("time([ +{timezone}+ ])", "A Time.", ("A single map consisting of the following:", ""), ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
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
        section("time.transaction()", "functions-time-current-transaction") {
          p(
            """`time.transaction()` returns the current _Time_ value using the `transaction` clock.
              |This value will be the same for each invocation within the same transaction.
              |However, a different value may be produced for different transactions.
            """.stripMargin)
          function("time.transaction([ +{timezone}+ ])", "A Time.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN time.transaction() AS currentTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[OffsetTime]("currentTime").next()
              now should be(a[OffsetTime])
            })) {
            resultTable()
          }
        }
        section("time.statement()", "functions-time-current-statement") {
          p(
            """`time.statement()` returns the current _Time_ value  using the `statement` clock.
              |This value will be the same for each invocation within the same statement.
              |However, a different value may be produced for different statements within the same transaction.
            """.stripMargin)
          function("time.statement([ +{timezone}+ ])", "A Time.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN time.statement() AS currentTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[OffsetTime]("currentTime").next()
              now should be(a[OffsetTime])
            })) {
            resultTable()
          }
          query(
            """RETURN time.statement('America/Los Angeles') AS currentTimeInLA""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[OffsetTime]("currentTimeInLA").next()
              now should be(a[OffsetTime])
            })) {
            resultTable()
          }
        }
        section("time.realtime()", "functions-time-current-realtime") {
          p(
            """`time.realtime()` returns the current _Time_ value using the `realtime` clock.
              |This value will be the live clock of the system.
            """.stripMargin)
          function("time.realtime([ +{timezone}+ ])", "A Time.", ("timezone", "A string expression that represents the <<cypher-temporal-specify-time-zone, time zone>>"))
          query(
            """RETURN time.realtime() AS currentTime""".stripMargin, ResultAssertions((r) => {
              val now = r.columnAs[OffsetTime]("currentTime").next()
              now should be(a[OffsetTime])
            })) {
            resultTable()
          }
        }
      }
      section("Creating a _Time_", "functions-time-create") {
        p(
          """`time()` returns a _Time_ value with the specified _hour_, _minute_, _second_, _millisecond_, _microsecond_, _nanosecond_ and _timezone_ component values.""".stripMargin)
        function("time({hour [, minute, second, millisecond, microsecond, nanosecond, timezone]})", "A Time.", ("A single map consisting of the following:", ""), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("The _hour_ component will default to `0` if `hour` is omitted.", "The _minute_ component will default to `0` if `minute` is omitted.",
          "The _second_ component will default to `0` if `second` is omitted.", "Any missing `millisecond`, `microsecond` or `nanosecond` values will default to `0`.",
          "The _timezone_ component will default to the configured default time zone if `timezone` is omitted.",
          "If `millisecond`, `microsecond` and `nanosecond` are given in combination (as part of the same set of parameters), the individual values must be in the range `0` to `999`.",
          "The least significant components in the set `hour`, `minute`, and `second` may be omitted; i.e. it is possible to specify only `hour` and `minute`, but specifying `hour` and `second` is not permitted.", "One or more of `millisecond`, `microsecond` and `nanosecond` can only be specified as long as `second` is also specified.")
        preformattedQuery(
          """UNWIND [
            |  time({hour:12, minute:31, second:14, millisecond: 123, microsecond: 456, nanosecond: 789}),
            |  time({hour:12, minute:31, second:14, nanosecond: 645876123}),
            |  time({hour:12, minute:31, second:14, microsecond: 645876, timezone: '+01:00'}),
            |  time({hour:12, minute:31, timezone: '+01:00'}),
            |  time({hour:12, timezone: '+01:00'})
            |] AS theTime
            |RETURN theTime""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theTime" -> TimeValue.parse("12:31:14.123456789Z", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("12:31:14.645876123Z", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("12:31:14.645876+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("12:31+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("12:00+01:00", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _Time_ from a string", "functions-time-create-string") {
        p(
          """`time()` returns the _Time_ value obtained by parsing a string representation of a temporal value.""".stripMargin)
        function("time(temporalValue)", "A Time.", ("temporalValue", "A string representing a temporal value."))
        considerations("`temporalValue` must comply with the format defined for <<cypher-temporal-specify-time, times>> and <<cypher-temporal-specify-time-zone, time zones>>.",
          "The _timezone_ component will default to the configured default time zone if it is omitted.",
          "`temporalValue` must denote a valid time; i.e. a `temporalValue` denoting `15:67` is invalid.",
          "`time(null)` returns null.")
        preformattedQuery(
          """UNWIND [
            |  time('21:40:32.142+0100'),
            |  time('214032.142Z'),
            |  time('21:40:32+01:00'),
            |  time('214032-0100'),
            |  time('21:40-01:30'),
            |  time('2140-00:00'),
            |  time('2140-02'),
            |  time('22+18:00')
            |] AS theTime
            |RETURN theTime""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(
              Map("theTime" -> TimeValue.parse("21:40:32.142+0100", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("214032.142Z", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("21:40:32+01:00", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("214032-0100", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("21:40-01:30", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("2140-00:00", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("2140-02", defaultZoneSupplier).asObjectCopy()),
              Map("theTime" -> TimeValue.parse("22+18:00", defaultZoneSupplier).asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Creating a _Time_ using other temporal values as components", "functions-time-temporal") {
        p(
          """`time()` returns the _Time_ value obtained by selecting and composing components from another temporal value.
            |In essence, this allows a _DateTime_, _LocalDateTime_ or _LocalTime_ value to be converted to a _Time_, and for "missing" components to be provided.
          """.stripMargin)
        function("time({time [, hour, ..., timezone]})", "A Time.", ("A single map consisting of the following:", ""), ("time", "A _Time_ value."), ("hour", "An integer between `0` and `23` that specifies the hour of the day."), ("minute", "An integer between `0` and `59` that specifies the number of minutes."), ("second", "An integer between `0` and `59` that specifies the number of seconds."), ("millisecond", "An integer between `0` and `999` that specifies the number of milliseconds."), ("microsecond", "An integer between `0` and `999,999` that specifies the number of microseconds."), ("nanosecond", "An integer between `0` and `999,999,999` that specifies the number of nanoseconds."), ("timezone", "An expression that specifies the time zone."))
        considerations("If any of the optional parameters are provided, these will override the corresponding components of `time`.",
          "`time(tt)` may be written instead of `time({time: tt})`.",
          "Selecting a _Time_ or _DateTime_ value as the `time` component also selects its time zone. If a _LocalTime_ or _LocalDateTime_ is selected instead, the default time zone is used. In any case, the time zone can be overridden explicitly.",
          "Selecting a _DateTime_ or _Time_ as the `time` component and overwriting the time zone will adjust the local time to keep the same point in time.")
        preformattedQuery(
          """WITH localtime({hour:12, minute:31, second:14, microsecond: 645876}) AS tt
            |RETURN time({time:tt}) AS timeOnly,
            |       time({time:tt, timezone:'+05:00'}) AS timeTimezone,
            |       time({time:tt, second: 42}) AS timeSS,
            |       time({time:tt, second: 42, timezone:'+05:00'}) AS timeSSTimezone""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "timeOnly" -> TimeValue.parse("12:31:14.645876", defaultZoneSupplier).asObjectCopy(),
              "timeTimezone" -> TimeValue.parse("12:31:14.645876+05:00", defaultZoneSupplier).asObjectCopy(),
              "timeSS" -> TimeValue.parse("12:31:42.645876Z", defaultZoneSupplier).asObjectCopy(),
              "timeSSTimezone" -> TimeValue.parse("12:31:42.645876+05:00", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
      section("Truncating a _Time_", "functions-time-truncate") {
        p(
          """`time.truncate()` returns the _Time_ value obtained by truncating a specified temporal instant value at the nearest preceding point in time at the specified component boundary (which is denoted by the truncation unit passed as a parameter to the function).
            |In other words, the _Time_ returned will have all components that are less significant than the specified truncation unit set to their default values.""".stripMargin)
        p(
          """It is possible to supplement the truncated value by providing a map containing components which are less significant than the truncation unit.
            |This will have the effect of _overriding_ the default values which would otherwise have been set for these less significant components.
            |For example, `minute` -- with some value `x` -- may be provided when the truncation unit is `hour` in order to ensure the returned value has the _minute_ set to `x` instead of the default _minute_ (which is `1`).
          """.stripMargin)
        function("time.truncate(unit [, temporalInstantValue [, mapOfComponents ] ])", "A Time.", ("unit", "A string expression evaluating to one of the following: {`day`, `hour`, `minute`, `second`, `millisecond`, `microsecond`}."), ("temporalInstantValue", "An expression of one of the following types: {_DateTime_, _LocalDateTime_, _Time_, _LocalTime_}."), ("mapOfComponents", "An expression evaluating to a map containing components less significant than `unit`. During truncation, a time zone can be attached or overridden using the key `timezone`."))
        considerations("Truncating time to day -- i.e. `unit` is 'day'  -- is supported, and yields midnight at the start of the day (`00:00`), regardless of the value of `temporalInstantValue`. However, the time zone of `temporalInstantValue` is retained.",
          "The time zone of `temporalInstantValue` may be overridden; for example, `time.truncate('minute', input, {timezone:'+0200'})`. ",
          "If `temporalInstantValue` is one of {_Time_, _DateTime_} -- a value with a time zone -- and the time zone is overridden, no time conversion occurs.",
          "If `temporalInstantValue` is one of {_LocalTime_, _LocalDateTime_, _Date_} -- a value without a time zone -- and the time zone is not overridden, the configured default time zone will be used.",
          "Any component that is provided in `mapOfComponents` must be less significant than `unit`; i.e. if `unit` is 'second', `mapOfComponents` cannot contain information pertaining to a _minute_.",
          "Any component that is not contained in `mapOfComponents` and which is less significant than `unit` will be set to its <<cypher-temporal-accessing-components-temporal-instants, minimal value>>.",
          "If `mapOfComponents` is not provided, all components of the returned value which are less significant than `unit` will be set to their default values.",
          "If `temporalInstantValue` is not provided, it will be set to the current time and timezone, i.e. `time.truncate(unit)` is equivalent of `time.truncate(unit, time())`.")
        query(
          """WITH time({hour:12, minute:31, second:14, nanosecond: 645876123, timezone: '-01:00'}) AS t
            |RETURN time.truncate('day', t) AS truncDay,
            |   time.truncate('hour', t) AS truncHour,
            |   time.truncate('minute', t) AS truncMinute,
            |   time.truncate('second', t) AS truncSecond,
            |   time.truncate('millisecond', t, {nanosecond:2}) AS truncMillisecond,
            |   time.truncate('microsecond', t) AS truncMicrosecond
            |""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "truncDay" -> TimeValue.parse("00:00-01:00", defaultZoneSupplier).asObjectCopy(),
              "truncHour" -> TimeValue.parse("12:00-01:00", defaultZoneSupplier).asObjectCopy(),
              "truncMinute" -> TimeValue.parse("12:31-01:00", defaultZoneSupplier).asObjectCopy(),
              "truncSecond" -> TimeValue.parse("12:31:14-01:00", defaultZoneSupplier).asObjectCopy(),
              "truncMillisecond" -> TimeValue.parse("12:31:14.645000002-01:00", defaultZoneSupplier).asObjectCopy(),
              "truncMicrosecond" -> TimeValue.parse("12:31:14.645876-01:00", defaultZoneSupplier).asObjectCopy()
            )))
          })) {
          resultTable()
        }
      }
    }
  }.build()
}
