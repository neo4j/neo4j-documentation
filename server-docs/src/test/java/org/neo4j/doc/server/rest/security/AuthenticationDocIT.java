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

import org.codehaus.jackson.JsonNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import javax.ws.rs.core.HttpHeaders;

import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.doc.server.rest.RESTDocsGenerator;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.HTTP.RawPayload;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AuthenticationDocIT extends CommunityServerTestBase
{
    @Rule
    public TestData<RESTDocsGenerator> gen = TestData.producedThrough( RESTDocsGenerator.PRODUCER );

    @Before
    public void setUp()
    {
        gen.get().setSection( "http-api/authentication" );
    }

    @Test
    @Documented( "Missing authorization\n" +
                 "\n" +
                 "If an +Authorization+ header is not supplied, the server will reply with an error." )
    public void missing_authorization() throws JsonParseException, IOException
    {
        // Given
        startServerWithConfiguredUser();

        // Document
        RESTDocsGenerator.ResponseEntity response = gen.get()
                .noGraph()
                .expectedStatus( 401 )
                .expectedHeader( "WWW-Authenticate", "Basic realm=\"Neo4j\"" )
                .get( dataURL() );

        // Then
        JsonNode data = JsonHelper.jsonNode( response.entity() );
        JsonNode firstError = data.get( "errors" ).get( 0 );
        assertThat( firstError.get( "code" ).asText(), equalTo( "Neo.ClientError.Security.Unauthorized" ) );
        assertThat( firstError.get( "message" ).asText(), equalTo( "No authentication header supplied." ) );
    }

    @Test
    @Documented( "Authenticate to access the server\n" +
                 "\n" +
                 "Authenticate by sending a username and a password to Neo4j using HTTP Basic Auth.\n" +
                 "Requests should include an +Authorization+ header, with a value of +Basic <payload>+,\n" +
                 "where \"payload\" is a base64 encoded string of \"username:password\"." )
    public void successful_authentication() throws JsonParseException, IOException
    {
        // Given
        startServerWithConfiguredUser();

        // Document
        RESTDocsGenerator.ResponseEntity response = gen.get()
                .noGraph()
                .expectedStatus( 200 )
                .withHeader( HttpHeaders.AUTHORIZATION, challengeResponse( "neo4j", "secret" ) )
                .get( userURL( "neo4j" ) );

        // Then
        JsonNode data = JsonHelper.jsonNode( response.entity() );
        assertThat( data.get( "username" ).asText(), equalTo( "neo4j" ) );
        assertThat( data.get( "password_change_required" ).asBoolean(), equalTo( false ) );
        assertThat( data.get( "password_change" ).asText(), equalTo( passwordURL( "neo4j" ) ) );
    }

    @Test
    @Documented( "Incorrect authentication\n" +
                 "\n" +
                 "If an incorrect username or password is provided, the server replies with an error." )
    public void incorrect_authentication() throws JsonParseException, IOException
    {
        // Given
        startServerWithConfiguredUser();

        // Document
        RESTDocsGenerator.ResponseEntity response = gen.get()
                .noGraph()
                .expectedStatus( 401 )
                .withHeader( HttpHeaders.AUTHORIZATION, challengeResponse( "neo4j", "incorrect" ) )
                .expectedHeader( "WWW-Authenticate", "Basic realm=\"Neo4j\"" )
                .post( dataURL() );

        // Then
        JsonNode data = JsonHelper.jsonNode( response.entity() );
        JsonNode firstError = data.get( "errors" ).get( 0 );
        assertThat( firstError.get( "code" ).asText(), equalTo( "Neo.ClientError.Security.Unauthorized" ) );
        assertThat( firstError.get( "message" ).asText(), equalTo( "Invalid username or password." ) );
    }

    @Test
    @Documented( "Required password changes\n" +
                 "\n" +
                 "In some cases, like the very first time Neo4j is accessed, the user will be required to choose\n" +
                 "a new password. The database will signal that a new password is required and deny access.\n" +
                 "\n" +
                 "See <<rest-api-security-user-status-and-password-changing>> for how to set a new password." )
    public void password_change_required() throws JsonParseException, IOException
    {
        // Given
        startServer( true );

        // Document
        RESTDocsGenerator.ResponseEntity response = gen.get()
                .noGraph()
                .expectedStatus( 403 )
                .withHeader( HttpHeaders.AUTHORIZATION, challengeResponse( "neo4j", "neo4j" ) )
                .get( dataURL() );

        // Then
        JsonNode data = JsonHelper.jsonNode( response.entity() );
        JsonNode firstError = data.get( "errors" ).get( 0 );
        assertThat( firstError.get( "code" ).asText(), equalTo( "Neo.ClientError.Security.Forbidden" ) );
        assertThat( firstError.get( "message" ).asText(), equalTo( "User is required to change their password." ) );
        assertThat( data.get( "password_change" ).asText(), equalTo( passwordURL( "neo4j" ) ) );
    }

    @Test
    @Documented( "When auth is disabled\n" +
                 "\n" +
                 "When auth has been disabled in the configuration, requests can be sent without an +Authorization+ header." )
    public void auth_disabled() throws IOException
    {
        // Given
        startServer( false );

        // Document
        gen.get()
            .noGraph()
            .expectedStatus( 200 )
            .get( dataURL() );
    }

    @Test
    public void shouldSayMalformedHeaderIfMalformedAuthorization() throws Exception
    {
        // Given
        startServerWithConfiguredUser();

        // When
        HTTP.Response response = HTTP.withHeaders( HttpHeaders.AUTHORIZATION, "This makes no sense" ).GET( dataURL() );

        // Then
        assertThat( response.status(), equalTo( 400 ) );
        assertThat( response.get( "errors" ).get( 0 ).get( "code" ).asText(), equalTo( "Neo.ClientError.Request.InvalidFormat" ) );
        assertThat( response.get( "errors" ).get( 0 ).get( "message" ).asText(), equalTo( "Invalid authentication header." ) );
    }

    public void startServerWithConfiguredUser() throws IOException
    {
        startServer( true );
        // Set the password
        HTTP.Response post = HTTP.withHeaders( HttpHeaders.AUTHORIZATION, challengeResponse( "neo4j", "neo4j" ) ).POST(
                server.baseUri().resolve( "/user/neo4j/password" ).toString(),
                RawPayload.quotedJson( "{'password':'secret'}" )
        );
        assertEquals( 200, post.status() );
    }
}
