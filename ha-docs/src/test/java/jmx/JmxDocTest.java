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
package jmx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.management.ObjectInstance;

import org.neo4j.cluster.ClusterSettings;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.jmx.JmxBeanDocumenter;
import org.neo4j.doc.test.rule.TestDirectory;
import org.neo4j.doc.util.FileUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;

import static org.junit.Assert.assertEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class JmxDocTest {

    private static final String BEAN_NAME = "name";
    private static final String QUERY = "org.neo4j:*";
    private static final int EXPECTED_NUMBER_OF_BEANS = 2;
    private static final List<String> INCLUDES = Arrays.asList("Branched Store", "High Availability");

    @ClassRule
    public static final TestDirectory test = TestDirectory.testDirectory();
    private static GraphDatabaseService db;
    private final Path outPath = Paths.get("target", "docs", "ops");
    private final JmxBeanDocumenter jmxBeanDocumenter = new JmxBeanDocumenter();
    private final FileUtil fileUtil = new FileUtil(outPath, "jmx-ha-%s.adoc");

    @BeforeClass
    public static void startDb() throws Exception {
        File storeDir = test.databaseDir();
        GraphDatabaseBuilder builder = new HighlyAvailableGraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir);
        db = builder.setConfig(ClusterSettings.server_id, "1")
                .setConfig(setting("jmx.port", STRING, NO_DEFAULT), "9913")
                .setConfig(ClusterSettings.initial_hosts, ":5001")
                .newGraphDatabase();
    }

    @AfterClass
    public static void stopDb() throws Exception {
        if (db != null) {
            db.shutdown();
        }
        db = null;
    }

    @Test
    public void dumpJmxInfo() throws Exception {
        List<ObjectInstance> objectInstances = jmxBeanDocumenter.query(QUERY).stream()
                .filter(it -> INCLUDES.contains(it.getObjectName().getKeyProperty(BEAN_NAME)))
                .sorted(Comparator.comparing(it -> it.getObjectName().getKeyProperty(BEAN_NAME).toLowerCase()))
                .collect(Collectors.toList());

        assertEquals("Sanity checking the number of beans found;", EXPECTED_NUMBER_OF_BEANS, objectInstances.size());
        jmxBeanDocumenter.document(
                objectInstances,
                fileUtil,
                new AsciiDocListGenerator("ha-only-jmx-list", "MBeans exposed by Neo4j in High Availability mode", false)
        );
    }

}
