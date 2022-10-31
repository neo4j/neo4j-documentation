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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.neo4j.doc.server.HTTP;

public class RestRequest {

    private final URI baseUri;
    private static final Client DEFAULT_CLIENT = Client.create();
    private final Client client;
    private final Map<String,String> headers = new HashMap<>();
    private MediaType accept = MediaType.APPLICATION_JSON_TYPE;

    public RestRequest(URI baseUri) {
        this(baseUri, null, null);
    }

    public RestRequest(URI baseUri, String username, String password) {
        this.baseUri = uriWithoutSlash(baseUri);
        if (username != null) {
            client = Client.create();
            client.addFilter(new HTTPBasicAuthFilter(username, password));
        }
        else {
            client = DEFAULT_CLIENT;
        }
    }

    public RestRequest(URI uri, Client client) {
        this.baseUri = uriWithoutSlash(uri);
        this.client = client;
    }

    public RestRequest() {
        this(null);
    }

    private URI uriWithoutSlash(URI uri) {
        if (uri == null) {
            return null;
        }
        String uriString = uri.toString();
        return uriString.endsWith("/") ? uri(uriString.substring(0, uriString.length() - 1)) : uri;
    }

    private Builder builder(String path) {
        return builder(path, accept);
    }

    private Builder builder(String path, final MediaType accept) {
        WebResource resource = client.resource(uri(pathOrAbsolute(path)));
        Builder builder = resource.accept(accept);
        if (!headers.isEmpty()) {
            for (Map.Entry<String,String> header : headers.entrySet()) {
                builder = builder.header(header.getKey(), header.getValue());
            }
        }
        return builder;
    }

    private String pathOrAbsolute(String path) {
        if (path.startsWith("http://")) {
            return path;
        }
        return baseUri + "/" + path;
    }

    public JaxRsResponse get(String path) {
        return JaxRsResponse.extractFrom(HTTP.sanityCheck(builder(path).get(ClientResponse.class)));
    }

    public JaxRsResponse delete(String path) {
        return JaxRsResponse.extractFrom(HTTP.sanityCheck(builder(path).delete(ClientResponse.class)));
    }

    public JaxRsResponse post(String path, String data) {
        return post(path, data, MediaType.APPLICATION_JSON_TYPE);
    }

    public JaxRsResponse post(String path, String data, final MediaType mediaType) {
        Builder builder = builder(path);
        if (data != null) {
            builder = builder.entity(data, mediaType);
        }
        else {
            builder = builder.type(mediaType);
        }
        return JaxRsResponse.extractFrom(HTTP.sanityCheck(builder.post(ClientResponse.class)));
    }

    public JaxRsResponse put(String path, String data) {
        Builder builder = builder(path);
        if (data != null) {
            builder = builder.entity(data, MediaType.APPLICATION_JSON_TYPE);
        }
        return new JaxRsResponse(HTTP.sanityCheck(builder.put(ClientResponse.class)));
    }

    private URI uri(String uri) {
        try {
            return new URI(uri);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public JaxRsResponse get() {
        return get("");
    }

    public JaxRsResponse get(String path, final MediaType acceptType) {
        Builder builder = builder(path, acceptType);
        return JaxRsResponse.extractFrom(HTTP.sanityCheck(builder.get(ClientResponse.class)));
    }

    public static RestRequest req() {
        return new RestRequest();
    }

    public JaxRsResponse delete(URI location) {
        return delete(location.toString());
    }

    public JaxRsResponse put(URI uri, String data) {
        return put(uri.toString(), data);
    }

    public RestRequest accept(MediaType accept) {
        this.accept = accept;
        return this;
    }

    public RestRequest header(String header, String value) {
        this.headers.put(header, value);
        return this;
    }
}
