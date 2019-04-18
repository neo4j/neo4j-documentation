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
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.dbms.database.DatabaseManagementService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.facade.DatabaseManagementServiceFactory;
import org.neo4j.graphdb.facade.ExternalDependencies;
import org.neo4j.graphdb.facade.GraphDatabaseDependencies;
import org.neo4j.graphdb.factory.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.factory.DatabaseManagementServiceInternalBuilder;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.graphdb.mockfs.UncloseableDelegatingFileSystemAbstraction;
import org.neo4j.graphdb.security.URLAccessRule;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.extension.ExtensionFactory;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.internal.locker.StoreLocker;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.logging.internal.LogService;
import org.neo4j.logging.internal.SimpleLogService;
import org.neo4j.monitoring.Monitors;
import org.neo4j.time.SystemNanoClock;

import static org.neo4j.configuration.GraphDatabaseSettings.ephemeral;
import static org.neo4j.configuration.Settings.TRUE;
import static org.neo4j.configuration.connectors.Connector.ConnectorType.BOLT;

/**
 * Test factory for graph databases.
 * Please be aware that since it's a database it will close filesystem as part of its lifecycle.
 * If you expect your file system to be open after database is closed, use {@link UncloseableDelegatingFileSystemAbstraction}
 */
public class TestGraphDatabaseFactory extends DatabaseManagementServiceBuilder
{
    private static final File EPHEMERAL_PATH = new File( "target/test data/" + GraphDatabaseSettings.DEFAULT_DATABASE_NAME );

    public TestGraphDatabaseFactory()
    {
        super( new TestGraphDatabaseFactoryState() );
        setUserLogProvider( NullLogProvider.getInstance() );
    }

    public DatabaseManagementService newImpermanentDatabase()
    {
        return newImpermanentDatabaseBuilder().newDatabaseManagementService();
    }

    public DatabaseManagementService newImpermanentDatabase( File storeDir )
    {
        return newImpermanentDatabaseBuilder( storeDir ).newDatabaseManagementService();
    }

    public DatabaseManagementService newImpermanentDatabase( Map<Setting<?>,String> config )
    {
        DatabaseManagementServiceInternalBuilder builder = newImpermanentDatabaseBuilder();
        setConfig( config, builder );
        return builder.newDatabaseManagementService();
    }

    public DatabaseManagementService newImpermanentDatabase( File storeDir , Map<Setting<?>,String> config )
    {
        DatabaseManagementServiceInternalBuilder builder = newImpermanentDatabaseBuilder(storeDir);
        setConfig( config, builder );
        return builder.newDatabaseManagementService();
    }

    private void setConfig( Map<Setting<?>,String> config, DatabaseManagementServiceInternalBuilder builder )
    {
        for ( Map.Entry<Setting<?>,String> entry : config.entrySet() )
        {
            Setting<?> key = entry.getKey();
            String value = entry.getValue();
            builder.setConfig( key, value );
        }
    }

    public DatabaseManagementServiceInternalBuilder newImpermanentDatabaseBuilder()
    {
        return newImpermanentDatabaseBuilder( EPHEMERAL_PATH );
    }

    @Override
    protected void configure( DatabaseManagementServiceInternalBuilder builder )
    {
        // Reduce the default page cache memory size to 8 mega-bytes for test databases.
        builder.setConfig( GraphDatabaseSettings.pagecache_memory, "8m" );
        builder.setConfig( GraphDatabaseSettings.shutdown_transaction_end_timeout, "1s" );
        builder.setConfig( new BoltConnector( "bolt" ).type, BOLT.name() );
        builder.setConfig( new BoltConnector( "bolt" ).enabled, "false" );
    }

    private void configure( DatabaseManagementServiceInternalBuilder builder, File storeDir )
    {
        configure( builder );
        builder.setConfig( GraphDatabaseSettings.logs_directory, new File( storeDir, "logs" ).getAbsolutePath() );
    }

    @Override
    protected TestGraphDatabaseFactoryState getCurrentState()
    {
        return (TestGraphDatabaseFactoryState) super.getCurrentState();
    }

    @Override
    protected TestGraphDatabaseFactoryState getStateCopy()
    {
        return new TestGraphDatabaseFactoryState( getCurrentState() );
    }

    public FileSystemAbstraction getFileSystem()
    {
        return getCurrentState().getFileSystem();
    }

    public TestGraphDatabaseFactory setFileSystem( FileSystemAbstraction fileSystem )
    {
        getCurrentState().setFileSystem( fileSystem );
        return this;
    }

    @Override
    public TestGraphDatabaseFactory setMonitors( Monitors monitors )
    {
        getCurrentState().setMonitors( monitors );
        return this;
    }

    @Override
    public TestGraphDatabaseFactory setUserLogProvider( LogProvider logProvider )
    {
        return (TestGraphDatabaseFactory) super.setUserLogProvider( logProvider );
    }

    public TestGraphDatabaseFactory setInternalLogProvider( LogProvider logProvider )
    {
        getCurrentState().setInternalLogProvider( logProvider );
        return this;
    }

    public TestGraphDatabaseFactory setClock( SystemNanoClock clock )
    {
        getCurrentState().setClock( clock );
        return this;
    }

