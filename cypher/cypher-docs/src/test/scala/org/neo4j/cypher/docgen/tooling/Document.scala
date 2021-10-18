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

import org.neo4j.cypher.docgen.tooling.RunnableInitialization.InitializationFunction
import org.neo4j.cypher.example.JavaExecutionEngineDocTest
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.kernel.GraphDatabaseQueryService
import org.neo4j.cypher.internal.util.Eagerly
import org.neo4j.exceptions.InternalException
import org.neo4j.graphdb.Result
import org.neo4j.internal.kernel.api.security.LoginContext
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.test.DoubleLatch

import scala.collection.JavaConverters._
import scala.concurrent.Future


case class ContentWithInit(init: RunnableInitialization, query: Option[DatabaseQuery], queryResultPlaceHolder: QueryResultPlaceHolder) {

  assert(init.initQueries.nonEmpty || query.nonEmpty, "Should never produce ContentWithInit with empty queries")

  // In the DSL it is assumed that the last init query will be presented if the place holder is not inside an explicit query,
  // e.g. a graphViz() to show the graph created by initQueries().
  // The key is used to group queries by their initialization requirements (to minimize database creations by the query runner)
  val initKey: RunnableInitialization = if (query.nonEmpty) init else init.copy(initQueries = init.initQueries.dropRight(1))
  val queryToPresent: DatabaseQuery = if (query.isDefined) query.get else init.initQueries.last
}

object  RunnableInitialization {
  type InitializationFunction = GraphDatabaseCypherService => Unit

  def empty = RunnableInitialization()

  def apply(initQueries: Seq[String], userDefinedFunctions: Seq[Class[_]]): RunnableInitialization =
    new RunnableInitialization(initQueries = initQueries.map(InitializationQuery(_)), userDefinedFunctions = userDefinedFunctions)

  def apply(initQueries: Seq[String]): RunnableInitialization =
    new RunnableInitialization(initQueries = initQueries.map(InitializationQuery(_)))
}

case class RunnableInitialization(initCode: Seq[InitializationFunction] = Seq.empty,
                                  initQueries: Seq[DatabaseQuery] = Seq.empty,
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

  def NewLine: String = if (System.getProperty("os.name").toLowerCase.startsWith("windows")) "\r\n" else "\n"

  def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]): Seq[ContentWithInit]
}

trait NoQueries {
  self: Content =>
  override def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]) = Seq.empty
}

case object NoContent extends Content with NoQueries {
  override def asciiDoc(level: Int) = ""
}

case class ContentChain(a: Content, b: Content) extends Content {
  override def asciiDoc(level: Int) = a.asciiDoc(level) + b.asciiDoc(level)

  override def toString: String = s"$a ~ $b"

  override def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]): Seq[ContentWithInit] =
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

case class EnumTable(title: String, entries: Seq[(String, String)]) extends Content with NoQueries {
  override def asciiDoc(level: Int) = {
    val entryLines = entries.map(x => "| `" + x._1 + "` | " + x._2).mkString("", NewLine, "")
    val formattedLines = Array("*" + title + ":*", "[options=\"header\"]", "|===", entryLines, "|===").mkString(NewLine, NewLine, "")
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

    val header = if (rows.nonEmpty) "header," else ""
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

  private def escape(in: String): String = if (in.isEmpty) "" else "+%s+".format(in)
}

trait SimpleQueryResultTable extends Content with NoQueries {
  val role: String
  val text: String
  override def asciiDoc(level: Int): String = {
    // Remove trailing white space, then add <space>+ at the end of all rows (except the last one)
    val formattedText = text.replaceAll("\\s+$", "").replaceAllLiterally("\n", " +\n")

    s"""[role="$role"]
       |$formattedText
       |
       |""".stripMargin
  }
}

case class StatsOnlyQueryResultTable(text: String) extends SimpleQueryResultTable {
  val role: String = "statsonlyqueryresult"
}

case class ErrorOnlyQueryResultTable(text: String) extends SimpleQueryResultTable {
  val role: String = "erroronlyqueryresult"
}

trait DatabaseQuery {
  def prettified: String
  def database: Option[String]
  def runtime: Option[String]
  def login: Option[(String, String)]
  def explain: DatabaseQuery = InitializationQuery(s"EXPLAIN $runnable", runtime, database, login)
  def profile: DatabaseQuery = InitializationQuery(s"PROFILE $runnable", runtime, database, login)
  def runnable: String = if(runtime.isDefined) s"CYPHER runtime=${runtime.get} ${prettified}" else prettified
  def before(dbms: RestartableDatabase): Unit = ()
  def after(dbms: RestartableDatabase): Unit = ()
}

case class InitializationQuery(prettified: String,
                               runtime: Option[String] = None,
                               database: Option[String] = None,
                               login: Option[(String, String)] = None) extends DatabaseQuery

trait ContentQuery extends Content with DatabaseQuery {
  val params: Seq[(String, Any)]
  val content: Content
  val myInit: RunnableInitialization
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

  override def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]) =
    content.runnableContent(init ++ myInit, queryText = Some(this))

  private def mapMapValue(v: Any): Any = v match {
    case v: Map[_, _] => Eagerly.immutableMapValues(v, mapMapValue).asJava
    case seq: Seq[_]  => seq.map(mapMapValue).asJava
    case v: Any       => v
  }
}

