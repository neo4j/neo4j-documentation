package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityAdministrationDBMSTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing DBMS privileges", "administration-managing-dbms-privileges")
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
    synopsis("This section explains how to use Cypher to manage DBMS privileges.")
    p(
      """
        |* <<administration-security-administration-dbms-introduction, Introduction>>
        |* <<administration-security-administration-dbms-custom, Using a custom role to manage DBMS privileges>>
        |* <<administration-security-administration-dbms-privileges-role-management, The DBMS `ROLE MANAGEMENT` privileges>>
        |""".stripMargin)
    section("Introduction", "administration-security-administration-dbms-privileges") {
    p(
      """All of the commands described in <<administration>> require that the user who is executing the commands has the privileges to do so.
        |These privileges can be assigned either by granting the user the `admin` role, which enables all administrative rights, or by granting specific combinations of privileges.
        |""".stripMargin)
      p(
        """All DBMS privileges are relevant system-wide.
          |Like user management, they do not belong to one specific database or graph.
          |For more details on the differences between graphs, databases and the DBMS, refer to <<neo4j-databases-graphs>>.""".stripMargin)
      p("include::admin-role-dbms.asciidoc[]")

      section("Using a custom role to manage DBMS privileges", "administration-security-administration-dbms-custom") {
        p("include::admin-role-dbms-custom.asciidoc[]")
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

      section("The DBMS `ROLE MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-role-management") {
        p("The dbms privileges for role management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms-role-management-syntax.asciidoc[]")

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
