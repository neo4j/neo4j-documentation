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

import org.neo4j.cypher.internal.parser.Base
import org.neo4j.cypher.internal.parser.Strings
import org.neo4j.cypher.internal.parser.WSChar
import org.neo4j.exceptions.SyntaxException
import org.parboiled.scala._

class PrettifierParser(val keepMyNewlines: Boolean) extends Parser with Base with Strings {

  def main: Rule1[Seq[SyntaxToken]] = rule("main") {
    oneOrMoreExteriorToken | noTokens
  }

  def oneOrMoreExteriorToken: Rule1[Seq[SyntaxToken]] =
    rule("anyTokens") {
      oneOrMore(exteriorToken, whiteSpace)
    }

  def noTokens: Rule1[Seq[SyntaxToken]] = EMPTY ~ push(Seq.empty)

  def exteriorToken: Rule1[SyntaxToken] = {
    if (this.keepMyNewlines) {
      rule("anyToken") {
        anyKeywords | comma | escapedText | anyText | newline | grouping
      }
    } else {
      rule("anyToken") {
        anyKeywords | comma | escapedText | anyText | grouping
      }
    }
  }

  def whiteSpace: Rule0 =
    if (this.keepMyNewlines) nonBreakWS else WS

  def anyKeywords: Rule1[KeywordToken] = rule("anyKeywords") {
    nonBreakingKeyword | breakingKeywords
  }

  def nonBreakingKeyword: Rule1[NonBreakingKeywords] = rule("nonBreakingKeywords") {
    group(
      keyword("WITH HEADERS") |
        keyword("IS UNIQUE") |
        keyword("ALL") |
        keyword("NULL") |
        keyword("TRUE") |
        keyword("FALSE") |
        keyword("DISTINCT") |
        keyword("END") |
        keyword("NOT") |
        keyword("HAS") |
        keyword("ANY") |
        keyword("NONE") |
        keyword("SINGLE") |
        keyword("OR") |
        keyword("XOR") |
        keyword("AND") |
        keyword("AS") |
        keyword("IN") |
        keyword("IS") |
        keyword("UNIQUE") |
        keyword("BY") |
        keyword("ASSERT") |
        keyword("ASC") |
        keyword("DESC") |
        keyword("SCAN") |
        keyword("FROM") |
        keyword("EXISTS") |
        keyword("STARTS WITH") |
        keyword("ENDS WITH") |
        keyword("CONTAINS") |
        keyword("YIELD") |
        keyword("FOR") |
        keyword("IF NOT EXISTS") |
        keyword("IF EXISTS") |
        keyword("BRIEF OUTPUT") |
        keyword("BRIEF") |
        keyword("VERBOSE OUTPUT") |
        keyword("VERBOSE") |
        keyword("OPTIONS") |
        keyword("EACH")
    ) ~> NonBreakingKeywords
  }

  def breakingKeywords: Rule1[BreakingKeywords] = rule("breakingKeywords") {
    joinWithUpdatingBreakingKeywords |
      plainBreakingKeywords |
      updatingBreakingKeywords
  }

  def plainBreakingKeywords: Rule1[BreakingKeywords] = rule("plainBreakingKeywords") {
    group(
      keyword("LOAD CSV") |
        keyword("ORDER BY") |
        keyword("CREATE INDEX ON") | // Deprecated
        keyword("CREATE INDEX") | // These are for the named versions, sadly they will break the query on the ON keyword
        keyword("CREATE BTREE INDEX") | // Sadly these will break the query on the ON keyword
        keyword("CREATE LOOKUP INDEX") | // Sadly these will break the query on the ON keyword
        keyword("DROP INDEX") |
        showIndexBreakingKeyword |
        keyword("CREATE CONSTRAINT ON") |
        keyword("CREATE CONSTRAINT") | // These are for the named versions, sadly they will break the query on the ON keyword
        keyword("DROP CONSTRAINT") |
        showConstraintBreakingKeyword |
        keyword("USING PERIODIC COMMIT") |
        keyword("USING INDEX") |
        keyword("USING SCAN") |
        keyword("USING JOIN ON") |
        keyword("OPTIONAL MATCH") |
        keyword("DETACH DELETE") |
        keyword("START") |
        keyword("MATCH") |
        keyword("WHERE") |
        keyword("WITH") |
        keyword("RETURN") |
        keyword("SKIP") |
        keyword("LIMIT") |
        keyword("ORDER BY") |
        keyword("ASC") |
        keyword("DESC") |
        keyword("ON") |
        keyword("WHEN") |
        keyword("CASE") |
        keyword("THEN") |
        keyword("ELSE") |
        keyword("ASSERT") |
        keyword("SCAN") |
        keyword("CALL") |
        keyword("UNION") |
        keyword("UNWIND")
    ) ~> BreakingKeywords
  }

