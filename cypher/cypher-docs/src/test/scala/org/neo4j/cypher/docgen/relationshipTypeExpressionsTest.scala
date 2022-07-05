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

class relationshipTypeExpressionsTest extends DocumentingTest with QueryStatisticsTestSupport {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Relationship Type Expressions", "query-syntax-type")
    runtime("pipelined") // R1:Teacher, R2:Student, R3:Parent
    initQueries("""CREATE
                  #  (:A:B)-[:R1 {name:'Teaches'}]->(:B),
                  #  (:C)-[:R2 {name:'Studies'}]->(:D),
                  #  (:E)-[:R3 {name:'Parents'}]->(:F)""".stripMargin('#'))
    p("""Relationship Type Expressions evaluate to true or false when applied to the type of a relationship.
        # Assuming no other filters are applied, then a relationship type expression evaluating to true means the relationship is matched.""".stripMargin('#'))
    p("The following graph is used for the examples below:")
    graphViz()
    section("Match without relationship type expression", "syntax-no-rel-type") {
      p("""A match without a relationship type expression returns all relationships in the graph""".stripMargin('#'))
      query("""MATCH ()-[r]->() RETURN r.name as name""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Teaches"), Map("name" -> "Studies"), Map("name" -> "Parents")))
      })) {
        resultTable()
      }
    }
    section("Match on single relationship type", "syntax-on-single-type") {
      p("""A match on a single relationship type returns the relationships that contains the specified type""".stripMargin('#'))
      query("""MATCH ()-[r:R1]->() RETURN r.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Teaches")))
        })) {
        resultTable()
      }
    }
    section("Match with an `OR` expression for the relationship types", "syntax-or-type") {
      p("""A match with an `OR` expression for the relationship type returns the relationships that contains either of the specified types""".stripMargin('#'))
      query("""MATCH ()-[r:R1|R2]->() RETURN r.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Teaches"), Map("name" -> "Studies")))
        })) {
        resultTable()
      }
    }
    section("Match with a `NOT` expression for the relationship types", "syntax-not-type") {
      p("""A match with an `NOT` expression for the relationship type returns the relationships that does not contain the specified type""".stripMargin('#'))
      query("""MATCH ()-[r:!R1]->() RETURN r.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Studies"), Map("name" -> "Parents")))
        })) {
        resultTable()
      }
    }
    section("Match with a nesting of relationship type expressions for the relationship types", "syntax-nesting-type") {
      p("""A match with a nesting of relationship type expressions for the relationship type returns the nodes for which the full expression is true""".stripMargin('#'))
      query("""MATCH ()-[r:(!R1&!R2)|R3]->() RETURN r.name as name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Parents")))
        })) {
        resultTable()
      }
    }
    section("Match with relationship type expressions in the predicate", "syntax-predicate-type") {
      p("""A relationship type expression can also be used as a predicate in the where clause""".stripMargin('#'))
      query("""MATCH (n)-[r]->(m) WHERE r:R1|R2 RETURN r.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Teaches"), Map("name" -> "Studies")))
        })) {
        resultTable()
      }
    }
    section("Match with relationship type expression in the return", "syntax-return-type") {
      p("""A relationship type expression can also be used in the with or return statement""".stripMargin('#'))
      query(
        """MATCH (n)-[r]->(m) RETURN r:R1|R2 AS result""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> true), Map("result" -> true), Map("result" -> false)))
        })) {
        resultTable()
      }
    }
    section("Match with relationship type expression and node label expression in a case", "syntax-case-type") {
      p("""A relationship type expression and a label expression can also be used in a case statement""".stripMargin('#'))
      query(
        """MATCH (n)-[r]->(m)
          # RETURN
          # CASE
          #    WHEN n:A&B THEN 1
          #    WHEN r:!R1&!R2 THEN 2
          #    ELSE -1
          #    END AS result
          #""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("result" -> 1), Map("result" -> -1), Map("result" -> 2)))
        })) {
        resultTable()
      }
    }
  }.build()
}
