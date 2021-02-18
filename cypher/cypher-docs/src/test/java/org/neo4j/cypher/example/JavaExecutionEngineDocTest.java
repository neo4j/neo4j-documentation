/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.cypher.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.neo4j.cypher.docgen.tooling.Prettifier;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.doc.tools.AsciiDocGenerator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.Iterators;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.visualization.asciidoc.AsciidocHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.internal.helpers.collection.Iterators.count;

public class JavaExecutionEngineDocTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();
    private static final File docsTargetDir = new File( "target/docs/dev/syntax" );
    private GraphDatabaseService db;
    private Long bobNodeId;
    private Long johanNodeId;
    private Long michaelaNodeId;
    private DatabaseManagementService managementService;

    @BeforeClass
    public static void prepare()
    {
        if( docsTargetDir.exists() )
        {
            return;
        }

        if( !docsTargetDir.mkdirs() )
        {
            fail("Failed to created necessary directories.");
        }
    }

    @Before
    public void setUp() throws IOException
    {
        managementService = new TestDatabaseManagementServiceBuilder().impermanent().build();
        db = managementService.database( DEFAULT_DATABASE_NAME );

        try ( Transaction tx = this.db.beginTx() )
        {
            tx.schema().indexFor( Label.label( "Person" ) ).on( "name" ).withName( "My index" ).create();
            tx.commit();
        }

        try ( Transaction tx = this.db.beginTx() )
        {
            Node michaelaNode = tx.createNode( Label.label( "Person" ) );
            Node bobNode = tx.createNode( Label.label( "Person" ) );
            Node johanNode = tx.createNode( Label.label( "Person" ) );
            bobNode.setProperty( "name", "Bob" );
            johanNode.setProperty( "name", "Johan" );
            michaelaNode.setProperty( "name", "Michaela" );

            michaelaNodeId = michaelaNode.getId();
            bobNodeId = bobNode.getId();
            johanNodeId = johanNode.getId();
            tx.commit();
        }
    }

    @After
    public void shutdownDb()
    {
        if ( managementService != null )
        {
            managementService.shutdown();
        }
        db = null;
    }

    public static String parametersToAsciidoc( final Object params ) throws IOException
    {
        StringBuilder sb = new StringBuilder( 2048 );
        String prettifiedJson = WRITER.writeValueAsString( params );
        sb.append( "\n.Parameters\n[source,javascript]\n----\n" )
                .append( prettifiedJson )
                .append( "\n----\n\n" );
        return sb.toString();
    }

    private void dumpToFile( final String id, final String query, final Object params ) throws Exception
    {
        StringBuilder sb = new StringBuilder( 2048 );
        String prettifiedJson = WRITER.writeValueAsString( params );
        sb.append( "\n.Parameters\n[source,javascript]\n----\n" )
                .append( prettifiedJson )
                .append( "\n----\n\n.Query\n" )
                .append( AsciidocHelper.createAsciiDocSnippet( "cypher", query ) );
        AsciiDocGenerator.dumpToSeparateFile( docsTargetDir, id, sb.toString() );
    }

    @Test
    public void exampleQuery() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::JavaQuery[]
            String query =
                "MATCH (n)" + "\n" +
                "WHERE id(n) = 0 AND 1 = 1" + "\n" +
                "RETURN n";

            Result result = transaction.execute( query );

            assertThat( result.columns(), hasItem( "n" ) );
            Iterator<Node> n_column = result.columnAs( "n" );
            assertThat( Iterators.asCollection( n_column ), hasItem( transaction.getNodeById( 0 ) ) );
            // end::JavaQuery[]
            transaction.commit();
        }
    }

    @Test
    public void shouldBeAbleToEmitJavaIterables() throws Exception
    {
        makeFriends( michaelaNodeId, bobNodeId );
        makeFriends( michaelaNodeId, johanNodeId );

        try ( Transaction transaction = db.beginTx() )
        {
            Node bobNode = transaction.getNodeById( bobNodeId );
            Node johanNode = transaction.getNodeById( johanNodeId );

            String query =
                "MATCH (n)-->(friend)" + "\n" +
                "WHERE id(n) = 0" + "\n" +
                "RETURN collect(friend)";

            Result result = transaction.execute( query );

            Iterable<Node> friends = (Iterable<Node>) result.columnAs( "collect(friend)" ).next();
            assertThat( friends, hasItems( bobNode, johanNode ) );
            assertThat( friends, instanceOf( Iterable.class ) );
            transaction.commit();
        }
    }

    @Test
    public void testColumnAreInTheRightOrder() throws Exception
    {
        createTenNodes();
        try ( Transaction transaction = db.beginTx() )
        {
            String query =
                "MATCH (one), (two), (three), (four), (five), (six), (seven), (eight), (nine), (ten)" + "\n" +
                "WHERE id(one) = 1 AND id(two) = 2 AND id(three) = 3 AND id(four) = 4 AND id(five) = 5" + "\n" +
                "AND id(six) = 6 AND id(seven) = 7 AND id(eight) = 8 AND id(nine) = 9 AND id(ten) = 10" + "\n" +
                "RETURN one, two, three, four, five, six, seven, eight, nine, ten";

            Result result = transaction.execute( query );

            Pattern pattern = Pattern.compile( "one.*two.*three.*four.*five.*six.*seven.*eight.*nine.*ten" );
            assertTrue( pattern.matcher( result.resultAsString() ).find() );
            transaction.commit();
        }
    }

    private void createTenNodes()
    {
        try ( Transaction tx = db.beginTx() )
        {
            for ( int i = 0; i < 10; i++ )
            {
                tx.createNode();
            }
            tx.commit();
        }
    }

    @Test
    public void exampleWithParameterForNodeId() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterForNodeId[]
            Map<String,Object> params = new HashMap<>();
            params.put( "id", 0 );

            String query =
                "MATCH (n)" + "\n" +
                "WHERE id(n) = $id" + "\n" +
                "RETURN n.name";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterForNodeId[]

            assertThat( result.columns(), hasItem( "n.name" ) );
            Iterator<Object> n_column = result.columnAs( "n.name" );
            assertEquals( "Michaela", n_column.next() );
            dumpToFile( "exampleWithParameterForNodeId", query, params );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterForMultipleNodeIds() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterForMultipleNodeIds[]
            Map<String,Object> params = new HashMap<>();
            params.put( "ids", asList( 0, 1, 2 ) );

            String query =
                "MATCH (n)" + "\n" +
                "WHERE id(n) IN $ids" + "\n" +
                "RETURN n.name";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterForMultipleNodeIds[]

            assertEquals( asList( "Michaela", "Bob", "Johan" ), this.<String>toList( result, "n.name" ) );
            dumpToFile( "exampleWithParameterForMultipleNodeIds", query, params );
            transaction.commit();
        }
    }

    private <T> List<T> toList( Result result, String column )
    {
        List<T> results = new ArrayList<>();
        Iterators.addToCollection( result.columnAs( column ), results );
        return results;
    }

    @Test
    public void exampleWithStringLiteralAsParameter() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node johanNode = transaction.getNodeById( johanNodeId );

            // tag::exampleWithStringLiteralAsParameter[]
            Map<String,Object> params = new HashMap<>();
            params.put( "name", "Johan" );

            String query =
                "MATCH (n:Person)" + "\n" +
                "WHERE n.name = $name" + "\n" +
                "RETURN n";

            Result result = transaction.execute( query, params );
            // end::exampleWithStringLiteralAsParameter[]

            assertEquals( singletonList( johanNode ), this.<Node>toList( result, "n" ) );
            dumpToFile( "exampleWithStringLiteralAsParameter", query, params );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithShortSyntaxStringLiteralAsParameter() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node johanNode = transaction.getNodeById( johanNodeId );

            // tag::exampleWithShortSyntaxStringLiteralAsParameter[]
            Map<String,Object> params = new HashMap<>();
            params.put( "name", "Johan" );

            String query =
                "MATCH (n:Person {name: $name})" + "\n" +
                "RETURN n";

            Result result = transaction.execute( query, params );
            // end::exampleWithShortSyntaxStringLiteralAsParameter[]

            assertEquals( singletonList( johanNode ), this.<Node>toList( result, "n" ) );
            dumpToFile( "exampleWithShortSyntaxStringLiteralAsParameter", query, params );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterForNodeObject() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node bobNode = transaction.getNodeById( bobNodeId );

            // tag::exampleWithParameterForNodeObject[]
            Map<String,Object> params = new HashMap<>();
            params.put( "node", bobNode );

            String query =
                "MATCH (n:Person)" + "\n" +
                "WHERE n = $node" + "\n" +
                "RETURN n.name";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterForNodeObject[]
            assertThat( result.columns(), hasItem( "n.name" ) );
            Iterator<Object> n_column = result.columnAs( "n.name" );
            assertEquals( "Bob", n_column.next() );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterForSkipAndLimit() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterForSkipLimit[]
            Map<String,Object> params = new HashMap<>();
            params.put( "s", 1 );
            params.put( "l", 1 );

            String query =
                "MATCH (n:Person)" + "\n" +
                "RETURN n.name" + "\n" +
                "SKIP $s" + "\n" +
                "LIMIT $l";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterForSkipLimit[]

            assertThat( result.columns(), hasItem( "n.name" ) );
            Iterator<Object> n_column = result.columnAs( "n.name" );
            assertEquals( "Bob", n_column.next() );
            dumpToFile( "exampleWithParameterForSkipLimit", query, params );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterRegularExpression() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterRegularExpression[]
            Map<String,Object> params = new HashMap<>();
            params.put( "regex", ".*h.*" );

            String query =
                "MATCH (n:Person)" + "\n" +
                "WHERE n.name =~ $regex" + "\n" +
                "RETURN n.name";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterRegularExpression[]

            dumpToFile( "exampleWithParameterRegularExpression", query, params );

            assertThat( result.columns(), hasItem( "n.name" ) );
            Iterator<Object> n_column = result.columnAs( "n.name" );
            Set<Object> results = Iterators.asSet( n_column );
            assertTrue( results.remove( "Michaela" ) );
            assertTrue( results.remove( "Johan" ) );
            assertTrue( results.isEmpty() );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterCSCIStringPatternMatching() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterCSCIStringPatternMatching[]
            Map<String,Object> params = new HashMap<>();
            params.put( "name", "Michael" );

            String query =
                "MATCH (n:Person)" + "\n" +
                "WHERE n.name STARTS WITH $name" + "\n" +
                "RETURN n.name";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterCSCIStringPatternMatching[]

            dumpToFile( "exampleWithParameterCSCIStringPatternMatching", query, params );

            assertThat( result.columns(), hasItem( "n.name" ) );
            Iterator<Object> n_column = result.columnAs( "n.name" );
            assertEquals( "Michaela", n_column.next() );
            transaction.commit();
        }
    }

    @Test
    public void exampleWithParameterProcedureCall() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::exampleWithParameterProcedureCall[]
            Map<String,Object> params = new HashMap<>();
            params.put( "indexname", "My index" );

            String query = "CALL db.resampleIndex($indexname)";

            Result result = transaction.execute( query, params );
            // end::exampleWithParameterProcedureCall[]

            dumpToFile( "exampleWithParameterProcedureCall", query, params );

            assert result.columns().isEmpty();
            transaction.commit();
        }
    }

    @Test
    public void create_node_from_map() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::create_node_from_map[]
            Map<String,Object> props = new HashMap<>();
            props.put( "name", "Andy" );
            props.put( "position", "Developer" );

            Map<String,Object> params = new HashMap<>();
            params.put( "props", props );

            String query = "CREATE ($props)";

            transaction.execute( query, params );
            // end::create_node_from_map[]

            dumpToFile( "create_node_from_map", query, params );

            Result result = transaction.execute( "MATCH (n) WHERE n.name = 'Andy' AND n.position = 'Developer' RETURN n" );
            assertThat( count( result ), is( 1L ) );
            transaction.commit();
        }
    }

    @Test
    public void create_multiple_nodes_from_map() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::create_multiple_nodes_from_map[]
            Map<String,Object> n1 = new HashMap<>();
            n1.put( "name", "Andy" );
            n1.put( "position", "Developer" );
            n1.put( "awesome", true );

            Map<String,Object> n2 = new HashMap<>();
            n2.put( "name", "Michael" );
            n2.put( "position", "Developer" );
            n2.put( "children", 3 );

            Map<String,Object> params = new HashMap<>();
            List<Map<String,Object>> maps = asList( n1, n2 );
            params.put( "props", maps );

            String query =
                "UNWIND $props AS properties" + "\n" +
                "CREATE (n:Person)" + "\n" +
                "SET n = properties" + "\n" +
                "RETURN n";

            Result result = transaction.execute( query, params );
            // end::create_multiple_nodes_from_map[]

            dumpToFile( "create_multiple_nodes_from_map", query, params );
            result.close();
            result = transaction.execute( "MATCH (n:Person) WHERE n.name IN ['Andy', 'Michael'] AND n.position = 'Developer' RETURN n" );
            assertThat( count( result ), is( 2L ) );

            result = transaction.execute( "MATCH (n:Person) WHERE n.children = 3 RETURN n" );
            assertThat( count( result ), is( 1L ) );

            result = transaction.execute( "MATCH (n:Person) WHERE n.awesome = true RETURN n" );
            assertThat( count( result ), is( 1L ) );
            transaction.commit();
        }
    }

    @Test
    public void set_properties_on_a_node_from_a_map() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::set_properties_on_a_node_from_a_map[]
            Map<String, Object> n1 = new HashMap<>();
            n1.put( "name", "Andy" );
            n1.put( "position", "Developer" );

            Map<String, Object> params = new HashMap<>();
            params.put( "props", n1 );

            String query =
                "MATCH (n:Person)" + "\n" +
                "WHERE n.name = 'Michaela'" + "\n" +
                "SET n = $props";

            transaction.execute( query, params );
            // end::set_properties_on_a_node_from_a_map[]

            dumpToFile( "set_properties_on_a_node_from_a_map", query, params );

            transaction.execute( "MATCH (n:Person) WHERE n.name IN ['Andy', 'Michael'] AND n.position = 'Developer' RETURN n" );
            assertThat( transaction.getNodeById( michaelaNodeId ).getProperty( "name" ).toString(), is( "Andy" ) );
        }
    }

    @Test
    public void create_node_using_create_unique_with_java_maps() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Map<String,Object> props = new HashMap<>();
            props.put( "name", "Andy" );
            props.put( "position", "Developer" );

            Map<String,Object> params = new HashMap<>();
            params.put( "props", props );

            String query =
                "MATCH (n)" + "\n" +
                "WHERE id(n) = 0" + "\n" +
                "MERGE p = (n)-[:REL]->({name: $props.name, position: $props.position})" + "\n" +
                "RETURN last(nodes(p)) AS X";

            Result result = transaction.execute( query, params );
            assertThat( count( result ), is( 1L ) );
            transaction.commit();
        }
    }

    @Test
    public void should_be_able_to_handle_two_params_without_named_nodes() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Map<String,Object> props1 = new HashMap<>();
            props1.put( "name", "Andy" );
            props1.put( "position", "Developer" );

            Map<String,Object> props2 = new HashMap<>();
            props2.put( "name", "Lasse" );
            props2.put( "awesome", "true" );

            Map<String,Object> params = new HashMap<>();
            params.put( "props1", props1 );
            params.put( "props2", props2 );

            String query =
                "MATCH (n) WHERE id(n) = 0 " + "\n" +
                "MERGE p = (n)-[:REL]->({name: $props1.name, position: $props1.position})-[:LER]->({name: $props2.name, awesome: $props2.awesome})" + "\n" +
                "RETURN p";

            Result result = transaction.execute( query, params );
            assertThat( count( result ), is( 1L ) );
            transaction.commit();
        }
    }

    @Test
    public void explain_returns_plan() throws Exception
    {
        try ( Transaction transaction = db.beginTx() )
        {
            // tag::explain_returns_plan[]
            Result result = transaction.execute( "EXPLAIN CREATE (user:User {name: $name}) RETURN user" );

            assert result.getQueryExecutionType().isExplained();
            assert result.getQueryExecutionType().requestedExecutionPlanDescription();
            assert !result.hasNext();
            assert !result.getQueryStatistics().containsUpdates();
            assert !result.getExecutionPlanDescription().hasProfilerStatistics();
            // end::explain_returns_plan[]
            transaction.commit();
        }
    }

    private void makeFriends( Long aId, Long bId )
    {
        try ( Transaction transaction = db.beginTx() )
        {
            Node a = transaction.getNodeById( aId );
            Node b = transaction.getNodeById( bId );
            a.createRelationshipTo( b, RelationshipType.withName( "friend" ) );
            transaction.commit();
        }
    }
}
