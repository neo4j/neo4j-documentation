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

import java.io

import org.neo4j.cypher.docgen.tooling.Admonitions.{Caution, Important, Note, Tip}
import org.neo4j.cypher.docgen.tooling.RunnableInitialization.InitializationFunction

import scala.collection.mutable

/**
 * DocBuilder allows for a stack based approach to building Documents,
 * instead of having to hand craft the Document tree
 */
trait DocBuilder {

  import DocBuilder._

  def build(): Document = {
    current match {
      case b: DocScope =>
        Document(b.title, b.id, b.init, b.content)
    }
  }

  private val scope = new mutable.Stack[Scope]

  private def current = scope.top

  def doc(name: String, id: String) {
    scope.push(DocScope(name, id))
  }

  def initCode(code: InitializationFunction): Unit = current.setInitCode(code)

  def initQueries(queries: String*): Unit = current.setInitQueries(queries.map{ query =>
    val mayBeDatabase: Option[String] = scope.collectFirst {
      case s: Scope if (s._database.isDefined) => s._database.get
    }
    InitializationQuery(query.stripMargin, database = mayBeDatabase)
  })

  def registerUserDefinedFunctions(udfs: java.lang.Class[_]*): Unit = current.setUserDefinedFunctions(udfs)

  def registerUserDefinedAggregationFunctions(udafs: java.lang.Class[_]*): Unit = current.setUserDefinedAggregationFunctions(udafs)

  def registerProcedures(procedures: java.lang.Class[_]*): Unit = current.setProcedures(procedures)

  def p(text: String): Unit = current.addContent(Paragraph(text.stripMargin))

  private def formattedSyntax(syntax: String) : String = {
    if (!syntax.isEmpty) "*Syntax:* `" + syntax + "`" else ""
  }

  def function(syntax: String, arguments: (String, String)*): Unit = {
    current.addContent(Function(formattedSyntax(syntax), "", arguments))
  }

  def function(syntax: String, returns: String, arguments: (String, String)*): Unit = {
    current.addContent(Function(formattedSyntax(syntax), returns, arguments))
  }

  def functionWithCypherStyleFormatting(syntax: String, arguments: (String, String)*): Unit = {
    val formattedSyntax = if (!syntax.isEmpty) Array("*Syntax:*", "[source, cypher, role=noplay]", syntax).mkString("\n", "\n", "") else ""
    current.addContent(Function(formattedSyntax, "", arguments))
  }

  def considerations(lines: String*): Unit = {
    current.addContent(Consideration(lines))
  }

  def enumTable(title: String, lines: (String, String)*): Unit = {
    current.addContent(EnumTable(title, lines))
  }

  def database(name: String): Unit = {
    scope.collectFirst {
      case section: SectionScope => section
      case doc: DocScope => doc
      case query: QueryScope => query
    }.get.setDatabase(name)
  }

  def runtime(name: String): Unit = {
    scope.collectFirst {
      case section: SectionScope => section
      case doc: DocScope => doc
      case query: QueryScope => query
    }.get.setRuntime(name)
  }

  def login(name: String, password: String): Unit = {
    scope.collectFirst {
      case section: SectionScope => section
      case doc: DocScope => doc
      case query: QueryScope => query
    }.get.setLogin((name, password))
  }

  def logout(): Unit = {
    scope.collectFirst {
      case section: SectionScope => section
      case doc: DocScope => doc
      case query: QueryScope => query
    }.get.unsetLogin()
  }

