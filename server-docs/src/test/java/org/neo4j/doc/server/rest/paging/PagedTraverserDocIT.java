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
package org.neo4j.doc.server.rest.paging;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.database.Database;
import org.neo4j.doc.server.helpers.CommunityServerBuilder;
import org.neo4j.doc.server.helpers.FunctionalTestHelper;
import org.neo4j.doc.server.helpers.ServerHelper;
import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RESTDocsGenerator;
import org.neo4j.doc.server.rest.RESTDocsGenerator.ResponseEntity;
import org.neo4j.doc.server.rest.RestRequest;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.web.ScriptExecutionMode;
import org.neo4j.server.scripting.javascript.GlobalJavascriptInitializer;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.server.ExclusiveServerTestBase;
import org.neo4j.time.Clocks;
import org.neo4j.time.FakeClock;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.neo4j.doc.test.rule.SuppressOutput.suppressAll;

public class PagedTraverserDocIT extends ExclusiveServerTestBase
{
    private static CommunityNeoServer server;
    private static FunctionalTestHelper functionalTestHelper;

    private Node theStartNode;
    private static final String PAGED_TRAVERSE_LINK_REL = "paged_traverse";
    private static final int SHORT_LIST_LENGTH = 33;
    private static final int LONG_LIST_LENGTH = 444;
    private static final int VERY_LONG_LIST_LENGTH = LONG_LIST_LENGTH*2;

    @ClassRule
    public static TemporaryFolder staticFolder = new TemporaryFolder();

    @Rule
    public
    TestData<RESTDocsGenerator> gen = TestData.producedThrough( RESTDocsGenerator.PRODUCER );
    private static FakeClock clock;

    @Before
    public void setUp()
    {
        gen.get().setSection( "dev/rest-api" );
    }

    @BeforeClass
    public static void setupServer() throws Exception
    {
        clock = Clocks.fakeClock();
        server = CommunityServerBuilder.server()
                .usingDataDir( staticFolder.getRoot().getAbsolutePath() )
                .withClock( clock )
                .build();

        suppressAll().call( (Callable<Void>) () -> {
            server.start();
            return null;
        } );
        functionalTestHelper = new FunctionalTestHelper( server );
    }

    @Before
    public void setupTheDatabase() throws Exception
    {
        ServerHelper.cleanTheDatabase( server );
    }

    @AfterClass
    public static void stopServer() throws Exception
    {
        suppressAll().call( (Callable<Void>) () -> {
            server.stop();
            return null;
        } );
    }

    @Test
    public void nodeRepresentationShouldHaveLinkToPagedTraverser() throws Exception
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        JaxRsResponse response = RestRequest.req().get( functionalTestHelper.nodeUri( theStartNode.getId() ) );

        Map<String, Object> jsonMap = JsonHelper.jsonToMap( response.getEntity() );

