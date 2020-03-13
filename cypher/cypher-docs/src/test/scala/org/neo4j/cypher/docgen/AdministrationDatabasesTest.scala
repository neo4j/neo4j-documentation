package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, QueryStatisticsTestSupport, ResultAndDbAssertions, ResultAssertions}
import org.neo4j.graphdb.{Label, Node}
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AdministrationDatabasesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing databases", "administration-managing-databases")
    database("system")
    synopsis("This section explains how to use Cypher to manage Neo4j databases: creating, deleting, starting and stopping individual databases within a single server.")
    p(
      """
        |* <<administration-managing-databases-introduction, Introduction>>
        |* <<administration-managing-databases-syntax, Syntax>>
        |* <<administration-managing-databases-examples, Examples>>
        |** <<administration-managing-databases-examples-show, Listing databases>>
        |** <<administration-managing-databases-examples-create, Creating databases>>
        |** <<administration-managing-databases-examples-stop, Stopping databases>>
        |** <<administration-managing-databases-examples-start, Starting databases>>
        |** <<administration-managing-databases-examples-drop, Deleting databases>>
        |""".stripMargin)
    section("Introduction", "administration-managing-databases-introduction") {
      p(
        """With Neo4j, multiple databases can be managed within the same DBMS.
          |The metadata for these databases, including the associated security model, is maintained in a special database called the `system` database.
          |All commands described in this section must be executed against the `system` database.""".stripMargin)
      p(
        """In order for a user to manage databases, they must first be assigned the required privileges.
          |A user who is assigned the `admin` role can perform all the actions decribed in this section.
          |The privileges to `START` and `STOP` databases can be granted to roles using Cypher commands.
          |For details, see <<administration-managing-database-privileges>>.
          |The privileges to `CREATE` and `DROP` databases are only available through the `admin` role.""".stripMargin)
    }
    section("Syntax", "administration-managing-databases-syntax") {
      p("include::managing-databases/database-management-syntax.asciidoc[]")
    }
    section("Examples", "administration-managing-databases-examples") {
    section("Listing databases", "administration-managing-databases-examples-show") {
      p("There are three different commands for listing databases. Listing all databases, listing a particular database or listing the default database.")
      p("All available databases can be seen using the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("A particular database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE system", assertDatabaseShown("system")) {
        resultTable()
      }
      p("The default database can be seen using the command `SHOW DEFAULT DATABASE`.")
      query("SHOW DEFAULT DATABASE", assertDatabaseShown("neo4j")) {
        resultTable()
      }
      note {
        p(
          """Note that for failed databases, the `currentStatus` and `requestedStatus` are different.
            |This often implies an error, but **does not always**.
            |For example, a database may take a while to transition from `offline` to `online` due to performing recovery.
            |Or, during normal operation a database's `currentStatus` may be transiently different from its `requestedStatus` due to a necessary automatic process, such as one Neo4j instance copying store files from another.
            |The possible statuses are `initial`, `online`, `offline`, `store copying` and `unknown`.
            |""".stripMargin)
      }
    }
    section("Creating databases", "administration-managing-databases-examples-create", "enterprise-edition") {
      p("Databases can be created using `CREATE DATABASE`.")
      query("CREATE DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database has been created, it will show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database already exists. " +
        "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database already exist. " +
        "Adding `OR REPLACE` to the command will result in any existing database being deleted and a new one created.")
      query("CREATE DATABASE customers IF NOT EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
      query("CREATE OR REPLACE DATABASE customers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        p("This is equivalent to running `DROP DATABASE customers IF EXISTS` followed by `CREATE DATABASE customers`.")
      }
      note {
        p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
      }
    }
    section("Stopping databases", "administration-managing-databases-examples-stop", "enterprise-edition") {
      p("Databases can be stopped using the command `STOP DATABASE`.")
      query("STOP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the stopped database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Starting databases", "administration-managing-databases-examples-start", "enterprise-edition") {
      p("Databases can be started using the command `START DATABASE`.")
      query("START DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the started database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Deleting databases", "administration-managing-databases-examples-drop", "enterprise-edition") {
      p("Databases can be deleted using the command `DROP DATABASE`.")
      query("DROP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database has been deleted, it will no longer show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database does not exists. " +
        "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database not exist.")
      query("DROP DATABASE customers IF EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
    }
    }
  }.build()

  private def assertDatabasesShown = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val dbNodes = tx.findNodes(Label.label("Database")).asScala.toList
      val dbNames = dbNodes.map(n => n.getProperty("name")).toSet
      val result = p.columnAs[String]("name").toSet
      result should equal(dbNames)
    } finally {
      tx.close()
    }
  })

  private def assertDatabaseShown(expected: String) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("name").toSet
      result should equal(Set(expected))
    } finally {
      tx.close()
    }
  })
}
