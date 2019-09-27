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
// tag::sampleDocumentation[]
// tag::_sampleDocumentation[]
package org.neo4j.examples;

import org.junit.Test;

import java.io.IOException;

import org.neo4j.annotations.documented.Documented;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.neo4j.visualization.asciidoc.AsciidocHelper.createGraphVizWithNodeId;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createOutputSnippet;

public class TraversalDocTest extends AbstractJavaDocTestBase
{
    private static final String JAVADOC_TRAVERSAL_DESCRIPTION_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/TraversalDescription.html";
    private static final String JAVADOC_TRAVERSER_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/Traverser.html";
    private static final String JAVADOC_EVALUATORS_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/Evaluators.html";
    private static final String JAVADOC_EVALUATOR_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/Evaluator.html";
    private static final String JAVADOC_PATH_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/Path.html";
    private static final String JAVADOC_NODE_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/Node.html";
    private static final String JAVADOC_TRAVERSER_NODES_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/Traverser.html#nodes--";
    private static final String JAVADOC_TRAVERSER_RELATIONSHIPS_URI = "{neo4j-javadoc-base-uri}/org/neo4j/graphdb/traversal/Traverser.html#relationships--";

    private static final String TRAVERSAL_DOC =
            "A\n" +
            "link:" + JAVADOC_TRAVERSAL_DESCRIPTION_URI + "[traversal description] is built using a\n" +
            "fluent interface and such a description can then spawn\n" +
            "link:" + JAVADOC_TRAVERSER_URI + "[traversers].\n" +
            "\n" +
            "@@graph\n" +
            "\n" +
            "With the definition of the `RelationshipTypes` as\n" +
            "\n" +
            "@@sourceRels\n" +
            "\n" +
            "The graph can be traversed with for example the following traverser, starting at the ``Joe'' node:\n" +
            "\n" +
            "@@knowslikestraverser\n" +
            "\n" +
            "The traversal will output:\n" +
            "\n" +
            "@@knowslikesoutput\n" +
            "\n" +
            "Since link:" + JAVADOC_TRAVERSAL_DESCRIPTION_URI + "[`TraversalDescription`]s\n" +
            "are immutable it is also useful to create template descriptions which holds common settings shared by different traversals.\n" +
            "For example, let's start with this traverser:\n" +
            "\n" +
            "@@basetraverser\n" +
            "\n" +
            "This traverser would yield the following output (we will keep starting from the ``Joe'' node):\n" +
            "\n" +
            "@@baseoutput\n" +
            "\n" +
            "Now let's create a new traverser from it, restricting depth to three:\n" +
            "\n" +
            "@@depth3\n" +
            "\n" +
            "This will give us the following result:\n" +
            "\n" +
            "@@output3\n" +
            "\n" +
            "Or how about from depth two to four?\n" +
            "That's done like this:\n" +
            "\n" +
            "@@depth4\n" +
            "\n" +
            "This traversal gives us:\n" +
            "\n" +
            "@@output4\n" +
            "\n" +
            "For various useful evaluators, see the\n" +
            "link:" + JAVADOC_EVALUATORS_URI + "[Evaluators] Java API\n" +
            "or simply implement the\n" +
            "link:" + JAVADOC_EVALUATOR_URI + "[Evaluator] interface yourself.\n" +
            "\n" +
            "If you're not interested in the link:" + JAVADOC_PATH_URI + "[`Path`]s,\n" +
            "but the link:" + JAVADOC_NODE_URI + "[`Node`]s\n" +
            "you can transform the traverser into an iterable of link:" + JAVADOC_TRAVERSER_NODES_URI + "[nodes]\n" +
            "like this:\n" +
            "\n" +
            "@@nodes\n" +
            "\n" +
            "In this case we use it to retrieve the names:\n" +
            "\n" +
            "@@nodeoutput\n" +
            "\n" +
            "link:" + JAVADOC_TRAVERSER_RELATIONSHIPS_URI + "[Relationships]\n" +
            "are fine as well, here's how to get them:\n" +
            "\n" +
            "@@relationships\n" +
            "\n" +
            "Here the relationship types are written, and we get:\n" +
            "\n" +
            "@@relationshipoutput\n" +
            "\n" +
            "TIP: The source code for the traversers in this example is available at:\n" +
            "@@github";
    @Test
    @Documented( TRAVERSAL_DOC )
    @Graph( { "Joe KNOWS Sara", "Lisa LIKES Joe", "Peter KNOWS Sara",
            "Dirk KNOWS Peter", "Lars KNOWS Dirk", "Ed KNOWS Lars",
            "Lisa KNOWS Lars" } )
    public void how_to_use_the_Traversal_framework()
    {
        Node joe = data.get().get( "Joe" );
        TraversalExample example = new TraversalExample( graphdb() );
        gen.get().addSnippet(
                "graph",
                        createGraphVizWithNodeId( "Traversal Example Graph", graphdb(),
                        gen.get().getTitle() ) );

        try ( Transaction tx = graphdb().beginTx() )
        {
            joe = tx.getNodeById( joe.getId() );
            example.init( tx );
            String output = example.knowsLikesTraverser( tx, joe );
            gen.get().addSnippet( "knowslikesoutput", createOutputSnippet( output ) );

            output = example.traverseBaseTraverser( joe );
            gen.get().addSnippet( "baseoutput", createOutputSnippet( output ) );

            output = example.depth3( joe );
            gen.get().addSnippet( "output3", createOutputSnippet( output ) );

            output = example.depth4( joe );
            gen.get().addSnippet( "output4", createOutputSnippet( output ) );

            output = example.nodes( joe );
            gen.get().addSnippet( "nodeoutput", createOutputSnippet( output ) );

            output = example.relationships( joe );
            gen.get().addSnippet( "relationshipoutput", createOutputSnippet( output ) );

            gen.get().addSourceSnippets( example.getClass(), "knowslikestraverser",
                    "sourceRels", "basetraverser", "depth3", "depth4",
                    "nodes", "relationships" );
            gen.get().addGithubSourceLink( "github", example.getClass(), "embedded-examples" );
        }
    }

    @Test
    public void runAll() throws IOException
    {
        TraversalExample.main( null );
    }
}
