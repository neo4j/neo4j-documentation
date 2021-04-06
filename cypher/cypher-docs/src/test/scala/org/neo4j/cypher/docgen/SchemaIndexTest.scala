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

import org.hamcrest.CoreMatchers._
import org.hamcrest.Matcher
import org.junit.Assert._
import org.junit.Test
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.internal.plandescription.Arguments.Planner
import org.neo4j.cypher.internal.planner.spi.DPPlannerName
import org.neo4j.cypher.internal.planner.spi.IDPPlannerName
import org.neo4j.cypher.internal.runtime.interpreted.pipes.IndexSeekByRange
import org.neo4j.cypher.internal.util.Foldable.FoldableAny
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.schema.IndexSettingImpl._
import org.neo4j.kernel.impl.index.schema.GenericNativeIndexProvider
import org.neo4j.kernel.impl.index.schema.fusion.NativeLuceneFusionIndexProviderFactory30

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


  @Test def create_index_on_a_single_property() {
    testQuery(
      title = "Create a single-property index for nodes",
      text = "A named index on a single property for all nodes that have a particular label can be created with `CREATE INDEX index_name FOR (n:Label) ON (n.property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX node_index_name FOR (n:Person) ON (n.surname)",
      optionalResultExplanation = "Note that the index name needs to be unique.",
      assertions = _ => assertIndexWithNameExists("node_index_name", "Person", List("surname"))
    )
    testQuery(
      title = "Create a single-property index for relationships",
      text = "A named index on a single property for all relationships that have a particular relationship type can be created with `CREATE INDEX index_name FOR ()-[r:TYPE]-() ON (r.property)`. " +
        "Note that the index is not immediately available, but will be created in the background.",
      queryText = "CREATE INDEX rel_index_name FOR ()-[r:KNOWS]-() ON (r.since)",
      optionalResultExplanation = "Note that the index name needs to be unique.",
      assertions = _ => assertIndexWithNameExists("rel_index_name", "KNOWS", List("since"))
    )
    testQuery(
      title = "Create a single-property index only if it does not already exist",
      text = "If it is unknown if an index exists or not but we want to make sure it does, we add `IF NOT EXISTS`.",
      queryText = "CREATE INDEX node_index_name IF NOT EXISTS FOR (n:Person) ON (n.surname)",
      optionalResultExplanation = "Note that the index will not be created if there already exists an index with the same name, same schema or both.",
      assertions = _ => assertIndexWithNameExists("node_index_name", "Person", List("surname"))
    )
    testQuery(
      title = "Create a single-property index with specified index provider",
      text =
        """To create a single property index with a specific index provider, the `OPTIONS` clause is used.
          |Valid values for the index provider is `native-btree-1.0` and `lucene+native-3.0`, default if nothing is specified is `native-btree-1.0`.""".stripMargin,
      queryText = "CREATE BTREE INDEX index_with_provider FOR ()-[r:TYPE]-() ON (r.prop1) OPTIONS {indexProvider: 'native-btree-1.0'}",
      optionalResultExplanation = "Can be combined with specifying index configuration.",
      assertions = _ => assertIndexWithNameExists("index_with_provider", "TYPE", List("prop1"))
    )
    testQuery(
      title = "Create a single-property index with specified index configuration",
      text =
        """To create a single property index with a specific index configuration, the `OPTIONS` clause is used.
          |Valid configuration settings are `spatial.cartesian.min`, `spatial.cartesian.max`, `spatial.cartesian-3d.min`, `spatial.cartesian-3d.max`,
          |`spatial.wgs-84.min`, `spatial.wgs-84.max`, `spatial.wgs-84-3d.min`, and `spatial.wgs-84-3d.max`.
          |Non-specified settings get their respective default values.""".stripMargin,
      queryText =
        """CREATE BTREE INDEX index_with_config FOR (n:Label) ON (n.prop2)
          |OPTIONS {indexConfig: {`spatial.cartesian.min`: [-100.0, -100.0], `spatial.cartesian.max`: [100.0, 100.0]}}""".stripMargin,
      optionalResultExplanation = "Can be combined with specifying index provider.",
      assertions = _ => assertIndexWithNameExists("index_with_config", "Label", List("prop2"))
    )
  }

  @Test def create_index_on_a_node_composite_property() {
    testQuery(
      title = "Create a composite index for nodes",
      text = "A named index on multiple properties for all nodes that have a particular label -- i.e. a composite index -- can be created with " +
      "`CREATE INDEX index_name FOR (n:Label) ON (n.prop1, ..., n.propN)`. " +
      "Only nodes labeled with the specified label and which contain all the properties in the index definition will be added to the index. " +
      "Note that the composite index is not immediately available, but will be created in the background. " +
      "The following statement will create a named composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      queryText = "CREATE INDEX node_index_name FOR (n:Person) ON (n.age, n.country)",
      optionalResultExplanation = "Note that the index name needs to be unique.",
      assertions = _ => assertIndexWithNameExists("node_index_name", "Person", List("age", "country"))
    )
  }

  @Test def create_index_on_a_relationship_composite_property() {
    testQuery(
      title = "Create a composite index for relationships",
      text = "A named index on multiple properties for all relationships that have a particular relationship type -- i.e. a composite index -- can be created with " +
      "`CREATE INDEX index_name FOR ()-[r:TYPE]-() ON (r.prop1, ..., r.propN)`. " +
      "Only relationships labeled with the specified type and which contain all the properties in the index definition will be added to the index. " +
      "Note that the composite index is not immediately available, but will be created in the background. " +
      "The following statement will create a named composite index on all relationships labeled with `PURCHASED` and which have both a `date` and `amount` property: ",
      queryText = "CREATE INDEX rel_index_name FOR ()-[r:PURCHASED]-() ON (r.date, r.amount)",
      optionalResultExplanation = "Note that the index name needs to be unique.",
      assertions = _ => assertIndexWithNameExists("rel_index_name", "PURCHASED", List("date", "amount"))
    )
  }

  @Test def create_composite_index_with_options() {
    val nativeProvider = GenericNativeIndexProvider.DESCRIPTOR.name()
    val nativeLuceneProvider = NativeLuceneFusionIndexProviderFactory30.DESCRIPTOR.name()
    val cartesianMin = SPATIAL_CARTESIAN_MIN.getSettingName
    val cartesianMax = SPATIAL_CARTESIAN_MAX.getSettingName
    val cartesian3dMin = SPATIAL_CARTESIAN_3D_MIN.getSettingName
    val cartesian3dMax = SPATIAL_CARTESIAN_3D_MAX.getSettingName
    val wgsMin = SPATIAL_WGS84_MIN.getSettingName
    val wgsMax = SPATIAL_WGS84_MAX.getSettingName
    val wgs3dMin = SPATIAL_WGS84_3D_MIN.getSettingName
    val wgs3dMax = SPATIAL_WGS84_3D_MAX.getSettingName
    testQuery(
      title = "Create a composite index with specified index provider and configuration",
      text =
        s"""To create a composite index with a specific index provider and configuration, the `OPTIONS` clause is used.
          |Valid values for the index provider is `$nativeProvider` and `$nativeLuceneProvider`, default if nothing is specified is `$nativeProvider`.
          |Valid configuration settings are `$cartesianMin`, `$cartesianMax`, `$cartesian3dMin`, `$cartesian3dMax`,
          |`$wgsMin`, `$wgsMax`, `$wgs3dMin`, and `$wgs3dMax`.
          |Non-specified settings get their respective default values.""".stripMargin,
      queryText =
        s"""CREATE INDEX index_with_options FOR (n:Label) ON (n.prop1, n.prop2)
          |OPTIONS {
          | indexProvider: '$nativeLuceneProvider',
          | indexConfig: {`$wgsMin`: [-100.0, -80.0], `$wgsMax`: [100.0, 80.0]}
          |}""".stripMargin,
      optionalResultExplanation = "Specifying index provider and configuration can be done individually.",
      assertions = _ => assertIndexWithNameExists("index_with_options", "Label", List("prop1", "prop2"))
    )
  }

  @Test def list_indexes() {
    prepareAndTestQuery(
      title = "Listing all indexes",
      text =
        """
          |To list all indexes with the default output columns, the `SHOW INDEXES` command can be used.
          |If all columns are wanted, use `SHOW INDEXES YIELD *`.""".stripMargin,
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.firstname)")),
      queryText = "SHOW INDEXES",
      optionalResultExplanation =
        """One of the output columns from `SHOW INDEXES` is the name of the index.
          |This can be used to drop the index with the <<administration-indexes-drop-an-index, `DROP INDEX` command>>.""".stripMargin,
      assertions = p => assertEquals(3, p.size)
    )
    prepareAndTestQuery(
      title = "Listing indexes with filtering",
      text =
        """
          |One way of filtering the output from `SHOW INDEXES` by index type is the use of type keywords,
          |listed in the <<administration-indexes-syntax, syntax table>>.
          |As an example, to show only B-tree indexes, use `SHOW BTREE INDEXES`.
          |Another, more flexible way of filtering the output is to use the `WHERE` clause.
          |An example is to only show indexes not belonging to constraints.""".stripMargin,
      prepare = _ => executePreparationQueries(List("create index for ()-[r:KNOWS]-() on (r.since)")),
      queryText = "SHOW BTREE INDEXES WHERE uniqueness = 'NONUNIQUE'",
      optionalResultExplanation =
        """This will only return the default output columns.
          |To get all columns, use `SHOW INDEXES YIELD * WHERE ...`.""".stripMargin,
      assertions = p => assertEquals(4, p.size)
    )
  }

  @Test def drop_index_on_a_label_single_property() {
    prepareAndTestQuery(
      title = "Drop a single-property index",
      text = "An index on all nodes that have a label and single property combination can be dropped with `DROP INDEX ON :Label(property)`.",
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.firstname)")),
      queryText = "DROP INDEX ON :Person(firstname)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("location"), List("highScore")))
    )
  }

  @Test def drop_index_on_a_label_composite_property() {
    prepareAndTestQuery(
      title = "Drop a composite index",
      text = "A composite index on all nodes that have a label and multiple property combination can be dropped with `DROP INDEX ON :Label(prop1, ..., propN)`. " +
      "The following statement will drop a composite index on all nodes labeled with `Person` and which have both an `age` and `country` property: ",
      prepare = _ => executePreparationQueries(List("create index for (p:Person) on (p.age, p.country)")),
      queryText = "DROP INDEX ON :Person(age, country)",
      optionalResultExplanation = "",
      assertions = (p) => assertIndexesOnLabels("Person", List(List("firstname"), List("location"), List("highScore")))
    )
  }

  @Test def drop_index() {
    prepareAndTestQuery(
      title = "Drop an index",
      text =
        """An index can be dropped using the name with the `DROP INDEX index_name` command. This command can drop node, relationship or lookup indexes.
          |The name of the index can be found using the <<administration-indexes-list-indexes, `SHOW INDEXES` command>>, given in the output column `name`.""".stripMargin,
      prepare = _ => executePreparationQueries(List("CREATE INDEX index_name FOR (n:Person) ON (n.surname)")),
      queryText = "DROP INDEX index_name",
      assertions = _ => assertIndexWithNameDoesNotExists("index_name")
    )
    testQuery(
      title = "Drop a non-existing index",
      text = "If it is uncertain if an index exists and you want to drop it if it does but not get an error should it not, use: ",
      queryText = "DROP INDEX missing_index_name IF EXISTS",
      assertions = _ => assertIndexWithNameDoesNotExists("missing_index_name")
    )
  }

  @Test def use_index() {
    profileQuery(
      title = "A simple example",
      text = "In the example below, the query will use a `Person(firstname)` index, if it exists. ",
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

  @Test def indexes_are_case_sensitive() {
    profileQuery(
      title = "Indexes are case sensitive",
      text = "Note that indexes are case sensitive, that means we cannot use indexes for queries using `toLower` and `toUpper`. For example, the following query cannot use an index:",
      queryText = "MATCH (person:Person) WHERE toLower(person.firstname) = 'andy' RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkNotInPlanDescription(p)("NodeIndexSeek")
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
        "The following query will use the composite index defined <<administration-indexes-create-a-composite-index-for-nodes, earlier>>: ",
      prepare = _ => executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.age, p.country)")),
      queryText = "MATCH (n:Person) WHERE n.age = 35 AND n.country = 'UK' RETURN n",
      optionalResultExplanation = "However, the query `MATCH (n:Person) WHERE n.age = 35 RETURN n` will not be backed by the composite index, " +
        "as the query does not contain a predicate on the `country` property. " +
      "It will only be backed by an index on the `Person` label and `age` property defined thus: `:Person(age)`; i.e. a single-property index. ",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescription(p)("n:Person(age, country) WHERE age = $autoint_0 AND country = $autostring_1")
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

          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescription(p)("person:Person(firstname, highScore) WHERE firstname > $autostring_0 AND highScore IS NOT NULL")
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
      queryText = "MATCH (person:Person) WHERE 10000 < person.highScore < 20000 AND person.name IS NOT NULL RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)

          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescriptionArgument(p)("person:Person(highScore, name) WHERE highScore > $autoint_0 AND highScore < $autoint_1 AND name IS NOT NULL")
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

          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescription(p)("person:Person(age, country) WHERE age IN $autolist_0 AND country IN $autolist_1")
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
      queryText = "MATCH (person:Person) WHERE person.firstname STARTS WITH 'And' AND person.surname IS NOT NULL RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescription(p)("person:Person(firstname, surname) WHERE firstname STARTS WITH $autostring_0 AND surname IS NOT NULL")
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
      queryText = "MATCH (person:Person) WHERE person.surname ENDS WITH '300' AND person.age IS NOT NULL RETURN person",
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
      queryText = "MATCH (person:Person) WHERE person.surname CONTAINS '300' AND person.age IS NOT NULL RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
      }
    )
  }

  @Test def use_index_with_property_is_not_null() {
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Existence check using `IS NOT NULL` (single-property index)",
      text =
        "The `p.firstname IS NOT NULL` predicate in the following query will use the `Person(firstname)` index, if it exists. ",
      queryText = "MATCH (p:Person) WHERE p.firstname IS NOT NULL RETURN p",
      assertions = {
        (p) =>
          assertEquals(2, p.size)
          assertThat(p.executionPlanDescription().toString, containsString("NodeIndexScan"))
      }
    )
  }

  @Test def use_index_with_property_is_not_null_composite() {
    executePreparationQueries(List("CREATE INDEX FOR (p:Person) ON (p.firstname, p.surname)"))
    // Need to make index preferable in terms of cost
    executePreparationQueries((0 to 250).map { i =>
      "CREATE (:Person)"
    }.toList)
    profileQuery(
      title = "Existence check using `IS NOT NULL` (composite index)",
      text =
        "The `p.firstname IS NOT NULL` and `p.surname IS NOT NULL` predicates in the following query will use the `Person(firstname,surname)` index, if it exists. " +
        "Any (non-existence check) predicate on `person.surname` will be rewritten as existence check with a filter.",
      queryText = "MATCH (p:Person) WHERE p.firstname IS NOT NULL AND p.surname IS NOT NULL RETURN p",
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
      queryText = "MATCH (p:Person) WHERE distance(p.place, point({x: 1, y: 2})) < 2 AND p.name IS NOT NULL RETURN p.place",
      assertions = {
        (p) =>
          assertEquals(9, p.size)
          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescriptionArgument(p)("p:Person(place, name) WHERE distance(place, point($autoint_0, $autoint_1)) < $autoint_2 AND name IS NOT NULL")
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
      queryText = "MATCH (person:Person) WHERE point({x: 1, y: 5}) < person.place < point({x: 2, y: 6}) AND person.firstname IS NOT NULL RETURN person",
      assertions = {
        (p) =>
          assertEquals(1, p.size)
          checkPlanDescription(p)("NodeIndexSeek")
          checkPlanDescriptionArgument(p)("person:Person(place, firstname) WHERE place > point({x: $autoint_0, y: $autoint_1}) AND place < point({x: $autoint_2, y: $autoint_3}) AND firstname IS NOT NULL")
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

  def assertIndexWithNameExists(name: String, expectedEntity: String, expectedProperties: List[String]) {
    val transaction = graphOps.beginTx()
    try {
      val indexDef = transaction.schema.getIndexByName(name)
      val entity = if (indexDef.isNodeIndex) indexDef.getLabels.iterator().next().name() else indexDef.getRelationshipTypes.iterator().next().name()
      val properties = indexDef.getPropertyKeys.asScala.toSet
      assert(entity.equals(expectedEntity))
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
   checkPlanDescription(result, containsString(costString))
  }

  private def checkNotInPlanDescription(result: DocsExecutionResult)(costString: String): Unit = {
    checkPlanDescription(result, not(containsString(costString)))
  }

  private def checkPlanDescription(result: DocsExecutionResult, costMatcher: Matcher[String]): Unit = {
    val planDescription = result.executionPlanDescription()
    val plannerArgument = planDescription.arguments.find(a => a.name == "planner")

    plannerArgument match {
      case Some(Planner(IDPPlannerName.name)) =>
        assertThat(planDescription.toString, costMatcher)
      case Some(Planner(DPPlannerName.name)) =>
        assertThat(planDescription.toString, costMatcher)
      case Some(Planner(name)) if name.equals("COST") =>
        assertThat(planDescription.toString, costMatcher)

      case x =>
        fail(s"Couldn't determine used planner: $x")
    }
  }

  private def checkPlanDescriptionArgument(result: DocsExecutionResult)(expected: String): Unit = {
    val planDescription = result.executionPlanDescription()

    val res = planDescription.treeExists {
      case str: String => str.contains(expected)
    }

    assertTrue(s"Could not find expected string: $expected", res)
  }
}
