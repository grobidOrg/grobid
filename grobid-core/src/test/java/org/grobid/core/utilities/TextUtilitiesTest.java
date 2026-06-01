package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.Ignore;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.analyzers.GrobidDefaultAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.test.EngineTest;

public class TextUtilitiesTest extends EngineTest {

    @Test
    public void testHTMLEncode_complete() throws Exception {
        String test = "Dé&amps, C & Bidule, D.;";
        String result = TextUtilities.HTMLEncode(test);
        assertThat("Dé&amp;amps, C &amp; Bidule, D.;", is(result));
    }

    @Test
    public void testHTMLEncode_partial() throws Exception {
        String test = "Dé&amps, C &";
        String result = TextUtilities.HTMLEncode(test);
        assertThat("Dé&amp;amps, C &amp;", is(result));
    }

    @Test
    public void testDephynization_withoutSpaces() {
        assertThat(
                TextUtilities.dehyphenize("This is hype-\nnized.We are here."),
                is("This is hypenized.We are here."));
        assertThat(
                TextUtilities.dehyphenize("This is hype-\nnized. We are here."),
                is("This is hypenized. We are here."));
    }

    @Test
    public void testDephynization_withSpaces() {
        assertThat(
                TextUtilities.dehyphenize("This is hype- \n nized. We are here."),
                is("This is hypenized. We are here."));
        assertThat(
                TextUtilities.dehyphenize("This is hype- \nnized. We are here."),
                is("This is hypenized. We are here."));
        assertThat(
                TextUtilities.dehyphenize("This is hype - \n nized. We are here."),
                is("This is hypenized. We are here."));
    }

    @Test
    public void testDephynization_withDigits_shouldNotDephypenize() {
        //        assertThat(TextUtilities.dehyphenize("This is 1234-\n44A. Patent."), is("This is 123444A. Patent."));
        //        assertThat(TextUtilities.dehyphenize("This is 1234 - \n44A. Patent."), is("This is 123444A. Patent."));
        assertThat(TextUtilities.dehyphenize("This is 1234-44A. Patent."), is("This is 1234-44A. Patent."));
        assertThat(TextUtilities.dehyphenize("This is 1234 - 44A. Patent."), is("This is 1234 - 44A. Patent."));
    }

    @Test
    public void testDephynization_citation() {
        assertThat(
                TextUtilities.dehyphenize(
                        "Anonymous. Runtime process infection. Phrack, 11(59):ar-\n"
                                +
                                "            ticle 8 of 18, December 2002."),
                is("Anonymous. Runtime process infection. Phrack, 11(59):article 8 of 18, December 2002."));
    }

    @Test
    public void testDephynization_falseTruncation_shouldReturnSameString() {
        assertThat(
                TextUtilities.dehyphenize(
                        "sd. Linux on-the-fly kernel patching without lkm. Phrack, 11(58):article 7 of 15, December 2001."),
                is("sd. Linux on-the-fly kernel patching without lkm. Phrack, 11(58):article 7 of 15, December 2001."));

        //        assertThat(TextUtilities.dehyphenize("sd. Linux on-the-fly kernel patching without lkm. Phrack, \n" +
        //                "11(58):article 7 of 15, December 2001. \n" +
        //                "[41] K. Seifried. \n" +
        //                "Honeypotting with VMware: basics. \n" +
        //                "http://www.seifried.org/security/ids/ \n" +
        //                "20020107-honeypot-vmware-basics.ht%ml. \n" +
        //                "[42] Silvio Cesare. \n" +
        //                "Runtime Kernel Kmem Patch-\n" +
        //                "ing. \n" +
        //                "http://www.big.net.au/˜silvio/ \n" +
        //                "runtime-kernel-kmem-patching.txt."), startsWith("sd. Linux on-the-fly kernel"));
    }

    @Test
    public void testDephynization_FalseTruncation_shouldReturnSameString() {
        assertThat(
                TextUtilities.dehyphenize(
                        "Nettop also relies on VMware Workstation for its VMM. Ultimately, since VMware is a closed-source product, it is impossible to verify this claim through open review."),
                is(
                        "Nettop also relies on VMware Workstation for its VMM. Ultimately, since VMware is a closed-source product, it is impossible to verify this claim through open review."));
    }

    @Test
    public void testDephynization_NormalCase() {
        assertThat(
                TextUtilities.dehyphenize(
                        "Implementation bugs in the VMM can compromise its ability to provide secure isolation, and modify-\n ing the VMM presents the risk of introducing bugs."),
                is(
                        "Implementation bugs in the VMM can compromise its ability to provide secure isolation, and modifying the VMM presents the risk of introducing bugs."));
    }

