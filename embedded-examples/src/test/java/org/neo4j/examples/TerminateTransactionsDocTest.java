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

import static org.neo4j.visualization.asciidoc.AsciidocHelper.createOutputSnippet;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.doc.tools.JavaDocsGenerator;

public class TerminateTransactionsDocTest {
    private static TerminateTransactions terminateTransactions;
    private static final JavaDocsGenerator gen = new JavaDocsGenerator("terminate-tx-java", "dev");

    @BeforeAll
    public static void setUpBeforeClass() {
        terminateTransactions = new TerminateTransactions();
    }

    @Test
    void test() throws IOException {
        String result = terminateTransactions.run();
        gen.saveToFile("result", createOutputSnippet(result));
    }
}