    public TestGraphDatabaseFactory addKernelExtensions( Iterable<ExtensionFactory<?>> newKernelExtensions )
    {
        getCurrentState().addExtensions( newKernelExtensions );
        return this;
    }

    public TestGraphDatabaseFactory addExtension( ExtensionFactory<?> newExtension )
    {
        return addKernelExtensions( Collections.singletonList( newExtension ) );
    }

    public TestGraphDatabaseFactory setExtensions( Iterable<ExtensionFactory<?>> extensionFactories )
    {
        getCurrentState().setExtensions( extensionFactories );
        return this;
    }

    @Override
    public TestGraphDatabaseFactory addURLAccessRule( String protocol, URLAccessRule rule )
    {
        return (TestGraphDatabaseFactory) super.addURLAccessRule( protocol, rule );
    }

    public DatabaseManagementServiceInternalBuilder newImpermanentDatabaseBuilder( final File storeDir )
    {
        final TestGraphDatabaseFactoryState state = getStateCopy();
        DatabaseManagementServiceInternalBuilder.DatabaseCreator creator =
                createImpermanentDatabaseCreator( storeDir, state );
        TestGraphDatabaseBuilder builder = createImpermanentGraphDatabaseBuilder( creator );
        configure( builder, storeDir );
        return builder;
    }

    private TestGraphDatabaseBuilder createImpermanentGraphDatabaseBuilder(
            DatabaseManagementServiceInternalBuilder.DatabaseCreator creator )
    {
        return new TestGraphDatabaseBuilder( creator );
    }

    @Override
    protected DatabaseManagementService newEmbeddedDatabase( File storeDir, Config config, ExternalDependencies dependencies )
    {
        return new TestGraphDatabaseFacadeFactory( getCurrentState() ).newFacade( storeDir, config,
                GraphDatabaseDependencies.newDependencies( dependencies ) );
    }

    protected DatabaseManagementServiceInternalBuilder.DatabaseCreator createImpermanentDatabaseCreator( final File storeDir,
                                                                                     final TestGraphDatabaseFactoryState state )
    {
        return config -> new TestGraphDatabaseFacadeFactory( state, true ).newFacade( storeDir, config,
                GraphDatabaseDependencies.newDependencies( state.databaseDependencies() ) );
    }

    public static class TestGraphDatabaseFacadeFactory extends DatabaseManagementServiceFactory
    {
        private final TestGraphDatabaseFactoryState state;
        private final boolean impermanent;

        protected TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state, boolean impermanent )
        {
            this( state, impermanent, DatabaseInfo.COMMUNITY, CommunityEditionModule::new );
        }

        protected TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state, boolean impermanent,
                                                  DatabaseInfo databaseInfo, Function<GlobalModule,AbstractEditionModule> editionFactory )
        {
            super( databaseInfo, editionFactory );
            this.state = state;
            this.impermanent = impermanent;
        }

        TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state )
        {
            this( state, false );
        }

        @Override
        protected GlobalModule createGlobalModule( File storeDir, Config config, ExternalDependencies dependencies )
        {
            config.augment( GraphDatabaseSettings.databases_root_path, storeDir.getAbsolutePath() );
            if ( impermanent )
            {
                config.augment( ephemeral, TRUE );
                return new ImpermanentTestDatabaseGlobalModule( storeDir, config, dependencies, this.databaseInfo );
            }
            else
            {
                return new TestDatabaseGlobalModule( storeDir, config, dependencies, this.databaseInfo );
            }
        }

        class TestDatabaseGlobalModule extends GlobalModule
        {

            TestDatabaseGlobalModule( File storeDir, Config config, ExternalDependencies dependencies, DatabaseInfo databaseInfo )
            {
                super( storeDir, config, databaseInfo, dependencies );
            }

            @Override
            protected FileSystemAbstraction createFileSystemAbstraction()
            {
                FileSystemAbstraction fs = state.getFileSystem();
                if ( fs != null )
                {
                    return fs;
                }
                else
                {
                    return createNewFileSystem();
                }
            }

            protected FileSystemAbstraction createNewFileSystem()
            {
                return super.createFileSystemAbstraction();
            }

            @Override
            protected LogService createLogService( LogProvider userLogProvider )
            {
                LogProvider internalLogProvider = state.getInternalLogProvider();
                if ( internalLogProvider == null )
                {
                    if ( !impermanent )
                    {
                        return super.createLogService( userLogProvider );
                    }
                    internalLogProvider = NullLogProvider.getInstance();
                }
                return new SimpleLogService( userLogProvider, internalLogProvider );
            }

            @Override
            protected SystemNanoClock createClock()
            {
                SystemNanoClock clock = state.clock();
                return clock != null ? clock : super.createClock();
            }
        }

        private class ImpermanentTestDatabaseGlobalModule extends TestDatabaseGlobalModule
        {

            ImpermanentTestDatabaseGlobalModule( File storeDir, Config config, ExternalDependencies dependencies, DatabaseInfo databaseInfo )
            {
                super( storeDir, config, dependencies, databaseInfo );
            }

            @Override
            protected FileSystemAbstraction createNewFileSystem()
            {
                return new EphemeralFileSystemAbstraction();
            }

            @Override
            protected StoreLocker createStoreLocker()
            {
                return new StoreLocker( getFileSystem(), getStoreLayout() );
            }
        }
    }
}

