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
package org.neo4j.kernel.impl.store;

import org.junit.Rule;
import org.junit.Test;

import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.impl.store.format.standard.DynamicRecordFormat;
import org.neo4j.kernel.impl.store.format.standard.NodeRecordFormat;
import org.neo4j.kernel.impl.store.format.standard.PropertyRecordFormat;
import org.neo4j.kernel.impl.store.format.standard.RelationshipRecordFormat;
import org.neo4j.test.docs.DocsIncludeFile;

import static java.util.Arrays.asList;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.array_block_size;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.string_block_size;
import static org.neo4j.kernel.impl.store.StoreFactory.NODE_STORE_NAME;
import static org.neo4j.kernel.impl.store.StoreFactory.PROPERTY_ARRAYS_STORE_NAME;
import static org.neo4j.kernel.impl.store.StoreFactory.PROPERTY_STORE_NAME;
import static org.neo4j.kernel.impl.store.StoreFactory.PROPERTY_STRINGS_STORE_NAME;
import static org.neo4j.kernel.impl.store.StoreFactory.RELATIONSHIP_STORE_NAME;

public class RecordSizesDocTest
{
    @Rule
    public final DocsIncludeFile writer = DocsIncludeFile.inSection( "ops" );

    @Test
    public void record_sizes_table() throws Exception
    {
        writer.println( "[options=\"header\",cols=\"<45,>20m,<35\", width=\"80%\"]" );
        writer.println( "|======================================" );
        writer.println( "| Store file  | Record size  | Contents" );
        for ( Store store : asList(
                store( NODE_STORE_NAME, NodeRecordFormat.RECORD_SIZE, "Nodes" ),
                store( RELATIONSHIP_STORE_NAME, RelationshipRecordFormat.RECORD_SIZE, "Relationships" ),
                store( PROPERTY_STORE_NAME, PropertyRecordFormat.RECORD_SIZE, "Properties for nodes and relationships" ),
                dynamicStore( PROPERTY_STRINGS_STORE_NAME, string_block_size, "Values of string properties" ),
                dynamicStore( PROPERTY_ARRAYS_STORE_NAME, array_block_size, "Values of array properties" )
        ) )
        {
            writer.printf( "| %s | %d B | %s%n", store.simpleFileName, store.recordSize, store.contentsDescription );
        }
        writer.println( "|======================================" );
        writer.println();
    }

    private static Store dynamicStore( String storeFileName, Setting<Integer> blockSizeSetting, String contentsDescription )
    {
        return store( storeFileName, defaultDynamicSize( blockSizeSetting ), contentsDescription );
    }

    private static Store store( String storeFileName, int recordSize, String contentsDescription )
    {
        return new Store( MetaDataStore.DEFAULT_NAME + storeFileName, recordSize, contentsDescription );
    }

    private static int defaultDynamicSize( Setting<Integer> setting )
    {
        return DynamicRecordFormat.RECORD_HEADER_SIZE + Integer.parseInt( setting.getDefaultValue() );
    }

    private static class Store
    {
        final String simpleFileName;
        final int recordSize;
        final String contentsDescription;

        Store( String simpleFileName, int recordSize, String contentsDescription )
        {
            this.simpleFileName = simpleFileName;
            this.recordSize = recordSize;
            this.contentsDescription = contentsDescription;
        }
    }
}
