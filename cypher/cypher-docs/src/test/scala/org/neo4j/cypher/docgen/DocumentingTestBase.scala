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

import com.neo4j.configuration.OnlineBackupSettings
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Before
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.example.JavaExecutionEngineDocTest
import org.neo4j.cypher.export.DatabaseSubGraph
import org.neo4j.cypher.export.SubGraphExporter
import org.neo4j.cypher.internal.ExecutionEngine
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService
import org.neo4j.cypher.internal.javacompat.GraphImpl
import org.neo4j.cypher.internal.javacompat.ResultSubscriber
import org.neo4j.cypher.internal.runtime.RuntimeJavaValueConverter
import org.neo4j.cypher.internal.runtime.isGraphKernelResultValue
import org.neo4j.cypher.internal.util.Eagerly
import org.neo4j.cypher.ExecutionEngineHelper
import org.neo4j.cypher.GraphIcing
import org.neo4j.cypher.TestEnterpriseDatabaseManagementServiceBuilder
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.neo4j.doc.test.GraphDatabaseServiceCleaner.cleanDatabaseContent
import org.neo4j.doc.test.GraphDescription
import org.neo4j.doc.tools.AsciiDocGenerator
import org.neo4j.exceptions.Neo4jException
import org.neo4j.graphdb._
import org.neo4j.graphdb.config.Setting
import org.neo4j.internal.kernel.api.security.SecurityContext
import org.neo4j.io.fs.EphemeralFileSystemAbstraction
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.impl.api.index.IndexingService
import org.neo4j.kernel.impl.api.index.IndexSamplingMode
import org.neo4j.kernel.impl.coreapi.InternalTransaction
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade
import org.neo4j.kernel.impl.query.Neo4jTransactionalContextFactory
import org.neo4j.kernel.impl.query.QuerySubscriber
import org.neo4j.kernel.impl.query.QuerySubscriberAdapter
import org.neo4j.kernel.impl.util.ValueUtils
import org.neo4j.test.utils.TestDirectory
import org.neo4j.values.virtual.VirtualValues
import org.neo4j.visualization.asciidoc.AsciidocHelper
import org.neo4j.visualization.graphviz.AsciiDocStyle
import org.neo4j.visualization.graphviz.GraphStyle
import org.neo4j.visualization.graphviz.GraphvizWriter
import org.neo4j.walk.Walker
import org.scalatest.junit.JUnitSuite

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

trait DocumentationHelper extends GraphIcing with ExecutionEngineHelper {
  def generateConsole: Boolean
  def db: GraphDatabaseCypherService

  def niceify(parent: Option[String], section: String, title: String): String =
    if (parent.isDefined) niceify(parent.get + " " + section + " " + title) else niceify(section + " " + title)

  def niceify(in: String): String = in.toLowerCase
    .replace("(★) ", "")
    .replace(" ", "-")
    .replace("(", "")
    .replace(")", "")
    .replace("`", "")
    .replace("/", "-")

  def simpleName: String = this.getClass.getSimpleName.replaceAll("Test", "").toLowerCase

  def createDir(folder: String): File = createDir(path, folder)

  def createDir(where: File, folder: String): File = {
    val dir = new File(where, niceify(folder))
    if (!dir.exists()) {
      dir.mkdirs()
    }
    dir
  }

  def createWriter(title: String, dir: File): PrintWriter = {
    new PrintWriter(new File(dir, niceify(title) + ".asciidoc"), StandardCharsets.UTF_8.name())
  }

  def asciidocSubstitutions: String = null
  def createCypherSnippet(query: String): String = {
    //val escapedQuery = query.trim().replace("\\", "\\\\")
    val escapedQuery = query.trim()
    //Prettifier is broken and not maintained.
    //val prettifiedQuery = Prettifier(escapedQuery, keepMyNewlines = false)
    if (asciidocSubstitutions != null) {
      AsciidocHelper.createCypherSnippetFromPreformattedQueryWithCustomSubstitutions(escapedQuery, true, asciidocSubstitutions)
    } else {
      AsciidocHelper.createCypherSnippetFromPreformattedQuery(escapedQuery, true)
    }
  }

