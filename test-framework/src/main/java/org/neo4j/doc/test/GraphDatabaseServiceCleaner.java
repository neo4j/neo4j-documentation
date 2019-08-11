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
package org.neo4j.doc.test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;

public class GraphDatabaseServiceCleaner
{
    private GraphDatabaseServiceCleaner()
    {
        throw new UnsupportedOperationException();
    }

    public static void cleanDatabaseContent( GraphDatabaseService db )
    {
        cleanupSchema( db );
        cleanupAllRelationshipsAndNodes( db );
    }

    public static void cleanupSchema( GraphDatabaseService db )
    {
        try ( Transaction tx = db.beginTx() )
        {
            for ( ConstraintDefinition constraint : db.schema().getConstraints() )
            {
                constraint.drop();
            }

            for ( IndexDefinition index : db.schema().getIndexes() )
            {
                index.drop();
            }
            tx.commit();
        }
    }

    public static void cleanupAllRelationshipsAndNodes( GraphDatabaseService db )
    {
        try ( Transaction tx = db.beginTx() )
        {
            for ( Relationship relationship : db.getAllRelationships() )
            {
                relationship.delete();
            }

            for ( Node node : db.getAllNodes() )
            {
                node.delete();
            }
            tx.commit();
        }
    }
}
