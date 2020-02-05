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
import org.neo4j.graphdb.{Node, Path, Relationship}
import org.neo4j.internal.kernel.api.Transaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class MatchTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"
  override def doc = new DocBuilder {
    doc("MATCH", "query-match")
    initQueries(
      """CREATE (charlie:Person {name: 'Charlie Sheen'}),
        |       (martin:Person {name: 'Martin Sheen'}),
        |       (michael:Person {name: 'Michael Douglas'}),
        |       (oliver:Person {name: 'Oliver Stone'}),
        |       (rob:Person {name: 'Rob Reiner'}),
        |
        |       (wallStreet:Movie {title: 'Wall Street'}),
        |
        |       (charlie)-[:ACTED_IN {role: 'Bud Fox'}]->(wallStreet),
        |       (martin)-[:ACTED_IN {role: 'Carl Fox'}]->(wallStreet),
        |       (michael)-[:ACTED_IN {role: 'Gordon Gekko'}]->(wallStreet),
        |       (oliver)-[:DIRECTED]->(wallStreet),
        |
        |       (thePresident:Movie {title: 'The American President'}),
        |
        |       (martin)-[:ACTED_IN {role: 'A.J. MacInerney'}]->(thePresident),
        |       (michael)-[:ACTED_IN {role: 'President Andrew Shepherd'}]->(thePresident),
        |       (rob)-[:DIRECTED]->(thePresident)"""
    )
    synopsis("The `MATCH` clause is used to search for the pattern described in it.")
    p(
      """
        |* <<match-introduction, Introduction>>
        |* <<basic-node-finding, Basic node finding>>
        | ** <<get-all-nodes, Get all nodes>>
        | ** <<get-all-nodes-with-label, Get all nodes with a label>>
        | ** <<related-nodes, Related nodes>>
        | ** <<match-with-labels, Match with labels>>
        |* <<relationship-basics, Relationship basics>>
        | ** <<outgoing-relationships, Outgoing relationships>>
        | ** <<directed-rels-and-variable, Directed relationships and variable>>
        | ** <<match-on-rel-type, Match on relationship type>>
        | ** <<match-on-multiple-rel-types, Match on multiple relationship types>>
        | ** <<match-on-rel-type-use-variable, Match on relationship type and use a variable>>
        |* <<relationships-in-depth, Relationships in depth>>
        | ** <<rel-types-with-uncommon-chars, Relationship types with uncommon characters>>
        | ** <<multiple-rels, Multiple relationships>>
        | ** <<varlength-rels, Variable length relationships>>
        | ** <<rel-variable-in-varlength-rels, Relationship variable in variable length relationships>>
        | ** <<match-props-on-varlength-path, Match with properties on a variable length path>>
        | ** <<zero-length-paths, Zero length paths>>
        | ** <<named-paths, Named paths>>
        | ** <<match-on-bound-rel, Matching on a bound relationship>>
        |* <<query-shortest-path, Shortest path>>
        | ** <<single-shortest-path, Single shortest path>>
        | ** <<single-shortest-path-with-predicates, Single shortest path with predicates>>
        | ** <<all-shortest-paths, All shortest paths>>
        |* <<get-node-rel-by-id, Get node or relationship by id>>
        | ** <<match-node-by-id, Node by id>>
        | ** <<match-rel-by-id, Relationship by id>>
        | ** <<match-multiple-nodes-by-id, Multiple nodes by id>>
      """.stripMargin)
    section("Introduction", "match-introduction") {
      p( """The `MATCH` clause allows you to specify the patterns Neo4j will search for in the database.
           |This is the primary way of getting data into the current set of bindings.
           |It is worth reading up more on the specification of the patterns themselves in <<cypher-patterns>>.""")
      p( """`MATCH` is often coupled to a `WHERE` part which adds restrictions, or predicates, to the `MATCH` patterns, making them more specific.
           |The predicates are part of the pattern description, and should not be considered a filter applied only after the matching is done.
           |_This means that `WHERE` should always be put together with the `MATCH` clause it belongs to._""")
      p( """`MATCH` can occur at the beginning of the query or later, possibly after a `WITH`.
           |If it is the first clause, nothing will have been bound yet, and Neo4j will design a search to find the results matching the clause and any associated predicates specified in any `WHERE` part.
           |This could involve a scan of the database, a search for nodes having a certain label, or a search of an index to find starting points for the pattern matching.
           |Nodes and relationships found by this search are available as _bound pattern elements,_ and can be used for pattern matching of sub-graphs.
           |They can also be used in any further `MATCH` clauses, where Neo4j will use the known elements, and from there find further unknown elements.""")
      p( """Cypher is declarative, and so usually the query itself does not specify the algorithm to use to perform the search.
           |Neo4j will automatically work out the best approach to finding start nodes and matching patterns.
           |Predicates in `WHERE` parts can be evaluated before pattern matching, during pattern matching, or after finding matches.
           |However, there are cases where you can influence the decisions taken by the query compiler.
           |Read more about indexes in <<query-schema-index>>, and more about specifying hints to force Neo4j to solve a query in a specific way in <<query-using>>.""")
      tip {
        p("To understand more about the patterns used in the `MATCH` clause, read <<cypher-patterns>>")
      }
      p("The following graph is used for the examples below:")
      graphViz()
    }
    section("Basic node finding", "basic-node-finding") {
      section("Get all nodes", "get-all-nodes") {
        p("By just specifying a pattern with a single node and no labels, all nodes in the graph will be returned.")
        query("MATCH (n) RETURN n", assertAllNodesReturned) {
          p("Returns all the nodes in the database.")
          resultTable()
        }
      }
      section("Get all nodes with a label", "get-all-nodes-with-label") {
        p("Getting all nodes with a label on them is done with a single node pattern where the node has a label on it.")
        query("MATCH (movie:Movie) RETURN movie.title", assertAllMoviesAreReturned) {
          p("Returns all the movies in the database.")
          resultTable()
        }
      }
      section("Related nodes", "related-nodes") {
        p("The symbol `--` means _related to,_ without regard to type or direction of the relationship.")
        query("MATCH (director {name: 'Oliver Stone'})--(movie) RETURN movie.title", assertWallStreetIsReturned) {
          p("Returns all the movies directed by *'Oliver Stone'*.")
          resultTable()
        }
      }
      section("Match with labels", "match-with-labels") {
        p("To constrain your pattern with labels on nodes, you add it to your pattern nodes, using the label syntax.")
        query("MATCH (:Person {name: 'Oliver Stone'})--(movie:Movie) RETURN movie.title", assertWallStreetIsReturned) {
          p("Returns any nodes connected with the `Person` *'Oliver'* that are labeled `Movie`.")
          resultTable()
        }
      }
    }
    section("Relationship basics", "relationship-basics") {
      section("Outgoing relationships", "outgoing-relationships") {
        p("When the direction of a relationship is of interest, it is shown by using `-->` or `<--`, like this:")
        query("MATCH (:Person {name: 'Oliver Stone'})-->(movie) RETURN movie.title", assertWallStreetIsReturned) {
          p("Returns any nodes connected with the `Person` *'Oliver'* by an outgoing relationship.")
          resultTable()
        }
      }
      section("Directed relationships and variable", "directed-rels-and-variable") {
        p("If a variable is required, either for filtering on properties of the relationship, or to return the relationship, this is how you introduce the variable.")
        query("MATCH (:Person {name: 'Oliver Stone'})-[r]->(movie) RETURN type(r)", assertRelationshipIsDirected) {
          p("Returns the type of each outgoing relationship from *'Oliver'*.")
          resultTable()
        }
      }
      section("Match on relationship type", "match-on-rel-type") {
        p("When you know the relationship type you want to match on, you can specify it by using a colon together with the relationship type.")
        query("MATCH (wallstreet:Movie {title: 'Wall Street'})<-[:ACTED_IN]-(actor) RETURN actor.name", assertAllActorsOfWallStreetAreFound) {
          p("Returns all actors that `ACTED_IN` *'Wall Street'*.")
          resultTable()
        }
      }
      section("Match on multiple relationship types", "match-on-multiple-rel-types") {
        p("To match on one of multiple types, you can specify this by chaining them together with the pipe symbol `|`.")
        query("MATCH (wallstreet {title: 'Wall Street'})<-[:ACTED_IN|:DIRECTED]-(person) RETURN person.name", assertEveryoneConnectedToWallStreetIsFound) {
          p("Returns nodes with an `ACTED_IN` or `DIRECTED` relationship to *'Wall Street'*.")
          resultTable()
        }
      }
      section("Match on relationship type and use a variable", "match-on-rel-type-use-variable") {
        p("If you both want to introduce an variable to hold the relationship, and specify the relationship type you want, just add them both, like this:")
        query("MATCH (wallstreet {title: 'Wall Street'})<-[r:ACTED_IN]-(actor) RETURN r.role", assertRelationshipsToWallStreetAreReturned) {
          p("Returns `ACTED_IN` roles for *'Wall Street'*.")
          resultTable()
        }
      }
    }
    section("Relationships in depth", "relationships-in-depth") {
      note {
        p("Inside a single pattern, relationships will only be matched once. You can read more about this in <<cypherdoc-uniqueness>>.")
      }
      section("Relationship types with uncommon characters", "rel-types-with-uncommon-chars") {
        val initQuery =
          """MATCH (charlie:Person {name: 'Charlie Sheen'}), (rob:Person {name: 'Rob Reiner'})
            |CREATE (rob)-[:`TYPE INCLUDING A SPACE`]->(charlie)"""
        p("""Sometimes your database will have types with non-letter characters, or with spaces in them.
            | Use ``` (backtick) to quote these.
            | To demonstrate this we can add an additional relationship between *'Charlie Sheen'* and *'Rob Reiner'*:""")
        query(initQuery, NoAssertions) {
          p("Which leads to the following graph: ")
          graphViz()
        }
        query("MATCH (n {name: 'Rob Reiner'})-[r:`TYPE INCLUDING A SPACE`]->() RETURN type(r)", assertRelType("TYPE INCLUDING A SPACE")) {
          initQueries(initQuery)
          p("Returns a relationship type with spaces in it.")
          resultTable()
        }
      }
      section("Multiple relationships", "multiple-rels") {
        p("Relationships can be expressed by using multiple statements in the form of `()--()`, or they can be strung together, like this:")
        query("match (charlie {name: 'Charlie Sheen'})-[:ACTED_IN]->(movie)<-[:DIRECTED]-(director) RETURN movie.title, director.name", assertFindAllDirectors) {
          p("Returns the movie *'Charlie Sheen'* acted in and its director.")
          resultTable()
        }
      }
      section("Variable length relationships", "varlength-rels") {
        p("""Nodes that are a variable number of relationship->node hops away can be found using the following syntax:
            | `-[:TYPE*minHops..maxHops]->`.
            | `minHops` and `maxHops` are optional and default to 1 and infinity respectively.
            | When no bounds are given the dots may be omitted.
            | The dots may also be omitted when setting only one bound and this implies a fixed length pattern.""")
        query("match (martin {name: 'Charlie Sheen'})-[:ACTED_IN*1..3]-(movie:Movie) return movie.title", assertAllMoviesAreReturned) {
          p("Returns all movies related to *'Charlie Sheen'* by 1 to 3 hops.")
          resultTable()
        }
      }
      section("Relationship variable in variable length relationships", "rel-variable-in-varlength-rels") {
        p("When the connection between two nodes is of variable length, the list of relationships comprising the connection can be returned using the following syntax:")
        query("MATCH p = (actor {name: 'Charlie Sheen'})-[:ACTED_IN*2]-(co_actor) RETURN relationships(p)", assertActedInRelationshipsAreReturned) {
          p("Returns a list of relationships.")
          resultTable()
        }
      }
      section("Match with properties on a variable length path", "match-props-on-varlength-path") {
        val initQuery =
          """MATCH (charlie:Person {name: 'Charlie Sheen'}), (martin:Person {name: 'Martin Sheen'})
            |CREATE (charlie)-[:X {blocked: false}]->(:UNBLOCKED)<-[:X {blocked: false}]-(martin)
            |CREATE (charlie)-[:X {blocked: true}]->(:BLOCKED)<-[:X {blocked: false}]-(martin)"""
        p("""A variable length relationship with properties defined on in it means that all
            |relationships in the path must have the property set to the given value. In this query,
            |there are two paths between *'Charlie Sheen'* and his father *'Martin Sheen'*. One of them includes a
            |*'blocked'* relationship and the other doesn't. In this case we first alter the original
            |graph by using the following query to add `BLOCKED` and `UNBLOCKED` relationships:""")
        query(initQuery, assertBlockingRelationshipsAdded) {
          p("This means that we are starting out with the following graph: ")
          graphViz()
        }
        query(
          """MATCH p = (charlie:Person)-[* {blocked:false}]-(martin:Person)
            | WHERE charlie.name = 'Charlie Sheen' AND martin.name = 'Martin Sheen'
            | RETURN p""".stripMargin, assertBlockingRelationshipsAdded) {
          initQueries(initQuery)
          p("Returns the paths between *'Charlie Sheen'* and *'Martin Sheen'* where all relationships have the `blocked` property set to `false`.")
          resultTable()
        }
      }
      section("Zero length paths", "zero-length-paths") {
        p(
          """Using variable length paths that have the lower bound zero means that two variables can point to the same node.
            |If the path length between two nodes is zero, they are by definition the same node.
            |Note that when matching zero length paths the result may contain a match even when matching on a relationship type not in use.""")
        query("MATCH (wallstreet:Movie {title: 'Wall Street'})-[*0..1]-(x) RETURN x", assertLabelStats("x", Map("Movie" -> 1, "Person" -> 4))) {
          p("Returns the movie itself as well as actors and directors one relationship away")
          resultTable()
        }
      }
      section("Named paths", "named-paths") {
        p("If you want to return or filter on a path in your pattern graph, you can a introduce a named path.")
        query("MATCH p = (michael {name: 'Michael Douglas'})-->() RETURN p", assertRowCount(2)) {
          p("Returns the two paths starting from *'Michael Douglas'*")
          resultTable()
        }
      }
      section("Matching on a bound relationship", "match-on-bound-rel") {
        p("""When your pattern contains a bound relationship, and that relationship pattern doesn’t
            | specify direction, Cypher will try to match the relationship in both directions.""")
        query("MATCH (a)-[r]-(b) WHERE id(r)= 0 RETURN a,b", assertRowCount(2)) {
          p("This returns the two connected nodes, once as the start node, and once as the end node")
          resultTable()
        }
      }
    }
    section("Shortest path", "query-shortest-path") {
      section("Single shortest path", "single-shortest-path") {
        p("Finding a single shortest path between two nodes is as easy as using the `shortestPath` function. It's done like this:")
        query(
          """MATCH (martin:Person {name: 'Martin Sheen'}),
            |      (oliver:Person {name: 'Oliver Stone'}),
            |      p = shortestPath((martin)-[*..15]-(oliver))
            |RETURN p""", assertShortestPathLength) {
          p(
            """This means: find a single shortest path between two nodes, as long as the path is max 15 relationships long.
              |Within the parentheses you define a single link of a path -- the starting node, the connecting relationship
              |and the end node. Characteristics describing the relationship like relationship type, max hops and direction
              |are all used when finding the shortest path. If there is a `WHERE` clause following the match of a
              |`shortestPath`, relevant predicates will be included in the `shortestPath`.
              |If the predicate is a `none()` or `all()` on the relationship elements of the path,
              |it will be used during the search to improve performance (see <<query-shortestpath-planning>>).""")
          resultTable()
        }
      }
      section("Single shortest path with predicates", "single-shortest-path-with-predicates") {
        p("""Predicates used in the `WHERE` clause that apply to the shortest path pattern are evaluated before deciding
          |what the shortest matching path is.""")
        query(
          """MATCH (charlie:Person {name: 'Charlie Sheen'}),
            |      (martin:Person {name: 'Martin Sheen'}),
            |      p = shortestPath( (charlie)-[*]-(martin) )
            |WHERE none(r in relationships(p) WHERE type(r) = 'FATHER')
            |RETURN p""", assertShortestPathLength) {
          p(
            """This query will find the shortest path between *'Charlie Sheen'* and *'Martin Sheen'*, and the `WHERE` predicate
              |will ensure that we don't consider the father/son relationship between the two.""")
          resultTable()
        }
      }
      section("All shortest paths", "all-shortest-paths") {
        p("Finds all the shortest paths between two nodes.")
        query("""MATCH (martin:Person {name: 'Martin Sheen'} ),
                |      (michael:Person {name: 'Michael Douglas'}),
                |      p = allShortestPaths((martin)-[*]-(michael))
                |RETURN p""", assertAllShortestPaths) {
          p("Finds the two shortest paths between *'Martin Sheen'* and *'Michael Douglas'*.")
          resultTable()
        }
      }
    }
    section("Get node or relationship by id", "get-node-rel-by-id") {
      section("Node by id", "match-node-by-id") {
        p("Searching for nodes by id can be done with the `id()` function in a predicate.")
        note {
          p("""Neo4j reuses its internal ids when nodes and relationships are deleted.
              |This means that applications using, and relying on internal Neo4j ids, are brittle or at risk of making mistakes.
              |It is therefore recommended to rather use application-generated ids.""")
        }
        query("MATCH (n) WHERE id(n) = 0 RETURN n", assertHasNodes(0)) {
          p("The corresponding node is returned.")
          resultTable()
        }
      }
      section("Relationship by id", "match-rel-by-id") {
        p("""
            |Search for relationships by id can be done with the `id()` function in a predicate.
            |
            |This is not recommended practice. See <<match-node-by-id>> for more information on the use of Neo4j ids.
            |""")
        query("MATCH ()-[r]->() WHERE id(r) = 0 RETURN r", assertHasRelationships(0)) {
          p("The relationship with id `0` is returned.")
          resultTable()
        }
      }
      section("Multiple nodes by id", "match-multiple-nodes-by-id") {
        p("Multiple nodes are selected by specifying them in an IN clause.")
        query("MATCH (n) WHERE id(n) IN [0, 3, 5] RETURN n", assertHasNodes(0, 3, 5)) {
          p("This returns the nodes listed in the `IN` expression.")
          resultTable()
        }
      }
    }
  }.build()

  private def assertAllNodesReturned = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read() )
    try {
      val allNodes: List[Node] = db.getAllNodes().asScala.toList
      allNodes should equal(p.columnAs[Node]("n").toList)
    } finally tx.close()
  })

  private def assertLabelStats(variable: String, stats: Map[String, Int]) = ResultAndDbAssertions((result, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read() )
    try {
      val nodes = result.columnAs[Node](variable).toList
      val labelStats = nodes.foldLeft(Map[String,Int]()) { (acc, node) =>
        val label = node.getLabels.iterator().next().name()
        val count = if (acc.isDefinedAt(label)) acc(label) else 0
        acc + (label -> (count + 1))
      }
      labelStats should equal(stats)
    } finally tx.close()
  })

  private def assertRowCount(count: Int) = ResultAssertions(result =>
    result.toList.length should equal(count)
  )

  private def assertAllMoviesAreReturned = ResultAssertions(result =>
    result.toSet should equal(Set(
      Map("movie.title" -> "The American President"),
      Map("movie.title" -> "Wall Street")))
  )

  private def assertBlockingRelationshipsAdded = NoAssertions
  private def assertWallStreetIsReturned = ResultAssertions(result =>
    result.toList should equal(
      List(Map("movie.title" -> "Wall Street")))
  )

  private def assertActedInRelationshipsAreReturned = ResultAssertions(result =>
    result.toSet.size shouldBe 2
  )

  private def assertRelationshipIsDirected = ResultAssertions(result =>
    result.toList should equal(
      List(Map("type(r)" -> "DIRECTED")))
  )

  private def assertAllActorsOfWallStreetAreFound = ResultAssertions(result =>
    result.toSet should equal(Set(
      Map("actor.name" -> "Michael Douglas"),
      Map("actor.name" -> "Martin Sheen"),
      Map("actor.name" -> "Charlie Sheen")))
  )

  private def assertRelType(name: String) = ResultAssertions(result =>
    result.toList should equal(List(Map("type(r)" -> name))))

  private def assertEveryoneConnectedToWallStreetIsFound = ResultAssertions(result =>
    result.toSet should equal(Set(
      Map("person.name" -> "Michael Douglas"),
      Map("person.name" -> "Oliver Stone"),
      Map("person.name" -> "Martin Sheen"),
      Map("person.name" -> "Charlie Sheen")))
  )

  private def assertRelationshipsToWallStreetAreReturned = ResultAssertions(result =>
    result.size should equal(3)
  )

  private def assertFindAllDirectors = ResultAssertions(result =>
    result.toList should equal(
      List(Map("movie.title" -> "Wall Street", "director.name" -> "Oliver Stone"))))

  private def assertShortestPathLength = ResultAssertions(result =>
    result.toList.head("p").asInstanceOf[Path].length() shouldBe 2)

  private def assertAllShortestPaths = ResultAssertions(result =>
    result.toList.size shouldBe 2)


  private def assertHasRelationships(relIds: Long*) = ResultAssertions(result =>
    result.toList.map(_.head._2.asInstanceOf[Relationship].getId) should equal(relIds.toList)
  )

  private def assertHasNodes(nodeIds: Long*) = ResultAssertions(result =>
    result.toList.map(_.head._2.asInstanceOf[Node].getId) should equal(nodeIds.toList)
  )

}