  def prepareFormatting(query: String): String = {
    //val str = Prettifier(query.trim(), keepMyNewlines = false)
    val str = query.trim()
    if ((str takeRight 1) == ";") {
      str
    } else {
      str + ";"
    }
  }

  def dumpSetupQueries(queries: List[String], dir: File) {
    dumpQueries(queries, dir, simpleName + "-setup")
  }

  def dumpSetupConstraintsQueries(queries: List[String], dir: File) {
    dumpQueries(queries, dir, simpleName + "-setup-constraints")
  }

  def dumpPreparationQueries(queries: List[String], dir: File, testid: String) {
    dumpQueries(queries, dir, simpleName + "-" + niceify(testid) + ".preparation")
  }

  private def dumpQueries(queries: List[String], dir: File, testid: String): String = {
    if (queries.isEmpty) {
      ""
    } else {
      val queryStrings = queries.map(prepareFormatting)
      val output = AsciidocHelper.createCypherSnippetFromPreformattedQuery(queryStrings.mkString("\n"), true)
      AsciiDocGenerator.dumpToSeparateFile(dir, testid, output)
    }
  }

  def path: File = new File("target/docs/dev/ql/")

  val graphvizFileName = "cypher-" + simpleName + "-graph"

  def dumpGraphViz(dir: File, graphVizOptions: String): String = {
    emitGraphviz(dir, graphvizFileName, graphVizOptions)
  }

  def dumpPreparationGraphviz(dir: File, testid: String, graphVizOptions: String): String = {
    emitGraphviz(dir, simpleName + "-" + niceify(testid) + ".preparation-graph", graphVizOptions)
  }

  private def emitGraphviz(dir: File, testid: String, graphVizOptions: String): String = {
    val out = new ByteArrayOutputStream()
    val writer = new GraphvizWriter(getGraphvizStyle)

    db.withTx(tx => {
      writer.emit(out, Walker.fullGraph(tx))
    })

    val graphOutput = """["dot", "%s.svg", "neoviz", "%s"]
----
%s
----

""".format(testid, graphVizOptions, out)
    ".Graph\n" + AsciiDocGenerator.dumpToSeparateFile(dir, testid, graphOutput)
  }

  protected def getGraphvizStyle: GraphStyle = AsciiDocStyle.withAutomaticRelationshipTypeColors()

}

abstract class DocumentingTestBase extends JUnitSuite with DocumentationHelper with ResetStrategy {

  private val javaValues = new RuntimeJavaValueConverter(isGraphKernelResultValue)

  def testQuery(title: String,
                text: String,
                queryText: String,
                optionalResultExplanation: String = null,
                parameters: Map[String, Any] = Map.empty,
                planners: Seq[String] = Seq.empty,
                assertions: DocsExecutionResult => Unit): Unit = {

    internalTestQuery(title, text, queryText, optionalResultExplanation, None, None, parameters, planners, assertions)
  }

  def testFailingQuery[T <: Exception: ClassTag](title: String, text: String, queryText: String, optionalResultExplanation: String = null) {
    val classTag = implicitly[ClassTag[T]]
    internalTestQuery(title, text, queryText, optionalResultExplanation, Some(classTag), None, Map.empty, Seq.empty, _ => {})
  }

  def prepareAndTestQuery(title: String, text: String, queryText: String, optionalResultExplanation: String = "",
                          prepare: GraphDatabaseCypherService => Unit, assertions: DocsExecutionResult => Unit) {
    internalTestQuery(title, text, queryText, optionalResultExplanation, None, Some(prepare), Map.empty, Seq.empty, assertions)
  }

  def profileQuery(title: String,
                   text: String,
                   queryText: String,
                   realQuery: Option[String] = None,
                   prepare: Option[GraphDatabaseCypherService => Unit] = None,
                   assertions: DocsExecutionResult => Unit) {
    internalProfileQuery(title, text, queryText, realQuery, None, prepare, assertions)
  }

