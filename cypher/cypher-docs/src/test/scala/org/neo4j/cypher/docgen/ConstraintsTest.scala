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

import java.io.File

import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder
import org.junit.Test
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.exceptions.CypherExecutionException
import org.neo4j.graphdb.{ConstraintViolationException, Label, RelationshipType}

import scala.collection.JavaConverters._

class ConstraintsTest extends DocumentingTestBase with SoftReset {

  override def parent = Some("Administration")
  override def section: String = "Constraints"

  override protected def newDatabaseManagementService(directory: File): DatabaseManagementService = new EnterpriseDatabaseManagementServiceBuilder(directory).build()

  @Test def create_unique_constraint() {
    testQuery(
      title = "Create unique constraint",
      text = "To create a constraint that makes sure that your database will never contain more than one node with a specific " +
        "label and one property value, use the `IS UNIQUE` syntax.",
      queryText = "CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE",
      optionalResultExplanation = "",
      assertions = (p) => assertNodeConstraintExist("Book", "isbn")
    )
  }

  @Test def get_all_constraints() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Get a list of all constraints in the database",
      text = "Calling the built-in procedure `db.constraints` will list all the constraints in the database.",
      queryText = "CALL db.constraints",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (book:Book) ASSERT book.isbn IS UNIQUE")),
      assertions = (p) => assert(p.size == 1)
    )
  }

  @Test def drop_unique_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop unique constraint",
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
        "data. We may choose to use <<administration-indexes>> instead or remove the offending nodes and then re-apply the " +
        "constraint."
    )
  }

  @Test def create_node_property_existence_constraint() {
    testQuery(
      title = "Create node property existence constraint",
      text = "To create a constraint that ensures that all nodes with a certain label have a certain property, use the `ASSERT exists(variable.propertyName)` syntax.",
      queryText = "CREATE CONSTRAINT ON (book:Book) ASSERT exists(book.isbn)",
      optionalResultExplanation = "",
      assertions = (p) => assertNodeConstraintExist("Book", "isbn")
    )
  }

  @Test def drop_node_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop node property existence constraint",
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
      title = "Create relationship property existence constraint",
      text = "To create a constraint that makes sure that all relationships with a certain type have a certain property, use the `ASSERT exists(variable.propertyName)` syntax.",
      queryText = "CREATE CONSTRAINT ON ()-[like:LIKED]-() ASSERT exists(like.day)",
      optionalResultExplanation = "",
      assertions = (p) => assertRelationshipConstraintExist("LIKED", "day")
    )
  }

  @Test def drop_relationship_property_existence_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop relationship property existence constraint",
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
      title = "Create a Node Key",
      text = "To create a Node Key ensuring that all nodes with a particular label have a set of defined properties whose combined value is unique, and where all properties in the set are present, use the `ASSERT (variable.propertyName_1, ..., variable.propertyName_n) IS NODE KEY` syntax.",
      queryText = "CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "",
      assertions = (p) => assertNodeKeyConstraintExists("Person", "firstname", "surname")
    )
  }

  @Test def drop_node_key_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Drop a Node Key",
      text = "Use `DROP CONSTRAINT` to remove a Node Key from the database.",
      queryText = "DROP CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "",
      prepare = _ => executePreparationQueries(List("CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY")),
      assertions = (p) => assertNodeKeyConstraintDoesNotExist("Person", "firstname", "surname")
    )
  }

  @Test def play_nice_with_node_key_constraint() {
    generateConsole = false

    prepareAndTestQuery(
      title = "Create a node that complies with a Node Key",
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
      title = "Create a node that violates a Node Key",
      text = "Trying to create a `Person` node without a `surname` property, given a Node Key on `:Person(firstname, surname)`, will fail.",
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
      title = "Failure to create a Node Key due to existing node",
      text = "Trying to create a Node Key on the property `surname` on nodes with the `Person` label will fail when " +
        " a node without a `surname` already exists in the database.",
      queryText = "CREATE CONSTRAINT ON (n:Person) ASSERT (n.firstname, n.surname) IS NODE KEY",
      optionalResultExplanation = "In this case the Node Key can't be created because it is violated by existing " +
        "data. We may choose to remove the offending nodes and then re-apply the constraint."
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
