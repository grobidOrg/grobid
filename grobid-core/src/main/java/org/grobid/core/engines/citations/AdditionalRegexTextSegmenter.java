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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  DEPRECATED !
 *
 *  A machine learning model is used now to segment references, see org.grobid.core.engines.ReferenceSegmenterParser
 *
 */
public class AdditionalRegexTextSegmenter {
    public static final Logger LOGGER = LoggerFactory.getLogger(AdditionalRegexTextSegmenter.class.getName());
    private final static Pattern BRACKET_NUMBER_LOOKUP_PATTERN = Pattern
            .compile("(?s).{0,15}\\[\\d\\] .{10,701}\\n\\[\\d+\\] .*");
    private final static Pattern BULLET_NUMBER_LOOKUP_PATTERN = Pattern
            .compile("(?s).{0,10}1\\. .{10,701}\\n[\\s0]*2\\. .*");

    private final static Pattern BRACKET_SPLIT_PATTERN = Pattern.compile("\\[(\\d+)\\] ");
    private final static Pattern BULLET_SPLIT_PATTERN = Pattern.compile("\\n(\\d+)\\. ");
    private final static Pattern GENERIC_SPLIT_PATTERN = Pattern.compile("\\.[\\s]*\\n");

    private final static Pattern BROKEN_RACKETS_PATTERN = Pattern.compile("(\\[\\d+\\]\\s*\\n){5,}");

    private final static int MAX_CITATION_COUNT = 512;
    private final static int MAXIMUM_SEGMENT_LENGTH = 700;
    private static final int MINIMUM_SEGMENT_LENGTH = 15;

    // Letters which are (not only directly) followed by uncommon letters.
    private static List<Character> sparseLetters = Arrays.asList('O', 'P', 'T', 'U', 'V', 'W', 'X', 'Y');

    // Locate the References part of the fulltext by looking for keywords,
    // if found, the style of citations are detected and the citations are split accordingly.
    // if no References block is found, an attempt will be made to detect certain citation styles from the fulltext.
    public List<String> extractCitationSegments(String referencesText) {

        if (referencesText == null || referencesText.isEmpty()) {
            return Collections.emptyList();
        }

        Matcher brokenBracketMatcher = BROKEN_RACKETS_PATTERN.matcher(referencesText);
        if (brokenBracketMatcher.find()) {
            return cleanCitations(splitGenerically(referencesText));
        }

        List<String> parts;
        try {
            if (BRACKET_NUMBER_LOOKUP_PATTERN.matcher(referencesText).find()) {
                parts = splitAlongBracketedNumbers(referencesText);
            } else if (BULLET_NUMBER_LOOKUP_PATTERN.matcher(referencesText).find()) {
                parts = splitAlongBulletNumbers(referencesText);
            } else {
                parts = splitGenerically(referencesText);
            }
        } catch (StackOverflowError e) {
            //TODO: FIX regexps properly
            LOGGER.error("Stackoverflow");
            throw new RuntimeException("Runtime exception with stackoverflow in AdditionalRegexTextSegmenter");
        }
        return cleanCitations(parts);
    }

    private List<String> cleanCitations(List<String> parts) {
        if (parts.size() > MAX_CITATION_COUNT) {
            parts = parts.subList(0, MAX_CITATION_COUNT);
        }
        if (parts.size() <= 1) {
            // failed to do splitting
            return Collections.emptyList();
        }

        List<String> citations = new ArrayList<String>(parts.size());
        for (String part : parts) {
            // this checks to avoid including any false positive long segment
            if (part.length() >= MAXIMUM_SEGMENT_LENGTH) {
                continue;
            }

            if (part.trim().length() != 0) {
                citations.add(part);
            }
        }
        return citations;
    }

    // splits along bracketed number citations: [1] [2].
    // checks for consecutive numbering.
    private List<String> splitAlongBracketedNumbers(String referencesText) {

        List<String> parts = new ArrayList<String>();
        Matcher matcher = BRACKET_SPLIT_PATTERN.matcher(referencesText);
        Integer currentNumber;
        Integer currentSegmentEndIndex;

        if (!matcher.find()) {
            return Collections.emptyList();
        }
        Integer currentSegmentStartIndex = matcher.end();
        Integer previousNumber = Integer.valueOf(matcher.group(1));

        while (matcher.find()) {
            currentNumber = Integer.valueOf(matcher.group(1));
            if (currentNumber == previousNumber + 1) {
                currentSegmentEndIndex = matcher.start() - 1;
                if (currentSegmentEndIndex - currentSegmentStartIndex < 0) {
                    continue;
                }
                parts.add(referencesText.substring(currentSegmentStartIndex, currentSegmentEndIndex));
                currentSegmentStartIndex = matcher.end();
                previousNumber++;
            }
        }
        parts.add(referencesText.substring(currentSegmentStartIndex));
        return parts;
    }

