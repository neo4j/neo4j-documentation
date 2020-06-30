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
package org.neo4j.examples.backup;

import com.neo4j.backup.OnlineBackup;
import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.neo4j.dbms.api.DatabaseManagementService;

import static org.junit.Assert.assertTrue;

public class TestBackup
{
    private static final Path databaseDirectory = Path.of( "target/backup-store" );
    private static final Path backupDirectory = Path.of( "target/backup-destination" );
    private DatabaseManagementService managementService;

    @Before
    public void before() throws IOException
    {
        Files.createDirectories( backupDirectory );
        managementService = new EnterpriseDatabaseManagementServiceBuilder( databaseDirectory ).build();
    }

    @After
    public void shutDownServers()
    {
        if ( managementService != null )
        {
            managementService.shutdown();
        }
    }

    @Test
    public void fullBackup()
    {
        // tag::onlineBackup[]
        var backup = OnlineBackup.from( "127.0.0.1" );

        var backupResult = backup.backup( "neo4j", backupDirectory );
        assertTrue( "Should be consistent", backupResult.isConsistent() );
        // end::onlineBackup[]
    }

    @Test
    public void backupNonDefaults()
    {
        // tag::backupNonDefaults[]
        var out = new ByteArrayOutputStream();
        var backup = OnlineBackup.from( "127.0.0.1" )
                .withFallbackToFullBackup( false )
                .withConsistencyCheck( true )
                .withOutputStream( out );

        var backupResult = backup.backup( "neo4j", backupDirectory );
        assertTrue( "Custom output stream should not be empty", out.toByteArray().length > 0 );
        assertTrue( "Should be consistent", backupResult.isConsistent() );
        // end::backupNonDefaults[]
    }
}
