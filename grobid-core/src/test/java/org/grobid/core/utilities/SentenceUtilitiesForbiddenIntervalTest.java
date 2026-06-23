package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;
import org.grobid.core.layout.LayoutToken;

/**
 * Runnable (no PowerMock) regression test for issue #753.
 *
 * <p>The reference-marker heuristic in {@link SentenceUtilities#runSentenceDetection} used to push a
 * sentence boundary one character past the end of the previous sentence whenever the following token
 * looked like a superscript numerical reference marker, even when no reference marker was actually
 * there. On arXiv:2103.12028 this split the word "The": the first sentence ended with "...over 90%. T"
 * and the next one started with "e quality...".
 *
 * <p>The forbidden-interval safeguard only performs that push when the candidate marker offset falls
 * inside a forbidden span (a real reference). This test verifies the word "The" stays intact.
 *
 * <p>Unlike {@link SentenceUtilitiesTest} (which relies on PowerMock and is {@code @Ignore}d on
 * JDK 17+), the detector is injected via plain reflection so this test actually runs.
 */
public class SentenceUtilitiesForbiddenIntervalTest {

    @Before
    public void setUp() {
        GrobidProperties.getInstance();
    }

    /**
     * Inject a stub {@link SentenceDetectorFactory} into the {@link SentenceUtilities} singleton so the
     * post-processing (forbidden spans + reference-marker attachment) runs over controlled sentence
     * positions, independently of the configured segmenter.
     */
    private void injectSentencePositions(List<OffsetPosition> positions) throws Exception {
        SentenceDetectorFactory factory = () -> new SentenceDetector() {
            @Override
            public List<OffsetPosition> detect(String text) {
                return positions;
            }

            @Override
            public List<OffsetPosition> detect(String text, Language lang) {
                return positions;
            }
        };
        Field sdfField = SentenceUtilities.class.getDeclaredField("sdf");
        sdfField.setAccessible(true);
        sdfField.set(SentenceUtilities.getInstance(), factory);
    }

    @Test
    public void testIssue753_referenceMarkerShouldNotSplitFollowingWord() throws Exception {
        String text = "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl. Documents are aligned if they are in the same language according to FastText LangID (Joulin et al., 2016(Joulin et al., , 2017, and have the same URL but for a differing language code. These alignments are refined with cross-lingual LASER embeddings (Artetxe and Schwenk, 2019). For sentence-level data, they split on newlines and align with LASER, but perform no further filtering. Human annotators evaluated the quality of document alignments for six languages (de, zh, ar, ro, et, my) selected for their different scripts and amount of retrieved documents, reporting precision of over 90%. The quality of the extracted parallel sentences is evaluated in a machine translation (MT) task on six European (da, cr, sl, sk, lt, et) languages of the TED corpus (Qi et al., 2018), where it compares favorably to systems built on crawled sentences from WikiMatrix and ParaCrawl   (Qi et al., 2018); WMT-5: cs, de, fi, lv, ro. POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id.";

        String textLayoutToken = "CCAligned (El-Kishky et al., 2020) is a 119-\n"
                + "language 1 parallel dataset built off 68 snapshots \n"
                + "of Common Crawl. Documents are aligned if they \n"
                + "are in the same language according to FastText \n"
                + "LangID (Joulin et al., 2016, 2017), and have the \n"
                + "same URL but for a differing language code. These \n"
                + "alignments are refined with cross-lingual LASER \n"
                + "embeddings (Artetxe and Schwenk, 2019). For \n"
                + "sentence-level data, they split on newlines and \n"
                + "align with LASER, but perform no further filtering. \n"
                + "Human annotators evaluated the quality of docu-\n"
                + "ment alignments for six languages (de, zh, ar,  ro, et, my) selected for their different scripts and \n" // codespell:ignore ment
                + "amount of retrieved documents, reporting precision \n"
                + "of over 90%. The quality of the extracted paral-\n"
                + "lel sentences is evaluated in a machine translation \n"
                + "(MT) task on six European (da, cr, sl, sk, lt,  et) languages of the TED corpus(Qi et al., 2018), \n"
                + "where it compares favorably to systems built on \n"
                + "crawled sentences from WikiMatrix and ParaCrawl \n"
                + "(Qi et al., 2018); WMT-5: cs, \n"
                + "de, fi, lv, ro. POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id. \n"
                + "\n";

        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(textLayoutToken);
        tokens.get(25).setSuperscript(true);

        List<OffsetPosition> referencesSpans = Arrays.asList(
                new OffsetPosition(172, 192),
                new OffsetPosition(192, 214),
                new OffsetPosition(338, 365),
                new OffsetPosition(551, 575),
                new OffsetPosition(793, 817),
                new OffsetPosition(846, 863),
                new OffsetPosition(963, 980));

        List<OffsetPosition> sentencesPositions = Arrays.asList(
                new OffsetPosition(0, 87),
                new OffsetPosition(88, 272),
                new OffsetPosition(273, 366),
                new OffsetPosition(367, 470),
                new OffsetPosition(471, 680),
                new OffsetPosition(681, 1008),
                new OffsetPosition(1009, 1090));

        injectSentencePositions(sentencesPositions);

        List<OffsetPosition> theSentences = SentenceUtilities.getInstance()
                .runSentenceDetection(text, referencesSpans, tokens, null);

        assertThat(theSentences.size(), is(7));

        // the offsets must remain exactly as provided: no reference marker sits at the "90%. The"
        // boundary, so the boundary must not be pushed into the next word
        assertThat(theSentences.get(4).start, is(471));
        assertThat(theSentences.get(4).end, is(680));
        assertThat(theSentences.get(5).start, is(681));
        assertThat(theSentences.get(5).end, is(1008));

        // the core of issue #753: the word "The" must not be split across the boundary
        assertTrue(text.substring(theSentences.get(4).start, theSentences.get(4).end).endsWith("over 90%."));
        assertThat(text.substring(theSentences.get(5).start, theSentences.get(5).start + 11), is("The quality"));
    }
}