    @Test
    public void testGetLastToken_spaceParenthesis() {
        assertThat(
                TextUtilities.getLastToken("secure isolation, and modify"),
                is("modify"));
        assertThat(
                TextUtilities.getLastToken("secure isolation, (and modify"),
                is("modify"));
        assertThat(
                TextUtilities.getLastToken("secure isolation, and) modify"),
                is("modify"));
        assertThat(
                TextUtilities.getLastToken("secure isolation, and (modify"),
                is("(modify"));
        assertThat(
                TextUtilities.getLastToken("secure isolation, .and modify"),
                is("modify"));
    }

    @Test
    public void testGetFirstToken_spaceParenthesis() {
        assertThat(
                TextUtilities.getFirstToken("Secure isolation, and modify"),
                is("Secure"));
        assertThat(
                TextUtilities.getFirstToken(" secure isolation, (and modify"),
                is("secure"));
        assertThat(
                TextUtilities.getFirstToken("\n secure isolation, and) modify"),
                is("\n"));
        assertThat(
                TextUtilities.getFirstToken(" \nsecure isolation, and (modify"),
                is("\nsecure"));
        assertThat(
                TextUtilities.getFirstToken("\nsecure isolation, and (modify"),
                is("\nsecure"));
    }

    @Ignore
    @Test
    public void testDephynizationHard_withoutSpaces() {
        assertThat(
                TextUtilities.dehyphenizeHard("This is hype-\nnized.We are here."),
                is("This is hypenized.We are here."));
        assertThat(
                TextUtilities.dehyphenizeHard("This is hype-\nnized. We are here."),
                is("This is hypenized. We are here."));
    }

    @Ignore
    @Test
    public void testDephynizationHard_withSpaces() {
        assertThat(
                TextUtilities.dehyphenizeHard("This is hype- \n nized. We are here."),
                is("This is hypenyzed. We are here."));
        assertThat(
                TextUtilities.dehyphenizeHard("This is hype- \nnized. We are here."),
                is("This is hypenyzed. We are here."));
        assertThat(
                TextUtilities.dehyphenizeHard("This is hype - \n nized. We are here."),
                is("This is hypenyzed. We are here."));
    }

    @Ignore
    @Test
    public void testDephynizationHard_withDigits_shouldNotDephypenize() {
        assertThat(TextUtilities.dehyphenizeHard("This is 1234-\n44A. Patent."), is("This is 1234-44A. Patent."));
        assertThat(TextUtilities.dehyphenizeHard("This is 1234 - \n44A. Patent."), is("This is 1234 - 44A.Patent."));
    }

    @Ignore
    @Test
    public void testDephynizationHard_citation() {
        assertThat(
                TextUtilities.dehyphenizeHard(
                        "Anonymous. Runtime process infection. Phrack, 11(59):ar-\n+ "
                                +
                                "            ticle 8 of 18, December 2002."),
                is("Anonymous. Runtime process infection. Phrack, 11(59):article 8 of 18, December 2002."));
    }

