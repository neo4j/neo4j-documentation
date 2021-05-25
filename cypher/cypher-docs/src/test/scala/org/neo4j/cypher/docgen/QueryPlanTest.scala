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

import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder
import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.dbms.api.DatabaseManagementService

import java.io.File

class QueryPlanTest extends DocumentingTestBase with SoftReset {

  override protected def newDatabaseManagementService(directory: File): DatabaseManagementService =
    new EnterpriseDatabaseManagementServiceBuilder(directory).setConfig(databaseConfig()).build()

  override val setupQueries = List(
    """CREATE (me:Person {name: 'me'})
       CREATE (andy:Person {name: 'Andy'})
       CREATE (bob:Person {name: 'Bob'})
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

       CREATE (me)-[:WORKS_IN {duration: 190, title: 'senior sales engineer'}]->(london)
       CREATE (bob)-[:WORKS_IN {duration: 187, title: 'junior developer'}]->(london)
       CREATE (andy)-[:WORKS_IN {duration: 150, title: ''}]->(london)
       CREATE (mattias)-[:WORKS_IN {duration: 230, title: 'senior developer'}]->(london)
       CREATE (lovis)-[:WORKS_IN {duration: 230, title: 'junior developer'}]->(sf)
       CREATE (pontus)-[:WORKS_IN {duration: 230, title: 'junior developer'}]->(malmo)
       CREATE (max)-[:WORKS_IN {duration: 230, title: 'field engineer'}]->(newyork)
       CREATE (konstantin)-[:WORKS_IN {duration: 230, title: 'frontend developer'}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230, title: 'chief architect'}]->(london)
       CREATE (stefan)-[:WORKS_IN {duration: 230, title: 'language architect'}]->(berlin)
       CREATE (mats)-[:WORKS_IN {duration: 230, title: 'senior developer'}]->(malmo)
       CREATE (petra)-[:WORKS_IN {duration: 230, title: 'language architect'}]->(london)
       CREATE (craig)-[:WORKS_IN {duration: 230, title: 'senior developer'}]->(malmo)
       CREATE (steven)-[:WORKS_IN {duration: 230, title: 'junior developer'}]->(malmo)
       CREATE (chris)-[:WORKS_IN {duration: 230, title: 'field engineer'}]->(madrid)
       CREATE (london)-[:IN]->(england)
       CREATE (me)-[:FRIENDS_WITH]->(andy)
       CREATE (andy)-[:FRIENDS_WITH]->(bob)
    """.stripMargin)

  override val setupConstraintQueries = List(
    "CREATE INDEX FOR (n:Location) ON (n.name)",
    "CREATE INDEX FOR (n:Person) ON (n.name)",
    "CREATE INDEX FOR ()-[r:WORKS_IN]-() ON (r.duration)",
    "CREATE INDEX FOR ()-[r:WORKS_IN]-() ON (r.title)",
    "CREATE CONSTRAINT ON (team:Team) ASSERT team.name is UNIQUE",
    "CREATE CONSTRAINT ON (team:Team) ASSERT team.id is UNIQUE"
  )

  def section = "Query Plan"

