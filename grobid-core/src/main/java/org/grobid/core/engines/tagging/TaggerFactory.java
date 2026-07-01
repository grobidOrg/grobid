/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.engines.tagging;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.utilities.GrobidProperties;

/**
 * Factory for a sequence labelling, aka a tagger, instance.
 * Supported implementations are CRF (CRFPP, Wapiti) and Deep Learning (DeLFT)
 *
 */
public class TaggerFactory {
    public static final Logger LOGGER = LoggerFactory.getLogger(TaggerFactory.class);

    private static Map<GrobidModel, GenericTagger> cache = new HashMap<>();
    private static Map<String, String> failedModels = new LinkedHashMap<>();

    private TaggerFactory() {
    }

    public static synchronized GenericTagger getTagger(GrobidModel model) {
        return getTagger(model, GrobidProperties.getGrobidEngine(model), GrobidProperties.getDelftArchitecture(model));
    }

    public static synchronized GenericTagger getTagger(GrobidModel model, GrobidCRFEngine engine) {
        return getTagger(model, engine, GrobidProperties.getDelftArchitecture(model));
    }

    public static synchronized GenericTagger getTagger(GrobidModel model, GrobidCRFEngine engine, String architecture) {
        GenericTagger t = cache.get(model);
        if (t == null) {
            if (model.equals(GrobidModels.DUMMY)) {
                return new DummyTagger(model);
            }

            if (engine != null) {
                try {
                    switch (engine) {
                        case CRFPP :
                            t = new CRFPPTagger(model);
                            break;
                        case WAPITI :
                            t = new WapitiTagger(model);
                            break;
                        case DELFT :
                            t = new DeLFTTagger(model, architecture);
                            break;
                        default :
                            throw new IllegalStateException(
                                    "Unsupported Grobid sequence labelling engine: " + engine.getExt());
                    }
                    cache.put(model, t);
                } catch (Exception e) {
                    String modelName = model.getModelName();
                    failedModels.put(modelName, e.getMessage() != null ? e.getMessage() : e.getClass().getName());
                    LOGGER.error("Failed to create tagger for model " + modelName + " with engine " + engine, e);
                    throw e;
                }
            } else {
                throw new IllegalStateException(
                        "Unsupported or null Grobid sequence labelling engine: " + engine.getExt());
            }
        }
        return t;
    }

    /**
     * Create a tagger loading from an explicit file path. Not cached.
     * Currently only supported for the Wapiti engine.
     */
    public static GenericTagger getTaggerFromPath(File modelFile, GrobidCRFEngine engine) {
        if (engine == GrobidCRFEngine.WAPITI)
            return new WapitiTagger(modelFile);
        throw new IllegalArgumentException(
                "Custom model path is only supported for Wapiti engine, got: " + engine);
    }

    /**
     * Returns a map of successfully loaded models and their engine types.
     */
    public static synchronized Map<String, String> getLoadedModels() {
        Map<String, String> loaded = new LinkedHashMap<>();
        for (Map.Entry<GrobidModel, GenericTagger> entry : cache.entrySet()) {
            String engineType;
            GenericTagger tagger = entry.getValue();
            if (tagger instanceof WapitiTagger) {
                engineType = "wapiti";
            } else if (tagger instanceof DeLFTTagger) {
                engineType = "delft";
            } else if (tagger instanceof CRFPPTagger) {
                engineType = "crfpp";
            } else {
                engineType = "unknown";
            }
            loaded.put(entry.getKey().getModelName(), engineType);
        }
        return loaded;
    }

    /**
     * Returns a map of models that failed to load and their error messages.
     */
    public static synchronized Map<String, String> getFailedModels() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(failedModels));
    }

    /**
     * Returns true if any model failed to load.
     */
    public static synchronized boolean hasFailures() {
        return !failedModels.isEmpty();
    }
}
