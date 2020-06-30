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
package org.neo4j.cypher.example;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.io.fs.FileUtils.deletePathRecursively;

public class JavaQuery
{
    private static final Path databaseDirectory = Path.of( "target/java-query-db" );
    String resultString;
    String columnsString;
    String nodeResult;
    String rows = "";

    public static void main( String[] args )
    {
        JavaQuery javaQuery = new JavaQuery();
        javaQuery.run();
    }

    void run()
    {
        clearDbPath();

        // tag::addData[]
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder( databaseDirectory ).build();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );

        try ( Transaction tx = db.beginTx())
        {
            Node myNode = tx.createNode();
            myNode.setProperty( "name", "my node" );
            tx.commit();
        }
        // end::addData[]

        // tag::execute[]
        try ( Transaction tx = db.beginTx();
              Result result = tx.execute( "MATCH (n {name: 'my node'}) RETURN n, n.name" ) )
        {
            while ( result.hasNext() )
            {
                Map<String,Object> row = result.next();
                for ( Entry<String,Object> column : row.entrySet() )
                {
                    rows += column.getKey() + ": " + column.getValue() + "; ";
                }
                rows += "\n";
            }
        }
        // end::execute[]
        // the result is now empty, get a new one
        try ( Transaction tx = db.beginTx();
              Result result = tx.execute( "MATCH (n {name: 'my node'}) RETURN n, n.name" ) )
        {
            // tag::items[]
            Iterator<Node> n_column = result.columnAs( "n" );
            n_column.forEachRemaining( node -> nodeResult = node + ": " + node.getProperty( "name" ) );
            // end::items[]

            // tag::columns[]
            List<String> columns = result.columns();
            // end::columns[]
            columnsString = columns.toString();
            resultString = tx.execute( "MATCH (n {name: 'my node'}) RETURN n, n.name" ).resultAsString();
        }

        managementService.shutdown();
    }

    private void clearDbPath()
    {
        try
        {
            deletePathRecursively( databaseDirectory );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }
}
