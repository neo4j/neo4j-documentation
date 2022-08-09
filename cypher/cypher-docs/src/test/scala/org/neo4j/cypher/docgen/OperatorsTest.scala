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
import org.neo4j.values.storable.{DateValue, DurationValue, LocalDateTimeValue}

class OperatorsTest extends DocumentingTest with QueryStatisticsTestSupport {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Operators", "query-operators")
    synopsis("This section contains an overview of operators.")
    p(
      """* <<query-operators-summary, Operators at a glance>>
        |* <<query-operators-aggregation, Aggregation operators>>
        | ** <<syntax-using-the-distinct-operator, Using the `DISTINCT` operator>>
        |* <<query-operators-property, Property operators>>
        | ** <<syntax-accessing-the-property-of-a-node-or-relationship, Statically accessing a property of a node or relationship using the `.` operator>>
        | ** <<syntax-filtering-on-a-dynamically-computed-property-key, Filtering on a dynamically-computed property key using the `[]` operator>>
        | ** <<syntax-property-replacement-operator, Replacing all properties of a node or relationship using the `=` operator>>
        | ** <<syntax-property-mutation-operator, Mutating specific properties of a node or relationship using the `+=` operator>>
        |* <<query-operators-mathematical, Mathematical operators>>
        | ** <<syntax-using-the-exponentiation-operator, Using the exponentiation operator `^`>>
        | ** <<syntax-using-the-unary-minus-operator, Using the unary minus operator `-`>>
        |* <<query-operators-comparison, Comparison operators>>
        | ** <<syntax-comparing-two-numbers, Comparing two numbers>>
        | ** <<syntax-using-starts-with-to-filter-names, Using `STARTS WITH` to filter names>>
        | ** <<cypher-comparison, Equality and comparison of values>>
        | ** <<cypher-ordering, Ordering and comparison of values>>
        | ** <<cypher-operations-chaining, Chaining comparison operations>>
        | ** <<syntax-using-a-regular-expression-to-filter-words, Using a regular expression with `=~` to filter words>>
        |* <<query-operators-boolean, Boolean operators>>
        | ** <<syntax-using-boolean-operators-to-filter-numbers, Using boolean operators to filter numbers>>
        |* <<query-operators-string, String operators>>
        | ** <<syntax-concatenating-two-strings, Concatenating two strings using `+`>>
        |* <<query-operators-temporal, Temporal operators>>
        | ** <<syntax-add-subtract-duration-to-temporal-instant, Adding and subtracting a _Duration_ to or from a temporal instant>>
        | ** <<syntax-add-subtract-duration-to-duration, Adding and subtracting a _Duration_ to or from another _Duration_>>
        | ** <<syntax-multiply-divide-duration-number, Multiplying and dividing a _Duration_ with or by a number>>
        |* <<query-operators-map, Map operators>>
        | ** <<syntax-accessing-the-value-of-a-nested-map, Statically accessing the value of a nested map by key using the `.` operator">>
        | ** <<syntax-accessing-dynamic-map-parameter, Dynamically accessing the value of a map by key using the `[]` operator and a parameter>>
        |* <<query-operators-list, List operators>>
        | ** <<syntax-concatenating-two-lists, Concatenating two lists using `+`>>
        | ** <<syntax-using-in-to-check-if-a-number-is-in-a-list, Using `IN` to check if a number is in a list>>
        | ** <<syntax-using-in-for-more-complex-list-membership-operations, Using `IN` for more complex list membership operations>>
        | ** <<syntax-accessing-elements-in-a-list, Accessing elements in a list using the `[]` operator>>
        | ** <<syntax-accessing-element-in-a-list-parameter, Dynamically accessing an element in a list using the `[]` operator and a parameter>>
        | ** <<syntax-using-in-with-nested-list-subscripting, Using `IN` with `[]` on a nested list>>
      """.stripMargin)
    section("Operators at a glance", "query-operators-summary") {
      p(
        """
          |[subs=none]
          ||===
          || <<query-operators-aggregation, Aggregation operators>> | `DISTINCT`
          || <<query-operators-property, Property operators>> | `.` for static property access, `[]` for dynamic property access, `=` for replacing all properties, `+=` for mutating specific properties
          || <<query-operators-mathematical, Mathematical operators>> | `+`, `-`, `*`, `/`, `%`, `^`
          || <<query-operators-comparison, Comparison operators>>     | `=`, `<>`, `<`, `>`, `+<=+`, `>=`, `IS NULL`, `IS NOT NULL`
          || <<query-operators-comparison, String-specific comparison operators>> | `STARTS WITH`, `ENDS WITH`, `CONTAINS`, `=~` for regex matching
          || <<query-operators-boolean, Boolean operators>> | `AND`, `OR`, `XOR`, `NOT`
          || <<query-operators-string, String operators>>   | `+` for concatenation
          || <<query-operators-temporal, Temporal operators>>   | `+` and `-` for operations between durations and temporal instants/durations, `*` and `/` for operations between durations and numbers
          || <<query-operators-map, Map operators>>       |  `.` for static value access by key, `[]` for dynamic value access by key
          || <<query-operators-list, List operators>>       | `+` for concatenation, `IN` to check existence of an element in a list, `[]` for accessing element(s) dynamically
          ||===
          |""")
    }
    section("Aggregation operators", "query-operators-aggregation") {
      p(
        """The aggregation operators comprise:
          |
          |* remove duplicates values: `DISTINCT`""".stripMargin)
      section("Using the `DISTINCT` operator", "syntax-using-the-distinct-operator") {
        p("Retrieve the unique eye colors from `Person` nodes.")
        query("""CREATE
                #  (a:Person {name: 'Anne', eyeColor: 'blue'}),
                #  (b:Person {name: 'Bill', eyeColor: 'brown'}),
                #  (c:Person {name: 'Carol', eyeColor: 'blue'})
                #WITH [a, b, c] AS ps
                #UNWIND ps AS p
                #RETURN DISTINCT p.eyeColor""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("p.eyeColor" -> "blue"), Map("p.eyeColor" -> "brown")))
          })) {
          p("Even though both *'Anne'* and *'Carol'* have blue eyes, *'blue'* is only returned once.")
          resultTable()
        }
        p("`DISTINCT` is commonly used in conjunction with <<query-functions-aggregating,aggregating functions>>.")
      }
    }
    section("Property operators", "query-operators-property") {
      p(
        """The property operators pertain to a node or a relationship, and comprise:
          |
          |* statically access the property of a node or relationship using the dot operator: `.`
          |* dynamically access the property of a node or relationship using the subscript operator: `[]`
          |* property replacement `=` for replacing all properties of a node or relationship
          |* property mutation operator `+=` for setting specific properties of a node or relationship""".stripMargin)
      section("Statically accessing a property of a node or relationship using the `.` operator", "syntax-accessing-the-property-of-a-node-or-relationship") {
        query("""CREATE
                #  (a:Person {name: 'Jane', livesIn: 'London'}),
                #  (b:Person {name: 'Tom', livesIn: 'Copenhagen'})
                #WITH a, b
                #MATCH (p:Person)
                #RETURN  p.name""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("p.name" -> "Jane"), Map("p.name" -> "Tom")))
          })) {
          resultTable()
        }
      }
      section("Filtering on a dynamically-computed property key using the `[]` operator", "syntax-filtering-on-a-dynamically-computed-property-key") {
        query(
          """CREATE
            #  (a:Restaurant {name: 'Hungry Jo', rating_hygiene: 10, rating_food: 7}),
            #  (b:Restaurant {name: 'Buttercup Tea Rooms', rating_hygiene: 5, rating_food: 6}),
            #  (c1:Category {name: 'hygiene'}),
            #  (c2:Category {name: 'food'})
            #WITH a, b, c1, c2
            #MATCH (restaurant:Restaurant), (category:Category)
            #WHERE restaurant["rating_" + category.name] > 6
            #RETURN DISTINCT restaurant.name""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("restaurant.name" -> "Hungry Jo")))
          })) {
          resultTable()
        }
        p("See <<query-where-basic>> for more details on dynamic property access.")
        note {
          p("""The behavior of the `[]` operator with respect to `null` is detailed <<cypher-null-bracket-operator, here>>.""")
        }
      }
      section("Replacing all properties of a node or relationship using the `=` operator", "syntax-property-replacement-operator") {
        query(
          """CREATE (a:Person {name: 'Jane', age: 20})
            #WITH a
            #MATCH (p:Person {name: 'Jane'})
            #SET p = {name: 'Ellen', livesIn: 'London'}
            #RETURN p.name, p.age, p.livesIn""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("p.name" -> "Ellen", "p.age" -> null, "p.livesIn" -> "London")))
            assertStats(r, propertiesWritten = 5, nodesCreated = 1, labelsAdded = 1)
          })) {
          p("""All the existing properties on the node are replaced by those provided in the map; i.e. the `name` property is updated from `Jane` to `Ellen`, the `age` property is deleted, and the `livesIn` property is added.""")
          resultTable()
        }
        p("See <<set-replace-properties-using-map>> for more details on using the property replacement operator `=`.")
      }
      section("Mutating specific properties of a node or relationship using the `+=` operator", "syntax-property-mutation-operator") {
        query("""CREATE (a:Person {name: 'Jane', age: 20})
                #WITH a
                #MATCH (p:Person {name: 'Jane'})
                #SET p += {name: 'Ellen', livesIn: 'London'}
                #RETURN p.name, p.age, p.livesIn""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("p.name" -> "Ellen", "p.age" -> 20, "p.livesIn" -> "London")))
            assertStats(r, propertiesWritten = 4, nodesCreated = 1, labelsAdded = 1)
          })) {
          p("""The properties on the node are updated as follows by those provided in the map: the `name` property is updated from `Jane` to `Ellen`, the `age` property is left untouched, and the `livesIn` property is added.""")
          resultTable()
        }
        p("See <<set-setting-properties-using-map>> for more details on using the property mutation operator `+=`.")
      }
    }
    section("Mathematical operators", "query-operators-mathematical") {
      p(
        """The mathematical operators comprise:
          |
          |* addition: `+`
          |* subtraction or unary minus: `-`
          |* multiplication: `*`
          |* division: `/`
          |* modulo division: `%`
          |* exponentiation: `^`""".stripMargin)
      section("Using the exponentiation operator `^`", "syntax-using-the-exponentiation-operator") {
        query(
          """WITH 2 AS number, 3 AS exponent
            #RETURN number ^ exponent AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> 8l)))
          })) {
          resultTable()
        }
      }
      section("Using the unary minus operator `-`", "syntax-using-the-unary-minus-operator") {
        query("""WITH -3 AS a, 4 AS b
                #RETURN b - a AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> 7l)))
          })) {
          resultTable()
        }
      }
    }
    section("Comparison operators", "query-operators-comparison") {
      p(
        """The comparison operators comprise:
          |
          |* equality: `=`
          |* inequality: `<>`
          |* less than: `<`
          |* greater than: `>`
          |* less than or equal to: `\<=`
          |* greater than or equal to: `>=`
          |* `IS NULL`
          |* `IS NOT NULL`
          |* matching a regular expression: `=~`""".stripMargin)
      section("String-specific comparison operators comprise:", "query-operator-comparison-string-specific") {
        p(
          """* `STARTS WITH`: perform case-sensitive prefix searching on strings
            |* `ENDS WITH`: perform case-sensitive suffix searching on strings
            |* `CONTAINS`: perform case-sensitive inclusion searching in strings""".stripMargin)
      }
      section("Comparing two numbers", "syntax-comparing-two-numbers") {
        query("""WITH 4 AS one, 3 AS two
                #RETURN one > two AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> true)))
          })) {
          resultTable()
        }
      }
      p("""See <<cypher-comparison>> for more details on the behavior of comparison operators, and <<query-where-ranges>> for more examples showing how these may be used.""")
      section("Using `STARTS WITH` to filter names", "syntax-using-starts-with-to-filter-names") {
        query("""WITH ['John', 'Mark', 'Jonathan', 'Bill'] AS somenames
                #UNWIND somenames AS names
                #WITH names AS candidate
                #WHERE candidate STARTS WITH 'Jo'
                #RETURN candidate""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("candidate" -> "John"), Map("candidate" -> "Jonathan")))
          })) {
          resultTable()
        }
      }
      p("""<<query-where-string>> contains more information regarding the string-specific comparison operators as well as additional examples illustrating the usage thereof.""")
      p("include::../syntax/comparison.asciidoc[leveloffset=+1]")
    }
    section("Using a regular expression with `=~` to filter words", "syntax-using-a-regular-expression-to-filter-words") {
        query("""WITH ['mouse', 'chair', 'door', 'house'] AS wordlist
                #UNWIND wordlist AS word
                #WITH word
                #WHERE word =~ '.*ous.*'
                #RETURN word""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("word" -> "mouse"), Map("word" -> "house")))
          })) {
          resultTable()
        }
      }
      p("""Further information and examples regarding the use of regular expressions in filtering can be found in <<query-where-regex>>.""".stripMargin('#'))
    }
    section("Boolean operators", "query-operators-boolean") {
      p(
        """The boolean operators -- also known as logical operators -- comprise:
          |
          |* conjunction: `AND`
          |* disjunction: `OR`,
          |* exclusive disjunction: `XOR`
          |* negation: `NOT`""".stripMargin)
      p(
        """Here is the truth table for `AND`, `OR`, `XOR` and `NOT`.
          |
          |[options="header", cols="^,^,^,^,^,^", width="85%"]
          ||===
          ||a | b | a `AND` b | a `OR` b | a `XOR` b | `NOT` a
          ||`false` | `false` | `false` | `false` | `false` | `true`
          ||`false` | `null` | `false` | `null` | `null` | `true`
          ||`false` | `true` | `false` | `true` | `true` | `true`
          ||`true` | `false` | `false` | `true` | `true` | `false`
          ||`true` | `null` | `null` | `true` | `null` | `false`
          ||`true` | `true` | `true` | `true` | `false` | `false`
          ||`null` | `false` | `false` | `null` | `null` | `null`
          ||`null` | `null` | `null` | `null` | `null` | `null`
          ||`null` | `true` | `null` | `true` | `null` | `null`
          ||===
          |
          |""")
      section("Using boolean operators to filter numbers", "syntax-using-boolean-operators-to-filter-numbers") {
        query("""WITH [2, 4, 7, 9, 12] AS numberlist
                #UNWIND numberlist AS number
                #WITH number
                #WHERE number = 4 OR (number > 6 AND number < 10)
                #RETURN number""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("number" -> 4l), Map("number" -> 7l), Map("number" -> 9l)))
          })) {
          resultTable()
        }
      }
    }
    section("String operators", "query-operators-string") {
      p(
        """The string operators comprise:
          |
          |* concatenating strings: `+`""".stripMargin)
      section("Concatenating two strings with `+`", "syntax-concatenating-two-strings") {
        query("""RETURN ['neo'] + ['4j'] AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> List(neo, 4j)))
          })) {
          resultTable()
        }
      }
    }
    section("Temporal operators", "query-operators-temporal") {
      p(
        """Temporal operators comprise:
          |
          |* adding a <<cypher-temporal-durations, _Duration_>> to either a <<cypher-temporal-instants, temporal instant>> or another _Duration_: `+`
          |* subtracting a _Duration_ from either a temporal instant or another _Duration_: `-`
          |* multiplying a _Duration_ with a number: `*`
          |* dividing a _Duration_ by a number: `/`
          |
          |The following table shows -- for each combination of operation and operand type -- the type of the value returned from the application of each temporal operator:
          |
          |[options="header"]
          ||===
          || Operator | Left-hand operand | Right-hand operand | Type of result
          || <<syntax-add-subtract-duration-to-temporal-instant, `+`>> | Temporal instant           | _Duration_                 | The type of the temporal instant
          || <<syntax-add-subtract-duration-to-temporal-instant, `+`>> | _Duration_                 | Temporal instant           | The type of the temporal instant
          || <<syntax-add-subtract-duration-to-temporal-instant, `-`>> | Temporal instant           | _Duration_                 | The type of the temporal instant
          || <<syntax-add-subtract-duration-to-duration, `+`>>         | _Duration_                 | _Duration_                 | _Duration_
          || <<syntax-add-subtract-duration-to-duration, `-`>>         | _Duration_                 | _Duration_                 | _Duration_
          || <<syntax-multiply-divide-duration-number, `*`>>           | _Duration_                 | <<property-types, Number>> | _Duration_
          || <<syntax-multiply-divide-duration-number, `*`>>           | <<property-types, Number>> | _Duration_                 | _Duration_
          || <<syntax-multiply-divide-duration-number, `/`>>           | _Duration_                 | <<property-types, Number>> | _Duration_
          ||===
          |
        """)
      section("Adding and subtracting a _Duration_ to or from a temporal instant", "syntax-add-subtract-duration-to-temporal-instant") {
        query("""WITH
                #  localdatetime({year:1984, month:10, day:11, hour:12, minute:31, second:14}) AS aDateTime,
                #  duration({years: 12, nanoseconds: 2}) AS aDuration
                #RETURN aDateTime + aDuration, aDateTime - aDuration""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(
              Map(
                "aDateTime - aDuration" -> LocalDateTimeValue.parse("1972-10-11T12:31:13.999999998").asObjectCopy(),
                "aDateTime + aDuration" -> LocalDateTimeValue.parse("1996-10-11T12:31:14.000000002").asObjectCopy())
            ))
          })) {
          resultTable()
        }
        p(
          """<<cypher-temporal-duration-component, Components of a _Duration_>> that do not apply to the temporal instant are ignored.
            |For example, when adding a _Duration_ to a _Date_, the _hours_, _minutes_, _seconds_ and _nanoseconds_ of the _Duration_ are ignored (_Time_ behaves in an analogous manner):
          """.stripMargin)
        query("""WITH
                #  date({year:1984, month:10, day:11}) AS aDate,
                #  duration({years: 12, nanoseconds: 2}) AS aDuration
                #RETURN aDate + aDuration, aDate - aDuration""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(
              Map(
                "aDate - aDuration" -> DateValue.parse("1972-10-11").asObjectCopy(),
                "aDate + aDuration" -> DateValue.parse("1996-10-11").asObjectCopy())
            ))
          })) {
          resultTable()
        }
        p(
          """Adding two durations to a temporal instant is not an associative operation.
            |This is because non-existing dates are truncated to the nearest existing date:""".stripMargin)
        query(
          """RETURN
            #  (date("2011-01-31") + duration("P1M")) + duration("P12M") AS date1,
            #  date("2011-01-31") + (duration("P1M") + duration("P12M")) AS date2""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(
              Map(
                "date1" -> DateValue.parse("2012-02-28").asObjectCopy(),
                "date2" -> DateValue.parse("2012-02-29").asObjectCopy())
            ))
          })) {
          resultTable()
        }
      }
      section("Adding and subtracting a _Duration_ to or from another _Duration_", "syntax-add-subtract-duration-to-duration") {
        query("""WITH
                #  duration({years: 12, months: 5, days: 14, hours: 16, minutes: 12, seconds: 70, nanoseconds: 1}) as duration1,
                #  duration({months:1, days: -14, hours: 16, minutes: -12, seconds: 70}) AS duration2
                #RETURN duration1, duration2, duration1 + duration2, duration1 - duration2""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(
              Map(
                "duration1" -> DurationValue.parse("P12Y5M14DT16H13M10.000000001S"),
                "duration2" -> DurationValue.parse("P1M-14DT15H49M10S"),
                "duration1 + duration2" -> DurationValue.parse("P12Y6MT32H2M20.000000001S"),
                "duration1 - duration2" -> DurationValue.parse("P12Y4M28DT24M0.000000001S"))
            ))
          })) {
          resultTable()
        }
      }
      section("Multiplying and dividing a _Duration_ with or by a number", "syntax-multiply-divide-duration-number") {
        p("""These operations are interpreted simply as component-wise operations with overflow to smaller units based on an average length of units in the case of division (and multiplication with fractions).""".stripMargin)
        query("""WITH duration({days: 14, minutes: 12, seconds: 70, nanoseconds: 1}) AS aDuration
                #RETURN aDuration, aDuration * 2, aDuration / 3""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(
              Map(
                "aDuration" -> DurationValue.parse("P14DT13M10.000000001S"),
                "aDuration * 2" -> DurationValue.parse("P28DT26M20.000000002S"),
                "aDuration / 3" -> DurationValue.parse("P4DT16H4M23.333333333S"))
            ))
          })) {
          resultTable()
        }
      }
    }
    section("Map operators", "query-operators-map") {
      p(
        """The map operators comprise:
          |
          |* statically access the value of a map by key using the dot operator: `.`
          |* dynamically access the value of a map by key using the subscript operator: `[]`
          |""".stripMargin)
      note {
        p("""The behavior of the `[]` operator with respect to `null` is detailed in <<cypher-null-bracket-operator>>.""")
      }
      section("Statically accessing the value of a nested map by key using the `.` operator", "syntax-accessing-the-value-of-a-nested-map") {
        query("""WITH {person: {name: 'Anne', age: 25}} AS p
                #RETURN  p.person.name""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("p.person.name" -> "Anne")))
          })) {
          resultTable()
        }
      }
      section("Dynamically accessing the value of a map by key using the `[]` operator and a parameter", "syntax-accessing-dynamic-map-parameter") {
        p(
          """A parameter may be used to specify the key of the value to access:""".stripMargin)
        query("""WITH {name: 'Anne', age: 25} AS a
                #RETURN a[$myKey] AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> "Anne")))
          }),
          ("myKey", "name")) {
          resultTable()
        }
      }
      p("More details on maps can be found in <<cypher-maps>>.")
    }
    section("List operators", "query-operators-list") {
      p(
        """The list operators comprise:
          |
          |* concatenating lists `l~1~` and `l~2~`: `[l~1~] + [l~2~]`
          |* checking if an element `e` exists in a list `l`: `e IN [l]`
          |* dynamically accessing an element(s) in a list using the subscript operator: `[]`""".stripMargin)
      note {
        p("""The behavior of the `IN` and `[]` operators with respect to `null` is detailed <<cypher-working-with-null, here>>.""")
      }
      section("Concatenating two lists using `+`", "syntax-concatenating-two-lists") {
        query("""RETURN [1,2,3,4,5] + [6,7] AS myList""",
        ResultAssertions((r) => {
            r.toList should equal(List(Map("myList" -> List(1L, 2L, 3L, 4L, 5L, 6L, 7L))))
          })) {
          resultTable()
        }
      }
      section("Using `IN` to check if a number is in a list", "syntax-using-in-to-check-if-a-number-is-in-a-list") {
        query("""WITH [2, 3, 4, 5] AS numberlist
                #UNWIND numberlist AS number
                #WITH number
                #WHERE number IN [2, 3, 8]
                #RETURN number""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("number" -> 2l), Map("number" -> 3l)))
          })) {
          resultTable()
        }
      }
      section("Using `IN` for more complex list membership operations", "syntax-using-in-for-more-complex-list-membership-operations") {
        p(
          """The general rule is that the `IN` operator will evaluate to `true` if the list given as the right-hand operand contains an element which has the same _type and contents (or value)_ as the left-hand operand.
            |Lists are only comparable to other lists, and elements of a list `innerList` are compared pairwise in ascending order from the first element in `innerList` to the last element in `innerList`.""".stripMargin)
        p("""The following query checks whether or not the list `[2, 1]` is an element of the list `[1, [2, 1], 3]`:""".stripMargin)
        query("""RETURN [2, 1] IN [1, [2, 1], 3] AS inList""",
        ResultAssertions((r) => {
            r.toList should equal(List(Map("inList" -> true)))
          })) {
          p(
            """The query evaluates to `true` as the right-hand list contains, as an element, the list `[1, 2]` which is of the same type (a list) and contains the same contents (the numbers `2` and `1` in the given order) as the left-hand operand.
              |If the left-hand operator had been `[1, 2]` instead of `[2, 1]`, the query would have returned `false`.
            """.stripMargin)
          resultTable()
        }
        p(
          """At first glance, the contents of the left-hand operand and the right-hand operand _appear_ to be the same in the following query:""".stripMargin)
        query("""RETURN [1, 2] IN [1, 2] AS inList""",
        ResultAssertions((r) => {
            r.toList should equal(List(Map("inList" -> false)))
          })) {
          p("""However, `IN` evaluates to `false` as the right-hand operand does not contain an element that is of the same _type_ -- i.e. a _list_ -- as the left-hand-operand.""")
          resultTable()
        }
        p(
          """The following query can be used to ascertain whether or not a list -- obtained from, say, the <<functions-labels, labels()>> function -- contains at least one element that is also present in another list:""".stripMargin)
        p("""[source, cypher]
            |----
            |MATCH (n)
            |WHERE size([label IN labels(n) WHERE label IN ['Person', 'Employee'] | 1]) > 0
            |RETURN count(n)
            |----
            |""")
        p("""As long as `labels(n)` returns either `Person` or `Employee` (or both), the query will return a value greater than zero.""")
      }
      section("Accessing elements in a list using the `[]` operator", "syntax-accessing-elements-in-a-list") {
        query("""WITH ['Anne', 'John', 'Bill', 'Diane', 'Eve'] AS names
                #RETURN names[1..3] AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> List("John", "Bill"))))
          })) {
          p("""The square brackets will extract the elements from the start index `1`, and up to (but excluding) the end index `3`.""")
          resultTable()
        }
      }
      section("Dynamically accessing an element in a list using the `[]` operator and a parameter", "syntax-accessing-element-in-a-list-parameter") {
        p(
          """A parameter may be used to specify the index of the element to access:""".stripMargin)
        query("""WITH ['Anne', 'John', 'Bill', 'Diane', 'Eve'] AS names
                #RETURN names[$myIndex] AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> "John")))
          }),
          ("myIndex", 1L)) {
          resultTable()
        }
      }
      section("Using `IN` with `[]` on a nested list", "syntax-using-in-with-nested-list-subscripting") {
        p(
          """`IN` can be used in conjunction with `[]` to test whether an element exists in a nested list:""".stripMargin)
        query("""WITH [[1, 2, 3]] AS l
                #RETURN 3 IN l[0] AS result""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> true)))
          }),
          ("myIndex", 1L)) {
          resultTable()
        }
      }
      p("More details on lists can be found in <<cypher-lists-general>>.")
    }

  }.build()
}
