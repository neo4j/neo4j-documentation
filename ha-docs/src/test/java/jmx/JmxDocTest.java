/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package jmx;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.cluster.ClusterSettings;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.SettingDescription;
import org.neo4j.doc.jmx.JmxBeanDocumenter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.TestHighlyAvailableGraphDatabaseFactory;
import org.neo4j.test.rule.TestDirectory;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class JmxDocTest {

    private static final String BEAN_NAME = "name";
    private static final String QUERY = "org.neo4j:*";
    private static final int EXPECTED_NUMBER_OF_BEANS = 13;
    private static final String EXCLUDE = "JMX Server";

    @ClassRule
    public static final TestDirectory test = TestDirectory.testDirectory();
    private static GraphDatabaseService db;
    private final Path outPath = Paths.get("target", "docs", "ops");
    private final Path includesFilePath = outPath.resolve("jmx-includes.adoc");
    private final JmxBeanDocumenter jmxBeanDocumenter = new JmxBeanDocumenter();

    @BeforeClass
    public static void startDb() throws Exception {
        File storeDir = test.graphDbDir();
        GraphDatabaseBuilder builder = new TestHighlyAvailableGraphDatabaseFactory().newEmbeddedDatabaseBuilder(storeDir);
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
        Stream<ObjectInstance> objectInstanceStream = jmxBeanDocumenter.query(QUERY).stream();

        List<Map.Entry<String, ObjectName>> sorted = objectInstanceStream
                .map(ObjectInstance::getObjectName)
                .filter(it -> !EXCLUDE.equalsIgnoreCase(it.getKeyProperty(BEAN_NAME)))
                .sorted(Comparator.comparing(o -> o.getKeyProperty(BEAN_NAME).toLowerCase()))
                .map(it -> new HashMap.SimpleEntry<>(it.getKeyProperty(BEAN_NAME), it))
                .collect(Collectors.toList());

        assertEquals("Sanity checking the number of beans found;", EXPECTED_NUMBER_OF_BEANS, sorted.size());
        document(sorted);
    }

    private void document(List<Map.Entry<String, ObjectName>> neo4jBeans) throws IOException, IntrospectionException, InstanceNotFoundException, ReflectionException {
        List<SettingDescription> settingDescriptions = new ArrayList<>();
        for (Map.Entry<String, ObjectName> beanEntry : neo4jBeans) {
            String details = jmxBeanDocumenter.asDetails(beanEntry.getValue(), beanEntry.getKey());
            write(details, path(beanEntry.getKey()));
            settingDescriptions.add(jmxBeanDocumenter.asSettingDescription(beanEntry.getValue(), beanEntry.getKey()));
        }
        AsciiDocListGenerator listGenerator = new AsciiDocListGenerator("jmx-list", "MBeans exposed by Neo4j", false);
        write(listGenerator.generateListAndTableCombo(settingDescriptions), path("List"));

        String includes = settingDescriptions.stream()
                .map(it -> String.format("include::jmx-%s.adoc[]%n%n", it.name().replace( " ", "-" ).toLowerCase()))
                .reduce("", String::concat);
        write(includes, includesFilePath);
    }

    private Path path(String name) {
        String filename = String.format("jmx-%s.adoc", name.replace(" ", "-").toLowerCase());
        return outPath.resolve(filename);
    }

    private void write(String content, Path filePath) throws IOException {
        Path parentDir = filePath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        Files.write(filePath, content.getBytes("UTF-8"));
    }

}
