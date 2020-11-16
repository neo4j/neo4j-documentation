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

import java.io.File

import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder
import org.junit.Test
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.exceptions.CypherExecutionException
import org.neo4j.graphdb.{ConstraintViolationException, Label, RelationshipType}

import scala.collection.JavaConverters._

class ConstraintsTest extends DocumentingTestBase with SoftReset {

  override def parent: Option[String] = Some("Administration")
  override def section: String = "Constraints"

  override protected def newDatabaseManagementService(directory: File): DatabaseManagementService = new EnterpriseDatabaseManagementServiceBuilder(directory)
    .setConfig(databaseConfig()).build()


  @Test def create_unique_constraint() {
    testQuery(
      title = "Create a unique constraint",
      text = "When creating a unique constraint, a name can be provided. The constraint ensures that your database " +
        "will never contain more than one node with a specific label and one property value.",
      queryText = "CREATE CONSTRAINT constraint_name ON (book:Book) ASSERT book.isbn IS UNIQUE",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "Book", List("isbn"))
    )
    prepareAndTestQuery(
      title = "Create a unique constraint only if it does not already exist",
      text = "If it is unknown if a constraint exists or not but we want to make sure it does, we add the `IF NOT EXISTS`. " +
        "The uniqueness constraint ensures that your database will never contain more than one node with a specific label and one property value. " +
        "Note: The `IF NOT EXISTS` syntax for constraints is only available in Neo4j 4.1.3 and onwards.",
      prepare = _ => executePreparationQueries(List("DROP CONSTRAINT constraint_name IF EXISTS")),
      queryText = "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (book:Book) ASSERT book.isbn IS UNIQUE",
      optionalResultExplanation = "Note no constraint will be created if any other constraint with that name or another uniqueness constraint on the same schema already exists. " +
        "Assuming no such constraints existed:",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "Book", List("isbn"))
    )
  }

  @Test def list_constraints() {
    generateConsole = false

    prepareAndTestQuery(
      title = "List constraints",
      text = "Calling the built-in procedure `db.constraints` will list all constraints, including their names.",
      queryText = "CALL db.constraints",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE")),
      assertions = (p) => assert(p.size == 1)
    )
  }

  @Test def drop_unique_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a unique constraint",
      text = "By using `DROP CONSTRAINT`, you remove a constraint from the database.",
      queryText = "DROP CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE")),
      assertions = (p) => assertNodeConstraintDoesNotExist("Book", "isbn")
    )
  }

  @Test def play_nice_with_unique_property_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Create a node that complies with unique property constraints",
      text = "Create a `Book` node with an `isbn` that isn't already in the database.",
      queryText = "CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE")),
      assertions = (p) => assertNodeConstraintExist("Book", "isbn")
    )
  }

  @Test def violate_unique_property_constraint() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE")
    execute("CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})")

    testFailingQuery[CypherExecutionException](
      title = "Create a node that violates a unique property constraint",
      text = "Create a `Book` node with an `isbn` that is already used in the database.",
      queryText = "CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})",
      optionalResultExplanation = "In this case the node isn't created in the graph."
    )
  }

  @Test def fail_to_create_constraint() {
    generateConsole = false
    execute("CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})")
    execute("CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases 2'})")

    testFailingQuery[CypherExecutionException](
      title = "Failure to create a unique property constraint due to conflicting nodes",
      text = "Create a unique property constraint on the property `isbn` on nodes with the `Book` label when there are two nodes with" +
        " the same `isbn`.",
      queryText = "CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE",
      optionalResultExplanation = "In this case the constraint can't be created because it is violated by existing " +
        "data. We may choose to use <<administration-indexes-search-performance>> instead or remove the offending nodes and then re-apply the " +
        "constraint."
    )
  }

  @Test def create_node_property_existence_constraint() {
    testQuery(
      title = "Create a node property existence constraint",
      text = "When creating a node property existence constraint, a name can be provided. The constraint ensures that all nodes " +
        "with a certain label have a certain property.",
      queryText = "CREATE CONSTRAINT constraint_name ON (book:Book) ASSERT exists(book.isbn)",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "Book", List("isbn"))
    )
    prepareAndTestQuery(
      title = "Create a node property existence constraint only if it does not already exist",
      text = "If it is unknown if a constraint exists or not but we want to make sure it does, we add the `IF NOT EXISTS`. " +
        "The node property existence constraint ensures that all nodes with a certain label have a certain property. " +
        "Note: The `IF NOT EXISTS` syntax for constraints is only available in Neo4j 4.1.3 and onwards.",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (book:Book) ASSERT book.isbn IS UNIQUE")),
      queryText = "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (book:Book) ASSERT exists(book.isbn)",
      optionalResultExplanation = "Note no constraint will be created if any other constraint with that name or another node property existence constraint on the same schema already exists. " +
        "Assuming a constraint with the name `constraint_name` already existed:",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "Book", List("isbn"))
    )
  }

  @Test def drop_node_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a node property existence constraint",
      text = "By using `DROP CONSTRAINT`, you remove a constraint from the database.",
      queryText = "DROP CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)")),
      assertions = (p) => assertNodeConstraintDoesNotExist("Book", "isbn")
    )
  }

  @Test def play_nice_with_node_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Create a node that complies with property existence constraints",
      text = "Create a `Book` node with an `isbn` property.",
      queryText = "CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)")),
      assertions = (p) => assertNodeConstraintExist("Book", "isbn")
    )
  }

  @Test def violate_node_property_existence_constraint() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)")
    testFailingQuery[ConstraintViolationException](
      title = "Create a node that violates a property existence constraint",
      text = "Trying to create a `Book` node without an `isbn` property, given a property existence constraint on `:Book(isbn)`.",
      queryText = "CREATE (book:Book {title: 'Graph Databases'})",
      optionalResultExplanation = "In this case the node isn't created in the graph."
    )
  }

  @Test def violate_node_property_existence_constraint_by_removing_property() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)")
    execute("CREATE (book:Book {isbn: '1449356265', title: 'Graph Databases'})")
    testFailingQuery[ConstraintViolationException](
      title = "Removing an existence constrained node property",
      text = "Trying to remove the `isbn` property from an existing node `book`, given a property existence constraint on `:Book(isbn)`.",
      queryText = "MATCH (book:Book {title: 'Graph Databases'}) REMOVE book.isbn",
      optionalResultExplanation = "In this case the property is not removed."
    )
  }

  @Test def fail_to_create_node_property_existence_constraint() {
    generateConsole = false
    execute("CREATE (book:Book {title: 'Graph Databases'})")

    testFailingQuery[CypherExecutionException](
      title = "Failure to create a node property existence constraint due to existing node",
      text = "Create a constraint on the property `isbn` on nodes with the `Book` label when there already exists " +
        " a node without an `isbn`.",
      queryText = "CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)",
      optionalResultExplanation = "In this case the constraint can't be created because it is violated by existing " +
        "data. We may choose to remove the offending nodes and then re-apply the constraint."
    )
  }

  @Test def create_relationship_property_existence_constraint() {
    testQuery(
      title = "Create a relationship property existence constraint",
      text = "When creating a relationship property existence constraint, a name can be provided. The constraint ensures all relationships " +
        "with a certain type have a certain property.",
      queryText = "CREATE CONSTRAINT constraint_name ON ()-[like:LIKED]-() ASSERT exists(like.day)",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "LIKED", List("day"), forRelationship = true)
    )
    prepareAndTestQuery(
      title = "Create a relationship property existence constraint only if it does not already exist",
      text = "If it is unknown if a constraint exists or not but we want to make sure it does, we add the `IF NOT EXISTS`. " +
        "The relationship property existence constraint ensures all relationships with a certain type have a certain property. " +
        "Note: The `IF NOT EXISTS` syntax for constraints is only available in Neo4j 4.1.3 and onwards.",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[like:LIKED]-() ASSERT exists(like.since)")),
      queryText = "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON ()-[like:LIKED]-() ASSERT exists(like.day)",
      optionalResultExplanation = "Note no constraint will be created if any other constraint with that name or another relationship property existence constraint on the same schema already exists. " +
        "Assuming a constraint with the name `constraint_name` already existed:",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "LIKED", List("since"), forRelationship = true)
    )
  }

  @Test def drop_relationship_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a relationship property existence constraint",
      text = "To remove a constraint from the database, use `DROP CONSTRAINT`.",
      queryText = "DROP CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)")),
      assertions = (p) => assertRelationshipConstraintDoesNotExist("LIKED", "day")
    )
  }

  @Test def play_nice_with_relationship_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Create a relationship that complies with property existence constraints",
      text = "Create a `LIKED` relationship with a `day` property.",
      queryText = "CREATE (user:User)-[like:LIKED {day: 'yesterday'}]->(book:Book)",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)")),
      assertions = (p) => assertRelationshipConstraintExist("LIKED", "day")
    )
  }

  @Test def violate_relationship_property_existence_constraint() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)")
    testFailingQuery[ConstraintViolationException](
      title = "Create a relationship that violates a property existence constraint",
      text = "Trying to create a `LIKED` relationship without a `day` property, given a property existence constraint `:LIKED(day)`.",
      queryText = "CREATE (user:User)-[like:LIKED]->(book:Book)",
      optionalResultExplanation = "In this case the relationship isn't created in the graph."
    )
  }

  @Test def violate_relationship_property_existence_constraint_by_removing_property() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)")
    execute("CREATE (user:User)-[like:LIKED {day: 'today'}]->(book:Book)")
    testFailingQuery[ConstraintViolationException](
      title = "Removing an existence constrained relationship property",
      text = "Trying to remove the `day` property from an existing relationship `like` of type `LIKED`, given a property existence constraint `:LIKED(day)`.",
      queryText = "MATCH (user:User)-[like:LIKED]->(book:Book) REMOVE like.day",
      optionalResultExplanation = "In this case the property is not removed."
    )
  }

  @Test def fail_to_create_relationship_property_existence_constraint() {
    generateConsole = false
    execute("CREATE (user:User)-[like:LIKED]->(book:Book)")

    testFailingQuery[CypherExecutionException](
      title = "Failure to create a relationship property existence constraint due to existing relationship",
      text = "Create a constraint on the property `day` on relationships with the `LIKED` type when there already " +
        "exists a relationship without a property named `day`.",
      queryText = "CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)",
      optionalResultExplanation = "In this case the constraint can't be created because it is violated by existing " +
        "data. We may choose to remove the offending relationships and then re-apply the constraint."
    )
  }

  @Test def create_node_key_constraint() {
    testQuery(
      title = "Create a node key constraint",
      text = "When creating a node key constraint, a name can be provided. The constraint ensures that all nodes " +
        "with a particular label have a set of defined properties whose combined value is unique " +
        "and all properties in the set are present.",
      queryText = "CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      assertions = _ => assertConstraintWithNameExists("constraint_name", "Person", List("firstname", "surname"))
    )
    prepareAndTestQuery(
      title = "Create a node key constraint only if it does not already exist",
      text = "If it is unknown if a constraint exists or not but we want to make sure it does, we add the `IF NOT EXISTS`. " +
        "The node key constraint ensures that all nodes with a particular label have a set of defined properties whose combined value is unique " +
        "and all properties in the set are present. " +
        "Note: The `IF NOT EXISTS` syntax for constraints is only available in Neo4j 4.1.3 and onwards.",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT old_constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")),
      queryText = "CREATE CONSTRAINT constraint_name IF NOT EXISTS ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "Note no constraint will be created if any other constraint with that name or another node key constraint on the same schema already exists. " +
        "Assuming a node key constraint on `(:Person {firstname, surname})` already existed:",
      assertions = _ => assertConstraintWithNameExists("old_constraint_name", "Person", List("firstname", "surname"))
    )
  }

  @Test def drop_node_key_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a node key constraint",
      text = "Use `DROP CONSTRAINT` to remove a node key constraint from the database.",
      queryText = "DROP CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")),
      assertions = (p) => assertNodeKeyConstraintDoesNotExist("Person", "firstname", "surname")
    )
  }

  @Test def play_nice_with_node_key_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Create a node that complies with node key constraints",
      text = "Create a `Person` node with both a `firstname` and `surname` property.",
      queryText = "CREATE (p:Person {firstname: 'John', surname: 'Wood', age: 55})",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")),
      assertions = (p) => assertNodeKeyConstraintExists("Person", "firstname", "surname")
    )
  }

  @Test def violate_node_key_constraint() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")
    testFailingQuery[ConstraintViolationException](
      title = "Create a node that violates a node key constraint",
      text = "Trying to create a `Person` node without a `surname` property, given a node key constraint on `:Person(firstname, surname)`, will fail.",
      queryText = "CREATE (p:Person {firstname: 'Jane', age: 34})",
      optionalResultExplanation = "In this case the node isn't created in the graph."
    )
  }

  @Test def break_node_key_constraint_by_removing_property() {
    generateConsole = false
    execute("CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")
    execute("CREATE (p:Person {firstname: 'John', surname: 'Wood'})")
    testFailingQuery[ConstraintViolationException](
      title = "Removing a `NODE KEY`-constrained property",
      text = "Trying to remove the `surname` property from an existing node `Person`, given a `NODE KEY` constraint on `:Person(firstname, surname)`.",
      queryText = "MATCH (p:Person {firstname: 'John', surname: 'Wood'}) REMOVE p.surname",
      optionalResultExplanation = "In this case the property is not removed."
    )
  }

  @Test def fail_to_create_node_key_constraint() {
    generateConsole = false
    execute("CREATE (p:Person {firstname: 'Jane', age: 34})")

    testFailingQuery[CypherExecutionException](
      title = "Failure to create a node key constraint due to existing node",
      text = "Trying to create a node key constraint on the property `surname` on nodes with the `Person` label will fail when " +
        " a node without a `surname` already exists in the database.",
      queryText = "CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "In this case the node key constraint can't be created because it is violated by existing " +
        "data. We may choose to remove the offending nodes and then re-apply the constraint."
    )
  }

  @Test def drop_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a constraint",
      text = "A constraint can be dropped using the name with the `DROP CONSTRAINT constraint_name` command. " +
        "It is the same command for unique property, property existence and node key constraints.",
      queryText = "DROP CONSTRAINT constraint_name",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT constraint_name ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")),
      assertions = _ => assertConstraintWithNameDoesNotExists("constraint_name")
    )
    testQuery(
      title = "Drop a non-existing constraint",
      text = "If it is uncertain if any constraint with a given name exists and you want to drop it if it does but not get an error should it not, use `IF EXISTS`. " +
        "It is the same command for unique property, property existence and node key constraints. " +
        "Note: The `IF EXISTS` syntax for constraints is only available in Neo4j 4.1.3 and onwards.",
      queryText = "DROP CONSTRAINT missing_constraint_name IF EXISTS",
      assertions = _ => assertConstraintWithNameDoesNotExists("missing_constraint_name")
    )
  }

  private def assertNodeConstraintExist(labelName: String, propName: String) {
    assert(hasNodeConstraint(labelName, propName))
  }

  private def assertNodeKeyConstraintExists(labelName: String, propNames: String*) {
    assert(hasNodeKeyConstraint(labelName, propNames.toSeq))
  }

  private def assertNodeConstraintDoesNotExist(labelName: String, propName: String) {
    assert(!hasNodeConstraint(labelName, propName))
  }

  private def assertNodeKeyConstraintDoesNotExist(labelName: String, propNames: String*) {
    assert(!hasNodeKeyConstraint(labelName, propNames.toSeq))
  }

  private def assertRelationshipConstraintExist(typeName: String, propName: String) {
    assert(hasRelationshipConstraint(typeName, propName))
  }

  private def assertRelationshipConstraintDoesNotExist(typeName: String, propName: String) {
    assert(!hasRelationshipConstraint(typeName, propName))
  }

  def assertConstraintWithNameExists(name: String, expectedLabelOrType: String, expectedProperties: List[String], forRelationship: Boolean = false) {
    val transaction = graphOps.beginTx()
    try {
      val constraintDef = transaction.schema.getConstraintByName(name)
      val properties = constraintDef.getPropertyKeys.asScala.toSet
      assert(properties === expectedProperties.toSet)
      if (forRelationship) {
        val relType = constraintDef.getRelationshipType
        assert(relType.equals(RelationshipType.withName(expectedLabelOrType)))
      } else {
        val label = constraintDef.getLabel
        assert(label.equals(Label.label(expectedLabelOrType)))
      }
    } finally {
      transaction.close()
    }
  }

  def assertConstraintWithNameDoesNotExists(name: String) {
    val transaction = graphOps.beginTx()
    try {
      assertThrows[IllegalArgumentException](transaction.schema.getConstraintByName(name))
    } finally {
      transaction.close()
    }
  }

  private def hasNodeConstraint(labelName: String, propName: String): Boolean = {
    val transaction = graphOps.beginTx();
    try {
      val constraints = transaction.schema().getConstraints(Label.label(labelName)).asScala
      constraints.exists(_.getPropertyKeys.asScala.exists(_ == propName))
    } finally {
      transaction.close()
    }
  }

  private def hasRelationshipConstraint(typeName: String, propName: String): Boolean = {
    val transaction = graphOps.beginTx();
    try {
      val constraints = transaction.schema().getConstraints(RelationshipType.withName(typeName)).asScala
      constraints.exists(_.getPropertyKeys.asScala.exists(_ == propName))
    } finally {
      transaction.close()
    }
  }

  private def hasNodeKeyConstraint(labelName: String, propNames: Seq[String]): Boolean = {
      val transaction = graphOps.beginTx();
      try {
        val constraints = transaction.schema().getConstraints( Label.label(labelName) ).asScala
        constraints.nonEmpty && constraints.head.getPropertyKeys().asScala.toList == propNames.toList
      } finally {
        transaction.close()
      }
  }
}
