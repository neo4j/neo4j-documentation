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

import static com.google.common.collect.Iterables.addAll;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;
import static org.neo4j.internal.helpers.collection.Iterators.addToCollection;
import static org.neo4j.internal.helpers.collection.Iterators.single;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

class SocnetTest {
    private static final Random r = new Random(System.currentTimeMillis());
    private static final int nrOfPersons = 20;

    private GraphDatabaseService graphDb;
    private PersonRepository personRepository;
    private DatabaseManagementService managementService;

    @TempDir
    Path folder;

    @BeforeEach
    void setup() throws Exception {
        managementService = new DatabaseManagementServiceBuilder(folder).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
        try (Transaction tx = graphDb.beginTx()) {
            personRepository = new PersonRepository(graphDb, tx);
            createPersons(tx);
            setupFriendsBetweenPeople(tx, 10);
            tx.commit();
        }
    }

    @AfterEach
    void teardown() {
        managementService.shutdown();
    }

    @Test
    void addStatusAndRetrieveIt() {
        String personName;
        try (Transaction tx = graphDb.beginTx()) {
            var person = getRandomPerson(tx);
            person.addStatus(tx, "Testing!");
            personName = person.getName();
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            StatusUpdate update = personRepository.getPersonByName(tx, personName).getStatus(tx).iterator().next();
            assertThat(update).isNotNull();
            assertThat(update.getStatusText()).isEqualTo("Testing!");
        }
    }

    @Test
    void multipleStatusesComeOutInTheRightOrder() {
        ArrayList<String> statuses = new ArrayList<>();
        statuses.add("Test1");
        statuses.add("Test2");
        statuses.add("Test3");

        try (Transaction tx = graphDb.beginTx()) {
            Person person = getRandomPerson(tx);
            for (String status : statuses) {
                person.addStatus(tx, status);
            }

            int i = statuses.size();
            for (StatusUpdate update : person.getStatus(tx)) {
                i--;
                assertThat(update.getStatusText()).isEqualTo(statuses.get(i));
            }
        }
    }

    @Test
    void removingOneFriendIsHandledCleanly() {
        Person person1;
        Person person2;
        long noOfFriends;
        try (Transaction tx = graphDb.beginTx()) {
            person1 = personRepository.getPersonByName(tx, "person#1");
            person2 = personRepository.getPersonByName(tx, "person#2");
            person1.addFriend(tx, person2);

            noOfFriends = person1.getNrOfFriends(tx);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            person1.removeFriend(tx, person2);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            long noOfFriendsAfterChange = person1.getNrOfFriends(tx);
            assertThat(noOfFriends).isEqualTo(noOfFriendsAfterChange + 1);
        }
    }

    @Test
    void retrieveStatusUpdatesInDateOrder() {
        Person person;
        int numberOfStatuses;
        try (Transaction tx = graphDb.beginTx()) {
            person = getRandomPersonWithFriends(tx);
            numberOfStatuses = 20;

            for (int i = 0; i < numberOfStatuses; i++) {
                Person friend = getRandomFriendOf(tx, person);
                friend.addStatus(tx, "Dum-deli-dum...");
            }
            tx.commit();
        }

        ArrayList<StatusUpdate> updates;
        try (Transaction tx = graphDb.beginTx()) {
            updates = fromIterableToArrayList(person.friendStatuses(tx));

            assertThat(updates).hasSize(numberOfStatuses);
            assertUpdatesAreSortedByDate(updates);
        }
    }

    @Test
    void friendsOfFriendsWorks() {
        try (Transaction tx = graphDb.beginTx()) {
            Person person = getRandomPerson(tx);
            Person friend = getRandomFriendOf(tx, person);

            for (Person friendOfFriend : friend.getFriends(tx)) {
                if (!friendOfFriend.equals(person)) { // You can't be friends with yourself.
                    assertThat(person.getFriendsOfFriends(tx)).contains(friendOfFriend);
                }
            }
        }
    }

    @Test
    void shouldReturnTheCorrectPersonFromAnyStatusUpdate() {
        try (Transaction tx = graphDb.beginTx()) {
            Person person = getRandomPerson(tx);
            person.addStatus(tx, "Foo");
            person.addStatus(tx, "Bar");
            person.addStatus(tx, "Baz");

            for (StatusUpdate status : person.getStatus(tx)) {
                assertThat(status.getPerson()).isEqualTo(person);
            }
        }
    }

    @Test
    void getPathBetweenFriends() throws Exception {
        deleteSocialGraph();

        Person start;
        Person middleMan1;
        Person middleMan2;
        Person endMan;
        try (Transaction tx = graphDb.beginTx()) {
            start = personRepository.createPerson(tx, "start");
            middleMan1 = personRepository.createPerson(tx, "middle1");
            middleMan2 = personRepository.createPerson(tx, "middle2");
            endMan = personRepository.createPerson(tx, "endMan");

            // Start -> middleMan1 -> middleMan2 -> endMan

            start.addFriend(tx, middleMan1);
            middleMan1.addFriend(tx, middleMan2);
            middleMan2.addFriend(tx, endMan);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            Iterable<Person> path = start.getShortestPathTo(tx, endMan, 4);

            assertPathIs(path, start, middleMan1, middleMan2, endMan);
            //assertThat( path, matchesPathByProperty(Person.NAME, "start", "middle1", "middle2", "endMan"));
        }
    }

