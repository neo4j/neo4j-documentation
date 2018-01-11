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
package org.neo4j.cypher.docgen

import org.junit.Assert._
import org.junit.Test

class UnwindTest extends DocumentingTestBase {

  def section = "Unwind"

  @Test def simple_unwind() {
    testQuery(
      title = "Unwinding a list",
      text = "We want to transform the literal list into rows named `x` and return them.",
      queryText = """UNWIND [1, 2, 3, null] AS x RETURN x, 'val' AS y""",
      optionalResultExplanation = "Each value of the original list -- including `null` -- is returned as an individual row.",
      assertions = (p) => assertEquals(List(Map("x" -> 1, "y" -> "val"), Map("x" -> 2, "y" -> "val"), Map("x" -> 3, "y" -> "val"), Map("x" -> null, "y" -> "val")), p.toList)
    )
  }
  @Test def distinct_list() {
    testQuery(
      title = "Creating a distinct list",
      text = "We want to transform a list of duplicates into a set using `DISTINCT`.",
      queryText = """WITH [1, 1, 2, 2] AS coll UNWIND coll AS x WITH DISTINCT x RETURN collect(x) AS set""",
      optionalResultExplanation = "Each value of the original list is unwound and passed through `DISTINCT` to create a unique set.",
      assertions = (p) => assertEquals(List(List(1,2)), p.columnAs[Int]("set").toList)
    )
  }

  @Test def concatenated_list() {
    testQuery(
      title = "Using `UNWIND` with any expression returning a list",
      text = "Any expression that returns a list may be used with `UNWIND`.",
      queryText =
        """WITH [1, 2] AS a, [3, 4] AS b
           UNWIND (a + b) AS x
           RETURN x""",
      optionalResultExplanation = "The two lists -- _a_ and _b_ -- are concatenated to form a new list, which is then operated upon by `UNWIND`.",
      assertions = (p) => assertEquals(List(1, 2, 3, 4), p.columnAs[Int]("x").toList)
    )
  }

  @Test def nested_lists() {
    testQuery(
      title = "Using `UNWIND` with a list of lists",
      text = "Multiple `UNWIND` clauses can be chained to unwind nested list elements.",
      queryText =
        """WITH [[1, 2], [3, 4], 5] AS nested
          UNWIND nested AS x
          UNWIND x AS y
          RETURN y""",
      optionalResultExplanation = "The first `UNWIND` results in three rows for `x`, each of which contains an element of the original list (two of which are also lists); namely, `[1, 2]`, `[3, 4]` and `5`. " +
        "The second `UNWIND` then operates on each of these rows in turn, resulting in five rows for `y`.",
      assertions = (p) => assertEquals(List(1, 2, 3, 4, 5), p.columnAs[Int]("y").toList)
    )
  }

  @Test def empty_list() {
    testQuery(
      title = "Using `UNWIND` with an empty list",
      text = "Using an empty list with `UNWIND` will produce no rows, irrespective of whether or not any rows existed beforehand, or whether or not other values are being projected. " +
        "Essentially, `UNWIND []` reduces the number of rows to zero, and thus causes the query to cease its execution, returning no results. " +
        "This has value in cases such as `UNWIND v`, where `v` is a variable from an earlier clause that may or may not be an empty list -- when it is an empty list, this will behave just as a `MATCH` that has no results.",
      queryText =
        """UNWIND [] AS empty
           RETURN empty, 'literal_that_is_not_returned'""".stripMargin,
      assertions = (p) => assertEquals(List(), p.columnAs[Int]("empty").toList)
    )
  }

  @Test def using_non_lists() {
    testQuery(
      title = "Using `UNWIND` with an expression that is not a list",
      text = "Attempting to use `UNWIND` on an expression that does not return a list -- such as `UNWIND 5` -- will cause an error. " +
        "The exception to this is when the expression returns `null` -- this will reduce the number of rows to zero, causing it to cease its execution and return no results.",
      queryText = """UNWIND null AS x RETURN x, 'some_literal'""",
      assertions = (p) => assertEquals(List(), p.toList)
    )
  }

  @Test def create_data_from_list_parameter() {
    testQuery(
      title = "Creating nodes from a list parameter",
      text = "Create a number of nodes and relationships from a parameter-list without using `FOREACH`.",
      parameters = Map("events" -> List(Map("year" -> 2014, "id" -> 1), Map("year" -> 2014, "id" -> 2))),
      queryText =
        """UNWIND $events as event
           MERGE (y:Year {year: event.year})
           MERGE (y)<-[:IN]-(e:Event {id: event.id})
           RETURN e.id AS x ORDER BY x""",
      optionalResultExplanation = "Each value of the original list is unwound and passed through `MERGE` to find or create the nodes and relationships.",
      assertions = (p) => assertEquals(List(1,2), p.columnAs[Int]("x").toList)
    )
  }

}
