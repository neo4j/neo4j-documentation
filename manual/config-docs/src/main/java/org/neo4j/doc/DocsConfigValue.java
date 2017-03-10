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
    private final String valueDescription;
    private final Optional<String> defaultValue;
    private final boolean isInternal;
    private final Optional<String> replacement;

    public DocsConfigValue(String id,
                           String name,
                           Optional<String> description,
                           boolean isDeprecated,
                           String valueDescription,
                           Optional<String> defaultValue,
                           Optional<?> value,
                           boolean isInternal,
                           Optional<String> replacement) {
        super(  name,
                description,
                defaultValue,
                value,
                valueDescription,
                isInternal,
                isDeprecated,
                replacement);
        this.id = id;
        this.name = name;
        this.description = description;
        this.deprecationMessage = "The `%s` configuration setting has been deprecated.";
        this.valueDescription = valueDescription;
        this.defaultValue = defaultValue;
        this.isInternal = isInternal;
        this.replacement = replacement;
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
        return deprecated();
    }

    @Override
    public boolean hasDefault() {
        return defaultValue.isPresent();
    }

    @Override
    public boolean isInternal() {
        return isInternal;
    }

    @Override
    public String defaultValue() {
        return defaultValue.map(Object::toString).orElse(String.format("No default value available for `%s`.", name));
    }

    @Override
    public String deprecationMessage() {
        return String.format(deprecationMessage, name);
    }

    @Override
    public String validationMessage() {
        return valueDescription;
    }

    public boolean hasReplacement() {
        return replacement.isPresent();
    }

    @Override
    public String replacedBy() {
        return replacement().get();
    }

    @Override
    public SettingDescription formatted(Function<String, String> format)
    {
        Function<String, String> f = ( str ) -> str == null ? null : format.apply(str);
        return new DocsConfigValue(
                id, name,
                description.isPresent() ? Optional.of(f.apply(description.get())) : description,
                deprecated(),

                // I don't like this, but validationdescription contains a lot of
                // technical terms, and the formatters barf on it. Leave it out for now,
                // which is what the old impl did, and improve the formatters at some point
                valueDescription,
                defaultValue,
                value(),
                isInternal,
                replacement());
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
