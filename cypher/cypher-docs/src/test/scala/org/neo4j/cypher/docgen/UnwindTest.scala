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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class UnwindTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("UNWIND", "query-unwind")
    synopsis("`UNWIND` expands a list into a sequence of rows.")
    p(
      """* <<query-unwind-introduction, Introduction>>
        |* <<unwind-unwinding-a-list, Unwinding a list>>
        |* <<unwind-creating-a-distinct-list, Creating a distinct list>>
        |* <<unwind-using-unwind-with-any-expression-returning-a-list, Using `UNWIND` with any expression returning a list>>
        |* <<unwind-using-unwind-with-a-list-of-lists, Using `UNWIND` with a list of lists>>
        |* <<unwind-using-unwind-with-an-empty-list, Using `UNWIND` with an empty list>>
        |* <<unwind-using-unwind-with-an-expression-that-is-not-a-list, Using `UNWIND` with an expression that is not a list>>
        |* <<unwind-creating-nodes-from-a-list-parameter, Creating nodes from a list parameter>>""".stripMargin)
    section("Introduction", "query-unwind-introduction") {
      p("""With `UNWIND`, you can transform any list back into individual rows.
          |These lists can be parameters that were passed in, previously `collect` -ed result or other list expressions.""".stripMargin)
      p("""One common usage of unwind is to create distinct lists.
          |Another is to create data from parameter lists that are provided to the query.""".stripMargin)
      p("""`UNWIND` requires you to specify a new name for the inner values.""")
    }
    section("Unwinding a list", "unwind-unwinding-a-list") {
      p(
        """We want to transform the literal list into rows named `x` and return them.""".stripMargin)
      query("""UNWIND [1, 2, 3, null] AS x
              #RETURN x, 'val' AS y""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("x" -> 1, "y" -> "val"), Map("x" -> 2, "y" -> "val"), Map("x" -> 3, "y" -> "val"), Map("x" -> null, "y" -> "val")))
        })) {
        p("Each value of the original list -- including `null` -- is returned as an individual row.")
        resultTable()
      }
    }
    section("Creating a distinct list", "unwind-creating-a-distinct-list") {
      p(
        """We want to transform a list of duplicates into a set using `DISTINCT`.""".stripMargin)
      query("""WITH [1, 1, 2, 2] AS coll
              #UNWIND coll AS x
              #WITH DISTINCT x
              #RETURN collect(x) AS setOfVals""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("setOfVals" -> List(1, 2))))
        })) {
        p("Each value of the original list is unwound and passed through `DISTINCT` to create a unique set.")
        resultTable()
      }
    }
    section("Using `UNWIND` with any expression returning a list", "unwind-using-unwind-with-any-expression-returning-a-list") {
      p(
        """Any expression that returns a list may be used with `UNWIND`.""".stripMargin)
      query("""WITH
              #  [1, 2] AS a,
              #  [3, 4] AS b
              #UNWIND (a + b) AS x
              #RETURN x""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("x" -> 1), Map("x" -> 2), Map("x" -> 3), Map("x" -> 4)))
        })) {
        p("""The two lists -- _a_ and _b_ -- are concatenated to form a new list, which is then operated upon by `UNWIND`.""")
        resultTable()
      }
    }
    section("Using `UNWIND` with a list of lists", "unwind-using-unwind-with-a-list-of-lists") {
      p("""Multiple `UNWIND` clauses can be chained to unwind nested list elements.""")
      query("""WITH [[1, 2], [3, 4], 5] AS nested
              #UNWIND nested AS x
              #UNWIND x AS y
              #RETURN y""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("y" -> 1), Map("y" -> 2), Map("y" -> 3), Map("y" -> 4), Map("y" -> 5)))
        })) {
        p(
          """The first `UNWIND` results in three rows for `x`, each of which contains an element of the original list (two of which are also lists); namely, `[1, 2]`, `[3, 4]` and `5`.
            |The second `UNWIND` then operates on each of these rows in turn, resulting in five rows for `y`.""".stripMargin)
        resultTable()
      }
    }
    section("Using `UNWIND` with an empty list", "unwind-using-unwind-with-an-empty-list") {
      p("""Using an empty list with `UNWIND` will produce no rows, irrespective of whether or not any rows existed beforehand, or whether or not other values are being projected.""")
      p(
        """Essentially, `UNWIND []` reduces the number of rows to zero, and thus causes the query to cease its execution, returning no results.
          |This has value in cases such as `UNWIND v`, where `v` is a variable from an earlier clause that may or may not be an empty list -- when it is an empty list, this will behave just as a `MATCH` that has no results.""".stripMargin)
      query("""UNWIND [] AS empty
              #RETURN empty, 'literal_that_is_not_returned'""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[Int]("empty").toList should equal(List())
        })) {
        resultTable()
      }
      p("""To avoid inadvertently using `UNWIND` on an empty list, `CASE` may be used to replace an empty list with a `null`:""")
      p("""[source, cypher]
          #----
          #WITH [] AS list
          #UNWIND
          #  CASE
          #    WHEN list = [] THEN [null]
          #    ELSE list
          #  END AS emptylist
          #RETURN emptylist
          #----""".stripMargin('#'))
    }
    section("Using `UNWIND` with an expression that is not a list", "unwind-using-unwind-with-an-expression-that-is-not-a-list") {
      p(
        """Using `UNWIND` on an expression that does not return a list, will return the same result as using `UNWIND` on a list that just contains that expression.
          |As an example, `UNWIND 5` is effectively equivalent to  `UNWIND[5]`.
          |The exception to this is when the expression returns `null` -- this will reduce the number of rows to zero, causing it to cease its execution and return no results.""".stripMargin)
      query("""UNWIND null AS x
              #RETURN x, 'some_literal'""".stripMargin('#'),
      ResultAssertions((r) => {
          r.columnAs[Any]("x").toList should equal(List())
        })) {
        resultTable()
      }
    }
    section("Creating nodes from a list parameter", "unwind-creating-nodes-from-a-list-parameter") {
      p("""Create a number of nodes and relationships from a parameter-list without using `FOREACH`.""")
      query("""UNWIND $events as event
              #MERGE (y:Year {year: event.year})
              #MERGE (y)<-[:IN]-(e:Event {id: event.id})
              #RETURN e.id AS x ORDER BY x""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("x" -> 1), Map("x" -> 2)))
        }), ("events", List(Map("year" -> 2014, "id" -> 1), Map("year" -> 2014, "id" -> 2)) )
      ) {
        p("""Each value of the original list is unwound and passed through `MERGE` to find or create the nodes and relationships.""")
        resultTable()
      }
    }
  }.build()
}
