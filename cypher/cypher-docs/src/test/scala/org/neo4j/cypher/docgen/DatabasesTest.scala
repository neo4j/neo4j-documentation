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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, QueryStatisticsTestSupport, ResultAndDbAssertions, ResultAssertions}
import org.neo4j.graphdb.{Label, Node}
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class DatabasesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Databases", "administration-databases")
    database("system")
    synopsis("This section explains how to use Cypher to manage Neo4j databases: creating, deleting, starting and stopping individual databases within a single server.")
    p(
      """
        |* <<administration-databases-introduction, Introduction>>
        |* <<administration-databases-show-databases, Listing databases>>
        |* <<administration-databases-create-database, Creating databases>>
        |* <<administration-databases-stop-database, Stopping databases>>
        |* <<administration-databases-start-database, Starting databases>>
        |* <<administration-databases-drop-database, Deleting databases>>
        |""".stripMargin)
    section("Introduction", "administration-databases-introduction") {
      p(
        """Neo4j allows the same server to manage multiple databases. The metadata for these databases,
          |including the associated security model, is maintained in a special database called the `system` database.
          |All multi-database administrative commands need to be executing against the `system` database.""".stripMargin)
    }
    section("Listing databases", "administration-databases-show-databases") {
      p("There are three different commands for listing databases. Listing all databases, listing a particular database or listing the default database.")
      p("All available databases can be seen using the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      note {
        p(
          """Note that the results of this command are filtered according to the `ACCESS` privileges the user has.
            |However, a user with `CREATE/DROP DATABASE` or `DATABASE MANAGEMENT` privileges can see all databases regardless of their `ACCESS` privileges.
            |If a user has not been granted `ACCESS` privilege to any databases, the command can still be executed but will only return the `system` database, which is always visible.
            |""".stripMargin)
          }
      p("A particular database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE system", assertDatabaseShown("system")) {
        resultTable()
      }
      p("The default database can be seen using the command `SHOW DEFAULT DATABASE`.")
      query("SHOW DEFAULT DATABASE", assertDatabaseShown("neo4j")) {
        resultTable()
      }
      note {
        p(
          """Note that for failed databases, the `currentStatus` and `requestedStatus` are different.
            |This often implies an error, but **does not always**.
            |For example, a database may take a while to transition from `offline` to `online` due to performing recovery.
            |Or, during normal operation a database's `currentStatus` may be transiently different from its `requestedStatus` due to a necessary automatic process, such as one Neo4j instance copying store files from another.
            |The possible statuses are `initial`, `online`, `offline`, `store copying` and `unknown`.
            |""".stripMargin)
      }
    }
    section("Creating databases", "administration-databases-create-database", "enterprise-edition") {
      p("Databases can be created using `CREATE DATABASE`.")
      query("CREATE DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      note {
        p("""Database names are subject to the <<cypher-manual#cypher-naming, standard Cypher restrictions on valid identifiers>>.
          |The following naming rules apply:""")
        p(
          """
            |* Database name length must be between 3 and 63 characters.
            |* The first character must be an ASCII alphabetic character.
            |* Subsequent characters can be ASCII alphabetic (`mydatabase`), numeric characters (`mydatabase2`), dots (`main.db`), and dashes (enclosed within backticks, e.g., `CREATE DATABASE ++`main-db`++`).
            |* Names cannot end with dots or dashes.
            |* Names that begin with an underscore or with the prefix `system` are reserved for internal use.
          """.stripMargin)
     }
      p("When a database has been created, it will show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database already exists. " +
        "Appending `IF NOT EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database already exist. " +
        "Adding `OR REPLACE` to the command will result in any existing database being deleted and a new one created.")
      query("CREATE DATABASE customers IF NOT EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
      query("CREATE OR REPLACE DATABASE customers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        p("This is equivalent to running `DROP DATABASE customers IF EXISTS` followed by `CREATE DATABASE customers`.")
      }
      note {
        p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
      }
    }
    section("Stopping databases", "administration-databases-stop-database", "enterprise-edition") {
      p("Databases can be stopped using the command `STOP DATABASE`.")
      query("STOP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the stopped database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Starting databases", "administration-databases-start-database", "enterprise-edition") {
      p("Databases can be started using the command `START DATABASE`.")
      query("START DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("The status of the started database can be seen using the command `SHOW DATABASE name`.")
      query("SHOW DATABASE customers", assertDatabaseShown("customers")) {
        resultTable()
      }
    }
    section("Deleting databases", "administration-databases-drop-database", "enterprise-edition") {
      p("Databases can be deleted using the command `DROP DATABASE`.")
      query("DROP DATABASE customers", ResultAssertions((r) => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database has been deleted, it will no longer show up in the listing provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASES", assertDatabasesShown) {
        resultTable()
      }
      p("This command is optionally idempotent, with the default behavior to throw an exception if the database does not exists. " +
        "Appending `IF EXISTS` to the command will ensure that no exception is thrown and nothing happens should the database not exist.")
      query("DROP DATABASE customers IF EXISTS", ResultAssertions(r => {
        assertStats(r, systemUpdates = 0)
      })) {}
    }
  }.build()

  private def assertDatabasesShown = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val dbNodes = tx.findNodes(Label.label("Database")).asScala.toList
      val dbNames = dbNodes.map(n => n.getProperty("name")).toSet
      val result = p.columnAs[String]("name").toSet
      result should equal(dbNames)
    } finally {
      tx.close()
    }
  })

  private def assertDatabaseShown(expected: String) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.explicit, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("name").toSet
      result should equal(Set(expected))
    } finally {
      tx.close()
    }
  })
}
