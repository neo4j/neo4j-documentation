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
package org.neo4j.cypher.internal

import java.util.{Map => JavaMap}

import org.neo4j.cypher.SyntaxException
import org.neo4j.cypher.internal.runtime.InternalExecutionResult
import org.neo4j.kernel.GraphDatabaseQueryService
import org.neo4j.kernel.impl.query.TransactionalContext
import org.neo4j.logging.{LogProvider, NullLogProvider}

class DocsExecutionEngine(graph: GraphDatabaseQueryService, logProvider: LogProvider = NullLogProvider.getInstance,
                          compatibilityFactory: CompatibilityFactory)
  extends ExecutionEngine(graph, logProvider, compatibilityFactory) {

  @throws(classOf[SyntaxException])
  def internalExecute(query: String, params: JavaMap[String, AnyRef], context: TransactionalContext): InternalExecutionResult =
    RewindableExecutionResult(execute(query, params, context))

  @throws(classOf[SyntaxException])
  def internalProfile(query: String, params: JavaMap[String, AnyRef], context: TransactionalContext): InternalExecutionResult =
    RewindableExecutionResult(profile(query, params, context))
}
