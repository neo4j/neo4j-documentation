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
package org.neo4j.examples.server.unmanaged;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.MapUtil;

// tag::ColleaguesCypherExecutionResource[]
@Path("/colleagues-cypher-execution")
public class ColleaguesCypherExecutionResource {
    private final ObjectMapper objectMapper;
    private final DatabaseManagementService dbms;

    public ColleaguesCypherExecutionResource(@Context DatabaseManagementService dbms) {
        this.dbms = dbms;
        this.objectMapper = new ObjectMapper();
    }

    private static final String COLLEAGUES_QUERY =
            "MATCH (p:Person {name: $personName })-[:ACTED_IN]->()<-[:ACTED_IN]-(colleague) RETURN colleague";

    @GET
    @Path("/{personName}")
    public Response findColleagues(@PathParam("personName") final String personName) {
        final Map<String,Object> params = MapUtil.map("personName", personName);

        StreamingOutput stream = os -> {
            JsonGenerator jg = objectMapper.getFactory().createGenerator(os, JsonEncoding.UTF8);
            jg.writeStartObject();
            jg.writeFieldName("colleagues");
            jg.writeStartArray();

            final GraphDatabaseService graphDb = dbms.database("neo4j");
            try (Transaction tx = graphDb.beginTx();
                    Result result = tx.execute(COLLEAGUES_QUERY, params)) {
                while (result.hasNext()) {
                    Map<String,Object> row = result.next();
                    jg.writeString(((Node) row.get("colleague")).getProperty("name").toString());
                }
                tx.commit();
            }

            jg.writeEndArray();
            jg.writeEndObject();
            jg.flush();
            jg.close();
        };

        return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
    }
}
// end::ColleaguesCypherExecutionResource[]
