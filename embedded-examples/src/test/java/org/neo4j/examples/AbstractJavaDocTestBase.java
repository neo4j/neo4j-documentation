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

import static org.neo4j.doc.test.GraphDatabaseServiceCleaner.cleanDatabaseContent;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.cypher.docgen.tooling.CypherPrettifier;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.harness.junit.extension.Neo4jExtension;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

@ExtendWith(Neo4jExtension.class)
public abstract class AbstractJavaDocTestBase {
    @RegisterExtension
    final TestData<JavaTestDocsGenerator> gen = TestData.producedThrough(JavaTestDocsGenerator.PRODUCER);
    @RegisterExtension
    final TestData<Map<String,Node>> data = TestData.producedThrough(GraphDescription.createGraphFor());

    protected String createCypherSnippet(String cypherQuery) {
        String snippet = CypherPrettifier.apply(cypherQuery);
        return AsciidocHelper.createAsciiDocSnippet("cypher", snippet);
    }

    @BeforeEach
    public void setUp(GraphDatabaseService graphDb) {
        cleanDatabaseContent(graphDb);
        gen.setGraphDatabaseService(graphDb);
        data.setGraphDatabaseService(graphDb);

        gen.get().setGraph(graphDb);
    }

    @AfterEach
    public void doc() {
        gen.get().document("target/docs/dev", "examples");
    }
}
