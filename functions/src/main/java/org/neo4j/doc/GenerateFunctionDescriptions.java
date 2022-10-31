/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.doc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import org.neo4j.internal.helpers.Args;

public class GenerateFunctionDescriptions {

    public static void main(String[] args) throws IOException {
        Args arguments = Args.parse(args);
        printUsage();

        List<String> orphans = arguments.orphans();
        Path outFile = orphans.size() == 1 ? Paths.get(orphans.get(0)) : null;

        try {
            String doc = new FunctionDescriptionsGenerator().document();
            if (null != outFile) {
                Path parentDir = outFile.getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }
                System.out.println("Saving docs in '" + outFile.toFile().getAbsolutePath() + "'.");
                Files.write(outFile, doc.getBytes());
            }
            else {
                System.out.println(doc);
            }
        }
        catch (NoSuchElementException | NoSuchFileException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void printUsage() {
        System.out.printf("Usage: FunctionReferenceTool <out_file>%n");
        System.out.printf("    If no <out-file> is given prints to stdout.%n");
    }
}
