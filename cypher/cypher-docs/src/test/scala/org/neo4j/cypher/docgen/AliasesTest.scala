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

import org.neo4j.cypher.docgen.tooling._
import org.neo4j.graphdb.Label
import org.neo4j.kernel.api.KernelTransaction.Type
import org.neo4j.kernel.api.security.AnonymousContext

import scala.collection.JavaConverters._

class AliasesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/"

  override def doc: Document = new DocBuilder {
    doc("Database alias management", "alias-management")
    database("system")
    initQueries(
      "CREATE DATABASE `movies`",
      "CREATE ALIAS `films` FOR DATABASE `movies`",
      "CREATE ALIAS `motion pictures` FOR DATABASE `movies`",
      "CREATE DATABASE `northwind-graph-2020`",
      "CREATE DATABASE `northwind-graph-2021`",
      """CREATE ALIAS `movie scripts` FOR DATABASE `scripts` AT "neo4j+s://location:7687" USER alice PASSWORD "password" DRIVER {
        |    ssl_enforced: true,
        |    connection_timeout: duration({seconds: 5}),
        |    connection_max_lifetime: duration({hours: 1}),
        |    connection_pool_acquisition_timeout: duration({minutes: 1}),
        |    connection_pool_idle_test: duration({minutes: 2}),
        |    connection_pool_max_size: 10,
        |    logging_level: 'info'
        |}""".stripMargin
    )
    synopsis("This chapter explains how to use Cypher to manage database aliases in Neo4j.")
    p(
      """There are two kinds of aliases, local database aliases and remote database aliases.
        |A local database alias can only target a database within the same DBMS.
        |A remote alias may target a database from another Neo4j DBMS.
        |When a query is run against an alias, it will be redirected to the target database.
        |The home database for users can be set to an alias, which will be resolved to the target database on use.
        """.stripMargin)
    p(
      """A local alias can be used in all other Cypher commands in place of the target database.
        |Please note that the local alias will be resolved while executing the command.
        |Privileges are defined on the database, and not the local alias.
        """.stripMargin)
    p(
      """A remote alias can be used for connecting to a database of a remote Neo4j DBMS, use clauses, setting a user's home database and defining the
        |access privileges to the remote database. Remote aliases requires configuration to safely connect to the remote target, which is described in
        |<<operations-manual#manage-remote-aliases, Connecting remote databases>>. It is not possible to execute an administration command on the
        |remote database via a remote alias.
        """.stripMargin)
    p(
      """Aliases can be created and managed using a set of Cypher administration commands executed against the `system` database.
        |The required privileges are described <<cypher-manual#access-control-dbms-administration-alias-management, here>>.
        |When connected to the DBMS over bolt, administration commands are automatically routed to the `system` database.""".stripMargin)
    p("include::alias/alias-command-syntax.asciidoc[]")
    p("include::alias/alias-driver-settings-table.asciidoc[]")
    section("Listing database aliases", "alias-management-show-alias", role = "enterprise-edition" ) {
      p(
        """Available database aliases can be seen using `SHOW ALIASES FOR DATABASE`.
           The required privileges are described <<cypher-manual#access-control-dbms-administration-alias-management, here>>.
           `SHOW ALIASES FOR DATABASE` will produce a table of database aliases with the following columns:
          """.stripMargin)
      p("include::alias/show-database-alias-columns.asciidoc[]")
      p(
        """A summary of all available databases alias can be displayed using the command `SHOW ALIASES FOR DATABASE`.""")
      query("SHOW ALIASES FOR DATABASE", assertAliasesShown) {
        resultTable()
      }
      p(
        """
          |The detailed information for a particular database alias can be displayed using the command `SHOW ALIASES FOR DATABASE YIELD *`.
          |When a `YIELD *` clause is provided, the full set of columns is returned.
          |""".stripMargin)
      query("SHOW ALIASES FOR DATABASE YIELD *", assertNameField("films", "motion pictures", "movie scripts")) {
        resultTable()
      }
      p("The number of database aliases can be seen using a `count()` aggregation with `YIELD` and `RETURN`.")
      query("SHOW ALIASES FOR DATABASE YIELD * RETURN count(*) as count", ResultAssertions({ r: DocsExecutionResult =>
        r.columnAs[Int]("count").toSet should be(Set(3))
      })){
        resultTable()
      }
      p("It is also possible to filter and sort the results by using `YIELD`, `ORDER BY` and `WHERE`.")
      query("SHOW ALIASES FOR DATABASE YIELD name, url, database ORDER BY database WHERE name CONTAINS 'e'",
        assertNameField("motion pictures", "movie scripts")) {
        p(
          """In this example:
            |
            |* The number of columns returned has been reduced with the `YIELD` clause.
            |* The order of the returned columns has been changed.
            |* The results have been filtered to only show database alias names containing 'e'.
            |* The results are ordered by the 'database' column using `ORDER BY`.
            |
            |It is also possible to use `SKIP` and `LIMIT` to paginate the results.
            |""".stripMargin)
        resultTable()
      }
    }
    section(title="Creating database aliases", id="alias-management-create-database-alias", role = "enterprise-edition") {
      p("Aliases can be created using `CREATE ALIAS`. The required privileges are described <<cypher-manual#access-control-dbms-administration-alias-management, here>>.")
      p("include::alias-management-syntax-create-alias.asciidoc[]")
      p(
        """This command is optionally idempotent, with the default behavior to fail with an error if the database alias already exists.
          |Inserting `IF NOT EXISTS` after the alias name ensures that no error is returned and nothing happens should a database alias with that name already exist.
          |Adding `OR REPLACE` to the command will result in any existing database alias being deleted and a new one created.
          |`CREATE OR REPLACE ALIAS` will fail if there is an existing database with the same name.""".stripMargin)
      note {
        p("The `IF NOT EXISTS` and `OR REPLACE` parts of this command cannot be used together.")
      }
      note {
        p("""Alias names are subject to the <<cypher-manual#cypher-naming, standard Cypher restrictions on valid identifiers>>.
            |The following naming rules apply:""")
        p(
          """
            |* A name is a valid identifier, additionally allowing dots e.g. `main.alias`.
            |* Name length can be up to 65534 characters.
            |* Names cannot end with dots.
            |* Names that begin with an underscore or with the prefix `system` are reserved for internal use.
            |* Non-alphabetic characters, including numbers, symbols and whitespace characters, can be used in names, but must be escaped using backticks.
            |""".stripMargin)
      }

      section(title="Creating local database aliases", id="database-management-create-local-database-alias", role = "enterprise-edition") {
        p("Local aliases are created with a target database.")
        query("CREATE ALIAS `northwind` FOR DATABASE `northwind-graph-2021`", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("When a local database alias has been created, it will show up in the aliases column provided by the command `SHOW DATABASES` and in the `SHOW ALIASES FOR DATABASE` command.")
        query("SHOW DATABASE `northwind`", assertNameField("northwind-graph-2021")) {
          resultTable()
        }
        query("SHOW ALIASES FOR DATABASE WHERE name = 'northwind'", assertNameField("northwind")) {
          resultTable()
        }
        p("Adding a local alias with the same name as an existing local or remote alias will do nothing with the `IF NOT EXISTS` clause but fail without it.")
        query("CREATE ALIAS `northwind` IF NOT EXISTS FOR DATABASE `northwind-graph-2020` ", ResultAssertions(r => {
          assertStats(r)
        })) {
          statsOnlyResultTable()
        }
        p("It is also possible to replace an alias. The old alias may be either local or remote.")
        query("CREATE OR REPLACE ALIAS `northwind` FOR DATABASE `northwind-graph-2020`", ResultAssertions(r => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        p("This is equivalent to running ``DROP ALIAS `northwind` IF EXISTS FOR DATABASE`` followed by ``CREATE ALIAS `northwind` FOR DATABASE `northwind-graph-2020` ``.")
      }

      section(title="Creating remote database aliases", id="database-management-create-remote-database-alias", role = "enterprise-edition") {
        p(
          """Database aliases can also point to remote databases by providing an url and the credentials of a user on the remote Neo4j DBMS.
            |See <<operations-manual#manage-remote-aliases, Connecting remote databases>> for the necessary configurations.""".stripMargin)
        query("""CREATE ALIAS `remote-northwind` FOR DATABASE `northwind-graph-2020` AT "neo4j+s://location:7687" USER alice PASSWORD 'password'""", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("""It is possible to override the default driver settings per alias, which are used for connecting to the remote database.
            |The full list of supported driver settings can be seen <<remote-alias-driver-settings, here>>.""".stripMargin)
        query(
          """CREATE ALIAS `remote-with-driver-settings` FOR DATABASE `northwind-graph-2020` AT "neo4j+s://location:7687" USER alice PASSWORD 'password'
            |DRIVER { connection_timeout: duration({ minutes: 1 }), connection_pool_max_size: 10 }""".stripMargin, ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("When a database alias pointing to a remote database has been created, its details can be shown with the `SHOW ALIASES FOR DATABASE` command.")
        query("SHOW ALIASES FOR DATABASE WHERE name = 'remote-northwind'", assertNameField("remote-northwind")) {
          resultTable()
        }
        query("SHOW ALIASES FOR DATABASE YIELD * WHERE name = 'remote-with-driver-settings'", assertNameField("remote-with-driver-settings")) {
          resultTable()
        }
        p(
          """Creating remote aliases also allows `IF NOT EXISTS` and `OR REPLACE` clauses.
            |Both check for any remote or local database aliases.""".stripMargin)
      }
    }
    section(title="Altering database aliases", id="alias-management-alter-database-alias", role = "enterprise-edition") {
      p(
        s"""Aliases can be altered using `ALTER ALIAS` to change its database target, url, user credentials, or driver settings.
           |The required privileges are described <<cypher-manual#access-control-dbms-administration-alias-management, here>>.
           |Only the clauses used will be altered.""".stripMargin)
      p("include::alias-management-syntax-alter-alias.asciidoc[]")
      note {
        p("Local aliases can not be altered to remote aliases or vice versa.")
      }
      p("Example of altering a local alias target.")
      query("ALTER ALIAS `northwind` SET DATABASE TARGET `northwind-graph-2021`", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("Example of altering a remote alias target.")
      query("""ALTER ALIAS `remote-northwind` SET DATABASE TARGET `northwind-graph-2020` AT "neo4j+s://other-location:7687"""", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("Example of altering a remote alias credentials and driver settings.")
      query(
        """ALTER ALIAS `remote-with-driver-settings` SET DATABASE USER bob PASSWORD 'newPassword'
           |DRIVER { connection_timeout: duration({ minutes: 1 }), logging_level: "debug" }""".stripMargin, ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
        note{
          p(
            """All driver settings are replaced by the new ones.
              |In this case, by not repeating the driver setting `connection_pool_max_size` the value will be deleted and fallback to the default value.""".stripMargin)
        }
      }
      p("Example of altering a remote alias to remove all custom driver settings.")
      query("ALTER ALIAS `movie scripts` SET DATABASE DRIVER { }", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a local database alias has been altered, it will show up in the aliases column for the target database provided by the command `SHOW DATABASES`.")
      query("SHOW DATABASE `northwind`", assertNameField("northwind-graph-2021")) {
        resultTable()
      }
      p("The changes for all database aliases will show up in the `SHOW ALIASES FOR DATABASE` command.")
      query("SHOW ALIASES FOR DATABASE YIELD * WHERE name in ['northwind', 'remote-northwind', 'remote-with-driver-settings', 'movie scripts']",
        assertNameField("northwind", "remote-northwind", "remote-with-driver-settings", "movie scripts")) {
        resultTable()
      }
      p("""This command is optionally idempotent, with the default behavior to fail with an error if the alias does not exist.
        |Appending `IF EXISTS` to the command ensures that no error is returned and nothing happens should the alias not exist.""".stripMargin)
      query("ALTER ALIAS `no-alias` IF EXISTS SET DATABASE TARGET `northwind-graph-2021`", ResultAssertions(r => {
        assertStats(r)
      })) {
        statsOnlyResultTable()
      }
    }
    section(title="Deleting database aliases", id="alias-management-drop-database-alias", role = "enterprise-edition") {
      p(
        """Both local and remote aliases can be deleted using the `DROP ALIAS` command.
          |The required privileges are described <<cypher-manual#access-control-dbms-administration-alias-management, here>>.""".stripMargin)
      query("DROP ALIAS `northwind` FOR DATABASE", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      query("DROP ALIAS `remote-northwind` FOR DATABASE", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p("When a database alias has been deleted, it will no longer show up in the aliases column provided by the command `SHOW DATABASES` or in the `SHOW ALIAS FOR DATABASE` command.")
      query("SHOW DATABASE `northwind-graph-2021`", assertNameField("northwind-graph-2021")) {
        resultTable()
      }
      query("SHOW ALIASES FOR DATABASE", assertAliasesShown) {
        resultTable()
      }
      p(s"""This command is optionally idempotent, with the default behavior to fail with an error if the alias does not exist.
           |Inserting IF EXISTS after the alias name ensures that no error is returned and nothing happens should the alias not exist."""
        .stripMargin)
      query("DROP ALIAS `northwind` IF EXISTS FOR DATABASE", ResultAssertions(r => {
        assertStats(r)
      })) {
        statsOnlyResultTable()
      }
    }
  }.build()

  private def assertAliasesShown = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val aliasNodes = tx.findNodes(Label.label("DatabaseName")).asScala.toList
        .filter(n => Option(n.getProperty("primary")).contains(false))
      val aliasNames = aliasNodes.map(n => n.getProperty("name")).toSet
      val result = p.columnAs[String]("name").toSet
      result should equal(aliasNames)
    } finally {
      tx.close()
    }
  })

  private def assertNameField(expected: String*) = ResultAndDbAssertions((p, db) => {
    val tx = db.beginTransaction(Type.EXPLICIT, AnonymousContext.read())
    try {
      val result = p.columnAs[String]("name").toSet
      result should equal(expected.toSet)
    } finally {
      tx.close()
    }
  })
}
