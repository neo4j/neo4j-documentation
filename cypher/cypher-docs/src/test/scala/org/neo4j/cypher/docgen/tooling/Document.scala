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

import org.neo4j.cypher.docgen.tooling.RunnableInitialization.InitializationFunction
import org.neo4j.cypher.example.JavaExecutionEngineDocTest
import org.neo4j.cypher.internal.compiler.v3_4.prettifier.Prettifier
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.runtime.InternalExecutionResult
import org.neo4j.cypher.internal.util.v3_4.{Eagerly, InternalException}
import org.neo4j.kernel.GraphDatabaseQueryService

import scala.collection.JavaConverters._


case class ContentWithInit(init: RunnableInitialization, queryText: Option[String], queryResultPlaceHolder: QueryResultPlaceHolder) {

  assert(init.initQueries.nonEmpty || queryText.nonEmpty, "Should never produce ContentWithInit with empty queries")

  // In the DSL it is assumed that the last init query will be presented if the place holder is not inside an explicit query,
  // e.g. a graphViz() to show the graph created by initQueries().
  // The key is used to group queries by their initialization requirements (to minimize database creations by the query runner)
  val initKey: RunnableInitialization = if (queryText.nonEmpty) init else init.copy(initQueries = init.initQueries.dropRight(1))
  val queryToPresent: String = if (queryText.isDefined) queryText.get else init.initQueries.last
}

object  RunnableInitialization {
  type InitializationFunction = GraphDatabaseCypherService => Unit

  def empty = RunnableInitialization()
}

case class RunnableInitialization(initCode: Seq[InitializationFunction] = Seq.empty,
                                  initQueries: Seq[String] = Seq.empty,
                                  userDefinedFunctions: Seq[java.lang.Class[_]] = Seq.empty,
                                  userDefinedAggregationFunctions: Seq[java.lang.Class[_]] = Seq.empty,
                                  procedures: Seq[java.lang.Class[_]] = Seq.empty) {

  def ++(other: RunnableInitialization): RunnableInitialization = {
    RunnableInitialization(
      initCode ++ other.initCode,
      initQueries ++ other.initQueries,
      userDefinedFunctions ++ other.userDefinedFunctions,
      userDefinedAggregationFunctions ++ other.userDefinedAggregationFunctions,
      procedures ++ other.procedures
    )
  }
}

case class Document(title: String, id: String,
                    private val init: RunnableInitialization,
                    content: Content) {

  def asciiDoc =
      s"""[[$id]]
         |= $title
         |
         |""".stripMargin + content.asciiDoc(0)

  def contentWithQueries: Seq[ContentWithInit] = content.runnableContent(init, None)
}

sealed trait Content {
  def ~(other: Content): Content = ContentChain(this, other)

  def asciiDoc(level: Int): String

  def NewLine: String = "\n"

  def runnableContent(init: RunnableInitialization, queryText: Option[String]): Seq[ContentWithInit]
}

trait NoQueries {
  self: Content =>
  override def runnableContent(init: RunnableInitialization, queryText: Option[String]) = Seq.empty
}

case object NoContent extends Content with NoQueries {
  override def asciiDoc(level: Int) = ""
}

case class ContentChain(a: Content, b: Content) extends Content {
  override def asciiDoc(level: Int) = a.asciiDoc(level) + b.asciiDoc(level)

  override def toString: String = s"$a ~ $b"

  override def runnableContent(init: RunnableInitialization, queryText: Option[String]): Seq[ContentWithInit] =
    a.runnableContent(init, queryText) ++ b.runnableContent(init, queryText)
}

case class Abstract(s: String) extends Content with NoQueries {
  override def asciiDoc(level: Int) =
    s"""[abstract]
       |--
       |$s
       |--
       |
       |""".stripMargin
}

case class Heading(s: String) extends Content with NoQueries {
  override def asciiDoc(level: Int) = "." + s + NewLine
}

case class Paragraph(s: String) extends Content with NoQueries {
  override def asciiDoc(level: Int) = s + NewLine + NewLine
}

