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
package org.neo4j.doc.test;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.neo4j.annotations.documented.Documented;
import org.neo4j.graphdb.GraphDatabaseService;

public class TestData<T> implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        final Documented doc = method.getAnnotation(Documented.class);
        GraphDescription.Graph g = method.getAnnotation(GraphDescription.Graph.class);
        if (g == null) {
            g = context.getRequiredTestClass().getAnnotation(GraphDescription.Graph.class);
        }
        final GraphDescription graph = GraphDescription.create(g);
        testDataCache.set(create(graph, null, doc == null ? null : doc.value(), method.getName()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        testDataCache.remove();
    }

    public static class TestDataRecord<T> {
        private final GraphDescription graph;
        private final String title;
        private final String doc;
        private T entry;

        public TestDataRecord(GraphDescription graph, String title, String doc) {
            this.graph = graph;
            this.title = title;
            this.doc = doc;
        }
    }

    public interface Producer<T> {
        T create(GraphDefinition graph, String title, String documentation, GraphDatabaseService db);
    }

    public static <T> TestData<T> producedThrough(Producer<T> transformation) {
        return new TestData<>(requireNonNull(transformation));
    }

    public void setGraphDatabaseService(GraphDatabaseService db) {
        TestDataRecord<T> testDataRecord = testDataCache.get();
        testDataRecord.entry = producer.create(testDataRecord.graph, testDataRecord.title, testDataRecord.doc, db);
    }

    private final Producer<T> producer;
    private final ThreadLocal<TestDataRecord<T>> testDataCache = new InheritableThreadLocal<>();

    private TestData(Producer<T> producer) {
        this.producer = producer;
    }

    public T get() {
        TestDataRecord<T> testDataRecord = testDataCache.get();
        if (testDataRecord == null) {
            throw new IllegalStateException("You have to call setGraphDatabaseService() first");
        }
        return testDataRecord.entry;
    }

    private static final String EMPTY = "";

    private static <T> TestDataRecord<T> create(GraphDescription graph, String title, String doc, String methodName) {
        if (doc != null) {
            if (title == null) {
                // standard javadoc means of finding a title
                int dot = doc.indexOf('.');
                if (dot > 0) {
                    title = doc.substring(0, dot);
                    if (title.contains("\n")) {
                        title = null;
                    }
                    else {
                        title = title.trim();
                        doc = doc.substring(dot + 1);
                    }
                }
            }
            String[] lines = doc.split("\n");
            int indent = Integer.MAX_VALUE;
            int start = 0;
            int end = 0;
            for (int i = 0; i < lines.length; i++) {
                if (EMPTY.equals(lines[i].trim())) {
                    lines[i] = EMPTY;
                    if (start == i) {
                        end = ++start; // skip initial blank lines
                    }
                }
                else {
                    for (int j = 0; j < lines[i].length(); j++) {
                        if (!Character.isWhitespace(lines[i].charAt(j))) {
                            indent = Math.min(indent, j);
                            break;
                        }
                    }
                    end = i; // skip blank lines at the end
                }
            }
            if (end == lines.length) {
                end--; // all lines were empty
            }
            // If there still is no title, and the first line looks like a
            // title, take the first line as title
            if (title == null && start < end && EMPTY.equals(lines[start + 1])) {
                title = lines[start].trim();
                start += 2;
            }
            StringBuilder documentation = new StringBuilder();
            for (int i = start; i <= end; i++) {
                documentation.append(EMPTY.equals(lines[i]) ? EMPTY : lines[i].substring(indent)).append("\n");
            }
            doc = documentation.toString();
        }
        else {
            doc = EMPTY;
        }
        if (title == null) {
            title = methodName.replace("_", " ");
        }
        return new TestDataRecord<>(graph, title, doc);
    }
}
