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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;

public class LayoutTokensUtilTest {

    /**
     * We fake the new line in the layout token coordinates
     */
    @Test
    public void testDoesRequireDehyphenization_shouldReturnTrue() throws Exception {
        String input = "The study of iron-based supercondu- \n"
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 11), is(true));
    }

    @Test
    public void testDoesRequireDehyphenization2_shouldReturnTrue() throws Exception {
        String input = "The study of iron-based supercondu - \n"
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 12), is(true));
    }

    @Test
    public void testDoesRequireDehyphenization_composedWords_shouldReturnFalse() throws Exception {
        String input = "The study of iron-based supercondu - \n"
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 7), is(false));
        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 24), is(false));
    }

    @Test
    public void testDoesRequireDehyphenization2_composedWords_shouldReturnFalse() throws Exception {
        String input = "The study of iron- based supercondu - \n"
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 7), is(false));
    }

    @Test
    public void testDoesRequireDehyphenization3_composedWords_shouldReturnFalse() throws Exception {
        String input = "The study of iron - based supercondu - \n"
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 8), is(false));
    }

    @Test
    public void testDoesRequireDehyphenization_usingCoordinates_shouldReturnTrue() throws Exception {
        String input = "The study of iron-based supercondu -  "
                +
                "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        IntStream.range(0, 15).forEach(i -> layoutTokens.get(i).setY(10));
        IntStream.range(15, layoutTokens.size()).forEach(i -> layoutTokens.get(i).setY(30));

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 12), is(true));
    }

    //    @Test
    //    public void testDoesRequireDehyphenization_withoutNewLine() throws Exception {
    //        String input = "The study of iron-based supercondu -  " +
    //            "ctors superconductivity in the iron-pnictide LaFeAsO 1-x F x has been expanding and has \n";
    //
    //        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
    //
    //        IntStream.range(0, 15).forEach(i -> layoutTokens.get(i).setY(10));
    //        IntStream.range(15, layoutTokens.size()).forEach(i -> layoutTokens.get(i).setY(30));
    //
    //        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 12), is(true));
    //    }

    @Test
    public void testDoesRequireDehyphenization_hypenAtEndOfString_shouldReturnFalse() throws Exception {
        String input = "The study of iron-based supercondu-";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);

        assertThat(LayoutTokensUtil.doesRequireDehypenisation(layoutTokens, 11), is(false));
    }

    @Test
    public void testSubList() throws Exception {

        String text = "This is a simple text that I'm making up just for fun... or well for the sake of the test!";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(layoutToken -> layoutToken.setOffset(layoutToken.getOffset() + 1000));

        List<LayoutToken> result = LayoutTokensUtil.subListByOffset(layoutTokens, 1005, 1008);

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getText(), is("is"));
        assertThat(result.get(1).getText(), is(" "));

    }

    @Test
    public void testSubList_noEnd() throws Exception {

        String text = "This is a simple text that I'm making up just for fun... or well for the sake of the test!";

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(layoutToken -> layoutToken.setOffset(layoutToken.getOffset() + 1000));

        List<LayoutToken> result = LayoutTokensUtil.subListByOffset(layoutTokens, 1005);

        assertThat(result, hasSize(43));
        assertThat(result.get(0).getText(), is("is"));
        assertThat(Iterables.getLast(result).getText(), is("!"));

    }

    @Test
    public void testMaterialNameWithHypenInFormula_shouldNotDehypenise() throws Exception {

        String text = "based \n"
                +
                "(Nd 1-x Ce x ) 2 Fe 14-y Co y B nanostructured magnets"; // codespell:ignore nd

        List<LayoutToken> layoutTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        layoutTokens.stream().forEach(layoutToken -> layoutToken.setOffset(layoutToken.getOffset() + 1000));
        layoutTokens.get(4).setY(406.746);
        layoutTokens.get(4).setX(55.754000000000005);

        layoutTokens.get(6).setSubscript(true);
        layoutTokens.get(6).setY(410.506);
        layoutTokens.get(6).setX(65.2255);

        layoutTokens.get(7).setSubscript(true);
        layoutTokens.get(7).setY(410.506);
        layoutTokens.get(7).setX(67.66675);

        layoutTokens.get(8).setSubscript(true);
        layoutTokens.get(8).setY(410.538);
        layoutTokens.get(8).setX(70.108);

        layoutTokens.get(10).setY(406.76);
        layoutTokens.get(10).setX(73.3461);

        layoutTokens.get(12).setSubscript(true);

        layoutTokens.get(21).setSubscript(true);

        List<LayoutToken> result = LayoutTokensUtil.dehyphenize(layoutTokens);

        assertThat(result.get(7).getText(), is("-"));

        System.out.println(result);
    }

}
