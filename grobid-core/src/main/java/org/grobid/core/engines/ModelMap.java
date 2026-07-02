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
package org.grobid.core.engines;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.chasen.crfpp.Model;
import org.chasen.crfpp.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;

/**
 * Class that creates a tagger from a given model or reuse it if it already exists.
 *
 */
@Deprecated
public class ModelMap {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelMap.class);

    /**
     * Map that contains all the models loaded in memory.
     */
    private static Map<String, Model> models = null;

    /**
     * Return a model tagger created corresponding to the model given in argument.
     *
     * @param grobidModel
     *            the model to use for the creation of the tagger.
     * @return Tagger
     *
     */
    @Deprecated
    public static Tagger getTagger(GrobidModel grobidModel) {
        LOGGER.debug("start getTagger");
        Tagger tagger;
        try {
            LOGGER.debug("Creating tagger");
            Model model = getModel(grobidModel.getModelPath());
            tagger = model.createTagger();
        } catch (Throwable thb) {
            throw new GrobidException("Cannot instantiate a tagger", thb);
        }
        LOGGER.debug("end getTagger");
        return tagger;
    }

    /**
     * Loading of the models.
     */
    @Deprecated
    public static synchronized void initModels() {
        LOGGER.info("Loading models");
        GrobidModels[] models = GrobidModels.values();
        for (GrobidModels model : models) {
            if (new File(model.getModelPath()).exists()) {
                getModel(model.getModelPath());
            } else {
                LOGGER.info("Loading model " + model.getModelPath() + " failed because the path is not valid.");
            }
        }
        LOGGER.info("Models loaded");
    }

    public static Model getModel(GrobidModel grobidModel) {
        return getModel(grobidModel.getModelPath());
    }

    /**
     * Return the model corresponding to the given path. Models are loaded in
     * memory if they don't exist.
     *
     * @param modelPath
     *            the path to the model
     * @return the model corresponding to the given path.
     */
    @Deprecated
    protected static Model getModel(String modelPath) {
        LOGGER.debug("start getModel");
        if (models == null) {
            models = new HashMap<String, Model>();
        }
        if (models.get(modelPath) == null) {
            getNewModel(modelPath);
        }
        LOGGER.debug("end getModel");
        return models.get(modelPath);
    }

    /**
     * Set models with a new model.
     *
     * @param modelPath
     *            The path of the model to use.
     */
    @Deprecated
    protected static synchronized void getNewModel(String modelPath) {
        LOGGER.info("Loading model " + modelPath + " in memory");
        models.put(modelPath, new Model("-m " + modelPath + " "));
    }
}
