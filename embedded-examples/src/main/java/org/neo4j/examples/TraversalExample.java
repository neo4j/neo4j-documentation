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
// tag::sampleDocumentation[]
// tag::_sampleDocumentation[]
package org.neo4j.examples;

import java.io.IOException;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.io.fs.FileUtils;

import static java.lang.System.out;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TraversalExample
{
    private GraphDatabaseService db;
    private TraversalDescription friendsTraversal;

    private static final java.nio.file.Path databaseDirectory = java.nio.file.Path.of( "target/neo4j-traversal-example" );

    public static void main( String[] args ) throws IOException
    {
        FileUtils.deletePathRecursively( databaseDirectory );
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        GraphDatabaseService database = managementService.database( DEFAULT_DATABASE_NAME );
        TraversalExample example = new TraversalExample( database );
        Node joe = example.createData();
        example.run( joe );
        managementService.shutdown();
    }

    public TraversalExample( GraphDatabaseService db )
    {
        this.db = db;
    }

    private Node createData()
    {
        try ( Transaction transaction = db.beginTx() )
        {
            String query = "CREATE (joe {name: 'Joe'}), (sara {name: 'Sara'}), " + "(lisa {name: 'Lisa'}), (peter {name: 'PETER'}), (dirk {name: 'Dirk'}), " +
                    "(lars {name: 'Lars'}), (ed {name: 'Ed'})," + "(joe)-[:KNOWS]->(sara), (lisa)-[:LIKES]->(joe), " +
                    "(peter)-[:KNOWS]->(sara), (dirk)-[:KNOWS]->(peter), " + "(lars)-[:KNOWS]->(drk), (ed)-[:KNOWS]->(lars), " + "(lisa)-[:KNOWS]->(lars) " +
                    "RETURN joe";
            Result result = transaction.execute( query );
            Object joe = result.columnAs( "joe" ).next();
            if ( joe instanceof Node )
            {
                transaction.commit();
                return (Node) joe;
            }
            else
            {
                throw new RuntimeException( "Joe isn't a node!" );
            }
        }
    }

    private void run( Node joe )
    {
        try (Transaction tx = db.beginTx())
        {
            joe = tx.getNodeById( joe.getId() );
            init( tx );
            out.println( knowsLikesTraverser( tx, joe ) );
            out.println( traverseBaseTraverser( joe ) );
            out.println( depth3( joe ) );
            out.println( depth4( joe ) );
            out.println( nodes( joe ) );
            out.println( relationships( joe ) );
        }
    }

    void init( Transaction tx )
    {
        // tag::basetraverser[]
        friendsTraversal = tx.traversalDescription()
                .depthFirst()
                .relationships( Rels.KNOWS )
                .uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
        // end::basetraverser[]
    }

    public String knowsLikesTraverser( Transaction transaction, Node node )
    {
        String output = "";
        // tag::knowslikestraverser[]
        for ( Path position : transaction.traversalDescription()
                .depthFirst()
                .relationships( Rels.KNOWS )
                .relationships( Rels.LIKES, Direction.INCOMING )
                .evaluator( Evaluators.toDepth( 5 ) )
                .traverse( node ) )
        {
            output += position + "\n";
        }
        // end::knowslikestraverser[]
        return output;
    }

    public String traverseBaseTraverser( Node node )
    {
        String output = "";
        // tag::traversebasetraverser[]
        for ( Path path : friendsTraversal.traverse( node ) )
        {
            output += path + "\n";
        }
        // end::traversebasetraverser[]
        return output;
    }

    public String depth3( Node node )
    {
        String output = "";
        // tag::depth3[]
        for ( Path path : friendsTraversal
                .evaluator( Evaluators.toDepth( 3 ) )
                .traverse( node ) )
        {
            output += path + "\n";
        }
        // end::depth3[]
        return output;
    }

    public String depth4( Node node )
    {
        String output = "";
        // tag::depth4[]
        for ( Path path : friendsTraversal
                .evaluator( Evaluators.fromDepth( 2 ) )
                .evaluator( Evaluators.toDepth( 4 ) )
                .traverse( node ) )
        {
            output += path + "\n";
        }
        // end::depth4[]
        return output;
    }

    public String nodes( Node node )
    {
        String output = "";
        // tag::nodes[]
        for ( Node currentNode : friendsTraversal
                .traverse( node )
                .nodes() )
        {
            output += currentNode.getProperty( "name" ) + "\n";
        }
        // end::nodes[]
        return output;
    }

    public String relationships( Node node )
    {
        String output = "";
        // tag::relationships[]
        for ( Relationship relationship : friendsTraversal
                .traverse( node )
                .relationships() )
        {
            output += relationship.getType().name() + "\n";
        }
        // end::relationships[]
        return output;
    }

    // tag::sourceRels[]
    private enum Rels implements RelationshipType
    {
        LIKES, KNOWS
    }
    // end::sourceRels[]
}
