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
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.internal.helpers.collection.Iterators.loop;

public class EmbeddedNeo4jWithIndexing
{
    private static final Path databaseDirectory = Path.of( "target/neo4j-store-with-new-indexing" );

    public static void main( final String[] args ) throws IOException
    {
        System.out.println( "Starting database ..." );
        FileUtils.deletePathRecursively( databaseDirectory );

        // tag::startDb[]
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        GraphDatabaseService graphDb = managementService.database( DEFAULT_DATABASE_NAME );
        // end::startDb[]

        {
            // tag::createIndex[]
            IndexDefinition usernamesIndex;
            try ( Transaction tx = graphDb.beginTx() )
            {
                Schema schema = tx.schema();
                usernamesIndex = schema.indexFor( Label.label( "User" ) )  // <1>
                        .on( "username" )                                  // <2>
                        .withName( "usernames" )                           // <3>
                        .create();                                         // <4>
                tx.commit();                                               // <5>
            }
            // end::createIndex[]
            // tag::wait[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                Schema schema = tx.schema();
                schema.awaitIndexOnline( usernamesIndex, 10, TimeUnit.SECONDS );
            }
            // end::wait[]
            // tag::progress[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                Schema schema = tx.schema();
                System.out.println( String.format( "Percent complete: %1.0f%%",
                        schema.getIndexPopulationProgress( usernamesIndex ).getCompletedPercentage() ) );
            }
            // end::progress[]
        }

        {
            // tag::addUsers[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                Label label = Label.label( "User" );

                // Create some users
                for ( int id = 0; id < 100; id++ )
                {
                    Node userNode = tx.createNode( label );
                    userNode.setProperty( "username", "user" + id + "@neo4j.org" );
                }
                System.out.println( "Users created" );
                tx.commit();
            }
            // end::addUsers[]
        }

        {
            // tag::findUsers[]
            Label label = Label.label( "User" );
            int idToFind = 45;
            String nameToFind = "user" + idToFind + "@neo4j.org";
            try ( Transaction tx = graphDb.beginTx() )
            {
                try ( ResourceIterator<Node> users =
                              tx.findNodes( label, "username", nameToFind ) )
                {
                    ArrayList<Node> userNodes = new ArrayList<>();
                    while ( users.hasNext() )
                    {
                        userNodes.add( users.next() );
                    }

                    for ( Node node : userNodes )
                    {
                        System.out.println(
                                "The username of user " + idToFind + " is " + node.getProperty( "username" ) );
                    }
                }
            }
            // end::findUsers[]
        }

        {
            // tag::resourceIterator[]
            Label label = Label.label( "User" );
            int idToFind = 45;
            String nameToFind = "user" + idToFind + "@neo4j.org";
            try ( Transaction tx = graphDb.beginTx();
                  ResourceIterator<Node> users = tx.findNodes( label, "username", nameToFind ) )
            {
                Node firstUserNode;
                if ( users.hasNext() )
                {
                    firstUserNode = users.next();
                }
                users.close();
                // ... Do stuff with the firstUserNode we found ...
            }
            // end::resourceIterator[]
        }

        {
            // tag::updateUsers[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                Label label = Label.label( "User" );
                int idToFind = 45;
                String nameToFind = "user" + idToFind + "@neo4j.org";

                for ( Node node : loop( tx.findNodes( label, "username", nameToFind ) ) )
                {
                    node.setProperty( "username", "user" + (idToFind + 1) + "@neo4j.org" );
                }
                tx.commit();
            }
            // end::updateUsers[]
        }

        {
            // tag::deleteUsers[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                Label label = Label.label( "User" );
                int idToFind = 46;
                String nameToFind = "user" + idToFind + "@neo4j.org";

                for ( Node node : loop( tx.findNodes( label, "username", nameToFind ) ) )
                {
                    node.delete();
                }
                tx.commit();
            }
            // end::deleteUsers[]
        }

        {
            // tag::dropIndex[]
            try ( Transaction tx = graphDb.beginTx() )
            {
                IndexDefinition usernamesIndex = tx.schema().getIndexByName( "usernames" ); // <1>
                usernamesIndex.drop();
                tx.commit();
            }
            // end::dropIndex[]
        }

        System.out.println( "Shutting down database ..." );
        // tag::shutdownDb[]
        managementService.shutdown();
        // end::shutdownDb[]
    }
}
