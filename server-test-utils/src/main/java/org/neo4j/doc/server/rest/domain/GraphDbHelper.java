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
package org.neo4j.doc.server.rest.domain;

import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintCreator;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.internal.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.Kernel;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.security.AnonymousContext;
import org.neo4j.server.database.DatabaseService;

import static org.neo4j.graphdb.Label.label;
import static org.neo4j.internal.helpers.collection.Iterables.count;
import static org.neo4j.internal.helpers.collection.Iterables.single;
import static org.neo4j.internal.kernel.api.security.LoginContext.AUTH_DISABLED;
import static org.neo4j.kernel.api.KernelTransaction.Type.implicit;

public class GraphDbHelper
{
    private final DatabaseService database;

    public GraphDbHelper( DatabaseService database )
    {
        this.database = database;
    }

    public int getNumberOfNodes()
    {
        Kernel kernel = database.getDatabase().getDependencyResolver().resolveDependency( Kernel.class );
        try ( KernelTransaction tx = kernel.beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return Math.toIntExact( tx.dataRead().nodesGetCount() );
        }
        catch ( TransactionFailureException e )
        {
            throw new RuntimeException( e );
        }
    }

    public int getNumberOfRelationships()
    {
        Kernel kernel = database.getDatabase().getDependencyResolver().resolveDependency( Kernel.class );
        try ( KernelTransaction tx = kernel.beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return Math.toIntExact( tx.dataRead().relationshipsGetCount() );
        }
        catch ( TransactionFailureException e )
        {
            throw new RuntimeException( e );
        }
    }

    public Map<String, Object> getNodeProperties( long nodeId )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Node node = tx.getNodeById( nodeId );
            Map<String, Object> allProperties = node.getAllProperties();
            tx.commit();
            return allProperties;
        }
    }

    public void setNodeProperties( long nodeId, Map<String, Object> properties )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = tx.getNodeById( nodeId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                node.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.commit();
        }
    }

    public long createNode( Label... labels )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = tx.createNode( labels );
            tx.commit();
            return node.getId();
        }
    }

    public long createNode( Map<String, Object> properties, Label... labels )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node node = tx.createNode( labels );
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            tx.commit();
            return node.getId();
        }
    }

    public void deleteNode( long id )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.write() ) )
        {
            Node node = tx.getNodeById( id );
            node.delete();
            tx.commit();
        }
    }

    public long createRelationship( String type, long startNodeId, long endNodeId )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node startNode = tx.getNodeById( startNodeId );
            Node endNode = tx.getNodeById( endNodeId );
            Relationship relationship = startNode.createRelationshipTo( endNode, RelationshipType.withName( type ) );
            tx.commit();
            return relationship.getId();
        }
    }

    public long createRelationship( String type )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Node startNode = tx.createNode();
            Node endNode = tx.createNode();
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    RelationshipType.withName( type ) );
            tx.commit();
            return relationship.getId();
        }
    }

    public void setRelationshipProperties( long relationshipId, Map<String, Object> properties )

    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            Relationship relationship = tx.getRelationshipById( relationshipId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                relationship.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.commit();
        }
    }

    public Map<String, Object> getRelationshipProperties( long relationshipId )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Relationship relationship = tx.getRelationshipById( relationshipId );
            Map<String, Object> allProperties = relationship.getAllProperties();
            tx.commit();
            return allProperties;
        }
    }

    public Relationship getRelationship( long relationshipId )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Relationship relationship = tx.getRelationshipById( relationshipId );
            tx.commit();
            return relationship;
        }
    }

    public long getFirstNode()
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.write() ) )
        {
            try
            {
                Node referenceNode = tx.getNodeById( 0L );

                tx.commit();
                return referenceNode.getId();
            }
            catch ( NotFoundException e )
            {
                Node newNode = tx.createNode();
                tx.commit();
                return newNode.getId();
            }
        }
    }

    public void addLabelToNode( long node, String labelName )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.writeToken() ) )
        {
            tx.getNodeById( node ).addLabel( label( labelName ) );
            tx.commit();
        }
    }

    public Iterable<IndexDefinition> getSchemaIndexes( String labelName )
    {
        return database.getDatabase().schema().getIndexes( label( labelName ) );
    }

    public IndexDefinition createSchemaIndex( String labelName, String propertyKey )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AUTH_DISABLED ) )
        {
            IndexDefinition index = database.getDatabase().schema().indexFor( label( labelName ) ).on( propertyKey ).create();
            tx.commit();
            return index;
        }
    }

    public Iterable<ConstraintDefinition> getPropertyUniquenessConstraints( String labelName, final String propertyKey )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            Iterable<ConstraintDefinition> definitions = Iterables.filter( item ->
            {
                if ( item.isConstraintType( ConstraintType.UNIQUENESS ) )
                {
                    Iterable<String> keys = item.getPropertyKeys();
                    return single( keys ).equals( propertyKey );
                }
                else
                {
                    return false;
                }

            }, database.getDatabase().schema().getConstraints( label( labelName ) ) );
            tx.commit();
            return definitions;
        }
    }

    public ConstraintDefinition createPropertyUniquenessConstraint( String labelName, List<String> propertyKeys )
    {
        try ( Transaction tx = database.getDatabase().beginTransaction( implicit, AUTH_DISABLED ) )
        {
            ConstraintCreator creator = database.getDatabase().schema().constraintFor( label( labelName ) );
            for ( String propertyKey : propertyKeys )
            {
                creator = creator.assertPropertyIsUnique( propertyKey );
            }
            ConstraintDefinition result = creator.create();
            tx.commit();
            return result;
        }
    }

    public long getLabelCount( long nodeId )
    {
        try ( Transaction transaction = database.getDatabase().beginTransaction( implicit, AnonymousContext.read() ) )
        {
            return count( transaction.getNodeById( nodeId ).getLabels());
        }
    }
}
