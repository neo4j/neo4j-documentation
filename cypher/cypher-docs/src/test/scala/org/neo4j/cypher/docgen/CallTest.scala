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

import org.neo4j.cypher.docgen.tooling.{DocBuilder, DocumentingTest, ResultAssertions}

class CallTest extends DocumentingTest {

  override def outputPath = "target/docs/dev/ql"

  var nodeId:Long = 1

  override def doc = new DocBuilder {
    doc("CALL procedure", "query-call")
    initQueries("CREATE (a:User:Administrator {name: 'Adrian'})")
    synopsis("The `CALL` clause is used to call a procedure deployed in the database.")
    section("Introduction", "query-call-introduction") {
      p("Procedures are called using the `CALL` clause.")
      tip {
        p("""The `CALL` clause is also used to evaluate a subquery.
            #For descriptions of the `CALL` clause in this context, refer to <<query-call-subquery>>.""".stripMargin('#'))
      }
      p("""Each procedure call needs to specify all required procedure arguments.
          #This may be done either explicitly, by using a comma-separated list wrapped in parentheses after the procedure name, or implicitly by using available query parameters as procedure call arguments.
          #The latter form is available only in a so-called standalone procedure call, when the whole query consists of a single `CALL` clause.""".stripMargin('#'))
      p("""Most procedures return a stream of records with a fixed set of result fields, similar to how running a Cypher query returns a stream of records.
          #The `YIELD` sub-clause is used to explicitly select which of the available result fields are returned as newly-bound variables from the procedure call to the user or for further processing by the remaining query.
          #Thus, in order to be able to use `YIELD` for explicit columns, the names (and types) of the output parameters need be known in advance.
          #Each yielded result field may optionally be renamed using aliasing (i.e., `resultFieldName AS newName`).
          #All new variables bound by a procedure call are added to the set of variables already bound in the current scope.
          #It is an error if a procedure call tries to rebind a previously bound variable (i.e., a procedure call cannot shadow a variable that was previously bound in the current scope).
          #In a standalone procedure call, `+YIELD *+` can be used to select all columns. In this case, the name of the output parameters does not need to be known in advance.""".stripMargin('#'))
      p("For more information on how to determine the input parameters for the `CALL` procedure and the output parameters for the `YIELD` procedure, see <<call-view-the-signature-for-a-procedure>>.")
      p("Inside a larger query, the records returned from a procedure call with an explicit `YIELD` may be further filtered using a `WHERE` sub-clause followed by a predicate (similar to `+WITH ... WHERE ...+`).")
      p("""If the called procedure declares at least one result field, `YIELD` may generally not be omitted.
          #However `YIELD` may always be omitted in a standalone procedure call.
          #In this case, all result fields are yielded as newly-bound variables from the procedure call to the user.""".stripMargin('#'))
      p("""Neo4j supports the notion of `VOID` procedures.
          #A `VOID` procedure is a procedure that does not declare any result fields and returns no result records and that has explicitly been declared as `VOID`.
          #Calling a `VOID` procedure may only have a side effect and thus does neither allow nor require the use of `YIELD`.
          #Calling a `VOID` procedure in the middle of a larger query will simply pass on each input record (i.e., it acts like `WITH *` in terms of the record stream).""".stripMargin('#'))
      note {
        p("""Neo4j comes with a number of built-in procedures.
            #For a list of these, see <<operations-manual#neo4j-procedures, Operations Manual -> Procedures>>.""".stripMargin('#'))
        p("""Users can also develop custom procedures and deploy to the database.
            #See <<java-reference#extending-neo4j-procedures, Java Reference -> User-defined procedures>> for details.""".stripMargin('#'))
      }
    }

    section("Call a procedure using `CALL`", "call-call-a-procedure-using-call") {
      p("This calls the built-in procedure `db.labels`, which lists all labels used in the database.")
      query("CALL db.labels()", assertNotEmpty) {
        resultTable()
      }
      p("Cypher allows the omission of parentheses on procedures of arity-0 (no arguments).")
      note{
        p("Best practice is to use parentheses for procedures.")
      }
      query("CALL db.labels", assertNotEmpty) {
        resultTable()
      }
    }

    section("View the signature for a procedure", "call-view-the-signature-for-a-procedure") {
      p("""To `CALL` a procedure, its input parameters need to be known, and to use `YIELD`, its output parameters need to be known.
          #The `SHOW PROCEDURES` command returns the name, signature and description for all procedures.
          #The following query can be used to return the signature for a particular procedure:""".stripMargin('#'))
      query("""SHOW PROCEDURES YIELD name, signature
              #WHERE name='dbms.listConfig'
              #RETURN signature""".stripMargin('#'), assertNotEmpty) {
        p("We can see that the `dbms.listConfig` has one input parameter, `searchString`, and three output parameters, `name`, `description` and `value`.")
        resultTable()
      }
    }

    section("Call a procedure using a quoted namespace and name", "call-call-a-procedure-using-a-quoted-namespace-and-name") {
      p("This calls the built-in procedure `db.labels`, which lists all labels used in the database.")
      query("CALL `db`.`labels()`", assertNotEmpty){}
      query("CALL `db`.`labels`", assertNotEmpty){}
    }

    section("Call a procedure with literal arguments", "call-call-a-procedure-with-literal-arguments") {
      p("""This calls the example procedure `dbms.security.createUser` using literal arguments.
          #The arguments are written out directly in the statement text.""".stripMargin('#'))
      query("CALL dbms.security.createUser('example_username', 'example_password', false)", assertEmpty) {
        p("Since our example procedure does not return any result, the result is empty.")
      }
    }

    section("Call a procedure with parameter arguments", "call-call-a-procedure-with-parameter-arguments") {
      p("""This calls the example procedure `dbms.security.createUser` using parameters as arguments.
          #Each procedure argument is taken to be the value of a corresponding statement parameter with the same name (or null if no such parameter has been given).""".stripMargin('#'))
      note{
        p("""Examples that use parameter arguments shows the given parameters in JSON format; the exact manner in which they are to be submitted depends upon the driver being used.
          #See <<cypher-parameters>>, for more about querying with parameters""".stripMargin('#'))
      }
      query("CALL dbms.security.createUser($username, $password, $requirePasswordChange)", assertEmpty, ("username", "example_username"), ("password", "example_password"), ("requirePasswordChange", false)) {
        p("Since our example procedure does not return any result, the result is empty.")
      }
      p("Cypher allows the omission of parentheses for procedures with arity-n (n arguments), Cypher implicitly passes the parameter arguments.")
      note{
        p("""Best practice is to use parentheses for procedures.
            #Omission of parantheses is available only in a so-called standalone procedure call, when the whole query consists of a single `CALL` clause.""".stripMargin('#'))
      }
      query("CALL dbms.security.createUser", assertEmpty, ("username", "example_username"), ("password", "example_password"), ("requirePasswordChange", false)) {
        p("Since our example procedure does not return any result, the result is empty.")
      }
    }

    section("Call a procedure with mixed literal and parameter arguments", "call-call-a-procedure-with-mixed-literal-and-parameter-arguments") {
      p("This calls the example procedure `dbms.security.createUser` using both literal and parameter arguments.")
      query("CALL dbms.security.createUser('example_username', $password, false)", assertEmpty, ("password", "example_password")) {
        p("Since our example procedure does not return any result, the result is empty.")
      }
    }

    section("Call a procedure with literal and default arguments", "call-call-a-procedure-with-literal-and-default-arguments") {
      p("""This calls the example procedure `dbms.security.createUser` using literal arguments.
          #That is, arguments that are written out directly in the statement text, and a trailing default argument that is provided by the procedure itself.""".stripMargin('#'))
      query("CALL dbms.security.createUser('example_username', 'example_password')", assertEmpty) {
        p("Since our example procedure does not return any result, the result is empty.")
      }
    }

    section("Call a procedure using `+CALL YIELD *+`", "call-call-a-procedure-call-yield-star") {
      p("This calls the built-in procedure `db.labels` to count all labels used in the database.")
      query("CALL db.labels() YIELD *", assertNotEmpty) {
        p("If the procedure has deprecated return columns, those columns are also returned.")
      }
    }

    section("Call a procedure within a complex query using `CALL YIELD`", "call-call-a-procedure-within-a-complex-query-using-call-yield") {
      p("This calls the built-in procedure `db.labels` to count all labels used in the database.")
      query("""CALL db.labels() YIELD label
              #RETURN count(label) AS numLabels""".stripMargin('#'), assertNotEmpty) {
        p("Since the procedure call is part of a larger query, all outputs must be named explicitly.")
      }
    }

    section("Call a procedure and filter its results", "call-call-a-procedure-and-filter-its-results") {
      p("This calls the built-in procedure `db.labels` to count all in-use labels in the database that contain the word 'User'.")
      query("""CALL db.labels() YIELD label
              #WHERE label CONTAINS 'User'
              #RETURN count(label) AS numLabels""".stripMargin('#'), assertNotEmpty) {
        p("Since the procedure call is part of a larger query, all outputs must be named explicitly.")
      }
    }

    section("Call a procedure within a complex query and rename its outputs", "call-call-a-procedure-within-a-complex-query-and-rename-its-outputs") {
      p("This calls the built-in procedure `db.propertyKeys` as part of counting the number of nodes per property key that is currently used in the database.")
      query("""CALL db.propertyKeys() YIELD propertyKey AS prop
              #MATCH (n)
              #WHERE n[prop] IS NOT NULL
              #RETURN prop, count(n) AS numNodes""".stripMargin('#'), assertNotEmpty) {
        p("Since the procedure call is part of a larger query, all outputs must be named explicitly.")
      }
    }
  }.build()

  private def assertNotEmpty = ResultAssertions(result => result.toList.size should be > 0)
  private def assertEmpty = ResultAssertions(result => result.toList.size shouldBe 0)

}
