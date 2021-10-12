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

  case class CartesianPoint3D(x: Double, y: Double, z: Double, crs: CRS) extends TestPoint3D(x, y, z, crs)

  override def outputPath = "target/docs/dev/ql/functions"

  override def doc = new DocBuilder {
    doc("Spatial functions", "query-functions-spatial")
    initQueries(
      """CREATE (copenhagen:TrainStation {longitude: 12.564590, latitude: 55.672874, city: 'Copenhagen'}),
        |       (malmo:Office {longitude: 12.994341, latitude: 55.611784, city: 'Malmo'}),
        |
        |       (copenhagen)-[:TRAVEL_ROUTE]->(malmo)""")
    synopsis("These functions are used to specify 2D or 3D points in a Coordinate Reference System (CRS) and to calculate the geodesic distance between two points.")
    p(
      """Functions:
        |
        |* <<functions-distance,point.distance()>>
        |* <<functions-withinBBox,point.withinBBox()>>
        |* <<functions-point-wgs84-2d,point() - WGS 84 2D>>
        |* <<functions-point-wgs84-3d,point() - WGS 84 3D>>
        |* <<functions-point-cartesian-2d,point() - Cartesian 2D>>
        |* <<functions-point-cartesian-3d,point() - Cartesian 3D>>
      """.stripMargin)
    p("The following graph is used for some of the examples below.")
    graphViz()
    section("point.distance()", "functions-distance") {
      p(
        """`point.distance()` returns a floating point number representing the geodesic distance between two points in the same Coordinate Reference System (CRS).
          |
          |* If the points are in the _Cartesian_ CRS (2D or 3D), then the units of the returned distance will be the same as the units of the points, calculated using Pythagoras' theorem.
          |* If the points are in the _WGS-84_ CRS (2D), then the units of the returned distance will be meters, based on the haversine formula over a spherical earth approximation.
          |* If the points are in the _WGS-84_ CRS (3D), then the units of the returned distance will be meters.
          | ** The distance is calculated in two steps.
          |  *** First, a haversine formula over a spherical earth is used, at the average height of the two points.
          |  *** To account for the difference in height, Pythagoras' theorem is used, combining the previously calculated spherical distance with the height difference.
          | ** This formula works well for points close to the earth's surface; for instance, it is well-suited for calculating the distance of an airplane flight.
          |It is less suitable for greater heights, however, such as when calculating the distance between two satellites.
        """.stripMargin)
      function("point.distance(point1, point2)", "A Float.", ("point1", "A point in either a geographic or cartesian coordinate system."), ("point2", "A point in the same CRS as `point1`."))
      considerations("`point.distance(null, null)`, `point.distance(null, point2)` and `point.distance(point1, null)` all return `null`.", "Attempting to use points with different Coordinate Reference Systems (such as WGS 84 2D and WGS 84 3D) will return `null`.")
      query(
        """WITH point({x: 2.3, y: 4.5, crs: 'cartesian'}) AS p1, point({x: 1.1, y: 5.4, crs: 'cartesian'}) AS p2
          |RETURN point.distance(p1,p2) AS dist""".stripMargin, ResultAssertions((r) => {
          r.toList.head("dist").asInstanceOf[Double] should equal(1.5)
        })) {
        p("The distance between two 2D points in the _Cartesian_ CRS is returned.")
        resultTable()
      }
      query(
        """WITH point({longitude: 12.78, latitude: 56.7, height: 100}) as p1, point({latitude: 56.71, longitude: 12.79, height: 100}) as p2
          |RETURN point.distance(p1,p2) as dist""".stripMargin, ResultAssertions((r) => {
          Math.round(r.toList.head("dist").asInstanceOf[Double]) should equal(1270)
        })) {
        p("The distance between two 3D points in the _WGS 84_ CRS is returned.")
        resultTable()
      }
      query(
        """MATCH (t:TrainStation)-[:TRAVEL_ROUTE]->(o:Office)
          |WITH point({longitude: t.longitude, latitude: t.latitude}) AS trainPoint, point({longitude: o.longitude, latitude: o.latitude}) AS officePoint
          |RETURN round(point.distance(trainPoint, officePoint)) AS travelDistance""".stripMargin, ResultAssertions((r) => {
          r.toList.head("travelDistance").asInstanceOf[Double] should equal(27842)
        })) {
        p("The distance between the train station in Copenhagen and the Neo4j office in Malmo is returned.")
        resultTable()
      }
      query("RETURN point.distance(null, point({longitude: 56.7, latitude: 12.78})) AS d", ResultAssertions((r) => {
        r.toList should equal(List(Map("d" -> null)))
      })) {
        p("If `null` is provided as one or both of the arguments, `null` is returned.")
        resultTable()
      }
    }
    section("point.withinBBox()", "functions-withinBBox") {
      p(
        """`point.withinBBox()` takes three arguments, the first argument is the point to check and the two other define the lower-left (or south-west) and upper-right (or north-east) point of a bounding box respectively.
          | The return value will be `true` if the provided point is contained in the bounding box (boundary included) otherwise `false`.
        """.stripMargin)
      function("point.withinBBox(point, lowerLeft, upperRight)", "A Boolean.", ("point", "A point in either a geographic or cartesian coordinate system."), ("lowerLeft", "A point in the same CRS as 'point'."), ("upperRight", "A point in the same CRS as 'point'."))
      considerations("`point.withinBBox(p1, p2, p3)` will return `null` if any of the arguments evaluate to `null`.",
        "Attempting to use points with different Coordinate Reference Systems (such as WGS 84 2D and WGS 84 3D) will return `null`.",
         "`point.withinBBox` will handle crossing the 180th meridian in geographic coordinates.",
         "Switching the longitude of the `lowerLeft` and `upperRight` in geographic coordinates will switch the direction of the resulting bounding box.",
         "Switching the latitude of the `lowerLeft` and `upperRight` in geographic coordinates so that the former is north of the latter will result in an empty range.",
      )
      query(
        """WITH point({x: 0, y: 0, crs: 'cartesian'}) AS lowerLeft, point({x: 10, y: 10, crs: 'cartesian'}) AS upperRight
          |RETURN point.withinBBox(point({x: 5, y: 5, crs: 'cartesian'}), lowerLeft, upperRight) AS result""".stripMargin, ResultAssertions((r) => {
          r.toList.head("result").asInstanceOf[Boolean] shouldBe true
        })) {
        p("Checking if a point in _Cartesian_ CRS is contained in the bounding box.")
        resultTable()
      }
      query(
        """WITH point({longitude: 12.53, latitude: 55.66}) AS lowerLeft, point({longitude: 12.614, latitude: 55.70}) AS upperRight
          |MATCH (t:TrainStation)
          |WHERE point.withinBBox(point({longitude: t.longitude, latitude: t.latitude}), lowerLeft, upperRight)
          |RETURN count(t)""".stripMargin, ResultAssertions((r) => {
          r.toList.head("count(t)").asInstanceOf[Long] should equal(1)
        })) {
        p("Finds all train stations contained in a bounding box around Copenhagen.")
        resultTable()
      }
      query(
        """WITH point({longitude: 179, latitude: 55.66}) AS lowerLeft, point({longitude: -179, latitude: 55.70}) AS upperRight
          |RETURN point.withinBBox(point({longitude: 180, latitude: 55.66}, lowerLeft, upperRight) AS result
          |""".stripMargin, ResultAssertions((r) => {
          r.toList.head("result").asInstanceOf[Boolean] shouldBe true
        })) {
        p("A bounding box that crosses the 180th meridian.")
        resultTable()
      }
      query("RETURN point.withinBBox(null, point({longitude: 56.7, latitude: 12.78}),  point({longitude: 57.0, latitude: 13.0})) AS in", ResultAssertions((r) => {
        r.toList should equal(List(Map("in" -> null)))
      })) {
        p("If `null` is provided as any of the arguments, `null` is returned.")
        resultTable()
      }
    }
    section("point() - WGS 84 2D", "functions-point-wgs84-2d") {
      p("`point({longitude | x, latitude | y [, crs][, srid]})` returns a 2D point in the _WGS 84_ CRS corresponding to the given coordinate values.")
      function("point({longitude | x, latitude | y [, crs][, srid]})", "A 2D point in _WGS 84_.", ("A single map consisting of the following:", ""), ("longitude/x", "A numeric expression that represents the longitude/x value in decimal degrees"), ("latitude/y", "A numeric expression that represents the latitude/y value in decimal degrees"), ("crs", "The optional string 'WGS-84'"), ("srid", "The optional number 4326"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "If the coordinates are specified using `latitude` and `longitude`, the `crs` or `srid` fields are optional and inferred to be `'WGS-84'` (srid=4326).", "If the coordinates are specified using `x` and `y`, then either the `crs` or `srid` field is required if a geographic CRS is desired.")
      query("RETURN point({longitude: 56.7, latitude: 12.78}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint2D(56.7, 12.78, CRS.WGS84))))
      })) {
        p("A 2D point with a `longitude` of `56.7` and a `latitude` of `12.78` in the _WGS 84_ CRS is returned.")
        resultTable()
      }
      query("RETURN point({x: 2.3, y: 4.5, crs: 'WGS-84'}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint2D(2.3, 4.5, CRS.WGS84))))
      })) {
        p("`x` and `y` coordinates may be used in the _WGS 84_ CRS instead of `longitude` and `latitude`, respectively, providing `crs` is set to `'WGS-84'`, or `srid` is set to `4326`.")
        resultTable()
      }
      query(
        """MATCH (p:Office)
          |RETURN point({longitude: p.longitude, latitude: p.latitude}) AS officePoint""".stripMargin, ResultAssertions((r) => {
          r.toList should equal(List(Map("officePoint" -> GeographicPoint2D(12.994341, 55.611784, CRS.WGS84))))
        })) {
        p("A 2D point representing the coordinates of the city of Malmo in the _WGS 84_ CRS is returned.")
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
      p("`point({longitude | x, latitude | y, height | z, [, crs][, srid]})` returns a 3D point in the _WGS 84_ CRS corresponding to the given coordinate values.")
      function("point({longitude | x, latitude | y, height | z, [, crs][, srid]})", "A 3D point in _WGS 84_.", ("A single map consisting of the following:", ""), ("longitude/x", "A numeric expression that represents the longitude/x value in decimal degrees"), ("latitude/y", "A numeric expression that represents the latitude/y value in decimal degrees"), ("height/z", "A numeric expression that represents the height/z value in meters"), ("crs", "The optional string 'WGS-84-3D'"), ("srid", "The optional number 4979"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "If the `height/z` key and value is not provided, a 2D point in the _WGS 84_ CRS will be returned.", "If the coordinates are specified using `latitude` and `longitude`, the `crs` or `srid` fields are optional and inferred to be `'WGS-84-3D'` (srid=4979).", "If the coordinates are specified using `x` and `y`, then either the `crs` or `srid` field is required if a geographic CRS is desired.")
      query("RETURN point({longitude: 56.7, latitude: 12.78, height: 8}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> GeographicPoint3D(56.7, 12.78, 8, CRS.WGS843D))))
      })) {
        p("A 3D point with a `longitude` of `56.7`, a `latitude` of `12.78` and a height of `8` meters in the _WGS 84_ CRS is returned.")
        resultTable()
      }
    }
    section("point() - Cartesian 2D", "functions-point-cartesian-2d") {
      p("`point({x, y [, crs][, srid]})` returns a 2D point in the _Cartesian_ CRS corresponding to the given coordinate values.")
      function("point({x, y [, crs][, srid]})", "A 2D point in _Cartesian_.", ("A single map consisting of the following:", ""), ("x", "A numeric expression"), ("y", "A numeric expression"), ("crs", "The optional string 'cartesian'"), ("srid", "The optional number 7203"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "The `crs` or `srid` fields are optional and default to the _Cartesian_ CRS (which means `srid:7203`).")
      query("RETURN point({x: 2.3, y: 4.5}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> CartesianPoint2D(2.3, 4.5, CRS.Cartesian))))
      })) {
        p("A 2D point with an `x` coordinate of `2.3` and a `y` coordinate of `4.5` in the _Cartesian_ CRS is returned.")
        resultTable()
      }
    }
    section("point() - Cartesian 3D", "functions-point-cartesian-3d") {
      p("`point({x, y, z, [, crs][, srid]})` returns a 3D point in the _Cartesian_ CRS corresponding to the given coordinate values.")
      function("point({x, y, z, [, crs][, srid]})", "A 3D point in _Cartesian_.", ("A single map consisting of the following:", ""), ("x", "A numeric expression"), ("y", "A numeric expression"), ("z", "A numeric expression"), ("crs", "The optional string 'cartesian-3D'"), ("srid", "The optional number 9157"))
      considerations("If any argument provided to `point()` is `null`, `null` will be returned.", "If the `z` key and value is not provided, a 2D point in the _Cartesian_ CRS will be returned.", "The `crs` or `srid` fields are optional and default to the _3D Cartesian_ CRS (which means `srid:9157`).")
      query("RETURN point({x: 2.3, y: 4.5, z: 2}) AS point", ResultAssertions((r) => {
        r.toList should equal(List(Map("point" -> CartesianPoint3D(2.3, 4.5, 2, CRS.Cartesian3D))))
      })) {
        p("A 3D point with an `x` coordinate of `2.3`, a `y` coordinate of `4.5` and a `z` coordinate of `2` in the _Cartesian_ CRS is returned.")
        resultTable()
      }
    }
  }.build()
}
