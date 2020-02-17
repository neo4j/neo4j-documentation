/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.junit.Test;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.MapUtil;

import static org.neo4j.doc.test.index.Neo4jTestCase.assertContains;

public class TestLuceneIndex extends AbstractLuceneIndexTest {

    @Test
    public void exactIndexWithCaseInsensitiveWithBetterConfig() throws Exception
    {
        // START SNIPPET: exact-case-insensitive
        Index<Node> index = graphDb.index().forNodes( "exact-case-insensitive",
                MapUtil.stringMap( "type", "exact", "to_lower_case", "true" ) );
        Node node = graphDb.createNode();
        index.add( node, "name", "Thomas Anderson" );
        assertContains( index.query( "name", "\"Thomas Anderson\"" ), node );
        assertContains( index.query( "name", "\"thoMas ANDerson\"" ), node );
        // END SNIPPET: exact-case-insensitive
        restartTx();
        assertContains( index.query( "name", "\"Thomas Anderson\"" ), node );
        assertContains( index.query( "name", "\"thoMas ANDerson\"" ), node );
    }

}
