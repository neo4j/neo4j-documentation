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

import org.junit.Test;

import org.neo4j.annotations.documented.Documented;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;

import static org.junit.Assert.assertEquals;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createGraphVizWithNodeId;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createOutputSnippet;

public class UniquenessOfPathsDocTest extends AbstractJavaDocTestBase
{
    private static final String UNIQUENESS_OF_PATHS_DOC =
            "Uniqueness of Paths in traversals.\n" +
            " \n" +
            "This example is demonstrating the use of node uniqueness.\n" +
            "Below an imaginary domain graph with Principals that own pets that are descendant to other pets.\n" +
            " \n" +
            "@@graph\n" +
            " \n" +
            "In order to return all descendants of `Pet0` which have the relation `owns` to `Principal1` (`Pet1` and `Pet3`),\n" + "the Uniqueness of the traversal needs to be set to `NODE_PATH` rather than the default `NODE_GLOBAL`.\n" +
            "This way nodes can be traversed more that once, and paths that have different nodes but can have some nodes in common (like the start and end node) can be returned.\n" +
            " \n" +
            "@@traverser\n" +
            " \n" +
            "This will return the following paths:\n" +
            " \n" +
            "@@output\n" +
            " \n" +
            "In the default `path.toString()` implementation, `(1)--[knows,2]-->(4)` denotes\n" +
            "a node with ID=1 having a relationship with ID=2 or type `knows` to a node with ID=4.\n" +
            " \n" +
            "Let's create a new `TraversalDescription` from the old one,\n" +
            "having `NODE_GLOBAL` uniqueness to see the difference.\n" +
            " \n" +
            "[TIP]\n"+
            "--\n" +
            "The `TraversalDescription` object is immutable, so we have to use the new instance returned with the new uniqueness setting.\n" +
            "--\n" +
            "\n" +
            "@@traverseNodeGlobal\n" +
            " \n" +
            "Now only one path is returned:\n" +
            " \n" +
            "@@outNodeGlobal";

    @Graph({"Pet0 descendant Pet1",
        "Pet0 descendant Pet2",
        "Pet0 descendant Pet3",
        "Principal1 owns Pet1",
        "Principal2 owns Pet2",
        "Principal1 owns Pet3"})
    @Test
    @Documented( UNIQUENESS_OF_PATHS_DOC )
    public void pathUniquenessExample()
    {
        Node start = data.get().get( "Pet0" );
        gen.get().addSnippet( "graph", createGraphVizWithNodeId("Descendants example graph", graphdb(), gen.get().getTitle()) );
        gen.get();
        gen.get().addTestSourceSnippets( this.getClass(), "traverser", "traverseNodeGlobal" );
        // tag::traverser[]
        Node dataTarget = data.get().get( "Principal1" );
        String output = "";
        int count = 0;
        try ( Transaction transaction = graphdb().beginTx() )
        {
            start = transaction.getNodeById( start.getId() );
            final Node target = transaction.getNodeById( dataTarget.getId() );
            TraversalDescription td = transaction.traversalDescription()
                    .uniqueness( Uniqueness.NODE_PATH )
                    .evaluator( new Evaluator()
            {
                @Override
                public Evaluation evaluate( Path path )
                {
                    boolean endNodeIsTarget = path.endNode().equals( target );
                    return Evaluation.of( endNodeIsTarget, !endNodeIsTarget );
                }
            } );

            Traverser results = td.traverse( start );
            // end::traverser[]
        //we should get two paths back, through Pet1 and Pet3

            for ( Path path : results )
            {
                count++;
                output += path.toString() + "\n";
            }
        }
        gen.get().addSnippet( "output", createOutputSnippet( output ) );
        assertEquals( 2, count );

        String output2 = "";
        count = 0;
        try ( Transaction tx = graphdb().beginTx() )
        {
            start = tx.getNodeById( start.getId() );
            final Node target = tx.getNodeById( dataTarget.getId() );
            // tag::traverseNodeGlobal[]
            TraversalDescription nodeGlobalTd = tx.traversalDescription().uniqueness( Uniqueness.NODE_PATH ).evaluator( new Evaluator()
            {
                @Override
                public Evaluation evaluate( Path path )
                {
                    boolean endNodeIsTarget = path.endNode().equals( target );
                    return Evaluation.of( endNodeIsTarget, !endNodeIsTarget );
                }
            } ).uniqueness( Uniqueness.NODE_GLOBAL );
            Traverser results = nodeGlobalTd.traverse( start );
            // end::traverseNodeGlobal[]
            // we should get two paths back, through Pet1 and Pet3
            for ( Path path : results )
            {
                count++;
                output2 += path.toString() + "\n";
            }
        }
        gen.get().addSnippet( "outNodeGlobal", createOutputSnippet( output2 ) );
        assertEquals( 1, count );
    }
}
