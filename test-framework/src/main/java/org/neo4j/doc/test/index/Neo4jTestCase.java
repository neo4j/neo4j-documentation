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
package org.neo4j.doc.test.index;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public abstract class Neo4jTestCase
{
    private static GraphDatabaseService graphDb;
    private Transaction tx;
    private static DatabaseManagementService managementService;
    private static File folder;

    @BeforeClass
    public static void setUpDb()
    {
        folder = new File( "target/example-db" + System.nanoTime() );
        managementService = new DatabaseManagementServiceBuilder( folder ).build();
        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
    }

    @AfterClass
    public static void tearDownDb() throws IOException
    {
        managementService.shutdown();
        FileUtils.deleteDirectory( folder );
    }

    @Before
    public void setUpTest()
    {
        tx = graphDb.beginTx();
    }

    @After
    public void tearDownTest()
    {
        if ( !manageMyOwnTxFinish() )
        {
            finishTx( true );
        }
    }

    protected boolean manageMyOwnTxFinish()
    {
        return false;
    }

    protected void finishTx( boolean commit )
    {
        if ( tx == null )
        {
            return;
        }

        if ( commit )
        {
            tx.commit();
        }
        tx.close();
        tx = null;
    }

    protected Transaction beginTx()
    {
        if ( tx == null )
        {
            tx = graphDb.beginTx();
        }
        return tx;
    }

    public static void deleteFileOrDirectory( File file )
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            for ( File child : Objects.requireNonNull( file.listFiles() ) )
            {
                deleteFileOrDirectory( child );
            }
        }
        assertTrue( "delete " + file, file.delete() );
    }

    protected static GraphDatabaseService graphDb()
    {
        return graphDb;
    }

    public static <T> void assertContains( Collection<T> collection,
                                           T... expectedItems )
    {
        String collectionString = join( ", ", collection.toArray() );
        assertEquals( collectionString, expectedItems.length,
                collection.size() );
        for ( T item : expectedItems )
        {
            assertTrue( collection.contains( item ) );
        }
    }

    public static <T> void assertContains( Iterable<T> items, T... expectedItems )
    {
        assertContains( asCollection( items ), expectedItems );
    }

    public static <T> void assertContainsInOrder( Collection<T> collection,
                                                  T... expectedItems )
    {
        String collectionString = join( ", ", collection.toArray() );
        assertEquals( collectionString, expectedItems.length, collection.size() );
        Iterator<T> itr = collection.iterator();
        for ( int i = 0; itr.hasNext(); i++ )
        {
            assertEquals( expectedItems[i], itr.next() );
        }
    }

    public static <T> void assertContainsInOrder( Iterable<T> collection,
                                                  T... expectedItems )
    {
        assertContainsInOrder( asCollection( collection ), expectedItems );
    }

    public static <T> Collection<T> asCollection( Iterable<T> iterable )
    {
        List<T> list = new ArrayList<>();
        for ( T item : iterable )
        {
            list.add( item );
        }
        return list;
    }

    public static <T> String join( String delimiter, T... items )
    {
        StringBuilder buffer = new StringBuilder();
        for ( T item : items )
        {
            if ( buffer.length() > 0 )
            {
                buffer.append( delimiter );
            }
            buffer.append( item.toString() );
        }
        return buffer.toString();
    }
}
