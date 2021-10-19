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
.List transactions output
[options="header", cols="4,6"]
||===
|| Column
|| Description

|m|database
|a|The name of the database running the transaction label:default-output[]

|m|transactionId
|a|The id of the transaction label:default-output[]

|m|currentQueryId
|a|The id of the query current executing in the transaction label:default-output[]

|m|connectionId
|a|The id of the database connection attached to the transaction or blank for embedded connections. label:default-output[]

|m|clientAddress
|a|The address the client is connecting from, or blank if this is not available. label:default-output[]

|m|username
|a|The username of the user executing the transaction. label:default-output[]

|m|startTime
|a|The time the transaction was started. label:default-output[]

|m|status
|a|The current status of this transaction (`Terminated`, `Blocked`, `Closing` or `Running`). label:default-output[]

|m|elapsedTime
|a|The total time elapsed since the start of this transaction. label:default-output[]

|m|allocatedBytes
|a|The number of bytes allocated on the heap by this transaction. label:default-output[]

m|currentQuery
a|The query text of the query of the query current executing in the transaction. label:default-output[]

|m|outerTransactionId
|a|

|m|metaData
|a|Metadata attached to the transaction, or empty if there is none.

|m|parameters
|a|The parameters passed to the query currently executing in this transaction.

|m|Planner
|a|The name of the Cypher planned used to plan the query currently executing in this transaction.

|m|runtime
|a|The name of the Cypher runtime used by the query currently executing in this transaction.

|m|indexes
|a|The indexes utilised by the query currently executing in this transaction.

|m|protocol
|a| Which protocol was used for this connection.
|This is not necessarily an internet protocol (like http et.c.) although it could be. It might also be "embedded" for example, if this connection represents an embedded session.

|m|requestUri
|a|The URI of this server that the client connected to, or null if the URI is not available.

|m|statusDetails
|a|Provide additional status details from underlying transaction or blank if none is available.

|m|resourceInformation
|a|

|m|activeLockCount
|a|The number of active locks granted for this transaction.

|m|cpuTime
|a|CPU time (in milliseconds) utilised by this transaction

|m|waitTime
|a|Accumulated transaction waiting time that includes waiting time of all already executed queries plus waiting time of the currently executed query in milliseconds.

|m|idleTime
|a|Idle time (in milliseconds) for this transaction

|m|allocatedDirectBytes
|a|Amount of off-heap (native) memory allocated by this transaction in bytes.

|m|estimatedUsedHeapMemory
|a|The estimated amount of used heap memory allocated by this transaction in bytes.

|m|pageHits
|a|The total number of page cache hits that this transaction performed.

|m|pageFaults
|a|The total number of page cache faults that this transaction performed.
||===""")
      section("Syntax") {
        p(
          """
List all transactions on the current server::

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

Required privilege <<access-control-database-administration-transaction,`SHOW TRANSACTION`>>.
""".stripMargin('#'))
      }
      section("Listing all transactions") {
        p(
          """To list all available transactions with the default output columns, the `SHOW TRANSACTIONS` command can be used.
            #If all columns are required, use `SHOW TRANSACTIONS YIELD *`.""".stripMargin('#'))
        showTransactionsQuery("SHOW TRANSACTIONS", List("MATCH (n) RETURN n"), ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("database", "transactionId", "currentQueryId", "connectionId", "clientAddress", "username", "currentQuery", "startTime", "status", "elapsedTime", "allocatedBytes")
        })) {
          limitedResultTable(20)
        }
      }
      section("Listing transactions with filtering on output columns") {
        p(
          """The listed transactions can be filtered by using the `WHERE` clause.
            #For example, getting the databases for all transactions where the currently executing query contains 'Mark':""".stripMargin('#'))
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
