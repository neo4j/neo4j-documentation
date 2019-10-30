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

import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.internal.plandescription.Arguments.Planner
import org.neo4j.cypher.internal.planner.spi.{DPPlannerName, IDPPlannerName}
import org.neo4j.cypher.internal.runtime.interpreted.pipes.IndexSeekByRange
import org.neo4j.cypher.{GraphIcing, QueryStatisticsTestSupport}
import org.neo4j.graphdb.Label

import scala.collection.JavaConverters._

class SchemaIndexTest extends DocumentingTestBase with QueryStatisticsTestSupport with GraphIcing {

  //need a couple of 'Person' to make index operations more efficient than label scans
  override val setupQueries = (1 to 20 map (_ => """CREATE (:Person)""")).toList

  override def graphDescription = List(
    "andy:Person KNOWS john:Person"
  )

  override val properties = Map(
    "andy" -> Map("firstname" -> "Andy", "surname" -> "Jones", "age" -> 40, "country" -> "Sweden", "highScore" -> 12345),
    "john" -> Map("firstname" -> "John", "surname" -> "Smith", "age" -> 35, "country" -> "UK", "highScore" -> 54321)
  )

  override val setupConstraintQueries = List(
    "CREATE INDEX FOR (p:Person) ON (p.firstname)",
    "CREATE INDEX FOR (p:Person) ON (p.location)",
    "CREATE INDEX FOR (p:Person) ON (p.highScore)"
  )

  override def parent = Some("Administration")
  override def section = "Indexes"

  @Test def create_index_on_a_label_single_property() {
    testQuery(
      title = "Create a single-property index",
      text = "An index on a single property for all nodes that have a particular label can be created with `CREATE INDEX FOR (n:Label) ON (n.property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX FOR (p:Person) ON (p.surname)",
      optionalResultExplanation = "",
      assertions = p => assertIndexesOnLabels( "Person", List(List("location"), List("firstname"), List("surname"), List("highScore")))
    )
  }

  @Test def create_named_index_on_a_label_single_property() {
    testQuery(
      title = "Create a named single-property index",
      text = "A named index on a single property for all nodes that have a particular label can be created with `CREATE INDEX index_name FOR (n:Label) ON (n.property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX my_index FOR (n:Person) ON (n.surname)",
      optionalResultExplanation = "Note that the index name needs to be unique. ",
      assertions = _ => assertIndexWithNameExists("my_index", "Person", List("surname"))
    )
  }

  @Test def create_index_on_a_label_composite_property() {
    testQuery(
      title = "Create a composite index",
      text = "An index on multiple properties for all nodes that have a particular label -- i.e. a composite index -- can be created with `CREATE INDEX FOR (n:Label) ON (n.prop1, ..., n.propN)`. " +
      "Only nodes labeled with the specified label and which contain all the properties in the index definition will be added to the index. " +
      "The following statement will create a composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      queryText = "CREATE INDEX FOR (p:Person) ON (p.age, p.country)",
      optionalResultExplanation = "Assume we execute the query `CREATE (a:Person {firstname: 'Bill', age: 34, country: 'USA'}), (b:Person {firstname: 'Sue', age: 39})`. " +
        "Node `a` has both an `age` and a `country` property, and so it will be added to the composite index. " +
        "However, as node `b` has no `country` property, it will not be added to the composite index. " +
        "Note that the composite index is not immediately available, but will be created in the background. ",
      assertions = (p) => assertIndexesOnLabels( "Person", List(List("firstname"), List("location"), List("highScore"), List("age", "country"), List("location")))
    )
  }

