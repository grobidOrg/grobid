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
package org.grobid.trainer.evaluation.utilities;

import javax.xml.xpath.XPath;

import org.w3c.dom.Document;

import org.grobid.trainer.evaluation.Stats;

/**
 * Evaluation strategy for a field that cannot be scored by the generic path of the end-to-end
 * evaluation (extract text nodes for a set of XPaths, concatenate, string-compare).
 *
 * <p>A {@link FieldSpecification} carrying a non-null evaluator is skipped by that generic path
 * entirely: the evaluator is responsible for extracting whatever it needs from the two documents
 * and for incrementing the four {@link Stats} accumulators itself, under its own field name.</p>
 *
 * <p>Implementations must be stateless and thread-safe: a single instance is shared by the
 * catalogue across all documents, which are evaluated concurrently.</p>
 */
public interface CustomFieldEvaluator {

    /**
     * Score one document pair and accumulate into the four matching variants.
     *
     * @param gold the gold document (JATS/NLM or TEI, per {@code inputType})
     * @param tei the grobid-produced TEI document
     * @param inputType {@code "nlm"} or {@code "tei"}
     * @param xp the XPath instance to use (namespace-aware, not thread-safe, per document)
     * @return the number of units scored for this document. A value {@code > 0} means the document
     *     contributes to this field's document-count support (see
     *     {@link FieldSpecification#reportsSupportAsDocumentCount}).
     */
    int evaluate(
            Document gold,
            Document tei,
            String inputType,
            XPath xp,
            Stats strictStats,
            Stats softStats,
            Stats levenshteinStats,
            Stats ratcliffObershelpStats) throws Exception;
}
