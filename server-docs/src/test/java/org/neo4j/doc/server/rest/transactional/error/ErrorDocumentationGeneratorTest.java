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
package org.neo4j.doc.server.rest.transactional.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ErrorDocumentationGeneratorTest {
    @Test
    void tablesShouldFormatAsAsciiDoc() {
        // Given
        ErrorDocumentationGenerator.Table table = new ErrorDocumentationGenerator.Table();
        table.setCols("COLS");
        table.setHeader("A", "B");
        table.addRow(1, 2);
        table.addRow(3, 4);

        // When
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buf, false, StandardCharsets.UTF_8);
        table.print(out);
        out.flush();

        // Then
        String result = buf.toString(StandardCharsets.UTF_8);
        String n = System.lineSeparator();
        String expected =
                "[options=\"header\", cols=\"COLS\"]" + n +
                        "|===" + n +
                        "|A |B " + n +
                        "|1 |2 " + n +
                        "|3 |4 " + n +
                        "|===" + n;
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldGenerateTableOfClassifications() {
        // Given
        ErrorDocumentationGenerator gen = new ErrorDocumentationGenerator();

        // When
        ErrorDocumentationGenerator.Table table = gen.generateClassificationDocs();

        // Then
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        table.print(new PrintStream(buf, true, StandardCharsets.UTF_8));
        String actual = buf.toString(StandardCharsets.UTF_8);

        // More or less randomly chosen bits of text that should be in the output:
        assertThat(actual).containsSubsequence("DatabaseError", "Rollback");
    }

    @Test
    void shouldGenerateTableOfStatusCodes() {
        // Given
        ErrorDocumentationGenerator gen = new ErrorDocumentationGenerator();

        // When
        ErrorDocumentationGenerator.Table table = gen.generateStatusCodeDocs();

        // Then
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        table.print(new PrintStream(buf, true, StandardCharsets.UTF_8));
        String actual = buf.toString(StandardCharsets.UTF_8);

        // More or less randomly chosen bits of text that should be in the output:
        assertThat(actual).containsSubsequence("UnknownError", "An unknown error occurred");
    }
}
