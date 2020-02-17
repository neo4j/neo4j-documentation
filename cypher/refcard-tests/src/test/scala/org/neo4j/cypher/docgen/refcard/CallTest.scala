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

import org.junit.Before
import org.neo4j.collection.RawIterator
import org.neo4j.cypher.QueryStatisticsTestSupport
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.internal.runtime.InternalExecutionResult
import org.neo4j.internal.kernel.api.exceptions.ProcedureException
import org.neo4j.internal.kernel.api.procs.Neo4jTypes
import org.neo4j.internal.kernel.api.procs.ProcedureSignature._
import org.neo4j.kernel.api.{InwardKernel, ResourceTracker}
import org.neo4j.kernel.api.proc.CallableProcedure.BasicProcedure
import org.neo4j.kernel.api.proc.Context

class CallTest extends RefcardTest with QueryStatisticsTestSupport {

  val graphDescription = List("ROOT KNOWS A:Person", "A KNOWS B:Person", "B KNOWS C:Person", "C KNOWS ROOT")
  val title = "CALL"
  override val linkId = "clauses/call"

  @Before
  override def init() {
    super.init()

    val kernel = db.getDependencyResolver.resolveDependency(classOf[InwardKernel])
    val builder = procedureSignature(Array("java", "stored"), "procedureWithArgs")
      .in("input", Neo4jTypes.NTString)
      .out("result", Neo4jTypes.NTString)

    val proc = new BasicProcedure(builder.build) {
      override def apply(ctx: Context, input: Array[AnyRef], resourceTracker: ResourceTracker): RawIterator[Array[AnyRef], ProcedureException] =
        RawIterator.of[Array[AnyRef], ProcedureException](input)
    }
    kernel.registerProcedure(proc)
  }

  override def assert(name: String, result: InternalExecutionResult) {
    name match {
      case "labels" =>
        assert(result.toList.size === 1)
      case "arg" =>
        assert(result.toList.size === 1)
        assert(result.toList == List(Map("result" ->"foo")))
      case "none" =>
    }
  }

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=arg" => Map("input" ->"foo")
      case "" => Map.empty
    }

  def text = """
### assertion=labels
//

CALL db.labels() YIELD label
###

This shows a standalone call to the built-in procedure `db.labels` to list all labels used in the database.
Note that required procedure arguments are given explicitly in brackets after the procedure name.

### assertion=arg parameters=arg
//

CALL java.stored.procedureWithArgs
###

Standalone calls may omit `YIELD` and also provide arguments implicitly via statement parameters, e.g. a standalone call requiring one argument `input` may be run by passing the parameter map `{input: 'foo'}`.

### assertion=labels
//

CALL db.labels() YIELD label
RETURN count(label) AS count
###

Calls the built-in procedure `db.labels` inside a larger query to count all labels used in the database.
Calls inside a larger query always requires passing arguments and naming results explicitly with `YIELD`.
"""
}

