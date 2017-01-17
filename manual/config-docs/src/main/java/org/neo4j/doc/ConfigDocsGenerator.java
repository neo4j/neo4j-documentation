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
package org.neo4j.doc;

import org.neo4j.configuration.ConfigValue;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.DocsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigDocsGenerator {

    public ConfigDocsGenerator() {
    }

    public void printSomeConfigValues() {
        List<String> keys = new ArrayList<>();
        keys.add("ha.host.data");
        keys.add("dbms.directories.logs");
        keys.add("metrics.neo4j.server.enabled");
        int counter = 0;
        DocsConfig docsConfig = DocsConfig.documentedSettings();
        for (Map.Entry<String, DocsConfigValue> entry : docsConfig.getDocumentableSettings(keys).entrySet()) {
            DocsConfigValue value = entry.getValue();
            System.out.printf("[%d] %s%n", ++counter, entry.getKey());
            System.out.printf("  description: %s%n", value.description().orElse("MISSING DESCRIPTION!"));
            if (value.isDeprecated()) {
                System.out.printf("  deprecation: %s%n", value.deprecationMessage());
            }
        }
    }

    public static void main(String[] args) {
        ConfigDocsGenerator generator = new ConfigDocsGenerator();
//        generator.printStuff();
//        generator.printOtherStuff();
//        generator.printAllConfigValues();
        generator.printSomeConfigValues();
    }

}
