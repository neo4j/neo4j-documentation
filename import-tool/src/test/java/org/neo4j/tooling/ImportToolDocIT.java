/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.tooling;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.neo4j.cli.ExecutionContext;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.importer.ImportCommand;
import org.neo4j.internal.helpers.collection.Iterables;

import static java.nio.file.Files.newOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.internal.helpers.collection.Iterators.asSet;
import static org.neo4j.io.fs.FileUtils.writeToFile;

class ImportToolDocIT
{
    private static final int NODE_COUNT = 6;
    private static final int RELATIONSHIP_COUNT = 9;
    private static final int SEQUEL_COUNT = 2;

    @TempDir
    Path testDirectory;

    @Test
    void basicCsvImport() throws Exception
    {
        // GIVEN
        Path movies = file( "ops", "movies.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
            out.println( "tt0133093,\"The Matrix\",1999,Movie" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003,Movie;Sequel" );
            out.println( "tt0242653,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }

        Path actors = file( "ops", "actors.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
        }

        Path roles = file( "ops", "roles.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID,role,:END_ID,:TYPE" );
            out.println( "keanu,\"Neo\",tt0133093,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0234215,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0242653,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0133093,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0234215,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0242653,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0133093,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0234215,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0242653,ACTED_IN" );
        }

        // WHEN
        String[] arguments =
                arguments( "--database", "the-database", "--nodes", movies.toAbsolutePath().toString(),
                        "--nodes", actors.toAbsolutePath().toString(), "--relationships", roles.toAbsolutePath().toString() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "example-command.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void separateHeadersCsvImport() throws Exception
    {
        // GIVEN
        Path moviesHeader = file( "ops", "movies3-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( moviesHeader ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
        }
        Path movies = file( "ops", "movies3.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "tt0133093,\"The Matrix\",1999,Movie" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003,Movie;Sequel" );
            out.println( "tt0242653,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }
        Path actorsHeader = file( "ops", "actors3-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actorsHeader ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
        }
        Path actors = file( "ops", "actors3.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
        }
        Path rolesHeader = file( "ops", "roles3-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( rolesHeader ) ) )
        {
            out.println( ":START_ID,role,:END_ID,:TYPE" );
        }
        Path roles = file( "ops", "roles3.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( "keanu,\"Neo\",tt0133093,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0234215,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0242653,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0133093,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0234215,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0242653,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0133093,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0234215,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0242653,ACTED_IN" );
        }

        // WHEN
        String[] arguments = arguments( "--database", "the-database", "--nodes",
                moviesHeader.toAbsolutePath() + "," + movies.toAbsolutePath(), "--nodes",
                actorsHeader.toAbsolutePath() + "," + actors.toAbsolutePath(), "--relationships",
                rolesHeader.toAbsolutePath() + "," + roles.toAbsolutePath() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "separate-header-example-command.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void multipleInputFiles() throws Exception
    {
        // GIVEN
        Path moviesHeader = file( "ops", "movies4-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( moviesHeader ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
        }
        Path moviesPart1 = file( "ops", "movies4-part1.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( moviesPart1 ) ) )
        {
            out.println( "tt0133093,\"The Matrix\",1999,Movie" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003,Movie;Sequel" );
        }
        Path moviesPart2 = file( "ops", "movies4-part2.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( moviesPart2 ) ) )
        {
            out.println( "tt0242653,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }
        Path actorsHeader = file( "ops", "actors4-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actorsHeader ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
        }
        Path actorsPart1 = file( "ops", "actors4-part1.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actorsPart1 ) ) )
        {
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
        }
        Path actorsPart2 = file( "ops", "actors4-part2.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actorsPart2 ) ) )
        {
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
        }
        Path rolesHeader = file( "ops", "roles4-header.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( rolesHeader ) ) )
        {
            out.println( ":START_ID,role,:END_ID,:TYPE" );
        }
        Path rolesPart1 = file( "ops", "roles4-part1.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( rolesPart1 ) ) )
        {
            out.println( "keanu,\"Neo\",tt0133093,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0234215,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0242653,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0133093,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0234215,ACTED_IN" );
        }
        Path rolesPart2 = file( "ops", "roles4-part2.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( rolesPart2 ) ) )
        {
            out.println( "laurence,\"Morpheus\",tt0242653,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0133093,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0234215,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0242653,ACTED_IN" );
        }

        // WHEN
        String[] arguments = arguments( "--database", "the-database", "--nodes",
                moviesHeader.toAbsolutePath() + "," + moviesPart1.toAbsolutePath() +
                        "," + moviesPart2.toAbsolutePath(), "--nodes",
                actorsHeader.toAbsolutePath() + "," + actorsPart1.toAbsolutePath() +
                        "," + actorsPart2.toAbsolutePath(), "--relationships",
                rolesHeader.toAbsolutePath() + "," + rolesPart1.toAbsolutePath() +
                        "," + rolesPart2.toAbsolutePath() );
        importTool( arguments );

        // DOCS
        String realDir = moviesPart2.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "multiple-input-files.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void sameNodeLabelEverywhere() throws Exception
    {
        // GIVEN
        Path movies = file( "ops", "movies5.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID,title,year:int" );
            out.println( "tt0133093,\"The Matrix\",1999" );
        }

        Path sequels = file( "ops", "sequels5.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( sequels ) ) )
        {
            out.println( "movieId:ID,title,year:int" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003" );
            out.println( "tt0242653,\"The Matrix Revolutions\",2003" );
        }

        Path actors = file( "ops", "actors5.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name" );
            out.println( "keanu,\"Keanu Reeves\"" );
            out.println( "laurence,\"Laurence Fishburne\"" );
            out.println( "carrieanne,\"Carrie-Anne Moss\"" );
        }

        Path roles = file( "ops", "roles5.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID,role,:END_ID,:TYPE" );
            out.println( "keanu,\"Neo\",tt0133093,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0234215,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0242653,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0133093,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0234215,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0242653,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0133093,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0234215,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0242653,ACTED_IN" );
        }
        // WHEN
        String[] arguments = arguments( "--database", "the-database",
                "--nodes=Movie=" + movies.toAbsolutePath(),
                "--nodes=Movie:Sequel=" + sequels.toAbsolutePath(),
                "--nodes=Actor=" + actors.toAbsolutePath(), "--relationships", roles.toAbsolutePath().toString() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "same-node-label-everywhere.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void sameRelationshipTypeEverywhere() throws Exception
    {
        // GIVEN
        Path movies = file( "ops", "movies6.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
            out.println( "tt0133093,\"The Matrix\",1999,Movie" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003,Movie;Sequel" );
            out.println( "tt0242653,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }

        Path actors = file( "ops", "actors6.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
        }

        Path roles = file( "ops", "roles6.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID,role,:END_ID" );
            out.println( "keanu,\"Neo\",tt0133093" );
            out.println( "keanu,\"Neo\",tt0234215" );
            out.println( "keanu,\"Neo\",tt0242653" );
            out.println( "laurence,\"Morpheus\",tt0133093" );
            out.println( "laurence,\"Morpheus\",tt0234215" );
            out.println( "laurence,\"Morpheus\",tt0242653" );
            out.println( "carrieanne,\"Trinity\",tt0133093" );
            out.println( "carrieanne,\"Trinity\",tt0234215" );
            out.println( "carrieanne,\"Trinity\",tt0242653" );
        }
        // WHEN
        String[] arguments =
                arguments( "--database", "the-database", "--nodes", movies.toAbsolutePath().toString(),
                        "--nodes", actors.toAbsolutePath().toString(), "--relationships=ACTED_IN=" + roles.toAbsolutePath() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "same-relationship-type-everywhere.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void idSpaces() throws Exception
    {
        // GIVEN
        Path movies = file( "ops", "movies8.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID(Movie),title,year:int,:LABEL" );
            out.println( "1,\"The Matrix\",1999,Movie" );
            out.println( "2,\"The Matrix Reloaded\",2003,Movie;Sequel" );
            out.println( "3,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }

        Path actors = file( "ops", "actors8.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID(Actor),name,:LABEL" );
            out.println( "1,\"Keanu Reeves\",Actor" );
            out.println( "2,\"Laurence Fishburne\",Actor" );
            out.println( "3,\"Carrie-Anne Moss\",Actor" );
        }

        Path roles = file( "ops", "roles8.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID(Actor),role,:END_ID(Movie)" );
            out.println( "1,\"Neo\",1" );
            out.println( "1,\"Neo\",2" );
            out.println( "1,\"Neo\",3" );
            out.println( "2,\"Morpheus\",1" );
            out.println( "2,\"Morpheus\",2" );
            out.println( "2,\"Morpheus\",3" );
            out.println( "3,\"Trinity\",1" );
            out.println( "3,\"Trinity\",2" );
            out.println( "3,\"Trinity\",3" );
        }
        // WHEN
        String[] arguments =
                arguments( "--database", "the-database", "--nodes", movies.toAbsolutePath().toString(),
                        "--nodes", actors.toAbsolutePath().toString(), "--relationships=ACTED_IN=" + roles.toAbsolutePath() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "id-spaces.adoc" );

        // THEN
        verifyData();
    }

    @Test
    void badRelationships() throws IOException
    {
        // GIVEN
        Path movies = file( "ops", "movies9.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
            out.println( "tt0133093,\"The Matrix\",1999,Movie" );
            out.println( "tt0234215,\"The Matrix Reloaded\",2003,Movie;Sequel" );
            out.println( "tt0242653,\"The Matrix Revolutions\",2003,Movie;Sequel" );
        }

        Path actors = file( "ops", "actors9.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
        }

        Path roles = file( "ops", "roles9.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID,role,:END_ID,:TYPE" );
            out.println( "keanu,\"Neo\",tt0133093,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0234215,ACTED_IN" );
            out.println( "keanu,\"Neo\",tt0242653,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0133093,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0234215,ACTED_IN" );
            out.println( "laurence,\"Morpheus\",tt0242653,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0133093,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0234215,ACTED_IN" );
            out.println( "carrieanne,\"Trinity\",tt0242653,ACTED_IN" );
            out.println( "emil,\"Emil\",tt0133093,ACTED_IN" );
        }

        // WHEN
        Path badFile = badFile();
        String[] arguments = arguments(
                "--database", "the-database",
                "--nodes", movies.toAbsolutePath().toString(),
                "--nodes", actors.toAbsolutePath().toString(),
                "--relationships", roles.toAbsolutePath().toString(),
                "--skip-bad-relationships" );
        importTool( arguments );
        assertTrue( Files.exists( badFile ) );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printFileWithPathsRemoved( badFile, realDir, "bad-relationships-default-not-imported.bad.adoc" );
        printCommandToFile( arguments, realDir, "bad-relationships-default.adoc" );

        // THEN
        verifyData();
    }

    /* This depends heavily on the internal implementation of BadCollector. */
    private PrintStream getPrintStream( final FileOutputStream fileOutputStream )
    {
        return new PrintStream( System.err ) {
            @Override
            public void println( String s )
            {
            }

            @Override
            public void write( byte[] bytes, int i, int i1 )
            {
                try
                {
                    fileOutputStream.write( bytes, i, i1 );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e );
                }
            }

            @Override
            public void write(byte[] b) throws IOException
            {
                fileOutputStream.write(b);
            }
        };
    }

    @Test
    void badDuplicateNodes() throws IOException
    {
        // GIVEN
        Path actors = file( "ops", "actors10.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
            out.println( "keanu,\"Keanu Reeves\",Actor" );
            out.println( "laurence,\"Laurence Fishburne\",Actor" );
            out.println( "carrieanne,\"Carrie-Anne Moss\",Actor" );
            out.println( "laurence,\"Laurence Harvey\",Actor" );
        }

        // WHEN
        Path badFile = badFile();
        String[] arguments = arguments(
                "--database", "the-database",
                "--nodes", actors.toAbsolutePath().toString(),
                "--skip-duplicate-nodes" );
        importTool( arguments );
        assertTrue( Files.exists( badFile ) );

        // DOCS
        String realDir = actors.getParent().toAbsolutePath().toString();
        printFileWithPathsRemoved( badFile, realDir, "bad-duplicate-nodes-default-not-imported.bad.adoc" );
        printCommandToFile( arguments, realDir, "bad-duplicate-nodes-default.adoc" );

        // THEN
        DatabaseManagementService managementService = databaseService( "the-database" );
        GraphDatabaseService db = managementService.database( "the-database" );
        try ( Transaction tx = db.beginTx();
              ResourceIterator<Node> nodes = tx.findNodes( Label.label( "Actor" ) ) )
        {
            assertEquals( asSet( "Keanu Reeves", "Laurence Fishburne", "Carrie-Anne Moss" ), namesOf( nodes ) );
            tx.commit();
        }
        managementService.shutdown();
    }

    @Test
    void propertyTypes() throws IOException
    {
        // GIVEN
        Path movies = file( "ops", "movies7.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( movies ) ) )
        {
            out.println( "movieId:ID,title,year:int,:LABEL" );
            out.println( "tt0099892,\"Joe Versus the Volcano\",1990,Movie" );
        }

        Path actors = file( "ops", "actors7.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( actors ) ) )
        {
            out.println( "personId:ID,name,:LABEL" );
            out.println( "meg,\"Meg Ryan\",Actor" );
        }

        Path roles = file( "ops", "roles7.csv" );
        try ( PrintStream out = new PrintStream( newOutputStream( roles ) ) )
        {
            out.println( ":START_ID,roles:string[],:END_ID,:TYPE" );
            out.println( "meg,\"DeDe;Angelica Graynamore;Patricia Graynamore\",tt0099892,ACTED_IN" );
        }

        // WHEN
        String[] arguments =
                arguments( "--database", "the-database", "--nodes", movies.toAbsolutePath().toString(),
                        "--nodes", actors.toAbsolutePath().toString(), "--relationships", roles.toAbsolutePath().toString() );
        importTool( arguments );

        // DOCS
        String realDir = movies.getParent().toAbsolutePath().toString();
        printCommandToFile( arguments, realDir, "property-types.adoc" );

        // THEN
        DatabaseManagementService managementService = databaseService( "the-database" );
        GraphDatabaseService db = managementService.database( "the-database" );
        try ( Transaction tx = db.beginTx() )
        {
            long nodeCount = Iterables.count( tx.getAllNodes() ), relationshipCount = 0;
            assertEquals( 2, nodeCount );

            for ( Relationship relationship : tx.getAllRelationships() )
            {
                assertTrue( relationship.hasProperty( "roles" ) );

                String[] retrievedRoles = (String[]) relationship.getProperty( "roles" );
                assertEquals( 3, retrievedRoles.length );

                relationshipCount++;
            }
            assertEquals( 1, relationshipCount );

            tx.commit();
        }
        finally
        {
            managementService.shutdown();
        }
    }

    private Set<String> namesOf( Iterator<Node> nodes )
    {
        Set<String> names = new HashSet<>();
        while ( nodes.hasNext() )
        {
            names.add( (String) nodes.next().getProperty( "name" ) );
        }
        return names;
    }

    private void verifyData()
    {
        DatabaseManagementService managementService = databaseService( "the-database" );
        GraphDatabaseService db = managementService.database( "the-database" );
        try ( Transaction tx = db.beginTx() )
        {
            long nodeCount = Iterables.count( tx.getAllNodes() ), relationshipCount = 0, sequelCount = 0;
            assertEquals( NODE_COUNT, nodeCount );
            for ( Relationship relationship : tx.getAllRelationships() )
            {
                assertTrue( relationship.hasProperty( "role" ) );
                relationshipCount++;
            }
            assertEquals( RELATIONSHIP_COUNT, relationshipCount );
            ResourceIterator<Node> movieSequels = tx.findNodes( Label.label( "Sequel" ) );
            while ( movieSequels.hasNext() )
            {
                Node sequel = movieSequels.next();
                assertTrue( sequel.hasProperty( "title" ) );
                sequelCount++;
            }
            assertEquals( SEQUEL_COUNT, sequelCount );
            Number year = (Number) tx.findNode( Label.label( "Movie" ), "title", "The Matrix" ).getProperty( "year" );
            assertEquals( 1999, year.intValue() );
            tx.commit();
        }
        finally
        {
            managementService.shutdown();
        }
    }

    private DatabaseManagementService databaseService( String databaseName )
    {
        return new DatabaseManagementServiceBuilder( storeDirForDatabase() )
                .setConfig( GraphDatabaseSettings.default_database, databaseName )
                .setConfig( GraphDatabaseSettings.transaction_logs_root_path, transactionLogsDirectory().toPath().toAbsolutePath() )
                .build();
    }

    private Path storeDirForDatabase()
    {
        return testDirectory.toAbsolutePath();
    }

    private File transactionLogsDirectory()
    {
        return new File( new File( testDirectory.toAbsolutePath().toFile(), "data" ), "transactions" );
    }

    private void printCommandToFile( String[] arguments, String dir, String fileName ) throws IOException
    {
        List<String> cleanedArguments = new ArrayList<>();
        for ( String argument : arguments )
        {
            if ( argument.contains( " " ) || argument.contains( "," ) ||
                    Arrays.asList( new String[]{";", "|", "'"} ).contains( argument ) )
            {
                cleanedArguments.add( '"' + argument + '"' );
            }
            else
            {
                cleanedArguments.add( argument );
            }
        }
        String documentationArgs = StringUtils.join( cleanedArguments, " " );
        documentationArgs = documentationArgs.replace( dir + File.separator, "" )
                .replace( testDirectory.toAbsolutePath().toString(), "path_to_target_directory" );
        String docsCommand = "neo4j-admin import " + documentationArgs;
        try ( PrintStream out = new PrintStream( newOutputStream( file( "ops", fileName ) ) ) )
        {
            out.println( docsCommand );
        }
    }

    @Test
    void printOptionsForManpage() throws Exception
    {
        try ( PrintStream out = new PrintStream( newOutputStream( file( "man", "options.adoc" ) ) ) )
        {
            ExecutionContext ctx = new ExecutionContext( testDirectory.toAbsolutePath(), testDirectory.toAbsolutePath() );
            ImportCommand cmd = new ImportCommand( ctx );
            CommandLine.usage( cmd, out );
        }
    }

    @Test
    void printOptionsForManual() throws Exception
    {
        try ( PrintStream out = new PrintStream( newOutputStream( file( "ops", "options.adoc" ) ) ) )
        {
            ExecutionContext ctx = new ExecutionContext( testDirectory.toAbsolutePath(), testDirectory.toAbsolutePath() );
            ImportCommand cmd = new ImportCommand( ctx );
            CommandLine.usage( cmd, out );
        }
    }

    private void printFileWithPathsRemoved( Path badFile, String realDir, String destinationFileName )
            throws IOException
    {
        String contents = FileUtils.readFileToString( badFile.toFile(), StandardCharsets.UTF_8 );
        String cleanedContents = contents.replace( realDir + File.separator, "" );
        writeToFile( file( "ops", destinationFileName ), cleanedContents, false );
    }

    private Path file( String section, String name ) throws IOException
    {
        Path directory = Path.of( "target", "docs", section );
        Files.createDirectories( directory );
        return directory.resolve( name );
    }

    private String[] arguments( String... arguments )
    {
        return arguments;
    }

    private void importTool( String[] arguments )
    {
        ExecutionContext ctx = new ExecutionContext( testDirectory.toAbsolutePath(), testDirectory.toAbsolutePath() );
        ImportCommand importCommand = new ImportCommand( ctx );
        CommandLine.populateCommand( importCommand, arguments );
        importCommand.execute();
    }

    private Path badFile()
    {
        return Path.of( "import.report" );
    }
}
