package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityWritePrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Write privileges", "administration-security-writes")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "GRANT ROLE regularUsers TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUsers"
    )
    synopsis("This section explains how to use Cypher to manage write privileges on graphs.")

    p(
      """
        |* <<administration-security-writes-create, The `CREATE` privilege>>
        |* <<administration-security-writes-delete, The `DELETE` privilege>>
        |* <<administration-security-writes-set-label, The `SET LABEL` privilege>>
        |* <<administration-security-writes-remove-label, The `REMOVE LABEL` privilege>>
        |* <<administration-security-writes-set-property, The `SET PROPERTY` privilege>>
        |* <<administration-security-writes-merge, The `MERGE` privilege>>
        |* <<administration-security-writes-write, The `WRITE` privilege>>
        |* <<administration-security-writes-all, The `ALL GRAPH PRIVILEGES` privilege>>
        |""".stripMargin)

        p(
      """
        |Write privileges are defined for different parts of the graph:
        |
        |* `CREATE` - allows creating nodes and relationships.
        |* `DELETE` - allows deleting nodes and relationships.
        |* `SET LABEL labels` - allows setting the specified node labels using the `SET` clause.
        |* `REMOVE LABEL labels` - allows removing the specified node labels using the `REMOVE` clause.
        |* `SET PROPERTY +{props}+` - allows setting properties on nodes and relationships.
        |
        |There are also compound privileges which combine the above specific privileges:
        |
        |* `MERGE` - allows match, create and set property to permit the `MERGE` command.
        |* `WRITE` - allows all write operations on an entire graph.
        |* `ALL GRAPH PRIVILEGES` - allows all read and write operation on an entire graph.
        |""".stripMargin)

    section("The `CREATE` privilege", "administration-security-writes-create", "enterprise-edition") {
      p(
        """The `CREATE` privilege allows a user to create new node and relationship elements in a graph.
          |See the Cypher <<query-create, CREATE>> clause.""".stripMargin)
      p("include::grant-create-syntax.asciidoc[]")

      p(
        """For example, granting the ability to create elements on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT CREATE ON GRAPH neo4j ELEMENTS * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p("The `CREATE` privilege can also be denied.")
      p("include::deny-create-syntax.asciidoc[]")

      p("For example, denying the ability to create nodes with the label `foo` on all graphs to the role `regularUsers` would be achieved using:")
      query("DENY CREATE ON GRAPH * NODES foo TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p(
          """If the user attempts to create nodes with a label that does not already exist in the database, then the user must also possess the
            |<<administration-security-administration-database-tokens, CREATE NEW LABEL>> privilege. The same applies to new relationships - the
            |<<administration-security-administration-database-tokens, CREATE NEW RELATIONSHIP TYPE>> privilege is required.""".stripMargin)
      }
    }

    section("The `DELETE` privilege", "administration-security-writes-delete", "enterprise-edition") {
      p(
        """The `DELETE` privilege allows a user to delete node and relationship elements in a graph.
          |See the Cypher <<query-delete, DELETE>> clause.""".stripMargin)
      p("include::grant-delete-syntax.asciidoc[]")

      p(
        """For example, granting the ability to delete elements on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT DELETE ON GRAPH neo4j ELEMENTS * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p("The `DELETE` privilege can also be denied.")
      p("include::deny-delete-syntax.asciidoc[]")

      p("For example, denying the ability to delete relationships with the relationship type `bar` on all graphs to the role `regularUsers` would be achieved using:")
      query("DENY DELETE ON GRAPH * RELATIONSHIPS bar TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p(
          """Users with `DELETE` privilege, but restricted `TRAVERSE` privileges, will not be able to do `DETACH DELETE` in all cases.
            | See <<operations-manual#detach-delete-restricted-user, Operations Manual -> Fine-grained access control>> for more info.""".stripMargin)
      }
    }

    section("The `SET LABEL` privilege", "administration-security-writes-set-label", "enterprise-edition") {
      p(
        """The `SET LABEL` privilege allows you to set labels on a node using the <<set-set-a-label-on-a-node, SET clause>>.""".stripMargin)
      p("include::grant-set-label-syntax.asciidoc[]")

      p(
        """For example, granting the ability to set any label on nodes of the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT SET LABEL * ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("Unlike many of the other read and write privileges, it is not possible to restrict the `SET LABEL` privilege to specific ELEMENTS, NODES or RELATIONSHIPS.")
      }

      p("The `SET LABEL` privilege can also be denied.")
      p("include::deny-set-label-syntax.asciidoc[]")

      p("For example, denying the ability to set the label `foo` on nodes of all graphs to the role `regularUsers` would be achieved using:")
      query("DENY SET LABEL foo ON GRAPH * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("If no instances of this label exist in the database, then the <<administration-security-administration-database-tokens, CREATE NEW LABEL>> privilege is also required.")
      }
    }

    section("The `REMOVE LABEL` privilege", "administration-security-writes-remove-label", "enterprise-edition") {
      p(
        """The `REMOVE LABEL` privilege allows you to remove labels from a node using the <<remove-remove-a-label-from-a-node, REMOVE clause>>.""".stripMargin)
      p("include::grant-remove-label-syntax.asciidoc[]")

      p(
        """For example, granting the ability to remove any label from nodes of the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT REMOVE LABEL * ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("Unlike many of the other read and write privileges, it is not possible to restrict the `REMOVE LABEL` privilege to specific ELEMENTS, NODES or RELATIONSHIPS.")
      }

      p("The `REMOVE LABEL` privilege can also be denied.")
      p("include::deny-remove-label-syntax.asciidoc[]")

      p("For example, denying the ability to remove the label `foo` from nodes of all graphs to the role `regularUsers` would be achieved using:")
      query("DENY REMOVE LABEL foo ON GRAPH * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }

    section("The `SET PROPERTY` privilege", "administration-security-writes-set-property", "enterprise-edition") {
      p(
        """The `SET PROPERTY` privilege allows a user to set a property on a node or relationship element in a graph using the <<set-set-a-property, SET clause>>.""".stripMargin)
      p("include::grant-set-property-syntax.asciidoc[]")

      p(
        """For example, granting the ability to set any property on all elements of the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT SET PROPERTY {*} ON DEFAULT GRAPH ELEMENTS * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p("The `SET PROPERTY` privilege can also be denied.")
      p("include::deny-set-property-syntax.asciidoc[]")

      p("For example, denying the ability to set the property `foo` on nodes with the label `bar` on all graphs to the role `regularUsers` would be achieved using:")
      query("DENY SET PROPERTY { foo } ON GRAPH * NODES bar TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p(
          """If the users attempts to set a property with a property name that does not already exist in the database the user must also possess the
            |<<administration-security-administration-database-tokens, CREATE NEW PROPERTY NAME>> privilege.""".stripMargin)
      }
    }

    section("The `MERGE` privilege", "administration-security-writes-merge", "enterprise-edition") {
      p(
        """The `MERGE` privilege is a compound privilege that combines `TRAVERSE` and `READ` (i.e. `MATCH`) with `CREATE` and `SET PROPERTY`. This is intended to
          | permit use of <<query-merge, the MERGE command>> but is applicable to all reads and writes that require these privileges.""".stripMargin)
      p("include::grant-merge-syntax.asciidoc[]")

      p(
        """For example, granting `MERGE` on all elements of the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT MERGE {*} ON GRAPH neo4j ELEMENTS * TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p(
        """It is not possible to deny the `MERGE` privilege. If it is desirable to prevent a users from creating elements and setting properties, use
          |<<administration-security-writes-create, DENY CREATE>> or <<administration-security-writes-set-property,DENY SET PROPERTY>>.""".stripMargin)

      note {
        p(
          """If the users attempts to create nodes with a label that does not already exist in the database the user must also possess the
            |<<administration-security-administration-database-tokens, CREATE NEW LABEL>> privilege. The same applies to new relationships and properties - the
            |<<administration-security-administration-database-tokens, CREATE NEW RELATIONSHIP TYPE>> or
            |<<administration-security-administration-database-tokens, CREATE NEW PROPERTY NAME>> privileges are required.""".stripMargin)
      }
    }

    section("The `WRITE` privilege", "administration-security-writes-write", "enterprise-edition") {
      p(
        """The `WRITE` privilege allows the user to execute any write command on a graph.""".stripMargin)
      p("include::grant-write-syntax.asciidoc[]")

      p(
        """For example, granting the ability to write on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT WRITE ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }
      note {
        p(
          """Unlike the more specific write commands, it is not possible to restrict `WRITE` privileges to specific ELEMENTS, NODES or RELATIONSHIPS.
            |If it is desirable to prevent a user from writing to a subset of database objects, a `GRANT WRITE` can be combined with more specific
            |`DENY` commands to target these elements.""".stripMargin)
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

    section("`ALL GRAPH PRIVILEGES`", "administration-security-writes-all", "enterprise-edition") {
      p(
        """The `ALL GRAPH PRIVILEGES` privilege allows the user to execute any command on a graph.""".stripMargin)
      p("include::grant-all-graph-privileges-syntax.asciidoc[]")

      p(
        """For example, granting all graph privileges on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT ALL GRAPH PRIVILEGES ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("""
        |Unlike the more specific read and write commands, it is not possible to restrict `ALL GRAPH PRIVILEGES` privileges to specific ELEMENTS,
        |NODES or RELATIONSHIPS. If it is desirable to prevent a user from reading or writing to a subset of database objects,
        |a `GRANT ALL GRAPH PRIVILEGES` can be combined with more specific `DENY` commands to target these elements.""".stripMargin)
      }

      p("The `ALL GRAPH PRIVILEGES` privilege can also be denied.")
      p("include::deny-all-graph-privileges-syntax.asciidoc[]")

      p("For example, denying all graph privileges on the graph `neo4j` to the role `regularUsers` would be achieved using:")
      query("DENY ALL GRAPH PRIVILEGES ON GRAPH neo4j TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }
  }.build()

}
