/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.javacompat.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.cypher.internal.CommunityCompatibilityFactory;
import org.neo4j.cypher.internal.DocsExecutionEngine;
import org.neo4j.cypher.internal.EnterpriseCompatibilityFactory;
import org.neo4j.cypher.internal.javacompat.GraphDatabaseCypherService;
import org.neo4j.cypher.internal.runtime.InternalExecutionResult;
import org.neo4j.doc.test.TestEnterpriseGraphDatabaseFactory;
import org.neo4j.graphdb.DependencyResolver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.internal.kernel.api.Transaction.Type;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.coreapi.PropertyContainerLocker;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.query.Neo4jTransactionalContextFactory;
import org.neo4j.kernel.impl.query.TransactionalContext;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;
import org.neo4j.kernel.impl.query.clientconnection.BoltConnectionInfo;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.kernel.monitoring.Monitors;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;

import java.net.InetSocketAddress;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.neo4j.values.virtual.VirtualValues.EMPTY_MAP;

public class DocsExecutionEngineTest
{
    private static GraphDatabaseCypherService database;
    private static DocsExecutionEngine engine;
    private static TransactionalContextFactory contextFactory;

    @Before
    public void setup()
    {
        DocsSetup stuff = createStuff();
        database = stuff.database();
        engine = stuff.engine();
        contextFactory = stuff.contextFactory();
    }

    @After
    public void teardown()
    {
        database.getGraphDatabaseService().shutdown();
    }

    @Test
    public void actually_works_in_rewindable_fashion()
    {
        String query = "CREATE (n:Person {name:'Adam'}) RETURN n";
        InternalExecutionResult result = engine.internalProfile( query, Collections.emptyMap(),
                createTransactionalContext( query ) );
        String dump = result.dumpToString();
        assertThat( dump, containsString( "1 row" ) );
        assertThat( result.javaIterator().hasNext(), equalTo( true ) );
    }

    @Test
    public void should_work_in_rewindable_fashion()
    {
        String query = "RETURN 'foo'";
        InternalExecutionResult result = engine.internalProfile( query, Collections.emptyMap(),
                createTransactionalContext( query ) );
        String dump = result.dumpToString();
        assertThat( dump, containsString( "1 row" ) );
        assertThat( result.javaIterator().hasNext(), equalTo( true ) );
    }

    private static TransactionalContext createTransactionalContext( String query )
    {
        InternalTransaction transaction = database.beginTransaction( Type.implicit, SecurityContext.AUTH_DISABLED );
        BoltConnectionInfo boltConnection = new BoltConnectionInfo(
                "username",
                "neo4j-java-bolt-driver",
                new InetSocketAddress("127.0.0.1", 56789),
                new InetSocketAddress("127.0.0.1", 7687));
        return contextFactory.newContext(boltConnection, transaction, query, EMPTY_MAP );
    }

    public interface DocsSetup
    {
        GraphDatabaseCypherService database();

        DocsExecutionEngine engine();

        TransactionalContextFactory contextFactory();
    }

    private static DocsSetup createStuff()
    {
        EphemeralFileSystemAbstraction fs = new EphemeralFileSystemAbstraction();
        GraphDatabaseService graph =
                new TestEnterpriseGraphDatabaseFactory().setFileSystem( fs ).newImpermanentDatabase();
        GraphDatabaseCypherService database = new GraphDatabaseCypherService( graph );
        GraphDatabaseCypherService queryService = new GraphDatabaseCypherService( graph );
        GraphDatabaseAPI graphAPI = (GraphDatabaseAPI) graph;
        DependencyResolver resolver = graphAPI.getDependencyResolver();
        LogService logService = resolver.resolveDependency( LogService.class );
        Monitors monitors = resolver.resolveDependency( Monitors.class );
        LogProvider logProvider = logService.getInternalLogProvider();
        CommunityCompatibilityFactory inner =
                new CommunityCompatibilityFactory( queryService, monitors, logProvider );

        EnterpriseCompatibilityFactory compatibilityFactory =
                new EnterpriseCompatibilityFactory( inner, queryService, monitors, logProvider );

        NullLogProvider logProvider1 = NullLogProvider.getInstance();
        DocsExecutionEngine engine = new DocsExecutionEngine( database, logProvider1, compatibilityFactory );
        PropertyContainerLocker locker = new PropertyContainerLocker();
        TransactionalContextFactory contextFactory = Neo4jTransactionalContextFactory.create( database, locker );

        return new DocsSetup()
        {
            @Override
            public GraphDatabaseCypherService database()
            {
                return database;
            }

            @Override
            public DocsExecutionEngine engine()
            {
                return engine;
            }

            @Override
            public TransactionalContextFactory contextFactory()
            {
                return contextFactory;
            }
        };
    }
}