  private def internalProfileQuery(title: String,
                                   text: String,
                                   queryText: String,
                                   realQuery: Option[String],
                                   expectedException: Option[ClassTag[_ <: Neo4jException]],
                                   prepare: Option[GraphDatabaseCypherService => Unit],
                                   assertions: DocsExecutionResult => Unit) {
    preparationQueries = List()

    dumpSetupConstraintsQueries(setupConstraintQueries, dir)
    dumpSetupQueries(setupQueries, dir)

    val consoleData: String = "none"

    val keySet = nodeMap.keySet
    val writer: PrintWriter = createWriter(title, dir)
    prepareForTest(title, prepare)

    val query = db.withTx(tx => {
      keySet.foldLeft(realQuery.getOrElse(queryText)) {
        (acc, key) => acc.replace("%" + key + "%", node(tx, key).getId.toString)
      }
    })

    try {
      db.withTx({ tx =>
        val txContext = graph.transactionalContext(tx, query = query -> Map())
        val queryResult = DocsExecutionResult(tx.execute("PROFILE " + query), txContext)

        if (expectedException.isDefined) {
          fail(s"Expected the test to throw an exception: $expectedException")
        }

        val testId = niceify(parent, section, title)
        writer.println("[[" + testId + "]]")
        if (!noTitle) writer.println("== " + title + " ==")
        writer.println(text)
        writer.println()

        val output = new StringBuilder(2048)
        output.append(".Query\n")
        output.append(createCypherSnippet(query))
        writer.println(AsciiDocGenerator.dumpToSeparateFile(dir, testId + ".query", output.toString()))
        writer.println()
        writer.println()

        writer.append(".Query Plan\n")
        writer.append(AsciidocHelper.createOutputSnippet(queryResult.executionPlanDescription().toString))

        writer.flush()
        writer.close()

        assertions(queryResult)
      } )
    } catch {
      case e: Neo4jException if expectedException.nonEmpty =>
        val expectedExceptionType = expectedException.get
        e match {
          case expectedExceptionType(typedE) =>
            dumpToFileWithException(dir, writer, title, query, "", text, typedE, consoleData, Map.empty)
          case _ => fail(s"Expected an exception of type $expectedException but got ${e.getClass}", e)
        }
    }
  }


  private def internalTestQuery(title: String,
                                text: String,
                                queryText: String,
                                optionalResultExplanation: String,
                                expectedException: Option[ClassTag[_ <: Exception]],
                                prepare: Option[GraphDatabaseCypherService => Unit],
                                parameters: Map[String, Any],
                                planners: Seq[String],
                                assertions: DocsExecutionResult => Unit)
  {
    preparationQueries = List()
    //dumpGraphViz(dir, graphvizOptions.trim)
    if (!graphvizExecutedAfter) {
      dumpGraphViz(dir, graphvizOptions.trim)
    }
    dumpSetupConstraintsQueries(setupConstraintQueries, dir)
    dumpSetupQueries(setupQueries, dir)

    var consoleData: String = ""
    if (generateConsole) {
      if (generateInitialGraphForConsole) {
        val out = new StringWriter()
        db.withTx( tx=> {
          new SubGraphExporter(DatabaseSubGraph.from(tx)).export(new PrintWriter(out))
          consoleData = out.toString
        })
      }
      if (consoleData.isEmpty) {
        consoleData = "none"
      }
    }

    val keySet = nodeMap.keySet
    val writer: PrintWriter = createWriter(title, dir)
    prepareForTest(title, prepare)

    val query = db.withTx( tx => {
      keySet.foldLeft(queryText)((acc, key) => acc.replace("%" + key + "%", node(tx, key).getId.toString))
    })

    assert(filePaths.size == urls.size)

    val testQuery = filePaths.foldLeft(query)( (acc, entry) => acc.replace(entry._1, entry._2))
    val docQuery = urls.foldLeft(query)( (acc, entry) => acc.replace(entry._1, entry._2))

    executeWithAllPlannersAndAssert(
      testQuery,
      assertions,
      expectedException,
      dumpToFileWithException(dir, writer, title, docQuery, optionalResultExplanation, text, _, consoleData, parameters),
      parameters,
      planners,
      prepareForTest(title, prepare))
    match {
      case Some(result) => dumpToFileWithResult(dir, writer, title, docQuery, optionalResultExplanation, text, result, consoleData, parameters)
      case  None =>
    }
  }

