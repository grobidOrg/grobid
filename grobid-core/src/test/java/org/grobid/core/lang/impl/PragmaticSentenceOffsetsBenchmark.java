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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import org.grobid.core.utilities.OffsetPosition;

/**
 * Micro-benchmark comparing the diff-based offset recovery ({@link PragmaticSentenceDetector#getSentenceOffsets})
 * against the legacy heuristic ({@link PragmaticSentenceDetector#getSentenceOffsetsOld}).
 *
 * <p>This is not a unit test (no assertions on timing) and is {@code @Ignore}d so it does not run in CI.
 * Run it on demand with:
 * <pre>
 *   ./gradlew :grobid-core:test --tests org.grobid.core.lang.impl.PragmaticSentenceOffsetsBenchmark -Dtest.ignoreFailures=false
 * </pre>
 * or temporarily remove {@code @Ignore}.
 *
 * <p>Two regimes are measured:
 * <ul>
 *   <li><b>clean</b>: every sentence matches the original text exactly, so {@code getSentenceOffsets}
 *       resolves every sentence with a bounded {@code indexOf} and never calls the diff engine. This
 *       isolates the structural overhead of the new code path.</li>
 *   <li><b>mismatch</b>: the segmenter collapsed whitespace / dropped characters, so the bounded
 *       {@code indexOf} fails and the diff-based {@code findInText} fallback fires for several
 *       sentences. This measures the cost of {@code diff_main}.</li>
 * </ul>
 */
@Ignore("benchmark, run on demand")
public class PragmaticSentenceOffsetsBenchmark {

    private static final int WARMUP = 2000;
    private static final int ITERATIONS = 20000;

    @Test
    public void benchmark() {
        Dataset clean = buildClean();
        Dataset mismatch = buildMismatch();

        // sanity: both regimes produce the expected number of sentences with the diff-based method
        sanity("clean", clean);
        sanity("mismatch", mismatch);

        System.out.println("\n================ Pragmatic sentence offsets benchmark ================");
        System.out.printf("warmup=%d iterations=%d%n", WARMUP, ITERATIONS);
        System.out.printf(
                "clean dataset: %d chars, %d sentences (diff path NOT triggered)%n",
                clean.text.length(),
                clean.sentences.size());
        System.out.printf(
                "mismatch dataset: %d chars, %d sentences (diff path triggered)%n%n",
                mismatch.text.length(),
                mismatch.sentences.size());

        Dataset longMismatch = buildLongMismatch();
        System.out.printf(
                "longMismatch dataset: %d chars, %d sentences (worst-case wide diff window)%n%n",
                longMismatch.text.length(),
                longMismatch.sentences.size());

        System.out.printf("%-28s %14s %14s %10s%n", "scenario", "new (diff)", "old (heuristic)", "ratio");
        runRow("clean", clean);
        runRow("mismatch", mismatch);
        runRow("longMismatch", longMismatch);
        System.out.println("======================================================================\n");
    }

