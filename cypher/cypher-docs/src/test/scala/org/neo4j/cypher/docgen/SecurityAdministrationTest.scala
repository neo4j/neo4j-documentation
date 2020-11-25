package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling._

class SecurityAdministrationTest extends DocumentingTest with QueryStatisticsTestSupport {
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
      "CREATE ROLE roleDropper",
      "CREATE ROLE roleAssigner",
      "CREATE ROLE roleRemover",
      "CREATE ROLE roleShower",
      "CREATE ROLE roleManager",
      "CREATE ROLE userAdder",
      "CREATE ROLE userDropper",
      "CREATE ROLE userModifier",
      "CREATE ROLE passwordModifier",
      "CREATE ROLE statusModifier",
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
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage Neo4j administrative privileges.")
    p(
      """All of the commands described in the enclosing <<administration, Administration>> section require that the user executing the commands has the rights to do so.
        |These privileges can be conferred either by granting the user the `admin` role, which enables all administrative rights, or by granting specific combinations of privileges.
        |""".stripMargin)
    p(
      """
        |* <<administration-security-administration-introduction, The `admin` role>>
        |* <<administration-security-administration-database-privileges, Database administration>>
        |** <<administration-security-administration-database-access, The database `ACCESS` privilege>>
        |** <<administration-security-administration-database-startstop, The database `START`/`STOP` privileges>>
        |** <<administration-security-administration-database-indexes, The `INDEX MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-constraints, The `CONSTRAINT MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-tokens, The `NAME MANAGEMENT` privileges>>
        |** <<administration-security-administration-database-all, Granting `ALL DATABASE PRIVILEGES`>>
        |** <<administration-security-administration-database-transaction, Granting `TRANSACTION MANAGEMENT` privileges>>
        |* <<administration-security-administration-dbms-privileges, DBMS administration>>
        |** <<administration-security-administration-dbms-custom, Using a custom role to manage DBMS privileges>>
        |** <<administration-security-administration-dbms-privileges-role-management, The dbms `ROLE MANAGEMENT` privileges>>
        |** <<administration-security-administration-dbms-privileges-user-management, The dbms `USER MANAGEMENT` privileges>>
        |** <<administration-security-administration-dbms-privileges-database-management, The dbms `DATABASE MANAGEMENT` privileges>>
        |** <<administration-security-administration-dbms-privileges-privilege-management, The dbms `PRIVILEGE MANAGEMENT` privileges>>
        |** <<administration-security-administration-dbms-privileges-all, Granting `ALL DBMS PRIVILEGES`>>
        |""".stripMargin)
    section("The `admin` role", "administration-security-administration-introduction", "enterprise-edition") {
      p("include::admin-role-introduction.asciidoc[]")
      query("SHOW ROLE admin PRIVILEGES", assertPrivilegeShown(Seq(
        Map("access" -> "GRANTED", "action" -> "access")
      ))) {
        resultTable()
      }
      p("If the built-in admin role has been altered or dropped, and needs to be restored to its original state, see " +
        "<<operations-manual#password-and-user-recovery, Operations Manual -> Password and user recovery>>.")
    }
    section("Database administration", "administration-security-administration-database-privileges", "enterprise-edition") {
      p("include::database/admin-role-database.asciidoc[]")
      p("image::privilege-hierarchy-database.png[title=\"Database privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/1sCTHNdobSVA7REKe4o2m_exohMPGQ3_Z1dhc03mgrBE/edit?usp=sharing
      p("include::database/admin-database-syntax.asciidoc[]")
      p("image::grant-privileges-database.png[title=\"Syntax of GRANT and DENY Database Privileges\"]")
      // image source: https://docs.google.com/drawings/d/1OEo-s6zv0psAR16DOm61Du1iQRhyQsIFL6llDq0jgJk/edit?usp=sharing
      section("The database `ACCESS` privilege", "administration-security-administration-database-access", "enterprise-edition") {
        p(
          """The `ACCESS` privilege enables users to connect to a database.
            |With `ACCESS` you can run calculations, for example, `RETURN 2*5 AS answer` or call functions `RETURN timestamp() AS time`.""".stripMargin)
        p("include::database/grant-database-access-syntax.asciidoc[]")

        p(
          """For example, granting the ability to access the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `ACCESS` privilege can also be denied.")
        p("include::database/deny-database-access-syntax.asciidoc[]")

        p("For example, denying the ability to access to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY ACCESS ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access"),
          Map("access" -> "DENIED", "action" -> "access")
        ))) {
          resultTable()
        }
      }
      section("The database `START`/`STOP` privileges", "administration-security-administration-database-startstop", "enterprise-edition") {
        p(
          """The `START` privilege can be used to enable the ability to start a database.""".stripMargin)
        p("include::database/grant-database-start-syntax.asciidoc[]")

        p(
          """For example, granting the ability to start the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT START ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `START` privilege can also be denied.")
        p("include::database/deny-database-start-syntax.asciidoc[]")

        p("For example, denying the ability to start to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY START ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p(
          """The `STOP` privilege can be used to enable the ability to stop a database.""".stripMargin)
        p("include::database/grant-database-stop-syntax.asciidoc[]")

        p(
          """For example, granting the ability to stop the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT STOP ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The `STOP` privilege can also be denied.")
        p("include::database/deny-database-stop-syntax.asciidoc[]")

        p("For example, denying the ability to stop to the database `neo4j` to the role `regularUsers` is done using the following query.")
        query("DENY STOP ON DATABASE system TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access"),
          Map("access" -> "DENIED", "action" -> "access"),
          Map("access" -> "GRANTED", "action" -> "start_database"),
          Map("access" -> "DENIED", "action" -> "start_database"),
          Map("access" -> "GRANTED", "action" -> "stop_database"),
          Map("access" -> "DENIED", "action" -> "stop_database")
        ))) {
          resultTable()
        }

        note {
          p("Note that `START` and `STOP` privileges are not included in the <<administration-security-administration-database-all, `ALL DATABASE PRIVILEGES`>>.")
        }
      }
      section("The `INDEX MANAGEMENT` privileges", "administration-security-administration-database-indexes", "enterprise-edition") {
        p(
          """Indexes can be created or deleted with the `CREATE INDEX` and `DROP INDEX` commands.
            |The privilege to do this can be granted with `GRANT CREATE INDEX` and `GRANT DROP INDEX` commands.""".stripMargin)
        p("include::database/index-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create indexes on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE INDEX ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("The `CONSTRAINT MANAGEMENT` privileges", "administration-security-administration-database-constraints", "enterprise-edition") {
        p(
          """Constraints can be created or deleted with the `CREATE CONSTRAINT` and `DROP CONSTRAINT` commands.
            |The privilege to do this can be granted with `GRANT CREATE CONSTRAINT` and `GRANT DROP CONSTRAINT` commands.""".stripMargin)
        p("include::database/constraint-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create constraints on the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE CONSTRAINT ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("The `NAME MANAGEMENT` privileges", "administration-security-administration-database-tokens", "enterprise-edition") {
        p(
          """The right to create new labels, relationship types or property names is different from the right to create nodes, relationships or properties.
            |The latter is managed using database `WRITE` privileges, while the former is managed using specific `GRANT/DENY CREATE NEW ...` commands for each type.""".stripMargin)
        p("include::database/name-management-syntax.asciidoc[]")

        p(
          """For example, granting the ability to create new properties on nodes or relationships in the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT CREATE NEW PROPERTY NAME ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
      }
      section("Granting `ALL DATABASE PRIVILEGES`", "administration-security-administration-database-all", "enterprise-edition") {
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
        query("GRANT ALL DATABASE PRIVILEGES ON DATABASE neo4j TO databaseAdminUsers", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE databaseAdminUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "database_actions", "role" -> "databaseAdminUsers")
        ))) {
          resultTable()
        }
      }
      section("Granting `TRANSACTION MANAGEMENT` privileges", "administration-security-administration-database-transaction", "enterprise-edition") {
        p(
          """The right to run the procedures `dbms.listTransactions`, `dbms.listQueries`, `dbms.killQuery`, `dbms.killQueries`,
            |`dbms.killTransaction` and `dbms.killTransactions` are managed through the `SHOW TRANSACTION` and `TERMINATE TRANSACTION` privileges.""".stripMargin)
        p("include::database/transaction-management-syntax.asciidoc[]")

        note {
          p("Note that the `TRANSACTION MANAGEMENT` privileges are not included in the <<administration-security-administration-database-all, `ALL DATABASE PRIVILEGES`>>.")
        }

        p(
          """For example, granting the ability to list transactions for user `jake` in the database `neo4j` to the role `regularUsers` is done using the following query.""".stripMargin)
        query("GRANT SHOW TRANSACTION (jake) ON DATABASE neo4j TO regularUsers", ResultAssertions((r) => {
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
      // image source: https://docs.google.com/drawings/d/1rr_sDbfi29n18pdFE_utoFoIMZTlrhPLju8Wl1TFmtM/edit?usp=sharing
      p("image::privilege-hierarchy-dbms.png[title=\"Dbms privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/1-42KDWaGeOVsBbLUBZ5lHb7n8pn789G85lKivhD0OJE/edit?usp=sharing
      p("include::dbms/admin-role-dbms.asciidoc[]")

      section("Using a custom role to manage DBMS privileges", "administration-security-administration-dbms-custom", "enterprise-edition") {
        p("include::dbms/admin-role-dbms-custom.asciidoc[]")
        p("First we copy the 'admin' role:")
        query("CREATE ROLE usermanager AS COPY OF admin", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 2)
        })) {
          statsOnlyResultTable()
        }
        p("Then we DENY ACCESS to normal databases:")
        query("DENY ACCESS ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And DENY START and STOP for normal databases:")
        query("DENY START ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("DENY STOP ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And DENY index and constraint management:")
        query("DENY INDEX MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        query("DENY CONSTRAINT MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("And finally DENY label, relationship type and property name:")
        query("DENY NAME MANAGEMENT ON DATABASE * TO usermanager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The resulting role should have privileges that only allow the DBMS capabilities, like user and role management:")
        query("SHOW ROLE usermanager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'usermanager'")
          resultTable()
        }
      }

      section("The dbms `ROLE MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-role-management", "enterprise-edition") {
        p("The dbms privileges for role management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/role-management-syntax.asciidoc[]")

        p("The ability to add roles can be granted via the `CREATE ROLE` privilege. The following query shows an example of this:")
        query("GRANT CREATE ROLE ON DBMS TO roleAdder", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow adding roles:")
        query("SHOW ROLE roleAdder PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleAdder'")
          resultTable()
        }

        p("The ability to delete roles can be granted via the `DROP ROLE` privilege. The following query shows an example of this:")
        query("GRANT DROP ROLE ON DBMS TO roleDropper", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow deleting roles:")
        query("SHOW ROLE roleDropper PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleDropper'")
          resultTable()
        }

        p("The ability to assign roles to users can be granted via the `ASSIGN ROLE` privilege. The following query shows an example of this:")
        query("GRANT ASSIGN ROLE ON DBMS TO roleAssigner", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow assigning/granting roles:")
        query("SHOW ROLE roleAssigner PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleAssigner'")
          resultTable()
        }

        p("The ability to remove roles from users can be granted via the `REMOVE ROLE` privilege. The following query shows an example of this:")
        query("GRANT REMOVE ROLE ON DBMS TO roleRemover", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow removing/revoking roles:")
        query("SHOW ROLE roleRemover PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleRemover'")
          resultTable()
        }

        p("The ability to show roles can be granted via the `SHOW ROLE` privilege. A user with this privilege is allowed to execute the `SHOW ROLES` and `SHOW POPULATED ROLES` administration commands. " +
          "For the `SHOW ROLES WITH USERS` and `SHOW POPULATED ROLES WITH USERS` administration commands, both this privilege and the `SHOW USER` privilege are required. The following query shows an example of how to grant the `SHOW ROLE` privilege:")
        query("GRANT SHOW ROLE ON DBMS TO roleShower", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow showing roles:")
        query("SHOW ROLE roleShower PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleShower'")
          resultTable()
        }

        p("All of the above mentioned privileges can be granted via the `ROLE MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT ROLE MANAGEMENT ON DBMS TO roleManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage roles:")
        query("SHOW ROLE roleManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'roleManager'")
          resultTable()
        }
      }

      section("The dbms `USER MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-user-management", "enterprise-edition") {
        p("The dbms privileges for user management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/user-management-syntax.asciidoc[]")

        p("The ability to add users can be granted via the `CREATE USER` privilege. The following query shows an example of this:")
        query("GRANT CREATE USER ON DBMS TO userAdder", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow adding users:")
        query("SHOW ROLE userAdder PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userAdder'")
          resultTable()
        }

        p("The ability to delete users can be granted via the `DROP USER` privilege. The following query shows an example of this:")
        query("GRANT DROP USER ON DBMS TO userDropper", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow deleting users:")
        query("SHOW ROLE userDropper PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userDropper'")
          resultTable()
        }

        p("The ability to modify users can be granted via the `ALTER USER` privilege. The following query shows an example of this:")
        query("GRANT ALTER USER ON DBMS TO userModifier", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow modifying users:")
        query("SHOW ROLE userModifier PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userModifier'")
          resultTable()
        }
        p("A user that is granted `ALTER USER` is allowed to run the `ALTER USER` administration command with one or several of the `SET PASSWORD`, `SET PASSWORD CHANGE [NOT] REQUIRED` and `SET STATUS` parts:")
        query("ALTER USER jake SET PASSWORD 'secret' SET STATUS SUSPENDED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The ability to modify users' passwords and whether those passwords must be changed upon first login can be granted via the `SET PASSWORDS` privilege. The following query shows an example of this:")
        query("GRANT SET PASSWORDS ON DBMS TO passwordModifier", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow modifying users' passwords and whether those passwords must be changed upon first login:")
        query("SHOW ROLE passwordModifier PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'passwordModifier'")
          resultTable()
        }
        p("A user that is granted `SET PASSWORDS` is allowed to run the `ALTER USER` administration command with one or both of the `SET PASSWORD` and `SET PASSWORD CHANGE [NOT] REQUIRED` parts:")
        query("ALTER USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The ability to modify the account status of users can be granted via the `SET USER STATUS` privilege. The following query shows an example of this:")
        query("GRANT SET USER STATUS ON DBMS TO statusModifier", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow modifying the account status of users:")
        query("SHOW ROLE statusModifier PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'statusModifier'")
          resultTable()
        }
        p("A user that is granted `SET USER STATUS` is allowed to run the `ALTER USER` administration command with only the `SET STATUS` part:")
        query("ALTER USER jake SET STATUS ACTIVE", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        note {
          p("Note that the combination of the `SET PASSWORDS` and the `SET USER STATUS` privilege actions is equivalent to the `ALTER USER` privilege action.")
        }

        p("The ability to show users can be granted via the `SHOW USER` privilege. The following query shows an example of this:")
        query("GRANT SHOW USER ON DBMS TO userShower", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow showing users:")
        query("SHOW ROLE userShower PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userShower'")
          resultTable()
        }

        p("All of the above mentioned privileges can be granted via the `USER MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT USER MANAGEMENT ON DBMS TO userManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage users:")
        query("SHOW ROLE userManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'userManager'")
          resultTable()
        }
      }

      section("The dbms `DATABASE MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-database-management", "enterprise-edition") {
        p("The dbms privileges for database management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/database-management-syntax.asciidoc[]")

        p("The ability to create databases can be granted via the `CREATE DATABASE` privilege. The following query shows an example of this:")
        query("GRANT CREATE DATABASE ON DBMS TO databaseAdder", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow creating databases:")
        query("SHOW ROLE databaseAdder PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'databaseAdder'")
          resultTable()
        }

        p("The ability to delete databases can be granted via the `DROP DATABASE` privilege. The following query shows an example of this:")
        query("GRANT DROP DATABASE ON DBMS TO databaseDropper", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow deleting databases:")
        query("SHOW ROLE databaseDropper PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'databaseDropper'")
          resultTable()
        }

        p("Both of the above mentioned privileges can be granted via the `DATABASE MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT DATABASE MANAGEMENT ON DBMS TO databaseManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage databases:")
        query("SHOW ROLE databaseManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'databaseManager'")
          resultTable()
        }
      }

      section("The dbms `PRIVILEGE MANAGEMENT` privileges", "administration-security-administration-dbms-privileges-privilege-management", "enterprise-edition") {
        p("The dbms privileges for privilege management are assignable using Cypher administrative commands. They can be granted, denied and revoked like other privileges.")
        p("include::dbms/privilege-management-syntax.asciidoc[]")

        p("The ability to list privileges can be granted via the `SHOW PRIVILEGE` privilege. A user with this privilege is allowed to execute the `SHOW PRIVILEGES` and `SHOW ROLE roleName PRIVILEGES` administration commands. " +
          "For the `SHOW USER username PRIVILEGES` administration command, both this privilege and the `SHOW USER` privilege are required. The following query shows an example of how to grant the `SHOW PRIVILEGE` privilege:")
        query("GRANT SHOW PRIVILEGE ON DBMS TO privilegeShower", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow showing privileges:")
        query("SHOW ROLE privilegeShower PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'privilegeShower'")
          resultTable()
        }

        note {
          p("Note that no specific privileges are required for showing the current user's privileges using either `SHOW USER _username_ PRIVILEGES`, or `SHOW USER PRIVILEGES`.")
          p("Please note that if a non-native auth provider like LDAP is in use, `SHOW USER PRIVILEGES` will only work in a limited capacity; " +
            "It is only possible for a user to show their own privileges. Other users' privileges cannot be listed when using a non-native auth provider.")
        }
        p("The ability to assign privileges to roles can be granted via the `ASSIGN PRIVILEGE` privilege. A user with this privilege is allowed to execute GRANT and DENY administration commands. The following query shows an example of how to grant this privilege:")
        query("GRANT ASSIGN PRIVILEGE ON DBMS TO privilegeAssigner", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow assigning privileges:")
        query("SHOW ROLE privilegeAssigner PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'privilegeAssigner'")
          resultTable()
        }

        p("The ability to remove privileges from roles can be granted via the `REMOVE PRIVILEGE` privilege. A user with this privilege is allowed to execute REVOKE administration commands. The following query shows an example of how to grant this privilege:")
        query("GRANT REMOVE PRIVILEGE ON DBMS TO privilegeRemover", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have privileges that only allow removing privileges:")
        query("SHOW ROLE privilegeRemover PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'privilegeRemover'")
          resultTable()
        }

        p("All of the above mentioned privileges can be granted via the `PRIVILEGE MANAGEMENT` privilege. The following query shows an example of this:")
        query("GRANT PRIVILEGE MANAGEMENT ON DBMS TO privilegeManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }
        p("The resulting role should have all privileges to manage privileges:")
        query("SHOW ROLE privilegeManager PRIVILEGES", assertPrivilegeShown(Seq(Map()))) {
          p("Lists all privileges for role 'privilegeManager'")
          resultTable()
        }
      }

      section("Granting `ALL DBMS PRIVILEGES`", "administration-security-administration-dbms-privileges-all", "enterprise-edition") {
        p(
          """The right to create, drop, assign, remove and show roles, create, alter, drop and show users, create and drop databases and show, assign and remove privileges can be achieved with a single command:""".stripMargin)
        p("include::dbms/all-management-syntax.asciidoc[]")

        p(
          """For example, granting the abilities above to the role `dbmsManager` is done using the following query.""".stripMargin)
        query("GRANT ALL DBMS PRIVILEGES ON DBMS TO dbmsManager", ResultAssertions((r) => {
          assertStats(r, systemUpdates = 1)
        })) {
          statsOnlyResultTable()
        }

        p("The privileges granted can be seen using the `SHOW PRIVILEGES` command:")
        query("SHOW ROLE dbmsManager PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "dbms_actions", "role" -> "dbmsManager")
        ))) {
          resultTable()
        }
      }
    }
  }.build()

  private def assertPrivilegeShown(expected: Seq[Map[String, AnyRef]]) = ResultAssertions(p => {
    val found = p.toList.filter { row =>
      val m = expected.filter { expectedRow =>
        expectedRow.forall {
          case (k, v) => row.contains(k) && row(k) == v
        }
      }
      m.nonEmpty
    }
    found.nonEmpty should be(true)
  })
}
