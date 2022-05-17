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

import org.neo4j.cypher.docgen.tooling.DocBuilder
import org.neo4j.cypher.docgen.tooling.DocumentingTest
import org.neo4j.cypher.docgen.tooling.ResultAssertions

class CountTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("COUNT", "query-count")
    initQueries("""CREATE (andy:Swedish:Person {name: 'Andy', age: 36, belt: 'white'}),
                  #(timothy:Person {name: 'Timothy', age: 25, address: 'Sweden/Malmo'}),
                  #(peter:Person {name: 'Peter', age: 35, email: 'peter_n@example.com'}),
                  #(andy)-[:KNOWS {since: 2012}]->(timothy),
                  #(andy)-[:KNOWS {since: 1999}]->(peter),
                  #(andy)-[:HAS_DOG {since: 2016}]->(:Dog {name:'Andy'}),
                  #(fido:Dog {name:'Fido'})<-[:HAS_DOG {since: 2010}]-(peter)-[:HAS_DOG {since: 2018}]->(:Dog {name:'Ozzy'}),
                  #(fido)-[:HAS_TOY]->(:Toy{name:'Banana'})""".stripMargin('#'))
    synopsis("An count subquery `COUNT` can be used to find out how many times a specified pattern appears in the data.")
    p(
      """
        |* <<count-subqueries, Using count subqueries in WHERE>>
        |** <<count-subquery-simple-case, Simple count subquery>>
        |** <<count-subquery-with-where, Count subquery with `WHERE` clause>>
        |** <<count-subquery-nesting, Nesting count subqueries>>
        |* <<count-subqueries-other-clauses, Using count subqueries inside other clauses>>
        |** <<count-subqueries-with-return, Using count subqueries with `RETURN` clause>>
        |** <<count-subqueries-with-set, Using count subqueries with `SET` clause>>
        |** <<count-subqueries-with-case, Using count subqueries with `CASE` expression>>
        |** <<count-subqueries-with-with, Using count subqueries with `WITH` expression>>
      """.stripMargin)
    p("""#It can be used in the same way as a path pattern but it allows you to use `WHERE` clauses internally.
        #A subquery has a scope, as indicated by the opening and closing braces, `{` and `}`.
        #Any variable that is defined in the outside scope can be referenced inside the subquery's own scope.
        #Variables introduced inside the subquery are not part of the outside scope and therefore can't be accessed on the outside.""".stripMargin('#'))
    functionWithCypherStyleFormatting("""COUNT {
                                        #  [Pattern]
                                        #  WHERE [Expression]
                                        #}""".stripMargin('#'))
    p("The `WHERE` clause is optional.")
    p("It is worth noting that the `MATCH` keyword can be omitted in subqueries and that the `WHERE` clause is optional.")
    section("Using count subqueries in `WHERE`", "count-subqueries") {
      section("Simple count subquery", "count-subquery-simple-case") {
        p("""Variables introduced by the outside scope can be used in the inner `MATCH` clause. The following query exemplifies this and would output the owners of more tahan one dog:""")
        query("""MATCH (person:Person)
                #WHERE COUNT { (person)-[:HAS_DOG]->(:Dog) } > 1
                #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Peter")))
          })) {
          resultTable()
        }
      }
      section("Count subquery with `WHERE` clause", "count-subquery-with-where") {
        p("""A `WHERE` clause can be used inside the `COUNT` pattern.
            #Variables introduced by the `MATCH` clause and the outside scope can be used in this scope.""".stripMargin('#'))
        query("""MATCH (person:Person)
                #WHERE COUNT {
                #  (person)-[:HAS_DOG]->(dog:Dog)
                #  WHERE person.name = dog.name
                #} > 0
                #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy")))
          })) {
          resultTable()
        }
      }
      section("Nesting count subqueries", "count-subquery-nesting") {
        p("""Count subqueries can be nested like the following example shows.
            #The nesting also affects the scopes.
            #That means that it is possible to access all variables from inside the subquery which are either on the outside scope or defined in the very same subquery.""".stripMargin('#'))
        query("""MATCH (person:Person)
                #WHERE COUNT {
                #  (person)-[:HAS_DOG]->(dog:Dog)
                #  WHERE COUNT {
                #    (dog)-[:HAS_TOY]->(toy:Toy)
                #    WHERE toy.name = 'Banana'
                #  } > 0
                #} > 0
                #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Peter")))
          })) {
          resultTable()
        }
      }
    }
    section("Using count subqueries inside other clauses", "count-subqueries-other-clauses") {
      p("COUNT can be used in any position in a query, with the exception of administration commands, where it is restricted. We provide a few examples below.")
      section("Using count in RETURN", "count-subqueries-with-return") {
        query(
          """MATCH (person:Person)
            #RETURN person.name, COUNT { (person)-[:HAS_DOG]->(:Dog) } as howManyDogs
          """.stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("howManyDogs" -> 1, "person.name" -> "Andy"), Map("howManyDogs" -> 0, "person.name" -> "Timothy"), Map("howManyDogs" -> 2, "person.name" -> "Peter")))
          })) {
          resultTable()
        }
      }
      section("Using count in SET", "count-subqueries-with-set") {
        query(
          """MATCH (person:Person) WHERE person.name ="Andy"
            #SET person.howManyDogs = COUNT { (person)-[:HAS_DOG]->(:Dog) } + 1
            #RETURN person.howManyDogs as howManyDogs
          """.stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("howManyDogs" -> 2)))
          })) {
          resultTable()
        }
      }
    }
    section("Using count in CASE", "count-subqueries-with-case") {
      query(
        """MATCH (person:Person)
          #RETURN
          #   CASE
          #     WHEN COUNT { (person)-[:HAS_DOG]->(:Dog) } > 1 THEN person.name
          #     ELSE "noDogsHere"
          #   END AS result
          """.stripMargin('#'),
        ResultAssertions(r => {
          r.toList should equal(List(Map("result" -> "noDogsHere"), Map("result" -> "noDogsHere"), Map("result" -> "Peter")))
        })) {
        resultTable()
      }
    }
    section("Using count in WITH", "count-subqueries-with-with") {
      query(
        """MATCH (person:Person)
          #WITH COUNT { (person)-[:HAS_DOG]->(:Dog) } AS numDogs, person.name as name
          #RETURN name, numDogs
          """.stripMargin('#'),
        ResultAssertions(r => {
          r.toList should equal(List(Map("name" -> "Andy", "numDogs" -> 1), Map("name" -> "Timothy", "numDogs" -> 0), Map("name" -> "Peter", "numDogs" -> 2)))
        })) {
        resultTable()
      }
    }
  }.build()
}
