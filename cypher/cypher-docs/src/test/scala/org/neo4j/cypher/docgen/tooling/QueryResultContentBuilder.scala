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
package org.neo4j.cypher.docgen.tooling

/**
 * This class is responsible for replacing the Content tags asking for query results
 * with the actual results from running the queries, formatted according to the normal
 * textual output of ExecutionResultDumper
 */
class QueryResultContentBuilder(valueFormatter: Any => String)
    extends (DocsExecutionResult => Content) {

  override def apply(result: DocsExecutionResult): Content = {

    var rowCount = 0

    val (updatedResults, columns) = getResultList(result)
    val rows = updatedResults.map { resultRow =>
      rowCount += 1
      val values = columns.map { key =>
        val value = resultRow(key)
        valueFormatter(value)
      }.toSeq
      ResultRow(values)
    }

    val footerRows = s"Rows: $rowCount"
    val footer =
      if (result.queryStatistics().containsUpdates)
        footerRows + "\n" + result.queryStatistics().toString
      else
        footerRows

    QueryResultTable(columns, rows, footer)
  }

  /* Need to do .toList here, to see the results. The iterator has been emptied,
   but it is a DocsExecutionResult we have here that can still provide
   the backing List. Yeah, it's a hack, but it allows us to both assert on the
   results and produce text output */
  protected def getResultList(result: DocsExecutionResult): (List[Map[String, Any]], Array[String]) =
    (result.toList, result.columns)
}

/**
 * This class is responsible for replacing the Content tags asking for query results
 * with the actual results from running the queries limiting the number of output rows and columns,
 * formatted according to the normal textual output of ExecutionResultDumper
 */
class LimitedQueryResultContentBuilder(
  maybeWantedColumns: Option[List[String]],
  numberOfRows: Int,
  valueFormatter: Any => String
) extends QueryResultContentBuilder(valueFormatter) {

  override def getResultList(result: DocsExecutionResult): (List[Map[String, Any]], Array[String]) = {
    val (oldResult, resultColumns) = super.getResultList(result)
    val wantedColumns =
      maybeWantedColumns.getOrElse(resultColumns.toList) // if no columns are given, use those from the result
    val limitedOnRows = oldResult.slice(0, numberOfRows)
    val limitedOnColumns = limitedOnRows.map(m => m.view.filterKeys(k => wantedColumns.contains(k)).toMap)
    val columnsRemoved =
      limitedOnColumns.head.keySet.size < limitedOnRows.head.keySet.size // assumes we have at least one row

    // This will add a new (empty) column if any columns were removed.
    // It also keep the order of the given columns for printing the result.
    val limitedResult =
      if (columnsRemoved) limitedOnColumns.map(m => m ++ Map("..." -> LimitedValueFormatter.NO_VALUE))
      else limitedOnColumns
    val columns = if (columnsRemoved) wantedColumns :+ "..." else wantedColumns

    (limitedResult, columns.toArray)
  }
}

class StatsOnlyQueryResultContentBuilder() extends (DocsExecutionResult => Content) {

  override def apply(result: DocsExecutionResult): Content = {

    assert(
      result.toList.isEmpty,
      "We can only use the 'StatsOnly' results content builder for queries that return no rows"
    )

    val footerRows = "0 rows"
    val stats = result.queryStatistics()
    val footer =
      if (stats.containsUpdates || stats.containsSystemUpdates()) {
        footerRows + "\n" + result.queryStatistics().toString
      } else {
        footerRows
      }
    StatsOnlyQueryResultTable(footer.strip().replaceAll("\n", ", "))
  }
}

class ErrorOnlyQueryResultContentBuilder() extends (Throwable => Content) {
  override def apply(error: Throwable): Content = ErrorOnlyQueryResultTable(error.getMessage)
}
