package org.grobid.core.engines.tagging.delft;

import ai.onnxruntime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ONNX Runtime wrapper for running DeLFT encoder models.
 */
public class OnnxModelRunner implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxModelRunner.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final boolean hasFeatures;

    /**
     * Load an ONNX model.
     * 
     * @param modelPath Path to the .onnx file
     */
    public OnnxModelRunner(Path modelPath) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

        this.session = env.createSession(modelPath.toString(), options);

        // Check if model has features input
        this.hasFeatures = session.getInputNames().contains("features_input");

        LOGGER.info("Loaded ONNX model from {}", modelPath);
        LOGGER.info("Input names: {}", session.getInputNames());
        LOGGER.info("Output names: {}", session.getOutputNames());
    }

    /**
     * Run inference to get emission scores.
     * 
     * @param wordEmbeddings Word embeddings [batch, seq_len, embed_size]
     * @param charIndices    Character indices [batch, seq_len, max_char]
     * @return Emission scores [batch, seq_len, num_tags]
     */
    public float[][][] runInference(float[][][] wordEmbeddings, long[][][] charIndices) throws OrtException {
        return runInference(wordEmbeddings, charIndices, null);
    }

    /**
     * Run inference with optional features.
     * 
     * @param wordEmbeddings Word embeddings [batch, seq_len, embed_size]
     * @param charIndices    Character indices [batch, seq_len, max_char]
     * @param featureIndices Optional feature indices [batch, seq_len, num_features]
     * @return Emission scores [batch, seq_len, num_tags]
     */
    public float[][][] runInference(
            float[][][] wordEmbeddings,
            long[][][] charIndices,
            long[][][] featureIndices) throws OrtException {

        int batchSize = wordEmbeddings.length;
        int seqLen = wordEmbeddings[0].length;
        int embedSize = wordEmbeddings[0][0].length;
        int maxChar = charIndices[0][0].length;

        // Create input tensors
        Map<String, OnnxTensor> inputs = new HashMap<>();

        // Word embeddings tensor
        float[] wordFlat = flatten3D(wordEmbeddings);
        OnnxTensor wordTensor = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(wordFlat),
                new long[] { batchSize, seqLen, embedSize });
        inputs.put("word_input", wordTensor);

        // Char indices tensor
        long[] charFlat = flatten3DLong(charIndices);
        OnnxTensor charTensor = OnnxTensor.createTensor(env,
                LongBuffer.wrap(charFlat),
                new long[] { batchSize, seqLen, maxChar });
        inputs.put("char_input", charTensor);

        // Features tensor (if model supports and provided)
        if (hasFeatures && featureIndices != null) {
            int numFeatures = featureIndices[0][0].length;
            long[] featFlat = flatten3DLong(featureIndices);
            OnnxTensor featTensor = OnnxTensor.createTensor(env,
                    LongBuffer.wrap(featFlat),
                    new long[] { batchSize, seqLen, numFeatures });
            inputs.put("features_input", featTensor);
        }

        // Run inference
        try (OrtSession.Result result = session.run(inputs)) {
            // Get output tensor
            OnnxTensor outputTensor = (OnnxTensor) result.get("emissions").get();

            // Get shape [batch, seq_len, num_tags]
            long[] shape = outputTensor.getInfo().getShape();
            int numTags = (int) shape[2];

            // Copy output to array
            float[] outputFlat = outputTensor.getFloatBuffer().array();

            // Reshape to 3D
            float[][][] emissions = new float[batchSize][seqLen][numTags];
            int idx = 0;
            for (int b = 0; b < batchSize; b++) {
                for (int s = 0; s < seqLen; s++) {
                    for (int t = 0; t < numTags; t++) {
                        emissions[b][s][t] = outputFlat[idx++];
                    }
                }
            }

            return emissions;
        } finally {
            // Clean up input tensors
            for (OnnxTensor tensor : inputs.values()) {
                tensor.close();
            }
        }
    }

    /**
     * Try to infer a fixed maximum sequence length from the ONNX model input tensor
     * shape.
     * <p>
     * This only works when the exported model declares a static seq_len dimension
     * (e.g. [batch, 3000, embed]). If the model uses a dynamic dimension (often
     * reported as -1 / symbolic), this method returns -1.
     */
    public int inferMaxSequenceLength() {
        try {
            NodeInfo inputInfo = session.getInputInfo().get("word_input");
            if (inputInfo == null) {
                return -1;
            }
            TensorInfo tensorInfo = (TensorInfo) inputInfo.getInfo();
            long[] shape = tensorInfo.getShape();
            if (shape == null || shape.length < 2) {
                return -1;
            }
            long seqLen = shape[1];
            if (seqLen <= 0) {
                return -1;
            }
            if (seqLen > Integer.MAX_VALUE) {
                return -1;
            }
            return (int) seqLen;
        } catch (Exception e) {
            // Be conservative: inference should never fail because of this helper.
            return -1;
        }
    }

    private float[] flatten3D(float[][][] arr) {
        int d1 = arr.length;
        int d2 = arr[0].length;
        int d3 = arr[0][0].length;
        float[] flat = new float[d1 * d2 * d3];
        int idx = 0;
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d2; j++) {
                for (int k = 0; k < d3; k++) {
                    flat[idx++] = arr[i][j][k];
                }
            }
        }
        return flat;
    }

    private long[] flatten3DLong(long[][][] arr) {
        int d1 = arr.length;
        int d2 = arr[0].length;
        int d3 = arr[0][0].length;
        long[] flat = new long[d1 * d2 * d3];
        int idx = 0;
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d2; j++) {
                for (int k = 0; k < d3; k++) {
                    flat[idx++] = arr[i][j][k];
                }
            }
        }
        return flat;
    }

    public boolean hasFeatures() {
        return hasFeatures;
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.warn("Error closing ONNX session", e);
        }
    }
}
