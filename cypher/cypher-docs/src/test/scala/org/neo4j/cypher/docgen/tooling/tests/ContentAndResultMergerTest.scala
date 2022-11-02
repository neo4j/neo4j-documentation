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

import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.docgen.tooling._
import org.scalatest.Assertions
import org.scalatest.Suite
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class ContentAndResultMergerTest extends Suite
    with AnyFunSuiteLike
    with Assertions
    with Matchers
    with GraphIcing {

  val GRAPHVIZ_RESULT = GraphViz("APA")
  val TABLE_RESULT = Paragraph("14")
  val QUERY = "match (n) return n"
  val TABLE_PLACEHOLDER = new TablePlaceHolder(NoAssertions)
  val GRAPHVIZ_PLACEHOLDER = new GraphVizPlaceHolder("")

  test("simple doc with query") {
    // given
    val doc = Document("title", "myId", init = RunnableInitialization.empty, TABLE_PLACEHOLDER)

    val testResult = TestRunResult(Seq(QueryRunResult(QUERY, TABLE_PLACEHOLDER, Right(TABLE_RESULT))))

    // when
    val result = contentAndResultMerger(doc, testResult)

    // then
    result should equal(
      Document("title", "myId", init = RunnableInitialization.empty, TABLE_RESULT)
    )
  }

  test("simple doc with GraphVizBefore") {
    // given
    val doc = Document("title", "myId", init = RunnableInitialization.empty, GRAPHVIZ_PLACEHOLDER)

    val testResult = TestRunResult(Seq(GraphVizRunResult(GRAPHVIZ_PLACEHOLDER, GRAPHVIZ_RESULT)))

    // when
    val result = contentAndResultMerger(doc, testResult)

    // then
    result should equal(
      Document("title", "myId", init = RunnableInitialization.empty, GRAPHVIZ_RESULT)
    )
  }

  test("doc with GraphVizBefore and Result Table without Query") {
    // given
    val doc = Document("title", "myId", init = RunnableInitialization.empty, GRAPHVIZ_PLACEHOLDER ~ TABLE_PLACEHOLDER)

    val testResult = TestRunResult(Seq(
      GraphVizRunResult(GRAPHVIZ_PLACEHOLDER, GRAPHVIZ_RESULT),
      QueryRunResult(QUERY, TABLE_PLACEHOLDER, Right(TABLE_RESULT))
    ))

    // when
    val result = contentAndResultMerger(doc, testResult)

    // then
    result should equal(
      Document("title", "myId", init = RunnableInitialization.empty, GRAPHVIZ_RESULT ~ TABLE_RESULT)
    )
  }

  test("doc with GraphVizBefore and Result Table within Query") {
    // given
    val queryObj =
      Query(QUERY, NoAssertions, RunnableInitialization.empty, TABLE_PLACEHOLDER ~ GRAPHVIZ_PLACEHOLDER, Seq.empty)
    val doc = Document("title", "myId", init = RunnableInitialization.empty, queryObj)

    val testResult = TestRunResult(Seq(
      QueryRunResult(QUERY, TABLE_PLACEHOLDER, Right(TABLE_RESULT)),
      GraphVizRunResult(GRAPHVIZ_PLACEHOLDER, GRAPHVIZ_RESULT)
    ))

    // when
    val result = contentAndResultMerger(doc, testResult)

    // then
    result should equal(
      Document(
        "title",
        "myId",
        init = RunnableInitialization.empty,
        Query(QUERY, NoAssertions, RunnableInitialization.empty, TABLE_RESULT ~ GRAPHVIZ_RESULT, Seq.empty)
      )
    )
  }
}
