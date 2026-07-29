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
package org.grobid.trainer.evaluation.utilities;

import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * String normalization helpers shared by the end-to-end evaluation and by the custom field
 * evaluators. These are pure functions extracted from {@code EndToEndEvaluation} so that
 * {@link CustomFieldEvaluator} implementations can apply exactly the same normalization as the
 * generic field-matching path.
 */
public class EvaluationNormalization {

    private EvaluationNormalization() {
        // utility class
    }

    public static String basicNormalization(String string) {
        string = string.trim();
        string = string.replace("\n", " ");
        string = string.replace("\t", " ");
        string = string.replaceAll(" ( )*", " ");
        string = string.replace("&apos;", "'");
        return string.trim().toLowerCase();
    }

    public static String identifierNormalization(String string) {
        string = basicNormalization(string);
        if (string.startsWith("pmcpmc")) {
            string = string.replace("pmcpmc", "");
        }
        string = string.replace("pmc", "");
        if (string.startsWith("doi")) {
            string = string.replace("doi", "").trim();
            if (string.startsWith(":")) {
                string = string.substring(1, string.length());
                string = string.trim();
            }
        }
        if (string.startsWith("pmid")) {
            string = string.replace("pmid", "").trim();
            if (string.startsWith(":")) {
                string = string.substring(1, string.length());
                string = string.trim();
            }
        }
        return string.trim().toLowerCase();
    }

    public static String basicNormalizationFullText(String string, String fieldName) {
        string = string.trim();
        string = UnicodeUtil.normaliseText(string);
        string = string.replace("\n", " ");
        string = string.replace("\t", " ");
        string = string.replace("_", " ");
        string = string.replace(" ", " ");
        if (fieldName.equals("reference_figure")) {
            string = string.replace("figure", "")
                    .replace("Figure", "")
                    .replace("fig.", "")
                    .replace("Fig.", "")
                    .replace("fig", "")
                    .replace("Fig", "");
        }
        if (fieldName.equals("reference_table")) {
            string = string.replace("table", "").replace("Table", "");
        }
        string = string.replaceAll(" ( )*", " ");
        if (string.startsWith("[") || string.startsWith("("))
            string = string.substring(1, string.length());
        while (string.endsWith("]") || string.endsWith(")") || string.endsWith(","))
            string = string.substring(0, string.length() - 1);
        return string.trim();
    }

    public static String removeFullPunct(String string) {
        StringBuilder result = new StringBuilder();
        string = string.toLowerCase();
        String allMismatchToIgnore = TextUtilities.fullPunctuations
                + "‐ \t\n\r "
                + "·◼▲►◆○◇●◎◽◸◹◺";//last are placeholders used for to be OCR chars
        for (int i = 0; i < string.length(); i++) {
            if (allMismatchToIgnore.indexOf(string.charAt(i)) == -1) {
                result.append(string.charAt(i));
            }
        }
        return result.toString();
    }
}
