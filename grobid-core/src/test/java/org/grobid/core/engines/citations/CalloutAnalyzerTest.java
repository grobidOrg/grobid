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
package org.grobid.core.engines.citations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import org.grobid.core.analyzers.GrobidDefaultAnalyzer;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.layout.LayoutToken;

public class CalloutAnalyzerTest {

    @Test
    public void testGetCalloutTypeText() throws Exception {
        String input = "(Dé&amps, C & Bidule, D., 2010)";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.PARENTHESIS_TEXT;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), is(result));
    }

    @Test
    public void testGetCalloutTypeTextFail() throws Exception {
        String input = "(1,3)";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.PARENTHESIS_TEXT;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), not(result));
    }

    @Test
    public void testGetCalloutTypeNumber() throws Exception {
        String input = "[1-5]";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.BRACKET_NUMBER;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), is(result));
    }

    @Test
    public void testGetCalloutTypeNumberFail() throws Exception {
        String input = "[Foppiano et. al, 2004]";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.BRACKET_NUMBER;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), not(result));
    }

    @Test
    public void testGetCalloutCatastrophicBacktracking() throws Exception {
        String input = "(1915-1919, 1920-1924, 1925-1929, 1930-1934, and 1935-1939)";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.PARENTHESIS_NUMBER;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), is(result));
    }

    @Test
    public void testGetCalloutCatastrophicBacktrackingFail() throws Exception {
        String input = "(1915-1919, 1920-1924, 1925-1929, 1930-1934, and 1935-1939)";
        List<LayoutToken> inputCallout = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        MarkerType result = MarkerType.BRACKET_NUMBER;
        assertThat(CalloutAnalyzer.getCalloutType(inputCallout), not(result));
    }
}
