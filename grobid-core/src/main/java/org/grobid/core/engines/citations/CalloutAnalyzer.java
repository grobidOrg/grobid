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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;

/**
 *  Identify the type of the marker callout with regex
 *
 */
public class CalloutAnalyzer {

    // callout/marker type, this is used to discard incorrect numerical reference marker candidates
    // that do not follow the majority reference marker pattern
    public enum MarkerType {
        UNKNOWN, BRACKET_TEXT, BRACKET_NUMBER, PARENTHESIS_TEXT, PARENTHESIS_NUMBER, SUPERSCRIPT_NUMBER, NUMBER, ROMAN
    }

    // simple patterns just to capture the majority callout style
    private final static Pattern BRACKET_TEXT_PATTERN = Pattern.compile("\\[(.)+\\]");
    //private final static Pattern BRACKET_NUMBER_PATTERN = Pattern.compile("\\[((\\d{0,4}[a-f]?)|[,-;•])+\\]");
    private final static Pattern BRACKET_NUMBER_PATTERN = Pattern
            .compile("\\[(?>[0-9]{1,4}[a-f]?[\\-;•,]?((and)|&|(et))?)+\\]");
    private final static Pattern PARENTHESIS_TEXT_PATTERN = Pattern.compile("\\((.)+\\)");
    //private final static Pattern PARENTHESIS_NUMBER_PATTERN = Pattern.compile("\\(((\\d+[a-f]?)|[,-;•])+\\)");
    private final static Pattern PARENTHESIS_NUMBER_PATTERN = Pattern
            .compile("\\((?>[0-9]{1,4}[a-f]?[\\-;•,]?((and)|&|(et))?)+\\)");
    private final static Pattern NUMBER_PATTERN = Pattern.compile("(?>\\d+)[a-f]?");
    private final static Pattern ROMAN_PATTERN = Pattern.compile("(IX|IV|V?I{0,3})");

    public static MarkerType getCalloutType(List<LayoutToken> callout) {
        if (callout == null)
            return MarkerType.UNKNOWN;

        String calloutString = LayoutTokensUtil.toText(callout);
        if (calloutString == null || calloutString.trim().length() == 0)
            return MarkerType.UNKNOWN;

        calloutString = calloutString.replace(" ", "");
        boolean isSuperScript = true;

        for (LayoutToken token : callout) {
            if (token.getText().trim().length() == 0)
                continue;
            if (!token.isSuperscript()) {
                isSuperScript = false;
                break;
            }
        }

        Matcher matcher = NUMBER_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            if (isSuperScript) {
                return MarkerType.SUPERSCRIPT_NUMBER;
            }
        }

        matcher = BRACKET_NUMBER_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.BRACKET_NUMBER;
        }

        matcher = PARENTHESIS_NUMBER_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.PARENTHESIS_NUMBER;
        }

        matcher = BRACKET_TEXT_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.BRACKET_TEXT;
        }

        matcher = PARENTHESIS_TEXT_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.PARENTHESIS_TEXT;
        }

        matcher = NUMBER_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.NUMBER;
        }

        matcher = ROMAN_PATTERN.matcher(calloutString);
        if (matcher.find()) {
            return MarkerType.ROMAN;
        }

        return MarkerType.UNKNOWN;
    }

}
