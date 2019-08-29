/*
 * Licensed to Neo4j under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo4j licenses this file to you under
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
package org.neo4j.doc.tools;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.neo4j.doc.test.TestData.Producer;

/**
 * This class is supporting the generation of ASCIIDOC documentation
 * from Java JUnit tests. Snippets can be supplied programmatically in the Java-section
 * and will replace their @@snippetName placeholders in the documentation description.
 *
 * @author peterneubauer
 *
 */
public class JavaTestDocsGenerator extends AsciiDocGenerator
{
    public static final Producer<JavaTestDocsGenerator> PRODUCER =
            ( graph, title, documentation ) -> (JavaTestDocsGenerator) new JavaTestDocsGenerator( title ).description( documentation );

    public JavaTestDocsGenerator(String title) {
        super(title, "docs");
    }

    public void document(String directory, String sectionName) {
        this.setSection(sectionName);
        String name = title.replace(" ", "-").toLowerCase();
        File dir = new File(new File(directory), section);
        String filename = name + ".asciidoc";
        Writer fw = getFW(dir, filename);
        description = replaceSnippets(description, dir, name);
        try {
            line(fw, "[[" + sectionName + "-" + name.replaceAll("\\(|\\)", "") + "]]");
            String firstChar = title.substring(0, 1).toUpperCase();
            line(fw, firstChar + title.substring(1));
            for (int i = 0; i < title.length(); i++) {
                fw.append("=");
            }
            fw.append("\n");
            line(fw, "");
            line(fw, description);
            line(fw, "");
            fw.flush();
            fw.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void addImageSnippet(String tagName, String imageName, String title) {
        this.addSnippet( tagName, "\nimage:" + imageName + "[" + title + "]\n" );
    }
}
