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

import org.neo4j.cypher.docgen.tooling.DocBuilder
import org.neo4j.cypher.docgen.tooling.Document
import org.neo4j.cypher.docgen.tooling.DocumentingTest
import org.neo4j.cypher.docgen.tooling.ResultAssertions
import org.neo4j.kernel.impl.index.schema.FulltextIndexProviderFactory

class FulltextIndexTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/administration/indexes"

  override def doc: Document = new DocBuilder {
    private val createMatrixMovieNode = """CREATE (m:Movie {title: "The Matrix", reviews: ["The best movie ever", "The movie is nonsense"]}) RETURN m.title"""
    private val createJacketMovies = """CREATE (:Movie {title: "Full Metal Jacket"}), (:Movie {title: "The Jacket"}), (:Movie {title: "Yellow Jacket"}), (:Movie {title: "Full Moon High"}), (:Movie {title: "Metallica Through The Never", description: "The movie follows the young roadie Trip through his surreal adventure with the band."}) """
    private val createTitleAndDescriptionFulltextIndex = "CREATE FULLTEXT INDEX titlesAndDescriptions FOR (n:Movie|Book) ON EACH [n.title, n.description]"
    private val createReviewsFulltextIndex = "CREATE FULLTEXT INDEX reviews FOR (n:Movie) ON EACH [n.reviews]"
    private val awaitIndexesOnline = """CALL db.awaitIndexes(1000)"""
    private val queryForMatrixNode = """CALL db.index.fulltext.queryNodes("titlesAndDescriptions", "matrix") YIELD node, score
                                       #RETURN node.title, node.description, score""".stripMargin('#')
    private val createRelationshipFulltextIndexWithConfig = "CREATE FULLTEXT INDEX taggedByRelationshipIndex FOR ()-[r:TAGGED_AS]-() ON EACH [r.taggedByUser] OPTIONS {indexConfig: {`fulltext.analyzer`: 'url_or_email', `fulltext.eventually_consistent`: true}}"

    doc("Full-text search index", "administration-indexes-fulltext-search")
    initQueries(
      createMatrixMovieNode,
      createJacketMovies,
      createTitleAndDescriptionFulltextIndex,
      createReviewsFulltextIndex,
      awaitIndexesOnline)
    synopsis("This chapter describes how to use full-text indexes, to enable full-text search.")
    p("""Full-text indexes are powered by the link:https://lucene.apache.org/[Apache Lucene] indexing and search library, and can be used to index nodes and relationships by string properties.
        #A full-text index allows you to write queries that match within the _contents_ of indexed string properties.
        #For instance, the b-tree indexes described in previous sections can only do exact matching or prefix matches on strings.
        #A full-text index will instead tokenize the indexed string values, so it can match _terms_ anywhere within the strings.
        #How the indexed strings are tokenized and broken into terms, is determined by what analyzer the full-text index is configured with.
        #For instance, the _swedish_ analyzer knows how to tokenize and stem Swedish words, and will avoid indexing Swedish stop words.
        #The complete list of stop words for each analyzer is included in the result of the `db.index.fulltext.listAvailableAnalyzers` procedure.
        #""".stripMargin('#'))
    p("Full-text indexes:")
    p("""* support the indexing of both nodes and relationships.
        #* support configuring custom analyzers, including analyzers that are not included with Lucene itself.
        #* can be queried using the Lucene query language.
        #* can return the _score_ for each result from a query.
        #* are kept up to date automatically, as nodes and relationships are added, removed, and modified.
        #* will automatically populate newly created indexes with the existing data in a store.
        #* can be checked by the consistency checker, and they can be rebuilt if there is a problem with them.
        #* are a projection of the store, and can only index nodes and relationships by the contents of their properties.
        #* include only property values of types String or String Array.
        #* can support any number of documents in a single index.
        #* are created, dropped, and updated transactionally, and is automatically replicated throughout a cluster.
        #* can be accessed via Cypher procedures.
        #* can be configured to be _eventually consistent_, in which index updating is moved from the commit path to a background thread.
        #Using this feature, it is possible to work around the slow Lucene writes from the performance critical commit process, thus removing the main bottlenecks for Neo4j write performance.""".stripMargin('#'))
    p("""At first sight, the construction of full-text indexes can seem similar to regular indexes.
        #However there are some things that are interesting to note:
        #In contrast to <<administration-indexes-search-performance, b-tree indexes>>, a full-text index can be:""".stripMargin('#'))
    p("""* applied to more than one label.
        #* applied to more than one relationship type.
        #* applied to more than one property at a time (similar to a <<administration-indexes-create-a-composite-b-tree-index-for-nodes, _composite index_>>) but with an important difference:
        #While a composite index applies only to entities that match the indexed label and _all_ of the indexed properties, full-text index will index entities that have at least one of the indexed labels or relationship types, and at least one of the indexed properties.""".stripMargin('#'))
    p("For information on how to configure full-text indexes, refer to <<operations-manual#index-configuration-fulltext, Operations Manual -> Indexes to support full-text search>>.")

    section("Full-text search procedures", "administration-indexes-fulltext-search-manage") {
      p("""Full-text indexes are managed through commands and used through built-in procedures, see <<operations-manual#neo4j-procedures, Operations Manual -> Procedures>> for a complete reference.
          #
          #The commands and procedures for full-text indexes are listed in the table below:""".stripMargin('#'))
      p(
        """
          |[options="header"]
          ||===
          || Usage                               | Procedure/Command                                         | Description
          || Create full-text node index         | `CREATE FULLTEXT INDEX ...`                               | Create a node fulltext index for the given labels and properties. The optional 'options' map can be used to supply provider and settings to the index. Supported settings are 'fulltext.analyzer', for specifying what analyzer to use when indexing and querying. Use the `db.index.fulltext.listAvailableAnalyzers` procedure to see what options are available. And 'fulltext.eventually_consistent' which can be set to 'true' to make this index eventually consistent, such that updates from committing transactions are applied in a background thread.
          || Create full-text relationship index | `CREATE FULLTEXT INDEX ...`                               | Create a relationship fulltext index for the given relationship types and properties. The optional 'options' map can be used to supply provider and settings to the index. Supported settings are 'fulltext.analyzer', for specifying what analyzer to use when indexing and querying. Use the `db.index.fulltext.listAvailableAnalyzers` procedure to see what options are available. And 'fulltext.eventually_consistent' which can be set to 'true' to make this index eventually consistent, such that updates from committing transactions are applied in a background thread.
          || List available analyzers            | `db.index.fulltext.listAvailableAnalyzers`                | List the available analyzers that the full-text indexes can be configured with.
          || Use full-text node index            | `db.index.fulltext.queryNodes`                            | Query the given full-text index. Returns the matching nodes and their Lucene query score, ordered by score.
          || Use full-text relationship index    | `db.index.fulltext.queryRelationships`                    | Query the given full-text index. Returns the matching relationships and their Lucene query score, ordered by score.
          || Drop full-text index                | `DROP INDEX ...`                                          | Drop the specified index.
          || Eventually consistent indexes       | `db.index.fulltext.awaitEventuallyConsistentIndexRefresh` | Wait for the updates from recently committed transactions to be applied to any eventually-consistent full-text indexes.
          || Listing all fulltext indexes        | `SHOW FULLTEXT INDEXES`                                   | Lists all fulltext indexes, see <<administration-indexes-list-indexes, the `SHOW INDEXES` command>> for details.
          ||===
          |"""
      )
    }

    section("Create and configure full-text indexes", "administration-indexes-fulltext-search-create-and-configure") {
      p("""Full-text indexes are created with the `CREATE FULLTEXT INDEX` command.
          #An index can be given a unique name when created (or get a generated one), which is used to reference the specific index when querying or dropping it.
          #A full-text index applies to a list of labels or a list of relationship types, for node and relationship indexes respectively, and then a list of property names.""".stripMargin('#'))
      p("include::../indexes-for-full-text-search/create-fulltext-syntax.asciidoc[]")
      p("For instance, if we have a movie with a title.")
      query(createMatrixMovieNode, ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p("And we have a full-text index on the `title` and `description` properties of movies and books.")
      query(createTitleAndDescriptionFulltextIndex, ResultAssertions(r => {})) {}
      p("Then our movie node from above will be included in the index, even though it only has one of the indexed labels, and only one of the indexed properties:")
      query(queryForMatrixNode, ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p("""The same is true for full-text indexes on relationships.
          #Though a relationship can only have one type, a relationship full-text index can index multiple types, and all relationships will be included that match one of the relationship types, and at least one of the indexed properties.""".stripMargin('#'))
      p(s"""The `CREATE FULLTEXT INDEX` command take an optional clause, called `options`. This have two parts, the `indexProvider` and `indexConfig`.
          #The provider can only have the default value, `'${FulltextIndexProviderFactory.DESCRIPTOR.name()}'`.
          #The `indexConfig` is a map from string to string and booleans, and can be used to set index-specific configuration settings.
          #The `fulltext.analyzer` setting can be used to configure an index-specific analyzer.
          #The possible values for the `fulltext.analyzer` setting can be listed with the `db.index.fulltext.listAvailableAnalyzers` procedure.
          #The `fulltext.eventually_consistent` setting, if set to `true`, will put the index in an _eventually consistent_ update mode.
          #This means that updates will be applied in a background thread "as soon as possible", instead of during transaction commit like other indexes.""".stripMargin('#'))
      query(createRelationshipFulltextIndexWithConfig, ResultAssertions(r => {})) {
        p(
          """In this example, an eventually consistent relationship full-text index is created for the `TAGGED_AS` relationship type, and the `taggedByUser` property, and the index uses the `url_or_email` analyzer.
            #This could, for instance, be a system where people are assigning tags to documents, and where the index on the `taggedByUser` property will allow them to quickly find all of the documents they have tagged.
            #Had it not been for the relationship index, one would have had to add artificial connective nodes between the tags and the documents in the data model, just so these nodes could be indexed.""".stripMargin('#'))
        resultTable()
      }
    }

    section("Query full-text indexes", "administration-indexes-fulltext-search-query") {
      p(
        """Full-text indexes will, in addition to any exact matches, also return _approximate_ matches to a given query.
          #Both the property values that are indexed, and the queries to the index, are processed through the analyzer such that the index can find that don't _exactly_ matches.
          #The `score` that is returned alongside each result entry, represents how well the index thinks that entry matches the given query.
          #The results are always returned in _descending score order_, where the best matching result entry is put first.
          #To illustrate, in the example below, we search our movie database for `"Full Metal Jacket"`, and even though there is an exact match as the first result, we also get three other less interesting results:""".stripMargin('#'))
      query("""CALL db.index.fulltext.queryNodes("titlesAndDescriptions", "Full Metal Jacket") YIELD node, score
              #RETURN node.title, score""".stripMargin('#'), ResultAssertions(r => {
        r.size should equal(4)
      })) {
        resultTable()
      }
      p(
        """
          |Full-text indexes are powered by the link:https://lucene.apache.org/[Apache Lucene] indexing and search library.
          |This means that we can use Lucene's full-text query language to express what we wish to search for.
          |For instance, if we are only interested in exact matches, then we can quote the string we are searching for.""")
      query("""CALL db.index.fulltext.queryNodes("titlesAndDescriptions", '"Full Metal Jacket"') YIELD node, score
              #RETURN node.title, score""".stripMargin('#'), ResultAssertions(r => {
        r.size should equal(1)
      })) {
        p("""When we put "Full Metal Jacket" in quotes, Lucene only gives us exact matches.""")
        resultTable()
      }
      p("Lucene also allows us to use logical operators, such as `AND` and `OR`, to search for terms:")
      query("""CALL db.index.fulltext.queryNodes("titlesAndDescriptions", 'full AND metal') YIELD node, score
              #RETURN node.title, score""".stripMargin('#'), ResultAssertions(r => {})) {
        p("""Only the `Full Metal Jacket` movie in our database has both the words `full` and `metal`.""")
        resultTable()
      }
      p("It is also possible to search for only specific properties, by putting the property name and a colon in front of the text being searched for.")
      query("""CALL db.index.fulltext.queryNodes("titlesAndDescriptions", 'description:"surreal adventure"') YIELD node, score
              #RETURN node.title, node.description, score""".stripMargin('#'), ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p("A complete description of the Lucene query syntax can be found in the link:https://lucene.apache.org/core/8_2_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description[Lucene documentation].")
    }

    section( "Handling of Text Array properties", id="administration-indexes-fulltext-search-query") {
      p(
        """If the indexed property contains a text array, each element of this array is analyzed independently and all produced terms are associated with the same property name.
           #This means that when querying such an indexed node or relationship, there is a match if any of the array elements match the query.
           #For example, both of the following queries match the same node while referring different elements""".stripMargin('#'))
      query("""CALL db.index.fulltext.queryNodes("reviews", 'best') YIELD node, score
              #RETURN node.title, node.reviews, score;""".stripMargin('#'), ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      query("""CALL db.index.fulltext.queryNodes("reviews", 'nonsense') YIELD node, score
              #RETURN node.title, node.reviews, score;""".stripMargin('#'), ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
    }

    section("Drop full-text indexes", "administration-indexes-fulltext-search-drop") {
      initQueries(createRelationshipFulltextIndexWithConfig)
      p("A full-text node index is dropped by using the <<administration-indexes-drop-an-index, same command as for other indexes>>, `DROP INDEX`.")
      p("In the following example, we will drop the `taggedByRelationshipIndex` that we created previously:")
      query("DROP INDEX taggedByRelationshipIndex", ResultAssertions(r => {})) {
        resultTable()
      }
    }
  }.build()

}
