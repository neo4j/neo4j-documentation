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

class SecurityPrivilegesTest extends DocumentingTest with QueryStatisticsTestSupport {
  override def outputPath = "target/docs/dev/ql/administration/security/"

  override def doc: Document = new DocBuilder {
    doc("Graph and sub-graph access control", "administration-security-subgraph")
    database("system")
    initQueries(
      "CREATE USER jake SET PASSWORD 'abc123' CHANGE NOT REQUIRED SET STATUS ACTIVE",
      "CREATE ROLE regularUsers",
      "CREATE ROLE noAccessUsers",
      "GRANT ROLE regularUsers TO jake",
      "GRANT ACCESS ON DATABASE neo4j TO regularUsers",
      "DENY ACCESS ON DATABASE neo4j TO noAccessUsers"
    )
    synopsis("This section explains how to use Cypher to manage privileges for Neo4j role-based access control and fine-grained security.")

    p(
      """
        |* <<administration-security-subgraph-introduction, The `GRANT`, `DENY` and `REVOKE` commands>>
        |* <<administration-security-subgraph-show, Listing privileges>>
        |** <<administration-security-subgraph-show-all, Examples for listing all privileges>>
        |** <<administration-security-subgraph-show-role, Examples for listing privileges for specific role>>
        |** <<administration-security-subgraph-show-user, Examples for listing privileges for specific user>>
        |* <<administration-security-subgraph-revoke, The `REVOKE` command>>
        |""".stripMargin)

    p(
      """
        |Privileges control the access rights to graph elements using a combined whitelist/blacklist mechanism.
        |It is possible to grant access, or deny access, or a combination of the two.
        |The user will be able to access the resource if they have a grant (whitelist) and do not have a deny (blacklist) relevant to that resource.
        |All other combinations of `GRANT` and `DENY` will result in the matching path being inaccessible.
        |What this means in practice depends on whether we are talking about a <<administration-security-reads, read privilege>> or a <<administration-security-writes, write privilege>>.
        |
        |* If a entity is not accessible due to <<administration-security-reads, read privileges>>, the data will become invisible to attempts to read it.
        |  It will appear to the user as if they have a smaller database (smaller graph).
        |* If an entity is not accessible due to <<administration-security-writes, write privileges>>, an error will occur on any attempt to write that data.
        |""".stripMargin)
    note {
      p(
        """In this document we will often use the terms _'allows'_ and _'enables'_ in seemingly identical ways. However, there is a subtle difference.
          |We will use _'enables'_ to refer to the consequences of <<administration-security-reads, read privileges>> where a restriction will not cause an error,
          |only a reduction in the apparent graph size. We will use _'allows'_ to refer to the consequence of <<administration-security-writes, write privileges>>
          |where a restriction can result in an error.
          |""".stripMargin
      )
    }
    note {
      p(
        """If a user was not also provided with the database `ACCESS` privilege then access to the entire database will be denied.
          |Information about the database access privilege can be found in <<administration-security-administration-database-access, The ACCESS privilege>>.
          |""".stripMargin)
    }
    section("The `GRANT`, `DENY` and `REVOKE` commands", "administration-security-subgraph-introduction", "enterprise-edition") {
      p("include::grant-deny-syntax.asciidoc[]")
      p("The general grant and deny syntax is illustrated in the image below.")
      p("image::grant-privileges-overview.png[title=\"GRANT and DENY Syntax\"]")
      // image source: https://docs.google.com/drawings/d/1EK1i4cKLoXrMJmNauKMu3yIO1OULcW0uUXLbo33qoU0/edit?usp=sharing
      p("A more detailed syntax illustration would be the image below for graph privileges.")
      p("image::grant-privileges-graph.png[title=\"Syntax of GRANT and DENY Graph Privileges. The `{` and `}` are part of the syntax and not used for grouping.\"]")
      // image source: https://docs.google.com/drawings/d/1dueKAcaQORul-_Ocb5jK9bUkWgtQfdLdFw4uo7PFjTs/edit
      p("The below image shows the hierarchy between the different graph privileges.")
      p("image::privilege-hierarchy-graph.png[title=\"Graph privileges hierarchy\"]")
      // image source: https://docs.google.com/drawings/d/1bhCbhhlaJfp9wKwSVut-YJsaJ9hQBxFVBpCWkZt8KUM/edit?usp=sharing
    }
    section("Listing privileges", "administration-security-subgraph-show", "enterprise-edition") {
      p("Available privileges can be displayed using the different `SHOW PRIVILEGES` commands.")
      p("include::show-privileges-syntax.asciidoc[]")
      section("Examples for listing all privileges", "administration-security-subgraph-show-all") {
        p("Available privileges for all roles can be displayed using `SHOW PRIVILEGES`.")
        p("include::show-all-privileges-syntax.asciidoc[]")
        query("SHOW PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers"),
          Map("access" -> "DENIED", "action" -> "access", "role" -> "noAccessUsers")
        ))) {
          p(
            """Lists all privileges for all roles.
              |The table contains columns describing the privilege:
              |
              |* `access`: whether the privilege is granted or denied
              |* `action`: which type of privilege this is: traverse, read, match, write, a database privilege, a dbms privilege or admin
              |* `resource`: what type of scope this privilege applies to: the entire dbms, a database, a graph or sub-graph access
              |* `graph`: the specific database or graph this privilege applies to
              |* `segment`: for sub-graph access control, this describes the scope in terms of labels or relationship types
              |* `role`: the role the privilege is granted to
              |""".stripMargin)
          resultTable()
        }

