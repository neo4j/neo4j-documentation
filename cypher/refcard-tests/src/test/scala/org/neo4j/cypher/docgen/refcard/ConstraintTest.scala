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
package org.neo4j.cypher.docgen.refcard

import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_WGS84_MAX
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_WGS84_MIN
import org.neo4j.kernel.impl.index.schema.GenericNativeIndexProvider

class ConstraintTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A:Person KNOWS B:Person")
  val title = "CONSTRAINT"
  override val linkId = "administration/constraints"
  private val nativeProvider = GenericNativeIndexProvider.DESCRIPTOR.name()

  //noinspection RedundantDefaultArgument
  // Disable warnings for redundant default argument since its used for clarification of the `assertStats` when nothing should have happened
  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "create-unique-property-constraint" =>
        assertStats(result, uniqueConstraintsAdded = 1)
        assert(result.toList.size === 0)
      case "create-property-existence-constraint" =>
        assertStats(result, existenceConstraintsAdded = 1)
        assert(result.toList.size === 0)
      case "create-existing-property-existence-constraint" =>
        assertStats(result, existenceConstraintsAdded = 0)
        assert(result.toList.size === 0)
      case "create-node-key-constraint" =>
        assertStats(result, nodekeyConstraintsAdded = 1)
        assert(result.toList.size === 0)
      case "drop-named-constraint" =>
        assertStats(result, namedConstraintsRemoved = 1)
        assert(result.toList.size === 0)
      case "drop-non-existing-constraint" =>
        assertStats(result, namedConstraintsRemoved = 0)
        assert(result.toList.size === 0)
      case "match" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "show" =>
        assertStats(result)
        assert(result.toList.size === 3)
    }
  }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice", "firstname" -> "Alice", "surname" -> "Johnson", "age" -> 15),
    "B" -> Map("name" -> "Bobo", "firstname" -> "Bobo", "surname" -> "Baumann", "age" -> 11))

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Alice")
      case _ =>
        Map()
    }

  def text: String = s"""
###assertion=create-unique-property-constraint
//

CREATE CONSTRAINT ON (p:Person)
       ASSERT p.name IS UNIQUE
###

Create a unique property constraint on the label `Person` and property `name`.
If any other node with that label is updated or created with a `name` that
already exists, the write operation will fail.
This constraint will create an accompanying index.

###assertion=create-unique-property-constraint
//

CREATE CONSTRAINT uniqueness ON (p:Person)
       ASSERT p.age IS UNIQUE
###

Create a unique property constraint on the label `Person` and property `age` with the name `uniqueness`.
If any other node with that label is updated or created with a `age` that
already exists, the write operation will fail.
This constraint will create an accompanying index.

###assertion=create-unique-property-constraint
//

CREATE CONSTRAINT ON (p:Person)
       ASSERT p.surname IS UNIQUE
       OPTIONS {indexProvider: '$nativeProvider'}
###

Create a unique property constraint on the label `Person` and property `surname` with the index provider `$nativeProvider` for the accompanying index.

###assertion=create-property-existence-constraint
//

CREATE CONSTRAINT ON (p:Person)
       ASSERT p.name IS NOT NULL
###

(★) Create a node property existence constraint on the label `Person` and property `name`, throws an error if the constraint already exists.
If a node with that label is created without a `name`, or if the `name` property is
removed from an existing node with the `Person` label, the write operation will fail.

###assertion=create-existing-property-existence-constraint
//

CREATE CONSTRAINT node_exists IF NOT EXISTS ON (p:Person)
       ASSERT p.name IS NOT NULL
###

(★) If a node property existence constraint on the label `Person` and property `name` or any constraint with the name `node_exists` already exist then nothing happens.
If no such constraint exists, then it will be created.

###assertion=create-property-existence-constraint
//

CREATE CONSTRAINT ON ()-[l:LIKED]-()
       ASSERT l.when IS NOT NULL
###

(★) Create a relationship property existence constraint on the type `LIKED` and property `when`.
If a relationship with that type is created without a `when`, or if the `when` property is
removed from an existing relationship with the `LIKED` type, the write operation will fail.

###assertion=create-property-existence-constraint
//

CREATE CONSTRAINT relationship_exists ON ()-[l:LIKED]-()
       ASSERT l.since IS NOT NULL
###

(★) Create a relationship property existence constraint on the type `LIKED` and property `since` with the name `relationship_exists`.
If a relationship with that type is created without a `since`, or if the `since` property is
removed from an existing relationship with the `LIKED` type, the write operation will fail.

###assertion=show
//

SHOW UNIQUE CONSTRAINTS YIELD *
###

List all unique constraints.

""".concat(if (!versionFenceAllowsThisTest("3.2.9")) "" else s"""
###assertion=create-node-key-constraint
//

CREATE CONSTRAINT ON (p:Person)
      ASSERT (p.firstname, p.surname) IS NODE KEY
###

(★) Create a node key constraint on the label `Person` and properties `firstname` and `surname`.
If a node with that label is created without both `firstname` and `surname`
or if the combination of the two is not unique,
or if the `firstname` and/or `surname` properties on an existing node with the `Person` label
is modified to violate these constraints, the write operation will fail.
This constraint will create an accompanying index.

###assertion=create-node-key-constraint
//

CREATE CONSTRAINT node_key ON (p:Person)
      ASSERT p.firstname IS NODE KEY
###

(★) Create a node key constraint on the label `Person` and property `firstname` with the name `node_key`.
If a node with that label is created without the `firstname` property
or if the value is not unique,
or if the `firstname` property on an existing node with the `Person` label
is modified to violate these constraints, the write operation will fail.
This constraint will create an accompanying index.

###assertion=create-node-key-constraint
//

CREATE CONSTRAINT node_key_with_config ON (p:Person)
      ASSERT (p.name, p.age) IS NODE KEY
      OPTIONS {indexConfig: {`${SPATIAL_WGS84_MIN.getSettingName}`: [-100.0, -100.0], `${SPATIAL_WGS84_MAX.getSettingName}`: [100.0, 100.0]}}
###

(★) Create a node key constraint on the label `Person` and properties `name` and `age` with the name `node_key_with_config` and given `spatial.wgs-84` settings for the accompanying index.
The other index settings will have their default values.

""").concat("""
###assertion=drop-named-constraint
//

DROP CONSTRAINT uniqueness
###

Drop the constraint with the name `uniqueness`, throws an error if the constraint does not exist.
If the constraint has an accompanying index, that will also be dropped.

###assertion=drop-non-existing-constraint
//

DROP CONSTRAINT uniqueness IF EXISTS
###

Drop the constraint with the name `uniqueness` if it exists, does nothing if it does not exist.
If the constraint has an accompanying index, that will also be dropped.
""")
}
