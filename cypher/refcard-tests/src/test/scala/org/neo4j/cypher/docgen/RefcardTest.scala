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

import java.io._
import java.nio.charset.StandardCharsets
import java.util

import com.neo4j.configuration.OnlineBackupSettings
import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder
import org.apache.commons.io.FileUtils
import org.apache.maven.artifact.versioning.ComparableVersion
import org.junit.{After, Before, Test}
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.docgen.tooling.{DocsExecutionResult, Prettifier}
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, GraphImpl, ResultSubscriber}
import org.neo4j.cypher.internal.runtime.{RuntimeJavaValueConverter, isGraphKernelResultValue}
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.doc.test.{GraphDatabaseServiceCleaner, GraphDescription}
import org.neo4j.exceptions.{InternalException, Neo4jException}
import org.neo4j.graphdb._
import org.neo4j.graphdb.config.Setting
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.impl.coreapi.InternalTransaction
import org.neo4j.kernel.impl.query.Neo4jTransactionalContextFactory
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.visualization.asciidoc.AsciidocHelper
import org.scalatest.Assertions

import scala.collection.JavaConverters._

/*
Use this base class for refcard tests
 */
abstract class RefcardTest extends Assertions with DocumentationHelper with GraphIcing {

  private val javaValues = new RuntimeJavaValueConverter(isGraphKernelResultValue)

  val neo4jVersion: String = System.getenv("NEO4JVERSION")

  def versionFenceAllowsThisTest(featureVersion: String): Boolean = {
    // Unknown Neo4j version: run all tests
    if (null == neo4jVersion) {
      return true
    }
    // Neo4j version is greater than the version required by the test?
    new ComparableVersion(neo4jVersion).compareTo(new ComparableVersion(featureVersion)) > -1
  }

  var folder: File = _
  var managementService: DatabaseManagementService = null
  var db: GraphDatabaseCypherService = null
  implicit var engine: ExecutionEngine = null
  var nodes: Map[String, Long] = null
  val properties: Map[String, Map[String, Any]] = Map()
  var generateConsole: Boolean = true
  var dir: File = createDir(section)
  var allQueriesWriter: Writer = null

  // these 2 methods are need by ExecutionEngineHelper to do its job
  override def graph = db
  override def eengine = engine

  def graphOps: GraphDatabaseService = db.getGraphDatabaseService

  def title: String
  def linkId: String = null
  def section: String = "refcard"
  def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit
  def parameters(name: String): Map[String, Any] = Map()
  def graphDescription: List[String]
  def indexProps: List[String] = List()

  protected val baseUrl = System.getProperty("remote-csv-upload")
  var filePaths: Map[String, String] = Map.empty
  var urls: Map[String, String] = Map.empty

