/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher;

import com.neo4j.causalclustering.core.MixedEditionModule;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.common.DependencyResolver;
import org.neo4j.common.Edition;
import org.neo4j.configuration.Config;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.security.URLAccessRule;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.factory.DbmsInfo;
import org.neo4j.logging.InternalLogProvider;
import org.neo4j.logging.LogProvider;
import org.neo4j.monitoring.Monitors;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.time.SystemNanoClock;

public class TestEnterpriseDatabaseManagementServiceBuilder extends TestDatabaseManagementServiceBuilder
{
    public TestEnterpriseDatabaseManagementServiceBuilder( Path databaseRootDir )
    {
        super( databaseRootDir );
    }

    @Override
    protected Config augmentConfig( Config config )
    {
        return config;
    }

    @Override
    protected DbmsInfo getDbmsInfo( Config config )
    {
        return DbmsInfo.ENTERPRISE;
    }

    @Override
    protected Function<GlobalModule,AbstractEditionModule> getEditionFactory( Config config )
    {
        return MixedEditionModule::new;
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
    public TestEnterpriseDatabaseManagementServiceBuilder useLazyProcedures( boolean useLazyProcedures )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.useLazyProcedures( useLazyProcedures );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setFileSystem( FileSystemAbstraction fileSystem )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setFileSystem( fileSystem );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setDatabaseRootDirectory( Path storeDir )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setDatabaseRootDirectory( storeDir );
    }

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setInternalLogProvider( InternalLogProvider internalLogProvider )
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

    @Override
    public TestEnterpriseDatabaseManagementServiceBuilder setConfig( Config fromConfig )
    {
        return (TestEnterpriseDatabaseManagementServiceBuilder) super.setConfig( fromConfig );
    }
}
