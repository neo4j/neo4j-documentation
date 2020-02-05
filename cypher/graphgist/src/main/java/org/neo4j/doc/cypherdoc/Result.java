/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class Result
{
    final String query;
    final String text;
    final String profile;
    final Set<Long> nodeIds = new HashSet<>();
    final Set<Long> relationshipIds = new HashSet<>();

    public Result( String query, org.neo4j.graphdb.Result result, GraphDatabaseService graphOps )
    {
        this.query = query;
        text = result.resultAsString();
        try ( Transaction tx = graphOps.beginTx() )
        {
            extract( result );
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

    private void extract( Iterator<?> source )
    {
        while ( source.hasNext() )
        {
            Object item = source.next();
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
                extract( ((Map<?,?>) item).values().iterator() );
            }
            else if ( item instanceof Iterable<?> )
            {
                extract( ((Iterable<?>) item).iterator() );
            }
        }
    }
}
