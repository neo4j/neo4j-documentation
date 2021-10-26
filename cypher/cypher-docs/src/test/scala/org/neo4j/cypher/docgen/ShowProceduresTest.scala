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
    doc("SHOW PROCEDURES", "query-listing-procedures")
    synopsis("This section explains the `SHOW PROCEDURES` command.")
    p("Listing the available procedures can be done with `SHOW PROCEDURES`.")
    p("""
    #[NOTE]
    #====
    #The command `SHOW PROCEDURES` returns only the default output. For a full output use the optional `YIELD` command.
    #Full output: `SHOW PROCEDURES YIELD *`.
    #====""".stripMargin('#'))
    p("This command will produce a table with the following columns:")
    p("""
.List procedures output
[options="header", cols="4,6"]
||===
|| Column
|| Description

|m| name
|a| The name of the procedure. label:default-output[]

|m| description
|a| The procedure description. label:default-output[]

|m| mode
|a| The procedure mode, for example `READ` or `WRITE`. label:default-output[]

|m| worksOnSystem
|a| Whether the procedure can be run on the `system` database or not. label:default-output[]

|m| signature
|a| The signature of the procedure.

|m| argumentDescription
|a| List of the arguments for the procedure, as map of strings with name, type, default, and description.

|m| returnDescription
|a| List of the returned values for the procedure, as map of strings with name, type, and description.

|m| admin
|a| `true` if this procedure is an admin procedure.

|m| rolesExecution
|a|
List of roles permitted to execute this procedure.
Is `null` without the <<administration-security-administration-dbms-privileges-role-management,`SHOW ROLE`>> privilege.

|m| rolesBoostedExecution
|a|
List of roles permitted to use boosted mode when executing this procedure.
Is `null` without the <<administration-security-administration-dbms-privileges-role-management,`SHOW ROLE`>> privilege.

|m| option
|a| Map of extra output, e.g. if the procedure is deprecated.
||===
""")
    section("Syntax") {
      p("""
List all procedures::

[source, cypher, role=noplay]
----
SHOW PROCEDURE[S]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----
[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and must not be omitted.
====

List procedures that the current user can execute::

[source, cypher, role=noplay]
----
SHOW PROCEDURE[S] EXECUTABLE [BY CURRENT USER]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and must not be omitted.
====

List procedures that the specified user can execute::

[source, cypher, role=noplay]
----
SHOW PROCEDURE[S] EXECUTABLE BY username
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

Requires the privilege <<administration-security-administration-dbms-privileges-user-management,`SHOW USER`>>.
This command cannot be used for LDAP users.

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and must not be omitted.
====
""".stripMargin('#'))
    }
    section("Listing all procedures") {
      p(
        """To list all available procedures with the default output columns, the `SHOW PROCEDURES` command can be used.
          #If all columns are required, use `SHOW PROCEDURES YIELD *`.""".stripMargin('#'))
      query("SHOW PROCEDURES", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem")
      })) {
        limitedResultTable(15)
      }
    }
    section("Listing procedures with filtering on output columns") {
      p(
        """The listed procedures can be filtered in multiple ways, one way is to use the `WHERE` clause.
          #For example, returning the names of all admin procedures:""".stripMargin('#'))
      query("""
        #SHOW PROCEDURES YIELD name, admin
        #WHERE admin""".stripMargin('#'),
      ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "admin")
        p.columnAs[Boolean]("admin").foreach(_ should be(true))
      })) {
        limitedResultTable(7)
      }
    }
    section("Listing procedures with other filtering") {
      database("system")
      initQueries(
        "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED"
      )
      database("neo4j")
      p("""The listed procedures can also be filtered by whether a user can execute them.
          #This filtering is only available through the `EXECUTABLE` clause and not through the `WHERE` clause.
          #This is due to using the user's privileges instead of filtering on the available output columns.""".stripMargin('#'))
      p("""There are two options, how to use the `EXECUTABLE` clause.
          #The first option, is to filter for the current user:""".stripMargin('#'))
      login("jake", "abc123")
      query("SHOW PROCEDURES EXECUTABLE BY CURRENT USER YIELD *", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem", "signature", "argumentDescription", "returnDescription", "admin", "rolesExecution", "rolesBoostedExecution", "option")
        p.columnAs[List[String]]("rolesExecution").foreach(_ should be(null))
        p.columnAs[List[String]]("rolesBoostedExecution").foreach(_ should be(null))
      })) {
        limitedResultTable(maybeWantedColumns = Some(List("name", "description", "rolesExecution", "rolesBoostedExecution")))
      }
      p("Note that the two `roles` columns are empty due to missing the <<administration-security-administration-dbms-privileges-role-management,SHOW ROLE>> privilege.")
      logout()
      p("The second option, filters the list to only contain procedures executable by a specific user:")
      query("SHOW PROCEDURES EXECUTABLE BY jake", ResultAssertions(p => {
        p.columns should contain theSameElementsAs Array("name", "description", "mode", "worksOnSystem")
      })) {
        limitedResultTable()
      }
    }
  }.build()
}
