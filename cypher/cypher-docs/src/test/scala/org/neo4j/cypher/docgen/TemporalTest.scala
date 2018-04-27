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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class TemporalTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
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
        | ** <<cypher-temporal-accessing-components-temporal-instants, Accessing components of temporal instants>>
        |* <<cypher-temporal-durations, Durations>>
        | ** <<cypher-temporal-specifying-durations, Specifying durations>>
        | ** <<cypher-temporal-accessing-components-durations, Accessing components of durations>>
      """.stripMargin)
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
          |A temporal instant value expresses a point in time -- an instant -- with varying degrees of precision.
        """.stripMargin)
      p(
        """By contrast, _Duration_ is not a temporal instant type.
          |A _Duration_ represents a temporal amount, capturing the difference in time between two instants, and can be negative.""".stripMargin)
      important {
        p("Duration only captures the amount of time between two instants, and thus does not encapsulate a start time and end time.")
      }
    }
    section("Time zones", "cypher-temporal-timezones") {
      p(
        """Time zones are represented in one of two ways: either (i) as an offset from UTC, or (ii) as a logical identifier of a _named time zone_ (these are based on the https://www.iana.org/time-zones[IANA time zone database]).
          |In either case the time is stored as UTC internally, and the time zone offset is only applied when the time is presented.
          |This means that temporal instants can be ordered without taking time zone into account.""".stripMargin)
      p(
        """When creating a time using a named time zone, the offset from UTC is computed from the rules in the time zone database to create a time instant in UTC, and to ensure the named time zone is a valid one.
        """.stripMargin)
      p(
        """If the rules for a time zone changes in the time zone database after the creation of a temporal instant -- for example, alterations to the rules for daylight savings time in a certain area -- the presented time could differ from the originally-entered time insofar as the local timezone is concerned.
          |However, the absolute time in UTC would remain the same.""".stripMargin)
      p(
        """There are three ways of specifying a time zone in Cypher:
          |
          |* Specifying the offset from UTC in hours and minutes (https://en.wikipedia.org/wiki/ISO_8601[ISO 8601])
          |* Specifying a named time zone
          |* Specifying both the offset and the time zone name (with the requirement that these match)
        """.stripMargin)
      p(
        """The named time zone form uses the time zone rules of the time zone database to manage _daylight savings time_ (DST).""".stripMargin)
    }
    section("Temporal instants", "cypher-temporal-instants") {
      section("Specifying temporal instants", "cypher-temporal-specifying-temporal-instants") {
        p(
          """A temporal instant consists of three parts: the `date`, the `time` and the `timezone`.
            |These parts may then be combined to produce the various temporal value types (literal characters are denoted **`thus`**):
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
              || Year  | `YYYY` | Specified with at least four digits (special rules apply in certain cases DOCS-TODO-DAGGER-SYMBOL)
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
              |DOCS-TODO-DAGGER-SYMBOL If the year is before `0000` or after `9999`, the following additional rules apply:
              |
              |* **`-`** must prefix any year before `0000`
              |* **`+`** must prefix any year after `9999`
              |* The year must be separated from the next component with the following characters:
              | ** **`-`** if the next component is month or day of the year
              | ** Either **`-`** or **`W`** if the next component is week of the year
              | ** **`Q`** if the next component is quarter of the year
              |
              |If the year component is prefixed with either `-` or `+` (and is separated from the next component), `Year` is allowed to contain any number of digits.
              |For all other cases -- i.e. the year is between `0000` and `9999` (inclusive) - `Year` must have exactly four digits (the year component is interpreted as a year of the Common Era (CE)).
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
              || Hour  | `HH` | Specified with a double digit number from `00` to `23`
              || Minute | `MM` | Specified with a double digit number from `00` to `59`
              || Second | `SS` | Specified with a double digit number from `00` to `59`
              || fraction (Fractional second DOCS-TODO-SYMBOL) | `sss` | Specified with a triple digit number from `000` to `999`
              ||===
              |
              |DOCS-TODO `Second` may have a _fractional seconds_ component (`fraction`) representing a sub-second component.
              |This can be separated from `Second` using either a full stop (`.`) or a comma (`,`).
              |The decimal fraction is _in addition_ to the two digits of `Second`.
              |Cypher does not support leap seconds; https://www.cl.cam.ac.uk/~mgk25/time/utc-sls/[UTC-SLS] (_UTC with Smoothed Leap Seconds_) is used to manage the difference in time between UTC and TAI (_International Atomic Time_).
              |""")
          p(
            """The following formats are supported for specifying times:
              |
              |[options="header", width="85%"]
              ||===
              || Format | Description | Example | Interpretation of example
              || `HH:MM:SS.ss`  | `Hour:Minute:Second.fraction` | `21:40:32.142` | `21:40:32.142`
              || `HHMMSS.sss`  | `Hour:Minute:Second.fraction` | `214032.142` | `21:40:32.142`
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
              |On the other hand, specifying a time with `Hour` and `Second` -- leaving out `Minute` -- is not possible.
            """.stripMargin)
        }
        section("Specifying time zones", "cypher-temporal-specify-time-zone") {
          p(
            """The <<cypher-temporal-timezones, time zone>> is specified in one of the following ways:
              |
              |* As an offset from UTC
              |* Using the "**z**" shorthand for the UTC (`±00:00`) time zone
              |
          """.stripMargin)
          p(
            """
            |When specifying a time zone as an offset from UTC, the rules below apply:
            |
            |* The time zone always starts with either a plus (`+`) or minus (`-`) sign.
            | ** Positive offsets -- time zones beginning with `+` -- denote time zones east of UTC.
            | ** Negative offsets -- time zones beginning with `-` -- denote time zones west of UTC.

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
              || Format | Description | Example
              || **`Z`**^1,2^  | UTC | `Z` (UTC)
              || `±HH:MM`^1,2^ | `Hour:Minute` | `+09:30` (ACST)
              || `±HH:MM[ZoneName]`^1^ | `Hour:Minute[ZoneName]` | `+08:45[Australia/Eucla]` (CWST)
              || `±HHMM`^1,2^ | `Hour:Minute` | `+0100` (CET)
              || `±HHMM[ZoneName]`^1^ | `Hour:Minute[ZoneName]` | `+0200[Africa/Johannesburg]` (SAST)
              || `±HH`^1,2^ | `Hour` | `-08` (PST)
              || `±HH[ZoneName]`^1^ | `Hour[ZoneName]` | `+08[Asia/Singapore]` (SST)
              || `[ZoneName]`^1^  | `[ZoneName]` | `[America/Regina]` (CST)
              ||===
              |
              |^1^Supported for _DateTime_.
              |
              |^2^Supported for _Time_.
              |""")
        }
      }
      section("Accessing components of temporal instants", "cypher-temporal-accessing-components-temporal-instants") {
        p("Components of temporal instant values can be accessed as properties.")
        p(
          """
            |.Components of temporal instant values and where they are supported
            |[options="header"]
            ||===
            || Component      | Description  | Type | Range   | Date | DateTime | LocalDateTime | Time | LocalTime
            || `instant.year` | The `year` component represents the https://en.wikipedia.org/wiki/Astronomical_year_numbering[astronomical year number] of the instant DOCS-TODO-x | Integer | <<link to DOCS-TODO-DAGGER-SYMBOL>> | X | X | X |  |
            || `instant.quarter` |  The _quarter-of-the-year_ component | Integer | `1` to `4` | X | X | X |  |
            || `instant.month` | The _month-of-the-year_ component | Integer | `1` to `12` | X | X | X |  |
            || `instant.week` | The _week-of-the-year_ component DOCS-TODO-y | Integer | `1` to `53` | X | X | X |  |
            || `instant.weekYear` | The _year_ that the _week-of-year_ component belongs to DOCS-TODO-z | Integer | <<link to DOCS-TODO-DAGGER-SYMBOL>> | X | X | X |  |
            || `instant.day` |  The _day-of-the-month_ component | Integer | `1` to `31` | X | X | X |  |
            || `instant.ordinalDay` | The _day-of-the-year_ component  | Integer | `1` to `366` | X | X | X |  |
            || `instant.weekDay` | The _day-of-the-week_ component (the first day of the week is _Monday_) | Integer | `1` to `7` | X | X | X  | |
            || `instant.hour` |  The _hour_ component  | Integer | `0` to `23` |   | X  | X | X | X
            || `instant.minute` | The _minute_ component | Integer | `0` to `59` |  | X | X  | X | X
            || `instant.second` | The _second_ component | Integer | `0` to `60` |  | X | X  | X | X
            || `instant.millisecond` |  The _millisecond_ component | Integer  | `0` to `999` |  | X | X | X | X
            || `instant.microsecond` | The _microsecond_ component  | Integer | `0` to `999999` |  | X | X  | X | X
            || `instant.nanosecond` | The _nanosecond_ component | Integer | `0` to `999999999` |  | X | X | X | X
            || `instant.timezone` | The _timezone_ component | String | <<cypher-temporal-specify-time-zone, Depending on how the time zone was specified, this is either a time zone name or an offset from UTC in the format `±HHMM`>> |  | X |   | X |
            || `instant.offset` | The _timezone_ offset | String  | ` ±HHMM` |  | X |  | X |
            || `instant.offsetMinutes` | The _timezone_ offset in minutes | String CYPHER-TODO | `±00` to `±59` |  | X |  | X |
            || `instant.offsetSeconds` | The _timezone_ offset in seconds | String CYPHER-TODO | `±00` to `±60` |  | X |  | X |
            || `instant.epochMillis` | The number of milliseconds between `1970-01-01T00:00:00+0000` and the instant | Integer | Positive for instants after and negative for instants before `1970-01-01T00:00:00+0000` |  | X |   | |
            || `instant.epochSeconds` | The number of seconds between `1970-01-01T00:00:00+0000` and the instant DOCS-TODO-xx | Integer | Positive for instants after and negative for instants before `1970-01-01T00:00:00+0000` |  | X |  |   | |
            ||===
            |
            |DOCS-TODO-x: This is in accordance with the https://en.wikipedia.org/wiki/Gregorian_calendar[Gregorian calendar]; i.e. years AD/CE start at year 1, and the year before that (year 1 BC/BCE) is 0, while year 2 BCE is -1 etc.
            |
            |DOCS-TODO-y: The https://en.wikipedia.org/wiki/ISO_week_date#First_week[first week of any year] is the week that contains the first Thursday of the year, and thus always contains January 4.
            |
            |DOCS-TODO-z For dates from December 29, this could be the next year, and for dates until January 3 this could be the previous year, depending on how week 1 begins.
            |
            |DOCS-TODO-xx: For the _nanosecond_ part of the _epoch_ offset, the regular _nanosecond_ component (`instant.nanosecond`) can be used.
            |
            |""")
      }
    }
    section("Durations", "cypher-temporal-durations") {
      section("Specifying durations", "cypher-temporal-specifying-durations") {
        p(
          """A _Duration_ may be specified using either a _unit based form_ or a _date-and-time based form_:
            |
            |* Unit based form: **`P`**`[n`**`Y`**`][n`**`M`**`][n`**`W`**`][n`**`D`**`][`**`T`**`[n`**`H`**`][n`**`M`**`][n`**`S`**`]]`
            | ** The square brackets (`[]`) denote an optional component (components with a zero value may be omitted).
            | ** The `n` denotes a numeric value which can be arbitrarily large.
            | ** The value of the last -- and least significant -- component may contain a decimal fraction.
            | ** Each component must be suffixed by a component identifier denoting the unit.
            | ** As the unit based form uses **`M`** as a suffix for both months and minutes, time parts must always be preceded with **`T`**, even when no components of the date part are given.
            |* Date-and-time based form: **`P`**`<date>`**`T`**`<time>`
            | ** Unlike the unit based form, this form requires each component to be within the bounds of a valid _LocalDateTime_.
            |""".stripMargin)
        p("""Both formats mandates the prefix **`P`** (which stands for "period").""")
        p(
          """The following table lists the component identifiers for the unit based form:
            |
            |[options="header", width="85%"]
            ||===
            || Component identifier | Description
            || **`Y`** | Years
            || **`M`** (before the **`T`**) | Months
            || **`W`** | Weeks
            || **`D`** | Days
            || **`H`** | Hours
            || **`M`** (after the **`T`**) | Minutes
            || **`S`** | Seconds
            ||===
            |
            |""")
      }
    }
  }.build()
}
