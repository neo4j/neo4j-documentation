/*
 * Copyright (c) 2002-2019 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
import org.neo4j.graphdb.{Node, Path, Relationship}

class ReturnTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql"

  override def doc = new DocBuilder {
    doc("RETURN", "query-return")
    initQueries(
      """
        |CREATE (a {name: 'A', happy: 'Yes!', age: 55}),
        |       (b {name: 'B'}),
        |
        |       (a)-[:KNOWS]->(b),
        |       (a)-[:BLOCKS]->(b)
      """.stripMargin)
    synopsis("The `RETURN` clause defines what to include in the query result set.")
    p(
      """* <<return-introduction, Introduction>>
        |* <<return-nodes, Return nodes>>
        |* <<return-relationships, Return relationships>>
        |* <<return-property, Return property>>
        |* <<return-all-elements, Return all elements>>
        |* <<return-variable-with-uncommon-characters, Variable with uncommon characters>>
        |* <<return-column-alias, Column alias>>
        |* <<return-optional-properties, Optional properties>>
        |* <<return-other-expressions, Other expressions>>
        |* <<return-unique-results, Unique results>>
      """.stripMargin)
    section("Introduction", "return-introduction") {
      p(
        """In the `RETURN` part of your query, you define which parts of the pattern you are interested in.
          |It can be nodes, relationships, or properties on these.""".stripMargin)
      tip {
        p(
          """If what you actually want is the value of a property, make sure to not return the full node/relationship.
            |This will improve performance.""".stripMargin)
      }
      graphViz()
    }
    section("Return nodes", "return-nodes") {
      p(
        """To return a node, list it in the `RETURN` statement.""".stripMargin)
      query(
        """MATCH (n {name: 'B'})
          |RETURN n""".stripMargin, assertHasNodes(1)) {
        p("The example will return the node.")
        resultTable()
      }
    }
    section("Return relationships", "return-relationships") {
      p(
        """To return a relationship, just include it in the `RETURN` list.""".stripMargin)
      query(
        """MATCH (n {name: 'A'})-[r:KNOWS]->(c)
          |RETURN r""".stripMargin, assertHasRelationships(0)) {
        p("The relationship is returned by the example.")
        resultTable()
      }
    }
    section("Return property", "return-property") {
      p(
        """To return a property, use the dot separator, like this:""".stripMargin)
      query(
        """MATCH (n {name: 'A'})
          |RETURN n.name""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.name" -> "A")))
        })) {
        p("The value of the property `name` gets returned.")
        resultTable()
      }
    }
    section("Return all elements", "return-all-elements") {
      p(
        """When you want to return all nodes, relationships and paths found in a query, you can use the `*` symbol.""".stripMargin)
      query(
        """MATCH p = (a {name: 'A'})-[r]->(b)
          |RETURN *""".stripMargin, ResultAssertions((r) => {
          r.toList.head.keys should equal(Set("a", "b", "r", "p"))
        })) {
        p("This returns the two nodes, the relationship and the path used in the query.")
        resultTable()
      }
    }
    section("Variable with uncommon characters", "return-variable-with-uncommon-characters") {
      p(
        """To introduce a placeholder that is made up of characters that are not contained in the English alphabet, you can use the ``` to enclose the variable, like this:""".stripMargin)
      query(
        """MATCH (`This isn\'t a common variable`)
         WHERE `This isn\'t a common variable`.name = 'A'
         RETURN `This isn\'t a common variable`.happy""", ResultAssertions((r) => {
          r.toList should equal(List(Map("`This isn\\'t a common variable`.happy" -> "Yes!")))
        })) {
        p("The node with name \"A\" is returned.")
        resultTable()
      }
    }
    section("Column alias", "return-column-alias") {
      p(
        """If the name of the column should be different from the expression used, you can rename it by using `AS` <new name>.""".stripMargin)
      query(
        """MATCH (a {name: 'A'})
          |RETURN a.age AS SomethingTotallyDifferent""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("SomethingTotallyDifferent" -> 55l)))
        })) {
        p("Returns the age property of a node, but renames the column.")
        resultTable()
      }
    }
    section("Optional properties", "return-optional-properties") {
      p(
        """If a property might or might not be there, you can still select it as usual.
          |It will be treated as `null` if it is missing.""".stripMargin)
      query(
        """MATCH (n)
          |RETURN n.age""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("n.age" -> 55l), Map("n.age" -> null)))
        })) {
        p("This example returns the age when the node has that property, or `null` if the property is not there.")
        resultTable()
      }
    }
    section("Other expressions", "return-other-expressions") {
      p(
        """Any expression can be used as a return item -- literals, predicates, properties, functions, and everything else.""".stripMargin)
      query(
        """MATCH (a {name: 'A'})
          |RETURN a.age > 30, "I'm a literal", (a)-->()""".stripMargin, ResultAssertions(r => {
          r.toList.head("a.age > 30") shouldBe true
          r.toList.head("\"I'm a literal\"") shouldBe "I'm a literal"
          r.toList.head("(a)-->()").asInstanceOf[Seq[Path]].size shouldBe 2
        })) {
        p("Returns a predicate, a literal and function call with a pattern expression parameter.")
        resultTable()
      }
    }
    section("Unique results", "return-unique-results") {
      p(
        """`DISTINCT` retrieves only unique rows depending on the columns that have been selected to output.""".stripMargin)
      query(
        """MATCH (a {name: 'A'})-->(b)
          |RETURN DISTINCT b""".stripMargin, assertHasNodes(1)) {
        p("The node named \"B\" is returned by the query, but only once.")
        resultTable()
      }
    }
  }.build()

  private def assertHasNodes(nodeIds: Long*) = ResultAssertions(result => result.toList.map(_.head._2.asInstanceOf[Node].getId) should equal(nodeIds.toList))

  private def assertHasRelationships(relIds: Long*) = ResultAssertions(result => result.toList.map(_.head._2.asInstanceOf[Relationship].getId) should equal(relIds.toList))
}
