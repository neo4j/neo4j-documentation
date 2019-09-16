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
package org.neo4j.doc;

import com.neo4j.enterprise.edition.factory.EnterpriseDatabaseManagementServiceBuilder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;

public class Neo4jInstance {

    private static final Path baseDatabaseDirectory = Paths.get("target/databases");

    public DatabaseManagementService newEnterpriseInstance() {
        baseDatabaseDirectory.toFile().mkdirs();
        DatabaseManagementService managementService =
                new EnterpriseDatabaseManagementServiceBuilder( databaseDirectory() ).setConfig( GraphDatabaseSettings.auth_enabled, true ).build();
        registerShutdownHook(managementService);
        return managementService;
    }

    public DatabaseManagementService newCommunityInstance() {
        boolean mkdirs = baseDatabaseDirectory.toFile().mkdirs();
        DatabaseManagementService managementService =
                new DatabaseManagementServiceBuilder( databaseDirectory() ).setConfig( GraphDatabaseSettings.auth_enabled, true ).build();
        registerShutdownHook(managementService);
        return managementService;
    }

    private File databaseDirectory() {
        String uniqueDbDirString = String.format("graph-db-%d", System.currentTimeMillis());
        return baseDatabaseDirectory.resolve(uniqueDbDirString).toFile();
    }

    private static void registerShutdownHook(final DatabaseManagementService managementService) {
        Runtime.getRuntime().addShutdownHook(new Thread(managementService::shutdown));
    }

}
