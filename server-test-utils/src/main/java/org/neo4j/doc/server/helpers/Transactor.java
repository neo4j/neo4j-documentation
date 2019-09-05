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
package org.neo4j.doc.server.helpers;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public class Transactor
{

    private final UnitOfWork unitOfWork;
    private final GraphDatabaseService graphDb;
    private final int attempts; // how many times to try, if the transaction fails for some reason

    public Transactor( GraphDatabaseService graphDb, UnitOfWork unitOfWork )
    {
        this( graphDb, unitOfWork, 1 );
    }

    public Transactor( GraphDatabaseService graphDb, UnitOfWork unitOfWork, int attempts )
    {
        assert attempts > 0 : "The Transactor should make at least one attempt at running the transaction.";
        this.unitOfWork = unitOfWork;
        this.graphDb = graphDb;
        this.attempts = attempts;
    }

    public void execute()
    {
        for ( int attemptsLeft = attempts - 1; attemptsLeft >= 0; attemptsLeft-- )
        {
            try ( Transaction tx = graphDb.beginTx() )
            {
                unitOfWork.doWork( tx );
                tx.commit();
            }
            catch ( RuntimeException e )
            {
                if ( attemptsLeft == 0 )
                {
                    throw e;
                }
            }
        }
    }

}
