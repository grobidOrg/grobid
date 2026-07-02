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

import org.grobid.core.lang.LanguageDetector;
import org.grobid.core.lang.LanguageDetectorFactory;

/**
 * Implementation of a language detector factory with Lingua language identifier
 */
public class LinguaLanguageDetectorFactory implements LanguageDetectorFactory {
    private static volatile LanguageDetector instance = null;

    private static void init() {

    }

    public LanguageDetector getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    init();
                    instance = new LinguaLanguageDetector();
                }
            }

        }
        return instance;
    }

}