    /**
     * Worst-case regime: one very long sentence (~1.5k chars) whose segmenter version has many
     * scattered whitespace modifications, so {@code findInText} runs {@code diff_main} over a wide
     * (~2x sentence length) window with a non-trivial edit distance.
     */
    private Dataset buildLongMismatch() {
        StringBuilder original = new StringBuilder();
        StringBuilder mangled = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            // original keeps double spaces between clauses, the "segmented" version collapses them
            original.append("the measured value of clause number ")
                    .append(i)
                    .append(" was clearly above   the threshold, ");
            mangled.append("the measured value of clause number ")
                    .append(i)
                    .append(" was clearly above the threshold, ");
        }
        original.append("which concludes the analysis.");
        mangled.append("which concludes the analysis.");
        return new Dataset(original.toString(), Arrays.asList(mangled.toString()));
    }

    private void runRow(String label, Dataset d) {
        // warmup both
        time(() -> PragmaticSentenceDetector.getSentenceOffsets(d.text, d.sentences), WARMUP);
        time(() -> PragmaticSentenceDetector.getSentenceOffsetsOld(d.text, d.sentences), WARMUP);

        long nNew = time(() -> PragmaticSentenceDetector.getSentenceOffsets(d.text, d.sentences), ITERATIONS);
        long nOld = time(() -> PragmaticSentenceDetector.getSentenceOffsetsOld(d.text, d.sentences), ITERATIONS);

        double nsNew = nNew / (double) ITERATIONS;
        double nsOld = nOld / (double) ITERATIONS;
        System.out.printf("%-28s %11.0f ns %11.0f ns %9.2fx%n", label, nsNew, nsOld, nsNew / nsOld);
    }

    private long time(Runnable r, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            r.run();
        }
        return System.nanoTime() - start;
    }

    private void sanity(String label, Dataset d) {
        List<OffsetPosition> spans = PragmaticSentenceDetector.getSentenceOffsets(d.text, d.sentences);
        if (spans.size() != d.sentences.size()) {
            throw new IllegalStateException(label + ": expected " + d.sentences.size() + " spans, got " + spans.size());
        }
    }

    private static class Dataset {
        final String text;
        final List<String> sentences;

        Dataset(String text, List<String> sentences) {
            this.text = text;
            this.sentences = sentences;
        }
    }

    /** Clean regime: sentences are exact substrings of the text, so no diff recovery is needed. */
    private Dataset buildClean() {
        String text = "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl. Documents are aligned if they are in the same language according to FastText LangID. These alignments are refined with cross-lingual LASER embeddings. For sentence-level data, they split on newlines and align with LASER, but perform no further filtering. Human annotators evaluated the quality of document alignments for six languages selected for their different scripts and amount of retrieved documents, reporting precision of over 90%. The quality of the extracted parallel sentences is evaluated in a machine translation task on six European languages of the TED corpus. POS/DEP-5: part-of-speech labeling and dependency parsing for several languages.";
        // split into exact sentences on ". " boundaries (keeping the trailing period)
        List<String> sentences = new ArrayList<>();
        int from = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '.' && (i + 1 == text.length() || text.charAt(i + 1) == ' ')) {
                sentences.add(text.substring(from, i + 1));
                from = Math.min(i + 2, text.length());
            }
        }
        return new Dataset(text, sentences);
    }

    /**
     * Mismatch regime: the same paragraph as {@code testGetSentenceOffsets_realcase_3}, where the
     * segmenter modified several sentences (collapsed whitespace, merged citation markers), forcing
     * the diff-based {@code findInText} fallback.
     */
    private Dataset buildMismatch() {
        String text = "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl. Documents are aligned if they are in the same language according to FastText LangID (Joulin et al., 2016(Joulin et al., , 2017, and have the same URL but for a differing language code. These alignments are refined with cross-lingual LASER embeddings (Artetxe and Schwenk, 2019). For sentence-level data, they split on newlines and align with LASER, but perform no further filtering. Human annotators evaluated the quality of document alignments for six languages (de, zh, ar, ro, et, my) selected for their different scripts and amount of retrieved documents, reporting precision of over 90%. The quality of the extracted parallel sentences is evaluated in a machine translation (MT) task on six European (da, cr, sl, sk, lt, et) languages of the TED corpus (Qi et al., 2018)   (Qi et al., 2018); WMT-5: cs, de, fi, lv, ro. POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id.";

        List<String> sentences = Arrays.asList(
                "CCAligned ) is a 119language 1 parallel dataset built off 68 snapshots of Common Crawl.",
                "Documents are aligned if they are in the same language according to FastText LangID (Joulin et al., 2016(Joulin et al., , 2017, and have the same URL but for a differing language code.",
                "These alignments are refined with cross-lingual LASER embeddings (Artetxe and Schwenk, 2019).",
                "For sentence-level data, they split on newlines and align with LASER, but perform no further filtering.",
                "Human annotators evaluated the quality of document alignments for six languages (de, zh, ar, ro, et, my) selected for their different scripts and amount of retrieved documents, reporting precision of over 90%.",
                "The quality of the extracted parallel sentences is evaluated in a machine translation (MT) task on six European (da, cr, sl, sk, lt, et) languages of the TED corpus (Qi et al., 2018)   (Qi et al., 2018); WMT-5: cs, de, fi, lv, ro.",
                "POS/DEP-5: part-of-speech labeling and dependency parsing for bg, ca, da, fi, id.");
        return new Dataset(text, sentences);
    }
}
