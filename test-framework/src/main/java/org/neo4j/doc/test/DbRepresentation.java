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

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.neo4j.configuration.Config;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.io.fs.IoPrimitiveUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.default_database;

public class DbRepresentation
{
    private final Map<Long,NodeRep> nodes = new TreeMap<>();
    private final Set<IndexDefinition> schemaIndexes = new HashSet<>();
    private final Set<ConstraintDefinition> constraints = new HashSet<>();
    private long highestNodeId;
    private long highestRelationshipId;

    public static DbRepresentation of( GraphDatabaseService db )
    {
        return of( db, true );
    }

    public static DbRepresentation of( GraphDatabaseService db, boolean includeIndexes )
    {
        int retryCount = 5;
        while ( true )
        {
            try ( Transaction ignore = db.beginTx() )
            {
                DbRepresentation result = new DbRepresentation();
                for ( Node node : db.getAllNodes() )
                {
                    NodeRep nodeRep = new NodeRep( db, node, includeIndexes );
                    result.nodes.put( node.getId(), nodeRep );
                    result.highestNodeId = Math.max( node.getId(), result.highestNodeId );
                    result.highestRelationshipId =
                            Math.max( nodeRep.highestRelationshipId, result.highestRelationshipId );

                }
                for ( IndexDefinition indexDefinition : db.schema().getIndexes() )
                {
                    result.schemaIndexes.add( indexDefinition );
                }
                for ( ConstraintDefinition constraintDefinition : db.schema().getConstraints() )
                {
                    result.constraints.add( constraintDefinition );
                }
                return result;
            }
            catch ( TransactionFailureException e )
            {
                if ( retryCount-- < 0 )
                {
                    throw e;
                }
            }
        }
    }

    public static DbRepresentation of( File storeDir )
    {
        return of( storeDir, true, Config.defaults() );
    }

    public static DbRepresentation of( File storeDir, Config config )
    {
        return of( storeDir, true, config );
    }

    public static DbRepresentation of( File storeDir, boolean includeIndexes, Config config )
    {
        DatabaseManagementServiceBuilder builder = new TestDatabaseManagementServiceBuilder( storeDir.getParentFile() );
        builder.setConfigRaw( config.getRaw() );

        DatabaseManagementService managementService = builder.build();
        GraphDatabaseService db = managementService.database( config.get( default_database ) );
        try
        {
            return of( db );
        }
        finally
        {
            managementService.shutdown();
        }
    }

    @Override
    public boolean equals( Object obj )
    {
        return compareWith( (DbRepresentation) obj ).isEmpty();
    }

    // Accessed from HA-robustness, needs to be public
    @SuppressWarnings( "WeakerAccess" )
    public Collection<String> compareWith( DbRepresentation other )
    {
        Collection<String> diffList = new ArrayList<>();
        DiffReport diff = new CollectionDiffReport( diffList );
        nodeDiff( other, diff );
        indexDiff( other, diff );
        constraintDiff( other, diff );
        return diffList;
    }

    private void constraintDiff( DbRepresentation other, DiffReport diff )
    {
        for ( ConstraintDefinition constraint : constraints )
        {
            if ( !other.constraints.contains( constraint ) )
            {
                diff.add( "I have constraint " + constraint + " which other doesn't" );
            }
        }
        for ( ConstraintDefinition otherConstraint : other.constraints )
        {
            if ( !constraints.contains( otherConstraint ) )
            {
                diff.add( "Other has constraint " + otherConstraint + " which I don't" );
            }
        }
    }

    private void indexDiff( DbRepresentation other, DiffReport diff )
    {
        for ( IndexDefinition schemaIndex : schemaIndexes )
        {
            if ( !other.schemaIndexes.contains( schemaIndex ) )
            {
                diff.add( "I have schema index " + schemaIndex + " which other doesn't" );
            }
        }
        for ( IndexDefinition otherSchemaIndex : other.schemaIndexes )
        {
            if ( !schemaIndexes.contains( otherSchemaIndex ) )
            {
                diff.add( "Other has schema index " + otherSchemaIndex + " which I don't" );
            }
        }
    }

