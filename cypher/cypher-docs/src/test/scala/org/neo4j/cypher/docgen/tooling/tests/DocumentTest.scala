/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
import org.neo4j.cypher.docgen.tooling.Admonitions._
import org.neo4j.cypher.docgen.tooling._
import org.scalatest.Assertions
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import org.scalatest.Suite

class DocumentAsciiDocTest extends Suite
                           with FunSuiteLike
                           with Assertions
                           with Matchers
                           with GraphIcing  {
  test("Simplest possible document") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty, Paragraph("lorem ipsum"))

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |lorem ipsum
        |
        |""".stripMargin)
  }

  test("Heading inside Document") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty, Heading("My heading") ~ Paragraph("lorem ipsum"))

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |.My heading
        |lorem ipsum
        |
        |""".stripMargin)
  }

  test("Abstract for Document") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty, Abstract("abstract intro"))

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[abstract]
        |--
        |abstract intro
        |--
        |
        |""".stripMargin)
  }

  test("Section inside Section") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Section("outer", None, RunnableInitialization.empty,
        Paragraph("first") ~ Section("inner", None, RunnableInitialization.empty, Paragraph("second"))
      ))

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |== outer
        |
        |first
        |
        |=== inner
        |
        |second
        |
        |""".stripMargin)
  }

  test("Section with IDREF") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Section("outer", Some("IDREF1"), RunnableInitialization.empty,
        Paragraph("first") ~ Section("inner", Some("IDREF2"), RunnableInitialization.empty, Paragraph("second"))
      ))

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[[IDREF1]]
        |== outer
        |
        |first
        |
        |[[IDREF2]]
        |=== inner
        |
        |second
        |
        |""".stripMargin)
  }

  test("Tip with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Tip(Paragraph("tip text")) ~
        Tip("custom heading", Paragraph("tip text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[TIP]
        |====
        |tip text
        |
        |
        |====
        |
        |[TIP]
        |.custom heading
        |====
        |tip text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("Note with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Note(Paragraph("tip text")) ~
        Note("custom heading", Paragraph("tip text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[NOTE]
        |====
        |tip text
        |
        |
        |====
        |
        |[NOTE]
        |.custom heading
        |====
        |tip text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("Warning with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Warning(Paragraph("tip text")) ~
        Warning("custom heading", Paragraph("tip text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[WARNING]
        |====
        |tip text
        |
        |
        |====
        |
        |[WARNING]
        |.custom heading
        |====
        |tip text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("Important section with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Important(Paragraph("important text")) ~
        Important("custom heading", Paragraph("important text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[IMPORTANT]
        |====
        |important text
        |
        |
        |====
        |
        |[IMPORTANT]
        |.custom heading
        |====
        |important text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("Caution with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Caution(Paragraph("tip text")) ~
        Caution("custom heading", Paragraph("tip text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[CAUTION]
        |====
        |tip text
        |
        |
        |====
        |
        |[CAUTION]
        |.custom heading
        |====
        |tip text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("Important with and without heading") {
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Important(Paragraph("tip text")) ~
        Important("custom heading", Paragraph("tip text again"))
    )

    doc.asciiDoc should equal(
      """[[myId]]
        |= title
        |
        |[IMPORTANT]
        |====
        |tip text
        |
        |
        |====
        |
        |[IMPORTANT]
        |.custom heading
        |====
        |tip text again
        |
        |
        |====
        |
        |""".stripMargin)
  }

  test("QueryResult that creates data and returns nothing") {
    val doc = QueryResultTable(Seq(), Seq.empty, footer = "0 rows\nNodes created: 2\nRelationships created: 1\n")

    doc.asciiDoc(0) should equal(
      """.Result
        |[role="queryresult",options="footer",cols="1*<m"]
        ||===
        |1+|(empty result)
        |1+d|0 rows +
        |Nodes created: 2 +
        |Relationships created: 1
        ||===
        |
        |""".stripMargin)
  }

  test("QueryResult that creates nothing and but returns data") {
    val doc = QueryResultTable(Seq("n1", "n2"), Seq(ResultRow(Seq("1", "2"))), footer = "1 row")

    doc.asciiDoc(0) should equal(
      """.Result
        |[role="queryresult",options="header,footer",cols="2*<m"]
        ||===
        || +n1+ | +n2+
        || +1+ | +2+
        |2+d|1 row
        ||===
        |
        |""".stripMargin)
  }

  test("QueryResult that returns data containing pipes") {
    val doc = QueryResultTable(Seq("n1|x1", "n2"), Seq(ResultRow(Seq("1|2", "2"))), footer = "1 row")

    doc.asciiDoc(0) should equal(
      """.Result
        |[role="queryresult",options="header,footer",cols="2*<m"]
        ||===
        || +n1|x1+ | +n2+
        || +1|2+ | +2+
        |2+d|1 row
        ||===
        |
        |""".stripMargin)
  }

  test("Simple console data") {
    val consoleData = ConsoleData.of(Seq("global1", "global2"), Seq("local1", "local2"), "myquery")

    consoleData.asciiDoc(0) should equal(
      """ifndef::nonhtmloutput[]
        |[subs="none"]
        |++++
        |<formalpara role="cypherconsole">
        |<title>Try this query live</title>
        |<para><database><![CDATA[
        |global1
        |global2
        |local1
        |local2
        |]]></database><command><![CDATA[
        |myquery
        |]]></command></para></formalpara>
        |++++
        |endif::nonhtmloutput[]
        |
        |""".stripMargin)
  }
}

class DocumentQueryTest extends Suite
                        with FunSuiteLike
                        with Assertions
                        with Matchers
                        with GraphIcing  {

  class Udf1
  class Udf2

  test("finds all queries and the init-queries they need") {
    val tableV = new TablePlaceHolder(NoAssertions)
    val graphV: GraphVizPlaceHolder = new GraphVizPlaceHolder("")
    val doc = Document("title", "myId", RunnableInitialization(initQueries = Seq("1")),
      Section("h1", None, RunnableInitialization(initQueries = Seq("2")),
      Section("h2", None, RunnableInitialization(initQueries = Seq("3")),
        Query("q", NoAssertions, RunnableInitialization.empty, tableV, Seq.empty)
      ) ~ Query("q2", NoAssertions, RunnableInitialization.empty, graphV, Seq.empty)
    ))

    doc.contentWithQueries should equal(Seq(
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2", "3")), Some(InitializationQuery("q")), tableV),
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2")), Some(InitializationQuery("q2")), graphV))
    )
  }

  test("finds all queries and the user defined functions they need") {
    val tableV = new TablePlaceHolder(NoAssertions)
    val graphV: GraphVizPlaceHolder = new GraphVizPlaceHolder("")
    val doc = Document("title", "myId", RunnableInitialization(initQueries = Seq("1"), userDefinedFunctions = Seq(classOf[Udf1])),
      Section("h1", None, RunnableInitialization(initQueries = Seq("2")),
      Section("h2", None, RunnableInitialization(initQueries = Seq("3"), userDefinedFunctions = Seq(classOf[Udf2])),
        Query("q", NoAssertions, RunnableInitialization.empty, tableV, Seq.empty)
      ) ~ Query("q2", NoAssertions, RunnableInitialization.empty, graphV, Seq.empty)
    ))

    doc.contentWithQueries should equal(Seq(
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2", "3"), userDefinedFunctions = Seq(classOf[Udf1], classOf[Udf2])), Some(InitializationQuery("q")), tableV),
      ContentWithInit(RunnableInitialization(initQueries = Seq("1", "2"), userDefinedFunctions = Seq(classOf[Udf1])), Some(InitializationQuery("q2")), graphV))
    )
  }

  test("Simplest possible document with a query in it") {
    val query = "match (n) return n"
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Query(query, NoAssertions, RunnableInitialization.empty, Paragraph("hello world"), Seq.empty))

    val asciiDocResult = doc.asciiDoc
    asciiDocResult should equal(
      """[[myId]]
        |= title
        |
        |
        |.Query
        |[source, cypher]
        |----
        |MATCH (n)
        |RETURN n
        |----
        |
        |hello world
        |
        |""".stripMargin)
  }


  test("Document with a query and parameters") {
    val query = "MATCH (n) SET n.name = $name RETURN n.name"
    val parameters = Seq[(String, Any)]("name" -> "John")
    val doc = Document("title", "myId", init = RunnableInitialization.empty,
      Query(query, NoAssertions, RunnableInitialization.empty, Paragraph("hello world"), parameters))

    val asciiDocResult = doc.asciiDoc
    asciiDocResult should equal(
      """[[myId]]
        |= title
        |
        |
        |.Parameters
        |[source,javascript]
        |----
        |{
        |  "name" : "John"
        |}
        |----
        |
        |
        |.Query
        |[source, cypher]
        |----
        |MATCH (n)
        |SET n.name = $name
        |RETURN n.name
        |----
        |
        |hello world
        |
        |""".stripMargin)
  }
}
