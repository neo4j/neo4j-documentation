/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import javax.ws.rs.core.HttpHeaders;

import org.neo4j.annotations.documented.Documented;
import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.HTTP.RawPayload;
import org.neo4j.doc.server.rest.RESTDocsGenerator;
import org.neo4j.doc.test.TestData;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.doc.server.HTTP.RawPayload.rawPayload;

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
                .payload( simpleCypherRequestBody() )
                .post( txCommitURL() );

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

        HTTP.Response response = HTTP.withBasicAuth( "neo4j", "secret" ).POST( txCommitURL( "system" ), query( "SHOW USERS" ) );

        assertThat( response.status(), equalTo( 200 ) );

        final JsonNode jsonNode = getResultRow( response );
        assertThat( jsonNode.get(0).asText(), equalTo( "neo4j" ) );
        assertThat( jsonNode.get(1).asBoolean(), equalTo( false ) );
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
                .payload( simpleCypherRequestBody() )
                .post( txCommitURL() );

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

        // It should be possible to authenticate with password change required
        gen.get().expectedStatus( 200 ).withHeader( HttpHeaders.AUTHORIZATION, HTTP.basicAuthHeader( "neo4j", "neo4j" ) );

        // When
        HTTP.Response responseBeforePasswordChange = HTTP.withBasicAuth( "neo4j", "neo4j" ).POST( txCommitURL( "system" ), query( "SHOW USERS" ) );

        // The server should throw error when trying to do something else than changing password
        assertPermissionErrorAtSystemAccess( responseBeforePasswordChange );

        // When
        // Changing the user password
        HTTP.Response response =
                HTTP.withBasicAuth( "neo4j", "neo4j" ).POST( txCommitURL( "system" ), query( "ALTER CURRENT USER SET PASSWORD FROM 'neo4j' TO 'secret'" ) );
        // Then
        assertThat( response.status(), equalTo( 200 ) );
        assertThat( "Should have no errors", response.get( "errors" ).size(), equalTo( 0 ) );

        // When
        HTTP.Response responseAfterPasswordChange = HTTP.withBasicAuth( "neo4j", "secret" ).POST( txCommitURL( "system" ), query( "SHOW USERS" ) );

        // Then
        assertThat( responseAfterPasswordChange.status(), equalTo( 200 ) );
        assertThat( "Should have no errors", response.get( "errors" ).size(), equalTo( 0 ) );
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
                .payload( simpleCypherRequestBody() )
                .post( txCommitURL() );
    }

    @Test
    public void shouldSayMalformedHeaderIfMalformedAuthorization() throws Exception
    {
        // Given
        startServerWithConfiguredUser();

        // When
        HTTP.Response response = HTTP.withHeaders( HttpHeaders.AUTHORIZATION, "This makes no sense" )
                .POST( txCommitURL(), rawPayload( simpleCypherRequestBody() ) );

        // Then
        assertThat( response.status(), equalTo( 400 ) );
        assertThat( response.get( "errors" ).get( 0 ).get( "code" ).asText(), equalTo( "Neo.ClientError.Request.InvalidFormat" ) );
        assertThat( response.get( "errors" ).get( 0 ).get( "message" ).asText(), equalTo( "Invalid authentication header." ) );
    }

    protected void startServerWithConfiguredUser() throws IOException
    {
        startServer( true );
        // Set the password
        HTTP.Response post = HTTP.withBasicAuth( "neo4j", "neo4j" ).POST( txCommitURL( "system" ),
                query("ALTER CURRENT USER SET PASSWORD FROM 'neo4j' TO 'secret'" ) );
        assertEquals( 200, post.status() );
    }

    private JsonNode getResultRow( HTTP.Response response ) throws JsonParseException
    {
        return response.get( "results" ).get( 0 ).get( "data" ).get( 0 ).get( "row" );
    }
}
