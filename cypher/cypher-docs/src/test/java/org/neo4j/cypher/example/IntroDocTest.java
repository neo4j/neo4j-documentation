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
package org.neo4j.cypher.example;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.test.TestGraphDatabaseFactory;
import org.neo4j.doc.tools.AsciiDocGenerator;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.neo4j.test.GraphDatabaseServiceCleaner.cleanDatabaseContent;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createCypherSnippet;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createQueryResultSnippet;

public class IntroDocTest implements GraphHolder
{
    private static final String DOCS_TARGET = "target/docs/dev/general/";
    @Rule
    public TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );
    @Rule
    public TestData<Map<String,Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this, true ) );
    private static GraphDatabaseService graphdb;

    @Test
    public void intro_examples() throws Exception
    {
        try ( Transaction ignored = graphdb.beginTx() )
        {
            Writer fw = AsciiDocGenerator.getFW( DOCS_TARGET, gen.get().getTitle() );
            data.get();
            fw.append( "\nLet's create a simple example graph with the following query:\n\n" );
            String setupQuery = "CREATE (john:Person {name: 'John'}) " +
                                "CREATE (joe:Person {name: 'Joe'}) " +
                                "CREATE (steve:Person {name: 'Steve'}) " +
                                "CREATE (sara:Person {name: 'Sara'}) " +
                                "CREATE (maria:Person {name: 'Maria'}) " +
                                "CREATE (john)-[:FRIEND]->(joe)-[:FRIEND]->(steve) " +
                                "CREATE (john)-[:FRIEND]->(sara)-[:FRIEND]->(maria)";
            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.query",
                    createCypherSnippet( setupQuery ) ) );
            graphdb.execute( setupQuery ).close();

            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.graph",
                    AsciidocHelper.createGraphViz( "Example Graph",
                            graphdb(), "cypher-intro" ) ) );

            fw.append( "\nFor example, here is a query which finds a user called *'John'* and *'John's'* friends (though not " +
                    "his direct friends) before returning both *'John'* and any friends-of-friends that are found." );
            fw.append( "\n\n" );
            String query = "MATCH (john {name: 'John'})-[:FRIEND]->()-[:FRIEND]->(fof) RETURN john.name, fof.name ";
            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.query",
                    createCypherSnippet( query ) ) );
            fw.append( "\nResulting in:\n\n" );
            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.result",
                    createQueryResultSnippet( graphdb.execute( query ).resultAsString() ) ) );

            fw.append( "\nNext up we will add filtering to set more parts "
                    + "in motion:\n\nWe take a list of user names "
                    + "and find all nodes with names from this list, match their friends and return "
                    + "only those followed users who have a *'name'* property starting with *'S'*." );
            query = "MATCH (user)-[:FRIEND]->(follower) WHERE "
                    + "user.name IN ['Joe', 'John', 'Sara', 'Maria', 'Steve'] AND follower.name =~ 'S.*' "
                            + "RETURN user.name, follower.name ";
            fw.append( "\n\n" );
            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.query",
                    createCypherSnippet( query ) ) );
            fw.append( "\nResulting in:\n\n" );
            fw.append( AsciiDocGenerator.dumpToSeparateFileWithType( new File( DOCS_TARGET ), "intro.result",
                    createQueryResultSnippet( graphdb.execute( query ).resultAsString() ) ) );
            fw.close();
        }
    }

    @BeforeClass
    public static void setup() throws IOException
    {
        graphdb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        cleanDatabaseContent( graphdb );
    }

    @AfterClass
    public static void shutdown()
    {
        try
        {
            if ( graphdb != null ) graphdb.shutdown();
        }
        finally
        {
            graphdb = null;
        }
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }
}
