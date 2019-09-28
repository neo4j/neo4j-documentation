package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, QueryStatisticsTestSupport, ResultAndDbAssertions, ResultAssertions}
import org.neo4j.graphdb.{Label, Node}
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class DatabasesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"
  override def doc: Document = new DocBuilder {
    doc("Databases", "administration-databases")
    database("system")
    synopsis("This section explains how to use Cypher to manage Neo4j databases.")
    p(
      """
        |* <<administration-databases-introduction, Introduction>>
        |* <<administration-databases-commands, Multi-database commands>>
        | ** <<administration-databases-show-databases, Listing databases>>
        | ** <<administration-databases-create-database, Creating databases>>
        | ** <<administration-databases-stop-database, Stopping databases>>
        | ** <<administration-databases-start-database, Starting databases>>
        | ** <<administration-databases-drop-databases, Deleting databases>>
        |""".stripMargin)
    section("Introduction", "administration-databases-introduction") {
      p(
        """Neo4j allows the same server to manage multiple databases. The metadata for these databases,
          |including the associated security model, is maintained in a special database called the `system` database.
          |All administrative commands need to be executing against the `system` database.""".stripMargin)
    }
    section("Multi-database commands", "administration-databases-commands") {
      section("Listing databases", "administration-databases-show-databases") {
        p("Available databases can be seen using the `SHOW DATABASES`.")
        query("SHOW DATABASES", assertDatabaseShown) {
          resultTable()
        }
        considerations(
          """The `status` of the database is the desired status, and might not necessarily reflect the actual status across all members of a cluster.
            |""".stripMargin)
      }
      section("Creating databases", "administration-databases-create-database") {
        p("Databases can be created using the `CREATE DATABASE`.")
        query("CREATE DATABASE customers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of administrative commands.")
          resultTable()
        }
        p("The status of any databases created can be seen using the command `SHOW DATABASES`.")
        query("SHOW DATABASES", assertDatabaseShown) {
          resultTable()
        }
      }
      section("Stopping databases", "administration-databases-stop-database") {
        p("Databases can be stopped using the `STOP DATABASE` command.")
        query("STOP DATABASE customers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of administrative commands.")
          resultTable()
        }
        p("The status of any databases stopped can be seen using the command `SHOW DATABASES`.")
        query("SHOW DATABASES", assertDatabaseShown) {
          resultTable()
        }
      }
      section("Starting databases", "administration-databases-start-database") {
        p("Databases can be started using the `START DATABASE` command.")
        query("START DATABASE customers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of administrative commands.")
          resultTable()
        }
        p("The status of any databases started can be seen using the command `SHOW DATABASES`.")
        query("SHOW DATABASES", assertDatabaseShown) {
          resultTable()
        }
      }
      section("Deleting databases", "administration-databases-drop-database") {
        p("Databases can be deleted using the `DROP DATABASE` command.")
        query("DROP DATABASE customers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of administrative commands.")
          resultTable()
        }
        p("When a database has been deleted, it will no longer show up in the listing provided by the command `SHOW DATABASES`.")
        query("SHOW DATABASES", assertDatabaseShown) {
          resultTable()
        }
      }
    }
  }.build()

  private def assertDatabaseShown = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read() )
    try {
      val dbNodes = tx.findNodes(Label.label("Database")).asScala.toList
      val dbNames = dbNodes.map(n => n.getProperty("name"))
      dbNames should equal(p.columnAs[String]("name").toList)
    } finally tx.close()
  })
}
