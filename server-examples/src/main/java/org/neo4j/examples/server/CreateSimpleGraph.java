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
package org.neo4j.examples.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.MediaType;

public class CreateSimpleGraph {
    private static final String SERVER_ROOT_URI = "http://localhost:7474/db/neo4j/";

    public static void main(String[] args) throws URISyntaxException {
        checkDatabaseIsRunning();

        // tag::nodesAndProps[]
        URI firstNode = createNode();
        addProperty(firstNode, "name", "Joe Strummer");
        URI secondNode = createNode();
        addProperty(secondNode, "band", "The Clash");
        // end::nodesAndProps[]

        // tag::addRel[]
        URI relationshipUri = addRelationship(firstNode, secondNode, "singer",
                "{ \"from\" : \"1976\", \"until\" : \"1986\" }");
        // end::addRel[]

        // tag::addMetaToRel[]
        addMetadataToProperty(relationshipUri, "stars", "5");
        // end::addMetaToRel[]

        // tag::queryForSingers[]
        findSingersInBands(firstNode);
        // end::queryForSingers[]

        sendTransactionalCypherQuery("MATCH (n) WHERE has(n.name) RETURN n.name AS name");
    }

    private static void sendTransactionalCypherQuery(String query) {
        // tag::queryAllNodes[]
        final String txUri = SERVER_ROOT_URI + "transaction/commit";
        WebResource resource = Client.create().resource(txUri);

        String payload = "{\"statements\" : [ {\"statement\" : \"" + query + "\"} ]}";
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .post(ClientResponse.class);

        System.out.println(String.format(
                "POST [%s] to [%s], status code [%d], returned data: "
                        + System.lineSeparator() + "%s",
                payload, txUri, response.getStatus(),
                response.getEntity(String.class)));

        response.close();
        // end::queryAllNodes[]
    }

    private static void findSingersInBands(URI startNode)
            throws URISyntaxException {
        // tag::traversalDesc[]
        // TraversalDefinition turns into JSON to send to the Server
        TraversalDefinition t = new TraversalDefinition();
        t.setOrder(TraversalDefinition.DEPTH_FIRST);
        t.setUniqueness(TraversalDefinition.NODE);
        t.setMaxDepth(10);
        t.setReturnFilter(TraversalDefinition.ALL);
        t.setRelationships(new Relation("singer", Relation.OUT));
        // end::traversalDesc[]

        // tag::traverse[]
        URI traverserUri = new URI(startNode.toString() + "/traverse/node");
        WebResource resource = Client.create()
                .resource(traverserUri);
        String jsonTraverserPayload = t.toJson();
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(jsonTraverserPayload)
                .post(ClientResponse.class);

        System.out.println(String.format(
                "POST [%s] to [%s], status code [%d], returned data: "
                        + System.lineSeparator() + "%s",
                jsonTraverserPayload, traverserUri, response.getStatus(),
                response.getEntity(String.class)));
        response.close();
        // end::traverse[]
    }

    // tag::insideAddMetaToProp[]
    private static void addMetadataToProperty(URI relationshipUri,
            String name, String value) throws URISyntaxException {
        URI propertyUri = new URI(relationshipUri.toString() + "/properties");
        String entity = toJsonNameValuePairCollection(name, value);
        WebResource resource = Client.create()
                .resource(propertyUri);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(entity)
                .put(ClientResponse.class);

        System.out.println(String.format(
                "PUT [%s] to [%s], status code [%d]", entity, propertyUri,
                response.getStatus()));
        response.close();
    }

    // end::insideAddMetaToProp[]

    private static String toJsonNameValuePairCollection(String name,
            String value) {
        return String.format("{ \"%s\" : \"%s\" }", name, value);
    }

    private static URI createNode() {
        // tag::createNode[]
        final String nodeEntryPointUri = SERVER_ROOT_URI + "node";
        // http://localhost:7474/db/neo4j/node

        WebResource resource = Client.create()
                .resource(nodeEntryPointUri);
        // POST {} to the node entry point URI
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("{}")
                .post(ClientResponse.class);

        final URI location = response.getLocation();
        System.out.println(String.format(
                "POST to [%s], status code [%d], location header [%s]",
                nodeEntryPointUri, response.getStatus(), location.toString()));
        response.close();

        return location;
        // end::createNode[]
    }

    // tag::insideAddRel[]
    private static URI addRelationship(URI startNode, URI endNode,
            String relationshipType, String jsonAttributes)
            throws URISyntaxException {
        URI fromUri = new URI(startNode.toString() + "/relationships");
        String relationshipJson = generateJsonRelationship(endNode,
                relationshipType, jsonAttributes);

        WebResource resource = Client.create()
                .resource(fromUri);
        // POST JSON to the relationships URI
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(relationshipJson)
                .post(ClientResponse.class);

        final URI location = response.getLocation();
        System.out.println(String.format(
                "POST to [%s], status code [%d], location header [%s]",
                fromUri, response.getStatus(), location.toString()));

        response.close();
        return location;
    }
    // end::insideAddRel[]

    private static String generateJsonRelationship(URI endNode,
            String relationshipType, String... jsonAttributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"to\" : \"");
        sb.append(endNode.toString());
        sb.append("\", ");

        sb.append("\"type\" : \"");
        sb.append(relationshipType);
        if (jsonAttributes == null || jsonAttributes.length < 1) {
            sb.append("\"");
        }
        else {
            sb.append("\", \"data\" : ");
            for (int i = 0; i < jsonAttributes.length; i++) {
                sb.append(jsonAttributes[i]);
                if (i < jsonAttributes.length - 1) { // Miss off the final comma
                    sb.append(", ");
                }
            }
        }

        sb.append(" }");
        return sb.toString();
    }

    private static void addProperty(URI nodeUri, String propertyName,
            String propertyValue) {
        // tag::addProp[]
        String propertyUri = nodeUri.toString() + "/properties/" + propertyName;
        // http://localhost:7474/db/data/node/{node_id}/properties/{property_name}

        WebResource resource = Client.create()
                .resource(propertyUri);
        ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity("\"" + propertyValue + "\"")
                .put(ClientResponse.class);

        System.out.println(String.format("PUT to [%s], status code [%d]",
                propertyUri, response.getStatus()));
        response.close();
        // end::addProp[]
    }

    private static void checkDatabaseIsRunning() {
        // tag::checkServer[]
        WebResource resource = Client.create()
                .resource(SERVER_ROOT_URI);
        ClientResponse response = resource.get(ClientResponse.class);

        System.out.println(String.format("GET on [%s], status code [%d]",
                SERVER_ROOT_URI, response.getStatus()));
        response.close();
        // end::checkServer[]
    }
}
