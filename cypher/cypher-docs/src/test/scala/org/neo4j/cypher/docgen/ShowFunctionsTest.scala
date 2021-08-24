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
    doc("SHOW FUNCTIONS", "query-listing-functions")
    synopsis("This section explains the `SHOW FUNCTIONS` command.")
    p("Listing the available functions can be done with `SHOW FUNCTIONS`.")
    p("""
    #[NOTE]
    #====
    #The command `SHOW FUNCTIONS` only outputs the default output; for a full output use the optional `YIELD` command.
    #Full output: `SHOW FUNCTIONS YIELD *`.
    #====""".stripMargin('#'))
    p("This command will produce a table with the following columns:")
    p("""
.List functions output
[options="header", cols="4,6"]
||===
|| Column
|| Description

|m| name
|a| The name of the function. label:default-output[]

|m| category
|a| The function category, for example `scalar` or `string`. label:default-output[]

|m| description
|a| The function description. label:default-output[]

|m| signature
|a| The signature of the function.

|m| isBuiltIn
|a| Whether the function is built-in or user-defined.

|m| argumentDescription
|a| List of the arguments for the function, as map of strings with name, type, default, and description.

|m| returnDescription
|a| The return value type.

|m| aggregating
|a| Whether the function is aggregating or not.

|m| rolesExecution
|a|
List of roles permitted to execute this function.
Is `null` without the <<administration-security-administration-dbms-privileges-role-management,`SHOW ROLE`>> privilege.

|m| rolesBoostedExecution
|a|
List of roles permitted to use boosted mode when executing this function.
Is `null` without the <<administration-security-administration-dbms-privileges-role-management,`SHOW ROLE`>> privilege.
||===""")
    section("Syntax") {
      p("""
List functions, either all or only built-in or user-defined::

[source, cypher, role=noplay]
----
SHOW [ALL|BUILT IN|USER DEFINED] FUNCTION[S]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and may not be omitted.
====

List functions that the current user can execute::

[source, cypher, role=noplay]
----
SHOW [ALL|BUILT IN|USER DEFINED] FUNCTION[S] EXECUTABLE [BY CURRENT USER]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and may not be omitted.
====

List functions that the specified user can execute::

[source, cypher, role=noplay]
----
SHOW [ALL|BUILT IN|USER DEFINED] FUNCTION[S] EXECUTABLE BY username
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

Required privilege <<administration-security-administration-dbms-privileges-user-management,`SHOW USER`>>.
This command cannot be used for LDAP users.

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and may not be omitted.
====""".stripMargin('#'))
    }
    section("Listing all functions") {
      p("""To list all available functions with the default output columns, the `SHOW FUNCTIONS` command can be used.
          #If all columns are required, use `SHOW FUNCTIONS YIELD *`.""".stripMargin('#'))
      query("SHOW FUNCTIONS", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "category", "description")
      })) {
        limitedResultTable(20)
      }
    }
    section("Listing functions with filtering on output columns") {
      p(
        """The listed functions can be filtered in multiple ways.
          #One way is through the type keywords, `BUILT IN` and `USER DEFINED`.
          #A more flexible way is to use the `WHERE` clause.
          #For example, getting the name of all built-in functions starting with the letter 'a':""".stripMargin('#'))
      query("""
          #SHOW BUILT IN FUNCTIONS YIELD name, isBuiltIn
          #WHERE name STARTS WITH 'a'""".stripMargin('#'),
      ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "isBuiltIn")
        p.columnAs[String]("name").foreach(_ should startWith("a"))
        p.columnAs[Boolean]("isBuiltIn").foreach(_ should be(true))
      })) {
        limitedResultTable(15)
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
          #This filtering is only available through the `EXECUTABLE` clause and not through the `WHERE` clause.
          #This is due to using the user's privileges instead of filtering on the available output columns.""".stripMargin('#'))
      p(
        """There are two options, how to use the `EXECUTABLE` clause.
          #The first option, is to filter for the current user:""".stripMargin('#'))
      login("jake", "abc123")
      query("SHOW FUNCTIONS EXECUTABLE BY CURRENT USER YIELD *", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "category", "description", "signature", "isBuiltIn", "argumentDescription", "returnDescription", "aggregating", "rolesExecution", "rolesBoostedExecution")
        p.columnAs[List[String]]("rolesExecution").foreach(_ should be(null))
        p.columnAs[List[String]]("rolesBoostedExecution").foreach(_ should be(null))
      })) {
        limitedResultTable(maybeWantedColumns = Some(List("name", "category", "description", "rolesExecution", "rolesBoostedExecution")))
      }
      p("Notice that the two `roles` columns are empty due to missing the <<administration-security-administration-dbms-privileges-role-management,`SHOW ROLE`>> privilege.")
      logout()
      p("The second option, is to filter for a specific user:")
      query("SHOW FUNCTIONS EXECUTABLE BY jake", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "category", "description")
      })) {
        limitedResultTable()
      }
    }
  }.build()
}
