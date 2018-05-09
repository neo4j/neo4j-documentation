/*
 * Copyright (c) 2002-2018 "Neo4j,"
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
package org.neo4j.doc.server;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import javax.ws.rs.core.MediaType;

import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RestRequest;
import org.neo4j.helpers.ListenSocketAddress;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.configuration.ServerSettings;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.doc.server.helpers.CommunityServerBuilder.server;

public class ServerConfigDocIT extends ExclusiveServerTestBase
{
    private CommunityNeoServer server;

    @After
    public void stopTheServer()
    {
        server.stop();
    }

    @Test
    public void shouldPickUpAddressFromConfig() throws Exception
    {
        ListenSocketAddress nonDefaultAddress = new ListenSocketAddress( "0.0.0.0", 4321 );
        server = server().onAddress( nonDefaultAddress )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();

        assertEquals( nonDefaultAddress, server.getAddress() );

        JaxRsResponse response = new RestRequest( server.baseUri() ).get();

        assertThat( response.getStatus(), is( 200 ) );
        response.close();
    }

    @Test
    public void shouldPickupRelativeUrisForMangementApiAndRestApi() throws IOException
    {
        String dataUri = "/a/different/data/uri/";
        String managementUri = "/a/different/management/uri/";

        server = server().withRelativeRestApiUriPath( dataUri )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .withRelativeManagementApiUriPath( managementUri )
                .build();
        server.start();

        JaxRsResponse response = new RestRequest().get( "http://localhost:7474" + dataUri,
                MediaType.TEXT_HTML_TYPE );
        assertEquals( 200, response.getStatus() );

        response = new RestRequest().get( "http://localhost:7474" + managementUri );
        assertEquals( 200, response.getStatus() );
        response.close();
    }

    @Test
    public void shouldGenerateWADLWhenExplicitlyEnabledInConfig() throws IOException
    {
        server = server().withProperty( ServerSettings.wadl_enabled.name(), "true" )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();
        JaxRsResponse response = new RestRequest().get( "http://localhost:7474/application.wadl",
                MediaType.WILDCARD_TYPE );

        assertEquals( 200, response.getStatus() );
        assertEquals( "application/vnd.sun.wadl+xml", response.getHeaders().get( "Content-Type" ).iterator().next() );
        assertThat( response.getEntity(), containsString( "<application xmlns=\"http://wadl.dev.java" +
                                                          ".net/2009/02\">" ) );
    }

    @Test
    public void shouldNotGenerateWADLWhenNotExplicitlyEnabledInConfig() throws IOException
    {
        server = server()
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();
        JaxRsResponse response = new RestRequest().get( "http://localhost:7474/application.wadl",
                MediaType.WILDCARD_TYPE );

        assertEquals( 404, response.getStatus() );
    }

    @Test
    public void shouldNotGenerateWADLWhenExplicitlyDisabledInConfig() throws IOException
    {
        server = server().withProperty( ServerSettings.wadl_enabled.name(), "false" )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();
        JaxRsResponse response = new RestRequest().get( "http://localhost:7474/application.wadl",
                MediaType.WILDCARD_TYPE );

        assertEquals( 404, response.getStatus() );
    }

    @Test
    public void shouldEnablConsoleServiceByDefault() throws IOException
    {
        // Given
        server = server().usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() ).build();
        server.start();

        // When & then
        assertEquals( 200, new RestRequest().get( "http://localhost:7474/db/manage/server/console" ).getStatus() );
    }

    @Test
    public void shouldDisableConsoleServiceWhenAskedTo() throws IOException
    {
        // Given
        server = server().withProperty( ServerSettings.console_module_enabled.name(), "false" )
                .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                .build();
        server.start();

        // When & then
        assertEquals( 404, new RestRequest().get( "http://localhost:7474/db/manage/server/console" ).getStatus() );
    }
}
