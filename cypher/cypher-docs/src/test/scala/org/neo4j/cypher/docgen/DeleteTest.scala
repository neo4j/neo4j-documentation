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

class DeleteTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("DELETE", "query-delete")
    initQueries("""CREATE
                  #  (a:Person {name: 'Andy', age: 36}),
                  #  (p:Person {name: 'Timothy', age: 25}),
                  #  (t:Person {name: 'Peter', age: 34}),
                  #  (z:Person {name: 'UNKNOWN'}),
                  #  (a)-[:KNOWS]->(t),
                  #  (a)-[:KNOWS]->(p)""".stripMargin('#'))
    synopsis("The `DELETE` clause is used to delete nodes, relationships or paths.")
    p("""* <<query-delete-introduction, Introduction>>
        #* <<delete-delete-single-node, Delete a single node>>
        #* <<delete-delete-all-nodes-and-relationships, Delete all nodes and relationships>>
        #* <<delete-delete-a-node-with-all-its-relationships, Delete a node with all its relationships>>
        #* <<delete-delete-relationships-only, Delete relationships only>>""".stripMargin('#'))
    section("Introduction", "query-delete-introduction") {
      p("""For removing properties and labels, see <<query-remove>>.
          #Remember that you cannot delete a node without also deleting relationships that start or end on said node.
          #Either explicitly delete the relationships, or use `DETACH DELETE`.""".stripMargin('#'))
      p("The examples start out with the following database:")
      graphViz()
    }
    section("Delete single node", "delete-delete-single-node") {
      p("To delete a node, use the `DELETE` clause.")
      query("""MATCH (n:Person {name: 'UNKNOWN'})
              #DELETE n""".stripMargin('#'),
      ResultAssertions((r) => {
          assertStats(r, nodesDeleted = 1)
        })) {
        resultTable()
      }
    }
    section("Delete all nodes and relationships", "delete-delete-all-nodes-and-relationships") {
      important(p(
        """This query is not for deleting large amounts of data, but is useful when experimenting with small example data sets.
          |When deleting large amounts of data, use <<delete-with-call-in-transactions, CALL { } IN TRANSACTIONS>>.""".stripMargin))
      query("""MATCH (n)
              #DETACH DELETE n""".stripMargin('#'),
      ResultAssertions((r) => {
          assertStats(r, relationshipsDeleted = 2, nodesDeleted = 4)
        })) {
        resultTable()
      }
    }
    section("Delete a node with all its relationships", "delete-delete-a-node-with-all-its-relationships") {
      p("When you want to delete a node and any relationship going to or from it, use `DETACH DELETE`.")
      query("""MATCH (n {name: 'Andy'})
              #DETACH DELETE n""".stripMargin('#'),
      ResultAssertions((r) => {
          assertStats(r, relationshipsDeleted = 2, nodesDeleted = 1)
        })) {
        resultTable()
      }
      note {
        p("For `DETACH DELETE` for users with restricted security privileges, see <<operations-manual#detach-delete-restricted-user, Operations Manual -> Fine-grained access control>>.")
      }
    }
    section("Delete relationships only", "delete-delete-relationships-only") {
      p("It is also possible to delete relationships only, leaving the node(s) otherwise unaffected.")
      query("""MATCH (n {name: 'Andy'})-[r:KNOWS]->()
              #DELETE r""".stripMargin('#'),
      ResultAssertions((r) => {
          assertStats(r, relationshipsDeleted = 2, nodesDeleted = 0)
        })) {
        p("""This deletes all outgoing `KNOWS` relationships from the node with the name *'Andy'*.""")
        resultTable()
      }
    }

  }.build()

}
