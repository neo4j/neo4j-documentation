/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.metrics.docs;

import java.nio.file.Path;
import java.util.List;

import org.neo4j.internal.helpers.Args;
import org.neo4j.io.fs.FileUtils;

public class GenerateMetricsDocumentation
{
    private static final String OUTPUT_FILE_FLAG = "output";

    public static void main( String[] input ) throws Exception
    {
        Args args = Args.withFlags( OUTPUT_FILE_FLAG ).parse( input );

        List<String> metricsClassNames = args.orphans();
        if ( metricsClassNames.isEmpty() )
        {
            System.out.println( "Usage: GenerateMetricsDocumentation [--output file] className..." );
            System.exit( 1 );
        }

        MetricsAsciiDocGenerator generator = new MetricsAsciiDocGenerator();
        StringBuilder builder = new StringBuilder();
        for ( String className : metricsClassNames )
        {
            generator.generateDocsFor( className, builder );
        }

        String outputFileName = args.get( OUTPUT_FILE_FLAG );
        if ( outputFileName != null )
        {
            Path output = Path.of( outputFileName );
            System.out.println( "Saving docs for '" + metricsClassNames + "' in '" + output.toAbsolutePath() + "'." );
            FileUtils.writeToFile( output, builder.toString(), false );
        }
        else
        {
            System.out.println( builder.toString() );
        }
    }
}
