package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AdministrationUserManagementTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Managing users", "administration-managing-users")
    database("system")
    synopsis("This section explains how to use Cypher to manage users.")

    p(
      """
        |* <<administration-managing-users-introduction, Introduction>>
        |* <<administration-managing-users-syntax, Syntax>>
        |* <<administration-managing-users-examples, Examples>>
        |** <<administration-managing-users-examples-show, Listing users>>
        |** <<administration-managing-users-examples-create, Creating users>>
        |** <<administration-managing-users-examples-alter, Modifying users>>
        |** <<administration-managing-users-examples-alter-password, Changing the current user's password>>
        |** <<administration-managing-users-examples-drop, Deleting users>>
        |""".stripMargin)

    section("Introduction", "administration-managing-users-introduction") {
      p("Users can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p(
        """In order for a user to manage users, they must first be assigned the required privileges.
          |This is done by assigning them the built-in `admin` role, or a role which is created as a copy of this role.""".stripMargin)
    }
    section("Syntax", "administration-managing-users-syntax") {
      p("include::user-management-syntax.asciidoc[]")
    }
    section("Examples", "administration-managing-users-examples") {
      section("Listing users", "administration-managing-users-examples-show") {
        p("Available users can be listed using the `SHOW USERS` command, which will produce a table of users with the following columns:")
        p("include::list-users-table-columns.asciidoc[]")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p(
          """The single default user `neo4j`, with administrative privileges, is always present after initially starting the Neo4j DBMS.
            |It is possible to define the initial password of the default `neo4j` user from the command line immediately after creating the database.
            |See <<operations-manual#post-installation-set-initial-password, Operations Manual -> Set an initial password>> for details.
            |If this is not done then the password of the `neo4j` user must be changed at the first login after creation.
            |""".stripMargin)
        note {
          p("The `SHOW USER name PRIVILEGES` command is described in <<administration-security-subgraph-show>>.")
        }
      }
      section("Creating users", "administration-managing-users-examples-create") {
        p("Users can be created using `CREATE USER`.")
        p("include::user-management-syntax-create-user.asciidoc[]")
        p("If the optional `SET PASSWORD CHANGE [NOT] REQUIRED` is omitted then the default is `CHANGE REQUIRED`. " +
          "The default for `SET STATUS` is `ACTIVE`. The `password` can either be a string value or a string parameter.")
        p("For example, we can create the user `jake` in a suspended state and the requirement to change his password.")
        query("CREATE USER jake SET PASSWORD 'abc' CHANGE REQUIRED SET STATUS SUSPENDED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
          note {
            p("[enterprise-edition]#The `SUSPENDED` flag is an enterprise feature.#")
          }
        }
        p("The created user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        note {
          p(
            """When creating a user, their initial list of roles is empty.
              |In Neo4j Community Edition this is not important, as all users have implied administator privileges.
              |In Neo4j Enterprise Edition, in order for the user to be able to perform any actions, they have to be granted roles.
              |For details on granting roles, see <<administration-managing-roles>>.""".stripMargin)
        }
        p("The `CREATE USER` command is optionally idempotent, with the default behavior to throw an exception if the user already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens in case the user already exists. " +
          "Adding `OR REPLACE` to the command will result in any existing user being deleted and a new one created.")
        query("CREATE USER jake IF NOT EXISTS SET PASSWORD 'xyz'", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
        query("CREATE OR REPLACE USER jake SET PASSWORD 'xyz'", ResultAssertions(r => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
          p("This is equivalent to running `DROP USER jake IF EXISTS` followed by `CREATE USER jake SET PASSWORD 'xyz'`.")
        }
        note {
          p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
        }
      }
      section("Modifying users", "administration-managing-users-examples-alter") {
        p("Users can be modified using `ALTER USER`.")
        p("include::user-management-syntax-alter-user.asciidoc[]")
        p("The `password` can either be a string value or a string parameter.")
        p("For example, we can modify the user `jake` with a new password and active status as well as remove the requirement to change his password.")
        query("ALTER USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        note(p(
          """When altering a user it is only necessary to specify the changes required.
            |For example, leaving out any `STATUS` change part of the query will leave that unchanged.""".stripMargin))
        p("The changes to the user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
      }
      section("Changing the current user's password", "administration-managing-users-examples-alter-password") {
        initQueries("CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED")
        p(
          """Users can change their own password using `ALTER CURRENT USER SET PASSWORD`.
            |The old password is required in addition to the new one, and either or both can be a string value or a string parameter.
            |When a user executes this command it will change their password as well as set the `CHANGE NOT REQUIRED` flag.""".stripMargin)
        login("jake", "abc123")
        query("ALTER CURRENT USER SET PASSWORD FROM 'abc123' TO '123xyz'", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        //note {
        //  p("This command only works for a logged-in user and cannot be run with auth disabled.")
        //}
      }
      section("Deleting users", "administration-managing-users-examples-drop") {
        p("Users can be deleted using `DROP USER`.")
        query("DROP USER jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("When a user has been deleted, it will no longer appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the user does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens in case the user does not exist.")
        query("DROP USER jake IF EXISTS", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
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
