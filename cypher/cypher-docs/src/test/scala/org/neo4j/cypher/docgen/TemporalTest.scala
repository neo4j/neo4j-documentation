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
package org.neo4j.cypher.docgen

import java.time.temporal.ChronoField
import java.time.{LocalDate, Period, ZoneId, ZoneOffset}
import java.util.function.Supplier

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, NoAssertions, ResultAssertions}
import org.neo4j.values.storable._

class TemporalTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    val defaultZoneSupplier: Supplier[ZoneId] = new Supplier[ZoneId] {
      override def get(): ZoneId = ZoneOffset.UTC
    }

    doc("Temporal (Date/Time) values", "cypher-temporal")
    synopsis("Cypher has built-in support for handling temporal values, and the underlying database supports storing these temporal values as properties on nodes and relationships.")
    p(
      """* <<cypher-temporal-introduction, Introduction>>
        |* <<cypher-temporal-timezones, Time zones>>
        |* <<cypher-temporal-instants, Temporal instants>>
        | ** <<cypher-temporal-specifying-temporal-instants, Specifying temporal instants>>
        |  *** <<cypher-temporal-specify-date, Specifying dates>>
        |  *** <<cypher-temporal-specify-time, Specifying times>>
        |  *** <<cypher-temporal-specify-time-zone, Specifying time zones>>
        |  *** <<cypher-temporal-specify-instant-examples, Examples>>
        | ** <<cypher-temporal-accessing-components-temporal-instants, Accessing components of temporal instants>>
        |* <<cypher-temporal-durations, Durations>>
        | ** <<cypher-temporal-specifying-durations, Specifying durations>>
        |  *** <<cypher-temporal-specify-duration-examples, Examples>>
        | ** <<cypher-temporal-accessing-components-durations, Accessing components of durations>>
        |* <<cypher-temporal-examples, Examples>>
        |* <<cypher-temporal-index, Temporal indexing>>
      """.stripMargin)
    note {
      p("""Refer to <<query-functions-temporal>> for information regarding temporal _functions_ allowing for the creation and manipulation of temporal values.""")
      p("""Refer to <<query-operators-temporal>> for information regarding temporal _operators_.""")
      p("""Refer to <<cypher-ordering>> for information regarding the comparison and ordering of temporal values.""")
    }
    section("Introduction", "cypher-temporal-introduction") {
      p(
        """The following table depicts the temporal value types and supported components:
          |
          |[options="header", cols="^,^,^,^", width="85%"]
          ||===
          || Type | Date support | Time support | Time zone support
          || Date | X  | |
          || Time | | X | X
          || LocalTime | | X |
          || DateTime |  X | X | X
          || LocalDateTime | X | X |
          || Duration | - | - | -
          ||===
          |
          |""")
      p(
        """_Date_, _Time_, _LocalTime_, _DateTime_ and _LocalDateTime_ are _temporal instant_ types.
          |A temporal instant value expresses a point in time with varying degrees of precision.
        """.stripMargin)
      p(
        """By contrast, _Duration_ is not a temporal instant type.
          |A _Duration_ represents a temporal amount, capturing the difference in time between two instants, and can be negative.
          |Duration only captures the amount of time between two instants, and thus does not encapsulate a start time and end time.""".stripMargin)
    }
    section("Time zones", "cypher-temporal-timezones") {
      p(
        """Time zones are represented either as an offset from UTC, or as a logical identifier of a _named time zone_ (these are based on the https://www.iana.org/time-zones[IANA time zone database]).
          |In either case the time is stored as UTC internally, and the time zone offset is only applied when the time is presented.
          |This means that temporal instants can be ordered without taking time zone into account.
          |If, however, two times are identical in UTC, then they are ordered by timezone.""".stripMargin)
      p(
        """When creating a time using a named time zone, the offset from UTC is computed from the rules in the time zone database to create a time instant in UTC, and to ensure the named time zone is a valid one.
        """.stripMargin)
      p(
        """It is possible for time zone rules to change in the IANA time zone database.
          |For example, there could be alterations to the rules for daylight savings time in a certain area.
          |If this occurs after the creation of a temporal instant, the presented time could differ from the originally-entered time, insofar as the local timezone is concerned.
          |However, the absolute time in UTC would remain the same.""".stripMargin)
      p(
        """There are three ways of specifying a time zone in Cypher:
          |
          |* Specifying the offset from UTC in hours and minutes (https://en.wikipedia.org/wiki/ISO_8601[ISO 8601])
          |* Specifying a named time zone
          |* Specifying both the offset and the time zone name (with the requirement that these match)
        """.stripMargin)
      p(
        """The named time zone form uses the rules of the IANA time zone database to manage _daylight savings time_ (DST).""".stripMargin)
      p(
        """The default time zone of the database can be configured using the configuration option <<operations-manual#config_db.temporal.timezone, `db.temporal.timezone`>>.
          |This configuration option influences the creation of temporal types for the following functions:
          |
          |* Getting the current date and time without specifying a time zone.
          |* Creating a temporal type from its components without specifying a time zone.
          |* Creating a temporal type by parsing a string without specifying a time zone.
          |* Creating a temporal type by combining or selecting values that do not have a time zone component, and without specifying a time zone.
          |* Truncating a temporal value that does not have a time zone component, and without specifying a time zone.""".stripMargin)
    }
    section("Temporal instants", "cypher-temporal-instants") {
      section("Specifying temporal instants", "cypher-temporal-specifying-temporal-instants") {
        p(
          """A temporal instant consists of three parts; the `date`, the `time`, and the `timezone`.
            |These parts may then be combined to produce the various temporal value types.
            |Literal characters are denoted in **`bold`**.
            |
            |[options="header", width="85%"]
            ||===
            || Temporal instant type | Composition of parts
            || _Date_ | `<date>`
            || _Time_ | `<time><timezone>` or **`T`**`<time><timezone>`
            || _LocalTime_ | `<time>` or **`T`**`<time>`
            || _DateTime_* | `<date>`**`T`**`<time><timezone>`
            || _LocalDateTime_* | `<date>`**`T`**`<time>`
            ||===
            |
            |*When `date` and `time` are combined, `date` must be complete; i.e. fully identify a particular day.
            |""")
        section("Specifying dates", "cypher-temporal-specify-date") {
          p(
            """
              |
              |[options="header", width="85%"]
              ||===
              || Component | Format | Description
              || Year  | `YYYY` | Specified with at least four digits (<<cypher-temporal-year, special rules apply in certain cases>>)
              || Month |  `MM`  | Specified with a double digit number from `01` to `12`
              || Week  | `ww`   | Always prefixed with **`W`** and specified with a double digit number from `01` to `53`
              || Quarter | `q`  | Always prefixed with **`Q`** and specified with a single digit number from `1` to `4`
              || Day of the month | `DD` | Specified with a double digit number from `01` to `31`
              || Day of the week |  `D` |  Specified with a single digit number from `1` to `7`
              || Day of the quarter | `DD` | Specified with a double digit number from `01` to `92`
              || Ordinal day of the year | `DDD` | Specified with a triple digit number from `001` to `366`
              ||===
              |
              |""")
          p(
            """
              |[[cypher-temporal-year]]
              |
              |If the year is before `0000` or after `9999`, the following additional rules apply:
              |
              |* **`-`** must prefix any year before `0000`
              |* **`+`** must prefix any year after `9999`
              |* The year must be separated from the next component with the following characters:
              | ** **`-`** if the next component is month or day of the year
              | ** Either **`-`** or **`W`** if the next component is week of the year
              | ** **`Q`** if the next component is quarter of the year
              |
              |If the year component is prefixed with either `-` or `+`, and is separated from the next component, `Year` is allowed to contain up to nine digits.
              |Thus, the allowed range of years is between -999,999,999 and +999,999,999.
              |For all other cases, i.e. the year is between `0000` and `9999` (inclusive), `Year` must have exactly four digits (the year component is interpreted as a year of the Common Era (CE)).
            """.stripMargin)
          p(
            """The following formats are supported for specifying dates:
              |
              |[options="header", width="85%"]
              ||===
              || Format | Description | Example | Interpretation of example
              || `YYYY-MM-DD`  | Calendar date: `Year-Month-Day` | `2015-07-21` | `2015-07-21`
              || `YYYYMMDD`   | Calendar date: `Year-Month-Day`  | `20150721` |  `2015-07-21`
              || `YYYY-MM`  | Calendar date: `Year-Month`     | `2015-07` |  `2015-07-01`
              || `YYYYMM`  | Calendar date: `Year-Month`      | `201507` |  `2015-07-01`
              || `YYYY-`**`W`**`ww-D` | Week date: `Year-Week-Day` |  `2015-W30-2` | `2015-07-21`
              || `YYYY`**`W`**`wwD`   | Week date: `Year-Week-Day` | `2015W302` | `2015-07-21`
              || `YYYY-`**`W`**`ww`   | Week date: `Year-Week`    | `2015-W30` | `2015-07-20`
              || `YYYY`**`W`**`ww`    | Week date: `Year-Week`    | `2015W30`  | `2015-07-20`
              || `YYYY-`**`Q`**`q-DD` | Quarter date: `Year-Quarter-Day` | `2015-Q2-60` | `2015-05-30`
              || `YYYY`**`Q`**`qDD`   | Quarter date: `Year-Quarter-Day` | `2015Q260`  | `2015-05-30`
              || `YYYY-`**`Q`**`q`            | Quarter date: `Year-Quarter`     | `2015-Q2`   | `2015-04-01`
              || `YYYY`**`Q`**`q`     | Quarter date: `Year-Quarter`   | `2015Q2` | `2015-04-01`
              || `YYYY-DDD`         | Ordinal date: `Year-Day`   | `2015-202` | `2015-07-21`
              || `YYYYDDD`          | Ordinal date: `Year-Day`   | `2015202`  | `2015-07-21`
              || `YYYY`     | Year | `2015` |  `2015-01-01`
              ||===
              |
              |""")
          p(
            """The least significant components can be omitted.
              |Cypher will assume omitted components to have their lowest possible value.
              |For example, `2013-06` will be interpreted as being the same date as `2013-06-01`.""")
        }
        section("Specifying times", "cypher-temporal-specify-time") {
          p(
            """
              |
              |[options="header", width="85%"]
              ||===
              || Component | Format | Description
              || `Hour`  | `HH` | Specified with a double digit number from `00` to `23`
              || `Minute` | `MM` | Specified with a double digit number from `00` to `59`
              || `Second` | `SS` | Specified with a double digit number from `00` to `59`
              || `fraction` | `sssssssss` | Specified with a number from `0` to `999999999`. It is not required to specify trailing zeros.
              |  `fraction` is an optional, sub-second component of `Second`.
              |This can be separated from `Second` using either a full stop (`.`) or a comma (`,`).
              |The `fraction` is in addition to the two digits of `Second`.
              ||===
              |
              |
              |Cypher does not support leap seconds; https://www.cl.cam.ac.uk/~mgk25/time/utc-sls/[UTC-SLS] (_UTC with Smoothed Leap Seconds_) is used to manage the difference in time between UTC and TAI (_International Atomic Time_).
              |""")
          p(
            """The following formats are supported for specifying times:
              |
              |[options="header", width="85%"]
              ||===
              || Format | Description | Example | Interpretation of example
              || `HH:MM:SS.sssssssss`  | `Hour:Minute:Second.fraction` | `21:40:32.142` | `21:40:32.142`
              || `HHMMSS.sssssssss`  | `Hour:Minute:Second.fraction` | `214032.142` | `21:40:32.142`
              || `HH:MM:SS`  | `Hour:Minute:Second` | `21:40:32` | `21:40:32.000`
              || `HHMMSS`   | `Hour:Minute:Second` | `214032` | `21:40:32.000`
              || `HH:MM` | `Hour:Minute` | `21:40` | `21:40:00.000`
              || `HHMM`  | `Hour:Minute` | `2140` | `21:40:00.000`
              || `HH`   | `Hour` | `21` | `21:00:00.000`
              ||===
              |
              |""")
          p(
            """The least significant components can be omitted.
              |For example, a time may be specified with `Hour` and `Minute`, leaving out `Second` and `fraction`.
              |On the other hand, specifying a time with `Hour` and `Second`, while leaving out `Minute`, is not possible.
            """.stripMargin)
        }
        section("Specifying time zones", "cypher-temporal-specify-time-zone") {
          p(
            """The time zone is specified in one of the following ways:
              |
              |* As an offset from UTC
              |* Using the **`Z`** shorthand for the UTC (`±00:00`) time zone
              |
          """.stripMargin)
          p(
            """
            |When specifying a time zone as an offset from UTC, the rules below apply:
            |
            |* The time zone always starts with either a plus (`+`) or minus (`-`) sign.
            | ** Positive offsets, i.e. time zones beginning with `+`, denote time zones east of UTC.
            | ** Negative offsets, i.e. time zones beginning with `-`, denote time zones west of UTC.

            |* A double-digit hour offset follows the `+`/`-` sign.
            |* An optional double-digit minute offset follows the hour offset, optionally separated by a colon (`:`).
            |
            |* The time zone of the International Date Line is denoted either by `+12:00` or `-12:00`, depending on country.
          """.stripMargin)
          p(
            """When creating values of the _DateTime_ temporal instant type, the time zone may also be specified using a named time zone, using the names from the IANA time zone database.
              |This may be provided either in addition to, or in place of the offset.
              |The named time zone is given last and is enclosed in square brackets (`[]`).
              |Should both the offset and the named time zone be provided, the offset must match the named time zone.
              |
          """.stripMargin)
          p(
            """The following formats are supported for specifying time zones:
              |
              |[options="header", width="85%"]
              ||===
              || Format | Description | Example | Supported for `DateTime` | Supported for `Time`
              || **`Z`** | UTC | `Z` | X | X
              || `±HH:MM` | `Hour:Minute` | `+09:30` | X | X
              || `±HH:MM[ZoneName]` | `Hour:Minute[ZoneName]` | `+08:45[Australia/Eucla]` | X |
              || `±HHMM` | `Hour:Minute` | `+0100` | X | X
              || `±HHMM[ZoneName]` | `Hour:Minute[ZoneName]` | `+0200[Africa/Johannesburg]` | X |
              || `±HH` | `Hour` | `-08` | X | X
              || `±HH[ZoneName]` | `Hour[ZoneName]` | `+08[Asia/Singapore]` | X |
              || `[ZoneName]` | `[ZoneName]` | `[America/Regina]` | X |
              ||===
              |
              |""")
        }
        section("Examples", "cypher-temporal-specify-instant-examples") {
          p(
            """We show below examples of parsing temporal instant values using various formats.
              |For more details, refer to <<functions-temporal-create-overview>>.
            """.stripMargin)
          p("Parsing a _DateTime_ using the _calendar date_ format:")
          query(
            """RETURN datetime('2015-06-24T12:50:35.556+0100') AS theDateTime""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDateTime" -> DateTimeValue.parse("2015-06-24T12:50:35.556+0100", defaultZoneSupplier).asObjectCopy())))
            })) {
            resultTable()
          }
          p("Parsing a _LocalDateTime_ using the _ordinal date_ format:")
          query(
            """RETURN localdatetime('2015185T19:32:24') as theLocalDateTime""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theLocalDateTime" -> LocalDateTimeValue.parse("2015185T19:32:24").asObjectCopy())))
            })) {
            resultTable()
          }
          p("Parsing a _Date_ using the _week date_ format:")
          query(
            """RETURN date('+2015-W13-4') AS theDate""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDate" -> DateValue.parse("+2015-W13-4").asObjectCopy())))
            })) {
            resultTable()
          }
          p("Parsing a _Time_:")
          query(
            """RETURN time('125035.556+0100') AS theTime""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theTime" -> TimeValue.parse("125035.556+0100", defaultZoneSupplier).asObjectCopy())))
            })) {
            resultTable()
          }
          p("Parsing a _LocalTime_:")
          query(
            """RETURN localtime('12:50:35.556') AS theLocalTime""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theLocalTime" -> LocalTimeValue.parse("12:50:35.556").asObjectCopy())))
            })) {
            resultTable()
          }
        }
      }
      section("Accessing components of temporal instants", "cypher-temporal-accessing-components-temporal-instants") {
        p("Components of temporal instant values can be accessed as properties.")
        p(
          """
            |.Components of temporal instant values and where they are supported
            |[options="header"]
            ||===
            || Component      | Description  | Type | Range/Format   | Date | DateTime | LocalDateTime | Time | LocalTime
            || `instant.year` | The `year` component represents the https://en.wikipedia.org/wiki/Astronomical_year_numbering[astronomical year number] of the instant footnote:[This is in accordance with the https://en.wikipedia.org/wiki/Gregorian_calendar[Gregorian calendar]; i.e. years AD/CE start at year 1, and the year before that (year 1 BC/BCE) is 0, while year 2 BCE is -1 etc.] | Integer | At least 4 digits. For more information, see the <<cypher-temporal-year, rules for using the `Year` component>> | X | X | X |  |
            || `instant.quarter` |  The _quarter-of-the-year_ component | Integer | `1` to `4` | X | X | X |  |
            || `instant.month` | The _month-of-the-year_ component | Integer | `1` to `12` | X | X | X |  |
            || `instant.week` | The _week-of-the-year_ component footnote:[The https://en.wikipedia.org/wiki/ISO_week_date#First_week[first week of any year] is the week that contains the first Thursday of the year, and thus always contains January 4.] | Integer | `1` to `53` | X | X | X |  |
            || `instant.weekYear` | The _year_ that the _week-of-year_ component belongs to footnote:[For dates from December 29, this could be the next year, and for dates until January 3 this could be the previous year, depending on how week 1 begins.] | Integer | At least 4 digits. For more information, see the <<cypher-temporal-year, rules for using the `Year` component>> | X | X | X |  |
            || `instant.dayOfQuarter` | The _day-of-the-quarter_ component  | Integer | `1` to `92` | X | X | X |  |
            || `instant.day` |  The _day-of-the-month_ component | Integer | `1` to `31` | X | X | X |  |
            || `instant.ordinalDay` | The _day-of-the-year_ component  | Integer | `1` to `366` | X | X | X |  |
            || `instant.dayOfWeek` | The _day-of-the-week_ component (the first day of the week is _Monday_) | Integer | `1` to `7` | X | X | X  | |
            || `instant.hour` |  The _hour_ component  | Integer | `0` to `23` |   | X  | X | X | X
            || `instant.minute` | The _minute_ component | Integer | `0` to `59` |  | X | X  | X | X
            || `instant.second` | The _second_ component | Integer | `0` to `60` |  | X | X  | X | X
            || `instant.millisecond` |  The _millisecond_ component | Integer  | `0` to `999` |  | X | X | X | X
            || `instant.microsecond` | The _microsecond_ component  | Integer | `0` to `999999` |  | X | X  | X | X
            || `instant.nanosecond` | The _nanosecond_ component | Integer | `0` to `999999999` |  | X | X | X | X
            || `instant.timezone` | The _timezone_ component | String | Depending on how the <<cypher-temporal-specify-time-zone, time zone was specified>>, this is either a time zone name or an offset from UTC in the format `±HHMM` |  | X |   | X |
            || `instant.offset` | The _timezone_ offset | String  | `±HHMM` |  | X |  | X |
            || `instant.offsetMinutes` | The _timezone_ offset in minutes | Integer | `-1080` to `+1080` |  | X |  | X |
            || `instant.offsetSeconds` | The _timezone_ offset in seconds | Integer | `-64800` to `+64800` |  | X |  | X |
            || `instant.epochMillis` | The number of milliseconds between `1970-01-01T00:00:00+0000` and the instant footnote:[`datetime().epochMillis` returns the equivalent value of the <<functions-timestamp, `timestamp()`>> function.] | Integer | Positive for instants after and negative for instants before `1970-01-01T00:00:00+0000` |  | X |   | |
            || `instant.epochSeconds` | The number of seconds between `1970-01-01T00:00:00+0000` and the instant footnote:[For the _nanosecond_ part of the _epoch_ offset, the regular _nanosecond_ component (`instant.nanosecond`) can be used.] | Integer | Positive for instants after and negative for instants before `1970-01-01T00:00:00+0000` |  | X |  |   | |
            ||===
            |
            |""")
        p("The following query shows how to extract the components of a _Date_ value:")
        query(
          """WITH date({year:1984, month:10, day:11}) As d
            |RETURN d.year, d.quarter, d.month, d.week, d.weekYear, d.day, d.ordinalDay, d.dayOfWeek, d.dayOfQuarter""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "d.year" -> 1984,
              "d.weekYear" -> 1984,
              "d.quarter" -> 4,
              "d.month" -> 10,
              "d.week" -> 41,
              "d.day" -> 11,
              "d.ordinalDay" -> 285,
              "d.dayOfWeek" -> 4,
              "d.dayOfQuarter" -> 11
            )))
          })) {
          resultTable()
        }
        p("The following query shows how to extract the components of a _DateTime_ value:")
        query(
          """WITH datetime({year:1984, month:11, day:11, hour:12, minute:31, second:14, nanosecond: 645876123, timezone:'Europe/Stockholm'}) as d
            |RETURN d.year, d.quarter, d.month, d.week, d.weekYear, d.day, d.ordinalDay, d.dayOfWeek, d.dayOfQuarter,
            |   d.hour, d.minute, d.second, d.millisecond, d.microsecond, d.nanosecond,
            |   d.timezone, d.offset, d.offsetMinutes, d.epochSeconds, d.epochMillis""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "d.second" -> 14,
              "d.hour" -> 12,
              "d.microsecond" -> 645876,
              "d.weekYear" -> 1984,
              "d.offsetMinutes" -> 60,
              "d.quarter" -> 4,
              "d.year" -> 1984,
              "d.week" -> 45,
              "d.offset" -> "+01:00",
              "d.day" -> 11,
              "d.millisecond" -> 645,
              "d.minute" -> 31,
              "d.dayOfWeek" -> 7,
              "d.timezone" -> "Europe/Stockholm",
              "d.dayOfQuarter" -> 42,
              "d.epochSeconds" -> 469020674,
              "d.ordinalDay" -> 316,
              "d.epochMillis" -> 469020674645L,
              "d.month" -> 11,
              "d.nanosecond" -> 645876123
            )))
          })) {
          resultTable()
        }
      }
    }
    section("Durations", "cypher-temporal-durations") {
      section("Specifying durations", "cypher-temporal-specifying-durations") {
        p("""A _Duration_ represents a temporal amount, capturing the difference in time between two instants, and can be negative.""")
        p(
          """The specification of a _Duration_ is prefixed with a **`P`**, and can use either a _unit-based form_ or a _date-and-time-based form_:
            |
            |* Unit-based form: **`P`**`[n`**`Y`**`][n`**`M`**`][n`**`W`**`][n`**`D`**`][`**`T`**`[n`**`H`**`][n`**`M`**`][n`**`S`**`]]`
            | ** The square brackets (`[]`) denote an optional component (components with a zero value may be omitted).
            | ** The `n` denotes a numeric value which can be arbitrarily large.
            | ** The value of the last -- and least significant -- component may contain a decimal fraction.
            | ** Each component must be suffixed by a component identifier denoting the unit.
            | ** The unit-based form uses **`M`** as a suffix for both months and minutes. Therefore, time parts must always be preceded with **`T`**, even when no components of the date part are given.
            |* Date-and-time-based form: **`P`**`<date>`**`T`**`<time>`
            | ** Unlike the unit-based form, this form requires each component to be within the bounds of a valid _LocalDateTime_.
            |""".stripMargin)
        p(
          """The following table lists the component identifiers for the unit-based form:
            |
            |[[cypher-temporal-duration-component]]
            |
            |[options="header", width="85%"]
            ||===
            || Component identifier | Description | Comments
            || **`Y`** | Years |
            || **`M`** | Months | Must be specified before **`T`**
            || **`W`** | Weeks |
            || **`D`** | Days |
            || **`H`** | Hours |
            || **`M`** | Minutes | Must be specified after **`T`**
            || **`S`** | Seconds |
            ||===
            |
            |""")
        section("Examples", "cypher-temporal-specify-duration-examples") {
          p(
            """The following examples demonstrate various methods of parsing _Duration_ values.
              |For more details, refer to <<functions-duration-create-string>>.
            """.stripMargin)
          p("Return a _Duration_ of `14` _days_, `16` _hours_ and `12` _minutes_:")
          query(
            """RETURN duration('P14DT16H12M') AS theDuration""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P14DT16H12M"))))
            })) {
            resultTable()
          }
          p("Return a _Duration_ of `5` _months_, `1` _day_ and `12` _hours_:")
          query(
            """RETURN duration('P5M1.5D') AS theDuration""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P5M1.5D"))))
            })) {
            resultTable()
          }
          p("Return a _Duration_ of `45` seconds:")
          query(
            """RETURN duration('PT0.75M') AS theDuration""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDuration" -> DurationValue.parse("PT0.75M"))))
            })) {
            resultTable()
          }
          p("Return a _Duration_ of `2` _weeks_, `3` _days_ and `12` _hours_:")
          query(
            """RETURN duration('P2.5W') AS theDuration""".stripMargin, ResultAssertions((r) => {
              r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P2.5W"))))
            })) {
            resultTable()
          }
        }
      }
      section("Accessing components of durations", "cypher-temporal-accessing-components-durations") {
        p(
          """A _Duration_ can have several components.
            |These are categorized into the following groups:
            |
            |[options="header"]
            ||===
            || Component group | Constituent components
            || Months | _Years_, _Quarters_ and _Months_
            || Days | _Weeks_ and _Days_
            || Seconds | _Hours_, _Minutes_, _Seconds_, _Milliseconds_, _Microseconds_ and _Nanoseconds_
            ||===
            |
            |
          """)
        p(
          """Within each group, the components can be converted without any loss:
            |
            |* There are always `4` _quarters_ in `1` _year_.
            |* There are always `12` _months_ in `1` _year_.
            |* There are always `3` _months_ in `1` _quarter_.
            |* There are always `7` _days_ in `1` _week_.
            |* There are always `60` _minutes_ in `1` _hour_.
            |* There are always `60` _seconds_ in `1` _minute_ (Cypher uses https://www.cl.cam.ac.uk/~mgk25/time/utc-sls/[UTC-SLS] when handling leap seconds).
            |* There are always `1000` _milliseconds_ in `1` _second_.
            |* There are always `1000` _microseconds_ in `1` _millisecond_.
            |* There are always `1000` _nanoseconds_ in `1` _microsecond_.
            |
            |Please note that:
            |
            |* There are not always `24` _hours_ in `1` _day_; when switching to/from daylight savings time, a _day_ can have `23` or `25` _hours_.
            |* There are not always the same number of _days_ in a _month_.
            |* Due to leap years, there are not always the same number of _days_ in a _year_.
            |
          """.stripMargin)
        p(
          """
            |.Components of _Duration_ values and how they are truncated within their component group
            |[options="header"]
            ||===
            || Component      | Component Group | Description | Type | Details
            || `duration.years` | Months | The total number of _years_ | Integer | Each set of `4` _quarters_ is counted as `1` _year_; each set of `12` _months_ is counted as `1` _year_.
            || `duration.months` | Months | The total number of _months_ | Integer | Each _year_ is counted as `12` _months_; each _quarter_ is counted as `3` _months_.
            || `duration.days` | Days | The total number of _days_ | Integer | Each _week_ is counted as `7` _days_.
            || `duration.hours` | Seconds | The total number of _hours_ | Integer | Each set of `60` _minutes_ is counted as `1` _hour_; each set of `3600` _seconds_ is counted as `1` _hour_.
            || `duration.minutes` | Seconds | The total number of _minutes_ | Integer | Each _hour_ is counted as `60` _minutes_; each set of `60` _seconds_ is counted as `1` _minute_.
            || `duration.seconds` | Seconds | The total number of _seconds_ | Integer | Each _hour_ is counted as `3600` _seconds_; each _minute_ is counted as `60` _seconds_.
            || `duration.milliseconds` | Seconds | The total number of _milliseconds_ | Integer |
            || `duration.microseconds` | Seconds | The total number of _microseconds_ | Integer |
            || `duration.nanoseconds` | Seconds | The total number of _nanoseconds_ | Integer |
            ||===
            |
            |""")
        p(
          """It is also possible to access the smaller (less significant) components of a component group bounded by the largest (most significant) component of the group:
            |
            |[options="header"]
            ||===
            || Component      | Component Group | Description | Type
            || `duration.monthsOfYear` | Months | The number of _months_ in the group that do not make a whole _year_ | Integer
            || `duration.minutesOfHour` | Seconds | The total number of _minutes_ in the group that do not make a whole _hour_ | Integer
            || `duration.secondsOfMinute` | Seconds | The total number of _seconds_ in the group that do not make a whole _minute_ | Integer
            || `duration.millisecondsOfSecond` | Seconds | The total number of _milliseconds_ in the group that do not make a whole _second_ | Integer
            || `duration.microsecondsOfSecond` | Seconds | The total number of _microseconds_ in the group that do not make a whole _second_ | Integer
            || `duration.nanosecondsOfSecond` | Seconds | The total number of _nanoseconds_ in the group that do not make a whole _second_ | Integer
            ||===
            |
            |""")
        p("The following query shows how to extract the components of a _Duration_ value:")
        query(
          """WITH duration({years: 1, months:4, days: 111, hours: 1, minutes: 1, seconds: 1, nanoseconds: 111111111}) AS d
            |RETURN d.years, d.months, d.monthsOfYear, d.days, d.hours,
            |   d.minutes, d.minutesOfHour, d.seconds, d.secondsOfMinute, d.milliseconds, d.millisecondsOfSecond, d.microseconds,
            |   d.microsecondsOfSecond, d.nanoseconds, d.nanosecondsOfSecond""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map(
              "d.nanosecondsOfSecond" -> 111111111,
              "d.milliseconds" -> 3661111,
              "d.secondsOfMinute" -> 1,
              "d.millisecondsOfSecond" -> 111,
              "d.minutesOfHour" -> 1,
              "d.days" -> 111,
              "d.microseconds" -> 3661111111L,
              "d.nanoseconds" -> 3661111111111L,
              "d.microsecondsOfSecond" -> 111111,
              "d.years" -> 1,
              "d.months" -> 16,
              "d.minutes" -> 61,
              "d.hours" -> 1,
              "d.seconds" -> 3661,
              "d.monthsOfYear" -> 4
            )))

          })) {
          resultTable()
        }
      }
    }
    section("Examples", "cypher-temporal-examples") {
      p(
        """The following examples illustrate the use of some of the temporal functions and operators.
          |Refer to <<query-functions-temporal>> and <<query-operators-temporal>> for more details.
        """.stripMargin)
      p("Create a _Duration_ representing 1.5 _days_:")
      query(
        """RETURN duration({days: 1, hours: 12}) AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P1DT12H"))))
        })) {
        resultTable()
      }
      p("Compute the _Duration_ between two temporal instants:")
      query(
        """RETURN duration.between(date('1984-10-11'), date('2015-06-24')) AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P30Y8M13D"))))
        })) {
        resultTable()
      }
      p("Compute the number of days between two _Date_ values:")
      query(
        """RETURN duration.inDays(date('2014-10-11'), date('2015-08-06')) AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P299D"))))
        })) {
        resultTable()
      }
      p("Get the _Date_ of Thursday in the current week:")
      query(
        """RETURN date.truncate('week', date(), {dayOfWeek: 4}) as thursday""".stripMargin, ResultAssertions((r) => {
          r.toList.head("thursday").asInstanceOf[LocalDate].get(ChronoField.DAY_OF_WEEK) should equal(4)
        })) {
        resultTable()
      }
      p("Get the _Date_ of the last day of the next month:")
      query(
        """RETURN date.truncate('month', date() + duration('P2M')) - duration('P1D') AS lastDay""".stripMargin, ResultAssertions((r) => {
          r.toList.head("lastDay").asInstanceOf[LocalDate].plus(Period.ofDays(1)).get(ChronoField.DAY_OF_MONTH) should equal(1)
        })) {
        resultTable()
      }
      p("Add a _Duration_ to a _Date_:")
      query(
        """RETURN time('13:42:19') + duration({days: 1, hours: 12}) AS theTime""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theTime" -> TimeValue.parse("01:42:19", defaultZoneSupplier).asObjectCopy())))
        })) {
        resultTable()
      }
      p("Add two _Duration_ values:")
      query(
        """RETURN duration({days: 2, hours: 7}) + duration({months: 1, hours: 18}) AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("P1M2DT25H"))))
        })) {
        resultTable()
      }
      p("Multiply a _Duration_ by a number:")
      query(
        """RETURN duration({hours: 5, minutes: 21}) * 14 AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("PT74H54M"))))
        })) {
        resultTable()
      }
      p("Divide a _Duration_ by a number:")
      query(
        """RETURN duration({hours: 3, minutes: 16}) / 2 AS theDuration""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("theDuration" -> DurationValue.parse("PT1H38M"))))
        })) {
        resultTable()
      }
      p("Examine whether two instants are less than one day apart:")
      query(
        """WITH datetime('2015-07-21T21:40:32.142+0100') AS date1, datetime('2015-07-21T17:12:56.333+0100') AS date2
          |RETURN
          |   CASE WHEN date1 < date2
          |      THEN date1 + duration("P1D") > date2
          |   ELSE
          |      date2 + duration("P1D") > date1
          |   END
          |   AS lessThanOneDayApart
          |""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("lessThanOneDayApart" -> true)))
        })) {
        resultTable()
      }
      p("Return the abbreviated name of the current month:")
      query(
        """RETURN ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][date().month-1] AS month""".stripMargin, NoAssertions) {
        resultTable()
      }
    }
    section("Temporal indexing", "cypher-temporal-index") {
      p(
        """All temporal types can be indexed, and thereby support exact lookups for equality predicates.
           Indexes for temporal instant types additionally support range lookups.
        """.stripMargin)
    }
  }.build()
}
