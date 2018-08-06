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
        |* <<set-update-a-property, Update a property>>
        |* <<set-remove-a-property, Remove a property>>
        |* <<set-copying-properties-between-nodes-and-relationships, Copying properties between nodes and relationships>>
        |* <<set-adding-properties-from-maps, Adding properties from maps using `+=`>>
        |* <<set-set-multiple-properties-using-one-set-clause, Set multiple properties using one `SET` clause>>
        |* <<set-set-a-property-using-a-parameter, Set a property using a parameter>>
        |* <<set-set-all-properties-using-a-parameter, Set all properties using a parameter>>
        |* <<set-set-a-label-on-a-node, Set a label on a node>>
        |* <<set-set-multiple-labels-on-a-node, Set multiple labels on a node>>""".stripMargin)
    section("Introduction", "query-set-introduction") {
      p("""`SET` can be used with a map -- provided as a literal, or a parameter, or graph element -- to set properties.""")
      note {
        p("""Setting labels on a node is an idempotent operation -- nothing will occur if an attempt is made to set a label on a node that already has that label.
          |The query statistics will state whether any updates actually took place.""".stripMargin)
      }
      p("The examples use this graph as a starting point:")
      graphViz()
    }
    section("Set a property", "set-set-a-property") {
      p(
        """Use `SET` to set a property on a node or relationship:""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.surname = 'Taylor'
          |RETURN n.name, n.surname""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.surname" -> "Taylor")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        p("The newly-changed node is returned by the query.")
        resultTable()
      }
      p(
        """It is possible to set a property on a graph element using more complex expressions.
          |For instance, in contrast to specifying the node directly, the following query shows how to set a property for a node selected by an expression: """.stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET (CASE WHEN n.age = 36 THEN n END).worksIn = 'Malmo'
          |RETURN n.name, n.worksIn""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.worksIn" -> "Malmo")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        resultTable()
      }
    }
    section("Update a property", "set-update-a-property") {
      p(
        """`SET` can be used to update a property on a node or relationship.
          |This query forces a change of type in the `age` property: """.stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.age = toString(n.age)
          |RETURN n.name, n.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.age" -> "36")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        p("The `age` property has been converted to the string `'36'`.")
        resultTable()
      }
    }
    section("Remove a property", "set-remove-a-property") {
      p(
        """Although `<<query-remove, REMOVE>>` is normally used to remove a property, it's sometimes convenient to do it using the `SET` command.
          |A case in point is if the property is provided by a parameter.""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.name = null
          |RETURN n.name, n.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> null, "n.age" -> 36)))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        })) {
        p("The `name` property is now missing.")
        resultTable()
      }
    }
    section("Copying properties between nodes and relationships", "set-copying-properties-between-nodes-and-relationships") {
      p(
        """`SET` can be used to copy all properties from one graph element to another.
          |This will remove _all_ other properties on the graph element being copied to.""".stripMargin)
      query(
        """MATCH (at {name: 'Andres'}), (pn {name: 'Peter'})
          |SET at = pn
          |RETURN at.name, at.age, at.hungry, pn.name, pn.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("at.name" -> "Peter", "at.age" -> 34, "at.hungry" -> null, "pn.name" -> "Peter", "pn.age" -> 34)))
          assertStats(r, propertiesWritten = 3, nodesCreated = 0)
        })) {
        p("The *'Andres'* node has had all its properties replaced by the properties of the *'Peter'* node.")
        resultTable()
      }
    }
    section("Adding properties from maps using `+=`", "set-adding-properties-from-maps") {
      p(
        """The property mutation operator `+=` can be used with `SET` to set properties from a map in a granular fashion:
          |
          |* Any properties in the map that are not on the graph element will be _added_ to the graph element.
          |* Any properties not in the map that are on the graph element will be left as is; i.e. not removed from the graph element.
          |* Any properties that are in both the map and the graph element will be _replaced_ in the graph element.""".stripMargin)
      query(
        """MATCH (p {name: 'Peter'})
          |SET p += {age: 38, hungry: true, position: 'Entrepreneur'}
          |RETURN p.name, p.age, p.hungry, p.position""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("p.name" -> "Peter", "p.age" -> 38, "p.hungry" -> true, "p.position" -> "Entrepreneur")))
          assertStats(r, propertiesWritten = 3, nodesCreated = 0)
        })) {
        p("This query left the `name` property unchanged, updated the `age` property from `34` to `38`, and added the `hungry` and `position` properties to the *'Peter'* node.")
        resultTable()
      }
    }
    section("Set multiple properties using one `SET` clause", "set-set-multiple-properties-using-one-set-clause") {
      p(
        """Set multiple properties at once by separating them with a comma:""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.position = 'Developer', n.surname = 'Taylor'""".stripMargin, ResultAssertions((r) => {
          assertStats(r, propertiesWritten = 2, nodesCreated = 0)
        })){
        resultTable()
      }
    }
    section("Set a property using a parameter", "set-set-a-property-using-a-parameter") {
      p(
        """Use a parameter to set the value of a property:""".stripMargin)
      query(
        """MATCH (n {name: 'Andres'})
          |SET n.surname = $surname
          |RETURN n.name, n.surname""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andres", "n.surname" -> "Taylor")))
          assertStats(r, propertiesWritten = 1, nodesCreated = 0)
        }),
        ("surname", "Taylor")) {
        p("A `surname` property has been added to the *'Andres'* node.")
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
    section("Set a label on a node", "set-set-a-label-on-a-node") {
      p(
        """Use `SET` to set a label on a node:""".stripMargin)
      query(
        """MATCH (n {name: 'Stefan'})
          |SET n:German
          |RETURN n.name, labels(n) AS labels""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Stefan", "labels" -> List("German"))))
          assertStats(r, propertiesWritten = 0, nodesCreated = 0, labelsAdded = 1)
        })) {
        p("The newly-labeled node is returned by the query.")
        resultTable()
      }
    }
    section("Set multiple labels on a node", "set-set-multiple-labels-on-a-node") {
      p(
        """Set multiple labels on a node with `SET` and use `:` to separate the different labels:""".stripMargin)
      query(
        """MATCH (n {name: 'Emil'})
          |SET n:Swedish:Bossman
          |RETURN n.name, labels(n) AS labels""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Emil", "labels" -> List("Swedish", "Bossman"))))
          assertStats(r, propertiesWritten = 0, nodesCreated = 0, labelsAdded = 2)
        })) {
        p("The newly-labeled node is returned by the query.")
        resultTable()
      }
    }
  }.build()
}
