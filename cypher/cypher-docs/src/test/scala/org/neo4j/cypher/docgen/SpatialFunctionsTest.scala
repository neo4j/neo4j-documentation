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
    val Cartesian3D = CRS("cartesian-3d", 9157, "http://spatialreference.org/ref/sr-org/9157/")
    val WGS843D = CRS("WGS-84-3d", 4979, "http://spatialreference.org/ref/epsg/4979/")
  }

  class TestPoint2D(x: Double, y: Double, crs: CRS) extends org.neo4j.graphdb.spatial.Point {
    override def getCRS: spatial.CRS = crs

    override def getCoordinates: util.List[Coordinate] = List(new Coordinate(x, y)).asJava
  }

  class TestPoint3D(x: Double, y: Double, z: Double, crs: CRS) extends TestPoint2D(x, y, crs) {
    override def getCoordinates: util.List[Coordinate] = List(new Coordinate(x, y, z)).asJava
  }

  case class GeographicPoint2D(x: Double, y: Double, crs: CRS) extends TestPoint2D(x, y, crs)

  case class GeographicPoint3D(x: Double, y: Double, z: Double, crs: CRS) extends TestPoint3D(x, y, z, crs)

  case class CartesianPoint2D(x: Double, y: Double, crs: CRS) extends TestPoint2D(x, y, crs)

  case class CartesianPoint3D(x: Double, y: Double, z:Double, crs: CRS) extends TestPoint3D(x, y, z, crs)

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Spatial functions", "query-functions-spatial")
    initQueries(
      """CREATE (copenhagen:TrainStation {longitude: 12.564590, latitude: 55.672874, city: 'Copenhagen'}),
        |       (malmo:Office {longitude: 12.994341, latitude: 55.611784, city: 'Malmo'}),
        |
        |       (copenhagen)-[:TRAVEL_ROUTE]->(malmo)""")
    synopsis("These functions are used to specify 2D or 3D points in a Coordinate Reference System and to calculate the geodesic distance between two points.")
    p(
      """Four Coordinate Reference Systems (CRS) are supported, each of which falls within one of two CRS categories; namely _WGS 84_ and _Cartesian_:
        |
        |* http://spatialreference.org/ref/epsg/4326/[WGS 84 2D] - A 2D point in the _WGS 84_ CRS category is specified with a map containing coordinate values for either of the following:
        | ** `longitude` and `latitude` (this is the default)
        | ** `x` and `y`
        |* http://spatialreference.org/ref/epsg/4979/[WGS 84 3D] - A 3D point in the _WGS 84_ CRS category is specified with a map containing coordinate values for either of the following:
        | ** `longitude`, `latitude` and either `height` or `z` (this is the default)
        | ** `x`, `y` and `z`
        |* http://spatialreference.org/ref/sr-org/7203/[Cartesian 2D]
        | ** A 2D point in the _Cartesian_ CRS category is specified with a map containing `x` and `y` coordinate values
        |* http://spatialreference.org/ref/sr-org/9157/[Cartesian 3D]
        | ** A 3D point in the _Cartesian_ CRS category is specified with a map containing `x`, `y` and `z` coordinate values
        |""".stripMargin)
    p("It is not possible to convert from the 2D to the 3D systems, even within the same CRS category.")
    p(
      """Functions:
        |
        |* <<functions-distance,distance()>>
        |* <<functions-point-wgs84-2d,point() - WGS 84 2D>>
        |* <<functions-point-wgs84-3d,point() - WGS 84 3D>>
        |* <<functions-point-cartesian-2d,point() - Cartesian 2D>>
        |* <<functions-point-cartesian-3d,point() - Cartesian 3D>>
      """.stripMargin)
    p("The following graph is used for some of the examples below.")
    graphViz()
    section("distance()", "functions-distance") {
      p(
        """`distance()` returns a floating point number representing the geodesic distance between two points in the same Coordinate Reference System.
          |
          |* If the points are in the _Cartesian_ CRS category (2D or 3D), then the units of the returned distance will be the same as the units of the points, calculated using Pythagoras' theorem.
          |* If the points are in the _WGS-84_ CRS category (2D), then the units of the returned distance will be meters, based on the haversine formula over a spherical earth approximation.
          |* If the points are in the _WGS-84_ CRS category (3D), then the units of the returned distance will be meters. The distance is calculated in two steps.
          |  First, a haversine formula over a spherical earth is used, at the average height of the two points. To account for the difference in height,
          |  Pythagoras' theorem is used, combining the first calculated spherical distance and the height difference. This formula works well for points close
          |  to the earth's surface, i.e. it would describe the distance of a plane flight well. It is not well suited for greater heights, e.g. calculating
          |  the distance between two satellites.
        """.stripMargin)
      function("distance(point1, point2)", "A Float.", ("point1", "A point in either the WGS 84 or Cartesian CRS."), ("point2", "A point in the same CRS as 'point1'."))
      considerations("`distance(null, null)`, `distance(null, point2)` and `distance(point1, null)` all return `null`.", "Attempting to use points with different Coordinate Reference Systems (such as WGS 84 2D and WGS 84 3D) will return `null`.")
      query(
        """WITH point({x: 2.3, y: 4.5, crs: 'cartesian'}) AS p1, point({x: 1.1, y: 5.4, crs: 'cartesian'}) AS p2
          |RETURN distance(p1,p2) AS dist""".stripMargin, ResultAssertions((r) => {
        r.toList.head("dist").asInstanceOf[Double] should equal(1.5)
      })) {
        p("The distance between two 2D points in the _Cartesian_ CRS category is returned.")
        resultTable()
      }
      query(
        """WITH point({longitude: 12.78, latitude: 56.7, height: 100}) as p1, point({latitude: 56.71, longitude: 12.79, height: 100}) as p2
           |RETURN distance(p1,p2) as dist""".stripMargin, ResultAssertions((r) => {
        Math.round(r.toList.head("dist").asInstanceOf[Double]) should equal(1270)
      })) {
        p("The distance between two 3D points in the _WGS 84_ CRS category is returned.")
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
    section("point() - WGS 84 2D", "functions-point-wgs84-2d") {
      p("`point(longitude|x, latitude|y)` returns a 2D point in the _WGS 84_ CRS category -- i.e. a point in the WGS 84 2D CRS -- corresponding to the given coordinate values.")
      function("point({longitude | x, latitude | y [, crs]})", "A 2D point in _WGS 84_.", ("A single map consisting of the following:", ""), ("longitude/x", "A numeric expression that represents the longitude/x value in decimal degrees"), ("latitude/y", "A numeric expression that represents the latitude/y value in decimal degrees"), ("crs", "The string 'WGS-84'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.")
      query("RETURN point({longitude: 56.7, latitude: 12.78}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint2D(56.7, 12.78, CRS.WGS84))))
      })) {
        p("A 2D point with a `longitude` of `56.7` and a `latitude` of `12.78` in the _WGS 84_ CRS category is returned.")
        resultTable()
      }
      query("RETURN point({x: 2.3, y: 4.5, crs: 'WGS-84'}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint2D(2.3, 4.5, CRS.WGS84))))
      })) {
        p("`x` and `y` coordinates may be used in the _WGS 84_ CRS instead of `longitude` and `latitude`, respectively, providing `crs` is set to `'WGS-84'`.")
        resultTable()
      }
      query(
        """MATCH (p:Office)
          |RETURN point({longitude: p.longitude, latitude: p.latitude}) AS officePoint""".stripMargin, ResultAssertions((r) => {
        r.toList should equal(List(Map("officePoint" -> GeographicPoint2D(12.994341, 55.611784, CRS.WGS84))))
      })) {
        p("A 2D point representing the coordinates of the city of Malmo in the _WGS 84_ CRS category is returned.")
        resultTable()
      }
      query("RETURN point(null) AS p", ResultAssertions((r) => {
        r.toList should equal(List(Map("p" -> null)))
      })) {
        p("If `null` is provided as the argument, `null` is returned.")
        resultTable()
      }
    }
    section("point() - WGS 84 3D", "functions-point-wgs84-3d") {
      p("`point(longitude|x, latitude|y, height|z)` returns a 3D point in the _WGS 84_ CRS category -- i.e. a point in the WGS 84 3D CRS -- corresponding to the given coordinate values.")
      function("point({longitude | x, latitude | y, height | z, [, crs]})", "A 3D point in _WGS 84_.", ("A single map consisting of the following:", ""), ("longitude/x", "A numeric expression that represents the longitude/x value in decimal degrees"), ("latitude/y", "A numeric expression that represents the latitude/y value in decimal degrees"), ("height/z", "A numeric expression that represents the height/z value in meters"), ("crs", "The string 'WGS-84-3D'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "If the `height/z` key and value is not provided, a 2D point in _WGS 84_ will be returned.")
      query("RETURN point({longitude: 56.7, latitude: 12.78, height: 8}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint3D(56.7, 12.78, 8, CRS.WGS843D))))
      })) {
        p("A 3D point with a `longitude` of `56.7`, a `latitude` of `12.78` and a height of `8` meters in the _WGS 84_ CRS category is returned.")
        resultTable()
      }
    }
    section("point() - Cartesian 2D", "functions-point-cartesian-2d") {
      p("`point(x, y)` returns a 2D point in the _Cartesian_ CRS category -- i.e. a point in the Cartesian 2D CRS -- corresponding to the given coordinate values.")
      function("point({x, y [, crs]})", "A 2D point in _Cartesian_.", ("A single map consisting of the following:", ""), ("x", "A numeric expression"), ("y", "A numeric expression"), ("crs", "The string 'cartesian'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.")
      query("RETURN point({x: 2.3, y: 4.5}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> CartesianPoint2D(2.3, 4.5, CRS.Cartesian))))
      })) {
        p("A 2D point with an `x` coordinate of `2.3` and a `y` coordinate of `4.5` in the _Cartesian_ CRS category is returned")
        resultTable()
      }
    }
    section("point() - Cartesian 3D", "functions-point-cartesian-3d") {
      p("`point(x, y, z)` returns a 3D point in the _Cartesian_ CRS category -- i.e. a point in the Cartesian 3D CRS -- corresponding to the given coordinate values.")
      function("point({x, y, z, [, crs]})", "A 3D point in _Cartesian_.", ("A single map consisting of the following:", ""), ("x", "A numeric expression"), ("y", "A numeric expression"), ("z", "A numeric expression"), ("crs", "The string 'cartesian-3D'"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "If the `z` key and value is not provided, a 2D point in _Cartesian_ will be returned.")
      query("RETURN point({x: 2.3, y: 4.5, z: 2}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> CartesianPoint3D(2.3, 4.5, 2, CRS.Cartesian3D))))
      })) {
        p("A 3D point with an `x` coordinate of `2.3`, a `y` coordinate of `4.5` and a `z` coordinate of `2` in the _Cartesian_ CRS category is returned")
        resultTable()
      }
    }
  }.build()
}
