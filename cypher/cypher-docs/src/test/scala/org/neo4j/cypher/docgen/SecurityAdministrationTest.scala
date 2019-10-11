package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class SecurityAdministrationTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Security of Administration", "administration-security-administration")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "CREATE ROLE noAccessUsers",
      "GRANT ROLE regularUsers TO jake",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage Neo4j administrative rights.")
    section("The 'admin' role", "administration-security-administration-introduction") {
      p("include::admin-role-introduction.asciidoc[]")
      p("image::grant-privileges-database.png[title=\"GRANT and DENY Syntax\"]")
    }
    section("Database administration", "administration-security-administration-database-privileges") {
      synopsis("This section explains how to use Cypher to manage privileges for Neo4j database administrative rights.")
      section("The ACCESS privilege", "administration-security-administration-database-access") {
        p(
          """The `ACCESS` privilege can be used to allow the ability to access a database.""".stripMargin)
        p("include::grant-access-syntax.asciidoc[]")

        p(
          """For example, granting the ability to access the database `neo4j` to the role `regularUsers` is done like the following query.""".stripMargin)
        query("GRANT ACCESS ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `ACCESS` privilege can also be denied.")
        p("include::deny-access-syntax.asciidoc[]")

        p("For example, denying the ability to access to the database `neo4j` to the role `regularUsers` is done like the following query.")
        query("DENY ACCESS ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
    }
    section("DBMS administration", "administration-security-administration-dbms-privileges") {
      p("include::admin-role-dbms.asciidoc[]")
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
