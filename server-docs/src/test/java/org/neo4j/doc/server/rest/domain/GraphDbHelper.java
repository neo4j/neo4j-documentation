/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.server.rest.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.security.AnonymousContext;
import org.neo4j.kernel.api.security.SecurityContext;
import org.neo4j.server.database.Database;

public class GraphDbHelper
{
    private final Database database;

    public GraphDbHelper( Database database )
    {
        this.database = database;
    }

    public void setNodeProperties( long nodeId, Map<String, Object> properties )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().getNodeById( nodeId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                node.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public long createNode( Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().createNode( labels );
            tx.success();
            return node.getId();
        }
    }

    public long createNode( Map<String, Object> properties, Label... labels )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ) )
        {
            Node node = database.getGraph().createNode( labels );
            for ( Map.Entry<String, Object> entry : properties.entrySet() )
            {
                node.setProperty( entry.getKey(), entry.getValue() );
            }
            tx.success();
            return node.getId();
        }
    }

    public long createRelationship( String type, long startNodeId, long endNodeId )
    {
        try (Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ))
        {
            Node startNode = database.getGraph().getNodeById( startNodeId );
            Node endNode = database.getGraph().getNodeById( endNodeId );
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    RelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public long createRelationship( String type )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ) )
        {
            Node startNode = database.getGraph().createNode();
            Node endNode = database.getGraph().createNode();
            Relationship relationship = startNode.createRelationshipTo( endNode,
                    RelationshipType.withName( type ) );
            tx.success();
            return relationship.getId();
        }
    }

    public void setRelationshipProperties( long relationshipId, Map<String, Object> properties )

    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.writeToken() ) )
        {
            Relationship relationship = database.getGraph().getRelationshipById( relationshipId );
            for ( Map.Entry<String, Object> propertyEntry : properties.entrySet() )
            {
                relationship.setProperty( propertyEntry.getKey(), propertyEntry.getValue() );
            }
            tx.success();
        }
    }

    public void addNodeToIndex( String indexName, String key, Object value, long id )
    {
        try (Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, SecurityContext.AUTH_DISABLED ))
        {
            database.getGraph().index().forNodes( indexName ).add( database.getGraph().getNodeById( id ), key, value );
            tx.success();
        }
    }

    public Collection<Long> getIndexedNodes( String indexName, String key, Object value )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.write() ) )
        {
            Collection<Long> result = new ArrayList<>();
            for ( Node node : database.getGraph().index().forNodes( indexName ).get( key, value ) )
            {
                result.add( node.getId() );
            }
            tx.success();
            return result;
        }
    }

    public Collection<Long> getIndexedRelationships( String indexName, String key, Object value )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.write() ) )
        {
            Collection<Long> result = new ArrayList<>();
            for ( Relationship relationship : database.getGraph().index().forRelationships( indexName ).get( key, value ) )
            {
                result.add( relationship.getId() );
            }
            tx.success();
            return result;
        }
    }

    public void addRelationshipToIndex( String indexName, String key, String value, long relationshipId )
    {
        try ( Transaction tx = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, SecurityContext.AUTH_DISABLED ) )
        {
            Index<Relationship> index = database.getGraph().index().forRelationships( indexName );
            index.add( database.getGraph().getRelationshipById( relationshipId ), key, value );
            tx.success();
        }

    }

    public String[] getNodeIndexes()
    {
        try (Transaction transaction = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.read() ))
        {
            return database.getGraph().index().nodeIndexNames();
        }
    }

    public Index<Node> createNodeIndex( String named )
    {
        try ( Transaction transaction = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, SecurityContext.AUTH_DISABLED ) )
        {
            Index<Node> nodeIndex = database.getGraph().index()
                    .forNodes( named );
            transaction.success();
            return nodeIndex;
        }
    }

    public String[] getRelationshipIndexes()
    {
        try (Transaction transaction = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, AnonymousContext.read() ))
        {
            return database.getGraph().index()
                    .relationshipIndexNames();
        }
    }

    public Index<Relationship> createRelationshipIndex( String named )
    {
        try (Transaction transaction = database.getGraph().beginTransaction( KernelTransaction.Type.implicit, SecurityContext.AUTH_DISABLED ))
        {
            RelationshipIndex relationshipIndex = database.getGraph().index()
                    .forRelationships( named );
            transaction.success();
            return relationshipIndex;
        }
    }

}
