package org.grobid.core.utilities;

import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.factory.AbstractEngineFactory;

/**
 * Test utilities to make Grobid tests independent of the active grobid.yaml.
 *
 * A number of unit/integration tests assume Wapiti-style CRF models.
 * When grobid.yaml is configured to use DeLFT or ONNX for some models
 * (e.g. header, reference-segmenter, citation), these tests can fail.
 */
public class TestEngineUtils {

    private static volatile boolean wapitiForced = false;

    private TestEngineUtils() {
        // utility class
    }

    /**
     * Initialize Grobid and force all configured model engines to Wapiti.
     *
     * Should be called in {@code @BeforeClass} before any Engine/Parser is created.
     */
    public static void initGrobidForceWapiti() {
        // Ensure config/modelMap is loaded
        GrobidProperties.getInstance();

        if (!wapitiForced) {
            synchronized (TestEngineUtils.class) {
                if (!wapitiForced) {
                    forceAllModelsToWapiti();
                    wapitiForced = true;
                }
            }
        }

        // Continue with the normal initialization path
        AbstractEngineFactory.init();
    }

    private static void forceAllModelsToWapiti() {
        // Iterate over all model names from the configuration (not just enum values)
        // to ensure all models including citation, header, reference-segmenter etc. are forced to wapiti
        for (String modelName : GrobidProperties.getModelNames()) {
            GrobidConfig.ModelParameters current = GrobidProperties.getGrobidModelParameters(modelName);
            if (current == null) {
                continue;
            }

            // Overwrite engine selection only
            GrobidConfig.ModelParameters override = new GrobidConfig.ModelParameters();
            override.name = current.name;
            override.engine = GrobidCRFEngine.WAPITI.name().toLowerCase();
            override.wapiti = current.wapiti;
            override.delft = current.delft;
            override.onnx = current.onnx;

            GrobidProperties.addModel(override);
        }
    }
}

