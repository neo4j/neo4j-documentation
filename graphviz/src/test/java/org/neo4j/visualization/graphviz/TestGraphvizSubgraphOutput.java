/*
 * Copyright (c) 2002-2020 "Neo4j,"
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
import java.io.OutputStream;
import java.nio.file.Path;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.visualization.SubgraphMapper;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TestGraphvizSubgraphOutput
{
    enum type implements RelationshipType
    {
        KNOWS, WORKS_FOR
    }

    @Test
    public void testSimpleGraph() throws Exception
    {
        Path folder = Path.of( "target/example-db" + System.nanoTime() );
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( folder ).build();
        try
        {
            GraphDatabaseService neo = managementService.database( DEFAULT_DATABASE_NAME );
            try ( Transaction tx = neo.beginTx() )
            {
                final Node emil = tx.createNode();
                emil.setProperty( "name", "Emil Eifrém" );
                emil.setProperty( "country_of_residence", "USA" );
                final Node tobias = tx.createNode();
                tobias.setProperty( "name", "Tobias Ivarsson" );
                tobias.setProperty( "country_of_residence", "Sweden" );
                final Node johan = tx.createNode();
                johan.setProperty( "name", "Johan Svensson" );
                johan.setProperty( "country_of_residence", "Sweden" );

                final Relationship emilKNOWStobias = emil.createRelationshipTo( tobias, type.KNOWS );
                final Relationship johanKNOWSemil = johan.createRelationshipTo( emil, type.KNOWS );
                final Relationship tobiasKNOWSjohan = tobias.createRelationshipTo( johan, type.KNOWS );
                final Relationship tobiasWORKS_FORemil = tobias.createRelationshipTo( emil, type.WORKS_FOR );

                OutputStream out = new ByteArrayOutputStream();
                SubgraphMapper subgraphMapper = node ->
                {
                    if ( node.hasProperty( "country_of_residence" ) )
                    {
                        return (String) node.getProperty( "country_of_residence" );
                    }
                    return null;
                };
                GraphvizWriter writer = new GraphvizWriter();

                SubgraphMapper.SubgraphMappingWalker walker = new SubgraphMapper.SubgraphMappingWalker( subgraphMapper )
                {
                    @Override
                    protected Iterable<Node> nodes()
                    {
                        return asList( emil, tobias, johan );
                    }

                    @Override
                    protected Iterable<Relationship> relationships()
                    {
                        return asList( emilKNOWStobias, johanKNOWSemil, tobiasKNOWSjohan, tobiasWORKS_FORemil );
                    }
                };

                writer.emit( out, walker );
                tx.commit();
            }
        }
        finally
        {
            managementService.shutdown();
            deleteDirectory( folder.toFile() );
        }
    }
}
