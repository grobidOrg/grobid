package org.grobid.core.utilities;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link SentenceUtilities#trimAndFilterSentenceOffsets(String, List)} - the chokepoint
 * that guarantees returned sentence spans are non-empty and free of leading/trailing whitespace. These
 * run without any segmenter (no PowerMock), so they are JDK 17+ friendly.
 */
public class SentenceUtilitiesTrimTest {

    @Test
    public void testTrimsTrailingWhitespace() {
        String text = "abc  ";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(0, 5)));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).start, is(0));
        assertThat(result.get(0).end, is(3));
        assertThat(text.substring(result.get(0).start, result.get(0).end), is("abc"));
    }

    @Test
    public void testTrimsLeadingWhitespace() {
        String text = "   abc";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(0, 6)));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).start, is(3));
        assertThat(result.get(0).end, is(6));
        assertThat(text.substring(result.get(0).start, result.get(0).end), is("abc"));
    }

    @Test
    public void testDropsWhitespaceOnlySpan() {
        String text = "abc   def";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(3, 6)));
        assertThat(result, hasSize(0));
    }

    @Test
    public void testDropsEmptySpan() {
        String text = "abcdef";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(3, 3)));
        assertThat(result, hasSize(0));
    }

    @Test
    public void testClampsEndPastTextLength() {
        String text = "abc";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(0, 100)));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).end, is(3));
    }

    @Test
    public void testDropsNegativeStart() {
        String text = "abc";
        List<OffsetPosition> result = SentenceUtilities
                .trimAndFilterSentenceOffsets(text, Arrays.asList(new OffsetPosition(-1, 3)));
        assertThat(result, hasSize(0));
    }

    @Test
    public void testInvariantAcrossMixedSpans() {
        String text = "First sentence.  Second.  ";
        List<OffsetPosition> input = Arrays.asList(
                new OffsetPosition(0, 16),   // "First sentence. " -> trailing space
                new OffsetPosition(15, 17),  // "  " -> whitespace only, dropped
                new OffsetPosition(17, 26)); // "Second.  " -> trailing spaces
        List<OffsetPosition> result = SentenceUtilities.trimAndFilterSentenceOffsets(text, input);

        assertThat(result, hasSize(2));
        for (OffsetPosition span : result) {
            String s = text.substring(span.start, span.end);
            assertThat("span must be non-empty", s.isEmpty(), is(false));
            assertThat("span must be trimmed", s.equals(s.trim()), is(true));
        }
    }

    @Test
    public void testNullPositionsReturnedAsIs() {
        assertThat(SentenceUtilities.trimAndFilterSentenceOffsets("abc", null), is((List<OffsetPosition>) null));
    }

    @Test
    public void testEmptyInput() {
        List<OffsetPosition> result = SentenceUtilities.trimAndFilterSentenceOffsets("abc", Collections.emptyList());
        assertThat(result, hasSize(0));
    }
}