  def prepareForTest(title: String, prepare: Option[GraphDatabaseCypherService => Unit]) {
    prepare.foreach {
      (prepareStep: GraphDatabaseCypherService => Any) => prepareStep(db)
    }
    if (preparationQueries.nonEmpty) {
      dumpPreparationQueries(preparationQueries, dir, title)
      dumpPreparationGraphviz(dir, title, graphvizOptions)
    }
    db.withTx( tx => { tx.schema().awaitIndexesOnline(2, TimeUnit.SECONDS) } )
  }

  private def executeWithAllPlannersAndAssert(query: String,
                                              assertions: DocsExecutionResult => Unit,
                                              expectedException: Option[ClassTag[_ <: Exception]],
                                              expectedCaught: Exception => Unit,
                                              parameters: Map[String, Any],
                                              providedPlanners: Seq[String],
                                              prepareFunction: => Unit): Option[String] = {
    // COST planner is default. Can't specify it without getting exception thrown if it's unavailable.
    val planners = if (providedPlanners.isEmpty) Seq("") else providedPlanners

    val results = planners.flatMap {
      case planner if expectedException.isEmpty =>
        val parametersValue = ValueUtils.asMapValue(javaValues.asDeepJavaMap(parameters).asInstanceOf[java.util.Map[String, AnyRef]])

        val contextFactory = Neo4jTransactionalContextFactory.create( db )
        def txContext(transaction: InternalTransaction) =
          contextFactory.newContext(
            transaction,
            query,
            parametersValue
          )

        /*
        Note on transaction handling here:

        We have to execute the query in an implicit top-level transaction, because otherwise PERIODIC COMMIT
        does not work. Depending on the kind of query, the query might be completely executed and materialized
        under the hood by the cypher execution engine before returning from execute, and in some cases
        `executeTransaction` is also closed.

        For this reason we create a second `extractResultTransaction` to use while building the [[DocsExecutionResult]].
        If `executionTransaction` was closed by execute this will create a new real transaction, but in most cases it's
        simply going to become a `PlaceBoTransaction` inside `executeTransaction`, giving no overhead. We need to
        guarantee a transaction during result building in case the [[ResultStringBuilder]] needs to fetch e.g. node properties.

        After building the docsResult, both `executeTransaction` and `extractResultTransaction` are closed.

        We now create one final transaction is which to execute assertions, by doing

          `db.inTx(assertions(docsResult))`

        This transaction is necessary for Core API access in assertion code.
         */
        val executeTransaction = db.beginTransaction( Type.IMPLICIT, SecurityContext.AUTH_DISABLED )
        val docsResult = try {
          val context = txContext(executeTransaction)
          val subscriber = new ResultSubscriber(context)
          val result = engine.execute(s"$planner $query",
            parametersValue,
            context,
            profile = false,
            prePopulate = false,
            subscriber)
          subscriber.init(result)
          val docResult = DocsExecutionResult(subscriber, txContext(executeTransaction))
          executeTransaction.commit()
          docResult
        } finally executeTransaction.close()

        assertions(docsResult)
        val resultAsString = docsResult.resultAsString
        if (graphvizExecutedAfter && planner == planners.head) {
          dumpGraphViz(dir, graphvizOptions.trim)
        }
        reset()
        prepareFunction
        Some(resultAsString)

      case s =>
        val contextFactory = Neo4jTransactionalContextFactory.create( db )
        val transaction = db.beginTransaction( Type.IMPLICIT, SecurityContext.AUTH_DISABLED )
        val parametersValue = ValueUtils.asMapValue(javaValues.asDeepJavaMap(parameters).asInstanceOf[java.util.Map[String, AnyRef]])
        val context = contextFactory.newContext(
          transaction,
          query,
          parametersValue
          )

        var e: Throwable = null
        val subscriber = new QuerySubscriberAdapter {
          override def onError(throwable: Throwable): Unit = {
            e = throwable
          }
        }
        try {
          engine.execute(s"$s $query",
                         parametersValue,
                         context,
                         profile = false,
                         prePopulate = false,
                         subscriber).consumeAll()
          if (transaction.isOpen) {
            transaction.commit()
          }
        } catch {
          case t: Throwable =>
            e = t
        }

        val expectedExceptionType = expectedException.get
        e match {
          case expectedExceptionType(typedE) => expectedCaught(typedE)
          case _ => fail(s"Expected an exception of type $expectedException but got ${e.getClass}", e)
        }
        transaction.close()
        None
    }

    results.headOption
  }

