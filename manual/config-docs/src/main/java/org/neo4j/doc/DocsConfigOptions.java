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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class DocsConfigOptions extends ConfigOptions {

    private String deprecationMessage;

    public DocsConfigOptions(SettingGroup<?> settingGroup, Optional<String> description, String deprecationMessage) {
        super(settingGroup, description);
        this.deprecationMessage = deprecationMessage;
    }

    @Nonnull
    public List<DocsConfigValue> asDocsConfigValues(@Nonnull Map<String,String> validConfig)
    {
        return settingGroup().values( validConfig ).entrySet().stream()
                .map( val -> {
                    final String name = val.getKey();
                    if (null != deprecationMessage) {
                        deprecationMessage = String.format(deprecationMessage, name);
                    }
                    return new DocsConfigValue("config_" + (name.replace("(", "").replace(")", "")),
                            name,
                            description().orElse("NO DESCRIPTION"),
                            Optional.empty().toString(),
                            deprecationMessage,
                            Optional.empty().toString(),
//                            Optional.ofNullable(val.getValue()).get().toString(),
                            Optional.ofNullable(val.getValue()).toString(),
                            null==deprecationMessage, false, true);
                })
                .collect( Collectors.toList() );
    }

}
