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
package org.neo4j.doc.server;

import java.io.IOException;

import org.neo4j.server.NeoServer;
import org.neo4j.doc.server.helpers.CommunityServerBuilder;
import org.neo4j.doc.server.helpers.ServerHelper;

public final class ServerHolder extends Thread
{
    private static AssertionError allocation;
    private static NeoServer server;
    private static CommunityServerBuilder builder;

    static synchronized NeoServer allocate() throws IOException
    {
        if ( allocation != null ) throw allocation;
        if ( server == null ) server = startServer();
        allocation = new AssertionError( "The server was allocated from here but not released properly" );
        return server;
    }

    static synchronized void release( NeoServer server )
    {
        if ( server == null ) return;
        if ( server != ServerHolder.server )
            throw new AssertionError( "trying to suspend a server not allocated from here" );
        if ( allocation == null ) throw new AssertionError( "releasing the server although it is not allocated" );
        allocation = null;
    }

    static synchronized void ensureNotRunning()
    {
        if ( allocation != null ) throw allocation;
        shutdown();
    }

    static synchronized void setServerBuilderProperty( String key, String value )
    {
        initBuilder();
        builder = builder.withProperty( key, value );
    }

    private static NeoServer startServer() throws IOException
    {
        initBuilder();
        return ServerHelper.createNonPersistentServer( builder );
    }

    private static synchronized void shutdown()
    {
        allocation = null;
        try
        {
            if ( server != null ) server.stop();
        }
        finally
        {
            builder = null;
            server = null;
        }
    }

    private static void initBuilder()
    {
        if ( builder == null )
        {
            builder = CommunityServerBuilder.server();
        }
    }

    @Override
    public void run()
    {
        shutdown();
    }

    static
    {
        Runtime.getRuntime().addShutdownHook( new ServerHolder() );
    }

    private ServerHolder()
    {
        super( ServerHolder.class.getName() );
    }
}
