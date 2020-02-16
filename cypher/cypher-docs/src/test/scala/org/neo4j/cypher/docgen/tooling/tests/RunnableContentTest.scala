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

import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.docgen.tooling._
import org.scalatest.Assertions
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import org.scalatest.Suite

class RunnableContentTest extends Suite
                          with FunSuiteLike
                          with Assertions
                          with Matchers
                          with GraphIcing  {
  test("graph viz includes all init queries, and the actual query when inside a Query object") {
    val graphVizPlaceHolder = new GraphVizPlaceHolder("")
    val tablePlaceHolder = new TablePlaceHolder(NoAssertions)
    val queryObject = Query("5", NoAssertions, RunnableInitialization(initQueries = Seq("3", "4")), graphVizPlaceHolder ~ tablePlaceHolder, Seq.empty)
    val doc = Document("title", "id", RunnableInitialization(initQueries = Seq("1","2")), queryObject)

    doc.contentWithQueries should equal(Seq(
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2", "3", "4")), Some(InitializationQuery("5")), graphVizPlaceHolder),
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2", "3", "4")), Some(InitializationQuery("5")), tablePlaceHolder)
    ))
  }

  test("graph viz includes all init queries, and the actual query becomes the last init query when NOT inside a Query object") {
    val graphVizPlaceHolder = new GraphVizPlaceHolder("")
    val sectionObject = Section("", None, RunnableInitialization(initQueries = Seq("3", "4")), graphVizPlaceHolder)
    val doc = Document("title", "id", RunnableInitialization(initQueries = Seq("1","2")), sectionObject)

    doc.contentWithQueries should equal(Seq(
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2", "3", "4")), None, graphVizPlaceHolder)
    ))

    doc.contentWithQueries.head.initKey should equal(RunnableInitialization(initQueries = Seq("1", "2", "3")))
    doc.contentWithQueries.head.queryToPresent should equal("4")
  }
}
