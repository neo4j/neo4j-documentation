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
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.neo4j.io.fs.StoreChannel;

public class ChannelOutputStream extends OutputStream
{
    private final StoreChannel channel;
    private final ByteBuffer buffer = ByteBuffer.allocate( 8096 );

    public ChannelOutputStream( StoreChannel channel, boolean append ) throws IOException
    {
        this.channel = channel;
        if ( append )
        {
            this.channel.position( this.channel.size() );
        }
    }

    @Override
    public void write( int b ) throws IOException
    {
        buffer.clear();
        buffer.put( (byte) b );
        buffer.flip();
        channel.write( buffer );
    }

    @Override
    public void write( byte[] b ) throws IOException
    {
        write( b, 0, b.length );
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException
    {
        int written = 0;
        int index = off;
        while ( written < len )
        {
            buffer.clear();
            buffer.put( b, index + written, Math.min( len - written, buffer.capacity() ) );
            buffer.flip();
            written += channel.write( buffer );
        }
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}
