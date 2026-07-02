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
package org.grobid.core.lang.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;

/**
 * Tests for {@link OpenNLPSentenceDetector}. OpenNLP returns native character spans, but those can carry
 * trailing whitespace; the detector now normalises them through
 * {@link org.grobid.core.utilities.SentenceUtilities#trimAndFilterSentenceOffsets}, so every emitted span
 * must be non-empty and free of surrounding whitespace.
 *
 * <p>Requires the {@code en-sent.bin} model under grobid-home, so {@link GrobidProperties} is initialised
 * before the detector is constructed.
 */
public class OpenNLPSentenceDetectorTest {

    @BeforeAll
    public static void setUpClass() {
        GrobidProperties.getInstance();
    }

    private final OpenNLPSentenceDetector detector = new OpenNLPSentenceDetector();

    @Test
    public void testDetect_singleSentence() {
        String text = "This is a single sentence.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, hasSize(1));
        assertThat(text.substring(result.get(0).start, result.get(0).end), is("This is a single sentence."));
    }

    @Test
    public void testDetect_multipleSentences() {
        String text = "First sentence. Second sentence. Third sentence.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, hasSize(3));
        assertThat(text.substring(result.get(0).start, result.get(0).end), is("First sentence."));
        assertThat(text.substring(result.get(1).start, result.get(1).end), is("Second sentence."));
        assertThat(text.substring(result.get(2).start, result.get(2).end), is("Third sentence."));
    }

    @Test
    public void testDetect_emptyText() {
        List<OffsetPosition> result = detector.detect("");
        assertThat(result, is(empty()));
    }

    @Test
    public void testDetect_noTrailingWhitespaceOrEmptySpans() {
        // multiple spaces between sentences are a classic source of trailing whitespace in OpenNLP spans;
        // after normalisation every returned span must be non-empty and carry no surrounding whitespace
        String text = "First sentence.   Second sentence.    Third one here.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, is(not(empty())));
        for (OffsetPosition span : result) {
            String s = text.substring(span.start, span.end);
            assertThat("span must be non-empty", s.isEmpty(), is(false));
            assertThat("span must have no surrounding whitespace", s, is(s.strip()));
        }
    }

    @Test
    public void testDetect_longText_spansWithinBoundsAndClean() {
        String text = "Lung cancer is the most common cause of cancer-related death worldwide. "
                + "Epigenetic changes have been implicated during the early stages of carcinogenesis. "
                + "Several DNA methylation changes have been recently identified in relation to lung cancer risk. "
                + "Mendelian randomization may be adapted to the setting of DNA methylation with the use of mQTLs. "
                + "In this study, we performed a meta-analysis of four lung cancer EWAS nested within cohorts.";
        List<OffsetPosition> result = detector.detect(text);

        assertThat(result, is(not(empty())));
        for (OffsetPosition span : result) {
            assertThat(span.start >= 0, is(true));
            assertThat(span.end <= text.length(), is(true));
            String s = text.substring(span.start, span.end);
            assertThat(s.isEmpty(), is(false));
            assertThat(s, is(s.strip()));
        }
    }
}
