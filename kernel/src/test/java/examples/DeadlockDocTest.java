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
package examples;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.doc.test.rule.EmbeddedDatabaseRule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.helpers.TransactionTemplate;
import org.neo4j.kernel.DeadlockDetectedException;

import java.util.concurrent.TimeUnit;

public class DeadlockDocTest
{
    @Rule
    public final EmbeddedDatabaseRule rule = new EmbeddedDatabaseRule();

    @Test
    public void transactionWithRetries() throws InterruptedException
    {
        Object result = transactionWithRetry();
    }

    @Test
    public void transactionWithTemplate() throws InterruptedException
    {
        GraphDatabaseService graphDatabaseService = rule.getGraphDatabaseAPI();

        // tag::template[]
        TransactionTemplate template = new TransactionTemplate(  ).retries( 5 ).backoff( 3, TimeUnit.SECONDS );
        // end::template[]

        // tag::usage-template[]
        Object result = template.with(graphDatabaseService).execute( transaction -> {
            Object result1 = null;
            return result1;
        } );
        // end::usage-template[]
    }

    private Object transactionWithRetry()
    {
        GraphDatabaseService graphDatabaseService = rule.getGraphDatabaseAPI();

        // tag::retry[]
        Throwable txEx = null;
        int RETRIES = 5;
        int BACKOFF = 3000;
        for ( int i = 0; i < RETRIES; i++ )
        {
            try ( Transaction tx = graphDatabaseService.beginTx() )
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
        else if ( txEx instanceof RuntimeException )
        {
            throw ((RuntimeException) txEx);
        }
        else
        {
            throw new TransactionFailureException( "Failed", txEx );
        }
        // end::retry[]
    }

    private Object doStuff( Transaction tx )
    {
        return null;
    }
}
