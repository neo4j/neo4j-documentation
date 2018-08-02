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
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.internal.InternalExecutionResult
import org.neo4j.cypher.internal.compatibility.v3_3.runtime.pipes.IndexSeekByRange
import org.neo4j.cypher.internal.compatibility.v3_3.runtime.planDescription.InternalPlanDescription.Arguments.Planner
import org.neo4j.cypher.internal.compiler.v3_3.{DPPlannerName, IDPPlannerName}
import org.neo4j.cypher.internal.helpers.GraphIcing

class SchemaIndexTest extends DocumentingTestBase with QueryStatisticsTestSupport with GraphIcing {

  //need a couple of 'Person' to make index operations more efficient than label scans
  override val setupQueries = (1 to 20 map (_ => """CREATE (:Person)""")).toList

  override def graphDescription = List(
    "andres:Person KNOWS mark:Person"
  )

  override val properties = Map(
    "andres" -> Map("firstname" -> "Andres", "surname" -> "Taylor", "age" -> 40, "country" -> "Sweden"),
    "mark" -> Map("firstname" -> "Mark", "surname" -> "Needham", "age" -> 35, "country" -> "UK")
  )

  override val setupConstraintQueries = List(
    "CREATE INDEX ON :Person(firstname)"
  )

  def section = "Schema Index"

  @Test def create_index_on_a_label_single_property() {
    testQuery(
      title = "Create a single-property index",
      text = "An index on a single property for all nodes that have a particular label can be created with `CREATE INDEX ON :Label(property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("firstname")))
    )
  }