case class Query(prettified: String,
                 assertions: QueryAssertions,
                 override val myInit: RunnableInitialization,
                 override val content: Content,
                 override val params: Seq[(String, Any)],
                 runtime: Option[String] = None,
                 database: Option[String] = None,
                 login: Option[(String, String)] = None) extends ContentQuery {
}

case class ShowTransactionsQuery( beforeQueryText: List[String],
                 prettified: String,
                 assertions: QueryAssertions,
                 override val myInit: RunnableInitialization,
                 override val content: Content,
                 override val params: Seq[(String, Any)],
                 runtime: Option[String] = None,
                 database: Option[String] = None,
                 login: Option[(String, String)] = None) extends ContentQuery {

  private val latch: DoubleLatch = new DoubleLatch(beforeQueryText.size + 1)

  override def before(dbms: RestartableDatabase): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.before(dbms)
    val graph = dbms.graph

    beforeQueryText.foreach(query =>
      // Start each background transaction in a Future
      Future {
        val tx = graph.beginTransaction(KernelTransaction.Type.EXPLICIT, LoginContext.AUTH_DISABLED)

        try {
          var result: Result = null
          try {
            result = tx.execute(query)
            latch.startAndWaitForAllToStart()
          } finally {
            latch.start()
            latch.finishAndWaitForAllToFinish()
          }
          if (result != null) {
            result.accept((_: Result.ResultRow) => true)
            result.close()
          }
          tx.commit()
        } catch {
          case t: Throwable => t
        } finally {
          if (tx != null) tx.close()
        }
      })

    // Start the main query
    latch.startAndWaitForAllToStart()
  }

  override def after(dbms: RestartableDatabase): Unit = {
    super.after(dbms)
    latch.finishAndWaitForAllToFinish()
  }

}

object ConsoleData {
  def of(globalInitQueries: Seq[String], localInitQueries: Seq[String], query: String): ConsoleData =
    ConsoleData(globalInitQueries.map(InitializationQuery(_)), localInitQueries.map(InitializationQuery(_)), query)
}

case class ConsoleData(globalInitQueries: Seq[DatabaseQuery], localInitQueries: Seq[DatabaseQuery], query: String) extends Content with NoQueries {
  override def asciiDoc(level: Int): String = {
    val globalInitQueryRows = globalInitQueries.map(_.prettified).mkString(NewLine)
    val localInitQueryRows = localInitQueries.map(_.prettified).mkString(NewLine)
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

case class Section(heading: String, id: Option[String], init: RunnableInitialization, content: Content, role: Option[String] = None) extends Content {

  override def asciiDoc(level: Int) = {
    val roleRef = role.map("[role=" + _ + "]\n").getOrElse("")
    val idRef = id.map("[[" + _ + "]]\n").getOrElse("")
    val levelIndent = (0 to (level + 1)).map(_ => "=").mkString
    roleRef + idRef + levelIndent + " " + heading + NewLine + NewLine + content.asciiDoc(level + 1)
  }

  override def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]): Seq[ContentWithInit] = content.runnableContent(init ++ this.init, None)
}

sealed trait QueryAssertions

case class ResultAssertions(f: DocsExecutionResult => Unit) extends QueryAssertions

case class ResultAndDbAssertions(f: (DocsExecutionResult, GraphDatabaseQueryService) => Unit) extends QueryAssertions

case class ErrorAssertions(f: Throwable => Unit) extends QueryAssertions

case object NoAssertions extends QueryAssertions

// These objects are used to mark where in the document tree
// dynamic content should be inserted
trait QueryResultPlaceHolder {
  self: Content =>
  override def asciiDoc(level: Int) =
    throw new InternalException(s"This object should have been rewritten away already ${this.getClass.getSimpleName}")
  override def runnableContent(init: RunnableInitialization, queryText: Option[DatabaseQuery]) = Seq(ContentWithInit(init, queryText, this))
}

// NOTE: These must _not_ be case classes, otherwise they will not be compared by identity
class TablePlaceHolder(val assertions: QueryAssertions, val params: (String, Any)*) extends Content with QueryResultPlaceHolder
class LimitedTablePlaceHolder(val maybeWantedColumns: Option[List[String]], val rows: Int, assertions: QueryAssertions, params: (String, Any)*) extends TablePlaceHolder(assertions, params: _*)
class StatsOnlyTablePlaceHolder(assertions: QueryAssertions, params: (String, Any)*) extends TablePlaceHolder(assertions, params: _*)
class ErrorOnlyTablePlaceHolder(assertions: QueryAssertions, params: (String, Any)*) extends TablePlaceHolder(assertions, params: _*)
class GraphVizPlaceHolder(val options: String) extends Content with QueryResultPlaceHolder
class ErrorPlaceHolder() extends Content with QueryResultPlaceHolder
class ExecutionPlanPlaceHolder(val assertions: QueryAssertions) extends Content with QueryResultPlaceHolder
class ProfileExecutionPlanPlaceHolder(val assertions: QueryAssertions) extends Content with QueryResultPlaceHolder
