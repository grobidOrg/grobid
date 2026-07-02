/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.utilities.matching;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class LuceneIndexMatcher<T, V> implements Closeable {
    private Analyzer analyzer = new ClassicAnalyzer(Version.LUCENE_45);
    private static final String ID_LUCENE_FIELD_NAME = "idField";
    public static final String INDEXED_LUCENE_FIELD_NAME = "indexedField";
    private final Function<T, Object> indexedFieldSelector;
    private Function<V, Object> searchedFieldSelector;
    private IndexSearcher searcher = null;
    private Map<Integer, T> cache = new HashMap<Integer, T>();
    private boolean debug = false;

    // -- settings
    private double mustMatchPercentage = 0.9;
    private int maxResults = 10;
    // -- settings

    public LuceneIndexMatcher(Function<T, Object> indexedFieldSelector, Function<V, Object> searchedFieldSelector) {
        this.indexedFieldSelector = indexedFieldSelector;
        this.searchedFieldSelector = searchedFieldSelector;
    }

    public void load(Iterable<T> entities) throws EntityMatcherException {
        close();

        RAMDirectory directory = new RAMDirectory();
        IndexWriter writer = null;
        cache.clear();
        int idCounter = 0;
        try {
            writer = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_45, analyzer));
            for (T entity : entities) {
                Document doc = new Document();

                Object indexedFieldObj = getIndexedObject(entity);
                if (indexedFieldObj == null) {
                    continue;
                }

                cache.put(idCounter, entity);
                doc.add(
                        new Field(ID_LUCENE_FIELD_NAME, String.valueOf(idCounter), Field.Store.YES,
                                Field.Index.NOT_ANALYZED));

                doc.add(
                        new Field(INDEXED_LUCENE_FIELD_NAME, indexedFieldObj.toString(), Field.Store.YES,
                                Field.Index.ANALYZED));
                writer.addDocument(doc);
                if (debug) {
                    System.out.println("Doc added: " + doc);
                }
                idCounter++;
            }
            writer.commit();
            writer.close();
        } catch (IOException e) {
            directory.close();
            throw new EntityMatcherException("Cannot build a lucene index: " + e.getMessage(), e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ignored) {
                //no op
            }
        }

        try {
            searcher = new IndexSearcher(DirectoryReader.open(directory));
        } catch (IOException e) {
            throw new EntityMatcherException("Cannot open a lucene index searcher: " + e.getMessage(), e);
        }
    }

    public List<T> match(V entity) throws EntityMatcherException {
        try {
            Query query = createLuceneQuery(getSearchedObject(entity));
            if (query == null) {
                return Collections.emptyList();
            }
            TopDocs topDocs = searcher.search(query, maxResults);
            List<T> result = new ArrayList<T>();

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Integer id = Integer.valueOf(doc.get(ID_LUCENE_FIELD_NAME));
                result.add(cache.get(id));
            }

            return result;
        } catch (IOException e) {
            try {
                searcher.getIndexReader().close();
            } catch (IOException ignored) {
            }
            //            try {
            //
            //                searcher.close();
            //            } catch (IOException ignored) {
            //            }
            throw new EntityMatcherException("Error searching lucene Index: " + e.getMessage(), e);
        }
    }

    private Object getSearchedObject(V entity) throws EntityMatcherException {
        return searchedFieldSelector.apply(entity);
    }

    private Object getIndexedObject(T entity) throws EntityMatcherException {
        return indexedFieldSelector.apply(entity);
    }

    private Query createLuceneQuery(Object indexedObj) {
        if (indexedObj == null) {
            return null;
        }
        BooleanQuery query = new BooleanQuery();
        //        final Term term = new Term(INDEXED_LUCENE_FIELD_NAME);
        List<String> luceneTokens = LuceneUtil.tokenizeString(analyzer, indexedObj.toString());

        for (String luceneToken : luceneTokens) {
            TermQuery termQuery = new TermQuery(new Term(INDEXED_LUCENE_FIELD_NAME, luceneToken));
            query.add(termQuery, BooleanClause.Occur.SHOULD);
        }
        query.setMinimumNumberShouldMatch((int) (luceneTokens.size() * mustMatchPercentage));
        if (debug) {
            System.out.println(query);
        }
        return query;
    }

    public LuceneIndexMatcher<T, V> setMustMatchPercentage(double mustMatchPercentage) {
        this.mustMatchPercentage = mustMatchPercentage;
        return this;
    }

    public LuceneIndexMatcher<T, V> setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public LuceneIndexMatcher<T, V> setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void close() {
        if (searcher != null) {
            try {
                searcher.getIndexReader().close();
            } catch (IOException e) {
                //no op
            }
        }
    }
}
