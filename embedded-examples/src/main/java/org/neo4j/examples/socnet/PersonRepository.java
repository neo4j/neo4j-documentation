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
package org.neo4j.examples.socnet;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.internal.helpers.collection.IterableWrapper;

import static org.neo4j.examples.socnet.RelTypes.A_PERSON;

public class PersonRepository
{
    private final Label PERSON = Label.label( "Person" );
    private final GraphDatabaseService graphDb;
    private final Node personRefNode;

    public PersonRepository( GraphDatabaseService graphDb,Transaction transaction )
    {
        this.graphDb = graphDb;
        personRefNode = getPersonsRootNode( transaction );
    }

    private Node getPersonsRootNode( Transaction transaction )
    {
        Node node = transaction.findNode( PERSON, "reference", "person" );
        if ( node != null )
        {
            return node;
        }

        Node refNode = transaction.createNode();
        refNode.setProperty( "reference", "persons" );
        return refNode;
    }

    public Person createPerson( Transaction transaction, String name ) throws Exception
    {
        // to guard against duplications we use the lock grabbed on ref node
        // when
        // creating a relationship and are optimistic about person not existing
        Node newPersonNode = transaction.createNode(PERSON);
        transaction.getNodeById( personRefNode.getId() ).createRelationshipTo( newPersonNode, A_PERSON );
        // lock now taken, we can check if  already exist in index
        Node alreadyExist = transaction.findNode( PERSON, Person.NAME, name );
        if ( alreadyExist != null )
        {
            throw new Exception( "Person with this name already exists " );
        }
        newPersonNode.setProperty( Person.NAME, name );
        return new Person( graphDb, newPersonNode );
    }

    public Person getPersonByName( Transaction transaction, String name )
    {
        Node personNode = transaction.findNode( PERSON, Person.NAME, name );
        if ( personNode == null )
        {
            throw new IllegalArgumentException( "Person[" + name
                    + "] not found" );
        }
        return new Person( graphDb, personNode );
    }

    public void deletePerson( Transaction transaction, Person person )
    {
        Node personNode = person.getUnderlyingNode();
        for ( Person friend : person.getFriends( transaction ) )
        {
            person.removeFriend( transaction, friend );
        }
        personNode.getSingleRelationship( A_PERSON, Direction.INCOMING ).delete();

        for ( StatusUpdate status : person.getStatus( transaction ) )
        {
            Node statusNode = status.getUnderlyingNode();
            for ( Relationship r : statusNode.getRelationships() )
            {
                r.delete();
            }
            statusNode.delete();
        }

        personNode.delete();
    }

    public Iterable<Person> getAllPersons( Transaction transaction )
    {
        return new IterableWrapper<>( transaction.getNodeById( personRefNode.getId() ).getRelationships( A_PERSON ) )
        {
            @Override
            protected Person underlyingObjectToObject( Relationship personRel )
            {
                return new Person( graphDb, personRel.getEndNode() );
            }
        };
    }
}
