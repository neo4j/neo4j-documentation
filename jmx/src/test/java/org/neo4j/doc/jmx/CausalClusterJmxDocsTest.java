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
package org.neo4j.doc.jmx;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.test.rule.TestDirectory;
import org.neo4j.doc.util.FileUtil;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.server.enterprise.OpenEnterpriseNeoServer;

import javax.management.ObjectInstance;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.neo4j.kernel.GraphDatabaseDependencies.newDependencies;

public class CausalClusterJmxDocsTest {

    private static final String QUERY = "org.neo4j:instance=kernel#0,*";
    private static final int EXPECTED_NUMBER_OF_BEANS = 12;
    private final Path outPath = Paths.get("target", "docs", "ops");

    @Rule
    public TestDirectory testDirectory = TestDirectory.testDirectory();

    private JmxBeanDocumenter jmxBeanDocumenter;
    private FileUtil fileUtil;
    private List<OpenEnterpriseNeoServer> servers;

    /**
     * @param ids The IDs for all servers. Used to build a value for `initial_discovery_members`.
     * @param id  The ID for the server being configured. Used to set unique port numbers and database directory.
     * @return A {@link Config.Builder config builder} which can be further augmented before built and used.
     */
    private Config.Builder config(Integer[] ids, int id) {
        String initialDiscoveryMembers = String.join(",", Arrays.stream(ids).map(it -> String.format("localhost:500%d", it)).collect(Collectors.toList()));
        return Config.builder()
                .withServerDefaults()
                .withSetting("dbms.mode", "CORE")
                .withSetting("causal_clustering.initial_discovery_members", initialDiscoveryMembers)
                .withSetting("causal_clustering.minimum_core_cluster_size_at_formation", "2")
                .withSetting("causal_clustering.discovery_listen_address", ":500" + Integer.toString(id))
                .withSetting("causal_clustering.transaction_listen_address", ":600" + Integer.toString(id))
                .withSetting("causal_clustering.raft_listen_address", ":700" + Integer.toString(id))
                .withSetting("dbms.backup.enabled", "false")
                .withSetting("dbms.connector.bolt.listen_address", ":" + Integer.toString(7687 + id))
                .withSetting("dbms.connector.http.listen_address", ":" + Integer.toString(7474 + id))
                .withSetting("dbms.connector.https.listen_address", ":" + Integer.toString(7484 + id))
                .withSetting("dbms.directories.data", testDirectory.directory("server" + Integer.toString(id)).getAbsolutePath());
    }

    private OpenEnterpriseNeoServer server(Config config) {
        NullLogProvider logProvider = NullLogProvider.getInstance();
        return new OpenEnterpriseNeoServer(config, newDependencies().userLogProvider(logProvider), logProvider);
    }

    @Before
    public void init() throws InterruptedException, ExecutionException {
        jmxBeanDocumenter = new JmxBeanDocumenter();
        fileUtil = new FileUtil(outPath, "jmx-%s.adoc");

        // Configure two Core Servers; one with JMX enabled, one without.
        Integer[] serverIds = new Integer[]{0, 1};
        servers = new ArrayList<>();
        servers.add(server(config(serverIds, 0)
                        .withSetting("jmx.port", "9913")
                        .build()
                )
        );
        servers.add(server(config(serverIds, 1)
                        .withSetting("unsupported.dbms.jmx_module.enabled", "false")
                        .build()
                )
        );

        // OpenEnterpriseNeoServer#start() is blocking, so use an ExecutorService.
        // This is could probably be a little prettier.
        ExecutorService es = Executors.newCachedThreadPool();
        List<? extends Future<?>> futures = servers.stream().map(server -> es.submit(() -> {
            try {
                server.start();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        })).collect(Collectors.toList());
        for (Future<?> future : futures) {
            future.get();
        }
        es.shutdown();
        es.awaitTermination(5, TimeUnit.MINUTES);
    }

    @After
    public void exit() {
        servers.forEach(OpenEnterpriseNeoServer::stop);
    }

    @Test
    public void shouldFindCausalClusteringJmxBeans() throws Exception {
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
