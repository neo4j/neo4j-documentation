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

import com.neo4j.configuration.OnlineBackupSettings
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.cypher.docgen.RefcardTest
import org.neo4j.cypher.docgen.tooling.DocsExecutionResult
import org.neo4j.cypher.docgen.tooling.QueryStatisticsTestSupport
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.config.Setting
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_CARTESIAN_MAX
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_CARTESIAN_MIN
import org.neo4j.kernel.impl.index.schema.GenericNativeIndexProvider
import org.neo4j.kernel.impl.index.schema.RelationshipTypeScanStoreSettings

import java.util
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._

class IndexTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A:Person KNOWS B:Person")
  val title = "INDEX"
  override val linkId = "administration/indexes-for-search-performance"
  private val nativeProvider = GenericNativeIndexProvider.DESCRIPTOR.name()

  override protected def databaseConfig(): util.Map[Setting[_], Object] = {
    Map[Setting[_], Object](
      OnlineBackupSettings.online_backup_listen_address -> new SocketAddress("127.0.0.1", 0),
      OnlineBackupSettings.online_backup_enabled -> java.lang.Boolean.FALSE,
      RelationshipTypeScanStoreSettings.enable_relationship_property_indexes -> java.lang.Boolean.TRUE,
      RelationshipTypeScanStoreSettings.enable_scan_stores_as_token_indexes -> java.lang.Boolean.TRUE
    ).asJava
  }

  //noinspection RedundantDefaultArgument
  // Disable warnings for redundant default argument since its used for clarification of the `assertStats` when nothing should have happened
  override def assert(tx:Transaction, name: String, result: DocsExecutionResult): Unit = {
    name match {
      case "create-index" =>
        assertStats(result, indexesAdded = 1)
        assert(result.toList.size === 0)
        tx.schema().awaitIndexesOnline(10, TimeUnit.SECONDS)
      case "create-existing-index" =>
        assertStats(result, indexesAdded = 0)
        assert(result.toList.size === 0)
      case "drop-named-index" =>
        assertStats(result, indexesRemoved = 1)
        assert(result.toList.size === 0)
      case "drop-non-existing-index" =>
        assertStats(result, indexesRemoved = 0)
        assert(result.toList.size === 0)
      case "match" =>
        assertStats(result, nodesCreated = 0)
        assert(result.toList.size === 1)
      case "show" =>
        assertStats(result)
        assert(result.toList.size === 6)
    }
  }

  override val properties: Map[String, Map[String, Any]] = Map(
    "A" -> Map("name" -> "Alice", "age" -> 18 ),
    "B" -> Map("name" -> "Timothy", "age" -> 22 ))

  override def parameters(name: String): Map[String, Any] =
    name match {
      case "parameters=aname" =>
        Map("value" -> "Alice")
      case "parameters=nameandage" =>
        Map("value" -> "Alice" , "value2" -> 18 )
      case _ =>
        Map()
    }

  def text = s"""
###assertion=create-index
//

CREATE INDEX FOR (p:Person) ON (p.name)
###

Create an index on nodes with label `Person` and property `name`.

###assertion=create-index
//

CREATE INDEX index_name FOR ()-[k:KNOWS]-() ON (k.since)
###

Create an index on relationships with type `KNOWS` and property `since` with the name `index_name`.

###assertion=create-index
//

CREATE INDEX FOR (p:Person) ON (p.surname)
OPTIONS {indexProvider: '$nativeProvider', indexConfig: {`${SPATIAL_CARTESIAN_MIN.getSettingName}`: [-100.0, -100.0], `${SPATIAL_CARTESIAN_MAX.getSettingName}`: [100.0, 100.0]}}
###

Create an index on nodes with label `Person` and property `surname` with the index provider `$nativeProvider` and given `spatial.cartesian` settings.
The other index settings will have their default values.

###assertion=create-index
//

CREATE INDEX FOR (p:Person) ON (p.name, p.age)
###

Create a composite index on nodes with label `Person` and the properties `name` and `age`, throws an error if the index already exist.

###assertion=create-existing-index
//

CREATE INDEX IF NOT EXISTS FOR (p:Person) ON (p.name, p.age)
###

Create a composite index on nodes with label `Person` and the properties `name` and `age` if it does not already exist, does nothing if it did exist.

###assertion=create-index
//

CREATE LOOKUP INDEX lookup_index_name FOR (n) ON EACH labels(n)
###

Create a lookup index on nodes with any label with the name `lookup_index_name`.

###assertion=create-index
//

CREATE LOOKUP INDEX FOR ()-[r]-() ON EACH type(r)
###

Create a lookup index on relationships with any type.

###assertion=show
//

SHOW INDEXES
###

List all indexes.

###assertion=match parameters=aname
//

MATCH (n:Person) WHERE n.name = $$value

RETURN n
###

An index can be automatically used for the equality comparison.
Note that for example `toLower(n.name) = $$value` will not use an index.

###assertion=match parameters=aname
//

MATCH (n:Person)
WHERE n.name IN [$$value]

RETURN n
###

An index can automatically be used for the `IN` list checks.

###assertion=match parameters=nameandage
//

MATCH (n:Person)
WHERE n.name = $$value and n.age = $$value2

RETURN n
###

A composite index can be automatically used for equality comparison of both properties.
Note that there needs to be predicates on all properties of the composite index for it to be used.

###assertion=match parameters=aname
//

MATCH (n:Person)
USING INDEX n:Person(name)
WHERE n.name = $$value

RETURN n
###

Index usage can be enforced when Cypher uses a suboptimal index, or
more than one index should be used.

###assertion=drop-named-index
//

DROP INDEX index_name
###

Drop the index named `index_name`, throws an error if the index does not exist.

###assertion=drop-non-existing-index
//

DROP INDEX index_name IF EXISTS
###

Drop the index named `index_name` if it exists, does nothing if it does not exist.
"""
}
