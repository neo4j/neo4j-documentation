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

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.StringKeyObjectValueIgnoreCaseMultivaluedMap;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class JaxRsResponse extends Response {

    private final int status;
    private final MultivaluedMap<String,Object> metaData;
    private final MultivaluedMap<String,Object> headers;
    private final URI location;
    private String data;
    private MediaType type;

    public JaxRsResponse(ClientResponse response) {
        this(response, extractContent(response));
    }

    private static String extractContent(ClientResponse response) {
        if (response.getStatus() == Status.NO_CONTENT.getStatusCode()) {
            return null;
        }
        return response.getEntity(String.class);
    }

    public JaxRsResponse(ClientResponse response, String entity) {
        status = response.getStatus();
        metaData = extractMetaData(response);
        headers = extractHeaders(response);
        location = response.getLocation();
        type = response.getType();
        data = entity;
        response.close();
    }

    @Override
    public String getEntity() {
        return data;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return null;
    }

    @Override
    public Map<String,NewCookie> getCookies() {
        return null;
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public StatusType getStatusInfo() {
        return null;
    }

    @Override
    public MultivaluedMap<String,Object> getMetadata() {
        return metaData;
    }

    private MultivaluedMap<String,Object> extractMetaData(ClientResponse jettyResponse) {
        MultivaluedMap<String,Object> metadata = new StringKeyObjectValueIgnoreCaseMultivaluedMap();
        for (Map.Entry<String,List<String>> header : jettyResponse.getHeaders()
                .entrySet()) {
            for (Object value : header.getValue()) {
                metadata.putSingle(header.getKey(), value);
            }
        }
        return metadata;
    }

    @Override
    public MultivaluedMap<String,Object> getHeaders() {
        return headers;
    }

    @Override
    public MultivaluedMap<String,String> getStringHeaders() {
        return null;
    }

    @Override
    public String getHeaderString(String name) {
        return null;
    }

    private MultivaluedMap<String,Object> extractHeaders(ClientResponse jettyResponse) {
        MultivaluedHashMap<String,Object> result = new MultivaluedHashMap<>();

        MultivaluedMap<String,String> headers = jettyResponse.getHeaders();
        for (Map.Entry<String,List<String>> entry : headers.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return result;
    }

    // new URI( getHeaders().get( HttpHeaders.LOCATION ).get(0));
    public URI getLocation() {
        return location;
    }

    @Override
    public Set<Link> getLinks() {
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    public static JaxRsResponse extractFrom(ClientResponse clientResponse) {
        return new JaxRsResponse(clientResponse);
    }

    public MediaType getType() {
        return type;
    }
}
