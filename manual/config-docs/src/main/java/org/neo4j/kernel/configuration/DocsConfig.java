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
package org.neo4j.kernel.configuration;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.LoadableConfig;
import org.neo4j.doc.DocsConfigOptions;
import org.neo4j.doc.DocsConfigValue;
import org.neo4j.doc.DocumentableSettingGetter;
import org.neo4j.graphdb.config.SettingGroup;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.neo4j.kernel.configuration.HttpConnector.Encryption.NONE;
import static org.neo4j.kernel.configuration.HttpConnector.Encryption.TLS;
import static org.neo4j.kernel.configuration.Settings.TRUE;

public class DocsConfig extends Config implements DocumentableSettingGetter {

    private final List<LoadableConfig> settingsClasses;
    private List<DocsConfigOptions> docsConfigOptions = null;
    private Map<String, DocsConfigValue> docsConfigValues = null;
    private Predicate<Field> filter = (f) -> !f.isAnnotationPresent(Internal.class);

    private DocsConfig(
            Optional<File> configFile,
            Map<String, String> overriddenSettings,
            Consumer<Map<String, String>> settingsPostProcessor,
            Collection<ConfigurationValidator> additionalValidators,
            Optional<Log> log,
            List<LoadableConfig> settingsClasses) {
        super(configFile, overriddenSettings, settingsPostProcessor, additionalValidators, log, settingsClasses);
        this.settingsClasses = settingsClasses;
    }

    public static DocsConfig documentedSettings() {
        return documentedSettings(Optional.empty(), emptyMap(), emptyList());
    }

    private static DocsConfig documentedSettings(Optional<File> configFile, Map<String, String> additionalConfig, Collection<ConfigurationValidator> additionalValidators) {
        ArrayList<ConfigurationValidator> validators = new ArrayList<>();
        validators.addAll( additionalValidators );
        validators.add( new ServerConfigurationValidator() );

        HttpConnector http = new HttpConnector( "http", NONE );
        HttpConnector https = new HttpConnector( "https", TLS );
        BoltConnector bolt = new BoltConnector( "bolt" );

        DocsConfig docsConfig = new DocsConfig(
                configFile,
                additionalConfig,
                settings -> {
                    settings.putIfAbsent( GraphDatabaseSettings.auth_enabled.name(), TRUE );
                    settings.putIfAbsent( http.enabled.name(), TRUE );
                    settings.putIfAbsent( https.enabled.name(), TRUE );
                    settings.putIfAbsent( bolt.enabled.name(), TRUE );
                },
                validators,
                Optional.empty(),
                LoadableConfig.allConfigClasses());

        return docsConfig;
    }

    private List<DocsConfigOptions> getDocsConfigOptions(LoadableConfig loadableConfig) {
        List<DocsConfigOptions> configOptions = new ArrayList<>();

        for (Field f : loadableConfig.getClass().getDeclaredFields()) {
            try {
                Object publicSetting = f.get(loadableConfig);
                if (publicSetting instanceof SettingGroup && filter.test(f)) {
                    final Description documentation = f.getAnnotation(Description.class);
                    final Optional<String> description;
                    if (null == documentation) {
                        description = Optional.empty();
                    } else {
                        description = Optional.of(documentation.value());
                    }
                    String deprecationMessage = f.isAnnotationPresent( Obsoleted.class )
                            ? f.getAnnotation( Obsoleted.class ).value()
                            : f.isAnnotationPresent( Deprecated.class )
                            ? "The `%s` configuration setting has been deprecated."
                            : null;
                    configOptions.add(new DocsConfigOptions((SettingGroup) publicSetting, description, deprecationMessage));
                }
            } catch (IllegalAccessException ignored) {
                // Field is private, ignore it
                continue;
            }
        }
        return configOptions;
    }

    private List<DocsConfigOptions> getDocsConfigOptions() {
        if (null == docsConfigOptions) {
            docsConfigOptions = settingsClasses.stream()
                    .map( this::getDocsConfigOptions )
                    .flatMap( List::stream )
                    .collect( Collectors.toList() );
        }
        return docsConfigOptions;
    }

    private Map<String, DocsConfigValue> getDocsConfigValues() {
        if (null == docsConfigValues) {
            docsConfigValues = getDocsConfigOptions().stream()
                    .map(it -> it.asDocsConfigValues(getRaw()))
                    .flatMap(List::stream)
                    .sorted((a, b) -> a.name().compareTo(b.name()))
                    .collect(Collectors.toMap(DocsConfigValue::name, it -> it, (val1, val2) -> {
                        throw new RuntimeException("Duplicate setting: " + val1.name() + ": " + val1 + " and " + val2);
                    }, LinkedHashMap::new));
        }
        return docsConfigValues;
    }

    @Override
    public DocsConfigValue getDocumentableSetting(String settingKey) {
        return getDocsConfigValues().get(settingKey);
    }

    @Override
    public Map<String, DocsConfigValue> getDocumentableSettings(List<String> settingKeys) {
        return new HashMap<String, DocsConfigValue>() {{
            getDocsConfigValues().entrySet().stream().filter(it -> settingKeys.contains(it.getKey())).forEach(e -> put(e.getKey(), e.getValue()));
        }};
    }

    @Override
    public Map<String, DocsConfigValue> getAllDocumentableSettings() {
        return getAllDocumentableSettings(f -> {
            return f.isAnnotationPresent(Internal.class);
        });
    }

    public Map<String, DocsConfigValue> getAllDocumentableSettings(Predicate<Field> filter) {
        return getDocsConfigValues();
    }

}
