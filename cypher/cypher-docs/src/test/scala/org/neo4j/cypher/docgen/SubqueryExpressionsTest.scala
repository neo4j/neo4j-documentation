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

class SubqueryExpressionsTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Subquery expressions", "cypher-subquery-expressions")

    initQueries(
      """CREATE
        #(andy:Swedish:Person {name: 'Andy', age: 36}),
        #(timothy:Person {name: 'Timothy', age: 25}),
        #(peter:Person {name: 'Peter', age: 35}),
        #(andy)-[:HAS_DOG {since: 2016}]->(:Dog {name:'Andy'}),
        #(fido:Dog {name:'Fido'})<-[:HAS_DOG {since: 2010}]-(peter)-[:HAS_DOG {since: 2018}]->(:Dog {name:'Ozzy'}),
        #(fido)-[:HAS_TOY]->(:Toy{name:'Banana'})""".stripMargin('#')
    )
    synopsis("Cypher has expressions that evaluate a subquery and aggregate the result in different fashions.")

    p(
      """* <<existential-subqueries,`EXISTS` subqueries>>
        # ** <<existential-subquery-simple-case, Simple `EXISTS` subquery>>
        # ** <<existential-subquery-with-where, `EXISTS` subquery with `WHERE` clause>>
        # ** <<existential-subquery-nesting, Nesting `EXISTS` subqueries>>
        # ** <<existential-subquery-outside-where, `EXISTS` subquery outside of a `WHERE` clause>>
        #* <<count-subqueries, `COUNT` subqueries>>
        # ** <<count-subquery-simple-case, Simple `COUNT` subquery>>
        # ** <<count-subquery-with-where, `COUNT` subquery with `WHERE` clause>>
        # ** <<count-subqueries-other-clauses, Using `COUNT` subqueries inside other clauses>>
        # """.stripMargin('#')
    )

    p(
      """Subquery expressions can appear anywhere that an expression is valid.
        #A subquery has a scope, as indicated by the opening and closing braces, `{` and `}`.
        #Any variable that is defined in the outside scope can be referenced inside the subquery's own scope.
        #Variables introduced inside the subquery are not part of the outside scope and therefore can't be accessed on the outside.
        #""".stripMargin('#')
    )
    p("The following graph is used for the examples below:")
    graphViz()

    section("`EXISTS` subqueries", "existential-subqueries") {
      p(
        """An `EXISTS` subquery can be used to find out if a specified pattern exists at least once in the data.
          #It serves the same purpose as a <<filter-on-patterns, path pattern>> but is more powerful because it allows you to use `MATCH` and `WHERE` clauses internally.
          #Moreover, it can appear in any expression position, unlike path patterns.
          #If the subquery evaluates to at least one row, the whole expression will become true.
          #This also means that the system only needs to evaluate if there is at least one row and can skip the rest of the work.""".stripMargin(
          '#'
        )
      )
      functionWithCypherStyleFormatting(
        """EXISTS {
          #  MATCH [Pattern]
          #  WHERE [Expression]
          #}""".stripMargin('#')
      )
      p(
        "It is worth noting that the `MATCH` keyword can be omitted in such subqueries and that the `WHERE` clause is optional."
      )

      section("Simple `EXISTS` subquery", "existential-subquery-simple-case") {
        p(
          """Variables introduced by the outside scope can be used in the `EXISTS` subquery without importing them,
            #unlike the case for `CALL` subqueries, <<subquery-correlated-importing,as they require importing>>.
            #The following example shows this:""".stripMargin('#')
        )
        query(
          """MATCH (person:Person)
            #WHERE EXISTS {
            #  MATCH (person)-[:HAS_DOG]->(:Dog)
            #}
            #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy"), Map("name" -> "Peter")))
          })
        ) {
          resultTable()
        }
      }
      section("`EXISTS` subquery with `WHERE` clause", "existential-subquery-with-where") {
        p(
          """A `WHERE` clause can be used in conjunction to the `MATCH`.
            #Variables introduced by the `MATCH` clause and the outside scope can be used in this scope.""".stripMargin(
            '#'
          )
        )
        query(
          """MATCH (person:Person)
            #WHERE EXISTS {
            #  MATCH (person)-[:HAS_DOG]->(dog:Dog)
            #  WHERE person.name = dog.name
            #}
            #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy")))
          })
        ) {
          resultTable()
        }
      }
      section("Nesting `EXISTS` subqueries", "existential-subquery-nesting") {
        p(
          """`EXISTS` subqueries can be nested like the following example shows.
            #The nesting also affects the scopes.
            #That means that it is possible to access all variables from inside the subquery which are either from the outside scope or defined in the very same subquery.""".stripMargin(
            '#'
          )
        )
        query(
          """MATCH (person:Person)
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
          })
        ) {
          resultTable()
        }
      }
      section("`EXISTS` subquery outside of a `WHERE` clause", "existential-subquery-outside-where") {
        p(
          """`EXISTS` subquery expressions can appear anywhere that an expression is valid.
            |Here the result is a boolean that shows whether the subquery can find the given pattern.""".stripMargin
        )
        query(
          """MATCH (person:Person)
            #RETURN person.name AS name, EXISTS {
            #  MATCH (person)-[:HAS_DOG]->(:Dog)
            #} AS hasDog""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(
              Map("name" -> "Andy", "hasDog" -> true),
              Map("name" -> "Timothy", "hasDog" -> false),
              Map("name" -> "Peter", "hasDog" -> true)
            ))
          })
        ) {
          resultTable()
        }
      }
    }

    section("`COUNT` subqueries", "count-subqueries") {
      p("A `COUNT` subquery expression can be used to count the number of results of the subquery.")
      functionWithCypherStyleFormatting(
        """COUNT {
          #  MATCH [Pattern]
          #  WHERE [Expression]
          #}""".stripMargin('#')
      )
      p(
        "It is worth noting that the `MATCH` keyword can be omitted in such subqueries and that the `WHERE` clause is optional."
      )

      section("Simple `COUNT` subquery", "count-subquery-simple-case") {
        p(
          """Variables introduced by the outside scope can be used in the `COUNT` subquery without importing them,
            #unlike the case for `CALL` subqueries, <<subquery-correlated-importing,as they require importing>>.
            #The following query exemplifies this and outputs the owners of more than one dog:
            #""".stripMargin('#')
        )
        query(
          """MATCH (person:Person)
            #WHERE COUNT { (person)-[:HAS_DOG]->(:Dog) } > 1
            #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Peter")))
          })
        ) {
          resultTable()
        }
      }
      section("`COUNT` subquery with `WHERE` clause", "count-subquery-with-where") {
        p(
          """A `WHERE` clause can be used inside the `COUNT` pattern.
            #Variables introduced by the `MATCH` clause and the outside scope can be used in this scope.""".stripMargin(
            '#'
          )
        )
        query(
          """MATCH (person:Person)
            #WHERE COUNT {
            #  (person)-[:HAS_DOG]->(dog:Dog)
            #  WHERE person.name = dog.name
            #} = 1
            #RETURN person.name AS name""".stripMargin('#'),
          ResultAssertions(r => {
            r.toList should equal(List(Map("name" -> "Andy")))
          })
        ) {
          resultTable()
        }
      }
      section("Using `COUNT` subqueries inside other clauses", "count-subqueries-other-clauses") {
        p(
          """`COUNT` can be used in any position in a query, with the exception of administration commands, where it is restricted.
            #See a few examples below:""".stripMargin('#')
        )
        section("Using `COUNT` in `RETURN`", "count-subqueries-with-return") {
          query(
            """MATCH (person:Person)
              #RETURN person.name, COUNT { (person)-[:HAS_DOG]->(:Dog) } as howManyDogs
      """.stripMargin('#'),
            ResultAssertions(r => {
              r.toList should equal(List(
                Map("howManyDogs" -> 1, "person.name" -> "Andy"),
                Map("howManyDogs" -> 0, "person.name" -> "Timothy"),
                Map("howManyDogs" -> 2, "person.name" -> "Peter")
              ))
            })
          ) {
            resultTable()
          }
        }
        section("Using `COUNT` in `SET`", "count-subqueries-with-set") {
          query(
            """MATCH (person:Person) WHERE person.name ="Andy"
              #SET person.howManyDogs = COUNT { (person)-[:HAS_DOG]->(:Dog) }
              #RETURN person.howManyDogs as howManyDogs
      """.stripMargin('#'),
            ResultAssertions(r => {
              r.toList should equal(List(Map("howManyDogs" -> 1)))
            })
          ) {
            resultTable()
          }
        }
        section("Using `COUNT` in `CASE`", "count-subqueries-with-case") {
          query(
            """MATCH (person:Person)
              #RETURN
              #   CASE
              #     WHEN COUNT { (person)-[:HAS_DOG]->(:Dog) } > 1 THEN "Doglover " + person.name
              #     ELSE person.name
              #   END AS result
              """.stripMargin('#'),
            ResultAssertions(r => {
              r.toList should equal(List(
                Map("result" -> "Andy"),
                Map("result" -> "Timothy"),
                Map("result" -> "Doglover Peter")
              ))
            })
          ) {
            resultTable()
          }
        }
        section("Using `COUNT` as a grouping key", "count-subqueries-as-grouping-key") {
          p(
            """The following query groups all persons by how many dogs they own,
              #and then calculates the average age for each group.
              #""".stripMargin('#')
          )
          query(
            """MATCH (person:Person)
              #RETURN COUNT { (person)-[:HAS_DOG]->(:Dog) } AS numDogs,
              #       avg(person.age) AS averageAge
              # ORDER BY numDogs
              """.stripMargin('#'),
            ResultAssertions(r => {
              r.toList should equal(List(
                Map("numDogs" -> 0, "averageAge" -> 25),
                Map("numDogs" -> 1, "averageAge" -> 36),
                Map("numDogs" -> 2, "averageAge" -> 35)
              ))
            })
          ) {
            resultTable()
          }
        }
      }
    }
  }.build()
}
