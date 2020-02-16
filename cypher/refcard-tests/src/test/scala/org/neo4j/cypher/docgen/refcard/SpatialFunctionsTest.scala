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
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.internal.runtime.InternalExecutionResult

class SpatialFunctionsTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("ROOT KNOWS A")
  val title = "Spatial Functions"
  override val linkId = "functions/spatial"

  override def assert(name: String, result: InternalExecutionResult) {
    name match {
      case "returns-one" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=point" =>
        Map("x" -> 2.3, "y" -> 4.5, "z" -> 6.7)
      case "parameters=distance" =>
        Map("x1" -> 2.3, "y1" -> 4.5, "x2" -> 1.3, "y2" -> 3.5)
      case "" =>
        Map()
    }

  def text = """
###assertion=returns-one parameters=point
RETURN

point({x: $x, y: $y})
###
Returns a point in a 2D cartesian coordinate system.

###assertion=returns-one parameters=point
RETURN

point({latitude: $y, longitude: $x})
###
Returns a point in a 2D geographic coordinate system, with coordinates specified in decimal degrees.

###assertion=returns-one parameters=point
RETURN

point({x: $x, y: $y, z: $z})
###
Returns a point in a 3D cartesian coordinate system.

###assertion=returns-one parameters=point
RETURN

point({latitude: $y, longitude: $x, height: $z})
###
Returns a point in a 3D geographic coordinate system, with latitude and longitude in decimal degrees, and height in meters.

###assertion=returns-one parameters=distance
RETURN

distance(point({x: $x1, y: $y1}), point({x: $x2, y: $y2}))
###

Returns a floating point number representing the linear distance between two points.
The returned units will be the same as those of the point coordinates, and it will work for both 2D and 3D cartesian points.

###assertion=returns-one parameters=distance
RETURN

distance(point({latitude: $y1, longitude: $x1}), point({latitude: $y2, longitude: $x2}))
###

Returns the geodesic distance between two points in meters. It can be used for 3D geographic points as well.
"""
}
