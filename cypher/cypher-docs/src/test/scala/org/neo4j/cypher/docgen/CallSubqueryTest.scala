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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, ResultAssertions}

class CallSubqueryTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql"

  var nodeId:Long = 1

  override def doc: Document = new DocBuilder {
    doc("CALL {} (subquery)", "query-call-subquery")
    initQueries("""CREATE
                  #  (a:Person:Child {age: 20, name: 'Alice'}),
                  #  (b:Person {age: 27, name: 'Bob'}),
                  #  (c:Person:Parent {age: 65, name: 'Charlie'}),
                  #  (d:Person {age: 30, name: 'Dora'})
                  #  CREATE (a)-[:FRIEND_OF]->(b)
                  #  CREATE (a)-[:CHILD_OF]->(c)""".stripMargin('#'))
    synopsis("The `CALL {}` clause evaluates a subquery that returns some values.")
    p("""* <<subquery-call-introduction, Introduction>>
        #* <<subquery-correlated-importing, Importing variables into subqueries>>
        #* <<subquery-post-union, Post-union processing>>
        #* <<subquery-aggregation, Aggregations>>
        #* <<subquery-unit, Unit subqueries and side-effects>>
        #* <<subquery-correlated-aggregation, Aggregation on imported variables>>""".stripMargin('#'))
    section("Introduction", "subquery-call-introduction") {
      p("""CALL allows to execute subqueries, i.e. queries inside of other queries.
          #Subqueries allow you to compose queries, which is especially useful when working with `UNION` or aggregations.""".stripMargin('#'))
      tip{
        p("""The `CALL` clause is also used for calling procedures.
            #For descriptions of the `CALL` clause in this context, refer to <<query-call>>.""".stripMargin('#'))
      }

      p(
        """Subqueries which end in a `RETURN` statement are called _returning subqueries_ while subqueries without such a return statement are called _unit
          |subqueries_.""".stripMargin)
      p("""A subquery is evaluated for each incoming input row. Every output row of a *returning subquery* is combined with the input row to build the result of
          #the subquery.
          #That means that a returning subquery will influence the number of rows.
          #If the subquery does not return any rows, there will be no rows available after the subquery.""".stripMargin('#'))
      p(
        """*Unit subqueries* on the other hand are called for their side-effects and not for their results and do therefore not influence
          #the results of the enclosing query.""".stripMargin('#'))
      p("There are restrictions on how subqueries interact with the enclosing query:")
      p("""* A subquery can only refer to variables from the enclosing query if they are explicitly imported.
          #* A subquery cannot return variables with the same names as variables in the enclosing query.
          #* All variables that are returned from a subquery are afterwards available in the enclosing query.""".stripMargin('#'))
      p("The following graph is used for the examples below:")
      graphViz()
    }

    section("Importing variables into subqueries", "subquery-correlated-importing") {
      p("""Variables are imported into a subquery using an importing `WITH` clause.
          #As the subquery is evaluated for each incoming input row, the imported variables get bound to the corresponding values from the input row in each
          #evaluation.""".stripMargin('#'))

      query("""UNWIND [0, 1, 2] AS x
              #CALL {
              #  WITH x
              #  RETURN x * 10 AS y
              #}
              #RETURN x, y""".stripMargin('#'),
      ResultAssertions(r => r.toSet should equal(Set(
          Map("x" -> 0, "y" -> 0),
          Map("x" -> 1, "y" -> 10),
          Map("x" -> 2, "y" -> 20),
        )))) { resultTable() }

      p("An importing `WITH` clause must:")
      p("""* Consist only of simple references to outside variables - e.g. `WITH x, y, z`. Aliasing or expressions are not supported in importing `WITH`
          #clauses - e.g. `WITH a AS b` or `WITH a+1 AS b`.
          #* Be the first clause of a subquery (or the second clause, if directly following a `USE` clause).""".stripMargin('#'))
    }

    section("Post-union processing", "subquery-post-union") {
      p("""Subqueries can be used to process the results of a `UNION` query further.
          #This example query finds the youngest and the oldest person in the database and orders them by name.""".stripMargin('#'))
      query("""CALL {
              #  MATCH (p:Person)
              #  RETURN p
              #  ORDER BY p.age ASC
              #  LIMIT 1
              #UNION
              #  MATCH (p:Person)
              #  RETURN p
              #  ORDER BY p.age DESC
              #  LIMIT 1
              #}
              #RETURN p.name, p.age
              #ORDER BY p.name""".stripMargin('#'),
      ResultAssertions(r => r.toList should equal(Seq(
          Map("p.name" -> "Alice", "p.age" -> 20),
          Map("p.name" -> "Charlie", "p.age" -> 65)
        )))) { resultTable() }

      p("""If different parts of a result should be matched differently, with some aggregation over the whole results, subqueries need to be used.
          #This example query finds friends and/or parents for each person.
          #Subsequently the number of friends and parents are counted together.""".stripMargin('#'))
      query("""MATCH (p:Person)
              #CALL {
              #  WITH p
              #  OPTIONAL MATCH (p)-[:FRIEND_OF]->(other:Person)
              #  RETURN other
              #UNION
              #  WITH p
              #  OPTIONAL MATCH (p)-[:CHILD_OF]->(other:Parent)
              #  RETURN other
              #}
              #RETURN DISTINCT p.name, count(other)""".stripMargin('#'),
      ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "count(other)" -> 2),
          Map("p.name" -> "Bob", "count(other)" -> 0),
          Map("p.name" -> "Charlie", "count(other)" -> 0),
          Map("p.name" -> "Dora", "count(other)" -> 0),
        )))) { resultTable() }
    }

    section("Aggregations", "subquery-aggregation") {
      p(
        """Returning subqueries change the number of results of the query: The result of the `CALL` clause is the combined result of evaluating the subquery
          |for each input row.""".stripMargin)
      p("""The following example finds the name of each person and the names of their friends:""")
      query("""MATCH (p:Person)
              #CALL {
              #  WITH p
              #  MATCH (p)-[:FRIEND_OF]-(c:Person)
              #  RETURN c.name AS friend
              #}
              #RETURN p.name, friend""".stripMargin('#'),
      ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "friend" -> "Bob"),
          Map("p.name" -> "Bob", "friend" -> "Alice"),
        )))) { resultTable() }
      p(
        """The number of results of the subquery changed the number of results of the enclosing query: Instead of 4 rows, one for each node), there are
          |now 2 rows which were found for Alice and Bob respectively. No rows are returned for Charlie and Dora since they have no friends in our example
          |graph.""".stripMargin)
      p(
        """We can also use subqueries to perform isolated aggregations. In this example we count the number of relationships each person has. As we get one row
          | from each evaluation of the subquery, the number of rows is the same, before and after the `CALL` clause:""".stripMargin)
      query("""MATCH (p:Person)
              #CALL {
              #  WITH p
              #  MATCH (p)--(c)
              #  RETURN count(c) AS numberOfConnections
              #}
              #RETURN p.name, numberOfConnections""".stripMargin('#'),
        ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "numberOfConnections" -> 2),
          Map("p.name" -> "Bob", "numberOfConnections" -> 1),
          Map("p.name" -> "Charlie", "numberOfConnections" -> 1),
          Map("p.name" -> "Dora", "numberOfConnections" -> 0)
        )))) { resultTable() }
    }

    section("Unit subqueries and side-effects", "subquery-unit") {
      p("""Unit subqueries do not return any rows and are therefore used for their side effects.""")

      p(
        """This example query creates five clones of each existing person. As the subquery is a unit subquery, it does not change the number of rows of the
          |enclosing query.""".stripMargin)
      query("""MATCH (p:Person)
              #CALL {
              #  WITH p
              #  UNWIND range (1, 5) AS i
              #  CREATE (:Person {name: p.name})
              #}
              #RETURN count(*)""".stripMargin('#'),
        ResultAssertions(r => r.toSet should equal(Set(
          Map("count(*)" -> 4),
        )))) { resultTable() }
    }

    section("Aggregation on imported variables", "subquery-correlated-aggregation") {
      p("""Aggregations in subqueries are scoped to the subquery evaluation, also for imported variables.
          #The following example counts the number of younger persons for each person in the graph:""".stripMargin('#'))
      query("""MATCH (p:Person)
              #CALL {
              #  WITH p
              #  MATCH (other:Person)
              #  WHERE other.age < p.age
              #  RETURN count(other) AS youngerPersonsCount
              #}
              #RETURN p.name, youngerPersonsCount""".stripMargin('#'),
      ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "youngerPersonsCount" -> 0),
          Map("p.name" -> "Bob", "youngerPersonsCount" -> 1),
          Map("p.name" -> "Charlie", "youngerPersonsCount" -> 3),
          Map("p.name" -> "Dora", "youngerPersonsCount" -> 2)
        )))) { resultTable() }
  }
  }.build()
}