  @Test def create_index_on_a_label_composite_property() {
    testQuery(
      title = "Create a composite index",
      text = "An index on multiple properties for all nodes that have a particular label -- i.e. a composite index -- can be created with `CREATE INDEX ON :Label(prop1, ..., propN)`. " +
      "Only nodes labeled with the specified label and which contain all the properties in the index definition will be added to the index. " +
      "The following statement will create a composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      queryText = "CREATE INDEX ON :Person(age, country)",
      optionalResultExplanation = "Assume we execute the query `CREATE (a:Person {firstname: 'Bill', age: 34, country: 'USA'}), (b:Person {firstname: 'Sue', age: 39})`. " +
        "Node `a` has both an `age` and a `country` property, and so it will be added to the composite index. " +
        "However, as node `b` has no `country` property, it will not be added to the composite index. " +
        "Note that the composite index is not immediately available, but will be created in the background. ",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("firstname"), List("age", "country")))
    )
  }

  @Test def get_all_indexes() {
    prepareAndTestQuery(
      title = "Get a list of all indexes in the database",
      text = "Calling the built-in procedure `db.indexes` will list all the indexes in the database.",
      prepare = _ => executePreparationQueries(List("create index on :Person(firstname)")),
      queryText = "CALL db.indexes",
      optionalResultExplanation = "",
      assertions = (p) => assertEquals(1, p.size)
    )
  }

  @Test def drop_index_on_a_label_single_property() {
    prepareAndTestQuery(
      title = "Drop a single-property index",
      text = "An index on all nodes that have a label and single property combination can be dropped with `DROP INDEX ON :Label(property)`.",
      prepare = _ => executePreparationQueries(List("create index on :Person(firstname)")),
      queryText = "DROP INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List())
    )
  }

  @Test def drop_index_on_a_label_composite_property() {
    prepareAndTestQuery(
      title = "Drop a composite index",
      text = "A composite index on all nodes that have a label and multiple property combination can be dropped with `DROP INDEX ON :Label(prop1, ..., propN)`. " +
      "The following statement will drop a composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      prepare = _ => executePreparationQueries(List("create index on :Person(age, country)")),
      queryText = "DROP INDEX ON :Person(age, country)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("firstname")))
    )
  }

  @Test def use_index() {
    profileQuery(
      title = "Use index",
      text = "There is usually no need to specify which indexes to use in a query, Cypher will figure that out by itself. " +
        "For example the query below will use the `Person(firstname)` index, if it exists. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person {firstname: 'Andres'}) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_equality_single_property() {
    profileQuery(
      title = "Equality check using `WHERE` (single-property index)",
      text = "A query containing equality comparisons of a single indexed property in the `WHERE` clause is backed automatically by the index. " +
        "It is also possible for a query with multiple `OR` predicates to use multiple indexes, if indexes exist on the properties. " +
        "For example, if indexes exist on both `:Label(p1)` and `:Label(p2)`, `MATCH (n:Label) WHERE n.p1 = 1 OR n.p2 = 2 RETURN n` will use both indexes. "  +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname = 'Andres' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_equality_composite() {
    prepareAndTestQuery(
      title = "Equality check using `WHERE` (composite index)",
      text = "A query containing equality comparisons for all the properties of a composite index will automatically be backed by the same index. " +
        "The following query will use the composite index defined <<schema-index-create-a-composite-index, earlier>>: ",
      prepare = _ => executePreparationQueries(List("CREATE INDEX ON :Person(age, country)")),
      queryText = "MATCH (n:Person) WHERE n.age = 35 AND n.country = 'UK' RETURN n",
      optionalResultExplanation = "However, the query `MATCH (n:Person) WHERE n.age = 35 RETURN n` will not be backed by the composite index, as the query does not contain an equality predicate on the `country` property. " +
      "It will only be backed by an index on the `Person` label and `age` property defined thus: `:Person(age)`; i.e. a single-property index. " +
      "Moreover, unlike single-property indexes, composite indexes currently do not support queries containing the following types of predicates on properties in the index: " +
      "existence check: `exists(n.prop)`; range search: `n.prop > value`; prefix search: `STARTS WITH`; suffix search: `ENDS WITH`; and substring search: `CONTAINS`. " +
      "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_range_comparisons() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Range comparisons using `WHERE` (single-property index)",
      text = "Single-property indexes are also automatically used for inequality (range) comparisons of an indexed property in the `WHERE` clause. " +
        "Composite indexes are currently not able to support range comparisons. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname > 'B' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_in() {

    executePreparationQueries(
      List(
        "FOREACH (x IN range(0, 100) | CREATE (:Person) )",
        "FOREACH (x IN range(0, 400) | CREATE (:Person {firstname: x}) )"
      )
    )

    sampleAllIndexesAndWait()

    profileQuery(
      title = "List membership check using `IN` (single-property index)",
      text =
        "The `IN` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname IN ['Andres', 'Mark'] RETURN person",
      assertions = {
        (p) =>
          assertEquals(2, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_starts_with() {
    executePreparationQueries {
      val a = (0 to 100).map { i => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {firstname: '$i'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Prefix search using `STARTS WITH` (single-property index)",
      text =
        "The `STARTS WITH` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists. " +
          "Composite indexes are currently not able to support `STARTS WITH`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname STARTS WITH 'And' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString(IndexSeekByRange.name))
      }
    )
  }

  @Test def use_index_with_ends_with() {
    executePreparationQueries {
      val a = (0 to 100).map { i => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {firstname: '$i'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Suffix search using `ENDS WITH` (single-property index)",
      text =
        "The `ENDS WITH` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists. " +
          "All values stored in the `Person(firstname)` index will be searched, and entries ending with `'rk'` will be returned. " +
          "This means that although the search will not be optimized to the extent of queries using `=`, `IN`, `>`, `<` or `STARTS WITH`, it is still faster than not using an index in the first place. " +
          "Composite indexes are currently not able to support `ENDS WITH`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname ENDS WITH 'rk' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexEndsWithScan"))
      }
    )
  }

  @Test def use_index_with_contains() {
    executePreparationQueries {
      val a = (0 to 100).map { i => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {firstname: '$i'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Substring search using `CONTAINS` (single-property index)",
      text =
        "The `CONTAINS` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists. " +
          "All values stored in the `Person(firstname)` index will be searched, and entries containing `'r'` will be returned. " +
          "This means that although the search will not be optimized to the extent of queries using `=`, `IN`, `>`, `<` or `STARTS WITH`, it is still faster than not using an index in the first place. " +
          "Composite indexes are currently not able to support `CONTAINS`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname CONTAINS 'r' RETURN person",
      assertions = {
        (p) =>
          assertEquals(2, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexContainsScan"))
      }
    )
  }

  @Test def use_index_with_exists_property() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Existence check using `exists` (single-property index)",
      text =
        "The `exists(p.firstname)` predicate in the following query will use the `Person(firstname)` index, if it exists. " +
          "Composite indexes are currently not able to support the `exists` predicate. ",
      queryText = "MATCH (p:Person) WHERE exists(p.firstname) RETURN p",
      assertions = {
        (p) =>
          assertEquals(2, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
      }
    )
  }

  def assertIndexesOnLabels(label: String, expectedIndexes: List[List[String]]) {
    assert(expectedIndexes === db.indexPropsForLabel(label))
  }

  private def checkPlanDescription(result: InternalExecutionResult)(costString: String): Unit = {
    val planDescription = result.executionPlanDescription()
    val plannerArgument = planDescription.arguments.find(a => a.name == "planner")

    plannerArgument match {
      case Some(Planner(IDPPlannerName.name)) =>
        assertThat(planDescription.toString, containsString(costString))
      case Some(Planner(DPPlannerName.name)) =>
        assertThat(planDescription.toString, containsString(costString))
      case Some(Planner(name)) if name.equals("COST") =>
        assertThat(planDescription.toString, containsString(costString))

      case x =>
        fail(s"Couldn't determine used planner: $x")
    }
  }
}
