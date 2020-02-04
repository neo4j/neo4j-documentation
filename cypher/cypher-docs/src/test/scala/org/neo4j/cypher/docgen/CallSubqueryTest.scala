package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, ResultAssertions}

class CallSubqueryTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql"

  var nodeId:Long = 1

  override def doc: Document = new DocBuilder {
    doc("CALL {} (subquery)", "query-call-subquery")
    initQueries( """CREATE (a:Person:Child {age: 20, name: 'Alice'}), (b:Person {age: 27, name: 'Bob'}), (c:Person:Parent {age: 65, name: 'Charlie'}), (d:Person {age: 30, name: 'Dora'})
      |CREATE (a)-[:FRIEND_OF]->(b)
      |CREATE (a)-[:CHILD_OF]->(c)
    """.stripMargin)
    synopsis("The `CALL {}` clause evaluates a subquery that returns some values.")

    p(
      """* <<subquery-call-introduction, Introduction>>
        |* <<subquery-correlated-importing, Importing variables into subqueries>>
        |* <<subquery-post-union, Post-union processing>>
        |* <<subquery-aggregation, Aggregation and side-effects>>
        |* <<subquery-correlated-aggregation, Aggregation on imported variables>>
        |"""
      )

    section("Introduction", "subquery-call-introduction") {
      p( """CALL allows to execute subqueries, i.e. queries inside of other queries.
           |Subqueries allow you to compose queries, which is especially useful when working with `UNION` or aggregations.
         """.stripMargin)
      tip{
        p("""The `CALL` clause is also used for calling procedures.
            |For descriptions of the `CALL` clause in this context, refer to <<query-call>>.
          """.stripMargin)
      }
      p( """A subquery is evaluated for each incoming input row and may produce an arbitrary number of output rows.
           |Every output row is then combined with the input row to build the result of the subquery.
           |That means that a subquery will influence the number of rows.
           |If the subquery does not return any rows, there will be no rows available after the subquery.
         """.stripMargin)
      p( """There are restrictions on what queries are allowed as subqueries and how they interact with the enclosing query:""")
      p( """
           |* A subquery must end with a `RETURN` clause.
           |* A subquery can only refer to variables from the enclosing query if they are explicitly imported.
           |* A subquery cannot return variables with the same names as variables in the enclosing query.
           |* All variables that are returned from a subquery are afterwards available in the enclosing query.
         """.stripMargin)
      p("The following graph is used for the examples below:")
      graphViz()
    }

    section( "Importing variables into subqueries", "subquery-correlated-importing") {
      p("""Variables are imported into a subquery using an importing `WITH` clause.
          |As the subquery is evaluated for each incoming input row, the imported variables get bound to the corresponding values from the input row in each evaluation.
          |""".stripMargin)

      preformattedQuery("""UNWIND [0, 1, 2] AS x
                          |CALL {
                          |  WITH x
                          |  RETURN x * 10 AS y
                          |}
                          |RETURN x, y
                          |""".stripMargin,
        ResultAssertions(r => r.toSet should equal(Set(
          Map("x" -> 0, "y" -> 0),
          Map("x" -> 1, "y" -> 10),
          Map("x" -> 2, "y" -> 20),
        )))) { resultTable() }

      p("An importing `WITH` clause must:")
      p("""
          |* Consist only of simple references to outside variables - e.g. `WITH x, y, z`. Aliasing or expressions are not supported in importing `WITH` clauses - e.g. `WITH a AS b` or `WITH a+1 AS b`.
          |* Be the first clause of a subquery (or the second clause, if directly following a `USE` clause).
          |""".stripMargin)
    }

    section("Post-union processing", "subquery-post-union") {
      p( """Subqueries can be used to process the results of a `UNION` query further.
           |This example query finds the youngest and the oldest person in the database and orders them by name.""".stripMargin)
      preformattedQuery( """CALL {
                           |  MATCH (p:Person) RETURN p ORDER BY p.age ASC LIMIT 1
                           |  UNION
                           |  MATCH (p:Person) RETURN p ORDER BY p.age DESC LIMIT 1
                           |}
                           |RETURN p.name, p.age ORDER BY p.name
                         """.stripMargin,
        ResultAssertions(r => r.toList should equal(Seq(
          Map("p.name" -> "Alice", "p.age" -> 20),
          Map("p.name" -> "Charlie", "p.age" -> 65)
        )))) { resultTable() }

      p( """If different parts of a result should be matched differently, with some aggregation over the whole results, subqueries need to be used.
        |This example query finds friends and/or parents for each person.
        |Subsequently the number of friends and parents are counted together.
      """.stripMargin)
      preformattedQuery( """MATCH (p:Person)
                           | CALL {
                           |  WITH p OPTIONAL MATCH (p)-[:FRIEND_OF]->(other:Person) RETURN other
                           |  UNION
                           |  WITH p OPTIONAL MATCH (p)-[:CHILD_OF]->(other:Parent) RETURN other
                           |}
                           |RETURN DISTINCT p.name, count(other)
                         """.stripMargin,
        ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "count(other)" -> 2),
          Map("p.name" -> "Bob", "count(other)" -> 0),
          Map("p.name" -> "Charlie", "count(other)" -> 0),
          Map("p.name" -> "Dora", "count(other)" -> 0),
        )))) { resultTable() }
    }

    section("Aggregation and side-effects", "subquery-aggregation") {
      p( """Subqueries can be useful to do aggregations for each row and to isolate side-effects.
        |This example query creates five `Clone` nodes for each existing person.
        |The aggregation ensures that cardinality is not changed by the subquery.
        |Without this, the result would be five times as many rows.
      """.stripMargin)
      preformattedQuery( """MATCH (p:Person)
                           |CALL {
                           |  UNWIND range(1, 5) AS i
                           |  CREATE (c:Clone)
                           |  RETURN count(c) AS numberOfClones
                           |}
                           |RETURN p.name, numberOfClones
                           |""".stripMargin,
        ResultAssertions(r => r.toSet should equal(Set(
          Map("p.name" -> "Alice", "numberOfClones" -> 5),
          Map("p.name" -> "Bob", "numberOfClones" -> 5),
          Map("p.name" -> "Charlie", "numberOfClones" -> 5),
          Map("p.name" -> "Dora", "numberOfClones" -> 5)
        )))) { resultTable() }
    }

    section("Aggregation on imported variables", "subquery-correlated-aggregation") {
      p("Aggregations in subqueries are scoped to the subquery evaluation, also for imported variables. The following example counts the number of younger persons for each person in the graph:")
        preformattedQuery("""MATCH (p:Person)
                            |CALL {
                            |  WITH p
                            |  MATCH (other:Person) WHERE other.age < p.age
                            |  RETURN count(other) AS youngerPersonsCount
                            |}
                            |RETURN p.name, youngerPersonsCount
                            |""".stripMargin,
          ResultAssertions(r => r.toSet should equal(Set(
            Map("p.name" -> "Alice", "youngerPersonsCount" -> 0),
            Map("p.name" -> "Bob", "youngerPersonsCount" -> 1),
            Map("p.name" -> "Charlie", "youngerPersonsCount" -> 3),
            Map("p.name" -> "Dora", "youngerPersonsCount" -> 2)
          )))) { resultTable() }
  }
  }.build()
}

