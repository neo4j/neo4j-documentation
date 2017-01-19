/*
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
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
package org.neo4j.kernel.configuration;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.configuration.ConfigValue;
import org.neo4j.doc.DocsConfigValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DocsConfigTest {

    Map<String, ConfigValue> configValues;

    @Before
    public void setUp() throws Exception {
        Config config = Config.serverDefaults();
        configValues = config.getConfigValues();
    }

    @Test
    public void getDocumentableSetting() throws Exception {
        final String settingKey = "dbms.directories.logs";
        DocsConfig docsConfig = DocsConfig.documentedSettings();
        DocsConfigValue docsConfigValue = docsConfig.getDocumentableSetting(settingKey);
        System.out.println(docsConfigValue.toString());
        assertTrue(
                docsConfigValue.description().get().equals(
                        configValues.get(settingKey).description().get())
        );
    }

    @Test
    public void getDocumentableSettings() throws Exception {
        final List<String> keys = new ArrayList<>();
        keys.add("ha.host.data");
        keys.add("dbms.directories.logs");
        keys.add("metrics.neo4j.server.enabled");
        DocsConfig docsConfig = DocsConfig.documentedSettings();
        Map<String, DocsConfigValue> docsConfigValues = docsConfig.getDocumentableSettings(keys);
        System.out.printf("configValues.keySet.size(): %d%n", configValues.keySet().size());
        keys.forEach(key -> assertTrue(String.format("Assertion failed for key <%s>%n", key),
                docsConfigValues.containsKey(key) &&
                        docsConfigValues.get(key).description().get().equals(configValues.get(key).description().get()))
        );
    }

    @Test
    public void getAllDocumentableSettings() throws Exception {
        DocsConfig docsConfig = DocsConfig.documentedSettings();
        Map<String, DocsConfigValue> docsConfigValues = docsConfig.getAllDocumentableSettings();
        docsConfigValues.keySet().forEach(key -> {
                assertTrue(
                        String.format("key: %s    configValues.get(key).description(): %s", key, configValues.get(key).description()),
               configValues.containsKey(key) &&
               docsConfigValues.get(key).description().equals(configValues.get(key).description()));
        });
    }

}
