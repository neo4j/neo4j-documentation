/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, ResultAssertions}

class CallTest extends DocumentingTest {

  val title = "CALL"
  override def outputPath = "clauses/call"

  override def doc: Document = new DocBuilder {
    doc("CALL", "calling-stored-procedures")

    registerProcedures(classOf[org.neo4j.procedure.example.EchoProcedure])

    initQueries(
      """CREATE (ROOT)
        |CREATE (A:Person)
        |CREATE (B:Person)
        |CREATE (C:Person)
        |CREATE (ROOT)-[:KNOWS]->(A)
        |CREATE (A)-[:KNOWS]->(B)
        |CREATE (B)-[:KNOWS]->(C)
        |CREATE (C)-[:KNOWS]->(ROOT)""".stripMargin)

    query(
      "CALL db.labels() YIELD label",
      ResultAssertions( r => {
        assert(r.toList.size === 1)
        assert(r.toList == List(Map("label" ->"Person")))
      })
    ) { resultTable() }

    p("""
        |This shows a standalone call to the built-in procedure `db.labels` to list all labels used in the database.
        |Note that required procedure arguments are given explicitly in brackets after the procedure name.""")

    query(
      "CALL org.neo4j.procedure.example.echo('hi-o') YIELD echo",
      ResultAssertions( r => assert(r.toList.size === 1))
    ) { resultTable() }

    p("""
        |Standalone calls may omit `YIELD` and also provide arguments implicitly via statement parameters, e.g. a standalone call requiring one argument `input` may be run by passing the parameter map `{input: 'foo'}`.""")

    query(
      """CALL db.labels() YIELD label
        |RETURN count(label) AS count""".stripMargin,
      ResultAssertions( r => assert(r.toList.size === 1))
    ) { resultTable() }

    p("""
         |Calls the built-in procedure `db.labels` inside a larger query to count all labels used in the database.
         |Calls inside a larger query always requires passing arguments and naming results explicitly with `YIELD`.""")
  }.build()
}

