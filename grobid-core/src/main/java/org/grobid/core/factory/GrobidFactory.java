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

import org.grobid.core.engines.Engine;

/**
 * Factory to get engine instances.
 */
public class GrobidFactory extends AbstractEngineFactory {

    /**
     * The instance of GrobidFactory.
     */
    private static GrobidFactory factory = null;

    /**
     * Constructor.
     */
    protected GrobidFactory() {
        init();
    }

    /**
     * Return a new instance of GrobidFactory if it doesn't exist, the existing
     * instance else.
     *
     * @return GrobidFactory
     */
    public static GrobidFactory getInstance() {
        if (factory == null) {
            factory = newInstance();
        }
        return factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Engine getEngine() {
        return super.getEngine(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Engine getEngine(boolean preload) {
        return super.getEngine(preload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine createEngine() {
        return createEngine(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Engine createEngine(boolean preload) {
        return super.createEngine(preload);
    }

    /**
     * Creates a new instance of GrobidFactory.
     *
     * @return GrobidFactory
     */
    protected static GrobidFactory newInstance() {
        return new GrobidFactory();
    }

    /**
     * Resets this class and all its static fields. For instance sets the
     * current object to null.
     */
    public static void reset() {
        factory = null;
    }

}
