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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.server.rest.domain.JsonHelper.createJsonFrom;
import static org.neo4j.server.rest.domain.JsonHelper.readJson;

public class TraverserDocIT extends AbstractRestFunctionalTestBase
{

    @Test
    public void shouldGet404WhenTraversingFromNonExistentNode()
    {
        gen().expectedStatus( Status.NOT_FOUND.getStatusCode() ).payload(
                "{}" ).post( getDataUri() + "node/10000/traverse/node" ).entity();
    }

    @Test
    @Graph( nodes = {@NODE(name="I")} )
    public void shouldGet200WhenNoHitsFromTraversing()
    {
        assertSize( 0,gen().expectedStatus( 200 ).payload( "" ).post(
                getTraverseUriNodes( getNode( "I" ) ) ).entity());
    }

    private String getTraverseUriRelationships( Node node )
    {
        return getNodeUri( node) + "/traverse/relationship";
    }
    private String getTraverseUriPaths( Node node )
    {
        return getNodeUri( node) + "/traverse/path";
    }

    private String getTraverseUriNodes( Node node )
    {
        // TODO Auto-generated method stub
        return getNodeUri( node) + "/traverse/node";
    }

    @Test
    @Graph( "I know you" )
    public void shouldGetSomeHitsWhenTraversingWithDefaultDescription()
            throws JsonParseException
    {
        String entity = gen().expectedStatus( Status.OK.getStatusCode() ).payload( "{}" ).post(
                getTraverseUriNodes( getNode( "I" ) ) ).entity();

        expectNodes( entity, getNode( "you" ));
    }

    private void expectNodes( String entity, Node... nodes )
            throws JsonParseException
    {
        Set<String> expected = new HashSet<>();
        for ( Node node : nodes )
        {
            expected.add( getNodeUri( node ) );
        }
        Collection<?> items = (Collection<?>) readJson( entity );
        for ( Object item : items )
        {
            Map<?, ?> map = (Map<?, ?>) item;
            String uri = (String) map.get( "self" );
            assertTrue( uri + " not found", expected.remove( uri ) );
        }
        assertTrue( "Expected not empty:" + expected, expected.isEmpty() );
    }

    @Test
    @Graph( "I know you" )
    public void shouldGet400WhenSupplyingInvalidTraverserDescriptionFormat()
    {
        gen().expectedStatus( Status.BAD_REQUEST.getStatusCode() ).payload(
                "::not JSON{[ at all" ).post(
                getTraverseUriNodes( getNode( "I" ) ) ).entity();
    }

    @Test
    @Graph( {"Root knows Mattias",
             "Root knows Johan",  "Johan knows Emil", "Emil knows Peter",
             "Root eats Cork",    "Cork hates Root",
             "Root likes Banana", "Banana is_a Fruit"} )
    public void shouldAllowTypeOrderedTraversals()
            throws JsonParseException
    {
        Node start = getNode( "Root" );
        String description = createJsonFrom( map(
                "expander", "order_by_type",
                "relationships",
                    new Map[]{
                        map( "type", "eats"),
                        map( "type", "knows" ),
                        map( "type", "likes" )
                    },
                "prune_evaluator",
                    map( "language", "builtin",
                         "name", "none" ),
                "return_filter",
                    map( "language", "javascript",
                         "body", "position.length()<2;" )
        ) );
        @SuppressWarnings( "unchecked" )
        List<Map<String,Object>> nodes = (List<Map<String, Object>>) readJson( gen().expectedStatus( 200 ).payload(
                description ).post(
                getTraverseUriNodes( start ) ).entity() );

        assertThat( nodes.size(), is( 5 ) );
        assertThat( getName( nodes.get( 0 ) ), is( "Root" ) );
        assertThat( getName( nodes.get( 1 ) ), is( "Cork" ) );

        // We don't really care about the ordering between Johan and Mattias, we just assert that they
        // both are there, in between Root/Cork and Banana
        Set<String> knowsNodes = new HashSet<>( Arrays.asList( "Johan", "Mattias" ) );
        assertTrue( knowsNodes.remove( getName( nodes.get( 2 ) ) ) );
        assertTrue( knowsNodes.remove( getName( nodes.get( 3 ) ) ) );

        assertThat( getName( nodes.get( 4 ) ), is( "Banana" ) );
    }

    @SuppressWarnings( "unchecked" )
    private String getName( Map<String, Object> propContainer )
    {
        return (String) ((Map<String,Object>)propContainer.get( "data" )).get( "name" );
    }
}
