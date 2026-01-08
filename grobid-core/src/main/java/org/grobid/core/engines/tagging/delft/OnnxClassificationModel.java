package org.grobid.core.engines.tagging.delft;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.engines.tagging.GenericClassifier;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ONNX-based text classification model.
 * 
 * Provides pure Java inference for classification models exported from DeLFT,
 * eliminating the need for JEP/Python at runtime.
 * 
 * Model directory structure:
 * - classifier.onnx : The ONNX model
 * - config.json : Model config (maxlen, wordEmbeddingSize, embeddingsName)
 * - labels.json : Label mappings (labels array, indexToLabel)
 */
public class OnnxClassificationModel implements GenericClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxClassificationModel.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final WordEmbeddings embeddings;
    private final String[] labels;
    private final Map<String, Integer> labelToIndex;
    private final int maxlen;
    private final int embeddingSize;
    private final String modelName;

    /**
     * Load an ONNX classification model from a directory.
     * 
     * @param modelDir Directory containing classifier.onnx, config.json,
     *                 labels.json
     */
    public OnnxClassificationModel(Path modelDir) throws IOException, OrtException {
        Gson gson = new Gson();

        // Read config.json
        Path configPath = modelDir.resolve("config.json");
        JsonObject config;
        try (FileReader reader = new FileReader(configPath.toFile())) {
            config = gson.fromJson(reader, JsonObject.class);
        }

        this.modelName = config.get("modelName").getAsString();
        this.maxlen = config.get("maxlen").getAsInt();
        this.embeddingSize = config.get("wordEmbeddingSize").getAsInt();
        String embeddingsName = config.get("embeddingsName").getAsString();

        // Read labels.json
        Path labelsPath = modelDir.resolve("labels.json");
        JsonObject labelsJson;
        try (FileReader reader = new FileReader(labelsPath.toFile())) {
            labelsJson = gson.fromJson(reader, JsonObject.class);
        }

        JsonArray labelsArray = labelsJson.getAsJsonArray("labels");
        this.labels = new String[labelsArray.size()];
        for (int i = 0; i < labelsArray.size(); i++) {
            this.labels[i] = labelsArray.get(i).getAsString();
        }

        JsonObject labelToIndexJson = labelsJson.getAsJsonObject("labelToIndex");
        this.labelToIndex = new HashMap<>();
        for (String label : labelToIndexJson.keySet()) {
            this.labelToIndex.put(label, labelToIndexJson.get(label).getAsInt());
        }

        // Load embeddings from DeLFT path
        String delftPath = GrobidProperties.getDeLFTFilePath();
        Path embeddingsPath = Path.of(delftPath, "data", "db", embeddingsName);

        LOGGER.info("Loading ONNX classification model from: {}", modelDir);
        LOGGER.info("Loading embeddings from: {}", embeddingsPath);

        // Load ONNX model
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        this.session = env.createSession(modelDir.resolve("classifier.onnx").toString(), options);

        // Load embeddings
        this.embeddings = new WordEmbeddings(embeddingsPath, embeddingSize);

        LOGGER.info("ONNX classification model {} loaded", modelName);
        LOGGER.info("Labels: {}", String.join(", ", labels));
    }

    /**
     * Classify texts and return results in DeLFT JSON format.
     * 
     * @param texts List of texts to classify
     * @return JSON string matching DeLFT classifier output format
     */
    @Override
    public String classify(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return null;
        }

        int batchSize = texts.size();
        float[][][] batchEmbeddings = new float[batchSize][maxlen][embeddingSize];

        // Process each text
        for (int b = 0; b < batchSize; b++) {
            String text = texts.get(b);

            // Tokenize using GrobidAnalyzer (same as sequence labeling)
            List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
            List<String> words = new ArrayList<>();
            for (LayoutToken token : tokens) {
                String txt = token.getText();
                if (txt != null && !txt.trim().isEmpty()) {
                    words.add(txt);
                }
            }

            // Get embeddings for words
            String[] wordArray = words.toArray(new String[0]);
            float[][] wordEmbs = embeddings.getEmbeddings(wordArray);

            // Copy to batch (pad/truncate to maxlen)
            int numTokens = Math.min(wordEmbs.length, maxlen);
            for (int i = 0; i < numTokens; i++) {
                batchEmbeddings[b][i] = wordEmbs[i];
            }
            // Rest is zero-padded (default float array initialization)
        }

        // Run inference
        float[][] predictions = runInference(batchEmbeddings);

        // Format as DeLFT JSON
        return formatAsJson(texts, predictions);
    }

    /**
     * Run ONNX inference.
     * 
     * @param embeddings Input embeddings [batch, maxlen, embeddingSize]
     * @return Predictions [batch, numClasses]
     */
    private float[][] runInference(float[][][] embeddingsInput) throws OrtException {
        int batchSize = embeddingsInput.length;

        // Flatten embeddings for ONNX tensor
        float[] flat = new float[batchSize * maxlen * embeddingSize];
        int idx = 0;
        for (int b = 0; b < batchSize; b++) {
            for (int s = 0; s < maxlen; s++) {
                for (int e = 0; e < embeddingSize; e++) {
                    flat[idx++] = embeddingsInput[b][s][e];
                }
            }
        }

        // Create input tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(flat),
                new long[] { batchSize, maxlen, embeddingSize });

        Map<String, OnnxTensor> inputs = new HashMap<>();
        // Common input names for classification models
        String inputName = session.getInputNames().iterator().next();
        inputs.put(inputName, inputTensor);

        try (OrtSession.Result result = session.run(inputs)) {
            // Get output - should be [batch, numClasses] (raw logits)
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            long[] shape = outputTensor.getInfo().getShape();
            int numClasses = (int) shape[1];

            float[] outputFlat = outputTensor.getFloatBuffer().array();

            // Reshape to 2D
            float[][] logits = new float[batchSize][numClasses];
            idx = 0;
            for (int b = 0; b < batchSize; b++) {
                for (int c = 0; c < numClasses; c++) {
                    logits[b][c] = outputFlat[idx++];
                }
            }

            // Apply softmax to convert logits to probabilities
            float[][] predictions = new float[batchSize][numClasses];
            for (int b = 0; b < batchSize; b++) {
                predictions[b] = softmax(logits[b]);
            }

            return predictions;
        } finally {
            inputTensor.close();
        }
    }

    /**
     * Apply softmax activation to convert logits to probabilities.
     * For single-label classification (copyright, license).
     */
    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) {
            if (v > max)
                max = v;
        }

        float sum = 0.0f;
        float[] exp = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exp[i] = (float) Math.exp(logits[i] - max); // subtract max for numerical stability
            sum += exp[i];
        }

        float[] probs = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            probs[i] = exp[i] / sum;
        }
        return probs;
    }

    /**
     * Format predictions as DeLFT-compatible JSON.
     */
    private String formatAsJson(List<String> texts, float[][] predictions) {
        JsonObject root = new JsonObject();

        JsonArray classifications = new JsonArray();
        for (int i = 0; i < texts.size(); i++) {
            JsonObject classificationEntry = new JsonObject();
            classificationEntry.addProperty("text", texts.get(i));

            // Add probability for each label
            for (int j = 0; j < labels.length; j++) {
                classificationEntry.addProperty(labels[j], predictions[i][j]);
            }

            classifications.add(classificationEntry);
        }

        root.add("classifications", classifications);
        root.addProperty("model", modelName);
        root.addProperty("software", "DeLFT");
        root.addProperty("date", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Gson gson = new Gson();
        return gson.toJson(root);
    }

    @Override
    public void close() throws IOException {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing ONNX session", e);
        }
        if (embeddings != null) {
            embeddings.close();
        }
    }
}
