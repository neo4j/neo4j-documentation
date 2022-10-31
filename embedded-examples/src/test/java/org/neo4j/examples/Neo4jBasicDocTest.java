/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.ByteUnit;

/**
 * An example of unit testing with Neo4j.
 */
class Neo4jBasicDocTest {
    @TempDir
    Path directory;
    private GraphDatabaseService graphDb;
    private DatabaseManagementService managementService;

    /**
     * Create temporary database for each unit test.
     */
    // tag::beforeTest[]
    @BeforeEach
    void prepareTestDatabase() {
        managementService = new DatabaseManagementServiceBuilder(directory).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
    }
    // end::beforeTest[]

    /**
     * Shutdown the database.
     */
    // tag::afterTest[]
    @AfterEach
    void destroyTestDatabase() {
        managementService.shutdown();
    }
    // end::afterTest[]

    @Test
    void startWithConfiguration() {
        // tag::startDbWithConfig[]
        DatabaseManagementService service = new DatabaseManagementServiceBuilder(directory.resolve("withConfiguration"))
                .setConfig(GraphDatabaseSettings.pagecache_memory, ByteUnit.mebiBytes(512))
                .setConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds(60))
                .setConfig(GraphDatabaseSettings.preallocate_logical_logs, true).build();
        // end::startDbWithConfig[]
        service.shutdown();
    }

    @Test
    void shouldCreateNode() {
        // tag::unitTest[]
        Node n;
        try (Transaction tx = graphDb.beginTx()) {
            n = tx.createNode();
            n.setProperty("name", "Nancy");
            tx.commit();
        }

        // The node should have a valid id
        assertThat(n.getElementId()).isNotEmpty();

        // Retrieve a node by using the id of the created node. The id's and
        // property should match.
        try (Transaction tx = graphDb.beginTx()) {
            Node foundNode = tx.getNodeByElementId(n.getElementId());
            assertThat(foundNode.getElementId()).isEqualTo(n.getElementId());
            assertThat((String) foundNode.getProperty("name")).isEqualTo("Nancy");
        }
        // end::unitTest[]
    }
}
