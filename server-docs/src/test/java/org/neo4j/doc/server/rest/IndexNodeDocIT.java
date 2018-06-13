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
package org.neo4j.doc.server.rest;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.neo4j.function.Factory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.doc.server.helpers.FunctionalTestHelper;
import org.neo4j.doc.server.rest.RESTDocsGenerator.ResponseEntity;
import org.neo4j.doc.server.rest.domain.GraphDbHelper;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.domain.URIHelper;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.doc.server.helpers.FunctionalTestHelper.CLIENT;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasProperty;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.inTx;

public class IndexNodeDocIT extends AbstractRestFunctionalTestBase
{
    private static FunctionalTestHelper functionalTestHelper;
    private static GraphDbHelper helper;

    @BeforeClass
    public static void setupServer()
    {
        functionalTestHelper = new FunctionalTestHelper( server() );
        helper = functionalTestHelper.getGraphDbHelper();
    }

    @Before
    public void setup()
    {
        gen().setGraph( server().getDatabase().getGraph() );
    }

    @Test
    public void shouldCreateANamedNodeIndexWithSpaces()
    {
        String indexName =  indexes.newInstance() + " with spaces";
        int expectedIndexes = helper.getNodeIndexes().length + 1;
        Map<String, String> indexSpecification = new HashMap<>();
        indexSpecification.put( "name", indexName );

        gen()
                .payload( JsonHelper.createJsonFrom( indexSpecification ) )
                .expectedStatus( 201 )
                .expectedHeader( "Location" )
                .post( functionalTestHelper.nodeIndexUri() );

        assertEquals( expectedIndexes, helper.getNodeIndexes().length );
        assertThat( helper.getNodeIndexes(), FunctionalTestHelper.arrayContains( indexName ) );
    }

    @Test
    public void orderedResultsAreSupersetOfUnordered() throws Exception
    {
        // Given
        String indexName = indexes.newInstance();
        String key = "Name";
        String value = "Builder";
        long node = helper.createNode( MapUtil.map( key, value ) );
        helper.addNodeToIndex( indexName, key, value, node );
        helper.addNodeToIndex( indexName, "Gender", "Male", node );

        String entity = gen().expectedStatus( 200 ).get(
                functionalTestHelper.indexNodeUri( indexName )
                + "?query=" + key + ":Build~0.1%20AND%20Gender:Male" ).entity();

        @SuppressWarnings( "unchecked" )
        Collection<LinkedHashMap<String, String>> hits =
                (Collection<LinkedHashMap<String, String>>) JsonHelper.readJson( entity );
        LinkedHashMap<String, String> nodeMapUnordered = hits.iterator().next();

        // When
        entity = gen().expectedStatus( 200 ).get(
                functionalTestHelper.indexNodeUri( indexName )
                        + "?query="+key+":Build~0.1%20AND%20Gender:Male&order=score" ).entity();

        //noinspection unchecked
        hits = (Collection<LinkedHashMap<String, String>>) JsonHelper.readJson( entity );
        LinkedHashMap<String, String> nodeMapOrdered = hits.iterator().next();

        // Then
        for ( Map.Entry<String, String> unorderedEntry : nodeMapUnordered.entrySet() )
        {
            assertEquals( "wrong entry for key: " + unorderedEntry.getKey(),
                    unorderedEntry.getValue(),
                    nodeMapOrdered.get( unorderedEntry.getKey() ) );
        }
        assertTrue( "There should be only one extra value for the ordered map",
                nodeMapOrdered.size() == nodeMapUnordered.size() + 1 );
    }

