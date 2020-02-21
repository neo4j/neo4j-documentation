package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityAdministrationTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Security of administration", "administration-security-administration")
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
      "CREATE ROLE userAdder",
      "CREATE ROLE userDropper",
      "CREATE ROLE userModifier",
      "CREATE ROLE userShower",
      "CREATE ROLE userManager",
      "GRANT ROLE regularUsers TO jake",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage Neo4j administrative privileges.")
    p(
      """All of the commands described in the enclosing <<administration, Administration>> section require that the user executing the commands has the rights to do so.
        |These privileges can be conferred either by granting the user the `admin` role, which enables all administrative rights, or by granting specific combinations of privileges.
        |""".stripMargin)
    p(
      """
        |* <<administration-security-administration-introduction, The `admin` role>>
        |* <<administration-security-administration-database-privileges, Database administration>>
        |** <<administration-security-administration-database-access, The database `ACCESS` privilege>>
        |** <<administration-security-administration-database-startstop, The database `START`/`STOP` privileges>>
        |** <<administration-security-administration-database-indexes, The `INDEX MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-constraints, The `CONSTRAINT MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-tokens, The `NAME MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-all, Granting all database administration privileges>>
        |* <<administration-security-administration-dbms-privileges, DBMS administration>>
        |** <<administration-security-administration-dbms-custom, Using a custom role to manage DBMS privileges>>
        |** <<administration-security-administration-dbms-privileges-role-management, The dbms `ROLE MANAGEMENT` privileges>>
        |** <<administration-security-administration-dbms-privileges-user-management, The dbms `USER MANAGEMENT` privileges>>
        |""".stripMargin)
    section("The `admin` role", "administration-security-administration-introduction", "enterprise-edition") {
      p("include::admin-role-introduction.asciidoc[]")
      query("SHOW ROLE admin PRIVILEGES", assertPrivilegeShown(Seq(
        Map("access" -> "GRANTED", "action" -> "access")
      ))) {
        resultTable()
      }
    }
    section("Database administration", "administration-security-administration-database-privileges", "enterprise-edition") {
      synopsis("This section explains how to use Cypher to manage privileges for Neo4j database administrative rights.")
      p("include::database/admin-role-database.asciidoc[]")
      p("include::database/admin-database-syntax.asciidoc[]")
      p("image::grant-privileges-database.png[title=\"Syntax of GRANT and DENY Database Privileges\"]")
      // image source: https://docs.google.com/drawings/d/1Zc5eawW58r4CI8e4FeYnA8cwUyIJX-7B4Eecj-qkd_8/edit
      section("The database `ACCESS` privilege", "administration-security-administration-database-access", "enterprise-edition") {
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
      section("The database `START`/`STOP` privileges", "administration-security-administration-database-startstop", "enterprise-edition") {
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
      section("The `INDEX MANAGEMENT` privileges", "administration-security-administration-database-indexes", "enterprise-edition") {
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
      section("The `CONSTRAINT MANAGEMENT` privileges", "administration-security-administration-database-constraints", "enterprise-edition") {
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
      section("The `NAME MANAGEMENT` privileges", "administration-security-administration-database-tokens", "enterprise-edition") {
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
      section("Granting all database administration privileges", "administration-security-administration-database-all", "enterprise-edition") {
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

    section("DBMS administration", "administration-security-administration-dbms-privileges", "enterprise-edition") {
      p(
        """All DBMS privileges are relevant system-wide. Like user management, they do not belong to one specific database or graph.
          |For more details on the differences between graphs, databases and the DBMS, refer to <<neo4j-databases-graphs>>.""".stripMargin)
      p("include::dbms/admin-role-dbms.asciidoc[]")

      section("Using a custom role to manage DBMS privileges", "administration-security-administration-dbms-custom", "enterprise-edition") {
        p("include::dbms/admin-role-dbms-custom.asciidoc[]")
        p("First we copy the 'admin' role:")
        query("CREATE ROLE usermanager AS COPY OF admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        p("Then we DENY ACCESS to normal databases:")
        query("DENY ACCESS ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And DENY START and STOP for normal databases:")
        query("DENY START ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("DENY STOP ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And DENY index and constraint management:")
        query("DENY INDEX MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("DENY CONSTRAINT MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        p("And finally DENY label, relationship type and property name:")
        query("DENY NAME MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 3)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting role should have privileges that only allow the DBMS capabilities, like user and role management:")
        query("SHOW ROLE usermanager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'usermanager'")
          resultTable()
        }
      }

      section("The dbms `ROLE MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-role-management", "enterprise-edition") {
        p("The dbms privileges for role management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/role-management-syntax.asciidoc[]")

        p("The ability to add roles can be granted via the `CREATE ROLE` privilege. The following query shows an example of this:")
        query("GRANT CREATE ROLE ON DBMS TO roleAdder", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow adding roles:")
        query("SHOW ROLE roleAdder PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleAdder'")
          resultTable()
        }

        p("The ability to delete roles can be granted via the `DROP ROLE` privilege. The following query shows an example of this:")
        query("GRANT DROP ROLE ON DBMS TO roleDropper", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow deleting roles:")
        query("SHOW ROLE roleDropper PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleDropper'")
          resultTable()
        }

        p("The ability to assign roles to users can be granted via the `ASSIGN ROLE` privilege. The following query shows an example of this:")
        query("GRANT ASSIGN ROLE ON DBMS TO roleAssigner", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow assigning/granting roles:")
        query("SHOW ROLE roleAssigner PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleAssigner'")
          resultTable()
        }

        p("The ability to remove roles from users can be granted via the `REMOVE ROLE` privilege. The following query shows an example of this:")
        query("GRANT REMOVE ROLE ON DBMS TO roleRemover", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow removing/revoking roles:")
        query("SHOW ROLE roleRemover PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleRemover'")
          resultTable()
        }

        p("The ability to show roles can be granted via the `SHOW ROLE` privilege. The following query shows an example of this:")
        query("GRANT SHOW ROLE ON DBMS TO roleShower", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow showing roles:")
        query("SHOW ROLE roleShower PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleShower'")
          resultTable()
        }

        p("All of the above mentioned privileges can be granted via the `ROLE MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT ROLE MANAGEMENT ON DBMS TO roleManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage roles:")
        query("SHOW ROLE roleManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleManager'")
          resultTable()
        }
      }
      section("The dbms `USER MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-user-management", "enterprise-edition") {
        p("The dbms privileges for user management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/user-management-syntax.asciidoc[]")

        p("The ability to add users can be granted via the `CREATE USER` privilege. The following query shows an example of this:")
        query("GRANT CREATE USER ON DBMS TO userAdder", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow adding users:")
        query("SHOW ROLE userAdder PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userAdder'")
          resultTable()
        }

        p("The ability to delete users can be granted via the `DROP USER` privilege. The following query shows an example of this:")
        query("GRANT DROP USER ON DBMS TO userDropper", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow deleting users:")
        query("SHOW ROLE userDropper PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userDropper'")
          resultTable()
        }

        p("The ability to modify users can be granted via the `ALTER USER` privilege. The following query shows an example of this:")
        query("GRANT ALTER USER ON DBMS TO userModifier", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow modifying users:")
        query("SHOW ROLE userModifier PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userModifier'")
          resultTable()
        }

        p("The ability to show users can be granted via the `SHOW USER` privilege. The following query shows an example of this:")
        query("GRANT SHOW USER ON DBMS TO userShower", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow showing users:")
        query("SHOW ROLE userShower PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userShower'")
          resultTable()
        }

        p("All of the above mentioned privileges can be granted via the `USER MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT USER MANAGEMENT ON DBMS TO userManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage users:")
        query("SHOW ROLE userManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userManager'")
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
