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

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class CreateTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("CREATE", "query-create")
    initQueries(
      """CREATE (a:Person {name: 'A'}),
                (b:Person {name: 'B'})
      """.stripMargin)
    synopsis("The `CREATE` clause is used to create nodes and relationships.")
    p(
      """* <<create-nodes, Create nodes>>
        |** <<create-create-single-node, Create single node>>
        |** <<create-create-multiple-nodes, Create multiple nodes>>
        |** <<create-create-a-node-with-a-label, Create a node with a label>>
        |** <<create-create-a-node-with-multiple-labels, Create a node with multiple labels>>
        |** <<create-create-node-and-add-labels-and-properties, Create node and add labels and properties>>
        |** <<create-return-created-node, Return created node>>
        |* <<create-relationships, Create relationships>>
        |** <<create-create-a-relationship-between-two-nodes, Create a relationship between two nodes>>
        |** <<create-create-a-relationship-and-set-properties, Create a relationship and set properties>>
        |* <<create-create-a-full-path, Create a full path>>
        |* <<use-parameters-with-create, Use parameters with `CREATE`>>
        |** <<create-create-node-with-a-parameter-for-the-properties, Create node with a parameter for the properties>>
        |** <<create-create-multiple-nodes-with-a-parameter-for-their-properties, Create multiple nodes with a parameter for their properties>>""".stripMargin)
    tip{
      p("""In the `CREATE` clause, patterns are used extensively.
          |Read <<cypher-patterns>> for an introduction.""".stripMargin)
    }
    section("Create nodes", "create-nodes") {
      section("Create single node", "create-create-single-node") {
        p(
          """Creating a single node is done by issuing the following query:""".stripMargin)
        query(
          """CREATE (n)""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1)
          })) {
          p("Nothing is returned from this query, except the count of affected nodes.")
          resultTable()
        }
      }
      section("Create multiple nodes", "create-create-multiple-nodes") {
        p(
          """Creating multiple nodes is done by separating them with a comma.""".stripMargin)
        query(
          """CREATE (n), (m)""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 2)
          })) {
          resultTable()
        }
      }
      section("Create a node with a label", "create-create-a-node-with-a-label") {
        p(
          """To add a label when creating a node, use the syntax below:""".stripMargin)
        query(
          """CREATE (n:Person)""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, labelsAdded = 1)
          })) {
          p("Nothing is returned from this query.")
          resultTable()
        }
      }
      section("Create a node with multiple labels", "create-create-a-node-with-multiple-labels") {
        p(
          """To add labels when creating a node, use the syntax below.
            |In this case, we add two labels.""".stripMargin)
        query(
          """CREATE (n:Person:Swedish)""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, labelsAdded = 2)
          })) {
          p("Nothing is returned from this query.")
          resultTable()
        }
      }
      section("Create node and add labels and properties", "create-create-node-and-add-labels-and-properties") {
        p(
          """When creating a new node with labels, you can add properties at the same time.""".stripMargin)
        query(
          """CREATE (n:Person {name: 'Andy', title: 'Developer'})""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, labelsAdded = 1, propertiesWritten = 2)
          })) {
          p("Nothing is returned from this query.")
          resultTable()
        }
      }
      section("Return created node", "create-return-created-node") {
        p(
          """Creating a single node is done by issuing the following query:""".stripMargin)
        query(
          """CREATE (a {name: 'Andy'})
            |RETURN a.name""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("a.name" -> "Andy")))
            assertStats(r, propertiesWritten = 1, nodesCreated = 1)
          })) {
          p("The newly-created node is returned.")
          resultTable()
        }
      }
    }
    section("Create relationships", "create-relationships") {
      section("Create a relationship between two nodes", "create-create-a-relationship-between-two-nodes") {
        p(
          """To create a relationship between two nodes, we first get the two nodes.
            |Once the nodes are loaded, we simply create a relationship between them.""".stripMargin)
        initQueries()
        query(
          """MATCH (a:Person), (b:Person)
            |WHERE a.name = 'A' AND b.name = 'B'
            |CREATE (a)-[r:RELTYPE]->(b)
            |RETURN type(r)""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("type(r)" -> "RELTYPE")))
            assertStats(r, relationshipsCreated = 1)
          })) {
          p("The created relationship is returned by the query.")
          resultTable()
        }
      }
      section("Create a relationship and set properties", "create-create-a-relationship-and-set-properties") {
        p(
          """Setting properties on relationships is done in a similar manner to how it's done when creating nodes.
            |Note that the values can be any expression.""".stripMargin)
        query(
          """MATCH (a:Person), (b:Person)
            |WHERE a.name = 'A' AND b.name = 'B'
            |CREATE (a)-[r:RELTYPE {name: a.name + '<->' + b.name}]->(b)
            |RETURN type(r), r.name""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("type(r)" -> "RELTYPE", "r.name" -> "A<->B")))
            assertStats(r, propertiesWritten = 1, relationshipsCreated = 1)
          })) {
          p("The newly-created relationship is returned by the example query.")
          resultTable()
        }
      }
    }
    section("Create a full path", "create-create-a-full-path") {
      p(
        """When you use `CREATE` and a pattern, all parts of the pattern that are not already in scope at this time will be created.""".stripMargin)
      query(
        """CREATE p = (andy {name:'Andy'})-[:WORKS_AT]->(neo)<-[:WORKS_AT]-(michael {name: 'Michael'})
          |RETURN p""".stripMargin, ResultAssertions((r) => {
          assertStats(r, nodesCreated = 3, relationshipsCreated = 2, propertiesWritten = 2)
        })) {
        p("This query creates three nodes and two relationships in one go, assigns it to a path variable, and returns it.")
        resultTable()
      }
    }
    section("Use parameters with `CREATE`", "use-parameters-with-create") {
      section("Create node with a parameter for the properties", "create-create-node-with-a-parameter-for-the-properties") {
        p(
          """You can also create a graph entity from a map.
            |All the key/value pairs in the map will be set as properties on the created relationship or node.
            |In this case we add a `Person` label to the node as well.""".stripMargin)
        query(
          """CREATE (n:Person $props)
            |RETURN n""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, labelsAdded = 1, propertiesWritten = 2)
          }),
          ("props", Map("name" -> "Andy", "position" -> "Developer"))) {
          resultTable()
        }
      }
      section("Create multiple nodes with a parameter for their properties", "create-create-multiple-nodes-with-a-parameter-for-their-properties") {
        p(
          """By providing Cypher an array of maps, it will create a node for each map.""".stripMargin)
        query(
          """UNWIND $props AS map
            |CREATE (n) SET n = map""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 2, propertiesWritten = 4)
          }),
          "props" -> List(Map("name" -> "Andy", "position" -> "Developer"), Map("name" -> "Michael", "position" -> "Developer"))) {
          resultTable()
        }
      }
    }
  }.build()
}
