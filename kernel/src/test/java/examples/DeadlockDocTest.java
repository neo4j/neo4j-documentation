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
package examples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.harness.junit.extension.Neo4jExtension;
import org.neo4j.kernel.DeadlockDetectedException;

@ExtendWith( Neo4jExtension.class )
class DeadlockDocTest
{
    @Test
    void transactionWithRetries( GraphDatabaseService databaseService )
    {
        Object result = transactionWithRetry( databaseService );
    }

    private Object transactionWithRetry( GraphDatabaseService databaseService )
    {
        // tag::retry[]
        Throwable txEx = null;
        int RETRIES = 5;
        int BACKOFF = 3000;
        for ( int i = 0; i < RETRIES; i++ )
        {
            try ( Transaction tx = databaseService.beginTx() )
            {
                Object result = doStuff(tx);
                tx.success();
                return result;
            }
            catch ( Throwable ex )
            {
                txEx = ex;

                // Add whatever exceptions to retry on here
                if ( !(ex instanceof DeadlockDetectedException) )
                {
                    break;
                }
            }

            // Wait so that we don't immediately get into the same deadlock
            if ( i < RETRIES - 1 )
            {
                try
                {
                    Thread.sleep( BACKOFF );
                }
                catch ( InterruptedException e )
                {
                    throw new TransactionFailureException( "Interrupted", e );
                }
            }
        }

        if ( txEx instanceof TransactionFailureException )
        {
            throw ((TransactionFailureException) txEx);
        }
        else if ( txEx instanceof Error )
        {
            throw ((Error) txEx);
        }
        else
        {
            throw ((RuntimeException) txEx);
        }
        // end::retry[]
    }

    private Object doStuff( Transaction tx )
    {
        return null;
    }
}
