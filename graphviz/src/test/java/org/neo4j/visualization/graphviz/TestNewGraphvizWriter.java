/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.visualization.graphviz;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.walk.Walker;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TestNewGraphvizWriter
{
    enum type implements RelationshipType
    {
        KNOWS, WORKS_FOR
    }

    @Test
    public void testSimpleGraph() throws Exception
    {
        File folder = new File( "target/example-db" + System.nanoTime() );
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( folder ).build();
        try
        {
            GraphDatabaseService neo = managementService.database( DEFAULT_DATABASE_NAME );
            try ( Transaction tx = neo.beginTx() )
            {
                final Node emil = tx.createNode();
                emil.setProperty( "name", "Emil Eifrém" );
                emil.setProperty( "age", 30 );
                final Node tobias = tx.createNode();
                tobias.setProperty( "name", "Tobias \"thobe\" Ivarsson" );
                tobias.setProperty( "age", 23 );
                tobias.setProperty( "hours", new int[]{10, 10, 4, 4, 0} );
                final Node johan = tx.createNode();
                johan.setProperty( "!<>)", "!<>)" );
                johan.setProperty( "name", "!<>Johan '\\n00b' !<>Svensson" );
                final Relationship emilKNOWStobias = emil.createRelationshipTo( tobias, type.KNOWS );
                emilKNOWStobias.setProperty( "since", "2003-08-17" );
                final Relationship johanKNOWSemil = johan.createRelationshipTo( emil, type.KNOWS );
                final Relationship tobiasKNOWSjohan = tobias.createRelationshipTo( johan, type.KNOWS );
                final Relationship tobiasWORKS_FORemil = tobias.createRelationshipTo( emil, type.WORKS_FOR );
                OutputStream out = new ByteArrayOutputStream();
                GraphvizWriter writer = new GraphvizWriter();
                Iterable<Node> traverser =
                        tx.traversalDescription().depthFirst().relationships( type.KNOWS ).relationships( type.WORKS_FOR ).traverse( emil ).nodes();
                writer.emit( out, Walker.crosscut( traverser, type.KNOWS, type.WORKS_FOR ) );
                tx.commit();
                out.toString();
            }
        }
        finally
        {
            managementService.shutdown();
            deleteDirectory( folder );
        }
    }
}