    //TODO:add compatibility tests for old syntax
    @Test
    public void shouldAddToIndexAndRetrieveItByQuerySorted()
            throws JsonParseException
    {
        String indexName = indexes.newInstance();
        String key = "Name";
        long node1 = helper.createNode();
        long node2 = helper.createNode();

        helper.addNodeToIndex( indexName, key, "Builder2", node1 );
        helper.addNodeToIndex( indexName, "Gender", "Male", node1 );
        helper.addNodeToIndex( indexName, key, "Builder", node2 );
        helper.addNodeToIndex( indexName, "Gender", "Male", node2 );

        String entity = gen().expectedStatus( 200 ).get(
                functionalTestHelper.indexNodeUri( indexName )
                + "?query=" + key + ":Builder~%20AND%20Gender:Male&order=relevance" ).entity();

        Collection<?> hits = (Collection<?>) JsonHelper.readJson( entity );
        assertEquals( 2, hits.size() );
        @SuppressWarnings( "unchecked" )
        Iterator<LinkedHashMap<String, Object>> it = (Iterator<LinkedHashMap<String, Object>>) hits.iterator();

        LinkedHashMap<String, Object> node2Map = it.next();
        LinkedHashMap<String, Object> node1Map = it.next();
        float score2 = ( (Double) node2Map.get( "score" ) ).floatValue();
        float score1 = ( (Double) node1Map.get( "score" ) ).floatValue();
        assertTrue(
                "results returned in wrong order for relevance ordering",
                ( (String) node2Map.get( "self" ) ).endsWith( Long.toString( node2 ) ) );
        assertTrue(
                "results returned in wrong order for relevance ordering",
                ( (String) node1Map.get( "self" ) ).endsWith( Long.toString( node1 ) ) );
        /*
         * scores are always the same, just the ordering changes. So all subsequent tests will
         * check the same condition.
         */
        assertTrue( "scores are reversed", score2 > score1 );

        entity = gen().expectedStatus( 200 ).get(
                functionalTestHelper.indexNodeUri( indexName )
                        + "?query="+key+":Builder~%20AND%20Gender:Male&order=index" ).entity();

        hits = (Collection<?>) JsonHelper.readJson( entity );
        assertEquals( 2, hits.size() );
        //noinspection unchecked
        it = (Iterator<LinkedHashMap<String, Object>>) hits.iterator();

        /*
         * index order, so as they were added
         */
        node1Map = it.next();
        node2Map = it.next();
        score1 = ( (Double) node1Map.get( "score" ) ).floatValue();
        score2 = ( (Double) node2Map.get( "score" ) ).floatValue();
        assertTrue(
                "results returned in wrong order for index ordering",
                ( (String) node1Map.get( "self" ) ).endsWith( Long.toString( node1 ) ) );
        assertTrue(
                "results returned in wrong order for index ordering",
                ( (String) node2Map.get( "self" ) ).endsWith( Long.toString( node2 ) ) );
        assertTrue( "scores are reversed", score2 > score1 );

        entity = gen().expectedStatus( 200 ).get(
                functionalTestHelper.indexNodeUri( indexName )
                        + "?query="+key+":Builder~%20AND%20Gender:Male&order=score" ).entity();

        hits = (Collection<?>) JsonHelper.readJson( entity );
        assertEquals( 2, hits.size() );
        //noinspection unchecked
        it = (Iterator<LinkedHashMap<String, Object>>) hits.iterator();

        node2Map = it.next();
        node1Map = it.next();
        score2 = ( (Double) node2Map.get( "score" ) ).floatValue();
        score1 = ( (Double) node1Map.get( "score" ) ).floatValue();
        assertTrue(
                "results returned in wrong order for score ordering",
                ( (String) node2Map.get( "self" ) ).endsWith( Long.toString( node2 ) ) );
        assertTrue(
                "results returned in wrong order for score ordering",
                ( (String) node1Map.get( "self" ) ).endsWith( Long.toString( node1 ) ) );
        assertTrue( "scores are reversed", score2 > score1 );
    }

