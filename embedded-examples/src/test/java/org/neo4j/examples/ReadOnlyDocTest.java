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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.security.WriteOperationsNotAllowedException;
import org.neo4j.io.fs.FileUtils;

import static org.junit.Assert.fail;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

/**
 * How to get a read-only Neo4j instance.
 */
public class ReadOnlyDocTest
{
    protected GraphDatabaseService graphDb;
    private DatabaseManagementService managementService;

    /**
     * Create read only database.
     */
    @Before
    public void prepareReadOnlyDatabase() throws IOException
    {
        File dir = new File( "target/read-only-managementService/location" );
        if ( dir.exists() )
        {
            FileUtils.deleteRecursively( dir );
        }
        new DatabaseManagementServiceBuilder( dir ).build().shutdown();
        // tag::createReadOnlyInstance[]
        managementService = new DatabaseManagementServiceBuilder( dir ).setConfig( GraphDatabaseSettings.read_only, true ).build();
        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
        // end::createReadOnlyInstance[]
    }

    /**
     * Shutdown the database.
     */
    @After
    public void shutdownDatabase()
    {
        managementService.shutdown();
    }

    @Test
    public void makeSureDbIsOnlyReadable()
    {
        // when
        Transaction tx = graphDb.beginTx();
        try
        {
            graphDb.createNode();
            tx.commit();
            fail( "expected exception" );
        }
        // then
        catch ( WriteOperationsNotAllowedException e )
        {
            // ok
        }
    }
}
