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
package org.grobid.core.features;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * The feature vector must carry the token texts unchanged, otherwise the labelled result
 * cannot be synchronised back with the layout tokens. A middle dot preceded by a digit
 * ("MgSO4·12H2O") is kept by the normalisation of the full text, but the subdigit
 * retokenisation isolates it at the start of a sub-token ("·12"), which must not be
 * normalised a second time into "•12".
 */
public class FeaturesVectorCitationMiddleDotTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
        Lexicon.getInstance();
    }

    @Test
    public void addFeaturesCitation_middleDotAfterDigit_shouldKeepTokenTexts() throws Exception {
        assertFeatureTokensMatchLayoutTokens(
                "Pillay, V. et al. solubility products of MgSO4·12H2O(s) and MgSO4·7H2O(s).");
        assertFeatureTokensMatchLayoutTokens("CuSO4·5H2O");
        assertFeatureTokensMatchLayoutTokens("68.6·10");
    }

    @Test
    public void removeSpaces_shouldOnlyRemoveSpacesAndNewlines() {
        assertThat(UnicodeUtil.removeSpaces("·12"), is("·12"));
        assertThat(UnicodeUtil.removeSpaces(" a b\n"), is("ab"));
    }

    private static void assertFeatureTokensMatchLayoutTokens(String input) throws Exception {
        GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();
        List<LayoutToken> tokens = analyzer.tokenizeWithLayoutToken(input);
        tokens = analyzer.retokenizeSubdigitsFromLayoutToken(tokens);

        List<String> expected = new ArrayList<>();
        for (LayoutToken token : tokens) {
            if (token.getText().trim().length() > 0)
                expected.add(token.getText());
        }

        String features = FeaturesVectorCitation.addFeaturesCitation(
                tokens,
                null,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        List<String> actual = new ArrayList<>();
        for (String line : features.split("\n")) {
            if (line.trim().length() > 0)
                actual.add(line.split(" ")[0]);
        }

        assertThat(actual, is(expected));
    }
}
