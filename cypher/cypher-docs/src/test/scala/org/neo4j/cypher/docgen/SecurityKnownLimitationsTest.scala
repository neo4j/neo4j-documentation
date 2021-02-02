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

class SecurityKnownLimitationsTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Known limitations of security", "administration-security-limitations")
    //TODO: Make this page dynamic
//    database("neo4j")
//    initQueries(
//      "CREATE INDEX foo FOR (n:Person) ON (n.name)"
//    )
    synopsis("This section explains known limitations and implications of Neo4js role-based access control security.")

    section("Security and indexes", "administration-security-limitations-indexes") {
      p("include::security-and-indexes-intro.asciidoc[]")
    }

    section("Security and labels", "administration-security-limitations-labels") {
      p("include::security-and-labels.asciidoc[]")
    }

    section("Security and count store operations", "administration-security-limitations-db-operations") {
      p("include::security-and-db-operations.asciidoc[]")
    }

  }.build()
}
