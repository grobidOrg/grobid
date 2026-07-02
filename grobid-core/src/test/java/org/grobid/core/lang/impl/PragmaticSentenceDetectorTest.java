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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import org.grobid.core.utilities.OffsetPosition;

public class PragmaticSentenceDetectorTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testGetSentenceOffsets() {
        String original_text = "This is the original text.   Some spaces are going to be removed.";
        List<String> sentences = Arrays.asList("This is the original text.", "Some spaces are going to be removed.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(26));
        assertThat(sentence_spans.get(1).start, is(29));
        assertThat(sentence_spans.get(1).end, is(65));
    }

    @Test
    public void testGetSentenceOffsetsMismatchFirstSentence() {
        String original_text = "This is the   original text.   Some spaces are going to be removed.";
        List<String> sentences = Arrays.asList("This is the original text.", "Some spaces are going to be removed.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(28));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("This is the   original text."));
        assertThat(sentence_spans.get(1).start, is(31));
        assertThat(sentence_spans.get(1).end, is(67));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("Some spaces are going to be removed."));
    }

    @Test
    public void testGetSentenceOffsetsMismatchSecondSentence() {
        String original_text = "This is the original text.   Some spaces are    going to be removed.";
        List<String> sentences = Arrays.asList("This is the original text.", "Some spaces are going to be removed.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(26));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("This is the original text."));
        assertThat(sentence_spans.get(1).start, is(29));
        assertThat(sentence_spans.get(1).end, is(68));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("Some spaces are    going to be removed."));
    }

    @Test
    public void testGetSentenceOffsetsMismatchSecondSentence_sameSentence() {
        String original_text = "This is the original text.   This is the    original text.";
        List<String> sentences = Arrays.asList("This is the original text.", "This is the    original text.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(26));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("This is the original text."));
        assertThat(sentence_spans.get(1).start, is(29));
        assertThat(sentence_spans.get(1).end, is(58));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("This is the    original text."));
    }

    @Test
    public void testGetSentenceOffsetsMismatchAllSentences() {
        String original_text = "This is the    original text.   Some spaces are    going to be removed.";
        List<String> sentences = Arrays.asList("This is the original text.", "Some spaces are going to be removed.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(29));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("This is the    original text."));
        assertThat(sentence_spans.get(1).start, is(32));
        assertThat(sentence_spans.get(1).end, is(71));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("Some spaces are    going to be removed."));
    }

    @Test
    public void testGetSentenceOffsetsMismatch_realCase() {
        String original_text = "Figure 5 shows the time evolution of the volumeaveraged rms density fluctuations (normalized to the mean  density) in our thermal balance runs. Most of these runs show two stages of evolution -the first being a turbulent steady state and the second reflecting thermal instability that leads to multiphase condensation. The first stage occurs after an eddy turnover time scale for most of our runs. It depends on the amplitude of forcing, and thus on the parameter f turb (the fraction of turbulent heating). The second stage of evolution has much higher density fluctuations ( δρ rms / ρ ≥ 1). In this stage, the gas separates into hot and cold phases due to thermal instability. The multiphase gas formation time scale (t mp ) is very different for different parameter choices.";
        List<String> sentences = Arrays.asList(
                "Figure 5 shows the time evolution of the volumeaveraged rms density fluctuations (normalized to the mean density) in our thermal balance runs.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        // The segmenter returned only the first sentence of the paragraph. The remaining text must not be
        // left uncovered (TEIFormatter would drop it); the single span is extended to cover the trailing
        // text up to the last non-whitespace character, so no paragraph content is lost.
        assertThat(sentence_spans, hasSize(1));
        assertThat(sentence_spans.get(0).start, is(0));
        assertThat(sentence_spans.get(0).end, is(original_text.length()));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is(original_text));
    }

