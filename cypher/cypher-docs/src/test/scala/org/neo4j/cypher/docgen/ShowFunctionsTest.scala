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

class ShowFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/listing"

  override def doc: Document = new DocBuilder {
    doc("Listing functions", "query-listing-functions")
    p("Listing the available functions can be done with `SHOW FUNCTIONS`.")
    p("include::listing-functions-syntax.asciidoc[]")
    p("This command will produce a table with the following columns:")
    p("include::listing-functions-columns.asciidoc[]")
    section("Examples") {
      section("Listing all functions") {
        p(
          """To list all available functions with the default output columns, the `SHOW FUNCTIONS` command can be used.
            |If all columns are required, use `SHOW FUNCTIONS YIELD *`.""".stripMargin)
        query("SHOW FUNCTIONS", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "category", "description")
        })) {
          limitedResultTable(List("name", "category", "description"), 20)
        }
      }
      section("Listing functions with filtering on output columns") {
        p(
          """The listed functions can be filtered in multiple ways.
            |One way is through the type keywords, `BUILT IN` and `USER DEFINED`.
            |Another more flexible way is to use the `WHERE` clause.
            |For example getting the name of all built-in functions starting on the letter 'a':""".stripMargin)
        query("SHOW BUILT IN FUNCTIONS YIELD name, isBuiltIn WHERE name STARTS WITH 'a'", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "isBuiltIn")
          p.columnAs[String]("name").foreach(_ should startWith("a"))
          p.columnAs[Boolean]("isBuiltIn").foreach(_ should be(true))
        })) {
          limitedResultTable(List("name", "isBuiltIn"), 15)
        }
      }
      section("Listing functions with other filtering") {
        database("system")
        initQueries(
          "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED"
        )
        database("neo4j")
        p(
          """The listed functions can also be filtered on whether a user can execute them.
            |This filtering is only available through the `EXECUTABLE` clause and not through the `WHERE` clause.
            |This is due to using the users privileges instead of just filtering on the available output columns.""".stripMargin)
        p(
          """There are two versions of the `EXECUTABLE` clause.
            |The first one is to filter for the current user:""".stripMargin)
        login("jake", "abc123")
        query("SHOW FUNCTIONS EXECUTABLE BY CURRENT USER YIELD *", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "category", "description", "signature", "isBuiltIn", "argumentDescription", "returnDescription", "aggregating", "rolesExecution", "rolesBoostedExecution")
          p.columnAs[List[String]]("rolesExecution").foreach(_ should be(null))
          p.columnAs[List[String]]("rolesBoostedExecution").foreach(_ should be(null))
        })) {
          limitedResultTable(List("name", "category", "description", "rolesExecution", "rolesBoostedExecution"))
        }
        p("Notice that the two roles columns are empty due to missing the <<administration-security-administration-dbms-privileges-role-management, SHOW ROLE>> privilege.")
        logout()
        p("The second versions is to filter for a specific user:")
        query("SHOW FUNCTIONS EXECUTABLE BY jake", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("name", "category", "description")
        })) {
          limitedResultTable(List("name", "category", "description"))
        }
      }
    }
  }.build()
}