case class Function(syntax: String, returns: String, arguments: Seq[(String, String)]) extends Content with NoQueries {
  override def asciiDoc(level: Int) = {
    val args = arguments.map(x => "| `" + x._1 + "` | " + x._2).mkString("", NewLine, "")
    val formattedReturn = if (!returns.isEmpty) Array("*Returns:*", "|===", "|", returns, "|===").mkString(NewLine, NewLine, "") else ""
    val formattedArguments = if(arguments.nonEmpty) Array("*Arguments:*", "[options=\"header\"]", "|===", "| Name | Description", args, "|===").mkString(NewLine, NewLine, "") else ""
    String.format(
      """%s
        |%s%n
        |%s%n
        |""".stripMargin, syntax, formattedReturn, formattedArguments)
  }

}

case class Consideration(lines: Seq[(String)]) extends Content with NoQueries {
  override def asciiDoc(level: Int) = {
    val items = lines.map(x => "|" + x).mkString("", NewLine, "")
    val formattedLines = if(lines.nonEmpty) Array("*Considerations:*", "|===", items, "|===").mkString(NewLine, NewLine, "") else ""
    String.format(
      """%s%n
        |""".stripMargin, formattedLines)
  }

}

object Admonitions {

  object Tip {
    def apply(s: Content) = new Tip(None, s)

    def apply(heading: String, s: Content) = new Tip(Some(heading), s)
  }

  case class Tip(heading: Option[String], innerContent: Content) extends Admonitions

  object Warning {
    def apply(s: Content) = new Warning(None, s)

    def apply(heading: String, s: Content) = new Warning(Some(heading), s)
  }

  case class Warning(heading: Option[String], innerContent: Content) extends Admonitions

  object Note {
    def apply(s: Content) = new Note(None, s)

    def apply(heading: String, s: Content) = new Note(Some(heading), s)
  }

  case class Note(heading: Option[String], innerContent: Content) extends Admonitions

  object Caution {
    def apply(s: Content) = new Caution(None, s)

    def apply(heading: String, s: Content) = new Caution(Some(heading), s)
  }

  case class Caution(heading: Option[String], innerContent: Content) extends Admonitions

  object Important {
    def apply(s: Content) = new Important(None, s)

    def apply(heading: String, s: Content) = new Important(Some(heading), s)
  }

  case class Important(heading: Option[String], innerContent: Content) extends Admonitions {
    override def name = "IMPORTANT"
  }

}

trait Admonitions extends Content with NoQueries {
  def innerContent: Content

  def heading: Option[String]

  def name: String = this.getClass.getSimpleName.toUpperCase

  override def asciiDoc(level: Int) = {
    val inner = innerContent.asciiDoc(level)
    val head = heading.map("." + _ + NewLine).getOrElse("")

    s"[$name]" + NewLine + head +
      s"""====
         |$inner
          |====
          |
          |""".
        stripMargin
  }
}

case class ResultRow(values: Seq[String])

case class QueryResultTable(columns: Seq[String], rows: Seq[ResultRow], footer: String) extends Content with NoQueries {
  override def asciiDoc(level: Int): String = {

    val header = if (columns.nonEmpty) "header," else ""
    val cols = if (columns.isEmpty) 1 else columns.size
    val rowsOutput: String = if (rows.isEmpty) s"$cols+|(empty result)"
    else {
      val columnHeader = columns.map(escape).mkString("| ", " | ", "")
      val tableRows =
        rows.
          map(row => row.values.map(escape).mkString("|| ", " | ", "")).
          mkString("\n")

      s"$columnHeader\n$tableRows"
    }

    // Remove trailing white space, then add <space>+ at the end of all rows (except the last one)
    val footerRows = footer.replaceAll("\\s+$", "").replaceAllLiterally("\n", " +\n")

    s""".Result
       |[role="queryresult",options="${header}footer",cols="$cols*<m"]
       ||===
       |$rowsOutput
       |$cols+d|$footerRows
       ||===
       |
       |""".stripMargin
  }

  private def escape(in: String): String = "+%s+".format(in)
}

