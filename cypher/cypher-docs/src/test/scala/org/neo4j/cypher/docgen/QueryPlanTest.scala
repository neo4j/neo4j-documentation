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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, ResultAssertions}

class QueryPlanTest extends DocumentingTest {

  override def doc: Document = new DocBuilder {
    doc("Query Plan", "query-plan")

    initQueries(
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
       CREATE (engineering:Team {name: 'Engineering', id:42})
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
    """.stripMargin,
      "CREATE INDEX ON :Location(name)",
      "CREATE INDEX ON :Person(name)",
      "CREATE CONSTRAINT ON (team:Team) ASSERT team.name is UNIQUE",
      "CREATE CONSTRAINT ON (team:Team) ASSERT team.id is UNIQUE")

    operator("All Nodes Scan", "AllNodesScan",
      """The `AllNodesScan` operator reads all nodes from the node store. The variable that will contain the nodes is seen in the arguments.
        |Any query using this operator is likely to encounter performance problems on a non-trivial database.""",
      """MATCH (n) RETURN n"""
    )

    operator("Create Unique Constraint", "CreateUniqueConstraint",
      """The `CreateUniqueConstraint` operator creates a unique constraint on a property for all nodes having a certain label.
        |The following query will create a unique constraint on the `name` property of nodes with the `Country` label.""",
      """CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE"""
    )

    operator("Drop Unique Constraint", "DropUniqueConstraint",
      """The `DropUniqueConstraint` operator removes a unique constraint from a property for all nodes having a certain label.
        |The following query will drop a unique constraint on the `name` property of nodes with the `Country` label.""",
      """DROP CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE""",
      morePreparationQueries = "CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE"
    )

    operator("Create Node Property Existence Constraint", "CreateNodePropertyExistenceConstraint",
      """The `CreateNodePropertyExistenceConstraint` operator creates an existence constraint on a property for all nodes having a certain label.
        |This will only appear in Enterprise Edition.""",
      """CREATE CONSTRAINT ON (p:Person) ASSERT exists(p.name)"""
    )

    operator("Drop Node Property Existence Constraint", "DropNodePropertyExistenceConstraint",
      """The `DropNodePropertyExistenceConstraint` operator removes an existence constraint from a property for all nodes having a certain label.
        |This will only appear in Enterprise Edition.""",
      """DROP CONSTRAINT ON (p:Person) ASSERT exists(p.name)""",
      morePreparationQueries = "CREATE CONSTRAINT ON (p:Person) ASSERT exists(p.name)"
    )

    operator("Create Relationship Property Existence Constraint", "CreateRelationshipPropertyExistenceConstraint",
      """The `CreateRelationshipPropertyExistenceConstraint` operator creates an existence constraint on a property for all relationships of a certain type.
        |This will only appear in Enterprise Edition.""",
      """CREATE CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)"""
    )

    operator("Drop Relationship Property Existence Constraint", "DropRelationshipPropertyExistenceConstraint",
      """The `DropRelationshipPropertyExistenceConstraint` operator removes an existence constraint from a property for all relationships of a certain type.
        |This will only appear in Enterprise Edition.""",
      """DROP CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)""",
      morePreparationQueries = "CREATE CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)"
    )

    operator("Create Index", "CreateIndex",
      """The `CreateIndex` operator creates an index on a property for all nodes having a certain label.
        |The following query will create an index on the `name` property of nodes with the `Country` label.""",
      """CREATE INDEX ON :Country(name)"""
    )

    operator("Drop Index", "DropIndex",
      """The `DropIndex` operator removes an index from a property for all nodes having a certain label.
        |The following query will drop an index on the `name` property of nodes with the `Country` label.""",
      """DROP INDEX ON :Country(name)""",
      morePreparationQueries = "CREATE INDEX ON :Country(name)"
    )

    operator("Distinct", "Distinct",
      """The `Distinct` operator removes duplicate rows from the incoming stream of rows.
        |To ensure only distinct elements are returned, `Distinct` will pull in data lazily from its source and build up state.
        |This may lead to increased memory pressure in the system.""",
      """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN DISTINCT l"""
    )

    operator("Eager Aggregation", "EagerAggregation",
      """The `EagerAggregation` operator evaluates a grouping expression and uses the result to group rows into different groupings.
        |For each of these groupings, `EagerAggregation` will then evaluate all aggregation functions and return the result.
        |To do this, `EagerAggregation`, as the name implies, needs to pull in all data eagerly from its source and build up state, which leads to increased memory pressure in the system.""",
      """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN l.name AS location, collect(p.name) AS people"""
    )

    operator("Node Count From Count Store", "NodeCountFromCountStore",
      """The `NodeCountFromCountStore` operator uses the count store to answer questions about node counts.
        | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
        | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
        | For example, we can get counts for all nodes, and nodes with a label, but not nodes with more than one label.""",
      """MATCH (p:Person) RETURN count(p) AS people"""
    )

    operator("Relationship Count From Count Store", "RelationshipCountFromCountStore",
      """The `RelationshipCountFromCountStore` operator uses the count store to answer questions about relationship counts.
        | This is much faster than the `EagerAggregation` operator which achieves the same result by actually counting.
        | However, as the count store only stores a limited range of combinations, `EagerAggregation` will still be used for more complex queries.
        | For example, we can get counts for all relationships, relationships with a type, relationships with a label on one end, but not relationships with labels on both end nodes.""",
      """MATCH (p:Person)-[r:WORKS_IN]->() RETURN count(r) AS jobs"""
    )

    operator("Eager", "Eager",
      """For isolation purposes, the `Eager` operator ensures that operations affecting subsequent operations are executed fully for the whole dataset before continuing execution.
        | Information from the stores is fetched in a lazy manner; i.e. the pattern matching might not be fully exhausted before updates are applied.
        | To guarantee reasonable semantics, the query planner will insert `Eager` operators into the query plan to prevent updates from influencing pattern matching;
        | this scenario is exemplified by the query below, where the `DELETE` clause influences the `MATCH` clause.
        | The `Eager` operator can cause high memory usage when importing data or migrating graph structures.
        | In such cases, the operations should be split into simpler steps; e.g. importing nodes and relationships separately.
        | Alternatively, the records to be updated can be returned, followed by an update statement.""",
      """MATCH (a)-[r]-(b) DELETE r,a,b MERGE ()"""
    )

    operator("Create Node", "CreateNode",
      """The `CreateNode` operator is used to create a node.""",
      """CREATE (:Person {name: 'Jack'})"""
    )

    operator("Create Relationship", "CreateRelationship",
      """The `CreateRelationship` operator is used to create a relationship.""",
      """MATCH (a:Person {name: 'Max'}), (b:Person {name: 'Chris'})
        |CREATE (a)-[:FRIENDS_WITH]->(b)"""
    )

    operator("Delete", "Delete",
      """The `Delete` operator is used to delete a node or a relationship.""",
      """MATCH (me:Person {name: 'me'})-[w:WORKS_IN {duration: 190}]->(london:Location {name: 'London'})
        |DELETE w"""
    )

    operator("Detach Delete", "DetachDelete",
      """The `DetachDelete` operator is used in all queries containing the <<query-delete, DETACH DELETE>> clause, when deleting nodes and their relationships.""",
      """MATCH (p:Person)
        |DETACH DELETE p"""
    )

    operator("Merge Create Node", "MergeCreateNode",
      """The `MergeCreateNode` operator is used when creating a node as a result of a <<query-merge, MERGE>> clause failing to find the node.""",
      """MERGE (:Person {name: 'Sally'})"""
    )

    operator("Merge Create Relationship", "MergeCreateRelationship",
      """The `MergeCreateRelationship` operator is used when creating a relationship as a result of a <<query-merge, MERGE>> clause failing to find the relationship.""",
      """MATCH (s:Person {name: 'Sally'})
        |MERGE (s)-[:FRIENDS_WITH]->(s)"""
    )

    operator("Remove Labels", "RemoveLabels",
      """The `RemoveLabels` operator is used when deleting labels from a node.""",
      """MATCH (n)
        |REMOVE n:Person"""
    )

    operator("Set Labels", "SetLabels",
      """The `SetLabels` operator is used when setting labels on a node.""",
      """MATCH (n)
        |SET n:Person"""
    )

    operator("Set Node Property From Map", "SetNodePropertyFromMap",
      """The `SetNodePropertyFromMap` operator is used when setting properties from a map on a node.""",
      """MATCH (n)
        |SET n = {weekday: 'Monday', meal: 'Lunch'}"""
    )

    operator("Set Relationship Property From Map", "SetRelationshipPropertyFromMap",
      """The `SetRelationshipPropertyFromMap` operator is used when setting properties from a map on a relationship.""",
      """MATCH (n)-[r]->(m)
        |SET r = {weight: 5, unit: 'kg'}"""
    )

    operator("Set Property", "SetProperty",
      """The `SetProperty` operator is used when setting a property on a node or relationship.""",
      """MATCH (n)
        |SET n.checked = true"""
    )

    operator("Empty Result", "EmptyResult",
      """The `EmptyResult` operator eagerly loads all incoming data and discards it.""",
      """CREATE (:Person)"""
    )

    operator("Produce Results", "ProduceResults",
      """The `ProduceResults` operator prepares the result so that it is consumable by the user, such as transforming internal values to user values.
        |It is present in every single query that returns data to the user, and has little bearing on performance optimisation.""",
      """MATCH (n) RETURN n"""
    )

    operator("Node By Label Scan", "NodeByLabelScan",
      """The `NodeByLabelScan` operator fetches all nodes with a specific label from the node label index.""",
      """MATCH (person:Person) RETURN person"""
    )

    operator("Node Index Seek", "NodeIndexSeek",
      """The `NodeIndexSeek` operator finds nodes using an index seek.
        |The node variable and the index used is shown in the arguments of the operator.
        |If the index is a unique index, the operator is instead called <<query-plan-node-unique-index-seek, NodeUniqueIndexSeek>>.""",
      """MATCH (location:Location {name: 'Malmo'}) RETURN location"""
    )

    operator("Node Unique Index Seek", "NodeUniqueIndexSeek",
      """The `NodeUniqueIndexSeek` operator finds nodes using an index seek within a unique index. The node variable and the index used is shown in the arguments of the operator.
        |If the index is not unique, the operator is instead called <<query-plan-node-index-seek, NodeIndexSeek>>.
        |If the index seek is used to solve a <<query-merge, MERGE>> clause, it will also be marked with `(Locking)`.
        |This makes it clear that any nodes returned from the index will be locked in order to prevent concurrent conflicting updates.""",
      """MATCH (t:Team {name: 'Malmo'}) RETURN t"""
    )

    operator("Argument", "Argument",
      """The `Argument` operator indicates the variable to be used as an argument to the right-hand side of an <<query-plan-apply, Apply>> operator.""",
      """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)"""
    )

    operator("Load CSV", "LoadCSV",
      """The `LoadCSV` operator loads data from a CSV source into the query.
        |It is used whenever the <<query-load-csv, LOAD CSV>> clause is used in a query.""",
      """LOAD CSV FROM 'https://neo4j.com/docs/cypher-refcard/3.3/csv/artists.csv' AS line RETURN line"""
    )

    operator("Node Index Seek By Range", "NodeIndexSeekByRange",
      """The `NodeIndexSeekByRange` operator finds nodes using an index seek where the value of the property matches a given prefix string.
        |`NodeIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
        |If the index is a unique index, the operator is instead called `NodeUniqueIndexSeekByRange`.""",
      "MATCH (l:Location) WHERE l.name STARTS WITH 'Lon' RETURN l",
      morePreparationQueries = (0 to 300).map { i => s"CREATE (:Location {name: '$i'})" }: _* // TODO sampleAllIndexesAndWait() ?
    )

    operator("Node Unique Index Seek By Range", "NodeUniqueIndexSeekByRange",
      """The `NodeUniqueIndexSeekByRange` operator finds nodes using an index seek within a unique index, where the value of the property matches a given prefix string.
        |`NodeUniqueIndexSeekByRange` is used by `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
        |If the index is not unique, the operator is instead called `NodeIndexSeekByRange`.""",
      "MATCH (t:Team) WHERE t.name STARTS WITH 'Ma' RETURN t",
      morePreparationQueries = (0 to 300).map { i => s"CREATE (:Team {name: '$i'})" }: _* // TODO sampleAllIndexesAndWait() ?
    )

    operator("Node Index Scan", "NodeIndexScan",
      """The `NodeIndexScan` operator examines all values stored in an index, returning all nodes with a particular label having a specified property.""",
      "MATCH (l:Location) WHERE exists(l.name) RETURN l",
      morePreparationQueries = (0 to 250).map { _ => "CREATE (:Location)" }: _*
    )

    operator("Node Index Contains Scan", "NodeIndexContainsScan",
      """
        |The `NodeIndexContainsScan` operator examines all values stored in an index, searching for entries
        | containing a specific string; for example, in queries including `CONTAINS`.
        | Although this is slower than an index seek (since all entries need to be
        | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
        | filter.""",
      "MATCH (l:Location) WHERE l.name CONTAINS 'al' RETURN l",
      morePreparationQueries = (0 to 250).map { _ => "CREATE (:Location)" }: _*
    )

    operator("Node Index Ends With Scan", "NodeIndexEndsWithScan",
      """
        |The `NodeIndexEndsWithScan` operator examines all values stored in an index, searching for entries
        | ending in a specific string; for example, in queries containing `ENDS WITH`.
        | Although this is slower than an index seek (since all entries need to be
        | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
        | filter.""",
      "MATCH (l:Location) WHERE l.name ENDS WITH 'al' RETURN l",
      morePreparationQueries = (0 to 250).map { _ => "CREATE (:Location)" }: _*
    )

    operator("Node By Id Seek", "NodeByIdSeek",
      """The `NodeByIdSeek` operator reads one or more nodes by id from the node store.""",
      """MATCH (n) WHERE id(n) = 0 RETURN n"""
    )

    operator("Projection", "Projection",
      """For each incoming row, the `Projection` operator evaluates a set of expressions and produces a row with the results of the expressions.""",
      """RETURN 'hello' AS greeting"""
    )

    operator("Filter", "Filter",
      """The `Filter` operator filters each row coming from the child operator, only passing through rows that evaluate the predicates to `true`.""",
      """MATCH (p:Person) WHERE p.name =~ '^a.*' RETURN p"""
    )

    operator("Cartesian Product", "CartesianProduct",
      """The `CartesianProduct` operator produces a cartesian product of the two inputs -- each row coming from the left child operator will be combined with all the rows from the right child operator.
        |`CartesianProduct` generally exhibits bad performance and ought to be avoided if possible.
      """,
      """MATCH (p:Person), (t:Team) RETURN p, t"""
    )

    operator("Optional Expand All", "OptionalExpand(All)",
      """The `OptionalExpand(All)` operator is analogous to <<query-plan-expand-all, Expand(All)>>, apart from when no relationships match the direction, type and property predicates.
        |In this situation, `OptionalExpand(all)` will return a single row with the relationship and end node set to `null`.""",
      """MATCH (p:Person)
             OPTIONAL MATCH (p)-[works_in:WORKS_IN]->(l) WHERE works_in.duration > 180
             RETURN p, l"""
    )

    operator("Sort", "Sort",
      """The `Sort` operator sorts rows by a provided key.
        |In order to sort the data, all data from the source operator needs to be pulled in eagerly and kept in the query state, which will lead to increased memory pressure in the system.""",
      """MATCH (p:Person) RETURN p ORDER BY p.name"""
    )

    operator("Top", "Top",
      """The `Top` operator returns the first 'n' rows sorted by a provided key. Instead of sorting the entire input, only the top 'n' rows are retained.""",
      """MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 2"""
    )

    operator("Limit", "Limit",
      """The `Limit` operator returns the first 'n' rows from the incoming input.""",
      """MATCH (p:Person) RETURN p LIMIT 3"""
    )

    operator("Lock Nodes", "LockNodes",
      """The `LockNodes` operator locks the start and end node when creating a relationship.""",
      """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)"""
    )

    operator("Optional", "Optional",
      """The `Optional` operator is used to solve some <<query-optional-match, OPTIONAL MATCH>> queries.
        |It will pull data from its source, simply passing it through if any data exists.
        |However, if no data is returned by its source, `Optional` will yield a single row with all columns set to `null`.""",
      """MATCH (p:Person {name:'me'}) OPTIONAL MATCH (q:Person {name: 'Lulu'}) RETURN p, q"""
    )

    operator("Project Endpoints", "ProjectEndpoints",
      """The `ProjectEndpoints` operator projects the start and end node of a relationship.""",
      """CREATE (n)-[p:KNOWS]->(m) WITH p AS r MATCH (u)-[r]->(v) RETURN u, v"""
    )

    operator("Expand All", "Expand(All)",
      """Given a start node, and depending on the pattern relationship, the `Expand(All)` operator will traverse incoming or outgoing relationships.""",
      """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof) RETURN fof"""
    )

    operator("Expand Into", "Expand(Into)",
      """When both the start and end node have already been found, the `Expand(Into)` operator is used to find all relationships connecting the two nodes.
        |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
        |This can make a noticeable difference when dense nodes appear as end points.""",
      """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof)-->(p) RETURN fof"""
    )


    operator("Optional Expand Into", "OptionalExpand(Into)",
      """The `OptionalExpand(Into)` operator is analogous to <<query-plan-expand-into, Expand(Into)>>, apart from when no matching relationships are found.
        |In this situation, `OptionalExpand(Into)` will return a single row with the relationship and end node set to `null`.
        |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
        |This can make a noticeable difference when dense nodes appear as end points.""",
      """MATCH (p:Person)-[works_in:WORKS_IN]->(l) OPTIONAL MATCH (l)-->(p) RETURN p"""
    )

    operator("VarLength Expand All", "VarLengthExpand(All)",
      """Given a start node, the `VarLengthExpand(All)` operator will traverse variable-length relationships.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN p, q"""
    )

    operator("VarLength Expand Into", "VarLengthExpand(Into)",
      """When both the start and end node have already been found, the `VarLengthExpand(Into)` operator is used to find all variable-length relationships connecting the two nodes.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(p:Person) RETURN p"""
    )

    operator("VarLength Expand Full Pruning", "VarLengthExpand(FullPruning)",
      """The `VarLengthExpand(FullPruning)` operator is a more powerful variant of the <<query-plan-varlength-expand-pruning, `VarLengthExpand(Pruning)`>> operator.
        |By building up more state,`VarLengthExpand(FullPruning)` is guaranteed to produce unique end nodes.
      """,
      """MATCH (p:Person)-[:FRIENDS_WITH *4..5]-(q:Person) RETURN DISTINCT p, q"""
    )

    operator("VarLength Expand Pruning", "VarLengthExpand(Pruning)",
      """Given a start node, the `VarLengthExpand(Pruning)` operator will traverse variable-length relationships much like the <<query-plan-varlength-expand-all, `VarLengthExpand(All)`>> operator.
        |However, as an optimization, some paths will not be explored if they are guaranteed to produce an end node that has already been found (by means of a previous path traversal).
        |This will only be used in cases where the individual paths are not of interest.
        |`VarLengthExpand(Pruning)` does not guarantee that all the end nodes will be unique (in contrast to <<query-plan-varlength-expand-full-pruning, `VarLengthExpand(FullPruning)`>>), but fewer duplicates will be produced than if <<query-plan-varlength-expand-all, `VarLengthExpand(All)`>> were used.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN DISTINCT p, q"""
    )

    operator("Directed Relationship By Id Seek", "DirectedRelationshipByIdSeek",
      """The `DirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store, and produces both the relationship and the nodes on either side.""",
      """MATCH (n1)-[r]->()
             WHERE id(r) = 0
             RETURN r, n1"""
    )

    operator("Undirected Relationship By Id Seek", "UndirectedRelationshipByIdSeek",
      """The `UndirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store.
        |As the direction is unspecified, two rows are produced for each relationship as a result of alternating the combination of the start and end node.""",
      """MATCH (n1)-[r]-()
             WHERE id(r) = 1
             RETURN r, n1"""
    )


    operator("Skip", "Skip",
      """The `Skip` operator skips 'n' rows from the incoming rows.""",
      """MATCH (p:Person)
             RETURN p
             ORDER BY p.id
             SKIP 1"""
    )

    operator("Union", "Union",
      "The `Union` operator concatenates the results from the right child operator with the results from the left child operator.",
      """MATCH (p:Location)
             RETURN p.name
             UNION ALL
             MATCH (p:Country)
             RETURN p.name"""
    )

    operator("Unwind", "Unwind",
      """The `Unwind` operator returns one row per item in a list.""",
      """UNWIND range(1, 5) as value return value"""
    )

    operator("Apply", "Apply",
        """
          |All the different `Apply` operators (listed below) share the same basic functionality: they perform a nested loop by taking a single row from the left-hand side, and using the <<query-plan-argument, Argument>> operator on the right-hand side, execute the operator tree on the right-hand side.
          |The versions of the `Apply` operators differ in how the results are managed.
          |The `Apply` operator (i.e. the standard version) takes the row produced by the right-hand side -- which at this point contains data from both the left-hand and right-hand sides -- and yields it..""",
      """MATCH (p:Person {name:'me'})
        |MATCH (q:Person {name: p.secondName})
        |RETURN p, q"""
    )

    operator("Semi Apply", "SemiApply",
      """The `SemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the right-hand side operator yields at least one row, the row from the left-hand side operator is yielded by the `SemiApply` operator.
        |This makes `SemiApply` a filtering operator, used mostly for pattern predicates in queries.""",
      """MATCH (p:Person)
        |WHERE (p)-[:FRIENDS_WITH]->(:Person)
        |RETURN p.name"""
    )

    operator("Anti Semi Apply", "AntiSemiApply",
      """The `AntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the right-hand side operator yields no rows, the row from the left-hand side operator is yielded by the `AntiSemiApply` operator.
        |This makes `AntiSemiApply` a filtering operator, used for pattern predicates in queries.""",
      """MATCH (me:Person {name: "me"}), (other:Person)
        |WHERE NOT (me)-[:FRIENDS_WITH]->(other)
        |RETURN other.name"""
    )
    operator("Let Semi Apply", "LetSemiApply",
      """The `LetSemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
        |When a query contains multiple pattern predicates separated with `OR`, `LetSemiApply` will be used to evaluate the first of these.
        |It will record the result of evaluating the predicate but will leave any filtering to another operator.
        |In the example, `LetSemiApply` will be used to check for the presence of the `FRIENDS_WITH`
        |relationship from each person.""",
      """MATCH (other:Person)
        |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location)
        |RETURN other.name"""
    )

    operator("Let Anti Semi Apply", "LetAntiSemiApply",
      """The `LetAntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
        |When a query contains multiple negated pattern predicates -- i.e. predicates separated with `OR`, where at
        |least one predicate contains `NOT` -- `LetAntiSemiApply` will be used to evaluate the first of these.
        |It will record the result of evaluating the predicate but will leave any filtering to another operator.
        |In the example, `LetAntiSemiApply` will be used to check for the absence of
        |the `FRIENDS_WITH` relationship from each person.""",
      """MATCH (other:Person)
        |WHERE NOT ((other)-[:FRIENDS_WITH]->(:Person)) OR (other)-[:WORKS_IN]->(:Location)
        |RETURN other.name"""
    )

    operator("Select Or Semi Apply", "SelectOrSemiApply",
      """The `SelectOrSemiApply` operator tests for the presence of a pattern predicate and evaluates a predicate,
        |and is a variation of the <<query-plan-apply, Apply>> operator.
        |This operator allows for the mixing of normal predicates and pattern predicates
        |that check for the presence of a pattern.
        |First, the normal expression predicate is evaluated, and, only if it returns `false`, is the costly pattern predicate evaluated.""",
      """MATCH (other:Person)
        |WHERE other.age > 25 OR (other)-[:FRIENDS_WITH]->(:Person)
        |RETURN other.name"""
    )

    operator("Select Or Anti Semi Apply", "SelectOrAntiSemiApply",
      """The `SelectOrAntiSemiApply` operator is used to evaluate `OR` between a predicate and a negative pattern predicate
        |(i.e. a pattern predicate preceded with `NOT`), and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the predicate returns `true`, the pattern predicate is not tested.
        |If the predicate returns `false` or `null`, `SelectOrAntiSemiApply` will instead test the pattern predicate.""",
      """MATCH (other:Person)
        |WHERE other.age > 25 OR NOT (other)-[:FRIENDS_WITH]->(:Person)
        |RETURN other.name"""
    )

    operator("Conditional Apply", "ConditionalApply",
      """The `ConditionalApply` operator checks whether a variable is not `null`, and if so, the right child operator will be executed.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MERGE (p:Person {name: 'Andres'})
        |ON MATCH SET p.exists = true"""
    )

    operator("Anti Conditional Apply", "AntiConditionalApply",
      """The `AntiConditionalApply` operator checks whether a variable is `null`, and if so, the right child operator will be executed.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MERGE (p:Person {name: 'Andres'})
        |ON CREATE SET p.exists = true"""
    )

    operator("Assert Same Node", "AssertSameNode",
      """The `AssertSameNode` operator is used to ensure that no unique constraints are violated.
        |The example looks for the presence of a team with the supplied name and id, and if one does not exist,
        |it will be created. Owing to the existence of two unique constraints
        |on `:Team(name)` and `:Team(id)`, any node that would be found by the `UniqueIndexSeek`
        |must be the very same node, or the constraints would be violated.
      """,
      """MERGE (t:Team {name: 'Engineering', id: 42})"""
    )

    operator("Node By Id Seek", "NodeByIdSeek",
      """The `NodeByIdSeek` operator reads one or more nodes by id from the node store.""",
      """MATCH (n) WHERE id(n) = 0 RETURN n"""
    )

    operator("Projection", "Projection",
      """For each incoming row, the `Projection` operator evaluates a set of expressions and produces a row with the results of the expressions.""",
      """RETURN 'hello' AS greeting"""
    )

    operator("Filter", "Filter",
      """The `Filter` operator filters each row coming from the child operator, only passing through rows that evaluate the predicates to `true`.""",
      """MATCH (p:Person) WHERE p.name =~ '^a.*' RETURN p"""
    )

    operator("Cartesian Product", "CartesianProduct",
      """The `CartesianProduct` operator produces a cartesian product of the two inputs -- each row coming from the left child operator will be combined with all the rows from the right child operator.
        |`CartesianProduct` generally exhibits bad performance and ought to be avoided if possible.
      """,
      """MATCH (p:Person), (t:Team) RETURN p, t"""
    )

    operator("Optional Expand All", "OptionalExpand(All)",
      """The `OptionalExpand(All)` operator is analogous to <<query-plan-expand-all, Expand(All)>>, apart from when no relationships match the direction, type and property predicates.
        |In this situation, `OptionalExpand(all)` will return a single row with the relationship and end node set to `null`."""
      ,
      """MATCH (p:Person)
             OPTIONAL MATCH (p)-[works_in:WORKS_IN]->(l) WHERE works_in.duration > 180
             RETURN p, l"""
    )

    operator("Sort", "Sort",
      """The `Sort` operator sorts rows by a provided key.
        |In order to sort the data, all data from the source operator needs to be pulled in eagerly and kept in the query state, which will lead to increased memory pressure in the system.""",
      """MATCH (p:Person) RETURN p ORDER BY p.name"""
    )

    operator("Top", "Top",
      """The `Top` operator returns the first 'n' rows sorted by a provided key. Instead of sorting the entire input, only the top 'n' rows are retained.""",
      """MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 2"""
    )

    operator("Limit", "Limit",
      """The `Limit` operator returns the first 'n' rows from the incoming input.""",
      """MATCH (p:Person) RETURN p LIMIT 3"""
    )

    operator("Lock Nodes", "LockNodes",
      """The `LockNodes` operator locks the start and end node when creating a relationship.""",
      """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)"""
    )

    operator("Optional", "Optional",
      """The `Optional` operator is used to solve some <<query-optional-match, OPTIONAL MATCH>> queries.
        |It will pull data from its source, simply passing it through if any data exists.
        |However, if no data is returned by its source, `Optional` will yield a single row with all columns set to `null`.""",
      """MATCH (p:Person {name:'me'}) OPTIONAL MATCH (q:Person {name: 'Lulu'}) RETURN p, q"""
    )

    operator("Project Endpoints", "ProjectEndpoints",
      """The `ProjectEndpoints` operator projects the start and end node of a relationship.""",
      """CREATE (n)-[p:KNOWS]->(m) WITH p AS r MATCH (u)-[r]->(v) RETURN u, v"""
    )

    operator("Expand All", "Expand(All)",
      """Given a start node, and depending on the pattern relationship, the `Expand(All)` operator will traverse incoming or outgoing relationships.""",
      """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof) RETURN fof"""
    )

    operator("Expand Into", "Expand(Into)",
      """When both the start and end node have already been found, the `Expand(Into)` operator is used to find all relationships connecting the two nodes.
        |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
        |This can make a noticeable difference when dense nodes appear as end points.""",
      """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof)-->(p) RETURN fof"""
    )


    operator("Optional Expand Into", "OptionalExpand(Into)",
      """The `OptionalExpand(Into)` operator is analogous to <<query-plan-expand-into, Expand(Into)>>, apart from when no matching relationships are found.
        |In this situation, `OptionalExpand(Into)` will return a single row with the relationship and end node set to `null`.
        |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
        |This can make a noticeable difference when dense nodes appear as end points.""",
      """MATCH (p:Person)-[works_in:WORKS_IN]->(l) OPTIONAL MATCH (l)-->(p) RETURN p"""
    )

    operator("VarLength Expand All", "VarLengthExpand(All)",
      """Given a start node, the `VarLengthExpand(All)` operator will traverse variable-length relationships.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN p, q"""
    )

    operator("VarLength Expand Into", "VarLengthExpand(Into)",
      """When both the start and end node have already been found, the `VarLengthExpand(Into)` operator is used to find all variable-length relationships connecting the two nodes.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(p:Person) RETURN p"""
    )

    operator("VarLength Expand Full Pruning", "VarLengthExpand(FullPruning)",
      """The `VarLengthExpand(FullPruning)` operator is a more powerful variant of the <<query-plan-varlength-expand-pruning, `VarLengthExpand(Pruning)`>> operator.
        |By building up more state,`VarLengthExpand(FullPruning)` is guaranteed to produce unique end nodes.
      """,
      """MATCH (p:Person)-[:FRIENDS_WITH *4..5]-(q:Person) RETURN DISTINCT p, q"""
    )

    operator("VarLength Expand Pruning", "VarLengthExpand(Pruning)",
      """Given a start node, the `VarLengthExpand(Pruning)` operator will traverse variable-length relationships much like the <<query-plan-varlength-expand-all, `VarLengthExpand(All)`>> operator.
        |However, as an optimization, some paths will not be explored if they are guaranteed to produce an end node that has already been found (by means of a previous path traversal).
        |This will only be used in cases where the individual paths are not of interest.
        |`VarLengthExpand(Pruning)` does not guarantee that all the end nodes will be unique (in contrast to <<query-plan-varlength-expand-full-pruning, `VarLengthExpand(FullPruning)`>>), but fewer duplicates will be produced than if <<query-plan-varlength-expand-all, `VarLengthExpand(All)`>> were used.""",
      """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN DISTINCT p, q"""
    )

    operator("Directed Relationship By Id Seek", "DirectedRelationshipByIdSeek",
      """The `DirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store, and produces both the relationship and the nodes on either side.""",
      """MATCH (n1)-[r]->()
             WHERE id(r) = 0
             RETURN r, n1"""
    )

    operator("Undirected Relationship By Id Seek", "UndirectedRelationshipByIdSeek",
      """The `UndirectedRelationshipByIdSeek` operator reads one or more relationships by id from the relationship store.
        |As the direction is unspecified, two rows are produced for each relationship as a result of alternating the combination of the start and end node.""",
      """MATCH (n1)-[r]-()
             WHERE id(r) = 1
             RETURN r, n1"""
    )


    operator("Skip", "Skip",
      """The `Skip` operator skips 'n' rows from the incoming rows.""",
      """MATCH (p:Person)
             RETURN p
             ORDER BY p.id
             SKIP 1"""
    )

    operator("Union", "Union",
      "The `Union` operator concatenates the results from the right child operator with the results from the left child operator.",
      """MATCH (p:Location)
             RETURN p.name
             UNION ALL
             MATCH (p:Country)
             RETURN p.name"""
    )

    operator("Unwind", "Unwind",
      """The `Unwind` operator returns one row per item in a list.""",
      """UNWIND range(1, 5) as value return value"""
    )

    operator("Apply", "Apply",
      """
        |All the different `Apply` operators (listed below) share the same basic functionality: they perform a nested loop by taking a single row from the left-hand side, and using the <<query-plan-argument, Argument>> operator on the right-hand side, execute the operator tree on the right-hand side.
        |The versions of the `Apply` operators differ in how the results are managed.
        |The `Apply` operator (i.e. the standard version) takes the row produced by the right-hand side -- which at this point contains data from both the left-hand and right-hand sides -- and yields it..""",
      """MATCH (p:Person {name:'me'})
        |MATCH (q:Person {name: p.secondName})
        |RETURN p, q"""
    )

    operator("Semi Apply", "SemiApply",
      """The `SemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the right-hand side operator yields at least one row, the row from the left-hand side operator is yielded by the `SemiApply` operator.
        |This makes `SemiApply` a filtering operator, used mostly for pattern predicates in queries.""",
      """MATCH (p:Person)
        |WHERE (p)-[:FRIENDS_WITH]->(:Person)
        |RETURN p.name"""
    )

    operator("Anti Semi Apply", "AntiSemiApply",
      """The `AntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the right-hand side operator yields no rows, the row from the left-hand side operator is yielded by the `AntiSemiApply` operator.
        |This makes `AntiSemiApply` a filtering operator, used for pattern predicates in queries.""",
      """MATCH (me:Person {name: "me"}), (other:Person)
        |WHERE NOT (me)-[:FRIENDS_WITH]->(other)
        |RETURN other.name"""
    )
    operator("Let Semi Apply", "LetSemiApply",
      """The `LetSemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
        |When a query contains multiple pattern predicates separated with `OR`, `LetSemiApply` will be used to evaluate the first of these.
        |It will record the result of evaluating the predicate but will leave any filtering to another operator.
        |In the example, `LetSemiApply` will be used to check for the presence of the `FRIENDS_WITH`
        |relationship from each person.""",
      """MATCH (other:Person)
        |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location)
        |RETURN other.name"""
    )

    operator("Let Anti Semi Apply", "LetAntiSemiApply",
      """The `LetAntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
        |When a query contains multiple negated pattern predicates -- i.e. predicates separated with `OR`, where at
        |least one predicate contains `NOT` -- `LetAntiSemiApply` will be used to evaluate the first of these.
        |It will record the result of evaluating the predicate but will leave any filtering to another operator.
        |In the example, `LetAntiSemiApply` will be used to check for the absence of
        |the `FRIENDS_WITH` relationship from each person.""",
      """MATCH (other:Person)
        |WHERE NOT ((other)-[:FRIENDS_WITH]->(:Person)) OR (other)-[:WORKS_IN]->(:Location)
        |RETURN other.name"""
    )

    operator("Select Or Semi Apply", "SelectOrSemiApply",
      """The `SelectOrSemiApply` operator tests for the presence of a pattern predicate and evaluates a predicate,
        |and is a variation of the <<query-plan-apply, Apply>> operator.
        |This operator allows for the mixing of normal predicates and pattern predicates
        |that check for the presence of a pattern.
        |First, the normal expression predicate is evaluated, and, only if it returns `false`, is the costly pattern predicate evaluated.""",
      """MATCH (other:Person)
        |WHERE other.age > 25 OR (other)-[:FRIENDS_WITH]->(:Person)
        |RETURN other.name"""
    )

    operator("Select Or Anti Semi Apply", "SelectOrAntiSemiApply",
      """The `SelectOrAntiSemiApply` operator is used to evaluate `OR` between a predicate and a negative pattern predicate
        |(i.e. a pattern predicate preceded with `NOT`), and is a variation of the <<query-plan-apply, Apply>> operator.
        |If the predicate returns `true`, the pattern predicate is not tested.
        |If the predicate returns `false` or `null`, `SelectOrAntiSemiApply` will instead test the pattern predicate.""",
      """MATCH (other:Person)
        |WHERE other.age > 25 OR NOT (other)-[:FRIENDS_WITH]->(:Person)
        |RETURN other.name"""
    )

    operator("Conditional Apply", "ConditionalApply",
      """The `ConditionalApply` operator checks whether a variable is not `null`, and if so, the right child operator will be executed.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MERGE (p:Person {name: 'Andres'})
        |ON MATCH SET p.exists = true"""
    )

    operator("Anti Conditional Apply", "AntiConditionalApply",
      """The `AntiConditionalApply` operator checks whether a variable is `null`, and if so, the right child operator will be executed.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MERGE (p:Person {name: 'Andres'})
        |ON CREATE SET p.exists = true"""
    )

    operator("Assert Same Node", "AssertSameNode",
      """The `AssertSameNode` operator is used to ensure that no unique constraints are violated.
        |The example looks for the presence of a team with the supplied name and id, and if one does not exist,
        |it will be created. Owing to the existence of two unique constraints
        |on `:Team(name)` and `:Team(id)`, any node that would be found by the `UniqueIndexSeek`
        |must be the very same node, or the constraints would be violated.
      """,
      """MERGE (t:Team {name: 'Engineering', id: 42})"""
    )

    operator("Node Hash Join", "NodeHashJoin",
      """
        |The `NodeHashJoin` operator is a variation of the <<execution-plans-operators-hash-join-general, hash join>>.
        |`NodeHashJoin` executes the hash join on node ids.
        |As primitive types and arrays can be used, it can be done very efficiently.""",
      """MATCH (andy:Person {name:'Andreas'})-[:WORKS_IN]->(loc)<-[:WORKS_IN]-(matt:Person {name:'Mattis'})
        |RETURN loc.name""",
      morePreparationQueries =
        """MATCH (london:Location {name: 'London'}), (person:Person {name: 'Pontus'})
              FOREACH(x in range(0, 250) |
                CREATE (person) -[: WORKS_IN] ->(london)
                )"""
    )

    operator("Triadic Selection", "TriadicSelection",
      """The `TriadicSelection` operator is used to solve triangular queries, such as the very
        |common 'find my friend-of-friends that are not already my friend'.
        |It does so by putting all the friends into a set, and uses the set to check if the
        |friend-of-friends are already connected to me.
        |The example finds the names of all friends of my friends that are not already my friends.""",
      """MATCH (me:Person)-[:FRIENDS_WITH]-()-[:FRIENDS_WITH]-(other)
        |WHERE NOT (me)-[:FRIENDS_WITH]-(other)
        |RETURN other.name"""
    )

    operator("Foreach", "Foreach",
      """The `Foreach` operator executes a nested loop between the left child operator and the right child operator.
        | In an analogous manner to the <<query-plan-apply, Apply>> operator, it takes a row from the left-hand side and, using the <<query-plan-argument, Argument>> operator, provides it to the operator tree on the right-hand side.
        | `Foreach` will yield all the rows coming in from the left-hand side; all results from the right-hand side are pulled in and discarded.""",
      """FOREACH (value IN [1,2,3] |
        |CREATE (:Person {age: value})
        |)"""
    )

    operator("Empty Row", "EmptyRow",
      """The `EmptyRow` operator returns a single row with no columns.""",
      """FOREACH (value IN [1,2,3] |
        |CREATE (:Person {age: value})
        |)"""
    )

    operator("Let Select Or Semi Apply", "LetSelectOrSemiApply",
      """The `LetSelectOrSemiApply` operator is planned for pattern predicates that are combined with other predicates using `OR`.
        |This is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MATCH (other:Person)
        |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
        |RETURN other.name"""
    )

    operator("Let Select Or Anti Semi Apply", "LetSelectOrAntiSemiApply",
      """The `LetSelectOrAntiSemiApply` operator is planned for negated pattern predicates -- i.e. pattern predicates
        |preceded with `NOT` -- that are combined with other predicates using `OR`.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.
      """,
      """MATCH (other:Person)
        |WHERE NOT (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
        |RETURN other.name"""
    )

    operator("Node Outer Hash Join", "NodeOuterHashJoin",
      """The `NodeOuterHashJoin` operator is a variation of the <<execution-plans-operators-hash-join-general, hash join>>.
        |Instead of discarding rows that are not found in the probe table, `NodeOuterHashJoin` will instead yield a single row with `null`.""",
      """MATCH (p:Person {name:'me'})
        |OPTIONAL MATCH (p)--(q:Person {name: p.surname})
        |USING JOIN ON p
        |RETURN p,q"""
    )

    operator("Roll Up Apply", "RollUpApply",
      """The `RollUpApply` operator is used to execute an expression which takes as input a pattern, and returns a list with content from the matched pattern;
        |for example, when using a pattern expression or pattern comprehension in a query.
        |This operator is a variation of the <<query-plan-apply, Apply>> operator.""",
      """MATCH (p:Person)
        |RETURN p.name, [ (p)-[:WORKS_IN]->(location) | location.name ] AS cities"""
    )

    operator("Value Hash Join", "ValueHashJoin",
      """The `ValueHashJoin` operator is a variation of the <<execution-plans-operators-hash-join-general, hash join>>.
             This operator allows for arbitrary values to be used as the join key.
             It is most frequently used to solve predicates of the form: `n.prop1 = m.prop2` (i.e. equality predicates between two property columns).
          """,
      """MATCH (p:Person),(q:Person)
        |WHERE p.age = q.age
        |RETURN p,q"""
    )

    operator("Procedure Call", "ProcedureCall",
      """The `ProcedureCall` operator indicates an invocation to a procedure.""",
      """CALL db.labels() YIELD label RETURN * ORDER BY label"""
    )

    def operator(sectionName: String,
                 operatorName: String,
                 paragraph: String,
                 queryString: String,
                 morePreparationQueries: String*): Unit = {
      section(sectionName) {
        initQueries(morePreparationQueries: _*)
        p(paragraph.stripMargin)
        query(queryString.stripMargin, ResultAssertions((r) => {
          r.executionPlanDescription().toString should include(operatorName)
        })) {
          profileExecutionPlan()
        }
      }
    }

  }.build()

}
