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
package org.neo4j.doc.server.helpers;

import java.io.File;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;

public final class WebContainerHelper
{
    private WebContainerHelper()
    {
    }

    public static void cleanTheDatabase( final TestWebContainer testWebContainer )
    {
        if ( testWebContainer == null )
        {
            return;
        }

        rollbackAllOpenTransactions( testWebContainer );

        cleanTheDatabase( testWebContainer.getDefaultDatabase() );
    }

    public static void cleanTheDatabase( GraphDatabaseService db )
    {
        new Transactor( db, new DeleteAllData(), 10 ).execute();
        new Transactor( db, new DeleteAllSchema(), 10 ).execute();
    }

    public static TestWebContainer createReadOnlyContainer( File path ) throws Exception
    {
        CommunityWebContainerBuilder builder = CommunityWebContainerBuilder.builder();
        builder.withProperty( BoltConnector.listen_address.name(), ":0" );
        builder.withProperty( BoltConnector.advertised_address.name(), ":0" );
        builder.withProperty( GraphDatabaseSettings.read_only_database_default.name(), "true" );
        return createContainer( builder, path, true );
    }

    public static TestWebContainer createContainer( CommunityWebContainerBuilder builder, File path, boolean onRandomPorts ) throws Exception
    {
        if ( onRandomPorts )
        {
            builder.onRandomPorts();
        }
        return builder
                .usingDataDir( path != null ? path.getAbsolutePath() : null )
                .build();
    }

    private static void rollbackAllOpenTransactions( TestWebContainer testWebContainer )
    {
        testWebContainer.getTransactionRegistry().rollbackAllSuspendedTransactions();
    }

    private static class DeleteAllData implements UnitOfWork
    {
        @Override
        public void doWork( Transaction transaction )
        {
            deleteAllNodesAndRelationships( transaction );
        }

        private void deleteAllNodesAndRelationships( Transaction tx )
        {
            Iterable<Node> allNodes = tx.getAllNodes();
            for ( Node n : allNodes )
            {
                Iterable<Relationship> relationships = n.getRelationships();
                for ( Relationship rel : relationships )
                {
                    rel.delete();
                }
                n.delete();
            }
        }
    }

    private static class DeleteAllSchema implements UnitOfWork
    {
        @Override
        public void doWork( Transaction transaction )
        {
            deleteAllIndexRules( transaction );
            deleteAllConstraints( transaction );
        }

        private void deleteAllIndexRules( Transaction transaction )
        {
            for ( IndexDefinition index : transaction.schema().getIndexes() )
            {
                if ( !index.isConstraintIndex() )
                {
                    index.drop();
                }
            }
        }

        private void deleteAllConstraints( Transaction transaction )
        {
            for ( ConstraintDefinition constraint : transaction.schema().getConstraints() )
            {
                constraint.drop();
            }
        }
    }
}
