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
package org.neo4j.doc.server;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.doc.server.helpers.TestWebContainer;
import org.neo4j.doc.server.helpers.WebContainerHelper;

import static org.neo4j.configuration.SettingValueParsers.TRUE;
import static org.neo4j.doc.server.WebContainerHolder.release;
import static org.neo4j.doc.server.WebContainerHolder.setWebContainerBuilderProperty;

public class SharedWebContainerTestBase
{
    protected static TestWebContainer container()
    {
        return testWebContainer;
    }

    private static TestWebContainer testWebContainer;

    @BeforeClass
    public static void allocateServer() throws Throwable
    {
        System.setProperty( "org.neo4j.useInsecureCertificateGeneration", "true" );
        setWebContainerBuilderProperty( GraphDatabaseSettings.cypher_hints_error.name(), TRUE );
        setWebContainerBuilderProperty( BoltConnector.enabled.name(), TRUE );
        setWebContainerBuilderProperty( BoltConnector.listen_address.name(), "localhost:0" );
        setWebContainerBuilderProperty( BoltConnector.advertised_address.name(), ":0" );
        setWebContainerBuilderProperty( GraphDatabaseSettings.transaction_timeout.name(), "300s" );
        testWebContainer = WebContainerHolder.allocate( false );
        WebContainerHelper.cleanTheDatabase( testWebContainer );
    }

    @AfterClass
    public static void releaseServer()
    {
        try
        {
            release( testWebContainer );
        }
        finally
        {
            testWebContainer = null;
        }
    }
}
