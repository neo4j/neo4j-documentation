package org.neo4j.cypher.docgen

import org.neo4j.cypher.docgen.tooling.{DocBuilder, Document, DocumentingTest, ResultAssertions}

class FulltextIndexTest extends DocumentingTest {
  override def outputPath = "target/docs/dev/ql/"

  override def doc: Document = new DocBuilder {
    private val createMatrixMovieNode = """create (m:Movie {title: "The Matrix"}) return m.title"""
    private val createJacketMovies = """create (:Movie {title: "Full Metal Jacket"}), (:Movie {title: "The Jacket"}), (:Movie {title: "Yellow Jacket"}), (:Movie {title: "Full Moon High"}), (:Movie {title: "Metallica Through The Never", description: "The movie follows the young roadie Trip through his surreal adventure with the band."}) """
    private val createTitleAndDescriptionFulltextIndex = """call db.index.fulltext.createNodeIndex("titlesAndDescriptions", ["Movie","Book"], ["title","description"])"""
    private val awaitIndexesOnline = """call db.awaitIndexes(1000)"""
    private val queryForMatrixNode = """call db.index.fulltext.queryNodes("titlesAndDescriptions", "matrix") yield node, score return node.title, node.description, score"""
    private val createRelationshipFulltextIndexWithConfig = """call db.index.fulltext.createRelationshipIndex("taggedByRelationshipIndex", ["TAGGED_AS"], ["taggedByUser"], {analyzer: "url_or_email", eventually_consistent: "true"})"""
    doc("Indexes to support full-text search", "administration-indexes-fulltext-search")
    initQueries(
      createMatrixMovieNode,
      createJacketMovies,
      createTitleAndDescriptionFulltextIndex,
      awaitIndexesOnline)
    synopsis("This section describes how to use full-text indexes, to enable full-text search.")
    section("Introduction", "administration-indexes-fulltext-search-introduction") {
      p(
        """
          |Full-text indexes are powered by the http://lucene.apache.org/[Apache Lucene] indexing and search library, and can be used to index nodes and relationships by string properties.
          |A full-text index allows you to write queries that match within the _contents_ of indexed string properties.
          |For instance, the btree indexes described in previous sections can only do exact matching or prefix matches on strings.
          |A full-text index will instead tokenize the indexed string values, so it can match _terms_ anywhere within the strings.
          |How the indexed strings are tokenized and broken into terms, is determined by what analyzer the full-text index is configured with.
          |For instance, the _swedish_ analyzer knows how to tokenize and stem Swedish words, and will avoid indexing Swedish stop words.
          |"""
      )
      p("Full-text indexes:")
      p(
        """
          |* support the indexing of both nodes and relationships.
          |* support configuring custom analyzers, including analyzers that are not included with Lucene itself.
          |* can be queried using the Lucene query language.
          |* can return the _score_ for each result from a query.
          |* are kept up to date automatically, as nodes and relationships are added, removed, and modified.
          |* will automatically populate newly created indexes with the existing data in a store.
          |* can be checked by the consistency checker, and they can be rebuilt if there is a problem with them.
          |* are a projection of the store, and can only index nodes and relationships by the contents of their properties.
          |* can support any number of documents in a single index.
          |* are created, dropped, and updated transactionally, and is automatically replicated throughout a cluster.
          |* can be accessed via Cypher procedures.
          |* can be configured to be _eventually consistent_, in which index updating is moved from the commit path to a background thread.
          |Using this feature, it is possible to work around the slow Lucene writes from the performance critical commit process, thus removing the main bottlenecks for Neo4j write performance.
          |"""
      )
      p(
        """
          |At first sight, the construction of full-text indexes can seem similar to regular indexes.
          |However there are some things that are interesting to note:
          |In contrast to <<administration-indexes-introduction, btree indexes>>, a full-text index
          |* can be applied to more than one label.
          |* can be applied to relationship types (one or more).
          |* can be applied to more than one property at a time (similar to a <<administration-indexes-create-a-composite-index, _composite index_>>) but with an important difference:
          |While a composite index applies only to entities that match the indexed label and _all_ of the indexed properties, full-text index will index entities that have at least one of the indexed labels or relationship types, and at least one of the indexed properties.
          |"""
      )
      p("For information on how to configure full-text indexes, refer to <<operations-manual#index-configuration-fulltext-search,  Operations Manual -> Indexes to support full-text search>>.")
    }
    section("Procedures to manage full-text indexes", "administration-indexes-fulltext-search-manage") {
      p(
        """
          |Full-text indexes are managed through built-in procedures.
          |The most common procedures are listed in the table below:
          |"""
      )
      p(
        """
          |[options="header"]
          ||===
          || Usage                               | Procedure                                                 | Description
          || Create full-text node index         | `db.index.fulltext.createNodeIndex`                       | Create a node full-text index for the given labels and properties. The optional 'config' map parameter can be used to supply settings to the index. Note: index specific settings are currently experimental, and might not replicated correctly in a cluster, or during backup. Supported settings are 'analyzer', for specifying what analyzer to use when indexing and querying. Use the `db.index.fulltext.listAvailableAnalyzers` procedure to see what options are available. And 'eventually_consistent' which can be set to 'true' to make this index eventually consistent, such that updates from committing transactions are applied in a background thread.
          || Create full-text relationship index | `db.index.fulltext.createRelationshipIndex`               | Create a relationship full-text index for the given relationship types and properties. The optional 'config' map parameter can be used to supply settings to the index. Note: index specific settings are currently experimental, and might not replicated correctly in a cluster, or during backup. Supported settings are 'analyzer', for specifying what analyzer to use when indexing and querying. Use the `db.index.fulltext.listAvailableAnalyzers` procedure to see what options are available. And 'eventually_consistent' which can be set to 'true' to make this index eventually consistent, such that updates from committing transactions are applied in a background thread.
          || List available analyzers            | `db.index.fulltext.listAvailableAnalyzers`                | List the available analyzers that the full-text indexes can be configured with.
          || Use full-text node index            | `db.index.fulltext.queryNodes`                            | Query the given full-text index. Returns the matching nodes and their Lucene query score, ordered by score.
          || Use full-text relationship index    | `db.index.fulltext.queryRelationships`                    | Query the given full-text index. Returns the matching relationships and their Lucene query score, ordered by score.
          || Drop full-text index                | `db.index.fulltext.drop`                                  | Drop the specified index.
          ||===
          |"""
      )
    }

    section("Create and configure full-text indexes", "administration-indexes-fulltext-search-create-and-configure") {
      p(
        """
          |Full-text indexes are created with the `db.index.fulltext.createNodeIndex` and `db.index.fulltext.createRelationshipIndex`.
          |The indexes must each be given a unique name when created, which is used to reference the specific index in question, when querying or dropping an index.
          |A full-text index then applies to a list of labels or a list of relationship types, for node and relationship indexes respectively, and then a list of property names.
          |"""
      )
      p("For instance, if we have a movie with a title.")
      query(createMatrixMovieNode, ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p("And we have a full-text index on the `title` and `description` properties of movies and books.")
      query(createTitleAndDescriptionFulltextIndex, ResultAssertions(r => {})) {}
      p("Then our movie node from above will be included in the index, even though it only have one of the indexed labels, and only one of the indexed properties:")
      query(queryForMatrixNode, ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p(
        """
          |The same is true for full-text indexes on relationships.
          |Though a relationship can only have one type, a relationship full-text index can index multiple types, and all relationships will be included that match one of the relationship types, and at least one of the indexed properties.""")
      p(
        """
          |The `db.index.fulltext.createNodeIndex` and `db.index.fulltext.createRelationshipIndex` takes an optional fourth argument, called `config`.
          |The `config` parameter is a map from string to string, and can be used to set index-specific configuration settings.
          |The `analyzer` setting can be used to configure an index-specific analyzer.
          |The possible values for the `analyzer` setting can be listed with the `db.index.fulltext.listAvailableAnalyzers` procedure.
          |The `eventually_consistent` setting, if set to `"true"`, will put the index in an _eventually consistent_ update mode.
          |this means that updates will be applied in a background thread "as soon as possible", instead of during transaction commit like other indexes.
        """)
      note(p(
        """
          |Using index-specific settings via the `config` parameter is to be considered as experimental, because these settings are currently not replicated in a clustered environment.
          |See <<operations-manual#index-configuration-fulltext-search, Operations Manual -> Indexes to support full-text search>> for instructions on how to configure full-text indexes in <<operations-manual#file-locations, neo4j.conf>>."""))
      query(createRelationshipFulltextIndexWithConfig, ResultAssertions(r => {})) {
        p(
          """
            |In this example, an eventually consistent relationship full-text index is created for the `TAGGED_AS` relationship type, and the `taggedByUser` property, and the index uses the `url_or_email` analyzer.
            |This could, for instance, be a system where people are assigning tags to documents, and where the index on the `taggedByUser` property will allow them to quickly find all of the documents they have tagged.
            |Had it not been for the relationship index, one would have had to add artificial connective nodes between the tags and the documents in the data model, just so these nodes could be indexed.""")
        resultTable()
      }
    }

    section("Query full-text indexes", "administration-indexes-fulltext-search-query") {
      p(
        """
          |Full-text indexes will, in addition to any exact matches, also return _approximate_ matches to a given query.
          |Both the property values that are indexed, and the queries to the index, are processed through the analyzer such that the index can find that don't _exactly_ matches.
          |The `score` that is returned alongside each result entry, represents how well the index thinks that entry matches the given query.
          |The results are always returned in _descending score order_, where the best matching result entry is put first.
          |To illustrate, in the example below, we search our movie database for "Full Metal Jacket", and even though there is an exact match as the first result, we also get three other less interesting results:""")
      query("""call db.index.fulltext.queryNodes("titlesAndDescriptions", "Full Metal Jacket") yield node, score return node.title, score""", ResultAssertions(r => {
        r.size should equal(4)
      })) {
        resultTable()
      }
      p(
        """
          |Full-text indexes are powered by the http://lucene.apache.org/[Apache Lucene] indexing and search library.
          |This means that we can use Lucene's full-text query language to express what we wish to search for.
          |For instance, if we are only interested in exact matches, then we can quote the string we are searching for.""")
      query("""call db.index.fulltext.queryNodes("titlesAndDescriptions", "\\\"Full Metal Jacket\\\"") yield node, score return node.title, score""", ResultAssertions(r => {
        r.size should equal(1)
      })) {
        p("When we put \"Full Metal Jacket\" in quotes, Lucene only gives us exact matches.")
        resultTable()
      }
      p("Lucene also allows us to use logical operators, such as `AND` and `OR`, to search for terms:")
      query("""call db.index.fulltext.queryNodes("titlesAndDescriptions", 'full AND metal') yield node, score return node.title, score""", ResultAssertions(r => {})) {
        p("Only the \"Full Metal Jacket\" movie in our database has both the words \"full\" and \"metal\".")
        resultTable()
      }
      p("It is also possible to search for only specific properties, by putting the property name and a colon in front of the text being searched for.")
      query("""call db.index.fulltext.queryNodes("titlesAndDescriptions", 'description:"surreal adventure"') yield node, score return node.title, node.description, score""", ResultAssertions(r => {
        r.size should equal(1)
      })) {
        resultTable()
      }
      p("A complete description of the Lucene query syntax can be found in the http://lucene.apache.org/core/5_5_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#package.description[Lucene documentation].")
    }

    section("Drop full-text indexes", "administration-indexes-fulltext-search-drop") {
      p("A full-text node index is dropped by using the procedure  `db.index.fulltext.drop`.")
      p("In the following example, we will drop the `taggedByRelationshipIndex` that we created previously:")
      query("CALL db.index.fulltext.drop(\"taggedByRelationshipIndex\")", ResultAssertions(r => {})) {
        resultTable()
      }
    }
  }.build()

}
