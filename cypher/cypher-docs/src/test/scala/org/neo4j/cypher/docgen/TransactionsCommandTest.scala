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
|a|The ID of the query currently executing in this transaction, or an empty string if no query is currently executing. label:default-output[]
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
|a|The query text of the query currently executing in this transaction, or an empty string if no query is currently executing. label:default-output[]
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

|m|outerTransactionId
|a|The ID of this transaction's outer transaction, if such exists, otherwise an empty string. For details, see <<subquery-call-in-transactions, `CALL { ... } IN TRANSACTIONS`>>.
|m|STRING

|m|metaData
|a|Any metadata associated with the transaction or an empty map if there is none.
|m|MAP

|m|parameters
|a|A map containing all the parameters used by the query currently executing in this transaction, or an empty map if no query is currently executing.
|m|MAP

|m|planner
|a|The name of the Cypher planner used to plan the query currently executing in this transaction, or an empty string if no query is currently executing. For details, see <<cypher-planner, Cypher planner>>.
|m|STRING

|m|runtime
|a|The name of the Cypher runtime used by the query currently executing in this transaction, or an empty string if no query is currently executing. For details, see <<cypher-runtime, Cypher runtime>>.
|m|STRING

|m|indexes
|a|The indexes utilised by the query currently executing in this transaction, or an empty list if no query is currently executing.
|m|LIST OF MAP

m|currentQueryStartTime
a|The time at which the query currently executing in this transaction was started, or an empty string if no query is currently executing.
m|STRING

|m|protocol
|a|The protocol used by the connection issuing the transaction.
|This is not necessarily an internet protocol, such as _http_, et.c., although it could be. It might also be "embedded", for example, if this connection represents an embedded session.
|m|STRING

|m|requestUri
|a|The request URI used by the client connection issuing the transaction, or `null` if the URI is not available.
|m|STRING

m|currentQueryStatus
a|The current status of the query currently executing in this transaction (`parsing`, `planning`, `planned`, `running`, or `waiting`), or an empty string if no query is currently executing.
m|STRING

|m|statusDetails
|a|Provide additional status details from the underlying transaction or an empty string if none is available.
|m|STRING

|m|resourceInformation
|a|Information about any blocked transactions or an empty map if there is none.
|m|MAP

|m|activeLockCount
|a|Count of active locks held by the transaction.
|m|LONG

|m|currentQueryActiveLockCount
|a|Count of active locks held by the query currently executing in this transaction, or `null` if no query is currently executing.
|m|LONG

|m|cpuTime
|a|CPU time that has been actively spent executing the transaction or `null` if unavailable.
|m|DURATION

|m|waitTime
|a|Wait time that has been spent waiting to acquire locks.
|m|DURATION

|m|idleTime
|a|Idle time for this transaction or `null` if unavailable.
|m|DURATION

m|currentQueryElapsedTime
a|The time that has elapsed since the query currently executing in this transaction was started, or `null` if no query is currently executing.
m|DURATION

|m|currentQueryCpuTime
|a|CPU time that has been actively spent executing the query currently executing in this transaction, or `null` if unavailable or no query is currently executing.
|m|DURATION

|m|currentQueryWaitTime
|a|Wait time that has been spent waiting to acquire locks for the query currently executing in this transaction, or `null` if no query is currently executing.
|m|DURATION

|m|currentQueryIdleTime
|a|Idle time for the query currently executing in this transaction, or `null` if unavailable or no query is currently executing.
|m|DURATION

m|currentQueryAllocatedBytes
a|The number of bytes allocated on the heap so far by the query currently executing in this transaction, or `null` if unavailable or no query is currently executing.
m|LONG

|m|allocatedDirectBytes
|a|Amount of off-heap (native) memory allocated by the transaction in bytes or `null` if unavailable.
|m|LONG

|m|estimatedUsedHeapMemory
|a|The estimated amount of used heap memory allocated by the transaction in bytes or `null` if unavailable.
|m|LONG

|m|pageHits
|a|The total number of page cache hits that the transaction performed.
|m|LONG

|m|pageFaults
|a|The total number of page cache faults that the transaction performed.
|m|LONG

|m|currentQueryPageHits
|a|The total number of page cache hits that the query currently executing in this transaction performed.
|m|LONG

|m|currentQueryPageFaults
|a|The total number of page cache faults that the query currently executing in this transaction performed.
|m|LONG

