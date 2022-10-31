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
package org.neo4j.examples;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

import java.io.IOException;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Paths;
import org.neo4j.io.fs.FileUtils;

public class CalculateShortestPath {
    private static final java.nio.file.Path databaseDirectory = java.nio.file.Path.of("target/neo4j-shortest-path");
    private static final String NAME_KEY = "name";
    private static final Label NODE_LABEL = Label.label("NODE");
    private static final RelationshipType KNOWS = RelationshipType.withName("KNOWS");

    private static GraphDatabaseService graphDb;
    private static DatabaseManagementService managementService;

    public static void main(final String[] args) throws IOException {
        FileUtils.deleteDirectory(databaseDirectory);
        managementService = new DatabaseManagementServiceBuilder(databaseDirectory).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
        registerShutdownHook();
        try (Transaction tx = graphDb.beginTx()) {
            /*
             *  (Neo) --> (Trinity)
             *     \       ^
             *      v     /
             *    (Morpheus) --> (Cypher)
             *            \         |
             *             v        v
             *            (Agent Smith)
             */
            createChain(tx, "Neo", "Trinity");
            createChain(tx, "Neo", "Morpheus", "Trinity");
            createChain(tx, "Morpheus", "Cypher", "Agent Smith");
            createChain(tx, "Morpheus", "Agent Smith");
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            // So let's find the shortest path between Neo and Agent Smith
            Node neo = getOrCreateNode(tx, "Neo");
            Node agentSmith = getOrCreateNode(tx, "Agent Smith");
            // tag::shortestPathUsage[]
            PathFinder<Path> finder = GraphAlgoFactory.shortestPath(new BasicEvaluationContext(tx, graphDb),
                    PathExpanders.forTypeAndDirection(KNOWS, Direction.BOTH), 4);
            Path foundPath = finder.findSinglePath(neo, agentSmith);
            System.out.println("Path from Neo to Agent Smith: "
                    + Paths.simplePathToString(foundPath, NAME_KEY));
            // end::shortestPathUsage[]
        }

        System.out.println("Shutting down database ...");
        managementService.shutdown();
    }

    private static void createChain(Transaction transaction, String... names) {
        for (int i = 0; i < names.length - 1; i++) {
            Node firstNode = getOrCreateNode(transaction, names[i]);
            Node secondNode = getOrCreateNode(transaction, names[i + 1]);
            firstNode.createRelationshipTo(secondNode, KNOWS);
        }
    }

    private static Node getOrCreateNode(Transaction transaction, String name) {
        Node node = transaction.findNode(NODE_LABEL, NAME_KEY, name);
        if (node == null) {
            node = transaction.createNode(NODE_LABEL);
            node.setProperty(NAME_KEY, name);
        }
        return node;
    }

    private static void registerShutdownHook() {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> managementService.shutdown()));
    }
}
