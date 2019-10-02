package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityPrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Database, Graph and Sub-graph Access Control", "administration-security-subgraph")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUser",
      "GRANT ROLE regularUser TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUser"
    )
    synopsis("This section explains how to use Cypher to manage privileges for Neo4j role-based access control and fine-grained security.")
    p(
      """
        |Privileges control the access rights to graph elements using a combined whitelist/blacklist mechanism.
        |It is possible to grant access, or deny access, or both.
        |The user will be able to access the resource if they have a grant (whitelist) and do not have a deny (blacklist) relevant to that resource.
        |If there are no read-privileges provided at all, then the user will be denied access to the entire graph, and this will generate an error.
        |All other combinations of GRANT and DENY will result in the matching subgraph being visible.
        |It will appear to the user as if they have a smaller database (smaller graph).
        |""".stripMargin)
    section("The GRANT, DENY and REVOKE commands", "administration-security-subgraph-introduction") {
      p("include::grant-deny-syntax.asciidoc[]")
      p("image::grant-privileges-graph.png[title=\"GRANT and DENY Syntax\"]")
    }
    section("Listing privileges", "administration-security-subgraph-show") {
      p("Available privileges for a particular role can be seen using `SHOW  PRIVILEGES`.")
      query("SHOW ROLE regularUser PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
        p("Lists all privileges")
        resultTable()
      }
    }
    section("The TRAVERSE privilege", "administration-security-subgraph-traverse") {
      p("Users can be granted the right to find nodes and relationships using the `GRANT TRAVERSE` privilege.")
      p("include::grant-traverse-syntax.asciidoc[]")
      p("For example, we can allow the user `jake`, who has role 'regularUser' to find all nodes with the label `Post`.")
      query("GRANT TRAVERSE ON GRAPH neo4j NODES Post TO regularUser", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        p("Nothing is returned from this query, except the count of system database changes made.")
        resultTable()
      }
      p("The privileges granted to the `regularUser` role will appear on the list provided by `SHOW PRIVILEGES`.")
      query("SHOW ROLE regularUser PRIVILEGES", assertPrivilegeShown(Seq(Map("grant" -> "GRANTED", "segment" -> "NODE(Post)", "role" -> "regularUser")))) {
        resultTable()
      }
    }
    section("The READ privilege", "administration-security-subgraph-read") {
      p("The READ privilege")
    }
    section("The MATCH privilege", "administration-security-subgraph-match") {
      p("The MATCH privilege")
    }
    section("The REVOKE command", "administration-security-subgraph-revoke") {
      p("The REVOKE command")
    }
  }.build()

  private def assertPrivilegeShown(expected: Seq[Map[String, AnyRef]]) = ResultAndDbAssertions((p, db) => {
    println(p.resultAsString)
    println(s"Searching for $expected")
    val found = p.toList.filter { row =>
      println(s"Checking row: $row")
      val m = expected.filter { expectedRow =>
        expectedRow.forall {
          case (k, v) => row.contains(k) && row(k) == v
        }
      }
      println(s"\tmatched: $m")
      m.nonEmpty
    }
    found.nonEmpty
  })
}
