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

import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.doc.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.logging.LogProvider;

/**
 * JUnit @Rule for configuring, creating and managing an ImpermanentGraphDatabase instance.
 */
public class ImpermanentDatabaseRule extends DatabaseRule
{
    private final LogProvider userLogProvider;
    private final LogProvider internalLogProvider;

    public ImpermanentDatabaseRule()
    {
        this( null );
    }

    public ImpermanentDatabaseRule( LogProvider logProvider )
    {
        this.userLogProvider = logProvider;
        this.internalLogProvider = logProvider;
    }

    @Override
    public ImpermanentDatabaseRule startLazily()
    {
        return (ImpermanentDatabaseRule) super.startLazily();
    }

    @Override
    protected DatabaseManagementServiceBuilder newFactory()
    {
        return maybeSetInternalLogProvider( maybeSetUserLogProvider( new TestDatabaseManagementServiceBuilder() ) );
    }

    @Override
    protected DatabaseManagementServiceBuilder newBuilder( DatabaseManagementServiceBuilder factory )
    {
        return ((TestDatabaseManagementServiceBuilder) factory).impermanent();
    }

    protected final TestDatabaseManagementServiceBuilder maybeSetUserLogProvider( TestDatabaseManagementServiceBuilder factory )
    {
        return ( userLogProvider == null ) ? factory : factory.setUserLogProvider( userLogProvider );
    }

    protected final TestDatabaseManagementServiceBuilder maybeSetInternalLogProvider( TestDatabaseManagementServiceBuilder factory )
    {
        return ( internalLogProvider == null ) ? factory : factory.setInternalLogProvider( internalLogProvider );
    }
}