|m|initializationStackTrace
|a|The initialization stacktrace for this transaction or an empty string if unavailable.
|m|STRING
||===""")
      p("The `SHOW TRANSACTIONS` command can be combined with multiple `SHOW TRANSACTIONS` and `TERMINATE TRANSACTIONS`, see <<query-combine-tx-commands, transaction commands combination>>.")
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

The format of `transaction-id` is `<databaseName>-transaction-<id>`.
Transaction IDs must be supplied as one or more comma-separated quoted strings or as an expression resolving to a string or a list of strings.

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
            p.columns should contain theSameElementsAs
              Array("database", "transactionId", "currentQueryId", "connectionId", "clientAddress", "username", "currentQuery", "startTime", "status", "elapsedTime")
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
        p(
          """Several of the output columns have the `duration` type, which can be hard to read.
            #They can instead be returned in a more readable format:""".stripMargin('#'))
        backgroundQueries(List(
          "MATCH (n) RETURN n",
          "UNWIND range(1,100000) AS x CREATE (:Number {value: x}) RETURN x"
        )) {
          query(
            """SHOW TRANSACTIONS
              #YIELD transactionId, elapsedTime, cpuTime, waitTime, idleTime,
              #      currentQueryElapsedTime, currentQueryCpuTime, currentQueryWaitTime, currentQueryIdleTime
              #RETURN transactionId AS txId,
              #       elapsedTime.milliseconds AS elapsedTimeMillis,
              #       cpuTime.milliseconds AS cpuTimeMillis,
              #       waitTime.milliseconds AS waitTimeMillis,
              #       idleTime.seconds AS idleTimeSeconds,
              #       currentQueryElapsedTime.milliseconds AS currentQueryElapsedTimeMillis,
              #       currentQueryCpuTime.milliseconds AS currentQueryCpuTimeMillis,
              #       currentQueryWaitTime.microseconds AS currentQueryWaitTimeMicros,
              #       currentQueryIdleTime.seconds AS currentQueryIdleTimeSeconds""".stripMargin('#'),
            ResultAssertions(p => {
              p.columns should contain theSameElementsAs Array(
                "txId",
                "elapsedTimeMillis",
                "cpuTimeMillis",
                "waitTimeMillis",
                "idleTimeSeconds",
                "currentQueryElapsedTimeMillis",
                "currentQueryCpuTimeMillis",
                "currentQueryWaitTimeMicros",
                "currentQueryIdleTimeSeconds"
              )
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
              |[role="queryresult",options="header,footer",cols="10*<m"]
              ||===
              || +database+ | +transactionId+ | +currentQueryId+ | +connectionId+ | +clientAddress+ | +username+ | +currentQuery+ | +startTime+ | +status+ | +elapsedTime+
              || +"neo4j"+ | +"neo4j-transaction-3"+ | +"query-1"+ | +""+ | +""+ | +""+ | +"MATCH (n) RETURN n"+ | +"2021-10-20T08:29:39.423Z"+ | +"Running"+ | +PT2.603S+
              |10+d|Rows: 1
              ||===
              |""")
      }
    }
    section("TERMINATE TRANSACTIONS", id="query-terminate-transactions") {
      p("The `TERMINATE TRANSACTIONS` command is used to terminate running transactions by their IDs.")
      p(
        """
          #[NOTE]
          #====
          #For the `TERMINATE TRANSACTIONS` command there is no difference between the default output and full output, all columns are default.
          #====""".stripMargin('#'))
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
      p("The `TERMINATE TRANSACTIONS` command can be combined with multiple `SHOW TRANSACTIONS` and `TERMINATE TRANSACTIONS`, see <<query-combine-tx-commands, transaction commands combination>>.")
      section("Syntax") {
        p(
          """
Terminate transactions by ID on the current server::

[source, cypher, role=noplay]
----
TERMINATE TRANSACTION[S] transaction_id[, ...]
[YIELD { * \| field[, ...] }
  [ORDER BY field[, ...]]
  [SKIP n]
  [LIMIT n]
  [WHERE expression]
  [RETURN field[, ...] [ORDER BY field[, ...]] [SKIP n] [LIMIT n]]
]
----

The format of `transaction-id` is `<databaseName>-transaction-<id>`.
Transaction IDs must be supplied as one or more comma-separated quoted strings or as an expression resolving to a string or a list of strings.

[NOTE]
====
When using the `WHERE` or `RETURN` clauses, the `YIELD` clause is mandatory and must not be omitted.
====

A user with the <<access-control-database-administration-transaction, `TERMINATE TRANSACTION`>> privilege can terminate transactions in accordance with the privilege grants.
All users may terminate their own currently executing transactions.
""".stripMargin('#'))
      }
      section("Terminate transactions") {
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
            |3+d|Rows: 2
            ||===""")
      }
      section("Terminate transactions with filtering on output columns") {
        p(
          """The output from the `TERMINATE TRANSACTIONS` command can be filtered using the `YIELD` and `WHERE` clauses.
            #For example, returning the transaction ids and message for the transactions that didn't terminate:""".stripMargin('#'))
        query(
          """TERMINATE TRANSACTIONS "neo4j-transaction-1","neo4j-transaction-2"
            #YIELD transactionId, message
            #WHERE message <> "Transaction terminated."""".stripMargin('#'), ResultAssertions(p => {
          p.columns should contain theSameElementsAs Array("transactionId", "message")
        })) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="2*<m"]
            ||===
            || +transactionId+ | +message+
            || +"neo4j-transaction-2"+ | +"Transaction not found."+
            |2+d|Rows: 1
            ||===""")
        p("""In difference to `SHOW TRANSACTIONS`, `TERMINATE TRANSACTIONS` doesn't allow `WHERE` without `YIELD`:""")
        query(
          """TERMINATE TRANSACTIONS "neo4j-transaction-1","neo4j-transaction-2"
            #WHERE message <> "Transaction terminated."""".stripMargin('#'), ErrorAssertions(error =>
            error.getMessage should startWith("`WHERE` is not allowed by itself, please use `TERMINATE TRANSACTION ... YIELD ... WHERE ...` instead")
          )) {
          errorOnlyResultTable()
        }
      }
    }
    section("Combining transaction commands", id="query-combine-tx-commands") {
      p(
        """
In difference to other show commands, the `SHOW` and `TERMINATE TRANSACTIONS` commands can be combined in the same query.

[NOTE]
====
When combining multiple commands the `YIELD` and `RETURN` clauses are mandatory and must not be omitted.
In addition, the `YIELD` clause needs to explicitly list the yielded columns, `YIELD *` is not permitted.
====

[NOTE]
====
At this point in time, no other cypher clauses are allowed to be combined with the transaction commands.
====
""".stripMargin('#'))
      section("Terminating all transactions by a given user") {
        p(
          """To terminate all transactions by a user, first find the transactions using `SHOW TRANSACTIONS` then pass them onto `TERMINATE TRANSACTIONS`.""")
        query(
          """SHOW TRANSACTIONS
            #YIELD transactionId AS txId, username AS user
            #WHERE user = "Alice"
            #TERMINATE TRANSACTIONS txId
            #YIELD message
            #RETURN txId, message""".stripMargin('#'), NoAssertions) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="2*<m"]
            ||===
            || +txId+ | +message+
            || +"neo4j-transaction-1"+ | +"Transaction terminated."+
            || +"neo4j-transaction-2"+ | +"Transaction terminated."+
            |2+d|Rows: 2
            ||===""")
      }
      section("Terminating starving transactions") {
        p(
          """To terminate transactions that have been waiting for more than 30 minutes,
            #first find the transactions using `SHOW TRANSACTIONS` then pass them onto `TERMINATE TRANSACTIONS`.
            #
            #The following example finds one such transaction that has been waiting for 40 minutes.""".stripMargin('#'))
        query(
          """SHOW TRANSACTIONS
            #YIELD transactionId, waitTime
            #WHERE waitTime > duration({minutes: 30})
            #TERMINATE TRANSACTIONS transactionId
            #YIELD username, message
            #RETURN *""".stripMargin('#'), NoAssertions) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="4*<m"]
            ||===
            || +transactionId+ | +waitTime+ | +username+ | +message+
            || +"neo4j-transaction-1"+ | +PT40M+ | +"Alice"+ | +"Transaction terminated."+
            |4+d|Rows: 1
            ||===""")
      }
      section("Listing other transactions by the same user that was terminated") {
        p(
          """To list remaining transactions by users whose transactions got terminated,
            #first terminate the transactions using `TERMINATE TRANSACTIONS` then filter on the users from `SHOW TRANSACTIONS`.""".stripMargin('#'))
        query(
          """TERMINATE TRANSACTION 'neo4j-transaction-1', 'neo4j-transaction-2'
            #YIELD username AS terminatedUser
            #SHOW TRANSACTIONS
            #YIELD username AS showUser, transactionId AS txId, database, currentQuery, status
            #WHERE showUser = terminatedUser AND NOT status STARTS WITH 'Terminated'
            #RETURN txId, showUser AS user, database, currentQuery""".stripMargin('#'), NoAssertions) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="4*<m"]
            ||===
            || +txId+ | +user+ | +database+ | +currentQuery+
            || +"neo4j-transaction-3"+ | +"Alice"+ | +"neo4j"+ | +"MATCH (n) RETURN n"+
            || +"mydb-transaction-1"+ | +"Bob"+ | +"mydb"+ | +"MATCH (n:Label) SET n.prop=false"+
            || +"system-transaction-999"+ | +"Bob"+ | +"system"+ | +"SHOW DATABASES"+
            |4+d|Rows: 2
            ||===""")
      }
      section("Listing other transactions by the same user as a given transaction") {
        p(
          """To list the other transactions by the same user as a given transaction,
            #first find the transactions using `SHOW TRANSACTIONS` then filter on the users in a second `SHOW TRANSACTIONS`.""".stripMargin('#'))
        query(
          """SHOW TRANSACTION 'neo4j-transaction-1'
            #YIELD username AS originalUser, transactionId AS originalTxId
            #SHOW TRANSACTIONS
            #YIELD username AS newUser, transactionId AS txId, database, currentQuery, status
            #WHERE newUser = originalUser AND NOT txId = originalTxId
            #RETURN txId, newUser AS user, database, currentQuery, status""".stripMargin('#'), NoAssertions) {}
        p(
          """.Result
            |[role="queryresult",options="header,footer",cols="5*<m"]
            ||===
            || +txId+ | +user+ | +database+ | +currentQuery+ | +status+
            || +"mydb-transaction-1"+ | +"Bob"+ | +"mydb"+ | +"MATCH (n:Label) SET n.prop=false"+ | +"Running"+
            || +"system-transaction-999"+ | +"Bob"+ | +"system"+ | +"SHOW DATABASES"+ | +"Running"+
            |5+d|Rows: 2
            ||===""")
      }
    }
  }.build()
}
