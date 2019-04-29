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

import org.junit.Rule;
import org.junit.Test;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.database.DatabaseManagementService;
import org.neo4j.doc.test.rule.TestDirectory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.DatabaseManagementServiceBuilder;

import static org.junit.Assert.assertNotNull;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class StartWithConfigurationDocTest
{
    @Rule
    public final TestDirectory testDirectory = TestDirectory.testDirectory();

    @Test
    public void loadFromFile()
    {
        String pathToConfig = "src/test/resources/";
        // tag::startDbWithConfig[]
        DatabaseManagementService managementService =
                new DatabaseManagementServiceBuilder( testDirectory.databaseDir() ).loadPropertiesFromFile(
                        pathToConfig + "neo4j.conf" ).build();
        GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );
                // end::startDbWithConfig[]
        assertNotNull( graphDb );
        managementService.shutdown();
    }

    @Test
    public void loadFromHashmap()
    {
        // tag::startDbWithMapConfig[]
        DatabaseManagementService managementService =
                new DatabaseManagementServiceBuilder( testDirectory.databaseDir() ).setConfig( GraphDatabaseSettings.pagecache_memory,
                        "512M" ).setConfig( GraphDatabaseSettings.string_block_size, "60" ).setConfig( GraphDatabaseSettings.array_block_size,
                        "300" ).build();
        GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );

        // end::startDbWithMapConfig[]
        assertNotNull( graphDb );
        managementService.shutdown();
    }
}
