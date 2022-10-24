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

import org.neo4j.cypher.docgen.tooling.ResultAssertions
import org.scalatest.matchers.should.Matchers._

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
