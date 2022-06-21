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

class CaseExpressionsTest extends DocumentingTest with QueryStatisticsTestSupport {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("`CASE` expressions", "query-syntax-case")
    runtime("interpreted")
    initQueries("""CREATE
                  #  (alice:A {name:'Alice', age: 38, eyes: 'brown'}),
                  #  (bob:B {name: 'Bob', age: 25, eyes: 'blue'}),
                  #  (charlie:C {name: 'Charlie', age: 53, eyes: 'green'}),
                  #  (daniel:D {name: 'Daniel', eyes: 'brown'}),
                  #  (eskil:E {name: 'Eskil', age: 41, eyes: 'blue', array: ['one', 'two', 'three']}),
                  #  (alice)-[:KNOWS]->(bob),
                  #  (alice)-[:KNOWS]->(charlie),
                  #  (bob)-[:KNOWS]->(daniel),
                  #  (charlie)-[:KNOWS]->(daniel),
                  #  (bob)-[:MARRIED]->(eskil)""".stripMargin('#'))
    p("""Generic conditional expressions may be expressed using the `CASE` construct.
        #Two variants of `CASE` exist within Cypher: the simple form, which allows an expression to be compared against multiple values, and the generic form, which allows multiple conditional statements to be expressed.""".stripMargin('#'))
    note{
      p("""CASE can only be used as part of RETURN or WITH if you want to use the result in the succeeding clause or statement.""".stripMargin('#'))
    }
    p("The following graph is used for the examples below:")
    graphViz()
    section("Simple `CASE` form: comparing an expression against multiple values", "syntax-simple-case") {
      p("""The expression is calculated, and compared in order with the `WHEN` clauses until a match is found.
          #If no match is found, the expression in the `ELSE` clause is returned.
          #However, if there is no `ELSE` case and no match is found, `null` will be returned.""".stripMargin('#'))
      functionWithCypherStyleFormatting("""CASE test
                                          #  WHEN value THEN result
                                          #  [WHEN ...]
                                          #  [ELSE default]
                                          #END""".stripMargin('#'),
      ("test", "A valid expression."),
      ("value", "An expression whose result will be compared to `test`."),
      ("result", "This is the expression returned as output if `value` matches `test`."),
      ("default", "If no match is found, `default` is returned."))
      query("""MATCH (n)
              #RETURN
              #CASE n.eyes
              #  WHEN 'blue'  THEN 1
              #  WHEN 'brown' THEN 2
              #  ELSE 3
              #END AS result""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> 2), Map("result" -> 1), Map("result" -> 3), Map("result" -> 2), Map("result" -> 1)))
      })) {
        resultTable()
      }
    }
    section("Generic `CASE` form: allowing for multiple conditionals to be expressed", "syntax-generic-case") {
      p("""The predicates are evaluated in order until a `true` value is found, and the result value is used.
          #If no match is found, the expression in the `ELSE` clause is returned.
          #However, if there is no `ELSE` case and no match is found, `null` will be returned.""".stripMargin('#'))
      functionWithCypherStyleFormatting("""CASE
                                          #  WHEN predicate THEN result
                                          #  [WHEN ...]
                                          #  [ELSE default]
                                          #END""".stripMargin('#'),
      ("predicate", "A predicate that is tested to find a valid alternative."),
      ("result", "This is the expression returned as output if `predicate` evaluates to `true`."),
      ("default", "If no match is found, `default` is returned."))
      query("""MATCH (n)
              #RETURN
              #CASE
              #  WHEN n.eyes = 'blue' THEN 1
              #  WHEN n.age < 40      THEN 2
              #  ELSE 3
              #END AS result""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> 2), Map("result" -> 1), Map("result" -> 3), Map("result" -> 3), Map("result" -> 1)))
        })) {
        resultTable()
      }
    }
    section("Distinguishing between when to use the simple and generic `CASE` forms", "syntax-distinguish-case") {
      p(
        """Owing to the close similarity between the syntax of the two forms, sometimes it may not be clear at the outset as to which form to use.
          #We illustrate this scenario by means of the following query, in which there is an expectation that `age_10_years_ago` is `-1` if `n.age` is `null`:
        """.stripMargin('#'))
      query("""MATCH (n)
              #RETURN n.name,
              #CASE n.age
              #  WHEN n.age IS NULL THEN -1
              #  ELSE n.age - 10
              #END AS age_10_years_ago""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("age_10_years_ago" -> 28, "n.name" -> "Alice"), Map("age_10_years_ago" -> 15, "n.name" -> "Bob"), Map("age_10_years_ago" -> 43, "n.name" -> "Charlie"), Map("age_10_years_ago" -> null, "n.name" -> "Daniel"), Map("age_10_years_ago" -> 31, "n.name" -> "Eskil")))
        })) {
        p("""However, as this query is written using the simple `CASE` form, instead of `age_10_years_ago` being `-1` for the node named `Daniel`, it is `null`.
            #This is because a comparison is made between `n.age` and `n.age IS NULL`.
            #As `n.age IS NULL` is a boolean value, and `n.age` is an integer value, the `WHEN n.age IS NULL THEN -1` branch is never taken.
            #This results in the `ELSE n.age - 10` branch being taken instead, returning `null`.""".stripMargin('#'))
        resultTable()
      }
      p("""The corrected query, behaving as expected, is given by the following generic `CASE` form:""")
      query("""MATCH (n)
              #RETURN n.name,
              #CASE
              #  WHEN n.age IS NULL THEN -1
              #  ELSE n.age - 10
              #END AS age_10_years_ago""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("age_10_years_ago" -> 28, "n.name" -> "Alice"), Map("age_10_years_ago" -> 15, "n.name" -> "Bob"), Map("age_10_years_ago" -> 43, "n.name" -> "Charlie"), Map("age_10_years_ago" -> -1, "n.name" -> "Daniel"), Map("age_10_years_ago" -> 31, "n.name" -> "Eskil")))
        })) {
        p("""We now see that the `age_10_years_ago` correctly returns `-1` for the node named `Daniel`.""".stripMargin)
        resultTable()
      }
    }
    section("Using the result of `CASE` in the succeeding clause or statement", "syntax-use-case-result") {
      p("""You can use the result of `CASE` to set properties on a node or relationship.
          #For example, instead of specifying the node directly, you can set a property for a node selected by an expression:""".stripMargin('#'))
      query("""MATCH (n)
              #WITH n,
              #CASE n.eyes
              #  WHEN 'blue'  THEN 1
              #  WHEN 'brown' THEN 2
              #  ELSE 3
              #END AS colourCode
              #SET n.colourCode = colourCode""".stripMargin('#'),
        ResultAssertions((r) => {
          assertStats(r, propertiesWritten = 5)
        })) {
        p("For more information about using the `SET` clause, see <<query-set>>.")
        resultTable()
      }
    }
    section("Using `CASE` with NULL values", "syntax-use-case-with-null") {
      p("""When using the simple `CASE` form, it is useful to remember that in Cypher `null = null` yields `null`.
          #For example, you might expect `age_10_years_ago` to be `-1` for the node named `Daniel`:""".stripMargin('#'))
      query("""MATCH (n)
              #RETURN n.name,
              #CASE n.age
              #  WHEN NULL THEN -1
              #  ELSE n.age - 10
              #END AS age_10_years_ago""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("age_10_years_ago" -> 28, "n.name" -> "Alice"), Map("age_10_years_ago" -> 15, "n.name" -> "Bob"), Map("age_10_years_ago" -> 43, "n.name" -> "Charlie"), Map("age_10_years_ago" -> null, "n.name" -> "Daniel"), Map("age_10_years_ago" -> 31, "n.name" -> "Eskil")))
        })) {
        p("""However, as `null = null` does not yield true, the `WHEN NULL THEN -1` branch is never taken,
            #resulting in the `ELSE n.age - 10` branch being taken instead, returning `null`.""".stripMargin('#'))
        resultTable()
      }
    }
  }.build()
}
