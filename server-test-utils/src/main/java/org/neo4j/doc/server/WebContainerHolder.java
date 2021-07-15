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

import java.io.File;
import java.nio.file.Files;

import org.neo4j.doc.server.helpers.CommunityWebContainerBuilder;
import org.neo4j.doc.server.helpers.TestWebContainer;

import static org.neo4j.doc.server.helpers.WebContainerHelper.createContainer;

public final class WebContainerHolder extends Thread
{
    private static AssertionError allocation;
    private static TestWebContainer testWebContainer;
    private static CommunityWebContainerBuilder builder;

    static synchronized TestWebContainer allocate( boolean onRandomPorts ) throws Exception
    {
        if ( allocation != null )
        {
            throw allocation;
        }
        if ( testWebContainer == null )
        {
            testWebContainer = startServer( Files.createTempDirectory( "webcontainer" ).toFile(), onRandomPorts );
        }
        allocation = new AssertionError( "The server was allocated from here but not released properly" );
        return testWebContainer;
    }

    static synchronized void release( TestWebContainer server )
    {
        if ( server == null )
        {
            return;
        }
        if ( server != WebContainerHolder.testWebContainer )
        {
            throw new AssertionError( "trying to suspend a server not allocated from here" );
        }
        if ( allocation == null )
        {
            throw new AssertionError( "releasing the server although it is not allocated" );
        }
        allocation = null;
    }

    static synchronized void ensureNotRunning()
    {
        if ( allocation != null )
        {
            throw allocation;
        }
        shutdown();
    }

    static synchronized void setWebContainerBuilderProperty( String key, String value )
    {
        initBuilder();
        builder = builder.withProperty( key, value );
    }

    private static TestWebContainer startServer( File path, boolean onRandomPorts ) throws Exception
    {
        initBuilder();
        return createContainer( builder, path, onRandomPorts );
    }

    private static synchronized void shutdown()
    {
        allocation = null;
        try
        {
            if ( testWebContainer != null )
            {
                testWebContainer.shutdown();
            }
        }
        finally
        {
            builder = null;
            testWebContainer = null;
        }
    }

    private static void initBuilder()
    {
        if ( builder == null )
        {
            builder = CommunityWebContainerBuilder.serverOnRandomPorts();
        }
    }

    @Override
    public void run()
    {
        shutdown();
    }
}
