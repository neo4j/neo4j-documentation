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

import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.internal.compiler.v3_1.pipes.IndexSeekByRange

class QueryPlanTest extends DocumentingTestBase with SoftReset {
  override val setupQueries = List(
    """CREATE (me:Person {name: 'me'})
       CREATE (andres:Person {name: 'Andres'})
       CREATE (andreas:Person {name: 'Andreas'})
       CREATE (mattias:Person {name: 'Mattias'})
       CREATE (lovis:Person {name: 'Lovis'})
       CREATE (pontus:Person {name: 'Pontus'})
       CREATE (max:Person {name: 'Max'})
       CREATE (konstantin:Person {name: 'Konstantin'})
       CREATE (stefan:Person {name: 'Stefan'})
       CREATE (mats:Person {name: 'Mats'})
       CREATE (petra:Person {name: 'Petra'})
       CREATE (craig:Person {name: 'Craig'})
       CREATE (steven:Person {name: 'Steven'})
       CREATE (chris:Person {name: 'Chris'})

       CREATE (london:Location {name: 'London'})
       CREATE (malmo:Location {name: 'Malmo'})
       CREATE (sf:Location {name: 'San Francisco'})
       CREATE (berlin:Location {name: 'Berlin'})
       CREATE (newyork:Location {name: 'New York'})
       CREATE (kuala:Location {name: 'Kuala Lumpur'})
       CREATE (stockholm:Location {name: 'Stockholm'})
       CREATE (paris:Location {name: 'Paris'})
       CREATE (madrid:Location {name: 'Madrid'})
       CREATE (rome:Location {name: 'Rome'})

       CREATE (england:Country {name: 'England'})
       CREATE (field:Team {name: 'Field'})
       CREATE (engineering:Team {name: 'Engineering'})
       CREATE (sales:Team {name: 'Sales'})
       CREATE (monads:Team {name: 'Team Monads'})
       CREATE (birds:Team {name: 'Team Enlightened Birdmen'})
       CREATE (quality:Team {name: 'Team Quality'})
       CREATE (rassilon:Team {name: 'Team Rassilon'})
       CREATE (executive:Team {name: 'Team Executive'})
       CREATE (remoting:Team {name: 'Team Remoting'})
       CREATE (other:Team {name: 'Other'})

       CREATE (me)-[:WORKS_IN {duration: 190}]->(london)
       CREATE (andreas)-[:WORKS_IN {duration: 187}]->(london)
       CREATE (andres)-[:WORKS_IN {duration: 150}]->(london)
       CREATE (mattias)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (lovis)-[:WORKS_IN {duration: 230}]->(sf)
       CREATE (pontus)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (max)-[:WORKS_IN {duration: 230}]->(newyork)
       CREATE (konstantin)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230}]->(berlin)
       CREATE (mats)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (petra)-[:WORKS_IN {duration: 230}]->(london)
       CREATE (craig)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (steven)-[:WORKS_IN {duration: 230}]->(malmo)
       CREATE (chris)-[:WORKS_IN {duration: 230}]->(madrid)
       CREATE (london)-[:IN]->(england)
       CREATE (me)-[:FRIENDS_WITH]->(andres)
       CREATE (andres)-[:FRIENDS_WITH]->(andreas)
    """.stripMargin)

  override val setupConstraintQueries = List(
    "CREATE INDEX ON :Location(name)",
    "CREATE INDEX ON :Person(name)",
    "CREATE CONSTRAINT ON (team:Team) ASSERT team.name is UNIQUE"
  )

  def section = "Query Plan"

  @Test def allNodesScan() {
    profileQuery(
      title = "All Nodes Scan",
      text =
        """The `AllNodesScan` operator reads all nodes from the node store. The variable that will contain the nodes is seen in the arguments.
          |If your query is using this operator, you are very likely to see performance problems on any non-trivial database.""".stripMargin,
      queryText = """MATCH (n) RETURN n""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("AllNodesScan"))
    )
  }

  @Test def constraintOperation() {
    profileQuery(
      title = "Constraint Operation",
      text =
        """Creates a constraint on a (label,property) pair.
          |The following query will create a unique constraint on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CreateUniqueConstraint"))
    )
  }

