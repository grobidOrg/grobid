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
package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.Test;

import org.grobid.core.analyzers.GrobidDefaultAnalyzer;

/**
 * U+00B7 MIDDLE DOT is ambiguous: a list bullet in most documents, but a letter-level
 * character inside words in Catalan (the "punt volat" separating a geminate l), in
 * chemical formulas of hydrates, and as a dot-product sign. It must only be normalised
 * to a bullet in the former case.
 */
public class UnicodeUtilMiddleDotTest {

    /** Intra-word middle dots must survive normalisation untouched. */
    @Test
    public void normaliseText_middleDotInsideWord_shouldBePreserved() {
        assertThat(UnicodeUtil.normaliseText("intel·ligència"), is("intel·ligència"));
        assertThat(UnicodeUtil.normaliseText("cel·lular"), is("cel·lular"));
        assertThat(UnicodeUtil.normaliseText("síl·laba"), is("síl·laba"));
        // uppercase, and mixed letter/digit neighbours
        assertThat(UnicodeUtil.normaliseText("IN·LIGENT"), is("IN·LIGENT"));
        assertThat(UnicodeUtil.normaliseText("CuSO4·5H2O"), is("CuSO4·5H2O"));
        assertThat(UnicodeUtil.normaliseText("a·b"), is("a·b"));
    }

    /** The precomposed form U+0140 LATIN SMALL LETTER L WITH MIDDLE DOT is not a bullet either. */
    @Test
    public void normaliseText_precomposedLWithMiddleDot_shouldBePreserved() {
        assertThat(UnicodeUtil.normaliseText("inteŀligència"), is("inteŀligència"));
    }

    /** A middle dot that is not inside a word is still a list marker and must be normalised. */
    @Test
    public void normaliseText_middleDotAsListMarker_shouldBecomeBullet() {
        assertThat(UnicodeUtil.normaliseText("· First item"), is("• First item"));
        assertThat(UnicodeUtil.normaliseText("one · two"), is("one • two"));
        assertThat(UnicodeUtil.normaliseText("item·"), is("item•"));
        assertThat(UnicodeUtil.normaliseText("·"), is("•"));
    }

    /** The unambiguous bullet code points keep being normalised to U+2022. */
    @Test
    public void normaliseText_otherBulletChars_shouldStillBecomeBullet() {
        assertThat(UnicodeUtil.normaliseText("● item"), is("• item"));
        assertThat(UnicodeUtil.normaliseText("‣ item"), is("• item"));
        assertThat(UnicodeUtil.normaliseText("• item"), is("• item"));
    }

    /**
     * The regression this guards: U+2022 is one of TextUtilities.delimiters while U+00B7 is
     * not, so normalising the punt volat to a bullet used to split the word into three
     * tokens ("intel", "•", "ligència") before it ever reached a model.
     */
    @Test
    public void tokenize_catalanGeminate_shouldStaySingleToken() {
        List<String> tokens = GrobidDefaultAnalyzer.getInstance().tokenize("intel·ligència");
        assertThat(tokens.size(), is(1));
        assertThat(tokens.get(0), is("intel·ligència"));
    }
}
