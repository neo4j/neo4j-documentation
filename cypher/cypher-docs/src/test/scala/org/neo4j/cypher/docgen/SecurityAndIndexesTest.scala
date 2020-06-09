package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityAndIndexesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Security and indexing", "administration-security-indexes")
    database("neo4j")
    initQueries(
      "CREATE INDEX foo FOR (n:Person) ON (n.name)"
    )
    synopsis("This section explains how security privileges impact the results returned by indexes.")

    section("How do privileges impact index results") {
      p("include::security-and-indexes-intro.asciidoc[]")
    }

    section("Neo4j indexes on node label and properties") {
    }

  }.build()
}
