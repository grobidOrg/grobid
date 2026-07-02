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
package org.grobid.core.document.xml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class XmlBuilderUtilsTest {

    @Test
    public void stripNonValidXMLCharacters_keepsPlainText() {
        String in = "Hello, world. 123";
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(in), is(in));
    }

    @Test
    public void stripNonValidXMLCharacters_preservesSupplementaryCharacters() {
        // mathematical-italic letters (U+1D400 block) are valid XML supplementary characters and must be
        // preserved; the old char-wise filter stripped both halves of every surrogate pair, dropping all
        // such content from sentence-segmented text (e.g. biorxiv 358077v1 formula prose)
        String mathItalic = "𝐴 𝑢𝑛𝑖"; // math-italic "A uni"
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(mathItalic), is(mathItalic));
    }

    @Test
    public void stripNonValidXMLCharacters_dropsControlChars() {
        // control chars such as backspace (0x08) are invalid in XML 1.0 and must be removed, text kept
        String in = "abc";
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(in), is("abc"));
    }

    @Test
    public void stripNonValidXMLCharacters_dropsLoneSurrogate() {
        // an unpaired high surrogate is not a valid scalar value and must be dropped, keeping the rest
        String in = "a\uD835b";
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(in), is("ab"));
    }

    @Test
    public void stripNonValidXMLCharacters_nullAndEmpty() {
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(null), is(""));
        assertThat(XmlBuilderUtils.stripNonValidXMLCharacters(""), is(""));
    }
}
