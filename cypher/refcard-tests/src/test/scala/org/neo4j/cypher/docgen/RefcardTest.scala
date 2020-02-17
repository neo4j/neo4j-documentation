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
package org.neo4j.cypher.docgen

import java.io._
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

import org.apache.maven.artifact.versioning.ComparableVersion
import org.junit.{After, Before, Test}
import org.neo4j.cypher._
import org.neo4j.cypher.internal.compiler.v3_4.prettifier.Prettifier
import org.neo4j.cypher.internal.javacompat.{GraphDatabaseCypherService, GraphImpl}
import org.neo4j.cypher.internal.runtime.{InternalExecutionResult, RuntimeJavaValueConverter, isGraphKernelResultValue}
import org.neo4j.cypher.internal.{ExecutionEngine, RewindableExecutionResult}
import org.neo4j.doc.test.{GraphDatabaseServiceCleaner, GraphDescription, TestEnterpriseGraphDatabaseFactory, TestGraphDatabaseFactory}
import org.neo4j.graphdb._
import org.neo4j.graphdb.index.Index
import org.neo4j.internal.kernel.api.Transaction
import org.neo4j.kernel.impl.coreapi.PropertyContainerLocker
import org.neo4j.kernel.impl.query.Neo4jTransactionalContextFactory
import org.neo4j.kernel.impl.query.clientconnection.BoltConnectionInfo
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

  var db: GraphDatabaseCypherService = null
  implicit var engine: ExecutionEngine = null
  var nodes: Map[String, Long] = null
  var nodeIndex: Index[Node] = null
  var relIndex: Index[Relationship] = null
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
  def assert(name: String, result: InternalExecutionResult)
  def parameters(name: String): Map[String, Any] = Map()
  def graphDescription: List[String]
  def indexProps: List[String] = List()

  protected val baseUrl = System.getProperty("remote-csv-upload")
  var filePaths: Map[String, String] = Map.empty
  var urls: Map[String, String] = Map.empty

  def executeQuery(queryText: String, params: Map[String, Any])(implicit engine: ExecutionEngine): Result = try {
    val query = replaceNodeIds(queryText)

    assert(filePaths.size == urls.size)
    val testQuery = filePaths.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))
    val docQuery = urls.foldLeft(query)((acc, entry) => acc.replace(entry._1, entry._2))

    val fullQuerySnippet = AsciidocHelper.createCypherSnippetFromPreformattedQuery(Prettifier(docQuery), true)
    allQueriesWriter.append(fullQuerySnippet).append("\n\n")

    val contextFactory = Neo4jTransactionalContextFactory.create( db, new PropertyContainerLocker )
    val result = db.withTx(
      tx => engine.execute(testQuery, params,
        contextFactory.newContext(
          new BoltConnectionInfo(
            "username",
            "neo4j-java-bolt-driver",
            new InetSocketAddress("127.0.0.1", 56789),
            new InetSocketAddress("127.0.0.1", 7687)
          ),
          tx,
          testQuery,
          ValueUtils.asMapValue(javaValues.asDeepJavaMap(params).asInstanceOf[java.util.Map[String,AnyRef]])
        )
      ), Transaction.Type.`implicit` )
    result
  } catch {
    case e: CypherException => throw new InternalException(queryText, e)
  }

  def replaceNodeIds(_query: String): String = {
    var query = _query
    nodes.keySet.foreach((key) => query = query.replace("%" + key + "%", node(key).getId.toString))
    query
  }

  def indexProperties[T <: PropertyContainer](n: T, index: Index[T]) {
    indexProps.foreach((property) => {
      if (n.hasProperty(property)) {
        val value = n.getProperty(property)
        index.add(n, property, value)
      }
    })
  }

  def node(name: String): Node = graphOps.getNodeById(nodes.getOrElse(name, throw new NotFoundException(name)))

  def rel(id: Long): Relationship = graphOps.getRelationshipById(id)

  def text: String

  def expandQuery(query: String, queryPart: String, dir: File, possibleAssertion: Seq[String], parametersChoice: String) = {
    runQuery(query, possibleAssertion, parametersChoice)

    queryPart
  }

  def runQuery(query: String, possibleAssertion: Seq[String], parametersChoice: String): InternalExecutionResult = {
    val result =
      db.inTx {
        if (parametersChoice == null) {
          RewindableExecutionResult(executeQuery(query, Map.empty))
        } else {
          RewindableExecutionResult(executeQuery(query, parameters(parametersChoice)))
        }
      }

    db.inTx {
      possibleAssertion.foreach(name => {
        try {
          assert(name, result)
        } catch {
          case e: Exception => throw new RuntimeException("Test: %s\nQuery: %sParams: %s\n%s".format(name, query, parametersChoice, e.getMessage), e)
        }
      })

      result
    }
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
      writer.println("| link:{developer-manual-base-uri}/cypher/" + linkId + "[" + title + "]")
    } else {
      writer.println("|" + title)
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

          val rest = query.split("\n").tail.mkString("\n")
          val q = rest.replaceAll("#", "")
          val parts = q.split("\n\n")
          val publishPart = if (parts.length > 1) parts(1) else parts(0)
          producedText = producedText.replace(query, expandQuery(q, publishPart, dir, asserts, parameterChoice))
        }
    }

    producedText
  }

  @After
  def teardown() {
    if (db != null) db.shutdown()
    allQueriesWriter.close()
  }

  @Before
  def init() {
    dir = createDir(section)
    allQueriesWriter = new OutputStreamWriter(new FileOutputStream(new File("target/all-queries.asciidoc"), true),
      StandardCharsets.UTF_8)
    val graph = newTestGraphDatabaseFactory().newImpermanentDatabaseBuilder().newGraphDatabase()
    db = new GraphDatabaseCypherService(graph)

    GraphDatabaseServiceCleaner.cleanDatabaseContent(db.getGraphDatabaseService)

    db.inTx {
      nodeIndex = db.index().forNodes("nodeIndexName")
      relIndex = db.index().forRelationships("relationshipIndexName")
      val g = new GraphImpl(graphDescription.toArray[String])
      val description = GraphDescription.create(g)

      nodes = description.create(db.getGraphDatabaseService).asScala.map {
        case (name, node) => name -> node.getId
      }.toMap

      properties.foreach((n) => {
        val nod = node(n._1)
        n._2.foreach((kv) => nod.setProperty(kv._1, kv._2))
      })

      db.getAllNodes.asScala.foreach((n) => {
        indexProperties(n, nodeIndex)
        n.getRelationships(Direction.OUTGOING).asScala.foreach(indexProperties(_, relIndex))
      })
    }
    engine = ExecutionEngineFactory.createCommunityEngineFromDb(graph) // TODO: This should be using the EnterpriseEngine
  }

  protected def newTestGraphDatabaseFactory(): TestGraphDatabaseFactory = new TestEnterpriseGraphDatabaseFactory()
}

