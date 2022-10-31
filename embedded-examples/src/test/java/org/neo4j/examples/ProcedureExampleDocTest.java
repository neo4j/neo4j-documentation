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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.internal.helpers.collection.MapUtil.map;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.doc.kernel.impl.proc.JarBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

class ProcedureExampleDocTest {
    private GraphDatabaseService db;
    private DatabaseManagementService managementService;

    @TempDir
    Path directory;

    @Test
    void listDenseNodesShouldWork() throws Throwable {
        // Given
        new JarBuilder().createJarFor(directory.resolve("myProcedures.jar").toFile(), ProcedureExample.class);
        managementService =
                new DatabaseManagementServiceBuilder(directory).setConfig(GraphDatabaseSettings.plugin_dir, directory.toAbsolutePath()).build();
        db = managementService.database(DEFAULT_DATABASE_NAME);

        try (Transaction transaction = db.beginTx()) {
            Node node1 = transaction.createNode();
            Node node2 = transaction.createNode();
            Node node3 = transaction.createNode();

            node1.createRelationshipTo(node1, RelationshipType.withName("KNOWS"));
            node1.createRelationshipTo(node2, RelationshipType.withName("KNOWS"));
            node1.createRelationshipTo(node3, RelationshipType.withName("KNOWS"));

            // When
            Result res = transaction.execute("CALL org.neo4j.examples.findDenseNodes(2)");

            // Then
            assertEquals(map("degree", 3L, "nodeId", node1.getId()), res.next());
            assertFalse(res.hasNext());
        }
    }

    @AfterEach
    void tearDown() {
        if (managementService != null) {
            managementService.shutdown();
        }
    }
}