  var dbFolder: File = _
  var managementService: DatabaseManagementService = _
  var db: GraphDatabaseCypherService = _
  var engine: ExecutionEngine = _
  var nodeMap: Map[String, Long] = _
  val properties: Map[String, Map[String, Any]] = Map()
  var generateConsole: Boolean = true
  var generateInitialGraphForConsole: Boolean = true
  val graphvizOptions: String = ""
  val noTitle: Boolean = false
  val graphvizExecutedAfter: Boolean = false
  var preparationQueries: List[String] = List()

  protected val baseUrl = System.getProperty("remote-csv-upload")
  var filePaths: Map[String, String] = Map.empty
  var urls: Map[String, String] = Map.empty

  def parent: Option[String] = None
  def section: String
  lazy val dir: File = if (parent.isDefined) createDir(new File(path, niceify(parent.get)), section) else createDir(section)

  def graphDescription: List[String] = List()

  val setupQueries: List[String] = List()
  val setupConstraintQueries: List[String] = List()

  // these 2 methods are need by ExecutionEngineHelper to do its job
  override def graph = db
  override def eengine = engine

  def graphOps: GraphDatabaseService = db.getGraphDatabaseService

  def indexProps: List[String] = List()

  def dumpToFileWithResult(dir: File, writer: PrintWriter, title: String, query: String, returns: String, text: String,
                 result: String, consoleData: String, parameters: Map[String, Any]) {
    dumpToFile(dir, writer, title, query, returns, text, Right(result), consoleData, parameters)
  }

  def dumpToFileWithException(dir: File, writer: PrintWriter, title: String, query: String, returns: String, text: String,
                 failure: Exception, consoleData: String, parameters: Map[String, Any]) {
    dumpToFile(dir, writer, title, query, returns, text, Left(failure), consoleData, parameters)
  }

  private def dumpToFile(dir: File, writer: PrintWriter, title: String, query: String, returns: String, text: String,
                         result: Either[Exception, String], consoleData: String, parameters: Map[String, Any]) {
    val testId = niceify(parent, section, title)
    writer.println("[[" + testId + "]]")
    if (!noTitle) writer.println("== " + title + " ==")
    writer.println(text)
    writer.println()
    dumpQuery(dir, writer, testId, query, returns, result, consoleData, parameters)
    writer.flush()
    writer.close()
  }

  def executePreparationQueries(queries: List[String]) {
    preparationQueries = queries

    graph.withTx( executeQueries(_, queries), Type.IMPLICIT )
  }

  private def executeQueries(tx: InternalTransaction, queries: List[String]) {
    val contextFactory = Neo4jTransactionalContextFactory.create( db )
    queries.foreach { query => {
      engine.execute(
        query,
        VirtualValues.EMPTY_MAP,
        contextFactory.newContext(
          tx,
          query,
          VirtualValues.EMPTY_MAP
          ),
        profile = false,
        prePopulate = false,
        QuerySubscriber.DO_NOTHING_SUBSCRIBER
        ).consumeAll()
    } }
  }

  protected def indexingService = db.getDependencyResolver.resolveDependency(classOf[IndexingService])

  protected def sampleAllIndexesAndWait(mode: IndexSamplingMode = IndexSamplingMode.backgroundRebuildAll(), time: Long = 10, unit: TimeUnit = TimeUnit.SECONDS) = {
    indexingService.triggerIndexSampling(mode)
    unit.sleep(time)
  }

  protected def getLabelsFromNode(p: DocsExecutionResult): Iterable[String] = p.columnAs[Node]("n").next().labels

  def node(tx: Transaction, name: String): Node = tx.getNodeById(nodeMap.getOrElse(name, throw new NotFoundException(name)))
  def nodes(tx:Transaction, names: String*): List[Node] = names.map(name => node(tx,name)).toList
  def rel(tx:Transaction, id: Long): Relationship = tx.getRelationshipById(id)

  @After
  def tearDown() {
    if (managementService != null) {
      managementService.shutdown()
    }
  }

  @Before
  def init() {
    hardReset()
  }

