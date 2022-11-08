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

import com.neo4j.configuration.EnterpriseEditionInternalSettings
import com.neo4j.configuration.OnlineBackupSettings
import com.neo4j.configuration.SecuritySettings
import org.apache.maven.artifact.versioning.ComparableVersion
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.TestEnterpriseDatabaseManagementServiceBuilder
import org.neo4j.cypher.docgen.tooling.CypherPrettifier
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.javacompat.GraphImpl
import org.neo4j.cypher.internal.javacompat.ResultSubscriber
import org.neo4j.cypher.internal.runtime.RuntimeJavaValueConverter
import org.neo4j.cypher.internal.runtime.isGraphKernelResultValue
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.doc.test.GraphDatabaseServiceCleaner
import org.neo4j.doc.test.GraphDescription
import org.neo4j.exceptions.InternalException
import org.neo4j.exceptions.Neo4jException
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.NotFoundException
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.config.Setting
import org.neo4j.io.fs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.api.KernelTransaction
import org.neo4j.kernel.impl.coreapi.InternalTransaction
import org.neo4j.kernel.impl.query.Neo4jTransactionalContextFactory
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.string.SecureString
import org.neo4j.test.AsyncDatabaseOperation
import org.neo4j.test.utils.TestDirectory
import org.neo4j.visualization.asciidoc.AsciidocHelper
import org.scalatest.Assertions

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.jdk.CollectionConverters.MapHasAsScala

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
  var managementService: DatabaseManagementService = _
  private var database: GraphDatabaseService = _
  var db: GraphDatabaseCypherService = _
  implicit var engine: ExecutionEngine = _
  var nodes: Map[String, Long] = _
  val properties: Map[String, Map[String, Any]] = Map()
  var generateConsole: Boolean = true
  var dir: File = createDir(section)
  var allQueriesWriter: Writer = _

  // these 2 methods are need by ExecutionEngineHelper to do its job
  override def graph: GraphDatabaseCypherService = db
  override def eengine: ExecutionEngine = engine

  def title: String
  def linkId: String = null
  def section: String = "refcard"
  def assert(tx: Transaction, name: String, result: DocsExecutionResult): Unit
  def parameters(name: String): Map[String, Any] = Map()
  def graphDescription: List[String]
  def indexProps: List[String] = List()

  protected val baseUrl: String = System.getProperty("remote-csv-upload")
  var filePaths: Map[String, String] = Map.empty
  var urls: Map[String, String] = Map.empty

  def executeQuery(queryText: String, params: Map[String, Any])(implicit engine: ExecutionEngine): DocsExecutionResult =
    try {
      val docsResult = db.inTx(
        tx => {
          val query = replaceNodeIds(tx, queryText)

          assert(filePaths.size == urls.size)
          val testQuery = filePaths.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))
          val docQuery = urls.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))

          val fullQuerySnippet =
            AsciidocHelper.createCypherSnippetFromPreformattedQuery(CypherPrettifier(docQuery), true)
          allQueriesWriter.append(fullQuerySnippet).append("\n\n")

          val contextFactory = Neo4jTransactionalContextFactory.create(db)
          val parameterValue =
            ValueUtils.asMapValue(javaValues.asDeepJavaMap(params).asInstanceOf[java.util.Map[String, AnyRef]])
          val txContext = contextFactory.newContext(tx, testQuery, parameterValue)
          val subscriber = new ResultSubscriber(txContext)
          val execution =
            engine.execute(testQuery, parameterValue, txContext, profile = false, prePopulate = false, subscriber)
          subscriber.init(execution)
          DocsExecutionResult(subscriber, txContext)
        },
        KernelTransaction.Type.IMPLICIT
      )
      docsResult
    } catch {
      case e: Neo4jException => throw new InternalException(queryText, e)
    }

  def replaceNodeIds(tx: InternalTransaction, _query: String): String = {
    var query = _query
    nodes.keySet.foreach(key => query = query.replace("%" + key + "%", node(tx, key).getId.toString))
    query
  }

  def node(tx: InternalTransaction, name: String): Node =
    tx.getNodeById(nodes.getOrElse(name, throw new NotFoundException(name)))

  def rel(tx: InternalTransaction, id: Long): Relationship = tx.getRelationshipById(id)

  def text: String

  def expandQuery(
    query: String,
    queryPart: String,
    possibleAssertion: Seq[String],
    parametersChoice: String,
    doRun: Boolean
  ): String = {
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

    db.withTx(tx => {
      possibleAssertion.foreach(name => {
        try {
          assert(tx, name, result)
        } catch {
          case e: Exception => throw new RuntimeException(
              "Test: %s\nQuery: %sParams: %s\n%s".format(name, query, parametersChoice, e.getMessage),
              e
            )
        }
      })

      result
    })
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
        val txt =
          startText.replaceAllLiterally("###graph-image" + optionString + "###", dumpGraphViz(dir, optionString.trim))
        txt
    }
  }

  private val assertiongRegEx = "assertion=([^\\s]*)".r
  private val parametersRegEx = "parameters=([^\\s]*)".r
  private val dontRunRegEx = "dontrun".r

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
          producedText = producedText.replace(query, expandQuery(q, publishPart, asserts, parameterChoice, doRun))
        }
    }

    producedText
  }

  @AfterEach
  def teardown() {
    if (managementService != null) {
      managementService.shutdown()
    }
    allQueriesWriter.close()
  }

  @BeforeEach
  def init() {
    dir = createDir(section)
    allQueriesWriter = new OutputStreamWriter(
      new FileOutputStream(new File("target/all-queries.asciidoc"), true),
      StandardCharsets.UTF_8
    )
    val fs = new EphemeralFileSystemAbstraction()
    val td = TestDirectory.testDirectory(this.getClass, fs)
    folder = td.prepareDirectoryForTest("target/example-db" + System.nanoTime()).toFile
    managementService = new TestEnterpriseDatabaseManagementServiceBuilder(folder.toPath)
      .setFileSystem(fs)
      .setConfig(databaseConfig())
      .build()
    database = getGraph
    db = new GraphDatabaseCypherService(database)

    cleanGraph()

    val g = new GraphImpl(graphDescription.toArray[String])
    val description = GraphDescription.create(g)

    nodes = description.create(database).asScala.map {
      case (name, node) => name -> node.getId
    }.toMap

    db.withTx(tx => {
      properties.foreach(n => {
        val nod = node(tx, n._1)
        n._2.foreach(kv => nod.setProperty(kv._1, kv._2))
      })

    })
    engine = ExecutionEngineFactory.getExecutionEngine(database)
  }

  // override to start against SYSTEM_DATABASE_NAME or another database
  protected def getGraph: GraphDatabaseService =
    AsyncDatabaseOperation.findDatabaseEventually(managementService, DEFAULT_DATABASE_NAME)

  protected def cleanGraph(): Unit = GraphDatabaseServiceCleaner.cleanDatabaseContent(database)

  protected def databaseConfig(): util.Map[Setting[_], Object] = {
    Map[Setting[_], Object](
      EnterpriseEditionInternalSettings.replication_enabled -> java.lang.Boolean.FALSE,
      OnlineBackupSettings.online_backup_listen_address -> new SocketAddress("127.0.0.1", 0),
      OnlineBackupSettings.online_backup_enabled -> java.lang.Boolean.FALSE,
      SecuritySettings.keystore_path -> java.nio.file.Path.of(
        getClass.getClassLoader.getResource("keystore_11_0_5.pkcs12").toURI
      ),
      SecuritySettings.keystore_password -> new SecureString("test24"),
      SecuritySettings.key_name -> "256bitkey"
    ).asJava
  }
}
