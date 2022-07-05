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

class labelExpressionsTest extends DocumentingTest with QueryStatisticsTestSupport {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Label expressions", "query-syntax-label")
    runtime("pipelined") // A:Teacher, B:Student, C:Parent
    initQueries("""CREATE
                  #  (:A {name:'Alice'}),
                  #  (:B {name:'Bob'}),
                  #  (:C {name:'Charlie'}),
                  #  (:A:B {name:'Daniel'}),
                  #  (:A:C {name:'Eskil'}),
                  #  (:B:C {name:'Frank'}),
                  #  (:A:B:C {name:'George'}),
                  #  ({name:'Henry'})""".stripMargin('#'))
    p("""Label expressions evaluate to true or false when applied to the label set of a node, or the type of a relationship.
        # Assuming no other filters are applied, then a label expression evaluating to true means the node or relationship is matched.""".stripMargin('#'))
    p("The following graph is used for the examples below:")
    graphViz()
    section("Match without label expression", "syntax-no-label") {
      p("""A match without a label expression returns all nodes in the graph, non withstanding if the node is empty""".stripMargin('#'))
      query("""MATCH (n) RETURN n.name AS name""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice"), Map("name" -> "Bob"), Map("name" -> "Charlie"), Map("name" -> "Daniel"), Map("name" -> "Eskil"), Map("name" -> "Frank"), Map("name" -> "George"), Map("name" -> "Henry")))
      })) {
        resultTable()
      }
    }
    section("Match on single node label", "syntax-on-single-label") {
      p("""A match on a single node label returns the nodes that contains the specified label""".stripMargin('#'))
      query("""MATCH (n:A) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice"), Map("name" -> "Daniel"), Map("name" -> "Eskil"), Map("name" -> "George")))
        })) {
        resultTable()
      }
    }
    section("Match with an `AND` expression for the node labels", "syntax-and-label") {
      p("""A match with an `AND` expression for the node label returns the nodes that contains both the specified labels""".stripMargin('#'))
      query("""MATCH (n:A&B) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Daniel"), Map("name" -> "George")))
        })) {
        resultTable()
      }
    }
    section("Match with an `OR` expression for the node labels", "syntax-or-label") {
      p("""A match with an `OR` expression for the node label returns the nodes that contains either of the specified labels""".stripMargin('#'))
      query("""MATCH (n:A|B) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice"), Map("name" -> "Bob"), Map("name" -> "Daniel"), Map("name" -> "Eskil"), Map("name" -> "Frank"), Map("name" -> "George")))
        })) {
        resultTable()
      }
    }
    section("Match with a `NOT` expression for the node labels", "syntax-not-label") {
      p("""A match with an `NOT` expression for the node label returns the nodes that does not contain the specified label""".stripMargin('#'))
      query("""MATCH (n:!A) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Bob"), Map("name" -> "Charlie"), Map("name" -> "Frank"), Map("name" -> "Henry")))
        })) {
        resultTable()
      }
    }
    section("Match with a `Wildcard` expression for the node labels", "syntax-wild-label") {
      p("""A match with a `Wildcard` expression for the node label returns the nodes that contains any label""".stripMargin('#'))
      query("""MATCH (n:%) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice"), Map("name" -> "Bob"), Map("name" -> "Charlie"), Map("name" -> "Daniel"), Map("name" -> "Eskil"), Map("name" -> "Frank"), Map("name" -> "George")))
        })) {
        resultTable()
      }
    }
    section("Match with a nesting of label expressions for the node labels", "syntax-nesting-label") {
      p("""A match with a nesting of label expressions for the node label returns the nodes for which the full expression is true""".stripMargin('#'))
      query("""MATCH (n:(!A&!B)|C) RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Charlie"), Map("name" -> "Eskil"), Map("name" -> "Frank"), Map("name" -> "George"), Map("name" -> "Henry")))
        })) {
        resultTable()
      }
    }
    section("Match with label expressions in the predicate", "syntax-predicate-label") {
      p("""A label expression can also be used as a predicate in the WHERE clause""".stripMargin('#'))
      query("""MATCH (n) WHERE n:A|B RETURN n.name AS name""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("name" -> "Alice"), Map("name" -> "Bob"), Map("name" -> "Daniel"), Map("name" -> "Eskil"), Map("name" -> "Frank"), Map("name" -> "George")))
        })) {
        resultTable()
      }
    }
    section("Match with label expressions in the return", "syntax-return-label") {
      p("""A label expression can also be used in the with or return statement""".stripMargin('#'))
      query("""MATCH (n) RETURN n:A&B""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("n:A&B" -> false), Map("n:A&B" -> false), Map("n:A&B" -> false), Map("n:A&B" -> true), Map("n:A&B" -> false), Map("n:A&B" -> false), Map("n:A&B" -> true), Map("n:A&B" -> false)))
        })) {
        resultTable()
      }
    }
  }.build()
}
