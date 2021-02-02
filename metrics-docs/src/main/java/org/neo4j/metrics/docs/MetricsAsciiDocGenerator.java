/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.metrics.docs;

import java.lang.reflect.Field;

import org.neo4j.annotations.documented.Documented;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class MetricsAsciiDocGenerator
{
    private static final String NEW_LINE = System.lineSeparator();

    public void generateDocsFor( String metricsResource, StringBuilder builder )
    {
        Class<?> clazz;
        try
        {
            clazz = Class.forName( metricsResource );
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException( "Couldn't load metrics class: ", e );
        }

        {
            Documented documented =  clazz.getAnnotation( Documented.class );
            if ( documented == null )
            {
                throw new IllegalStateException( "Missing Documented annotation on the class: " + metricsResource );
            }
            builder.append( documented.value() ).append( NEW_LINE ).append( NEW_LINE );
        }

        Field[] fields = clazz.getDeclaredFields();
        if ( existsDocumentedFields( fields ) )
        {

            builder.append( "[options=\"header\",cols=\"<1m,<4\"]" ).append( NEW_LINE );
            builder.append( "|===" ).append( NEW_LINE );
            builder.append( "|Name |Description" ).append( NEW_LINE );

            for ( Field field : fields )
            {
                documentField( builder, clazz, field );
            }

            builder.append( "|===" ).append( NEW_LINE );
            builder.append( NEW_LINE );
        }
    }

    private boolean existsDocumentedFields( Field[] fields )
    {
        for ( Field field : fields )
        {
            if ( field.isAnnotationPresent( Documented.class ) )
            {
                return true;
            }
        }
        return false;
    }

    private void documentField( StringBuilder builder, Class clazz, Field field )
    {
        Documented documented = field.getAnnotation( Documented.class );
        field.setAccessible( true );
        if ( documented != null )
        {
            String fieldValue = getStaticFieldValue( clazz, field );
            String documentedValue = escapeHtml4( "<prefix>." + fieldValue );
            builder.append( "|" ).append( documentedValue )
                    .append( "|" ).append( documented.value() )
                    .append( NEW_LINE );
        }
    }

    private <T> T getStaticFieldValue( Class clazz, Field field )
    {
        try
        {
            //noinspection unchecked
            return (T) field.get( null );
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Cannot fetch value of field " + field + " in " + clazz, e );
        }
    }

}
