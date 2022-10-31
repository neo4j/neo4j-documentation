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

import java.util.Iterator;

/**
 * Decorator class that wraps any iterator and remembers the current node.
 */

public class PositionedIterator<T> implements Iterator<T> {
    private Iterator<? extends T> inner;
    private T current;
    private Boolean initiated = false;

    /**
     * Creates an instance of the class, wrapping iterator
     *
     * @param iterator The iterator to wrap
     */
    public PositionedIterator(Iterator<? extends T> iterator) {
        inner = iterator;
    }

    @Override
    public boolean hasNext() {
        return inner.hasNext();
    }

    @Override
    public T next() {
        initiated = true;
        current = inner.next();
        return current;
    }

    @Override
    public void remove() {
        inner.remove();
    }

    /**
     * Returns the current node. Any subsequent calls to current will return the same object, unless the next() method has been called.
     *
     * @return The current node.
     */
    public T current() {
        if (!initiated) {
            return next();
        }

        return current;
    }
}
