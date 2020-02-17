/*
 * Copyright (c) 2002-2020 "Neo4j,"
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
package org.neo4j.cypher.internal.compatibility.v3_4.runtime

import java.time._
import java.time.temporal.TemporalAmount

import org.neo4j.cypher.internal.runtime.QueryContext
import org.neo4j.graphdb.{Node, Path, PropertyContainer, Relationship}
import org.neo4j.graphdb.spatial.Point

import scala.collection.Map

trait CypherSerializer {

  /*
  Be explicit to force the decision how to represent each type.
  Don't use internal types.
   */
  protected def serialize(a: Any, qtx: QueryContext): String = a match {
    case x: Node                  => x.toString + serializeProperties(x, qtx)
    case x: Relationship          => ":" + x.getType.name() + "[" + x.getId + "]" + serializeProperties(x, qtx)
    case x: Path                  => x.toString
    case x: Map[_, _]             => makeString(x.asInstanceOf[Map[String, Any]], qtx)
    case x: Seq[_]                => x.map(elem => serialize(elem, qtx)).mkString("[", ",", "]")
    case x: Array[_]              => x.map(elem => serialize(elem, qtx)).mkString("[", ",", "]")
    case x: String                => "\"" + x + "\""
    case x: Integer               => x.toString
    case x: Long                  => x.toString
    case x: Double                => x.toString
    case x: Boolean               => x.toString
    case x: TemporalAmount        => x.toString
    case x: LocalDate             => x.toString
    case x: LocalDateTime         => x.toString
    case x: LocalTime             => x.toString
    case x: OffsetTime            => x.toString
    case x: ZonedDateTime         => x.toString
    case x: Point                 => x.toString
    case null                     => "<null>"
    case x                        => throw new IllegalArgumentException(s"Type ${x.getClass} must be explicitly handled.")
  }

  protected def serializeProperties(x: PropertyContainer, qtx: QueryContext): String = {
    val (ops, id, deleted) = x match {
      case n: Node => (qtx.nodeOps, n.getId, qtx.nodeOps.isDeletedInThisTx(n.getId))
      case r: Relationship => (qtx.relationshipOps, r.getId, qtx.relationshipOps.isDeletedInThisTx(r.getId))
    }

    val keyValStrings = if (deleted) Iterator("deleted")
    else ops.propertyKeyIds(id).
      map(pkId => qtx.getPropertyKeyName(pkId) + ":" + serialize(ops.getProperty(id, pkId).asObject(), qtx))

    keyValStrings.mkString("{", ",", "}")
  }

  private def makeString(m: Map[String, Any], qtx: QueryContext) = m.map {
    case (k, v) => k + " -> " + serialize(v, qtx)
  }.mkString("{", ", ", "}")
}
