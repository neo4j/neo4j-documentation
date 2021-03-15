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

class PredicateFunctionsTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Predicate functions", "query-functions-predicate")
    initQueries("""CREATE
                  #  (alice {name:'Alice', age: 38, eyes: 'brown'}),
                  #  (bob {name: 'Bob', age: 25, eyes: 'blue'}),
                  #  (charlie {name: 'Charlie', age: 53, eyes: 'green'}),
                  #  (daniel {name: 'Daniel', age: 54, eyes: 'brown', liked_colors: []}),
                  #  (eskil {name: 'Eskil', age: 41, eyes: 'blue', liked_colors: ['pink', 'yellow', 'black']}),
                  #  (frank {alias: 'Frank', age: 61, eyes: '', liked_colors: ['blue', 'green']}),
                  #  (alice)-[:KNOWS]->(bob),
                  #  (grace:Person),
                  #  (alice)-[:KNOWS]->(charlie),
                  #  (bob)-[:KNOWS]->(daniel),
                  #  (charlie)-[:KNOWS]->(daniel),
                  #  (bob)-[:MARRIED]->(eskil)""".stripMargin('#'))
    synopsis("""Predicates are boolean functions that return `true` or `false` for a given set of non-`null` input.
               #They are most commonly used to filter out paths in the `WHERE` part of a query.""".stripMargin('#'))
    p("""Functions:
        #
        #* <<functions-all,all()>>
        #* <<functions-any,any()>>
        #* <<functions-exists,exists()>>
        #* <<functions-isempty,isEmpty()>>
        #* <<functions-none,none()>>
        #* <<functions-single,single()>>""".stripMargin('#'))
    graphViz()
    section("all()", "functions-all") {
      p("""The function `all()` returns `true` if the predicate holds for all elements in the given list.
          #`null` is returned if the list is `null` or all of its elements are `null`.""".stripMargin('#'))
      function("all(variable IN list WHERE predicate)",
        "A Boolean.",
        ("list",
        """An expression that returns a list.
          #A single element cannot be explicitly passed as a literal in the cypher statement.
          #However, an implicit conversion will happen for single elements when passing node properties during cypher execution.""".stripMargin('#')),
        ("variable", "A variable that can be used from within the predicate."),
        ("predicate", "A predicate that is tested against all items in the list."))
      query("""MATCH p = (a)-[*1..3]->(b)
              #WHERE
              #  a.name = 'Alice'
              #  AND b.name = 'Daniel'
              #  AND all(x IN nodes(p) WHERE x.age > 30)
              #RETURN p""".stripMargin('#'),
      ResultAssertions(r => {
          r.toList.length should equal(1)
        })) {
        p("All nodes in the returned paths will have a property `age` with a value larger than `30`.")
        resultTable()
      }
    }
    section("any()", "functions-any") {
      p("""The function `any()` returns `true` if the predicate holds for at least one element in the given list.
          #`null` is returned if the list is `null` or all of its elements are `null`.""".stripMargin('#'))
      function("any(variable IN list WHERE predicate)",
        "A Boolean.",
        ("list",
        """An expression that returns a list.
          #A single element cannot be explicitly passed as a literal in the cypher statement.
          #However, an implicit conversion will happen for single elements when passing node properties during cypher execution.""".stripMargin('#')),
        ("variable", "A variable that can be used from within the predicate."),
        ("predicate", "A predicate that is tested against all items in the list."))
      query("""MATCH (n)
              #WHERE any(color IN n.liked_colors WHERE color = 'yellow')
              #RETURN n""".stripMargin('#'),
      ResultAssertions(r => {
          r.toList.length should equal(1)
          //r.columnAs[String]("n.name").toList.head should equal("Eskil")
        })) {
        p("The query returns nodes with the property `liked_colors` (as a list), where at least one element has the value `'yellow'`.")
        resultTable()
      }
    }
    section("exists()", "functions-exists") {
      p("""The function `exists()` returns `true` if a match for the given pattern exists in the graph, or if the specified property exists in the node, relationship or map.
          #`null` is returned if the input argument is `null`.""".stripMargin('#'))
      function("exists(pattern-or-property)",
        "A Boolean.",
        ("pattern-or-property", "A pattern or a property (in the form 'variable.prop')."))
      query("""MATCH (n)
              #WHERE exists(n.name)
              #RETURN
              #  n.name AS name,
              #  exists((n)-[:MARRIED]->()) AS is_married""".stripMargin('#'),
      ResultAssertions(r => {
          r.toList should equal(List(
            Map("name" -> "Alice", "is_married" -> false),
            Map("name" -> "Bob", "is_married" -> true),
            Map("name" -> "Charlie", "is_married" -> false),
            Map("name" -> "Daniel", "is_married" -> false),
            Map("name" -> "Eskil", "is_married" -> false)))
        })) {
        p("The names of all nodes with the `name` property are returned, along with a boolean (`true` or `false`) indicating if they are married.")
        resultTable()
      }
      query("""MATCH
              #  (a),
              #  (b)
              #WHERE
              #  exists(a.name)
              #  AND NOT exists(b.name)
              #OPTIONAL MATCH (c:DoesNotExist)
              #RETURN
              #  a.name AS a_name,
              #  b.name AS b_name,
              #  exists(b.name) AS b_has_name,
              #  c.name AS c_name,
              #  exists(c.name) AS c_has_name
              #ORDER BY a_name, b_name, c_name
              #LIMIT 1""".stripMargin('#'),
      /*Result:
      +-----------------------------------------------------+
      | a_name  | b_name | b_has_name | c_name | c_has_name |
      +-----------------------------------------------------+
      | "Alice" | NULL   | FALSE      | NULL   | NULL       |
      +-----------------------------------------------------+
      */
      ResultAssertions(r => {
          r.toList should equal(List(
            Map("a_name" -> "Alice", "b_name" -> null, "b_has_name" -> false, "c_name" -> null, "c_has_name" -> null)))
        })) {
        p("""Three nodes are returned: one with a property `name`, one without a property `name`, and one that does not exist (e.g., is `null`).
            #This query exemplifies the behavior of `exists()` when operating on `null` nodes.""".stripMargin('#'))
        resultTable()
      }
    }
    section("isEmpty()", "functions-isempty") {
      p("The function `isEmpty()` returns `true` if the given list or map contains no elements or if the given string contains no characters.")
      function("isEmpty(list)",
        "A Boolean.",
        ("list", "An expression that returns a list."))
      query("""MATCH (n)
              #WHERE NOT isEmpty(n.liked_colors)
              #RETURN n""".stripMargin('#'),
      /*Result:
      ({name: "Eskil", eyes: "blue", age: 41, liked_colors: ["pink", "yellow", "black"]})
      ({alias: "Frank", eyes: "", age: 61, liked_colors: ["blue", "green"]})
      */ 
      ResultAssertions(r => {
          r.toList.length should equal(2)
        })) {
        p("The nodes with the property `liked_colors` being non-empty are returned.")
        resultTable()
      }
      function("isEmpty(map)",
        "A Boolean.",
        ("map", "An expression that returns a map."))
      query("""MATCH (n)
              #WHERE isEmpty(properties(n))
              #RETURN n""".stripMargin('#'),
      /*Result:
      (:Person)
      */
      ResultAssertions(r => {
          r.toList.length should equal(1)
        })) {
        p("Nodes that does not have any properties are returned.")
        resultTable()
      }
      function("isEmpty(string)",
        "A Boolean.",
        ("string", "An expression that returns a string."))
      query("""MATCH (n)
              #WHERE isEmpty(n.eyes)
              #RETURN n.age AS age""".stripMargin('#'),
      ResultAssertions(r => {
          r.toList should equal(List(Map("age" -> 61)))
        })) {
        p("The age are returned for each node that has a property `eyes` where the value evaulates to be empty (empty string).")
        resultTable()
      }
      note {
        p("""The function `isEmpty()`, like most other Cypher functions, returns `null` if `null is passed in to the function.
            #That means that a predicate `isEmpty(n.eyes)` will filter out all nodes where the `eyes` property is not set.
            #Thus, `isEmpty()` is not suited to test for null values.
            #`IS NULL` or `IS NOT NULL` should be used for that purpose.""".stripMargin('#'))
      }
    }
    section("none()", "functions-none") {
      p("""The function `none()` returns `true` if the predicate does _not_ hold for any element in the given list.
          #`null` is returned if the list is `null` or all of its elements are `null`.""".stripMargin('#'))
      function("none(variable IN list WHERE predicate)",
        "A Boolean.",
        ("list",
        """An expression that returns a list.
          #A single element cannot be explicitly passed as a literal in the cypher statement.
          #However, an implicit conversion will happen for single elements when passing node properties during cypher execution.""".stripMargin('#')),
        ("variable", "A variable that can be used from within the predicate."),
        ("predicate", "A predicate that is tested against all items in the list."))
      query("""MATCH p = (n)-[*1..3]->(b)
              #WHERE
              #  n.name = 'Alice'
              #  AND none(x IN nodes(p) WHERE x.age = 25)
              #RETURN p""".stripMargin('#'),
      /*Result:
      ({name: "Alice", eyes: "brown", age: 38})-[:KNOWS]->({name: "Charlie", eyes: "green", age: 53})
      ({name: "Alice", eyes: "brown", age: 38})-[:KNOWS]->({name: "Charlie", eyes: "green", age: 53})-[:KNOWS]->({name: "Daniel", eyes: "brown", age: 54, liked_colors: []})
      */
      ResultAssertions(r => {
          r.toList.length should equal(2)
        })) {
        p("No node in the returned paths has a property `age` with the value `25`.")
        resultTable()
      }
    }
    section("single()", "functions-single") {
      p("""The function `single()` returns `true` if the predicate holds for exactly _one_ of the elements in the given list.
          #`null` is returned if the list is `null` or all of its elements are `null`.""".stripMargin('#'))
      function("single(variable IN list WHERE predicate)",
        "A Boolean.",
        ("list", "An expression that returns a list."),
        ("variable", "A variable that can be used from within the predicate."),
        ("predicate", "A predicate that is tested against all items in the list."))
      query("""MATCH p = (n)-->(b)
              #WHERE
              #  n.name = 'Alice'
              #  AND single(var IN nodes(p) WHERE var.eyes = 'blue')
              #RETURN p""".stripMargin('#'),
      /*Result:
      ({name: "Alice", eyes: "brown", age: 38})-[:KNOWS]->({name: "Bob", eyes: "blue", age: 25})
      */
      ResultAssertions(r => {
          r.toList.length should equal(1)
        })) {
        p("In every returned path there is exactly one node that has a property `eyes` with the value `'blue'`.")
        resultTable()
      }
    }
  }.build()
}
