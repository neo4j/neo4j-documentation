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
package org.neo4j.doc.metatest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.neo4j.annotations.documented.Documented;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TestJavaTestDocsGenerator implements GraphHolder
{
    private static GraphDatabaseService graphdb;
    @Rule
    public final TestData<Map<String,Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this ) );
    @Rule
    public final TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );

    private final String sectionName = "testsection";
    private Path directory;
    private Path sectionDirectory;
    private static Path databaseDirectory;
    private static DatabaseManagementService managementService;

    @Before
    public void setup()
    {
        directory = Path.of( "target/testdocs" + System.nanoTime() );
        sectionDirectory = directory.resolve( sectionName );
    }

    @Documented( value = "Title1.\n\nhej\n@@snippet1\n\nmore docs\n@@snippet_2-1\n@@snippet12\n." )
    @Test
    @Graph( "I know you" )
    public void can_create_docs_from_method_name() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.setGraph( graphdb );
        assertNotNull( data.get().get( "I" ) );
        String snippet1 = "snippet1-value";
        String snippet12 = "snippet12-value";
        String snippet2 = "snippet2-value";
        doc.addSnippet( "snippet1", snippet1 );
        doc.addSnippet( "snippet12", snippet12 );
        doc.addSnippet( "snippet_2-1", snippet2 );
        doc.document( directory.toAbsolutePath().toString(), sectionName );

        String result = readFileAsString( sectionDirectory.resolve( "title1.asciidoc" ) );
        assertTrue( result.contains( "include::includes/title1-snippet1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title1-snippet_2-1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title1-snippet12.asciidoc[]" ) );

        Path includes = sectionDirectory.resolve( "includes" );
        result = readFileAsString( includes.resolve( "title1-snippet1.asciidoc" ) );
        assertTrue( result.contains( snippet1 ) );
        result = readFileAsString( includes.resolve( "title1-snippet_2-1.asciidoc" ) );
        assertTrue( result.contains( snippet2 ) );
        result = readFileAsString( includes.resolve( "title1-snippet12.asciidoc" ) );
        assertTrue( result.contains( snippet12 ) );
    }

    @Documented( value = "@@snippet1\n" )
    @Test
    @Graph( "I know you" )
    public void will_not_complain_about_missing_snippets() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.document( directory.toAbsolutePath().toString(), sectionName );
    }

    @Documented( "Title2.\n" +
                 "\n" +
                 "@@snippet1\n" +
                 "\n" +
                 "           more stuff\n" +
                 "\n" +
                 "\n" +
                 "@@snippet2" )
    @Test
    @Graph( "I know you" )
    public void canCreateDocsFromSnippetsInAnnotations() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.setGraph( graphdb );
        assertNotNull( data.get().get( "I" ) );
        String snippet1 = "snippet1-value";
        String snippet2 = "snippet2-value";
        doc.addSnippet( "snippet1", snippet1 );
        doc.addSnippet( "snippet2", snippet2 );
        doc.document( directory.toAbsolutePath().toString(), sectionName );
        String result = readFileAsString( sectionDirectory.resolve( "title2.asciidoc" ) );
        assertTrue( result.contains( "include::includes/title2-snippet1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title2-snippet2.asciidoc[]" ) );
        result = readFileAsString( sectionDirectory.resolve( "includes" ).resolve( "title2-snippet1.asciidoc" ) );
        assertTrue( result.contains( snippet1 ) );
        result = readFileAsString( sectionDirectory.resolve( "includes" ).resolve( "title2-snippet2.asciidoc" ) );
        assertTrue( result.contains( snippet2 ) );
    }

    public static String readFileAsString( Path file ) throws java.io.IOException
    {
        byte[] buffer = new byte[(int) Files.size( file )];
        try ( BufferedInputStream f = new BufferedInputStream( Files.newInputStream( file ) ) )
        {
            f.read( buffer );
            return new String( buffer );
        }
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }

    @BeforeClass
    public static void setUp()
    {
        databaseDirectory = Path.of( "target/example-db" + System.nanoTime() );
        managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        graphdb = managementService.database( DEFAULT_DATABASE_NAME );
    }

    @After
    public void tearDown() throws Exception
    {
        deleteDirectory( directory.toFile() );
    }

    @AfterClass
    public static void shutdown() throws IOException
    {
        try
        {
            if ( managementService != null )
            {
                managementService.shutdown();
                deleteDirectory( databaseDirectory.toFile() );
            }
        }
        finally
        {
            graphdb = null;
        }
    }
}
