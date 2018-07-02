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
package org.neo4j.doc.test.rule;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;

import org.neo4j.doc.test.TestGraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


/**
 * JUnit @Rule for configuring, creating and managing an EmbeddedGraphDatabase instance.
 * <p>
 * The database instance is created lazily, so configurations can be injected prior to calling
 * {@link #getGraphDatabaseAPI()}.
 */
public class EmbeddedDatabaseRule extends DatabaseRule
{
    private final TestDirectory testDirectory;

    public EmbeddedDatabaseRule()
    {
        this.testDirectory = TestDirectory.testDirectory();
    }

    @Override
    public EmbeddedDatabaseRule startLazily()
    {
        return (EmbeddedDatabaseRule) super.startLazily();
    }

    @Override
    public File getStoreDir()
    {
        return testDirectory.graphDbDir();
    }

    @Override
    public String getStoreDirAbsolutePath()
    {
        return testDirectory.graphDbDir().getAbsolutePath();
    }

    @Override
    protected GraphDatabaseFactory newFactory()
    {
        return new TestGraphDatabaseFactory();
    }

    @Override
    protected GraphDatabaseBuilder newBuilder( GraphDatabaseFactory factory )
    {
        return factory.newEmbeddedDatabaseBuilder( testDirectory.graphDbDir() );
    }

    @Override
    public Statement apply( Statement base, Description description )
    {
        return testDirectory.apply( super.apply( base, description ), description );
    }

    /**
     * Get the inner {@link TestDirectory} instance that is used to prepare the store directory for this database.
     * <p>
     * <strong>Note:</strong> There is no need to add a {@link org.junit.Rule} annotation on this {@link TestDirectory}
     * instance.
     */
    public TestDirectory getTestDirectory()
    {
        return testDirectory;
    }
}

