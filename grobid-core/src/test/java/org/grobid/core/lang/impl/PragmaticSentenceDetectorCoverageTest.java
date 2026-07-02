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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;

/**
 * Ground-truth coverage probe for the real Pragmatic (Ruby) segmenter on the 8922-char figure caption
 * from biorxiv doc 037689v1 (a single-letter amino-acid sequence dump) that is dropped under
 * sentence segmentation. We feed the exact text to the real detector and report what fraction of the
 * non-whitespace characters end up covered by some sentence span. Diagnostic, not an assertion gate.
 */
public class PragmaticSentenceDetectorCoverageTest {

    @BeforeAll
    public static void setUpClass() {
        GrobidProperties.getInstance();
    }

    @Test
    public void probeCoverage_037689_figDesc() throws Exception {
        probe("/sentence/figdesc_037689.txt");
    }

    @Test
    public void probeCoverage_358077_astralParagraph() throws Exception {
        // paragraph carrying mathematical-italic (astral / surrogate-pair) characters whose content is
        // dropped under sentence segmentation; check whether the detector covers the supplementary chars
        probe("/sentence/para_astral_358077.txt");
    }

    private void probe(String resource) throws Exception {
        String text;
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            text = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        // the resource file may carry a trailing newline; keep as-is to mirror production input
        PragmaticSentenceDetector detector = new PragmaticSentenceDetector();
        List<OffsetPosition> spans = detector.detect(text);

        boolean[] covered = new boolean[text.length()];
        int last = -1;
        for (OffsetPosition p : spans) {
            for (int i = Math.max(0, p.start); i < Math.min(text.length(), p.end); i++)
                covered[i] = true;
            if (p.end > last)
                last = p.end;
        }
        int nonWs = 0, nonWsCovered = 0, astral = 0, astralCovered = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isHighSurrogate(ch)) {
                astral++;
                if (covered[i])
                    astralCovered++;
            }
            if (!Character.isWhitespace(ch)) {
                nonWs++;
                if (covered[i])
                    nonWsCovered++;
            }
        }
        System.out.println("=== COVERAGE PROBE " + resource + " ===");
        System.out.println("astral (supplementary) chars: " + astral + "  covered: " + astralCovered);
        System.out.println("text length              : " + text.length());
        System.out.println("spans returned           : " + spans.size());
        System.out.println("last span end            : " + last);
        System.out.println("non-whitespace chars     : " + nonWs);
        System.out.println("non-ws chars covered     : " + nonWsCovered);
        System.out.println("non-ws chars DROPPED      : " + (nonWs - nonWsCovered));
        if (!spans.isEmpty()) {
            OffsetPosition first = spans.get(0);
            OffsetPosition lastSpan = spans.get(spans.size() - 1);
            System.out.println(
                    "first span ["
                            + first.start
                            + ","
                            + first.end
                            + "] = "
                            + text.substring(first.start, Math.min(first.end, first.start + 60)));
            System.out.println(
                    "last  span ["
                            + lastSpan.start
                            + ","
                            + lastSpan.end
                            + "] tail = "
                            + text.substring(Math.max(lastSpan.start, lastSpan.end - 60), lastSpan.end));
        }
    }
}
