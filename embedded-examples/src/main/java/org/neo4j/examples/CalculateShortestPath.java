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

import java.io.File;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Paths;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class CalculateShortestPath
{
    private static final File databaseDirectory = new File( "target/neo4j-shortest-path" );
    private static final String NAME_KEY = "name";
    private static final Label NODE_LABEL = Label.label( "NODE" );
    private static final RelationshipType KNOWS = RelationshipType.withName( "KNOWS" );

    private static GraphDatabaseService graphDb;
    private static DatabaseManagementService managementService;

    public static void main( final String[] args )
    {
        deleteFileOrDirectory( databaseDirectory );
        managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
        registerShutdownHook();
        try ( Transaction tx = graphDb.beginTx() )
        {
            /*
             *  (Neo) --> (Trinity)
             *     \       ^
             *      v     /
             *    (Morpheus) --> (Cypher)
             *            \         |
             *             v        v
             *            (Agent Smith)
             */
            createChain( "Neo", "Trinity" );
            createChain( "Neo", "Morpheus", "Trinity" );
            createChain( "Morpheus", "Cypher", "Agent Smith" );
            createChain( "Morpheus", "Agent Smith" );
            tx.commit();
        }

        try ( Transaction tx = graphDb.beginTx() )
        {
            // So let's find the shortest path between Neo and Agent Smith
            Node neo = getOrCreateNode( "Neo" );
            Node agentSmith = getOrCreateNode( "Agent Smith" );
            // tag::shortestPathUsage[]
            PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
                    PathExpanders.forTypeAndDirection( KNOWS, Direction.BOTH ), 4 );
            Path foundPath = finder.findSinglePath( neo, agentSmith );
            System.out.println( "Path from Neo to Agent Smith: "
                                + Paths.simplePathToString( foundPath, NAME_KEY ) );
            // end::shortestPathUsage[]
        }

        System.out.println( "Shutting down database ..." );
        managementService.shutdown();
    }

    private static void createChain( String... names )
    {
        for ( int i = 0; i < names.length - 1; i++ )
        {
            Node firstNode = getOrCreateNode( names[i] );
            Node secondNode = getOrCreateNode( names[i + 1] );
            firstNode.createRelationshipTo( secondNode, KNOWS );
        }
    }

    private static Node getOrCreateNode( String name )
    {
        Node node = graphDb.findNode( NODE_LABEL, NAME_KEY, name );
        if ( node == null )
        {
            node = graphDb.createNode( NODE_LABEL );
            node.setProperty( NAME_KEY, name );
        }
        return node;
    }

    private static void registerShutdownHook()
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> managementService.shutdown()));
    }

    private static void deleteFileOrDirectory( File file )
    {
        if ( file.exists() )
        {
            if ( file.isDirectory() )
            {
                for ( File child : file.listFiles() )
                {
                    deleteFileOrDirectory( child );
                }
            }
            file.delete();
        }
    }
}
