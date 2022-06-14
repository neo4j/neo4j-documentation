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

import org.neo4j.cypher.docgen.tooling.Admonitions.Note
import org.neo4j.cypher.docgen.tooling.{Paragraph, QueryStatisticsTestSupport}
import org.neo4j.visualization.graphviz.{AsciiDocSimpleStyle, GraphStyle}
import org.junit.Test
import java.io.File
import org.junit.Assert._

class LoadCSVTest extends DocumentingTestBase with QueryStatisticsTestSupport with SoftReset {

  override protected def getGraphvizStyle: GraphStyle =
    AsciiDocSimpleStyle.withAutomaticRelationshipTypeColors()
  override val asciidocSubstitutions = "attributes+"

  implicit var csvFilesDir: File = createDir(dir, "csv-files")

  def section = "Load CSV"

  private val artist = new CsvFile("artists.csv").withContentsF(
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992")
  )

  private val artistWithHeaders = new CsvFile("artists-with-headers.csv").withContentsF(
    Seq("Id", "Name", "Year"),
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992")
  )

  private val artistFieldTerminator = new CsvFile("artists-fieldterminator.csv", ';').withContentsF(
    Seq("1", "ABBA", "1992"),
    Seq("2", "Roxette", "1986"),
    Seq("3", "Europe", "1979"),
    Seq("4", "The Cardigans", "1992")
  )

  private val artistsWithEscapeChar = new CsvFile("artists-with-escaped-char.csv").withContentsF(quoted = true,
    Seq("1", "The \"\"Symbol\"\"", "1992")
  )

  filePaths = Map(
    "%ARTIST%" -> CsvFile.urify(artist),
    "%ARTIS_WITH_HEADER%" -> CsvFile.urify(artistWithHeaders),
    "%ARTIST_WITH_FIELD_DELIMITER%" -> CsvFile.urify(artistFieldTerminator),
    "%ARTIST_WITH_ESCAPE_CHAR%" -> CsvFile.urify(artistsWithEscapeChar)
  )

  urls = Map(
    "%ARTIST%" -> ("""file:///""" + artist.getName),
    "%ARTIS_WITH_HEADER%" -> ("""file:///""" + artistWithHeaders.getName),
    "%ARTIST_WITH_FIELD_DELIMITER%" -> ("""file:///""" + artistFieldTerminator.getName),
    "%ARTIST_WITH_ESCAPE_CHAR%" -> ("""file:///""" + artistsWithEscapeChar.getName)
  )

