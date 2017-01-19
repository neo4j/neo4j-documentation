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

import org.neo4j.configuration.ConfigValue;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class DocsConfigValue extends ConfigValue implements SettingDescription {

    private final String id;
    private final String name;
    private final Optional<String> description;
    private final String deprecationMessage;
    private final String validationDescription;
    private final String defaultValue;

    public DocsConfigValue(String id,
                           String name,
                           Optional<String> description,
                           String deprecationMessage,
                           String validationDescription,
                           Optional<?> defaultValue,
                           boolean isDeprecated, boolean hasDefault) {
        super(name, description, defaultValue);
        this.id = id;
        this.name = name;
        this.description = description;
        this.validationDescription = validationDescription;
        this.defaultValue = defaultValue.get().toString();
        this.deprecationMessage = deprecationMessage;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Optional<String> description() {
        return description;
    }

    @Override
    public boolean isDeprecated() {
        return null != deprecationMessage;
    }

    @Override
    public boolean hasDefault() {
        return defaultValue != null;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String deprecationMessage() {
        return deprecationMessage;
    }

    @Override
    public String validationMessage() {
        return validationDescription;
    }

    @Override
    public SettingDescription formatted(Function<String, String> format) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        SettingDescription that = (SettingDescription) o;
        return Objects.equals( name, that.name() ) &&
                Objects.equals( description, that.description() );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( name, description );
    }

    @Override
    public String toString()
    {
        return "SettingDescription{" + "id='" + id() + "\', name='" + name + "\', description='" + description + "\'}";
    }

}
