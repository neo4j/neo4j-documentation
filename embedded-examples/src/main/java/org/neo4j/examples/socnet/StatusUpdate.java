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
package org.neo4j.examples.socnet;

import static org.neo4j.examples.socnet.RelTypes.NEXT;
import static org.neo4j.examples.socnet.RelTypes.STATUS;
import static org.neo4j.internal.helpers.collection.Iterators.singleOrNull;

import java.util.Date;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

public class StatusUpdate {
    private final GraphDatabaseService databaseService;
    private final Transaction transaction;
    private final Node underlyingNode;
    static final String TEXT = "TEXT";
    static final String DATE = "DATE";

    public StatusUpdate(GraphDatabaseService databaseService, Transaction transaction, Node underlyingNode) {
        this.databaseService = databaseService;
        this.transaction = transaction;
        this.underlyingNode = underlyingNode;
    }

    public Node getUnderlyingNode() {
        return underlyingNode;
    }

    public Person getPerson() {
        return new Person(databaseService, getPersonNode());
    }

    private Node getPersonNode() {
        TraversalDescription traversalDescription = transaction
                .traversalDescription()
                .depthFirst()
                .relationships(NEXT, Direction.INCOMING)
                .relationships(STATUS, Direction.INCOMING)
                .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(STATUS));

        Traverser traverser = traversalDescription.traverse(getUnderlyingNode());

        return singleOrNull(traverser.iterator()).endNode();
    }

    public String getStatusText() {
        return (String) underlyingNode.getProperty(TEXT);
    }

    public Date getDate() {
        Long l = (Long) underlyingNode.getProperty(DATE);

        return new Date(l);
    }
}
