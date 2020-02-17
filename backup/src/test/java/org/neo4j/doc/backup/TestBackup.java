/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.doc.backup;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.backup.OnlineBackup;
import org.neo4j.kernel.impl.enterprise.configuration.OnlineBackupSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.impl.api.TransactionHeaderInformation;
import org.neo4j.kernel.impl.factory.CommunityEditionModule;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo4j.kernel.impl.store.format.standard.StandardV3_0;
import org.neo4j.kernel.impl.transaction.TransactionHeaderInformationFactory;
import org.neo4j.doc.test.DbRepresentation;
import org.neo4j.doc.test.TestGraphDatabaseFactory;
import org.neo4j.doc.test.rule.TestDirectory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBackup
{
    @Rule
    public final TestDirectory testDir = TestDirectory.testDirectory();

    private File serverPath;
    private File otherServerPath;
    private File backupPath;
    private List<ServerInterface> servers;

    @Before
    public void before() throws Exception
    {
        servers = new ArrayList<>();
        File base = testDir.directory();
        serverPath = new File( base, "server" );
        otherServerPath = new File( base, "server2" );
        backupPath = new File( base, "backuedup-serverdb" );
    }

    @After
    public void shutDownServers()
    {
        for ( ServerInterface server : servers )
        {
            server.shutdown();
        }
        servers.clear();
    }

    @Test
    public void fullThenIncremental() throws Exception
    {
        DbRepresentation initialDataSetRepresentation = createInitialDataSet( serverPath );
        ServerInterface server = startServer( serverPath );

        // START SNIPPET: onlineBackup
        OnlineBackup backup = OnlineBackup.from( "127.0.0.1" );
        backup.backup( backupPath );
        assertTrue( "Should be consistent", backup.isConsistent() );
        // END SNIPPET: onlineBackup
        assertEquals( initialDataSetRepresentation, getDbRepresentation() );
        shutdownServer( server );

        DbRepresentation furtherRepresentation = addMoreData( serverPath );
        server = startServer( serverPath );
        // START SNIPPET: onlineBackup
        backup.backup( backupPath );
        // END SNIPPET: onlineBackup
        assertTrue( "Should be consistent", backup.isConsistent() );
        assertEquals( furtherRepresentation, getDbRepresentation() );
        shutdownServer( server );
    }

    private ServerInterface startServer( File path ) throws Exception
    {
        ServerInterface server = new EmbeddedServer( path, "127.0.0.1:6362" );
        server.awaitStarted();
        servers.add( server );
        return server;
    }

    private void shutdownServer( ServerInterface server ) throws Exception
    {
        server.shutdown();
        servers.remove( server );
    }

    private DbRepresentation addMoreData( File path )
    {
        GraphDatabaseService db = startGraphDatabase( path, false );
        DbRepresentation representation;
        try ( Transaction tx = db.beginTx() )
        {
            Node node = db.createNode();
            node.setProperty( "backup", "Is great" );
            db.createNode().createRelationshipTo( node,
                    RelationshipType.withName( "LOVES" ) );
            tx.success();
        }
        finally
        {
            representation = DbRepresentation.of( db );
            db.shutdown();
        }
        return representation;
    }

    private GraphDatabaseService startGraphDatabase( File storeDir, boolean withOnlineBackup )
    {
        GraphDatabaseFactory dbFactory = new TestGraphDatabaseFactory()
        {
            @Override
            protected GraphDatabaseService newDatabase( File storeDir, Config config,
                    GraphDatabaseFacadeFactory.Dependencies dependencies )
            {
                return new GraphDatabaseFacadeFactory( DatabaseInfo.COMMUNITY,
                        ( platformModule) -> new CommunityEditionModule( platformModule )
                {

                    @Override
                    protected TransactionHeaderInformationFactory createHeaderInformationFactory()
                    {
                        return new TransactionHeaderInformationFactory.WithRandomBytes()
                        {
                            @Override
                            protected TransactionHeaderInformation createUsing( byte[] additionalHeader )
                            {
                                return new TransactionHeaderInformation( 1, 2, additionalHeader );
                            }
                        };
                    }
                } ).newFacade( storeDir, config, dependencies );
            }
        };
        return dbFactory.newEmbeddedDatabaseBuilder( storeDir )
                .setConfig( OnlineBackupSettings.online_backup_enabled, String.valueOf( withOnlineBackup ) )
                .setConfig( GraphDatabaseSettings.keep_logical_logs, Settings.TRUE )
                .setConfig( GraphDatabaseSettings.record_format, StandardV3_0.NAME )
                .newGraphDatabase();
    }

    private DbRepresentation createInitialDataSet( File path )
    {
        GraphDatabaseService db = startGraphDatabase( path, false );
        try
        {
            createInitialDataset( db );
            return DbRepresentation.of( db );
        }
        finally
        {
            db.shutdown();
        }
    }

    private void createInitialDataset( GraphDatabaseService db )
    {
        // 4 transactions: THE transaction, "mykey" property key, "db-index" index, "KNOWS" rel type.
        try ( Transaction tx = db.beginTx() )
        {
            Node node = db.createNode( Label.label( "Me" ) );
            node.setProperty( "myKey", "myValue" );
            Index<Node> nodeIndex = db.index().forNodes( "db-index" );
            nodeIndex.add( node, "myKey", "myValue" );
            db.createNode().createRelationshipTo( node, RelationshipType.withName( "KNOWS" ) );
            tx.success();
        }
    }

    private Config getFormatConfig()
    {
        return Config.defaults( GraphDatabaseSettings.record_format, StandardV3_0.NAME );
    }

    private DbRepresentation getDbRepresentation()
    {
        return DbRepresentation.of( backupPath, getFormatConfig() );
    }
}
