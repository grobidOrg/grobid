package org.grobid.core.engines.tagging;

import com.google.common.base.Joiner;
import org.grobid.core.GrobidModel;
import org.grobid.core.engines.tagging.delft.DeLFTOnnxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * ONNX-based tagger for sequence labeling.
 * Uses DeLFT models exported to ONNX format with CRF decoding in pure Java.
 *
 * This tagger provides a pure Java alternative to the DeLFT tagger, which
 * requires
 * Python/JEP at runtime. ONNX models are loaded from directories with the
 * naming
 * convention: {model-name}-{architecture}.onnx
 */
public class OnnxTagger implements GenericTagger {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnnxTagger.class);

    private final DeLFTOnnxModel model;
    private final GrobidModel grobidModel;

    /**
     * Create an ONNX tagger for the given model.
     *
     * @param grobidModel  The GROBID model to use
     * @param architecture The DeLFT architecture (e.g., "BidLSTM_CRF_FEATURES") -
     *                     not used, included for API compatibility
     */
    public OnnxTagger(GrobidModel grobidModel, String architecture) {
        this.grobidModel = grobidModel;

        // Model path is now correctly resolved by GrobidProperties.getModelPath() for
        // ONNX engine
        File modelDir = new File(grobidModel.getModelPath());

        LOGGER.info("Loading ONNX model from: {}", modelDir);

        try {
            this.model = new DeLFTOnnxModel(modelDir.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX model: " + modelDir, e);
        }
    }

    @Override
    public String label(Iterable<String> data) {
        return label(Joiner.on('\n').join(data));
    }

    @Override
    public String label(String data) {
        return model.labelGrobidInput(data);
    }

    @Override
    public void close() throws IOException {
        if (model != null) {
            model.close();
        }
    }
}
