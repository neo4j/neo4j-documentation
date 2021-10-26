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

class TransactionsCommandTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/listing"

  override def doc: Document = new DocBuilder {
    doc("Transaction commands", "query-transaction-clauses")
    synopsis("This section explains the `SHOW TRANSACTIONS` and `TERMINATION TRANSACTIONS` commands.")
    section("SHOW TRANSACTIONS", id="query-listing-transactions") {
      p("The `SHOW TRANSACTIONS` command is used to display running transactions within the instance.")
      p(
        """
          #[NOTE]
          #====
          #The command `SHOW TRANSACTIONS` returns only the default output. For a full output use the optional `YIELD` command.
          #Full output: `SHOW TRANSACTIONS YIELD *`.
          #====""".stripMargin('#'))
      p("This command will produce a table with the following columns:")
      p(
        """
.List transactions output
[options="header", cols="4,6,2"]
||===
|| Column
|| Description
|| Type

|m|database
|a|The name of the database the transaction is executing against. label:default-output[]
|m|STRING

|m|transactionId
|a|The transaction ID. label:default-output[]
|m|STRING

|m|currentQueryId
|a|The ID of the query currently executing in this transaction. label:default-output[]
|m|STRING

|m|connectionId
|a|The ID of the database connection attached to the transaction or an empty string for embedded connections. label:default-output[]
|m|STRING

|m|clientAddress
|a|The client address of the connection issuing the transaction or an empty string if unavailable. label:default-output[]
|m|STRING

|m|username
|a|The username of the user executing the transaction. label:default-output[]
|m|STRING

|m|currentQuery
|a|The query text of the query currently executing in this transaction. label:default-output[]
|m|STRING

|m|startTime
|a|The time at which the transaction was started. label:default-output[]
|m|STRING

|m|status
|a|The current status of the transaction (`Terminated`, `Blocked`, `Closing` or `Running`). label:default-output[]
|m|STRING

|m|elapsedTime
|a|The time that has elapsed since the transaction was started. label:default-output[]
|m|DURATION

|m|allocatedBytes
|a|The number of bytes allocated on the heap so far by the transaction. label:default-output[]
|m|LONG

|m|outerTransactionId
|a|The ID of this transaction's outer transaction if such exists. For details, see <<subquery-call-in-transactions, `CALL { ... } IN TRANSACTIONS`>>.
|m|STRING

|m|metaData
|a|Any metadata associated with the transaction or empty if there is none.
|m|MAP

|m|parameters
|a|A map containing all the parameters used by the query currently executing in this transaction.
|m|MAP

|m|planner
|a|The name of the Cypher planner used to plan the query currently executing in this transaction. For details, see <<cypher-planner, Cypher planner>>.
|m|STRING

|m|runtime
|a|The name of the Cypher runtime used by the query currently executing in this transaction. For details, see <<cypher-runtime, Cypher runtime>>.
|m|STRING

|m|indexes
|a|The indexes utilised by the query currently executing in this transaction.
|m|LIST OF MAP

|m|protocol
|a|The protocol used by the connection issuing the transaction.
|This is not necessarily an internet protocol, such as _http_, et.c., although it could be. It might also be "embedded", for example, if this connection represents an embedded session.
|m|STRING

|m|requestUri
|a|The request URI used by the client connection issuing the transaction, or null if the URI is not available.
|m|STRING

|m|statusDetails
|a|Provide additional status details from the underlying transaction or an empty string if none is available.
|m|STRING

|m|resourceInformation
|a|Information about any blocked transactions.
|m|MAP

|m|activeLockCount
|a|Count of active locks held by the transaction.
|m|LONG

|m|cpuTime
|a|CPU time that has been actively spent executing the transaction.
|m|DURATION

|m|waitTime
|a|Wait time that has been spent waiting to acquire locks.
|m|DURATION

|m|idleTime
|a|Idle time for this transaction.
|m|DURATION

|m|allocatedDirectBytes
|a|Amount of off-heap (native) memory allocated by the transaction in bytes.
|m|LONG

|m|estimatedUsedHeapMemory
|a|The estimated amount of used heap memory allocated by the transaction in bytes.
|m|LONG

|m|pageHits
|a|The total number of page cache hits that the transaction performed.
|m|LONG

|m|pageFaults
|a|The total number of page cache faults that the transaction performed.
|m|LONG
||===""")
      section("Syntax") {
        p(
          """
List transactions on the current server::

[source, cypher, role=noplay]
----
SHOW TRANSACTION[S] [transaction-id[,...]]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

The format of `transaction-id` is `<databaseName>-transaction-<id>`. Transaction IDs must be supplied as a comma-separated list of one or more quoted strings, a string parameter, or a list parameter.

[NOTE]
====
When using the `RETURN` clause, the `YIELD` clause is mandatory and must not be omitted.
====
A user with the <<access-control-database-administration-transaction, `SHOW TRANSACTION`>> privilege can view the currently executing transactions in accordance with the privilege grants.
All users may view all of their own currently executing transactions.
""".stripMargin('#'))
      }
      section("Listing all transactions") {
        p(
          """To list all available transactions with the default output columns, use the `SHOW TRANSACTIONS` command.
            #If all columns are required, use `SHOW TRANSACTIONS YIELD *`.""".stripMargin('#'))
        backgroundQueries(List("MATCH (n) RETURN n")) {
          query("SHOW TRANSACTIONS", ResultAssertions(p => {
            p.columns should contain theSameElementsAs Array("database", "transactionId", "currentQueryId", "connectionId", "clientAddress", "username", "currentQuery", "startTime", "status", "elapsedTime", "allocatedBytes")
          })) {
            resultTable()
          }
        }
      }
      section("Listing transactions with filtering on output columns") {
        p(
          """The listed transactions can be filtered by using the `WHERE` clause.
            #For example, getting the databases for all transactions where the currently executing query contains 'Mark':""".stripMargin('#'))
        backgroundQueries(List(
          "MATCH (p:Person) WHERE p.name='Mark' RETURN p",
          "MATCH (n) RETURN n"
        )) {
          query("""SHOW TRANSACTIONS YIELD database, currentQuery WHERE currentQuery contains 'Mark'""",
            ResultAssertions(p => {
              p.columns should contain theSameElementsAs Array("database", "currentQuery")
              p.columnAs[String]("database").foreach(_ should be("neo4j"))
              p.columnAs[String]("currentQuery").foreach(_ should include("Mark"))
            })) {
            resultTable()
          }
        }
      }
      section("Listing specific transactions") {
        p("""It is possible to specify which transactions to return in the list by transaction ID.""")
          query("""SHOW TRANSACTIONS "neo4j-transaction-3"""", NoAssertions) { }
          p(
            """
              |.Result
              |[role="queryresult",options="header,footer",cols="11*<m"]
              ||===
              || +database+ | +transactionId+ | +currentQueryId+ | +connectionId+ | +clientAddress+ | +username+ | +currentQuery+ | +startTime+ | +status+ | +elapsedTime+ | +allocatedBytes+
              || +"neo4j"+ | +"neo4j-transaction-3"+ | +"query-1"+ | +""+ | +""+ | +""+ | +"MATCH (n) RETURN n"+ | +"2021-10-20T08:29:39.423Z"+ | +"Running"+ | +PT2.603S+ | +0+
              |11+d|Rows: 2
              ||===
              |""")
      }
    }
    section("TERMINATE TRANSACTIONS", id="query-terminate-transactions") {
      p("The `TERMINATE TRANSACTIONS` command is used to terminate running transactions by their IDs.")
      p("This command will produce a table with the following columns:")
      p(
        """
.Terminate transactions output
[options="header", cols="4,6,2"]
||===
|| Column
|| Description
|| Type

|m|transactionId
|a|The transaction ID.
|m|STRING

|m|username
|a|The username of the user executing the transaction.
|m|STRING

|m|message
|a|The result of the `TERMINATE TRANSACTION` command as applied to this transaction.
|m|STRING
||===""")
      section("Syntax") {
        p(
          """
Terminate transactions by ID on the current server::

[source, cypher, role=noplay]
----
TERMINATE TRANSACTION[S] transaction_id[, ...]
----

The format of `transaction-id` is `<databaseName>-transaction-<id>`. Transaction IDs must be supplied as a comma-separated list of one or more quoted strings, a string parameter, or a list parameter.

A user with the <<access-control-database-administration-transaction, `TERMINATE TRANSACTION`>> privilege can terminate transactions in accordance with the privilege grants.
All users may terminate their own currently executing transactions.
""".stripMargin('#'))
      }
      section("Terminate Transactions") {
        p(
          """To end running transactions without waiting for them to complete on their own, use the `TERMINATE TRANSACTIONS` command.""")
        query("""TERMINATE TRANSACTIONS "neo4j-transaction-1","neo4j-transaction-2"""", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("transactionId", "username", "message")
        })) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="3*<m"]
            ||===
            || +transactionId+ | +username+ | +message+
            || +"neo4j-transaction-1"+ | +"neo4j"+ | +"Transaction terminated."+
            || +"neo4j-transaction-2"+ | +null+ | +"Transaction not found."+
            |3+d|Rows: 1
            ||===""")
      }
    }
  }.build()
}