    @Test
    public void testDehyphenizationWithLayoutTokens() throws Exception {
        List<String> tokens = GrobidAnalyzer.getInstance().tokenize("This is hype-\n nized.");

        List<LayoutToken> layoutTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.equals("\n")) {
                layoutTokens.get(layoutTokens.size() - 1).setNewLineAfter(true);
            }
            layoutTokens.add(new LayoutToken(token));
        }

        List<LayoutToken> output = TextUtilities.dehyphenize(layoutTokens);
        assertNotNull(output);
        assertThat(LayoutTokensUtil.toText(output), is("This is hypenized."));
    }

    @Test
    public void testPrefix() {
        String word = "Grobid";
        assertEquals("", TextUtilities.prefix(word, 0));
        assertEquals("G", TextUtilities.prefix(word, 1));
        assertEquals("Gr", TextUtilities.prefix(word, 2));
        assertEquals("Gro", TextUtilities.prefix(word, 3));
        assertEquals("Grob", TextUtilities.prefix(word, 4));

        assertEquals("Grobid", TextUtilities.prefix(word, 6));

        assertEquals("Grobid", TextUtilities.prefix(word, 100));

        assertEquals(null, TextUtilities.prefix(null, 0));
        assertEquals(null, TextUtilities.prefix(null, 1));
    }

    @Test
    public void testSuffixes() {
        String word = "Grobid";
        assertEquals("", TextUtilities.suffix(word, 0));
        assertEquals("d", TextUtilities.suffix(word, 1));
        assertEquals("id", TextUtilities.suffix(word, 2));
        assertEquals("bid", TextUtilities.suffix(word, 3));
        assertEquals("obid", TextUtilities.suffix(word, 4));

        assertEquals("Grobid", TextUtilities.suffix(word, 6));

        assertEquals("Grobid", TextUtilities.suffix(word, 100));

        assertEquals(null, TextUtilities.suffix(null, 0));
        assertEquals(null, TextUtilities.suffix(null, 1));
    }

    @Test
    public void testWordShape() {
        testWordShape("This", "Xxxx", "Xx");
        testWordShape("Equals", "Xxxx", "Xx");
        testWordShape("O'Conor", "X'Xxxx", "X'Xx");
        testWordShape("McDonalds", "XxXxxx", "XxXx");
        testWordShape("any-where", "xx-xxx", "x-x");
        testWordShape("1.First", "d.Xxxx", "d.Xx");
        testWordShape("ThisIsCamelCase", "XxXxXxXxxx", "XxXxXxXx");
        testWordShape("This:happens", "Xx:xxx", "Xx:x");
        testWordShape("ABC", "XXX", "X");
        testWordShape("AC", "XX", "X");
        testWordShape("A", "X", "X");
        testWordShape("Ab", "Xx", "Xx");
        testWordShape("AbA", "XxX", "XxX");
        testWordShape("uü", "xx", "x");
        testWordShape("Üwe", "Xxx", "Xx");

        testWordShape(" ", " ", " ");
        testWordShape("Tes9t99", "Xxdxdd", "Xxdxd");
        testWordShape("T", "X", "X");
    }

    private void testWordShape(String orig, String expected, String expectedTrimmed) {
        assertThat(TextUtilities.wordShape(orig), is(expected));
        assertThat(TextUtilities.wordShapeTrimmed(orig), is(expectedTrimmed));
    }

    @Test
    public void testFormat4Digits() throws Exception {
        assertThat(TextUtilities.formatFourDecimals(0.0002), is("0.0002"));
        assertThat(TextUtilities.formatFourDecimals(20000), is("20000"));
        assertThat(TextUtilities.formatFourDecimals(2000.00234434), is("2000.0023"));
        assertThat(TextUtilities.formatFourDecimals(0.00234434), is("0.0023"));
    }

    @Test
    public void testFormat2Digits() throws Exception {
        assertThat(TextUtilities.formatTwoDecimals(0.0002), is("0"));
        assertThat(TextUtilities.formatTwoDecimals(20000), is("20000"));
        assertThat(TextUtilities.formatTwoDecimals(2000.00234434), is("2000"));
        assertThat(TextUtilities.formatTwoDecimals(0.01234434), is("0.01"));
    }

    @Test
    public void testDoesRequireDehypenisation_standard_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample dehypen-\nyzation text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_withSpaceAfter_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample dehypen- \nyzation text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_withSpacesAfter_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample dehypen-   \n    yzation text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_withSpaceBefore_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample dehypen -\nyzation text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 10), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_withSpacesBefore_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample dehypen    -\nyzation text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 13), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_usualWord_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample open-source text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_usualWordWithSpace_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample open- source text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_usualWordWith2Space_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample open - source text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_sequence_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample ABC123-3434 text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_sequence2_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample ABC123 - 3434 text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_sequence3_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a sample ABC123    -     3434 text");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence1_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a bad sample -\n\n");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 10), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence2_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a bad sample -");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 10), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence3_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a bad sample - \n\n    ");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 10), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence4_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a bad sample-\n\n");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence5_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken("This is a bad sample-");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence6_shouldReturnFalse() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("This is a bad sample- \n\n    ");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 9), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence7_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("-\ncore is a bad sample.");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 0), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_trickySequence8_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken("- \n\n core is a bad sample.");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 0), is(true));
    }

    @Test
    public void testDoesRequireDehypenisation_falseFriend1_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken(
                        "which was mediated through the inhibition of expression of α2-\n integrin (1,2). ");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 19), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_falseFriend2_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken(
                        "which was mediated through the inhibition of expression of α2 -\n integrin (1,2). ");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 19), is(false));
    }

    @Test
    public void testDoesRequireDehypenisation_falseFriend3_shouldReturnTrue() {
        List<LayoutToken> tokens = GrobidDefaultAnalyzer.getInstance()
                .tokenizeWithLayoutToken(
                        "which was mediated through the inhibition of expression of α 2  - \n  integrin (1,2). ");
        assertThat(TextUtilities.doesRequireDehypenisation(tokens, 19), is(false));
    }

    @Test
    public void testIsAllUpperCaseOrDigitOrDot() throws Exception {
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("this"), is(false));
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("UPPERCASE"), is(true));
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("."), is(true));
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("123456"), is(true));
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("P.C.T."), is(true));
        assertThat(TextUtilities.isAllUpperCaseOrDigitOrDot("P.C,T."), is(false));
    }

    @Test
    public void testOrcidPattern() {
        String[] falseOrcids = {"1234", "1234-5698-137X", "0000-0001-9877-137Y", "http://orcid.fr/0000-0001-9877-137X"};
        String[] trueOrcids = {"0000-0001-9877-137X", "http://orcid.org/0000-0001-9877-137X",
                "orcid.org/0000-0001-9877-137X"};
        for (String falseOrcid : falseOrcids) {
            Matcher orcidMatcher = TextUtilities.ORCIDPattern.matcher(falseOrcid);
            assertFalse(orcidMatcher.find());
        }
        for (String trueOrcid : trueOrcids) {
            Matcher orcidMatcher = TextUtilities.ORCIDPattern.matcher(trueOrcid);
            if (orcidMatcher.find()) {
                assertThat(
                        orcidMatcher.group(1)
                                + "-"
                                + orcidMatcher.group(2)
                                + "-"
                                + orcidMatcher.group(3)
                                + "-"
                                + orcidMatcher.group(4),
                        is("0000-0001-9877-137X"));
            }
        }
    }

    @Test
    public void testMatchTokenAndString() throws Exception {
        final String input = "This work is available at https://github.com/lfoppiano/ \n"
                +
                "supercon2. The repository contains the code of the \n"
                +
                "SuperCon 2 interface, the curation workflow, and the \n"
                +
                "\n"
                +
                "Table 2. Data support, the number of entities for each label in \n"
                +
                "each of the datasets used for evaluating the ML models. The \n"
                +
                "base dataset is the original dataset described in [18], and the \n"
                +
                "curation dataset is automatically collected based on the data-\n"
                +
                "base corrections by the interface and manually corrected. \n"
                +
                "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final String inputReal = "This work is available at https://github.com/lfoppiano/ supercon2. The repository contains the code of the SuperCon 2 interface, the curation workflow, and the Table 2. Data support, the number of entities for each label in each of the datasets used for evaluating the ML models. The base dataset is the original dataset described in [18], and the curation dataset is automatically collected based on the database corrections by the interface and manually corrected. ";
        List<OffsetPosition> urlTokens = Arrays.asList(new OffsetPosition(10, 23));

        List<OffsetPosition> offsetPositions = TextUtilities.matchTokenAndString(tokenisedInput, inputReal, urlTokens);

        assertThat(offsetPositions, hasSize(1));
        OffsetPosition url1 = offsetPositions.get(0);
        assertThat(url1.start, is(26));
        assertThat(url1.end, is(65));
        assertThat(inputReal.substring(url1.start, url1.end), is("https://github.com/lfoppiano/ supercon2"));

    }

    @Test
    public void testMatchTokenAndString_twoElements() throws Exception {
        final String input = "This work is available at https://github.com/lfoppiano/ \n"
                +
                "supercon2. The repository contains the code of the \n"
                +
                "SuperCon 2 interface, the curation workflow, and the \n"
                +
                "\n"
                +
                "Table 2. Data support, the number of entities for each label in \n"
                +
                "each of the datasets used for evaluating the ML models. The \n"
                +
                "base dataset is the original dataset described in [18], and the \n"
                +
                "curation dataset is automatically collected based on the data-\n"
                +
                "base corrections by the interface and manually corrected. \n"
                +
                "\n";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        final String inputReal = "This work is available at https://github.com/lfoppiano/ supercon2. The repository contains the code of the SuperCon 2 interface, the curation workflow, and the Table 2. Data support, the number of entities for each label in each of the datasets used for evaluating the ML models. The base dataset is the original dataset described in [18], and the curation dataset is automatically collected based on the database corrections by the interface and manually corrected. ";
        List<OffsetPosition> urlTokens = Arrays.asList(new OffsetPosition(0, 3), new OffsetPosition(10, 23));

        List<OffsetPosition> offsetPositions = TextUtilities.matchTokenAndString(tokenisedInput, inputReal, urlTokens);

        assertThat(offsetPositions, hasSize(2));
        OffsetPosition url0 = offsetPositions.get(0);
        assertThat(url0.start, is(0));
        assertThat(url0.end, is(9));

        assertThat(inputReal.substring(url0.start, url0.end), is("This work"));

        OffsetPosition url1 = offsetPositions.get(1);
        assertThat(url1.start, is(26));
        assertThat(url1.end, is(65));

        assertThat(inputReal.substring(url1.start, url1.end), is("https://github.com/lfoppiano/ supercon2"));

    }

    @Test
    public void testMatchTokenAndString_twoElementsWithEqualValue() throws Exception {
        final String input = "Christophe Castagne, Claudie Marec, Claudie Marec, Claudio Stalder,";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<OffsetPosition> urlTokens = Arrays.asList(
                new OffsetPosition(0, 3),
                new OffsetPosition(5, 8),
                new OffsetPosition(10, 13),
                new OffsetPosition(15, 18));

        List<OffsetPosition> offsetPositions = TextUtilities.matchTokenAndString(tokenisedInput, input, urlTokens);

        assertThat(offsetPositions, hasSize(4));

        OffsetPosition url0 = offsetPositions.get(0);
        assertThat(url0.start, is(0));
        assertThat(url0.end, is(19));

        assertThat(input.substring(url0.start, url0.end), is("Christophe Castagne"));

        OffsetPosition url1 = offsetPositions.get(1);
        assertThat(url1.start, is(21));
        assertThat(url1.end, is(34));

        assertThat(input.substring(url1.start, url1.end), is("Claudie Marec"));

        OffsetPosition url2 = offsetPositions.get(2);
        assertThat(url2.start, is(36));
        assertThat(url2.end, is(49));

        assertThat(input.substring(url2.start, url2.end), is("Claudie Marec"));

        OffsetPosition url3 = offsetPositions.get(3);
        assertThat(url3.start, is(51));
        assertThat(url3.end, is(66));

        assertThat(input.substring(url3.start, url3.end), is("Claudio Stalder"));

    }

    @Test
    public void testMatchTokenAndString_twoElementsWithEqualValue2() throws Exception {
        final String input = "We thank Felix Randow, Shigeki Higashiyama and Feng Zhang for plasmids.We thank Florian Steinberg for discussions and disclosure of unpublished results.We thank Matthew Freeman for helpful discussions.We express our deep gratitude to Moises Mallo for advice concerning CRISPR plus CRISPR reagents.We are grateful for the assistance of Ana Nóvoa and IGC's transgenics and mouse facilities.We thank IGC's cell sorting/flow cytometry, sequencing, and histopathology facilities.";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<OffsetPosition> annotationTokenPositions = Arrays.asList(
                new OffsetPosition(4, 7),
                new OffsetPosition(9, 12),
                new OffsetPosition(15, 18),
                new OffsetPosition(27, 30),
                new OffsetPosition(49, 52),
                new OffsetPosition(71, 74),
                new OffsetPosition(103, 106),
                new OffsetPosition(109, 110),
                new OffsetPosition(125, 126));

        List<OffsetPosition> offsetPositions = TextUtilities
                .matchTokenAndString(tokenisedInput, input, annotationTokenPositions);

        assertThat(offsetPositions, hasSize(9));

        OffsetPosition url7 = offsetPositions.get(7);
        assertThat(url7.start, is(349));
        assertThat(url7.end, is(352));

        assertThat(input.substring(url7.start, url7.end), is("IGC"));

        OffsetPosition url8 = offsetPositions.get(8);
        assertThat(url8.start, is(397));
        assertThat(url8.end, is(400));

        assertThat(input.substring(url8.start, url8.end), is("IGC"));

    }

    @Test
    public void testMatchTokenAndString_twoElementsWithEqualValue3() throws Exception {
        final String input = "We thank Benoit Demars for providing reaeration data and comments that signficantly improved the manuscript.This study was supported a NERC Case studentship awarded to DP, GYD and SJ, an ERC starting grant awarded to GYD, and the University of Exeter.";

        List<LayoutToken> tokenisedInput = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input);
        List<OffsetPosition> annotationTokenPositions = Arrays.asList(
                new OffsetPosition(4, 7),
                new OffsetPosition(40, 41),
                new OffsetPosition(62, 63),
                new OffsetPosition(79, 84));

        List<OffsetPosition> offsetPositions = TextUtilities
                .matchTokenAndString(tokenisedInput, input, annotationTokenPositions);

        assertThat(offsetPositions, hasSize(4));

        OffsetPosition url7 = offsetPositions.get(1);
        assertThat(input.substring(url7.start, url7.end), is("NERC"));

        OffsetPosition url8 = offsetPositions.get(2);
        assertThat(input.substring(url8.start, url8.end), is("ERC"));
    }
}