  @Test def create_named_index_on_a_label_composite_property() {
    testQuery(
      title = "Create a named composite index",
      text = "A named index on multiple properties for all nodes that have a particular label -- i.e. a composite index -- can be created with " +
      "`CREATE INDEX index_name FOR (n:Label) ON (n.prop1, ..., n.propN)`. " +
      "Only nodes labeled with the specified label and which contain all the properties in the index definition will be added to the index. " +
      "Note that the composite index is not immediately available, but will be created in the background. " +
      "The following statement will create a named composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      queryText = "CREATE INDEX my_index FOR (n:Person) ON (n.age, n.country)",
      optionalResultExplanation = "Note that the index name needs to be unique. ",
      assertions = _ => assertIndexWithNameExists("my_index", "Person", List("age", "country"))
    )
  }

  @Test def get_all_indexes() {
    prepareAndTestQuery(
      title = "Get a list of all indexes in the database",
      text = "Calling the built-in procedure `db.indexes` will list all the indexes in the database, including their names.",
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.firstname)")),
      queryText = "CALL db.indexes",
      optionalResultExplanation = "",
      assertions = (p) => assertEquals(3, p.size)
    )
  }

  @Test def drop_index_on_a_label_single_property() {
    prepareAndTestQuery(
      title = "Drop a single-property index",
      text = "This is deprecated. An index on all nodes that have a label and single property combination can be dropped with `DROP INDEX ON :Label(property)`.",
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.firstname)")),
      queryText = "DROP INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("location"), List("highScore")))
    )
  }

  @Test def drop_index_on_a_label_composite_property() {
    prepareAndTestQuery(
      title = "Drop a composite index",
      text = "This is deprecated. A composite index on all nodes that have a label and multiple property combination can be dropped with `DROP INDEX ON :Label(prop1, ..., propN)`. " +
      "The following statement will drop a composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.age, p.country)")),
      queryText = "DROP INDEX ON :Person(age, country)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("firstname"), List("location"), List("highScore")))
    )
  }

  @Test def drop_named_index() {
    prepareAndTestQuery(
      title = "Drop an index by name",
      text = "An index on all nodes that have a label and property/properties combination can be dropped using the name with the `DROP INDEX index_name` command.",
      prepare = _ => executePreparationQueries(List("CREATE INDEX my_index FOR (n:Person) ON (n.surname)")),
      queryText = "DROP INDEX my_index",
      assertions = _ => assertIndexWithNameDoesNotExists("my_index")
    )
  }

  @Test def use_index() {
    profileQuery(
      title = "Using indexes",
      text = "There is usually no need to specify which indexes to use in a query, Cypher will figure that out by itself. " +
        "For example the query below will use the `Person(firstname)` index, if it exists. ",
      queryText = "MATCH (person:Person {firstname: 'Andy'}) RETURN person",
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
        "For example, if indexes exist on both `:Label(p1)` and `:Label(p2)`, `MATCH (n:Label) WHERE n.p1 = 1 OR n.p2 = 2 RETURN n` will use both indexes. ",
      queryText = "MATCH (person:Person) WHERE person.firstname = 'Andy' RETURN person",
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
        "However, the query does not need to have equality on all properties. It can have ranges and existence predicates as well. " +
        "But in these cases rewrites might happen depending on which properties have which predicates, " +
        "see <<administration-indexes-single-vs-composite-index, composite index limitations>>. " +
        "The following query will use the composite index defined <<administration-indexes-create-a-composite-index, earlier>>: ",
      prepare = _ => executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.age, p.country)")),
      queryText = "MATCH (n:Person) WHERE n.age = 35 AND n.country = 'UK' RETURN n",
      optionalResultExplanation = "However, the query `MATCH (n:Person) WHERE n.age = 35 RETURN n` will not be backed by the composite index, " +
        "as the query does not contain a predicate on the `country` property. " +
      "It will only be backed by an index on the `Person` label and `age` property defined thus: `:Person(age)`; i.e. a single-property index. ",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek(equality,equality)")
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
      text = "Single-property indexes are also automatically used for inequality (range) comparisons of an indexed property in the `WHERE` clause.",
      queryText = "MATCH (person:Person) WHERE person.firstname > 'B' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_range_comparisons_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.firstname, p.highScore)"))
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Range comparisons using `WHERE` (composite index)",
      text = "Composite indexes are also automatically used for inequality (range) comparisons of indexed properties in the `WHERE` clause. " +
        "Equality or list membership check predicates may precede the range predicate. " +
        "However, predicates after the range predicate may be rewritten as an existence check predicate and a filter " +
        "as described in <<administration-indexes-single-vs-composite-index, composite index limitations>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname > 'B' AND person.highScore > 10000 RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek(range,exists)")
      }
    )
  }

  @Test def use_index_with_where_using_multiple_range_comparisons() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Multiple range comparisons using `WHERE` (single-property index)",
      text = "When the `WHERE` clause contains multiple inequality (range) comparisons for the same property, these can be combined " +
        "in a single index range seek.",
      queryText = "MATCH (person:Person) WHERE 10000 < person.highScore < 20000 RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_where_using_multiple_range_comparisons_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.highScore, p.name)"))
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 300).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Multiple range comparisons using `WHERE` (composite index)",
      text = "When the `WHERE` clause contains multiple inequality (range) comparisons for the same property, these can be combined " +
        "in a single index range seek. " +
        "That single range seek created in the following query will then use the composite index `Person(highScore, name)` if it exists.",
      queryText = "MATCH (person:Person) WHERE 10000 < person.highScore < 20000 AND exists(person.name) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek(range,exists)")
      }
    )
  }

  @Test def use_single_property_index_with_in() {

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
        "The `IN` predicate on `person.firstname` in the following query will use the single-property index `Person(firstname)` if it exists. ",
      queryText = "MATCH (person:Person) WHERE person.firstname IN ['Andy', 'John'] RETURN person",
      assertions = {
        (p) =>
          assertEquals(2, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_composite_index_with_in() {

    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.age, p.country)"))

    executePreparationQueries(
      List(
        "FOREACH (x IN range(0, 100) | CREATE (:Person) )",
        "FOREACH (x IN range(0, 400) | CREATE (:Person {age: x, country: x}) )"
      )
    )

    sampleAllIndexesAndWait()

    profileQuery(
      title = "List membership check using `IN` (composite index)",
      text =
        "The `IN` predicates on `person.age` and `person.country` in the following query will use the composite index `Person(age, country)` if it exists. ",
      queryText = "MATCH (person:Person) WHERE person.age IN [10, 20, 35] AND person.country IN ['Sweden', 'USA', 'UK'] RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek(equality,equality)")
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
        "The `STARTS WITH` predicate on `person.firstname` in the following query will use the `Person(firstname)` index, if it exists.",
      queryText = "MATCH (person:Person) WHERE person.firstname STARTS WITH 'And' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString(IndexSeekByRange.name))
      }
    )
  }

  @Test def use_index_with_starts_with_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.firstname, p.surname)"))

    executePreparationQueries {
      val a = (0 to 100).map { i => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {firstname: '$i', surname: '${-i}'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Prefix search using `STARTS WITH` (composite index)",
      text =
        "The `STARTS WITH` predicate on `person.firstname` in the following query will use the `Person(firstname,surname)` index, if it exists. " +
        "Any (non-existence check) predicate on `person.surname` will be rewritten as existence check with a filter. " +
        "However, if the predicate on `person.firstname` is a equality check " +
        "then a `STARTS WITH` on `person.surname` would also use the index (without rewrites). " +
        "More information about how the rewriting works can be found in <<administration-indexes-single-vs-composite-index, composite index limitations>>.",
      queryText = "MATCH (person:Person) WHERE person.firstname STARTS WITH 'And' AND exists(person.surname) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeek(range,exists)"))
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
          "All values stored in the `Person(firstname)` index will be searched, and entries ending with `'hn'` will be returned. " +
          "This means that although the search will not be optimized to the extent of queries using `=`, `IN`, `>`, `<` or `STARTS WITH`, it is still faster than not using an index in the first place. " +
          "Composite indexes are currently not able to support `ENDS WITH`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname ENDS WITH 'hn' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexEndsWithScan"))
      }
    )
  }

  @Test def use_index_with_ends_with_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.surname, p.age)"))

    executePreparationQueries {
      val a = (0 to 100).map { _ => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {age: $i, surname: '${-i}'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Suffix search using `ENDS WITH` (composite index)",
      text =
        "The `ENDS WITH` predicate on `person.surname` in the following query will use the `Person(surname,age)` index, if it exists. " +
          "However, it will be rewritten as existence check and a filter due to the index not supporting actual suffix searches for composite indexes, " +
          "this is still faster than not using an index in the first place. " +
          "Any (non-existence check) predicate on `person.age` will also be rewritten as existence check with a filter. " +
          "More information about how the rewriting works can be found in <<administration-indexes-single-vs-composite-index, composite index limitations>>.",
      queryText = "MATCH (person:Person) WHERE person.surname ENDS WITH '300' AND exists(person.age) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
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
          "All values stored in the `Person(firstname)` index will be searched, and entries containing `'h'` will be returned. " +
          "This means that although the search will not be optimized to the extent of queries using `=`, `IN`, `>`, `<` or `STARTS WITH`, it is still faster than not using an index in the first place. " +
          "Composite indexes are currently not able to support `CONTAINS`. ",
      queryText = "MATCH (person:Person) WHERE person.firstname CONTAINS 'h' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexContainsScan"))
      }
    )
  }

  @Test def use_index_with_contains_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.surname, p.age)"))

    executePreparationQueries {
      val a = (0 to 100).map { _ => "CREATE (:Person)" }.toList
      val b = (0 to 300).map { i => s"CREATE (:Person {age: $i, surname: '${-i}'})" }.toList
      a ++ b
    }

    sampleAllIndexesAndWait()

    profileQuery(
      title = "Substring search using `CONTAINS` (composite index)",
      text =
        "The `CONTAINS` predicate on `person.surname` in the following query will use the `Person(surname,age)` index, if it exists. " +
          "However, it will be rewritten as existence check and a filter due to the index not supporting actual suffix searches for composite indexes, " +
          "this is still faster than not using an index in the first place. " +
          "Any (non-existence check) predicate on `person.age` will also be rewritten as existence check with a filter. " +
          "More information about how the rewriting works can be found in <<administration-indexes-single-vs-composite-index, composite index limitations>>.",
      queryText = "MATCH (person:Person) WHERE person.surname CONTAINS '300' AND exists(person.age) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
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
        "The `exists(p.firstname)` predicate in the following query will use the `Person(firstname)` index, if it exists. ",
      queryText = "MATCH (p:Person) WHERE exists(p.firstname) RETURN p",
      assertions = {
        (p) =>
          assertEquals(2, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
      }
    )
  }

  @Test def use_index_with_exists_property_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.firstname, p.surname)"))
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Existence check using `exists` (composite index)",
      text =
        "The `exists(p.firstname)` and `exists(p.surname)` predicate in the following query will use the `Person(firstname,surname)` index, if it exists. " +
        "Any (non-existence check) predicate on `person.surname` will be rewritten as existence check with a filter.",
      queryText = "MATCH (p:Person) WHERE exists(p.firstname) AND exists(p.surname) RETURN p",
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
      title = "Spatial distance searches (single-property index)",
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

  @Test def use_index_with_distance_query_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.place,p.name)"))
    executePreparationQueries(
      (for(x <- -10 to 10; y <- -10 to 10) yield s"CREATE (:Person {place: point({x:$x, y:$y}), name: '${x+y}' } )").toList)
    profileQuery(
      title = "Spatial distance searches (composite index)",
      text =
        "If a property with point values is indexed, the index is used for spatial distance searches as well as for range queries. " +
        "Any following (non-existence check) predicates (here on property `p.name` for index `:Person(place,name)`) " +
        "will be rewritten as existence check with a filter.",
      queryText = "MATCH (p:Person) WHERE distance(p.place, point({x: 1, y: 2})) < 2 AND exists(p.name) RETURN p.place",
      assertions = {
        (p) =>
          assertEquals(9, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexSeek(range,exists)"))
      }
    )
  }

  @Test def use_index_with_bbox_query() {
    executePreparationQueries(
      (for(x <- -10 to 10; y <- -10 to 10) yield s"CREATE (:Person {location: point({x:$x, y:$y})})").toList ++ List(
        "MATCH (n:Person {firstname: 'Andy'}) SET n.location = point({x: 1.2345, y: 5.4321})",
        "MATCH (n:Person {firstname: 'Mark'}) SET n.location = point({y: 1.2345, x: 5.4321})"
      )
    )
    profileQuery(
      title = "Spatial bounding box searches (single-property index)",
      text = "The ability to do index seeks on bounded ranges works even with the 2D and 3D spatial `Point` types.",
      queryText = "MATCH (person:Person) WHERE point({x: 1, y: 5}) < person.location < point({x: 2, y: 6}) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          checkPlanDescription(p)("NodeIndexSeek")
      }
    )
  }

  @Test def use_index_with_bbox_query_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.place,p.firstname)"))
    executePreparationQueries(
      (for(x <- -10 to 10; y <- -10 to 10) yield s"CREATE (:Person {place: point({x:$x, y:$y}), firstname: '${x+y}'})").toList ++ List(
        "MATCH (n:Person {firstname: 'Andy'}) SET n.place = point({x: 1.2345, y: 5.4321})",
        "MATCH (n:Person {firstname: 'Mark'}) SET n.place = point({y: 1.2345, x: 5.4321})"
      )
    )
    profileQuery(
      title = "Spatial bounding box searches (composite index)",
      text = "The ability to do index seeks on bounded ranges works even with the 2D and 3D spatial `Point` types. " +
        "Any following (non-existence check) predicates (here on property `p.firstname` for index `:Person(place,firstname)`) " +
        "will be rewritten as existence check with a filter. " +
        "For index `:Person(firstname,place)`, if the predicate on `firstname` is equality or list membership then the bounded range is handled as a range itself. " +
        "If the predicate on `firstname` is anything else then the bounded range is rewritten to existence and filter.",
      queryText = "MATCH (person:Person) WHERE point({x: 1, y: 5}) < person.place < point({x: 2, y: 6}) AND exists(person.firstname) RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          checkPlanDescription(p)("NodeIndexSeek(range,exists)")
      }
    )
  }

  def assertIndexesOnLabels(label: String, expectedIndexes: List[List[String]]) {
    val transaction = graphOps.beginTx()
    try {
      val indexDefs = transaction.schema.getIndexes(Label.label(label)).asScala.toList
      val properties = indexDefs.map(_.getPropertyKeys.asScala.toList)
      assert(properties.toSet === expectedIndexes.toSet)
    } finally {
      transaction.close()
    }
  }

  def assertIndexWithNameExists(name: String, expectedLabel: String, expectedProperties: List[String]) {
    val transaction = graphOps.beginTx()
    try {
      val indexDef = transaction.schema.getIndexByName(name)
      val label = indexDef.getLabels.iterator().next()
      val properties = indexDef.getPropertyKeys.asScala.toSet
      assert(label.equals(Label.label(expectedLabel)))
      assert(properties === expectedProperties.toSet)
    } finally {
      transaction.close()
    }
  }

  def assertIndexWithNameDoesNotExists(name: String) {
    val transaction = graphOps.beginTx()
    try {
      assertThrows[IllegalArgumentException](transaction.schema.getIndexByName(name))
    } finally {
      transaction.close()
    }
  }

  private def checkPlanDescription(result: DocsExecutionResult)(costString: String): Unit = {
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
