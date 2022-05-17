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
import org.scalatest.Inside.inside

class ExistsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("EXISTS", "query-exists")
    initQueries("""CREATE (andy:Swedish:Person {name: 'Andy', age: 36, belt: 'white'}),
                  #(timothy:Person {name: 'Timothy', age: 25, address: 'Sweden/Malmo'}),
                  #(peter:Person {name: 'Peter', age: 35, email: 'peter_n@example.com'}),
                  #(andy)-[:KNOWS {since: 2012}]->(timothy),
                  #(andy)-[:KNOWS {since: 1999}]->(peter),
                  #(andy)-[:HAS_DOG {since: 2016}]->(:Dog {name:'Andy'}),
                  #(fido:Dog {name:'Fido'})<-[:HAS_DOG {since: 2010}]-(peter)-[:HAS_DOG {since: 2018}]->(:Dog {name:'Ozzy'}),
                  #(fido)-[:HAS_TOY]->(:Toy{name:'Banana'})""".stripMargin('#'))
    synopsis("An existential subquery `EXISTS` can be used to find out if a specified pattern exists at least once in the data.")
    p(
      """
        |* <<existential-subqueries, Using existencial subqueries in WHERE>>
        |** <<existential-subquery-simple-case, Simple existential subquery>>
        |** <<existential-subquery-with-where, Existential subquery with `WHERE` clause>>
        |** <<existential-subquery-nesting, Nesting existential subqueries>>
      """.stripMargin)
    p("""#It can be used in the same way as a path pattern but it allows you to use `MATCH` and `WHERE` clauses internally.
        #A subquery has a scope, as indicated by the opening and closing braces, `{` and `}`.
        #Any variable that is defined in the outside scope can be referenced inside the subquery's own scope.
        #Variables introduced inside the subquery are not part of the outside scope and therefore can't be accessed on the outside.
        #If the subquery evaluates even once to anything that is not null, the whole expression will become true.
        #This also means that the system only needs to calculate the first occurrence where the subquery evaluates to something that is not null and can skip the rest of the work.""".stripMargin('#'))
    functionWithCypherStyleFormatting("""EXISTS {
                                        #  MATCH [Pattern]
                                        #  WHERE [Expression]
                                        #}""".stripMargin('#'))
    p("It is worth noting that the `MATCH` keyword can be omitted in subqueries and that the `WHERE` clause is optional.")
    section("Using existential subqueries in `WHERE`", "existential-subqueries") {
      section("Simple existential subquery", "existential-subquery-simple-case") {
        p("""Variables introduced by the outside scope can be used in the inner `MATCH` clause. The following example shows this:""")
        query("""MATCH (person:Person)
                            #WHERE EXISTS {
                            #  MATCH (person)-[:HAS_DOG]->(:Dog)
                            #}
                            #RETURN person.name AS name""".stripMargin('#'),
        ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy"), Map("name" -> "Peter")))
          })) {
          resultTable()
        }
      }
      section("Existential subquery with `WHERE` clause", "existential-subquery-with-where") {
        p("""A `WHERE` clause can be used in conjunction to the `MATCH`.
            #Variables introduced by the `MATCH` clause and the outside scope can be used in this scope.""".stripMargin('#'))
        query("""MATCH (person:Person)
                            #WHERE EXISTS {
                            #  MATCH (person)-[:HAS_DOG]->(dog:Dog)
                            #  WHERE person.name = dog.name
                            #}
                            #RETURN person.name AS name""".stripMargin('#'),
        ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy")))
          })) {
          resultTable()
        }
      }
      section("Nesting existential subqueries", "existential-subquery-nesting") {
        p("""Existential subqueries can be nested like the following example shows.
            #The nesting also affects the scopes.
            #That means that it is possible to access all variables from inside the subquery which are either on the outside scope or defined in the very same subquery.""".stripMargin('#'))
        query("""MATCH (person:Person)
                            #WHERE EXISTS {
                            #  MATCH (person)-[:HAS_DOG]->(dog:Dog)
                            #  WHERE EXISTS {
                            #    MATCH (dog)-[:HAS_TOY]->(toy:Toy)
                            #    WHERE toy.name = 'Banana'
                            #  }
                            #}
                            #RETURN person.name AS name""".stripMargin('#'),
        ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Peter")))
          })) {
          resultTable()
        }
      }
    }
  }.build()
}
