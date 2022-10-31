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

import com.neo4j.metrics.source.MetricGroup;
import com.neo4j.metrics.source.Metrics;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.neo4j.internal.helpers.Args;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.service.Services;

public class GenerateMetricsDocumentation {
    private static final String OUTPUT_FILE_FLAG = "output";

    public static void main(String[] input) throws Exception {
        Args args = Args.withFlags(OUTPUT_FILE_FLAG).parse(input);

        List<String> metricGroups = args.orphans();
        if (metricGroups.size() != 1) {
            System.out.println("Usage: GenerateMetricsDocumentation [--output file] metricGroup");
            System.exit(1);
        }
        MetricGroup metricGroup = MetricGroup.valueOf(metricGroups.get(0));

        MetricsAsciiDocGenerator generator = new MetricsAsciiDocGenerator();
        StringBuilder builder = new StringBuilder();

        Collection<Metrics> metricsClasses = Services.loadAll(Metrics.class);
        ArrayList<Metrics> sortedMetrics = new ArrayList<>(metricsClasses);
        sortedMetrics.sort(Comparator.comparing((metric) -> metric.getClass().getName()));

        for (Metrics metricsClass : sortedMetrics) {
            if (metricGroup.equals(metricsClass.getGroup())) {
                generator.generateDocsFor(metricsClass, builder);
            }
        }

        String outputFileName = args.get(OUTPUT_FILE_FLAG);
        if (outputFileName != null) {
            Path output = Path.of(outputFileName);
            System.out.println("Saving docs for '" + metricGroups + "' metrics in '" + output.toAbsolutePath() + "'.");
            FileUtils.writeToFile(output, builder.toString(), false);
        }
        else {
            System.out.println(builder.toString());
        }
    }
}
