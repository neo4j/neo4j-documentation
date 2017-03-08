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

import org.neo4j.function.Predicates;
import org.neo4j.helpers.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tool to generate config documenatation.
 */
public class ConfigDocsTool {

    public static void main(String[] args) throws IOException {
        printUsage();
        Args arguments = Args.parse(args);

        List<String> orphans = arguments.orphans();
        Path outFile = orphans.size() == 1 ? Paths.get(orphans.get(0)) : null;

        Predicate<DocsConfigValue> filter = filters(arguments);

        try {
            String doc = new ConfigDocsGenerator().document(filter);
            if (null != outFile) {
                Path parentDir = outFile.getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                System.out.println("Saving docs in '" + outFile.toFile().getAbsolutePath() + "'.");
                Files.write(outFile, doc.getBytes());
            } else {
                System.out.printf("%n====%nSomething has gone horribly wrong.%n");
                System.out.println(arguments);
                System.out.println("====");
            }
        } catch (NoSuchElementException nse) {
            nse.printStackTrace();
            throw nse;
        } catch (NoSuchFileException nsf) {
            nsf.printStackTrace();
            throw nsf;
        }

    }

    /**
     * Create a combined filter to apply to "all settings" based on arguments.
     * For each of the filters that can be specified as an argument to this tool, create a {@code Predicate<DocsConfigValue>}.
     * Combine these predicates and pass along to the {@code ConfigDocsGenerator}.
     * @param arguments Arguments passed to this tool.
     * @return A Predicate used for filtering which settings to document.
     */
    private static Predicate<DocsConfigValue> filters(Args arguments) {
        Predicate<DocsConfigValue> filters = Predicates.all(arguments.asMap().entrySet().stream()
                .<Predicate<DocsConfigValue>>flatMap(e -> {
            switch (e.getKey()) {
                // Include deprecated settings?
                // If true, no filter is added. If false, require {@code DocsConfigValue#isDeprecated()} to be false.
                case "deprecated":
                    return null == e.getValue() || "true".equalsIgnoreCase(e.getValue())
                            ? Stream.empty()
                            : Stream.of(v -> !v.isDeprecated());
                // Include only deprecated settings.
                case "deprecated-only":
                    return Stream.of(SettingDescription::isDeprecated);
                // Include internal settings?
                // If true, no filter is added. If false, require {@code DocsConfigValue#isInternal()} to be false.
                case "internal":
                    return null == e.getValue() || "true".equalsIgnoreCase(e.getValue())
                            ? Stream.empty()
                            : Stream.of(v -> !v.isInternal());
                // Include only the setting matching this name.
                case "name":
                    return Stream.of(v -> v.name().equals(e.getValue()));
                // Include settings matching any of these names.
                case "names":
                    return Stream.of(v -> Arrays.asList(e.getValue().split(",")).contains(v.name()));
                // Include settings matching this prefix (that are in this namespace).
                case "prefix":
                    return Stream.of(v -> v.name().startsWith(e.getValue()));
                default:
                    return Stream.empty();
            }
        }).collect(Collectors.toList()));
        return arguments.has("unsupported") ? filters : filters.and(v -> !v.isInternal());
    }

    private static void printUsage() {
        System.out.printf("Usage: ConfigDocsTool [--options] <out_file>%n");
        System.out.printf("    No options are mandatory. If no <out-file> is given prints to stdout.%n");
        System.out.printf("Options:%n");
        System.out.printf("    %-30s%s%n", "--deprecated", "Include deprecated settings [true]");
        System.out.printf("    %-30s%s%n", "--deprecated-only", "Include only deprecated settings [false]");
        System.out.printf("    %-30s%s%n", "--name=<name>", "Single setting by name []");
        System.out.printf("    %-30s%s%n", "--names=<name1>,<name2>", "Multiple settings by name []");
        System.out.printf("    %-30s%s%n", "--prefix=<prefix>", "All settings whose namespace match <prefix> []");
        System.out.printf("    %-30s%s%n", "--unsupported", "Include internal/unsupported settings [false]");

    }

}
