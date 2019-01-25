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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.graphdb.facade.ExternalDependencies;
import org.neo4j.graphdb.facade.GraphDatabaseFacadeFactory;
import org.neo4j.graphdb.facade.embedded.EmbeddedGraphDatabase;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.helpers.Service;
import org.neo4j.helpers.collection.Iterables;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.ExtensionFactory;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.internal.locker.StoreLocker;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.logging.internal.LogService;
import org.neo4j.logging.internal.SimpleLogService;

import static org.neo4j.graphdb.facade.GraphDatabaseDependencies.newDependencies;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.ephemeral;
import static org.neo4j.graphdb.factory.GraphDatabaseSettings.pagecache_memory;
import static org.neo4j.kernel.configuration.Settings.TRUE;

/**
 * A database meant to be used in unit tests. It will always be empty on start.
 */
public class ImpermanentGraphDatabase extends EmbeddedGraphDatabase
{
    /**
     * If enabled will track unclosed database instances in tests. The place of instantiation
     * will get printed in an exception with the message "Unclosed database instance".
     */
    private static final boolean TRACK_UNCLOSED_DATABASE_INSTANCES = false;
    private static final Map<File, Exception> startedButNotYetClosed = new ConcurrentHashMap<>();

    protected static final File PATH = new File( "target/test-data/impermanent-db" );

    /**
     * This is deprecated. Use {@link TestGraphDatabaseFactory} instead
     */
    @Deprecated
    public ImpermanentGraphDatabase()
    {
        this( new HashMap<>() );
    }

    /*
     * TODO this shouldn't be here. It so happens however that some tests may use the database
     * directory as the path to store stuff and in this case we need to define the path explicitly,
     * otherwise we end up writing outside the workspace and hence leave stuff behind.
     * The other option is to explicitly remove all files present on startup. Either way,
     * the fact that this discussion takes place is indication that things are inconsistent,
     * since an ImpermanentGraphDatabase should not have any mention of a store directory in
     * any case.
     */
    public ImpermanentGraphDatabase( File storeDir )
    {
        this( storeDir, new HashMap<>() );
    }

    /**
     * This is deprecated. Use {@link TestGraphDatabaseFactory} instead
     */
    @Deprecated
    public ImpermanentGraphDatabase( Map<String, String> params )
    {
        this( PATH, params );
    }

    /**
     * This is deprecated. Use {@link TestGraphDatabaseFactory} instead
     */
    @Deprecated
    public ImpermanentGraphDatabase( File storeDir, Map<String, String> params )
    {
        this( storeDir, params,
                Iterables.cast( Service.load( ExtensionFactory.class ) ) );
    }

    /**
     * This is deprecated. Use {@link TestGraphDatabaseFactory} instead
     */
    @Deprecated
    public ImpermanentGraphDatabase( Map<String, String> params,
                                     Iterable<ExtensionFactory<?>> extensionFactories )
    {
        this( PATH, params, extensionFactories );
    }

    /**
     * This is deprecated. Use {@link TestGraphDatabaseFactory} instead
     */
    @Deprecated
    public ImpermanentGraphDatabase( File storeDir, Map<String, String> params,
                                     Iterable<ExtensionFactory<?>> extensionFactories )
    {
        this( storeDir, params, getDependencies( extensionFactories ) );
    }

    private static ExternalDependencies getDependencies( Iterable<ExtensionFactory<?>> extensionFactories )
    {
        return newDependencies().extensions( extensionFactories );
    }

    public ImpermanentGraphDatabase( File storeDir, Map<String, String> params, ExternalDependencies dependencies )
    {
        super( storeDir, params, dependencies );
        trackUnclosedUse( storeDir );
    }

    public ImpermanentGraphDatabase( File storeDir, Config config,
                                     ExternalDependencies dependencies )
    {
        super( storeDir, config, dependencies );
        trackUnclosedUse( storeDir );
    }

    @Override
    protected void create( File storeDir, Map<String, String> params, ExternalDependencies dependencies )
    {
        new GraphDatabaseFacadeFactory( DatabaseInfo.COMMUNITY, CommunityEditionModule::new )
        {
            @Override
            protected GlobalModule createGlobalPlatform( File storeDir, Config config, ExternalDependencies dependencies )
            {
                return new ImpermanentGlobalModule( storeDir, config, databaseInfo, dependencies );
            }
        }.initFacade( storeDir, params, dependencies, this );
    }

    private void trackUnclosedUse( File storeDir )
    {
        if ( TRACK_UNCLOSED_DATABASE_INSTANCES )
        {
            Exception testThatDidNotCloseDb = startedButNotYetClosed.put( storeDir,
                    new Exception( "Unclosed database instance" ) );
            if ( testThatDidNotCloseDb != null )
            {
                testThatDidNotCloseDb.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown()
    {
        if ( TRACK_UNCLOSED_DATABASE_INSTANCES )
        {
            startedButNotYetClosed.remove( databaseLayout() );
        }

        super.shutdown();
    }

    private static Config withForcedInMemoryConfiguration( Config config )
    {
        config.augment( ephemeral, TRUE );
        config.augmentDefaults( pagecache_memory, "8M" );
        return config;
    }

    protected static class ImpermanentGlobalModule extends GlobalModule
    {
        public ImpermanentGlobalModule( File storeDir, Config config, DatabaseInfo databaseInfo, ExternalDependencies dependencies )
        {
            super( storeDir, withForcedInMemoryConfiguration(config), databaseInfo, dependencies );
        }

        @Override
        protected StoreLocker createStoreLocker()
        {
            return new StoreLocker( getFileSystem(), getStoreLayout() );
        }

        @Override
        protected FileSystemAbstraction createFileSystemAbstraction()
        {
            return new EphemeralFileSystemAbstraction();
        }

        @Override
        protected LogService createLogService( LogProvider userLogProvider )
        {
            return new SimpleLogService( NullLogProvider.getInstance(), NullLogProvider.getInstance() );
        }
    }
}

