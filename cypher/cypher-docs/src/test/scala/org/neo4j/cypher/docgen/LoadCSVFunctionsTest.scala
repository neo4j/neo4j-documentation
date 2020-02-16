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
package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class LoadCSVFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("LOAD CSV functions", "query-functions-load-csv")
    synopsis("LOAD CSV functions can be used to get information about the file that is processed by `LOAD CSV`.")
    important {
      p(
        """The functions described on this page are only useful when run on a query that uses `LOAD CSV`. In all other contexts they will always return `null`.""")
    }
    p(
      """Functions:
        |
        |* <<functions-linenumber, linenumber()>>
        |* <<functions-file, file()>>""")
    section("linenumber()", "functions-linenumber") {
      p(
        "`linenumber()` returns the line number that `LOAD CSV` is currently using.")
      function("linenumber()", "An Integer.")
      considerations("`null` will be returned if this function is called without a `LOAD CSV` context.")
      //TODO: Add Query and Result here when CSV support in DocumentingTest is done (needs to happen when porting LoadCSVTest.java)
    }
    section("file()", "functions-file") {
      p(
        """`file()` returns the absolute path of the file that `LOAD CSV` is using.""".stripMargin)
      function("file()", "A String.")
      considerations("`null` will be returned if this function is called without a `LOAD CSV` context.")
      //TODO: Add Query and Result here when CSV support in DocumentingTest is done (needs to happen when porting LoadCSVTest.java)
    }
  }.build()
}
