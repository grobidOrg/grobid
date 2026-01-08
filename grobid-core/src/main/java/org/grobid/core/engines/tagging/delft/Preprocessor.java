package org.grobid.core.engines.tagging.delft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Preprocessor for converting text to model inputs.
 * 
 * Uses GrobidAnalyzer for tokenization and converts to character indices.
 * Supports feature preprocessing for BidLSTM_CRF_FEATURES models.
 */
public class Preprocessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Preprocessor.class);

    private final Map<String, Integer> charVocab;
    private final Map<Integer, String> tagIndex;
    private final int maxCharLength;
    private final int padIndex;
    private final int unkIndex;

    // Feature preprocessing support
    private final List<Integer> featuresIndices;
    private final Map<Integer, Map<String, Integer>> featuresMapToIndex;
    private final boolean hasFeatures;

    /**
     * Create preprocessor with vocabularies (no features).
     */
    public Preprocessor(Map<String, Integer> charVocab, Map<Integer, String> tagIndex, int maxCharLength) {
        this(charVocab, tagIndex, maxCharLength, null, null);
    }

    /**
     * Create preprocessor with vocabularies and features support.
     */
    public Preprocessor(Map<String, Integer> charVocab, Map<Integer, String> tagIndex, int maxCharLength,
            List<Integer> featuresIndices, Map<Integer, Map<String, Integer>> featuresMapToIndex) {
        this.charVocab = charVocab;
        this.tagIndex = tagIndex;
        this.maxCharLength = maxCharLength;
        this.padIndex = charVocab.getOrDefault("<PAD>", 0);
        this.unkIndex = charVocab.getOrDefault("<UNK>", 1);
        this.featuresIndices = featuresIndices;
        this.featuresMapToIndex = featuresMapToIndex;
        this.hasFeatures = featuresIndices != null && !featuresIndices.isEmpty();
    }

    /**
     * Load preprocessor from vocab.json exported by Python.
     */
    public static Preprocessor fromJson(Path vocabPath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(vocabPath.toFile())) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            // Parse char vocab
            Map<String, Double> charVocabDouble = gson.fromJson(json.get("charVocab"), HashMap.class);
            Map<String, Integer> charVocab = new HashMap<>();
            for (Map.Entry<String, Double> entry : charVocabDouble.entrySet()) {
                charVocab.put(entry.getKey(), entry.getValue().intValue());
            }

            // Parse tag index (index -> tag name)
            Map<String, String> tagIndexStr = gson.fromJson(json.get("tagIndex"), HashMap.class);
            Map<Integer, String> tagIndex = new HashMap<>();
            for (Map.Entry<String, String> entry : tagIndexStr.entrySet()) {
                tagIndex.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }

            int maxCharLength = json.get("maxCharLength").getAsInt();

            // Parse feature mappings if present
            List<Integer> featuresIndices = null;
            Map<Integer, Map<String, Integer>> featuresMapToIndex = null;

            if (json.has("featuresIndices") && !json.get("featuresIndices").isJsonNull()) {
                JsonArray indicesArray = json.getAsJsonArray("featuresIndices");
                featuresIndices = new ArrayList<>();
                for (JsonElement el : indicesArray) {
                    featuresIndices.add(el.getAsInt());
                }

                LOGGER.info("Loaded {} feature indices", featuresIndices.size());
            }

            if (json.has("featuresMapToIndex") && !json.get("featuresMapToIndex").isJsonNull()) {
                JsonObject fmti = json.getAsJsonObject("featuresMapToIndex");
                featuresMapToIndex = new HashMap<>();

                for (String featureIdxStr : fmti.keySet()) {
                    Integer featureIdx = Integer.parseInt(featureIdxStr);
                    JsonObject valueMap = fmti.getAsJsonObject(featureIdxStr);
                    Map<String, Integer> innerMap = new HashMap<>();

                    for (String valueName : valueMap.keySet()) {
                        innerMap.put(valueName, valueMap.get(valueName).getAsInt());
                    }

                    featuresMapToIndex.put(featureIdx, innerMap);
                }

                LOGGER.info("Loaded feature vocabulary with {} feature columns", featuresMapToIndex.size());
            }

            return new Preprocessor(charVocab, tagIndex, maxCharLength, featuresIndices, featuresMapToIndex);
        }
    }

    /**
     * Tokenize text using GrobidAnalyzer.
     * Filters out whitespace-only tokens to match Python DeLFT behavior.
     * 
     * @param text Input text
     * @return List of tokens (excluding whitespace-only tokens)
     */
    public List<LayoutToken> tokenize(String text) {
        List<LayoutToken> allTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        List<LayoutToken> filtered = new ArrayList<>();
        for (LayoutToken token : allTokens) {
            String txt = token.getText();
            // Filter out whitespace-only tokens
            if (txt != null && !txt.trim().isEmpty()) {
                filtered.add(token);
            }
        }
        return filtered;
    }

    /**
     * Convert tokens to character indices.
     * 
     * @param tokens    List of tokens
     * @param seqLength Padded sequence length
     * @return Character indices [seq_len][max_char_length]
     */
    public long[][] tokensToCharIndices(List<LayoutToken> tokens, int seqLength) {
        long[][] charIndices = new long[seqLength][maxCharLength];

        for (int i = 0; i < Math.min(tokens.size(), seqLength); i++) {
            String word = tokens.get(i).getText();
            for (int j = 0; j < Math.min(word.length(), maxCharLength); j++) {
                String ch = String.valueOf(word.charAt(j));
                charIndices[i][j] = charVocab.getOrDefault(ch, unkIndex);
            }
            // Rest is padded with 0 (default)
        }

        return charIndices;
    }

    /**
     * Convert tokens to string array.
     */
    public String[] tokensToStrings(List<LayoutToken> tokens) {
        String[] strings = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            strings[i] = tokens.get(i).getText();
        }
        return strings;
    }

    /**
     * Convert tag indices to tag names.
     */
    public String[] indicesToTags(int[] indices) {
        String[] tags = new String[indices.length];
        for (int i = 0; i < indices.length; i++) {
            tags[i] = tagIndex.getOrDefault(indices[i], "O");
        }
        return tags;
    }

    /**
     * Create mask for valid tokens.
     */
    public boolean[] createMask(int numTokens, int seqLength) {
        boolean[] mask = new boolean[seqLength];
        for (int i = 0; i < Math.min(numTokens, seqLength); i++) {
            mask[i] = true;
        }
        return mask;
    }

    public int getMaxCharLength() {
        return maxCharLength;
    }

    public Map<Integer, String> getTagIndex() {
        return tagIndex;
    }

    /**
     * Check if this preprocessor supports features.
     */
    public boolean hasFeatures() {
        return hasFeatures;
    }

    /**
     * Get the number of features expected per token.
     */
    public int getNumFeatures() {
        return featuresIndices != null ? featuresIndices.size() : 0;
    }

    /**
     * Get the feature indices (column positions in training data).
     */
    public List<Integer> getFeaturesIndices() {
        return featuresIndices;
    }

    /**
     * Convert feature strings to indices for ONNX model input.
     * 
     * Features are provided as a 2D array: [numTokens][numFeatures]
     * where each element is the feature value string (e.g., "ALLCAP", "LINESTART").
     * 
     * @param features  Feature strings per token [numTokens][numFeatures]
     * @param seqLength Padded sequence length
     * @return Feature indices [seqLength][numFeatures] ready for ONNX model
     */
    public long[][] tokensToFeatureIndices(String[][] features, int seqLength) {
        if (!hasFeatures) {
            return null;
        }

        int numFeatures = featuresIndices.size();
        long[][] result = new long[seqLength][numFeatures];

        int numTokens = Math.min(features.length, seqLength);

        for (int tokenIdx = 0; tokenIdx < numTokens; tokenIdx++) {
            for (int featIdx = 0; featIdx < numFeatures; featIdx++) {
                if (featIdx < features[tokenIdx].length) {
                    String featureValue = features[tokenIdx][featIdx];
                    Integer originalColumnIdx = featuresIndices.get(featIdx);

                    // Look up the index for this feature value
                    Map<String, Integer> valueMap = featuresMapToIndex.get(originalColumnIdx);
                    if (valueMap != null && featureValue != null) {
                        result[tokenIdx][featIdx] = valueMap.getOrDefault(featureValue, 0);
                    }
                    // Default is 0 (padding)
                }
            }
        }

        return result;
    }
}
