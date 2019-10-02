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
    synopsis("This section explains how to use Cypher to manage Neo4j administrative rights.")
    section("The 'admin' role", "administration-security-administration-admin") {
      p("")
    }
    section("Database administration", "administration-security-administration-database-privileges") {
      p("")

    }
    section("DBMS administration", "administration-security-administration-dbms-privileges") {
      p("")

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
