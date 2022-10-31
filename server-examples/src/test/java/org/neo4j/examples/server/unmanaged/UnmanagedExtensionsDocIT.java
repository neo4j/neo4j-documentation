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
package org.neo4j.examples.server.unmanaged;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.doc.server.HTTP;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.junit.extension.Neo4jExtension;

class UnmanagedExtensionsDocIT {
    @RegisterExtension
    static Neo4jExtension neo4jExtension = Neo4jExtension.builder()
            .withFixture("UNWIND ['Keanu Reeves','Hugo Weaving','Carrie-Anne Moss','Laurence Fishburne'] AS actor " +
                    "MERGE (m:Movie  {name: 'The Matrix'}) " +
                    "MERGE (p:Person {name: actor}) " +
                    "MERGE (p)-[:ACTED_IN]->(m) ")
            .withUnmanagedExtension("/path/to/my/extension1", ColleaguesCypherExecutionResource.class)
            .withUnmanagedExtension("/path/to/my/extension2", ColleaguesResource.class)
            .build();

    @Test
    void shouldRetrieveColleaguesViaExecutionEngine(Neo4j neo4j) {
        // When
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve(
                "/path/to/my/extension1/colleagues-cypher-execution/Keanu%20Reeves").toString());

        // Then
        assertEquals(200, response.status());

        Map<String,Object> content = response.content();
        List<String> colleagues = (List<String>) content.get("colleagues");

        assertThat(colleagues).containsExactlyInAnyOrder("Laurence Fishburne", "Hugo Weaving", "Carrie-Anne Moss");
    }

    @Test
    void shouldRetrieveColleaguesViaTransactionAPI(Neo4j neo4j) {
        // When
        HTTP.Response response = HTTP.GET(neo4j.httpURI().resolve(
                "/path/to/my/extension2/colleagues/Keanu%20Reeves").toString());

        // Then
        assertEquals(200, response.status());

        Map<String,Object> content = response.content();
        List<String> colleagues = (List<String>) content.get("colleagues");

        assertThat(colleagues).containsExactlyInAnyOrder("Laurence Fishburne", "Hugo Weaving", "Carrie-Anne Moss");
    }
}
