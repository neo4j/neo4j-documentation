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
import org.neo4j.cypher.docgen.tooling.Document
import org.neo4j.cypher.docgen.tooling.DocumentingTest
import org.neo4j.cypher.docgen.tooling.ResultAssertions

class PatternTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc: Document = new DocBuilder {
    doc("Patterns", "cypher-patterns")
    synopsis("This section contains an overview of data patterns in Cypher.")

    initQueries(
      """CREATE (a {name: 'Anders'}),
        |(b {name: 'Becky'}),
        |(c {name: 'Cesar'}),
        |(d {name: 'Dilshad'}),
        |(e {name: 'George'}),
        |(f {name: 'Filipa'}),
        |
        |(a)-[:KNOWS]->(b),
        |(a)-[:KNOWS]->(c),
        |(a)-[:KNOWS]->(d),
        |(b)-[:KNOWS]->(e),
        |(c)-[:KNOWS]->(e),
        |(d)-[:KNOWS]->(f)

      """.stripMargin
    )

    p("""* <<cypher-pattern-node-introduction,Introduction>>
        |* <<cypher-pattern-node,Patterns for nodes>>
        |* <<cypher-pattern-related-nodes,Patterns for related nodes>>
        |* <<cypher-pattern-label,Patterns for labels>>
        |* <<cypher-pattern-properties,Specifying properties>>
        |* <<cypher-pattern-relationship,Patterns for relationships>>
        |* <<cypher-pattern-varlength, Variable-length pattern matching>>
        |* <<cypher-pattern-path-variables,Assigning to path variables>>""")

    section("Introduction", "cypher-pattern-node-introduction") {
      p(
        """Patterns and pattern-matching are at the very heart of Cypher, so being effective with Cypher requires a good understanding of patterns.""".stripMargin
      )
      p(
        """Using patterns, you describe the shape of the data you are looking for.
          |For example, in the `MATCH` clause you describe the shape with a pattern, and Cypher will figure out how to get that data for you.""".stripMargin
      )
      p(
        """The pattern describes the data using a form that is very similar to how one typically draws the shape of property graph data on a whiteboard: usually as circles (representing nodes) and arrows between them to represent relationships."""
      )
      p("""Patterns appear in multiple places in Cypher: in `MATCH`, `CREATE` and `MERGE` clauses, and in pattern expressions.
     Each of these is described in more detail in:
          |
          |* <<query-match>>
          |* <<query-optional-match>>
          |* <<query-create>>
          |* <<query-merge>>
          |* <<query-where-patterns>>""")
    }

    section("Patterns for nodes", "cypher-pattern-node") {
      p("""The very simplest 'shape' that can be described in a pattern is a node.
     A node is described using a pair of parentheses, and is typically given a name.
     For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)
          |----
          |""")
      p("""This simple pattern describes a single node, and names that node using the variable `a`.""")
    }

    section("Patterns for related nodes", "cypher-pattern-related-nodes") {
      p("""A more powerful construct is a pattern that describes multiple nodes and relationships between them.
          |Cypher patterns describe relationships by employing an arrow between two nodes.
          |For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-->(b)
          |----
          |""")
      p(
        """This pattern describes a very simple data shape: two nodes, and a single relationship from one to the other.
          |In this example, the two nodes are both named as `a` and `b` respectively, and the relationship is 'directed': it goes from `a` to `b`."""
      )
      p(
        """This manner of describing nodes and relationships can be extended to cover an arbitrary number of nodes and the relationships between them, for example:"""
      )
      p("""[source, cypher, role=noplay]
          |----
          |(a)-->(b)<--(c)
          |----
          |""")
      p("Such a series of connected nodes and relationships is called a \"path\".")
      p("""Note that the naming of the nodes in these patterns is only necessary should one need to refer to the same node again, either later in the pattern or elsewhere in the Cypher query.
          |If this is not necessary, then the name may be omitted, as follows:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-->()<--(c)
          |----
          |""")
    }

    section("Patterns for labels", "cypher-pattern-label") {
      p("""In addition to simply describing the shape of a node in the pattern, one can also describe attributes.
          |The most simple attribute that can be described in the pattern is a label that the node must have.
          |For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a:User)-->(b)
          |----
          |""")
      p("""One can also describe a node that has multiple labels:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a:User:Admin)-->(b)
          |----
          |""")
    }

    section("Specifying properties", "cypher-pattern-properties") {
      p(
        """Nodes and relationships are the fundamental structures in a graph. Neo4j uses properties on both of these to allow for far richer models."""
      )
      p("""Properties can be expressed in patterns using a map-construct: curly brackets surrounding a number of key-expression pairs, separated by commas.
          |E.g. a node with two properties on it would look like:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a {name: 'Andy', sport: 'Brazilian Ju-Jitsu'})
          |----
          |""")
      p("""A relationship with expectations on it is given by:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[{blocked: false}]->(b)
          |----
          |""")
      p(
        """When properties appear in patterns, they add an additional constraint to the shape of the data.
          |In the case of a `CREATE` clause, the properties will be set in the newly-created nodes and relationships.
          |In the case of a `MERGE` clause, the properties will be used as additional constraints on the shape any existing data must have (the specified properties must exactly match any existing data in the graph).
          |If no matching data is found, then `MERGE` behaves like `CREATE` and the properties will be set in the newly created nodes and relationships."""
      )
      p(
        """Note that patterns supplied to `CREATE` may use a single parameter to specify properties, e.g: `CREATE (node $paramName)`.
          |This is not possible with patterns used in other clauses, as Cypher needs to know the property names at the time the query is compiled, so that matching can be done effectively."""
      )
    }

