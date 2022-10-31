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

import java.io.IOException;
import java.nio.file.Path;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.io.fs.FileUtils;

public class EmbeddedNeo4jWithBolt {
    private static final Path DB_PATH = Path.of("target/neo4j-store-with-bolt");

    public static void main(final String[] args) throws IOException {
        System.out.println("Starting database ...");
        FileUtils.deleteDirectory(DB_PATH);

        // tag::startDb[]
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(DB_PATH)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 7687))
                .build();
        // end::startDb[]

        managementService.shutdown();
    }
}
