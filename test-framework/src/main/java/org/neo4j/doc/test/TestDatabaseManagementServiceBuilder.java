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
import java.util.function.Predicate;

import org.neo4j.common.DependencyResolver;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.dbms.database.DatabaseManagementService;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.facade.DatabaseManagementServiceFactory;
import org.neo4j.graphdb.facade.ExternalDependencies;
import org.neo4j.graphdb.facade.GraphDatabaseDependencies;
import org.neo4j.graphdb.factory.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.graphdb.mockfs.UncloseableDelegatingFileSystemAbstraction;
import org.neo4j.graphdb.security.URLAccessRule;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.extension.ExtensionFactory;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.impl.index.schema.AbstractIndexProviderFactory;
import org.neo4j.kernel.internal.locker.StoreLocker;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.logging.internal.LogService;
import org.neo4j.logging.internal.SimpleLogService;
import org.neo4j.monitoring.Monitors;
import org.neo4j.time.SystemNanoClock;
import org.neo4j.util.Preconditions;

import static org.neo4j.configuration.GraphDatabaseSettings.ephemeral;
import static org.neo4j.configuration.Settings.TRUE;
import static org.neo4j.configuration.connectors.Connector.ConnectorType.BOLT;

/**
 * Test factory for graph databases.
 * Please be aware that since it's a database it will close filesystem as part of its lifecycle.
 * If you expect your file system to be open after database is closed, use {@link UncloseableDelegatingFileSystemAbstraction}
 */
public class TestDatabaseManagementServiceBuilder extends DatabaseManagementServiceBuilder
{
    private static final File EPHEMERAL_PATH = new File( "target/test data/" + GraphDatabaseSettings.DEFAULT_DATABASE_NAME );
    public static final Predicate<ExtensionFactory<?>> INDEX_PROVIDERS_FILTER = extension -> extension instanceof AbstractIndexProviderFactory;

    protected FileSystemAbstraction fileSystem;
    protected LogProvider internalLogProvider;
    protected SystemNanoClock clock;
    protected boolean impermanent;

    public TestDatabaseManagementServiceBuilder()
    {
        this( null );
        setUserLogProvider( NullLogProvider.getInstance() );
    }

    public TestDatabaseManagementServiceBuilder( File databaseRootDir )
    {
        super( databaseRootDir );
        setUserLogProvider( NullLogProvider.getInstance() );
    }

    @Override
    protected DatabaseManagementService newDatabaseManagementService( File storeDir, Config config, ExternalDependencies dependencies )
    {
        Preconditions.checkArgument( storeDir != null || impermanent, "Database must have a root path or be impermanent." );
        return new TestDatabaseManagementServiceFactory( getDatabaseInfo(), getEditionFactory(), impermanent, fileSystem, clock, internalLogProvider )
                .build( storeDir, augmentConfig( config ), GraphDatabaseDependencies.newDependencies( dependencies ) );
    }

    @Override
    protected Config augmentConfig( Config config )
    {
        config.augmentDefaults( GraphDatabaseSettings.pagecache_memory, "8m" );
        config.augmentDefaults( new BoltConnector( "bolt" ).type, BOLT.name() );
        config.augmentDefaults( new BoltConnector( "bolt" ).enabled, "false" );
        return config;
    }

    public FileSystemAbstraction getFileSystem()
    {
        return fileSystem;
    }

    public TestDatabaseManagementServiceBuilder setFileSystem( FileSystemAbstraction fileSystem )
    {
        this.fileSystem = fileSystem;
        return this;
    }

    public TestDatabaseManagementServiceBuilder setDatabaseRootDirectory( File storeDir )
    {
        this.databaseRootDir = storeDir;
        return this;
    }

    public TestDatabaseManagementServiceBuilder setInternalLogProvider( LogProvider internalLogProvider )
    {
        this.internalLogProvider = internalLogProvider;
        return this;
    }

    public TestDatabaseManagementServiceBuilder setClock( SystemNanoClock clock )
    {
        this.clock = clock;
        return this;
    }

    private TestDatabaseManagementServiceBuilder addExtensions( Iterable<ExtensionFactory<?>> extensions )
    {
        for ( ExtensionFactory<?> extension : extensions )
        {
            this.extensions.add( extension );
        }
        return this;
    }

    public TestDatabaseManagementServiceBuilder addExtension( ExtensionFactory<?> extension )
    {
        return addExtensions( Collections.singletonList( extension ) );
    }

    public TestDatabaseManagementServiceBuilder setExtensions( Iterable<ExtensionFactory<?>> newExtensions )
    {
        extensions.clear();
        addExtensions( newExtensions );
        return this;
    }

    public TestDatabaseManagementServiceBuilder removeExtensions( Predicate<ExtensionFactory<?>> toRemove )
    {
        extensions.removeIf( toRemove );
        return this;
    }

    public TestDatabaseManagementServiceBuilder impermanent()
    {
        impermanent = true;
        if ( databaseRootDir == null )
        {
            databaseRootDir = EPHEMERAL_PATH;
        }
        return this;
    }

    // Override to allow chaining

    @Override
    public TestDatabaseManagementServiceBuilder setExternalDependencies( DependencyResolver dependencies )
    {
        return (TestDatabaseManagementServiceBuilder) super.setExternalDependencies( dependencies );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setMonitors( Monitors monitors )
    {
        return (TestDatabaseManagementServiceBuilder) super.setMonitors( monitors );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setUserLogProvider( LogProvider logProvider )
    {
        return (TestDatabaseManagementServiceBuilder) super.setUserLogProvider( logProvider );
    }

    @Override
    public TestDatabaseManagementServiceBuilder addURLAccessRule( String protocol, URLAccessRule rule )
    {
        return (TestDatabaseManagementServiceBuilder) super.addURLAccessRule( protocol, rule );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setConfig( String name, String value )
    {
        return (TestDatabaseManagementServiceBuilder) super.setConfig( name, value );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setConfig( Setting<?> setting, String value )
    {
        return (TestDatabaseManagementServiceBuilder) super.setConfig( setting, value );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setConfig( Map<Setting<?>,String> config )
    {
        return (TestDatabaseManagementServiceBuilder) super.setConfig( config );
    }

    @Override
    public TestDatabaseManagementServiceBuilder setConfigRaw( Map<String,String> config )
    {
        return (TestDatabaseManagementServiceBuilder) super.setConfigRaw( config );
    }

    public static class TestDatabaseManagementServiceFactory extends DatabaseManagementServiceFactory
    {
        private final boolean impermanent;
        private FileSystemAbstraction fs;
        private LogProvider internalLogProvider;
        private SystemNanoClock clock;

        protected TestDatabaseManagementServiceFactory( boolean impermanent )
        {
            this( impermanent, DatabaseInfo.COMMUNITY, CommunityEditionModule::new );
        }

        protected TestDatabaseManagementServiceFactory( boolean impermanent, DatabaseInfo databaseInfo, Function<GlobalModule,AbstractEditionModule> editionFactory )
        {
            super( databaseInfo, editionFactory );
            this.impermanent = impermanent;
        }

        public TestDatabaseManagementServiceFactory( DatabaseInfo databaseInfo, Function<GlobalModule, AbstractEditionModule> editionFactory, boolean impermanent, FileSystemAbstraction fileSystem, SystemNanoClock clock, LogProvider internalLogProvider )
        {
            super( databaseInfo, editionFactory );
            this.impermanent = impermanent;
            this.fs = fileSystem;
            this.clock = clock;
            this.internalLogProvider = internalLogProvider;
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
