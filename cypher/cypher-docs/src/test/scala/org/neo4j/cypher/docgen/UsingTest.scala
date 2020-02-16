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

class UsingTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"
  override def doc = new DocBuilder {
    doc("Planner hints and the USING keyword", "query-using")
    initQueries(
      "CREATE INDEX ON :Scientist(name)",
      "CREATE INDEX ON :Science(name)",

      """CREATE
        |(liskov:Scientist {name: 'Liskov', born: 1939})-[:KNOWS]->(wing:Scientist {name: 'Wing', born: 1956})-[:RESEARCHED]->(cs:Science {name: 'Computer Science'})<-[:RESEARCHED]-(conway:Scientist {name: 'Conway', born: 1938}),
        |(liskov)-[:RESEARCHED]->(cs),
        |(wing)-[:RESEARCHED]->(:Science {name: 'Engineering'}),
        |(chemistry:Science {name: 'Chemistry'})<-[:RESEARCHED]-(:Scientist {name: 'Curie', born: 1867}),
        |(chemistry)<-[:RESEARCHED]-(:Scientist {name: 'Arden'}),
        |(chemistry)<-[:RESEARCHED]-(:Scientist {name: 'Franklin'}),
        |(chemistry)<-[:RESEARCHED]-(:Scientist {name: 'Harrison'})
      """
    )
    synopsis("A planner hint is used to influence the decisions of the planner when building an execution plan for a query. Planner hints are specified in a query with the `USING` keyword.")
    caution {
      p("Forcing planner behavior is an advanced feature, and should be used with caution by experienced developers and/or database administrators only, as it may cause queries to perform poorly.")
    }
    p("""* <<query-using-introduction,Introduction>>
        |* <<query-using-index-hint,Index hints>>
        |* <<query-using-scan-hint,Scan hints>>
        |* <<query-using-join-hint,Join hints>>
        |* <<query-using-periodic-commit-hint,`PERIODIC COMMIT` query hint>>""")
    section("Introduction", "query-using-introduction") {
      p("""When executing a query, Neo4j needs to decide where in the query graph to start matching.
          |This is done by looking at the `MATCH` clause and the `WHERE` conditions and using that information to find useful indexes, or other starting points.""")
      p("""However, the selected index might not always be the best choice.
          |Sometimes multiple indexes are possible candidates, and the query planner picks the suboptimal one from a performance point of view.
          |Moreover, in some circumstances (albeit rarely) it is better not to use an index at all.""")
      p("""Neo4j can be forced to use a specific starting point through the `USING` keyword. This is called giving a planner hint.
          |There are four types of planner hints: index hints, scan hints, join hints, and the `PERIODIC COMMIT` query hint.""")
      note {
        p("You cannot use planner hints if your query has a `START` clause.")
      }
      p("The following graph is used for the examples below:")
      graphViz()
      query(s"$matchString RETURN 1 AS $columnName", assertIntegersReturned(1)) {
        p("""The following query will be used in some of the examples on this page. It has intentionally been constructed in
            |such a way that the statistical information will be inaccurate for the particular subgraph that this query
            |matches. For this reason, it can be improved by supplying planner hints.""")
        profileExecutionPlan()
      }
    }
    section("Index hints", "query-using-index-hint") {
      p("""Index hints are used to specify which index, if any, the planner should use as a starting point.
          |This can be beneficial in cases where the index statistics are not accurate for the specific values that
          |the query at hand is known to use, which would result in the planner picking a non-optimal index.
          |To supply an index hint, use `USING INDEX variable:Label(property)` or `USING INDEX SEEK variable:Label(property)` after the applicable `MATCH` clause.""")
      p("""It is possible to supply several index hints, but keep in mind that several starting points
          |will require the use of a potentially expensive join later in the query plan.""")
      section("Query using an index hint") {
        p("""The query above will not naturally pick an index to solve the plan.
            |This is because the graph is very small, and label scans are faster for small databases.
            |In general, however, query performance is ranked by the dbhit metric, and we see that using an index is
            |slightly better for this query.""")
        query(s"$matchString USING INDEX liskov:Scientist(name) RETURN liskov.born AS $columnName",
              assertIntegersReturned(1939)) {
          p("Returns the year *'Barbara Liskov'* was born.")
          profileExecutionPlan()
        }
      }
      section("Query using an index seek hint") {
        p(
          """Similar to the index (scan) hint, but an index seek will be used rather than an index scan.
            |Index seeks require no post filtering, they are most efficient when a relatively small number of nodes have the specified value on the queried property.""")
        query(s"$matchString USING INDEX SEEK liskov:Scientist(name) RETURN liskov.born AS $columnName",
          assertIntegersReturned(1939)) {
          p("Returns the year *'Barbara Liskov'* was born.")
          profileExecutionPlan()
        }
      }
      section("Query using multiple index hints") {
        p("""Supplying one index hint changed the starting point of the query, but the plan is still linear, meaning it
            |only has one starting point. If we give the planner yet another index hint, we force it to use two starting points,
            |one at each end of the match. It will then join these two branches using a join operator. """)
        query(s"$matchString USING INDEX liskov:Scientist(name) USING INDEX conway:Scientist(name) RETURN liskov.born AS $columnName",
              assertIntegersReturned(1939)) {
          p("Returns the year *'Barbara Liskov'* was born, using a slightly better plan.")
          profileExecutionPlan()
        }
      }
    }
    section("Scan hints", "query-using-scan-hint") {
      p("""If your query matches large parts of an index, it might be faster to scan the label and filter out nodes that do not match.
          |To do this, you can use `USING SCAN variable:Label` after the applicable `MATCH` clause.
          |This will force Cypher to not use an index that could have been used, and instead do a label scan.""")
      section("Hinting a label scan") {
        p("""If the best performance is to be had by scanning all nodes in a label and then filtering on that set, use `USING SCAN`.""")
        query(s"""MATCH (s:Scientist)
                 |USING SCAN s:Scientist
                 |WHERE s.born < 1939
                 |RETURN s.born AS $columnName""",
              assertIntegersReturned(1938, 1867)) {
          p("Returns all scientists born before *'1939'*.")
          profileExecutionPlan()
        }
      }
    }
    section("Join hints", "query-using-join-hint") {
      p("""Join hints are the most advanced type of hints, and are not used to find starting points for the
          |query execution plan, but to enforce that joins are made at specified points. This implies that there
          |has to be more than one starting point (leaf) in the plan, in order for the query to be able to join the two branches ascending
          |from these leaves. Due to this nature, joins, and subsequently join hints, will force
          |the planner to look for additional starting points, and in the case where there are no more good ones,
          |potentially pick a very bad starting point. This will negatively affect query performance. In other cases,
          |the hint might force the planner to pick a _seemingly_ bad starting point, which in reality proves to be a very good one.""")
      section("Hinting a join on a single node") {
        p("""In the example above using multiple index hints, we saw that the planner chose to do a join on the `cs` node.
            |This means that the relationship between `wing` and `cs` was traversed in the outgoing direction, which is better
            |statistically because the pattern `()-[:RESEARCHED]->(:Science)` is more common than the pattern `(:Scientist)-[:RESEARCHED]->()`.
            |However, in the actual graph, the `cs` node only has two such relationships, so expanding from it will be beneficial
            |to expanding from the `wing` node. We can force the join to happen on `wing` instead with a join hint.""")
        query(s"""$matchString
              |USING INDEX liskov:Scientist(name)
              |USING INDEX conway:Scientist(name)
              |USING JOIN ON wing
              |RETURN wing.born AS $columnName""",
              assertIntegersReturnedAndUsingHashJoin(1956)) {
          p("Returns the birth date of *'Jeanette Wing'*, using a slightly better plan.")
          profileExecutionPlan()
        }
      }
      section("Hinting a join on multiple nodes") {
        p("The query planner can be made to produce a join between several specific points. This requires the query to expand from the same node from several directions.")
        query(s"""MATCH (liskov:Scientist {name:'Liskov'})-[:KNOWS]->(wing:Scientist {name:'Wing'})-[:RESEARCHED]->(cs:Science {name:'Computer Science'})<-[:RESEARCHED]-(liskov)
              |USING INDEX liskov:Scientist(name)
              |USING JOIN ON liskov, cs
              |RETURN wing.born AS $columnName""", assertIntegersReturnedAndUsingHashJoin(1956)) {
          p("Returns the birth date of *'Jeanette Wing'*.")
          profileExecutionPlan()
        }
      }
    }
    section("`PERIODIC COMMIT` query hint", "query-using-periodic-commit-hint") {
      note {
        p("""See <<getting-started#cypher-intro-load-csv>> on how to import data from CSV files.""")
      }
      p("""Importing large amounts of data using `LOAD CSV` with a single Cypher query may fail due to memory constraints.
          |This will manifest itself as an `OutOfMemoryError`.""")
      p("""For this situation _only,_ Cypher provides the global `USING PERIODIC COMMIT` query hint for updating queries using `LOAD CSV`.
          |If required, the limit for the number of rows per commit may be set as follows: `USING PERIODIC COMMIT 500`.""")
      p("""`PERIODIC COMMIT` will process the rows until the number of rows reaches a limit.
          |Then the current transaction will be committed and replaced with a newly opened transaction.
          |If no limit is set, a default value will be used.""")
      p("""See <<load-csv-importing-large-amounts-of-data>> in <<query-load-csv>> for examples of `USING PERIODIC COMMIT` with and without setting the number of rows per commit.""")
      important {
        p("""Using `PERIODIC COMMIT` will prevent running out of memory when importing large amounts of data.
                |However, it will also break transactional isolation and thus it should only be used where needed.""")
        }
    }
  }.build()

  private def columnName = "column"
  private def matchString =
    "MATCH (liskov:Scientist {name:'Liskov'})-[:KNOWS]->(wing:Scientist)-[:RESEARCHED]->(cs:Science {name:'Computer Science'})<-[:RESEARCHED]-(conway:Scientist {name: 'Conway'})"

  private def assertIntegersReturned(values: Long*) = ResultAssertions(result => {
    result.columnAs[Long](columnName).toSet should equal(values.toSet)
  })

  private def assertIntegersReturnedAndUsingHashJoin(values: Long*) = ResultAssertions(result => {
    result.columnAs[String](columnName).toSet should equal(values.toSet)
    result.executionPlanDescription().toString should include("NodeHashJoin")
  })
}
