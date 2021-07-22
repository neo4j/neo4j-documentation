/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
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
package org.neo4j.doc.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.config.Setting;

public final class WebContainerTestUtils
{
    private WebContainerTestUtils() {}

    public static void recursiveDeleteOnShutdownHook(final Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        Files.walkFileTree(path,
                            new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path file, @SuppressWarnings("unused") BasicFileAttributes attrs) throws IOException {
                                    Files.delete(file);
                                    return FileVisitResult.CONTINUE;
                                }

                                @Override
                                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                                    if (e == null) {
                                        Files.delete(dir);
                                        return FileVisitResult.CONTINUE;
                                    }
                                    // directory iteration failed
                                    throw e;
                                }
                            }
                        );
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete temporary files at "+path, e);
                    }
                }
            }));
    }

    public static Path createTempDir(String name) throws IOException {
        Path tmpPath = Files.createTempDirectory( name );
        WebContainerTestUtils.recursiveDeleteOnShutdownHook( tmpPath );
        return tmpPath;

        //The WebContainerTestUtils.recursiveDeleteOnShutdownHook guarantees that the directory will be purged.
        //
        //For example:
        //
        //Path tmpPath = Files.createTempDirectory( "neo4j-test-x" );
        //tmpPath.toFile().deleteOnExit();
        //
        //The deleteOnExit requests that the file or directory should be deleted when the virtual machine terminates.
        //However you cannot delete a directory unless it is empty.
    }

    public static String getRelativePath(Path folder, Setting<Path> setting) {
        return folder.resolve(setting.defaultValue()).toString();
    }

    public static void addDefaultRelativeProperties(Map<String, String> properties, Path temporaryFolder) {
        addRelativeProperty( temporaryFolder, properties, GraphDatabaseSettings.data_directory );
        addRelativeProperty( temporaryFolder, properties, GraphDatabaseSettings.logs_directory );
    }

    private static void addRelativeProperty(Path temporaryFolder, Map<String, String> properties,
                                            Setting<Path> setting) {
        properties.put(setting.name(), getRelativePath(temporaryFolder, setting));
    }

    public static void writeConfigToFile(Map<String, String> properties, Path file) {
        Properties props = loadProperties(file);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
        }
        storeProperties(file, props);
    }

    public static String asOneLine(Map<String, String> properties) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            builder.append((builder.length() > 0 ? "," : ""));
            builder.append(property.getKey()).append("=").append(property.getValue());
        }
        return builder.toString();
    }

    private static void storeProperties(Path file, Properties properties) {
        OutputStream out = null;
        try {
            out = Files.newOutputStream(file);
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(out);
        }
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            InputStream in = null;
            try {
                in = Files.newInputStream( file );
                properties.load(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                safeClose(in);
            }
        }
        return properties;
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
