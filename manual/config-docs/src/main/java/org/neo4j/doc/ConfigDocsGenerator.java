/*
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
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
package org.neo4j.doc;

import org.neo4j.function.Predicates;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.DocsConfig;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigDocsGenerator {

    private static final Pattern CONFIG_SETTING_PATTERN = Pattern.compile( "\\+?[a-z0-9]+((\\.|_)[a-z0-9]+)+\\+?" );
    // TODO: This one, and the blacklist below, exist because we try and infer what is a config name
    //       in prose text. This is fraught with accidental error. We should instead look into
    //       adopting a convention for how we mark references to other config options in the @Description
    //       et cetera, for instance using back-ticks: "`my.setting`".
    private static final Pattern NUMBER_OR_IP = Pattern.compile( "[0-9\\.]+" );
    private static final List<String> CONFIG_NAMES_BLACKLIST = Arrays.asList( "round_robin", "keep_all", "keep_last",
            "keep_none", "metrics.neo4j", "i.e", "e.g", "fixed_ascending", "fixed_descending", "high_limit",
            "dbms.cluster.routing.get", "example_provider_name", "ldap.example.com", "javax.naming", "apoc.convert",
            "apoc.load.json", "apoc.trigger.add", "branch_then_copy", "copy_then_branch", "neo4j.cert", "neo4j.key" );

    public static final String IFDEF_HTMLOUTPUT = String.format("ifndef::nonhtmloutput[]%n");
    public static final String IFDEF_NONHTMLOUTPUT = String.format("ifdef::nonhtmloutput[]%n");
    public static final String ENDIF = String.format("endif::nonhtmloutput[]%n%n");
    private DocsConfig docsConfig;
    private Config config;
    private PrintStream out;

    public ConfigDocsGenerator() {
        config = Config.serverDefaults();
        docsConfig = DocsConfig.documentedSettings();
    }

    public String documentWithWorkarounds(Predicate<DocsConfigValue> filter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = new PrintStream( baos );
        List<SettingDescription> settingDescriptions = docsConfig.getAllDocumentableSettings().values().stream()
                .map(c -> new DocsConfigValue(
                        "config_" + (c.name().replace("(", "").replace(")", "")),
                        c.name(),
                        Optional.of(c.description().orElse("no description, likely an internal setting")),
                        c.deprecated(),
                        "VALIDATION_DESCRIPTION",
                        c.getDocumentedDefaultValue(),
                        c.value(),
                        false,
                        c.replacement()

                ))
                .collect(Collectors.toList());
        out.print(documentSummary(settingDescriptions));
        settingDescriptions.forEach(this::documentForAllOutputs);
        out.flush();
        return baos.toString();
    }

    public String document(Predicate<DocsConfigValue> filter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = new PrintStream( baos );
        Map<String, Optional<String>> documentedDefaults = config.getDocumentedDefaults();
        List<SettingDescription> settingDescriptions = config.serverDefaults().getConfigValues().values().stream()
                .map(c -> new DocsConfigValue(
                        "config_" + (c.name().replace("(", "").replace(")", "")),
                        c.name(),
                        Optional.of(c.description().orElse("no description, likely an internal setting")),
                        c.deprecated(),
                        "VALIDATION_DESCRIPTION",
                        documentedDefaults.get(c.name()),
                        c.value(),
                        false,
                        c.replacement()

                ))
                .collect(Collectors.toList());
        out.print(documentSummary(settingDescriptions));
        settingDescriptions.forEach(this::documentForAllOutputs);
        out.flush();
        return baos.toString();
    }

    private String documentSummary(List<SettingDescription> settingDescriptions) {
        return new AsciiDocListGenerator("settings-all", "Settings", true).generateListAndTableCombo(settingDescriptions);
    }

    private void documentForAllOutputs(SettingDescription item) {
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForHTML ) )  );
    }

    private void documentForHTML( SettingDescription item ) {
        out.print( IFDEF_HTMLOUTPUT );
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForHTML ) )  );
        out.print( ENDIF );
    }

    private void documentForPDF( SettingDescription item ) {
        out.print( IFDEF_NONHTMLOUTPUT );
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForPDF ) ) );
        out.print( ENDIF );
    }

    private void document(SettingDescription item) {
        out.printf("[[%s]]%n" +
                        ".%s%n" +
                        "[cols=\"<1h,<4\"]%n" +
                        "|===%n" +
                        "|Description a|%s%n" +
                        "|Valid values a|%s%n",
                item.id(), item.name(),
                item.description().get(), item.validationMessage() );

        if (item.hasDefault()) {
            out.printf("|Default value m|%s%n", item.defaultValue() );
        }

        if (item.isDeprecated()) {
            out.printf( "|Deprecated a|%s%n", item.deprecationMessage() );
        }
        if (item.isInternal()) {
            out.printf("|Internal a|%s is an internal, unsupported setting.%n", item.name());
        }

        out.printf("|===%n");
    }
    private String formatParagraph( String settingName, String paragraph, Function<String, String> renderReferenceToOtherSetting ) {
        return ensureEndsWithPeriod( transformSettingNames( paragraph, settingName, renderReferenceToOtherSetting ) );
    }

    private String transformSettingNames( String text, String settingBeingRendered, Function<String, String> transform ) {
        Matcher matcher = CONFIG_SETTING_PATTERN.matcher( text );
        StringBuffer result = new StringBuffer( 256 );
        while ( matcher.find() ) {
            String match = matcher.group();
            if ( match.endsWith( ".log" ) ) {
                // a filenamne
                match = "_" + match + "_";
            }
            else if ( match.startsWith( "+" ) && match.endsWith( "+" ) ) {
                // marked as passthrough, strip the mark but otherwise do nothing
                match = match.replaceAll( "^\\+|\\+$", "" );
            }
            else if ( match.equals( settingBeingRendered ) ) {
                // don't link to the settings we're describing
                match = "`" + match + "`";
            }
            else if ( CONFIG_NAMES_BLACKLIST.contains( match ) ) {
                // an option value; do nothing
            }
            else if ( NUMBER_OR_IP.matcher( match ).matches() ) {
                // number or ip; do nothing
            }
            else {
                // If all fall through, assume this key refers to a setting name,
                // and render it as requested by the caller.
                match = transform.apply( match );
            }
            matcher.appendReplacement( result, match );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    private String ensureEndsWithPeriod(String message) {
        if (!message.endsWith( "." ) && !message.endsWith( ". ")) {
            message += ".";
        }
        return message;
    }

    private String settingReferenceForHTML(String settingName) {
        return "<<config_" + settingName + "," + settingName + ">>";
    }

    private String settingReferenceForPDF( String settingName ) {
        return "`" + settingName + "`";
    }

}
