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
package org.neo4j.doc;

import java.util.List;

/**
 * Generate a table with links to more details. For PDF generation, a list is used instead of a table.
 */
public class AsciiDocListGenerator {
    private final String listId;
    private final String title;
    private final boolean shortenDescription;

    public AsciiDocListGenerator(String listId, String title, boolean shortenDescription) {
        this.listId = listId;
        this.title = title;
        this.shortenDescription = shortenDescription;
    }

    public String generateListAndTableCombo(List<SettingDescription> items) {
        StringBuilder sb = new StringBuilder(200 * items.size());
        StringBuilder print = new StringBuilder(100 * items.size());
        sb.append(String.format("// tag::%s[]%n", listId));
        if (listId != null) {
            sb.append("[[").append(listId).append(String.format("]]%n"));
        }
        if (title != null) {
            sb.append('.').append(title).append(System.lineSeparator());
        }
        sb.append(SettingsDocumenter.IFDEF_HTMLOUTPUT)
                .append(String.format("[options=\"header\"]%n"))
                .append(String.format("|===%n"))
                .append(String.format("|Name|Description%n"));
        print.append(SettingsDocumenter.IFDEF_NONHTMLOUTPUT);
        for (SettingDescription item : items) {
            String id = item.id();
            String name = item.name();
            String description = item.description().orElse(String.format("No description available for `%s`.", name));
            if (item.isEnterprise()) {
                description = "label:enterprise-edition[Enterprise only]" + description;
            }
            if (shortenDescription) {
                description = shortenDescription(description);
            }
            sb.append("|<<")
                    .append(id)
                    .append(',')
                    .append(name)
                    .append(">>|")
                    .append(description);

            print.append("* <<")
                    .append(id)
                    .append(',')
                    .append(name)
                    .append(">>: ")
                    .append(description);

            if (!description.endsWith(".")) {
                sb.append('.');
                print.append('.');
            }
            sb.append(System.lineSeparator());
            print.append(System.lineSeparator());
        }
        sb.append(String.format("|===%n"))
                .append(SettingsDocumenter.ENDIF);
        print.append(SettingsDocumenter.ENDIF)
                .append(System.lineSeparator());
        sb.append(print.toString());
        sb.append(String.format("// end::%s[]%n%n", listId));
        return sb.toString();
    }

    /**
     * Shorten the description to the first sentence. If that sentence contains "deprecated", include one more sentence in the shortened description. This is to
     * handle the common "This setting is deprecated." which is insufficient as a summary of the description.
     *
     * @param description
     * @return The description shortened to one or two sentences.
     */
    private String shortenDescription(String description) {
        int pos = description.indexOf(". ");
        if (-1 == pos) {
            pos = description.indexOf("; ");
        }
        else if (pos < 30 && description.substring(0, pos).contains("deprecated")) {
            pos = description.indexOf(". ", pos + 1);
        }
        if (pos > 10) {
            description = description.substring(0, pos) + ".";
        }
        return description;
    }
}