    @Test
    public void testFindInText() throws Exception {
        String originalText = "This is the   original text.   Some spaces are going to be removed.";
        List<String> sentences = Arrays.asList("This is the original text.", "Some spaces are going to be removed.");
        Pair<String, Integer> inText = PragmaticSentenceDetector.findInText(sentences.get(0), originalText);

        assertThat(inText.getRight(), is(0));
        assertThat(inText.getLeft(), is("This is the   original text."));
    }

    @Test
    public void testFindInText_mismatchRealCase() throws Exception {
        String originalText = "Figure 5 shows the time evolution of the volumeaveraged rms density fluctuations (normalized to the mean  density) in our thermal balance runs. Most of these runs show two stages of evolution -the first being a turbulent steady state and the second reflecting thermal instability that leads to multiphase condensation. The first stage occurs after an eddy turnover time scale for most of our runs. It depends on the amplitude of forcing, and thus on the parameter f turb (the fraction of turbulent heating). The second stage of evolution has much higher density fluctuations ( δρ rms / ρ ≥ 1). In this stage, the gas separates into hot and cold phases due to thermal instability. The multiphase gas formation time scale (t mp ) is very different for different parameter choices.";
        String sentence = "Figure 5 shows the time evolution of the volumeaveraged rms density fluctuations (normalized to the mean density) in our thermal balance runs.";

        Pair<String, Integer> inText = PragmaticSentenceDetector.findInText(sentence, originalText);

        assertThat(inText.getRight(), is(0));
        assertThat(
                inText.getLeft(),
                is(
                        "Figure 5 shows the time evolution of the volumeaveraged rms density fluctuations (normalized to the mean  density) in our thermal balance runs."));
    }

    @Test
    public void testFindInText_errorCase() throws Exception {
        String originalText = "In two species of toads and in Salamandra, which are among the most terrestrial of lissamphibians, SF were developed more prominently (i.e. the fibres were thicker and covered greater area of the section) than in species spending more time in water, such as the fire-bellied toads (compare Fig. 4b, c, g with a). Thus, one could suspect that this may be related to greater forces acting on the limbs during terrestrial locomotion. However, SF are very well developed in femora and humeri of the aquatic Chinese salamander Andrias davidianus (Canoville et al. 2018), as well as in the Triassic temnospondyl Metoposaurus krasiejowensis, which is interpreted as almost exclusively aquatic (nonetheless, this amphibian was probably able to burrow—this requires strong muscles, which would be consistent with the presence of well developed SF; Konietzko-Meier and Sander 2013). On the other hand, we did not observe well developed SF in P. fuscus, despite partially burrowing lifestyle of this amphibian. However, in closely related P. varaldii these fibres can readily be observed in at least some specimens (Guarino et al. 2011). Also, it should be noted that the presence of SF may be dependent on a number of physiological stimuli, at least in mammals. These include influence of hormones (such as estrogen), degree of physical activity, ageing or pathologies such as osteoporosis or osteoarthritis (Aaron 2012). Explaining the reasons of these differences in amphibians requires further studies.";
        String sentence = "In two species of toads and in Salamandra, which are among the most terrestrial of lissamphibians, SF were developed more prominently (i.e. the fibres were thicker and covered greater area of the section) than in species spending more time in water, such as the fire-bellied toads (compare Fig. 4b, c, g with a).";

        Pair<String, Integer> inText = PragmaticSentenceDetector.findInText(sentence, originalText);

        assertThat(inText.getRight(), is(0));
        assertThat(
                inText.getLeft(),
                is(
                        "In two species of toads and in Salamandra, which are among the most terrestrial of lissamphibians, SF were developed more prominently (i.e. the fibres were thicker and covered greater area of the section) than in species spending more time in water, such as the fire-bellied toads (compare Fig. 4b, c, g with a)."));
    }

