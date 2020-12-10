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

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Path

class ShortestPathPlanningTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/execution-plan-groups/"

  override def doc = new DocBuilder {
    doc("Shortest path planning", "query-shortestpath-planning")
    initQueries(
      """CREATE (KevinB:Person {name: 'Kevin Bacon'}),
        |       (JackN:Person {name: 'Jack Nicholson'}),
        |       (Keanu:Person {name: 'Keanu Reeves'}),
        |       (Al:Person {name: 'Al Pacino'}),
        |       (NancyM:Person {name: 'Nancy Meyers'}),
        |       (RobR:Person {name: 'Rob Reiner'}),
        |       (Taylor:Person {name: 'Taylor Hackford'}),
        |
        |       (AFewGoodMen:Movie {title: 'A Few Good Men'}),
        |       (JackN)-[:ACTED_IN {role: 'Col. Nathan R. Jessup'}]->(AFewGoodMen),
        |       (KevinB)-[:ACTED_IN {role: 'Capt. Jack Ross'}]->(AFewGoodMen),
        |       (RobR)-[:DIRECTED]->(AFewGoodMen),
        |
        |       (SomethingsGottaGive:Movie {title: 'Something´s Gotta Give'}),
        |       (JackN)-[:ACTED_IN {role: 'Harry Sanborn'}]->(SomethingsGottaGive),
        |       (Keanu)-[:ACTED_IN {role: 'Julian Mercer'}]->(SomethingsGottaGive),
        |       (NancyM)-[:DIRECTED]->(SomethingsGottaGive),
        |
        |       (TheDevilsAdvocate:Movie {title: 'The Devil´s Advocate'}),
        |       (Keanu)-[:ACTED_IN {role: 'Kevin Lomax'}]->(TheDevilsAdvocate),
        |       (Al)-[:ACTED_IN {role: 'John Milton'}]->(TheDevilsAdvocate)""", "CREATE INDEX FOR (n:Person) ON (n.name)")
    synopsis("Shortest path finding in Cypher and how it is planned.")
    p(
      """Planning shortest paths in Cypher can lead to different query plans depending on the predicates that need
        |to be evaluated. Internally, Neo4j will use a fast bidirectional breadth-first search algorithm if the
        |predicates can be evaluated whilst searching for the path. Therefore, this fast algorithm will always
        |be certain to return the right answer when there are universal predicates on the path; for example, when
        |searching for the shortest path where all nodes have the `Person` label, or where there are no nodes with
        |a `name` property.""".stripMargin)
    p(
      """If the predicates need to inspect the whole path before deciding on whether it is valid or not, this fast
        |algorithm cannot be relied on to find the shortest path, and Neo4j may have to resort to using a slower
        |exhaustive depth-first search algorithm to find the path. This means that query plans for shortest path
        |queries with non-universal predicates will include a fallback to running the exhaustive search to find
        |the path should the fast algorithm not succeed. For example, depending on the data, an answer to a shortest
        |path query with existential predicates -- such as the requirement that at least one node contains the property
        |`name='Kevin Bacon'` -- may not be able to be found by the fast algorithm. In this case, Neo4j will fall back to using
        |the exhaustive search to enumerate all paths and potentially return an answer.""".stripMargin)
    p(
      """The running times of these two algorithms may differ by orders of magnitude, so it is important to ensure
        |that the fast approach is used for time-critical queries.""".stripMargin)
    p(
      """When the exhaustive search is planned, it is still only executed when the fast algorithm fails to find any
        |matching paths. The fast algorithm is always executed first, since it is possible that it can find a valid
        |path even though that could not be guaranteed at planning time.""".stripMargin)
    p(
      """Please note that falling back to the exhaustive search may prove to be a very time consuming strategy in some
        |cases; such as when there is no shortest path between two nodes.
        |Therefore, in these cases, it is recommended to set `cypher.forbid_exhaustive_shortestpath` to `true`,
        |as explained in <<operations-manual#config_cypher.forbid_exhaustive_shortestpath, Operations Manual -> Configuration settings>>""".stripMargin)
    section("Shortest path with fast algorithm") {
      query(
        """MATCH (KevinB:Person {name: 'Kevin Bacon'} ),
          |      (Al:Person {name: 'Al Pacino'}),
          |      p = shortestPath((KevinB)-[:ACTED_IN*]-(Al))
          |WHERE all(r IN relationships(p) WHERE r.role IS NOT NULL)
          |RETURN p""", assertShortestPathLength) {
        p(
          """This query can be evaluated with the fast algorithm -- there are no predicates that need to see the whole
            |path before being evaluated.""")
        profileExecutionPlan()
      }
    }
    section("Shortest path with additional predicate checks on the paths") {
      section("Consider using the exhaustive search as a fallback") {
        p(
          """Predicates used in the `WHERE` clause that apply to the shortest path pattern are evaluated before deciding
            |what the shortest matching path is. """)
        query(
          """MATCH (KevinB:Person {name: 'Kevin Bacon'}),
            |      (Al:Person {name: 'Al Pacino'}),
            |      p = shortestPath((KevinB)-[*]-(Al))
            |WHERE length(p) > 1
            |RETURN p""", assertShortestPathLength) {
          p(
            """This query, in contrast with the one above, needs to check that the whole path follows the predicate
              |before we know if it is valid or not, and so the query plan will also include the fallback to the slower
              |exhaustive search algorithm.""")
          profileExecutionPlan()
        }
        p(
          """The way the bigger exhaustive query plan works is by using `Apply`/`Optional` to ensure that when the
            |fast algorithm does not find any results, a `null` result is generated instead of simply stopping the result
            |stream.
            |On top of this, the planner will issue an `AntiConditionalApply`, which will run the exhaustive search
            |if the path variable is pointing to `null` instead of a path.""")
        p(
          """An `ErrorPlan` operator will appear in the execution plan in cases where (i)
            |`cypher.forbid_exhaustive_shortestpath` is set to `true`, and (ii) the fast algorithm is not able to find the shortest path.""".stripMargin
        )
      }
      section("Prevent the exhaustive search from being used as a fallback") {
        query(
          """MATCH (KevinB:Person {name: 'Kevin Bacon'}),
            |      (Al:Person {name: 'Al Pacino'}),
            |      p = shortestPath((KevinB)-[*]-(Al))
            |WITH p
            |WHERE length(p) > 1
            |RETURN p""", assertShortestPathLength) {
          p(
            """This query, just like the one above, needs to check that the whole path follows the predicate
              |before we know if it is valid or not. However, the inclusion of the `WITH` clause means that the query
              |plan will not include the fallback to the slower exhaustive search algorithm. Instead, any
              |paths found by the fast algorithm will subsequently be filtered, which may result in no answers
              | being returned.""")
          profileExecutionPlan()
        }
      }
    }
  }.build()

  private def assertShortestPathLength = ResultAssertions(result => result.toList.head("p").asInstanceOf[Path].length() shouldBe 6)
}
