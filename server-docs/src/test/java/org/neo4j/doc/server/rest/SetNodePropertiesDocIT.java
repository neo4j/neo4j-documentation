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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasProperty;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.inTx;

public class SetNodePropertiesDocIT extends
        AbstractRestFunctionalTestBase
{

    @Graph( "jim knows joe" )
    @Test
    public void set_node_properties_in_Unicode()
            throws JsonParseException
    {
        Node jim = data.get().get( "jim" );
        gen.get().payload(
                JsonHelper.createJsonFrom( MapUtil.map( "name", "\u4f8b\u5b50" ) ) ).expectedStatus(
                204 ).put( getPropertiesUri( jim ) );
        assertThat( jim, inTx( graphdb(), hasProperty( "name" ).withValue( "\u4f8b\u5b50" ) ) );
    }

    @Test
    @Graph( "jim knows joe" )
    public void shouldReturn400WhenSendinIncompatibleJsonProperties()
            throws JsonParseException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "jim", new HashMap<String, Object>() );
        gen.get().payload( JsonHelper.createJsonFrom( map ) ).expectedStatus(
                400 ).put( getPropertiesUri( data.get().get( "jim" ) ) );
    }

    @Test
    @Graph( "jim knows joe" )
    public void shouldReturn400WhenSendingCorruptJsonProperties()
    {
        JaxRsResponse response = RestRequest.req().put(
                getPropertiesUri( data.get().get( "jim" ) ),
                "this:::Is::notJSON}" );
        assertEquals( 400, response.getStatus() );
        response.close();
    }

    @Test
    @Graph( "jim knows joe" )
    public void shouldReturn404WhenPropertiesSentToANodeWhichDoesNotExist()
            throws JsonParseException
    {
        gen.get().payload(
                JsonHelper.createJsonFrom( MapUtil.map( "key", "val" ) ) ).expectedStatus(
                404 ).put( getDataUri() + "node/12345/properties" );
    }

    private URI getPropertyUri( Node node, String key ) throws Exception
    {
        return new URI( getPropertiesUri( node ) + "/" + key );
    }

    @Test
    @Graph( "jim knows joe" )
    public void shouldReturn400WhenSendingCorruptJsonProperty()
            throws Exception
    {
        JaxRsResponse response = RestRequest.req().put(
                getPropertyUri( data.get().get( "jim" ), "foo" ),
                "this:::Is::notJSON}" );
        assertEquals( 400, response.getStatus() );
        response.close();
    }

    @Test
    @Graph( "jim knows joe" )
    public void shouldReturn404WhenPropertySentToANodeWhichDoesNotExist()
            throws Exception
    {
        JaxRsResponse response = RestRequest.req().put(
                getDataUri() + "node/1234/foo",
                JsonHelper.createJsonFrom( "bar" ) );
        assertEquals( 404, response.getStatus() );
        response.close();
    }

}
