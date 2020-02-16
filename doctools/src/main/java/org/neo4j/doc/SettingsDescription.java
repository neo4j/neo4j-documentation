/*
 * Copyright (c) 2002-2020 "Neo4j,"
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

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.GroupSetting;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.SettingImpl;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.internal.helpers.Exceptions;

/**
 * A meta description of a settings class, used to generate documentation.
 */
public class SettingsDescription
{
    /**
     * Create a description of a given class.
     */
    @SuppressWarnings( "unchecked" )
    public static SettingsDescription describe( Class<?> settingClass )
    {
        String classDescription = settingClass.isAnnotationPresent( Description.class )
              ? settingClass.getAnnotation( Description.class ).value()
              : "List of configuration settings";
        String settingsName = settingClass.getName().replace( "$", "-" );
        Object instance = GroupSetting.class.isAssignableFrom( settingClass ) ? groupInstance( settingClass ) : null;

        List<SettingDescription> settings = new LinkedList<>();
        Arrays.stream( FieldUtils.getAllFields( settingClass ) )
                .filter( f -> f.getType().isAssignableFrom( SettingImpl.class ) )
                .forEach( field ->
                {
                    try
                    {
                        field.setAccessible( true );
                        SettingImpl<Object> setting = (SettingImpl<Object>) field.get( instance );

                        String name = setting.name();
                        String id = "config_" + (name.replace( "(", "" ).replace( ")", "" ));
                        boolean deprecated = field.isAnnotationPresent( Deprecated.class );
                        String deprecationMsg = deprecated ? "The `" + name + "` configuration setting has been deprecated." : null;
                        String validationMsg = setting.toString();
                        Optional<String> descr = field.isAnnotationPresent( Description.class ) ? Optional.of( field.getAnnotation( Description.class ).value() ) : Optional.of( "No description" );
                        boolean hasDefault = setting.defaultValue() != null;
                        String defaultValue = hasDefault ? setting.valueToString( setting.defaultValue() ) : null;

                        settings.add( new SettingDescriptionImpl( id, name, descr, deprecationMsg, validationMsg, defaultValue, deprecated, hasDefault ) );
                    }
                    catch ( Exception e )
                    {
                        String msg = String.format( "Can not describe %s, reason: %s", settingClass.getSimpleName(), e.getMessage() );
                        throw new RuntimeException( msg, e );
                    }
                } );

        return new SettingsDescription(
                // Nested classes have `$` in the name, which is an asciidoc keyword
                settingsName,
                classDescription,
                settings );
    }

    private static Object groupInstance( Class<?> settingClass )
    {
        try
        {
            // Group classes are special, we need to instantiate them to read their
            // configuration, this is how the group config DSL works
            var constructor = settingClass.getConstructor( String.class );
            constructor.setAccessible( true );
            return constructor.newInstance( "<id>" );
        }
        catch(Exception e1)
        {
            try
            {
                var constructor = settingClass.getConstructor();
                constructor.setAccessible( true );
                return constructor.newInstance();
            }
            catch ( Exception e2 )
            {
                throw new RuntimeException( Exceptions.chain( e1, e2 ) );
            }
        }
    }

    private static Optional<Setting<?>> fieldAsSetting( Class<?> settingClass, Object instance, Field field )
    {
        Setting<?> setting;
        try
        {
            setting = (Setting<?>) field.get( instance );
        }
        catch ( Exception e )
        {
            return Optional.empty();
        }

        if( field.isAnnotationPresent( Internal.class ) )
        {
            return Optional.empty();
        }

        if( !field.isAnnotationPresent( Description.class ))
        {
            throw new RuntimeException( String.format(
                    "Public setting `%s` is missing description in %s.",
                    setting.name(), settingClass.getName() ) );
        }
        return Optional.of(setting);
    }

    private final String name;
    private final String description;
    private final List<SettingDescription> settings;

    public SettingsDescription( String name, String description, List<SettingDescription> settings )
    {
        this.name = name;
        this.description = description;
        this.settings = settings;
    }

    public Stream<SettingDescription> settings()
    {
        return settings.stream().sorted(Comparator.comparing(SettingDescription::name));
    }

    public String id()
    {
        return "config-" + name();
    }

    public String description()
    {
        return description;
    }

    public String name()
    {
        return name;
    }

    /**
     * Combine this description with another one. This is an immutable operation,
     * meaning it returns a new description that is the combination of this and the
     * one passed in.
     *
     * The name and description is taken from this description, name and setting
     * from the provided one are discarded.
     *
     * @param other another setting description
     * @return the union of this and the provided settings description
     */
    public SettingsDescription union( SettingsDescription other )
    {
        ArrayList<SettingDescription> union = new ArrayList<>( this.settings );
        union.addAll( other.settings );
        return new SettingsDescription( name, description, union );
    }
}
