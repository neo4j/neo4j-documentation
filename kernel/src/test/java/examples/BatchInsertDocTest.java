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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import org.neo4j.io.layout.Neo4jLayout;
import org.neo4j.test.rule.TestDirectory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class BatchInsertDocTest
{
    @Rule
    public TestDirectory directory = TestDirectory.testDirectory();
    private DatabaseLayout databaseLayout;

    @Before
    public void setUp()
    {
        databaseLayout = Neo4jLayout.of( directory.homeDir() ).databaseLayout( DEFAULT_DATABASE_NAME );
    }

    @Test
    public void insert() throws Exception
    {
        // tag::insert[]
        BatchInserter inserter = null;
        try
        {
            inserter = BatchInserters.inserter( databaseLayout );

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
                new DatabaseManagementServiceBuilder( directory.homeDir() ).build();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );
        try ( Transaction tx = db.beginTx() )
        {
            tx.schema().awaitIndexesOnline( 10, TimeUnit.SECONDS );
        }
        try ( Transaction tx = db.beginTx() )
        {
            Label personLabelForTesting = Label.label( "Person" );
            Node mNode = tx.findNode( personLabelForTesting, "name", "Mattias" );
            Node cNode = mNode.getSingleRelationship( RelationshipType.withName( "KNOWS" ), Direction.OUTGOING ).getEndNode();
            assertThat( cNode.getProperty( "name" ), is( "Chris" ) );
            assertThat( tx.schema()
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
        // tag::configuredInsert[]
        Config config = Config.defaults( GraphDatabaseSettings.pagecache_memory, "512m" );
        BatchInserter inserter = BatchInserters.inserter( databaseLayout, config );
        // Insert data here ... and then shut down:
        inserter.shutdown();
        // end::configuredInsert[]
    }
}
