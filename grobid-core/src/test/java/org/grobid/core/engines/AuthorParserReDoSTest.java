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
package org.grobid.core.engines;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Tests that the ET_AL_PATTERN regex does not exhibit ReDoS behavior.
 * Tests the regex directly without requiring model loading.
 */
public class AuthorParserReDoSTest {

    // Mirror the pattern from AuthorParser
    private static final Pattern ET_AL_PATTERN = Pattern.compile("et\\.? al\\.?");

    @Test(timeout = 5000)
    public void testReDoS_repeatedEtAl() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            sb.append("et al. ");
        }
        String input = sb.toString();

        long start = System.currentTimeMillis();
        Matcher matcher = ET_AL_PATTERN.matcher(input);
        assertTrue("Should find 'et al.' in input", matcher.find());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Regex matching took too long (" + elapsed + "ms), possible ReDoS", elapsed < 1000);
    }

    @Test(timeout = 5000)
    public void testReDoS_noMatch() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            sb.append("Smith Johnson ");
        }
        String input = sb.toString();

        long start = System.currentTimeMillis();
        Matcher matcher = ET_AL_PATTERN.matcher(input);
        assertFalse("Should not find 'et al.' in input", matcher.find());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Regex matching took too long (" + elapsed + "ms), possible ReDoS", elapsed < 1000);
    }

    @Test(timeout = 5000)
    public void testReDoS_nearMissPattern() {
        // Pathological near-miss: many "et " without "al" — forces scanning
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            sb.append("et ");
        }
        String input = sb.toString();

        long start = System.currentTimeMillis();
        Matcher matcher = ET_AL_PATTERN.matcher(input);
        assertFalse("Should not find 'et al.' in near-miss input", matcher.find());
        long elapsed = System.currentTimeMillis() - start;

        assertTrue("Regex matching took too long (" + elapsed + "ms), possible ReDoS", elapsed < 1000);
    }
}
