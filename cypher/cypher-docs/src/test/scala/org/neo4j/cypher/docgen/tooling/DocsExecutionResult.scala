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

import org.neo4j.cypher.internal.plandescription.InternalPlanDescription
import org.neo4j.cypher.internal.result.string.ResultStringBuilder
import org.neo4j.cypher.internal.runtime._
import org.neo4j.graphdb.Result.ResultVisitor
import org.neo4j.graphdb.{Notification, Result}
import org.neo4j.kernel.impl.query.TransactionalContext

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
  * Helper class to ease asserting on cypher results from scala.
  *
  * This class was forked form the Neo4j repo RewindableExecutionResult, to remove the
  * test-dependency between the repositories.
  */
class DocsExecutionResult(val columns: Array[String],
                          result: Seq[Map[String, Any]],
                          val resultAsString: String,
                          planDescription: InternalPlanDescription,
                          statistics: QueryStatistics,
                          val notifications: Iterable[Notification]) {

  def columnAs[T](column: String): Iterator[T] = result.iterator.map(row => row(column).asInstanceOf[T])
  def toList: List[Map[String, Any]] = result.toList
  def toSet: Set[Map[String, Any]] = result.toSet
  def size: Long = result.size
  def head: Map[String, Any] = result.head

  def executionPlanDescription(): InternalPlanDescription = planDescription

  def executionPlanString(): String = planDescription.toString

  def queryStatistics(): QueryStatistics = statistics

  def isEmpty: Boolean = result.isEmpty
  def nonEmpty: Boolean = result.nonEmpty
}

object DocsExecutionResult {

  val scalaValues = new RuntimeScalaValueConverter(isGraphKernelResultValue)

  def apply(in: Result, txContext: TransactionalContext): DocsExecutionResult = {
    val resultStringBuilder = ResultStringBuilder(in.columns().toArray(new Array[String](0)), txContext)
    val result = new ArrayBuffer[Map[String, Any]]()
    val columns = in.columns().toArray(new Array[String](0))

    in.accept(new ResultVisitor[Exception] {
      override def visit(row: org.neo4j.graphdb.Result.ResultRow): Boolean = {
        resultStringBuilder.addRow(row)
        val map = new java.util.HashMap[String, Any]()
        for (c <- columns) {
          map.put(c, row.get(c))
        }
        result += scalaValues.asDeepScalaMap(map)
        true
      }
    })

    val statistics = in.getQueryStatistics match {
      case s:QueryStatistics => s
      case _ => QueryStatistics()
    }

    new DocsExecutionResult(
      columns,
      result.toList,
      resultStringBuilder.result(statistics),
      in.getExecutionPlanDescription.asInstanceOf[InternalPlanDescription],
      statistics,
      in.getNotifications.asScala
      )
  }
}
