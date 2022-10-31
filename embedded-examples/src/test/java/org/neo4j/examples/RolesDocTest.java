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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createOutputSnippet;
import static org.neo4j.visualization.asciidoc.AsciidocHelper.createQueryResultSnippet;

import org.junit.jupiter.api.Test;
import org.neo4j.annotations.documented.Documented;
import org.neo4j.doc.test.GraphDescription.Graph;
import org.neo4j.doc.tools.JavaTestDocsGenerator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

public class RolesDocTest extends AbstractJavaDocTestBase {
    private static final String NAME = "name";

    public enum RoleRels implements RelationshipType {
        ROOT,
        PART_OF,
        MEMBER_OF
    }

    private static final String ROLES_DOC =
            "This is an example showing a hierarchy of\n" +
                    "roles.\n" +
                    "What's interesting is that a tree is not sufficient for storing this kind of structure,\n" +
                    "as elaborated below.\n" +
                    " \n" +
                    "image::roles.png[]\n" +
                    " \n" +
                    "This is an implementation of an example found in the article\n" +
                    "http://www.codeproject.com/Articles/22824/A-Model-to-Represent-Directed-Acyclic-Graphs-DAG-o[A Model to Represent Directed Acyclic Graphs (DAG) on SQL Databases]\n" +
                    "by http://www.codeproject.com/script/Articles/MemberArticles.aspx?amid=274518[Kemal Erdogan].\n" +
                    "The article discusses how to store http://en.wikipedia.org/wiki/Directed_acyclic_graph[\n" +
                    "directed acyclic graphs] (DAGs)\n" +
                    "in SQL based DBs. DAGs are almost trees, but with a twist: it may be possible to reach\n" +
                    "the same node through different paths. Trees are restricted from this possibility, which\n" +
                    "makes them much easier to handle. In our case it is ``Ali'' and ``Engin'',\n" +
                    "as they are both admins and users and thus reachable through these group nodes.\n" +
                    "Reality often looks this way and can't be captured by tree structures.\n" +
                    " \n" +
                    "In the article an SQL Stored Procedure solution is provided. The main idea,\n" +
                    "that also have some support from scientists, is to pre-calculate all possible (transitive) paths.\n" +
                    "Pros and cons of this approach:\n" +
                    " \n" +
                    "* decent performance on read\n" +
                    "* low performance on insert\n" +
                    "* wastes _lots_ of space\n" +
                    "* relies on stored procedures\n" +
                    " \n" +
                    "In Neo4j storing the roles is trivial. In this case we use +PART_OF+ (green edges) relationships\n" +
                    "to model the group hierarchy and +MEMBER_OF+ (blue edges) to model membership in groups.\n" +
                    "We also connect the top level groups to the reference node by +ROOT+ relationships.\n" +
                    "This gives us a useful partitioning of the graph. Neo4j has no predefined relationship\n" +
                    "types, you are free to create any relationship types and give them the semantics you want.\n" +
                    " \n" +
                    "Lets now have a look at how to retrieve information from the graph. The the queries are done using <<cypher, Cypher>>,\n" +
                    "the Java code is using the Neo4j Traversal API (see <<tutorial-traversal-java-api>>, which is part of <<advanced-usage>>).\n" +
                    "\n" +
                    "== Get the admins ==\n" +
                    " \n" +
                    "In Cypher, we could get the admins like this:\n" +
                    " \n" +
                    "@@query-get-admins\n" +
                    " \n" +
                    "resulting in:\n" +
                    " \n" +
                    "@@o-query-get-admins\n" +
                    "\n" +
                    "And here's the code when using the Java Traversal API:\n" +
                    "\n" +
                    "@@get-admins\n" +
                    " \n" +
                    "resulting in the output\n" +
                    " \n" +
                    "@@o-get-admins\n" +
                    " \n" +
                    "The result is collected from the traverser using this code:\n" +
                    " \n" +
                    "@@read-traverser\n" +
                    " \n" +
                    "== Get the group memberships of a user ==\n" +
                    " \n" +
                    "In Cypher:\n" +
                    " \n" +
                    "@@query-get-user-memberships\n" +
                    " \n" +
                    "@@o-query-get-user-memberships\n" +
                    " \n" +
                    "Using the Neo4j Java Traversal API, this query looks like:\n" +
                    " \n" +
                    "@@get-user-memberships\n" +
                    " \n" +
                    "resulting in:\n" +
                    " \n" +
                    "@@o-get-user-memberships\n" +
                    "\n" +
                    "== Get all groups ==\n" +
                    " \n" +
                    "In Cypher:\n" +
                    " \n" +
                    "@@query-get-groups\n" +
                    " \n" +
                    "@@o-query-get-groups\n" +
                    " \n" +
                    "In Java:\n" +
                    " \n" +
                    "@@get-groups\n" +
                    " \n" +
                    "resulting in:\n" +
                    " \n" +
                    "@@o-get-groups\n" +
                    " \n" +
                    "== Get all members of all groups ==\n" +
                    " \n" +
                    "Now, let's try to find all users in the system being part of any group.\n" +
                    "\n" +
                    "In Cypher, this looks like:\n" +
                    " \n" +
                    "@@query-get-members\n" +
                    " \n" +
                    "and results in the following output:\n" +
                    " \n" +
                    "@@o-query-get-members\n" +
                    " \n" +
                    "in Java:\n" +
                    " \n" +
                    "@@get-members\n" +
                    " \n" +
                    "@@o-get-members\n" +
                    " \n" +
                    "As seen above, querying even more complex scenarios can be done using comparatively short\n" +
                    "constructs in Cypher or Java.";

