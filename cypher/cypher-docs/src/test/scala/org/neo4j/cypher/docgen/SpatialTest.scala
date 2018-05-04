/*
 * Copyright (c) 2002-2018 "Neo Technology,"
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

class SpatialTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Spatial values", "cypher-spatial")
    synopsis("Cypher has built-in support for handling spatial values, and the underlying database supports storing these spatial values as properties on nodes and relationships.")
    note{
      p("""Refer to <<query-functions-spatial>> for information regarding spatial _functions_ allowing for the creation and manipulation of spatial values.""")
      p("""Refer to <<cypher-ordering>> for information regarding the comparison and ordering of spatial values.""")
    }
    p(
      """Four Coordinate Reference Systems (CRS) are supported, each of which falls within one of two types: _geographic coordinates_ modeling points on the earth, or _cartesian coordinates_ modeling points in euclidean space:
        |
        |* http://spatialreference.org/ref/epsg/4326/[WGS 84 2D] - A 2D geographic point in the _WGS 84_ CRS is specified with a map containing coordinate values for either of the following:
        | ** `longitude` and `latitude` (if these are specified, and the `crs` is not, then the `crs` is assumed to be `WGS-84`)
        | ** `x` and `y` (in this case the `crs` must be specified, or will be assumed to be `Cartesian`)
        |* http://spatialreference.org/ref/epsg/4979/[WGS 84 3D] - A 3D geographic point in the _WGS 84_ CRS is specified with a map containing coordinate values for either of the following:
        | ** `longitude`, `latitude` and either `height` or `z` (if these are specified, and the `crs` is not, then the `crs` is assumed to be `WGS-84-3D`)
        | ** `x`, `y` and `z` (in this case the `crs` must be specified, or will be assumed to be `Cartesian-3D`)
        |* http://spatialreference.org/ref/sr-org/7203/[Cartesian 2D]
        | ** A 2D point in the _Cartesian_ CRS is specified with a map containing `x` and `y` coordinate values
        |* http://spatialreference.org/ref/sr-org/9157/[Cartesian 3D]
        | ** A 3D point in the _Cartesian_ CRS is specified with a map containing `x`, `y` and `z` coordinate values
        |""".stripMargin)
    p("Data within different coordinate systems are entirely incomparable, and cannot be implicitly converted from one to the other. This is true even if they are both cartesian or both geographic. For example, if you search for 3D points using a 2D range, you will get no results.")
  }.build()
}
