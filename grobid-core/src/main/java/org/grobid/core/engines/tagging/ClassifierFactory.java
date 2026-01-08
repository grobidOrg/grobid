package org.grobid.core.engines.tagging;

import org.grobid.core.engines.tagging.delft.OnnxClassificationModel;
import org.grobid.core.jni.DeLFTClassifierModel;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for text classification models.
 * 
 * Supports both JEP-based DeLFT classifiers and ONNX-based classifiers.
 * The engine is selected based on grobid.yaml configuration.
 */
public class ClassifierFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassifierFactory.class);

    private static Map<String, GenericClassifier> cache = new HashMap<>();

    private ClassifierFactory() {
    }

    /**
     * Get a classifier for the given model name.
     * 
     * @param modelName The model name (e.g., "copyright", "license")
     * @return A GenericClassifier instance
     */
    public static synchronized GenericClassifier getClassifier(String modelName) {
        return getClassifier(modelName, GrobidProperties.getDelftArchitecture(modelName));
    }

    /**
     * Get a classifier for the given model name and architecture.
     * 
     * @param modelName    The model name (e.g., "copyright", "license")
     * @param architecture The DeLFT architecture (e.g., "gru")
     * @return A GenericClassifier instance
     */
    public static synchronized GenericClassifier getClassifier(String modelName, String architecture) {
        GenericClassifier classifier = cache.get(modelName);
        if (classifier == null) {
            GrobidCRFEngine engine = GrobidProperties.getGrobidEngine(modelName);

            if (engine == null) {
                throw new IllegalStateException("No engine configured for model: " + modelName);
            }

            switch (engine) {
                case DELFT:
                    LOGGER.info("Creating DeLFT classifier for model: {}", modelName);
                    classifier = new DeLFTClassifierModel(modelName, architecture);
                    break;
                case ONNX:
                    LOGGER.info("Creating ONNX classifier for model: {}", modelName);
                    try {
                        // Model path: {grobid-home}/models/{modelName}-{architecture}.onnx/
                        File modelPath = GrobidProperties.getModelPath();
                        Path modelDir = modelPath.toPath().resolve(modelName + "-" + architecture + ".onnx");
                        classifier = new OnnxClassificationModel(modelDir);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load ONNX classification model: " + modelName, e);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unsupported engine for classification: " + engine);
            }
            cache.put(modelName, classifier);
        }
        return classifier;
    }

    /**
     * Clear the classifier cache.
     */
    public static synchronized void clearCache() {
        for (GenericClassifier classifier : cache.values()) {
            try {
                classifier.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing classifier", e);
            }
        }
        cache.clear();
    }
}
