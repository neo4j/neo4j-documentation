/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.mockito.Mockito._
import org.neo4j.cypher.CypherException
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.docgen.tooling.{RestartableDatabase, RunnableInitialization}
import org.neo4j.doc.test.TestEnterpriseGraphDatabaseFactory
import org.scalatest.Assertions
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import org.scalatest.Suite
import org.scalatest.mock.MockitoSugar

class RestartableDatabaseTest extends Suite
                              with FunSuiteLike
                              with Assertions
                              with Matchers
                              with MockitoSugar
                              with GraphIcing  {
  test("just creating a restartable database should not create any temp-dbs") {
    // given
    val databaseFactory = mock[TestEnterpriseGraphDatabaseFactory]

    // when
    new RestartableDatabase(RunnableInitialization.empty, databaseFactory)

    // then
    verify(databaseFactory, never()).newImpermanentDatabase()
  }

  test("running two read queries should only need one database") {
    // given
    val databaseFactory = spy(new TestEnterpriseGraphDatabaseFactory())
    val db = new RestartableDatabase(RunnableInitialization.empty, databaseFactory)

    // when
    db.executeWithParams("MATCH (n) RETURN n")
    db.nowIsASafePointToRestartDatabase()
    db.execute("MATCH (n) RETURN n")

    // then
    verify(databaseFactory, times(1)).newImpermanentDatabase()

    db.shutdown()
  }

  test("running two write queries should need two databases") {
    // given
    val databaseFactory = spy(new TestEnterpriseGraphDatabaseFactory())
    val db = new RestartableDatabase(RunnableInitialization.empty, databaseFactory)

    // when
    db.executeWithParams("CREATE ()")
    db.nowIsASafePointToRestartDatabase()
    db.executeWithParams("CREATE ()")

    // then
    verify(databaseFactory, times(2)).newImpermanentDatabase()

    db.shutdown()
  }

  test("running two queries that throw exception should need two databases") {
    // given
    val databaseFactory = spy(new TestEnterpriseGraphDatabaseFactory())
    val db = new RestartableDatabase(RunnableInitialization.empty, databaseFactory)

    // when
    intercept[CypherException](db.executeWithParams("THIS SHOULD FAIL"))
    db.nowIsASafePointToRestartDatabase()
    intercept[CypherException](db.executeWithParams("THIS SHOULD FAIL"))

    // then
    verify(databaseFactory, times(2)).newImpermanentDatabase()

    db.shutdown()
  }
}
