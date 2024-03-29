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
package org.neo4j.cypher.docgen

import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

object CsvFile {

  def urify(file: File): String =
    file.toURI.toURL.toString.replace("\\", "\\\\")
}

class CsvFile(fileName: String, delimiter: Char = ',')(implicit csvFilesDir: File) {

  import org.neo4j.cypher.docgen.CsvFile._

  def withContents(lines: Seq[String]*): String = {
    val csvFile = withContentsF(false, lines: _*)
    urify(csvFile)
  }

  def withContentsF(lines: Seq[String]*): File = {
    withContentsF(false, lines: _*)
  }

  def withContentsF(quoted: Boolean, lines: Seq[String]*): File = {
    val csvFile = new File(csvFilesDir, fileName)
    val writer = new PrintWriter(csvFile, StandardCharsets.UTF_8.name())
    lines.foreach(line => {
      writer.println(line.map(s => if (quoted) '"' + s + '"' else s).mkString(delimiter.toString))
    })
    writer.flush()
    writer.close()
    csvFile
  }
}
