package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.exceptions.SyntaxException

class SecurityPrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Graph and sub-graph access control", "administration-security-subgraph")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "CREATE ROLE noAccessUsers",
      "GRANT ROLE regularUsers TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUsers",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage privileges for Neo4j role-based access control and fine-grained security.")

    p(
      """
        |* <<administration-security-subgraph-introduction, The `GRANT`, `DENY` and `REVOKE` commands>>
        |* <<administration-security-subgraph-show, Listing privileges>>
        |* <<administration-security-subgraph-traverse, The `TRAVERSE` privilege>>
        |* <<administration-security-subgraph-read, The `READ` privilege>>
        |* <<administration-security-subgraph-match, The `MATCH` privilege>>
        |* <<administration-security-subgraph-write, The `WRITE` privilege>>
        |* <<administration-security-subgraph-revoke, The `REVOKE` command>>
        |""".stripMargin)

    p(
      """
        |Privileges control the access rights to graph elements using a combined whitelist/blacklist mechanism.
        |It is possible to grant access, or deny access, or a combination of the two.
        |The user will be able to access the resource if they have a grant (whitelist) and do not have a deny (blacklist) relevant to that resource.
        |All other combinations of `GRANT` and `DENY` will result in the matching path being invisible.
        |It will appear to the user as if they have a smaller database (smaller graph).
        |""".stripMargin)
    note {
      p(
        """If a user was not also provided with the database `ACCESS` privilege then access to the entire database will be denied.
          |Information about the database access privilege can be found in <<administration-security-administration-database-access, The ACCESS privilege>>.
          |""".stripMargin)
    }
    section("The `GRANT`, `DENY` and `REVOKE` commands", "administration-security-subgraph-introduction", "enterprise-edition") {
      p("include::grant-deny-syntax.asciidoc[]")
      p("image::grant-privileges-graph.png[title=\"GRANT and DENY Syntax. The `{` and `}` are part of the syntax and not used for grouping.\"]")
      // image source: https://docs.google.com/drawings/d/10PrJ2xb0fvT0I_i5P0thmSEReIcsZD8cJqJMV7FS7yg/edit?usp=sharing
      p("The below image shows the hierarchy between the different graph privileges.")
      p("image::privilege-hierarchy-graph.png[title=\"Graph privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/1bChOmVPoIhJO4ygyngZ06K4eXIbvGUDZ6BBAi0qdskA/edit?usp=sharing
    }
    section("Listing privileges", "administration-security-subgraph-show", "enterprise-edition") {
      p("Available privileges for all roles can be seen using `SHOW PRIVILEGES`.")
      query("SHOW PRIVILEGES", assertPrivilegeShown(Seq(
        Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers"),
        Map("access" -> "DENIED", "action" -> "access", "role" -> "noAccessUsers")
      ))) {
        p(
          """Lists all privileges for all roles.
            |The table contains columns describing the privilege:
            |
            |* access: whether the privilege is granted or denied (whitelist or blacklist)
            |* action: which type of privilege this is: access, traverse, read, write, token, schema or admin
            |* resource: what type of scope this privilege applies to: the entire dbms, a database, a graph or sub-graph access
            |* graph: the specific database or graph this privilege applies to
            |* segment: for sub-graph access control, this describes the scope in terms of labels or relationship types
            |* role: the role the privilege is granted to
            |""".stripMargin)
        resultTable()
      }

      p("Available privileges for a particular role can be seen using `SHOW ROLE name PRIVILEGES`.")
      query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
        Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers")
      ))) {
        p("Lists all privileges for role 'regularUsers'")
        resultTable()
      }

      p("Available privileges for a particular user can be seen using `SHOW USER name PRIVILEGES`.")
      note {
        p("Please note that if a non-native auth provider like LDAP is in use, `SHOW USER PRIVILEGES` will only work in a limited capacity; " +
          "It is only possible for a user to show their own privileges. Other users' privileges cannot be listed when using a non-native auth provider.")
      }
      query("SHOW USER jake PRIVILEGES", assertPrivilegeShown(Seq(
        Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers", "user" -> "jake")
      ))) {
        p("Lists all privileges for user 'jake'")
        resultTable()
      }
    }

    section("The `TRAVERSE` privilege", "administration-security-subgraph-traverse", "enterprise-edition") {
      p("Users can be granted the right to find nodes and relationships using the `GRANT TRAVERSE` privilege.")
      p("include::grant-traverse-syntax.asciidoc[]")
      p("For example, we can allow the user `jake`, who has role 'regularUsers' to find all nodes with the label `Post`.")
      query("GRANT TRAVERSE ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      p("The `TRAVERSE` privilege can also be denied.")
      p("include::deny-traverse-syntax.asciidoc[]")
      p("For example, we can disallow the user `jake`, who has role 'regularUsers' to find all nodes with the label `Payments`.")
      query("DENY TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }

    section("The `READ` privilege", "administration-security-subgraph-read", "enterprise-edition") {
      p(
        """Users can be granted the right to do property reads on nodes and relationships using the `GRANT READ` privilege.
          |It is very important to note that users can only read properties on entities that they are allowed to find in the first place.""".stripMargin)
      p("include::grant-read-syntax.asciidoc[]")

      p(
        """For example, we can allow the user `jake`, who has role 'regularUsers' to read all properties on nodes with the label `Post`.
          |The `*` implies that the ability to read all properties also extends to properties that might be added in the future.""".stripMargin)
      query("GRANT READ { * } ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      p("The `READ` privilege can also be denied.")
      p("include::deny-read-syntax.asciidoc[]")

      p("Although we just granted the user 'jake' the right to read all properties, we may want to hide the `secret` property. The following example shows how to do that.")
      query("DENY READ { secret } ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }
    section("The `MATCH` privilege", "administration-security-subgraph-match", "enterprise-edition") {
      p("As a shorthand for `TRAVERSE` and `READ`, users can be granted the right to find and do property reads on nodes and relationships using the `GRANT MATCH` privilege. ")
      p("include::grant-match-syntax.asciidoc[]")

      p(
        """For example if you want to grant the ability to read the properties `language` and `length` for nodes with the label `Message`,
          |as well as the ability to find these nodes, to a role `regularUsers` you can use the following `GRANT MATCH` query.""".stripMargin)

      query("GRANT MATCH { language, length } ON GRAPH neo4j NODES Message TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 3)
      })) {
        statsOnlyResultTable()
      }

      p("""Like all other privileges, the `MATCH` privilege can also be denied.""".stripMargin)
      p("include::deny-match-syntax.asciidoc[]")

      p(
        """Please note that the effect of denying a `MATCH` privilege depends on whether concrete property keys are specified or a `+*+`.
          |If you specify concrete property keys then `DENY MATCH` will only deny reading those properties. Finding the elements to traverse would still be allowed.
          |If you specify `+*+` instead then both traversal of the element and all property reads will be disallowed.
          |The following queries will show examples for this.""".stripMargin)

      p(
        """Denying to read the property ´content´ on nodes with the label `Message` for the role `regularUsers` would look like the following query.
          |Although not being able to read this specific property, nodes with that label can still be traversed (and, depending on other grants, other properties on it could still be read).""".stripMargin)

      query("DENY MATCH { content } ON GRAPH neo4j NODES Message TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      p("The following query exemplifies how it would look like if you want to deny both reading all properties and traversing nodes labeled with `Account`.")

      query("DENY MATCH { * } ON GRAPH neo4j NODES Account TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      note {
        p("Please note that `REVOKE MATCH` is not allowed, instead revoke the individual `READ` and `TRAVERSE` privileges.")
      }
    }

    section("The `WRITE` privilege", "administration-security-subgraph-write", "enterprise-edition") {
      p(
        """The `WRITE` privilege enables you to write on a graph.""".stripMargin)
      p("include::grant-write-syntax.asciidoc[]")

      p(
        """For example, granting the ability to write on the graph `neo4j` to the role `regularUsers` would be achieved using:""".stripMargin)
      query("GRANT WRITE ON GRAPH neo4j TO regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("Unlike with `GRANT READ` it is not possible to restrict `WRITE` privileges to specific ELEMENTS, NODES or RELATIONSHIPS.")
      }
      p("For example, using `NODES A` will produce a syntax error.")
      query("GRANT WRITE ON GRAPH neo4j NODES A TO regularUsers", assertSyntaxException("The use of ELEMENT, NODE or RELATIONSHIP with the WRITE privilege is not supported")
      ) {
        errorOnlyResultTable()
      }

      p("The `WRITE` privilege can also be denied.")
      p("include::deny-write-syntax.asciidoc[]")

      p("For example, denying the ability to write on the graph `neo4j` to the role `regularUsers` would be achieved using:")
      query("DENY WRITE ON GRAPH neo4j TO regularUsers", ResultAssertions((r) => {
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

    section("The `REVOKE` command", "administration-security-subgraph-revoke", "enterprise-edition") {
      initQueries(
        "GRANT TRAVERSE ON GRAPH neo4j NODES Post TO regularUsers",
        "GRANT TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers",
        "DENY TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers",
        "CREATE ROLE indexUsers",
        "GRANT CREATE INDEX ON DATABASE * TO indexUsers",
        "GRANT DROP INDEX ON DATABASE * TO indexUsers"
      )
      p("Privileges that were granted or denied earlier can be revoked using the `REVOKE` command. ")
      p("include::revoke-syntax.asciidoc[]")

      note {
        p("Please note that `REVOKE MATCH` is not allowed, instead revoke the individual `READ` and `TRAVERSE` privileges.")
      }

      p("An example usage of the `REVOKE` command is given here:")
      query("REVOKE GRANT TRAVERSE ON GRAPH neo4j NODES Post FROM regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p(
        """While it can be explicitly specified that revoke should remove a `GRANT` or `DENY`, it is also possible to revoke either one by not specifying at all as the next example demonstrates.
          |Because of this, if there happen to be a `GRANT` and a `DENY` on the same privilege, it would remove both.""".stripMargin)
      query("REVOKE TRAVERSE ON GRAPH neo4j NODES Payments FROM regularUsers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p(
        """Some privileges are compound privileges and contains sub-privileges, for example <<administration-security-administration-database-indexes, `INDEX MANAGEMENT`>> which covers `CREATE INDEX` and `DROP INDEX`.
          |When these compound privileges are revoked, all sub-privileges matching the revoke command will also be revoked as shown in the example below.""".stripMargin)
      p("include::grant-create-drop-index-syntax.asciidoc[]")
      query("REVOKE INDEX MANAGEMENT ON DATABASE * FROM indexUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }
      query("SHOW ROLE indexUsers PRIVILEGES", ResultAssertions(r => {
        assertStats(r) // role has no privileges
      })) {
        p("Both the `CREATE INDEX` and `DROP INDEX` privileges have been revoked:")
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

  private def assertSyntaxException(expected: String) = ErrorAssertions {
    case s: SyntaxException => s.getMessage should startWith(expected)
    case _ => fail("Expected exception: SyntaxException($expected)")
  }
}