  @Test def allNodesScan() {
    profileQuery(
      title = "All Nodes Scan",
      text =
        """The `AllNodesScan` operator reads all nodes from the node store. The variable that will contain the nodes is seen in the arguments.
          |Any query using this operator is likely to encounter performance problems on a non-trivial database.""".stripMargin,
      queryText = """MATCH (n) RETURN n""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("AllNodesScan"))
    )
  }

  @Test def createUniqueConstraint() {
    profileQuery(
      title = "Create Unique Constraint",
      text =
        """The `CreateUniqueConstraint` operator creates a unique constraint on a property for all nodes having a certain label.
          |The following query will create a unique constraint with the name `uniqueness` on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """CREATE CONSTRAINT uniqueness ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateConstraint"))
        assertThat(plan, containsString("uniqueness"))
      }
    )
  }

  @Test def dropUniqueConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE")
    }

    profileQuery(
      title = "Drop Unique Constraint",
      text =
        """The `DropUniqueConstraint` operator removes a unique constraint from a property for all nodes having a certain label.
          |The following query will drop a unique constraint on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """DROP CONSTRAINT ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = p => assertThat(p.executionPlanString(), containsString("DropConstraint"))
    )
  }

  @Test def doNothingIfExistsForConstraint() {
    profileQuery(
      title = "Create Constraint only if it does not already exist",
      text =
        """To not get an error creating the same constraint twice, we use the `DoNothingIfExists` operator for constraints.
          |This will make sure no other constraint with the given name or another constraint of the same type and schema already exists before the specific `CreateConstraint` operator creates the constraint.
          |If it finds a constraint with the given name or with the same type and schema it will stop the execution and no new constraint is created.
          |The following query will create a unique constraint with the name `uniqueness` on the `name` property of nodes with the `Country` label only if
          |no constraint named `uniqueness` or unique constraint on `(:Country {name})` already exists.""".stripMargin,
      queryText = """CREATE CONSTRAINT uniqueness IF NOT EXISTS ON (c:Country) ASSERT c.name is UNIQUE""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateConstraint"))
        assertThat(plan, containsString("uniqueness"))
        assertThat(plan, containsString("DoNothingIfExists(CONSTRAINT)"))
      }
    )
  }

  @Test def createNodePropertyExistenceConstraint() {
    profileQuery(
      title = "Create Node Property Existence Constraint",
      text =
        """The `CreateNodePropertyExistenceConstraint` operator creates an existence constraint with the name `existence` on a property for all nodes having a certain label.
          |This will only appear in Enterprise Edition.
        """.stripMargin,
      queryText = """CREATE CONSTRAINT existence ON (p:Person) ASSERT p.name IS NOT NULL""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateConstraint"))
        assertThat(plan, containsString("existence"))
      }
    )
  }

  @Test def dropNodePropertyExistenceConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON (p:Person) ASSERT p.name IS NOT NULL")
    }

    profileQuery(
      title = "Drop Node Property Existence Constraint",
      text =
        """The `DropNodePropertyExistenceConstraint` operator removes an existence constraint from a property for all nodes having a certain label.
          |This will only appear in Enterprise Edition.
        """.stripMargin,
      queryText = """DROP CONSTRAINT ON (p:Person) ASSERT exists(p.name)""",
      assertions = p => assertThat(p.executionPlanString(), containsString("DropConstraint"))
    )
  }

  @Test def createNodeKeyConstraint() {
    profileQuery(
      title = "Create Node Key Constraint",
      text =
        """The `CreateNodeKeyConstraint` operator creates a node key constraint with the name `node_key` which ensures
          |that all nodes with a particular label have a set of defined properties whose combined value is unique, and where all properties in the set are present.
          |This will only appear in Enterprise Edition.
        """.stripMargin,
      queryText = """CREATE CONSTRAINT node_key ON (e:Employee) ASSERT (e.firstname, e.surname) IS NODE KEY""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateConstraint"))
        assertThat(plan, containsString("node_key"))
      }
    )
  }

  @Test def dropNodePropertyExistenceConstraint2() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON (e:Employee) ASSERT (e.firstname, e.surname) IS NODE KEY")
    }

    profileQuery(
      title = "Drop Node Key Constraint",
      text =
        """The `DropNodeKeyConstraint` operator removes a node key constraint from a set of properties for all nodes having a certain label.
          |This will only appear in Enterprise Edition.
        """.stripMargin,
      queryText = """DROP CONSTRAINT ON (e:Employee) ASSERT (e.firstname, e.surname) IS NODE KEY""",
      assertions = p => assertThat(p.executionPlanString(), containsString("DropConstraint"))
    )
  }

  @Test def createRelationshipPropertyExistenceConstraint() {
    profileQuery(
      title = "Create Relationship Property Existence Constraint",
      text =
        """The `CreateRelationshipPropertyExistenceConstraint` operator creates an existence constraint with the name `existence` on a property for all relationships of a certain type.
          |This will only appear in Enterprise Edition.
        """.stripMargin,
      queryText = """CREATE CONSTRAINT existence ON ()-[l:LIKED]-() ASSERT l.when IS NOT NULL""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateConstraint"))
        assertThat(plan, containsString("existence"))
      }
    )
  }

  @Test def dropRelationshipPropertyExistenceConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT ON ()-[l:LIKED]-() ASSERT l.when IS NOT NULL")
    }

    profileQuery(
      title = "Drop Relationship Property Existence Constraint",
      text =
        """The `DropRelationshipPropertyExistenceConstraint` operator removes an existence constraint from a property for all relationships of a certain type.
          |This will only appear in Enterprise Edition.""".stripMargin,
      queryText = """DROP CONSTRAINT ON ()-[l:LIKED]-() ASSERT exists(l.when)""",
      assertions = p => assertThat(p.executionPlanString(), containsString("DropConstraint"))
    )
  }

  @Test def dropNamedConstraint() {
    executePreparationQueries {
      List("CREATE CONSTRAINT name ON (c:Country) ASSERT c.name is UNIQUE")
    }

    profileQuery(
      title = "Drop Constraint by name",
      text =
        """The `DropConstraint` operator removes a constraint using the name of the constraint, no matter the type.""".stripMargin,
      queryText = """DROP CONSTRAINT name""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("DropConstraint"))
        assertThat(plan, containsString("name"))
      }
    )
  }

  @Test def showConstraints() {
    executePreparationQueries {
      List("CREATE CONSTRAINT name ON (c:Country) ASSERT c.name is UNIQUE")
    }

    profileQuery(
      title = "List constraints",
      text =
        """The `ShowConstraints` operator lists constraints. It may include filtering on constraint type and can have either default or full output.""".stripMargin,
      queryText = """SHOW CONSTRAINTS""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("ShowConstraints"))
      }
    )
  }

  @Test def createIndex() {
    profileQuery(
      title = "Create Index",
      text =
        """The `CreateIndex` operator creates an index. This index can either be on a property for all nodes or relationships having a certain label or relationship type,
          |or it can be a lookup index for all nodes or relationships having any label or relationship type.
          |The following query will create an index with the name `my_index` on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """CREATE INDEX my_index FOR (c:Country) ON (c.name)""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateIndex"))
        assertThat(plan, containsString("my_index"))
      }
    )
  }

  @Test def doNothingIfExistsForIndex() {
    executePreparationQueries {
      List("CREATE INDEX my_index FOR ()-[k:KNOWS]-() ON (k.since)")
    }

    profileQuery(
      title = "Create Index only if it does not already exist",
      text =
        """To not get an error creating the same index twice, we use the `DoNothingIfExists` operator for indexes.
          |This will make sure no other index with the given name or schema already exists before the `CreateIndex` operator creates an index.
          |If it finds an index with the given name or schema it will stop the execution and no new index is created.
          |The following query will create an index with the name `my_index` on the `since` property of relationships with the `KNOWS` relationship type only if no such index already exists.""".stripMargin,
      queryText = """CREATE INDEX my_index IF NOT EXISTS FOR ()-[k:KNOWS]-() ON (k.since)""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("CreateIndex"))
        assertThat(plan, containsString("my_index"))
        assertThat(plan, containsString("DoNothingIfExists(INDEX)"))
      }
    )
  }

  @Test def dropIndex() {
    executePreparationQueries {
      List("CREATE INDEX FOR (c:Country) ON (c.name)")
    }

    profileQuery(
      title = "Drop Index by schema",
      text =
        """The `DropIndex` operator removes an index from a property for all nodes having a certain label.
          |The following query will drop an index on the `name` property of nodes with the `Country` label.""".stripMargin,
      queryText = """DROP INDEX ON :Country(name)""",
      assertions = p => assertThat(p.executionPlanString(), containsString("DropIndex"))
    )
  }

  @Test def dropNamedIndex() {
    executePreparationQueries {
      List("CREATE INDEX name FOR (c:Country) ON (c.name)")
    }

    profileQuery(
      title = "Drop Index by name",
      text =
        """The `DropIndex` operator removes an index using the name of the index.""".stripMargin,
      queryText = """DROP INDEX name""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("DropIndex"))
        assertThat(plan, containsString("name"))
      }
    )
  }

  @Test def showIndexes() {
    executePreparationQueries {
      List("CREATE INDEX name FOR (c:Country) ON (c.name)")
    }

    profileQuery(
      title = "List indexes",
      text =
        """The `ShowIndexes` operator lists indexes. It may include filtering on index type and can have either default or full output.""".stripMargin,
      queryText = """SHOW INDEXES""",
      assertions = p => {
        val plan = p.executionPlanString()
        assertThat(plan, containsString("ShowIndexes"))
      }
    )
  }

  @Test def distinct() {
    profileQuery(
      title = "Distinct",
      text =
        """The `Distinct` operator removes duplicate rows from the incoming stream of rows.
          |To ensure only distinct elements are returned, `Distinct` will pull in data lazily from its source and build up state.
          |This may lead to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN DISTINCT l""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Distinct"))
    )
  }

  @Test def orderedDistinct() {
    profileQuery(
      title = "Ordered Distinct",
      text =
        """The `OrderedDistinct` operator is an optimization of the `Distinct` operator that takes advantage of the ordering of the incoming rows.
          |This operator has a lower memory pressure in the system than the `Distinct` operator.
        """.stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name STARTS WITH 'P' RETURN DISTINCT p.name""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("OrderedDistinct"))
      )
  }

  @Test def eagerAggregation() {
    profileQuery(
      title = "Eager Aggregation",
      text =
        """The `EagerAggregation` operator evaluates a grouping expression and uses the result to group rows into different groupings.
          |For each of these groupings, `EagerAggregation` will then evaluate all aggregation functions and return the result.
          |To do this, `EagerAggregation`, as the name implies, needs to pull in all data eagerly from its source and build up state, which leads to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN l.name AS location, collect(p.name) AS people""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("EagerAggregation"))
    )
  }

  @Test def orderedAggregation() {
    profileQuery(
      title = "Ordered Aggregation",
      text =
        """The `OrderedAggregation` operator is an optimization of the `EagerAggregation` operator that takes advantage of the ordering of the incoming rows.
          |This operator uses lazy evaluation and has a lower memory pressure in the system than the `EagerAggregation` operator.
        """.stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name STARTS WITH 'P' RETURN p.name, count(*) AS count""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("OrderedAggregation"))
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
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeCountFromCountStore"))
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
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("RelationshipCountFromCountStore"))
    )
  }

  @Test def eager() {
    profileQuery(
      title = "Eager",
      text =
        """For isolation purposes, the `Eager` operator ensures that operations affecting subsequent operations are executed fully for the whole dataset before continuing execution.
           | Information from the stores is fetched in a lazy manner; i.e. the pattern matching might not be fully exhausted before updates are applied.
           | To guarantee reasonable semantics, the query planner will insert `Eager` operators into the query plan to prevent updates from influencing pattern matching;
           | this scenario is exemplified by the query below, where the `DELETE` clause influences the `MATCH` clause.
           | The `Eager` operator can cause high memory usage when importing data or migrating graph structures.
           | In such cases, the operations should be split into simpler steps; e.g. importing nodes and relationships separately.
           | Alternatively, the records to be updated can be returned, followed by an update statement.""".stripMargin,
      queryText = """MATCH (a)-[r]-(b) DELETE r,a,b MERGE ()""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Eager"))
    )
  }

  @Test def create() {
    profileQuery(
      title = "Create Nodes / Relationships",
      text =
        """The `Create` operator is used to create nodes and relationships.""".stripMargin,
      queryText =
        """CREATE (max:Person {name: 'Max'}), (chris:Person {name: 'Chris'})
          |CREATE (max)-[:FRIENDS_WITH]->(chris)""".stripMargin,

      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Create"))
    )
  }

  @Test def delete() {
    profileQuery(
      title = "Delete",
      text =
        """The `Delete` operator is used to delete a node or a relationship.""".stripMargin,
      queryText =
        """MATCH (me:Person {name: 'me'})-[w:WORKS_IN {duration: 190}]->(london:Location {name: 'London'})
          |DELETE w""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Delete"))
    )
  }

  @Test def detachDelete() {
    profileQuery(
      title = "Detach Delete",
      text =
        """The `DetachDelete` operator is used in all queries containing the <<query-delete, DETACH DELETE>> clause, when deleting nodes and their relationships.""".stripMargin,
      queryText =
        """MATCH (p:Person)
          |DETACH DELETE p""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DetachDelete"))
    )
  }

  @Test def removeLabels() {
    profileQuery(
      title = "Remove Labels",
      text =
        """The `RemoveLabels` operator is used when deleting labels from a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |REMOVE n:Person""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("RemoveLabels"))
    )
  }

  @Test def setLabels() {
    profileQuery(
      title = "Set Labels",
      text =
        """The `SetLabels` operator is used when setting labels on a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n:Person""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SetLabels"))
    )
  }

  @Test def setNodePropertiesFromMap() {
    profileQuery(
      title = "Set Node Properties From Map",
      text =
        """The `SetNodePropertiesFromMap` operator is used when setting properties from a map on a node.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n = {weekday: 'Monday', meal: 'Lunch'}""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SetNodePropertiesFromMap"))
    )
  }

  @Test def setRelationshipPropertiesFromMap() {
    profileQuery(
      title = "Set Relationship Properties From Map",
      text =
        """The `SetRelationshipPropertiesFromMap` operator is used when setting properties from a map on a relationship.""".stripMargin,
      queryText =
        """MATCH (n)-[r]->(m)
          |SET r = {weight: 5, unit: 'kg'}""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SetRelationshipPropertiesFromMap"))
    )
  }

  @Test def setProperty() {
    profileQuery(
      title = "Set Property",
      text =
        """The `SetProperty` operator is used when setting a property on a node or relationship.""".stripMargin,
      queryText =
        """MATCH (n)
          |SET n.checked = true""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SetProperty"))
    )
  }

  @Test def emptyResult() {
    profileQuery(
      title = "Empty Result",
      text =
        """The `EmptyResult` operator eagerly loads all incoming data and discards it.""".stripMargin,
      queryText = """CREATE (:Person)""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("EmptyResult"))
    )
  }

  @Test def produceResults() {
    profileQuery(
      title = "Produce Results",
      text =
        """The `ProduceResults` operator prepares the result so that it is consumable by the user, such as transforming internal values to user values.
          |It is present in every single query that returns data to the user, and has little bearing on performance optimisation.""".stripMargin,
      queryText = """MATCH (n) RETURN n""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("ProduceResults"))
    )
  }

  @Test def nodeByLabelScan() {
    executePreparationQueries {
      List("CREATE LOOKUP INDEX lookup_index_name FOR (n) ON EACH labels(n)")
    }

    profileQuery(
      title = "Node By Label Scan",
      text = """The `NodeByLabelScan` operator fetches all nodes with a specific label from the node label index.""".stripMargin,
      queryText = """MATCH (person:Person) RETURN person""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeByLabelScan"))
    )
  }

  @Test def directedRelationshipTypeScan() {
    executePreparationQueries {
      List("CREATE LOOKUP INDEX rel_lookup_index_name FOR ()-[r]-() ON EACH type(r)")
    }

    profileQuery(
      title = "Directed Relationship Type Scan",
      text = """The `DirectedRelationshipTypeScan` operator fetches all relationships and their start and end nodes with a specific type from the relationship type index.""".stripMargin,
      queryText = """MATCH ()-[r: FRIENDS_WITH]->() RETURN r""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipTypeScan"))
    )
  }

  @Test def undirectedRelationshipTypeScan() {
    executePreparationQueries {
      List("CREATE LOOKUP INDEX rel_lookup_index_name FOR ()-[r]-() ON EACH type(r)")
    }

    profileQuery(
      title = "Undirected Relationship Type Scan",
      text = """The `UndirectedRelationshipTypeScan` operator fetches all relationships and their start and end nodes with a specific type from the relationship type index.""".stripMargin,
      queryText = """MATCH ()-[r: FRIENDS_WITH]-() RETURN r""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipTypeScan"))
    )
  }

  @Test def nodeByIndexSeek() {
    profileQuery(
      title = "Node Index Seek",
      text =
        """The `NodeIndexSeek` operator finds nodes using an index seek.
          |The node variable and the index used is shown in the arguments of the operator.
          |If the index is a unique index, the operator is instead called <<query-plan-node-unique-index-seek, NodeUniqueIndexSeek>>.""".stripMargin,
      queryText = """MATCH (location:Location {name: 'Malmo'}) RETURN location""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeek"))
    )
  }

  @Test def nodeByUniqueIndexSeek() {
    profileQuery(
      title = "Node Unique Index Seek",
      text = """The `NodeUniqueIndexSeek` operator finds nodes using an index seek within a unique index. The node variable and the index used is shown in the arguments of the operator.
               |If the index is not unique, the operator is instead called <<query-plan-node-index-seek, NodeIndexSeek>>.
               |If the index seek is used to solve a <<query-merge, MERGE>> clause, it will also be marked with `(Locking)`.
               |This makes it clear that any nodes returned from the index will be locked in order to prevent concurrent conflicting updates.""".stripMargin,
      queryText = """MATCH (t:Team {name: 'Malmo'}) RETURN t""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeUniqueIndexSeek"))
    )
  }

  @Test def multiNodeIndexSeek() {
    profileQuery(
      title = "Multi Node Index Seek",
      text =
        """The `MultiNodeIndexSeek` operator finds nodes using multiple index seeks.
          |It supports using multiple distinct indexes for different nodes in the query.
          |The node variables and the indexes used are shown in the arguments of the operator.
          |
          |The operator yields a cartesian product of all index seeks.
          |For example, if the operator does two seeks and the first seek finds the nodes `a1, a2` and the second `b1, b2, b3`,
          |the `MultiNodeIndexSeek` will yield the rows `(a1, b1), (a1, b2), (a1, b3), (a2, b1), (a2, b2), (a2, b3)`.
          |""".stripMargin,
      queryText =
        """CYPHER runtime=pipelined
          |MATCH (location:Location {name: 'Malmo'}), (person:Person {name: 'Bob'}) RETURN location, person""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("MultiNodeIndexSeek"))
    )
  }

  @Test def directedRelationshipIndexSeek() {
    profileQuery(
      title = "Directed Relationship Index Seek",
      text =
        """The `DirectedRelationshipIndexSeek` operator finds relationships and their start and end nodes using an index seek.
          |The relationship variable and the index used is shown in the arguments of the operator.""".stripMargin,
      queryText = """MATCH (candidate)-[r:WORKS_IN]->() WHERE r.title = 'chief architect' RETURN candidate""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipIndexSeek"))
    )
  }

  @Test def undirectedRelationshipIndexSeek() {
    profileQuery(
      title = "Relationship Index Seek",
      text =
        """The `UndirectedRelationshipIndexSeek` operator finds relationships and their start and end nodes using an index seek.
          |The relationship variable and the index used is shown in the arguments of the operator.""".stripMargin,
      queryText = """MATCH (candidate)-[r:WORKS_IN]-() WHERE r.title = 'chief architect' RETURN candidate""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipIndexSeek"))
    )
  }

  @Test def argument() {
    profileQuery(
      title = "Argument",
      text = """The `Argument` operator indicates the variable to be used as an argument to the right-hand side of an <<query-plan-apply, Apply>> operator.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Argument"))
    )
  }

  @Test def loadCSV() {
    profileQuery(
      title = "Load CSV",
      text =
        """The `LoadCSV` operator loads data from a CSV source into the query.
          |It is used whenever the <<query-load-csv, LOAD CSV>> clause is used in a query.""".stripMargin,
      queryText = """LOAD CSV FROM 'https://neo4j.com/docs/cypher-refcard/3.3/csv/artists.csv' AS line RETURN line""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LoadCSV"))
    )
  }

  @Test def nodeIndexRangeSeek() {
    executePreparationQueries {
      (0 to 300).map { i => s"CREATE (:Location {name: '$i'})" }.toList
    }

    sampleAllIndexesAndWait()

    profileQuery(title = "Node Index Seek By Range",
                 text =
                   """The `NodeIndexSeekByRange` operator finds nodes using an index seek where the value of the property matches a given prefix string.
                     |`NodeIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
                     |If the index is a unique index, the operator is instead called `NodeUniqueIndexSeekByRange`.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name STARTS WITH 'Lon' RETURN l",
                 assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeekByRange"))
    )
  }

  @Test def directedRelationshipIndexRangeSeek() {
    profileQuery(title = "Directed Relationship Index Seek By Range",
      text =
        """The `DirectedRelationshipIndexSeekByRange` operator finds relationships and their start and end nodes using an index seek where the value of the property matches a given prefix string.
          |`DirectedRelationshipIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.""".stripMargin,
      queryText = "MATCH (candidate: Person)-[r:WORKS_IN]->(location) WHERE r.duration > 100 RETURN candidate",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipIndexSeekByRange"))
    )
  }

  @Test def undirectedRelationshipIndexRangeSeek() {
    profileQuery(title = "Directed Relationship Index Seek By Range",
      text =
        """The `UndirectedRelationshipIndexSeekByRange` operator finds relationships and their start and end nodes using an index seek where the value of the property matches a given prefix string.
          |`UndirectedRelationshipIndexSeekByRange` can be used for `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.""".stripMargin,
      queryText = "MATCH (candidate: Person)-[r:WORKS_IN]->(location) WHERE r.duration > 100 RETURN candidate",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipIndexSeekByRange"))
    )
  }

  @Test def nodeUniqueIndexRangeSeek() {
    executePreparationQueries {
      (0 to 300).map { i => s"CREATE (:Team {name: '$i'})" }.toList
    }

    sampleAllIndexesAndWait()

    profileQuery(title = "Node Unique Index Seek By Range",
      text =
        """The `NodeUniqueIndexSeekByRange` operator finds nodes using an index seek within a unique index, where the value of the property matches a given prefix string.
          |`NodeUniqueIndexSeekByRange` is used by `STARTS WITH` and comparison operators such as `<`, `>`, `\<=` and `>=`.
          |If the index is not unique, the operator is instead called `NodeIndexSeekByRange`.""".stripMargin,
      queryText = "MATCH (t:Team) WHERE t.name STARTS WITH 'Ma' RETURN t",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeUniqueIndexSeekByRange"))
    )
  }


  @Test def nodeIndexScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Scan",
                 text = """
                          |The `NodeIndexScan` operator examines all values stored in an index, returning all nodes with a particular label having a specified property.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name IS NOT NULL RETURN l",
                 assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
    )
  }

  @Test def nodeIndexContainsScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Contains Scan",
                 text = """
                          |The `NodeIndexContainsScan` operator examines all values stored in an index, searching for entries
                          | containing a specific string; for example, in queries including `CONTAINS`.
                          | Although this is slower than an index seek (since all entries need to be
                          | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
                          | filter.""".stripMargin,
                 queryText = "MATCH (l:Location) WHERE l.name CONTAINS 'al' RETURN l",
                 assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexContainsScan"))
    )
  }

  @Test def nodeIndexEndsWithScan() {
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Location)"
    }.toList)
    profileQuery(title = "Node Index Ends With Scan",
      text = """
               |The `NodeIndexEndsWithScan` operator examines all values stored in an index, searching for entries
               | ending in a specific string; for example, in queries containing `ENDS WITH`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
               | filter.""".stripMargin,
      queryText = "MATCH (l:Location) WHERE l.name ENDS WITH 'al' RETURN l",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeIndexEndsWithScan"))
    )
  }

  @Test def directedRelationshipIndexScan() {
    profileQuery(title = "Directed Relationship Index Scan",
      text = """
               |The `DirectedRelationshipIndexScan` operator examines all values stored in an index, returning all relationships and their start and end nodes with a particular relationship type having a specified property.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]->() WHERE r.title IS NOT NULL RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipIndexScan"))
    )
  }

  @Test def undirectedRelationshipIndexScan() {
    profileQuery(title = "Undirected Relationship Index Scan",
      text = """
               |The `UndirectedRelationshipIndexScan` operator examines all values stored in an index, returning all relationships and their start and end nodes with a particular relationship type having a specified property.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]-() WHERE r.title IS NOT NULL RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipIndexScan"))
    )
  }

  @Test def directedRelationshipIndexContainsScan() {
    profileQuery(title = "Directed Relationship Index Contains Scan",
      text = """
               |The `DirectedRelationshipIndexContainsScan` operator examines all values stored in an index, searching for entries
               | containing a specific string; for example, in queries including `CONTAINS`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a type scan using `DirectedRelationshipTypeScan`, and a property store
               | filter.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]->() WHERE r.title CONTAINS 'senior' RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipIndexContainsScan"))
    )
  }

  @Test def undirectedRelationshipIndexContainsScan() {
    profileQuery(title = "Undirected Relationship Index Contains Scan",
      text = """
               |The `UndirectedRelationshipIndexContainsScan` operator examines all values stored in an index, searching for entries
               | containing a specific string; for example, in queries including `CONTAINS`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a type scan using `DirectedRelationshipTypeScan`, and a property store
               | filter.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]-() WHERE r.title CONTAINS 'senior' RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipIndexContainsScan"))
    )
  }

  @Test def directedRelationshipIndexEndsWithScan() {
    profileQuery(title = "Directed Relationship Index Ends With Scan",
      text = """
               |The `DirectedRelationshipIndexEndsWithScan` operator examines all values stored in an index, searching for entries
               | ending in a specific string; for example, in queries containing `ENDS WITH`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
               | filter.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]->() WHERE r.title ENDS WITH 'developer' RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipIndexEndsWithScan"))
    )
  }

  @Test def undirectedRelationshipIndexEndsWithScan() {
    profileQuery(title = "Undirected Relationship Index Ends With Scan",
      text = """
               |The `UndirectedRelationshipIndexEndsWithScan` operator examines all values stored in an index, searching for entries
               | ending in a specific string; for example, in queries containing `ENDS WITH`.
               | Although this is slower than an index seek (since all entries need to be
               | examined), it is still faster than the indirection resulting from a label scan using `NodeByLabelScan`, and a property store
               | filter.""".stripMargin,
      queryText = "MATCH ()-[r: WORKS_IN]-() WHERE r.title ENDS WITH 'developer' RETURN r",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipIndexEndsWithScan"))
    )
  }

  @Test def nodeByIdSeek() {
    profileQuery(
      title = "Node By Id Seek",
      text =
        """The `NodeByIdSeek` operator reads one or more nodes by id from the node store.""".stripMargin,
      queryText = """MATCH (n) WHERE id(n) = 0 RETURN n""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeByIdSeek"))
    )
  }

  @Test def projection() {
    profileQuery(
      title = "Projection",
      text =
        """For each incoming row, the `Projection` operator evaluates a set of expressions and produces a row with the results of the expressions.""".stripMargin,
      queryText = """RETURN 'hello' AS greeting""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Projection"))
    )
  }

  @Test def filter() {
    profileQuery(
      title = "Filter",
      text =
        """The `Filter` operator filters each row coming from the child operator, only passing through rows that evaluate the predicates to `true`.""".stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name =~ '^a.*' RETURN p""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Filter"))
    )
  }

  @Test def cartesianProduct() {
    profileQuery(
      title = "Cartesian Product",
      text =
        """The `CartesianProduct` operator produces a cartesian product of the two inputs -- each row coming from the left child operator will be combined with all the rows from the right child operator.
          |`CartesianProduct` generally exhibits bad performance and ought to be avoided if possible.
        """.stripMargin,
      queryText = """MATCH (p:Person), (t:Team) RETURN p, t""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("CartesianProduct"))
    )
  }

  @Test def optionalExpand() {
    profileQuery(
      title = "Optional Expand All",
      text =
        """The `OptionalExpand(All)` operator is analogous to <<query-plan-expand-all, Expand(All)>>, apart from when no relationships match the direction, type and property predicates.
          |In this situation, `OptionalExpand(all)` will return a single row with the relationship and end node set to `null`.
          |""".stripMargin,
      queryText =
        """MATCH (p:Person)
           OPTIONAL MATCH (p)-[works_in:WORKS_IN]->(l) WHERE works_in.duration > 180
           RETURN p, l""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(All)"))
    )
  }

  @Test def sort() {
    profileQuery(
      title = "Sort",
      text =
        """The `Sort` operator sorts rows by a provided key.
          |In order to sort the data, all data from the source operator needs to be pulled in eagerly and kept in the query state, which will lead to increased memory pressure in the system.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Sort"))
    )
  }

  @Test def partialSort() {
    profileQuery(
      title = "Partial Sort",
      text =
        """The `PartialSort` operator is an optimization of the `Sort` operator that takes advantage of the ordering of the incoming rows.
          |This operator uses lazy evaluation and has a lower memory pressure in the system than the `Sort` operator.
          |Partial sort is only applicable when sorting on multiple columns.
        """.stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name STARTS WITH 'P' RETURN p ORDER BY p.name, p.age""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("PartialSort"))
      )
  }

  @Test def top() {
    profileQuery(
      title = "Top",
      text =
        """The `Top` operator returns the first 'n' rows sorted by a provided key. Instead of sorting the entire input, only the top 'n' rows are retained.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p ORDER BY p.name LIMIT 2""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Top"))
    )
  }

  @Test def partialTop() {
    profileQuery(
      title = "Partial Top",
      text =
        """The `PartialTop` operator is an optimization of the `Top` operator that takes advantage of the ordering of the incoming rows.
          |This operator uses lazy evaluation and has a lower memory pressure in the system than the `Top` operator.
          |Partial top is only applicable when sorting on multiple columns.
        """.stripMargin,
      queryText = """MATCH (p:Person) WHERE p.name STARTS WITH 'P' RETURN p ORDER BY p.name, p.age LIMIT 2""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("PartialTop"))
      )
  }

  @Test def limit() {
    profileQuery(
      title = "Limit",
      text =
        """The `Limit` operator returns the first 'n' rows from the incoming input.""".stripMargin,
      queryText = """MATCH (p:Person) RETURN p LIMIT 3""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Limit"))
    )
  }

  @Test def exhaustiveLimit() {
    profileQuery(
      title = "Exhaustive Limit",
      text =
        """The `ExhaustiveLimit` operator is just like a normal `Limit` but will always exhaust the input.
          |Used when combining `LIMIT` and updates""".stripMargin,
      queryText = """MATCH (p:Person) SET p.seen=true RETURN p LIMIT 3""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("ExhaustiveLimit"))
    )
  }

  @Test def lockingMerge() {
    profileQuery(
      title = "Locking Merge",
      text =
        """The `LockingMerge` operator is just like a normal `Merge` but will lock the start and end node when creating a relationship if necessary.""".stripMargin,
      queryText = """MATCH (s:Person {name: 'me'}) MERGE (s)-[:FRIENDS_WITH]->(s)""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LockingMerge"))
    )
  }

  @Test def optional() {
    profileQuery(
      title = "Optional",
      text =
        """The `Optional` operator is used to solve some <<query-optional-match, OPTIONAL MATCH>> queries.
          |It will pull data from its source, simply passing it through if any data exists.
          |However, if no data is returned by its source, `Optional` will yield a single row with all columns set to `null`.""".stripMargin,
      queryText = """MATCH (p:Person {name:'me'}) OPTIONAL MATCH (q:Person {name: 'Lulu'}) RETURN p, q""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Optional"))
    )
  }

  @Test def projectEndpoints() {
    profileQuery(
      title = "Project Endpoints",
      text =
        """The `ProjectEndpoints` operator projects the start and end node of a relationship.""".stripMargin,
      queryText = """CREATE (n)-[p:KNOWS]->(m) WITH p AS r MATCH (u)-[r]->(v) RETURN u, v""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("ProjectEndpoints"))
    )
  }

  @Test def expandAll() {
    profileQuery(
      title = "Expand All",
      text =
        """Given a start node, and depending on the pattern relationship, the `Expand(All)` operator will traverse incoming or outgoing relationships.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof) RETURN fof""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Expand(All)"))
    )
  }

  @Test def expandInto() {
    profileQuery(
      title = "Expand Into",
      text =
        """When both the start and end node have already been found, the `Expand(Into)` operator is used to find all relationships connecting the two nodes.
          |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
          |This can make a noticeable difference when dense nodes appear as end points.""".stripMargin,
      queryText = """MATCH (p:Person {name: 'me'})-[:FRIENDS_WITH]->(fof)-->(p) RETURN fof""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Expand(Into)"))
    )
  }


  @Test def optionalExpandInto() {
    profileQuery(
      title = "Optional Expand Into",
      text =
        """The `OptionalExpand(Into)` operator is analogous to <<query-plan-expand-into, Expand(Into)>>, apart from when no matching relationships are found.
          |In this situation, `OptionalExpand(Into)` will return a single row with the relationship and end node set to `null`.
          |As both the start and end node of the relationship are already in scope, the node with the smallest degree will be used.
          |This can make a noticeable difference when dense nodes appear as end points.""".stripMargin,
      queryText = """MATCH (p:Person)-[works_in:WORKS_IN]->(l) OPTIONAL MATCH (l)-->(p) RETURN p""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("OptionalExpand(Into)"))
    )
  }

  @Test def varlengthExpandAll() {
    profileQuery(
      title = "VarLength Expand All",
      text =
        """Given a start node, the `VarLengthExpand(All)` operator will traverse variable-length relationships.""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(q:Person) RETURN p, q""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(All)"))
    )
  }

  @Test def varlengthExpandInto() {
    profileQuery(
      title = "VarLength Expand Into",
      text =
        """When both the start and end node have already been found, the `VarLengthExpand(Into)` operator is used to find all variable-length relationships connecting the two nodes.""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *1..2]-(p:Person) RETURN p""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(Into)"))
    )
  }

  @Test def varlengthExpandPruning() {
    profileQuery(
      title = "VarLength Expand Pruning",
      text =
        """Given a start node, the `VarLengthExpand(Pruning)` operator will traverse variable-length relationships much like the <<query-plan-varlength-expand-all, `VarLengthExpand(All)`>> operator.
          |However, as an optimization, some paths will not be explored if they are guaranteed to produce an end node that has already been found (by means of a previous path traversal).
          |This will only be used in cases where the individual paths are not of interest.
          |This operator guarantees that all the end nodes produced will be unique.""".stripMargin,
      queryText = """MATCH (p:Person)-[:FRIENDS_WITH *3..4]-(q:Person) RETURN DISTINCT p, q""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("VarLengthExpand(Pruning)"))
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
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("DirectedRelationshipByIdSeek"))
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
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("UndirectedRelationshipByIdSeek"))
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
        "The `Union` operator concatenates the results from the right child operator with the results from the left child operator.",
      queryText =
        """MATCH (p:Location)
           RETURN p.name
           UNION ALL
           MATCH (p:Country)
           RETURN p.name
        """.stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Union"))
    )
  }

  @Test def unwind() {
    profileQuery(
      title = "Unwind",
      text =
        """The `Unwind` operator returns one row per item in a list.""".stripMargin,
      queryText = """UNWIND range(1, 5) as value return value""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Unwind"))
    )
  }

  @Test def apply() {
    profileQuery(
      title = "Apply",
      text =
        """
          |All the different `Apply` operators (listed below) share the same basic functionality: they perform a nested loop by taking a single row from the left-hand side, and using the <<query-plan-argument, Argument>> operator on the right-hand side, execute the operator tree on the right-hand side.
          |The versions of the `Apply` operators differ in how the results are managed.
          |The `Apply` operator (i.e. the standard version) takes the row produced by the right-hand side -- which at this point contains data from both the left-hand and right-hand sides -- and yields it..""".stripMargin,
      queryText =
        """MATCH (p:Person {name:'me'})
          |MATCH (q:Person {name: p.secondName})
          |RETURN p, q""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Apply"))
    )
  }

  @Test def semiApply() {
    profileQuery(
      title = "Semi Apply",
      text =
        """The `SemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
          |If the right-hand side operator yields at least one row, the row from the left-hand side operator is yielded by the `SemiApply` operator.
          |This makes `SemiApply` a filtering operator, used mostly for pattern predicates in queries.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (p:Person)
          |WHERE (p)-[:FRIENDS_WITH]->(:Person)
          |RETURN p.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SemiApply"))
    )
  }

  @Test def antiSemiApply() {
    profileQuery(
      title = "Anti Semi Apply",
      text =
        """The `AntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
          |If the right-hand side operator yields no rows, the row from the left-hand side operator is yielded by the `AntiSemiApply` operator.
          |This makes `AntiSemiApply` a filtering operator, used for pattern predicates in queries.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (me:Person {name: "me"}), (other:Person)
          |WHERE NOT (me)-[:FRIENDS_WITH]->(other)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("AntiSemiApply"))
    )
  }

  @Test def anti() {
    profileQuery(
      title = "Anti",
      text =
        """The `Anti` operator tests for the absence of a pattern.
          |If there are incoming rows, the `Anti` operator will yield no rows.
          |If there are no incoming rows, the `Anti` operator will yield a single row.
          |""".stripMargin,
      queryText =
        """CYPHER runtime=pipelined
          |MATCH (me:Person {name: "me"}), (other:Person)
          |WHERE NOT (me)-[:FRIENDS_WITH]->(other)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Anti"))
    )
  }

  @Test def letSemiApply() {
    profileQuery(
      title = "Let Semi Apply",
      text =
        """The `LetSemiApply` operator tests for the presence of a pattern predicate, and is a variation of the <<query-plan-apply, Apply>> operator.
          |When a query contains multiple pattern predicates separated with `OR`, `LetSemiApply` will be used to evaluate the first of these.
          |It will record the result of evaluating the predicate but will leave any filtering to another operator.
          |In the example, `LetSemiApply` will be used to check for the presence of the `FRIENDS_WITH`
          |relationship from each person.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (other:Person)
          |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LetSemiApply"))
    )
  }

  @Test def letAntiSemiApply() {
    profileQuery(
      title = "Let Anti Semi Apply",
      text =
        """The `LetAntiSemiApply` operator tests for the absence of a pattern, and is a variation of the <<query-plan-apply, Apply>> operator.
          |When a query contains multiple negated pattern predicates -- i.e. predicates separated with `OR`, where at
          |least one predicate contains `NOT` -- `LetAntiSemiApply` will be used to evaluate the first of these.
          |It will record the result of evaluating the predicate but will leave any filtering to another operator.
          |In the example, `LetAntiSemiApply` will be used to check for the absence of
          |the `FRIENDS_WITH` relationship from each person.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (other:Person)
          |WHERE NOT ((other)-[:FRIENDS_WITH]->(:Person)) OR (other)-[:WORKS_IN]->(:Location)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LetAntiSemiApply"))
    )
  }

  @Test def selectOrSemiApply() {
    profileQuery(
      title = "Select Or Semi Apply",
      text =
        """The `SelectOrSemiApply` operator tests for the presence of a pattern predicate and evaluates a predicate,
          |and is a variation of the <<query-plan-apply, Apply>> operator.
          |This operator allows for the mixing of normal predicates and pattern predicates
          |that check for the presence of a pattern.
          |First, the normal expression predicate is evaluated, and, only if it returns `false`, is the costly pattern predicate evaluated.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE other.age > 25 OR (other)-[:FRIENDS_WITH]->(:Person)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SelectOrSemiApply"))
    )
  }

  @Test def selectOrAntiSemiApply() {
    profileQuery(
      title = "Select Or Anti Semi Apply",
      text =
        """The `SelectOrAntiSemiApply` operator is used to evaluate `OR` between a predicate and a negative pattern predicate
          |(i.e. a pattern predicate preceded with `NOT`), and is a variation of the <<query-plan-apply, Apply>> operator.
          |If the predicate returns `true`, the pattern predicate is not tested.
          |If the predicate returns `false` or `null`, `SelectOrAntiSemiApply` will instead test the pattern predicate.""".stripMargin,
      queryText =
        """MATCH (other:Person)
          |WHERE other.age > 25 OR NOT (other)-[:FRIENDS_WITH]->(:Person)
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("SelectOrAntiSemiApply"))
    )
  }

  @Test def merge() {
    profileQuery(
      title = "Merge",
      text =
        """The `Merge` operator will either read or create nodes and/or relationships.
           |
           |If matches are found it will execute the provided `ON MATCH` operations foreach incoming row.
           |If no matches are found instead nodes and relationships are created and all `ON CREATE` operations are run.
        """.stripMargin,
      queryText =
        """MERGE (p:Person {name: 'Andy'})
          |ON MATCH SET p.existed = true
          |ON CREATE SET p.existed = false""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Merge"))
    )
  }

  @Test def assertingMultiNodeIndexSeek() {
    profileQuery(
      title = "Asserting Multi Node Index Seek",
      text =
        """The `AssertingMultiNodeIndexSeek` operator is used to ensure that no unique constraints are violated.
          |The example looks for the presence of a team with the supplied name and id, and if one does not exist,
          |it will be created. Owing to the existence of two unique constraints
          |on `:Team(name)` and `:Team(id)`, any node that would be found by the `UniqueIndexSeek`
          |must be the very same node, or the constraints would be violated.
        """.stripMargin,
      queryText =
        """MERGE (t:Team {name: 'Engineering', id: 42})""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("AssertingMultiNodeIndexSeek"))
    )
  }

  @Test def assertSameNode() {
    profileQuery(
      title = "Assert Same Node",
      text =
        """The `AssertSameNode` operator is used to ensure that no unique constraints are violated in the slotted and interpreted runtime.
          |The example looks for the presence of a team with the supplied name and id, and if one does not exist,
          |it will be created. Owing to the existence of two unique constraints
          |on `:Team(name)` and `:Team(id)`, any node that would be found by the `UniqueIndexSeek`
          |must be the very same node, or the constraints would be violated.
        """.stripMargin,
      queryText =
        """CYPHER runtime=slotted MERGE (t:Team {name: 'Engineering', id: 42})""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("AssertSameNode"))
    )
  }

  @Test def nodeHashJoin() {
    executePreparationQueries(
      List(
        """MATCH (london:Location {name: 'London'}), (person:Person {name: 'Pontus'})
          FOREACH(x in range(0, 250) |
            CREATE (person) -[: WORKS_IN] ->(london)
            )""".stripMargin
      )
    )
    profileQuery(
      title = "Node Hash Join",
      text =
        """
          |The `NodeHashJoin` operator is a variation of the <<execution-plans-operators-hash-join-general, hash join>>.
          |`NodeHashJoin` executes the hash join on node ids.
          |As primitive types and arrays can be used, it can be done very efficiently.""".stripMargin,
      queryText =
        """MATCH (bob:Person {name:'Bob'})-[:WORKS_IN]->(loc)<-[:WORKS_IN]-(matt:Person {name:'Mattis'})
          |RETURN loc.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeHashJoin"))
    )
  }

  @Test def nodeOuterHashJoin() {
    profileQuery(
      title = "Node Left/Right Outer Hash Join",
      text =
        """
          |The `NodeLeftOuterHashJoin` and `NodeRightOuterHashJoin` operators are variations of the <<execution-plans-operators-hash-join-general, hash join>>.
          |The query below can be planned with either a left or a right outer join.
          |The decision depends on the cardinalities of the left-hand and right-hand sides; i.e. how many rows would be returned, respectively, for `(a:Person)` and `(a)-->(b:Person)`.
          |If `(a:Person)` returns fewer results than `(a)-->(b:Person)`, a left outer join -- indicated by `NodeLeftOuterHashJoin` -- is planned.
          |On the other hand, if `(a:Person)` returns more results than `(a)-->(b:Person)`, a right outer join -- indicated by `NodeRightOuterHashJoin` -- is planned instead.""".stripMargin,
      queryText =
        """MATCH (a:Person)
          |OPTIONAL MATCH (a)-->(b:Person)
          |USING JOIN ON a
          |RETURN a.name, b.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("NodeRightOuterHashJoin"))
    )
  }

  private val triadicSelectionQuery =
    """MATCH (me:Person)-[:FRIENDS_WITH]-()-[:FRIENDS_WITH]-(other)
      |WHERE NOT (me)-[:FRIENDS_WITH]-(other)
      |RETURN other.name""".stripMargin

  @Test def triadicSelection() {
    profileQuery(
      title = "Triadic Selection",
      text =
        """The `TriadicSelection` operator is used to solve triangular queries, such as the very
          |common 'find my friend-of-friends that are not already my friend'.
          |It does so by putting all the friends into a set, and uses the set to check if the
          |friend-of-friends are already connected to me.
          |The example finds the names of all friends of my friends that are not already my friends.""".stripMargin,
      queryText =
        "CYPHER runtime=slotted " + triadicSelectionQuery,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("TriadicSelection"))
    )
  }

  @Test def triadicBuild() {
    profileQuery(
      title = "Triadic Build",
      text =
        """The `TriadicBuild` operator is used in conjunction with `TriadicFilter` to solve triangular queries, such as the very
          |common 'find my friend-of-friends that are not already my friend'. These two operators are specific to Pipelined
          |runtime and together perform the same logic as `TriadicSelection` does for other runtimes.
          |`TriadicBuild` builds a set of all friends, which is later used by `TriadicFilter`.
          |The example finds the names of all friends of my friends that are not already my friends.""".stripMargin,
      queryText = "CYPHER runtime=pipelined " + triadicSelectionQuery,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("TriadicBuild"))
    )
  }

  @Test def triadicFilter() {
    profileQuery(
      title = "Triadic Filter",
      text =
        """The `TriadicFilter` operator is used in conjunction with `TriadicBuild` to solve triangular queries, such as the very
          |common 'find my friend-of-friends that are not already my friend'. These two operators are specific to Pipelined
          |runtime and together perform the same logic as `TriadicSelection` does for other runtimes.
          |`TriadicFilter` uses a set of friends previously built by `TriadicBuild` to check if the friend-of-friends are already connected to me.
          |The example finds the names of all friends of my friends that are not already my friends.""".stripMargin,
      queryText = "CYPHER runtime=pipelined " + triadicSelectionQuery,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("TriadicFilter"))
    )
  }

  @Test def foreach() {
    profileQuery(
      title = "Foreach",
      text =
        """The `Foreach` operator executes a nested loop between the left child operator and the right child operator.
          | In an analogous manner to the <<query-plan-apply, Apply>> operator, it takes a row from the left-hand side and, using the <<query-plan-argument, Argument>> operator, provides it to the operator tree on the right-hand side.
          | `Foreach` will yield all the rows coming in from the left-hand side; all results from the right-hand side are pulled in and discarded.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted FOREACH (value IN [1,2,3] |
          |CREATE (:Person {age: value})
          |)""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("Foreach"))
    )
  }

  @Test def emptyRow() {
    profileQuery(
      title = "Empty Row",
      text =
        """The `EmptyRow` operator returns a single row with no columns.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted FOREACH (value IN [1,2,3] |
          |MERGE (:Person {age: value})
          |)""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("EmptyRow"))
    )
  }

  @Test def letSelectOrSemiApply() {
    profileQuery(
      title = "Let Select Or Semi Apply",
      text =
        """The `LetSelectOrSemiApply` operator is planned for pattern predicates that are combined with other predicates using `OR`.
          |This is a variation of the <<query-plan-apply, Apply>> operator.
        """.stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (other:Person)
          |WHERE (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LetSelectOrSemiApply"))
    )
  }

  @Test def letSelectOrAntiSemiApply() {
    profileQuery(
      title = "Let Select Or Anti Semi Apply",
      text =
        """The `LetSelectOrAntiSemiApply` operator is planned for negated pattern predicates -- i.e. pattern predicates
          |preceded with `NOT` -- that are combined with other predicates using `OR`.
          |This operator is a variation of the <<query-plan-apply, Apply>> operator.
        """.stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (other:Person)
          |WHERE NOT (other)-[:FRIENDS_WITH]->(:Person) OR (other)-[:WORKS_IN]->(:Location) OR other.age = 5
          |RETURN other.name""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("LetSelectOrAntiSemiApply"))
    )
  }

  @Test def rollUpApply() {
    profileQuery(
      title = "Roll Up Apply",
      text =
        """The `RollUpApply` operator is used to execute an expression which takes as input a pattern, and returns a list with content from the matched pattern;
          |for example, when using a pattern expression or pattern comprehension in a query.
          |This operator is a variation of the <<query-plan-apply, Apply>> operator.""".stripMargin,
      queryText =
        """CYPHER runtime=slotted
          |MATCH (p:Person)
          |RETURN p.name, [ (p)-[:WORKS_IN]->(location) | location.name ] AS cities""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("RollUpApply"))
    )
  }

  @Test def valueHashJoin() {
    profileQuery(
      title = "Value Hash Join",
      text =
        """The `ValueHashJoin` operator is a variation of the <<execution-plans-operators-hash-join-general, hash join>>.
           This operator allows for arbitrary values to be used as the join key.
           It is most frequently used to solve predicates of the form: `n.prop1 = m.prop2` (i.e. equality predicates between two property columns).
        """.stripMargin,
      queryText =
        """MATCH (p:Person),(q:Person)
          |WHERE p.age = q.age
          |RETURN p,q""".stripMargin,
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("ValueHashJoin"))
    )
  }

  @Test def call() {
    profileQuery(
      title = "Procedure Call",
      text = """The `ProcedureCall` operator indicates an invocation to a procedure.""".stripMargin,
      queryText = """CALL db.labels() YIELD label RETURN * ORDER BY label""",
      assertions = p => assertThat(p.executionPlanDescription().toString, containsString("ProcedureCall"))
    )
  }

  @Test def cacheProperties() {
    profileQuery(
      title = "Cache Properties",
      text =
        """The `CacheProperties` operator reads nodes and relationship properties and caches them in the current row.
        |Future accesses to these properties can avoid reading from the store which will speed up the query.
        |In the plan below we will cache `l.name` before `Expand(All)` where there are fewer rows.
      """.stripMargin,
      queryText = """MATCH (l:Location)<-[:WORKS_IN]-(p:Person) RETURN l.name AS location, p.name AS name""",
      assertions = p => {
        assertThat(p.executionPlanDescription().toString, containsString("CacheProperties"))
        assertThat(p.executionPlanDescription().toString, containsString("Expand(All)"))
      }
      )
  }

  @Test def shortestPath() {
    profileQuery(
      title = "Shortest path",
      text =
        """The `ShortestPath` operator finds one or all shortest paths between two previously matches node variables.
      """.stripMargin,
      queryText =
        """MATCH (andy:Person {name: 'Andy'}),(mattias:Person {name: 'Mattias'}), p = shortestPath((andy)-[*]-(mattias))
          |RETURN p""".stripMargin,
      assertions = p => {
        assertThat(p.executionPlanDescription().toString, containsString("ShortestPath"))
      }
    )
  }
}
