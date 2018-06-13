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

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.web.PropertyValueException;
import org.neo4j.test.GraphDescription;
import org.neo4j.test.GraphDescription.LABEL;
import org.neo4j.test.GraphDescription.NODE;
import org.neo4j.test.GraphDescription.PROP;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.Label.label;
import static org.neo4j.helpers.collection.Iterables.map;
import static org.neo4j.helpers.collection.Iterators.asSet;
import static org.neo4j.server.rest.domain.JsonHelper.createJsonFrom;
import static org.neo4j.server.rest.domain.JsonHelper.readJson;
import static org.neo4j.test.GraphDescription.PropType.ARRAY;
import static org.neo4j.test.GraphDescription.PropType.STRING;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasLabel;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.hasLabels;
import static org.neo4j.test.mockito.matcher.Neo4jMatchers.inTx;

public class LabelsDocIT extends AbstractRestFunctionalTestBase
{

    @Test
    @Documented( "Get nodes by label and array property." )
    @GraphDescription.Graph( nodes = {
            @NODE(name = "Donald Sutherland", labels = {@LABEL("Person")}),
            @NODE(name = "Clint Eastwood", labels = {@LABEL("Person")}, properties =
                    {@PROP(key = "names", value = "Clint,Eastwood", type = ARRAY, componentType = STRING)}),
            @NODE(name = "Steven Spielberg", labels = {@LABEL("Person")}, properties =
                    {@PROP(key = "names", value = "Steven,Spielberg", type = ARRAY, componentType = STRING)})})
    public void get_nodes_with_label_and_array_property() throws JsonParseException, UnsupportedEncodingException
    {
        data.get();

        String labelName = "Person";

        String uri = getNodesWithLabelAndPropertyUri( labelName, "names", new String[] { "Clint", "Eastwood" } );

        String result = gen.get()
                .expectedStatus( 200 )
                .get( uri )
                .entity();

        List<?> parsed = (List<?>) readJson( result );
        assertEquals( 1, parsed.size() );

        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals( Iterables.asSet( asList( asList( "Clint", "Eastwood" ) ) ),
                Iterables.asSet( map( getProperty( "names", List.class ), parsed ) ) );
    }

    private <T> Function<Object, T> getProperty( final String propertyKey, final Class<T> propertyType )
    {
        return from -> {
            Map<?, ?> node = (Map<?, ?>) from;
            Map<?, ?> data1 = (Map<?, ?>) node.get( "data" );
            return propertyType.cast( data1.get( propertyKey ) );
        };
    }
}
