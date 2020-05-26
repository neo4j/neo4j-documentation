package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityWritePrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Fine-grained writes", "administration-security-writes")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "GRANT ROLE regularUsers TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUsers"
    )
    synopsis("This section explains how to use Cypher to manage privileges for Neo4j fine-grained write security.")

    p(
      """
        |* <<administration-security-writes-write, The `WRITE` privilege>>
        |""".stripMargin)

        p(
      """
        |There are several separate fine-grained write privileges:
        |
        |* `WRITE` - this privilege can only be assigned to all nodes, relationships, and properties in the entire graph.
        |""".stripMargin)

    section("The `WRITE` privilege", "administration-security-writes-write", "enterprise-edition") {
      p(
        """The `WRITE` privilege enables you to write on a graph.""".stripMargin)
      p("include::grant-write-syntax.asciidoc[]")

      p(
        """For example, granting the ability to write on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT WRITE ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("Unlike with `GRANT READ` it is not possible to restrict `WRITE` privileges to specific ELEMENTS, NODES or RELATIONSHIPS.")
      }

      p("The `WRITE` privilege can also be denied.")
      p("include::deny-write-syntax.asciidoc[]")

      p("For example, denying the ability to write on the graph `neo4j` to the role `regularUsers` would be achieved using:")
      query("DENY WRITE ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }
      note {
        p(
          """Users with `WRITE` privilege but restricted `TRAVERSE` privileges will not be able to do `DETACH DELETE` in all cases.
            | See <<operations-manual#detach-delete-restricted-user, Operations Manual -> Fine-grained access control>> for more info.""".stripMargin)
      }
    }
  }.build()

}
