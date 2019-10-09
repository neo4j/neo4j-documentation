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
      }
      section("Creating users", "administration-security-users-create") {
        p("Users can be created using `CREATE USER`.")
        p("include::user-management-syntax-create-user.asciidoc[]")
        p("For example, we can create the user `jake` in a suspended state and the requirement to change his password.")
        query("CREATE USER jake SET PASSWORD 'abc' CHANGE REQUIRED SET STATUS SUSPENDED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The user created will appear on the list provided by `SHOW USERS`.")
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
        })) {}
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
    }
    section("Role Management", "administration-security-roles") {
      p("Roles can be created and managed using a set of Cypher administration commands executed against the `system` database.")
      p("include::role-management-syntax.asciidoc[]")
      section("Listing roles", "administration-security-roles-show") {
        p("Available roles can be seen using `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role")) {
          resultTable()
        }
      }
      section("Creating roles", "administration-security-roles-create") {
        p("Roles can be created using `CREATE ROLE`.")
        query("CREATE ROLE myrole", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The role created will appear on the list provided by `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role", column = "role")) {
          resultTable()
        }
        p("This command is optionally idempotent, with the default behavior to throw an exception if the role already exists. " +
          "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the role already exist. " +
          "Adding `OR REPLACE` to the command will result in any existing role being deleted and a new one created.")
        query("CREATE ROLE myrole IF NOT EXISTS", ResultAssertions( r => {
          assertStats(r, systemUpdates = 0)
        })) {}
        query("CREATE OR REPLACE ROLE myrole", ResultAssertions( r => {
          assertStats(r, systemUpdates = 2)
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
      }
    }
  }.build()

  private def assertNodesShown(label: String, propertyKey: String = "name", column: String = "name") = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      println(p.resultAsString)
      val nodes = tx.findNodes(Label.label(label)).asScala.toList
      // TODO: Remove this conditional once we have system graph initialization working OK
      if (nodes.nonEmpty) {
        nodes.length should be > 0
        nodes.foreach(n => println(s"${n.labels}: ${n.getAllProperties}"))
        val props = nodes.map(n => n.getProperty(propertyKey))
        props should equal(p.columnAs[String](column).toList)
      }
    } finally {
      tx.close()
    }
  })
}