        p("It is also possible to filter and sort the results by using `YIELD`, `ORDER BY` and `WHERE`.")
        query("SHOW PRIVILEGES YIELD role, access, action ORDER BY action WHERE role = 'admin' ", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "admin"),
          Map("access" -> "GRANTED", "action" -> "write", "role" -> "admin")
        ))) {
          p(
            """In this example:
              |
              |* The number of columns returned has been reduced with the `YIELD` clause.
              |* The order of the returned columns has been changed.
              |* The results have been filtered to only return the `admin` role using a `WHERE` clause.
              |* The results are ordered by the `action` column using `ORDER BY`.
              |
              |`SKIP` and `LIMIT` can also be used to paginate the results.
              |""".stripMargin)
          resultTable()
        }

        p("`WHERE` can be used without `YIELD`")
        query("SHOW PRIVILEGES WHERE graph <> '*' ", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers", "graph" -> "neo4j"),
          Map("access" -> "DENIED", "action" -> "access", "role" -> "noAccessUsers", "graph" -> "neo4j")
        ))) {
          p(
            """In this example, the `WHERE` clause is used to filter privileges down to those that target specific graphs only.""".stripMargin)
          resultTable()
        }
      }
      section("Examples for listing privileges for specific role", "administration-security-subgraph-show-role") {
        p("Available privileges for a particular role can be displayed using `SHOW ROLE name PRIVILEGES`.")
        p("include::show-role-privileges-syntax.asciidoc[]")
        query("SHOW ROLE regularUsers PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers")
        ))) {
          p("Lists all privileges for role `regularUsers`.")
          resultTable()
        }
      }
      section("Examples for listing privileges for specific user", "administration-security-subgraph-show-user") {
        p("Available privileges for a particular user can be displayed using `SHOW USER name PRIVILEGES`.")
        note {
          p("Please note that if a non-native auth provider like LDAP is in use, `SHOW USER PRIVILEGES` will only work in a limited capacity; " +
            "It is only possible for a user to show their own privileges. Other users' privileges cannot be listed when using a non-native auth provider.")
        }

        p("include::show-user-privileges-syntax.asciidoc[]")
        query("SHOW USER jake PRIVILEGES", assertPrivilegeShown(Seq(
          Map("access" -> "GRANTED", "action" -> "access", "role" -> "regularUsers", "user" -> "jake")
        ))) {
          p("Lists all privileges for user `jake`.")
          resultTable()
        }

        p("The same command can be used at all times to review available privileges for the current user. " +
          "For this purpose, a shorter form of the the command also exists: SHOW USER PRIVILEGES.")
        query("SHOW USER PRIVILEGES", ResultAssertions(r => {
          assertStats(r)
        })){}
      }
    }

    section("The `REVOKE` command", "administration-security-subgraph-revoke", "enterprise-edition") {
      initQueries(
        "GRANT TRAVERSE ON GRAPH neo4j NODES Post TO regularUsers",
        "GRANT TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers",
        "DENY TRAVERSE ON GRAPH neo4j NODES Payments TO regularUsers"
      )
      p("Privileges that were granted or denied earlier can be revoked using the `REVOKE` command. ")
      p("include::revoke-syntax.asciidoc[]")

      p("An example usage of the `REVOKE` command is given here:")
      query("REVOKE GRANT TRAVERSE ON GRAPH neo4j NODES Post FROM regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 1)
      })) {
        statsOnlyResultTable()
      }
      p(
        """While it can be explicitly specified that revoke should remove a `GRANT` or `DENY`, it is also possible to revoke either one by not specifying at all as the next example demonstrates.
          |Because of this, if there happen to be a `GRANT` and a `DENY` on the same privilege, it would remove both.""".stripMargin)
      query("REVOKE TRAVERSE ON GRAPH neo4j NODES Payments FROM regularUsers", ResultAssertions(r => {
        assertStats(r, systemUpdates = 2)
      })) {
        statsOnlyResultTable()
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