    @Test
    public void testGetSentenceOffsets_realcase_2() throws Exception {

        String originalText = "With the success of large-scale pre-training and multilingual modeling in Natural Language Processing (NLP), recent years have seen a proliferation of large, web-mined text datasets covering hundreds of languages. However, to date there has been no systematic analysis of the quality of these publicly available datasets, or whether the datasets actually contain content in the languages they claim to represent. In this work, we manually audit the quality of 205 languagespecific corpora released with five major public datasets (CCAligned, ParaCrawl, WikiMatrix, OSCAR, mC4), and audit the correctness of language codes in a sixth (JW300). We find that lower-resource corpora have systematic issues: at least 15 corpora are completely erroneous, and a significant fraction contains less than 50% sentences of acceptable quality. Similarly, we find 82 corpora that are mislabeled or use nonstandard/ambiguous language codes. We demonstrate that these issues are easy to detect even for non-speakers of the languages in question, and supplement the human judgements with automatic analyses. Inspired by our analysis, we recommend techniques to evaluate and improve multilingual corpora and discuss the risks that come with low-quality data releases.";

        List<String> sentences = Arrays.asList(
                "With the success of large-scale pre-training and multilingual modeling in Natural Language Processing (NLP), recent years have seen a proliferation of large, web-mined text datasets covering hundreds of languages.",
                "However, to date there has been no systematic analysis of the quality of these publicly available datasets, or whether the datasets actually contain content in the languages they claim to represent.",
                "In this work, we manually audit the quality of 205 languagespecific corpora released with five major public datasets (CCAligned, ParaCrawl, WikiMatrix, OSCAR, mC4), and audit the correctness of language codes in a sixth (JW300).",
                "We find that lower-resource corpora have systematic issues: at least 15 corpora are completely erroneous, and a significant fraction contains less than 50% sentences of acceptable quality.",
                "Similarly, we find 82 corpora that are mislabeled or use nonstandard/ambiguous language codes.",
                "We demonstrate that these issues are easy to detect even for non-speakers of the languages in question, and supplement the human judgements with automatic analyses.",
                "Inspired by our analysis, we recommend techniques to evaluate and improve multilingual corpora and discuss the risks that come with low-quality data releases.");
        List<OffsetPosition> sentenceSpans = PragmaticSentenceDetector.getSentenceOffsets(originalText, sentences);

        assertThat(sentenceSpans, hasSize(7));
        for (int i = 0; i < sentenceSpans.size(); i++) {
            assertThat(
                    originalText.substring(sentenceSpans.get(i).start, sentenceSpans.get(i).end),
                    is(sentences.get(i)));
        }

    }

    @Test
    public void testGetSentenceOffsets_realcase_3() throws Exception {

        String originalText = "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl. Documents are aligned if they are in the same language according to FastText LangID (Joulin et al., 2016(Joulin et al., , 2017, and have the same URL but for a differing language code. These alignments are refined with cross-lingual LASER embeddings (Artetxe and Schwenk, 2019). For sentence-level data, they split on newlines and align with LASER, but perform no further filtering. Human annotators evaluated the quality of document alignments for six languages (de, zh, ar, ro, et, my) selected for their different scripts and amount of retrieved documents, reporting precision of over 90%. The quality of the extracted parallel sentences is evaluated in a machine translation (MT) task on six European (da, cr, sl, sk, lt, et) languages of the TED corpus (Qi et al., 2018)   (Qi et al., 2018); WMT-5: cs, de, fi, lv, ro. POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id.";

        List<String> sentences = Arrays.asList(
                "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl.",
                "Documents are aligned if they are in the same language according to FastText LangID (Joulin et al., 2016(Joulin et al., , 2017, and have the same URL but for a differing language code.",
                "These alignments are refined with cross-lingual LASER embeddings (Artetxe and Schwenk, 2019).",
                "For sentence-level data, they split on newlines and align with LASER, but perform no further filtering.",
                "Human annotators evaluated the quality of document alignments for six languages (de, zh, ar, ro, et, my) selected for their different scripts and amount of retrieved documents, reporting precision of over 90%.",
                "The quality of the extracted parallel sentences is evaluated in a machine translation (MT) task on six European (da, cr, sl, sk, lt, et) languages of the TED corpus (Qi et al., 2018)   (Qi et al., 2018); WMT-5: cs, de, fi, lv, ro.",
                "POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id.");
        List<OffsetPosition> sentenceSpans = PragmaticSentenceDetector.getSentenceOffsets(originalText, sentences);

        assertThat(sentenceSpans, hasSize(7));
        for (int i = 0; i < sentenceSpans.size(); i++) {
            assertThat(
                    originalText.substring(sentenceSpans.get(i).start, sentenceSpans.get(i).end),
                    is(sentences.get(i)));
        }

    }

