/*
 * Copyright (c) 2002-2019 "Neo4j,"
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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}
import org.neo4j.graphdb.spatial.Point

class SpatialTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc = new DocBuilder {
    doc("Spatial values", "cypher-spatial")
    synopsis("Cypher has built-in support for handling spatial values (points), and the underlying database supports storing these point values as properties on nodes and relationships.")
    p(
      """* <<cypher-spatial-introduction, Introduction>>
        |* <<cypher-spatial-crs, Coordinate Reference Systems>>
        | ** <<cypher-spatial-crs-geographic, Geographic coordinate reference systems>>
        | ** <<cypher-spatial-crs-cartesian, Cartesian coordinate reference systems>>
        |* <<cypher-spatial-instants, Spatial instants>>
        | ** <<cypher-spatial-specifying-spatial-instants, Creating points>>
        | ** <<cypher-spatial-accessing-components-spatial-instants, Accessing components of points>>
        |* <<cypher-spatial-index, Spatial index>>
        |* <<cypher-comparability-orderability, Comparability and Orderability>>
        |""".stripMargin)
    note{
      p("""Refer to <<query-functions-spatial>> for information regarding spatial _functions_ allowing for the creation and manipulation of spatial values.""")
      p("""Refer to <<cypher-ordering>> for information regarding the comparison and ordering of spatial values.""")
    }
    section("Introduction", "cypher-spatial-introduction") {
      p(
        """
          |Neo4j supports only one type of spatial geometry, the _Point_ with the following characteristics:
          |
          |* Each point can have either 2 or 3 dimensions. This means it contains either 2 or 3 64-bit floating point values, which together are called the _Coordinate_.
          |* Each point will also be associated with a specific <<cypher-spatial-crs,Coordinate Reference System>> (CRS) that determines the meaning of the values in the _Coordinate_.
          |* Instances of _Point_ and lists of _Point_ can be assigned to node and relationship properties.
          |* Nodes with _Point_ or _List(Point)_ properties can be indexed using a spatial index. This is true for all CRS (and for both 2D and 3D).
          |  There is no special syntax for creating spatial indexes, as it is supported using the existing <<administration-indexes-create-a-single-property-index,indexes>>.
          |* The <<functions-distance,distance function>> will work on points in all CRS and in both 2D and 3D but only if the two points have the same CRS (and therefore also same dimension).
        """.stripMargin)
    }
    section("Coordinate Reference Systems", "cypher-spatial-crs") {
      p(
        """Four Coordinate Reference Systems (CRS) are supported, each of which falls within one of two types: _geographic coordinates_ modeling points on the earth, or _cartesian coordinates_ modeling points in euclidean space:
          |
          |* <<cypher-spatial-crs-geographic, Geographic coordinate reference systems>>
          | ** WGS-84: longitude, latitude (x, y)
          | ** WGS-84-3D: longitude, latitude, height (x, y, z)
          |* <<cypher-spatial-crs-cartesian, Cartesian coordinate reference systems>>
          | ** Cartesian: x, y
          | ** Cartesian 3D: x, y, z
          |""".stripMargin)
      p(
        """
          |Data within different coordinate systems are entirely incomparable, and cannot be implicitly converted from one to the other.
          |This is true even if they are both cartesian or both geographic. For example, if you search for 3D points using a 2D range, you will get no results.
          |However, they can be ordered, as discussed in more detail in the section on <<cypher-ordering, Cypher ordering>>.
        """.stripMargin)
      section("Geographic coordinate reference systems", "cypher-spatial-crs-geographic") {
        p(
          """Two Geographic Coordinate Reference Systems (CRS) are supported, modeling points on the earth:
            |
            |* http://spatialreference.org/ref/epsg/4326/[WGS 84 2D]
            | ** A 2D geographic point in the _WGS 84_ CRS is specified in one of two ways:
            |  *** `longitude` and `latitude` (if these are specified, and the `crs` is not, then the `crs` is assumed to be `WGS-84`)
            |  *** `x` and `y` (in this case the `crs` must be specified, or will be assumed to be `Cartesian`)
            | ** Specifying this CRS can be done using either the name 'wgs-84' or the SRID 4326 as described in <<functions-point-wgs84-2d,Point(WGS-84)>>
            |* http://spatialreference.org/ref/epsg/4979/[WGS 84 3D]
            | ** A 3D geographic point in the _WGS 84_ CRS is specified one of in two ways:
            |  *** `longitude`, `latitude` and either `height` or `z` (if these are specified, and the `crs` is not, then the `crs` is assumed to be `WGS-84-3D`)
            |  *** `x`, `y` and `z` (in this case the `crs` must be specified, or will be assumed to be `Cartesian-3D`)
            | ** Specifying this CRS can be done using either the name 'wgs-84-3d' or the SRID 4979 as described in <<functions-point-wgs84-3d,Point(WGS-84-3D)>>
            |""".stripMargin)
        p(
          """
            |The units of the `latitude` and `longitude` fields are in decimal degrees, and need to be specified as floating point numbers using Cypher literals.
            |It is not possible to use any other format, like 'degrees, minutes, seconds'. The units of the `height` field are in meters. When geographic points
            |are passed to the `distance` function, the result will always be in meters. If the coordinates are in any other format or unit than supported, it
            |is necessary to explicitly convert them.
            |For example, if the incoming `$height` is a string field in kilometers, you would need to type `height: toFloat($height) * 1000`. Likewise if the
            |results of the `distance` function are expected to be returned in kilometers, an explicit conversion is required.
            |For example: `RETURN distance(a,b) / 1000 AS km`. An example demonstrating conversion on incoming and outgoing values is:
          """.stripMargin)
        query(
          """WITH point({latitude:toFloat('13.43'), longitude:toFloat('56.21')}) AS p1, point({latitude:toFloat('13.10'), longitude:toFloat('56.41')}) as p2
            |RETURN toInteger(distance(p1,p2)/1000) as km""".stripMargin, ResultAssertions((r) => {
            withClue("Expect the distance function to return an integer value") {
              r.head("km") should be(42)
            }
          })) {
          resultTable()
        }
      }
      section("Cartesian coordinate reference systems", "cypher-spatial-crs-cartesian") {
        p(
          """Two Cartesian Coordinate Reference Systems (CRS) are supported, modeling points in euclidean space:
            |
            |* http://spatialreference.org/ref/sr-org/7203/[Cartesian 2D]
            | ** A 2D point in the _Cartesian_ CRS is specified with a map containing `x` and `y` coordinate values
            | ** Specifying this CRS can be done using either the name 'cartesian' or the SRID 7203 as described in <<functions-point-cartesian-2d,Point(Cartesian)>>
            |* http://spatialreference.org/ref/sr-org/9157/[Cartesian 3D]
            | ** A 3D point in the _Cartesian_ CRS is specified with a map containing `x`, `y` and `z` coordinate values
            | ** Specifying this CRS can be done using either the name 'cartesian-3d' or the SRID 9157 as described in <<functions-point-cartesian-3d,Point(Cartesian-3D)>>
            |""".stripMargin)
        p(
          """
            |The units of the `x`, `y` and `z` fields are unspecified and can mean anything the user intends them to mean. This also means that when two cartesian
            |points are passed to the `distance` function, the resulting value will be in the same units as the original coordinates. This is true for both 2D and 3D
            |points, as the _pythagoras_ equation used is generalized to any number of dimensions. However, just as you cannot compare geographic points to cartesian
            |points, you cannot calculate the distance between a 2D point and a 3D point. If you need to do that, explicitly transform the one type into the other.
            |For example:
          """.stripMargin)
        query(
          """WITH point({x:3, y:0}) AS p2d, point({x:0, y:4, z:1}) as p3d
            |RETURN distance(p2d,p3d) as bad, distance(p2d,point({x:p3d.x, y:p3d.y})) as good""".stripMargin, ResultAssertions((r) => {
            withClue("Expect the invalid distance function to return null") {
              (r.head("bad") == null) should be(true)
            }
            withClue("Expect the valid distance function to contain the right value") {
              r.head("good") should be(5)
            }
          })) {
          resultTable()
        }
      }
    }
    section("Spatial instants","cypher-spatial-instants"){
      section("Creating points","cypher-spatial-specifying-spatial-instants"){
        p(
          """
            |All point types are created from two components:
            |
            |* The _Coordinate_ containing either 2 or 3 floating point values (64-bit)
            |* The Coordinate Reference System (or CRS) defining the meaning (and possibly units) of the values in the _Coordinate_
          """.stripMargin)
        p(
          """
            |For most use cases it is not necessary to specify the CRS explicitly as it will be deduced from the keys used to specify the coordinate. Two rules
            |are applied to deduce the CRS from the coordinate:
            |            |
            |* Choice of keys:
            |  ** If the coordinate is specified using the keys `latitude` and `longitude` the CRS will be assumed to be _Geographic_ and therefor either `WGS-84` or `WGS-84-3D`.
            |  ** If instead `x` and `y` are used, then the default CRS would be `Cartesian` or `Cartesian-3D`
            |* Number of dimensions:
            |  ** If there are 2 dimensions in the coordinate, `x` & `y` or `longitude` & `latitude` the CRS will be a 2D CRS
            |  ** If there is a third dimensions in the coordinate, `z` or `height` the CRS will be a 3D CRS
          """.stripMargin)
        p(
          """
            |All fields are provided to the `point` function in the form of a map of explicitly named arguments. We specifically do not support an ordered list
            |of coordinate fields because of the contradictory conventions between geographic and cartesian coordinates, where geographic coordinates normally
            |list `y` before `x` (`latitude` before `longitude`).
            |See for example the following query which returns points created in each of the four supported CRS. Take particular note of the order and keys
            |of the coordinates in the original `point` function calls, and how those values are displayed in the results:
          """.stripMargin)
        query(
          """RETURN
            |       point({x:3, y:0}) AS cartesian_2d, point({x:0, y:4, z:1}) as cartesian_3d,
            |       point({latitude: 12, longitude: 56}) AS geo_2d, point({latitude: 12, longitude: 56, height: 1000}) as geo_3d""".stripMargin, ResultAssertions((r) => {
            Map("cartesian_3d" -> 9157, "cartesian_2d" -> 7203, "geo_3d" -> 4979, "geo_2d" -> 4326).foreach { entry =>
              val field = entry._1
              val srid = entry._2
              withClue("Expect the point '$field' to have the SRID $srid") {
                val p = r.head(field).asInstanceOf[Point]
                p.getCRS.getCode should be(srid)
              }
            }
          })) {
          resultTable()
        }
        p(
          """
            |For the geographic coordinates, it is important to note that the `latitude` value should always lie in the interval `[-90, 90]` and any other value
            |outside this range will throw an exception. The `longitude` value should always lie in the interval `[-180, 180]` and any other value
            |outside this range will be wrapped around to fit in this range. The `height` value and any cartesian coordinates are not explicitly restricted,
            |and any value within the allowed range of the signed 64-bit floating point type will be accepted.
          """.stripMargin
        )
      }
      section("Accessing components of points", "cypher-spatial-accessing-components-spatial-instants"){
        p(
          """
            |Just as we construct points using a map syntax, we can also access components as properties of the instance.
          """.stripMargin)
        p(
          """
            |.Components of point instances and where they are supported
            |[options="header"]
            ||===
            || Component      | Description  | Type | Range/Format   | WGS-84 | WGS-84-3D | Cartesian | Cartesian-3D
            || `instant.x` | The first element of the _Coordinate_ | Float | Number literal, range depends on CRS | X | X | X | X
            || `instant.y` | The second element of the _Coordinate_ | Float | Number literal, range depends on CRS | X | X | X | X
            || `instant.z` | The third element of the _Coordinate_ | Float | Number literal, range depends on CRS |  | X |  | X
            || `instant.latitude` | The _second_ element of the _Coordinate_ for geographic CRS, degrees North of the equator | Float | Number literal, `-90.0` to `90.0` | X | X |   |
            || `instant.longitude` | The _first_ element of the _Coordinate_ for geographic CRS, degrees East of the prime meridian | Float | Number literal, `-180.0` to `180.0` | X | X |  |
            || `instant.height` | The third element of the _Coordinate_ for geographic CRS, meters above the ellipsoid defined by the datum (WGS-84) | Float | Number literal, range limited only by the underlying 64-bit floating point type |  | X |  |
            || `instant.crs` | The name of the CRS | String | One of `wgs-84`, `wgs-84-3d`, `cartesian`, `cartesian-3d` | X | X | X | X
            || `instant.srid` | The internal Neo4j ID for the CRS | Integer | One of `4326`, `4979`, `7203`, `9157` | X | X | X | X
            ||===
            |
            |""")
        p("The following query shows how to extract the components of a _Cartesian 2D_ point value:")
        query(
          """WITH point({x:3, y:4}) AS p
            |RETURN p.x, p.y, p.crs, p.srid""".stripMargin, ResultAssertions((r) => {
            withClue("Expect the components to have the right values") {
              r.head("p.crs") should be("cartesian")
              r.head("p.srid") should be(7203)
              r.head("p.x") should be(3)
              r.head("p.y") should be(4)
            }
          })) {
          resultTable()
        }
        p("The following query shows how to extract the components of a _WGS-84 3D_ point value:")
        query(
          """WITH point({latitude:3, longitude:4, height: 4321}) AS p
            |RETURN p.latitude, p.longitude, p.height, p.x, p.y, p.z, p.crs, p.srid""".stripMargin, ResultAssertions((r) => {
            withClue("Expect the components to have the right values") {
              r.head("p.crs") should be("wgs-84-3d")
              r.head("p.srid") should be(4979)
              r.head("p.x") should be(4)
              r.head("p.y") should be(3)
              r.head("p.z") should be(4321)
              r.head("p.latitude") should be(r.head("p.y"))
              r.head("p.longitude") should be(r.head("p.x"))
              r.head("p.height") should be(r.head("p.z"))
            }
          })) {
          resultTable()
        }
      }
    }
    section("Spatial index", "cypher-spatial-index") {
      p(
        """
          |If there is a <<administration-indexes-create-a-single-property-index,index>> on a particular `:Label(property)` combination, and a spatial point
          |is assigned to that property on a node with that label, the node will be indexed in a spatial index. For spatial indexing, Neo4j uses
          |space filling curves in 2D or 3D over an underlying generalized B+Tree. Points will be stored in up to four different trees, one for each of the
          |<<cypher-spatial-crs, four coordinate reference systems>>.
          |This allows for both <<administration-indexes-equality-check-using-where-single-property-index, equality>>
          |and <<administration-indexes-range-comparisons-using-where-single-property-index, range>> queries using exactly the same syntax and behaviour as for other property types.
          |If two range predicates are used, which define minimum and maximum points, this will effectively result in a
          |<<administration-indexes-spatial-bounding-box-searches-single-property-index, bounding box query>>.
          |In addition, queries using the `distance` function can, under the right conditions, also use the index, as described in the section
          |<<administration-indexes-spatial-distance-searches-single-property-index, 'Spatial distance searches'>>.
        """.stripMargin)
    }
    section("Comparability and Orderability", "cypher-comparability-orderability") {
      p(
        """
          |Points with different CRS are not comparable.
          |This means that any function operating on two points of different types will return `null`.
          |This is true of the <<functions-distance, distance function>> as well as inequality comparisons.
          |If these are used in a predicate, they will cause the associated `MATCH` to return no results.
        """.stripMargin
      )
      query(
        """WITH point({x:3, y:0}) AS p2d, point({x:0, y:4, z:1}) AS p3d
          |RETURN distance(p2d,p3d), p2d < p3d, p2d = p3d, p2d <> p3d, distance(p2d,point({x:p3d.x, y:p3d.y}))""".stripMargin, ResultAssertions(r => {
          r.nonEmpty should be(true)
          val record = r.head
          withClue("Expect the invalid distance function to return null") {
            (record("distance(p2d,p3d)") == null) should be(true)
          }
          withClue("Expect the inequality test to return null") {
            (record("p2d < p3d") == null) should be(true)
          }
          withClue("Expect the equality test to return false") {
            (record("p2d = p3d") == false) should be(true)
          }
          withClue("Expect the negative equality test to return true") {
            (record("p2d <> p3d") == true) should be(true)
          }
        })) {
        resultTable()
      }
      p(
        """
          |However, all types are orderable.
          |The Point types will be ordered after Numbers and before Temporal types.
          |Points with different CRS with be ordered by their SRID numbers.
          |For the current set of four <<cypher-spatial-crs, CRS>>, this means the order is WGS84, WGS84-3D, Cartesian, Cartesian-3D.
        """.stripMargin
      )
      query(
        """UNWIND [point({x:3, y:0}), point({x:0, y:4, z:1}), point({srid:4326, x:12, y:56}), point({srid:4979, x:12, y:56, z:1000})] AS point
          |RETURN point ORDER BY point""".stripMargin, ResultAssertions(r => {
          r.nonEmpty should be(true)
          val points = r.toList.map { p =>
            p("point").asInstanceOf[Point].getCRS.getCode
          }
          withClue("Expected four points") {
            points.length should be(4)
          }
          withClue("Expected ascending SRID order") {
            points should be(Seq(4326, 4979, 7203, 9157))
          }
        })) {
        resultTable()
      }
    }
  }.build()
}
