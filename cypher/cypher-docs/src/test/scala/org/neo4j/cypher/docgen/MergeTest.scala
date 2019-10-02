/*
 * Copyright (c) 2002-2019 "Neo4j,"
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

class MergeTest extends DocumentingTest with QueryStatisticsTestSupport {

  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("MERGE", "query-merge")
    initQueries(
      "CREATE CONSTRAINT ON (person:Person) ASSERT person.name IS UNIQUE",
      "CREATE CONSTRAINT ON (movie:Movie) ASSERT movie.title IS UNIQUE",
      """CREATE
        | (charlie:Person {name: 'Charlie Sheen', bornIn: 'New York', chauffeurName: 'John Brown'}),
        | (martin:Person  {name: 'Martin Sheen', bornIn: 'Ohio', chauffeurName: 'Bob Brown'}),
        | (michael:Person {name: 'Michael Douglas', bornIn: 'New Jersey', chauffeurName: 'John Brown'}),
        | (oliver:Person  {name: 'Oliver Stone', bornIn: 'New York', chauffeurName: 'Bill White'}),
        | (rob:Person     {name: 'Rob Reiner', bornIn: 'New York', chauffeurName: 'Ted Green'}),
        | (wallStreet:Movie           {title: 'Wall Street'}),
        | (theAmericanPresident:Movie {title: 'The American President'}),
        | (charlie)-[:ACTED_IN]->(wallStreet),
        | (martin)-[:ACTED_IN]->(wallStreet),
        | (michael)-[:ACTED_IN]->(wallStreet),
        | (martin)-[:ACTED_IN]->(theAmericanPresident),
        | (michael)-[:ACTED_IN]->(theAmericanPresident),
        | (oliver)-[:ACTED_IN]->(wallStreet),
        | (rob)-[:ACTED_IN]->(theAmericanPresident),
        | (charlie)-[:FATHER]->(martin)""".stripMargin)
    synopsis("""The `MERGE` clause ensures that a pattern exists in the graph.
               |Either the pattern already exists, or it needs to be created.""")
    p("""* <<query-merge-introduction, Introduction>>
        |* <<query-merge-node-derived, Merge nodes>>
        |** <<merge-merge-single-node-with-a-label, Merge single node with a label>>
        |** <<merge-merge-single-node-with-properties, Merge single node with properties>>
        |** <<merge-merge-single-node-specifying-both-label-and-property, Merge single node specifying both label and property>>
        |** <<merge-merge-single-node-derived-from-an-existing-node-property, Merge single node derived from an existing node property>>
        |* <<query-merge-on-create-on-match, Use `ON CREATE` and `ON MATCH`>>
        |** <<merge-merge-with-on-create, Merge with `ON CREATE`>>
        |** <<merge-merge-with-on-match, Merge with `ON MATCH`>>
        |** <<merge-merge-with-on-create-and-on-match, Merge with `ON CREATE` and `ON MATCH`>>
        |** <<merge-merge-with-on-match-setting-multiple-properties, Merge with `ON MATCH` setting multiple properties>>
        |* <<query-merge-relationships, Merge relationships>>
        |** <<merge-merge-on-a-relationship, Merge on a relationship>>
        |** <<merge-merge-on-multiple-relationships, Merge on multiple relationships>>
        |** <<merge-merge-on-an-undirected-relationship, Merge on an undirected relationship>>
        |** <<merge-merge-on-a-relationship-between-two-existing-nodes, Merge on a relationship between two existing nodes>>
        |** <<merge-merge-on-a-relationship-between-an-existing-node-and-a-merged-node-derived-from-a-node-property, Merge on a relationship between an existing node and a merged node derived from a node property>>
        |* <<query-merge-using-unique-constraints, Using unique constraints with `MERGE`>>
        |** <<merge-merge-using-unique-constraints-creates-a-new-node-if-no-node-is-found, Merge using unique constraints creates a new node if no node is found>>
        |** <<merge-merge-using-unique-constraints-matches-an-existing-node, Merge using unique constraints matches an existing node>>
        |** <<merge-merge-with-unique-constraints-and-partial-matches, Merge with unique constraints and partial matches>>
        |** <<merge-merge-with-unique-constraints-and-conflicting-matches, Merge with unique constraints and conflicting matches>>
        |* <<merge-using-map-parameters-with-merge, Using map parameters with `MERGE`>>""")

    section("Introduction", "query-merge-introduction") {
      p("""`MERGE` either matches existing nodes and binds them, or it creates new data and binds that.
          |It's like a combination of `MATCH` and `CREATE` that additionally allows you to specify what happens if the data was matched or created.""")
      p("""For example, you can specify that the graph must contain a node for a user with a certain name.
          |If there isn't a node with the correct name, a new node will be created and its name property set.""")
      p("""When using `MERGE` on full patterns, the behavior is that either the whole pattern matches, or the whole pattern is created.
          |`MERGE` will not partially use existing patterns -- it's all or nothing.
          |If partial matches are needed, this can be accomplished by splitting a pattern up into multiple `MERGE` clauses.""")
      p("""As with `MATCH`, `MERGE` can match multiple occurrences of a pattern.
          |If there are multiple matches, they will all be passed on to later stages of the query.""")
      p("""The last part of `MERGE` is the `ON CREATE` and `ON MATCH`.
          |These allow a query to express additional changes to the properties of a node or relationship, depending on if the element was `MATCH` -ed in the database or if it was `CREATE` -ed.""")
      p("The following graph is used for the examples below:")
      graphViz()
    }

    section("Merge nodes", "query-merge-node-derived") {
      section("Merge single node with a label", "merge-merge-single-node-with-a-label") {
        p("Merging a single node with the given label.")
        query(
          """MERGE (robert:Critic)
            |RETURN robert, labels(robert)""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, labelsAdded = 1)
          })) {
          p("A new node is created because there are no nodes labeled `Critic` in the database.")
          resultTable()
        }
      }

      section("Merge single node with properties", "merge-merge-single-node-with-properties") {
        p("Merging a single node with properties where not all properties match any existing node.")
        query(
          """MERGE (charlie {name: 'Charlie Sheen', age: 10})
            |RETURN charlie""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, propertiesWritten = 2)
          })) {
          p("""A new node with the name *'Charlie Sheen'* will be created since not all properties matched the existing *'Charlie Sheen'* node.""")
          resultTable()
        }
      }

      section("Merge single node specifying both label and property", "merge-merge-single-node-specifying-both-label-and-property") {
        p("Merging a single node with both label and property matching an existing node.")
        query(
          """MERGE (michael:Person {name: 'Michael Douglas'})
            |RETURN michael.name, michael.bornIn""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 0, propertiesWritten = 0)
          })) {
          p("*'Michael Douglas'* will be matched and the `name` and  `bornIn` properties returned.")
          resultTable()
        }
      }

      section("Merge single node derived from an existing node property", "merge-merge-single-node-derived-from-an-existing-node-property") {
        p("For some property 'p' in each bound node in a set of nodes, a single new node is created for each unique value for 'p'.")
        query(
          """MATCH (person:Person)
            |MERGE (city:City {name: person.bornIn})
            |RETURN person.name, person.bornIn, city""".stripMargin, ResultAssertions((r) => {
            assertStats(r, nodesCreated = 3, propertiesWritten = 3, labelsAdded = 3)
          })) {
          p("""Three nodes labeled `City` are created, each of which contains a `name` property with the value of *'New York'*, *'Ohio'*, and *'New Jersey'*, respectively.
              |Note that even though the `MATCH` clause results in three bound nodes having the value *'New York'* for the `bornIn` property, only a single *'New York'* node (i.e. a `City` node with a name of *'New York'*) is created.
              |As the *'New York'* node is not matched for the first bound node, it is created.
              |However, the newly-created *'New York'* node is matched and bound for the second and third bound nodes.""")
          resultTable()
        }
      }
    }

    section("Use `ON CREATE` and `ON MATCH`", "query-merge-on-create-on-match") {
      section("Merge with `ON CREATE`", "merge-merge-with-on-create") {
        p("Merge a node and set properties if the node needs to be created.")
        query("""MERGE (keanu:Person {name: 'Keanu Reeves'})
                |  ON CREATE SET keanu.created = timestamp()
                |RETURN keanu.name, keanu.created""",
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, propertiesWritten = 2, labelsAdded = 1)
          })) {
          p("The query creates the *'keanu'* node and sets a timestamp on creation time.")
          resultTable()
        }
      }

      section("Merge with `ON MATCH`", "merge-merge-with-on-match") {
        p("Merging nodes and setting properties on found nodes.")
        query(
          """MERGE (person:Person)
            |  ON MATCH SET person.found = true
            |RETURN person.name, person.found""".stripMargin,
          ResultAssertions((r) => {
            assertStats(r, propertiesWritten = 5)
          })) {
          p("The query finds all the `Person` nodes, sets a property on them, and returns them.")
          resultTable()
        }
      }

      section("Merge with `ON CREATE` and `ON MATCH`", "merge-merge-with-on-create-and-on-match") {
        query(
          """MERGE (keanu:Person {name: 'Keanu Reeves'})
            |  ON CREATE SET keanu.created = timestamp()
            |  ON MATCH SET keanu.lastSeen = timestamp()
            |RETURN keanu.name, keanu.created, keanu.lastSeen""",
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, propertiesWritten = 2, labelsAdded = 1)
          })) {
          p(
            """The query creates the *'keanu'* node, and sets a timestamp on creation time.
              |If *'keanu'* had already existed, a different property would have been set.""")
          resultTable()
        }
      }

      section("Merge with `ON MATCH` setting multiple properties", "merge-merge-with-on-match-setting-multiple-properties") {
        p("If multiple properties should be set, simply separate them with commas.")
        query(
          """MERGE (person:Person)
            |ON MATCH SET person.found = true, person.lastAccessed = timestamp()
            |RETURN person.name, person.found, person.lastAccessed""",
          ResultAssertions((r) => {
            assertStats(r, propertiesWritten = 10)
          })) {
          resultTable()
        }
      }
    }

    section("Merge relationships", "query-merge-relationships") {
      section("Merge on a relationship", "merge-merge-on-a-relationship") {
        p("`MERGE` can be used to match or create a relationship.")
        query(
          """MATCH (charlie:Person {name: 'Charlie Sheen'}), (wallStreet:Movie {title: 'Wall Street'})
            |MERGE (charlie)-[r:ACTED_IN]->(wallStreet)
            |RETURN charlie.name, type(r), wallStreet.title""",
          ResultAssertions((r) => {
            assertStats(r, relationshipsCreated = 0)
          })) {
          p("""*'Charlie Sheen'* had already been marked as acting in *'Wall Street'*, so the existing relationship is found and returned.
              |Note that in order to match or create a relationship when using `MERGE`, at least one bound node must be specified, which is done via the `MATCH` clause in the above example.""")
          resultTable()
        }
      }

      section("Merge on multiple relationships", "merge-merge-on-multiple-relationships") {
        query(
          """MATCH (oliver:Person {name: 'Oliver Stone'}), (reiner:Person {name: 'Rob Reiner'})
            |MERGE (oliver)-[:DIRECTED]->(movie:Movie)<-[:ACTED_IN]-(reiner)
            |RETURN movie""",
          ResultAssertions((r) => {
            assertStats(r, relationshipsCreated = 2, nodesCreated = 1, propertiesWritten = 0, labelsAdded = 1)
          })) {
          p("""In our example graph, *'Oliver Stone'* and *'Rob Reiner'* have never worked together.
              |When we try to `MERGE` a "movie between them, Neo4j will not use any of the existing movies already connected to either person.
              |Instead, a new *'movie'* node is created.""")
          resultTable()
        }
      }

      section("Merge on an undirected relationship", "merge-merge-on-an-undirected-relationship") {
        p(
          """`MERGE` can also be used with an undirected relationship.
            |When it needs to create a new one, it will pick a direction.""")
        query(
          """MATCH (charlie:Person {name: 'Charlie Sheen'}), (oliver:Person {name: 'Oliver Stone'})
            |MERGE (charlie)-[r:KNOWS]-(oliver)
            |RETURN r""",
          ResultAssertions((r) => {
            assertStats(r, relationshipsCreated = 1)
          })) {
          p(
            """As *'Charlie Sheen'* and *'Oliver Stone'* do not know each other this `MERGE` query will create a `KNOWS` relationship between them.
              |The direction of the created relationship is arbitrary.""")
          resultTable()
        }
      }

      section("Merge on a relationship between two existing nodes", "merge-merge-on-a-relationship-between-two-existing-nodes") {
        p("`MERGE` can be used in conjunction with preceding `MATCH` and `MERGE` clauses to create a relationship between two bound nodes 'm' and 'n', where 'm' is returned by `MATCH` and 'n' is created or matched by the earlier `MERGE`.")
        query(
          """MATCH (person:Person)
            |MERGE (city:City {name: person.bornIn})
            |MERGE (person)-[r:BORN_IN]->(city)
            |RETURN person.name, person.bornIn, city""",
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 3, propertiesWritten = 3, labelsAdded = 3, relationshipsCreated = 5)
          })) {
          p("""This builds on the example from <<merge-merge-single-node-derived-from-an-existing-node-property>>.
              |The second `MERGE` creates a `BORN_IN` relationship between each person and a city corresponding to the value of the person’s `bornIn` property. *'Charlie Sheen'*, *'Rob Reiner'* and *'Oliver Stone'* all have a `BORN_IN` relationship to the 'same' `City` node (*'New York'*).""")
          resultTable()
        }
      }

      section("Merge on a relationship between an existing node and a merged node derived from a node property", "merge-merge-on-a-relationship-between-an-existing-node-and-a-merged-node-derived-from-a-node-property") {
        p("`MERGE` can be used to simultaneously create both a new node 'n' and a relationship between a bound node 'm' and 'n'.")
        query(
          """MATCH (person:Person)
            |MERGE (person)-[r:HAS_CHAUFFEUR]->(chauffeur:Chauffeur {name: person.chauffeurName})
            |RETURN person.name, person.chauffeurName, chauffeur""",
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 5, propertiesWritten = 5, labelsAdded = 5, relationshipsCreated = 5)
          })) {
          p("""As `MERGE` found no matches -- in our example graph, there are no nodes labeled with `Chauffeur` and no `HAS_CHAUFFEUR` relationships -- `MERGE` creates five nodes labeled with `Chauffeur`, each of which contains a `name` property whose value corresponds to each matched `Person` node's `chauffeurName` property value.
              |`MERGE` also creates a `HAS_CHAUFFEUR` relationship between each `Person` node and the newly-created corresponding `Chauffeur` node.
              |As *'Charlie Sheen'* and *'Michael Douglas'* both have a chauffeur with the same name -- *'John Brown'* -- a new node is created in each case, resulting in 'two' `Chauffeur` nodes having a `name` of *'John Brown'*, correctly denoting the fact that even though the `name` property may be identical, these are two separate people.
              |This is in contrast to the example shown above in <<merge-merge-on-a-relationship-between-two-existing-nodes>>, where we used the first `MERGE` to bind the `City` nodes to prevent them from being recreated (and thus duplicated) in the second `MERGE`.""")
          resultTable()
        }
      }
    }

    section("Using unique constraints with `MERGE`", "query-merge-using-unique-constraints") {
      p("""Cypher prevents getting conflicting results from `MERGE` when using patterns that involve unique constraints.
          |In this case, there must be at most one node that matches that pattern.""")
      p("""For example, given two unique constraints on `:Person(id)` and `:Person(ssn)`, a query such as `MERGE (n:Person {id: 12, ssn: 437})` will fail, if there are two different nodes (one with `id` 12 and one with `ssn` 437) or if there is only one node with only one of the properties.
          |In other words, there must be exactly one node that matches the pattern, or no matching nodes.""")
      p("Note that the following examples assume the existence of unique constraints that have been created using:")
      p(
        """[source,cypher]
          |----
          |CREATE CONSTRAINT ON (n:Person) ASSERT n.name IS UNIQUE;
          |CREATE CONSTRAINT ON (n:Person) ASSERT n.role IS UNIQUE;
          |----"""
      )

      section("Merge using unique constraints creates a new node if no node is found", "merge-merge-using-unique-constraints-creates-a-new-node-if-no-node-is-found") {
        p("""Merge using unique constraints creates a new node if no node is found.""")
        query(
          """MERGE (laurence:Person {name: 'Laurence Fishburne'})
            |RETURN laurence.name""".stripMargin,
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, propertiesWritten = 1, labelsAdded = 1)
          })) {
          p(
            """The query creates the *'laurence'* node.
              |If *'laurence'* had already existed, `MERGE` would just match the existing node.""".stripMargin)
          resultTable()
        }
      }

      section("Merge using unique constraints matches an existing node", "merge-merge-using-unique-constraints-matches-an-existing-node") {
        p("Merge using unique constraints matches an existing node.")
        query(
          """MERGE (oliver:Person {name: 'Oliver Stone'})
            |RETURN oliver.name, oliver.bornIn""".stripMargin,
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 0, propertiesWritten = 0, labelsAdded = 0)
          })) {
          p("""The *'oliver'* node already exists, so `MERGE` just matches it.""")
          resultTable()
        }
      }

      section("Merge with unique constraints and partial matches", "merge-merge-with-unique-constraints-and-partial-matches") {
        p("Merge using unique constraints fails when finding partial matches.")
        query(
          """MERGE (michael:Person {name: 'Michael Douglas', role: 'Gordon Gekko'})
            |RETURN michael""".stripMargin,
          ResultAssertions((r) => {

          }))
        {
          p("""While there is a matching unique *'michael'* node with the name *'Michael Douglas'*, there is no unique node with the role of *'Gordon Gekko'* and `MERGE` fails to match.""")
          p(""".Error message
              |[source]
              |----
              |Merge did not find a matching node michael and can not create a new node due to
              |conflicts with existing unique nodes
              |----""")
        }
        p("""If we want to give Michael Douglas the role of Gordon Gekko, we can use the `SET` clause instead:""")
        query(
          """MERGE (michael:Person {name: 'Michael Douglas'})
            |SET michael.role = 'Gordon Gekko'""".stripMargin,
          ResultAssertions((r) => {

          }))
        {}
      }

      section("Merge with unique constraints and conflicting matches", "merge-merge-with-unique-constraints-and-conflicting-matches") {
        p("Merge using unique constraints fails when finding conflicting matches.")
        query(
          """MERGE (oliver:Person {name: 'Oliver Stone', role: 'Gordon Gekko'})
            |RETURN oliver""".stripMargin,
          ResultAssertions((r) => {

          })) {
          p("""While there is a matching unique *'oliver'* node with the name *'Oliver Stone'*, there is also another  unique node with the role of *'Gordon Gekko'* and `MERGE` fails to match.""")
          p(""".Error message
              |[source]
              |----
              |Merge did not find a matching node oliver and can not create a new node due to
              |conflicts with existing unique nodes
              |----""")
        }
      }

      section("Using map parameters with `MERGE`", "merge-using-map-parameters-with-merge") {
        p(
          """`MERGE` does not support map parameters the same way `CREATE` does.
            |To use map parameters with `MERGE`, it is necessary to explicitly use the expected properties, such as in the following example.
            |For more information on parameters, see <<cypher-parameters>>.""")
        query(
          """MERGE (person:Person {name: $param.name, role: $param.role})
            |RETURN person.name, person.role""".stripMargin,
          ResultAssertions((r) => {
            assertStats(r, nodesCreated = 1, propertiesWritten = 2, labelsAdded = 1)
          }), ("param", Map("name" -> "Keanu Reeves", "role" -> "Neo"))) {
          resultTable()
        }
      }
    }
  }.build()
}
