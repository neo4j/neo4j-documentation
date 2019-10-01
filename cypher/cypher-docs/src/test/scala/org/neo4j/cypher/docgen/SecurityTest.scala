package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Security", "administration-security")
    database("system")
    synopsis("This section explains how to use Cypher to manage Neo4j role-based access control and fine-grained security.")
    p(
      """
        |* <<administration-security-introduction,Introduction>>
        |* <<administration-security-users-and-roles,User and Role Management>>
        |* <<administration-security-subgraph,Database, Graph and Sub-graph Access Control>>
        |* <<administration-security-administration,Security of Administration>>
        |""".stripMargin)
    section("Introduction", "administration-security-introduction") {
      p(
        """Neo4j has a complex security model stored in the system graph, maintained in a special database called the `system` database.
          |All administrative commands need to be executing against the `system` database.
          |For more information on how to manage multiple databases, refer to the section on <<administration-databases, administering databases>>.
          |Neo4j 3.1 introduced the concept of _role-based access control_.
          |It was possible to create users and assign them to roles to control whether the users could read, write and administer the database.
          |In Neo4j 4.0 this model was enhanced significantly with the addition of _privileges_ which are the underlying access-control rules by which the users rights are defined.
          |The original built-in roles still exist with almost the exact same access rights, but they are no-longer statically defined.
          |Instead they are defined in terms of their underlying _privileges_ and they can be modified by adding an removing these access rights.
          |In addition any new roles created can by assigned any combination of _privileges_ to create the specific access control desired.
          |A major additional capability is _sub-graph_ access control whereby read-access to the graph can be limited to specific combinations of label, relationship-type and property.""".stripMargin)
    }
    section("Users and Roles", "administration-security-users-and-roles") {
      p(
        """Users and roles can be created and managed using a set of Cypher administration commands executed against the `system` database.""".stripMargin)
      section("Listing users and Roles", "administration-security-users-and-roles-show") {
        p("Available roles can be seen using `SHOW ROLES`.")
        query("SHOW ROLES", assertNodesShown("Role")) {
          resultTable()
        }
        p("Available users can be seen using `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User")) {
          resultTable()
        }
      }
      section("Creating users and Roles", "administration-security-users-and-roles-create") {
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
        p("Users can be created using `CREATE USER`.")
        query("CREATE USER jake SET PASSWORD 'abc' CHANGE NOT REQUIRED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          p("Nothing is returned from this query, except the count of system database changes made.")
          resultTable()
        }
        p("The user created will appear on the list provided by `SHOW USERS`.")
        query("SHOW USERS", assertNodesShown("User", column = "user")) {
          resultTable()
        }
        p("Users can be give access rights by assigning them to roles using `GRANT ROLE`.")
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
    section("Database, Graph and Sub-graph Access Control", "administration-security-subgraph") {
      p(
        """Database, Graph and Sub-graph Access Control.""".stripMargin)
    }
    section("Security of Administration", "administration-security-administration") {
      p(
        """Security of Administration.""".stripMargin)
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
