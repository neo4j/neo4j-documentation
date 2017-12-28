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
package org.neo4j.doc.jmx;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.causalclustering.discovery.Cluster;
import org.neo4j.causalclustering.discovery.CoreClusterMember;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.SettingDescription;
import org.neo4j.test.causalclustering.ClusterRule;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class CausalClusterJmxDocsTest {

    private static final String QUERY = "org.neo4j:*";
    private final Path outPath = Paths.get("target", "docs", "ops");
    private final Path includesFilePath = outPath.resolve("jmx-includes.asciidoc");

    @Rule
    public final ClusterRule clusterRule = new ClusterRule( getClass() );

    private JmxBeanDocumenter jmxBeanDocumenter;
    private Cluster cluster;

    @Before
    public void init() {
        this.jmxBeanDocumenter = new JmxBeanDocumenter();
    }

    @Test
    public void shouldFindCausalClusteringJmxBeans() throws Exception {
        // given
        cluster = clusterRule
                .withNumberOfCoreMembers( 3 )
                .withInstanceCoreParam(setting("jmx.port", STRING, NO_DEFAULT), id -> Integer.toString(9913 + id))
                .startCluster();
        CoreClusterMember coreClusterMember = cluster.getCoreMemberById(0);

        // when
        String core0JmxPort = coreClusterMember.settingValue("jmx.port");
        String core1JmxPort = cluster.getCoreMemberById(1).settingValue("jmx.port");

        // then
        assertEquals("9913", core0JmxPort);
        assertNotEquals("9913", core1JmxPort);

        // when
        Collection<ObjectInstance> objectInstances = jmxBeanDocumenter.query(QUERY).stream()
                .collect(Collectors.toMap(it -> it.getObjectName().getKeyProperty("name"), p -> p, (p, q) -> p))
                .values();

        // then
        assertFalse(objectInstances.isEmpty());

        document(objectInstances);
    }

    private void document(Collection<ObjectInstance> objectInstances) throws IntrospectionException, InstanceNotFoundException, ReflectionException, IOException {
        List<SettingDescription> settingDescriptions = new ArrayList<>();
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String name = objectName.getKeyProperty("name");
            settingDescriptions.add(jmxBeanDocumenter.asSettingDescription(objectName, name));
            write(jmxBeanDocumenter.asDetails(objectName, name), path(name));
        }

        write(new AsciiDocListGenerator("jmx-list", "MBeans exposed by Neo4j", false).generateListAndTableCombo(settingDescriptions), path("List"));

        String includes = settingDescriptions.stream()
                .map(it -> String.format("include::jmx-%s.adoc[]%n%n", it.name().replace(" ", "-").toLowerCase()))
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
