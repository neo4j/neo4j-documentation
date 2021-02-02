/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {

    private final Path workingDirectory;
    private final String filenameFormat;

    public FileUtil(Path workingDirectory, String filenameFormat) {
        this.filenameFormat = filenameFormat;
        this.workingDirectory = workingDirectory;
    }

    public Path path(String name) {
        return workingDirectory.resolve(filename(name));
    }

    public PrintStream fileBackedPrintStream(Path path) throws IOException {
        return new PrintStream(file(path));
    }

    public PrintStream fileBackedPrintStream(String name) throws IOException {
        return fileBackedPrintStream(path(name));
    }

    public Path write(String content, Path filePath) throws IOException {
        Path parentDir = filePath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        return Files.write(filePath, content.getBytes());
    }

    public Path write(String content, String name) throws IOException {
        return write(content, path(name));
    }

    public String filename(String string) {
        return String.format(filenameFormat, string.replace(" ", "-").toLowerCase());
    }

    private File file(Path filePath) throws IOException {
        Path parentDir = filePath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
        return filePath.toFile();
    }

}