  private def showConstraintBreakingKeyword: Rule0 = rule("showConstraintBreakingKeyword") {
    keyword("SHOW CONSTRAINT") |
    keyword("SHOW CONSTRAINTS") |
    keyword("SHOW UNIQUE CONSTRAINT") |
    keyword("SHOW UNIQUE CONSTRAINTS") |
    keyword("SHOW NODE KEY CONSTRAINT") |
    keyword("SHOW NODE KEY CONSTRAINTS") |
    keyword("SHOW NODE EXIST CONSTRAINT") |
    keyword("SHOW NODE EXIST CONSTRAINTS") |
    keyword("SHOW NODE EXISTS CONSTRAINT") |
    keyword("SHOW NODE EXISTS CONSTRAINTS") |
    keyword("SHOW NODE EXISTENCE CONSTRAINT") |
    keyword("SHOW NODE EXISTENCE CONSTRAINTS") |
    keyword("SHOW NODE PROPERTY EXIST CONSTRAINT") |
    keyword("SHOW NODE PROPERTY EXIST CONSTRAINTS") |
    keyword("SHOW NODE PROPERTY EXISTENCE CONSTRAINT") |
    keyword("SHOW NODE PROPERTY EXISTENCE CONSTRAINTS") |
    keyword("SHOW RELATIONSHIP EXIST CONSTRAINT") |
    keyword("SHOW RELATIONSHIP EXIST CONSTRAINTS") |
    keyword("SHOW RELATIONSHIP EXISTS CONSTRAINT") |
    keyword("SHOW RELATIONSHIP EXISTS CONSTRAINTS") |
    keyword("SHOW RELATIONSHIP EXISTENCE CONSTRAINT") |
    keyword("SHOW RELATIONSHIP EXISTENCE CONSTRAINTS") |
    keyword("SHOW RELATIONSHIP PROPERTY EXIST CONSTRAINT") |
    keyword("SHOW RELATIONSHIP PROPERTY EXIST CONSTRAINTS") |
    keyword("SHOW RELATIONSHIP PROPERTY EXISTENCE CONSTRAINT") |
    keyword("SHOW RELATIONSHIP PROPERTY EXISTENCE CONSTRAINTS") |
    keyword("SHOW REL EXIST CONSTRAINT") |
    keyword("SHOW REL EXIST CONSTRAINTS") |
    keyword("SHOW REL EXISTENCE CONSTRAINT") |
    keyword("SHOW REL EXISTENCE CONSTRAINTS") |
    keyword("SHOW REL PROPERTY EXIST CONSTRAINT") |
    keyword("SHOW REL PROPERTY EXIST CONSTRAINTS") |
    keyword("SHOW REL PROPERTY EXISTENCE CONSTRAINT") |
    keyword("SHOW REL PROPERTY EXISTENCE CONSTRAINTS") |
    keyword("SHOW EXIST CONSTRAINT") |
    keyword("SHOW EXIST CONSTRAINTS") |
    keyword("SHOW EXISTS CONSTRAINT") |
    keyword("SHOW EXISTS CONSTRAINTS") |
    keyword("SHOW EXISTENCE CONSTRAINT") |
    keyword("SHOW EXISTENCE CONSTRAINTS") |
    keyword("SHOW PROPERTY EXIST CONSTRAINT") |
    keyword("SHOW PROPERTY EXIST CONSTRAINTS") |
    keyword("SHOW PROPERTY EXISTENCE CONSTRAINT") |
    keyword("SHOW PROPERTY EXISTENCE CONSTRAINTS") |
    keyword("SHOW ALL CONSTRAINT") |
    keyword("SHOW ALL CONSTRAINTS")
  }

