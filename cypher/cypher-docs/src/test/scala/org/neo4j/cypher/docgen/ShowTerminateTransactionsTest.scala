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

class ShowTerminateTransactionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/listing"

  override def doc: Document = new DocBuilder {
    doc("Transaction Commands", "query-transaction-clauses")
    synopsis("This section explains the `SHOW TRANSACTIONS` and `TERMINATION TRANSACTIONS` commands.")
    section("SHOW TRANSACTIONS", id="query-listing-transactions") {
      p("The `SHOW TRANSACTIONS` command is used to display running transactions.")
      p(
        """
          #[NOTE]
          #====
          #The command `SHOW TRANSACTIONS` only outputs the default output; for a full output use the optional `YIELD` command.
          #Full output: `SHOW TRANSACTIONS YIELD *`.
          #====""".stripMargin('#'))
      p("This command will produce a table with the following columns:")
      p(
        """
.List functions output
[options="header", cols="4,6"]
||===
|| Column
|| Description

|m|database
|a|The name of the database running the transaction label:default-output[]

|m|transactionId
|a|The id of the transaction label:default-output[]

|m|currentQueryId
|a|The id of the current query executing in the transaction label:default-output[]

|m|connectionId
|a|The id of the database connection attached to the transaction label:default-output[]

|m|clientAddress
|a|label:default-output[]

|m|username
|a|The username of the user executing the function label:default-output[]

|m|startTime
|a|label:default-output[]

|m|status
|a|terminated, blocked, closing or running label:default-output[]

|m|elapsedTime
|a|label:default-output[]

|m|allocatedBytes
|a|label:default-output[]

m|currentQuery
a|label:default-output[]

|m|outerTransactionId
|a|

|m|metaData
|a|

|m|parameters
|a|

|m|Planner
|a|

|m|runtime
|a|

|m|indexes
|a|

|m|protocol
|a|

|m|requestUri
|a|

|m|statusDetails
|a|any string a dedicated kernel API will write to track the transaction progress (for example "X nodes analyzed by the algo Y"

|m|resourceInformation
|a|

|m|activeLockCount
|a|

|m|cpuTime
|a|

|m|waitTime
|a|

|m|idleTime
|a|

|m|allocatedDirectBytes
|a|

|m|estimatedUsedHeapMemory
|a|

|m|pageHits
|a|

|m|pageFaults
|a|
|===""")
      section("Syntax") {
        p(
          """
List functions, either all or only built-in or user-defined::

[source, cypher, role=noplay]
----
SHOW TRANSACTIONS[S]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and may not be omitted.
====

Required privilege <<administration-security-administration-dbms-privileges-user-management,`SHOW USER`>>.
This command cannot be used for LDAP users.

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and may not be omitted.
====""".stripMargin('#'))
      }
      section("Listing all transactions") {
        p(
          """To list all available functions with the default output columns, the `SHOW FUNCTIONS` command can be used.
            #If all columns are required, use `SHOW FUNCTIONS YIELD *`.""".stripMargin('#'))
        showTransactionsQuery("SHOW TRANSACTIONS", List("MATCH (n) RETURN n"), ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("database", "transactionId", "currentQueryId", "connectionId", "clientAddress", "username", "currentQuery", "startTime", "status", "elapsedTime", "allocatedBytes")
        })) {
          limitedResultTable(20)
        }
      }
      section("Listing transactions with filtering on output columns") {
        p(
          """The listed transactions can be filtered by using the `WHERE` clause.
            #For example, getting the name of all built-in functions starting with the letter 'a':""".stripMargin('#'))
        showTransactionsQuery(
          """SHOW TRANSACTIONS YIELD database, currentQuery WHERE currentQuery contains 'Mark'""",
          List(
            "MATCH (p:Person) WHERE p.name='Mark' RETURN p",
            "MATCH (n) RETURN n"
          ),
          ResultAssertions(p => {
            p.columns should contain theSameElementsAs Array("database", "currentQuery")
            p.columnAs[String]("database").foreach(_ should be("neo4j"))
            p.columnAs[String]("currentQuery").foreach(_ should include("Mark"))
          })) {
          limitedResultTable(15)
        }
      }
    }
    section("TERMINATE TRANSACTIONS", id="query-terminate-transactions") {
      p("The `TERMINATE TRANSACTIONS` command is used to terminate running transactions by their ids.")
    }
  }.build()
}
