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
package org.neo4j.doc.test;

import org.neo4j.graphdb.factory.GraphDatabaseFactoryState;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.logging.LogProvider;
import org.neo4j.time.SystemNanoClock;

public class TestGraphDatabaseFactoryState extends GraphDatabaseFactoryState
{
    private FileSystemAbstraction fileSystem;
    private LogProvider internalLogProvider;
    private SystemNanoClock clock;

    public TestGraphDatabaseFactoryState()
    {
        fileSystem = null;
        internalLogProvider = null;
    }

    public TestGraphDatabaseFactoryState( TestGraphDatabaseFactoryState previous )
    {
        super( previous );
        fileSystem = previous.fileSystem;
        internalLogProvider = previous.internalLogProvider;
        clock = previous.clock;
    }

    public FileSystemAbstraction getFileSystem()
    {
        return fileSystem;
    }

    public void setFileSystem( FileSystemAbstraction fileSystem )
    {
        this.fileSystem = fileSystem;
    }

    public LogProvider getInternalLogProvider()
    {
        return internalLogProvider;
    }

    public void setInternalLogProvider( LogProvider logProvider )
    {
        this.internalLogProvider = logProvider;
    }

    public SystemNanoClock clock()
    {
        return clock;
    }

    public void setClock( SystemNanoClock clock )
    {
        this.clock = clock;
    }
}
