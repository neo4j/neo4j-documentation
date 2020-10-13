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
        |The privileges listed in the following sections are the default set of privileges for each built-in role:
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
      p("All users are granted the `PUBLIC` role, and it can not be revoked or dropped. By default, it gives access to the default database and allows executing all procedures and user defined functions.")
      section("Privileges of the `PUBLIC` role", "administration-built-in-roles-public-privileges") {
        query("SHOW ROLE PUBLIC PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `PUBLIC` role", "administration-built-in-roles-public-recreate") {
        initQueries(
          // setup so that later when the grant query gets executed, it will show systemUpdates: 1
          "REVOKE GRANT ACCESS ON DEFAULT DATABASE FROM PUBLIC",
          "REVOKE EXECUTE PROCEDURES * ON DBMS FROM PUBLIC",
          "REVOKE EXECUTE USER DEFINED FUNCTIONS * ON DBMS FROM PUBLIC"
        )
        p(
          """The `PUBLIC` role can not be dropped and thus there is no need to recreate the role itself.
            |To restore the role to its original capabilities, two steps are needed. First, all `GRANT` or `DENY` privileges on this role should be revoked (see output of `SHOW ROLE PUBLIC PRIVILEGES` on what to revoke).
            |Secondly, the following queries must be run:""".stripMargin)
        query("GRANT ACCESS ON DEFAULT DATABASE TO PUBLIC", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT EXECUTE PROCEDURES * ON DBMS TO PUBLIC", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("GRANT EXECUTE USER DEFINED FUNCTIONS * ON DBMS TO PUBLIC", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting `PUBLIC` role now has the same privileges as the " +
          "original built-in `PUBLIC` role.")

//        query("SHOW ROLE PUBLIC PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `reader` role", "administration-built-in-roles-reader", "enterprise-edition") {
      p("The `reader` role can perform read-only queries on all graphs except for the `system` database.")
      section("Privileges of the `reader` role", "administration-built-in-roles-reader-privileges") {
        query("SHOW ROLE reader PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `reader` role", "administration-built-in-roles-reader-recreate") {
        initQueries(
          "DROP ROLE reader")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed. First, if not already done, execute `DROP ROLE reader`.
            |Secondly, the following queries must be run:""".stripMargin)

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

        p("The resulting `reader` role now has the same privileges as the " +
          "original built-in `reader` role.")

//        query("SHOW ROLE reader PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `editor` role", "administration-built-in-roles-editor", "enterprise-edition") {
      p("The `editor` role can perform read and write operations on all graphs except for the `system` database, but can not make new labels, property keys or relationship types.")
      section("Privileges of the `editor` role", "administration-built-in-roles-editor-privileges") {
        query("SHOW ROLE editor PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `editor` role", "administration-built-in-roles-editor-recreate") {
        initQueries(
          "DROP ROLE editor")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed. First, if not already done, execute `DROP ROLE editor`.
            |Secondly, the following queries must be run:""".stripMargin)
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

        p("The resulting `editor` role now has the same privileges as the " +
          "original built-in `editor` role.")

//        query("SHOW ROLE editor PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `publisher` role", "administration-built-in-roles-publisher", "enterprise-edition") {
      p("The `publisher` role can do the same as <<administration-built-in-roles-editor,`editor`>>, but can also create new labels, property keys and relationship types.")
      section("Privileges of the `publisher` role", "administration-built-in-roles-publisher-privileges") {
        query("SHOW ROLE publisher PRIVILEGES", ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `publisher` role", "administration-built-in-roles-publisher-recreate") {
        initQueries(
          "DROP ROLE publisher")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed. First, if not already done, execute `DROP ROLE publisher`.
            |Secondly, the following queries must be run:""".stripMargin)
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

        p("The resulting `publisher` role now has the same privileges as the " +
          "original built-in `publisher` role.")

//        query("SHOW ROLE publisher PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `architect` role", "administration-built-in-roles-architect", "enterprise-edition") {
      p("The `architect` role can do the same as the <<administration-built-in-roles-publisher,`publisher`>>, as well as create and manage indexes and constraints.")
      section("Privileges of the `architect` role", "administration-built-in-roles-architect-privileges") {
        query("SHOW ROLE architect PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `architect` role", "administration-built-in-roles-architect-recreate") {
        initQueries(
          "DROP ROLE architect")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed. First, if not already done, execute `DROP ROLE architect`.
            |Secondly, the following queries must be run:""".stripMargin)
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
        p("The resulting `architect` role now has the same privileges as the " +
          "original built-in `architect` role.")

//        query("SHOW ROLE architect PRIVILEGES", ResultAssertions(p => true)) {
//          resultTable()
//        }
      }
    }
    section("The `admin` role", "administration-built-in-roles-admin", "enterprise-edition") {
      p("The `admin` role can do the same as the <<administration-built-in-roles-architect,`architect`>>, as well as manage databases, users, roles and privileges.")
      section("Privileges of the `admin` role", "administration-built-in-roles-admin-privileges") {
        query("SHOW ROLE admin PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }
      }
      section("How to recreate the `admin` role", "administration-built-in-roles-admin-recreate") {
        initQueries(
          "DROP ROLE admin")  // setup so that later when the grant query gets executed, it will show systemUpdates: 1
        p(
          """
            |To restore the role to its original capabilities two steps are needed. First, if not already done, execute `DROP ROLE admin`.
            |Secondly, the following queries must be run in order to set up the privileges:""".stripMargin)
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

        p("The queries above are enough to grant most of the full admin capabilities. " +
          "Please note that the result of executing `SHOW ROLE admin PRIVILEGES` now appears to be slightly different from " +
          "the privileges shown for the original built-in `admin` role. This does not make any functional difference.")

        query("SHOW ROLE admin PRIVILEGES",ResultAssertions(p => true)) {
          resultTable()
        }

        p("Additional information about restoring the admin role can be found in the <<operations-manual#recover-admin-role, Operations Manual -> Recover the admin role>>.")
      }
    }
  }.build()
}