    @Test
    void singleFriendRecommendation() throws Exception {
        deleteSocialGraph();
        Person a;
        Person e;
        try (Transaction tx = graphDb.beginTx()) {
            a = personRepository.createPerson(tx, "a");
            Person b = personRepository.createPerson(tx, "b");
            Person c = personRepository.createPerson(tx, "c");
            Person d = personRepository.createPerson(tx, "d");
            e = personRepository.createPerson(tx, "e");

            // A is friends with B,C and D
            a.addFriend(tx, b);
            a.addFriend(tx, c);
            a.addFriend(tx, d);

            // E is also friend with B, C and D
            e.addFriend(tx, b);
            e.addFriend(tx, c);
            e.addFriend(tx, d);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            Person recommendation = single(a.getFriendRecommendation(tx, 1).iterator());
            assertThat(recommendation).isEqualTo(e);
        }
    }

    @Test
    void weightedFriendRecommendation() throws Exception {
        deleteSocialGraph();
        Person a;
        Person e;
        Person f;
        try (Transaction tx = graphDb.beginTx()) {
            a = personRepository.createPerson(tx, "a");
            Person b = personRepository.createPerson(tx, "b");
            Person c = personRepository.createPerson(tx, "c");
            Person d = personRepository.createPerson(tx, "d");
            e = personRepository.createPerson(tx, "e");
            f = personRepository.createPerson(tx, "f");

            // A is friends with B,C and D
            a.addFriend(tx, b);
            a.addFriend(tx, c);
            a.addFriend(tx, d);

            // E is only friend with B
            e.addFriend(tx, b);

            // F is friend with B, C, D
            f.addFriend(tx, b);
            f.addFriend(tx, c);
            f.addFriend(tx, d);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            ArrayList<Person> recommendations = fromIterableToArrayList(a.getFriendRecommendation(tx, 2).iterator());
            assertThat(recommendations.get(0)).isEqualTo(f);
            assertThat(recommendations.get(1)).isEqualTo(e);
        }
    }

    private <T> ArrayList<T> fromIterableToArrayList(Iterator<T> iterable) {
        ArrayList<T> collection = new ArrayList<>();
        addToCollection(iterable, collection);
        return collection;
    }

    private void assertPathIs(Iterable<Person> path,
            Person... expectedPath) {
        ArrayList<Person> pathArray = new ArrayList<>();
        addAll(pathArray, path);
        assertThat(pathArray).hasSize(expectedPath.length);
        for (int i = 0; i < expectedPath.length; i++) {
            assertThat(pathArray.get(i)).isEqualTo(expectedPath[i]);
        }
    }

    private void setupFriendsBetweenPeople(Transaction transaction, int maxNrOfFriendsEach) {
        for (Person person : personRepository.getAllPersons(transaction)) {
            int nrOfFriends = r.nextInt(maxNrOfFriendsEach) + 1;
            for (int j = 0; j < nrOfFriends; j++) {
                person.addFriend(transaction, getRandomPerson(transaction));
            }
        }
    }

    private Person getRandomPerson(Transaction transaction) {
        return personRepository.getPersonByName(transaction, "person#"
                + r.nextInt(nrOfPersons));
    }

    private void deleteSocialGraph() {
        try (Transaction tx = graphDb.beginTx()) {
            for (Person person : personRepository.getAllPersons(tx)) {
                personRepository.deletePerson(tx, person);
            }
        }
    }

    private Person getRandomFriendOf(Transaction transaction, Person p) {
        ArrayList<Person> friends = new ArrayList<>();
        addToCollection(p.getFriends(transaction).iterator(), friends);
        return friends.get(r.nextInt(friends.size()));
    }

    private Person getRandomPersonWithFriends(Transaction transaction) {
        Person p;
        do {
            p = getRandomPerson(transaction);
        }
        while (p.getNrOfFriends(transaction) == 0);
        return p;
    }

    private void createPersons(Transaction transaction) throws Exception {
        for (int i = 0; i < nrOfPersons; i++) {
            personRepository.createPerson(transaction, "person#" + i);
        }
    }

    private void assertUpdatesAreSortedByDate(
            ArrayList<StatusUpdate> statusUpdates) {
        Date date = new Date(0);
        for (StatusUpdate update : statusUpdates) {
            assertTrue(date.getTime() < update.getDate().getTime());
            // TODO: Should be assertThat(date, lessThan(update.getDate));
        }
    }
}
