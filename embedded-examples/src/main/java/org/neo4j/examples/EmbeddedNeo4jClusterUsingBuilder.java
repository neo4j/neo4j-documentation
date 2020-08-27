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

import com.neo4j.configuration.CausalClusteringSettings;
import com.neo4j.configuration.DiscoveryType;
import com.neo4j.dbms.api.ClusterDatabaseManagementServiceBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.io.fs.FileUtils;

import static org.neo4j.configuration.GraphDatabaseSettings.Mode.CORE;

public class EmbeddedNeo4jClusterUsingBuilder
{
    private static final Path homeDirectory = Path.of( "target/neo4j-home" );

    public static void main( final String[] args ) throws IOException
    {
        System.out.println( "Starting database ..." );
        FileUtils.deleteDirectory( homeDirectory );

        // tag::startCore[]
        var defaultAdvertised = new SocketAddress( "core01.example.com" );
        var defaultListen = new SocketAddress( "0.0.0.0" );

        var initialMembers = List.of(
                new SocketAddress( "core01.example.com" ),
                new SocketAddress( "core02.example.com" ),
                new SocketAddress( "core03.example.com" )
        );

        var managementService = new ClusterDatabaseManagementServiceBuilder( homeDirectory )
                .setConfig( GraphDatabaseSettings.mode, CORE )
                .setConfig( GraphDatabaseSettings.default_advertised_address, defaultAdvertised )
                .setConfig( GraphDatabaseSettings.default_listen_address, defaultListen )
                .setConfig( CausalClusteringSettings.discovery_type, DiscoveryType.LIST )
                .setConfig( CausalClusteringSettings.initial_discovery_members, initialMembers )
                .setConfig( BoltConnector.enabled, true )
                .setConfig( HttpConnector.enabled, true )
                .build();
        // end::startCore[]

        // This is the neo4j.conf that should go together with the loading of the property file in EmbeddedNeo4jClusterUsingNeo4jConf
        // but kept in this example file because it should be equivalent to the above configuration.

        /* tag::neo4jConf[]

        dbms.mode=CORE
        dbms.default_advertised_address=core01.example.com
        dbms.default_listen_address=0.0.0.0
        causal_clustering.discovery_type=LIST
        causal_clustering.initial_discovery_members=core01.example.com,core02.example.com,core03.example.com
        dbms.connector.bolt.enabled=true
        dbms.connector.http.enabled=true

        end::neo4jConf[] */

        managementService.shutdown();
    }
}