  @Test def distinct() {
    profileQuery(
      title = "Distinct",
      text =
        """The `Distinct` operator removes duplicate rows from the incoming stream of rows.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN DISTINCT l""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Distinct"))
    )
  }

  @Test def eagerAggregation() {
    profileQuery(
      title = "Eager Aggregation",
      text =
        """The `EagerAggregation` operator eagerly loads underlying results and stores it in a hash map, using the grouping keys as the keys for the map.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN l.name AS location, collect(p.name) AS people""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("EagerAggregation"))
    )
  }

  @Test def nodeCountFromCountStore() {
    profileQuery(
      title = "Node Count From Count Store",
      text =
        """The `NodeCountFromCountStore` operator uses the count store to answer questions about node counts.
          | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
          | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
          | For example, we can get counts for all nodes, and nodes with a label, but not nodes with more than one label.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN count(p) AS people""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeCountFromCountStore"))
    )
  }

  @Test def relationshipCountFromCountStore() {
    profileQuery(
      title = "Relationship Count From Count Store",
      text =
        """The `RelationshipCountFromCountStore` operator uses the count store to answer questions about relationship counts.
          | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
          | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
          | For example, we can get counts for all relationships, relationships with a type, relationships with a label on one end, but not relationships with labels on both end nodes.""".stripMargin,
      queryText = """MATCH (p:Person)-[r:WORKS_IN]->() RETURN count(r) AS jobs""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("RelationshipCountFromCountStore"))
    )
  }

  @Test def eager() {
    profileQuery(
      title = "Eager",
      text =
        """For isolation purposes, the `Eager` operator ensures that operations affecting subsequent operations are executed fully for the whole dataset before continuing execution.
           | Otherwise, endless loops could be triggered in which data that was just created is matched.
           | The `Eager` operator can cause high memory usage when importing data or migrating graph structures.
           | In such cases, the operations should be split into simpler steps; e.g. importing nodes and relationships separately.
           | Alternatively, the records to be updated can be returned, followed by an update statement.""".stripMargin,
      queryText = """MATCH (a)-[r]-(b) DELETE r,a,b MERGE ()""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Eager"))
    )
  }

  @Test def updateGraph() {
    profileQuery(
      title = "Update Graph",
      text =
        """Creates a node in the graph.""".stripMargin,
      queryText = """CYPHER planner=rule CREATE (:Person {name: 'Alistair'})""",
      assertions = (p) => {
        assertThat(p.executionPlanDescription().toString, containsString("CreateNode"))
        assertThat(p.executionPlanDescription().toString, containsString("UpdateGraph"))
      }
    )
  }

  @Test def mergeInto() {
    profileQuery(
      title = "Merge Into",
      text =
        """When both the start and end node have already been found, `Merge Into` is used to find all connecting relationships or creating a new relationship between the two nodes.""".stripMargin,
      queryText = """CYPHER planner=rule MATCH (p:Person {name: 'me'}), (f:Person {name: 'Andres'}) MERGE (p)-[:FRIENDS_WITH]->(f)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Merge(Into)"))
    )
  }

  @Test def emptyResult() {
    profileQuery(
      title = "Empty Result",
      text =
        """Eagerly loads everything coming in to the `EmptyResult` operator and discards it.""".stripMargin,
      queryText = """CREATE (:Person)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("EmptyResult"))
    )
  }

  @Test def nodeByLabelScan() {
    profileQuery(
      title = "Node By Label Scan",
      text = """The `NodeByLabelScan` operator fetches all nodes with a specific label from the node label index.""".stripMargin,
      queryText = """MATCH (person:Person) RETURN person""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeByLabelScan"))
    )
  }

  @Test def nodeByIndexSeek() {
    profileQuery(
      title = "Node Index Seek",
      text = """The `NodeIndexSeek`operator finds nodes using an index seek. The node variable and the index used is shown in the arguments of the operator.
                |If the index is a unique index, the operator is instead called `NodeUniqueIndexSeek`.""".stripMargin,
      queryText = """MATCH (location:Location {name: 'Malmo'}) RETURN location""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeek"))
    )
  }

  @Test def nodeByUniqueIndexSeek() {
    profileQuery(
      title = "Node Unique Index Seek",
      text = """The `NodeUniqueIndexSeek` operator finds nodes using an index seek within a unique index. The node variable and the index used is shown in the arguments of the operator.
               |If the index is not unique, the operator is instead called `NodeIndexSeek`.""".stripMargin,
      queryText = """MATCH (t:Team {name: 'Malmo'}) RETURN t""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeUniqueIndexSeek"))
    )
  }

  @Test def argument() {
    profileQuery(
      title = "Argument",
      text = """The `Argument` operator indicates the variable to be used as an argument to the right-hand side of an <<query-plan-apply, Apply>> operator.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Argument"))
    )
  }

  @Test def loadCSV() {
    profileQuery(
      title = "Load CSV",
      text = """The `LoadCSV` operator is used when executing a query containing the <<query-load-csv, LOAD CSV>> clause.""".stripMargin,
      queryText = """LOAD CSV FROM 'https://neo4j.com/docs/cypher-refcard/3.3/csv/artists.csv' AS line RETURN line""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("LoadCSV"))
    )
  }

  @Test def nodeIndexRangeSeek() {
    executePreparationQueries {
      val a = (0 to 100).map { i => "CREATE (:Location)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Location {name: '$i'})" }.toList
      a ++ b
    }

    sampleAllIndicesAndWait()

    profileQuery(title = "Node Index Seek By Range",
                 text =
                   """The `NodeIndexSeekByRange` operator finds nodes using an index seek where the value of the property matches a given prefix string.
                     |`NodeIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name STARTS WITH 'Lon' RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeekByRange"))
    )
  }

  @Test def nodeIndexScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Scan",
                 text = """
                          |The `NodeIndexScan` operator goes through all values stored in an index, and searches for all nodes with a particular label having a specified property (e.g. `exists(n.prop)`).""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE exists(l.name) RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
    )
  }

  @Test def nodeIndexContainsScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Contains Scan",
                 text = """
                          |The `NodeIndexContainsScan` operator goes through all values stored in an index, and searches for entries
                          | containing a specific string, such as when using `CONTAINS`. Although this is slower than an index seek (since all entries need to be
                          | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
                          | filter.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name CONTAINS 'al' RETURN l",
                 assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexContainsScan"))
    )
  }

  @Test def nodeIndexEndsWithScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Ends With Scan",
      text = """
               |The `NodeIndexEndsWithScan` operator goes through all values stored in an index, and searches for entries
               | ending in a specific string, such as when using `ENDS WITH`. Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
               | filter xxx.""".stripMargin,
      queryText = "MATCH (l:Location) WHERE l.name ENDS WITH 'al' RETURN l",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexEndsWithScan"))
    )
  }

  @Test def nodeByIdSeek() {
    profileQuery(
      title = "Node By Id Seek",
      text =
        """The `NodeByIdSeek` operator reads one or more nodes by id from the node store.""".stripMargin,
      queryText = """MATCH (n) WHERE id(n) = 0 RETURN n""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("NodeByIdSeek"))
    )
  }

  @Test def projection() {
    profileQuery(
      title = "Projection",
      text =
        """For each incoming row, the `Projection` operator evaluates a set of expressions and produces a row with the results of the expressions.""".stripMargin,
      queryText = """RETURN 'hello' AS greeting""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Projection"))
    )
  }

  @Test def filter() {
    profileQuery(
      title = "Filter",
      text =
        """The `Filter` operator filters each row coming from the child operator, only passing through rows that evaluate the predicates to `true`.""".stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name =~ '^a.*' RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Filter"))
    )
  }

  @Test def cartesianProduct() {
    profileQuery(
      title = "Cartesian Product",
      text =
        """Produces a cartesian product of the two inputs -- each row coming from the left child will be combined with all the rows from the right child operator.""".stripMargin,
      queryText = """MATCH (p:Person), (t:Team) RETURN p, t""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("CartesianProduct"))
    )
  }

  @Test def optionalExpand() {
    profileQuery(
      title = "Optional Expand All",
      text =
        """The `OptionalExpand(All)` operator traverses relationships from a given node, and ensures that predicates are evaluated before producing rows.
          |
          |If no matching relationships are found, a single row with `null` for the relationship and end node variable is produced.""".stripMargin,
      queryText =
        """MATCH (p:Person)
           OPTIONAL MATCH (p)-[works_in:WORKS_IN]->(l) WHERE works_in.duration > 180
           RETURN p, l""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(All)"))
    )
  }

  @Test def sort() {
    profileQuery(
      title = "Sort",
      text =
        """The `Sort` operator sorts rows by a provided key.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Sort"))
    )
  }

  @Test def top() {
    profileQuery(
      title = "Top",
      text =
        """The `Top` operator returns the first 'n' rows sorted by a provided key. Instead of sorting the entire input, only the top 'n' rows are retained.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 2""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Top"))
    )
  }

  @Test def limit() {
    profileQuery(
      title = "Limit",
      text =
        """The `Limit` operator returns the first 'n' rows from the incoming input.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p LIMIT 3""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Limit"))
    )
  }

  @Test def lock() {
    profileQuery(
      title = "Lock",
      text =
        """The `Lock` operator locks the start and end node when creating a relationship.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Lock"))
    )
  }

  @Test def optional() {
    profileQuery(
      title = "Optional",
      text =
        """xxx For use in optional match.""".stripMargin,
      queryText = """MATCH (p:Person {name:'me'}) OPTIONAL MATCH (q:Person {name: 'Lulu'}) RETURN p, q""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Optional"))
    )
  }

  @Test def projectEndpoints() {
    profileQuery(
      title = "Project Endpoints",
      text =
        """The `ProjectEndpoints` operator projects the start and end node of a relationship xxx.""".stripMargin,
      queryText = """CREATE (n)-[p:KNOWS]->(m) WITH p AS r MATCH (u)-[r]->(v) RETURN u, v""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ProjectEndpoints"))
    )
  }

  @Test def expandAll() {
    profileQuery(
      title = "Expand All",
      text =
        """Given a start node, the `Expand(All)` operator will follow incoming or outgoing relationships, depending on the pattern relationship.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof) RETURN fof""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Expand(All)"))
    )
  }

  @Test def expandInto() {
    profileQuery(
      title = "Expand Into",
      text =
        """When both the start and end node have already been found, the `Expand(Into)` operator is used to find all relationships connecting the two nodes.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof)-->(p) RETURN fof""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Expand(Into)"))
    )
  }


  @Test def optionalExpandInto() {
    profileQuery(
      title = "Optional Expand Into",
      text =
        """When both the start and end node have already been found, the `OptionalExpand(Into)` operator is used to find all relationships connecting the two nodes.
          |If no matching relationships are found, a single row with `null` for the relationship and end node variable is produced.xxxxx""".stripMargin,
      queryText = """MATCH (p:Person)-[works_in:WORKS_IN]->(l) OPTIONAL MATCH (l)-->(p) RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(Into)"))
    )
  }

  @Test def varlengthExpandAll() {
    profileQuery(
      title = "VarLength Expand All",
      text =
        """Given a start node, the `VarLengthExpand(All)` operator will follow variable-length relationships. xxx""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN p, q""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(All)"))
    )
  }

  @Test def varlengthExpandInto() {
    profileQuery(
      title = "VarLength Expand Into",
      text =
        """When both the start and end node have already been found, the `VarLengthExpand(Into)` operator is used to find all variable-length relationships connecting the two nodes. xxx""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(p:Person) RETURN p""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(Into)"))
    )
  }

  @Test def directedRelationshipById() {
    profileQuery(
      title = "Directed Relationship By Id Seek",
      text =
        """The `DirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store, and produces both the relationship and the nodes on either side.""".stripMargin,
      queryText =
        """MATCH (n1)-[r]->()
           WHERE id(r) = 0
           RETURN r, n1
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipByIdSeek"))
    )
  }

  @Test def undirectedRelationshipById() {
    profileQuery(
      title = "Undirected Relationship By Id Seek",
      text =
        """The `UndirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store.
          |As the direction is unspecified, two rows are produced for each relationship as a result of alternating the combination of the start and end node.""".stripMargin,
      queryText =
        """MATCH (n1)-[r]-()
           WHERE id(r) = 1
           RETURN r, n1
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipByIdSeek"))
    )
  }

  @Test def skip() {
    profileQuery(
      title = "Skip",
      text =
        """The `Skip` operator skips 'n' rows from the incoming rows.
        """.stripMargin,
      queryText =
        """MATCH (p:Person)
           RETURN p
           ORDER BY p.id
           SKIP 1
        """.stripMargin,
      assertions = (p) =>  assertThat(p.executionPlanDescription().toString, containsString("Skip"))
    )
  }

  @Test def union() {
    profileQuery(
      title = "Union",
      text =
        "The `Union` operator concatenates the results from the right plan with the results of the left plan.",
      queryText =
        """MATCH (p:Location)
           RETURN p.name
           UNION ALL
           MATCH (p:Country)
           RETURN p.name
        """.stripMargin,
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Union"))
    )
  }

  @Test def unwind() {
    profileQuery(
      title = "Unwind",
      text =
        """The `Unwind` operator returns one row per item in a list.""".stripMargin,
      queryText = """UNWIND range(1, 5) as value return value;""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("Unwind"))
    )
  }

  @Test def call(): Unit = {
    profileQuery(
      title = "Procedure Call",
      text = """The `ProcedureCall` operator indicates an invocation to a procedure.""".stripMargin,
      queryText = """CALL db.labels() YIELD label RETURN * ORDER BY label""",
      assertions = (p) => assertThat(p.executionPlanDescription().toString, containsString("ProcedureCall"))
    )
  }
}
