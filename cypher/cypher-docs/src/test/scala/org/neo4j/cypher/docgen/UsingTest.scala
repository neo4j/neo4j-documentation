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

import org.neo4j.cypher.docgen.tooling._
import org.scalatest.matchers.Matcher

class UsingTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"
  override def doc = new DocBuilder {
    doc("Planner hints and the USING keyword", "query-using")
    initQueries(
      """FOREACH(i IN range(1, 100) |
        |  CREATE (:Scientist {born: 1800 + i})-[:RESEARCHED]->
        |         (:Science)<-[:INVENTED_BY {year: 530 + (i % 50)}]-
        |         (:Pioneer {born: 500 + (i % 50)})-[:LIVES_IN]->
        |         (:City)-[:PART_OF]->
        |         (:Country {formed: 400 + i, name:'ACountry' + i})
        |)
        |""".stripMargin,
      "CREATE INDEX FOR (s:Scientist) ON (s.born)",
      "CREATE INDEX FOR (p:Pioneer) ON (p.born)",
      "CREATE INDEX FOR (c:Country) ON (c.formed)",
      "CREATE INDEX FOR (c:Country) ON (c.name)",
      "CREATE INDEX FOR ()-[i:INVENTED_BY]-() ON (i.year)",
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
      query(
        s"""$matchString
           |RETURN *""", assertPlan(AND(OR(ShouldUseNodeIndexSeekOn("s"), ShouldUseNodeIndexSeekOn("cc")), ShouldNotUseJoins))) {
        p(
          """The query above will be used in some of the examples on this page.
            |Without any hints, one index and no join is used.""")
        profileExecutionPlan()
      }
    }
    section("Index hints", "query-using-index-hint") {
      p("""Index hints are used to specify which index, the planner should use as a starting point.
          |This can be beneficial in cases where the index statistics are not accurate for the specific values that
          |the query at hand is known to use, which would result in the planner picking a non-optimal index.
          |To supply an index hint, use `USING INDEX variable:Label(property)` or `USING INDEX SEEK variable:Label(property)` after the applicable `MATCH` clause for node indexes,
          |and `USING INDEX variable:RELATIONSHIP_TYPE(property)` or `USING INDEX SEEK variable:RELATIONSHIP_TYPE(property)` for relationship indexes.""")
      p(
        """`USING INDEX` can be fulfilled by any of the following plans:
          |`NodeIndexScan`, `DirectedRelationshipIndexScan`, `UndirectedRelationshipIndexScan`, `NodeIndexSeek`, `DirectedRelationshipIndexSeek`, `UndirectedRelationshipIndexSeek`.
          |`USING INDEX SEEK` can only be fulfilled by `NodeIndexSeek`, `DirectedRelationshipIndexSeek` or `UndirectedRelationshipIndexSeek`.""")
      p("""It is possible to supply several index hints, but keep in mind that several starting points
          |will require the use of a potentially expensive join later in the query plan.""")
      section("Query using a node index hint") {
        p("""The query above can be tuned to pick a different index as the starting point.""")
        query(
          s"""$matchString
             |USING INDEX p:Pioneer(born)
             |RETURN *""",
          assertPlan(AND(ShouldUseNodeIndexSeekOn("p"), ShouldNotUseJoins))) {
          profileExecutionPlan()
        }
      }
      section("Query using a relationship index hint") {
        p("""The query above can be tuned to pick a relationship index as the starting point.""")
        query(
          s"""$matchString
             |USING INDEX i:INVENTED_BY(year)
             |RETURN *""",
          assertPlan(AND(ShouldUseRelationshipIndexSeekOn("i"), ShouldNotUseJoins))) {
          profileExecutionPlan()
        }
      }
      section("Query using multiple index hints") {
        p("""Supplying one index hint changed the starting point of the query, but the plan is still linear, meaning it
            |only has one starting point. If we give the planner yet another index hint, we force it to use two starting points,
            |one at each end of the match. It will then join these two branches using a join operator.""")
        query(
          s"""$matchString
             |USING INDEX s:Scientist(born)
             |USING INDEX cc:Country(formed)
             |RETURN *""",
          assertPlan(AND(AND(AND(ShouldUseNodeIndexSeekOn("s"), ShouldUseNodeIndexSeekOn("cc")), NOT(ShouldUseNodeIndexSeekOn("p"))), NOT(ShouldUseJoinOn("p"))))) {
          profileExecutionPlan()
        }
      }
      section("Query using multiple index hints with a disjunction") {
        p(
          """Supplying multiple index hints can also be useful if the query contains a disjunction (`OR`) in the `WHERE` clause.
            |This makes sure that all hinted indexes are used and the results are joined together with a `Union` and a `Distinct` afterwards.""".stripMargin)
        query(
          s"""
             |MATCH (country:Country)
             |USING INDEX country:Country(name)
             |USING INDEX country:Country(formed)
             |WHERE country.formed = 500 OR country.name STARTS WITH "A"
             |RETURN *""",
          assertPlan(AND(ShouldUseNodeIndexSeekOn("country"), ShouldUseUnionDistinct("country")))) {
          profileExecutionPlan()
        }
        p(
          """Cypher will usually provide a plan that uses all indexes for a disjunction without hints.
            |It may, however, decide to plan a `NodeByLabelScan` instead, if the predicates appear to be not very selective.
            |In this case, the index hints can be useful.
            |""".stripMargin)
      }
    }
    section("Scan hints", "query-using-scan-hint") {
      p("""If your query matches large parts of an index, it might be faster to scan the label or relationship type and filter out rows that do not match.
          |To do this, you can use `USING SCAN variable:Label` after the applicable `MATCH` clause for node indexes,
          |and `USING SCAN variable:RELATIONSHIP_TYPE` for relationship indexes.
          |This will force Cypher to not use an index that could have been used, and instead do a label scan/relationship type scan.
          |You can use the same hint to enforce a starting point where no index is applicable.""")
      section("Hinting a label scan") {
        query(
          s"""$matchString
             |USING SCAN s:Scientist
             |RETURN *""",
          assertPlan(ShouldUseLabelScanOn("s"))) {
          profileExecutionPlan()
        }
      }
      section("Hinting a relationship type scan") {
        query(
          s"""$matchString
             |USING SCAN i:INVENTED_BY
             |RETURN *""",
          assertPlan(ShouldUseRelationshipTypeScanOn("i"))) {
          profileExecutionPlan()
        }
      }
      section("Query using multiple scan hints with a disjunction") {
        p(
          """Supplying multiple scan hints can also be useful if the query contains a disjunction (`OR`) in the `WHERE` clause.
            |This makes sure that all involved label predicates are solved by a `NodeByLabelScan` and the results are joined together with a `Union` and a `Distinct` afterwards.""".stripMargin)
        query(
          s"""
             |MATCH (person)
             |USING SCAN person:Pioneer
             |USING SCAN person:Scientist
             |WHERE person:Pioneer OR person:Scientist
             |RETURN *""",
          assertPlan(AND(ShouldUseLabelScanOn("person"), ShouldUseUnionDistinct("person")))) {
          profileExecutionPlan()
        }
        p(
          """Cypher will usually provide a plan that uses scans for a disjunction without hints.
            |It may, however, decide to plan an `AllNodeScan` followed by a `Filter` instead, if the label predicates appear to be not very selective.
            |In this case, the scan hints can be useful.
            |""".stripMargin)
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
        p("""In the example above using multiple index hints, we saw that the planner chose to do a join, but not on the `p` node.
            |By supplying a join hint in addition to the index hints, we can enforce the join to happen on the `p` node.""")
        query(s"""$matchString
              |USING INDEX s:Scientist(born)
              |USING INDEX cc:Country(formed)
              |USING JOIN ON p
              |RETURN *""",
          assertPlan(AND(AND(ShouldUseNodeIndexSeekOn("s"), ShouldUseNodeIndexSeekOn("cc")), ShouldUseJoinOn("p")))) {
          profileExecutionPlan()
        }
      }
      section("Hinting a join for an OPTIONAL MATCH") {
        p(
          """A join hint can also be used to force the planner to pick a `NodeLeftOuterHashJoin` or `NodeRightOuterHashJoin` to solve an `OPTIONAL MATCH`.
            |In most cases, the planner will rather use an `OptionalExpand`.""".stripMargin)
        query(s"""MATCH (s:Scientist {born: 1850})
                 |OPTIONAL MATCH (s)-[:RESEARCHED]->(sc:Science)
                 |RETURN *""",
          assertPlan(ShouldNotUseJoins)) {
          p("Without any hint, the planner did not use a join to solve the `OPTIONAL MATCH`.")
          profileExecutionPlan()
        }
        query(s"""MATCH (s:Scientist {born: 1850})
                 |OPTIONAL MATCH (s)-[:RESEARCHED]->(sc:Science)
                 |USING JOIN ON s
                 |RETURN *""",
          assertPlan(ShouldUseJoinOn("s"))) {
          p("Now the planner uses a join to solve the `OPTIONAL MATCH`.")
          profileExecutionPlan()
        }
      }
    }
    section("`PERIODIC COMMIT` query hint", "query-using-periodic-commit-hint") {
      p("""Importing large amounts of data using <<query-load-csv, `LOAD CSV`>> with a single Cypher query may fail due to memory constraints.
          #This will manifest itself as an `OutOfMemoryError`.""".stripMargin('#'))
      p("""For this situation _only,_ Cypher provides the global `USING PERIODIC COMMIT` query hint for updating queries using `LOAD CSV`.
          #If required, the limit for the number of rows per commit may be set as follows: `USING PERIODIC COMMIT 500`.""".stripMargin('#'))
      p("""`PERIODIC COMMIT` will process the rows until the number of rows reaches a limit.
          #Then the current transaction will be committed and replaced with a newly opened transaction.
          #If no limit is set, a default value will be used.""".stripMargin('#'))
      p("See <<load-csv-importing-large-amounts-of-data, Importing large amounts of data>> in <<query-load-csv>> for examples of `USING PERIODIC COMMIT` with and without setting the number of rows per commit.")
      important {
        p("""Using `PERIODIC COMMIT` will prevent running out of memory when importing large amounts of data.
            #However, it will also break transactional isolation and thus it should only be used where needed.""".stripMargin('#'))
      }
      note {
        p("The <<query-use, `USE` clause>> can not be used together with the `PERIODIC COMMIT` query hint.")
      }
      note {
        p(
          """Queries with the `PERIODIC COMMIT` query hint can not be routed by <<operations-manual#causal-clustering-routing, Server-side routing>>.
            |Such queries must rely on standard client-side routing, done by the Neo4j Driver.""".stripMargin)
      }
    }
  }.build()

  private def matchString =
    """MATCH (s:Scientist {born: 1850})-[:RESEARCHED]->
      |      (sc:Science)<-[i:INVENTED_BY {year: 560}]-
      |      (p:Pioneer {born: 525})-[:LIVES_IN]->
      |      (c:City)-[:PART_OF]->
      |      (cc:Country {formed: 411})""".stripMargin

  sealed trait PlanAssertion {
    def matcher: Matcher[String]
  }

  case class ShouldUseNodeIndexSeekOn(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include regex s"NodeIndexSeek\\s*\\|\\s*$variable".r
  }

  case class ShouldUseRelationshipIndexSeekOn(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include regex s"(Undirected|Directed)RelationshipIndexSeek\\s*\\|\\s*\\(\\w*\\)-\\[$variable".r
  }

  case class ShouldUseLabelScanOn(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include regex s"NodeByLabelScan\\s*\\|\\s*$variable".r
  }

  case class ShouldUseRelationshipTypeScanOn(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include regex s"(Undirected|Directed)RelationshipTypeScan\\s*\\|\\s*\\(\\w*\\)-\\[$variable".r
  }

  case class ShouldUseJoinOn(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include regex s"Node(LeftOuter|RightOuter)?HashJoin\\s*\\|\\s*$variable".r
  }

  case class ShouldUseUnionDistinct(variable: String) extends PlanAssertion {
    override def matcher: Matcher[String] = include("Union") and include regex s"Distinct\\s*\\|\\s*$variable".r
  }

  case object ShouldNotUseJoins extends PlanAssertion {
    override def matcher: Matcher[String] = not(include("Join"))
  }

  case class AND(l: PlanAssertion, r: PlanAssertion) extends PlanAssertion {
    override def matcher: Matcher[String] = l.matcher.and(r.matcher)
  }

  case class OR(l: PlanAssertion, r: PlanAssertion) extends PlanAssertion {
    override def matcher: Matcher[String] = l.matcher.or(r.matcher)
  }

  case class NOT(p: PlanAssertion) extends PlanAssertion {
    override def matcher: Matcher[String] = not(p.matcher)
  }

  private def assertPlan(assertion: PlanAssertion) = ResultAssertions(result => {
    val planString = result.executionPlanDescription().toString
    planString should assertion.matcher
  })
}
