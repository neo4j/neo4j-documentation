/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.server.rest.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.Base64;
import org.junit.After;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.doc.server.ExclusiveServerTestBase;
import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.helpers.CommunityServerBuilder;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.string.UTF8;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.assertThat;
import static org.neo4j.doc.server.HTTP.RawPayload.rawPayload;

public class CommunityServerTestBase extends ExclusiveServerTestBase
{
    protected CommunityNeoServer server;

    @After
    public void cleanup()
    {
        if(server != null) {server.stop();}
    }

    protected void startServer( boolean authEnabled ) throws IOException
    {
        server = CommunityServerBuilder.server()
                .withProperty( GraphDatabaseSettings.auth_enabled.name(), Boolean.toString( authEnabled ) )
                .build();
        server.start();
    }

    protected String challengeResponse( String username, String password )
    {
        return "Basic " + base64( username + ":" + password );
    }

    protected String databaseURL()
    {
        return server.baseUri().resolve( "db/neo4j/" ).toString();
    }

    protected String base64(String value)
    {
        return UTF8.decode( Base64.encode( value ) );
    }

    protected String txCommitURL()
    {
        return databaseURL() + "tx/commit";
    }

    private void assertPermissionError( HTTP.Response response, List<String> errors ) throws JsonParseException
    {
        assertThat( response.status(), equalTo( 200 ) );
        assertThat( response.get( "errors" ).size(), equalTo( 1 ) );

        JsonNode firstError = response.get( "errors" ).get( 0 );
        assertThat( firstError.get( "code" ).asText(), isIn( errors ) );

        assertThat( firstError.get( "message" ).asText(), startsWith( "Permission denied." ) );
    }

    protected String txCommitURL( String database )
    {
        return server.baseUri().resolve( txCommitEndpoint( database ) ).toString();
    }

    void assertPermissionErrorAtSystemAccess( HTTP.Response response ) throws JsonParseException
    {
        List<String> possibleErrors = Arrays.asList( "Neo.ClientError.Security.CredentialsExpired", "Neo.ClientError.Security.Forbidden" );
        assertPermissionError( response, possibleErrors );
    }

    protected static HTTP.RawPayload query( String statement )
    {
        return rawPayload( "{\"statements\":[{\"statement\":\"" + statement + "\"}]}" );
    }

    protected String simpleCypherRequestBody()
    {
        return "{\"statements\": [{\"statement\": \"CREATE (n:MyLabel) RETURN n\"}]}";
    }
}
