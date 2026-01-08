package org.grobid.core.engines.tagging.delft;

import org.lmdbjava.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Word embeddings lookup using LMDB database.
 * 
 * Reads embeddings from LMDB where values are raw float32 arrays
 * (little-endian).
 * Use convert_lmdb_embeddings.py to convert from pickled numpy format.
 */
public class WordEmbeddings implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordEmbeddings.class);

    private final Env<ByteBuffer> env;
    private final Dbi<ByteBuffer> dbi;
    private final int embeddingSize;
    private final float[] zeroVector;

    /**
     * Open LMDB database for word embeddings.
     * 
     * @param dbPath        Path to the LMDB database directory
     * @param embeddingSize Dimension of the embeddings
     * @throws IOException if the database cannot be opened (missing path, LMDB
     *                     error, or native library issue)
     */
    public WordEmbeddings(Path dbPath, int embeddingSize) throws IOException {
        this.embeddingSize = embeddingSize;
        this.zeroVector = new float[embeddingSize];

        // Check if path exists before trying to open
        if (!Files.exists(dbPath)) {
            throw new IOException("Embeddings database not found: " + dbPath.toAbsolutePath() +
                    "\nPlease provide a valid path to an LMDB embeddings database.");
        }
        if (!Files.isDirectory(dbPath)) {
            throw new IOException("Embeddings path is not a directory: " + dbPath.toAbsolutePath() +
                    "\nLMDB databases are directories containing 'data.mdb' and 'lock.mdb' files.");
        }

        try {
            // Open LMDB environment with increased reader slots for high concurrency
            this.env = Env.create()
                    .setMapSize(10_000_000_000L) // 10GB max
                    .setMaxReaders(512) // Support high concurrency (default is 126)
                    .setMaxDbs(1)
                    .open(dbPath.toFile());

            // Open the default database
            this.dbi = env.openDbi((String) null, DbiFlags.MDB_CREATE);
        } catch (LmdbException e) {
            throw new IOException("Failed to open LMDB database at " + dbPath.toAbsolutePath() +
                    ": " + e.getMessage(), e);
        } catch (UnsatisfiedLinkError e) {
            throw new IOException("LMDB native library failed to load. " +
                    "Ensure lmdbjava dependency includes native libraries for your platform. " +
                    "Error: " + e.getMessage(), e);
        }

        // Validate that the database contains raw float32 format (not pickled numpy)
        validateEmbeddingFormat(dbPath);

        LOGGER.info("Opened LMDB database at {}", dbPath);
    }

    /**
     * Look up embedding for a word.
     * 
     * @param word The word to look up
     * @return Embedding vector, or zero vector if not found
     * @throws RuntimeException if LMDB database access fails
     */
    public float[] getEmbedding(String word) {
        // Normalize digits to "0" like Python's _normalize_num
        String normalizedWord = normalizeNum(word);

        byte[] keyBytes = normalizedWord.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        try (Txn<ByteBuffer> txn = env.txnRead()) {
            ByteBuffer valueBuffer = dbi.get(txn, keyBuffer);

            if (valueBuffer == null) {
                // Word not found, return zero vector
                return zeroVector.clone();
            }

            // Parse float array from raw bytes (little-endian float32)
            valueBuffer.order(ByteOrder.LITTLE_ENDIAN);
            float[] embedding = new float[embeddingSize];
            for (int i = 0; i < embeddingSize; i++) {
                embedding[i] = valueBuffer.getFloat();
            }
            return embedding;
        } catch (LmdbException e) {
            throw new RuntimeException(
                    "LMDB database error during embedding lookup for word '" + word + "': " + e.getMessage(), e);
        }
    }

    /**
     * Normalize digits in a word to "0" (matches Python's _normalize_num).
     * This is needed because the model was trained with this normalization.
     * 
     * @param word Input word
     * @return Word with all digits replaced by "0"
     */
    private String normalizeNum(String word) {
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append('0');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Look up embeddings for a sequence of words.
     * 
     * Uses a single LMDB read transaction for all lookups to avoid
     * exhausting reader slots under high concurrency.
     * 
     * @param words Array of words
     * @return 2D array [seq_len][embedding_size]
     * @throws RuntimeException if LMDB database access fails
     */
    public float[][] getEmbeddings(String[] words) {
        float[][] result = new float[words.length][embeddingSize];

        try (Txn<ByteBuffer> txn = env.txnRead()) {
            for (int i = 0; i < words.length; i++) {
                result[i] = getEmbeddingWithTxn(words[i], txn);
            }
        } catch (LmdbException e) {
            throw new RuntimeException(
                    "LMDB database error during batch embedding lookup: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Look up embedding for a word using an existing transaction.
     * 
     * @param word The word to look up
     * @param txn  Active read transaction
     * @return Embedding vector, or zero vector if not found
     */
    private float[] getEmbeddingWithTxn(String word, Txn<ByteBuffer> txn) {
        // Normalize digits to "0" like Python's _normalize_num
        String normalizedWord = normalizeNum(word);

        byte[] keyBytes = normalizedWord.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        ByteBuffer valueBuffer = dbi.get(txn, keyBuffer);

        if (valueBuffer == null) {
            // Word not found, return zero vector
            return zeroVector.clone();
        }

        // Parse float array from raw bytes (little-endian float32)
        valueBuffer.order(ByteOrder.LITTLE_ENDIAN);
        float[] embedding = new float[embeddingSize];
        for (int i = 0; i < embeddingSize; i++) {
            embedding[i] = valueBuffer.getFloat();
        }
        return embedding;
    }

    /**
     * Check if a word exists in the database.
     * 
     * @throws RuntimeException if LMDB database access fails
     */
    public boolean contains(String word) {
        byte[] keyBytes = word.getBytes(StandardCharsets.UTF_8);
        ByteBuffer keyBuffer = ByteBuffer.allocateDirect(keyBytes.length);
        keyBuffer.put(keyBytes).flip();

        try (Txn<ByteBuffer> txn = env.txnRead()) {
            return dbi.get(txn, keyBuffer) != null;
        } catch (LmdbException e) {
            throw new RuntimeException("LMDB database error checking word '" + word + "': " + e.getMessage(), e);
        }
    }

    public int getEmbeddingSize() {
        return embeddingSize;
    }

    /**
     * Validate that the embeddings database contains raw float32 format.
     * 
     * If the database contains pickled numpy arrays (the old DeLFT format),
     * the bytes will be interpreted as garbage floats with extreme values.
     * This validation fails fast at startup with a clear error message.
     * 
     * @param dbPath Path to the database (for error messages)
     * @throws IOException if validation fails
     */
    private void validateEmbeddingFormat(Path dbPath) throws IOException {
        // Common test words that should exist in any GloVe/word2vec vocabulary
        String[] testWords = { "the", "and", "of", "to", "in" };
        final float MAX_VALID_VALUE = 10.0f; // GloVe values are typically < 5

        for (String testWord : testWords) {
            if (contains(testWord)) {
                float[] embedding = getEmbedding(testWord);

                for (int i = 0; i < embedding.length; i++) {
                    float value = embedding[i];

                    if (Float.isNaN(value) || Float.isInfinite(value) || Math.abs(value) > MAX_VALID_VALUE) {
                        close(); // Clean up before throwing
                        throw new IOException(
                                "Embeddings database at " + dbPath.toAbsolutePath() + " appears to contain " +
                                        "pickled numpy format instead of raw float32.\n" +
                                        "Found invalid embedding value for word '" + testWord + "': " +
                                        (Float.isNaN(value) ? "NaN" : Float.isInfinite(value) ? "Infinity" : value) +
                                        " at index " + i + ".\n" +
                                        "Please regenerate embeddings using:\n" +
                                        "  python3 grobid-home/scripts/preload_embeddings.py --embedding glove-840B");
                    }
                }

                LOGGER.debug("Embeddings format validation passed for word '{}'", testWord);
                return; // Validation passed for one word, that's enough
            }
        }

        LOGGER.warn("Could not validate embeddings format - none of the test words found in database");
    }

    @Override
    public void close() {
        if (dbi != null) {
            dbi.close();
        }
        if (env != null) {
            env.close();
        }
    }
}
