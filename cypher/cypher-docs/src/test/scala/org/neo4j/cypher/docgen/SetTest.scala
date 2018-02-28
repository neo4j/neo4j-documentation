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

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class SetTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("SET", "query-set")
    initQueries(
      """CREATE (a:Swedish {name: 'Andres', age: 36, hungry: true}),
                (b {name: 'Stefan'}),
                (c {name: 'Peter', age: 34}),
                (d {name: 'Emil'}),

                (a)-[:KNOWS]->(c),
                (b)-[:KNOWS]->(a),
                (d)-[:KNOWS]->(c)
      """.stripMargin)
    synopsis("The `SET` clause is used to update labels on nodes and properties on nodes and relationships.")
    p(
      """* <<query-set-introduction,Introduction>>
        |* <<set-set-a-property, Set a property>>
        |* <<set-remove-a-property, Remove a property>>
        |* <<set-copying-properties-between-nodes-and-relationships, Copying properties between nodes and relationships>>
        |* <<set-adding-properties-from-maps, Adding properties from maps>>
        |* <<set-set-a-property-using-a-parameter, Set a property using a parameter>>
        |* <<set-set-all-properties-using-a-parameter, Set all properties using a parameter>>
        |* <<set-set-multiple-properties-using-one-set-clause, Set multiple properties using one `SET` clause>>
        |* <<set-set-a-label-on-a-node, Set a label on a node>>
        |* <<set-set-multiple-labels-on-a-node, Set multiple labels on a node>>""".stripMargin)
    section("Introduction", "query-set-introduction") {
      p("""`SET` can also be used with maps from parameters to set properties.""")
      note {
        p("""Setting labels on a node is an idempotent operations -- if you try to set a label on a node that already has that label on it, nothing happens.
          |The query statistics will tell you if something needed to be done or not.""".stripMargin)
      }
      p("The examples use this graph as a starting point:")
      graphViz()
    }
    section("Set a property", "set-set-a-property") {
      p(
        """To set a property on a node or relationship, use `SET`.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.surname = 'Taylor'
          |RETURN n.name, n.surname""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.surname" -> "Taylor")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        p("The newly changed node is returned by the query.")
        resultTable()
      }
    }
    section("Remove a property", "set-remove-a-property") {
      p(
        """Normally you remove a property by using `<<query-remove,REMOVE>>`, but it's sometimes convenient to do it using the `SET` command.
          |One example is if the property comes from a parameter.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.name = null
          |RETURN n.name, n.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> null, "n.age" -> 36)))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        p("The node is returned by the query, and the name property is now missing.")
        resultTable()
      }
    }
    section("Copying properties between nodes and relationships", "set-copying-properties-between-nodes-and-relationships") {
      p(
        """You can also use `SET` to copy all properties from one graph element to another.
          |Doing this will remove all other properties on the receiving graph element.""".stripMargin)
      query(
        """MATCH (at {name: 'Andres'}), (pn {name: 'Peter'})
          |SET at = pn
          |RETURN at.name, at.age, at.hungry, pn.name, pn.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("at.name" -> "Peter", "at.age" -> 34, "at.hungry" -> null, "pn.name" -> "Peter", "pn.age" -> 34)))
          assertStats(r, propertiesWritten = 3, nodesCreated = 0)
        })) {
        p("The *'Andres'* node has had all its properties replaced by the properties in the *'Peter'* node.")
        resultTable()
      }
    }
    section("Adding properties from maps", "set-adding-properties-from-maps") {
      p(
        """When setting properties from a map (literal, parameter, or graph element), you can use the `+=` form of `SET` to only add properties, and not remove any of the existing properties on the graph element.""".stripMargin)
      query(
        """MATCH (p {name: 'Peter'})
          |SET p += {hungry: true, position: 'Entrepreneur'}""".stripMargin, ResultAssertions((r) => {
          assertStats(r, propertiesWritten = 2, nodesCreated = 0)
        })) {
        resultTable()
      }
    }
    section("Set a property using a parameter", "set-set-a-property-using-a-parameter") {
      p(
        """Use a parameter to give the value of a property.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.surname = $surname
          |RETURN n.name, n.surname""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.surname" -> "Taylor")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        }),
        ("surname", "Taylor")) {
        p("The *'Andres'* node has got a surname added.")
        resultTable()
      }
    }
    section("Set all properties using a parameter", "set-set-all-properties-using-a-parameter") {
      p(
        """This will replace all existing properties on the node with the new set provided by the parameter.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n = $props
          |RETURN n.name, n.position, n.age, n.hungry""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.position" -> "Developer", "n.age" -> null, "n.hungry" -> null)))
          assertStats(r, propertiesWritten = 4, nodesCreated = 0)
        }),
        ("props", Map("name" -> "Andres", "position" -> "Developer"))) {
        p("The *'Andres'* node has had all its properties replaced by the properties in the `props` parameter.")
        resultTable()
      }
    }
    section("Set multiple properties using one `SET` clause", "set-set-multiple-properties-using-one-set-clause") {
      p(
        """If you want to set multiple properties in one go, simply separate them with a comma.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.position = 'Developer', n.surname = 'Taylor'""".stripMargin, ResultAssertions((r) => {
          assertStats(r, propertiesWritten = 2, nodesCreated = 0)
        })){
        resultTable()
      }
    }
    section("Set a label on a node", "set-set-a-label-on-a-node") {
      p(
        """To set a label on a node, use `SET`.""".stripMargin)
      query(
        """MATCH (n {name: 'Stefan'})
          |SET n:German
          |RETURN n.name, labels(n) AS labels""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Stefan", "labels" -> List("German"))))
          assertStats(r, propertiesWritten = 0, nodesCreated = 0, labelsAdded = 1)
        })) {
        p("The newly labeled node is returned by the query.")
        resultTable()
      }
    }
    section("Set multiple labels on a node", "set-set-multiple-labels-on-a-node") {
      p(
        """To set multiple labels on a node, use `SET` and separate the different labels using `:`.""".stripMargin)
      query(
        """MATCH (n {name: 'Emil'})
          |SET n:Swedish:Bossman
          |RETURN n.name, labels(n) AS labels""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Emil", "labels" -> List("Swedish", "Bossman"))))
          assertStats(r, propertiesWritten = 0, nodesCreated = 0, labelsAdded = 2)
        })) {
        p("The newly labeled node is returned by the query.")
        resultTable()
      }
    }
  }.build()
}
