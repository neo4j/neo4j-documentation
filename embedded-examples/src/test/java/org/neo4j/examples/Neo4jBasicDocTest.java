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
package org.neo4j.examples;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.doc.test.TestGraphDatabaseFactory;
import org.neo4j.doc.test.rule.TestDirectory;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * An example of unit testing with Neo4j.
 */
public class Neo4jBasicDocTest
{
    @Rule
    public TestDirectory testDirectory = TestDirectory.testDirectory();
    protected GraphDatabaseService graphDb;

    /**
     * Create temporary database for each unit test.
     */
    // tag::beforeTest[]
    @Before
    public void prepareTestDatabase()
    {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase( testDirectory.directory() );
    }
    // end::beforeTest[]

    /**
     * Shutdown the database.
     */
    // tag::afterTest[]
    @After
    public void destroyTestDatabase()
    {
        graphDb.shutdown();
    }
    // end::afterTest[]

    @Test
    public void startWithConfiguration()
    {
        // tag::startDbWithConfig[]
        GraphDatabaseService db = new TestGraphDatabaseFactory()
            .newImpermanentDatabaseBuilder()
            .setConfig( GraphDatabaseSettings.pagecache_memory, "512M" )
            .setConfig( GraphDatabaseSettings.string_block_size, "60" )
            .setConfig( GraphDatabaseSettings.array_block_size, "300" )
            .newGraphDatabase();
        // end::startDbWithConfig[]
        db.shutdown();
    }

    @Test
    public void shouldCreateNode()
    {
        // tag::unitTest[]
        Node n;
        try ( Transaction tx = graphDb.beginTx() )
        {
            n = graphDb.createNode();
            n.setProperty( "name", "Nancy" );
            tx.success();
        }

        // The node should have a valid id
        assertThat( n.getId(), is( greaterThan( -1L ) ) );

        // Retrieve a node by using the id of the created node. The id's and
        // property should match.
        try ( Transaction tx = graphDb.beginTx() )
        {
            Node foundNode = graphDb.getNodeById( n.getId() );
            assertThat( foundNode.getId(), is( n.getId() ) );
            assertThat( (String) foundNode.getProperty( "name" ), is( "Nancy" ) );
        }
        // end::unitTest[]
    }
}
