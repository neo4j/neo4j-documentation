package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class BuiltInRolesAdministrationTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Built-in roles", "administration-built-in-roles")
    database("system")
    synopsis("This section explains the default privileges of the built-in roles in Neo4j and how to recreate them if needed.")

    p(
      """All of the commands described in this chapter require that the user executing the commands has the rights to do so.
        |The privileges listed in the following sections are the default set of privileges for each built-in role.
        |""".stripMargin)
    p(
      """
        |* <<administration-built-in-roles-public, The `PUBLIC` role>>
        |* <<administration-built-in-roles-reader, The `reader` role>>
        |* <<administration-built-in-roles-editor, The `editor` role>>
        |* <<administration-built-in-roles-publisher, The `publisher` role>>
        |* <<administration-built-in-roles-architect, The `architect` role>>
        |* <<administration-built-in-roles-admin, The `admin` role>>
        |""".stripMargin)
    section("The `PUBLIC` role", "administration-built-in-roles-public", "enterprise-edition") {
      p("All users are granted the `PUBLIC` role. It cannot be revoked or dropped. By default it gives access to the default database.")
      section("Privileges of the `PUBLIC` role", "administration-built-in-roles-public-privileges", "enterprise-edition") {
        query("SHOW ROLE PUBLIC PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `PUBLIC` role", "administration-built-in-roles-public-recreate", "enterprise-edition") {
        initQueries(
          """REVOKE GRANT ACCESS ON DEFAULT DATABASE FROM PUBLIC""")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """The `PUBLIC` role can not be dropped and thus there is no need to recreate the role itself.
            |To restore the role to its original capabilities two steps are needed: First, all `GRANT` or `DENY` on this role should be revoked (see output of `SHOW ROLE PUBLIC PRIVILEGES` on what to revoke).
            |Secondly, the following query need to be executed:""".stripMargin)
        query("GRANT ACCESS ON DEFAULT DATABASE TO PUBLIC", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting `PUBLIC` role has the same privileges as the " +
          "<<administration-built-in-roles-public-privileges,original built-in `PUBLIC` role>>.")

//        query("SHOW ROLE PUBLIC PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `reader` role", "administration-built-in-roles-reader", "enterprise-edition") {
      p("The `reader` can perform read-only queries on all graphs except for the `system` database.")
      section("Privileges of the `reader` role", "administration-built-in-roles-reader-privileges", "enterprise-edition") {
        query("SHOW ROLE reader PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `reader` role", "administration-built-in-roles-reader-recreate", "enterprise-edition") {
        initQueries(
          "DROP ROLE reader")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed: First, if not already done, execute `DROP ROLE reader`.
            |Secondly, the following queries need to be executed:""".stripMargin)

        query("CREATE ROLE reader", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ACCESS ON DATABASE * TO reader", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT MATCH {*} ON GRAPH * TO reader", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting `reader` role has the same privileges as the " +
          "<<administration-built-in-roles-reader-privileges,original built-in `reader` role>>.")

//        query("SHOW ROLE reader PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `editor` role", "administration-built-in-roles-editor", "enterprise-edition") {
      p("The `editor` can perform read and write operations on all graphs except for the `system` database, but cannot make new labels, property keys or relationship types.")
      section("Privileges of the `editor` role", "administration-built-in-roles-editor-privileges", "enterprise-edition") {
        query("SHOW ROLE editor PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `editor` role", "administration-built-in-roles-editor-recreate", "enterprise-edition") {
        initQueries(
          "DROP ROLE editor")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed: First, if not already done, execute `DROP ROLE editor`.
            |Secondly, the following queries need to be executed:""".stripMargin)
        query("CREATE ROLE editor", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ACCESS ON DATABASE * TO editor", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT MATCH {*} ON GRAPH * TO editor", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT WRITE ON GRAPH * TO editor", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting `editor` role has the same privileges as the " +
          "<<administration-built-in-roles-editor-privileges,original built-in `editor` role>>.")

//        query("SHOW ROLE editor PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `publisher` role", "administration-built-in-roles-publisher", "enterprise-edition") {
      p("The `publisher` can do the same as <<administration-built-in-roles-editor,`editor`>>, but also create new labels, property keys and relationship types.")
      section("Privileges of the `publisher` role", "administration-built-in-roles-publisher-privileges", "enterprise-edition") {
        query("SHOW ROLE publisher PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `publisher` role", "administration-built-in-roles-publisher-recreate", "enterprise-edition") {
        initQueries(
          "DROP ROLE publisher")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed: First, if not already done, execute `DROP ROLE publisher`.
            |Secondly, the following queries need to be executed:""".stripMargin)
        query("CREATE ROLE publisher", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ACCESS ON DATABASE * TO publisher", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT MATCH {*} ON GRAPH * TO publisher", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT WRITE ON GRAPH * TO publisher", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT NAME MANAGEMENT ON DATABASE * TO publisher", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting `publisher` role has the same privileges as the " +
          "<<administration-built-in-roles-publisher-privileges,original built-in `publisher` role>>.")

//        query("SHOW ROLE publisher PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `architect` role", "administration-built-in-roles-architect", "enterprise-edition") {
      p("The `architect` can do the same as the <<administration-built-in-roles-publisher,`publisher`>> as well as create and manage indexes and constraints.")
      section("Privileges of the `architect` role", "administration-built-in-roles-architect-privileges", "enterprise-edition") {
        query("SHOW ROLE architect PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `architect` role", "administration-built-in-roles-architect-recreate", "enterprise-edition") {
        initQueries(
          "DROP ROLE architect")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed: First, if not already done, execute `DROP ROLE architect`.
            |Secondly, the following queries need to be executed:""".stripMargin)
        query("CREATE ROLE architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ACCESS ON DATABASE * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT MATCH {*} ON GRAPH * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT WRITE ON GRAPH * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT NAME MANAGEMENT ON DATABASE * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT INDEX MANAGEMENT ON DATABASE * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT CONSTRAINT MANAGEMENT ON DATABASE * TO architect", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting `architect` role has the same privileges as the " +
          "<<administration-built-in-roles-architect-privileges,original built-in `architect` role>>.")

//        query("SHOW ROLE architect PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `admin` role", "administration-built-in-roles-admin", "enterprise-edition") {
      p("The `admin` can do the same as  the <<administration-built-in-roles-architect,`architect`>>, as well as manage databases, users, roles and privileges.")
      section("Privileges of the `admin` role", "administration-built-in-roles-admin-privileges", "enterprise-edition") {
        query("SHOW ROLE admin PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `admin` role", "administration-built-in-roles-admin-recreate", "enterprise-edition") {
        initQueries(
          "DROP ROLE admin")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        note(p("In Neo4j 4.1.0 it is *not* possible to fully recreate a dropped admin role. " +
          "Specifically, the ability to run procedures with the `@Admin` annotation like e.g. `dbms.listConfig` can *not* be restored." +
          " Because of that, it is highly discouraged to drop the admin role."))
        p(
          """
            |To restore the role to its original capabilities (minus the ability to run admin procedures) two steps are needed: First, if not already done, execute `DROP ROLE admin`.
            |Secondly, the following queries need to be executed in order to set up the privileges:""".stripMargin)
        query("CREATE ROLE admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ALL DBMS PRIVILEGES ON DBMS TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT TRANSACTION MANAGEMENT ON DATABASE * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT START ON DATABASE * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT STOP ON DATABASE * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT MATCH {*} ON GRAPH * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT WRITE ON GRAPH * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT ALL ON DATABASE * TO admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("All of the queries above are enough to grant (almost) full admin capabilities. " +
          "Please note that the result of executing `SHOW ROLE admin PRIVILEGES` now appears to be slightly different from " +
          "the privileges shown for the <<administration-built-in-roles-admin-privileges,original built-in `admin` role>>. This does not make any functional difference.")

        query("SHOW ROLE admin PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }

        p("Additional information about restoring the admin role can be found in the <<operations-manual#recover-admin-role, Operations Manual -> Recover the admin role>>.")
      }
    }
  }.build()
}
