/*
 * Copyright (c) 2002-2019 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

}