    /**
     * POST ${org.neo4j.server.rest.web}/index/node/{indexName}/{key}/{value}
     * "http://uri.for.node.to.index"
     */
    @Test
    public void shouldRespondWith201CreatedWhenIndexingJsonNodeUri()
    {
        final long nodeId = helper.createNode();
        final String key = "key";
        final String value = "value";
        final String indexName = indexes.newInstance();
        helper.createNodeIndex( indexName );

        JaxRsResponse response = RestRequest.req()
                .post( functionalTestHelper.indexNodeUri( indexName ), createJsonStringFor( nodeId, key, value ) );
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeaders()
                .getFirst( "Location" ) );
        assertEquals( singletonList( nodeId ), helper.getIndexedNodes( indexName, key, value ) );
    }

    @Test
    public void shouldGetNodeRepresentationFromIndexUri() throws  JsonParseException
    {
        long nodeId = helper.createNode();
        String key = "key2";
        String value = "value";

        String indexName = indexes.newInstance();
        helper.createNodeIndex( indexName );
        JaxRsResponse response = RestRequest.req()
                .post( functionalTestHelper.indexNodeUri( indexName ),
                        createJsonStringFor( nodeId, key, value ));

        assertEquals( Status.CREATED.getStatusCode(), response.getStatus() );
        String indexUri = response.getHeaders()
                .getFirst( "Location" );

        response = RestRequest.req()
                .get( indexUri );
        assertEquals( 200, response.getStatus() );

        String entity = response.getEntity();

        Map<String, Object> map = JsonHelper.jsonToMap( entity );
        assertNotNull( map.get( "self" ) );
    }

    @Test
    public void shouldGet404WhenRequestingIndexUriWhichDoesntExist()
    {
        String key = "key3";
        String value = "value";
        String indexName = indexes.newInstance();
        String indexUri = functionalTestHelper.nodeIndexUri() + indexName + "/" + key + "/" + value;
        JaxRsResponse response = RestRequest.req()
                .get( indexUri );
        assertEquals( Status.NOT_FOUND.getStatusCode(), response.getStatus() );
    }

    @Test
    public void shouldGet404WhenDeletingNonExtistentIndex()
    {
        final String indexName = indexes.newInstance();
        String indexUri = functionalTestHelper.nodeIndexUri() + indexName;
        JaxRsResponse response = RestRequest.req().delete( indexUri );
        assertEquals( Status.NOT_FOUND.getStatusCode(), response.getStatus() );
    }

    @Test
    public void shouldGet200AndArrayOfNodeRepsWhenGettingFromIndex() throws JsonParseException
    {
        String key = "myKey";
        String value = "myValue";

        String name1 = "Thomas Anderson";
        String name2 = "Agent Smith";

        String indexName = indexes.newInstance();
        final RestRequest request = RestRequest.req();
        JaxRsResponse responseToPost = request.post( functionalTestHelper.nodeUri(), "{\"name\":\"" + name1 + "\"}" );
        assertEquals( 201, responseToPost.getStatus() );
        String location1 = responseToPost.getHeaders()
                .getFirst( HttpHeaders.LOCATION );
        responseToPost.close();
        responseToPost = request.post( functionalTestHelper.nodeUri(), "{\"name\":\"" + name2 + "\"}" );
        assertEquals( 201, responseToPost.getStatus() );
        String location2 = responseToPost.getHeaders()
                .getFirst( HttpHeaders.LOCATION );
        responseToPost.close();
        responseToPost = request.post( functionalTestHelper.indexNodeUri( indexName ),
                createJsonStringFor( functionalTestHelper.getNodeIdFromUri( location1 ), key, value ) );
        assertEquals( 201, responseToPost.getStatus() );
        String indexLocation1 = responseToPost.getHeaders()
                .getFirst( HttpHeaders.LOCATION );
        responseToPost.close();
        responseToPost = request.post( functionalTestHelper.indexNodeUri( indexName ),
                createJsonStringFor( functionalTestHelper.getNodeIdFromUri( location2 ), key, value ) );
        assertEquals( 201, responseToPost.getStatus() );
        String indexLocation2 = responseToPost.getHeaders()
                .getFirst( HttpHeaders.LOCATION );
        Map<String, String> uriToName = new HashMap<>();
        uriToName.put( indexLocation1, name1 );
        uriToName.put( indexLocation2, name2 );
        responseToPost.close();

        JaxRsResponse response = RestRequest.req()
                .get( functionalTestHelper.indexNodeUri( indexName, key, value ) );
        assertEquals( 200, response.getStatus() );
        Collection<?> items = (Collection<?>) JsonHelper.readJson( response.getEntity() );
        int counter = 0;
        for ( Object item : items )
        {
            Map<?, ?> map = (Map<?, ?>) item;
            Map<?, ?> properties = (Map<?, ?>) map.get( "data" );
            assertNotNull( map.get( "self" ) );
            String indexedUri = (String) map.get( "indexed" );
            assertEquals( uriToName.get( indexedUri ), properties.get( "name" ) );
            counter++;
        }
        assertEquals( 2, counter );
        response.close();
    }

    @Test
    public void shouldGet200WhenGettingNodesFromIndexWithNoHits()
    {
        final String indexName = indexes.newInstance();
        helper.createNodeIndex( indexName );
        JaxRsResponse response = RestRequest.req()
                .get( functionalTestHelper.indexNodeUri( indexName, "non-existent-key", "non-existent-value" ) );
        assertEquals( 200, response.getStatus() );
        response.close();
    }

    //
    // REMOVING ENTRIES
    //

    @Test
    public void shouldBeAbleToIndexValuesContainingSpaces() throws Exception
    {
        final long nodeId = helper.createNode();
        final String key = "key";
        final String value = "value with   spaces  in it";

        String indexName = indexes.newInstance();
        helper.createNodeIndex( indexName );
        final RestRequest request = RestRequest.req();
        JaxRsResponse response = request.post( functionalTestHelper.indexNodeUri( indexName ),
                createJsonStringFor( nodeId, key, value ) );

        assertEquals( Status.CREATED.getStatusCode(), response.getStatus() );
        URI location = response.getLocation();
        response.close();
        response = request.get( functionalTestHelper.indexNodeUri( indexName, key, URIHelper.encode( value ) ) );
        assertEquals( Status.OK.getStatusCode(), response.getStatus() );
        Collection<?> hits = (Collection<?>) JsonHelper.readJson( response.getEntity() );
        assertEquals( 1, hits.size() );
        response.close();

        CLIENT.resource( location )
                .delete();
        response = request.get( functionalTestHelper.indexNodeUri( indexName, key, URIHelper.encode( value ) ) );
        hits = (Collection<?>) JsonHelper.readJson( response.getEntity() );
        assertEquals( 0, hits.size() );
    }

    @Test
    public void shouldRespondWith400WhenSendingCorruptJson() throws Exception
    {
        final String indexName = indexes.newInstance();
        helper.createNodeIndex( indexName );
        final String corruptJson = "{\"key\" \"myKey\"}";
        JaxRsResponse response = RestRequest.req()
                .post( functionalTestHelper.indexNodeUri( indexName ),
                        corruptJson );
        assertEquals( 400, response.getStatus() );
        response.close();
    }

    @Test
    public void get_or_create_node_with_array_properties() throws Exception
    {
        final String index = indexes.newInstance(), key = "name", value = "Tobias";
        helper.createNodeIndex( index );
        ResponseEntity response = gen()
                .expectedStatus( 201 /* created */ )
                .payloadType( MediaType.APPLICATION_JSON_TYPE )
                .payload( "{\"key\": \"" + key + "\", \"value\": \"" + value
                                                       + "\", \"properties\": {\"" + key + "\": \"" + value
                                                       + "\", \"array\": [1,2,3]}}" )
                                     .post( functionalTestHelper.nodeIndexUri() + index + "?unique" );

        MultivaluedMap<String, String> headers = response.response().getHeaders();
        Map<String, Object> result = JsonHelper.jsonToMap( response.entity() );
        String location = headers.getFirst("Location");
        assertEquals( result.get( "indexed" ), location );
        Map<String, Object> data = assertCast( Map.class, result.get( "data" ) );
        assertEquals( value, data.get( key ) );
        assertEquals(Arrays.asList( 1, 2, 3), data.get( "array" ) );
        Node node;
        try ( Transaction tx = graphdb().beginTx() )
        {
            node = graphdb().index().forNodes(index).get(key, value).getSingle();
        }
        assertThat( node, inTx( graphdb(), hasProperty( key ).withValue( value ) ) );
        assertThat( node, inTx( graphdb(), hasProperty( "array" ).withValue( new int[]{1, 2, 3} ) ) );
    }

    @Documented( "Backward Compatibility Test (using old syntax ?unique)\n" +
                 "Put node if absent - Create.\n" +
                 "\n" +
                 "Add a node to an index unless a node already exists for the given index data. In\n" +
                 "this case, a new node is created since nothing existing is found in the index." )
    @Test
    public void put_node_if_absent___create() throws Exception
    {
        final String index = indexes.newInstance(), key = "name", value = "Mattias";
        helper.createNodeIndex( index );
        String uri = functionalTestHelper.nodeIndexUri() + index + "?unique";
        gen().expectedStatus( 201 /* created */ )
                 .payloadType( MediaType.APPLICATION_JSON_TYPE )
                 .payload( "{\"key\": \"" + key + "\", \"value\": \"" + value + "\", \"uri\":\"" + functionalTestHelper.nodeUri( helper.createNode() ) + "\"}" )
                 .post( uri );
    }

    @Test
    public void already_indexed_node_should_not_fail_on_create_or_fail() throws Exception
    {
        // Given
        final String index = indexes.newInstance(), key = "name", value = "Peter";
        GraphDatabaseService graphdb = graphdb();
        helper.createNodeIndex( index );
        Node node;
        try ( Transaction tx = graphdb.beginTx() )
        {
            node = graphdb.createNode();
            graphdb.index().forNodes( index ).add( node, key, value );
            tx.success();
        }

        // When & Then
        gen.get()
                .noGraph()
                .expectedStatus( 201 )
                .payloadType( MediaType.APPLICATION_JSON_TYPE )
                .payload(
                        "{\"key\": \"" + key + "\", \"value\": \"" + value + "\", \"uri\":\""
                                + functionalTestHelper.nodeUri( node.getId() ) + "\"}" )
                .post( functionalTestHelper.nodeIndexUri() + index + "?uniqueness=create_or_fail" );
    }

    private static <T> T assertCast( Class<T> type, Object object )
    {
        assertTrue( type.isInstance( object ) );
        return type.cast( object );
    }

    private long createNode()
    {
        GraphDatabaseService graphdb = server().getDatabase().getGraph();
        try ( Transaction tx = graphdb.beginTx() )
        {
            Node node = graphdb.createNode();
            tx.success();
            return node.getId();
        }
    }

    private String createJsonStringFor( final long nodeId, final String key, final String value )
    {
        return "{\"key\": \"" + key + "\", \"value\": \"" + value + "\", \"uri\": \""
               + functionalTestHelper.nodeUri( nodeId ) + "\"}";
    }

    private Object generateNodeIndexCreationPayload( String key, String value, String nodeUri )
    {
        Map<String, String> results = new HashMap<>();
        results.put( "key", key );
        results.put( "value", value );
        results.put( "uri", nodeUri );
        return results;
    }

    private final Factory<String> indexes =  UniqueStrings.withPrefix( "index" );
}
