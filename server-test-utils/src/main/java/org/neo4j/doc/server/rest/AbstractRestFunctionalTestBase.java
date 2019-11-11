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

import org.junit.Before;
import org.junit.Rule;

import java.util.Map;

import org.neo4j.doc.server.SharedServerTestBase;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class AbstractRestFunctionalTestBase extends SharedServerTestBase implements GraphHolder {

    @Rule
    public TestData<Map<String,Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this ) );

    @Rule
    public TestData<RESTDocsGenerator> gen = TestData.producedThrough(RESTDocsGenerator.PRODUCER);

    @Before
    public void setUp() {
        gen().setSection(getDocumentationSectionName());
        gen().setGraph(graphdb());
    }

    private Long idFor(String name) {
        return data.get().get(name).getId();
    }

    @Override
    public GraphDatabaseService graphdb() {
        return SharedServerTestBase.server().getDatabaseService().getDatabase();
    }

    protected static String databaseUri() {
        return "http://localhost:7474/db/neo4j/";
    }

    protected String getDatabaseUri() {
        return "http://localhost:7474/db/";
    }

    protected String txUri() {
        return databaseUri() + "tx";
    }

    protected static String txCommitUri() {
        return databaseUri() + "tx/commit";
    }

    public RESTDocsGenerator gen() {
        return gen.get();
    }

    protected String getDocumentationSectionName() {
        return "dev/rest-api";
    }

}