  def executeQuery(queryText: String, params: Map[String, Any])(implicit engine: ExecutionEngine): DocsExecutionResult = try {
    val docsResult = db.inTx(tx => {
      val query = replaceNodeIds(tx, queryText)

      assert(filePaths.size == urls.size)
      val testQuery = filePaths.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))
      val docQuery = urls.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))

      val fullQuerySnippet = AsciidocHelper.createCypherSnippetFromPreformattedQuery(Prettifier(docQuery), true)
      allQueriesWriter.append(fullQuerySnippet).append("\n\n")

      val contextFactory = Neo4jTransactionalContextFactory.create(db)
      val parameterValue = ValueUtils.asMapValue(javaValues.asDeepJavaMap(params).asInstanceOf[java.util.Map[String, AnyRef]])
      val txContext = contextFactory.newContext(tx, testQuery, parameterValue)
      val subscriber = new ResultSubscriber(txContext)
      val execution = engine.execute(testQuery,
        parameterValue,
        txContext,
        profile = false,
        prePopulate = false,
        subscriber)
      subscriber.init(execution)
      DocsExecutionResult(subscriber, txContext)
    }, KernelTransaction.Type.IMPLICIT)
    docsResult
  } catch {
    case e: Neo4jException => throw new InternalException(queryText, e)
  }

  def replaceNodeIds(tx:InternalTransaction, _query: String): String = {
    var query = _query
    nodes.keySet.foreach((key) => query = query.replace("%" + key + "%", node(tx, key).getId.toString))
    query
  }

  def node(tx:InternalTransaction, name: String): Node = tx.getNodeById(nodes.getOrElse(name, throw new NotFoundException(name)))

  def rel(tx:InternalTransaction, id: Long): Relationship = tx.getRelationshipById(id)

  def text: String

  def expandQuery(query: String, queryPart: String, dir: File, possibleAssertion: Seq[String], parametersChoice: String, doRun: Boolean): String = {
    if (doRun) runQuery(query, possibleAssertion, parametersChoice)

    queryPart
  }

  def runQuery(query: String, possibleAssertion: Seq[String], parametersChoice: String): DocsExecutionResult = {
    val result =
      if (parametersChoice == null) {
        executeQuery(query, Map.empty)
      } else {
        executeQuery(query, parameters(parametersChoice))
      }

    db.withTx( tx => {
      possibleAssertion.foreach(name => {
        try {
          assert(tx, name, result)
        } catch {
          case e: Exception => throw new RuntimeException("Test: %s\nQuery: %sParams: %s\n%s".format(name, query, parametersChoice, e.getMessage), e)
        }
      })

      result
    } )
  }

  @Test
  def produceDocumentation() {
    val writer: PrintWriter = createWriter(title, dir)
    val queryText = includeQueries(text.replaceAll("\r\n", "\n"), dir)
    val queryLines = queryText.split("\n\n")
    writer.println("[subs=attributes+]")
    writer.println("++++")
    writer.println("<div class='col card" + "{css}" +
      "\'><div class='blk'>")
    writer.println("++++")
    writer.println()
    writer.println("[options=\"header\"]")
    writer.println("|====")
    if (linkId != null) {
      writer.println("| " + title + " doclink:{cypher-manual-base-uri}/" + linkId + "[]")
    } else {
      writer.println("| " + title)
    }
    for (i <- 0 until queryLines.length by 2) {
      writer.print("a|[source, cypher")
      if (asciidocSubstitutions != null) {
        writer.print(", subs=" + asciidocSubstitutions)
      }
      writer.println("]")
      writer.println("----")
      def query = queryLines(i).trim().replace("|", "\\|")
      def docQuery = urls.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))
      writer.println(docQuery)
      writer.println("----")
      writer.println(queryLines(i + 1).trim())
    }
    writer.println("|====")
    writer.println()
    writer.println("++++")
    writer.println("</div></div>")
    writer.println("++++")
    writer.println()
    writer.close()
  }

  private def includeGraphviz(startText: String, dir: File): String = {
    val regex = "###graph-image(.*?)###".r
    regex.findFirstMatchIn(startText) match {
      case None => startText
      case Some(options) =>
        val optionString = options.group(1)
        val txt = startText.replaceAllLiterally("###graph-image" + optionString + "###",
          dumpGraphViz(dir, optionString.trim))
        txt
    }
  }

  val assertiongRegEx = "assertion=([^\\s]*)".r
  val parametersRegEx = "parameters=([^\\s]*)".r
  val dontRunRegEx = "dontrun".r

  private def includeQueries(query: String, dir: File) = {
    val startText = includeGraphviz(query, dir)
    val regex = "(?s)###(.*?)###".r
    val queries = (regex findAllIn startText).toList

    var producedText = startText
    queries.foreach {
      in =>
        {
          val query = in

          val firstLine = query.split("\n").head

          val asserts: Seq[String] = assertiongRegEx.findFirstMatchIn(firstLine).toSeq.flatMap(_.subgroups)
          val parameterChoice: String = parametersRegEx.findFirstMatchIn(firstLine).mkString("")
          val doRun: Boolean = dontRunRegEx.findFirstMatchIn(firstLine).isEmpty

          val rest = query.split("\n").tail.mkString("\n")
          val q = rest.replaceAll("#", "")
          val parts = q.split("\n\n")
          val publishPart = if (parts.length > 1) parts(1) else parts(0)
          producedText = producedText.replace(query, expandQuery(q, publishPart, dir, asserts, parameterChoice, doRun))
        }
    }

    producedText
  }

  @After
  def teardown() {
    if (managementService != null) {
      managementService.shutdown()
      FileUtils.deleteQuietly(folder)
    }
    allQueriesWriter.close()
  }

  @Before
  def init() {
    dir = createDir(section)
    allQueriesWriter = new OutputStreamWriter(new FileOutputStream(new File("target/all-queries.asciidoc"), true),
      StandardCharsets.UTF_8)
    folder = new File("target/example-db" + System.nanoTime())
    managementService = newDatabaseManagementService(folder)
    val graph = getGraph
    db = new GraphDatabaseCypherService(graph)

    cleanGraph

      val g = new GraphImpl(graphDescription.toArray[String])
      val description = GraphDescription.create(g)

      nodes = description.create(db.getGraphDatabaseService).asScala.map {
        case (name, node) => name -> node.getId
      }.toMap

    db.withTx( tx => {
      properties.foreach((n) => {
        val nod = node(tx, n._1)
        n._2.foreach((kv) => nod.setProperty(kv._1, kv._2))
      })

    } )
    engine = ExecutionEngineFactory.createExecutionEngineFromDb(graph)
  }

  // override to start against SYSTEM_DATABASE_NAME or another database
  protected def getGraph: GraphDatabaseService = managementService.database(DEFAULT_DATABASE_NAME)

  protected def cleanGraph: Unit = GraphDatabaseServiceCleaner.cleanDatabaseContent(db.getGraphDatabaseService)

  protected def newDatabaseManagementService(directory: File): DatabaseManagementService =
    new EnterpriseDatabaseManagementServiceBuilder(directory)
      .setConfig(databaseConfig())
      .build()

  protected def databaseConfig(): util.Map[Setting[_], Object] = {
    Map[Setting[_], Object](
      OnlineBackupSettings.online_backup_listen_address -> new SocketAddress("127.0.0.1", 0),
      OnlineBackupSettings.online_backup_enabled -> java.lang.Boolean.FALSE
    ).asJava
  }
}

