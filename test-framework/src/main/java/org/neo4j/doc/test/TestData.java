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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import org.neo4j.annotations.documented.Documented;

public class TestData<T> implements TestRule {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Title {
        String value();
    }

    public interface Producer<T> {
        T create(GraphDefinition graph, String title, String documentation);
    }

    public static <T> TestData<T> producedThrough(Producer<T> transformation) {
        Objects.requireNonNull(transformation);
        return new TestData<>(transformation);
    }

    public T get() {
        return get(true);
    }

    private static final class Lazy {
        private volatile Object productOrFactory;

        Lazy(GraphDefinition graph, String title, String documentation) {
            productOrFactory = new Factory(graph, title, documentation);
        }

        @SuppressWarnings("unchecked")
        <T> T get(Producer<T> producer, boolean create) {
            Object result = productOrFactory;
            if (result instanceof Factory) {
                synchronized (this) {
                    if ((result = productOrFactory) instanceof Factory) {
                        productOrFactory = result = ((Factory) result).create(producer, create);
                    }
                }
            }
            return (T) result;
        }
    }

    private static final class Factory {
        private final GraphDefinition graph;
        private final String title;
        private final String documentation;

        Factory(GraphDefinition graph, String title, String documentation) {
            this.graph = graph;
            this.title = title;
            this.documentation = documentation;
        }

        Object create(Producer<?> producer, boolean create) {
            return create ? producer.create(graph, title, documentation) : null;
        }
    }

    private final Producer<T> producer;
    private final ThreadLocal<Lazy> product = new InheritableThreadLocal<>();

    private TestData(Producer<T> producer) {
        this.producer = producer;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        final Title title = description.getAnnotation(Title.class);
        final Documented doc = description.getAnnotation(Documented.class);
        GraphDescription.Graph g = description.getAnnotation(GraphDescription.Graph.class);
        if (g == null) {
            g = description.getTestClass().getAnnotation(GraphDescription.Graph.class);
        }
        final GraphDescription graph = GraphDescription.create(g);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                product.set( create( graph, title == null ? null : title.value(), doc == null ? null : doc.value(), description.getMethodName() ) );
                try
                {
                    base.evaluate();
                }
                finally
                {
                    product.set( null );
                }
            }
        };
    }

    private T get(boolean create) {
        Lazy lazy = product.get();
        if (lazy == null) {
            if (create) {
                throw new IllegalStateException("Not in test case");
            }
            return null;
        }
        return lazy.get(producer, create);
    }

    private static final String EMPTY = "";

    private static Lazy create(GraphDescription graph, String title, String doc, String methodName) {
        if (doc != null) {
            if (title == null) {
                // standard javadoc means of finding a title
                int dot = doc.indexOf('.');
                if (dot > 0) {
                    title = doc.substring(0, dot);
                    if (title.contains("\n")) {
                        title = null;
                    } else {
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
                } else {
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
        } else {
            doc = EMPTY;
        }
        if (title == null) {
            title = methodName.replace("_", " ");
        }
        return new Lazy(graph, title, doc);
    }

}
