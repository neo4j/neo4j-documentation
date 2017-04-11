/*
 * Copyright (c) 2002-2017 "Neo Technology,"
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

class CaseExpressionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("`CASE` expressions", "query-syntax-case")
    initQueries(
      """CREATE (alice:A {name:'Alice', age: 38, eyes: 'brown'}),
        |       (bob:B {name: 'Bob', age: 25, eyes: 'blue'}),
        |       (charlie:C {name: 'Charlie', age: 53, eyes: 'green'}),
        |       (daniel:D {name: 'Daniel', age: 54, eyes: 'brown'}),
        |       (eskil:E {name: 'Eskil', age: 41, eyes: 'blue', array: ['one', 'two', 'three']}),
        |
        |       (alice)-[:KNOWS]->(bob),
        |       (alice)-[:KNOWS]->(charlie),
        |       (bob)-[:KNOWS]->(daniel),
        |       (charlie)-[:KNOWS]->(daniel),
        |       (bob)-[:MARRIED]->(eskil)""")
    p(
      """Cypher supports `CASE` expressions, which is a generic conditional expression, similar to if/else statements in other languages.
        |Two variants of `CASE` exist -- the simple form and the generic form.
      """.stripMargin)
    p("The following graph is used for the examples below:")
    graphViz()
    section("Simple `CASE`", "syntax-simple-case") {
      p(
        """The expression is calculated, and compared in order with the `WHEN` clauses until a match is found.
          |If no match is found the expression in the `ELSE` clause is used, or `null`, if no `ELSE` case exists.""".stripMargin)
      functionWithCypherStyleFormatting(
        "CASE test \n WHEN value THEN result \n  [WHEN ...] \n  [ELSE default] \nEND", ("test", "A valid expression."), ("value", "An expression whose result will be compared to the `test` expression."), ("result", "This is the result expression used if the value expression matches the `test` expression."), ("default", "The expression to use if no match is found."))
      query(
        """MATCH (n)
          |RETURN
          |CASE n.eyes
          |WHEN 'blue'  THEN 1
          |WHEN 'brown' THEN 2
          |ELSE 3
          |END AS result""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> 2), Map("result" -> 1), Map("result" -> 3), Map("result" -> 2), Map("result" -> 1)))
        })) {
        resultTable()
      }
    }
    section("Generic `CASE`", "syntax-generic-case") {
      p(
        """The predicates are evaluated in order until a true value is found, and the result value is used.
          |If no match is found the expression in the `ELSE` clause is used, or `null`, if no `ELSE` case exists.""".stripMargin)
      functionWithCypherStyleFormatting(
        "CASE \nWHEN predicate THEN result \n  [WHEN ...] \n  [ELSE default] \nEND", ("predicate", "A predicate that is tested to find a valid alternative."), ("result", "This is the result expression used if the predicate matches."), ("default", "The expression to use if no match is found."))
      query(
        """MATCH (n)
          |RETURN
          |CASE
          |WHEN n.eyes = 'blue'  THEN 1
          |WHEN n.age < 40       THEN 2
          |ELSE 3
          |END AS result""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> 2), Map("result" -> 1), Map("result" -> 3), Map("result" -> 3), Map("result" -> 1)))
        })) {
        resultTable()
      }
    }

  }.build()
}
