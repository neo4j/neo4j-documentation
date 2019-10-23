/*
 * Copyright (c) 2002-2019 "Neo4j,"
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

class ExistentialSubQueriesTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Existential subqueries", "existential-subqueries")
    initQueries(
      """|CREATE (:Person {name:'Alice', id: 0}),
         |       (:Person {name:'Bosse', lastname: 'Bobson', id: 1})-[:HAS_DOG {since: 2016}]->(:Dog {name:'Bosse'}),
         |       (:Dog {name:'Fido'})<-[:HAS_DOG {since: 2010}]-(:Person {name:'Chris', id:2})-[:HAS_DOG {since: 2018}]->(:Dog {name:'Ozzy'})""")

    p(
      """An existential subquery can be used to find out if a specified pattern exists at least once in the data.
         |It can be used in the same way as a predicate expression but it allows you to use `MATCH` and `WHERE` clauses internally.
         |A subquery has a scope, as indicated by the opening and closing braces, `{` and `}`.
         |Any variable that is defined in the outside scope can be referenced inside the subquery's own scope.
         |Variables introduced inside the subquery are not part of the outside scope and therefore can't be accessed on the outside.
         |If the subquery evaluates even once to anything that is not null, the whole expression will become true.
         |This also means, that the system only needs to calculate the first occurrence where the subquery evaluates to something that is not null and can skip the rest of the work.
      """.stripMargin)
    functionWithCypherStyleFormatting(
      "EXISTS { \n MATCH [Pattern] \n WHERE [Expression] \n}")
    p("It is worth noting that the `MATCH` keyword can be omitted in subqueries and that the `WHERE` clause is optional.")
    p("The following graph is used for the examples below:")
    graphViz()
    section("Simple existential subquery", "existential-subquery-simple-case") {
      p(
        """Variables introduced by the outside scope can be used in the inner `MATCH` clause. The following example shows this:
        """.stripMargin)
      preformattedQuery(
        """MATCH (person:Person)
           |WHERE EXISTS {
           |  MATCH (person)-[:HAS_DOG]->(:Dog)
           |}
           |RETURN person.name as name""".stripMargin, ResultAssertions(r => {
          r.toList should equal(List(Map("name" -> "Bosse"), Map("name" -> "Chris")))
        })) {
        resultTable()
      }
    }
    section("Existential subquery with WITH clause", "existential-subquery-with-with") {
      p(
        """A `WHERE` clause can be used in conjunction to the `MATCH`. Variables introduced by the `MATCH` clause and the outside scope can be used in this scope.
          |
        """.stripMargin)
      preformattedQuery(
        """MATCH (person:Person)
           |WHERE EXISTS {
           |  MATCH (person)-[:HAS_DOG]->(dog :Dog)
           |  WHERE person.name = dog.name
           |}
           |RETURN person.name as name""".stripMargin, ResultAssertions(r => {
          r.toList should equal(List(Map("name" -> "Bosse")))
        })) {
        resultTable()
      }
    }
    section("Nesting existential subqueries", "existential-subquery-nesting") {
      p(
        """Existential subqueries can be nested like the following example shows.
           The nesting also affects the scopes.
           That means that it is possible to access all variables from inside the subquery which are either on the outside scope or defined in the very same subquery.
        """.stripMargin)
      preformattedQuery(
        """MATCH (person:Person)
           |WHERE EXISTS {
           |  MATCH (person)-[:HAS_DOG]->(dog:Dog)
           |  WHERE EXISTS {
           |    MATCH (dog)
           |    WHERE dog.name = 'Ozzy'
           |  }
           |}
           |RETURN person.name as name""".stripMargin, ResultAssertions(r => {
          r.toList should equal(List(Map("name" -> "Chris")))
        })) {
        resultTable()
      }
    }
  }.build()
}
