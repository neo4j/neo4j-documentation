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

import org.neo4j.kernel.configuration.DocsConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigDocsGenerator {

    private DocsConfig docsConfig;
    private PrintStream out;

    public ConfigDocsGenerator() {
        this.docsConfig = DocsConfig.documentedSettings();
    }

    public String document()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = new PrintStream( baos );

        List<SettingDescription> settingDescriptions = docsConfig.getAllDocumentableSettings().values().stream()
                .filter(it -> !it.isDeprecated())
                .collect(Collectors.toList());
        out.print(new AsciiDocListGenerator("settings", "Settings", true).generateListAndTableCombo(settingDescriptions));
        out.printf("%d settings processed%n", settingDescriptions.size());

        out.flush();
        return baos.toString();
    }

    public static void main(String[] args) {
        ConfigDocsGenerator generator = new ConfigDocsGenerator();
//        generator.printStuff();
//        generator.printOtherStuff();
//        generator.printAllConfigValues();
//        generator.printSomeConfigValues();
        System.out.println(generator.document());
    }

}