  protected def databaseConfig(): util.Map[Setting[_], Object] =
    Map[Setting[_], Object](
      OnlineBackupSettings.online_backup_listen_address -> new SocketAddress("127.0.0.1", 0),
      OnlineBackupSettings.online_backup_enabled -> java.lang.Boolean.FALSE
    ).asJava

  override def hardReset() {
    tearDown()
    val fs = new EphemeralFileSystemAbstraction()
    val td = TestDirectory.testDirectory(this.getClass, fs)
    dbFolder = td.prepareDirectoryForTest("target/example-db" + System.nanoTime()).toFile
    managementService = new TestEnterpriseDatabaseManagementServiceBuilder(dbFolder.toPath).setFileSystem(fs).setConfig(databaseConfig).build()
    val database = managementService.database( DEFAULT_DATABASE_NAME )
    db = new GraphDatabaseCypherService(database)

    engine = ExecutionEngineFactory.createCommunityEngineFromDb(database) // TODO: This should be Enterprise!

    softReset()
  }

  override def softReset() {
    cleanDatabaseContent(db.getGraphDatabaseService)

    db.withTx(tx => tx.schema().awaitIndexesOnline(10, TimeUnit.SECONDS))

    val g = new GraphImpl(graphDescription.toArray[String])
    val description = GraphDescription.create(g)
    nodeMap = description.create(db.getGraphDatabaseService).asScala.map {
        case (name, node) => name -> node.getId
      }.toMap
    db.withTx( tx => {
      executeQueries(tx, setupQueries)

      asNodeMap(tx, properties) foreach {
        case (n: Node, seq: Map[String, Any]) =>
          seq foreach { case (k, v) => n.setProperty(k, v) }
      }
    }, Type.EXPLICIT )

    db.withTx( executeQueries(_, setupConstraintQueries), Type.EXPLICIT )
  }

  private def asNodeMap[T: ClassTag](tx:InternalTransaction,  m: Map[String, T]): Map[Node, T] =
    m map { case (k: String, v: T) => (node(tx, k), v) }

  private def mapMapValue(v: Any): Any = v match {
    case v: Map[_, _] => Eagerly.immutableMapValues(v, mapMapValue).asJava
    case seq: Seq[_]  => seq.map(mapMapValue).asJava
    case v: Any       => v
  }

  private def dumpQuery(dir: File,
                        writer: PrintWriter,
                        testId: String,
                        query: String,
                        returns: String,
                        result: Either[Exception, String],
                        consoleData: String,
                        parameters: Map[String, Any]) {
    if (parameters != null && parameters.nonEmpty) {
      writer.append(JavaExecutionEngineDocTest.parametersToAsciidoc(mapMapValue(parameters)))
    }
    val output = new StringBuilder(2048)
    output.append(".Query\n")
    output.append(createCypherSnippet(query))
    writer.println(AsciiDocGenerator.dumpToSeparateFile(dir, testId + ".query", output.toString()))
    writer.println()
    if (returns != null && !returns.isEmpty) {
      writer.println(returns)
      writer.println()
    }

    output.clear()
    result match {
      case Left(failure) =>
        output.append(".Error message\n")
        output.append(AsciidocHelper.createQueryFailureSnippet(failure.getMessage))
      case Right(rightResult) =>
        output.append(".Result\n")
        output.append(AsciidocHelper.createQueryResultSnippet(rightResult))
    }
    output.append('\n')
    writer.println(AsciiDocGenerator.dumpToSeparateFile(dir, testId + ".result", output.toString()))

    if (generateConsole && (parameters == null || parameters.isEmpty)) {
      output.clear()
      writer.println(".Try this query live")
      output.append("[console]\n")
      output.append("----\n")
      output.append(consoleData)
      output.append("\n\n")
      output.append(query)
      output.append("\n----")
      writer.println(AsciiDocGenerator.dumpToSeparateFile(dir, testId + ".console", output.toString()))
    }
  }
}

trait ResetStrategy {
  def reset() {}
  def hardReset() {}
  def softReset() {}
}

trait HardReset extends ResetStrategy {
  override def reset() {
    hardReset()
  }
}

trait SoftReset extends ResetStrategy {
  override def reset() {
    softReset()
  }
}
