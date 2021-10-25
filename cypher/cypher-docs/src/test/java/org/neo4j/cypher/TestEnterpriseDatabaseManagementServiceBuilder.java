/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 * This file is a commercial add-on to Neo4j Enterprise Edition.
 */
package org.neo4j.cypher;

import com.neo4j.enterprise.edition.EnterpriseEditionModule;
import com.neo4j.kernel.impl.enterprise.configuration.OnlineBackupSettings;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.common.DependencyResolver;
import org.neo4j.common.Edition;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.security.URLAccessRule;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.io.layout.Neo4jLayout;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.logging.LogProvider;
import org.neo4j.monitoring.Monitors;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.time.SystemNanoClock;

public class TestEnterpriseDatabaseManagementServiceBuilder extends TestDatabaseManagementServiceBuilder
{
    public TestEnterpriseDatabaseManagementServiceBuilder( File databaseRootDir )
    {
        super( databaseRootDir );
    }

    @Override
    protected Config augmentConfig( Config config )
    {
        return config;
    }

    @Override
    protected DatabaseInfo getDatabaseInfo()
    {
        return DatabaseInfo.ENTERPRISE;
    }

    @Override
    protected Function<GlobalModule,AbstractEditionModule> getEditionFactory()
    {
        return EnterpriseEditionModule::new;
    }

    @Override
    public String getEdition()
    {
        return Edition.ENTERPRISE.toString();
    }

    // Override to allow chaining

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder impermanent()
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.impermanent();
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setFileSystem( FileSystemAbstraction fileSystem )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setFileSystem( fileSystem );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setDatabaseRootDirectory( File storeDir )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setDatabaseRootDirectory( storeDir );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setInternalLogProvider( LogProvider internalLogProvider )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setInternalLogProvider( internalLogProvider );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setClock( SystemNanoClock clock )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setClock( clock );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder noOpSystemGraphInitializer()
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.noOpSystemGraphInitializer();
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setExternalDependencies( DependencyResolver dependencies )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setExternalDependencies( dependencies );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setMonitors( Monitors monitors )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setMonitors( monitors );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setUserLogProvider( LogProvider logProvider )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setUserLogProvider( logProvider );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder addURLAccessRule( String protocol, URLAccessRule rule )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.addURLAccessRule( protocol, rule );
    }

    @Override
    public <T> TestEnterpriseDatabaseManagementServiceBuilder setConfig( Setting<T> setting, T value )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setConfig( setting, value );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setConfig( Map<Setting<?>,Object> config )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setConfig( config );
    }
}

