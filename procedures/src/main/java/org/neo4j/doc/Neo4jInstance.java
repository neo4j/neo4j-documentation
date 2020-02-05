/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.EnterpriseGraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Neo4jInstance {

    private static final Path baseDatabaseDirectory = Paths.get("target/databases");

    public GraphDatabaseService newEnterpriseInstance() {
        baseDatabaseDirectory.toFile().mkdirs();
        GraphDatabaseService graphDb = new EnterpriseGraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(databaseDirectory())
                .setConfig(GraphDatabaseSettings.auth_enabled, "true")
                .newGraphDatabase();
        registerShutdownHook(graphDb);
        return graphDb;
    }

    public GraphDatabaseService newCommunityInstance() {
        boolean mkdirs = baseDatabaseDirectory.toFile().mkdirs();
        GraphDatabaseService graphDb = new GraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder(databaseDirectory())
                .setConfig(GraphDatabaseSettings.auth_enabled, "true")
                .newGraphDatabase();
        registerShutdownHook(graphDb);
        return graphDb;
    }

    private File databaseDirectory() {
        String uniqueDbDirString = String.format("graph-db-%d", System.currentTimeMillis());
        return baseDatabaseDirectory.resolve(uniqueDbDirString).toFile();
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread(graphDb::shutdown));
    }

}
