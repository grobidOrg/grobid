package org.grobid.core.engines.tagging.delft;

import org.grobid.core.utilities.GrobidProperties;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Integration test for WordEmbeddings LMDB lookup.
 * 
 * This test verifies that ONNX word embeddings can be loaded and queried
 * correctly from the LMDB database. It helps diagnose issues with:
 * - LMDB native library loading (especially on Linux)
 * - LMDB database format compatibility
 * - Embeddings path configuration
 * 
 * Prerequisites:
 * - Embeddings must be preloaded using: python3
 * grobid-home/scripts/preload_embeddings.py --embedding glove-840B
 * - The LMDB database should be at {delft}/data/db/glove-840B
 */
public class WordEmbeddingsIntegrationTest {

    private static final String EMBEDDINGS_NAME = "glove-840B";
    private static final int EMBEDDING_SIZE = 300; // GloVe-840B dimension

    private static Path embeddingsPath;
    private WordEmbeddings embeddings;

    @BeforeClass
    public static void setUpClass() {
        // Initialize GROBID properties
        GrobidProperties.getInstance();

        // Get embeddings path
        String delftPath = GrobidProperties.getDeLFTFilePath();
        embeddingsPath = Path.of(delftPath, "data", "db", EMBEDDINGS_NAME);
    }

    @Before
    public void setUp() throws IOException {
        // Skip test if embeddings are not available
        assumeTrue("Embeddings not found at " + embeddingsPath +
                ". Please run: python3 grobid-home/scripts/preload_embeddings.py --embedding glove-840B",
                Files.exists(embeddingsPath) && Files.isDirectory(embeddingsPath));

        // Open embeddings database
        embeddings = new WordEmbeddings(embeddingsPath, EMBEDDING_SIZE);
    }

    @After
    public void tearDown() {
        if (embeddings != null) {
            embeddings.close();
        }
    }

    @Test
    public void testEmbeddingsCanBeOpened() {
        // If we get here without exception, the database opened successfully
        assertThat(embeddings, is(notNullValue()));
        assertThat(embeddings.getEmbeddingSize(), is(EMBEDDING_SIZE));
    }

    @Test
    public void testLookupKnownWord() {
        // "the" is one of the most common words and should be in any embedding
        float[] embedding = embeddings.getEmbedding("the");

        assertThat("Embedding should not be null", embedding, is(notNullValue()));
        assertThat("Embedding should have correct dimension", embedding.length, is(EMBEDDING_SIZE));

        // Check that the embedding is not all zeros (word was found)
        double sumSquares = 0.0;
        for (float f : embedding) {
            sumSquares += f * f;
        }
        assertThat("Embedding for 'the' should not be all zeros", sumSquares, greaterThan(0.0));

        // Validate embedding values are in expected range for GloVe
        // If LMDB contains pickled numpy format (not raw float32), values will be
        // garbage
        validateEmbeddingValuesInRange(embedding, "the");
    }

    @Test
    public void testLookupAnotherKnownWord() {
        // Test another common word
        float[] embedding = embeddings.getEmbedding("science");

        assertThat(embedding, is(notNullValue()));
        assertThat(embedding.length, is(EMBEDDING_SIZE));

        // Check that the embedding is not all zeros
        double sumSquares = 0.0;
        for (float f : embedding) {
            sumSquares += f * f;
        }
        assertThat("Embedding for 'science' should not be all zeros", sumSquares, greaterThan(0.0));
    }

    @Test
    public void testContainsKnownWord() {
        // Test the contains method for a word that should exist
        assertTrue("Database should contain 'the'", embeddings.contains("the"));
        assertTrue("Database should contain 'and'", embeddings.contains("and"));
    }

    @Test
    public void testGetMultipleEmbeddings() {
        String[] words = { "the", "quick", "brown", "fox" };
        float[][] embeddingsResult = embeddings.getEmbeddings(words);

        assertThat("Batch result should have correct length",
                embeddingsResult.length, is(words.length));

        for (int i = 0; i < words.length; i++) {
            assertThat("Each embedding should have correct dimension",
                    embeddingsResult[i].length, is(EMBEDDING_SIZE));
        }
    }

    @Test
    public void testDigitNormalization() {
        // WordEmbeddings should normalize digits to "0"
        // So "2024" should look up "0000"
        float[] embedding = embeddings.getEmbedding("2024");

        assertThat(embedding, is(notNullValue()));
        assertThat(embedding.length, is(EMBEDDING_SIZE));
        // Note: The normalized form "0000" may or may not be in the vocabulary,
        // but the lookup should succeed (returning zero vector if not found)
    }

    @Test
    public void testUnknownWordReturnsZeroVector() {
        // A very unlikely word that should not be in the vocabulary
        float[] embedding = embeddings.getEmbedding("xyzzy12345qwerty");

        assertThat(embedding, is(notNullValue()));
        assertThat(embedding.length, is(EMBEDDING_SIZE));

        // Should be all zeros
        double sumSquares = 0.0;
        for (float f : embedding) {
            sumSquares += f * f;
        }
        assertThat("Unknown word should return zero vector", sumSquares, is(0.0));
    }

    /**
     * Validates that embedding values are in expected range for GloVe embeddings.
     * 
     * GloVe embeddings typically have values in the range of approximately -5 to 5.
     * If the LMDB database contains pickled numpy format instead of raw float32,
     * the bytes will be interpreted as garbage floats with extreme values (often
     * very large or NaN/Infinity).
     * 
     * @param embedding The embedding vector to validate
     * @param word      The word being looked up (for error messages)
     */
    private void validateEmbeddingValuesInRange(float[] embedding, String word) {
        final float MAX_VALID_VALUE = 10.0f; // GloVe values are typically < 5

        for (int i = 0; i < embedding.length; i++) {
            float value = embedding[i];

            // Check for NaN or Infinity (common when interpreting pickle bytes as float)
            assertTrue(
                    String.format("Embedding for '%s' contains NaN at index %d. " +
                            "This suggests the LMDB database contains pickled numpy format " +
                            "instead of raw float32. Please regenerate embeddings using: " +
                            "python3 grobid-home/scripts/preload_embeddings.py --embedding glove-840B",
                            word, i),
                    !Float.isNaN(value));

            assertTrue(
                    String.format("Embedding for '%s' contains Infinity at index %d. " +
                            "This suggests the LMDB database contains pickled numpy format " +
                            "instead of raw float32.", word, i),
                    !Float.isInfinite(value));

            // Check for extreme values (pickled data often produces very large floats)
            assertTrue(
                    String.format("Embedding for '%s' has extreme value %.2f at index %d " +
                            "(expected range: -%.0f to %.0f). This suggests the LMDB database " +
                            "contains pickled numpy format instead of raw float32.",
                            word, value, i, MAX_VALID_VALUE, MAX_VALID_VALUE),
                    Math.abs(value) <= MAX_VALID_VALUE);
        }
    }
}
