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

import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.neo4j.graphdb.schema.AnalyzerProvider;

public class AnalyzerProviderExample {
    // tag::customAnalyzerProvider[]
    public class CustomAnalyzerProvider extends AnalyzerProvider // <1>
    {
        public CustomAnalyzerProvider()                          // <2>
        {
            super("custom-analyzer");                          // <3>
        }

        @Override
        public Analyzer createAnalyzer()                         // <4>
        {
            try {
                return CustomAnalyzer.builder()                  // <5>
                        .withTokenizer(StandardTokenizerFactory.class)
                        .addTokenFilter(LowerCaseFilterFactory.class)
                        .addTokenFilter(StopFilterFactory.class, "ignoreCase", "false", "words", "stopwords.txt", "format", "wordset")
                        .build();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    // end::customAnalyzerProvider[]
}
