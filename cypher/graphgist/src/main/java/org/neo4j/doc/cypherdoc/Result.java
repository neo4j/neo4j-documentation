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
package org.neo4j.doc.cypherdoc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.internal.result.string.ResultStringBuilder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

class Result
{
    final String query;
    final String text;
    final String profile;
    final Set<Long> nodeIds = new HashSet<>();
    final Set<Long> relationshipIds = new HashSet<>();

    public Result( String query, org.neo4j.graphdb.Result result )
    {
        this.query = query;
        try
        {
            ResultVisitor visitor = new ResultVisitor( result.columns() );
            result.accept( visitor );
            text = visitor.resultStringBuilder.result( result.getQueryStatistics() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

        String profileText;
        try
        {
            profileText = result.getExecutionPlanDescription().toString();
        }
        catch ( Exception ex )
        {
            profileText = ex.getMessage();
        }
        profile = profileText;
    }

    public Result( String query, String text )
    {
        this.query = query;
        this.text = text;
        this.profile = "";
    }

    class ResultVisitor implements org.neo4j.graphdb.Result.ResultVisitor<Exception> {

        final String[] columns;
        final ResultStringBuilder resultStringBuilder;

        ResultVisitor( List<String> columns )
        {
            this.columns = columns.toArray( new String[0] );
            this.resultStringBuilder = ResultStringBuilder.apply( this.columns );
        }

        @Override
        public boolean visit( org.neo4j.graphdb.Result.ResultRow row )
        {
            for ( String column : columns )
            {
                extractEntityIds( row.get( column ) );
            }
            return resultStringBuilder.visit( row );
        }

        private void extractEntityIds( Object item )
        {
            if ( item instanceof Node )
            {
                Node node = (Node) item;
                nodeIds.add( node.getId() );
            }
            else if ( item instanceof Relationship )
            {
                Relationship relationship = (Relationship) item;
                relationshipIds.add( relationship.getId() );
                nodeIds.add( relationship.getStartNode().getId() );
                nodeIds.add( relationship.getEndNode().getId() );
            }
            else if ( item instanceof Path )
            {
                Path path = (Path) item;
                for ( Node node : path.nodes() )
                {
                    nodeIds.add( node.getId() );
                }
                for ( Relationship relationship : path.relationships() )
                {
                    relationshipIds.add( relationship.getId() );
                }
            }
            else if ( item instanceof Map<?,?> )
            {
                extractEntityIds( ((Map<?,?>) item).values().iterator() );
            }
            else if ( item instanceof Iterable<?> )
            {
                extractEntityIds( ((Iterable<?>) item).iterator() );
            }
        }
    }
}
