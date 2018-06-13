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

import com.sun.jersey.api.client.ClientResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import javax.ws.rs.core.Response.Status;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.doc.server.helpers.FunctionalTestHelper;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.repr.StreamingFormat;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.Graph;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;
import org.neo4j.test.GraphDescription.REL;
import org.neo4j.test.TestData.Title;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasProperty;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.inTx;

public class RelationshipDocIT extends AbstractRestFunctionalDocTestBase
{
    private static FunctionalTestHelper functionalTestHelper;

    @BeforeClass
    public static void setupServer() throws IOException
    {
        functionalTestHelper = new FunctionalTestHelper( server() );
    }

    @Test
    @Graph(nodes = {@NODE(name = "Romeo", setNameProperty = true),
            @NODE(name = "Juliet", setNameProperty = true)}, relationships = {@REL(start = "Romeo", end = "Juliet",
            type = "LOVES", properties = {@PROP(key = "cost", value = "high", type = GraphDescription.PropType
            .STRING)})})
    public void shouldReturn404WhenPropertyWhichDoesNotExistRemovedFromRelationshipStreaming()
    {
        data.get();
        Relationship loves = getFirstRelationshipFromRomeoNode();
        gen().withHeader( StreamingFormat.STREAM_HEADER, "true" )
                .expectedStatus( Status.NOT_FOUND.getStatusCode() )
                .delete( getPropertiesUri( loves ) + "/non-existent" )
                .entity();
    }

    private String getRelPropURI( Relationship loves, String propertyKey )
    {
        return getRelationshipUri( loves ) + "/properties/" + propertyKey;
    }

    private Relationship getFirstRelationshipFromRomeoNode()
    {
        Node romeo = getNode( "Romeo" );

        try ( Transaction transaction = romeo.getGraphDatabase().beginTx() )
        {
            return romeo.getRelationships().iterator().next();
        }
    }

    private String getRelPropsURI( Relationship rel )
    {
        return getRelationshipUri( rel ) + "/properties";
    }
}
