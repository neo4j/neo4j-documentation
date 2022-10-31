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
package org.neo4j.doc.kernel.impl.proc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Utility to create jar files containing classes from the current classpath.
 */
public class JarBuilder {
    public URL createJarFor(File f, Class<?>... classesToInclude) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(f);
                JarOutputStream jarOut = new JarOutputStream(fout)) {
            for (Class<?> target : classesToInclude) {
                String fileName = target.getName().replace(".", "/") + ".class";
                jarOut.putNextEntry(new ZipEntry(fileName));
                jarOut.write(classCompiledBytes(fileName));
                jarOut.closeEntry();
            }
        }
        return f.toURI().toURL();
    }

    private byte[] classCompiledBytes(String fileName) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (in.available() > 0) {
                out.write(in.read());
            }

            return out.toByteArray();
        }
    }
}