  def resultTable(): Unit = {
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new TablePlaceHolder(queryScope.assertions, queryScope.params:_*))
  }

  /** Print a smaller version of the result without changing the query.
   * Can limit the amount of printed rows and columns.
   * Will print the given columns in the given order.
   *
   * @param maybeWantedColumns the columns to be displayed, in the wanted display order.
   *                           If no column list is given, all columns will be displayed (this is default).
   * @param rows               the number of result rows to display
   */
  def limitedResultTable(rows: Int = 10, maybeWantedColumns: Option[List[String]] = None): Unit = {
    assert(rows > 0, "Cannot display less than one row")
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new LimitedTablePlaceHolder(maybeWantedColumns, rows, queryScope.assertions, queryScope.params: _*))
  }

  def statsOnlyResultTable(): Unit = {
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new StatsOnlyTablePlaceHolder(queryScope.assertions, queryScope.params:_*))
  }

  def errorOnlyResultTable(): Unit = {
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new ErrorOnlyTablePlaceHolder(queryScope.assertions, queryScope.params:_*))
  }

  def executionPlan(): Unit = {
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new ExecutionPlanPlaceHolder(queryScope.assertions))
  }

  def profileExecutionPlan(): Unit = {
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(new ProfileExecutionPlanPlaceHolder(queryScope.assertions))
  }

  def graphViz(options: String = ""): Unit = current.addContent(new GraphVizPlaceHolder(options))

  def consoleData(): Unit = {
    val docScope = scope.collectFirst {
      case d: DocScope => d
    }.get
    val queryScope = scope.collectFirst {
      case q: QueryScope => q
    }.get
    queryScope.addContent(ConsoleData(docScope.init.initQueries, queryScope.init.initQueries, queryScope.queryText))
  }

  def synopsis(text: String): Unit = current.addContent(Abstract(text))

  // Scopes
  private def inScope(newScope: Scope, f: => Unit): Unit = {
    scope.push(newScope)
    f
    val pop = popAndPopupateAttributes()
    current.addContent(pop.toContent)
  }

  def popAndPopupateAttributes(): Scope = {
    val mayBeDatabase: Option[String] = scope.collectFirst {
      case s: Scope if (s._database.isDefined) => s._database.get
    }
    val mayBeRuntime: Option[String] = scope.collectFirst {
      case s: Scope if (s._runtime.isDefined) => s._runtime.get
    }
    val mayBeLogin: Option[(String, String)] = scope.collectFirst {
      case s: Scope if (s._login.isDefined) => s._login.get
    }
    val pop = scope.pop()
    if (mayBeDatabase.isDefined) pop.setDatabase(mayBeDatabase.get)
    if (mayBeRuntime.isDefined) pop.setRuntime(mayBeRuntime.get)
    if (mayBeLogin.isDefined) pop.setLogin(mayBeLogin.get)
    pop
  }

  def section(title: String, id: String, role: String)(f: => Unit) = inScope(SectionScope(title, Some(id), Some(role)), f)
  def section(title: String, id: String)(f: => Unit) = inScope(SectionScope(title, Some(id)), f)
  def section(title: String)(f: => Unit) = inScope(SectionScope(title, None), f)

  def tip(f: => Unit) = inScope(AdmonitionScope(Tip.apply), f)
  def note(f: => Unit) = inScope(AdmonitionScope(Note.apply), f)
  def caution(f: => Unit) = inScope(AdmonitionScope(Caution.apply), f)
  def important(f: => Unit) = inScope(AdmonitionScope(Important.apply), f)

  def query(q: String, assertions: QueryAssertions, parameters: (String, Any)*)(innerBlockOfCode: => Unit): Unit =
    inScope(PreformattedQueryScope(q.stripMargin, assertions, parameters), {
      innerBlockOfCode
      consoleData() // Always append console data
    })
}

object DocBuilder {

  trait Scope {
    private var _initializations: RunnableInitialization = RunnableInitialization()
    private var _content: Content = NoContent
    var _database: Option[String] = None
    var _runtime: Option[String] = None
    var _login: Option[(String, String)] = None

    def init: RunnableInitialization = _initializations

    def content: Content = _content

    def setInitCode(code: InitializationFunction) {
      _initializations = _initializations.copy(initCode = _initializations.initCode :+ code)
    }

    def setInitQueries(queries: Seq[DatabaseQuery]) {
      _initializations = _initializations.copy(initQueries = _initializations.initQueries ++ queries)
    }

    def setUserDefinedFunctions(udfs: Seq[Class[_]]): Unit = {
      _initializations = _initializations.copy(userDefinedFunctions = _initializations.userDefinedFunctions ++ udfs)
    }

    def setUserDefinedAggregationFunctions(udafs: Seq[Class[_]]): Unit = {
      _initializations = _initializations.copy(
        userDefinedAggregationFunctions = _initializations.userDefinedAggregationFunctions ++ udafs)
    }

    def setProcedures(procedures: Seq[Class[_]]): Unit = {
      _initializations = _initializations.copy(
        procedures = _initializations.procedures ++ procedures)
    }

    def addContent(newContent: Content) {
      _content = _content match {
        case NoContent => newContent
        case _ => ContentChain(content, newContent)
      }
    }

    def setDatabase(database: String): Unit = {
      _database = Some(database)
    }

    def setRuntime(runtime: String): Unit = {
      _runtime = Some(runtime)
    }

    def setLogin(login: (String, String)): Unit = {
      _login = Some(login)
    }

    def unsetLogin(): Unit = {
      _login = None
    }

    def toContent: Content
  }

  case class DocScope(title: String, id: String) extends Scope {
    override def toContent = throw new LiskovSubstitutionPrincipleException
  }

  case class SectionScope(name: String, id: Option[String], role: Option[String] = None) extends Scope {
    override def toContent = Section(name, id, init, content, role)
  }

  case class AdmonitionScope(f: Content => Content) extends Scope {
    override def init = throw new LiskovSubstitutionPrincipleException

    override def toContent = f(content)
  }

  trait QueryScope extends Scope {
    def queryText: String
    def assertions: QueryAssertions
    def params: Seq[(String, Any)]
  }

  case class PreformattedQueryScope(queryText: String, assertions: QueryAssertions, params: Seq[(String, Any)]) extends QueryScope {
    override def toContent = Query(queryText, assertions, init, content, params, runtime = _runtime, database = _database, login = _login)
  }
}

class LiskovSubstitutionPrincipleException extends Exception
