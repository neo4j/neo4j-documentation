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
package org.neo4j.doc.server.rest;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.doc.server.SharedWebContainerTestBase;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class AbstractRestFunctionalTestBase extends SharedWebContainerTestBase implements GraphHolder {
    @RegisterExtension
    public TestData<Map<String,Node>> data = TestData.producedThrough(GraphDescription.createGraphFor());

    @RegisterExtension
    public TestData<RESTDocsGenerator> gen = TestData.producedThrough(RESTDocsGenerator.PRODUCER);

    @BeforeEach
    public void setUp() {
        gen.setGraphDatabaseService(container().getDefaultDatabase());
        data.setGraphDatabaseService(container().getDefaultDatabase());
        gen.get().setSection(getDocumentationSectionName());
        gen.get().setGraph(graphdb());
    }

    @Override
    public GraphDatabaseService graphdb() {
        return SharedWebContainerTestBase.container().getDefaultDatabase();
    }

    protected static String databaseUri() {
        return container().getBaseUri() + "db/neo4j/";
    }

    protected String getDatabaseUri() {
        return container().getBaseUri() + "db/";
    }

    protected String txUri() {
        return databaseUri() + "tx";
    }

    protected static String txCommitUri() {
        return databaseUri() + "tx/commit";
    }

    protected String getDocumentationSectionName() {
        return "dev/rest-api";
    }
}
