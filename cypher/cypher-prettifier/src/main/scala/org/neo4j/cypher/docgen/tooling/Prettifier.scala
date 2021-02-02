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

import scala.collection.mutable

sealed abstract class SyntaxToken {
  def text: String

  override def toString: String = text
}

sealed abstract class KeywordToken extends SyntaxToken {
  override def toString: String = text.toUpperCase
}

final case class BreakingKeywords(text: String) extends KeywordToken

final case class NonBreakingKeywords(text: String) extends KeywordToken

final case class GroupToken(start: String, close: String, innerTokens: Seq[SyntaxToken]) extends SyntaxToken {
  override def toString = s"$start${innerTokens.mkString(",")}$close"
  def text: String = toString
}

sealed abstract class GroupingText extends SyntaxToken

final case class OpenGroup(text: String) extends GroupingText

final case class CloseGroup(text: String) extends GroupingText

final case class EscapedText(text: String, quote: Char = '\"') extends SyntaxToken {
  override def toString = s"$quote$text$quote"
}

final case class AnyText(text: String) extends SyntaxToken

final case class NewlineToken(text: String) extends SyntaxToken

case object Comma extends SyntaxToken {
  override val text: String = ","
}

trait Prettifying {
  /**
    * @param input the input to prettify
    * @param keepMyNewlines if this is `true`the prettifier will keep original new lines
    */
  def apply(input: String, keepMyNewlines: Boolean): String
}

case object Prettifier extends Prettifying {

  override def apply(input: String, keepMyNewlines: Boolean = false): String = {
    val parser = new PrettifierParser(keepMyNewlines)
    val builder = new StringBuilder

    val parsedTokens = parser.parse(input)
    var tokens = flattenTokens(parsedTokens)

    while (tokens.nonEmpty) {
      val tail = tokens.tail
      builder ++= insertBreak(tokens.head, tail)
      tokens = tail
    }

    builder.toString()
  }

  def flattenTokens(tokens: Seq[SyntaxToken]): Seq[SyntaxToken] = {
    val tokenBuilder: mutable.Builder[SyntaxToken, Seq[SyntaxToken]] = Seq.newBuilder[SyntaxToken]
    flattenTokens(tokens, tokenBuilder)
    tokenBuilder.result()
  }

  def flattenTokens(tokens: Seq[SyntaxToken], tokenBuilder: mutable.Builder[SyntaxToken, Seq[SyntaxToken]]) {
    for (token <- tokens) {
      token match {
        case GroupToken(start, close, inner) =>
          tokenBuilder += OpenGroup(start)
          flattenTokens(inner, tokenBuilder)
          tokenBuilder += CloseGroup(close)
        case _ =>
          tokenBuilder += token
      }
    }
  }

  val space: String = " "
  val newline: String = System.lineSeparator()

  def insertBreak(token: SyntaxToken, tail: Seq[SyntaxToken]): String = {
    if (tail.isEmpty)
      token.toString
    else {
      (token, tail.head) match {
        // FOREACH : <NEXT>
        case (_: SyntaxToken,         _) if token.text.endsWith("|") => token.toString + space
        case (_: SyntaxToken,         _) if token.text.endsWith(":") => token.toString + space

        // don't put space or newline after or before a newline token
        case (_:NewlineToken,         _)                             => token.toString
        case (_,                      _:NewlineToken)                => token.toString

        // <NON-BREAKING-KW> <NEXT>
        case (_: NonBreakingKeywords, _:SyntaxToken)                 => token.toString + space

        // <HEAD> <BREAKING-KW>
        case (_:SyntaxToken,          _:BreakingKeywords)            => token.toString + newline

        // Never break between keywords
        case (_:KeywordToken,         _:KeywordToken)                => token.toString + space

        // <KW> <OPEN-GROUP>
        case (_:KeywordToken,         _:OpenGroup)                   => token.toString + space

        // <{> <NEXT>
        case (_@OpenGroup("{"),       _:SyntaxToken)                 => token.toString + space

        // <CLOSE-GROUP> <KW>
        case (_:CloseGroup,           _:KeywordToken)                => token.toString + space

        // <GROUPING> <NEXT>
        case (_:GroupingText,         _:SyntaxToken)                 => token.toString

        // <HEAD> <{>
        case (_:SyntaxToken,          OpenGroup("{"))                => token.toString + space

        // <HEAD> <}>
        case (_:SyntaxToken,          CloseGroup("}"))               => token.toString + space

        // <HEAD> <GROUPING>
        case (_:SyntaxToken,          _:GroupingText)                => token.toString

        // <HEAD> <COMMA>
        case (_:SyntaxToken,          Comma)                         => token.toString
        // default
        case _                                                       => token.toString + space
      }
    }
  }
}
