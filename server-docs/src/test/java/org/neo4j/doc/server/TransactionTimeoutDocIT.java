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
package org.neo4j.doc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.doc.server.helpers.CommunityWebContainerBuilder.builder;
import static org.neo4j.internal.helpers.collection.MapUtil.map;
import static org.neo4j.kernel.api.exceptions.Status.Transaction.TransactionNotFound;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.doc.server.helpers.TestWebContainer;

class TransactionTimeoutDocIT extends ExclusiveWebContainerTestBase {
    private TestWebContainer webContainer;

    @AfterEach
    public void stopTheServer() {
        if (webContainer != null) {
            webContainer.shutdown();
        }
    }

    @Test
    void shouldHonorReallyLowSessionTimeout() throws Exception {
        // Given
        webContainer = builder()
                .withProperty(GraphDatabaseSettings.transaction_timeout.name(), "1")
                .onRandomPorts()
                .usingDataDir(folder.getAbsolutePath()).build();

        String tx = HTTP.POST(txURI(), Collections.singletonList(map("statement", "CREATE (n)"))).location();

        // When
        Thread.sleep(1000 * 5);
        Map<String,Object> response = HTTP.POST(tx + "/commit").content();

        // Then
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> errors = (List<Map<String,Object>>) response.get("errors");
        assertThat(errors.get(0).get("code")).isEqualTo(TransactionNotFound.code().serialize());
    }

    private String txURI() {
        return webContainer.getBaseUri().toString() + "db/neo4j/tx";
    }
}
