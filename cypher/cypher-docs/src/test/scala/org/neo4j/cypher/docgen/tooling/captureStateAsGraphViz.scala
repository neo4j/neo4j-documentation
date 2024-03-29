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
package org.neo4j.cypher.docgen.tooling

import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.util._
import org.neo4j.visualization.graphviz.AsciiDocSimpleStyle
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Walker

import java.io.ByteArrayOutputStream

/**
 * Run this method to capture the graph state. The Content object sent in will be rewritten
 * away and replaced with a Content object containing the GraphViz
 */
object captureStateAsGraphViz extends GraphIcing {

  def apply(db: GraphDatabaseCypherService, name: String, count: Int, options: String): GraphViz =
    GraphViz(emitGraphviz(s"$name-$count", options, db))

  private def emitGraphviz(testid: String, graphVizOptions: String, db: GraphDatabaseCypherService): String = {
    val out = new ByteArrayOutputStream()
    val writer = new GraphvizWriter(AsciiDocSimpleStyle.withAutomaticRelationshipTypeColors())

    db.withTx(tx => {
      writer.emit(out, Walker.fullGraph(tx))
    })

    """.Graph
      |["dot", "%s.svg", "neoviz", "%s"]
      |----
      |%s
      |----
      | """.stripMargin.format(testid, graphVizOptions, out)
  }

}

case class replaceSingleObject(from: Content, to: Content) extends Rewriter {
  override def apply(input: AnyRef) = instance.apply(input)

  private val instance: Rewriter = bottomUp(Rewriter.lift {
    case x if x == from => to
  })
}
