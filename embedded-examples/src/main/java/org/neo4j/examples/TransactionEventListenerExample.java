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
import java.io.IOException;

import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventListener;
import org.neo4j.io.fs.FileUtils;

import static com.google.common.collect.Iterables.size;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TransactionEventListenerExample
{
    private static final File HOME_DIRECTORY = new File( "target/transaction-event-listener" );

    // tag::TransactionEventListener[]
    public static void main( String[] args ) throws IOException
    {
        FileUtils.deleteRecursively( HOME_DIRECTORY );
        var managementService = new DatabaseManagementServiceBuilder( HOME_DIRECTORY ).build();
        var database = managementService.database( DEFAULT_DATABASE_NAME );

        var countingListener = new CountingTransactionEventListener();
        managementService.registerTransactionEventListener( DEFAULT_DATABASE_NAME, countingListener );

        var connectionType = RelationshipType.withName( "CONNECTS" );
        try ( var transaction = database.beginTx() )
        {
            var startNode = transaction.createNode();
            var endNode = transaction.createNode();
            startNode.createRelationshipTo( endNode, connectionType );
            transaction.commit();
        }
    }

    private static class CountingTransactionEventListener implements TransactionEventListener<CreatedEntitiesCounter>
    {
        @Override
        public CreatedEntitiesCounter beforeCommit( TransactionData data, Transaction transaction, GraphDatabaseService databaseService ) throws Exception
        {
            return new CreatedEntitiesCounter( size( data.createdNodes() ), size( data.createdRelationships() ) );
        }

        @Override
        public void afterCommit( TransactionData data, CreatedEntitiesCounter entitiesCounter, GraphDatabaseService databaseService )
        {
            System.out.println( "Number of created nodes: " + entitiesCounter.getCreatedNodes() );
            System.out.println( "Number of created relationships: " + entitiesCounter.getCreatedRelationships() );
        }

        @Override
        public void afterRollback( TransactionData data, CreatedEntitiesCounter state, GraphDatabaseService databaseService )
        {
            //empty
        }
    }

    private static class CreatedEntitiesCounter
    {
        private final long createdNodes;
        private final long createdRelationships;

        public CreatedEntitiesCounter( long createdNodes, long createdRelationships )
        {
            this.createdNodes = createdNodes;
            this.createdRelationships = createdRelationships;
        }

        public long getCreatedNodes()
        {
            return createdNodes;
        }

        public long getCreatedRelationships()
        {
            return createdRelationships;
        }
    }
    // end::TransactionEventListener[]
}
