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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.internal.helpers.collection.Iterators.count;

import com.neo4j.harness.junit.extension.EnterpriseNeo4jExtension;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.doc.server.HTTP;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.junit.extension.Neo4jExtension;

class ExtensionTestingDocIT {
    @RegisterExtension
    static final Neo4jExtension neo4jExtension = EnterpriseNeo4jExtension.builder()
            .withUnmanagedExtension("/myExtension", MyUnmanagedExtension.class)
            .withFixture(graphDatabaseService ->
            {
                try (Transaction tx = graphDatabaseService.beginTx()) {
                    tx.createNode(Label.label("User"));
                    tx.commit();
                }
                return null;
            }).build();

    // tag::testEnterpriseExtension[]
    @javax.ws.rs.Path("/")
    public static class MyUnmanagedExtension {
        @GET
        public Response myEndpoint() {
            return Response.ok().build();
        }
    }

    @Test
    void testMyExtension(Neo4j neo4j) {
        // When
        HTTP.Response response = HTTP.GET(HTTP.GET(neo4j.httpURI().resolve("myExtension").toString()).location());

        // Then
        assertEquals(200, response.status());
    }

    @Test
    void testMyExtensionWithFunctionFixture(Neo4j neo4j) {
        final GraphDatabaseService graphDatabaseService = neo4j.defaultDatabaseService();
        try (Transaction transaction = graphDatabaseService.beginTx()) {
            // When
            Result result = transaction.execute("MATCH (n:User) return n");

            // Then
            assertEquals(1, count(result));
            transaction.commit();
        }
    }
    // end::testEnterpriseExtension[]
}
