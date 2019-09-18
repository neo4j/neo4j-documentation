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
package org.neo4j.visualization.graphviz;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class ConfigurationParser
{
    @SuppressWarnings( "unchecked" )
    public ConfigurationParser( File configFile, String... format )
    {
        this( getAllFormats( configFile, format ) );
    }

    public ConfigurationParser( String... format )
    {
        this( Arrays.asList( format ) );
    }

    public ConfigurationParser( Iterable<String> format )
    {
        Class<? extends ConfigurationParser> type = getClass();
        for ( String spec : format )
        {
            String[] parts = spec.split( "=", 2 );
            String name = parts[0];
            String[] args = null;
            Method method;
            Throwable error = null;
            try
            {
                if ( parts.length == 1 )
                {
                    method = type.getMethod( name, String[].class );
                }
                else
                {
                    try
                    {
                        method = type.getMethod( name, String.class );
                        args = new String[]{parts[1]};
                    }
                    catch ( NoSuchMethodException nsm )
                    {
                        error = nsm; // use as a flag to know how to invoke
                        method = type.getMethod( name, String[].class );
                        args = parts[1].split( "," );
                    }
                }
                try
                {
                    if ( error == null )
                    {
                        method.invoke( this, args );
                    }
                    else
                    {
                        error = null; // reset the flag use
                        method.invoke( this, (Object) args );
                    }
                }
                catch ( InvocationTargetException ex )
                {
                    error = ex.getTargetException();
                    if ( error instanceof RuntimeException )
                    {
                        throw (RuntimeException) error;
                    }
                }
                catch ( Exception ex )
                {
                    error = ex;
                }
            }
            catch ( NoSuchMethodException nsm )
            {
                error = nsm;
            }
            if ( error != null )
            {
                throw new IllegalArgumentException( "Unknown parameter \""
                                                    + name + "\"", error );
            }
        }
    }

    private final List<StyleParameter> styles = new ArrayList<>();

    public final StyleParameter[] styles( StyleParameter... params )
    {
        if ( params == null )
        {
            params = new StyleParameter[0];
        }
        StyleParameter[] result = styles.toArray( new StyleParameter[styles.size() + params.length] );
        System.arraycopy( params, 0, result, styles.size(), params.length );
        return result;
    }

    public void nodeTitle( String pattern )
    {
        final PatternParser parser = new PatternParser( pattern );
        styles.add( new StyleParameter.NodeTitle()
        {
            public String getTitle( Node container )
            {
                return parser.parse( container );
            }
        } );
    }

    public void relationshipTitle( String pattern )
    {
        final PatternParser parser = new PatternParser( pattern );
        styles.add( new StyleParameter.RelationshipTitle()
        {
            public String getTitle( Relationship container )
            {
                return parser.parse( container );
            }
        } );
    }

    public void nodePropertyFilter( String nodeProperties )
    {
        final String nodePropertiesString = nodeProperties;
        styles.add( new StyleParameter.NodePropertyFilter()
        {
            public boolean acceptProperty( String key )
            {
                return Arrays.asList( nodePropertiesString.split( "," ) ).contains( key );
            }
        } );
    }

    public void reverseOrder( String... typeNames )
    {
        if (typeNames== null || typeNames.length == 0) return;
        RelationshipType[] types = new RelationshipType[typeNames.length];
        for ( int i = 0; i < typeNames.length; i++ )
        {
            types[i] = RelationshipType.withName( typeNames[i] );
        }
        styles.add( new StyleParameter.ReverseOrderRelationshipTypes( types ) );
    }

    private static class PatternParser
    {
        private final String pattern;

        PatternParser( String pattern )
        {
            this.pattern = pattern;

        }

        String parse( Entity entity )
        {
            StringBuilder result = new StringBuilder();
            for ( int pos = 0; pos < pattern.length(); )
            {
                char cur = pattern.charAt( pos++ );
                if ( cur == '@' )
                {
                    String key = untilNonAlfa( pos );
                    result.append( getSpecial( key, entity ) );
                    pos += key.length();
                }
                else if ( cur == '$' )
                {
                    String key;
                    if ( pattern.charAt( pos ) == '{' )
                    {
                        key = pattern.substring( ++pos, pattern.indexOf( '}',
                                pos++ ) );
                    }
                    else
                    {
                        key = untilNonAlfa( pos );
                    }
                    pos += pattern.length();
                    result.append( entity.getProperty( key ) );
                }
                else if ( cur == '\\' )
                {
                    result.append( pattern.charAt( pos++ ) );
                }
                else
                {
                    result.append( cur );
                }
            }
            return result.toString();
        }

        private String untilNonAlfa( int start )
        {
            int end = start;
            while ( end < pattern.length() && Character.isLetter( pattern.charAt( end ) ) )
            {
                end++;
            }
            return pattern.substring( start, end );
        }

        private String getSpecial( String attribute, Entity container )
        {
            if ( attribute.equals( "id" ) )
            {
                return "" + container.getId();
            }
            else if ( attribute.equals( "type" ) )
            {
                if ( container instanceof Relationship )
                {
                    return ((Relationship) container).getType().name();
                }
            }
            return "@" + attribute;
        }
    }

    private static MutableList<String> getAllFormats( File configFile, String[] format )
    {
        try
        {
            var formats = Lists.mutable.of( format );
            formats.addAll( Files.readAllLines( configFile.toPath() ) );
            return formats;
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }
}
