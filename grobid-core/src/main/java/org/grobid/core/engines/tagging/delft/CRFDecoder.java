package org.grobid.core.engines.tagging.delft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * CRF Viterbi decoder for use with ONNX-exported DeLFT models.
 * 
 * Performs Viterbi decoding given emission scores and learned transition
 * matrices.
 * The transition matrices are loaded from JSON exported by Python.
 */
public class CRFDecoder {

    private final int numTags;
    private final float[][] transitions; // [from_tag][to_tag]
    private final float[] startTransitions; // [num_tags]
    private final float[] endTransitions; // [num_tags]

    /**
     * Create a CRF decoder with learned parameters.
     * 
     * @param transitions      Transition matrix [from_tag][to_tag]
     * @param startTransitions Start transition scores
     * @param endTransitions   End transition scores
     */
    public CRFDecoder(float[][] transitions, float[] startTransitions, float[] endTransitions) {
        this.numTags = transitions.length;
        this.transitions = transitions;
        this.startTransitions = startTransitions;
        this.endTransitions = endTransitions;
    }

    /**
     * Load CRF parameters from JSON file exported by Python.
     * 
     * @param jsonPath Path to crf_params.json
     * @return CRFDecoder instance
     */
    public static CRFDecoder fromJson(Path jsonPath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(jsonPath.toFile())) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // Parse transitions [num_tags][num_tags]
            double[][] transitionsDouble = gson.fromJson(json.get("transitions"), double[][].class);
            float[][] transitions = toFloatArray2D(transitionsDouble);

            // Parse start transitions [num_tags]
            double[] startDouble = gson.fromJson(json.get("startTransitions"), double[].class);
            float[] startTransitions = toFloatArray(startDouble);

            // Parse end transitions [num_tags]
            double[] endDouble = gson.fromJson(json.get("endTransitions"), double[].class);
            float[] endTransitions = toFloatArray(endDouble);

            return new CRFDecoder(transitions, startTransitions, endTransitions);
        }
    }

    private static float[] toFloatArray(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }

    private static float[][] toFloatArray2D(double[][] doubles) {
        float[][] floats = new float[doubles.length][];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = toFloatArray(doubles[i]);
        }
        return floats;
    }

    /**
     * Decode the best tag sequence using Viterbi algorithm.
     * 
     * @param emissions Emission scores from the model [seq_len][num_tags]
     * @param mask      Mask indicating valid tokens (true = valid). Can be null.
     * @return Best tag sequence as array of tag indices
     */
    public int[] decode(float[][] emissions, boolean[] mask) {
        int seqLength = emissions.length;

        if (mask == null) {
            mask = new boolean[seqLength];
            Arrays.fill(mask, true);
        }

        // Find actual sequence length (excluding padding)
        int actualLength = 0;
        for (int i = 0; i < seqLength; i++) {
            if (mask[i])
                actualLength = i + 1;
        }

        if (actualLength == 0) {
            return new int[0];
        }

        // Viterbi score matrix [seq_len][num_tags]
        float[][] viterbiScore = new float[actualLength][numTags];

        // Backpointer matrix [seq_len][num_tags]
        int[][] backpointers = new int[actualLength][numTags];

        // Initialize first position with start transitions + emissions
        for (int tag = 0; tag < numTags; tag++) {
            viterbiScore[0][tag] = startTransitions[tag] + emissions[0][tag];
        }

        // Forward pass
        for (int t = 1; t < actualLength; t++) {
            for (int currentTag = 0; currentTag < numTags; currentTag++) {
                float bestScore = Float.NEGATIVE_INFINITY;
                int bestPrevTag = 0;

                for (int prevTag = 0; prevTag < numTags; prevTag++) {
                    float score = viterbiScore[t - 1][prevTag]
                            + transitions[prevTag][currentTag]
                            + emissions[t][currentTag];

                    if (score > bestScore) {
                        bestScore = score;
                        bestPrevTag = prevTag;
                    }
                }

                viterbiScore[t][currentTag] = bestScore;
                backpointers[t][currentTag] = bestPrevTag;
            }
        }

        // Add end transitions to find best final tag
        float bestFinalScore = Float.NEGATIVE_INFINITY;
        int bestLastTag = 0;

        for (int tag = 0; tag < numTags; tag++) {
            float score = viterbiScore[actualLength - 1][tag] + endTransitions[tag];
            if (score > bestFinalScore) {
                bestFinalScore = score;
                bestLastTag = tag;
            }
        }

        // Backtrack to find best sequence
        int[] bestPath = new int[actualLength];
        bestPath[actualLength - 1] = bestLastTag;

        for (int t = actualLength - 2; t >= 0; t--) {
            bestPath[t] = backpointers[t + 1][bestPath[t + 1]];
        }

        return bestPath;
    }

    /**
     * Decode multiple sequences (batch processing).
     * 
     * @param emissions Batch of emission scores [batch_size][seq_len][num_tags]
     * @param masks     Masks for each sequence. Can be null.
     * @return Array of best tag sequences
     */
    public int[][] decodeBatch(float[][][] emissions, boolean[][] masks) {
        int batchSize = emissions.length;
        int[][] results = new int[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            boolean[] mask = (masks != null) ? masks[i] : null;
            results[i] = decode(emissions[i], mask);
        }

        return results;
    }

    public int getNumTags() {
        return numTags;
    }
}
