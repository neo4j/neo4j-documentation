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
package org.neo4j.doc.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.neo4j.internal.helpers.collection.Iterables;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;

import static java.util.Collections.unmodifiableMap;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.neo4j.internal.helpers.collection.MapUtil.stringMap;

/**
 * A tool for performing REST HTTP requests
 */
public class HTTP
{

    private static final Builder BUILDER = new Builder().withHeaders( "Accept", "application/json" );
    private static final Client CLIENT;
    static {
        DefaultClientConfig defaultClientConfig = new DefaultClientConfig();
        defaultClientConfig.getProperties().put( ClientConfig.PROPERTY_FOLLOW_REDIRECTS, Boolean.FALSE );
        CLIENT = Client.create( defaultClientConfig );
    }

    private HTTP()
    {
    }

    public static Builder withHeaders( String... kvPairs )
    {
        return BUILDER.withHeaders( kvPairs );
    }

    public static Builder withBaseUri( URI baseUri )
    {
        return BUILDER.withBaseUri( baseUri.toString() );
    }

    public static Response POST( String uri )
    {
        return BUILDER.POST( uri );
    }

    public static Response POST( String uri, Object payload )
    {
        return BUILDER.POST( uri, payload );
    }

    public static Response POST( String uri, RawPayload payload )
    {
        return BUILDER.POST( uri, payload );
    }

    public static Response GET( String uri )
    {
        return BUILDER.GET( uri );
    }

    public static Response request( String method, String uri, Object payload )
    {
        return BUILDER.request( method, uri, payload );
    }

    public static class Builder
    {
        private final Map<String, String> headers;
        private final String baseUri;

        private Builder()
        {
            this( Collections.emptyMap(), "" );
        }

        private Builder( Map<String, String> headers, String baseUri )
        {
            this.baseUri = baseUri;
            this.headers = unmodifiableMap( headers );
        }

        public Builder withHeaders( String... kvPairs )
        {
            return withHeaders( stringMap( kvPairs ) );
        }

        public Builder withHeaders( Map<String, String> newHeaders )
        {
            HashMap<String, String> combined = new HashMap<>();
            combined.putAll( headers );
            combined.putAll( newHeaders );
            return new Builder( combined, baseUri );
        }

        public Builder withBaseUri( String baseUri )
        {
            return new Builder( headers, baseUri );
        }

        public Response POST( String uri )
        {
            return request( "POST", uri );
        }

        public Response POST( String uri, Object payload )
        {
            return request( "POST", uri, payload );
        }

        public Response POST( String uri, RawPayload payload )
        {
            return request( "POST", uri, payload );
        }

        public Response DELETE( String uri )
        {
            return request( "DELETE", uri );
        }

        public Response GET( String uri )
        {
            return request( "GET", uri );
        }

        public Response request( String method, String uri )
        {
            return new Response( CLIENT.handle( build().build( buildUri( uri ), method ) ) );
        }

        public Response request( String method, String uri, Object payload )
        {
            if(payload == null)
            {
                return request(method, uri);
            }
            String jsonPayload = payload instanceof RawPayload ? ((RawPayload) payload).get() : JsonHelper.createJsonFrom(
                    payload );
            ClientRequest.Builder lastBuilder = build().entity( jsonPayload, MediaType.APPLICATION_JSON_TYPE );

            return new Response( CLIENT.handle( lastBuilder.build( buildUri( uri ), method ) ) );
        }

        private URI buildUri( String uri )
        {
            URI unprefixedUri = URI.create( uri );
            if ( unprefixedUri.isAbsolute() )
            {
                return unprefixedUri;
            }
            else
            {
                return URI.create( baseUri + uri );
            }
        }

        private ClientRequest.Builder build()
        {
            ClientRequest.Builder builder = ClientRequest.create();
            for ( Map.Entry<String, String> header : headers.entrySet() )
            {
                builder = builder.header( header.getKey(), header.getValue() );
            }

            return builder;
        }
    }

    /**
     * Check some general validations that all REST responses should always pass.
     */
    public static ClientResponse sanityCheck( ClientResponse response )
    {
        List<String> contentEncodings = response.getHeaders().get( "Content-Encoding" );
        String contentEncoding;
        if ( contentEncodings != null && (contentEncoding = Iterables.singleOrNull( contentEncodings )) != null )
        {
            // Specifically, this is never used for character encoding.
            contentEncoding = contentEncoding.toLowerCase();
            assertThat( contentEncoding, anyOf(
                    containsString( "gzip" ),
                    containsString( "deflate" ) ) );
            assertThat( contentEncoding, allOf(
                    not( containsString( "utf-8" ) ) ) );
        }
        return response;
    }

    public static class Response
    {
        private final ClientResponse response;
        private final String entity;

        public Response( ClientResponse response )
        {
            this.response = sanityCheck( response );
            if ( response.getStatus() == 204 )
            {
                entity = "";
            }
            else
            {
                this.entity = response.getEntity( String.class );
            }
        }

        public int status()
        {
            return response.getStatus();
        }

        public String location()
        {
            if ( response.getLocation() != null )
            {
                return response.getLocation().toString();
            }
            throw new RuntimeException( "The request did not contain a location header, " +
                    "unable to provide location. Status code was: " + status() );
        }

        @SuppressWarnings("unchecked")
        public <T> T content()
        {
            try
            {
                return (T) JsonHelper.readJson( entity );
            }
            catch ( JsonParseException e )
            {
                throw new RuntimeException( "Unable to deserialize: " + entity, e );
            }
        }

        public String rawContent()
        {
            return entity;
        }

        public JsonNode get(String fieldName) throws JsonParseException
        {
            return JsonHelper.jsonNode( entity ).get( fieldName );
        }

        public String header( String name )
        {
            return response.getHeaders().getFirst( name );
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "HTTP " ).append( response.getStatus() ).append( "\n" );
            for ( Map.Entry<String, List<String>> header : response.getHeaders().entrySet() )
            {
                for ( String headerEntry : header.getValue() )
                {
                    sb.append(header.getKey()).append(": " ).append(headerEntry).append( "\n" );
                }
            }
            sb.append( "\n" );
            sb.append( entity ).append( "\n" );

            return sb.toString();
        }
    }

    public static class RawPayload
    {
        private final String payload;

        public static RawPayload rawPayload( String payload )
        {
            return new RawPayload( payload );
        }

        public static RawPayload quotedJson( String json )
        {
            return new RawPayload( json.replaceAll( "'", "\"" ) );
        }

        private RawPayload( String payload )
        {
            this.payload = payload;
        }

        public String get()
        {
            return payload;
        }
    }
}
