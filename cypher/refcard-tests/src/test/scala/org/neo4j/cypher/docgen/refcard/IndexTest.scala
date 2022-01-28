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
import org.neo4j.graphdb.schema.IndexSettingImpl.FULLTEXT_ANALYZER
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_CARTESIAN_MAX
import org.neo4j.graphdb.schema.IndexSettingImpl.SPATIAL_CARTESIAN_MIN
import org.neo4j.kernel.impl.index.schema.GenericNativeIndexProvider

import java.util.concurrent.TimeUnit

class IndexTest extends RefcardTest with QueryStatisticsTestSupport {
  val graphDescription = List("A:Person KNOWS B:Person")
  val title = "INDEX"
  override val linkId = "indexes-for-search-performance"
  private val nativeProvider = GenericNativeIndexProvider.DESCRIPTOR.name()

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
        assert(result.toList.size === 10)
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

Create a b-tree index on nodes with label `Person` and property `name`.

###assertion=create-index
//

CREATE INDEX index_name FOR ()-[k:KNOWS]-() ON (k.since)
###

Create a b-tree index with the name `index_name` on relationships with type `KNOWS` and property `since`.

###assertion=create-index
//

CREATE INDEX FOR (p:Person) ON (p.surname)
OPTIONS {indexProvider: '$nativeProvider', indexConfig: {`${SPATIAL_CARTESIAN_MIN.getSettingName}`: [-100.0, -100.0], `${SPATIAL_CARTESIAN_MAX.getSettingName}`: [100.0, 100.0]}}
###

Create a b-tree index on nodes with label `Person` and property `surname` with the index provider `$nativeProvider` and given `spatial.cartesian` settings.
The other index settings will have their default values.

###assertion=create-index
//

CREATE INDEX FOR (p:Person) ON (p.name, p.age)
###

Create a composite b-tree index on nodes with label `Person` and the properties `name` and `age`, throws an error if the index already exist.

###assertion=create-existing-index
//

CREATE INDEX IF NOT EXISTS FOR (p:Person) ON (p.name, p.age)
###

Create a composite b-tree index on nodes with label `Person` and the properties `name` and `age` if it does not already exist, does nothing if it did exist.

###assertion=create-index
//

CREATE LOOKUP INDEX lookup_index_name FOR (n) ON EACH labels(n)
###

Create a token lookup index with the name `lookup_index_name` on nodes with any label.

###assertion=create-index
//

CREATE LOOKUP INDEX FOR ()-[r]-() ON EACH type(r)
###

Create a token lookup index on relationships with any relationship type.

###assertion=create-index
//

CREATE FULLTEXT INDEX node_fulltext_index_name FOR (n:Friend) ON EACH [n.name]
OPTIONS {indexConfig: {`${FULLTEXT_ANALYZER.getSettingName}`: 'swedish'}}
###

Create a fulltext index on nodes with the name `node_fulltext_index_name` and analyzer `swedish`. Fulltext indexes on nodes can only be used by from the procedure `db.index.fulltext.queryNodes`.
The other index settings will have their default values.

###assertion=create-index
//

CREATE FULLTEXT INDEX rel_fulltext_index_name FOR ()-[r:HAS_PET|BROUGHT_PET]-() ON EACH [r.since, r.price]
###

Create a fulltext index on relationships with the name `rel_fulltext_index_name`. Fulltext indexes on relationships can only be used by from the procedure `db.index.fulltext.queryRelationships`.

###assertion=create-index
//

CREATE TEXT INDEX FOR (f:Friend) ON (f.email)
###

Create a text index on nodes with label `Friend` and property `email`.

###assertion=create-index
//

CREATE TEXT INDEX text_index_name FOR ()-[h:HAS_PET]-() ON (h.favoriteToy)
###

Create a text index with the name `text_index_name` on relationships with type `HAS_PET` and property `favoriteToy`.

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

An BTREE index can be automatically used for the equality comparison.
Note that for example `toLower(n.name) = $$value` will not use an index.

###assertion=match
//

MATCH (n:Person) WHERE n.name = "Alice"

RETURN n
###

An TEXT index can be automatically used for the equality comparison when comparing to a string.
Note that for example `toLower(n.name) = "string"` does not use an index.

###assertion=match
//

MATCH (n:Person)
WHERE n.name < "Bob"

RETURN n
###

An index can automatically be used for range predicates.
Note that a TEXT index is only used if the predicate compares the property with a string.

###assertion=match parameters=aname
//

MATCH (n:Person)
WHERE n.name IN [$$value]

RETURN n
###

An index can automatically be used for the `IN` list checks.

###assertion=match parameters=aname
//

MATCH (n:Person)
WHERE n.name IN ['Bob', 'Alice']

RETURN n
###

An TEXT index can automatically be used for the `IN` list checks when all elements in the list are strings.

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
