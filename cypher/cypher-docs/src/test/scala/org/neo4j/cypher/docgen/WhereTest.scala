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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class WhereTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("WHERE", "query-where")
    initQueries(
      """CREATE (andy:Swedish {name: 'Andy', age: 36, belt: 'white'}),
        |       (timothy {name: 'Timothy', age: 25, address: 'Sweden/Malmo'}),
        |       (peter {name: 'Peter', age: 35, email: 'peter_n@example.com'}),
        |
        |       (andy)-[:KNOWS {since: 2012}]->(timothy),
        |       (andy)-[:KNOWS {since: 1999}]->(peter)
      """.stripMargin)
    synopsis("`WHERE` adds constraints to the patterns in a `MATCH` or `OPTIONAL MATCH` clause or filters the results of a `WITH` clause.")
    p(
      """
        |* <<where-introduction, Introduction>>
        |* <<query-where-basic, Basic usage>>
        | ** <<boolean-operations, Boolean operations>>
        | ** <<filter-on-node-label, Filter on node label>>
        | ** <<filter-on-node-property, Filter on node property>>
        | ** <<filter-on-relationship-property, Filter on relationship property>>
        | ** <<filter-on-dynamic-property, Filter on dynamically-computed property>>
        | ** <<property-existence-checking, Property existence checking>>
        |* <<query-where-string, String matching>>
        | ** <<match-string-start, Prefix string search using `STARTS WITH`>>
        | ** <<match-string-end, Suffix string search using `ENDS WITH`>>
        | ** <<match-string-contains, Substring search using `CONTAINS`>>
        | ** <<match-string-negation, String matching negation>>
        |* <<query-where-regex, Regular expressions>>
        | ** <<matching-using-regular-expressions, Matching using regular expressions>>
        | ** <<escaping-in-regular-expressions, Escaping in regular expressions>>
        | ** <<case-insensitive-regular-expressions, Case-insensitive regular expressions>>
        |* <<query-where-patterns, Using path patterns in `WHERE`>>
        | ** <<filter-on-patterns, Filter on patterns>>
        | ** <<filter-on-patterns-using-not, Filter on patterns using `NOT`>>
        | ** <<filter-on-patterns-with-properties, Filter on patterns with properties>>
        | ** <<filter-on-relationship-type, Filter on relationship type>>
        | * <<existential-subqueries, Using existential subqueries in `WHERE`>>
        | ** <<existential-subquery-simple-cas, Simple existential subquery>>
        | ** <<existential-subquery-with-with, Existential subquery with WITH clause>>
        | ** <<existential-subquery-nesting, Nesting existential subqueries>>
        |* <<query-where-lists, Lists>>
        | ** <<where-in-operator, `IN` operator>>
        |* <<missing-properties-and-values, Missing properties and values>>
        | ** <<default-to-false-missing-property, Default to `false` if property is missing>>
        | ** <<default-to-true-missing-property, Default to `true` if property is missing>>
        | ** <<filter-on-null, Filter on `null`>>
        |* <<query-where-ranges, Using ranges>>
        | ** <<simple-range, Simple range>>
        | ** <<composite-range, Composite range>>
      """.stripMargin)
    section("Introduction", "where-introduction") {
      p("`WHERE` is not a clause in its own right -- rather, it's part of `MATCH`, `OPTIONAL MATCH` and `WITH`.")
      p("In the case of `WITH`, `WHERE` simply filters the results.")
      p(
        """For `MATCH` and `OPTIONAL MATCH` on the other hand, `WHERE` adds constraints to the patterns described.
          |_It should not be seen as a filter after the matching is finished._""".stripMargin)
      important {
        p(
          """In the case of multiple `MATCH` / `OPTIONAL MATCH` clauses, the predicate in `WHERE` is always a part of the patterns in the directly preceding `MATCH` / `OPTIONAL MATCH`.
            |Both results and performance may be impacted if the `WHERE` is put inside the wrong `MATCH` clause.""".stripMargin)
      }
      note {
        p(
          """<<administration-indexes, Indexes>>` may be used to optimize queries using `WHERE` in a variety of cases.""".stripMargin)
      }
      p("The following graph is used for the examples below:")
      graphViz()
    }
    section("Basic usage", "query-where-basic") {
      section("Boolean operations", "boolean-operations") {
        p(
          """You can use the boolean operators `AND`, `OR`, `XOR` and `NOT`.
            |See <<cypher-working-with-null>> for more information on how this works with `null`.""".stripMargin)
        query(
          """MATCH (n)
            |WHERE n.name = 'Peter' XOR (n.age < 30 AND n.name = 'Timothy') OR NOT (n.name = 'Timothy' OR n.name = 'Peter')
            |RETURN n.name, n.age""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l), Map("n.name" -> "Timothy", "n.age" -> 25l), Map("n.name" -> "Peter", "n.age" -> 35l)))
          })) {
          resultTable()
        }
      }
      section("Filter on node label", "filter-on-node-label") {
        p("To filter nodes by label, write a label predicate after the `WHERE` keyword using `WHERE n:foo`.")
        query("MATCH (n)\nWHERE n:Swedish\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l)))
        })) {
          p("The name and age for the *'Andy'* node will be returned.")
          resultTable()
        }
      }
      section("Filter on node property", "filter-on-node-property") {
        p("To filter on a node property, write your clause after the `WHERE` keyword.")
        query("MATCH (n)\nWHERE n.age < 30\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Timothy", "n.age" -> 25l)))
        })) {
          p("The name and age values for the *'Timothy'* node are returned because he is less than 30 years of age.")
          resultTable()
        }
      }
      section("Filter on relationship property", "filter-on-relationship-property") {
        p("To filter on a relationship property, write your clause after the `WHERE` keyword.")
        query("MATCH (n)-[k:KNOWS]->(f)\nWHERE k.since < 2000\nRETURN f.name, f.age, f.email", ResultAssertions((r) => {
          r.toList should equal(List(Map("f.name" -> "Peter", "f.age" -> 35l, "f.email" -> "peter_n@example.com")))
        })) {
          p("The name, age and email values for the *'Peter'* node are returned because Andy has known him since before 2000.")
          resultTable()
        }
      }
      section("Filter on dynamically-computed node property", "filter-on-dynamic-property") {
        p("To filter on a property using a dynamically computed name, use square bracket syntax.")
        query("WITH 'AGE' as propname\nMATCH (n)\nWHERE n[toLower(propname)] < 30\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Timothy", "n.age" -> 25l)))
        })) {
          p("The name and age values for the *'Timothy'* node are returned because he is less than 30 years of age.")
          resultTable()
        }
      }
      section("Property existence checking", "property-existence-checking") {
        p("Use the `exists()` function to only include nodes or relationships in which a property exists.")
        query("MATCH (n)\nWHERE exists(n.belt)\nRETURN n.name, n.belt", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andy", "n.belt" -> "white")))
        })) {
          p("The name and belt for the *'Andy'* node are returned because he is the only one with a `belt` property.")
          important {
            p("The `has()` function has been superseded by `exists()` and has been removed.")
          }
          resultTable()
        }
      }
    }
    section("String matching", "query-where-string") {
      p(
        """The prefix and suffix of a string can be matched using `STARTS WITH` and `ENDS WITH`.
          |To undertake a substring search - i.e. match regardless of location within a string - use `CONTAINS`.
          |The matching is _case-sensitive_.
          |Attempting to use these operators on values which are not strings will return `null`.""".stripMargin)
      section("Prefix string search using `STARTS WITH`", "match-string-start") {
        p("The `STARTS WITH` operator is used to perform case-sensitive matching on the beginning of a string.")
        query("MATCH (n)\nWHERE n.name STARTS WITH 'Pet'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "n.age" -> 35l)))
        })) {
          p("The name and age for the *'Peter'* node are returned because his name starts with *'Pet'*.")
          resultTable()
        }
      }
      section("Suffix string search using `ENDS WITH`", "match-string-end") {
        p("The `ENDS WITH` operator is used to perform case-sensitive matching on the ending of a string.")
        query("MATCH (n)\nWHERE n.name ENDS WITH 'ter'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "n.age" -> 35l)))
        })) {
          p("The name and age for the *'Peter'* node are returned because his name ends with *'ter'*.")
          resultTable()
        }
      }
      section("Substring search using `CONTAINS`", "match-string-contains") {
        p("The `CONTAINS` operator is used to perform case-sensitive matching regardless of location within a string.")
        query("MATCH (n)\nWHERE n.name CONTAINS 'ete'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "n.age" -> 35l)))
        })) {
          p("The name and age for the *'Peter'* node are returned because his name contains with *'ete'*.")
          resultTable()
        }
      }
      section("String matching negation", "match-string-negation") {
        p("Use the `NOT` keyword to exclude all matches on given string from your result:")
        query("MATCH (n)\nWHERE NOT n.name ENDS WITH 'y'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Peter", "n.age" -> 35l)))
        })) {
          p("The name and age for the *'Peter'* node are returned because his name does not end with *'y'*.")
          resultTable()
        }
      }
    }
    section("Regular expressions", "query-where-regex") {
      p(
        """Cypher supports filtering using regular expressions.
          |The regular expression syntax is inherited from https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html[the Java regular expressions].
          |This includes support for flags that change how strings are matched, including case-insensitive `(?i)`, multiline `(?m)` and dotall `(?s)`.
          |Flags are given at the beginning of the regular expression, for example `MATCH (n) WHERE n.name =~ '(?i)Lon.*' RETURN n` will return nodes with name 'London' or with name 'LonDoN'.""".stripMargin)
      section("Matching using regular expressions", "matching-using-regular-expressions") {
        p("You can match on regular expressions by using `=~ 'regexp'`, like this:")
        query("MATCH (n)\nWHERE n.name =~ 'Tim.*'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Timothy", "n.age" -> 25l)))
        })) {
          p("The name and age for the *'Timothy'* node are returned because his name starts with *'Tim'*.")
          resultTable()
        }
      }
      section("Escaping in regular expressions", "escaping-in-regular-expressions") {
        p(
          """Characters like `.` or `*` have special meaning in a regular expression.
            |To use these as ordinary characters, without special meaning, escape them.""".stripMargin)
        query(
          """MATCH (n)
            |WHERE n.email =~ '.*\\\\.com'
            |RETURN n.name, n.age, n.email""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("n.name" -> "Peter", "n.age" -> 35l, "n.email" -> "peter_n@example.com")))
          })) {
          p("The name, age and email for the *'Peter'* node are returned because his email ends with *'.com'*.")
          resultTable()
        }
      }
      section("Case-insensitive regular expressions", "case-insensitive-regular-expressions") {
        p("By pre-pending a regular expression with `(?i)`, the whole expression becomes case-insensitive.")
        query("MATCH (n)\nWHERE n.name =~ '(?i)AND.*'\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l)))
        })) {
          p("The name and age for the *'Andy'* node are returned because his name starts with *'AND'* irrespective of casing.")
          resultTable()
        }
      }
    }
    section("Using path patterns in `WHERE`", "query-where-patterns") {
      section("Filter on patterns", "filter-on-patterns") {
        p(
          """Patterns are expressions in Cypher, expressions that return a list of paths.
            |List expressions are also predicates -- an empty list represents `false`, and a non-empty represents `true`.""".stripMargin)
        p(
          """So, patterns are not only expressions, they are also predicates.
            |The only limitation to your pattern is that you must be able to express it in a single path.
            |You cannot use commas between multiple paths like you do in `MATCH`.
            |You can achieve  the same effect by combining multiple patterns with `AND`.""".stripMargin)
        p(
          """Note that you cannot introduce new variables here.
            |Although it might look very similar to the `MATCH` patterns, the `WHERE` clause is all about eliminating matched subgraphs.
            |`MATCH (a)-[*]->(b)` is very different from `WHERE (a)-[*]->(b)`.
            |The first will produce a subgraph for every path it can find between `a` and `b`, whereas the latter will eliminate any matched subgraphs where `a` and `b` do not have a directed relationship chain between them.""".stripMargin)
        query(
          """MATCH (timothy {name: 'Timothy'}), (others)
            |WHERE others.name IN ['Andy', 'Peter'] AND (timothy)<--(others)
            |RETURN others.name, others.age""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("others.name" -> "Andy", "others.age" -> 36l)))
          })) {
          p("The name and age for nodes that have an outgoing relationship to the *'Timothy'* node are returned.")
          resultTable()
        }
      }
      section("Filter on patterns using `NOT`", "filter-on-patterns-using-not") {
        p("The `NOT` operator can be used to exclude a pattern.")
        query("MATCH (persons), (peter {name: 'Peter'})\nWHERE NOT (persons)-->(peter)\nRETURN persons.name, persons.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("persons.name" -> "Timothy", "persons.age" -> 25l), Map("persons.name" -> "Peter", "persons.age" -> 35l)))
        })) {
          p("Name and age values for nodes that do not have an outgoing relationship to the *'Peter'* node are returned.")
          resultTable()
        }
      }
      section("Filter on patterns with properties", "filter-on-patterns-with-properties") {
        p("You can also add properties to your patterns:")
        query("MATCH (n)\nWHERE (n)-[:KNOWS]-({name: 'Timothy'})\nRETURN n.name, n.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l)))
        })) {
          p("Finds all name and age values for nodes that have a `KNOWS` relationship to a node with the name *'Timothy'*.")
          resultTable()
        }
      }
      section("Filter on relationship type", "filter-on-relationship-type") {
        p(
          """You can put the exact relationship type in the `MATCH` pattern, but sometimes you want to be able to do more advanced filtering on the type.
            |You can use the special property `type` to compare the type with something else.
            |In this example, the query does a regular expression comparison with the name of the relationship type.""".stripMargin)
        query("MATCH (n)-[r]->()\nWHERE n.name='Andy' AND type(r) =~ 'K.*'\n RETURN type(r), r.since", ResultAssertions((r) => {
          r.toList should equal(List(Map("type(r)" -> "KNOWS", "r.since" -> 1999), Map("type(r)" -> "KNOWS", "r.since" -> 2012)))
        })) {
          p("This returns all relationships having a type whose name starts with *'K'*.")
          resultTable()
        }
      }
    }
    p("include::existential-subqueries.adoc[leveloffset=+1]")
    section("Lists", "query-where-lists") {
      section("`IN` operator", "where-in-operator") {
        p("To check if an element exists in a list, you can use the `IN` operator.")
        query("MATCH (a)\nWHERE a.name IN ['Peter', 'Timothy']\nRETURN a.name, a.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("a.name" -> "Timothy", "a.age" -> 25l), Map("a.name" -> "Peter", "a.age" -> 35l)))
        })) {
          p("This query shows how to check if a property exists in a literal list.")
          resultTable()
        }
      }
    }
    section("Missing properties and values", "missing-properties-and-values") {
      section("Default to `false` if property is missing", "default-to-false-missing-property") {
        p("As missing properties evaluate to `null`, the comparison in the example will evaluate to `false` for nodes without the `belt` property.")
        query("MATCH (n)\nWHERE n.belt = 'white'\nRETURN n.name, n.age, n.belt", ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l, "n.belt" -> "white")))
        })) {
          p("Only the name, age and belt values of nodes with white belts are returned.")
          resultTable()
        }
      }
      section("Default to `true` if property is missing", "default-to-true-missing-property") {
        p("If you want to compare a property on a node or relationship, but only if it exists, you can compare the property against both the value you are looking for and `null`, like:")
        query(
          """MATCH (n)
            |WHERE n.belt = 'white' OR n.belt IS NULL
            |RETURN n.name, n.age, n.belt
            |ORDER BY n.name""".stripMargin, ResultAssertions((r) => {
            r.toList should equal(List(Map("n.name" -> "Andy", "n.age" -> 36l, "n.belt" -> "white"), Map("n.name" -> "Peter", "n.age" -> 35l, "n.belt" -> null), Map("n.name" -> "Timothy", "n.age" -> 25l, "n.belt" -> null)))
          })) {
          p("This returns all values for all nodes, even those without the belt property.")
          resultTable()
        }
      }
      section("Filter on `null`", "filter-on-null") {
        p(
          """Sometimes you might want to test if a value or a variable is `null`.
            |This is done just like SQL does it, using `IS NULL`.
            |Also like SQL, the negative is `IS NOT NULL`, although `NOT(IS NULL x)` also works.""".stripMargin)
        query("MATCH (person)\nWHERE person.name = 'Peter' AND person.belt IS NULL\nRETURN person.name, person.age, person.belt", ResultAssertions((r) => {
          r.toList should equal(List(Map("person.name" -> "Peter", "person.age" -> 35l, "person.belt" -> null)))
        })) {
          p("The name and age values for nodes that have name *'Peter'* but no belt property are returned.")
          resultTable()
        }
      }
    }
    section("Using ranges", "query-where-ranges") {
      section("Simple range", "simple-range") {
        p("""To check for an element being inside a specific range, use the inequality operators `<`, `\<=`, `>=`, `>`.""".stripMargin)
        query("MATCH (a)\nWHERE a.name >= 'Peter'\nRETURN a.name, a.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("a.name" -> "Timothy", "a.age" -> 25l), Map("a.name" -> "Peter", "a.age" -> 35l)))
        })) {
          p("The name and age values of nodes having a name property lexicographically greater than or equal to *'Peter'* are returned.")
          resultTable()
        }
      }
      section("Composite range", "composite-range") {
        p("Several inequalities can be used to construct a range.")
        query("MATCH (a)\nWHERE a.name > 'Andy' AND a.name < 'Timothy'\nRETURN a.name, a.age", ResultAssertions((r) => {
          r.toList should equal(List(Map("a.name" -> "Peter", "a.age" -> 35l)))
        })) {
          p("The name and age values of nodes having a name property lexicographically between *'Andy'* and *'Timothy'* are returned.")
          resultTable()
        }
      }
    }
  }.build()
}