    @Test
    public void testGetSentenceOffsets_trailingSpaceChunk_notIncludedInOffset() {
        // the segmenter returns chunks with a trailing space; the computed end offset must stop at the
        // last non-space character so no trailing space is carried into the sentence element
        String original_text = "Hello world. Next one.";
        List<String> sentences = Arrays.asList("Hello world. ", "Next one.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("Hello world."));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("Next one."));
    }

    @Test
    public void testGetSentenceOffsets_whitespaceOnlyChunk_dropped() {
        // a whitespace-only chunk must not produce a span (it would surface as an empty <s> downstream),
        // and it must not desynchronise the following chunk
        String original_text = "A. B.";
        List<String> sentences = Arrays.asList("A.", "   ", "B.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("A."));
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("B."));
    }

    @Test
    public void testGetSentenceOffsets_emptyChunk_dropped() {
        // an empty chunk must not produce a zero-length span and must not corrupt the following offsets
        String original_text = "First sentence. Second sentence.";
        List<String> sentences = Arrays.asList("First sentence.", "", "Second sentence.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(2));
        for (OffsetPosition span : sentence_spans) {
            assertThat(span.end > span.start, is(true));
        }
        assertThat(
                original_text.substring(sentence_spans.get(1).start, sentence_spans.get(1).end),
                is("Second sentence."));
    }

    @Test
    public void testFindInText_noSharedContent_returnsNotFound() {
        // when the chunk shares nothing with the text window, recovery must report "not found" (-1)
        // rather than a degenerate zero-length match at position 0
        Pair<String, Integer> inText = PragmaticSentenceDetector.findInText("zzzzz", "ABCDE FGHIJ");
        assertThat(inText.getRight(), is(-1));
        assertThat(inText.getLeft(), is(""));
    }

    @Test
    public void testGetSentenceOffsets_trailingTokenAfterLastChunk_covered() {
        // the segmenter does not return the paragraph-final marker ("4", a footnote callout) as a chunk;
        // the final span must be extended to cover it, otherwise TEIFormatter drops the uncovered <ref>
        // (regression observed on biorxiv doc 322396v1, where foot_refs went 1 -> 0)
        String original_text = "This will prove unnecessary at future steps. 4";
        List<String> sentences = Arrays.asList("This will prove unnecessary at future steps.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(1));
        // trailing "4" is now inside the final span (no content left uncovered)
        assertThat(sentence_spans.get(0).end, is(original_text.length()));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("This will prove unnecessary at future steps. 4"));
    }

    @Test
    public void testGetSentenceOffsets_trailingTokenWithTrailingSpace_noTrailingSpaceInSpan() {
        // tail coverage must still stop at the last non-whitespace char so we do not reintroduce a
        // trailing-whitespace span
        String original_text = "Ends here. 4   ";
        List<String> sentences = Arrays.asList("Ends here.");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(1));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("Ends here. 4"));
    }

    @Test
    public void testGetSentenceOffsets_noUsableChunks_textNotDropped() {
        // if the segmenter returns nothing usable, the text must still be covered by a span rather than
        // silently lost; the span must be trimmed of surrounding whitespace
        String original_text = "  Orphan content. ";
        List<String> sentences = Arrays.asList("   ");
        List<OffsetPosition> sentence_spans = PragmaticSentenceDetector.getSentenceOffsets(original_text, sentences);

        assertThat(sentence_spans, hasSize(1));
        assertThat(
                original_text.substring(sentence_spans.get(0).start, sentence_spans.get(0).end),
                is("Orphan content."));
    }
}
