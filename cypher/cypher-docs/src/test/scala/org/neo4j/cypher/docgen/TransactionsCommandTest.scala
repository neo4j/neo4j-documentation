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
|a|This is the name of the database the transaction is executing against.:default-output[]

|m|transactionId
|a|This is the ID of the transaction label:default-output[]

|m|currentQueryId
|a|This is the ID of the current query executed by transaction. label:default-output[]

|m|connectionId
|a|The ID of the database connection attached to the transaction or blank for embedded connections. label:default-output[]

|m|clientAddress
|a|The client address of the connection issuing the transaction, or blank if this is not available. label:default-output[]

|m|username
|a|This is the username of the user who is executing the transaction. label:default-output[]

|m|currentQuery
|a|The query text of the query of the current query executed by the transaction. label:default-output[]

|m|startTime
|a|The time at which the transaction was started. label:default-output[]

|m|status
|a|The current status of this transaction (`Terminated`, `Blocked`, `Closing` or `Running`). label:default-output[]

|m|elapsedTime
|a|This is the time in milliseconds that has elapsed since the transaction was started. label:default-output[]

|m|allocatedBytes
|a|The number of bytes allocated on the heap so far by this transaction. label:default-output[]

|m|outerTransactionId
|a|The transaction ID of any outer transaction.

|m|metaData
|a|This is any metadata associated with the transaction., or empty if there is none.

|m|parameters
|a|This is a map containing all the parameters used by the query currently executing in this transaction.

|m|planner
|a|The name of the Cypher planned used to plan the query currently executing in this transaction. For details, see <<cypher-planner, Cypher planner>>

|m|runtime
|a|The name of the Cypher runtime used by the query currently executing in this transaction. For details, see <<cypher-runtime, Cypher runtime>>

|m|indexes
|a|The indexes utilised by the query currently executing in this transaction.

|m|protocol
|a|The protocol used by connection issuing the transaction.
|This is not necessarily an internet protocol (like http et.c.) although it could be. It might also be "embedded" for example, if this connection represents an embedded session.

|m|requestUri
|a|The request URI used by the client connection issuing the transaction, or null if the URI is not available.

|m|statusDetails
|a|Provide additional status details from underlying transaction or blank if none is available.

|m|resourceInformation
|a|Information about what transaction is waiting for when it is blocked.

|m|activeLockCount
|a|Count of active locks held by transaction executing the query.

|m|cpuTime
|a|CPU time in milliseconds that has been actively spent executing the query.
|This field will be null unless the config parameter `dbms.track_query_cpu_time` is set to true.

|m|waitTime
|a|Accumulated transaction waiting time that includes waiting time of all already executed queries plus waiting time of the currently executed query in milliseconds.
|This field will be null unless the config parameter `dbms.track_query_cpu_time` is set to true.

|m|idleTime
|a|Idle time (in milliseconds) for this transaction
|This field will be null unless the config parameter `dbms.track_query_cpu_time` is set to true.

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
List all transactions on the current server:

[source, cypher, role=noplay]
----
SHOW TRANSACTIONS[S] [transaction-id[,...]]
[YIELD { * | field[, ...] } [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
[WHERE expression]
[RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
----

The format of `transaction-id` is `<databaseName>-transaction-<id>` and they should be supplied as one or more quoted strings, a string parameter or a list parameter.

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
        p("""It is possible to specify which transactions to return in the list by ID.""")
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
      p("The `TERMINATE TRANSACTIONS` command is used to terminate running transactions by their ids.")
      p("This command will produce a table with the following columns:")
      p(
        """
.Terminate transactions output
[options="header", cols="4,6"]
||===
|| Column
|| Description

|m|transactionId
|a|The id of the transaction.

|m|username
|a|The username of the user executing the transaction.

|m|message
|a|The result of the `TERMINATE TRANSACTION` command as applied to this transaction.
||===""")
      section("Syntax") {
        p(
          """
Terminate transactions by ID on the current server::

[source, cypher, role=noplay]
----
TERMINATE TRANSACTIONS[S] transaction_id[, ...]
----

The format of `transaction-id` is `<databaseName>-transaction-<id>` and they should be supplied as one or more quoted strings, a string parameter or a list parameter.

Required privilege <<access-control-database-administration-transaction,`TERMINATE TRANSACTION`>>.
""".stripMargin('#'))
      }
    }
    section("Terminate Transactions") {
      p(
        """To end running transactions without waiting for them to complete on their own, the `TERMINATE TRANSACTIONS` command can be used.""")
        query("""TERMINATE TRANSACTIONS "neo4j-transaction-1","neo4j-transaction-2"""", ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("transactionId", "username", "message")
        })) { }
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
  }.build()
}
