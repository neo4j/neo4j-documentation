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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.Internal;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.helpers.TimeUtil;
import org.neo4j.kernel.configuration.Group;
import org.neo4j.kernel.configuration.Settings;

/**
 * A meta description of a settings class, used to generate documentation.
 */
public class SettingsDescription
{
    /**
     * Create a description of a given class.
     */
    public static SettingsDescription describe( Class<?> settingClass )
    {
        String classDescription = settingClass.isAnnotationPresent( Description.class )
              ? settingClass.getAnnotation( Description.class ).value()
              : "List of configuration settings";
        String settingsName = settingClass.getName().replace( "$", "-" );
        Object instance = null;

        for(Class<?> cls = settingClass; cls != null; cls = cls.getSuperclass() )
        {
            if( cls.isAnnotationPresent( Group.class ) )
            {
                instance = groupInstance( settingClass );
                break;
            }
        }

        List<SettingDescription> settings = new LinkedList<>();
        for ( Field field : settingClass.getFields() )
        {
            fieldAsSetting( settingClass, instance, field ).ifPresent( (setting) -> {
                String name = setting.name();
                Optional<String> description = Optional.of(field.getAnnotation( Description.class ).value());
                String validationMessage = setting.toString();

                String defaultValue = null;
                String mandatoryMessage = null;

                String deprecationMessage = field.isAnnotationPresent( Deprecated.class )
                                              ? "The `" + name + "` configuration setting has been deprecated."
                                              : null;
                try
                {
                    Object rawDefault = setting.apply( from -> null );
                    defaultValue = rawDefault != null ? rawDefault.toString() : null;
                    // Kludge--because for DURATION settings, the internal representation (MS) is leaked as the default value
                    Optional<? extends Function<String, ?>> parser = setting.getParser();
                    if (null != defaultValue && parser.isPresent() && Settings.DURATION.equals(parser.get())) {
                        defaultValue = Long.toString(TimeUtil.DEFAULT_TIME_UNIT.convert(Long.valueOf(defaultValue), TimeUnit.MILLISECONDS));
                    }
                    if (name.equals("dbms.threads.worker_count")) {
                        defaultValue = "The minimum between \"number of processors\" and 500";
                    }
                }
                catch ( IllegalArgumentException iae )
                {
                    if ( iae.toString().contains( "mandatory" ) )
                    {
                        mandatoryMessage = "The " + name + " configuration setting is mandatory.";
                    }
                }

                settings.add( new SettingDescriptionImpl(
                        "config_" + (name.replace( "(", "").replace( ")", "" ) ),
                        name, description,
                        deprecationMessage,
                        validationMessage,
                        defaultValue,
                        null != deprecationMessage, null != defaultValue
                ));
            });
        }

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
            return settingClass.newInstance();
        }
        catch(Exception e)
        {
            throw new RuntimeException( e );
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
