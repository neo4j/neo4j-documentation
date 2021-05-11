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

class ShowProceduresTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/listing"

  override def doc: Document = new DocBuilder {
    doc("Listing procedures", "query-listing-procedures")
    p("Listing the available procedures can be done with `SHOW PROCEDURES`.")
    p("include::listing-procedures-syntax.asciidoc[]")
    p("This command will produce a table with the following columns:")
    p("include::listing-procedures-columns.asciidoc[]")
    section("Examples") {
      section("Listing all procedures") {
        p(
          """To list all available procedures with the default output columns, the `SHOW PROCEDURES` command can be used.
            #If all columns are required, use `SHOW PROCEDURES YIELD *`.""".stripMargin('#'))
        query("SHOW PROCEDURES", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem")
        })) {
          limitedResultTable(List("name", "description", "mode", "worksOnSystem"), 15)
        }
      }
      section("Listing procedures with filtering on output columns") {
        p(
          """The listed procedures can be filtered in multiple ways, one way is to use the `WHERE` clause.
            #For example getting the name of all admin procedures:""".stripMargin('#'))
        query("SHOW PROCEDURES YIELD name, admin WHERE admin", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "admin")
          p.columnAs[Boolean]("admin").foreach(_ should be(true))
        })) {
          limitedResultTable(List("name", "admin"), 7)
        }
      }
      section("Listing procedures with other filtering") {
        database("system")
        initQueries(
          "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED"
        )
        database("neo4j")
        p(
          """The listed procedures can also be filtered by whether a user can execute them.
            #This filtering is only available through the `EXECUTABLE` clause and not through the `WHERE` clause.
            #This is due to using the users privileges instead of just filtering on the available output columns.""".stripMargin('#'))
        p(
          """There are two versions of the `EXECUTABLE` clause.
            #The first one is to filter for the current user:""".stripMargin('#'))
        login("jake", "abc123")
        query("SHOW PROCEDURES EXECUTABLE BY CURRENT USER YIELD *", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem", "signature", "argumentDescription", "returnDescription", "admin", "rolesExecution", "rolesBoostedExecution", "option")
          p.columnAs[List[String]]("rolesExecution").foreach(_ should be(null))
          p.columnAs[List[String]]("rolesBoostedExecution").foreach(_ should be(null))
        })) {
          limitedResultTable(List("name", "description", "rolesExecution", "rolesBoostedExecution"))
        }
        p("Notice that the two roles columns are empty due to missing the <<administration-security-administration-dbms-privileges-role-management, SHOW ROLE>> privilege.")
        logout()
        p("The second version filters the list to only contain procedures executable by a specific user:")
        query("SHOW PROCEDURES EXECUTABLE BY jake", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem")
        })) {
          limitedResultTable(List("name", "description", "mode", "worksOnSystem"))
        }
      }
    }
  }.build()
}
