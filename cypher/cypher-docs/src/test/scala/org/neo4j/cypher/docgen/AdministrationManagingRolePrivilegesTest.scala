package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AdministrationManagingRolePrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing role privileges", "administration-managing-role-privileges")
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
    synopsis("This section explains how to use Cypher to manage privileges necessary in order to manage roles.")
    p(
      """
        |* <<administration-managing-role-privileges-introduction, Introduction>>
        |* <<administration-managing-role-privileges-syntax, Syntax>>
        |* <<administration-managing-role-privileges-examples, Examples>>
        |""".stripMargin)
    section("Introduction", "administration-managing-role-privileges-introduction") {
      p("""
        |The privileges for role management are assignable using Cypher administrative commands.
        |They can be granted, denied and revoked like other privileges.
        |A user must be assigned the `admin` role in order to perform the commands described in this section.
        |""".stripMargin)
    }
    section("Syntax", "administration-managing-role-privileges-syntax") {
      p("include::managing-role-privileges/role-management-privilege-syntax.asciidoc[]")
    }

    section("Examples", "administration-managing-role-privileges-examples") {
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
