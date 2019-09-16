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
package org.neo4j.harness.enterprise.doc;

import com.neo4j.harness.junit.rule.EnterpriseNeo4jRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.neo4j.doc.server.HTTP;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.Assert.assertEquals;
import static org.neo4j.internal.helpers.collection.Iterators.count;

public class ExtensionTestingDocIT
{
    @ClassRule
    public static TestDirectory testDirectory = TestDirectory.testDirectory();

    @Rule
    public Neo4jRule neo4j = new EnterpriseNeo4jRule()
            .withUnmanagedExtension( "/myExtension", MyUnmanagedExtension.class )
            .withFixture( graphDatabaseService ->
            {
                try ( Transaction tx = graphDatabaseService.beginTx() )
                {
                    tx.createNode( Label.label( "User" ) );
                    tx.commit();
                }
                return null;
            } );

    // tag::testEnterpriseExtension[]
    @javax.ws.rs.Path("/")
    public static class MyUnmanagedExtension
    {
        @GET
        public Response myEndpoint()
        {
            return Response.ok().build();
        }
    }

    @Test
    public void testMyExtension() throws Exception
    {
        // When
        HTTP.Response response = HTTP.GET( HTTP.GET( neo4j.httpURI().resolve( "myExtension" ).toString() ).location() );

        // Then
        assertEquals( 200, response.status() );
    }

    @Test
    public void testMyExtensionWithFunctionFixture() throws Exception
    {
        final GraphDatabaseService graphDatabaseService = neo4j.defaultDatabaseService();
        try ( Transaction transaction = graphDatabaseService.beginTx() )
        {
            // When
            Result result = transaction.execute( "MATCH (n:User) return n" );

            // Then
            assertEquals( 1, count( result ) );
            transaction.commit();
        }
    }
    // end::testEnterpriseExtension[]
}