  private def showIndexBreakingKeyword: Rule0 = rule("showIndexBreakingKeyword") {
    keyword("SHOW INDEX") |
    keyword("SHOW INDEXES") |
    keyword("SHOW ALL INDEX") |
    keyword("SHOW ALL INDEXES") |
    keyword("SHOW BTREE INDEX") |
    keyword("SHOW BTREE INDEXES") |
    keyword("SHOW FULLTEXT INDEX") |
    keyword("SHOW FULLTEXT INDEXES") |
    keyword("SHOW LOOKUP INDEX") |
    keyword("SHOW LOOKUP INDEXES")
  }

  def joinWithUpdatingBreakingKeywords: Rule1[BreakingKeywords] =
    group(joinedBreakingKeywords ~~ updatingBreakingKeywords) ~~> ((k1: BreakingKeywords, k2: BreakingKeywords) =>
      BreakingKeywords(s"${k1.text} ${k2.text}")
      )

  def joinedBreakingKeywords: Rule1[BreakingKeywords] = group(keyword("ON CREATE") | keyword("ON MATCH")) ~> BreakingKeywords

  def updatingBreakingKeywords: Rule1[BreakingKeywords] = rule("breakingUpdateKeywords") {
    group(
      keyword("CREATE") |
        keyword("SET") |
        keyword("DELETE") |
        keyword("REMOVE") |
        keyword("FOREACH") |
        keyword("MERGE")
    ) ~> BreakingKeywords
  }

  def comma: Rule1[Comma.type] = rule("comma") {
    "," ~> (_ => Comma)
  }

  def escapedText: Rule1[EscapedText] = rule("string") {
    (((
      ch('\'') ~ StringCharacters('\'') ~ ch('\'') ~ push('\'')
        | ch('"') ~ StringCharacters('"') ~ ch('"') ~ push('\"')
      ) memoMismatches) suppressSubnodes) ~~> EscapedText
  }

  def anyText: Rule1[AnyText] = rule("anyText") {
    oneOrMore((!anyOf(" \n\r\t\f(){}[]")) ~ ANY) ~> AnyText
  }

  def newline: Rule1[NewlineToken] = rule("newline") {
    anyOf("\n\r") ~> NewlineToken
  }

  def grouping: Rule1[GroupToken] = rule("grouping") {
    validGrouping("(", ")") | validGrouping("{", "}") | validGrouping("[", "]")
  }

  def validGrouping(start: String, close: String): Rule1[GroupToken] =
    group(start ~ optional(whiteSpace) ~ zeroOrMoreInteriorToken ~ optional(whiteSpace) ~ close) ~~> ((innerTokens: Seq[SyntaxToken]) => GroupToken(start, close, innerTokens))

  def zeroOrMoreInteriorToken: Rule1[Seq[SyntaxToken]] = zeroOrMore(interiorToken, whiteSpace)

  def interiorToken: Rule1[SyntaxToken] =
    rule("interiorToken") {
      interiorNonBreakingKeywords | exteriorToken
    }

  def interiorNonBreakingKeywords: Rule1[NonBreakingKeywords] = rule("interiorNonBreakingKeywords") {
    keyword("WHERE") ~> NonBreakingKeywords
  }
  def nonBreakWS: Rule0 = rule("whitespace") {
    zeroOrMore(
      (oneOrMore(!anyOf("\n\r") ~ WSChar) memoMismatches)
        | (ch('/').label("comment") ~ (
        ch('*') ~ zeroOrMore(!"*/" ~ ANY) ~ "*/"
          | ch('/') ~ zeroOrMore(!anyOf("\n\r") ~ ANY) ~ (optional(ch('\r')) ~ ch('\n') | EOI)
        ) memoMismatches)
    )
  }.suppressNode

  def parse(input: String): Seq[SyntaxToken] = {
    val runner = parserunners.ReportingParseRunner(main)
    val v = runner.run(input)
    v match {
      case output: ParsingResult[_] if output.matched => output.result.get
      case output: ParsingResult[Seq[SyntaxToken]] => throw new SyntaxException(output.parseErrors.mkString("\n"))
    }
  }
}
