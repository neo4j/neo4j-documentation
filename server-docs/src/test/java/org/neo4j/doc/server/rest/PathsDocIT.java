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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;
import org.neo4j.test.GraphDescription.REL;
import org.neo4j.test.TestData.Title;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathsDocIT extends AbstractRestFunctionalTestBase
{

    private void assertThatPathStartsWith( final Map<?, ?> path, final long start )
    {
        assertTrue( "Path should start with " + start + "\nBut it was " + path, path.get( "start" )
                .toString()
                .endsWith( "/node/" + start ) );
    }

    private void assertThatPathEndsWith( final Map<?, ?> path, final long start )
    {
        assertTrue( "Path should end with " + start + "\nBut it was " + path, path.get( "end" )
                .toString()
                .endsWith( "/node/" + start ) );
    }

    private void assertThatPathHasLength( final Map<?, ?> path, final int length )
    {
        Object actual = path.get( "length" );

        assertEquals( "Expected path to have a length of " + length + "\nBut it was " + actual, length, actual );
    }

//      Layout
//
//      (e)----------------
//       |                 |
//      (d)-------------   |
//       |               \/
//      (a)-(c)-(b)-(f)-(g)
//           |\     /   /
//           | ----    /
//            --------
    @Test
    @Graph( value = { "a to c", "a to d", "c to b", "d to e", "b to f", "c to f", "f to g", "d to g", "e to g",
    "c to g" } )
    public void shouldReturn404WhenFailingToFindASinglePath() throws JsonParseException
    {
        long a = nodeId( data.get(), "a" );
        long g = nodeId( data.get(), "g" );
        String noHitsJson = "{\"to\":\""
            + nodeUri( g )
            + "\", \"max_depth\":1, \"relationships\":{\"type\":\"dummy\", \"direction\":\"in\"}, \"algorithm\":\"shortestPath\"}";
        String entity = gen()
        .expectedStatus( Status.NOT_FOUND.getStatusCode() )
        .payload( noHitsJson )
        .post( "http://localhost:7474/db/data/node/" + a + "/path" )
        .entity();
        System.out.println( entity );
    }

    private long nodeId( final Map<String, Node> map, final String string )
    {
        return map.get( string )
        .getId();
    }

    private String nodeUri( final long l )
    {
        return NODES + l;
    }

    private String getAllShortestPathPayLoad( final long to )
    {
        String json = "{\"to\":\""
            + nodeUri( to )
            + "\", \"max_depth\":3, \"relationships\":{\"type\":\"to\", \"direction\":\"out\"}, \"algorithm\":\"shortestPath\"}";
        return json;
    }

    //
    private String getAllPathsUsingDijkstraPayLoad( final long to, final boolean includeDefaultCost )
    {
        String json = "{\"to\":\"" + nodeUri( to ) + "\"" + ", \"cost_property\":\"cost\""
        + ( includeDefaultCost ? ", \"default_cost\":1" : "" )
        + ", \"relationships\":{\"type\":\"to\", \"direction\":\"out\"}, \"algorithm\":\"dijkstra\"}";
        return json;
    }

}
