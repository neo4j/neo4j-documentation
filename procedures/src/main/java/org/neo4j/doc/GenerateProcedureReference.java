/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc;

import org.neo4j.helpers.Args;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

public class GenerateProcedureReference {

    private static final String DEFAULT_ID = "procedure-reference";
    private static final String DEFAULT_TITLE = "Procedure reference";

    public static void main(String[] args) throws IOException {
        Args arguments = Args.parse(args);
        printUsage();

        List<String> orphans = arguments.orphans();
        Path outFile = orphans.size() == 1 ? Paths.get(orphans.get(0)) : null;

        String id = arguments.has("id") || warnMissingOption("ID", "--id=my-id", DEFAULT_ID)
                ? arguments.get("id") : DEFAULT_ID;
        String title = arguments.has("title") || warnMissingOption("title", "--title=my-title", DEFAULT_TITLE)
                ? arguments.get("title") : DEFAULT_TITLE;

        System.out.printf("[+++] id=%s  title=%s", id, title);

        try {
            String doc = new ProcedureReferenceGenerator().document(id, title);
            if (null != outFile) {
                Path parentDir = outFile.getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                System.out.println("Saving docs in '" + outFile.toFile().getAbsolutePath() + "'.");
                Files.write(outFile, doc.getBytes());
            } else {
                System.out.println(doc);
            }
        } catch (NoSuchElementException nse) {
            nse.printStackTrace();
            throw nse;
        } catch (NoSuchFileException nsf) {
            nsf.printStackTrace();
            throw nsf;
        }
    }

    private static boolean warnMissingOption(String name, String example, String defaultValue) {
        System.out.printf("    [x] No %s provided (%s), using default: '%s'%n", name, example, defaultValue);
        return false;
    }

    private static void printUsage() {
        System.out.printf("Usage: ProcedureReferenceTool [--options] <out_file>%n");
        System.out.printf("    No options are mandatory but in most cases user will want to set --id and --title.%n");
        System.out.printf("    If no <out-file> is given prints to stdout.%n");
        System.out.printf("Options:%n");
        System.out.printf("    %-30s%s [%s]%n", "--id", "ID to use for settings summary", DEFAULT_ID);
        System.out.printf("    %-30s%s [%s]%n", "--title", "Title to use for settings summary", DEFAULT_TITLE);
    }

}