    private void nodeDiff( DbRepresentation other, DiffReport diff )
    {
        for ( NodeRep node : nodes.values() )
        {
            NodeRep otherNode = other.nodes.get( node.id );
            if ( otherNode == null )
            {
                diff.add( "I have node " + node.id + " which other doesn't" );
                continue;
            }
            node.compareWith( otherNode, diff );
        }

        for ( Long id : other.nodes.keySet() )
        {
            if ( !nodes.containsKey( id ) )
            {
                diff.add( "Other has node " + id + " which I don't" );
            }
        }
    }

    @Override
    public int hashCode()
    {
        return nodes.hashCode();
    }

    @Override
    public String toString()
    {
        return nodes.toString();
    }

    private static class NodeRep
    {
        private final PropertiesRep properties;
        private final Map<Long,PropertiesRep> outRelationships = new HashMap<>();
        private final long highestRelationshipId;
        private final long id;

        NodeRep( GraphDatabaseService db, Node node, boolean includeIndexes )
        {
            id = node.getId();
            properties = new PropertiesRep( node, node.getId() );
            long highestRel = 0;
            for ( Relationship rel : node.getRelationships( Direction.OUTGOING ) )
            {
                outRelationships.put( rel.getId(), new PropertiesRep( rel, rel.getId() ) );
                highestRel = Math.max( highestRel, rel.getId() );
            }
            this.highestRelationshipId = highestRel;
        }

        void compareWith( NodeRep other, DiffReport diff )
        {
            if ( other.id != id )
            {
                diff.add( "Id differs mine:" + id + ", other:" + other.id );
            }
            properties.compareWith( other.properties, diff );
            compareRelationships( other, diff );
        }

        private void compareRelationships( NodeRep other, DiffReport diff )
        {
            for ( PropertiesRep rel : outRelationships.values() )
            {
                PropertiesRep otherRel = other.outRelationships.get( rel.entityId );
                if ( otherRel == null )
                {
                    diff.add( "I have relationship " + rel.entityId + " which other don't" );
                    continue;
                }
                rel.compareWith( otherRel, diff );
            }

            for ( Long id : other.outRelationships.keySet() )
            {
                if ( !outRelationships.containsKey( id ) )
                {
                    diff.add( "Other has relationship " + id + " which I don't" );
                }
            }
        }

        @Override
        public int hashCode()
        {
            int result = 7;
            result += properties.hashCode() * 7;
            result += outRelationships.hashCode() * 13;
            result += id * 17;
            return result;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            NodeRep nodeRep = (NodeRep) o;
            return id == nodeRep.id && Objects.equals( properties, nodeRep.properties ) &&
                   Objects.equals( outRelationships, nodeRep.outRelationships );
        }

        @Override
        public String toString()
        {
            return "<id: " + id + " props: " + properties + ", rels: " + outRelationships + ">";
        }
    }

    private static class PropertiesRep
    {
        private final Map<String,Serializable> props = new HashMap<>();
        private final String entityToString;
        private final long entityId;

        PropertiesRep( PropertyContainer entity, long id )
        {
            this.entityId = id;
            this.entityToString = entity.toString();
            for ( String key : entity.getPropertyKeys() )
            {
                Serializable value = (Serializable) entity.getProperty( key, null );
                // We do this because the node may have changed since we did getPropertyKeys()
                if ( value != null )
                {
                    if ( value.getClass().isArray() )
                    {
                        props.put( key, new ArrayList<>( Arrays.asList( IoPrimitiveUtils.asArray( value ) ) ) );
                    }
                    else
                    {
                        props.put( key, value );
                    }
                }
            }
        }

        protected void compareWith( PropertiesRep other, DiffReport diff )
        {
            boolean equals = props.equals( other.props );
            if ( !equals )
            {
                diff.add( "Properties diff for " + entityToString + " mine:" + props + ", other:" + other.props );
            }
        }

        @Override
        public int hashCode()
        {
            return props.hashCode();
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }
            PropertiesRep that = (PropertiesRep) o;
            return Objects.equals( props, that.props );
        }

        @Override
        public String toString()
        {
            return props.toString();
        }
    }

    private interface DiffReport
    {
        void add( String report );
    }

    private static class CollectionDiffReport implements DiffReport
    {
        private final Collection<String> collection;

        CollectionDiffReport( Collection<String> collection )
        {
            this.collection = collection;
        }

        @Override
        public void add( String report )
        {
            collection.add( report );
        }
    }
}
