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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.grobid.core.document.Document;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.utilities.TextUtilities;

/**
 *  DEPRECATED !
 *
 *  A machine learning model is used now to segment references, see org.grobid.core.engines.ReferenceSegmenterParser
 *
 */
public class RegexReferenceSegmenter implements ReferenceSegmenter {
    private static final Pattern m1 = Pattern.compile("((^|\\n)( )*\\[.+?\\])");
    private static final Pattern m2 = Pattern.compile("((^|\\n)( )*\\(.+?\\))");
    private static final Pattern m3 = Pattern.compile("((^|\\n)( )*\\d{1,3}\\.)");
    // static private Pattern m4 = Pattern.compile("(\\d{1,3})");

    private final static Pattern SPACE_DASH_PATTERN = Pattern.compile("[a-zA-Z]-\\s*[\\n\\r]+\\s*[a-zA-Z]");

    private static final Pattern[] CITATION_MARKERS = {m1, m2, m3};
    private static final AdditionalRegexTextSegmenter citationTextSegmenter = new AdditionalRegexTextSegmenter();
    public static final Function<String, LabeledReferenceResult> LABELED_REFERENCE_RESULT_FUNCTION = new Function<String, LabeledReferenceResult>() {
        @Override
        public LabeledReferenceResult apply(String input) {
            return new LabeledReferenceResult(input);
        }
    };

    @Override
    public List<LabeledReferenceResult> extract(String referenceBlock) {
        return Lists.transform(segmentReferences(referenceBlock), LABELED_REFERENCE_RESULT_FUNCTION);
    }

    @Override
    //public List<LabeledReferenceResult> extract(String referenceBlock) {
    public List<LabeledReferenceResult> extract(Document doc) {
        String referencesStr = doc.getDocumentPartText(SegmentationLabels.REFERENCES);
        return Lists.transform(segmentReferences(referencesStr), LABELED_REFERENCE_RESULT_FUNCTION);
    }

    private static class StringLengthPredicate implements Predicate<String> {
        private int len;

        private StringLengthPredicate(int len) {
            this.len = len;
        }

        @Override
        public boolean apply(String s) {
            return s != null && s.length() >= len;
        }
    }

    private static List<String> segmentReferences(String references) {
        List<String> grobidResults = new ArrayList<String>();
        int best = 0;
        Matcher bestMatcher;
        int bestIndex = -1;
        for (int i = 0; i < CITATION_MARKERS.length; i++) {
            Matcher ma = CITATION_MARKERS[i].matcher(references);
            int count = 0;
            while (ma.find()) {
                count++;
            }
            if (count > best) {
                bestIndex = i;
                best = count;
            }
        }

        List<String> diggitReferences = citationTextSegmenter.extractCitationSegments(references);

        if (bestIndex == -1) {
            return diggitReferences;
        } else {
            bestMatcher = CITATION_MARKERS[bestIndex].matcher(references);
        }
        int last = 0;
        int i = 0;
        while (bestMatcher.find()) {
            if (i == 0) {
                last = bestMatcher.end();
            } else {
                int newLast = bestMatcher.start();
                String lastRef = references.substring(last, newLast);
                if (testCitationProfile(lastRef)) {
                    grobidResults.add(lastRef);
                }
                last = bestMatcher.end();
            }
            i++;
        }
        // the last one - if at least one, has not been considered
        if (i > 0) {
            String lastRef = references.substring(last, references.length());
            if (testCitationProfile(lastRef)) {
                grobidResults.add(lastRef);
            }
        }

        diggitReferences = sanitizeCitationReferenceList(diggitReferences);
        grobidResults = sanitizeCitationReferenceList(grobidResults);

        return grobidResults.size() > diggitReferences.size() ? grobidResults : diggitReferences;
    }

    private static List<String> sanitizeCitationReferenceList(List<String> references) {
        List<String> res = new ArrayList<String>();
        for (String r : references) {
            res.add(TextUtilities.dehyphenizeHard(stripCitation(r)));
        }
        return Lists.newArrayList(Iterables.filter(res, new StringLengthPredicate(15)));
    }

    private static boolean testCitationProfile(String lastRef) {
        if (lastRef.length() < 400) {
            // we assume that a reference extracted from a full text cannot be be more than 400 characters
            StringTokenizer st = new StringTokenizer(lastRef, "\n");
            if (st.countTokens() < 9) {
                return true;
            }
        }
        return false;
    }

    private static String stripCitation(String citation) {
        // process hashes at the end of line
        citation = processSpaceDash(citation);

        citation = citation
                .replaceAll("\\r\\d* ", " ")  // remove the page number
                .replaceAll("\\n\\d\\. ", " ") // remove citation bullet number
                .replaceAll("\\n", " ")
                .replaceAll("\\\\", " ")
                .replaceAll("\"", " ")
                .replaceAll(",\\s*,", ",") // resolve double commas
                .replaceAll("\\r", " ")
                .replaceAll("\\s\\s+", " ")
                .trim()
                .replaceAll("^[\\d]+\\s", "");

        return citation;
    }

    //TODO move these functions to a separate class and add test units
    private static String processSpaceDash(String s) {
        while (true) {
            Matcher matcher = SPACE_DASH_PATTERN.matcher(s);
            if (matcher.find()) {
                s = s.substring(0, matcher.start() + 1) + "-" + s.substring(matcher.end() - 1);
            } else {
                break;
            }
        }
        return s;
    }

}
