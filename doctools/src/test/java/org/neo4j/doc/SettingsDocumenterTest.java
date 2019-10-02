/*
 * Copyright (c) 2002-2019 "Neo4j,"
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

import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.GroupSetting;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.SettingImpl;
import org.neo4j.configuration.SettingValueParsers;
import org.neo4j.graphdb.config.Setting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SettingsDocumenterTest
{
    @Test
    @Ignore
    public void shouldDocumentBasicSettingsClass() throws Throwable
    {
        // when
        String result = new SettingsDocumenter().document( SimpleSettings.class );

        // then
        // Note, I got the text below from invoking the existing un-tested
        // config documenter implementation, and running this on it:
        //
        // for ( String line : result.split( "\\n" ) )
        // {
        //    System.out.println("\"" + line
        //            .replace( "\\", "\\\\" )
        //            .replace( "\"", "\\\"" ) +"%n\" +");
        // }
        //
        // My intent here is to add tests and refactor the code, it could be
        // that there are errors in the original implementation that I've missed,
        // in which case you should trust your best judgement, and change the assertion
        // below accordingly.
        assertThat( result, equalTo( String.format(
            "// tag::config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings[]%n" +
            "[[config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings]]%n" +
            ".List of configuration settings%n" +
            "ifndef::nonhtmloutput[]%n" +
            "[options=\"header\"]%n" +
            "|===%n" +
            "|Name|Description%n" +
            "|<<config_public.default,public.default>>|Public with default.%n" +
            "|<<config_public.nodefault,public.nodefault>>|Public nodefault.%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifdef::nonhtmloutput[]%n" +
            "* <<config_public.default,public.default>>: Public with default.%n" +
            "* <<config_public.nodefault,public.nodefault>>: Public nodefault.%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "%n" +
            "// end::config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings[]%n%n" +
            "// tag::config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings-deprecated[]%n" +
            "[[config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings-deprecated]]%n" +
            ".Deprecated settings%n" +
            "ifndef::nonhtmloutput[]%n" +
            "[options=\"header\"]%n" +
            "|===%n" +
            "|Name|Description%n" +
            "|<<config_public.deprecated,public.deprecated>>|Public deprecated.%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifdef::nonhtmloutput[]%n" +
            "* <<config_public.deprecated,public.deprecated>>: Public deprecated.%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "%n" +
            "// end::config-org.neo4j.doc.SettingsDocumenterTest-SimpleSettings-deprecated[]%n%n" +
            "ifndef::nonhtmloutput[]%n" +
            "[[config_public.default]]%n" +
            ".public.default%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public with default.%n" +
            "|Valid values a|public.default is an integer%n" +
            "|Default value m|1%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifdef::nonhtmloutput[]%n" +
            "[[config_public.default]]%n" +
            ".public.default%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public with default.%n" +
            "|Valid values a|public.default is an integer%n" +
            "|Default value m|1%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifndef::nonhtmloutput[]%n" +
            "[[config_public.deprecated]]%n" +
            ".public.deprecated%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public deprecated.%n" +
            "|Valid values a|public.deprecated is a boolean%n" +
            "|Default value m|false%n" +
            "|Deprecated a|The `public.deprecated` configuration setting has been deprecated.%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifdef::nonhtmloutput[]%n" +
            "[[config_public.deprecated]]%n" +
            ".public.deprecated%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public deprecated.%n" +
            "|Valid values a|public.deprecated is a boolean%n" +
            "|Default value m|false%n" +
            "|Deprecated a|The `public.deprecated` configuration setting has been deprecated.%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifndef::nonhtmloutput[]%n" +
            "[[config_public.nodefault]]%n" +
            ".public.nodefault%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public nodefault.%n" +
            "|Valid values a|public.nodefault is a string%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n" +
            "%n" +
            "ifdef::nonhtmloutput[]%n" +
            "[[config_public.nodefault]]%n" +
            ".public.nodefault%n" +
            "[cols=\"<1h,<4\", options=\"noheader\"]%n" +
            "|===%n" +
            "|Description a|Public nodefault.%n" +
            "|Valid values a|public.nodefault is a string%n" +
            "|===%n" +
            "endif::nonhtmloutput[]%n%n"
        ) ));
    }

    @Test
    public void shouldDocumentGroupConfiguration() throws Throwable
    {
        // when
        String result = new SettingsDocumenter().document( Giraffe.class );

        // then
        String linuxString = "// tag::config-org.neo4j.doc.SettingsDocumenterTest-Giraffe[]\n" + "[[config-org.neo4j.doc.SettingsDocumenterTest-Giraffe]]\n" +
                ".Use this group to configure giraffes\n" + "ifndef::nonhtmloutput[]\n" + "[options=\"header\"]\n" + "|===\n" + "|Name|Description\n" +
                "|<<config_animal.giraffe.<id>.spot_count,animal.giraffe.<id>.spot_count>>|Number of spots this giraffe has, in number.\n" +
                "|<<config_animal.giraffe.<id>.type,animal.giraffe.<id>.type>>|Animal type.\n" + "|===\n" + "endif::nonhtmloutput[]\n" + "\n" +
                "ifdef::nonhtmloutput[]\n" +
                "* <<config_animal.giraffe.<id>.spot_count,animal.giraffe.<id>.spot_count>>: Number of spots this giraffe has, in number.\n" +
                "* <<config_animal.giraffe.<id>.type,animal.giraffe.<id>.type>>: Animal type.\n" + "endif::nonhtmloutput[]\n" + "\n" + "\n" +
                "// end::config-org.neo4j.doc.SettingsDocumenterTest-Giraffe[]\n" + "\n" + "ifndef::nonhtmloutput[]\n" +
                "[[config_animal.giraffe.<id>.spot_count]]\n" + ".animal.giraffe.<id>.spot_count\n" + "[cols=\"<1h,<4\", options=\"noheader\"]\n" + "|===\n" +
                "|Description a|Number of spots this giraffe has, in number.\n" + "|Valid values a|animal.giraffe.<id>.spot_count, an integer\n" +
                "|Default value m|12\n" + "|===\n" + "endif::nonhtmloutput[]\n" + "\n" + "ifdef::nonhtmloutput[]\n" +
                "[[config_animal.giraffe.<id>.spot_count]]\n" + ".animal.giraffe.<id>.spot_count\n" + "[cols=\"<1h,<4\", options=\"noheader\"]\n" + "|===\n" +
                "|Description a|Number of spots this giraffe has, in number.\n" + "|Valid values a|animal.giraffe.<id>.spot_count, an integer\n" +
                "|Default value m|12\n" + "|===\n" + "endif::nonhtmloutput[]\n" + "\n" + "ifndef::nonhtmloutput[]\n" + "[[config_animal.giraffe.<id>.type]]\n" +
                ".animal.giraffe.<id>.type\n" + "[cols=\"<1h,<4\", options=\"noheader\"]\n" + "|===\n" + "|Description a|Animal type.\n" +
                "|Valid values a|animal.giraffe.<id>.type, a string\n" + "|Default value m|mammal\n" + "|===\n" + "endif::nonhtmloutput[]\n" + "\n" +
                "ifdef::nonhtmloutput[]\n" + "[[config_animal.giraffe.<id>.type]]\n" + ".animal.giraffe.<id>.type\n" +
                "[cols=\"<1h,<4\", options=\"noheader\"]\n" + "|===\n" + "|Description a|Animal type.\n" +
                "|Valid values a|animal.giraffe.<id>.type, a string\n" + "|Default value m|mammal\n" + "|===\n" + "endif::nonhtmloutput[]\n\n";
        if(System.getProperty("os.name").toLowerCase().startsWith( "windows" )){
            assertThat( result, equalTo( linuxString.replaceAll( "\n","\r\n" ) ));
        } else {
        assertThat( result, equalTo( linuxString ));
        }
    }

    private static class SimpleSettings
    {
        @Description("Public nodefault")
        static Setting<String> public_nodefault = SettingImpl.newBuilder( "public.nodefault", SettingValueParsers.STRING, null ).build();

        @Description("Public with default")
        static Setting<Integer> public_with_default = SettingImpl.newBuilder("public.default", SettingValueParsers.INT, 1 ).build();

        @Deprecated
        @Description("Public deprecated")
        static Setting<Boolean> public_deprecated = SettingImpl.newBuilder("public.deprecated", SettingValueParsers.BOOL, false ).build();

        @Internal
        @Description("Internal with default")
        static Setting<String> internal_with_default = SettingImpl.newBuilder("unsupported.internal.default", SettingValueParsers.STRING, "something").build();
    }

    public abstract static class Animal extends GroupSetting
    {
        @Description( "Animal type" )
        public final Setting<String> type;

        Animal( String key, String typeDefault )
        {
            super( key );
            type = getBuilder( "type", SettingValueParsers.STRING, typeDefault ).build();
        }

        @Override
        public String getPrefix()
        {
            return "animal";
        }
    }

    @Description( "Use this group to configure giraffes" )
    public static class Giraffe extends Animal
    {
        @Description( "Number of spots this giraffe has, in number." )
        public final Setting<Integer> number_of_spots = getBuilder( "spot_count", SettingValueParsers.INT, 12 ).build();

        public Giraffe()
        {
            this( "(key)" );
        }

        public Giraffe( String key )
        {
            super( key, /* type=*/"mammal" );
        }

        @Override
        public String getPrefix()
        {
            return super.getPrefix() + ".giraffe";
        }
    }
}
