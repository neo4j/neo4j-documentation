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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a configuration item to be included into the generated Asciidoc.
 */
public final class SettingDescriptionImpl implements SettingDescription {

    private final String id;
    private final String name;
    private final Optional<String> description;
    private final String deprecationDescription;
    private final String validationDescription;
    private final String defaultValue;
    private final boolean isDeprecated;
    private final boolean hasDefault;
    private final boolean isEnterprise;

    public SettingDescriptionImpl( String id, String name, Optional<String> description,
                               String deprecationDescription,
                               String validationDescription, String defaultValue,
                               boolean isDeprecated, boolean hasDefault,boolean isEnterprise )
    {
        this.id = id;
        this.deprecationDescription = deprecationDescription;
        this.validationDescription = validationDescription;
        this.defaultValue = defaultValue;
        this.isDeprecated = isDeprecated;
        this.name = name.replace( "{", "\\{" ).replace( "}", "\\}" );
        this.description = description;
        this.hasDefault = hasDefault;
        this.isEnterprise = isEnterprise;
    }

    public SettingDescriptionImpl( String id, String name, Optional<String> description )
    {
        this( id, name, description, null, null, null, false, false, false );
    }

    @Override
    public String id()
    {
        return id;
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Optional<String> description()
    {
        return description;
    }

    @Override
    public boolean isDeprecated()
    {
        return isDeprecated;
    }

    @Override
    public boolean hasDefault()
    {
        //if ( !defaultValue.equals( DEFAULT_MARKER ) )
        return hasDefault;
    }

    @Override
    public boolean hasReplacement() {
        return false;
    }

    @Override
    public String replacedBy() {
        throw new UnsupportedOperationException("Old config docs generator doesn't handle 'Replaced by'");
    }

    @Override
    public boolean isInternal()
    {
        return false;
    }

    @Override
    public boolean isDynamic() {
        throw new UnsupportedOperationException("This implementation does not support 'dynamic' ");
    }

    @Override
    public boolean isEnterprise() {
        return isEnterprise;
    }

    @Override
    public String defaultValue()
    {
        return defaultValue;
    }

    @Override
    public String deprecationMessage()
    {
        // Note OBSOLETED & DEPRECATED
        return deprecationDescription;
    }

    @Override
    public String validationMessage()
    {
        // Note VALIDATION_MESSAGE
        return validationDescription;
    }

    /**
     * Return a new item with all prose descriptions formatted using
     * the passed-in format.
     */
    @Override
    public SettingDescription formatted(Function<String, String> format)
    {
        Function<String, String> f = ( str ) -> str == null ? null : format.apply(str);
        return new SettingDescriptionImpl(
                id, name,
                Optional.of(f.apply(description.get())),
                deprecationDescription,

                // I don't like this, but validationdescription contains a lot of
                // technical terms, and the formatters barf on it. Leave it out for now,
                // which is what the old impl did, and improve the formatters at some point
                validationDescription,
                defaultValue,
                isDeprecated, hasDefault, isEnterprise );
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
