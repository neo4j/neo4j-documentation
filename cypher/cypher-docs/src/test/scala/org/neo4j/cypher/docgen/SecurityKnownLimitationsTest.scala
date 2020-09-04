package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityKnownLimitationsTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Known limitations of security", "administration-security-limitations")
    database("neo4j")
    initQueries(
      "CREATE INDEX foo FOR (n:Person) ON (n.name)"
    )
    //TODO: Make this page dynamic
    synopsis("This section explains known limitations and implications of Neo4js role-based access control security.")

    section("Security and indexes", "administration-security-limitations-indexes") {

      section("How do privileges impact index results") {
        p("include::security-and-indexes-intro.asciidoc[]")
      }
    }

    section("Security and labels", "administration-security-limitations-labels") {
      p("include::security-and-labels.asciidoc[]")
    }

    section("Security and count store operations", "administration-security-limitations-db-operations") {
      p("include::security-and-db-operations.asciidoc[]")
    }

  }.build()
}
