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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.doc.test.TestGraphDatabaseFactory;
import org.neo4j.doc.test.rule.TestDirectory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestJavaTestDocsGenerator implements GraphHolder
{
    private static GraphDatabaseService graphdb;
    @Rule
    public final TestData<Map<String,Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this, true ) );
    @Rule
    public final TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );
    @Rule
    public final TestDirectory testDirectory = TestDirectory.testDirectory();

    private final String sectionName = "testsection";
    private File directory;
    private File sectionDirectory;

    @Before
    public void setup()
    {
        directory = testDirectory.directory( "testdocs" );
        sectionDirectory = new File( directory, sectionName );
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
        doc.document( directory.getAbsolutePath(), sectionName );

        String result = readFileAsString( new File( sectionDirectory, "title1.asciidoc" ) );
        assertTrue( result.contains( "include::includes/title1-snippet1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title1-snippet_2-1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title1-snippet12.asciidoc[]" ) );

        File includes = new File( sectionDirectory, "includes" );
        result = readFileAsString( new File( includes,
                "title1-snippet1.asciidoc" ) );
        assertTrue( result.contains( snippet1 ) );
        result = readFileAsString( new File( includes,
                "title1-snippet_2-1.asciidoc" ) );
        assertTrue( result.contains( snippet2 ) );
        result = readFileAsString( new File( includes,
                "title1-snippet12.asciidoc" ) );
        assertTrue( result.contains( snippet12 ) );
    }

    @Documented( value = "@@snippet1\n" )
    @Test
    @Graph( "I know you" )
    public void will_not_complain_about_missing_snippets() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.document( directory.getAbsolutePath(), sectionName );
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
        doc.document( directory.getAbsolutePath(), sectionName );
        String result = readFileAsString( new File( sectionDirectory, "title2.asciidoc" ) );
        assertTrue( result.contains( "include::includes/title2-snippet1.asciidoc[]" ) );
        assertTrue( result.contains( "include::includes/title2-snippet2.asciidoc[]" ) );
        result = readFileAsString( new File( new File( sectionDirectory, "includes" ), "title2-snippet1.asciidoc" ) );
        assertTrue( result.contains( snippet1 ) );
        result = readFileAsString( new File( new File( sectionDirectory, "includes" ), "title2-snippet2.asciidoc" ) );
        assertTrue( result.contains( snippet2 ) );
    }

    public static String readFileAsString( File file ) throws java.io.IOException
    {
        byte[] buffer = new byte[(int) file.length()];
        try ( BufferedInputStream f = new BufferedInputStream( new FileInputStream( file ) ) )
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
        graphdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    @AfterClass
    public static void shutdown()
    {
        try
        {
            if ( graphdb != null )
            {
                graphdb.shutdown();
            }
        }
        finally
        {
            graphdb = null;
        }
    }
}
