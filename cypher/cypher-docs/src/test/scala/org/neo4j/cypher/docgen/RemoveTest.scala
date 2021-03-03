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

import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class RemoveTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("REMOVE", "query-remove")
    initQueries("""CREATE
                  #  (a:Swedish {name: 'Andy', age: 36}),
                  #  (t:Swedish {name: 'Timothy', age: 25}),
                  #  (p:German:Swedish {name: 'Peter', age: 34}),
                  #  (a)-[:KNOWS]->(t),
                  #  (a)-[:KNOWS]->(p)""".stripMargin('#'))
    synopsis("The `REMOVE` clause is used to remove properties from nodes and relationships, and to remove labels from nodes.")
    p("""* <<query-remove-introduction, Introduction>>
        #* <<remove-remove-a-property, Remove a property>>
        #* <<remove-remove-all-properties, Remove all properties>>
        #* <<remove-remove-a-label-from-a-node, Remove a label from a node>>
        #* <<remove-remove-multiple-labels, Remove multiple labels from a node>>""".stripMargin('#'))
    section("Introduction", "query-remove-introduction") {
      p("For deleting nodes and relationships, see <<query-delete>>.")
      note{
        p("""Removing labels from a node is an idempotent operation: if you try to remove a label from a node that does not have that label on it, nothing happens.
            #The query statistics will tell you if something needed to be done or not.""".stripMargin('#'))
      }
      p("The examples use the following database:")
      graphViz()
    }
    section("Remove a property", "remove-remove-a-property") {
      p("""Neo4j doesn't allow storing `null` in properties.
          #Instead, if no value exists, the property is just not there.
          #So, `REMOVE` is used to remove a property value from a node or a relationship.""".stripMargin('#'))
      query("""MATCH (a {name: 'Andy'})
              #REMOVE a.age
              #RETURN a.name, a.age""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("a.name" -> "Andy", "a.age" -> null)))
          assertStats(r, propertiesWritten = 1, nodesDeleted = 0)
        })) {
        p("The node is returned, and no property `age` exists on it.")
        resultTable()
      }
    }
    section("Remove all properties", "remove-remove-all-properties") {
      p("""`REMOVE` cannot be used to remove all existing properties from a node or relationship.
          #Instead, using <<set-remove-properties-using-empty-map, `SET` with `=` and an empty map as the right operand>> will clear all properties from the node or relationship.""".stripMargin('#'))
    }
    section("Remove a label from a node", "remove-remove-a-label-from-a-node") {
      p("To remove labels, you use `REMOVE`.")
      query("""MATCH (n {name: 'Peter'})
              #REMOVE n:German
              #RETURN n.name, labels(n)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "labels(n)" -> List("Swedish"))))
          assertStats(r, labelsRemoved = 1, nodesDeleted = 0)
        })) {
        resultTable()
      }
    }
    section("Remove multiple labels from a node", "remove-remove-multiple-labels") {
      p("To remove multiple labels, you use `REMOVE`.")
      query("""MATCH (n {name: 'Peter'})
              #REMOVE n:German:Swedish
              #RETURN n.name, labels(n)""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "labels(n)" -> List.empty)))
          assertStats(r, labelsRemoved = 2, nodesDeleted = 0)
        })) {
        resultTable()
      }
    }
  }.build()

}
