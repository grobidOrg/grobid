package org.grobid.core.engines;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
