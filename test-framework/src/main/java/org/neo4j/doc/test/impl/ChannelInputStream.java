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
package org.neo4j.doc.test.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.neo4j.io.fs.StoreChannel;

public class ChannelInputStream extends InputStream
{
    private final StoreChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate( 8096 );
    private int position;

    public ChannelInputStream( StoreChannel channel )
    {
        this.channel = channel;
    }

    @Override
    public int read() throws IOException
    {
        buffer.clear();
        buffer.limit( 1 );
        while ( buffer.hasRemaining() )
        {
            int read = channel.read( buffer );

            if ( read == -1 )
            {
                return -1;
            }
        }
        buffer.flip();
        position++;
        // Return the *unsigned* byte value as an integer
        return buffer.get() & 0x000000FF;
    }

    @Override
    public int read( byte[] b, int off, int len ) throws IOException
    {
        // TODO implement properly
        return super.read( b, off, len );
    }

    @Override
    public int available() throws IOException
    {
        return (int) (position - channel.size());
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}