  @Test def should_import_data_from_a_csv_file() {
    testQuery(
      title = "Import data from a CSV file",
      text = """To import data from a CSV file into Neo4j, you can use `LOAD CSV` to get the data into your query.
               #Then you write it to your database using the normal updating clauses of Cypher.
               #
               #.artists.csv
               #[source]
               #----
               #include::csv-files/artists.csv[]
               #----""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST%' AS line
                    #CREATE (:Artist {name: line[1], year: toInteger(line[2])})""".stripMargin('#'),
      optionalResultExplanation = """A new node with the `Artist` label is created for each row in the CSV file.
                                    #In addition, two columns from the CSV file are set as properties on the nodes.""".stripMargin('#'),
      assertions = (p) => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4))
  }

  @Test def should_import_data_from_remote_csv() {
    testQuery(
      title = "Import data from a remote CSV file",
      text = """
Accordingly, you can import data from a CSV file in a remote location into Neo4j.
Note that this applies to all variations of CSV files (see examples below for other variations).

.data.neo4j.com/bands/artists.csv
[source]
----
1,ABBA,1992
2,Roxette,1986
3,Europe,1979
4,The Cardigans,1992
----
""",
      queryText = s"LOAD CSV FROM 'https://data.neo4j.com/bands/artists.csv' AS line CREATE (:Artist {name: line[1], year: toInteger(line[2])})",
      assertions = p => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4))
  }

  @Test def should_import_data_from_a_csv_file_with_headers() {
    testQuery(
      title = "Import data from a CSV file containing headers",
      text = """When your CSV file has headers, you can view each row in the file as a map instead of as an array of strings.
               #
               #.artists-with-headers.csv
               #[source]
               #----
               #include::csv-files/artists-with-headers.csv[]
               #----""".stripMargin('#'),
      queryText = """LOAD CSV WITH HEADERS FROM '%ARTIS_WITH_HEADER%' AS line
                    #CREATE (:Artist {name: line.Name, year: toInteger(line.Year)})""".stripMargin('#'),
      optionalResultExplanation = """This time, the file starts with a single row containing column names.
                                    #Indicate this using `WITH HEADERS` and you can access specific fields by their corresponding column name.""".stripMargin('#'),
      assertions = (p) => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4))
  }

  @Test def should_import_data_from_a_csv_file_with_custom_field_terminator() {
    testQuery(
      title = "Import data from a CSV file with a custom field delimiter",
      text = """Sometimes, your CSV file has other field delimiters than commas.
               #You can specify which delimiter your file uses, using `FIELDTERMINATOR`.
               #Hexadecimal representation of the unicode character encoding can be used if prepended by `{backslash}u`.
               #The encoding must be written with four digits.
               #For example, `{backslash}u003B` is equivalent to `;` (SEMICOLON).
               #
               #.artists-fieldterminator.csv
               #[source]
               #----
               #include::csv-files/artists-fieldterminator.csv[]
               #----
               #""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST_WITH_FIELD_DELIMITER%' AS line FIELDTERMINATOR ';'
                    #CREATE (:Artist {name: line[1], year: toInteger(line[2])})""".stripMargin('#'),
      optionalResultExplanation = "As values in this file are separated by a semicolon, a custom `FIELDTERMINATOR` is specified in the `LOAD CSV` clause.",
      assertions = (p) => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4))
  }

  @Test def should_import_data_from_a_csv_file_with_call_in_transactions() {
    testQuery(
      title = "Importing large amounts of data",
      text = """If the CSV file contains a significant number of rows (approaching hundreds of thousands or millions), `CALL {} IN TRANSACTIONS` can be used to instruct Neo4j to commit a transaction after a number of rows.
               #This reduces the memory overhead of the transaction state.
               #Note that `CALL {} IN TRANSACTIONS` is only allowed in <<query-transactions, implicit (auto-commit or `:auto`) transactions>>.
               #For more information, see <<subquery-call-in-transactions>>.""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST%' AS line
                    #CALL {
                    #  WITH line
                    #  CREATE (:Artist {name: line[1], year: toInteger(line[2])})
                    #} IN TRANSACTIONS""".stripMargin('#'),
      optionalResultExplanation = "",
      assertions = (p) => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4, transactionsCommitted = 1))
  }

  @Test def should_import_data_from_a_csv_file_with_call_in_transactions_after_500_rows() {
    testQuery(
      title = "Setting the rate of CALL IN TRANSACTIONS",
      text = "You can set the number of rows as in the example, where it is set to 500 rows.",
      queryText = """LOAD CSV FROM '%ARTIST%' AS line
                    #CALL {
                    #  WITH line
                    #  CREATE (:Artist {name: line[1], year: toInteger(line[2])})
                    #} IN TRANSACTIONS OF 500 ROWS""".stripMargin('#'),
      optionalResultExplanation = "",
      assertions = (p) => assertStats(p, nodesCreated = 4, propertiesWritten = 8, labelsAdded = 4, transactionsCommitted = 1))
  }

  @Test def should_import_data_from_a_csv_file_which_uses_the_escape_char() {
    testQuery(
      title = "Import data containing escaped characters",
      text = """In this example, we both have additional quotes around the values, as well as escaped quotes inside one value.
               #
               #.artists-with-escaped-char.csv
               #[source]
               #----
               #include::csv-files/artists-with-escaped-char.csv[]
               #----""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST_WITH_ESCAPE_CHAR%' AS line
                    #CREATE (a:Artist {name: line[1], year: toInteger(line[2])})
                    #RETURN
                    #  a.name AS name,
                    #  a.year AS year,
                    #  size(a.name) AS size""".stripMargin('#'),
      optionalResultExplanation = """Note that strings are wrapped in quotes in the output here.
                                    #You can see that when comparing to the length of the string in this case!""".stripMargin('#'),
      assertions = (p) => assertEquals(List(Map("name" -> """The "Symbol"""", "year" -> 1992, "size" -> 12)), p.toList)
    )
  }

  @Test def should_use_csv_function_linenumber() {
    testQuery(
      title = "Using linenumber() with LOAD CSV",
      text = """For certain scenarios, like debugging a problem with a csv file, it may be useful to get the current line number that `LOAD CSV` is operating on.
               #The `linenumber()` function provides exactly that or `null` if called without a `LOAD CSV` context.
               #
               #.artists.csv
               #[source]
               #----
               #include::csv-files/artists.csv[]
               #----
               #""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST%' AS line
                    #RETURN linenumber() AS number, line""".stripMargin('#'),
      optionalResultExplanation = "",
      assertions = p =>
        assertEquals(List(
          Map("number" -> 1, "line" -> List("1", "ABBA", "1992")),
          Map("number" -> 2, "line" -> List("2", "Roxette", "1986")),
          Map("number" -> 3, "line" -> List("3", "Europe", "1979")),
          Map("number" -> 4, "line" -> List("4", "The Cardigans", "1992"))
        ), p.toList)
    )
  }

  /* This outputs the file location of team city!!!
  @Test def should_use_csv_function_file() {
    testQuery(
      title = "Using file() with LOAD CSV",
      text = """For certain scenarios, like debugging a problem with a csv file, it may be useful to get the absolute path of the file that `LOAD CSV` is operating on.
               #The `file()` function provides exactly that or `null` if called without a `LOAD CSV` context.
               #
               #.artists.csv
               #[source]
               #----
               #include::csv-files/artists.csv[]
               #----""".stripMargin('#'),
      queryText = """LOAD CSV FROM '%ARTIST%' AS line
                    #RETURN DISTINCT file() AS path""".stripMargin('#'),
      optionalResultExplanation = """Since `LOAD CSV` can temporary download a file to process it, it is important to note that `file()` will always return the path on disk.
                                    #This is why you see different URIs in this example.
                                    #If `LOAD CSV` is invoked with a `file:///` URL that points to your disk `file()` will return that same path.""".stripMargin('#'),
      assertions = p => {
        val list = p.toList
        assertEquals(1, list.size)
        val path = list.head("path").asInstanceOf[String]
        assertTrue(path.endsWith("artists.csv"))
      }
    )
  }*/
}
