package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AdministrationManagingDatabasePrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing database privileges", "administration-managing-database-privileges")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "CREATE ROLE noAccessUsers",
      "CREATE ROLE roleAdder",
      "CREATE ROLE roleDropper",
      "CREATE ROLE roleAssigner",
      "CREATE ROLE roleRemover",
      "CREATE ROLE roleShower",
      "CREATE ROLE roleManager",
      "GRANT ROLE regularUsers TO jake",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage database privileges, i.e. the privileges to access, start and stop the database, as well as the privileges to manage indexes and constraints and the ability to create new node labels, relationship types and properties.")
    p(
      """
        |* <<administration-managing-database-privileges-introduction, Introduction>>
        |* <<administration-managing-database-privileges-syntax, Syntax>>
        |* <<administration-managing-database-privileges-examples, Examples>>
        |** <<administration-managing-database-privileges-examples-access, The database `ACCESS` privilege>>
        |** <<administration-managing-database-privileges-examples-startstop, The database `START`/`STOP` privileges>>
        |** <<administration-managing-database-privileges-examples-indexes, The `INDEX MANAGEMENT` privileges>>
        |** <<administration-managing-database-privileges-examples-constraints, The `CONSTRAINT MANAGEMENT` privileges>>
        |** <<administration-managing-database-privileges-examples-tokens, The `NAME MANAGEMENT` privileges>>
        |** <<administration-managing-database-privileges-examples-all, Granting all database administration privileges>>
        |""".stripMargin)

    section("Introduction", "administration-managing-database-privileges-introduction") {
    p(
      """All of the commands described in the <<administration, Administration>> chapter require that the user executing the commands has the privileges to do so.
        |These privileges can be assigned either by granting the `admin` role to the user, which enables all administrative rights, or by granting specific combinations of privileges.
        |A user must be assigned the `admin` role, or a copy of this role, in order to perform the commands described in this section.
        |""".stripMargin)
    }
    section("Syntax", "administration-managing-database-privileges-syntax") {
      p("include::managing-database-privileges-syntax.adoc[]")
    }
    section("Examples", "administration-managing-database-privileges-examples") {
      section("The database `ACCESS` privilege", "administration-managing-database-privileges-examples-access") {
        p(
          """The `ACCESS` privilege can be used to enable the ability to access a database.
            |If this is not granted to users, they will not even be able to start transactions on the relevant database.""".stripMargin)
        p("include::database/grant-database-access-syntax.asciidoc[]")

        p(
          """For example, granting the ability to access the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `ACCESS` privilege can also be denied.")
        p("include::database/deny-database-access-syntax.asciidoc[]")

        p("For example, denying the ability to access to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access"),
          Map("access" -> "DENIED", "action" -> "access")
        ))) {
          resultTable()
        }
      }
      section("The database `START`/`STOP` privileges", "administration-managing-database-privileges-examples-startstop") {
        p(
          """The `START` privilege can be used to enable the ability to start a database.""".stripMargin)
        p("include::database/grant-database-start-syntax.asciidoc[]")

        p(
          """For example, granting the ability to start the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT START ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `START` privilege can also be denied.")
        p("include::database/deny-database-start-syntax.asciidoc[]")

        p("For example, denying the ability to start to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY START ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p(
          """The `STOP` privilege can be used to enable the ability to stop a database.""".stripMargin)
        p("include::database/grant-database-stop-syntax.asciidoc[]")

        p(
          """For example, granting the ability to stop the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT STOP ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `STOP` privilege can also be denied.")
        p("include::database/deny-database-stop-syntax.asciidoc[]")

        p("For example, denying the ability to stop to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY STOP ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access"),
          Map("access" -> "DENIED", "action" -> "access"),
          Map("access" -> "GRANTED", "action" -> "start_database"),
          Map("access" -> "DENIED", "action" -> "start_database"),
          Map("access" -> "GRANTED", "action" -> "stop_database"),
          Map("access" -> "DENIED", "action" -> "stop_database")
        ))) {
          resultTable()
        }
      }
      section("The `INDEX MANAGEMENT` privileges", "administration-managing-database-privileges-examples-indexes") {
        p(
          """Indexes can be created or deleted with the `CREATE INDEX` and `DROP INDEX` commands.
            |The privilege to do this can be granted with `GRANT CREATE INDEX` and `GRANT DROP INDEX` commands.""".stripMargin)
        p("include::database/index-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create indexes on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE INDEX ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("The `CONSTRAINT MANAGEMENT` privileges", "administration-managing-database-privileges-examples-constraints") {
        p(
          """Constraints can be created or deleted with the `CREATE CONSTRAINT` and `DROP CONSTRAINT` commands.
            |The privilege to do this can be granted with `GRANT CREATE CONSTRAINT` and `GRANT DROP CONSTRAINT` commands.""".stripMargin)
        p("include::database/constraint-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create constraints on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE CONSTRAINT ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("The `NAME MANAGEMENT` privileges", "administration-managing-database-privileges-examples-tokens") {
        p(
          """The right to create new labels, relationship types or propery names is different from the right to create nodes, relationships or properties.
            |The latter is managed using database `WRITE` privileges, while the former is managed using specific `GRANT/DENY CREATE NEW ...` commands for each type.""".stripMargin)
        p("include::database/name-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create new properties on nodes or relationships in the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE NEW PROPERTY NAME ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("Granting all database administration privileges", "administration-managing-database-privileges-examples-all") {
        p(
          """Conferring the right to perform all of the above tasks can be achieved with a single command:""".stripMargin)
        p("include::database/all-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to access, start and stop all databases and create indexes, constraints, labels, relationship types and property names on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT ALL DATABASE PRIVILEGES ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 4)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "start_database", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "stop_database", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "create_index", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "drop_index", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "create_constraint", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "drop_constraint", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "create_label", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "create_reltype", "role" -> "regularUsers"),
          Map("access" -> "GRANTED", "action" -> "create_propertykey", "role" -> "regularUsers")
        ))) {
          resultTable()
        }
      }
    }
  }.build()

  private def assertPrivilegeShown(expected: Seq[Map[String, AnyRef]]) = ResultAssertions(p => {
    val found = p.toList.filter { row =>
      val m = expected.filter { expectedRow =>
        expectedRow.forall {
          case (k, v) => row.contains(k) && row(k) == v
        }
      }
      m.nonEmpty
    }
    found.nonEmpty should be(true)
  })
}
