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
package org.neo4j.doc.cypherdoc;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Parses AsciiDoc files with some special markup to produce Cypher tutorials.
 */
public class Main
{
    private static final String[] EXTENSIONS = new String[] { ".asciidoc", ".adoc" };
    private static final FileFilter fileFilter = new FileFilter(){
        public boolean accept( File file ) {
            return Arrays.stream( EXTENSIONS ).anyMatch(ext -> file.getAbsolutePath().endsWith( ext ));
        }
    };

    /**
     * Transforms the given files or directories (searched recursively for
     * .asciidoc or .adoc files). The output file name is based on the input
     * file name (and the relative path if a directory got searched). The first
     * argument is the base destination directory.
     *
     * @param args base destination directory, followed by files/directories to parse.
     */
    public static void main( String[] args ) throws Exception
    {
        if ( args.length >= 3 )
        {

            Path destinationDir = getDestinationDir( args[0] );
            String destinationUrl = args[1];

            for ( int i = 2; i < args.length; i++ )
            {
                String name = args[i];
                Path source = Paths.get( name );

                if ( Files.isDirectory( source ) )
                {
                    executeDirectory( source, destinationDir, destinationUrl, true );
                }
                else
                {
                    executeFile( source, destinationDir, destinationUrl );
                }
            }
        }
        else
        {
            //throw new IllegalArgumentException( "Destination directory, public URL and at least one source must be specified." );
        }
    }

    private static void executeDirectory( Path sourceDir, Path destinationDir, String destinationUrl, boolean isTopLevelDir )
    {
        String sourceDirName = sourceDir.getFileName().toString();
        Path nestedDestinationDir = isTopLevelDir ? destinationDir : destinationDir.resolve( sourceDirName );
        String nestedDestinationUrl = isTopLevelDir ? destinationUrl : destinationUrl + '/' + sourceDirName;
        File[] files = sourceDir.toFile().listFiles( new FileFilter()
        {
            @Override
            public boolean accept( File file )
            {
                return file.isDirectory() || fileFilter.accept( file );
            }
        } );
        for ( File fileInDir : files )
        {
            if ( fileInDir.isDirectory() )
            {
                executeDirectory( fileInDir.toPath(), nestedDestinationDir, nestedDestinationUrl, false );
            }
            else
            {
                try
                {
                    executeFile( fileInDir.toPath(), nestedDestinationDir, nestedDestinationUrl );
                }
                catch ( Throwable e )
                {
                    throw new RuntimeException( String.format( "Failed while executing file: %s in the "
                                                               + "directory %s", fileInDir.getName(),
                            destinationDir ), e );
                }
            }
        }
    }

    private static Path getDestinationDir( String name ) throws IOException
    {
        Path file = Paths.get(name);
        if ( Files.exists(file) && file.isAbsolute() )
        {
            throw new IllegalArgumentException(
                    String.format("Destination directory must either not exist or be a directory: [%s]", file)
            );
        }
        return file;
    }

    /**
     * Parse a single file.
     */
    private static void executeFile( Path sourceFile, Path destinationDir, String url ) throws Exception
    {
        try
        {
            String name = sourceFile.getFileName().toString();
            String delimiter = System.getProperty("os.name").toLowerCase().startsWith( "windows" ) ? "\r\n" : "\n";
            String input = String.join( delimiter, Files.readAllLines( sourceFile, StandardCharsets.UTF_8 ) );
            String output = CypherDoc.parse( input, sourceFile.toFile().getParentFile(), url );

            Files.createDirectories( destinationDir );
            Path target = destinationDir.resolve( name );
            Files.write( target, output.getBytes() );
        }
        catch ( TestFailureException failure )
        {
            failure.dumpSnapshots( destinationDir.toFile() );
            throw failure;
        }
    }
}
