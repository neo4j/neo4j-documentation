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
package org.neo4j.cypher.docgen.tooling

import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.internal.v4_0.parser.BufferPosition
import org.neo4j.cypher.internal.v4_0.parser.InvalidInputErrorFormatter
import org.parboiled.errors.InvalidInputError
import org.parboiled.scala._
import org.scalatest.Assertions
import org.scalatest.FunSuiteLike
import org.scalatest.Matchers
import org.scalatest.Suite

trait ParserTestBase[T, J] extends Suite
                           with FunSuiteLike
                           with Assertions
                           with Matchers
                           with GraphIcing  {

  def convert(astNode: T): J

  def parsing(s: String)(implicit p: Rule1[T]): ResultCheck = ReportingParseRunner(p ~ EOI).run(s).result match {
    case Some(t) => new ResultCheck(Seq(convert(t)), s)
    case None => fail(s"'$s' failed with: " + ReportingParseRunner(p ~ EOI).run(s).parseErrors.map {
      case error: InvalidInputError =>
        val position = BufferPosition(error.getInputBuffer, error.getStartIndex)
        val message = new InvalidInputErrorFormatter().format(error)
        s"$message ($position)"
      case error =>
        error.getClass.getSimpleName
    }.mkString(","))
  }

  class ResultCheck(val actuals: Seq[J], text: String) {

    def shouldGive(expected: J) {
      actuals foreach {
        actual =>
          actual should equal(expected)
      }
    }

    override def toString: String = s"ResultCheck( $text -> $actuals )"
  }

}
