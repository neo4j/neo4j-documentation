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

class OrderByTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("ORDER BY", "query-order")
    initQueries(
      """CREATE
        #  (a {name: 'A', age: 34, length: 170}),
        #  (b {name: 'B', age: 36}),
        #  (c {name: 'C', age: 32, length: 185}),
        #  (a)-[:KNOWS]->(b),
        #  (b)-[:KNOWS]->(c)""".stripMargin('#')
    )
    synopsis("`ORDER BY` is a sub-clause following `RETURN` or `WITH`, and it specifies that the output should be sorted and how.")
    p("""* <<order-introduction, Introduction>>
        #* <<order-nodes-by-property, Order nodes by property>>
        #* <<order-nodes-by-multiple-properties, Order nodes by multiple properties>>
        #* <<order-nodes-by-id, Order nodes by id>>
        #* <<order-nodes-by-expressions, Order nodes by expressions>>
        #* <<order-nodes-in-descending-order, Order nodes in descending order>>
        #* <<order-null, Ordering `null`>>
        #* <<order-with, Ordering in a `WITH` clause>>""".stripMargin('#'))
    section("Introduction", "order-introduction") {
      p("""`ORDER BY` relies on comparisons to sort the output, see <<cypher-ordering, Ordering and comparison of values>>.
          #You can sort on many different values, e.g. node/relationship properties, the node/relationship ids, or on most expressions.
          #If you do not specify what to sort on, there is a risk that the results are arbitrarily sorted and therefore it is best practice to be specific when using `ORDER BY`.""".stripMargin('#'))
      p("""In terms of scope of variables, `ORDER BY` follows special rules, depending on if the projecting `RETURN` or `WITH` clause is either aggregating or `DISTINCT`.
           #If it is an aggregating or `DISTINCT` projection, only the variables available in the projection are available.
           #If the projection does not alter the output cardinality (which aggregation and `DISTINCT` do), variables available from before the projecting clause are also available.
           #When the projection clause shadows already existing variables, only the new variables are available.""".stripMargin('#'))
      p("""Lastly, it is not allowed to use aggregating expressions in the `ORDER BY` sub-clause if they are not also listed in the projecting clause.
           #This last rule is to make sure that `ORDER BY` does not change the results, only the order of them.""".stripMargin('#'))
      p("""The performance of Cypher queries using `ORDER BY` on node properties can be influenced by the existence and use of an index for finding the nodes.
          #If the index can provide the nodes in the order requested in the query, Cypher can avoid the use of an expensive `Sort` operation.
          #Read more about this capability in <<query-tuning-indexes>>.""".stripMargin('#'))
      p("The following graph is used for the examples below:")
      graphViz()
    }

    note(
      p("""Strings that contain special characters can have inconsistent or non-deterministic ordering in Neo4j.
          #For details, see <<property-types-sip-note, Sorting of special characters>>.""".stripMargin('#'))
    )

    section("Order nodes by property", "order-nodes-by-property") {
      p("""`ORDER BY` is used to sort the output.""")
      query("""MATCH (n)
              #RETURN n.name, n.age
              #ORDER BY n.name""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList should equal(List(Map("n.name" -> "A", "n.age" -> 34), Map("n.name" -> "B", "n.age" -> 36), Map("n.name" -> "C", "n.age" -> 32)))
      })) {
        p("The nodes are returned, sorted by their name.")
        resultTable()
      }
    }
    section("Order nodes by multiple properties", "order-nodes-by-multiple-properties") {
      p("""You can order by multiple properties by stating each variable in the `ORDER BY` clause.
          #Cypher will sort the result by the first variable listed, and for equals values, go to the next property in the `ORDER BY` clause, and so on.""".stripMargin('#'))
      query("""MATCH (n)
              #RETURN n.name, n.age
              #ORDER BY n.age, n.name""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.age" -> 32, "n.name" -> "C"), Map("n.age" -> 34, "n.name" -> "A"), Map("n.age" -> 36, "n.name" -> "B")))
        })) {
        p("This returns the nodes, sorted first by their age, and then by their name.")
        resultTable()
      }
    }
    section("Order nodes by id", "order-nodes-by-id") {
      p("""`ORDER BY` is used to sort the output.""")
      query("""MATCH (n)
              #RETURN n.name, n.age
              #ORDER BY id(n)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList should equal(List(Map("n.name" -> "A", "n.age" -> 34), Map("n.name" -> "B", "n.age" -> 34), Map("n.name" -> "C", "n.age" -> 32)))
      })) {
        p("The nodes are returned, sorted by their internal id.")
        resultTable()
      }
    }

    note(
      p("""Keep in mind that Neo4j reuses its internal ids when nodes and relationships are deleted.
        #This means that applications using, and relying on, internal Neo4j ids, are brittle or at risk of making mistakes.
        #It is therefore recommended to use application-generated ids instead.""".stripMargin('#'))
    )

    section("Order nodes by expression", "order-nodes-by-expression") {
      p("""`ORDER BY` is used to sort the output.""")
      query("""MATCH (n)
              #RETURN n.name, n.age, n.length
              #ORDER BY keys(n)""".stripMargin('#'),
      ResultAssertions((r) => {
        r.toList should equal(List(Map("n.length" -> 170, "n.name" -> "A", "n.age" -> 34), Map("n.length" -> 185, "n.name" -> "C", "n.age" -> 32), Map("n.length" -> null, "n.name" -> "B", "n.age" -> 34)))
      })) {
        p("The nodes are returned, sorted by their properties.")
        resultTable()
      }
    }


    section("Order nodes in descending order", "order-nodes-in-descending-order") {
      p("""By adding `DESC[ENDING]` after the variable to sort on, the sort will be done in reverse order.""")
      query("""MATCH (n)
              #RETURN n.name, n.age
              #ORDER BY n.name DESC""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.age" -> 32, "n.name" -> "C"), Map("n.age" -> 36, "n.name" -> "B"), Map("n.age" -> 34, "n.name" -> "A")))
        })) {
        p("The example returns the nodes, sorted by their name in reverse order.")
        resultTable()
      }
    }
    section("Ordering `null`", "order-null") {
      p("""When sorting the result set, `null` will always come at the end of the result set for ascending sorting, and first when doing descending sort.""")
      query("""MATCH (n)
              #RETURN n.length, n.name, n.age
              #ORDER BY n.length""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("n.age" -> 34, "n.name" -> "A", "n.length" -> 170), Map("n.age" -> 32, "n.name" -> "C", "n.length" -> 185), Map("n.age" -> 36, "n.name" -> "B", "n.length" -> null)))
        })) {
        p("The nodes are returned sorted by the length property, with a node without that property last.")
        resultTable()
      }
    }
    section("Ordering in a `WITH` clause", "order-with") {
      p("""When `ORDER BY` is present on a `WITH` clause , the immediately following clause will receive records in the specified order.
          #The order is not guaranteed to be retained after the following clause, unless that also has an `ORDER BY` subclause.
          #The ordering guarantee can be useful to exploit by operations which depend on the order in which they consume values.
          #For example, this can be used to control the order of items in the list produced by the `collect()` aggregating function.
          #The `MERGE` and `SET` clauses also have ordering dependencies which can be controlled this way.
          #""".stripMargin('#'))
      query("""MATCH (n)
              #WITH n ORDER BY n.age
              #RETURN collect(n.name) AS names""".stripMargin('#'),
      ResultAssertions((r) => {
          r.toList should equal(List(Map("names" -> List("C", "A", "B"))))
        })) {
        p("The list of names built from the `collect` aggregating function contains the names in order of the `age` property.")
        resultTable()
      }
    }
  }.build()
}
