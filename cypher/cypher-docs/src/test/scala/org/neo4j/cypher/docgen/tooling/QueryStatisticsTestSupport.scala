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
                  systemUpdates: Int = 0
                 ): Unit = {
    val expected =
      QueryStatistics(
        nodesCreated,
        relationshipsCreated,
        propertiesWritten,
        nodesDeleted,
        relationshipsDeleted,
        labelsAdded,
        labelsRemoved,
        indexesAdded,
        indexesRemoved,
        uniqueConstraintsAdded,
        uniqueConstraintsRemoved,
        existenceConstraintsAdded,
        existenceConstraintsRemoved,
        nodekeyConstraintsAdded,
        nodekeyConstraintsRemoved,
        systemUpdates
      )

    assertResult(expected)(result.queryStatistics())
  }
}
