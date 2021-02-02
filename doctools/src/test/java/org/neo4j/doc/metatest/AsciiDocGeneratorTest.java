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
package org.neo4j.doc.metatest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.neo4j.doc.tools.AsciiDocGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsciiDocGeneratorTest
{
    private File sectionDirectory;

    @TempDir
    File directory;

    @BeforeEach
    void setup() {
        sectionDirectory = new File( new File( directory, "testasciidocs" ), "testsection" );
    }

    @Test
    void dumpToSeparateFile() throws IOException
    {
        String reference = AsciiDocGenerator.dumpToSeparateFile(sectionDirectory, "test1", ".title1\ntest1-content");
        assertEquals(".title1\ninclude::includes/test1.asciidoc[]\n", reference);
        File includeDir = new File(sectionDirectory, "includes");
        File includeFile = new File(includeDir, "test1.asciidoc");
        assertTrue(includeFile.canRead());
        String fileContent = readFileAsString(includeFile);
        assertEquals("test1-content", fileContent);
    }

    @Test
    void dumpToSeparateFileWithType() throws IOException
    {
        String reference = AsciiDocGenerator.dumpToSeparateFileWithType( sectionDirectory, "console", "test2-content" );
        assertEquals("include::includes/console-1.asciidoc[]\n", reference);
        File includeDir = new File(sectionDirectory, "includes");
        File includeFile = new File(includeDir, "console-1.asciidoc");
        String fileContent = readFileAsString(includeFile);
        assertEquals("test2-content", fileContent);

        // make sure the next console doesn't overwrite the first one
        AsciiDocGenerator.dumpToSeparateFileWithType(sectionDirectory, "console", "test3-content");
        includeFile = new File(includeDir, "console-2.asciidoc");
        fileContent = readFileAsString(includeFile);
        assertEquals("test3-content", fileContent);
    }

    private String readFileAsString(File file) throws java.io.IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (BufferedInputStream f = new BufferedInputStream(new FileInputStream(file)))
        {
            f.read(buffer);
            return new String(buffer);
        }
    }

}
