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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.WriteOperationsNotAllowedException;
import org.neo4j.internal.helpers.Exceptions;
import org.neo4j.io.fs.FileUtils;

/**
 * How to get a read-only Neo4j instance.
 */
class ReadOnlyDocTest {
    protected GraphDatabaseService graphDb;
    private DatabaseManagementService managementService;

    /**
     * Create read only database.
     */
    @BeforeEach
    public void prepareReadOnlyDatabase() throws IOException {
        Path dir = Path.of("target/read-only-managementService/location");
        if (Files.exists(dir)) {
            FileUtils.deleteDirectory(dir);
        }
        new DatabaseManagementServiceBuilder(dir).build().shutdown();
        // tag::createReadOnlyInstance[]
        managementService = new DatabaseManagementServiceBuilder(dir).setConfig(GraphDatabaseSettings.read_only_database_default, true).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
        // end::createReadOnlyInstance[]
    }

    /**
     * Shutdown the database.
     */
    @AfterEach
    public void shutdownDatabase() {
        managementService.shutdown();
    }

    @Test
    void makeSureDbIsOnlyReadable() {
        // when
        try (Transaction tx = graphDb.beginTx()) {
            tx.createNode();
            tx.commit();
            fail("expected exception");
        }
        // then
        catch (Exception e) {
            assertTrue(Exceptions.contains(e, c -> c instanceof WriteOperationsNotAllowedException), "Database should be in read only mode");
            // ok
        }
    }
}