case class Query(queryText: String, assertions: QueryAssertions, myInit: RunnableInitialization, content: Content, params: Seq[(String, Any)]) extends Content {

  val prettified = Prettifier(queryText)
  val parameterText: String = if (params.isEmpty) "" else JavaExecutionEngineDocTest.parametersToAsciidoc(mapMapValue(params.toMap))

  override def asciiDoc(level: Int) = {
    s"""$parameterText
       |.Query
       |[source, cypher]
       |----
       |$prettified
       |----
       |
       |""".stripMargin + content.asciiDoc(level)
  }

  override def runnableContent(init: RunnableInitialization, queryText: Option[String]) =
    content.runnableContent(init ++ myInit, queryText = Some(prettified))

  private def mapMapValue(v: Any): Any = v match {
    case v: Map[_, _] => Eagerly.immutableMapValues(v, mapMapValue).asJava
    case seq: Seq[_]  => seq.map(mapMapValue).asJava
    case v: Any       => v
  }
}

case class ConsoleData(globalInitQueries: Seq[String], localInitQueries: Seq[String], query: String) extends Content with NoQueries {
  override def asciiDoc(level: Int): String = {
    val globalInitQueryRows = globalInitQueries.mkString(NewLine)
    val localInitQueryRows = localInitQueries.mkString(NewLine)
    val initQueries =
      if (globalInitQueryRows.isEmpty && localInitQueryRows.isEmpty)
          "none"
        else
          globalInitQueryRows + "\n" + localInitQueryRows
    s"""ifndef::nonhtmloutput[]
       |[subs="none"]
       |++++
       |<formalpara role="cypherconsole">
       |<title>Try this query live</title>
       |<para><database><![CDATA[
       |$initQueries
       |]]></database><command><![CDATA[
       |$query
       |]]></command></para></formalpara>
       |++++
       |endif::nonhtmloutput[]
       |
       |""".stripMargin
  }
}

case class GraphViz(s: String) extends Content with NoQueries {
  override def asciiDoc(level: Int) = s + NewLine + NewLine
}

case class ExecutionPlan(planString: String) extends Content with NoQueries {
  override def asciiDoc(level: Int) = {
    s".Query plan\n[source]\n----\n$planString\n----\n\n"
  }
}

case class Section(heading: String, id: Option[String], init: RunnableInitialization, content: Content) extends Content {

  override def asciiDoc(level: Int) = {
    val idRef = id.map("[[" + _ + "]]\n").getOrElse("")
    val levelIndent = (0 to (level + 1)).map(_ => "=").mkString
    idRef + levelIndent + " " + heading + NewLine + NewLine + content.asciiDoc(level + 1)
  }

  override def runnableContent(init: RunnableInitialization, queryText: Option[String]): Seq[ContentWithInit] = content.runnableContent(init ++ this.init, None)
}

sealed trait QueryAssertions

case class ResultAssertions(f: InternalExecutionResult => Unit) extends QueryAssertions

case class ResultAndDbAssertions(f: (InternalExecutionResult, GraphDatabaseQueryService) => Unit) extends QueryAssertions

case object NoAssertions extends QueryAssertions

// These objects are used to mark where in the document tree
// dynamic content should be inserted
trait QueryResultPlaceHolder {
  self: Content =>
  override def asciiDoc(level: Int) =
    throw new InternalException(s"This object should have been rewritten away already ${this.getClass.getSimpleName}")
  override def runnableContent(init: RunnableInitialization, queryText: Option[String]) = Seq(ContentWithInit(init, queryText, this))
}

// NOTE: These must _not_ be case classes, otherwise they will not be compared by identity
class TablePlaceHolder(val assertions: QueryAssertions, val params: (String, Any)*) extends Content with QueryResultPlaceHolder
class GraphVizPlaceHolder(val options: String) extends Content with QueryResultPlaceHolder
class ErrorPlaceHolder() extends Content with QueryResultPlaceHolder
class ExecutionPlanPlaceHolder(val assertions: QueryAssertions) extends Content with QueryResultPlaceHolder
class ProfileExecutionPlanPlaceHolder(val assertions: QueryAssertions) extends Content with QueryResultPlaceHolder
