/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityUserAndRoleManagementTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("User and role management", "administration-security-users-and-roles")
    database("system")
    synopsis("This section explains how to use Cypher to manage Neo4j role-based access control through users and roles.")

    p(
      """
        |* <<administration-security-users, User Management>>
        |** <<administration-security-users-show-current, Listing current user>>
        |** <<administration-security-users-show, Listing users>>
        |** <<administration-security-users-create, Creating users>>
        |** <<administration-security-users-alter, Modifying users>>
        |** <<administration-security-users-alter-password, Changing the current user's password>>
        |** <<administration-security-users-drop, Deleting users>>
        |* <<administration-security-roles, Role management>>
        |** <<administration-security-roles-public, The `PUBLIC` role>>
        |** <<administration-security-roles-show, Listing roles>>
        |** <<administration-security-roles-create, Creating roles>>
        |** <<administration-security-roles-drop, Deleting roles>>
        |** <<administration-security-roles-grant, Assigning roles>>
        |** <<administration-security-roles-revoke, Revoking roles>>
        |""".stripMargin)

    section("User Management", "administration-security-users") {
      p("Users can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p("When connected to the DBMS over bolt, administration commands are automatically routed to the `system` database.")
      p("include::user-management-syntax.asciidoc[]")
      section("Listing current user", "administration-security-users-show-current") {
        initQueries("CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED")
        p("The currently logged-in user can be seen using `SHOW CURRENT USER` which will produce a table with four columns:")
        p("include::list-users-table-columns.asciidoc[]")
        login("jake", "abc123")
        query("SHOW CURRENT USER", assertUsersShown(Seq("jake"))) {
          resultTable()
        }
        note {
          p("This command is only supported for a logged-in user and will return an empty result if authorization has been disabled.")
        }
      }
      section("Listing users", "administration-security-users-show") {
        p("Available users can be seen using `SHOW USERS` which will produce a table of users with four columns:")
        p("include::list-users-table-columns.asciidoc[]")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p(
          """When first starting a Neo4j DBMS, there is always a single default user `neo4j` with administrative privileges.
            |It is possible to set the initial password using <<operations-manual#post-installation-set-initial-password, neo4j-admin set-initial-password>>,
            |otherwise it is necessary to change the password after first login.
            |""".stripMargin)
        note {
          p("The `SHOW USER name PRIVILEGES` command is described in <<administration-security-subgraph-show, Listing privileges>>.")
        }
      }
      section("Creating users", "administration-security-users-create") {
        p("Users can be created using `CREATE USER`.")
        p("include::user-management-syntax-create-user.asciidoc[]")
        p("""
            | * The `SET PASSWORD CHANGE [NOT] REQUIRED` and `SET STATUS` clauses can be applied in any order.
            | * For `SET PASSWORD`:
            | ** The `password` can either be a string value or a string parameter.
            | ** The optional `PLAINTEXT` in `SET PLAINTEXT PASSWORD` has the same behaviour as `SET PASSWORD`.
            | ** The optional `ENCRYPTED` can be used to create a user when the plaintext password is unknown but the encrypted password is available
            |    (e.g. from a database backup). In this case the password string is expected to be in the format of `<encryption-version>,<hash>,<salt>`.
            | * If the optional `SET PASSWORD CHANGE [NOT] REQUIRED` is omitted then the default is `CHANGE REQUIRED`.
            |   The `SET PASSWORD` part is only optional if it directly follows the `SET PASSWORD` clause.
            | * The default for `SET STATUS` is `ACTIVE`.
          """.stripMargin
        )
        p("For example, we can create the user `jake` in a suspended state and the requirement to change his password.")
        query("CREATE USER jake SET PASSWORD 'abc' CHANGE REQUIRED SET STATUS SUSPENDED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
          note {
            p("[enterprise-edition]#The `SET STATUS {ACTIVE | SUSPENDED}` part of the command is only available in Enterprise Edition.#")
          }
        }
        p("The created user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS YIELD user, suspended, passwordChangeRequired, roles WHERE user = 'jake'", assertUsersShown(Seq("jake"))) {
          p(
            """In this example we also:
              |
              |* Reorder the columns using a `YIELD` clause
              |* Filter the results using a `WHERE` clause to show only the new user
              |
              |""".stripMargin
          )
          resultTable()
        }
        query("SHOW USERS YIELD roles, user WHERE \"PUBLIC\" IN roles RETURN user as publicUsers", ResultAssertions({ r: DocsExecutionResult =>
          r.columnAs[String]("publicUsers").toSet should be(Set("neo4j", "jake"))
          })) {
          p(
            """It is also possible to add a `RETURN` clause to further manipulate the results after filtering. In this
              |case it is used to filter out the roles column and rename the users column to `publicUsers`.""".stripMargin)
          resultTable()
        }
        note {
          p(
            """In Neo4j Community Edition there are no roles, but all users have implied administrator privileges.
              |In Neo4j Enterprise Edition all users are automatically assigned the <<administration-security-roles-public, `PUBLIC`>> role, giving them a base set of privileges.""".stripMargin)
        }
        p("The `CREATE USER` command is optionally idempotent, with the default behavior to throw an exception if the user already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the user already exist. " +
          "Adding `OR REPLACE` to the command will result in any existing user being deleted and a new one created.")
        query("CREATE USER jake IF NOT EXISTS SET PASSWORD 'xyz'", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
        query("CREATE OR REPLACE USER jake SET PLAINTEXT PASSWORD 'xyz'", ResultAssertions(r => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
          p("This is equivalent to running `DROP USER jake IF EXISTS` followed by `CREATE USER jake SET PASSWORD 'xyz'`.")
        }
        note {
          p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
        }
      }
      section("Modifying users", "administration-security-users-alter") {
        p("Users can be modified using `ALTER USER`.")
        p("include::user-management-syntax-alter-user.asciidoc[]")
        p("""
            | * At least one of the `SET` clauses are required for the command.
            | * The `SET PASSWORD CHANGE [NOT] REQUIRED` and `SET STATUS` clauses can be applied in any order.
            |   The `SET PASSWORD` clause must come first, if used.
            | * For `SET PASSWORD`:
            | ** The `password` can either be a string value or a string parameter, and must not be identical to the old password.
            | ** The optional `PLAINTEXT` in `SET PLAINTEXT PASSWORD` has the same behaviour as `SET PASSWORD`.
            | ** The optional `ENCRYPTED` can be used to update a user's password when the plaintext password is unknown but the encrypted password
            |    is available (e.g. from a database backup). In this case the password string is expected to be in the format of `<encryption-version>,<hash>,<salt>`.
            | * For `SET PASSWORD CHANGE [NOT] REQUIRED`, the `SET PASSWORD` is only optional if it directly follows the `SET PASSWORD` clause.
          """.stripMargin
        )
        p("For example, we can modify the user `jake` with a new password and active status as well as remove the requirement to change his password.")
        query("ALTER USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        note{
          p(
          """When altering a user it is only necessary to specify the changes required.
            |For example, leaving out the `CHANGE [NOT] REQUIRED` part of the query will leave that unchanged.""".stripMargin)
        }
        note {
          p("[enterprise-edition]#The `SET STATUS {ACTIVE | SUSPENDED}` part of the command is only available in Enterprise Edition.#")
        }
        p("The changes to the user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the user does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the user not exist.")
        query("ALTER USER nonExistingUser IF EXISTS SET PASSWORD 'abc123'", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
      }
      section("Changing the current user's password", "administration-security-users-alter-password") {
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
        note {
          p("This command only works for a logged in user and cannot be run with auth disabled.")
        }
      }
      section("Deleting users", "administration-security-users-drop") {
        p("Users can be deleted using `DROP USER`.")
        query("DROP USER jake", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        note {
          p(
            "Deleting a user will not automatically terminate associated connections, sessions, transactions, or queries.")
        }
        p("When a user has been deleted, it will no longer appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertAllNodesShown("User", column = "user")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the user does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the user not exist.")
        query("DROP USER jake IF EXISTS", ResultAssertions(r => {
          assertStats(r, systemUpdates = 0)
        })) {
          statsOnlyResultTable()
        }
      }
    }
    section("Role Management", "administration-security-roles", "enterprise-edition") {
      initQueries("CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED",
        "CREATE USER user1 SET PASSWORD 'abc'", "CREATE USER user2 SET PASSWORD 'abc'", "CREATE USER user3 SET PASSWORD 'abc'")
      p("Roles can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p("When connected to the DBMS over bolt, administration commands are automatically routed to the `system` database.")
      p("include::role-management-syntax.asciidoc[]")
      section("The `PUBLIC` role", "administration-security-roles-public", "enterprise-edition") {
        p(
          """There exists a special built-in role, `PUBLIC`, which is assigned to all users.
            |This role cannot be dropped or revoked from any user, but its privileges may be modified.
            |By default, it is assigned the <<administration-security-administration-database-access, ACCESS>> privilege on the default database.
            |""".stripMargin)
        p("""In contrast to the `PUBLIC` role, the other built-in roles can be granted, revoked, dropped and re-created.""")
      }
      section("Listing roles", "administration-security-roles-show", "enterprise-edition") {
        p("Available roles can be seen using `SHOW ROLES`.")
        query("SHOW ROLES", assertAllNodesShown("Role", column = "role")) {
          p(
            """This is the same command as `SHOW ALL ROLES`.
              |When first starting a Neo4j DBMS there are a number of built-in roles:
              |
              |* `PUBLIC` - a role that all users have granted, by default it gives access to the default database
              |* `reader` - can perform traverse and read operations on all databases except `system`.
              |* `editor` - can perform traverse, read, and write operations on all databases except `system`, but cannot make new labels or relationship types.
              |* `publisher` - can do the same as `editor`, but also create new labels and relationship types.
              |* `architect` - can do the same as `publisher` as well as create and manage indexes and constraints.
              |* `admin` - can do the same as all the above, as well as manage databases, users, roles, and privileges.
              |
              |More information about the built-in roles can be found in <<operations-manual#auth-built-in-roles, Operations Manual -> Built-in roles>>
              |""".stripMargin)
          resultTable()
        }
        p("There are multiple versions of this command, the default being `SHOW ALL ROLES`. " +
          "To only show roles that are assigned to users, the command is `SHOW POPULATED ROLES`. " +
          "To see which users are assigned to roles `WITH USERS` can be appended to the commands. " +
          "This will give one result row for each user, so if a role is assigned to two users then it will show up twice in the result. ")
        query("SHOW POPULATED ROLES WITH USERS", assertRolesShown(Seq("admin"), Seq("PUBLIC"))) {
        p("The table of results will show information about the role and what database it belongs to. ")
          resultTable()

        }
        p("It is also possible to filter and sort the results by using `YIELD`, `ORDER BY` and `WHERE`.")
        query("SHOW ROLES YIELD role ORDER BY role WHERE role ENDS WITH 'r' ", assertRolesShown(Seq("reader", "editor", "publisher"))) {
          p(
            """In this example:
              |
              |* The results have been filtered to only return the roles ending in 'r'.
              |* The results are ordered by the 'action' column using `ORDER BY`.
              |
              |It is also possible to use `SKIP` and `LIMIT` to paginate the results.
              |""".stripMargin)
          resultTable()
        }
        note {
          p("The `SHOW ROLE name PRIVILEGES` command is found in <<administration-security-subgraph-show, Listing privileges>>.")
        }
      }
      section("Creating roles", "administration-security-roles-create", "enterprise-edition") {
        p("Roles can be created using `CREATE ROLE`.")
        query("CREATE ROLE myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        note {
          p("""The following naming rules apply:""")
          p("""
          |* The first character must be an ASCII alphabetic character.
          |* Subsequent characters can be ASCII alphabetic, numeric characters, and underscore.
          """.stripMargin)
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
      section("Deleting roles", "administration-security-roles-drop", "enterprise-edition") {
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
      section("Assigning roles to users", "administration-security-roles-grant", "enterprise-edition") {
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
      section("Revoking roles from users", "administration-security-roles-revoke", "enterprise-edition") {
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
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
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

  private def assertUsersShown(expected: Seq[String]) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val nodes = tx.findNodes(Label.label("User")).asScala.toList
      nodes.length should be > 0
      val result = p.columnAs[String]("user").toList
      result.toSet should equal(expected.toSet)
    } finally {
      tx.close()
    }
  })

  private def assertRolesShown(expected: Seq[String] = List.empty, ignore: Seq[String] = List.empty) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("role").toList.filter(!ignore.contains(_))
      result.toSet should equal(expected.toSet)
    } finally {
      tx.close()
    }
  })
}