    section("Patterns for relationships", "cypher-pattern-relationship") {
      p(
        """The simplest way to describe a relationship is by using the arrow between two nodes, as in the previous examples.
          |Using this technique, you can describe that the relationship should exist and the directionality of it.
          |If you don't care about the direction of the relationship, the arrow head can be omitted, as exemplified by:"""
      )
      p("""[source, cypher, role=noplay]
          |----
          |(a)--(b)
          |----
          |""")
      p("""As with nodes, relationships may also be given names.
          |In this case, a pair of square brackets is used to break up the arrow and the variable is placed between.
          |For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[r]->(b)
          |----
          |""")
      p("""Much like labels on nodes, relationships can have types.
          |To describe a relationship with a specific type, you can specify this as follows:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[r:REL_TYPE]->(b)
          |----
          |""")
      p(
        """Unlike labels, relationships can only have one type.
          |But if we'd like to describe some data such that the relationship could have any one of a set of types, then they can all be listed in the pattern, separating them with the pipe symbol `|` like this:"""
      )
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[r:TYPE1|TYPE2]->(b)
          |----
          |""")
      p(
        """Note that this form of pattern can only be used to describe existing data (ie. when using a pattern with `MATCH` or as an expression).
          |It will not work with `CREATE` or `MERGE`, since it's not possible to create a relationship with multiple types."""
      )
      p("As with nodes, the name of the relationship can always be omitted, as exemplified by:")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[:REL_TYPE]->(b)
          |----
          |""")
      p(
        "It is not possible to use the same name for a relationship multiple times within a pattern due to <<relationship-isomorphism, relationship isomorphism>>. The following example is therefore not allowed:"
      )
      p("""[source, cypher, role=noplay]
          |----
          |()-[r:REL_TYPE]-()-[r:REL_TYPE]-()
          |----
          |""")
    }

    section("Variable-length pattern matching", "cypher-pattern-varlength") {
      caution {
        p("""Variable length pattern matching in versions 2.1.x and earlier does not enforce relationship uniqueness for patterns described within a single `MATCH` clause.
            |This means that a query such as the following: `MATCH (a)-[r]\->(b), p = (a)-[\*]\->(c) RETURN *, relationships(p) AS rs` may include `r` as part of the `rs` set.
            |This behavior has changed in versions 2.2.0 and later, in such a way that `r` will be excluded from the result set, as this better adheres to the rules of relationship uniqueness as documented here <<cypher-result-uniqueness>>.
            |If you have a query pattern that needs to retrace relationships rather than ignoring them as the relationship uniqueness rules normally dictate, you can accomplish this using multiple match clauses, as follows: `MATCH (a)-[r]\->(b) MATCH p = (a)-[*]\->(c) RETURN *, relationships(p)`.
            |This will work in all versions of Neo4j that support the `MATCH` clause, namely 2.0.0 and later.""")
      }
      p("""Rather than describing a long path using a sequence of many node and relationship descriptions in a pattern, many relationships (and the intermediate nodes) can be described by specifying a length in the relationship description of a pattern.
          |For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[*2]->(b)
          |----
          |""")
      p("""This describes a graph of three nodes and two relationships, all in one path (a path of length 2).
          |This is equivalent to:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-->()-->(b)
          |----
          |""")
      p("""A range of lengths can also be specified: such relationship patterns are called 'variable length relationships'.
          |For example:""")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[*3..5]->(b)
          |----
          |""")
      p(
        """This is a minimum length of 3, and a maximum of 5.
          |It describes a graph of either 4 nodes and 3 relationships, 5 nodes and 4 relationships or 6 nodes and 5 relationships, all connected together in a single path."""
      )
      p("Either bound can be omitted. For example, to describe paths of length 3 or more, use:")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[*3..]->(b)
          |----
          |""")
      p("To describe paths of length 5 or less, use:")
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[*..5]->(b)
          |----
          |""")
      p(
        """Omitting both bounds is equivalent to specifying a minimum of 1, allowing paths of any positive length to be described:"""
      )
      p("""[source, cypher, role=noplay]
          |----
          |(a)-[*]->(b)
          |----
          |""")
      p("As a simple example, let's take the graph and query below:")
      graphViz()
      query(
        """MATCH (me)-[:KNOWS*1..2]-(remote_friend)
          |WHERE me.name = 'Filipa'
          |RETURN remote_friend.name""".stripMargin,
        ResultAssertions(r => {
          r.toList should equal(List(Map("remote_friend.name" -> "Dilshad"), Map("remote_friend.name" -> "Anders")))
        })
      ) {
        resultTable()
      }
      p(
        """This query finds data in the graph with a shape that fits the pattern: specifically a node (with the name property *'Filipa'*) and then the `KNOWS` related nodes, one or two hops away.
          |This is a typical example of finding first and second degree friends.""".stripMargin
      )
      p("Note that variable length relationships cannot be used with `CREATE` and `MERGE`.")
    }

    section("Assigning to path variables", "cypher-pattern-path-variables") {
      p("""As described above, a series of connected nodes and relationships is called a "path". Cypher allows paths to be named
          |using an identifer, as exemplified by:""")
      p("""[source, cypher, role=noplay]
          |----
          |p = (a)-[*3..5]->(b)
          |----
          |""")
      p("You can do this in `MATCH`, `CREATE` and `MERGE`, but not when using patterns as expressions.")
    }

  }.build()
}
