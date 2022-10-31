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
package org.neo4j.doc.server.helpers;

import com.sun.jersey.api.client.Client;
import java.net.URI;
import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RestRequest;

public final class FunctionalTestHelper {
    private final TestWebContainer container;

    public static final Client CLIENT = Client.create();
    private RestRequest request;

    public FunctionalTestHelper(TestWebContainer container) {
        this.container = container;
        this.request = new RestRequest(container.getBaseUri().resolve("db/neo4j/"));
    }

    public JaxRsResponse get(String path) {
        return request.get(path);
    }

    public URI baseUri() {
        return container.getBaseUri();
    }
}
