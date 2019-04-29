/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.doc.test;

import com.neo4j.commercial.edition.CommercialEditionModule;
import com.neo4j.kernel.impl.enterprise.configuration.OnlineBackupSettings;

import java.io.File;
import java.util.function.Function;

import org.neo4j.common.Edition;
import org.neo4j.configuration.Config;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.kernel.impl.factory.DatabaseInfo;

import static org.neo4j.configuration.Settings.FALSE;

/**
 * Factory for test graph database.
 */
public class TestEnterpriseGraphDatabaseFactory extends TestDatabaseManagementServiceBuilder
{
    public TestEnterpriseGraphDatabaseFactory()
    {
        super();
    }

    public TestEnterpriseGraphDatabaseFactory( File databaseRootDir )
    {
        super( databaseRootDir );
    }

    @Override
    protected Config augmentConfig( Config config )
    {
        config = super.augmentConfig( config );
        config.augmentDefaults( OnlineBackupSettings.online_backup_listen_address, "127.0.0.1:0" );
        config.augmentDefaults( OnlineBackupSettings.online_backup_enabled, FALSE );
        return config;
    }

    @Override
    protected DatabaseInfo getDatabaseInfo()
    {
        return DatabaseInfo.COMMERCIAL;
    }

    @Override
    protected Function<GlobalModule,AbstractEditionModule> getEditionFactory()
    {
        return CommercialEditionModule::new;
    }

    @Override
    public String getEdition()
    {
        return Edition.COMMERCIAL.toString();
    }
}

