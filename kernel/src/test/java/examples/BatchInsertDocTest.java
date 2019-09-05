/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
package examples;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.neo4j.batchinsert.BatchInserter;
import org.neo4j.batchinsert.BatchInserters;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.layout.DatabaseLayout;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.io.layout.DatabaseLayout.of;

public class BatchInsertDocTest
{

    @Before
    public void before() throws Exception
    {
        FileUtils.forceMkdir( new File( "target/docs" ) );
    }

    @Test
    public void insert() throws Exception
    {
        // Make sure our scratch directory is clean
        String database = "neo4j";
        File databaseDirectory = clean( "target/" + database );
        DatabaseLayout tempLayout = of( databaseDirectory, () ->
                {
                    Path storeDir = databaseDirectory.getParentFile().toPath();
                    Path defaultTxLogsPath = Path.of( "data", "tx-logs" );
                    return Optional.of( storeDir.resolve( defaultTxLogsPath ).toFile() );
                } );

        // tag::insert[]
        BatchInserter inserter = null;
        try
        {
            inserter = BatchInserters.inserter( tempLayout );

            Label personLabel = Label.label( "Person" );
            inserter.createDeferredSchemaIndex( personLabel ).on( "name" ).create();

            Map<String, Object> properties = new HashMap<>();

            properties.put( "name", "Mattias" );
            long mattiasNode = inserter.createNode( properties, personLabel );

            properties.put( "name", "Chris" );
            long chrisNode = inserter.createNode( properties, personLabel );

            RelationshipType knows = RelationshipType.withName( "KNOWS" );
            inserter.createRelationship( mattiasNode, chrisNode, knows, null );
        }
        finally
        {
            if ( inserter != null )
            {
                inserter.shutdown();
            }
        }
        // end::insert[]

        // try it out from a normal db

        DatabaseManagementService managementService =
                new DatabaseManagementServiceBuilder( tempLayout.getStoreLayout().storeDirectory() ).build();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().awaitIndexesOnline( 10, TimeUnit.SECONDS );
        }
        try ( Transaction tx = db.beginTx() )
        {
            Label personLabelForTesting = Label.label( "Person" );
            Node mNode = tx.findNode( personLabelForTesting, "name", "Mattias" );
            Node cNode = mNode.getSingleRelationship( RelationshipType.withName( "KNOWS" ), Direction.OUTGOING ).getEndNode();
            assertThat( cNode.getProperty( "name" ), is( "Chris" ) );
            assertThat( db.schema()
                    .getIndexes( personLabelForTesting )
                    .iterator()
                    .hasNext(), is( true ) );
        }
        finally
        {
            managementService.shutdown();
        }
    }

    @Test
    public void insertWithConfig() throws IOException
    {
        clean( "target/batchinserter-example-config" );

        // tag::configuredInsert[]
        Config config = Config.defaults( GraphDatabaseSettings.pagecache_memory, "512m" );
        BatchInserter inserter = BatchInserters.inserter( of( new File( "target/batchinserter-example-config" ) ), config );
        // Insert data here ... and then shut down:
        inserter.shutdown();
        // end::configuredInsert[]
    }

    @Test
    public void insertWithConfigFile() throws IOException
    {
        clean( "target/docs/batchinserter-example-config" );
        try ( Writer fw = new OutputStreamWriter( new FileOutputStream( new File( "target/docs/batchinsert-config" ).getAbsoluteFile() ),
                StandardCharsets.UTF_8 ) )
        {
            fw.append( "dbms.memory.pagecache.size=8m" );
        }

        // tag::configFileInsert[]
        File file = new File( "target/docs/batchinsert-config" ).getAbsoluteFile();
        Config config = Config.newBuilder().fromFile( file ).build();
        BatchInserter inserter = BatchInserters.inserter( of( new File( "target/docs/batchinserter-example-config" ) ), config );
        // Insert data here ... and then shut down:
        inserter.shutdown();
        // end::configFileInsert[]
    }

    private File clean( String fileName ) throws IOException
    {
        File directory = new File( fileName );
        FileUtils.deleteDirectory( directory );
        return directory;
    }
}
