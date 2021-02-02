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
package org.neo4j.cypher.docgen.tooling.tests

import org.neo4j.cypher.docgen.ExecutionEngineFactory
import org.neo4j.cypher.docgen.tooling._
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, ResultSubscriber}
import org.neo4j.cypher.{ExecutionEngineHelper, GraphIcing}
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.impl.coreapi.InternalTransaction
import org.neo4j.values.virtual.VirtualValues
import org.scalatest._

class QueryResultContentBuilderTest extends Suite
                                    with FunSuiteLike
                                    with Assertions
                                    with Matchers
                                    with GraphIcing
                                    with ExecutionEngineHelper
                                    with BeforeAndAfterAll {

  def graph: GraphDatabaseCypherService = _graph
  var _managementService: DatabaseManagementService = _
  var _graph: GraphDatabaseCypherService = _
  var _db: GraphDatabaseService = _
  def eengine: ExecutionEngine = _eengine
  var _eengine: ExecutionEngine = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val (managementService, db, engine) = ExecutionEngineFactory.createDbAndCommunityEngine()
    _managementService = managementService
    _db = db;
    _graph = new GraphDatabaseCypherService(db)
    _eengine = engine
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    _managementService.shutdown()
  }

  test("should handle query with result table output and empty results") {
    val result = runQuery("match (n) return n")

    result should equal(QueryResultTable(Seq("n"), Seq.empty, footer = "0 rows"))
  }

  test("should handle query with result table output and non-empty results") {
    val result = runQuery("MATCH (x) RETURN x", init = "CREATE ()").asInstanceOf[QueryResultTable]

    result.columns should equal(Seq("x"))
    result.footer should equal("1 row")
    result.rows should have size 1
  }

  def runQuery(query: String, init: String = ""): Content = {
    val transaction = _db.beginTx()
    try {
      if (init != "") transaction.execute(init)
      val builder = new QueryResultContentBuilder(x => x.toString)
      val txContext = graph.transactionalContext(transaction.asInstanceOf[InternalTransaction], query = query -> Map())
      val subscriber = new ResultSubscriber(txContext)
      val execution = eengine.execute(query,
        VirtualValues.EMPTY_MAP,
        txContext,
        profile = false,
        prePopulate = false,
        subscriber)
      subscriber.init(execution)
      val queryResult = DocsExecutionResult(subscriber, txContext)
      builder.apply(queryResult)
    }
    finally {
      transaction.close()
    }
  }
}
