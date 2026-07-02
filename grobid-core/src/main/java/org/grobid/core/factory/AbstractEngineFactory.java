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
package org.grobid.core.factory;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import org.grobid.core.engines.Engine;
import org.grobid.core.engines.ModelMap;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.main.LibraryLoader;
import org.grobid.core.utilities.GrobidProperties;

/**
 *
 * Abstract factory to get engine instance.
 *
 */
public class AbstractEngineFactory {

    /**
     * The engine.
     */
    private static Engine engine;

    /**
     * Return a new instance of engine if it doesn't exist, the existing
     * instance else.
     *
     * @return Engine
     */
    protected synchronized Engine getEngine() {
        return getEngine(false);
    }

    /**
     * Return a new instance of engine if it doesn't exist, the existing
     * instance else.
     *
     * @return Engine
     */
    protected synchronized Engine getEngine(boolean preload) {
        if (engine == null) {
            engine = createEngine(preload);
        }
        return engine;
    }

    /**
     * Return a new instance of engine.
     *
     * @return Engine
     */
    protected Engine createEngine() {
        return createEngine(false);
    }

    /**
     * Return a new instance of engine.
     *
     * @return Engine
     */
    protected Engine createEngine(boolean preload) {
        return new Engine(preload);
    }

    /**
     * Initializes all necessary things for starting grobid
     */
    public static void init() {
        GrobidProperties.getInstance();
        LibraryLoader.load();
        Lexicon.getInstance();
    }

    /**
     * Initializes all the models
     */
    @Deprecated
    public static void fullInit() {
        init();
        Set<GrobidCRFEngine> distinctModels = GrobidProperties.getDistinctModels();
        if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.CRFPP))) {
            ModelMap.initModels();
        }
        //Lexicon.getInstance();
    }
}