        assertNotNull( jsonMap.get( PAGED_TRAVERSE_LINK_REL ) );
        assertThat( String.valueOf( jsonMap.get( PAGED_TRAVERSE_LINK_REL ) ),
                containsString( "/db/data/node/" + String.valueOf( theStartNode.getId() )
                        + "/paged/traverse/{returnType}{?pageSize,leaseTime}" ) );
    }

    @Test
    public void shouldExpireTheTraverserAfterDefaultTimeoutAndGetA404Response()
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        JaxRsResponse postResponse = createPagedTraverser();
        assertEquals( 201, postResponse.getStatus() );

        final int TEN_MINUTES = 10;
        clock.forward( TEN_MINUTES, TimeUnit.MINUTES );

        JaxRsResponse getResponse = new RestRequest( postResponse.getLocation() ).get();

        assertEquals( 404, getResponse.getStatus() );
    }

    @Test
    public void shouldTraverseAllPagesWithANonDefaultTimeoutAndNonDefaultPageSize()
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        URI traverserLocation = createPagedTraverserWithTimeoutInMinutesAndPageSize( 10, 2 ).getLocation();

        int enoughPagesToExpireTheTraverser = 6;
        for ( int i = 0; i < enoughPagesToExpireTheTraverser; i++ )
        {

            JaxRsResponse response = new RestRequest( traverserLocation ).get();
            assertEquals( 200, response.getStatus() );
        }

        JaxRsResponse response = new RestRequest( traverserLocation ).get();
        assertEquals( 404, response.getStatus() );
    }

    @Test
    public void shouldRespondWith400OnNegativeLeaseTime()
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        int negativeLeaseTime = -9;
        JaxRsResponse response = RestRequest.req().post(
                functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?leaseTime=" +
                        String.valueOf( negativeLeaseTime ), traverserDescription() );

        assertEquals( 400, response.getStatus() );
    }

    @Test
    public void shouldRespondWith400OnNegativePageSize()
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        int negativePageSize = -99;
        JaxRsResponse response = RestRequest.req().post(
                functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?pageSize=" +
                        String.valueOf( negativePageSize ), traverserDescription() );

        assertEquals( 400, response.getStatus() );
    }

    @Test
    public void shouldRespondWith400OnScriptErrors()
    {
        GlobalJavascriptInitializer.initialize( ScriptExecutionMode.SANDBOXED );

        theStartNode = createLinkedList( 1, server.getDatabase() );

        JaxRsResponse response = RestRequest.req().post(
                functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?pageSize=50",
                "{"
                        + "\"prune_evaluator\":{\"language\":\"builtin\",\"name\":\"none\"},"
                        + "\"return_filter\":{\"language\":\"javascript\",\"body\":\"position.getClass()" +
                        ".getClassLoader();\"},"
                        + "\"order\":\"depth_first\","
                        + "\"relationships\":{\"type\":\"NEXT\",\"direction\":\"out\"}"
                        + "}" );

        assertEquals( 400, response.getStatus() );
    }

    @Test
    public void shouldRespondWith200OnFirstDeletionOfTraversalAnd404Afterwards()
    {
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        JaxRsResponse response = createPagedTraverser();

        final RestRequest request = RestRequest.req();
        JaxRsResponse deleteResponse = request.delete( response.getLocation() );
        assertEquals( 200, deleteResponse.getStatus() );

        deleteResponse = request.delete( response.getLocation() );
        assertEquals( 404, deleteResponse.getStatus() );
    }

    @Test
    public void shouldAcceptJsonAndStreamingFlagAndProduceStreamedJson()
    {
        // given
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );

        // when
        JaxRsResponse pagedTraverserResponse = createStreamingPagedTraverserWithTimeoutInMinutesAndPageSize( 60, 1 );

        System.out.println( pagedTraverserResponse.getHeaders().getFirst( "Content-Type" ) );

        // then
        assertNotNull( pagedTraverserResponse.getHeaders().getFirst( "Content-Type" ) );
        assertThat( pagedTraverserResponse.getHeaders().getFirst( "Content-Type" ),
                containsString( "application/json; charset=UTF-8; stream=true" ) );
    }

    private JaxRsResponse createStreamingPagedTraverserWithTimeoutInMinutesAndPageSize( int leaseTimeInSeconds,
                                                                                        int pageSize )
    {
        String description = traverserDescription();

        return RestRequest.req().header( "X-Stream", "true" ).post(
                functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?leaseTime="
                        + leaseTimeInSeconds + "&pageSize=" + pageSize, description );
    }

    @Test
    public void should201WithAcceptJsonHeader()
    {
        // given
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );
        String uri = functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node";

        // when
        JaxRsResponse response = RestRequest.req().accept( MediaType.APPLICATION_JSON_TYPE ).post( uri,
                traverserDescription() );

        // then
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeaders().getFirst( "Content-Type" ) );
        assertThat( response.getType().toString(), containsString( MediaType.APPLICATION_JSON ) );
    }

    @Test
    public void should201WithAcceptHtmlHeader()
    {
        // given
        theStartNode = createLinkedList( SHORT_LIST_LENGTH, server.getDatabase() );
        String uri = functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node";

        // when
        JaxRsResponse response = RestRequest.req().accept( MediaType.TEXT_HTML_TYPE ).post( uri,
                traverserDescription() );

        // then
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeaders().getFirst( "Content-Type" ) );
        assertThat( response.getType().toString(), containsString( MediaType.TEXT_HTML ) );
    }

    @Test
    public void shouldHaveTransportEncodingChunkedOnResponseHeader()
    {
        // given
        theStartNode = createLinkedList( VERY_LONG_LIST_LENGTH, server.getDatabase() );

        // when
        JaxRsResponse response = createStreamingPagedTraverserWithTimeoutInMinutesAndPageSize( 60, 1000 );

        // then
        assertEquals( 201, response.getStatus() );
        assertEquals( "application/json; charset=UTF-8; stream=true", response.getHeaders().getFirst( "Content-Type"
        ) );
        assertThat( response.getHeaders().getFirst( "Transfer-Encoding" ), containsString( "chunked" ) );
    }

    private JaxRsResponse createPagedTraverserWithTimeoutInMinutesAndPageSize( final int leaseTimeInSeconds,
                                                                               final int pageSize )
    {
        String description = traverserDescription();

        return RestRequest.req().post(
                functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?leaseTime="
                        + leaseTimeInSeconds + "&pageSize=" + pageSize, description );
    }

    private JaxRsResponse createPagedTraverserWithTimeoutInMinutes( final int leaseTime )
    {
        ResponseEntity responseEntity = gen.get()
                .expectedType( MediaType.APPLICATION_JSON_TYPE )
                .expectedStatus( 201 )
                .payload( traverserDescription() )
                .post( functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?leaseTime="
                        + String.valueOf( leaseTime ) );

        return responseEntity.response();
    }

    private JaxRsResponse createPagedTraverserWithPageSize( final int pageSize )
    {
        ResponseEntity responseEntity = gen.get()
                .expectedType( MediaType.APPLICATION_JSON_TYPE )
                .expectedStatus( 201 )
                .payload( traverserDescription() )
                .post( functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node?pageSize="
                        + String.valueOf( pageSize ) );

        return responseEntity.response();
    }

    private JaxRsResponse createPagedTraverser()
    {
        final String uri = functionalTestHelper.nodeUri( theStartNode.getId() ) + "/paged/traverse/node";
        return RestRequest.req().post( uri, traverserDescription() );
    }

    private String traverserDescription()
    {
        String description = "{"
                + "\"prune_evaluator\":{\"language\":\"builtin\",\"name\":\"none\"},"
                + "\"return_filter\":{\"language\":\"javascript\",\"body\":\"position.endNode().getProperty('name')" +
                ".contains('1');\"},"
                + "\"order\":\"depth_first\","
                + "\"relationships\":{\"type\":\"NEXT\",\"direction\":\"out\"}"
                + "}";

        return description;
    }

    private Node createLinkedList( final int listLength, final Database db )
    {
        Node startNode = null;
        try ( Transaction tx = db.getGraph().beginTx() )
        {
            Node previous = null;
            for ( int i = 0; i < listLength; i++ )
            {
                Node current = db.getGraph().createNode();
                current.setProperty( "name", String.valueOf( i ) );

                if ( previous != null )
                {
                    previous.createRelationshipTo( current, RelationshipType.withName( "NEXT" ) );
                }
                else
                {
                    startNode = current;
                }

                previous = current;
            }
            tx.success();
            return startNode;
        }
    }
}
