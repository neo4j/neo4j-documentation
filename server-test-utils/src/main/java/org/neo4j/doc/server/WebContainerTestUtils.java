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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Properties;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.config.Setting;

public class WebContainerTestUtils
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

    public static File createTempDir(String name) throws IOException {
        Path tmpDirPath = Files.createTempDirectory( name );
        WebContainerTestUtils.recursiveDeleteOnShutdownHook( tmpDirPath );
        return tmpDirPath.toFile();
    }

    // public static File createTempConfigFile() throws IOException {
    //     File file = File.createTempFile("neo4j", "conf");
    //     file.delete();
    //     return file;
    // }

    // public static File createTempConfigFile( File parentDir )
    // {
    //     File file = new File( parentDir, "test-" + new Random().nextInt() + ".properties" );
    //     file.deleteOnExit();
    //     return file;
    // }

    public static String getRelativePath(File folder, Setting<Path> setting) {
        return folder.toPath().resolve(setting.defaultValue()).toString();
    }

    public static void addDefaultRelativeProperties(Map<String, String> properties, File temporaryFolder) {
        addRelativeProperty( temporaryFolder, properties, GraphDatabaseSettings.data_directory );
        addRelativeProperty( temporaryFolder, properties, GraphDatabaseSettings.logs_directory );
    }

    private static void addRelativeProperty(File temporaryFolder, Map<String, String> properties,
                                            Setting<Path> setting) {
        properties.put(setting.name(), getRelativePath(temporaryFolder, setting));
    }

    public static void writeConfigToFile(Map<String, String> properties, File file) {
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

    private static void storeProperties(File file, Properties properties) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(out);
        }
    }

    private static Properties loadProperties(File file) {
        Properties properties = new Properties();
        if (file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
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