    // splits along numbered citations: 1. 2. 3.
    // checks for consecutive numbering
    private List<String> splitAlongBulletNumbers(String referencesText) {
        List<String> parts = new ArrayList<String>();

        Matcher matcher = BULLET_SPLIT_PATTERN.matcher(referencesText);
        Integer currentNumber;
        Integer currentSegmentEndIndex;
        //init
        if (!matcher.find()) {
            return Collections.emptyList();
        }
        Integer currentSegmentStartIndex = matcher.end();
        Integer previousNumber = Integer.valueOf(matcher.group(1));
        // a workaround to add the first citation, where there might be no linebreak at the beginning of the referencesText.
        if (previousNumber == 2) {
            parts.add(referencesText.substring(2, matcher.start()));
        }
        while (matcher.find()) {
            currentNumber = Integer.valueOf(matcher.group(1));
            if (currentNumber == previousNumber + 1) {
                currentSegmentEndIndex = matcher.start() - 1;
                if (currentSegmentEndIndex - currentSegmentStartIndex < 0) {
                    continue;
                }
                parts.add(referencesText.substring(currentSegmentStartIndex, currentSegmentEndIndex));
                currentSegmentStartIndex = matcher.end();
                previousNumber++;
            }
        }
        parts.add(referencesText.substring(currentSegmentStartIndex));
        return parts;
    }

    // splits along lines ended with dots.
    // checks for reasonable first letter of each segment, i.e not before, or way after the previous first letter,
    // unless the citation do not seem to be ordered.
    private List<String> splitGenerically(String referencesText) {

        List<String> parts = new ArrayList<String>();
        Matcher matcher = GENERIC_SPLIT_PATTERN.matcher(referencesText);

        // determine the gap size between two consecutive first letters.
        // determine if the citations are lexicographically ordered.
        boolean citationsAreOrdered = true;
        int numSegments = 0;
        int orderViolations = 0;
        char lastFirstLetter = 'A';
        while (matcher.find()) {
            numSegments++;
            if (matcher.end() >= referencesText.length()) {
                break;
            }
            if (referencesText.charAt(matcher.end()) < lastFirstLetter) {
                orderViolations++;
            }
        }

        if (numSegments == 0) {
            LOGGER.info("Single segment found!");
            return Arrays.asList(referencesText);
        }
        if (orderViolations > .25 * numSegments) {
            LOGGER.info("Citations not ordered.");
            citationsAreOrdered = false;
        }

        int gapsize = ((26 / numSegments) + 1) * 2;
        matcher.reset();

        char previousFirstChar = referencesText.charAt(0);
        Integer currentSegmentStartIndex = 0;
        Integer currentSegmentEndIndex;

        char currentFirstChar;
        while (matcher.find()) {
            if (matcher.end() >= referencesText.length()) {
                break;
            }
            currentFirstChar = referencesText.charAt(matcher.end());
            currentSegmentEndIndex = matcher.start();
            if (currentSegmentEndIndex - currentSegmentStartIndex > MINIMUM_SEGMENT_LENGTH &&
                    (!citationsAreOrdered || isValidNextFirstLetter(previousFirstChar, currentFirstChar, gapsize))) {
                parts.add(referencesText.substring(currentSegmentStartIndex, currentSegmentEndIndex));
                previousFirstChar = currentFirstChar;
                currentSegmentStartIndex = currentSegmentEndIndex + 2;
            }
        }
        parts.add(referencesText.substring(currentSegmentStartIndex));
        return parts;
    }

    /**
     * @param maxGapsize the maximum number of letters that are allowed to skip.
     *                   if previousFirstLetter is followed by uncommon letters (like Q), then
     *                   gapsize is increased.
     */
    private boolean isValidNextFirstLetter(char previousFirstLetter, char firstLetter, int maxGapsize) {
        if (firstLetter < previousFirstLetter) {
            return false;
        }
        if (sparseLetters.contains(previousFirstLetter)) {
            maxGapsize = maxGapsize + 2;
        }
        return firstLetter - previousFirstLetter <= maxGapsize;
    }

}
