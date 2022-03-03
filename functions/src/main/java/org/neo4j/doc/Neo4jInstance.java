/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.doc;

import com.neo4j.configuration.OnlineBackupSettings;
import com.neo4j.dbms.DatabaseStartupAwaitingListener;
import com.neo4j.dbms.api.EnterpriseDatabaseManagementServiceBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.neo4j.collection.Dependencies;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class Neo4jInstance {

    private static final Path baseDatabaseDirectory = Paths.get("target/databases");

    public DatabaseManagementService newEnterpriseInstance() throws IOException {
        Files.createDirectories( baseDatabaseDirectory );

        var databaseStartAwaitListener = DatabaseStartupAwaitingListener.createWithDefaultTimeout();
        var externalDependencies = new Dependencies();
        externalDependencies.satisfyDependency( databaseStartAwaitListener );

        DatabaseManagementService managementService =
                new EnterpriseDatabaseManagementServiceBuilder( databaseDirectory() ).setConfig(
                        Map.of( OnlineBackupSettings.online_backup_listen_address, new SocketAddress( "127.0.0.1", 0 ),
                                OnlineBackupSettings.online_backup_enabled, java.lang.Boolean.FALSE,
                                GraphDatabaseSettings.auth_enabled, true
                        ) )
                        .setExternalDependencies( externalDependencies ).build();

        databaseStartAwaitListener.await( List.of( DEFAULT_DATABASE_NAME ) );

        registerShutdownHook(managementService);
        return managementService;
    }

    public DatabaseManagementService newCommunityInstance() throws IOException {
        Files.createDirectories( baseDatabaseDirectory );
        DatabaseManagementService managementService =
                new DatabaseManagementServiceBuilder( databaseDirectory() ).setConfig( GraphDatabaseSettings.auth_enabled, true ).build();
        registerShutdownHook(managementService);
        return managementService;
    }

    private Path databaseDirectory() {
        String uniqueDbDirString = String.format("graph-db-%d", System.currentTimeMillis());
        return baseDatabaseDirectory.resolve(uniqueDbDirString);
    }

    private static void registerShutdownHook(final DatabaseManagementService managementService) {
        Runtime.getRuntime().addShutdownHook(new Thread(managementService::shutdown));
    }

}
