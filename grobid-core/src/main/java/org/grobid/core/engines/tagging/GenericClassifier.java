package org.grobid.core.engines.tagging;

import java.io.Closeable;
import java.util.List;

/**
 * Common interface for text classification models.
 * Supports both JEP-based DeLFT classifiers and ONNX-based classifiers.
 */
public interface GenericClassifier extends Closeable {

    /**
     * Classify texts in batch.
     * 
     * @param texts List of texts to classify
     * @return JSON string with classification results in DeLFT format
     */
    String classify(List<String> texts) throws Exception;
}
