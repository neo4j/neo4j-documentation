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

import static org.neo4j.visualization.asciidoc.AsciidocHelper.createGraphVizWithNodeId;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createOutputSnippet;

import org.junit.jupiter.api.Test;
import org.neo4j.annotations.documented.Documented;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.GraphDatabaseService;

public class DocumentationDocTest extends AbstractJavaDocTestBase {
    @Test
    // signaling this to be a documentation test
    @Documented("This is a sample documentation test, demonstrating different ways of\n" +
            "bringing code and other artifacts into Asciidoc form. The title of the\n" +
            "generated document is determined from the method name, replacing \"+_+\" with\n" +
            "\" \".\n" +
            " \n" +
            "Below you see a number of different ways to generate text from source,\n" +
            "inserting it into the JavaDoc documentation (really being Asciidoc markup)\n" +
            "via the snippet markers (see below) and programmatic adding with runtime data\n" +
            "in the Java code.\n" +
            " \n" +
            "- The annotated graph as http://www.graphviz.org/[GraphViz]-generated visualization:\n" +
            " \n" +
            "@@graph\n" +
            " \n" +
            "- A sample Cypher query:\n" +
            " \n" +
            "@@cypher\n" +
            " \n" +
            "- A sample text output snippet:\n" +
            " \n" +
            "@@output\n" +
            " \n" +
            "- a generated source link to the original GIThub source for this test:\n" +
            " \n" +
            "@@github\n" +
            " \n" +
            "- The full source for this example as a source snippet, highlighted as Java code:\n" +
            " \n" +
            "@@sampleDocumentation\n" +
            " \n" +
            "This is the end of this chapter.")
    // the graph data setup as simple statements
    @Graph("I know you")
    // title is determined from the method name
    public void hello_world_Sample_Chapter(GraphDatabaseService graphDb) {
        JavaTestDocsGenerator generator = gen.get();
        generator.addTestSourceSnippets(this.getClass(), "sampleDocumentation");
        generator.addGithubTestSourceLink("github", this.getClass(), "embedded-examples");
        generator.addSnippet("output", createOutputSnippet("Hello graphy world!"));
        generator.addSnippet("graph", createGraphVizWithNodeId("Hello World Graph", graphDb, generator.getTitle()));
        // A cypher snippet referring to the generated graph
        generator.addSnippet("cypher", createCypherSnippet("MATCH (n) WHERE id(n) = " + data.get().get("I").getId() + " RETURN n"));
    }
}
// end::_sampleDocumentation[]
// end::sampleDocumentation[]
