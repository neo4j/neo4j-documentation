package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.ResultAssertions
import org.scalatest.Matchers._

trait PrivilegesTestBase {
  def assertPrivilegeShown(expected: Seq[Map[String, AnyRef]]): ResultAssertions = ResultAssertions(p => {
    // Quite lenient method:
    // - Only checks specified rows, result can include more privileges that are not checked against
    // - Only checks specified fields, result can include more fields that are not checked against
    // - Only checks that the number of found matching rows are the same as number of expected rows

    // This can however lead to errors:
    // - If you specify too few fields you might match more than one privilege and the sizes will differ (ex. not specifying `segment` and finding both `NODE` and `RELATIONSHIP`)
    // - Will not fail when expecting ['existing matching 2 privileges', 'non-existing'], since we will find 2 privileges matching the first and none matching the second (so the sizes will be the same)
    val found = p.toList.filter { row =>
      val m = expected.filter { expectedRow =>
        expectedRow.forall {
          case (k, v) => row.contains(k) && row(k) == v
        }
      }
      m.nonEmpty
    }
    found.nonEmpty should be(true)
    found.size should equal(expected.size)
  })
}
