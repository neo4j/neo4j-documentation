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

import org.junit.Assert._
import org.neo4j.cypher.docgen.tooling._
import org.neo4j.values.storable.DurationValue

class AggregatingFunctionsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Aggregating functions", "query-functions-aggregating")
    initQueries(
      """CREATE
        #  (a:Person {name: 'A', age: 13}),
        #  (b:Person {name: 'B', age: 33, eyes: 'blue'}),
        #  (c:Person {name: 'C', age: 44, eyes: 'blue'}),
        #  (d1:Person {name: 'D', eyes: 'brown'}),
        #  (d2:Person {name: 'D'}),
        #  (book:Book {name: 'Cypher'}),
        #  (a)-[:READS]->(book),
        #  (a)-[:KNOWS]->(d1),
        #  (a)-[:KNOWS]->(c),
        #  (a)-[:KNOWS]->(b),
        #  (c)-[:KNOWS]->(d2),
        #  (b)-[:KNOWS]->(d2)""".stripMargin('#'))
    synopsis("Aggregating functions take a set of values and calculate an aggregated value over them.")
    p("""Functions:
        #
        #* <<functions-avg,avg() - Numeric values>>
        #* <<functions-avg-duration,avg() - Durations>>
        #* <<functions-collect,collect()>>
        #* <<functions-count,count()>>
        #* <<functions-max,max()>>
        #* <<functions-min,min()>>
        #* <<functions-percentilecont,percentileCont()>>
        #* <<functions-percentiledisc,percentileDisc()>>
        #* <<functions-stdev,stDev()>>
        #* <<functions-stdevp,stDevP()>>
        #* <<functions-sum,sum() - Numeric values>>
        #* <<functions-sum-duration,sum() - Durations>>""".stripMargin('#'))
    p("""Aggregation can be computed over all the matching paths, or it can be further divided by introducing grouping keys.
        #Grouping keys are non-aggregate expressions, that are used to group the values going into the aggregate functions.""".stripMargin('#'))
    p("Assume we have the following return statement:")
    p("""[source, cypher]
        #----
        #RETURN n, count(*)
        #----""".stripMargin('#'))
    p("""We have two return expressions: `n`, and `+count(*)+`.
        #The first, `n`, is not an aggregate function, so it will be the grouping key.
        #The latter, `+count(*)+` is an aggregate expression.
        #The matching paths will be divided into different buckets, depending on the grouping key.
        #The aggregate function will then be run on these buckets, calculating an aggregate value per bucket.""".stripMargin('#'))
    p("""The input expression of an aggregation function can contain any expression, including expressions that are not grouping keys.
        #However, not all expressions can be composed with aggregation functions.
        #The example below will throw an error since we compose `n.x`, which is not a grouping key, with the aggregation expression `+count(*)+`.
        #For more information see <<grouping-keys,Grouping keys>>.""".stripMargin('#'))
    p("""[source, cypher]
        #----
        #RETURN n.x + count(*)
        #----""".stripMargin('#'))
    p("To use aggregations to sort the result set, the aggregation must be included in the `RETURN` to be used in the `ORDER BY`.")
    p("""The `DISTINCT` operator works in conjunction with aggregation.
        #It is used to make all values unique before running them through an aggregate function.
        #More information about `DISTINCT` may be found in <<query-operators-aggregation, Syntax -> Aggregation operators>>.""".stripMargin('#'))
    p("The following graph is used for the examples below:")
    graphViz()
    section("avg() - Numeric values", "functions-avg") {
      p("The function `avg()` returns the average of a set of numeric values.")
      function("avg(expression)",
        "Either an Integer or a Float, depending on the values returned by `expression` and whether or not the calculation overflows.",
        ("expression", "An expression returning a set of numeric values."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`avg(null)` returns `null`.")
      query("""MATCH (n:Person)
              #RETURN avg(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("avg(n.age)") should equal(30L)
      })) {
        p("The average of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("avg() - Durations", "functions-avg-duration") {
      p("The function `avg()` returns the average of a set of Durations.")
      function("avg(expression)",
        "A Duration.",
        ("expression", "An expression returning a set of Durations."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`avg(null)` returns `null`.")
      query("""UNWIND [duration('P2DT3H'), duration('PT1H45S')] AS dur
              #RETURN avg(dur)""".stripMargin('#'),
      ResultAssertions(r => {
        r.toList.head("avg(dur)") should equal(DurationValue.duration(0, 1, 2 * 3600 + 22, 500000000))
      })) {
        p("The average of the two supplied Durations is returned.")
        resultTable()
      }
    }
    section("collect()", "functions-collect") {
      p("The function `collect()` returns a single aggregated list containing the values returned by an expression.")
      function("collect(expression)",
        "A list containing heterogeneous elements; the types of the elements are determined by the values returned by `expression`.",
        ("expression", "An expression returning a set of values."))
      considerations(
        "Any `null` values are ignored and will not be added to the list.",
        "`collect(null)` returns an empty list.")
      query("""MATCH (n:Person)
              #RETURN collect(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("collect(n.age)") should equal(Seq(13, 33, 44))
      })) {
        p("All the values are collected and returned in a single list.")
        resultTable()
      }
    }
    section("count()", "functions-count") {
      p("""The function `count()` returns the number of values or rows, and appears in two variants:
          #
          #`count(*)`:: returns the number of matching rows.
          #`count(expr)`:: returns the number of non-`null` values returned by an expression.""".stripMargin('#'))
      function("count(expression)",
        "An Integer.",
        ("expression", "An expression."))
      considerations(
        "`count(*)` includes rows returning `null`.",
        "`count(expr)` ignores `null` values.",
        "`count(null)` returns `0`.")
      section("Using `count(*)` to return the number of nodes") {
        p("The function `count(*)` can be used to return the number of nodes; for example, the number of nodes connected to some node `n`.")
        query("""MATCH (n {name: 'A'})-->(x)
                #RETURN labels(n), n.age, count(*)""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("labels(n)" -> List("Person"), "n.age" -> 13L, "count(*)" -> 4L)))
        })) {
          p("The labels and `age` property of the start node `n` and the number of nodes related to `n` are returned.")
          resultTable()
        }
      }
      section("Using `count(*)` to group and count relationship types") {
        p("The function `count(*)` can be used to group the type of matched relationships and return the number.")
        query("""MATCH (n {name: 'A'})-[r]->()
                #RETURN type(r), count(*)""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toSet should equal(Set(
            Map("type(r)" -> "KNOWS", "count(*)" -> 3L),
            Map("type(r)" -> "READS", "count(*)" -> 1L)))
        })) {
          p("The type of matched relationships are grouped and the group count are returned.")
          resultTable()
        }
      }
      section("Using `count(expression)` to return the number of values") {
        p("Instead of simply returning the number of rows with `count(*)`, it may be more useful to return the actual number of values returned by an expression.")
        query("""MATCH (n {name: 'A'})-->(x)
                #RETURN count(x)""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("count(x)" -> 4L)))
        })) {
          p("The number of nodes that are connected directly (one relationship) to the node, with the name `'A'`, is returned.")
          resultTable()
        }
      }
      section("Counting non-`null` values") {
        p("The function `count(expression)` can be used to return the number of non-`null` values returned by the expression.")
        query("""MATCH (n:Person)
                #RETURN count(n.age)""".stripMargin('#'),
        ResultAssertions((r) => {
          r.toList should equal(List(Map("count(n.age)" -> 3L)))
        })) {
          p("The number of nodes with the label `Person` and a property `age` is returned. (If you want the sum, use `sum(n.age)`)")
          resultTable()
        }
      }
      section("Counting with and without duplicates") {
        p("""In this example we are trying to find all our friends of friends, and count them:
            #
            #`count(DISTINCT friend_of_friend)`:: Will only count a `friend_of_friend` once, as `DISTINCT` removes the duplicates.
            #`count(friend_of_friend)`:: Will consider the same `friend_of_friend` multiple times.""".stripMargin('#'))
        query("""MATCH (me:Person)-->(friend:Person)-->(friend_of_friend:Person)
                #WHERE me.name = 'A'
                #RETURN count(DISTINCT friend_of_friend), count(friend_of_friend)""".stripMargin('#'),
        ResultAssertions((r) => {
            r.toList should equal(List(Map("count(DISTINCT friend_of_friend)" -> 1L, "count(friend_of_friend)" -> 2L)))
          })) {
          p("Both `B` and `C` know `D` and thus `D` will get counted twice when not using `DISTINCT`.")
          resultTable()
        }
      }
    }
    section("max()", "functions-max") {
      p("The function `max()` returns the maximum value in a set of values.")
      function("max(expression)",
        "A <<property-types, property type>>, or a list, depending on the values returned by `expression`.",
        ("expression", "An expression returning a set containing any combination of <<property-types, property types>> and lists thereof."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "In a mixed set, any numeric value is always considered to be higher than any string value, and any string value is always considered to be higher than any list.",
        "Lists are compared in dictionary order, i.e. list elements are compared pairwise in ascending order from the start of the list to the end.",
        "`max(null)` returns `null`.")
      query("""UNWIND [1, 'a', null, 0.2, 'b', '1', '99'] AS val
              #RETURN max(val)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("max(val)") should equal(1L)
      })) {
        p("The highest of all the values in the mixed set -- in this case, the numeric value `1` -- is returned.")
        note {
          p("The value `'99'` (a string), is considered to be a lower value than `1` (an integer), because `'99'` is a string.")
        }
        resultTable()
      }
      query("""UNWIND [[1, 'a', 89], [1, 2]] AS val
              #RETURN max(val)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("max(val)") should equal(List(1L, 2L))
      })) {
        p("""The highest of all the lists in the set -- in this case, the list `[1, 2]` -- is returned, as the number `2` is considered to be a higher value than the string `'a'`, even though the list `[1, 'a', 89]` contains more elements.""")
        resultTable()
      }
      query("""MATCH (n:Person)
              #RETURN max(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("max(n.age)") should equal(44L)
      })) {
        p("The highest of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("min()", "functions-min") {
      p("The function `min()` returns the minimum value in a set of values.")
      function("min(expression)",
        "A <<property-types, property type>>, or a list, depending on the values returned by `expression`.",
        ("expression", "An expression returning a set containing any combination of <<property-types, property types>> and lists thereof."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "In a mixed set, any string value is always considered to be lower than any numeric value, and any list is always considered to be lower than any string.",
        "Lists are compared in dictionary order, i.e. list elements are compared pairwise in ascending order from the start of the list to the end.",
        "`min(null)` returns `null`.")
      query("""UNWIND [1, 'a', null, 0.2, 'b', '1', '99'] AS val
              #RETURN min(val)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("min(val)") should equal("1")
      })) {
        p("""The lowest of all the values in the mixed set -- in this case, the string value `"1"` -- is returned.
            #Note that the (numeric) value `0.2`, which may _appear_ at first glance to be the lowest value in the list, is considered to be a higher value than `"1"` as the latter is a string.""".stripMargin('#'))
        resultTable()
      }
      query("""UNWIND ['d', [1, 2], ['a', 'c', 23]] AS val
              #RETURN min(val)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("min(val)") should equal(List("a", "c", 23L))
      })) {
        p("""The lowest of all the values in the set -- in this case, the list `['a', 'c', 23]` -- is returned, as (i) the two lists are considered to be lower values than the string `"d"`, and (ii) the string `"a"` is considered to be a lower value than the numerical value `1`.""")
        resultTable()
      }
      query("""MATCH (n:Person)
              #RETURN min(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("min(n.age)") should equal(13L)
      })) {
        p("The lowest of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("percentileCont()", "functions-percentilecont") {
      p("""The function `percentileCont()` returns the percentile of the given value over a group, with a percentile from 0.0 to 1.0.
          #It uses a linear interpolation method, calculating a weighted average between two values if the desired percentile lies between them.
          #For nearest values using a rounding method, see `percentileDisc`.""".stripMargin('#'))
      function("percentileCont(expression, percentile)",
        "A Float.",
        ("expression", "A numeric expression."),
        ("percentile", "A numeric value between 0.0 and 1.0"))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`percentileCont(null, percentile)` returns `null`.")
      query("""MATCH (n:Person)
              #RETURN percentileCont(n.age, 0.4)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("percentileCont(n.age, 0.4)") should equal(29L)
      })) {
        p("The 40th percentile of the values in the property `age` is returned, calculated with a weighted average.")
        resultTable()
      }
    }
    section("percentileDisc()", "functions-percentiledisc") {
      p("""The function `percentileDisc()` returns the percentile of the given value over a group, with a percentile from 0.0 to 1.0.
          #It uses a rounding method and calculates the nearest value to the percentile.
          #For interpolated values, see `percentileCont`.""".stripMargin('#'))
      function("percentileDisc(expression, percentile)",
        "Either an Integer or a Float, depending on the values returned by `expression` and whether or not the calculation overflows.",
        ("expression", "A numeric expression."),
        ("percentile", "A numeric value between 0.0 and 1.0"))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`percentileDisc(null, percentile)` returns `null`.")
      query("""MATCH (n:Person)
              #RETURN percentileDisc(n.age, 0.5)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("percentileDisc(n.age, 0.5)") should equal(33L)
      })) {
        p("The 50th percentile of the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("stDev()", "functions-stdev") {
      p("""The function `stDev()` returns the standard deviation for the given value over a group.
          #It uses a standard two-pass method, with `N - 1` as the denominator, and should be used when taking a sample of the population for an unbiased estimate.
          #When the standard variation of the entire population is being calculated, `stdDevP` should be used.""".stripMargin('#'))
      function("stDev(expression)",
        "A Float.",
        ("expression", "A numeric expression."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`stDev(null)` returns `0`.")
      query("""MATCH (n)
              #WHERE n.name IN ['A', 'B', 'C']
              #RETURN stDev(n.age)""".stripMargin('#'),
      ResultAssertions(f = (r) => {
        assertEquals(15.7162336455, r.toList.head("stDev(n.age)").asInstanceOf[Double], 0.0000001)
      })) {
        p("The standard deviation of the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("stDevP()", "functions-stdevp") {
      p("""The function `stDevP()` returns the standard deviation for the given value over a group.
          #It uses a standard two-pass method, with `N` as the denominator, and should be used when calculating the standard deviation for an entire population.
          #When the standard variation of only a sample of the population is being calculated, `stDev` should be used.""".stripMargin('#'))
      function("stDevP(expression)",
        "A Float.",
        ("expression", "A numeric expression."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`stDevP(null)` returns `0`.")
      query("""MATCH (n)
              #WHERE n.name IN ['A', 'B', 'C']
              #RETURN stDevP(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        assertEquals(12.8322510366, r.toList.head("stDevP(n.age)").asInstanceOf[Double], 0.0000001)
      })) {
        p("The population standard deviation of the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("sum() - Numeric values", "functions-sum") {
      p("The function `sum()` returns the sum of a set of numeric values.")
      function("sum(expression)",
        "Either an Integer or a Float, depending on the values returned by `expression`.",
        ("expression", "An expression returning a set of numeric values."))
      considerations(
        "Any `null` values are excluded from the calculation.",
        "`sum(null)` returns `0`.")
      query("""MATCH (n:Person)
              #RETURN sum(n.age)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList.head("sum(n.age)") should equal(90L)
      })) {
        p("The sum of all the values in the property `age` is returned.")
        resultTable()
      }
    }
    section("sum() - Durations", "functions-sum-duration") {
      p("The function `sum()` returns the sum of a set of durations.")
      function("sum(expression)",
        "A Duration.",
        ("expression", "An expression returning a set of Durations."))
      considerations("Any `null` values are excluded from the calculation.")
      query("""UNWIND [duration('P2DT3H'), duration('PT1H45S')] AS dur
              #RETURN sum(dur)""".stripMargin('#'),
      ResultAssertions(r => {
        r.toList.head("sum(dur)") should equal(DurationValue.duration(0, 2, 4 * 3600 + 45, 0))
      })) {
        p("The sum of the two supplied Durations is returned.")
        resultTable()
      }
    }
    section("Grouping keys", "grouping-keys") {
      p("""Aggregation expressions are expressions which contain one or more aggregation functions.
          #A simple aggregation expression consists of a single aggregation function.
          #For instance, `SUM(x.a)` is an aggregation expression that only consists of the aggregation function `SUM( )` with `x.a` as its argument.
          #Aggregation expressions are also allowed to be more complex, where the result of one or more aggregation functions
          #are input arguments to other expressions.
          #For instance, `0.1 * (SUM(x.a) / COUNT(x.b))` is an aggregation expression that contains two aggregation functions,
          #`SUM( )` with `x.a` as its argument and `COUNT( )` with `x.b` as its argument.
          #Both are input arguments to the division expression.
          #""".stripMargin(
          '#'))

      p(
        """For aggregation expressions to be correctly computable for the buckets formed by the grouping key(s), they have to fulfill some requirements.
          #Specifically, each sub expression in an aggregation expression has to be either:
          #
          #* an aggregation function, e.g. `SUM(x.a)`,
          #* a constant, e.g. `0.1`,
          #* a parameter, e.g. `$param`,
          #* a grouping key, e.g. the `a` in `RETURN a, count(*)`
          #* a local variable, e.g. the `x` in  `count(*) + size([ x IN range(1, 10) | x ])`, or
          #* a subexpression, all whose operands are operands allowed in an aggregation expression.
          #""".stripMargin('#'))

      section("Examples of aggregation expressions.", "grouping-key-examples") {
        p("Simple aggregation without any grouping keys:")
        query("""MATCH (p: Person) RETURN max(p.age)""",
          ResultAssertions((r) => {
            r.toList.head("max(p.age)") should equal(44L)
          })) {
          resultTable()
        }
        p("Addition of an aggregation and a constant, without any grouping keys:")
        query("""MATCH (p: Person) RETURN max(p.age) + 1""",
          ResultAssertions((r) => {
            r.toList.head("max(p.age) + 1") should equal(45L)
          })) {
          resultTable()
        }
        p("Subtraction of a property access and an aggregation. Note that `n` is a grouping key:")
        query("""MATCH (n: Person{name:"A"})-[:KNOWS]-(f:Person) RETURN n, n.age - max(f.age)""",
          ResultAssertions((r) => {
            r.toList.size shouldBe 1
            r.head.get("n.age - max(f.age)") should equal(Some(-31L))
          })) {
          resultTable()
        }
        p("Subtraction of a property access and an aggregation. Note that `n.age` is a grouping key:")
        query("""MATCH (n: Person{name:"A"})-[:KNOWS]-(f:Person) RETURN n.age, n.age - max(f.age)""",
          ResultAssertions((r) => {
            r.toList should equal(List(Map("n.age" -> 13L, "n.age - max(f.age)" -> -31)))
          })) {
          resultTable()
        }

        p(
          """
            #Grouping keys themselves can be complex expressions.
            #For better query readability, Cypher only recognizes a sub-expression in aggregation expressions as a grouping key if the grouping key is either:
            #
            #- A variable - e.g. the `n` in `RETURN n, n.age - max(f.age)`
            #- A property access - e.g. the `n.age` in `RETURN n.age, n.age - max(f.age)`
            #- A map access - e.g. the `n.age` in `WITH {age: 34, name:Chris} AS n RETURN n.age, n.age - max(n.age)`
            #
            #If more complex grouping keys are needed as operands in aggregation expression, it is always possible to project them in advance with `WITH`.
            #""".stripMargin('#'))

        p("Using the property `n.age` will throw an exception, since `n.age` is not a grouping keys:")
        query("""MATCH (n: Person{name:"A"})-[:KNOWS]-(f:Person) RETURN n.age - max(f.age)""",
          ErrorAssertions((t) => {
            t.getMessage should include("Aggregation column contains implicit grouping expressions.")
          })) {
        }
        p("`n.age + n.age` is not a valid grouping key, since the expression is not a variable, property access or map access. " +
          "It can therefore not be used in the expression which contains the aggregation function:")
        query("""MATCH (n: Person{name:"A"})-[:KNOWS]-(f:Person) RETURN n.age + n.age, n.age + n.age - max(f.age)""",
          ErrorAssertions((t) => {
            t.getMessage should include("Aggregation column contains implicit grouping expressions.")
          })) {
        }
        note {
          p(
            """The above query could be rewritten to:
              #`MATCH (n: Person{name:"A"})-[:KNOWS]-(f:Person) WITH n.age + n.age AS groupingKey, f RETURN groupingKey, groupingKey - max(f.age)`""".stripMargin('#'))
        }
      }
    }
  }.build()
}
