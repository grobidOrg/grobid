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

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.matching.DiffMatchPatch;

/**
 * Implementation of sentence segmentation via the Pragmatic Segmenter
 *
 */
public class PragmaticSentenceDetector implements SentenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PragmaticSentenceDetector.class);

    private ScriptingContainer instance = null;

    public PragmaticSentenceDetector() {
        String segmenterRbFile = GrobidProperties.getGrobidHomePath()
                + File.separator
                + "sentence-segmentation"
                +
                File.separator
                + "pragmatic_segmenter"
                + File.separator
                + "segmenter.rb";
        String segmenterLoadPath = GrobidProperties.getGrobidHomePath() + File.separator + "sentence-segmentation";
        /*String unicodeLoadPath = GrobidProperties.getGrobidHomePath() + File.separator + "sentence-segmentation" +
            File.separator + "pragmatic_segmenter" + File.separator + "gem" + File.separator + "gems" +
            File.separator + "unicode-0.4.4.4-java" + File.separator + "lib";*/
        String unicodeLoadPath = GrobidProperties.getGrobidHomePath()
                + File.separator
                + "sentence-segmentation"
                +
                File.separator
                + "pragmatic_segmenter"
                + File.separator
                + "lib";
        //System.out.println(vendorLoadPath);

        List<String> loadPaths = new ArrayList();
        loadPaths.add(segmenterLoadPath);
        loadPaths.add(unicodeLoadPath);

        instance = new ScriptingContainer(LocalContextScope.CONCURRENT, LocalVariableBehavior.PERSISTENT);
        instance.setClassLoader(instance.getClass().getClassLoader());
        instance.setLoadPaths(loadPaths);
        instance.runScriptlet(PathType.ABSOLUTE, segmenterRbFile);
    }

    @Override
    public List<OffsetPosition> detect(String text) {
        return detect(text, new Language(Language.EN));
    }

    @Override
    public List<OffsetPosition> detect(String text, Language lang) {
        String script = null;
        if (lang == null || "en".equals(lang.getLang()))
            script = "ps = PragmaticSegmenter::Segmenter.new(text: text, clean: false)\nps.segment";
        else
            script = "ps = PragmaticSegmenter::Segmenter.new(text: text, language: '"
                    + lang.getLang()
                    + "', clean: false)\nps.segment";

        // This detector is a process-wide singleton (see PragmaticSentenceDetectorFactory) and the JRuby
        // ScriptingContainer below is shared across all request threads. put("text", ...) writes a *shared*
        // Ruby variable, so two concurrent segmentations would clobber each other's input and the segmenter
        // could run on another call's text. The returned sentence strings then would not match this call's
        // `text`, the reconstructed offsets would not cover it, and TEIFormatter would drop the uncovered
        // content (observed as large, run-to-run-varying body/figure-caption text loss under concurrent
        // load). Serialise the put+segment so each segmentation runs against its own text.
        List<String> retList;
        synchronized (instance) {
            instance.put("text", text);
            retList = (List<String>) instance.runScriptlet(script);
        }

        return getSentenceOffsets(text, retList);
    }

    /**
     * Recover the offsets of a sentence chunk in the original text using a diff-based reconstruction.
     * The Pragmatic Segmenter can silently modify the strings it returns (inserted characters, dropped
     * whitespace), so a plain indexOf may fail. We diff the (bounded) original text against the chunk,
     * keep the characters that belong to the original text, trim trailing inserted/deleted characters,
     * and locate the resulting "adapted" substring in the original text.
     */
    public static Pair<String, Integer> findInText(String subString, String text) {

        LinkedList<DiffMatchPatch.Diff> diffs = new DiffMatchPatch().diff_main(text, subString);

        // Walk the diff in original-text order, keeping only the characters that exist in the original
        // text (EQUAL and DELETE operations); INSERT characters belong to subString only and are skipped.
        // The resulting characters are therefore aligned one-to-one with a contiguous run of `text`.
        List<Character> chars = new ArrayList<>();
        List<DiffMatchPatch.Operation> ops = new ArrayList<>();
        for (DiffMatchPatch.Diff d : diffs) {
            if (d.operation == DiffMatchPatch.Operation.INSERT)
                continue;
            for (int i = 0; i < d.text.length(); i++) {
                chars.add(d.text.charAt(i));
                ops.add(d.operation);
            }
        }

        // Drop the text-only (DELETE) characters before the first match and after the last match, so the
        // reconstructed substring spans from the first to the last character actually shared with subString.
        int from = 0;
        while (from < ops.size() && ops.get(from) == DiffMatchPatch.Operation.DELETE)
            from++;
        int to = ops.size();
        while (to > from && ops.get(to - 1) == DiffMatchPatch.Operation.DELETE)
            to--;

        if (to <= from) {
            // nothing shared with subString: signal "not found" rather than a degenerate zero-length match
            return Pair.of("", -1);
        }

        StringBuilder sb = new StringBuilder(to - from);
        for (int i = from; i < to; i++)
            sb.append(chars.get(i));
        String adaptedSubString = sb.toString();
        int start = text.indexOf(adaptedSubString);

        return Pair.of(adaptedSubString, start);
    }

    /**
     * Build the offset positions of the sentence chunks returned by the segmenter relative to the
     * original text. When a chunk does not match the original text (the segmenter modified it), we
     * fall back progressively: search within a bounded window after the previous sentence, then with
     * newlines normalised, and finally reconstruct the offsets via {@link #findInText} (diff-based).
     * The search window is bounded to twice the sentence length to avoid matching a pathologically
     * long string (same safe-guard as the Python segmenter).
     */
    protected static List<OffsetPosition> getSentenceOffsets(String text, List<String> retList) {
        // build offset positions from the string chunks
        List<OffsetPosition> result = new ArrayList<>();
        int n = text.length();
        int cursor = 0;

        for (String sentence : retList) {
            // strip all surrounding whitespace so the span starts/ends on a non-space character and we
            // never carry a leading/trailing space into the sentence
            String chunk = StringUtils.strip(sentence);
            if (chunk.isEmpty())
                continue;

            // skip whitespace in the original text before the next sentence
            while (cursor < n && Character.isWhitespace(text.charAt(cursor)))
                cursor++;

            int start = cursor;
            int ti = cursor;
            int ci = 0;
            int m = chunk.length();

            // Forward two-pointer alignment that consumes the original text strictly in order. It is
            // tolerant to differing whitespace (the segmenter turns PDF line-breaks into spaces, collapses
            // runs, etc.) and to the rare non-whitespace character the segmenter rewrites: in every
            // non-matching case we advance over the *original* text character, so the sentence keeps
            // covering the underlying text and characters between sentences are never dropped. This
            // replaces the window/indexOf reconstruction that drifted on long paragraphs and silently lost
            // runs of short reference-marker sentences (e.g. "1 Fig. 2 Fig. 3 Fig.").
            while (ci < m && ti < n) {
                char cc = chunk.charAt(ci);
                if (Character.isWhitespace(cc)) {
                    while (ci < m && Character.isWhitespace(chunk.charAt(ci)))
                        ci++;
                    while (ti < n && Character.isWhitespace(text.charAt(ti)))
                        ti++;
                } else if (text.charAt(ti) == cc) {
                    ti++;
                    ci++;
                } else {
                    // extra whitespace in the original text, or a character the segmenter rewrote:
                    // advance over the original-text character so its content is never dropped
                    ti++;
                }
            }

            // trim any trailing whitespace from the matched span
            int end = ti;
            while (end > start && Character.isWhitespace(text.charAt(end - 1)))
                end--;
            if (end > start)
                result.add(new OffsetPosition(start, end));
            cursor = ti;
        }

        // The segmenter only returns the chunks it recognises as sentences; any trailing non-whitespace
        // text left after the last returned chunk (e.g. a paragraph-final footnote or citation marker that
        // the segmenter does not emit as a sentence of its own) would otherwise stay uncovered and then be
        // dropped by TEIFormatter, which removes content sitting outside <s> elements. Extend the final span
        // (or emit a span when nothing matched at all) to cover that trailing text up to the last
        // non-whitespace character, so the marker is never lost. Bounded to the last non-whitespace char so
        // we do not reintroduce a trailing-whitespace span.
        int tail = n;
        while (tail > cursor && Character.isWhitespace(text.charAt(tail - 1)))
            tail--;
        if (tail > cursor) {
            if (!result.isEmpty()) {
                result.get(result.size() - 1).end = tail;
            } else {
                int s = 0;
                while (s < tail && Character.isWhitespace(text.charAt(s)))
                    s++;
                if (tail > s)
                    result.add(new OffsetPosition(s, tail));
            }
        }

        return result;
    }

    /**
     * Legacy heuristic offset recovery, kept for fallback/comparison. Superseded by
     * {@link #getSentenceOffsets(String, List)} which uses a diff-based reconstruction.
     */
    @Deprecated
    protected static List<OffsetPosition> getSentenceOffsetsOld(String text, List<String> retList) {
        // build offset positions from the string chunks
        List<OffsetPosition> result = new ArrayList<>();
        int pos = 0;
        int previousEnd = 0;
        // indicate when the sentence as provided by the Pragmatic Segmented does not match the original string
        // and we had to "massage" the string to identify/approximate offsets in the original string
        boolean recovered = false;
        for (int i = 0; i < retList.size(); i++) {
            String chunk = retList.get(i);
            recovered = false;
            int start = text.indexOf(chunk, pos);
            if (start == -1) {
                LOGGER.warn("Extracted sentence does not match original text - " + chunk);

                // Unfortunately the pragmatic segmenter can modify the string when it gives back the array of sentences as string.
                // it usually concerns removed white space, which then make it hard to locate exactly the offsets.
                // we take as first fallback the previous end of sentence and move it to the next non space character
                // next heuristics is to use the next sentence matching to re-synchronize to the original text

                // note: the white space removal can be avoided by commenting out @language::ExtraWhiteSpaceRule:
                // see https://github.com/echan00/pragmatic_segmenter/commit/e5e4244bacd0bd12e65b560b648d331980fc1ce4
                // but it requires then a modified version of the tool (which is OK :)

                if (previousEnd != pos) {
                    // previous sentence was "recovered", which means we are unsure about its end offset
                    start = text.indexOf(chunk, previousEnd);
                    if (start != -1) {
                        // apparently the current sentence match a bit before the end offset of the previous sentence, which mean that
                        // the previous sentence was modified by the segmenter and is longer than "real" (see example above).
                        // we need to correct the previous sentence end offset given the start of the current sentence
                        if (result.size() > 0) {
                            int newPreviousEnd = start;
                            while (newPreviousEnd >= 1 && text.charAt(newPreviousEnd - 1) == ' ') {
                                newPreviousEnd--;
                                if (start - newPreviousEnd > 10) {
                                    // this is a break to avoid going too far
                                    newPreviousEnd = start;
                                    // but look back previous character to cover general case
                                    if (newPreviousEnd >= 1 && text.charAt(newPreviousEnd - 1) == ' ') {
                                        newPreviousEnd--;
                                    }
                                }
                            }
                            result.get(result.size() - 1).end = newPreviousEnd;
                        }
                    }
                }

                // still no start, the provided sentence has been modified by the segmenter and it is really not matching the original string
                // we approximate the start of the non-matching sentence based on the end of the previous sentence
                if (start == -1) {
                    start = previousEnd;
                    while (text.charAt(start) == ' ') {
                        start++;
                        if (start - previousEnd > 10) {
                            // this is a break to avoid going too far
                            start = previousEnd + 1;
                        }
                    }
                    recovered = true;
                }
            }

            int end = start + chunk.length();

            // in case the last sentence is modified
            if (end > text.length() && i == retList.size() - 1)
                end = text.length();

            result.add(new OffsetPosition(start, end));
            pos = start + chunk.length();
            if (recovered)
                previousEnd += 1;
            else
                previousEnd = pos;
        }

        return result;
    }
}
