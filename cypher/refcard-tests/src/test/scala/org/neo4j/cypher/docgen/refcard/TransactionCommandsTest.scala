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
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.graphdb.Transaction

class TransactionCommandsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List()
  val title = "SHOW and TERMINATE TRANSACTIONS"
  override val linkId = "clauses/transaction-clauses"

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "show" =>
        assert(result.toList.size === 1)
      case "empty" =>
        assert(result.toList.size === 0)
    }
  }

  def text = """
###assertion=show
//

SHOW TRANSACTIONS
###

Listing all available transactions.

###dontrun
//

TERMINATE TRANSACTIONS 'neo4j-transaction-42'
###

Terminate the transaction with ID `neo4j-transaction-42`.

###empty
//

SHOW TRANSACTIONS
YIELD username, transactionId AS txId
WHERE username = 'MyUser'
TERMINATE TRANSACTIONS txId
YIELD message
RETURN message, txId
###

Terminate all transactions by MyUser.

"""
}
