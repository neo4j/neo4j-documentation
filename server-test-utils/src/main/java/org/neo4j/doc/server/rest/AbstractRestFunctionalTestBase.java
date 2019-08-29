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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.SharedServerTestBase;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import static org.junit.Assert.assertEquals;

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

    private String createParameterString(Pair<String, String>[] params) {
        String paramString = "";
        for (Pair<String, String> param : params) {
            String delimiter = paramString.isEmpty() || paramString.endsWith("{") ? "" : ",";

            paramString += delimiter + "\"" + param.first() + "\":\"" + param.other() + "\"";
        }

        return paramString;
    }

    protected String createScript(String template) {
        for (String key : data.get().keySet()) {
            template = template.replace("%" + key + "%", idFor(key).toString());
        }
        return template;
    }

    protected String startGraph(String name) {
        return AsciidocHelper.createGraphVizWithNodeId("Starting Graph", graphdb(), name);
    }

    @Override
    public GraphDatabaseService graphdb() {
        return SharedServerTestBase.server().getDatabaseService().getDatabase();
    }

    protected static String getDataUri() {
        return "http://localhost:7474/db/data/";
    }

    protected String getDatabaseUri() {
        return "http://localhost:7474/db/";
    }

    protected String txUri() {
        return getDataUri() + "transaction";
    }

    protected static String txCommitUri() {
        return getDataUri() + "transaction/commit";
    }

    protected String txUri(long txId) {
        return getDataUri() + "transaction/" + txId;
    }

    public static long extractTxId(HTTP.Response response) {
        int lastSlash = response.location().lastIndexOf("/");
        String txIdString = response.location().substring(lastSlash + 1);
        return Long.parseLong(txIdString);
    }

    protected Node getNode(String name) {
        return data.get().get(name);
    }

    protected Node[] getNodes(String... names) {
        Node[] nodes = {};
        ArrayList<Node> result = new ArrayList<>();
        for (String name : names) {
            result.add(getNode(name));
        }
        return result.toArray(nodes);
    }

    public void assertSize(int expectedSize, String entity) {
        Collection<?> hits;
        try {
            hits = (Collection<?>) JsonHelper.readJson(entity);
            assertEquals(expectedSize, hits.size());
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        }
    }

    public RESTDocsGenerator gen() {
        return gen.get();
    }

    protected String getDocumentationSectionName() {
        return "dev/rest-api";
    }

}
