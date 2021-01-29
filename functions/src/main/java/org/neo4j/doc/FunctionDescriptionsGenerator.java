/*
 * Copyright (c) 2002-2020 "Neo4j,"
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
package org.neo4j.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import static java.lang.String.format;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class FunctionDescriptionsGenerator
{

    private final Neo4jInstance neo;
    private static final Map<String, String> referenceExceptions = Map.of( "functions-datetime-fromepoch", "functions-datetime-timestamp",
                                                                           "functions-datetime-fromepochmillis", "functions-datetime-timestamp");

    enum Category
    {
        Predicate( "These functions return either true or false for the given arguments." ),
        Scalar( "These functions return a single value." ),
        Aggregating( "These functions take multiple values as arguments, and calculate and return an aggregated value from them." ),
        List( "These functions return lists of other values.\nFurther details and examples of lists may be found in <<cypher-lists>>." ),
        Numeric( "These functions all operate on numerical expressions only, and will return an error if used on any other values." ),
        Logarithmic( "These functions all operate on numerical expressions only, and will return an error if used on any other values." ),
        Trigonometric( "These functions all operate on numerical expressions only, and will return an error if used on any other values.\n\n" +
                       "All trigonometric functions operate on radians, unless otherwise specified." ),
        String( "These functions are used to manipulate strings or to create a string representation of another value." ),
        Temporal_instant_types( "Values of the <<cypher-temporal, temporal types>> -- _Date_, _Time_, _LocalTime_, _DateTime_, and _LocalDateTime_ -- can be created manipulated using the following functions:" ),
        Temporal_duration( "Duration values of the <<cypher-temporal, temporal types>> can be created manipulated using the following functions:" ),
        Spatial( "These functions are used to specify 2D or 3D points in a geographic or cartesian Coordinate Reference System and to calculate the geodesic distance between two points." ),
        LOAD_CSV( "LOAD CSV functions can be used to get information about the file that is processed by `LOAD CSV`." );

        private final String description;

        Category( java.lang.String description )
        {
            this.description = description;
        }

        public java.lang.String description()
        {
            return description;
        }

        String asciiReference()
        {
            return name().toLowerCase().replace( "_","-" );
        }

        static Category parse( String categoryString, String functionName, String functionDescription )
        {
            if ( functionDescription.contains( "LOAD CSV" ) )
            {
                return LOAD_CSV;
            }
            else if ( categoryString.equalsIgnoreCase( "Temporal" ) )
            {
                if ( functionName.toLowerCase().contains( "duration" ) )
                {
                    return Temporal_duration;
                }
                else
                {
                    return Temporal_instant_types;
                }
            }
            else
            {
                return Category.valueOf( categoryString );
            }
        }
    }

    public FunctionDescriptionsGenerator()
    {
        this.neo = new Neo4jInstance();
    }

    public String document() throws IOException
    {
        Map<Category,List<FunctionDescription>> enterpriseFunctions = enterpriseEditionFunctions();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream( baos );

        enterpriseFunctions.keySet().stream().sorted().forEach( category -> outputCategory( enterpriseFunctions, out, category ) );
        out.flush();
        return baos.toString();
    }

    private void outputCategory( Map<Category,List<FunctionDescription>> enterpriseFunctions, PrintStream out, Category category )
    {
        String categoryRef = category.asciiReference();
        out.print( "[[header-query-functions-" + categoryRef + "]]\n" );
        out.print( "**<<query-functions-" + categoryRef + ", " + category + " functions>>**\n\n" );
        out.print( category.description() + "\n\n" );
        out.print( "[options=\"header\"]\n" );
        out.print( "|===\n" );
        out.print( "| Function | Signature | Description\n" );
        List<FunctionDescription> functionDescriptions = enterpriseFunctions.get( category );
        Collections.sort( functionDescriptions );
        String lastName = null;
        for ( FunctionDescription f : functionDescriptions )
        {
            boolean isFirstRow = lastName == null || !lastName.equals( f.name );
            if (isFirstRow) {
                long sameNameCount = functionDescriptions.stream()
                                                         .filter( fd -> fd.name.equals( f.name ) )
                                                         .count();
                out.print( f.firstRow( (int) sameNameCount ) );
            } else {
                out.print( f.row() );
            }
            lastName = f.name;
        }
        out.print( "|===\n\n" );
    }

    private Map<Category,List<FunctionDescription>> enterpriseEditionFunctions() throws IOException
    {
        DatabaseManagementService managementService = neo.newEnterpriseInstance();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );
        Map<Category,List<FunctionDescription>> functions = functions( db );
        managementService.shutdown();
        return functions;
    }

    private Map<Category,List<FunctionDescription>> functions( GraphDatabaseService db )
    {
        Map<Category,List<FunctionDescription>> functions;
        try ( Transaction tx = db.beginTx() )
        {
            String query = "CALL dbms.functions()";
            try ( Result result = tx.execute( query ) )
            {
                functions = parseResult( result );
            }
        }
        return functions;
    }

    private Map<Category,List<FunctionDescription>> parseResult( Result result )
    {
        Map<Category,List<FunctionDescription>> functions = new HashMap<>();
        result.stream().forEach( row ->
                                 {
                                     FunctionDescription p = new FunctionDescription( row );
                                     if ( !functions.containsKey( p.category() ) )
                                     {
                                         functions.put( p.category(), new ArrayList<>() );
                                     }
                                     functions.get( p.category() ).add( p );
                                 } );

        return functions;
    }

    static class FunctionDescription implements Comparable<FunctionDescription>
    {
        private String name;
        private final String description;
        private final Category category;
        private final String signature;

        FunctionDescription( Map<String,Object> row )
        {
            setName( (String) row.get( "name" ) );
            this.description = (String) row.get( "description" );
            this.category = Category.parse( (String) row.get( "category" ), this.name, this.description );
            this.signature = asciiDocFriendly( (String) row.get( "signature" ) );
        }

        void setName( String name )
        {
            this.name = asciiDocFriendly( (name.endsWith( "()" ) ? name : name + "()").replace( '_', ' ' ) );
        }

        Category category()
        {
            return category;
        }

        String signature()
        {
            return signature;
        }

        private String asciiDocFriendly( String str )
        {
            return str.replace( " | ", " \\| " );
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            FunctionDescription procedure = (FunctionDescription) o;

            if ( !name.equals( procedure.name ) )
            {
                return false;
            }
            return signature.equals( procedure.signature );
        }

        @Override
        public int hashCode()
        {
            int result = name.hashCode();
            result = 31 * result + signature.hashCode();
            return result;
        }

        public String row()
        {
            return format( "| %s | %s%n", signature, description );
        }

        public String firstRow( int sameNameCount )
        {
            String referenceName = "functions-" + name.toLowerCase().replace( "()", "" ).replace( '.', '-' );
            if (referenceExceptions.containsKey( referenceName )) {
                referenceName = referenceExceptions.get( referenceName );
            }
            return format( "1.%s+| <<%s,%s>>  | %s | %s%n",
                           sameNameCount, referenceName, name, signature, description );
        }

        @Override
        public String toString()
        {
            return "Function{" +
                   "name='" + name + '\'' +
                   ", signature='" + signature + '\'' +
                   ", description='" + description + "'}";
        }

        @Override
        public int compareTo( FunctionDescription f )
        {
            return this.signature().compareTo( f.signature() );
        }
    }
}
