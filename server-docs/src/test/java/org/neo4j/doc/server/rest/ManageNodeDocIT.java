/*
 * Copyright (c) 2002-2018 "Neo4j,"
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
package org.neo4j.doc.server.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.neo4j.doc.metatest.TestJavaTestDocsGenerator;
import org.neo4j.doc.server.ExclusiveServerTestBase;
import org.neo4j.doc.server.HTTP;
import org.neo4j.doc.server.helpers.CommunityServerBuilder;
import org.neo4j.doc.server.helpers.FunctionalTestHelper;
import org.neo4j.doc.server.rest.domain.GraphDbHelper;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.internal.KernelData;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.configuration.ServerSettings;
import org.neo4j.server.database.Database;
import org.neo4j.server.database.WrappedDatabase;
import org.neo4j.server.rest.domain.JsonHelper;
import org.neo4j.server.rest.domain.JsonParseException;
import org.neo4j.server.rest.management.JmxService;
import org.neo4j.server.rest.management.VersionAndEditionService;
import org.neo4j.server.rest.management.console.ConsoleService;
import org.neo4j.server.rest.management.console.ConsoleSessionFactory;
import org.neo4j.server.rest.management.console.ScriptSession;
import org.neo4j.server.rest.management.console.ShellSession;
import org.neo4j.server.rest.repr.OutputFormat;
import org.neo4j.server.rest.repr.formats.JsonFormat;
import org.neo4j.shell.ShellSettings;
import org.neo4j.string.UTF8;
import org.neo4j.test.TestData;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.time.Clocks;
import org.neo4j.time.FakeClock;

import static java.lang.System.lineSeparator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import static org.neo4j.test.rule.SuppressOutput.suppressAll;

public class ManageNodeDocIT extends AbstractRestFunctionalDocTestBase
{
    private static final long NON_EXISTENT_NODE_ID = 999999;
    private static String NODE_URI_PATTERN = "^.*/node/[0-9]+$";

    private static FunctionalTestHelper functionalTestHelper;
    private static GraphDbHelper helper;

    @BeforeClass
    public static void setupServer() throws IOException
    {
        functionalTestHelper = new FunctionalTestHelper( server() );
        helper = functionalTestHelper.getGraphDbHelper();
    }

    private void checkGeneratedFiles()
    {
        String requestDocs, responseDocs, graphDocs;
        try
        {
            requestDocs = TestJavaTestDocsGenerator.readFileAsString( new File(
                    "target/docs/dev/rest-api/includes/create-node-with-properties.request.asciidoc" ) );
            responseDocs = TestJavaTestDocsGenerator.readFileAsString( new File(
                    "target/docs/dev/rest-api/includes/create-node-with-properties.response.asciidoc" ) );
            graphDocs = TestJavaTestDocsGenerator.readFileAsString( new File(
                    "target/docs/dev/rest-api/includes/create-node-with-properties.graph.asciidoc" ) );
        }
        catch ( IOException ioe )
        {
            throw new RuntimeException(
                    "Error reading generated documentation file: ", ioe );
        }
        for ( String s : new String[] { "POST", "Accept", "application/json",
                "Content-Type", "{", "foo", "bar", "}" } )
        {
            assertThat( requestDocs, containsString( s ) );
        }
        for ( String s : new String[] { "201", "Created", "Content-Length",
                "Content-Type", "Location", "{", "foo", "bar", "}" } )
        {
            assertThat( responseDocs, containsString( s ) );
        }
        for ( String s : new String[] { "foo", "bar" } )
        {
            assertThat( graphDocs, containsString( s ) );
        }
    }

    @Test
    public void create_node_with_array_properties() throws Exception
    {
        String response = gen.get()
                .payload( "{\"foo\" : [1,2,3]}" )
                .expectedStatus( 201 )
                .expectedHeader( "Location" )
                .expectedHeader( "Content-Length" )
                .post( functionalTestHelper.nodeUri() )
                .response().getEntity();
        assertThat( response, containsString( "[ 1, 2, 3 ]" ) );
    }

    private JaxRsResponse sendCreateRequestToServer(final String json)
    {
        return RestRequest.req().post( functionalTestHelper.dataUri() + "node/" , json );
    }

    private JaxRsResponse sendCreateRequestToServer()
    {
        return RestRequest.req().post( functionalTestHelper.dataUri() + "node/" , null, MediaType.APPLICATION_JSON_TYPE );
    }

    /*
        Note that when running this test from within an IDE, the version field will be an empty string. This is because the
        code that generates the version identifier is written by Maven as part of the build process(!). The tests will pass
        both in the IDE (where the empty string will be correctly compared).
         */
    public static class CommunityVersionAndEditionServiceDocIT extends ExclusiveServerTestBase
    {
        private static NeoServer server;
        private static FunctionalTestHelper functionalTestHelper;

        @ClassRule
        public static TemporaryFolder staticFolder = new TemporaryFolder();

        @Rule
        public
        TestData<RESTDocsGenerator> gen = TestData.producedThrough( RESTDocsGenerator.PRODUCER );
        private static FakeClock clock;

        @Before
        public void setUp()
        {
            gen.get().setSection( "dev/rest-api/database-version" );
        }

        @BeforeClass
        public static void setupServer() throws Exception
        {
            clock = Clocks.fakeClock();
            server = CommunityServerBuilder.server()
                    .usingDataDir( staticFolder.getRoot().getAbsolutePath() )
                    .withClock( clock )
                    .build();

            suppressAll().call( new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    server.start();
                    return null;
                }
            } );
            functionalTestHelper = new FunctionalTestHelper( server );
        }

        @Before
        public void setupTheDatabase() throws Exception
        {
            // do nothing, we don't care about the database contents here
        }

        @AfterClass
        public static void stopServer() throws Exception
        {
            suppressAll().call( new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    server.stop();
                    return null;
                }
            } );
        }

        @Test
        public void shouldReportCommunityEdition() throws Exception
        {
            // Given
            String releaseVersion = server.getDatabase().getGraph().getDependencyResolver().resolveDependency( KernelData
                    .class ).version().getReleaseVersion();

            // When
            HTTP.Response res =
                    HTTP.GET( functionalTestHelper.managementUri() + "/" + VersionAndEditionService.SERVER_PATH );

            // Then
            assertEquals( 200, res.status() );
            assertThat( res.get( "edition" ).asText(), equalTo( "community" ) );
            assertThat( res.get( "version" ).asText(), equalTo( releaseVersion ) );
        }
    }

    public static class ConfigureEnabledManagementConsolesDocIT extends ExclusiveServerTestBase
    {
        private NeoServer server;

        @After
        public void stopTheServer()
        {
            server.stop();
        }

        @Test
        public void shouldBeAbleToExplicitlySetConsolesToEnabled() throws Exception
        {
            server = CommunityServerBuilder.server().withProperty( ServerSettings.console_module_engines.name(), "" )
                    .usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() )
                    .build();
            server.start();

            assertThat( exec( "ls", "shell" ).getStatus(), is( 400 ) );
        }

        @Test
        public void shellConsoleShouldBeEnabledByDefault() throws Exception
        {
            server = CommunityServerBuilder.server().usingDataDir( folder.directory( name.getMethodName() ).getAbsolutePath() ).build();
            server.start();

            assertThat( exec( "ls", "shell" ).getStatus(), is( 200 ) );
        }

        private JaxRsResponse exec( String command, String engine )
        {
            return RestRequest.req().post( server.baseUri() + "db/manage/server/console", "{" +
                    "\"engine\":\"" + engine + "\"," +
                    "\"command\":\"" + command + "\\n\"}" );
        }
    }

    public static class ConsoleServiceDocTest
    {
        private final URI uri = URI.create( "http://peteriscool.com:6666/" );

        @Test
        public void correctRepresentation() throws URISyntaxException
        {
            ConsoleService consoleService = new ConsoleService( new ShellOnlyConsoleSessionFactory(), mock( Database.class ),
                    NullLogProvider.getInstance(), new OutputFormat( new JsonFormat(), uri, null ) );

            Response consoleResponse = consoleService.getServiceDefinition();

            assertEquals( 200, consoleResponse.getStatus() );
            String response = decode( consoleResponse );
            MatcherAssert.assertThat( response, containsString( "resources" ) );
            MatcherAssert.assertThat( response, containsString( uri.toString() ) );
        }

        @Test
        public void advertisesAvailableConsoleEngines() throws URISyntaxException
        {
            ConsoleService consoleServiceWithJustShellEngine = new ConsoleService( new ShellOnlyConsoleSessionFactory(),
                    mock( Database.class ), NullLogProvider.getInstance(), new OutputFormat( new JsonFormat(), uri, null ) );

            String response = decode( consoleServiceWithJustShellEngine.getServiceDefinition());

            MatcherAssert.assertThat( response, containsString( "\"engines\" : [ \"shell\" ]" ) );

        }

        private String decode( final Response response )
        {
            return UTF8.decode( (byte[]) response.getEntity() );
        }

        private static class ShellOnlyConsoleSessionFactory implements ConsoleSessionFactory
        {
            @Override
            public ScriptSession createSession( String engineName, Database database, LogProvider logProvider )
            {
                return null;
            }

            @Override
            public Iterable<String> supportedEngines()
            {
                return Collections.singletonList( "shell" );
            }
        }
    }

    public static class JmxServiceDocTest
    {
        public JmxService jmxService;
        private final URI uri = URI.create( "http://peteriscool.com:6666/" );

        @Test
        public void correctRepresentation() throws URISyntaxException
        {
            Response resp = jmxService.getServiceDefinition();

            assertEquals( 200, resp.getStatus() );

            String json = UTF8.decode( (byte[]) resp.getEntity() );
            MatcherAssert.assertThat( json, containsString( "resources" ) );
            MatcherAssert.assertThat( json, containsString( uri.toString() ) );
            MatcherAssert.assertThat( json, containsString( "jmx/domain/{domain}/{objectName}" ) );
        }

        @Test
        public void shouldListDomainsCorrectly() throws Exception
        {
            Response resp = jmxService.listDomains();

            assertEquals( 200, resp.getStatus() );
        }

        @Test
        public void testwork() throws Exception
        {
            jmxService.queryBeans( "[\"*:*\"]" );
        }

        @Before
        public void setUp() throws Exception
        {
            this.jmxService = new JmxService( new OutputFormat( new JsonFormat(), uri, null ), null );
        }

    }

    public static class Neo4jShellConsoleSessionDocTest implements ConsoleSessionFactory
    {
        private ConsoleService consoleService;
        private Database database;
        private final URI uri = URI.create( "http://peteriscool.com:6666/" );

        @Before
        public void setUp() throws Exception
        {
            this.database = new WrappedDatabase( (GraphDatabaseFacade) new TestGraphDatabaseFactory().
                    newImpermanentDatabaseBuilder().
                    setConfig( ShellSettings.remote_shell_enabled, Settings.TRUE ).
                    newGraphDatabase() );
            this.consoleService = new ConsoleService(
                    this,
                    database,
                    NullLogProvider.getInstance(),
                    new OutputFormat( new JsonFormat(), uri, null ) );
        }

        @After
        public void shutdownDatabase()
        {
            this.database.getGraph().shutdown();
        }

        @Override
        public ScriptSession createSession( String engineName, Database database, LogProvider logProvider )
        {
            return new ShellSession( database.getGraph() );
        }

        @Test
        public void doesntMangleNewlines() throws Exception
        {
            Response response = consoleService.exec( new JsonFormat(),
                    "{ \"command\" : \"create (n) return n;\", \"engine\":\"shell\" }" );

            assertEquals( 200, response.getStatus() );
            String result = decode( response ).get( 0 );

            String expected = "+-----------+" + lineSeparator()
                    + "| n         |" + lineSeparator()
                    + "+-----------+" + lineSeparator()
                    + "| Node[0]{} |" + lineSeparator()
                    + "+-----------+" + lineSeparator()
                    + "1 row";

            MatcherAssert.assertThat( result, containsString( expected ) );
        }

        private List<String> decode( final Response response ) throws JsonParseException
        {
            return (List<String>) JsonHelper.readJson( UTF8.decode( (byte[]) response.getEntity() ) );
        }

        @Override
        public Iterable<String> supportedEngines()
        {
            return new ArrayList<String>()
            {{
                add( "shell" );
            }};
        }
    }

    public static class VersionAndEditionServiceTest
    {
        @Test
        public void shouldReturnReadableStringForServiceName() throws Exception
        {
            // given
            VersionAndEditionService service = new VersionAndEditionService( mock( CommunityNeoServer.class ) );

            // when
            String serviceName = service.getName();
            // then
            assertEquals( "version", serviceName );
        }

        @Test
        public void shouldReturnSensiblePathWhereServiceIsHosted() throws Exception
        {
            // given
            VersionAndEditionService service = new VersionAndEditionService( mock( CommunityNeoServer.class ) );

            // when
            String serverPath = service.getServerPath();

            // then
            assertEquals( "server/version", serverPath );
        }
    }
}
