package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityReadPrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Fine-grained reads", "administration-security-reads")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "GRANT ROLE regularUsers TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUsers"
    )
    synopsis("This section explains how to use Cypher to manage privileges for Neo4j fine-grained read security.")

    p(
      """
        |* <<administration-security-reads-traverse, The `TRAVERSE` privilege>>
        |* <<administration-security-reads-read, The `READ` privilege>>
        |* <<administration-security-reads-match, The `MATCH` privilege>>
        |""".stripMargin)

    p(
      """
        |There are three separate fine-grained read privileges:
        |
        |* `TRAVERSE` - allows the specified entities to be found.
        |* `READ +{props}+` - allows the specified properties on the found entities to be read.
        |* `MATCH +{props}+` - combines both `TRAVERSE` and `READ`, allowing an entity to be found and its properties read.
        |""".stripMargin)

    section("The `TRAVERSE` privilege", "administration-security-reads-traverse", "enterprise-edition") {
      p("Users can be granted the right to find nodes and relationships using the `GRANT TRAVERSE` privilege.")
      p("include::grant-traverse-syntax.asciidoc[]")
      p("For example, we can allow the user `jake`, who has role 'regularUsers' to find all nodes with the label `Post`.")
      query("GRANT TRAVERSE ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      p("The `TRAVERSE` privilege can also be denied.")
      p("include::deny-traverse-syntax.asciidoc[]")
      p("For example, we can disallow the user `jake`, who has role 'regularUsers' to find all nodes with the label `Payments`.")
      query("DENY TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }

    section("The `READ` privilege", "administration-security-reads-read", "enterprise-edition") {
      p(
        """Users can be granted the right to do property reads on nodes and relationships using the `GRANT READ` privilege.
          |It is very important to note that users can only read properties on entities that they are allowed to find in the first place.""".stripMargin)
      p("include::grant-read-syntax.asciidoc[]")

      p(
        """For example, we can allow the user `jake`, who has role 'regularUsers' to read all properties on nodes with the label `Post`.
          |The `*` implies that the ability to read all properties also extends to properties that might be added in the future.""".stripMargin)
      query("GRANT READ { * } ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      note {
      p(
        """Granting property `READ` access does not imply that the entities with that property can be found.
          |For example, if there is also a `DENY TRAVERSE` present on the same entity as a `GRANT READ`, the entity will not be found by a Cypher `MATCH` statement.
          |""".stripMargin)
      }

      p("The `READ` privilege can also be denied.")
      p("include::deny-read-syntax.asciidoc[]")

      p("Although we just granted the user 'jake' the right to read all properties, we may want to hide the `secret` property. The following example shows how to do that.")
      query("DENY READ { secret } ON GRAPH neo4j NODES Post TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }
    section("The `MATCH` privilege", "administration-security-reads-match", "enterprise-edition") {
      p(
        """Users can be granted the right to find and do property reads on nodes and relationships using the `GRANT MATCH` privilege.
          |This is semantically the same as having both `TRAVERSE` and `READ` privileges.""".stripMargin)
      p("include::grant-match-syntax.asciidoc[]")

      p(
        """For example if you want to grant the ability to read the properties `language` and `length` for nodes with the label `Message`,
          |as well as the ability to find these nodes, to a role `regularUsers` you can use the following `GRANT MATCH` query.""".stripMargin)

      query("GRANT MATCH { language, length } ON GRAPH neo4j NODES Message TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
      }

      p("""Like all other privileges, the `MATCH` privilege can also be denied.""".stripMargin)
      p("include::deny-match-syntax.asciidoc[]")

      p(
        """Please note that the effect of denying a `MATCH` privilege depends on whether concrete property keys are specified or a `*`.
          |If you specify concrete property keys then `DENY MATCH` will only deny reading those properties. Finding the elements to traverse would still be allowed.
          |If you specify `*` instead then both traversal of the element and all property reads will be disallowed.
          |The following queries will show examples for this.""".stripMargin)

      p(
        """Denying to read the property ´content´ on nodes with the label `Message` for the role `regularUsers` would look like the following query.
          |Although not being able to read this specific property, nodes with that label can still be traversed (and, depending on other grants, other properties on it could still be read).""".stripMargin)

      query("DENY MATCH { content } ON GRAPH neo4j NODES Message TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }

      p("The following query exemplifies how it would look like if you want to deny both reading all properties and traversing nodes labeled with `Account`.")

      query("DENY MATCH { * } ON GRAPH neo4j NODES Account TO regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
    }
  }.build()
}
