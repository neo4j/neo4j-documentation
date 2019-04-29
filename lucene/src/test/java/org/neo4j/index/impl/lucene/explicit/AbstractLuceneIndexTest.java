/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.index.impl.lucene.explicit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.util.Map;

import org.neo4j.dbms.database.DatabaseManagementService;
import org.neo4j.doc.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.doc.test.rule.TestDirectory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.MapUtil;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public abstract class AbstractLuceneIndexTest
{
    @Rule
    public final TestName testname = new TestName();
    @ClassRule
    public static TestDirectory testDirectory = TestDirectory.testDirectory( AbstractLuceneIndexTest.class );
    protected static GraphDatabaseService graphDb;
    protected Transaction tx;
    private static DatabaseManagementService managementService;

    @BeforeClass
    public static void setUpStuff()
    {
        managementService = new TestDatabaseManagementServiceBuilder( testDirectory.storeDir() ).build();
        graphDb = managementService.database( DEFAULT_DATABASE_NAME );
    }

    @AfterClass
    public static void tearDownStuff()
    {
        managementService.shutdown();
    }

    @After
    public void commitTx()
    {
        finishTx( true );
    }

    public void rollbackTx()
    {
        finishTx( false );
    }

    public void finishTx( boolean success )
    {
        if ( tx != null )
        {
            if ( success )
            {
                tx.success();
            }
            tx.close();
            tx = null;
        }
    }

    @Before
    public void beginTx()
    {
        if ( tx == null )
        {
            tx = graphDb.beginTx();
        }
    }

    void restartTx()
    {
        commitTx();
        beginTx();
    }

    protected interface EntityCreator<T extends PropertyContainer>
    {
        T create( Object... properties );

        void delete( T entity );
    }

    private static final RelationshipType TEST_TYPE = RelationshipType.withName( "TEST_TYPE" );

    protected static final EntityCreator<Node> NODE_CREATOR = new EntityCreator<Node>()
    {
        @Override
        public Node create( Object... properties )
        {
            Node node = graphDb.createNode();
            setProperties( node, properties );
            return node;
        }

        @Override
        public void delete( Node entity )
        {
            entity.delete();
        }
    };
    protected static final EntityCreator<Relationship> RELATIONSHIP_CREATOR =
            new EntityCreator<Relationship>()
            {
                @Override
                public Relationship create( Object... properties )
                {
                    Relationship rel = graphDb.createNode().createRelationshipTo( graphDb.createNode(), TEST_TYPE );
                    setProperties( rel, properties );
                    return rel;
                }

                @Override
                public void delete( Relationship entity )
                {
                    entity.delete();
                }
            };

    private static void setProperties( PropertyContainer entity, Object... properties )
    {
        for ( Map.Entry<String, Object> entry : MapUtil.map( properties ).entrySet() )
        {
            entity.setProperty( entry.getKey(), entry.getValue() );
        }
    }
}
