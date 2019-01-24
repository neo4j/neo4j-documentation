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
package org.neo4j.doc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.neo4j.configuration.ConfigValue;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.kernel.configuration.BoltConnector;
import org.neo4j.kernel.configuration.HttpConnector;
import org.neo4j.kernel.configuration.HttpConnector.Encryption;
import org.neo4j.kernel.configuration.Config;

public class ConfigDocsGenerator {

    private static final Pattern CONFIG_SETTING_PATTERN = Pattern.compile( "\\+?[a-z0-9]+((\\.|_)[a-z0-9]+)+\\+?" );
    private static final Pattern ENDS_WITH_WORD_CHAR = Pattern.compile("\\w$");
    private static final String IFDEF_HTMLOUTPUT = String.format("ifndef::nonhtmloutput[]%n");
    private static final String IFDEF_NONHTMLOUTPUT = String.format("ifdef::nonhtmloutput[]%n");
    private static final String ENDIF = String.format("endif::nonhtmloutput[]%n%n");
    private final Config config;
    List<SettingDescription> settingDescriptions;
    private PrintStream out;

    public ConfigDocsGenerator() {
        BoltConnector boltConnector = new BoltConnector("bolt");
        HttpConnector httpConnector = new HttpConnector( "http", Encryption.NONE );
        HttpConnector httpsConnector = new HttpConnector( "https", Encryption.TLS );
        Map<String, String> connectorSettings = Arrays.stream(new Setting[]{
                boltConnector.advertised_address,
                boltConnector.encryption_level,
                boltConnector.listen_address,
                boltConnector.thread_pool_keep_alive,
                boltConnector.thread_pool_max_size,
                boltConnector.thread_pool_min_size,
                httpConnector.listen_address,
                httpConnector.advertised_address,
                httpsConnector.listen_address,
                httpsConnector.advertised_address
        }).collect(Collectors.toMap(Setting::name, Setting::getDefaultValue));
        Config.Builder builder = Config.builder().withServerDefaults().withSettings(connectorSettings);
        config = builder.build();
    }

    public String document(Predicate<ConfigValue> filter, String id, String title, String idPrefix) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = new PrintStream( baos );
        settingDescriptions = config.getConfigValues().values().stream()
                .filter(filter)
                .sorted(Comparator.comparing(ConfigValue::name))
                .map(c -> new DocsConfigValue(
                        idFromName(idPrefix, c.name()),
                        c.name(),
                        c.description(),
                        c.deprecated(),
                        c.valueDescription(),
                        c.documentedDefaultValue().isPresent() ? c.documentedDefaultValue() : valueAsString(c),
                        c.internal(),
                        c.replacement(),
                        c.dynamic()
                ))
                .collect(Collectors.toList());
        out.print(documentSummary(id, title, settingDescriptions));
        settingDescriptions.forEach(this::documentForAllOutputs);
        out.flush();
        return baos.toString();
    }

    private String idFromName(String idPrefix, String name) {
        return idPrefix + name.replace('<', '-').replace('>', '-');
    }

    private Optional<String> valueAsString(ConfigValue configValue) {
        Optional<String> valueString;
        try {
            Object value = configValue.value().get();
            if (value instanceof java.time.Duration) {
                valueString = Optional.of(parseDurationValue((Duration) value));
            } else {
                valueString = Optional.of(String.valueOf(value));
            }
        } catch (NoSuchElementException ex) {
            System.out.printf("    [x] failed to get value for setting `%s`%n", configValue.name());
            valueString = Optional.empty();
        }
        return valueString;
    }

    private String parseDurationValue(Duration value) {
        long ms = value.toMillis();
        if (ms % 1000 == 0) {
            return String.format("%ds", ms / 1000);
        } else {
            return String.format("%dms", ms);
        }
    }

    private String documentSummary(String id, String title, List<SettingDescription> settingDescriptions) {
        return new AsciiDocListGenerator(id, title, true).generateListAndTableCombo(settingDescriptions);
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
                        "|Description%n" +
                        "a|%s%n" +
                        "|Valid values%n" +
                        "a|%s%n",
                item.id(), item.name(),
                item.description().orElse("No description available."), item.validationMessage() );

        if (item.isDynamic()) {
            out.printf("|Dynamic a|true%n");
        }

        if (item.hasDefault()) {
            out.printf("|Default value%n" +
                       "m|%s%n",
                    item.defaultValue());
        }

        if (item.isDeprecated()) {
            out.printf( "|Deprecated%n" +
                        "a|%s%n",
                    item.deprecationMessage());
            if (item.hasReplacement()) {
                StringBuilder sb = new StringBuilder();
                Matcher matcher = CONFIG_SETTING_PATTERN.matcher(item.replacedBy());
                while (matcher.find()) {
                    if (0 < sb.length()) {
                        sb.append(", ");
                    }
                    sb.append(settingReferenceForHTML(matcher.group()));
                }
                out.printf("|Replaced by%n" +
                           "a|%s%n",
                        sb.toString());
            }
        }
        if (item.isInternal()) {
            out.printf("|Internal%n" +
                       "a|%s is an internal, unsupported setting.%n",
                    item.name());
        }

        out.printf("|===%n%n");
    }

    private String formatParagraph( String settingName, String paragraph, Function<String, String> renderReferenceToOtherSetting ) {
        return ensureEndsWithPeriod( transformSettingNames( paragraph, settingName, renderReferenceToOtherSetting ) );
    }

    private boolean shouldCreateCrossReference(String candidateSettingName) {
        return settingDescriptions.stream().anyMatch(p -> p.name().equals(candidateSettingName));
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
            else if (!shouldCreateCrossReference(match)) {
                // it's not a setting name, so do nothing
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
        if (ENDS_WITH_WORD_CHAR.matcher(message).find()) {
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
