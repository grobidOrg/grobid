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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;

/**
 * Which fields of the {@link FieldSpecification} catalogue the end-to-end evaluation scores, per
 * model flavor.
 *
 * <p>A flavor evaluates a different document shape from the full model, so it evaluates a
 * different set of fields: the light flavors target documents whose header is reduced to the
 * bibliographic essentials, and produce no full-text structure at all. Declaring the field names
 * here &mdash; rather than duplicating the field definitions per flavor &mdash; keeps a single
 * source of truth for the XPaths and makes the per-flavor difference readable at a glance.</p>
 *
 * <p>An empty section list disables that whole section for the flavor.</p>
 */
public class EvaluationFieldSelection {

    private final List<String> headerFieldNames;
    private final List<String> fulltextFieldNames;
    private final List<String> citationFieldNames;

    public EvaluationFieldSelection(
            List<String> headerFieldNames,
            List<String> fulltextFieldNames,
            List<String> citationFieldNames) {
        this.headerFieldNames = headerFieldNames;
        this.fulltextFieldNames = fulltextFieldNames;
        this.citationFieldNames = citationFieldNames;
    }

    public List<String> getHeaderFieldNames() {
        return headerFieldNames;
    }

    public List<String> getFulltextFieldNames() {
        return fulltextFieldNames;
    }

    public List<String> getCitationFieldNames() {
        return citationFieldNames;
    }

    private static final List<String> DEFAULT_HEADER = Arrays.asList(
            "title",
            "authors",
            "first_author",
            LinkedAffiliationEvaluator.AFFILIATION_LINKED_LABEL,
            "abstract",
            "keywords");

    private static final List<String> DEFAULT_FULLTEXT = Arrays.asList(
            "section_title",
            "reference_citation",
            "reference_figure",
            "reference_table",
            "figure_title",
            "table_title",
            "availability_stmt",
            "funding_stmt",
            "conflict_stmt",
            "contribution_stmt");

    private static final List<String> DEFAULT_CITATIONS = Arrays.asList(
            "title",
            "authors",
            "first_author",
            "date",
            "inTitle",
            "volume",
            "issue",
            "page",
            "id",
            "doi",
            "pmid",
            "pmcid");

    /**
     * The light flavors keep only the bibliographic core of the header. In particular they do not
     * evaluate affiliations: the light models target documents where author affiliations are not
     * the object of the extraction.
     */
    private static final List<String> LIGHT_HEADER = Arrays.asList("title", "authors", "first_author");

    private static final EvaluationFieldSelection DEFAULT = new EvaluationFieldSelection(
            DEFAULT_HEADER, DEFAULT_FULLTEXT, DEFAULT_CITATIONS);

    private static final Map<GrobidModels.Flavor, EvaluationFieldSelection> BY_FLAVOR = buildByFlavor();

    private static Map<GrobidModels.Flavor, EvaluationFieldSelection> buildByFlavor() {
        Map<GrobidModels.Flavor, EvaluationFieldSelection> byFlavor = new EnumMap<>(GrobidModels.Flavor.class);

        byFlavor.put(
                GrobidModels.Flavor.ARTICLE_LIGHT,
                new EvaluationFieldSelection(
                        LIGHT_HEADER, Collections.emptyList(), Collections.emptyList()));

        byFlavor.put(
                GrobidModels.Flavor.ARTICLE_LIGHT_WITH_REFERENCES,
                new EvaluationFieldSelection(
                        LIGHT_HEADER, Collections.emptyList(), DEFAULT_CITATIONS));

        // GrobidModels.Flavor.BLANK, _3GPP and IETF are intentionally absent: there is no
        // JATS/TEI gold corpus of that document shape to evaluate against, so forFlavor() fails
        // loudly rather than silently scoring nothing.

        return byFlavor;
    }

    /**
     * @param flavor the model flavor, or {@code null} for the default (full) model
     * @throws GrobidException if the flavor has no field selection defined
     */
    public static EvaluationFieldSelection forFlavor(GrobidModels.Flavor flavor) {
        if (flavor == null) {
            return DEFAULT;
        }
        EvaluationFieldSelection selection = BY_FLAVOR.get(flavor);
        if (selection == null) {
            throw new GrobidException("The flavor '"
                    + flavor.getLabel()
                    + "' is not supported by the end-to-end evaluation: no field selection is defined for it.");
        }
        return selection;
    }
}
