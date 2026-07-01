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
package org.grobid.core.lang.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;

/**
 * Implementation of a sentence segmenter factory with OpenNLP language identifier
 */
public class PragmaticSentenceDetectorFactory implements SentenceDetectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(PragmaticSentenceDetectorFactory.class);
    private static volatile SentenceDetector instance = null;

    public SentenceDetector getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    LOGGER.debug("synchronized getNewInstance");
                    instance = new PragmaticSentenceDetector();
                }
            }
        }
        return instance;
    }
}
