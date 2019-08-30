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
import com.sun.jersey.core.util.Base64;

import java.net.URI;

import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RestRequest;
import org.neo4j.doc.server.rest.domain.GraphDbHelper;
import org.neo4j.server.NeoServer;
import org.neo4j.string.UTF8;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public final class FunctionalTestHelper
{
    private final NeoServer server;
    private final GraphDbHelper helper;

    public static final Client CLIENT = Client.create();
    private RestRequest request;

    public FunctionalTestHelper( NeoServer server )
    {
        if ( server.getDatabaseService() == null )
        {
            throw new RuntimeException( "Server must be started before using " + getClass().getName() );
        }
        this.helper = new GraphDbHelper( server.getDatabaseService() );
        this.server = server;
        this.request = new RestRequest(server.baseUri().resolve("db/neo4j/"));
    }

    public GraphDbHelper getGraphDbHelper()
    {
        return helper;
    }

    public String databaseUri()
    {
        return server.baseUri().toString() + "db/neo4j/";
    }

    public JaxRsResponse get(String path) {
        return request.get(path);
    }

    public URI baseUri()
    {
        return server.baseUri();
    }

    public String cypherURL()
    {
        return databaseUri() + "tx/commit";
    }

    public String simpleCypherRequestBody()
    {
        return "{\"statements\": [{\"statement\": \"CREATE (n:MyLabel) RETURN n\"}]}";
    }

    public void verifyCypherResponse( String responseBody )
    {
        // if at least one node is returned, there will be "node" in the metadata part od the the row
        assertThat( responseBody, containsString( "node" ) );
    }

    public String userURL( String username )
    {
        return baseUri().resolve( "user/" + username ).toString();
    }

    public String passwordURL( String username )
    {
        return baseUri().resolve( "user/" + username + "/password" ).toString();
    }

    public String base64( String value )
    {
        return UTF8.decode( Base64.encode( value ) );
    }

    public String quotedJson( String singleQuoted )
    {
        return singleQuoted.replaceAll( "'", "\"" );
    }
}
