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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

public class GetOrCreateDocIT extends AbstractJavaDocTestBase {

    abstract static class GetOrCreate<D> {
        abstract Node getOrCreateUser(String username, GraphDatabaseService graphDb, D dependency);

        void assertUserExistsUniquely(Transaction tx, String username) {
            assertUserExistsUniquelyInGraphDb(tx, username);
        }
    }

    class CypherGetOrCreate extends GetOrCreate<GraphDatabaseService> {
        @Override
        public Node getOrCreateUser(String username, GraphDatabaseService graphDb, GraphDatabaseService engine) {
            return getOrCreateWithCypher(username, graphDb);
        }
    }

    abstract static class ThreadRunner<D> implements Runnable {
        static final int NUM_USERS = 100;
        final GetOrCreate<D> impl;
        private final String base;
        private final GraphDatabaseService graphDb;

        ThreadRunner(GetOrCreate<D> impl, String base, GraphDatabaseService graphDb) {
            this.impl = impl;
            this.base = base;
            this.graphDb = graphDb;
        }

        abstract D createDependency();

        @Override
        public void run() {
            final D dependency = createDependency();
            final List<GetOrCreateTask<D>> threads = new ArrayList<>();

            int numThreads = Runtime.getRuntime().availableProcessors() * 2;
            for (int i = 0; i < numThreads; i++) {
                String threadName = format("%s thread %d", GetOrCreateDocIT.class.getSimpleName(), i);
                threads.add(new GetOrCreateTask<>(graphDb, NUM_USERS, impl, threadName, dependency, base));
            }
            for (Thread thread : threads) {
                thread.start();
            }

            RuntimeException failure = null;
            List<List<Node>> results = new ArrayList<>();
            for (GetOrCreateTask<D> thread : threads) {
                try {
                    thread.join();
                    if (failure == null) {
                        failure = thread.failure;
                    }

                    results.add(thread.result);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (failure != null) {
                throw failure;
            }

            assertEquals(numThreads, results.size());
            List<Node> firstResult = results.remove(0);
            for (List<Node> subresult : results) {
                assertEquals(firstResult, subresult);
            }

            for (int i = 0; i < NUM_USERS; i++) {
                final String username = getUsername(base, i);
                impl.getOrCreateUser(username, graphDb, dependency);

                try (Transaction tx = graphDb.beginTx()) {
                    impl.assertUserExistsUniquely(tx, username);
                }
                catch (NoSuchElementException e) {
                    throw new RuntimeException(format("User '%s' not created uniquely.", username), e);
                }
            }
        }
    }

    private static String getUsername(String base, int j) {
        return format("%s%d", base, j);
    }

    private static class GetOrCreateTask<D> extends Thread {
        private final GraphDatabaseService db;
        private final int numUsers;
        private final GetOrCreate<D> impl;
        private final D dependency;
        private final String base;

        volatile List<Node> result;
        volatile RuntimeException failure;

        GetOrCreateTask(GraphDatabaseService db, int numUsers, GetOrCreate<D> impl, String name, D dependency, String base) {
            super(name);
            this.db = db;
            this.numUsers = numUsers;
            this.impl = impl;
            this.dependency = dependency;
            this.base = base;
        }

        @Override
        public void run() {
            try {
                List<Node> subResult = new ArrayList<>();
                for (int j = 0; j < numUsers; j++) {
                    subResult.add(impl.getOrCreateUser(getUsername(base, j), db, dependency));
                }
                this.result = subResult;
            }
            catch (RuntimeException e) {
                failure = e;
            }
        }
    }

    @Test
    public void getOrCreateUsingCypher(GraphDatabaseService graphDb) {
        new ThreadRunner<>(new CypherGetOrCreate(), "cypher", graphDb) {
            @Override
            GraphDatabaseService createDependency() {
                return createConstraint(graphDb);
            }
        }.run();
    }

    private Node getOrCreateWithCypher(String username, GraphDatabaseService graphDb) {
        // tag::getOrCreateWithCypher[]
        Node result = null;
        ResourceIterator<Node> resultIterator = null;
        try (Transaction tx = graphDb.beginTx()) {
            String queryString = "MERGE (n:User {name: $name}) RETURN n";
            Map<String,Object> parameters = new HashMap<>();
            parameters.put("name", username);
            resultIterator = tx.execute(queryString, parameters).columnAs("n");
            result = resultIterator.next();
            tx.commit();
            return result;
        }
        // end::getOrCreateWithCypher[]
        finally {
            if (resultIterator != null) {
                if (resultIterator.hasNext()) {
                    Node other = resultIterator.next();
                    //noinspection ThrowFromFinallyBlock
                    throw new IllegalStateException("Merge returned more than one node: " + result + " and " + other);
                }
            }
        }
    }

    private GraphDatabaseService createConstraint(GraphDatabaseService graphdb) {
        // tag::prepareConstraint[]
        try (Transaction tx = graphdb.beginTx()) {
            tx.schema()
                    .constraintFor(Label.label("User"))
                    .assertPropertyIsUnique("name")
                    .withName("usernames")
                    .create();
            tx.commit();
        }
        // end::prepareConstraint[]
        return graphdb;
    }

    private static void assertUserExistsUniquelyInGraphDb(Transaction tx, String username) {
        Label label = Label.label("User");
        Node result = tx.findNode(label, "name", username);
        assertNotNull(result, format("User '%s' not created.", username));
        tx.commit();
    }
}
