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

import java.util

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.spatial
import org.neo4j.graphdb.spatial.Coordinate

import collection.JavaConverters._

class SpatialFunctionsTest extends DocumentingTest {

  case class CRS(name: String, code: Int, href: String) extends org.neo4j.graphdb.spatial.CRS {
    override def getHref: String = href

    override def getType: String = name

    override def getCode: Int = code
  }

  object CRS {
    val Cartesian = CRS("cartesian", 7203, "http://spatialreference.org/ref/sr-org/7203/")
    val WGS84 = CRS("WGS-84", 4326, "http://spatialreference.org/ref/epsg/4326/")
  }

  class TestPoint(x: Double, y: Double, crs: CRS) extends org.neo4j.graphdb.spatial.Point {
    override def getCRS: spatial.CRS = crs

    override def getCoordinates: util.List[Coordinate] = List(new Coordinate(x, y)).asJava
  }

  case class GeographicPoint(x: Double, y: Double, crs: CRS) extends TestPoint(x, y, crs)

  case class CartesianPoint(x: Double, y: Double, crs: CRS) extends TestPoint(x, y, crs)

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Spatial functions", "query-functions-spatial")
    initQueries(
      """CREATE (copenhagen:TrainStation {longitude: 12.564590, latitude: 55.672874, city: 'Copenhagen'}),
        |       (malmo:Office {longitude: 12.994341, latitude: 55.611784, city: 'Malmo'}),
        |
        |       (copenhagen)-[:TRAVEL_ROUTE]->(malmo)""")
    synopsis("These functions are used to specify points in a 2D coordinate system and to calculate the geodesic distance between two points.")
    p("Two coordinate reference systems (CRS) are supported: (i) http://spatialreference.org/ref/epsg/4326/[WGS 84] and (ii) http://spatialreference.org/ref/sr-org/7203/[Cartesian 2D].")
    p("""_WGS 84_ is specified with a map containing coordinate values for either `longitude` and `latitude` (this is the default), or `x` and `y`.
        |_Cartesian_ is specified with a map containing only `x` and `y` coordinate values.""" stripMargin)
    p(
      """Functions:
        |
        |* <<functions-distance,distance()>>
        |* <<functions-point,point() - WGS 84>>
        |* <<functions-point-cartesian,point() - Cartesian 2D>>
      """.stripMargin)
    p("The following graph is used for some of the examples below.")
    graphViz()
    section("distance()", "functions-distance") {
      p(
        """`distance()` returns a floating point number representing the geodesic distance between two points in the same CRS.
          |If the points are in the _Cartesian_ CRS, then the units of the returned distance will be the same as the units of the points, calculated using Pythagoras' theorem.
          |If the points are in the _WGS-84_ CRS, then the units of the returned distance will be meters, based on the haversine formula over a spherical earth approximation.
        """.stripMargin)
      function("distance(point1, point2)", "A Float.", ("point1", "A point in either the WGS 84 or Cartesian CRS."), ("point2", "A point in the same CRS as 'point1'."))
      considerations("`distance(null, null)`, `distance(null, point2)` and `distance(point1, null)` all return `null`.")
      query("WITH point({x: 2.3, y: 4.5, crs: 'cartesian'}) as p1, point({x: 1.1, y: 5.4, crs: 'cartesian'}) as p2\nRETURN distance(p1,p2) AS dist", ResultAssertions((r) => {
        r.toList.head("dist").asInstanceOf[Double] should equal(1.5)
      })) {
        p("The distance between two points in the _Cartesian_ CRS is returned.")
        resultTable()
      }
      query(
        """MATCH (t:TrainStation)-[:TRAVEL_ROUTE]->(o:Office)
          |WITH point({longitude: t.longitude, latitude: t.latitude}) AS trainPoint, point({longitude: o.longitude, latitude: o.latitude}) AS officePoint
          |RETURN round(distance(trainPoint, officePoint)) AS travelDistance""".stripMargin, ResultAssertions((r) => {
          r.toList.head("travelDistance").asInstanceOf[Double] should equal(27842)
        })) {
        p("The distance between the train station in Copenhagen and the Neo4j office in Malmo is returned.")
        resultTable()
      }
      query("RETURN distance(null, point({longitude: 56.7, latitude: 12.78})) AS d", ResultAssertions((r) => {
        r.toList should equal(List(Map("d" -> null)))
      })) {
        p("If `null` is provided as one or both of the arguments, `null` is returned.")
        resultTable()
      }
    }
    section("point() - WGS 84", "functions-point") {
      p("`point()` returns a point in the _WGS 84_ coordinate system corresponding to the given coordinate values.")
      function("point({longitude | x, latitude | y [, crs]})", "A Point.", ("A single map consisting of the following:", ""), ("longitude/x", "A numeric expression"), ("latitude/y", "A numeric expression"), ("crs", "The string 'WGS-84'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.")
      query("RETURN point({longitude: 56.7, latitude: 12.78}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint(56.7, 12.78, CRS.WGS84))))
      })) {
        p("A point with a `longitude` of `56.7` and a `latitude` of `12.78` in the _WGS 84_ CRS is returned.")
        resultTable()
      }
      query("RETURN point({x: 2.3, y: 4.5, crs: 'WGS-84'}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint(2.3, 4.5, CRS.WGS84))))
      })) {
        p("`x` and `y` coordinates may be used in the _WGS 84_ CRS instead of `longitude` and `latitude`, respectively, providing `crs` is set to `'WGS-84'`.")
        resultTable()
      }
      query("MATCH (p:Office)\nRETURN point({longitude: p.longitude, latitude: p.latitude}) AS officePoint", ResultAssertions((r) => {
        r.toList should equal(List(Map("officePoint" -> GeographicPoint(12.994341, 55.611784, CRS.WGS84))))
      })) {
        p("A point representing the coordinates of the city of Malmo in the _WGS 84_ CRS is returned.")
        resultTable()
      }
      query("RETURN point(null) AS p", ResultAssertions((r) => {
        r.toList should equal(List(Map("p" -> null)))
      })) {
        p("If `null` is provided as the argument, `null` is returned.")
        resultTable()
      }
    }
    section("point() - Cartesian 2D", "functions-point-cartesian") {
      p("`point()` returns a point in the _Cartesian_ coordinate system corresponding to the given coordinate values.")
      function("point({x, y [, crs]})", "A Point.", ("A single map consisting of the following:", ""), ("x", "A numeric expression"), ("y", "A numeric expression"), ("crs", "The string 'cartesian'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.")
      query("RETURN point({x: 2.3, y: 4.5}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> CartesianPoint(2.3, 4.5, CRS.Cartesian))))
      })) {
        p("A point with an `x` coordinate of `2.3` and a `y` coordinate of `4.5` in the _Cartesian_ CRS is returned")
        resultTable()
      }
    }
  }.build()
}
