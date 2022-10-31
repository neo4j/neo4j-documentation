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
import java.util.function.Predicate;
import org.neo4j.function.Predicates;
import org.neo4j.internal.helpers.Args;

public class GenerateProcedureReference {

    private static final String DEFAULT_ID = "procedure-reference";
    private static final String DEFAULT_TITLE = "Procedure reference";
    private static final String DEFAULT_EDITION = "both";

    public static void main(String[] args) throws IOException {
        Args arguments = Args.parse(args);
        printUsage();

        List<String> orphans = arguments.orphans();
        Path outFile = orphans.size() == 1 ? Paths.get(orphans.get(0)) : null;

        String id = arguments.has("id") || warnMissingOption("ID", "--id=my-id", DEFAULT_ID)
                ? arguments.get("id") : DEFAULT_ID;
        String title = arguments.has("title") || warnMissingOption("title", "--title=my-title", DEFAULT_TITLE)
                ? arguments.get("title") : DEFAULT_TITLE;
        String edition = arguments.has("edition") || warnMissingOption("edition", "--edition=community", DEFAULT_EDITION)
                ? arguments.get("edition") : DEFAULT_EDITION;

        Predicate<ProcedureReferenceGenerator.Procedure> filter = filter(arguments);

        System.out.printf("[+++] id=%s  title=%s%n", id, title);

        try {
            String doc = new ProcedureReferenceGenerator().document(id, title, edition, filter);
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

    private static boolean warnMissingOption(String name, String example, String defaultValue) {
        System.out.printf("    [x] No %s provided (%s), using default: '%s'%n", name, example, defaultValue);
        return false;
    }

    private static void printUsage() {
        System.out.printf("Usage: ProcedureReferenceTool [--options] <out_file>%n");
        System.out.printf("    No options are mandatory but in most cases user will want to set --id and --title.%n");
        System.out.printf("    If no <out-file> is given prints to stdout.%n");
        System.out.printf("Options:%n");
        System.out.printf("    %-30s%s [%s]%n", "--id", "ID to use for procedures reference", DEFAULT_ID);
        System.out.printf("    %-30s%s [%s]%n", "--title", "Title to use for procedures reference", DEFAULT_TITLE);
        System.out.printf("    %-30s%s [%s]%n", "--filter", "Filter to apply, for example '^db.index.explicit.*` to only include procedures in that namespace",
                DEFAULT_TITLE);
        System.out.printf("    %-30s%s [%s]%n", "--edition", "Which Neo4j Edition to use. One of 'enterprise', 'community' or 'both'", DEFAULT_EDITION);
    }

    private static Predicate<ProcedureReferenceGenerator.Procedure> filter(Args arguments) {
        if (arguments.has("filter")) {
            return procedure -> procedure.name().matches(arguments.get("filter"));
        }
        return Predicates.alwaysTrue();
    }
}
