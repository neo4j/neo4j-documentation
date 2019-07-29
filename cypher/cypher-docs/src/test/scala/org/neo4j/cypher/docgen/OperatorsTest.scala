/*
 * Copyright (c) 2002-2019 "Neo Technology,"
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

import org.neo4j.cypher.docgen.tooling._

class OperatorsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Operators", "query-operators")
    p(
      """* <<query-operators-summary, Operators at a glance>>
        |* <<query-operators-general, General operators>>
        | ** <<syntax-using-the-distinct-operator, Using the `DISTINCT` operator>>
        | ** <<syntax-accessing-the-property-of-a-nested-literal-map, Accessing properties of a nested literal map using the `.` operator>>
        | ** <<syntax-filtering-on-a-dynamically-computed-property-key, Filtering on a dynamically-computed property key using the `[]` operator>>
        |* <<query-operators-mathematical, Mathematical operators>>
        | ** <<syntax-using-the-exponentiation-operator, Using the exponentiation operator `^`>>
        | ** <<syntax-using-the-unary-minus-operator, Using the unary minus operator `-`>>
        |* <<query-operators-comparison, Comparison operators>>
        | ** <<syntax-comparing-two-numbers, Comparing two numbers>>
        | ** <<syntax-using-starts-with-to-filter-names, Using `STARTS WITH` to filter names>>
        |* <<query-operators-boolean, Boolean operators>>
        | ** <<syntax-using-boolean-operators-to-filter-numbers, Using boolean operators to filter numbers>>
        |* <<query-operators-string, String operators>>
        | ** <<syntax-using-a-regular-expression-to-filter-words, Using a regular expression with `=~` to filter words>>
        |* <<query-operators-list, List operators>>
        | ** <<syntax-concatenating-two-lists, Concatenating two lists using `+`>>
        | ** <<syntax-using-in-to-check-if-a-number-is-in-a-list, Using `IN` to check if a number is in a list>>
        | ** <<syntax-accessing-elements-in-a-list, Accessing elements in a list using the `[]` operator>>
        |* <<query-operators-property, Property operators>>
        |* <<cypher-comparison, Equality and comparison of values>>
        |* <<cypher-ordering, Ordering and comparison of values>>
        |* <<cypher-operations-chaining, Chaining comparison operations>>
      """.stripMargin)
    section("Operators at a glance", "query-operators-summary") {
      p(
        """
          |[subs=none]
          ||===
           || <<query-operators-general, General operators>> | `DISTINCT`, `.` for property access, `[]` for dynamic property access
           || <<query-operators-mathematical, Mathematical operators>> | `+`, `-`, `*`, `/`, `%`, `^`
           || <<query-operators-comparison, Comparison operators>>     | `=`, `<>`, `<`, `>`, `+<=+`, `>=`, `IS NULL`, `IS NOT NULL`
           || <<query-operators-comparison, String-specific comparison operators>> | `STARTS WITH`, `ENDS WITH`, `CONTAINS`
           || <<query-operators-boolean, Boolean operators>> | `AND`, `OR`, `XOR`, `NOT`
           || <<query-operators-string, String operators>>   | `+` for concatenation, `=~` for regex matching
           || <<query-operators-list, List operators>>       | `+` for concatenation, `IN` to check existence of an element in a list, `[]` for accessing element(s)
           ||===
           |""")
    }
    section("General operators", "query-operators-general") {
      p(
        """The general operators comprise:
          |
          |* remove duplicates values: `DISTINCT`
          |* access the property of a node, relationship or literal map using the dot operator: `.`
          |* dynamic property access using the subscript operator: `[]`""".stripMargin)
      section("Using the `DISTINCT` operator", "syntax-using-the-distinct-operator") {
        p("Retrieve the unique eye colors from `Person` nodes.")
        query(
          """CREATE (a:Person {name: 'Anne', eyeColor: 'blue'}),
                        (b:Person {name: 'Bill', eyeColor: 'brown'}),
                        (c:Person {name: 'Carol', eyeColor: 'blue'})
                        WITH a, b, c
                  MATCH (p:Person)
                  RETURN DISTINCT p.eyeColor""", ResultAssertions((r) => {
            r.toList should equal(List(Map("p.eyeColor" -> "blue"), Map("p.eyeColor" -> "brown")))
          })) {
          p("Even though both *'Anne'* and *'Carol'* have blue eyes, *'blue'* is only returned once.")
          resultTable()
        }
        p("`DISTINCT` is commonly used in conjunction with <<query-functions-aggregating,aggregating functions>>.")
      }
      section("Accessing properties of a nested literal map using the `.` operator", "syntax-accessing-the-property-of-a-nested-literal-map") {
        query(
          """WITH {person: {name: 'Anne', age: 25}} AS p
            |RETURN  p.person.name""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("p.person.name" -> "Anne")))
          })) {
          resultTable()
        }
      }
      section("Filtering on a dynamically-computed property key using the `[]` operator", "syntax-filtering-on-a-dynamically-computed-property-key") {
        query(
          """CREATE (a:Restaurant {name: 'Hungry Jo', rating_hygiene: 10, rating_food: 7}),
            |(b:Restaurant {name: 'Buttercup Tea Rooms', rating_hygiene: 5, rating_food: 6}),
            |(c1:Category {name: 'hygiene'}),
            |(c2:Category {name: 'food'})
            |WITH a, b, c1, c2
            |MATCH (restaurant:Restaurant), (category:Category)
            |WHERE restaurant["rating_" + category.name] > 6
            |RETURN DISTINCT restaurant.name""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("restaurant.name" -> "Hungry Jo")))
          })) {
          resultTable()
        }
        p("See <<query-where-basic>> for more details on dynamic property access.")
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
            |RETURN number ^ exponent AS result""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> 8l)))
          })) {
          resultTable()
        }
      }
      section("Using the unary minus operator `-`", "syntax-using-the-unary-minus-operator") {
        query(
          """WITH -3 AS a, 4 AS b
            |RETURN b - a AS result""".stripMargin, ResultAssertions((r) => {
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
          |* `IS NOT NULL`""".stripMargin)
      section ("String-specific comparison operators comprise:", "query-operator-comparison-string-specific") {
        p("""* `STARTS WITH`: perform case-sensitive prefix searching on strings
            |* `ENDS WITH`: perform case-sensitive suffix searching on strings
            |* `CONTAINS`: perform case-sensitive inclusion searching in strings""")
      }
      section("Comparing two numbers", "syntax-comparing-two-numbers") {
        query(
          """WITH 4 AS one, 3 AS two
            |RETURN one > two AS result""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> true)))
          })) {
          resultTable()
        }
      }
      p("""See <<cypher-comparison>> for more details on the behavior of comparison operators, and <<query-where-ranges>> for more examples showing how these may be used.""")
      section("Using `STARTS WITH` to filter names", "syntax-using-starts-with-to-filter-names") {
        query("""WITH ['John', 'Mark', 'Jonathan', 'Bill'] AS somenames
         UNWIND somenames AS names
         WITH names AS candidate
         WHERE candidate STARTS WITH 'Jo'
         RETURN candidate""", ResultAssertions((r) => {
          r.toList should equal(List(Map("candidate" -> "John"), Map("candidate" -> "Jonathan")))
        })) {
          resultTable()
        }
      }
      p("""<<query-where-string>> contains more information regarding the string-specific comparison operators as well as additional examples illustrating the usage thereof.""")
    }
    section("Boolean operators", "query-operators-boolean") {
      p(
        """The boolean operators -- also known as logical operators -- comprise:
          |
          |* conjunction: `AND`
          |* disjunction: `OR`,
          |* exclusive disjunction: `XOR`
          |* negation: `NOT`""".stripMargin)
      p("""Here is the truth table for `AND`, `OR`, `XOR` and `NOT`.
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
        query(
          """WITH [2, 4, 7, 9, 12] AS numberlist
            |UNWIND numberlist AS number
            |WITH number
            |WHERE number = 4 OR (number > 6 AND number < 10)
            |RETURN number""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("number" -> 4l), Map("number" -> 7l), Map("number" -> 9l)))
          })) {
          resultTable()
        }
      }
    }
    section("String operators", "query-operators-string") {
      p(
        """String operators comprise:
          |
          |* concatenating strings: `+`
          |* matching a regular expression: `=~`""".stripMargin)
      section("Using a regular expression with `=~` to filter words", "syntax-using-a-regular-expression-to-filter-words") {
        query(
          """WITH ['mouse', 'chair', 'door', 'house'] AS wordlist
            |UNWIND wordlist AS word
            |WITH word
            |WHERE word =~ '.*ous.*'
            |RETURN word""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("word" -> "mouse"), Map("word" -> "house")))
          })) {
          resultTable()
        }
      }
      p("""Further information and examples regarding the use of regular expressions in filtering can be found in <<query-where-regex>>.
          |In addition, refer to <<query-operator-comparison-string-specific>> for details on string-specific comparison operators.""")
    }
    section("List operators", "query-operators-list") {
      p(
        """List operators comprise:
          |
          |* concatenating lists: `+`
          |* checking if an element exists in a list: `IN`
          |* accessing an element(s) in a list using the subscript operator: `[]`""".stripMargin)
      section("Concatenating two lists using `+`", "syntax-concatenating-two-lists") {
        query(
          """RETURN [1,2,3,4,5] + [6,7] AS myList""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("myList" -> List(1L, 2L, 3L, 4L, 5L, 6L, 7L))))
          })) {
          resultTable()
        }
      }
      section("Using `IN` to check if a number is in a list", "syntax-using-in-to-check-if-a-number-is-in-a-list") {
        query(
          """WITH [2, 3, 4, 5] AS numberlist
            |UNWIND numberlist AS number
            |WITH number
            |WHERE number IN [2, 3, 8]
            |RETURN number""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("number" -> 2l), Map("number" -> 3l)))
          })) {
          resultTable()
        }
      }
      section("Accessing elements in a list using the `[]` operator", "syntax-accessing-elements-in-a-list") {
        query(
          """WITH ['Anne', 'John', 'Bill', 'Diane', 'Eve'] AS names
            |RETURN names[1..3] AS result""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("result" -> List("John", "Bill"))))
          })) {
          p("""The square brackets will extract the elements from the start index `1`, and up to (but excluding) the end index `3`.""")
          resultTable()
        }
      }
      p("More details on lists can be found in <<cypher-lists-general>>.")
    }

  }.build()
}
