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
    section("User Management", "administration-security-users") {
      p("Users can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p("include::user-management-syntax.asciidoc[]")
      section("Listing users", "administration-security-users-show") {
        p("Available users can be seen using `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User")) {
          resultTable()
        }
        p("The `SHOW USER name PRIVILEGES` command is found in <<administration-security-subgraph-show, Listing privileges>>.")
      }
      section("Creating users", "administration-security-users-create") {
        p("Users can be created using `CREATE USER`.")
        p("include::user-management-syntax-create-user.asciidoc[]")
        p("If the optional `SET PASSWORD CHANGE [NOT] REQUIRED` is omitted then the default is `CHANGE REQUIRED`. " +
          "The default for `SET STATUS` is `ACTIVE`.")
        p("For example, we can create the user `jake` in a suspended state and the requirement to change his password.")
        query("CREATE USER jake SET PASSWORD 'abc' CHANGE REQUIRED SET STATUS SUSPENDED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The created user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the user already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the user already exist. " +
          "Adding `OR REPLACE` to the command will result in any existing user being deleted and a new one created.")
        query("CREATE USER jake IF NOT EXISTS SET PASSWORD 'xyz'", ResultAssertions( r => {
          assertStats(r, systemUpdates = 0)
        })) {}
        query("CREATE OR REPLACE USER jake SET PASSWORD 'xyz'", ResultAssertions( r => {
          assertStats(r, systemUpdates = 2)
        })) {
          p("This is equivalent to running `DROP USER jake IF EXISTS` followed by `CREATE USER jake SET PASSWORD 'xyz'`.")
        }
      }
      section("Modifying users", "administration-security-users-alter") {
        p("Users can be modified using `ALTER USER`.")
        p("include::user-management-syntax-alter-user.asciidoc[]")
        p("For example, we can modify the user `jake` with a new password and active state as well as remove the requirement to change his password.")
        query("ALTER USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The changes to the user will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
      }
      section("Changing the logged in users password", "administration-security-users-alter-password") {
        p("Users can be change their own password using `ALTER CURRENT USER SET PASSWORD`, " +
          "the old password is required in addition to the new. " +
          "This will both change the password and set the `CHANGE NOT REQUIRED` flag.")
        query("ALTER CURRENT USER SET PASSWORD FROM 'abc123' TO '123xyz'", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          // Can't pull on the result since the test is run with auth disabled, which is not allowed for this command
        }
        note {
          p("This command cannot be run with auth disabled.")
        }
      }
      section("Deleting users", "administration-security-users-drop") {
        p("Users can be deleted using `DROP USER`.")
        query("DROP USER jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("When a user has been deleted, it will no longer appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the user does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the user not exist.")
        query("DROP USER jake IF EXISTS", ResultAssertions( r => {
          assertStats(r, systemUpdates = 0)
        })) {}
      }
    }
    section("Role Management", "administration-security-roles") {
      initQueries("CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED")
      p("Roles can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p("include::role-management-syntax.asciidoc[]")
      section("Listing roles", "administration-security-roles-show") {
        p("Available roles can be seen using `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role")) {
          p("This is the same command as `SHOW ALL ROLES`.")
          resultTable()
        }
        p("There are multiple versions of this command, the default being `SHOW ALL ROLES`. " +
          "To only show roles that are assigned to users, the command is `SHOW POPULATED ROLES`. " +
          "To see which users are assigned to roles can `WITH USERS` be appended to the commands. " +
          "This will give one result row for each user, so if a role is assigned to two users then it will show up twice in the result.")
        query("SHOW POPULATED ROLES WITH USERS", assertNodesShown("Role")) {
          resultTable()
        }
        p("The `SHOW ROLE name PRIVILEGES` command is found in <<administration-security-subgraph-show, Listing privileges>>.")
      }
      section("Creating roles", "administration-security-roles-create") {
        p("Roles can be created using `CREATE ROLE`.")
        query("CREATE ROLE myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("A role can also be copied, keeping its privileges, using `CREATE ROLE AS COPY OF`.")
        query("CREATE ROLE mysecondrole AS COPY OF myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The created roles will appear on the list provided by `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role", column = "role")) {
          resultTable()
        }
        p("These command versions are optionally idempotent, with the default behavior to throw an exception if the role already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the role already exist. " +
          "Adding `OR REPLACE` to the command will result in any existing role being deleted and a new one created.")
        query("CREATE ROLE myrole IF NOT EXISTS", ResultAssertions( r => {
          assertStats(r, systemUpdates = 0)
        })) {}
        query("CREATE OR REPLACE ROLE myrole", ResultAssertions( r => {
          assertStats(r, systemUpdates = 2)
        })) {
          p("This is equivalent to running `DROP ROLE myrole IF EXISTS` followed by `CREATE ROLE myrole`.")
        }
      }
      section("Deleting roles", "administration-security-roles-drop") {
        p("Roles can be deleted using `DROP ROLE` command.")
        query("DROP ROLE mysecondrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("When a role has been deleted, it will no longer appear on the list provided by `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role", column = "role")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the role does not exists. " +
          "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the role not exist.")
        query("DROP ROLE mysecondrole IF EXISTS", ResultAssertions( r => {
          assertStats(r, systemUpdates = 0)
        })) {}
      }
      section("Assigning roles to users", "administration-security-roles-grant") {
        p("Users can be give access rights by assigning them roles using `GRANT ROLE`.")
        query("GRANT ROLE myrole TO jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The roles assigned to each user can be seen in the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
        p("It is possible to assign multiple roles to multiple users in one command.")
        query("GRANT ROLES role1, role2 TO user1,user2,user3", ResultAssertions( r => {
          assertStats(r, systemUpdates = 4)
        })) {
          // Can't pull on the result since neither roles nor users exist
        }
      }
      section("Revoking roles from users", "administration-security-roles-revoke") {
        p("Users can lose access rights by revoking roles from them using `REVOKE ROLE`.")
        query("REVOKE ROLE myrole FROM jake", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The roles revoked from users can no longer be seen in the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
        p("It is possible to revoke multiple roles from multiple users in one command.")
        query("REVOKE ROLES role1, role2 FROM user1,user2,user3", ResultAssertions( r => {
          assertStats(r, systemUpdates = 4)
        })) {
          // Can't pull on the result since neither roles nor users exist
        }
      }
    }
  }.build()

  private def assertNodesShown(label: String, propertyKey: String = "name", column: String = "name") = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
//      println(p.resultAsString)
      val nodes = tx.findNodes(Label.label(label)).asScala.toList
      // TODO: Remove this conditional once we have system graph initialization working OK
      if (nodes.nonEmpty) {
        nodes.length should be > 0
//        nodes.foreach(n => println(s"${n.labels}: ${n.getAllProperties}"))
        val props = nodes.map(n => n.getProperty(propertyKey))
        props should equal(p.columnAs[String](column).toList)
      }
    } finally {
      tx.close()
    }
  })
}
