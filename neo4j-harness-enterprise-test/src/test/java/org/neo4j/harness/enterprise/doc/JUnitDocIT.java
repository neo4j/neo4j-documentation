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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.internal.helpers.collection.Iterables.single;
import static org.neo4j.internal.helpers.collection.Iterators.count;

import com.neo4j.harness.junit.extension.EnterpriseNeo4jExtension;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.doc.server.HTTP;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.ConstraintType;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.junit.extension.Neo4jExtension;

class JUnitDocIT {
    // tag::useEnterpriseJUnitRule[]
    @RegisterExtension
    static final Neo4jExtension neo4jExtension = EnterpriseNeo4jExtension.builder()
            .withFixture("CREATE (admin:Admin)")
            .withFixture(graphDatabaseService ->
            {
                try (Transaction tx = graphDatabaseService.beginTx()) {
                    tx.createNode(Label.label("Admin"));
                    tx.commit();
                }
                return null;
            }).build();

    @Test
    void shouldWorkWithServer(Neo4j neo4j) {
        // Given
        URI serverURI = neo4j.httpURI();

        // When I access the server
        HTTP.Response response = HTTP.GET(serverURI.toString());

        // Then it should reply
        assertEquals(200, response.status());

        // and we have access to underlying GraphDatabaseService
        try (Transaction tx = neo4j.defaultDatabaseService().beginTx()) {
            assertEquals(2, count(tx.findNodes(Label.label("Admin"))));
            tx.commit();
        }
    }
    // end::useEnterpriseJUnitRule[]

    @Test
    void shouldUserEnterpriseFeatures(Neo4j neo4j) {
        // Given
        GraphDatabaseService db = neo4j.defaultDatabaseService();

        // When I create property existence constraint
        try (Transaction tx = db.beginTx()) {
            try (Result result = tx.execute("CREATE CONSTRAINT FOR (user:User) REQUIRE user.name IS NOT NULL")) {
                // nothing to-do
            }
            tx.commit();
        }

        // Then I can access created constraint
        try (Transaction tx = db.beginTx()) {
            ConstraintDefinition constraint = single(tx.schema().getConstraints());

            assertTrue(constraint.isConstraintType(ConstraintType.NODE_PROPERTY_EXISTENCE));
            assertEquals(Label.label("User"), constraint.getLabel());
            assertEquals("name", single(constraint.getPropertyKeys()));

            tx.commit();
        }
    }
}
