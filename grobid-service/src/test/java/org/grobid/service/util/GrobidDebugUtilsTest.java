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
package org.grobid.service.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.GrobidModels;
import org.grobid.core.engines.config.DebugLabelingCollector;
import org.grobid.core.utilities.GrobidProperties;

public class GrobidDebugUtilsTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        // GrobidModels' static initialiser resolves model paths through GrobidProperties
        GrobidProperties.getInstance();
    }

    @Test
    public void testFormatResponseBody_noFilter_shouldEmitEverything() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(GrobidModels.SEGMENTATION, "the\tsegmentation\toutput");
        collector.record(GrobidModels.REFERENCE_SEGMENTER, "the\treference\toutput");

        String body = GrobidDebugUtils.formatResponseBody(collector, null);

        assertThat(body, containsString("=== model: segmentation ==="));
        assertThat(body, containsString("=== model: reference-segmenter ==="));
    }

    @Test
    public void testFormatResponseBody_filterOnBaseName_shouldSelectFlavoredModel() {
        // A flavored run records under the hyphenated flavor name, e.g.
        // "segmentation-article-footnotes-refs"; asking for "segmentation" must still match.
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(
                GrobidModels.modelFor("segmentation/article/footnotes-refs"),
                "the\tsegmentation\toutput");
        collector.record(
                GrobidModels.modelFor("reference-segmenter/article/footnotes-refs"),
                "the\treference\toutput");
        collector.record(GrobidModels.CITATION, "the\tcitation\toutput");

        String body = GrobidDebugUtils.formatResponseBody(
                collector,
                Set.of("segmentation", "reference-segmenter"));

        assertThat(body, containsString("=== model: segmentation-article-footnotes-refs ==="));
        assertThat(body, containsString("=== model: reference-segmenter-article-footnotes-refs ==="));
        assertThat(body, not(containsString("=== model: citation ===")));
    }

    @Test
    public void testFormatResponseBody_filterOnFlavoredName_shouldSelectOnlyThatFlavor() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(GrobidModels.SEGMENTATION, "the\tbase\toutput");
        collector.record(
                GrobidModels.modelFor("segmentation/article/light"),
                "the\tflavored\toutput");

        String body = GrobidDebugUtils.formatResponseBody(collector, Set.of("segmentation-article-light"));

        assertThat(body, containsString("=== model: segmentation-article-light ==="));
        assertThat(body, not(containsString("=== model: segmentation ===")));
    }

    @Test
    public void testFormatResponseBody_filterMatchingNothing_shouldBeEmpty() {
        DebugLabelingCollector collector = new DebugLabelingCollector();
        collector.record(GrobidModels.SEGMENTATION, "the\tsegmentation\toutput");

        assertThat(GrobidDebugUtils.formatResponseBody(collector, Set.of("citation")), is(""));
    }

    @Test
    public void testParseModelsFilter_blank_shouldReturnNull() {
        assertThat(GrobidDebugUtils.parseModelsFilter("  "), is((Set<String>) null));
    }

    @Test
    public void testParseModelsFilter_shouldNormaliseAndTrim() {
        assertThat(
                GrobidDebugUtils.parseModelsFilter(" Segmentation , reference-segmenter "),
                is(Set.of("segmentation", "reference-segmenter")));
    }
}
