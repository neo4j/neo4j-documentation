/*
 * Copyright (c) 2002-2018 "Neo4j,"
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

import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.internal.planner.v3_4.spi.{DPPlannerName, IDPPlannerName}
import org.neo4j.cypher.internal.runtime.InternalExecutionResult
import org.neo4j.cypher.internal.runtime.interpreted.pipes.IndexSeekByRange
import org.neo4j.cypher.internal.runtime.planDescription.InternalPlanDescription.Arguments.Planner
import org.neo4j.cypher.{GraphIcing, QueryStatisticsTestSupport}

class SchemaIndexTest extends DocumentingTestBase with QueryStatisticsTestSupport with GraphIcing {

  //need a couple of 'Person' to make index operations more efficient than label scans
  override val setupQueries = (1 to 20 map (_ => """CREATE (:Person)""")).toList

  override def graphDescription = List(
    "andres:Person KNOWS mark:Person"
  )

  override val properties = Map(
    "andres" -> Map("firstname" -> "Andres", "surname" -> "Taylor", "highScore" -> 12345),
    "mark" -> Map("firstname" -> "Mark", "surname" -> "Needham", "highScore" -> 54321)
  )

  override val setupConstraintQueries = List(
    "CREATE INDEX ON :Person(firstname)",
    "CREATE INDEX ON :Person(location)",
    "CREATE INDEX ON :Person(highScore)"
  )

  def section = "Schema Index"

  @Test def create_index_on_a_label_single_property() {
    testQuery(
      title = "Create a single-property index",
      text = "An index on a single property for all nodes that have a particular label can be created with `CREATE INDEX ON :Label(property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("location"), List("firstname"), List("highScore")))
    )
  }

  @Test def get_all_indexes() {
    prepareAndTestQuery(
      title = "Get a list of all indexes in the database",
      text = "Calling the built-in procedure `db.indexes` will list all the indexes in the database.",
      prepare = _ => executePreparationQueries(List("create index on :Person(firstname)")),
      queryText = "CALL db.indexes",
      optionalResultExplanation = "",
      assertions = (p) => assertEquals(3, p.size)
    )
  }

  @Test def drop_index_on_a_label_single_property() {
    prepareAndTestQuery(
      title = "Drop a single-property index",
      text = "An index on all nodes that have a label and single property combination can be dropped with `DROP INDEX ON :Label(property)`.",
      prepare = _ => executePreparationQueries(List("create index on :Person(firstname)")),
      queryText = "DROP INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("location"), List("highScore")))
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
      title = "Use a single-property index with `WHERE` using equality",
      text = "A query containing equality comparisons of a single indexed property in the `WHERE` clause is backed automatically by the index. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname = 'Andres' RETURN person",
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
      title = "Use index with `WHERE` using range comparisons",
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

  @Test def use_index_with_where_using_multiple_range_comparisons() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Use index with `WHERE` using multiple range comparisons",
      text = "When the `WHERE` clause contains multiple inequality (range) comparisons for the same property these can be combined " +
        "in a single index range seek. " +
        "If you want Cypher to use specific indexes, you can enforce it using hints. See <<query-using>>.",
      queryText = "MATCH (person:Person) WHERE 10000 < person.highScore < 20000 RETURN person",
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
      title = "Use index with `IN`",
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
      title = "Use index with `STARTS WITH`",
      text =
        "The `STARTS WITH` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists. " +
          "Composite indexes are currently not able to support `STARTS WITH`, `ENDS WITH` and `CONTAINS`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname STARTS WITH 'And' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString(IndexSeekByRange.name))
      }
    )
  }

  @Test def use_index_with_exists_property() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Use index when checking for the existence of a property",
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

  @Test def use_index_with_distance_query() {
    executePreparationQueries(
      (for(x <- -10 to 10; y <- -10 to 10) yield s"CREATE (:Person {location: point({x:$x, y:$y}) } )").toList)
    profileQuery(
      title = "Use index when executing a spatial distance search",
      text =
        "If a property with point values is indexed, the index is used for spatial distance searches as well as for range queries.",
      queryText = "MATCH (p:Person) WHERE distance(p.location, point({x: 1, y: 2})) < 2 RETURN p.location",
      assertions = {
        (p) =>
          assertEquals(9, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeekByRange"))
      }
    )
  }

  @Test def use_index_with_bbox_query() {
    executePreparationQueries(
      (for(x <- -10 to 10; y <- -10 to 10) yield s"CREATE (:Person {location: point({x:$x, y:$y}) } )").toList ++ List(
        "MATCH (n:Person {firstname: 'Andres'}) SET n.location = point({x: 1.2345, y: 5.4321})",
        "MATCH (n:Person {firstname: 'Mark'}) SET n.location = point({y: 1.2345, x: 5.4321})"
      )
    )
    profileQuery(
      title = "Use index when executing a spatial bounding box search",
      text = "The ability to do index seeks on bounded ranges works even with the 2D and 3D spatial `Point` types.",
      queryText = "MATCH (person:Person) WHERE point({x: 1, y: 5}) < person.location < point({x: 2, y: 6}) RETURN person",
      assertions = {
        (p) =>
          // TODO: There is a bug in 3.4 spatial where the two other corners are incorrectly matched
          assertEquals(3, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  def assertIndexesOnLabels(label: String, expectedIndexes: List[List[String]]) {
    assert(db.indexPropsForLabel(label).toSet === expectedIndexes.toSet)
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
