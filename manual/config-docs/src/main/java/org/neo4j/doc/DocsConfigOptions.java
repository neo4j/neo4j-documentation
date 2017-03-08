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

import org.neo4j.configuration.ConfigOptions;
import org.neo4j.graphdb.config.SettingGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class DocsConfigOptions extends ConfigOptions {

    private boolean internal;
    private boolean deprecated;

    public DocsConfigOptions(SettingGroup<?> settingGroup,
                             Optional<String> description,
                             Optional<String> defaultValue,
                             boolean deprecated,
                             Optional<String> replacement,
                             boolean internal) {
        super(settingGroup,
              description,
              defaultValue,
              deprecated,
              internal,
              replacement);
        this.internal = internal;
        this.deprecated = deprecated;
    }

    @Nonnull
    public List<DocsConfigValue> asDocsConfigValues(@Nonnull Map<String,String> validConfig)
    {
        return settingGroup().values(validConfig).entrySet().stream()
                .map( val -> {
                    final String name = val.getKey();
                    return new DocsConfigValue(
                            "config_" + (name.replace("(", "").replace(")", "")),
                            name,
                            description(),
                            deprecated,
                            settingGroup().toString(),
                            documentedDefaultValue(),
                            Optional.ofNullable(val.getValue()),
                            internal,
                            replacement()
                    );
                })
                .collect( Collectors.toList() );
    }

    public Map<String, String> validValues(@Nonnull Map<String, String> validConfig) {
        Map<String, String> validValues = new HashMap<>();
        settingGroup().values(validConfig).forEach((k, v) -> validValues.put(k, settingGroup().toString()));
        return validValues;
    }

}
