/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.doc.server.helpers;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

import org.neo4j.doc.server.rest.JaxRsResponse;
import org.neo4j.doc.server.rest.RestRequest;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PluginFunctionalTestHelper
{
    private PluginFunctionalTestHelper()
    {
    }

    public static Map<String, Object> makeGet( String url ) throws JsonParseException
    {
        JaxRsResponse response = new RestRequest().get(url);

        String body = getResponseText(response);

        return deserializeMap(body);
    }

    protected static Map<String, Object> deserializeMap( final String body ) throws JsonParseException
    {
        Map<String, Object> result = JsonHelper.jsonToMap( body );
        assertThat( result, CoreMatchers.is( not( nullValue() ) ) );
        return result;
    }

    private static List<Map<String, Object>> deserializeList( final String body ) throws JsonParseException
    {
        List<Map<String, Object>> result = JsonHelper.jsonToList( body );
        assertThat( result, CoreMatchers.is( not( nullValue() ) ) );
        return result;
    }

    protected static String getResponseText( final JaxRsResponse response )
    {
        String body = response.getEntity();

        Assert.assertEquals( body, 200, response.getStatus() );
        return body;
    }

    protected static Map<String, Object> makePostMap( String url ) throws JsonParseException
    {
        JaxRsResponse response = new RestRequest().post(url,null);

        String body = getResponseText( response );

        return deserializeMap( body );
    }

    protected static Map<String, Object> makePostMap( String url, Map<String, Object> params )
            throws JsonParseException
    {
        String json = JsonHelper.createJsonFrom( params );
        JaxRsResponse response = new RestRequest().post(url, json, MediaType.APPLICATION_JSON_TYPE);

        String body = getResponseText( response );

        return deserializeMap( body );
    }

    protected static List<Map<String, Object>> makePostList( String url ) throws JsonParseException
    {
        JaxRsResponse response = new RestRequest().post(url, null);

        String body = getResponseText(response);

        return deserializeList(body);
    }

    protected static List<Map<String, Object>> makePostList( String url, Map<String, Object> params )
            throws JsonParseException
    {
        String json = JsonHelper.createJsonFrom(params);
        JaxRsResponse response = new RestRequest().post(url, json);

        String body = getResponseText(response);

        return deserializeList(body);
    }

    public static class RegExp extends TypeSafeMatcher<String>
    {
        enum MatchType
        {
            end( "ends with" )
                    {
                        @Override
                        boolean match( String pattern, String string )
                        {
                            return string.endsWith( pattern );
                        }
                    },
            matches()
                    {
                        @Override
                        boolean match( String pattern, String string )
                        {
                            return string.matches( pattern );
                        }
                    },
            ;
            private final String description;

            abstract boolean match( String pattern, String string );

            MatchType()
            {
                this.description = name();
            }

            MatchType( String description )
            {
                this.description = description;
            }
        }

        private final String pattern;
        private String string;
        private final MatchType type;

        RegExp( String regexp, MatchType type )
        {
            this.pattern = regexp;
            this.type = type;
        }

        @Factory
        public static Matcher<String> endsWith( String pattern )
        {
            return new RegExp( pattern, MatchType.end );
        }

        @Override
        public boolean matchesSafely( String string )
        {
            this.string = string;
            return type.match( pattern, string );
        }

        @Override
        public void describeTo( Description descr )
        {
            descr.appendText( "expected something that " )
                    .appendText( type.description )
                    .appendText( " [" )
                    .appendText( pattern )
                    .appendText( "] but got [" )
                    .appendText( string )
                    .appendText( "]" );
        }
    }

}
