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

class SecurityAdministrationTest extends DocumentingTest with QueryStatisticsTestSupport with PrivilegesTestBase {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Security of administration", "administration-security-administration")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "CREATE ROLE databaseAdminUsers",
      "CREATE ROLE noAccessUsers",
      "CREATE ROLE roleAdder",
      "CREATE ROLE roleNameModifier",
      "CREATE ROLE roleDropper",
      "CREATE ROLE roleAssigner",
      "CREATE ROLE roleRemover",
      "CREATE ROLE roleShower",
      "CREATE ROLE roleManager",
      "CREATE ROLE userAdder",
      "CREATE ROLE userNameModifier",
      "CREATE ROLE userModifier",
      "CREATE ROLE passwordModifier",
      "CREATE ROLE statusModifier",
      "CREATE ROLE homeModifier",
      "CREATE ROLE userDropper",
      "CREATE ROLE userShower",
      "CREATE ROLE userManager",
      "CREATE ROLE databaseAdder",
      "CREATE ROLE databaseDropper",
      "CREATE ROLE databaseManager",
      "CREATE ROLE privilegeShower",
      "CREATE ROLE privilegeAssigner",
      "CREATE ROLE privilegeRemover",
      "CREATE ROLE privilegeManager",
      "CREATE ROLE dbmsManager",
      "GRANT ROLE regularUsers TO jake",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers",
      "CREATE DATABASE otherDb"
    )
    synopsis("This section explains how to use Cypher to manage Neo4j administrative privileges.")
    p(
      """All of the commands described in the enclosing <<administration, Administration>> section require that the user executing the commands has the rights to do so.
        |These privileges can be conferred either by granting the user the `admin` role, which enables all administrative rights, or by granting specific combinations of privileges.
        |""".stripMargin)
    p(
      """
        |* <<administration-security-administration-introduction, The `admin` role>>
        |* <<access-control-database-administration, Database administration>>
        |** <<access-control-database-administration-access, The database `ACCESS` privilege>>
        |** <<access-control-database-administration-startstop, The database `START`/`STOP` privileges>>
        |** <<access-control-database-administration-index, The `INDEX MANAGEMENT` privileges>>
        |** <<access-control-database-administration-constraints, The `CONSTRAINT MANAGEMENT` privileges>>
        |** <<access-control-database-administration-tokens, The `NAME MANAGEMENT` privileges>>
        |** <<access-control-database-administration-all, Granting `ALL DATABASE PRIVILEGES`>>
        |** <<access-control-database-administration-transaction, Granting `TRANSACTION MANAGEMENT` privileges>>
        |* <<access-control-dbms-administration, DBMS administration>>
        |** <<administration-security-administration-dbms-custom, Using a custom role to manage DBMS privileges>>
        |** <<access-control-dbms-administration-role-management, The dbms `ROLE MANAGEMENT` privileges>>
        |** <<access-control-dbms-administration-user-management, The dbms `USER MANAGEMENT` privileges>>
        |** <<access-control-dbms-administration-database-management, The dbms `DATABASE MANAGEMENT` privileges>>
        |** <<access-control-dbms-administration-privilege-management, The dbms `PRIVILEGE MANAGEMENT` privileges>>
        |** <<access-control-dbms-administration-execute, The dbms `EXECUTE` privileges>>
        |*** <<execute-procedure-subsection, The dbms `EXECUTE PROCEDURE` privileges>>
        |*** <<boosted-execute-procedure-subsection, The dbms `EXECUTE BOOSTED PROCEDURE` privileges>>
        |*** <<admin-execute-procedure-subsection, The dbms `EXECUTE ADMIN PROCEDURES` privileges>>
        |*** <<execute-function-subsection, The dbms `EXECUTE USER DEFINED FUNCTION` privileges>>
        |*** <<boosted-execute-function-subsection, The dbms `EXECUTE BOOSTED USER DEFINED FUNCTION` privileges>>
        |*** <<name-globbing, Procedure and user defined function name-globbing>>
        |** <<access-control-dbms-administration-all, Granting `ALL DBMS PRIVILEGES`>>
        |""".stripMargin)
    section("The `admin` role", "administration-security-administration-introduction", "enterprise-edition") {
      p("include::admin-role-introduction.asciidoc[]")
      query("SHOW ROLE admin PRIVILEGES AS COMMANDS", assertPrivilegeShown(Seq(
        Map("command" -> "GRANT ACCESS ON DATABASE * TO `admin`")
      ))) {
        resultTable()
      }
      p("If the built-in admin role has been altered or dropped, and needs to be restored to its original state, see " +
        "<<operations-manual#password-and-user-recovery, Operations Manual -> Password and user recovery>>.")
    }
    section("Database administration", "access-control-database-administration", "enterprise-edition") {
      p("include::database/admin-role-database.asciidoc[]")
      p("The hierarchy between the different database privileges is shown in the image below.")
      p("image::privilege-hierarchy-database.png[title=\"Database privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/169TagNgf-aQtgcF1Df1K8SPrq-b77Tm57-vxs54z3nM/edit?usp=sharing
      p("include::database/admin-database-syntax.asciidoc[]")
      p("image::grant-privileges-database.png[title=\"Syntax of GRANT and DENY Database Privileges\"]")
      // image source: https://docs.google.com/drawings/d/1tQESJp-fcGjiZ97gWY7WkxKijBlwquafgYl_VNSpijk/edit?usp=sharing
      section("The database `ACCESS` privilege", "access-control-database-administration-access", "enterprise-edition") {
        p(
          """The `ACCESS` privilege enables users to connect to a database.
            |With `ACCESS` you can run calculations, for example, `RETURN 2*5 AS answer` or call functions `RETURN timestamp() AS time`.""".stripMargin)
        p("include::database/grant-database-access-syntax.asciidoc[]")

        p(
          """For example, granting the ability to access the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `ACCESS` privilege can also be denied.")
        p("include::database/deny-database-access-syntax.asciidoc[]")

        p("For example, denying the ability to access to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES AS COMMANDS", assertPrivilegeShown(Seq(
          Map("command" -> "GRANT ACCESS ON DATABASE `neo4j` TO `regularUsers`"),
          Map("command" -> "DENY ACCESS ON DATABASE `neo4j` TO `regularUsers`")
        ))) {
          resultTable()
        }
      }
      section("The database `START`/`STOP` privileges", "access-control-database-administration-startstop", "enterprise-edition") {
        p(
          """The `START` privilege can be used to enable the ability to start a database.""".stripMargin)
        p("include::database/grant-database-start-syntax.asciidoc[]")

        p(
          """For example, granting the ability to start the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT START ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `START` privilege can also be denied.")
        p("include::database/deny-database-start-syntax.asciidoc[]")

        p("For example, denying the ability to start to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY START ON DATABASE system TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p(
          """The `STOP` privilege can be used to enable the ability to stop a database.""".stripMargin)
        p("include::database/grant-database-stop-syntax.asciidoc[]")

        p(
          """For example, granting the ability to stop the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT STOP ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `STOP` privilege can also be denied.")
        p("include::database/deny-database-stop-syntax.asciidoc[]")

        p("For example, denying the ability to stop to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY STOP ON DATABASE system TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES AS COMMANDS", assertPrivilegeShown(Seq(
          Map("command" -> "GRANT ACCESS ON DATABASE `neo4j` TO `regularUsers`"),
          Map("command" -> "DENY ACCESS ON DATABASE `neo4j` TO `regularUsers`"),
          Map("command" -> "GRANT START ON DATABASE `neo4j` TO `regularUsers`"),
          Map("command" -> "DENY START ON DATABASE `system` TO `regularUsers`"),
          Map("command" -> "GRANT STOP ON DATABASE `neo4j` TO `regularUsers`"),
          Map("command" -> "DENY STOP ON DATABASE `system` TO `regularUsers`")
        ))) {
          resultTable()
        }

        note {
          p("Note that `START` and `STOP` privileges are not included in the <<access-control-database-administration-all, `ALL DATABASE PRIVILEGES`>>.")
        }
      }
      section("The `INDEX MANAGEMENT` privileges", "access-control-database-administration-index", "enterprise-edition") {
        p(
          """Indexes can be created, deleted, or listed with the `CREATE INDEX`, `DROP INDEX`, and `SHOW INDEXES` commands.
            |The privilege to do this can be granted with `GRANT CREATE INDEX`, `GRANT DROP INDEX`, and `GRANT SHOW INDEX` commands.
            |The privilege to do all three can be granted with `GRANT INDEX MANAGEMENT` command.""".stripMargin)
        p("include::database/index-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create indexes on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE INDEX ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p(
          """The `SHOW INDEXES` privilege only affects the <<administration-indexes-list-indexes, `SHOW INDEXES` command>>
            | and not the old procedures for listing indexes, such as `db.indexes`.""".stripMargin)
      }
      section("The `CONSTRAINT MANAGEMENT` privileges", "access-control-database-administration-constraints", "enterprise-edition") {
        p(
          """Constraints can be created, deleted, or listed with the `CREATE CONSTRAINT`, `DROP CONSTRAINT` and `SHOW CONSTRAINTS` commands.
            |The privilege to do this can be granted with `GRANT CREATE CONSTRAINT`, `GRANT DROP CONSTRAINT`, `GRANT SHOW CONSTRAINT` commands.
            |The privilege to do all three can be granted with `GRANT CONSTRAINT MANAGEMENT` command.""".stripMargin)
        p("include::database/constraint-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create constraints on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE CONSTRAINT ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p(
          """The `SHOW CONSTRAINTS` privilege only affects the <<administration-constraints-syntax-list, `SHOW CONSTRAINTS` command>>
            | and not the old procedures for listing constraints, such as `db.constraints`.""".stripMargin)
      }
      section("The `NAME MANAGEMENT` privileges", "access-control-database-administration-tokens", "enterprise-edition") {
        p(
          """The right to create new labels, relationship types, and property names is different from the right to create nodes, relationships, and properties.
            |The latter is managed using database `WRITE` privileges, while the former is managed using specific `GRANT/DENY CREATE NEW ...` commands for each type.""".stripMargin)
        p("include::database/name-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create new properties on nodes or relationships in the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE NEW PROPERTY NAME ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("Granting `ALL DATABASE PRIVILEGES`", "access-control-database-administration-all", "enterprise-edition") {
        p(
          """The right to access a database, create and drop indexes and constraints and create new labels, relationship types or property names can be achieved with a single command:""".stripMargin)
        p("include::database/all-management-syntax.asciidoc[]")

        note {
          p(
            """Note that the privileges for starting and stopping all databases, and transaction management, are not included in the `ALL DATABASE PRIVILEGES` grant.
              |These privileges are associated with administrators while other database privileges are of use to domain and application developers.""".stripMargin)
        }

        p(
          """For example, granting the abilities above on the database `neo4j` to the role `databaseAdminUsers` is done using the following query.""".stripMargin)
        query("GRANT ALL DATABASE PRIVILEGES ON DATABASE neo4j TO databaseAdminUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE databaseAdminUsers PRIVILEGES AS COMMANDS", assertPrivilegeShown(Seq(
          Map("command" -> "GRANT ALL DATABASE PRIVILEGES ON DATABASE `neo4j` TO `databaseAdminUsers`")
        ))) {
          resultTable()
        }
      }
      section("Granting `TRANSACTION MANAGEMENT` privileges", "access-control-database-administration-transaction", "enterprise-edition") {
        p(
          """The right to run the procedures `dbms.listTransactions`, `dbms.listQueries`, `dbms.killQuery`, `dbms.killQueries`,
            |`dbms.killTransaction` and `dbms.killTransactions` are managed through the `SHOW TRANSACTION` and `TERMINATE TRANSACTION` privileges.""".stripMargin)
        p("include::database/transaction-management-syntax.asciidoc[]")

        note {
          p("Note that the `TRANSACTION MANAGEMENT` privileges are not included in the <<access-control-database-administration-all, `ALL DATABASE PRIVILEGES`>>.")
        }

        p(
          """For example, granting the ability to list transactions for user `jake` in the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT SHOW TRANSACTION (jake) ON DATABASE neo4j TO regularUsers", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
    }

    section("DBMS administration", "administration-security-administration-dbms-privileges", "enterprise-edition") {
      p(
        """All DBMS privileges are relevant system-wide. Like user management, they do not belong to one specific database or graph.
          |For more details on the differences between graphs, databases and the DBMS, refer to <<neo4j-databases-graphs>>.""".stripMargin)
      p("image::grant-privileges-dbms.png[title=\"Syntax of GRANT and DENY DBMS Privileges\"]")
      // image source: https://docs.google.com/drawings/d/1UAyLvL7UdwYM1I9RVrwxnvWhB4zSu9cv4rCzzqC3-iU/edit?usp=sharing
      p("image::privilege-hierarchy-dbms.png[title=\"DBMS privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/1iTj0-Sv3UwGFOfuRBUt86AcpN1UPz8GNc15nz4aFZuA/edit?usp=sharing
      p("include::dbms/admin-role-dbms.asciidoc[]")

      section("Using a custom role to manage DBMS privileges", "administration-security-administration-dbms-custom", "enterprise-edition") {
        p(
          """If it is desired to have an administrator with a subset of privileges that includes all DBMS privileges, but not all database privileges, this can be achieved in multiple ways.
            |One way is to copy the `admin` role and revoking or denying the unwanted privileges.
            |A second option is to build a custom administrator from scratch by granting the wanted privileges instead.
            |""".stripMargin)
        p("As an example, let's create an administrator that can only manage users and roles by using the latter option.")
        p("First we create the new role:")
        query("CREATE ROLE usermanager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("Then we grant the privilege to manage users:")
        query("GRANT USER MANAGEMENT ON DBMS TO usermanager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And to manage roles:")
        query("GRANT ROLE MANAGEMENT ON DBMS TO usermanager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting role has privileges that only allow user and role management:")
        query("SHOW ROLE usermanager PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'usermanager'")
          resultTable()
        }

        p(
          """However, this role doesn't allow all DBMS capabilities.
            |For example, the role is missing privilege management, creating and dropping databases as well as executing admin procedures.
            |We can make a more powerful administrator by granting a different set of privileges.
            |Let's create an administrator that can perform almost all DBMS capabilities, excluding database management, but also with some limited database capabilities, such as managing transactions.""".stripMargin)
        p("Again, we start by creating a new role:")
        query("CREATE ROLE customAdministrator", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("Then we grant the privilege for all DBMS capabilities:")
        query("GRANT ALL DBMS PRIVILEGES ON DBMS TO customAdministrator", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And explicitly deny the privilege to manage databases:")
        query("DENY DATABASE MANAGEMENT ON DBMS TO customAdministrator", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("Thereafter we grant the transaction management privilege:")
        query("GRANT TRANSACTION MANAGEMENT (*) ON DATABASE * TO customAdministrator", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting role has privileges that allow all DBMS privileges except creating and dropping databases, as well as managing transactions:")
        query("SHOW ROLE customAdministrator PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'customAdministrator'")
          resultTable()
        }
      }

      section("The dbms `ROLE MANAGEMENT` privileges", "access-control-dbms-administration-role-management", "enterprise-edition") {
        p("The dbms privileges for role management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/role-management-syntax.asciidoc[]")

        p("The ability to add roles can be granted via the `CREATE ROLE` privilege. The following query shows an example of this:")
        query("GRANT CREATE ROLE ON DBMS TO roleAdder", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow adding roles:")
        query("SHOW ROLE roleAdder PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleAdder'")
          resultTable()
        }

        p("The ability to rename roles can be granted via the `RENAME ROLE` privilege. The following query shows an example of this:")
        query("GRANT RENAME ROLE ON DBMS TO roleNameModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow renaming roles:")
        query("SHOW ROLE roleNameModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleNameModifier'")
          resultTable()
        }

        p("The ability to delete roles can be granted via the `DROP ROLE` privilege. The following query shows an example of this:")
        query("GRANT DROP ROLE ON DBMS TO roleDropper", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow deleting roles:")
        query("SHOW ROLE roleDropper PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleDropper'")
          resultTable()
        }

        p("The ability to assign roles to users can be granted via the `ASSIGN ROLE` privilege. The following query shows an example of this:")
        query("GRANT ASSIGN ROLE ON DBMS TO roleAssigner", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow assigning/granting roles:")
        query("SHOW ROLE roleAssigner PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleAssigner'")
          resultTable()
        }

        p("The ability to remove roles from users can be granted via the `REMOVE ROLE` privilege. The following query shows an example of this:")
        query("GRANT REMOVE ROLE ON DBMS TO roleRemover", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow removing/revoking roles:")
        query("SHOW ROLE roleRemover PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleRemover'")
          resultTable()
        }

        p("The ability to show roles can be granted via the `SHOW ROLE` privilege. A user with this privilege is allowed to execute the `SHOW ROLES` and `SHOW POPULATED ROLES` administration commands. " +
          "For the `SHOW ROLES WITH USERS` and `SHOW POPULATED ROLES WITH USERS` administration commands, both this privilege and the `SHOW USER` privilege are required. The following query shows an example of how to grant the `SHOW ROLE` privilege:")
        query("GRANT SHOW ROLE ON DBMS TO roleShower", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow showing roles:")
        query("SHOW ROLE roleShower PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleShower'")
          resultTable()
        }

        p(
          """The privileges to create, rename, delete, assign, remove, and list roles can be granted via the `ROLE MANAGEMENT` privilege.
            |The following query shows an example of this:""".stripMargin)
        query("GRANT ROLE MANAGEMENT ON DBMS TO roleManager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has all privileges to manage roles:")
        query("SHOW ROLE roleManager PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'roleManager'")
          resultTable()
        }
      }

      section("The dbms `USER MANAGEMENT` privileges", "access-control-dbms-administration-user-management", "enterprise-edition") {
        p("The dbms privileges for user management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/user-management-syntax.asciidoc[]")

        p("The ability to add users can be granted via the `CREATE USER` privilege. The following query shows an example of this:")
        query("GRANT CREATE USER ON DBMS TO userAdder", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow adding users:")
        query("SHOW ROLE userAdder PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userAdder'")
          resultTable()
        }

        p("The ability to rename users can be granted via the `RENAME USER` privilege. The following query shows an example of this:")
        query("GRANT RENAME USER ON DBMS TO userNameModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow renaming users:")
        query("SHOW ROLE userNameModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userNameModifier'")
          resultTable()
        }

        p("The ability to modify users can be granted via the `ALTER USER` privilege. The following query shows an example of this:")
        query("GRANT ALTER USER ON DBMS TO userModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow modifying users:")
        query("SHOW ROLE userModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userModifier'")
          resultTable()
        }
        p("A user that is granted `ALTER USER` is allowed to run the `ALTER USER` administration command with one or several of the `SET PASSWORD`, `SET PASSWORD CHANGE [NOT] REQUIRED` and `SET STATUS` parts:")
        query("ALTER USER jake SET PASSWORD 'secret' SET STATUS SUSPENDED", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The ability to modify users' passwords and whether those passwords must be changed upon first login can be granted via the `SET PASSWORDS` privilege. The following query shows an example of this:")
        query("GRANT SET PASSWORDS ON DBMS TO passwordModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow modifying users' passwords and whether those passwords must be changed upon first login:")
        query("SHOW ROLE passwordModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'passwordModifier'")
          resultTable()
        }
        p("A user that is granted `SET PASSWORDS` is allowed to run the `ALTER USER` administration command with one or both of the `SET PASSWORD` and `SET PASSWORD CHANGE [NOT] REQUIRED` parts:")
        query("ALTER USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The ability to modify the account status of users can be granted via the `SET USER STATUS` privilege. The following query shows an example of this:")
        query("GRANT SET USER STATUS ON DBMS TO statusModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow modifying the account status of users:")
        query("SHOW ROLE statusModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'statusModifier'")
          resultTable()
        }
        p("A user that is granted `SET USER STATUS` is allowed to run the `ALTER USER` administration command with only the `SET STATUS` part:")
        query("ALTER USER jake SET STATUS ACTIVE", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The ability to modify the home database of users can be granted via the `SET USER HOME DATABASE` privilege. The following query shows an example of this:")
        query("GRANT SET USER HOME DATABASE ON DBMS TO statusModifier", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow modifying the home database of users:")
        query("SHOW ROLE statusModifier PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'statusModifier'")
          resultTable()
        }
        p("A user that is granted `SET USER HOME DATABASE` is allowed to run the `ALTER USER` administration command with only the `SET HOME DATABASE` or `REMOVE HOME DATABASE` part:")
        query("ALTER USER jake SET HOME DATABASE otherDb", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("ALTER USER jake REMOVE HOME DATABASE", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        note {
          p("Note that the combination of the `SET PASSWORDS`, `SET USER STATUS`, and the `SET USER HOME DATABASE` privilege actions is equivalent to the `ALTER USER` privilege action.")
        }

        p("The ability to delete users can be granted via the `DROP USER` privilege. The following query shows an example of this:")
        query("GRANT DROP USER ON DBMS TO userDropper", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow deleting users:")
        query("SHOW ROLE userDropper PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userDropper'")
          resultTable()
        }

        p("The ability to show users can be granted via the `SHOW USER` privilege. The following query shows an example of this:")
        query("GRANT SHOW USER ON DBMS TO userShower", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow showing users:")
        query("SHOW ROLE userShower PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userShower'")
          resultTable()
        }

        p(
          """The privileges to create, rename, modify, delete, and list users can be granted via the `USER MANAGEMENT` privilege.
            |The following query shows an example of this:""".stripMargin)
        query("GRANT USER MANAGEMENT ON DBMS TO userManager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has all privileges to manage users:")
        query("SHOW ROLE userManager PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'userManager'")
          resultTable()
        }
      }

      section("The dbms `DATABASE MANAGEMENT` privileges", "access-control-dbms-administration-database-management", "enterprise-edition") {
        p("The dbms privileges for database management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/database-management-syntax.asciidoc[]")

        p("The ability to create databases can be granted via the `CREATE DATABASE` privilege. The following query shows an example of this:")
        query("GRANT CREATE DATABASE ON DBMS TO databaseAdder", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow creating databases:")
        query("SHOW ROLE databaseAdder PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'databaseAdder'")
          resultTable()
        }

        p("The ability to delete databases can be granted via the `DROP DATABASE` privilege. The following query shows an example of this:")
        query("GRANT DROP DATABASE ON DBMS TO databaseDropper", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow deleting databases:")
        query("SHOW ROLE databaseDropper PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'databaseDropper'")
          resultTable()
        }

        p(
          """The privileges to create and delete databases can be granted via the `DATABASE MANAGEMENT` privilege.
            |The following query shows an example of this:""".stripMargin)
        query("GRANT DATABASE MANAGEMENT ON DBMS TO databaseManager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has all privileges to manage databases:")
        query("SHOW ROLE databaseManager PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'databaseManager'")
          resultTable()
        }
      }

      section("The dbms `PRIVILEGE MANAGEMENT` privileges", "access-control-dbms-administration-privilege-management", "enterprise-edition") {
        p("The dbms privileges for privilege management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/privilege-management-syntax.asciidoc[]")

        p("The ability to list privileges can be granted via the `SHOW PRIVILEGE` privilege. A user with this privilege is allowed to execute the `SHOW PRIVILEGES` and `SHOW ROLE roleName PRIVILEGES` administration commands. " +
          "For the `SHOW USER username PRIVILEGES` administration command, both this privilege and the `SHOW USER` privilege are required. The following query shows an example of how to grant the `SHOW PRIVILEGE` privilege:")
        query("GRANT SHOW PRIVILEGE ON DBMS TO privilegeShower", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow showing privileges:")
        query("SHOW ROLE privilegeShower PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'privilegeShower'")
          resultTable()
        }

        note {
          p("Note that no specific privileges are required for showing the current user's privileges using either `SHOW USER _username_ PRIVILEGES`, or `SHOW USER PRIVILEGES`.")
          p("Please note that if a non-native auth provider like LDAP is in use, `SHOW USER PRIVILEGES` will only work in a limited capacity; " +
            "It is only possible for a user to show their own privileges. Other users' privileges cannot be listed when using a non-native auth provider.")
        }
        p("The ability to assign privileges to roles can be granted via the `ASSIGN PRIVILEGE` privilege. A user with this privilege is allowed to execute GRANT and DENY administration commands. The following query shows an example of how to grant this privilege:")
        query("GRANT ASSIGN PRIVILEGE ON DBMS TO privilegeAssigner", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow assigning privileges:")
        query("SHOW ROLE privilegeAssigner PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'privilegeAssigner'")
          resultTable()
        }

        p("The ability to remove privileges from roles can be granted via the `REMOVE PRIVILEGE` privilege. A user with this privilege is allowed to execute REVOKE administration commands. The following query shows an example of how to grant this privilege:")
        query("GRANT REMOVE PRIVILEGE ON DBMS TO privilegeRemover", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has privileges that only allow removing privileges:")
        query("SHOW ROLE privilegeRemover PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'privilegeRemover'")
          resultTable()
        }

        p(
          """The privileges to list, assign, and remove privileges can be granted via the `PRIVILEGE MANAGEMENT` privilege.
            |The following query shows an example of this:""".stripMargin)
        query("GRANT PRIVILEGE MANAGEMENT ON DBMS TO privilegeManager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role has all privileges to manage privileges:")
        query("SHOW ROLE privilegeManager PRIVILEGES AS COMMANDS", NoAssertions) {
          p("Lists all privileges for role 'privilegeManager'")
          resultTable()
        }
      }

      section("The dbms `EXECUTE` privileges", "access-control-dbms-administration-execute", "enterprise-edition") {
        initQueries(
          "CREATE ROLE procedureExecutor",
          "CREATE ROLE deniedProcedureExecutor",
          "CREATE ROLE boostedProcedureExecutor",
          "CREATE ROLE deniedBoostedProcedureExecutor1",
          "CREATE ROLE deniedBoostedProcedureExecutor2",
          "CREATE ROLE deniedBoostedProcedureExecutor3",
          "CREATE ROLE deniedBoostedProcedureExecutor4",
          "CREATE ROLE adminProcedureExecutor",
          "CREATE ROLE functionExecutor",
          "CREATE ROLE deniedFunctionExecutor",
          "CREATE ROLE boostedFunctionExecutor",
          "CREATE ROLE globbing1",
          "CREATE ROLE globbing2",
          "CREATE ROLE globbing3",
          "CREATE ROLE globbing4",
          "CREATE ROLE globbing5"
        )
        p("The dbms privileges for procedure and user defined function execution are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/execute-syntax.asciidoc[]")
        p(
          """The `EXECUTE BOOSTED` privileges replace the `dbms.security.procedures.default_allowed` and `dbms.security.procedures.roles` configuration parameters for procedures and user defined functions.
            |The configuration parameters are still honoured as a set of temporary privileges. These cannot be revoked, but will be updated on each restart
            |with the current configuration values.""".stripMargin)

        section("The `EXECUTE PROCEDURE` privilege", "execute-procedure-subsection", "enterprise-edition") {
          p(
            """The ability to execute a procedure can be granted via the `EXECUTE PROCEDURE` privilege.
              |A user with this privilege is allowed to execute the procedures matched by the <<name-globbing, name-globbing>>.
              |The following query shows an example of how to grant this privilege:""".stripMargin)
          query("GRANT EXECUTE PROCEDURE db.schema.* ON DBMS TO procedureExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'procedureExecutor' can then run any procedure in the `db.schema` namespace. The procedure is run using the user's own privileges.")
          }
          p("The resulting role has privileges that only allow executing procedures in the `db.schema` namespace:")
          query("SHOW ROLE procedureExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'procedureExecutor'")
            resultTable()
          }

          p(
            """If we want to allow executing all but a few procedures, we can grant `EXECUTE PROCEDURES *` and deny the unwanted procedures.
              |For example, the following queries allow for executing all procedures, except those starting with `dbms.killTransaction`:""".stripMargin)
          query("GRANT EXECUTE PROCEDURE * ON DBMS TO deniedProcedureExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE PROCEDURE dbms.killTransaction* ON DBMS TO deniedProcedureExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p("The resulting role has privileges that only allow executing all procedures except those starting with `dbms.killTransaction`:")
          query("SHOW ROLE deniedProcedureExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedProcedureExecutor'")
            resultTable()
            p("The `dbms.killTransaction` and `dbms.killTransactions` are blocked, as well as any other procedures starting with `dbms.killTransaction`.")
          }
        }

        section("The `EXECUTE BOOSTED PROCEDURE` privilege", "boosted-execute-procedure-subsection", "enterprise-edition") {
          p(
            """The ability to execute a procedure with elevated privileges can be granted via the `EXECUTE BOOSTED PROCEDURE` privilege.
              |A user with this privilege is allowed to execute the procedures matched by the <<name-globbing, name-globbing>>
              |without the execution being restricted to their other privileges.
              |There is no need to grant an individual `EXECUTE PROCEDURE` privilege for the procedures either,
              |as granting the `EXECUTE BOOSTED PROCEDURE` includes an implicit `EXECUTE PROCEDURE` grant for them.
              |A denied `EXECUTE PROCEDURE` still denies executing the procedure.
              |The following query shows an example of how to grant this privilege:""".stripMargin)
          query("GRANT EXECUTE BOOSTED PROCEDURE db.labels, db.relationshipTypes ON DBMS TO boostedProcedureExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 2)
          })) {
            statsOnlyResultTable()
            p(
              """Users with the role 'boostedProcedureExecutor' can then run `db.labels` and `db.relationshipTypes` with full privileges,
                |seeing everything in the graph not just the labels and types that the user has `TRAVERSE` privilege on.""".stripMargin)
          }
          p("The resulting role has privileges that only allow executing procedures `db.labels` and `db.relationshipTypes`, but with elevated execution:")
          query("SHOW ROLE boostedProcedureExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'boostedProcedureExecutor'")
            resultTable()
          }

          p(
            """While granting `EXECUTE BOOSTED PROCEDURE` on its own allows the procedure to be both executed and given elevated privileges during the execution,
              |the deny behaves slightly differently and only denies the elevation and not the execution. However, a user with only a granted `EXECUTE BOOSTED PROCEDURE`
              |and a denied `EXECUTE BOOSTED PROCEDURE` will deny the execution as well. This is explained through the examples below:""".stripMargin)

          p("Example 1: Grant `EXECUTE PROCEDURE` and deny `EXECUTE BOOSTED PROCEDURE`")
          query("GRANT EXECUTE PROCEDURE * ON DBMS TO deniedBoostedProcedureExecutor1", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE BOOSTED PROCEDURE db.labels ON DBMS TO deniedBoostedProcedureExecutor1", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p(
            """The resulting role has privileges that allow executing all procedures using the user's own privileges,
              |as well as blocking `db.labels` from being elevated. The deny `EXECUTE BOOSTED PROCEDURE` does not block execution of `db.labels`.""".stripMargin)
          query("SHOW ROLE deniedBoostedProcedureExecutor1 PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedBoostedProcedureExecutor1'")
            resultTable()
          }

          p("Example 2: Grant `EXECUTE BOOSTED PROCEDURE` and deny `EXECUTE PROCEDURE`")
          query("GRANT EXECUTE BOOSTED PROCEDURE * ON DBMS TO deniedBoostedProcedureExecutor2", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE PROCEDURE db.labels ON DBMS TO deniedBoostedProcedureExecutor2", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p(
            """The resulting role has privileges that allow executing all procedures with elevated privileges
              |except `db.labels` which is not allowed to execute at all:""".stripMargin)
          query("SHOW ROLE deniedBoostedProcedureExecutor2 PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedBoostedProcedureExecutor2'")
            resultTable()
          }

          p("Example 3: Grant `EXECUTE BOOSTED PROCEDURE` and deny `EXECUTE BOOSTED PROCEDURE`")
          query("GRANT EXECUTE BOOSTED PROCEDURE * ON DBMS TO deniedBoostedProcedureExecutor3", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE BOOSTED PROCEDURE db.labels ON DBMS TO deniedBoostedProcedureExecutor3", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p(
            """The resulting role has privileges that allow executing all procedures with elevated privileges
              |except `db.labels` which is not allowed to execute at all:""".stripMargin)
          query("SHOW ROLE deniedBoostedProcedureExecutor3 PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedBoostedProcedureExecutor3'")
            resultTable()
          }

          p("Example 4: Grant `EXECUTE PROCEDURE` and `EXECUTE BOOSTED PROCEDURE` and deny `EXECUTE BOOSTED PROCEDURE`")
          query("GRANT EXECUTE PROCEDURE db.labels ON DBMS TO deniedBoostedProcedureExecutor4", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("GRANT EXECUTE BOOSTED PROCEDURE * ON DBMS TO deniedBoostedProcedureExecutor4", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE BOOSTED PROCEDURE db.labels ON DBMS TO deniedBoostedProcedureExecutor4", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p(
            """The resulting role has privileges that allow executing all procedures with elevated privileges
              |except `db.labels` which is only allowed to execute using the user's own privileges:""".stripMargin)
          query("SHOW ROLE deniedBoostedProcedureExecutor4 PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedBoostedProcedureExecutor4'")
            resultTable()
          }

          p("Example 5: How would the privileges from example 1-4 affect the output of a procedure?")
          p(
            """Let's assume there exists a procedure called `myProc`.
              |This procedure gives the result `A` and `B` for a user with `EXECUTE PROCEDURE` privilege
              |and `A`, `B` and `C` for a user with `EXECUTE BOOSTED PROCEDURE` privilege.
              |Now, let's adapt the privileges in examples 1 to 4 to apply to this procedure and show what is returned.""".stripMargin)
          p(
            """With the privileges from example 1, granted `EXECUTE PROCEDURE *` and denied `EXECUTE BOOSTED PROCEDURE myProc`,
              |the `myProc` procedure returns the result `A` and `B`.""".stripMargin)
          p(
            """With the privileges from example 2, granted `EXECUTE BOOSTED PROCEDURE *` and denied `EXECUTE PROCEDURE myProc`,
              |execution of the `myProc` procedure is not allowed.""".stripMargin)
          p(
            """With the privileges from example 3, granted `EXECUTE BOOSTED PROCEDURE *` and denied `EXECUTE BOOSTED PROCEDURE myProc`,
              |execution of the `myProc` procedure is not allowed.""".stripMargin)
          p(
            """With the privileges from example 4, granted `EXECUTE PROCEDURE myProc` and `EXECUTE BOOSTED PROCEDURE *` and denied `EXECUTE BOOSTED PROCEDURE myProc`,
              |the `myProc` procedure returns the result `A` and `B`.""".stripMargin)
          p(
            """For comparison, when only granted `EXECUTE BOOSTED PROCEDURE myProc`,
              |the `myProc` procedure returns the result `A`, `B` and `C`,
              |without needing to be granted the `EXECUTE PROCEDURE myProc` privilege.""".stripMargin)
        }

        section("The `EXECUTE ADMIN PROCEDURES` privilege", "admin-execute-procedure-subsection", "enterprise-edition") {
          p(
            """The ability to execute admin procedures (annotated with `@Admin`) can be granted via the `EXECUTE ADMIN PROCEDURES` privilege.
              |This privilege is equivalent with granting <<boosted-execute-procedure-subsection, the `EXECUTE BOOSTED PROCEDURE` privilege>> on each of the admin procedures.
              |Any new admin procedures that gets added are automatically included in this privilege.
              |The following query shows an example of how to grant this privilege:""".stripMargin)
          query("GRANT EXECUTE ADMIN PROCEDURES ON DBMS TO adminProcedureExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'adminProcedureExecutor' can then run any admin procedure with elevated privileges.")
          }
          p("The resulting role has privileges that allow executing all admin procedures:")
          query("SHOW ROLE adminProcedureExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'adminProcedureExecutor'")
            resultTable()
          }

          p(
            """To compare this with the `EXECUTE PROCEDURE` and `EXECUTE BOOSTED PROCEDURE` privileges, let's revisit the `myProc` procedure.
              |This time as an admin procedure, which gives the result `A`, `B` and `C` when allowed to execute.""".stripMargin)
          p(
            """Let's start with a user only granted the `EXECUTE PROCEDURE myProc` privilege,
              |execution of the `myProc` procedure is not allowed.""".stripMargin)
          p(
            """However, for a user granted `EXECUTE BOOSTED PROCEDURE myProc` or `EXECUTE ADMIN PROCEDURES`,
              |the `myProc` procedure returns the result `A`, `B` and `C`.""".stripMargin)
          p(
            """Any denied execute privilege results in the procedure not being allowed to execute.
              |It does not matter whether `EXECUTE PROCEDURE`, `EXECUTE BOOSTED PROCEDURE` or `EXECUTE ADMIN PROCEDURES` is denied.""".stripMargin)
        }

        section("The `EXECUTE USER DEFINED FUNCTION` privilege", "execute-function-subsection", "enterprise-edition") {
          p(
            """The ability to execute a user defined function (UDF) can be granted via the `EXECUTE USER DEFINED FUNCTION` privilege.
              |A user with this privilege is allowed to execute the UDFs matched by the <<name-globbing, name-globbing>>.
              |The following query shows an example of how to grant this privilege:""".stripMargin)
          query("GRANT EXECUTE FUNCTION apoc.coll.* ON DBMS TO functionExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'functionExecutor' can then run any UDF in the `apoc.coll` namespace. The function is run using the user's own privileges.")
          }
          p("The resulting role has privileges that only allow executing UDFs in the `apoc.coll` namespace:")
          query("SHOW ROLE functionExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'functionExecutor'")
            resultTable()
          }

          note {
            p("The `EXECUTE USER DEFINED FUNCTION` privileges do not apply to built-in functions which are always executable.")
          }

          p(
            """If we want to allow executing all but a few UDFs, we can grant `EXECUTE USER DEFINED FUNCTIONS *` and deny the unwanted functions.
              |For example, the following queries allow for executing all UDFs except those starting with `apoc.any.prop`:""".stripMargin)
          query("GRANT EXECUTE FUNCTIONS * ON DBMS TO deniedFunctionExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          query("DENY EXECUTE FUNCTION apoc.any.prop* ON DBMS TO deniedFunctionExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
          }
          p("The resulting role has privileges that only allow executing all procedures except those starting with `apoc.any.prop`:")
          query("SHOW ROLE deniedFunctionExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'deniedFunctionExecutor'")
            resultTable()
            p("The `apoc.any.property` and `apoc.any.properties` is blocked, as well as any other procedures starting with `apoc.any.prop`.")
          }
        }

        section("The `EXECUTE BOOSTED USER DEFINED FUNCTION` privilege", "boosted-execute-function-subsection", "enterprise-edition") {
          p(
            """The ability to execute a user defined function (UDF) with elevated privileges can be granted via the `EXECUTE BOOSTED USER DEFINED FUNCTION` privilege.
              |A user with this privilege is allowed to execute the UDFs matched by the <<name-globbing, name-globbing>>
              |without the execution being restricted to their other privileges.
              |There is no need to grant an individual `EXECUTE USER DEFINED FUNCTION` privilege for the functions either,
              |as granting the `EXECUTE BOOSTED USER DEFINED FUNCTION` includes an implicit `EXECUTE USER DEFINED FUNCTION` grant for them.
              |A denied `EXECUTE USER DEFINED FUNCTION` still denies executing the function.
              |The following query shows an example of how to grant this privilege:""".stripMargin)
          query("GRANT EXECUTE BOOSTED FUNCTION apoc.any.properties ON DBMS TO boostedFunctionExecutor", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p(
              """Users with the role 'boostedFunctionExecutor' can then run `apoc.any.properties` with full privileges,
                |seeing every property on the node/relationship not just the properties that the user has `READ` privilege on.""".stripMargin)
          }
          p("The resulting role has privileges that only allow executing the UDF `apoc.any.properties`, but with elevated execution:")
          query("SHOW ROLE boostedFunctionExecutor PRIVILEGES AS COMMANDS", NoAssertions) {
            p("Lists all privileges for role 'boostedFunctionExecutor'")
            resultTable()
          }

          note {
            p("The `EXECUTE BOOSTED USER DEFINED FUNCTION` privileges do not apply to built-in functions, as they have no concept of elevated privileges.")
          }

          p(
            """While granting `EXECUTE BOOSTED USER DEFINED FUNCTION` on its own allows the UDF to be both executed and given elevated privileges during the execution,
              |the deny behaves slightly differently and only denies the elevation and not the execution. However, a user with only a granted `EXECUTE BOOSTED USER DEFINED FUNCTION`
              |and a denied `EXECUTE BOOSTED USER DEFINED FUNCTION` denies the execution as well.
              |This is the same behavior as for `EXECUTE BOOSTED PROCEDURE`, for examples see <<boosted-execute-procedure-subsection>>""".stripMargin)
        }

        section("Procedure and user defined function name-globbing", "name-globbing", "enterprise-edition") {
          p(
            """The name-globbing for procedure and user defined function names is a simplified version of globbing for filename expansions, only allowing two wildcard characters -- `+*+` and `?`.
              |They are used for multiple and single character matches, where `+*+` means 0 or more characters and `?` matches exactly one character.""".stripMargin)
          p(
            """The examples below only use procedures but the same rules apply to user defined function names.
              |For the examples below, assume we have the following procedures:
              |
              |* mine.public.exampleProcedure
              |* mine.public.exampleProcedure1
              |* mine.public.exampleProcedure42
              |* mine.private.exampleProcedure
              |* mine.private.exampleProcedure1
              |* mine.private.exampleProcedure2
              |* your.exampleProcedure
              |""".stripMargin)

          query("GRANT EXECUTE PROCEDURE * ON DBMS TO globbing1", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'globbing1' can then run procedures all the procedures.")
          }

          query("GRANT EXECUTE PROCEDURE mine.*.exampleProcedure ON DBMS TO globbing2", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'globbing2' can then run procedures `mine.public.exampleProcedure` and `mine.private.exampleProcedure`, but none of the others.")
          }

          query("GRANT EXECUTE PROCEDURE mine.*.exampleProcedure? ON DBMS TO globbing3", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'globbing3' can then run procedures `mine.public.exampleProcedure1`, `mine.private.exampleProcedure1` and `mine.private.exampleProcedure2`, but none of the others.")
          }

          query("GRANT EXECUTE PROCEDURE *.exampleProcedure ON DBMS TO globbing4", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'globbing4' can then run procedures `your.exampleProcedure`, `mine.public.exampleProcedure` and `mine.private.exampleProcedure`, but none of the others.")
          }

          query("GRANT EXECUTE PROCEDURE mine.public.exampleProcedure* ON DBMS TO globbing5", ResultAssertions(r => {
            assertStats(r, systemUpdates = 1)
          })) {
            statsOnlyResultTable()
            p("Users with the role 'globbing5' can then run procedures `mine.public.exampleProcedure`, `mine.public.exampleProcedure1` and `mine.public.exampleProcedure42`, but none of the others.")
          }
        }
      }

      section("Granting `ALL DBMS PRIVILEGES`", "access-control-dbms-administration-all", "enterprise-edition") {
        p(
          """The right to perform the following privileges can be achieved with a single command:
            |
            |* create roles
            |* drop roles
            |* assign roles
            |* remove roles
            |* show roles
            |* create users
            |* alter users
            |* drop users
            |* show users
            |* create databases
            |* drop databases
            |* show privileges
            |* assign privileges
            |* remove privileges
            |* execute all procedures with elevated privileges
            |* execute all user defined functions with elevated privileges""".stripMargin)
        p("include::dbms/all-management-syntax.asciidoc[]")

        p(
          """For example, granting the abilities above to the role `dbmsManager` is done using the following query.""".stripMargin)
        query("GRANT ALL DBMS PRIVILEGES ON DBMS TO dbmsManager", ResultAssertions(r => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE dbmsManager PRIVILEGES AS COMMANDS", assertPrivilegeShown(Seq(
          Map("command" -> "GRANT ALL DBMS PRIVILEGES ON DBMS TO `dbmsManager`")
        ))) {
          resultTable()
        }
      }
    }
  }.build()
}
