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

import org.neo4j.configuration.ConfigOptions;
import org.neo4j.configuration.Description;
import org.neo4j.configuration.DocumentedDefaultValue;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.ReplacedBy;
import org.neo4j.configuration.LoadableConfig;
import org.neo4j.graphdb.config.SettingGroup;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.logging.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.neo4j.kernel.configuration.HttpConnector.Encryption.NONE;
import static org.neo4j.kernel.configuration.HttpConnector.Encryption.TLS;
import static org.neo4j.kernel.configuration.Settings.TRUE;

public class DocsConfig extends Config {

    private final List<LoadableConfig> settingsClasses;
    private Map<String, String> validValues = null;

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

    private Map<String, String> validValues(LoadableConfig loadableConfig) {
        Map<String, String> lValidValues = new HashMap<>();

        for (Field f : loadableConfig.getClass().getDeclaredFields()) {
            try {
                Object publicSetting = f.get(loadableConfig);
                if (publicSetting instanceof SettingGroup) {
                    final Description documentation = f.getAnnotation(Description.class);
                    final Optional<String> description;
                    if (null == documentation) {
                        description = Optional.empty();
                    } else {
                        description = Optional.of(documentation.value());
                    }
                    final Deprecated deprecatedAnnotation = f.getAnnotation( Deprecated.class );
                    final boolean deprecated = null != deprecatedAnnotation;
                    final ReplacedBy replacedByAnnotation = f.getAnnotation( ReplacedBy.class);
                    final Optional<String> replacement;
                    if (replacedByAnnotation == null ) {
                        replacement = Optional.empty();
                    } else {
                        replacement = Optional.of( replacedByAnnotation.value() );
                    }
                    final DocumentedDefaultValue defValue = f.getAnnotation( DocumentedDefaultValue.class );
                    final Optional<String> documentedDefaultValue;
                    if ( defValue == null ) {
                        documentedDefaultValue = Optional.empty();
                    } else {
                        documentedDefaultValue = Optional.of( defValue.value() );
                    }
                    final boolean isInternal = f.isAnnotationPresent(Internal.class);
                    ConfigOptions configOptions = new ConfigOptions(
                                (SettingGroup) publicSetting,
                                description,
                                documentedDefaultValue,
                                deprecated,
                                isInternal,
                                replacement
                            );
                    configOptions.settingGroup().values(getRaw()).forEach(
                            (k, v) -> lValidValues.put(k, configOptions.settingGroup().toString())
                    );

                }
            } catch (IllegalAccessException ignored) {
                continue;
            }
        }
        return lValidValues;
    }

    public Map<String, String> validValues() {
        if (null == validValues) {
            validValues = new HashMap<>();
            settingsClasses.stream()
                    .map(this::validValues)
                    .forEach(it -> validValues.putAll(it));
        }
        return validValues;
    }

}
