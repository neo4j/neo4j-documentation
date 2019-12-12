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
package org.neo4j.cypher.docgen.refcard

import java.io.File

import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, QueryStatisticsTestSupport}
import org.neo4j.cypher.docgen.{CsvFile, RefcardTest}
import org.neo4j.graphdb.Transaction

class ImportTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List()
  val title = "Import"
  override val linkId = "clauses/load-csv"
  override val asciidocSubstitutions = "attributes+"

  implicit var csvFilesDir: File = createDir(dir, "csv")

  private val artist = new CsvFile("artists.csv").withContentsF(
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992"))

  private val artistWithHeaders = new CsvFile("artists-with-headers.csv").withContentsF(
    Seq("Id", "Name", "Year"),
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992"))

  private val artistFieldTerminator = new CsvFile("artists-fieldterminator.csv", ';').withContentsF(
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992"))

  filePaths = Map(
    "%ARTIST%" -> CsvFile.urify(artist),
    "%ARTIS_WITH_HEADER%" -> CsvFile.urify(artistWithHeaders),
    "%ARTIST_WITH_FIELD_DELIMITER%" -> CsvFile.urify(artistFieldTerminator))

  urls = Map(
    "%ARTIST%" -> (baseUrl + artist.getName),
    "%ARTIS_WITH_HEADER%" -> (baseUrl + artistWithHeaders.getName),
    "%ARTIST_WITH_FIELD_DELIMITER%" -> (baseUrl + artistFieldTerminator.getName))

  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "created" =>
        assertStats(result, nodesCreated = 4, labelsAdded = 4, propertiesWritten = 8)
      case "file" =>
        assertStats(result, nodesCreated = 0, labelsAdded = 0, propertiesWritten = 0)
        assert(result.toList.size === 1)
      case "linenumber" =>
        assertStats(result, nodesCreated = 0, labelsAdded = 0, propertiesWritten = 0)
        assert(result.toList.size === 4)
    }
  }

  def text = """
###assertion=created
//

LOAD CSV FROM
'%ARTIST%' AS line
CREATE (:Artist {name: line[1], year: toInteger(line[2])})
###

Load data from a CSV file and create nodes.

###assertion=created
//

LOAD CSV WITH HEADERS FROM
'%ARTIS_WITH_HEADER%' AS line
CREATE (:Artist {name: line.Name, year: toInteger(line.Year)})
###

Load CSV data which has headers.

###assertion=created
//

USING PERIODIC COMMIT 500
LOAD CSV WITH HEADERS FROM
'%ARTIS_WITH_HEADER%' AS line
CREATE (:Artist {name: line.Name, year: toInteger(line.Year)})
###

Commit the current transaction after every 500 rows when importing large amounts of data.

###assertion=created
//

LOAD CSV FROM
'%ARTIST_WITH_FIELD_DELIMITER%'
AS line FIELDTERMINATOR ';'
CREATE (:Artist {name: line[1], year: toInteger(line[2])})
###

Use a different field terminator, not the default which is a comma (with no whitespace around it).

###assertion=file
//

LOAD CSV FROM
'%ARTIST%' AS line
RETURN DISTINCT file()
###

Returns the absolute path of the file that `LOAD CSV` is processing, returns `null` if called outside of `LOAD CSV` context.

###assertion=linenumber
//

LOAD CSV FROM
'%ARTIST%' AS line
RETURN linenumber()
###

Returns the line number that `LOAD CSV` is currently processing, returns `null` if called outside of `LOAD CSV` context.
"""
}
