/*
 * Copyright (c) 2002-2019 "Neo Technology,"
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
import org.neo4j.doc.util.FileUtil;
import org.neo4j.test.causalclustering.ClusterRule;

import javax.management.ObjectInstance;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class CausalClusterJmxDocsTest {

    private static final String QUERY = "org.neo4j:instance=kernel#0,*";
    private static final int EXPECTED_NUMBER_OF_BEANS = 11;
    private final Path outPath = Paths.get("target", "docs", "ops");

    @Rule
    public final ClusterRule clusterRule = new ClusterRule( getClass() );

    private JmxBeanDocumenter jmxBeanDocumenter;
    private FileUtil fileUtil;
    private Cluster cluster;

    @Before
    public void init() {
        this.jmxBeanDocumenter = new JmxBeanDocumenter();
        this.fileUtil = new FileUtil(outPath, "jmx-%s.adoc");
    }

    @Test
    public void shouldFindCausalClusteringJmxBeans() throws Exception {
        // given
        cluster = clusterRule
                .withNumberOfCoreMembers(2)
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
        List<ObjectInstance> objectInstances = jmxBeanDocumenter.query(QUERY).stream()
                .filter(o -> !o.getObjectName().getKeyProperty("name").equals("Configuration"))
                .sorted(Comparator.comparing(o -> o.getObjectName().getKeyProperty("name").toLowerCase()))
                .collect(Collectors.toList());

        // then
        assertEquals("Sanity checking the number of beans found;", EXPECTED_NUMBER_OF_BEANS, objectInstances.size());

        jmxBeanDocumenter.document(
                objectInstances,
                fileUtil,
                new AsciiDocListGenerator("jmx-list", "MBeans exposed by Neo4j", false)
        );
    }

}
