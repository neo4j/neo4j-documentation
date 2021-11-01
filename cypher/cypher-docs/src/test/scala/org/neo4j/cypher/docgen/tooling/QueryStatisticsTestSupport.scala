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

import org.neo4j.cypher.internal.runtime.QueryStatistics
import org.scalatest.Assertions
import org.scalatest.mock.MockitoSugar

/**
  * This class was forked form the Neo4j repo QueryStatisticsTestSupport, to remove the
  * test-dependency between the repositories.
  */
trait QueryStatisticsTestSupport extends MockitoSugar {
  self: Assertions =>

  def assertStats(result: DocsExecutionResult,
                  nodesCreated: Int = 0,
                  relationshipsCreated: Int = 0,
                  propertiesWritten: Int = 0,
                  nodesDeleted: Int = 0,
                  relationshipsDeleted: Int = 0,
                  labelsAdded: Int = 0,
                  labelsRemoved: Int = 0,
                  indexesAdded: Int = 0,
                  indexesRemoved: Int = 0,
                  uniqueConstraintsAdded: Int = 0,
                  uniqueConstraintsRemoved: Int = 0,
                  existenceConstraintsAdded: Int = 0,
                  existenceConstraintsRemoved: Int = 0,
                  nodekeyConstraintsAdded: Int = 0,
                  nodekeyConstraintsRemoved: Int = 0,
                  namedConstraintsRemoved: Int = 0,
                  systemUpdates: Int = 0
                 ): Unit = {
    val expected =
      QueryStatistics(
        nodesCreated = nodesCreated,
        relationshipsCreated = relationshipsCreated,
        propertiesSet = propertiesWritten,
        nodesDeleted = nodesDeleted,
        relationshipsDeleted = relationshipsDeleted,
        labelsAdded = labelsAdded,
        labelsRemoved = labelsRemoved,
        indexesAdded = indexesAdded,
        indexesRemoved = indexesRemoved,
        uniqueConstraintsAdded = uniqueConstraintsAdded,
        uniqueConstraintsRemoved = uniqueConstraintsRemoved,
        existenceConstraintsAdded = existenceConstraintsAdded,
        existenceConstraintsRemoved = existenceConstraintsRemoved,
        nodekeyConstraintsAdded = nodekeyConstraintsAdded,
        nodekeyConstraintsRemoved = nodekeyConstraintsRemoved,
        namedConstraintsRemoved = namedConstraintsRemoved,
        systemUpdates = systemUpdates,
      )

    assertResult(expected)(result.queryStatistics())
  }
}
