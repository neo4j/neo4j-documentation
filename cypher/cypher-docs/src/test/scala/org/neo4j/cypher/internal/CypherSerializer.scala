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
package org.neo4j.cypher.internal

import org.neo4j.cypher.internal.runtime.QueryContext
import org.neo4j.graphdb.Entity
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Path
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.spatial.Point
import org.neo4j.io.pagecache.context.CursorContext
import org.neo4j.memory.EmptyMemoryTracker

import java.time._
import java.time.temporal.TemporalAmount
import scala.collection.Map

trait CypherSerializer {

  /*
  Be explicit to force the decision how to represent each type.
  Don't use internal types.
   */
  protected def serialize(a: Any, qtx: QueryContext): String = a match {
    case x: Node           => x.toString + serializeProperties(x, qtx)
    case x: Relationship   => ":" + x.getType.name() + "[" + x.getId + "]" + serializeProperties(x, qtx)
    case x: Path           => x.toString
    case x: Map[_, _]      => makeString(x.asInstanceOf[Map[String, Any]], qtx)
    case x: Seq[_]         => x.map(elem => serialize(elem, qtx)).mkString("[", ",", "]")
    case x: Array[_]       => x.map(elem => serialize(elem, qtx)).mkString("[", ",", "]")
    case x: String         => "\"" + x + "\""
    case x: Integer        => x.toString
    case x: Long           => x.toString
    case x: Double         => x.toString
    case x: Boolean        => x.toString
    case x: TemporalAmount => x.toString
    case x: LocalDate      => x.toString
    case x: LocalDateTime  => x.toString
    case x: LocalTime      => x.toString
    case x: OffsetTime     => x.toString
    case x: ZonedDateTime  => x.toString
    case x: Point          => x.toString
    case null              => "<null>"
    case x                 => throw new IllegalArgumentException(s"Type ${x.getClass} must be explicitly handled.")
  }

  protected def serializeProperties(x: Entity, qtx: QueryContext): String = {
    val cursors = qtx.transactionalContext.cursors
    val property = cursors.allocatePropertyCursor(CursorContext.NULL_CONTEXT, EmptyMemoryTracker.INSTANCE)
    val (propertyText, id, deleted) = x match {
      case n: Node =>
        val ops = qtx.nodeReadOps
        val node = cursors.allocateNodeCursor(CursorContext.NULL_CONTEXT)
        (
          (id: Long) =>
            ops.propertyKeyIds(id, node, property).map(pkId =>
              qtx.getPropertyKeyName(pkId) + ":" + serialize(
                ops.getProperty(id, pkId, node, property, throwOnDeleted = true).asObject(),
                qtx
              )
            ),
          n.getId,
          qtx.nodeReadOps.isDeletedInThisTx(n.getId)
        )
      case r: Relationship =>
        val ops = qtx.relationshipReadOps
        val rel = cursors.allocateRelationshipScanCursor(CursorContext.NULL_CONTEXT)
        (
          (id: Long) =>
            ops.propertyKeyIds(id, rel, property).map(pkId =>
              qtx.getPropertyKeyName(pkId) + ":" + serialize(
                ops.getProperty(id, pkId, rel, property, throwOnDeleted = true).asObject(),
                qtx
              )
            ),
          r.getId,
          qtx.relationshipReadOps.isDeletedInThisTx(r.getId)
        )
    }

    val keyValStrings =
      if (deleted) Array("deleted")
      else propertyText(id)

    keyValStrings.mkString("{", ",", "}")
  }

  private def makeString(m: Map[String, Any], qtx: QueryContext) = m.map {
    case (k, v) => k + " -> " + serialize(v, qtx)
  }.mkString("{", ", ", "}")
}
