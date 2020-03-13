package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AdministrationRoleManagementTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing roles", "administration-managing-roles")
    database("system")
    synopsis("This section explains how to use Cypher to manage roles.")

    p(
      """
        |* <<administration-managing-roles-introduction, Introduction>>
        |* <<administration-managing-roles-syntax, Syntax>>
        |* <<administration-managing-roles-built-in-roles, Built-in roles>>
        |* <<administration-managing-roles-examples, Examples>>
        |** <<administration-managing-roles-examples-show, Listing roles>>
        |** <<administration-managing-roles-examples-create, Creating roles>>
        |** <<administration-managing-roles-examples-drop, Deleting roles>>
        |** <<administration-managing-roles-examples-grant, Assigning roles>>
        |** <<administration-managing-roles-examples-revoke, Revoking roles>>
        |""".stripMargin)

    section("Introduction", "administration-managing-roles-introduction") {
      p("""Roles can be created and managed using a set of Cypher administration commands executed against the `system` database.""")
      p(
        """In order for a user to manage roles, they must first be assigned the required privileges.
          |This can be done by assigning them the built-in `admin` role.
          |The privileges to manage roles can also be granted to roles using Cypher commands.
          |For details, see <<administration-managing-role-privileges>>.""".stripMargin)

      note {
        p("""The role name `PUBLIC` is reserved, and cannot be used as a name for a custom role.""")
      }
    }
    section("Built-in roles", "administration-managing-roles-built-in-roles") {
      p(
          """When first starting a Neo4j DBMS there are a number of built-in roles:
            |
            |* `reader` - can perform read-only queries on all databases except `system`.
            |* `editor` - can perform read and write operations on all databases except `system`, but cannot make new labels, relationship types or property names.
            |* `publisher` - can do the same as `editor`, but also create new labels and relationship types.
            |* `architect` - can do the same as `publisher` as well as create and manage indexes and constraints.
            |* `admin` - can do the same as all the above, as well as manage databases, users, roles and privileges.
            |
            |These can be viewed by running <<administration-managing-roles-examples-show, `SHOW ROLES`>> in a newly-created database.
            |The built-in roles are described in greater detail in <<operations-manual#auth-built-in-roles, Operations Manual -> Built-in roles>>.
            |""".stripMargin)
    }
//    section("The `admin` role", "administration-managing-roles-the-admin-role") {
//      p("include::admin-role-introduction.asciidoc[]")
//    }

    section("Syntax", "administration-managing-roles-syntax") {
      p("include::managing-roles/role-management-syntax.asciidoc[]")
    }
    section("Examples", "administration-managing-roles-examples"){
      initQueries("CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED",
        "CREATE USER user1 SET PASSWORD 'abc'", "CREATE USER user2 SET PASSWORD 'abc'", "CREATE USER user3 SET PASSWORD 'abc'")
      section("Listing roles", "administration-managing-roles-examples-show") {
        p("Available roles can be seen using `SHOW ROLES`.")
        query("SHOW ROLES", assertAllNodesShown("Role", column = "role")) {
          p(
            """This is the same command as `SHOW ALL ROLES`.
              |""".stripMargin)
          resultTable()
        }
        p("There are multiple versions of this command, the default being `SHOW ALL ROLES`. " +
          "To only show roles that are assigned to users, the command is `SHOW POPULATED ROLES`. " +
          "To see which users are assigned to roles `WITH USERS` can be appended to the commands. " +
          "This will give one result row for each user, so if a role is assigned to two users then it will show up twice in the result.")
        query("SHOW POPULATED ROLES WITH USERS", assertRolesShown(Seq("admin"), Seq("PUBLIC"))) {
          p("The table of results contains two columns, the first is the role name, and the other a flag indicating whether the role is a built-in role or a custom role.")
          resultTable()
        }
        p("The `SHOW ROLE name PRIVILEGES` command is found in <<administration-security-subgraph-show, Listing privileges>>.")
      }
      section("Creating roles", "administration-managing-roles-examples-create") {
        p("Roles can be created using `CREATE ROLE`.")
        query("CREATE ROLE myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("A role can also be copied, keeping its privileges, using `CREATE ROLE AS COPY OF`.")
        query("CREATE ROLE mysecondrole AS COPY OF myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The created roles will appear on the list provided by `SHOW ROLES`.")
        query("SHOW ROLES", assertAllNodesShown("Role", column = "role")) {
          resultTable()
        }
        p("These command versions are optionally idempotent, with the default behavior to throw an exception if the role already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the role already exist. " +
          "Adding `OR REPLACE` to the command will result in any existing role being deleted and a new one created.")
        query("CREATE ROLE myrole IF NOT EXISTS", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
        query("CREATE OR REPLACE ROLE myrole", ResultAssertions(r => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
          p("This is equivalent to running `DROP ROLE myrole IF EXISTS` followed by `CREATE ROLE myrole`.")
        }
        note {
          p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
        }
      }
      section("Deleting roles", "administration-managing-roles-examples-drop") {
        p("Roles can be deleted using `DROP ROLE` command.")
        initQueries("CREATE ROLE mysecondrole")
        query("DROP ROLE mysecondrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("When a role has been deleted, it will no longer appear on the list provided by `SHOW ROLES`.")
        query("SHOW ROLES", assertAllNodesShown("Role", column = "role")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the role does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the role not exist.")
        query("DROP ROLE mysecondrole IF EXISTS", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
      }
      section("Assigning roles to users", "administration-managing-roles-examples-grant") {
        p("Users can be given access rights by assigning them roles using `GRANT ROLE`.")
        initQueries("CREATE ROLE myrole", "CREATE ROLE role1", "CREATE ROLE role2")
        query("GRANT ROLE myrole TO jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The roles assigned to each user can be seen in the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p("It is possible to assign multiple roles to multiple users in one command.")
        query("GRANT ROLES role1, role2 TO user1, user2, user3", ResultAssertions(r => {
          assertStats(r, systemUpdates = 6)
        })) {
          statsOnlyResultTable()
        }
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
      }
      section("Revoking roles from users", "administration-managing-roles-examples-revoke") {
        p("Users can lose access rights by revoking roles from them using `REVOKE ROLE`.")
        initQueries("CREATE ROLE myrole", "GRANT ROLE myrole TO jake",
          "CREATE ROLE role1", "CREATE ROLE role2", "GRANT ROLES role1, role2 TO user1, user2, user3")
        query("REVOKE ROLE myrole FROM jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The roles revoked from users can no longer be seen in the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p("It is possible to revoke multiple roles from multiple users in one command.")
        query("REVOKE ROLES role1, role2 FROM user1, user2, user3", ResultAssertions(r => {
          assertStats(r, systemUpdates = 6)
        })) {
          statsOnlyResultTable()
        }
      }
    }
  }.build()

  private def assertAllNodesShown(label: String, column: String) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val nodes = tx.findNodes(Label.label(label)).asScala.toList
      nodes.length should be > 0
      val props = nodes.map(n => n.getProperty("name"))
      val result = p.columnAs[String](column).toList
      result.toSet should equal(props.toSet)
    } finally {
      tx.close()
    }
  })

  private def assertRolesShown(expected: Seq[String] = List.empty, ignore: Seq[String] = List.empty) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("role").toList.filter(!ignore.contains(_))
      result.toSet should equal(expected.toSet)
    } finally {
      tx.close()
    }
  })
}
