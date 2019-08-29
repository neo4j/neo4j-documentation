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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

import java.util.Map;

import org.neo4j.cypher.docgen.tooling.Prettifier;
import org.neo4j.doc.test.GraphDescription;
import org.neo4j.doc.test.GraphHolder;
import org.neo4j.doc.test.TestData;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.harness.junit.rule.Neo4jRule;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import static org.neo4j.doc.test.GraphDatabaseServiceCleaner.cleanDatabaseContent;

public abstract class AbstractJavaDocTestBase implements GraphHolder
{
    @ClassRule
    public static Neo4jRule neo4j = new Neo4jRule();

    @Rule
    public final TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );

    @Rule
    public final TestData<Map<String, Node>> data = TestData.producedThrough( GraphDescription.createGraphFor( this ) );

    @Override
    public GraphDatabaseService graphdb()
    {
        return neo4j.defaultDatabaseService();
    }

    protected String createCypherSnippet( String cypherQuery )
    {
        String snippet = Prettifier.apply( cypherQuery, false );
        return AsciidocHelper.createAsciiDocSnippet( "cypher", snippet );
    }

    @Before
    public void setUp()
    {
        GraphDatabaseService graphdb = graphdb();
        cleanDatabaseContent( graphdb );
        gen.get().setGraph( graphdb );
    }

    @After
    public void doc()
    {
        gen.get().document( "target/docs/dev", "examples" );
    }
}