    @Test
    @Documented(ROLES_DOC)
    @Graph({"Admins ROOT Reference_Node", "Users ROOT Reference_Node",
            "HelpDesk PART_OF Admins", "Managers PART_OF Users",
            "Technicians PART_OF Users", "ABCTechnicians PART_OF Technicians",
            "Ali MEMBER_OF Users", "Ali MEMBER_OF Admins",
            "Engin MEMBER_OF Users", "Engin MEMBER_OF HelpDesk",
            "Demet MEMBER_OF HelpDesk", "Burcu MEMBER_OF Users",
            "Can MEMBER_OF Users", "Gul MEMBER_OF Managers",
            "Fuat MEMBER_OF Managers", "Hakan MEMBER_OF Technicians",
            "Irmak MEMBER_OF Technicians", "Jale MEMBER_OF ABCTechnicians"
    })
    public void user_roles_in_graphs(GraphDatabaseService graphDb) {
        // get Admins
        JavaTestDocsGenerator generator = gen.get();
        generator
                .addTestSourceSnippets(this.getClass(), "get-admins",
                        "get-user-memberships", "get-groups", "get-members", "read-traverser");
        System.out.println("All admins:");
        // tag::get-admins[]
        Node admins = getNodeByName("Admins");
        // end::get-admins[]

        try (Transaction tx = graphDb.beginTx()) {
            TraversalDescription traversalDescription = tx.traversalDescription()
                    .breadthFirst()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(RoleRels.PART_OF, Direction.INCOMING)
                    .relationships(RoleRels.MEMBER_OF, Direction.INCOMING);
            Traverser traverser = traversalDescription.traverse(tx.getNodeByElementId(admins.getElementId()));
            generator.addSnippet("o-get-admins", createOutputSnippet(traverserToString(traverser)));
            String query = "match ({name: 'Admins'})<-[:PART_OF*0..]-(group)<-[:MEMBER_OF]-(user) return user.name, group.name";
            generator.addSnippet("query-get-admins", createCypherSnippet(query));
            String result = tx.execute(query)
                    .resultAsString();
            assertTrue(result.contains("Engin"));
            generator.addSnippet("o-query-get-admins", createQueryResultSnippet(result));

            //Jale's memberships
            // tag::get-user-memberships[]
            Node jale = tx.getNodeByElementId(getNodeByName("Jale").getElementId());
            traversalDescription = tx.traversalDescription()
                    .depthFirst()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(RoleRels.MEMBER_OF, Direction.OUTGOING)
                    .relationships(RoleRels.PART_OF, Direction.OUTGOING);
            traverser = traversalDescription.traverse(jale);
            // end::get-user-memberships[]

            generator.addSnippet("o-get-user-memberships", createOutputSnippet(traverserToString(traverser)));
            query = "match ({name: 'Jale'})-[:MEMBER_OF]->()-[:PART_OF*0..]->(group) return group.name";
            generator.addSnippet("query-get-user-memberships", createCypherSnippet(query));
            result = tx.execute(query)
                    .resultAsString();
            assertTrue(result.contains("Users"));
            generator
                    .addSnippet("o-query-get-user-memberships",
                            createQueryResultSnippet(result));

            // get all groups
            // tag::get-groups[]
            Node referenceNode = tx.getNodeByElementId(getNodeByName("Reference_Node").getElementId());
            traversalDescription = tx.traversalDescription()
                    .breadthFirst()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(RoleRels.ROOT, Direction.INCOMING)
                    .relationships(RoleRels.PART_OF, Direction.INCOMING);
            traverser = traversalDescription.traverse(referenceNode);
            // end::get-groups[]

            generator.addSnippet("o-get-groups", createOutputSnippet(traverserToString(traverser)));
            query = "match ({name: 'Reference_Node'})<-[:ROOT]->()<-[:PART_OF*0..]-(group) return group.name";
            generator.addSnippet("query-get-groups", createCypherSnippet(query));
            result = tx.execute(query)
                    .resultAsString();
            assertTrue(result.contains("Users"));
            generator
                    .addSnippet("o-query-get-groups",
                            createQueryResultSnippet(result));

            //get all members
            // tag::get-members[]
            traversalDescription = tx.traversalDescription()
                    .breadthFirst()
                    .evaluator(
                            Evaluators.includeWhereLastRelationshipTypeIs(RoleRels.MEMBER_OF));
            traverser = traversalDescription.traverse(referenceNode);
            // end::get-members[]

            generator.addSnippet("o-get-members", createOutputSnippet(traverserToString(traverser)));
            query = "match ({name: 'Reference_Node'})<-[:ROOT]->(root), p=(root)<-[PART_OF*0..]-()<-[:MEMBER_OF]-(user) " +
                    "return user.name, min(length(p)) " +
                    "order by min(length(p)), user.name";
            generator.addSnippet("query-get-members", createCypherSnippet(query));
            result = tx.execute(query)
                    .resultAsString();
            assertTrue(result.contains("Engin"));
            generator
                    .addSnippet("o-query-get-members",
                            createQueryResultSnippet(result));

            /* more advanced example
            query = "start refNode=node("+ referenceNode.getId() +") " +
                    "match p=refNode<-[:ROOT]->parent<-[:PART_OF*0..]-group, group<-[:MEMBER_OF]-user return group.name, user.name, LENGTH(p) " +
                    "order by LENGTH(p)";
                    */
        }
    }

    private String traverserToString(Traverser traverser) {
        // tag::read-traverser[]
        String output = "";
        for (Path path : traverser) {
            Node node = path.endNode();
            output += "Found: " + node.getProperty(NAME) + " at depth: "
                    + (path.length() - 1) + "\n";
        }
        // end::read-traverser[]
        return output;
    }

    private Node getNodeByName(String string) {
        return data.get().get(string);
    }
}
