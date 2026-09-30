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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;

public class FieldSpecificationTest {

    /** The flavors that are deliberately not supported by the end-to-end evaluation. */
    private static final Set<GrobidModels.Flavor> UNSUPPORTED_FLAVORS = EnumSet.of(
            GrobidModels.Flavor.BLANK,
            GrobidModels.Flavor._3GPP,
            GrobidModels.Flavor.IETF,
            GrobidModels.Flavor.ARTICLE_FOOTNOTES_REFS,
            GrobidModels.Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN);

    /**
     * Every flavor must resolve to an explicit decision: either a field selection, or a clear
     * failure. This is the guard against a flavor silently evaluating nothing, and fails as soon
     * as a new Flavor constant is added without a corresponding decision.
     */
    @Test
    public void testForFlavor_everyFlavorResolvesToAnExplicitDecision() {
        // the default (full) model
        assertThat(EvaluationFieldSelection.forFlavor(null), is(notNullValue()));

        for (GrobidModels.Flavor flavor : GrobidModels.Flavor.values()) {
            if (UNSUPPORTED_FLAVORS.contains(flavor)) {
                try {
                    EvaluationFieldSelection.forFlavor(flavor);
                    fail("Expected flavor " + flavor + " to be rejected by the end-to-end evaluation");
                } catch (GrobidException e) {
                    assertTrue(e.getMessage().contains(flavor.getLabel()));
                }
            } else {
                EvaluationFieldSelection selection = EvaluationFieldSelection.forFlavor(flavor);
                assertThat("no selection for flavor " + flavor, selection, is(notNullValue()));
                // all declared names must resolve against the catalogue
                FieldSpecification.headerFields(selection.getHeaderFieldNames());
                FieldSpecification.fulltextFields(selection.getFulltextFieldNames());
                FieldSpecification.citationFields(selection.getCitationFieldNames());
            }
        }
    }

    /**
     * Regression lock on the default model's field sets: these are the fields that were evaluated
     * before the catalogue refactor, and the reported benchmarks depend on them.
     */
    @Test
    public void testDefaultSelection_matchesLegacyFieldSets() {
        EvaluationFieldSelection selection = EvaluationFieldSelection.forFlavor(null);

        assertThat(
                selection.getHeaderFieldNames(),
                is(
                        Arrays.asList(
                                "title",
                                "authors",
                                "first_author",
                                "affiliation_linked",
                                "abstract",
                                "keywords")));

        assertThat(
                selection.getFulltextFieldNames(),
                is(
                        Arrays.asList(
                                "section_title",
                                "reference_citation",
                                "reference_figure",
                                "reference_table",
                                "figure_title",
                                "table_title",
                                "availability_stmt",
                                "funding_stmt",
                                "conflict_stmt",
                                "contribution_stmt")));

        assertThat(
                selection.getCitationFieldNames(),
                is(
                        Arrays.asList(
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
                                "pmcid")));
    }

    /** The light flavors keep only the bibliographic core, and evaluate no affiliations. */
    @Test
    public void testLightFlavors_haveReducedHeaderAndNoAffiliation() {
        List<String> expectedHeader = Arrays.asList("title", "authors", "first_author");

        EvaluationFieldSelection light = EvaluationFieldSelection.forFlavor(GrobidModels.Flavor.ARTICLE_LIGHT);
        assertThat(light.getHeaderFieldNames(), is(expectedHeader));
        assertThat(light.getFulltextFieldNames(), is(empty()));
        assertThat(light.getCitationFieldNames(), is(empty()));

        EvaluationFieldSelection lightRef = EvaluationFieldSelection
                .forFlavor(GrobidModels.Flavor.ARTICLE_LIGHT_WITH_REFERENCES);
        assertThat(lightRef.getHeaderFieldNames(), is(expectedHeader));
        assertThat(lightRef.getFulltextFieldNames(), is(empty()));
        // light-ref evaluates the same citation fields as the default model
        assertThat(
                lightRef.getCitationFieldNames(),
                is(EvaluationFieldSelection.forFlavor(null).getCitationFieldNames()));
    }

    @Test
    public void testCitationFields_startWithTheBaseField() {
        List<FieldSpecification> fields = FieldSpecification.citationFields(Arrays.asList("title", "volume"));

        assertThat(fields, hasSize(3));
        assertThat(fields.get(0).fieldName, is(FieldSpecification.BASE_FIELD_NAME));
        assertThat(fields.get(0), is(FieldSpecification.citationBase()));
        assertThat(fields.get(1).fieldName, is("title"));
        assertThat(fields.get(2).fieldName, is("volume"));
    }

    /**
     * An empty citation selection must yield an empty list rather than a list holding only the
     * base field, since the evaluation skips the whole section on emptiness.
     */
    @Test
    public void testCitationFields_emptySelectionYieldsEmptyList() {
        assertThat(FieldSpecification.citationFields(Collections.emptyList()), is(empty()));
    }

    @Test
    public void testAffiliationLinked_isEvaluatedByACustomEvaluator() {
        List<FieldSpecification> fields = FieldSpecification.headerFields(
                Arrays.asList("affiliation_linked"));

        assertThat(fields, hasSize(1));
        FieldSpecification affiliationLinked = fields.get(0);

        assertThat(affiliationLinked.customEvaluator, is(notNullValue()));
        assertThat(affiliationLinked.reportsSupportAsDocumentCount, is(true));
        assertThat(affiliationLinked.reportNote, is(notNullValue()));
        // scored by the evaluator, so the generic XPath extraction never applies
        assertThat(affiliationLinked.grobidPath, is(empty()));
        assertThat(affiliationLinked.nlmPath, is(empty()));
    }

    /** Every other field is scored by the generic extract-and-compare path. */
    @Test
    public void testRegularFields_haveNoCustomEvaluator() {
        List<FieldSpecification> fields = new ArrayList<>();
        fields.addAll(FieldSpecification.headerFields(Arrays.asList("title", "abstract")));
        fields.addAll(FieldSpecification.fulltextFields(Arrays.asList("section_title")));
        fields.addAll(FieldSpecification.citationFields(Arrays.asList("doi")));

        for (FieldSpecification field : fields) {
            assertThat("unexpected evaluator on " + field.fieldName, field.customEvaluator, is(nullValue()));
        }
    }

    @Test(expected = GrobidException.class)
    public void testHeaderFields_unknownNameThrows() {
        FieldSpecification.headerFields(Arrays.asList("not_a_field"));
    }

    @Test(expected = GrobidException.class)
    public void testCitationFields_unknownNameThrows() {
        FieldSpecification.citationFields(Arrays.asList("not_a_field"));
    }

    /**
     * The catalogue keeps fields that no flavor currently selects (they used to be commented-out
     * code); they must stay resolvable so they can be re-enabled by naming them in a selection.
     */
    @Test
    public void testCatalogue_retainsCurrentlyUnselectedFields() {
        assertTrue(FieldSpecification.headerFieldNames().containsAll(Arrays.asList("affiliations", "date", "doi")));
        assertTrue(FieldSpecification.citationFieldNames().contains("publisher"));
        assertTrue(
                FieldSpecification.fulltextFieldNames()
                        .containsAll(
                                Arrays.asList(
                                        "references",
                                        "figure_caption",
                                        "figure_label",
                                        "table_label",
                                        "table_caption")));
    }
}
