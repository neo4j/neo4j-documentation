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
import org.junit.Rule;

import java.util.concurrent.Callable;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.server.NeoServer;
import org.neo4j.doc.server.helpers.ServerHelper;
import org.neo4j.doc.test.rule.SuppressOutput;

import static org.neo4j.doc.test.rule.SuppressOutput.suppressAll;

public class SharedServerTestBase
{
    private static final boolean useExternal = Boolean.valueOf( System.getProperty( "neo-server.external", "false" ) );

    protected static NeoServer server()
    {
        return server;
    }

    private static NeoServer server;

    @Rule
    public SuppressOutput suppressOutput = suppressAll();

    @BeforeClass
    public static void allocateServer() throws Throwable
    {
        System.setProperty( "org.neo4j.useInsecureCertificateGeneration", "true" );
        if ( !useExternal )
        {
            suppressAll().call((Callable<Void>) () -> {
                ServerHolder.setServerBuilderProperty( GraphDatabaseSettings.cypher_hints_error.name(), "true" );
                ServerHolder.setServerBuilderProperty( GraphDatabaseSettings.record_id_batch_size.name(), "1" );
                server = ServerHolder.allocate();
                ServerHelper.cleanTheDatabase( server );
                return null;
            });
        }
    }

    @AfterClass
    public static void releaseServer() throws Exception
    {
        if ( !useExternal )
        {
            try
            {
                suppressAll().call((Callable<Void>) () -> {
                    ServerHolder.release( server );
                    return null;
                });
            }
            finally
            {
                server = null;
            }
        }
    }
}
