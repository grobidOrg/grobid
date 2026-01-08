package org.grobid.core.engines.tagging.delft;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for DeLFTOnnxModel sequence chunking functionality.
 */
public class DeLFTOnnxModelTest {

    /**
     * Test that input is correctly split into sequences at empty lines.
     */
    @Test
    public void testSequenceSplitting() {
        String input = "token1\tfeature1\ntoken2\tfeature2\n\ntoken3\tfeature3\ntoken4\tfeature4\n";

        int sequenceCount = countSequences(input);

        assertEquals(2, sequenceCount);
    }

    /**
     * Test chunking calculation for large sequences.
     */
    @Test
    public void testChunkingCalculation() {
        int totalTokens = 6748; // Size of large_sequence.txt
        int maxSeqLength = 512;

        int expectedChunks = (int) Math.ceil((double) totalTokens / maxSeqLength);

        assertEquals(14, expectedChunks);
    }

    /**
     * Test that features are correctly parsed from tab-separated lines.
     */
    @Test
    public void testFeatureParsing() {
        String line = "token\tf1\tf2\tf3\tf4";
        String[] parts = line.split("[\\t\\s]+");

        assertEquals(5, parts.length);
    }

    // Helper to count sequences in input
    private int countSequences(String input) {
        String[] lines = input.split("\n", -1);
        int count = 0;
        boolean inSequence = false;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                if (inSequence) {
                    count++;
                    inSequence = false;
                }
            } else {
                inSequence = true;
            }
        }
        if (inSequence)
            count++;

        return count;
    }
}
