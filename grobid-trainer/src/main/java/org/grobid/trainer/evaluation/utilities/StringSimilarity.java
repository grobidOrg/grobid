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

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import scala.Option;

import org.grobid.core.utilities.TextUtilities;

/**
 * Similarity measures used by the Levenshtein and Ratcliff/Obershelp matching variants of the
 * end-to-end evaluation.
 */
public final class StringSimilarity {

    private StringSimilarity() {
    }

    /**
     * Levenshtein similarity relative to the longest string: {@code (max - distance) / max}.
     * Two equal strings (including two empty ones) have similarity 1.
     */
    public static double levenshtein(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        int distance = TextUtilities.getLevenshteinDistance(a, b);
        int bigger = Math.max(a.length(), b.length());
        return (double) (bigger - distance) / bigger;
    }

    /**
     * Ratcliff/Obershelp similarity. Two equal strings have similarity 1, and a string compared
     * with an empty one has similarity 0.
     */
    public static double ratcliffObershelp(String a, String b) {
        if (a.trim().equals(b.trim())) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Option<Object> similarity = RatcliffObershelpMetric.compare(a, b);
        if (similarity != null && similarity.isDefined() && similarity.get() != null) {
            return (Double) similarity.get();
        }
        return 0.0;
    }
}
