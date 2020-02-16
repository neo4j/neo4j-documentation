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
package org.neo4j.doc.server;

import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.neo4j.doc.server.helpers.TestWebContainer;
import org.neo4j.server.configuration.ServerSettings;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.neo4j.doc.server.helpers.CommunityWebContainerBuilder.builder;
import static org.neo4j.internal.helpers.collection.MapUtil.map;
import static org.neo4j.kernel.api.exceptions.Status.Transaction.TransactionNotFound;

public class TransactionTimeoutDocIT extends ExclusiveWebContainerTestBase
{
    private TestWebContainer webContainer;

    @After
    public void stopTheServer()
    {
        if ( webContainer != null )
        {
            webContainer.shutdown();
        }
    }

    @Test
    public void shouldHonorReallyLowSessionTimeout() throws Exception
    {
        // Given
        webContainer = builder().withProperty( ServerSettings.transaction_idle_timeout.name(), "1" ).usingDataDir( folder.getAbsolutePath() ).build();

        String tx = HTTP.POST( txURI(), Collections.singletonList(map("statement", "CREATE (n)"))).location();

        // When
        Thread.sleep( 1000 * 5 );
        Map<String, Object> response = HTTP.POST( tx + "/commit" ).content();

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List<Map<String, Object>>) response.get( "errors" );
        assertThat( errors.get( 0 ).get( "code" ), equalTo( TransactionNotFound.code().serialize() ) );
    }

    private String txURI()
    {
        return webContainer.getBaseUri().toString() + "db/neo4j/tx";
    }
}
