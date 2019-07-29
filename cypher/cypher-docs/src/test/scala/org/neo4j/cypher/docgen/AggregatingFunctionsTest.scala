/*
 * Copyright (c) 2002-2019 "Neo Technology,"
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

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.{Relationship, Path, Node}
import org.junit.Assert._

class AggregatingFunctionsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Aggregating functions", "query-functions-aggregating")
    initQueries(
      """CREATE (a:Person {name: 'A', age: 13}),
        |       (b:Person {name: 'B', age: 33, eyes: 'blue'}),
        |       (c:Person {name: 'C', age: 44, eyes: 'blue'}),
        |       (d1:Person {name: 'D', eyes: 'brown'}),
        |       (d2:Person {name: 'D'}),
        |
        |       (a)-[:KNOWS]->(d1),
        |       (a)-[:KNOWS]->(c),
        |       (a)-[:KNOWS]->(b),
        |       (c)-[:KNOWS]->(d2),
        |       (b)-[:KNOWS]->(d2)""")
    p("""To calculate aggregated data, Cypher offers aggregation, much like SQL's `GROUP BY`.""")
    p(
      """Aggregate functions take multiple input values and calculate an aggregated value from them.
        |Examples are `avg()` that calculates the average of multiple numeric values, or `min()` that finds the smallest numeric value in a set of values.""")
    p(
      """Aggregation can be done over all the matching subgraphs, or it can be further divided by introducing key values.
        |These are non-aggregate expressions, that are used to group the values going into the aggregate functions.""")
    p("""So, if the return statement looks something like this:""")
    p(
      """[source, cypher]
        |----
        |RETURN n, count(*)
        |----
        |""")
    p(
      """We have two return expressions: `n`, and `count(*)`.
        |The first, `n`, is not an aggregate function, and so it will be the grouping key.
        |The latter, `count(*)` is an aggregate expression.
        |So the matching subgraphs will be divided into different buckets, depending on the grouping key.
        |The aggregate function will then run on these buckets, calculating the aggregate values.""")
    p(
      """If you want to use aggregations to sort your result set, the aggregation must be included in the `RETURN` to be used in your `ORDER BY`.
        |""")
    p(
      """The last piece of the puzzle is the `DISTINCT` operator.
        |It is used to make all values unique before running them through an aggregate function.
        |More information about `DISTINCT` may be found <<query-operators-general,here>>.""")
    p(
      """
        |* <<functions-avg,avg()>>
        |* <<functions-collect,collect()>>
        |* <<functions-count,count()>>
        |* <<functions-max,max()>>
        |* <<functions-min,min()>>
        |* <<functions-percentilecont,percentileCont()>>
        |* <<functions-percentiledisc,percentileDisc()>>
        |* <<functions-stdev,stDev()>>
        |* <<functions-stdevp,stDevP()>>
        |* <<functions-sum,sum()>>
      """.stripMargin)
    p("The following graph is used for the examples below:")
    graphViz()
    section("avg()", "functions-avg") {
      p("`avg()` calculates the average of a numeric column.")
      function("avg(expression)", ("expression", "A numeric expression."))
      query("MATCH (n:Person) RETURN avg(n.age)", ResultAssertions((r) => {
        r.toList.head("avg(n.age)") should equal(30L)
      })) {
        p("The average of all the values in the property `age` is returned by the example query.")
        resultTable()
      }
    }
    section("collect()", "functions-collect") {
      p(
        """`collect()` collects all the values into a list.
          |It will ignore `null` values.""".stripMargin)
      function("collect(expression)", ("expression", "An expression."))
      query("MATCH (n:Person) RETURN collect(n.age)", ResultAssertions((r) => {
        r.toList.head("collect(n.age)") should equal(Seq(13, 33, 44))
      })) {
        p("Returns a single row, with all the values collected.")
        resultTable()
      }
    }
    section("count()", "functions-count") {
      p(
        """`count()` is used to count the number of rows.
          |`count()` can be used in two forms -- `count(*)` which just counts the number of matching rows, and `count(<expression>)`, which counts the number of non-`null` values in `<expression>`.
        """.stripMargin)
      function("count(expression)", ("expression", "An expression."))
      section("Count nodes") {
        p("`count(*)` can be used to count the number of nodes; for example, the number of nodes connected to one node.")
        query("MATCH (n {name: 'A'})-->(x) RETURN labels(n), n.age, count(*)", ResultAssertions((r) => {
          r.toList should equal(List(Map("labels(n)" -> List("Person"), "n.age" -> 13L, "count(*)" -> 3L)))
        })) {
          p("This returns the start node and the count of related nodes.")
          resultTable()
        }
      }
      section("Group count relationship types") {
        p("To count the groups of relationship types, return the types and count them with `count(*)`.")
        query("MATCH (n {name: 'A'})-[r]->() RETURN type(r), count(*)", ResultAssertions((r) => {
          r.toList should equal(List(Map("type(r)" -> "KNOWS", "count(*)" -> 3L)))
        })) {
          p("The relationship types and their group count is returned.")
          resultTable()
        }
      }
      section("Count entities") {
        p("Instead of counting the number of results with `count(*)`, it might be more expressive to include the name of the variable you care about.")
        query("MATCH (n {name: 'A'})-->(x) RETURN count(x)", ResultAssertions((r) => {
          r.toList should equal(List(Map("count(x)" -> 3L)))
        })) {
          p("The number of nodes connected to the start node is returned.")
          resultTable()
        }
      }
      section("Count non-`null` values") {
        p("The non-`null` values can be counted by using `count(<expression>)`.")
        query("MATCH (n:Person) RETURN count(n.age)", ResultAssertions((r) => {
          r.toList should equal(List(Map("count(n.age)" -> 3L)))
        })) {
          p("The count of related nodes with the `age` property set is returned.")
          resultTable()
        }
      }
      section("Counting with and without duplicates") {
        p(
          """In this example we are trying to find all our friends of friends, and count them.
            |The first aggregate function, `count(DISTINCT friend_of_friend)`, will only see a `friend_of_friend` once -- `DISTINCT` removes the duplicates.
            |The latter aggregate function, `count(friend_of_friend)`, might very well see the same `friend_of_friend` multiple times.
            |In this case, both `B` and `C` know `D` and thus `D` will get counted twice, when not using `DISTINCT`.""")
        query(
          """MATCH (me:Person)-->(friend:Person)-->(friend_of_friend:Person)
            |WHERE me.name = 'A'
            |RETURN count(DISTINCT friend_of_friend), count(friend_of_friend)""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("count(DISTINCT friend_of_friend)" -> 1L, "count(friend_of_friend)" -> 2L)))
          })) {
          resultTable()
        }
      }
    }
    section("max()", "functions-max") {
      p("`max()` takes a numeric property as input, and returns the highest value in that column.")
      function("max(expression)", ("expression", "A numeric expression."))
      query("MATCH (n:Person) RETURN max(n.age)", ResultAssertions((r) => {
        r.toList.head("max(n.age)") should equal(44L)
      })) {
        p("The highest of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("min()", "functions-min") {
      p("`min()` takes a numeric property as input, and returns the lowest value in that column.")
      function("min(expression)", ("expression", "A numeric expression."))
      query("MATCH (n:Person) RETURN min(n.age)", ResultAssertions((r) => {
        r.toList.head("min(n.age)") should equal(13L)
      })) {
        p("The lowest of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("percentileCont()", "functions-percentilecont") {
      p(
        """`percentileCont()` calculates the percentile of a given value over a group, with a percentile from 0.0 to 1.0.
          |It uses a linear interpolation method, calculating a weighted average between two values, if the desired percentile lies between them.
          |For nearest values using a rounding method, see `percentileDisc`.""")
      function("percentileCont(expression, percentile)", ("expression", "A numeric expression."), ("percentile", "A numeric value between 0.0 and 1.0"))
      query("MATCH (n:Person) RETURN percentileCont(n.age, 0.4)", ResultAssertions((r) => {
        r.toList.head("percentileCont(n.age, 0.4)") should equal(29L)
      })) {
        p("The 40th percentile of the values in the property `age` is returned by the example query, calculated with a weighted average.")
        resultTable()
      }
    }
    section("percentileDisc()", "functions-percentiledisc") {
      p(
        """`percentileDisc()` calculates the percentile of a given value over a group, with a percentile from 0.0 to 1.0.
          |It uses a rounding method, returning the nearest value to the percentile.
          |For interpolated values, see `percentileCont`.""")
      function("percentileDisc(expression, percentile)", ("expression", "A numeric expression."), ("percentile", "A numeric value between 0.0 and 1.0"))
      query("MATCH (n:Person) RETURN percentileDisc(n.age, 0.5)", ResultAssertions((r) => {
        r.toList.head("percentileDisc(n.age, 0.5)") should equal(33L)
      })) {
        p("The 50th percentile of the values in the property `age` is returned by the example query. In this case, 0.5 is the median, or 50th percentile.")
        resultTable()
      }
    }
    section("stDev()", "functions-stdev") {
      p(
        """`stDev()` calculates the standard deviation for a given value over a group.
          |It uses a standard two-pass method, with `N - 1` as the denominator, and should be used when taking a sample of the population for an unbiased estimate.
          |When the standard variation of the entire population is being calculated, `stdDevP` should be used.""")
      function("stDev(expression)", ("expression", "A numeric expression."))
      query("MATCH (n) WHERE n.name IN ['A', 'B', 'C'] RETURN stDev(n.age)", ResultAssertions(f = (r) => {
        assertEquals(15.7162336455, r.toList.head("stDev(n.age)").asInstanceOf[Double], 0.0000001)
      })) {
        p("The standard deviation of the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("stDevP()", "functions-stdevp") {
      p(
        """`stDevP()` calculates the standard deviation for a given value over a group.
          |It uses a standard two-pass method, with `N` as the denominator, and should be used when calculating the standard deviation for an entire population.
          |When the standard variation of only a sample of the population is being calculated, `stDev` should be used.""")
      function("stDevP(expression)", ("expression", "A numeric expression."))
      query("MATCH (n) WHERE n.name IN ['A', 'B', 'C'] RETURN stDevP(n.age)", ResultAssertions((r) => {
        assertEquals(12.8322510366, r.toList.head("stDevP(n.age)").asInstanceOf[Double], 0.0000001)
      })) {
        p("The population standard deviation of the values in the property `age` is returned by the example query.")
        resultTable()
      }
    }
    section("sum()", "functions-sum") {
      p(
        """`sum()` simply adds up all the numeric values it encounters.
          |Any `null` values are silently dropped.""".stripMargin)
      function("sum(expression)", ("expression", "A numeric expression."))
      query("MATCH (n:Person) RETURN sum(n.age)", ResultAssertions((r) => {
        r.toList.head("sum(n.age)") should equal(90L)
      })) {
        p("This returns the sum of all the values in the property `age`.")
        resultTable()
      }
    }
  }.build()
}







