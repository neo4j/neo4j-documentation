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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

class StartWithConfigurationDocTest
{
    @TempDir
    Path directory;

    @Test
    void loadFromFile()
    {
        String pathToConfig = "src/test/resources/";
        // tag::startDbWithConfig[]
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( directory )
                                                            .loadPropertiesFromFile( pathToConfig + "neo4j.conf" ).build();
        GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );
                // end::startDbWithConfig[]
        Assertions.assertNotNull( graphDb );
        managementService.shutdown();
    }

    @Test
    void loadFromHashmap()
    {
        // tag::startDbWithMapConfig[]
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( directory)
                                .setConfig( GraphDatabaseSettings.pagecache_memory, "512M" )
                                .setConfig( GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds( 60 ) )
                                .setConfig( GraphDatabaseSettings.preallocate_logical_logs, true ).build();
        GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );

        // end::startDbWithMapConfig[]
        Assertions.assertNotNull( graphDb );
        managementService.shutdown();
    }
}
