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

import java.util.ArrayList;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.lang.LanguageDetector;

public class CybozuLanguageDetector implements LanguageDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CybozuLanguageDetector.class);
    @Override
    public Language detect(String text) {
        Detector detector;
        try {
            detector = DetectorFactory.create();
            detector.append(text);
            ArrayList<com.cybozu.labs.langdetect.Language> probabilities = detector.getProbabilities();
            if (probabilities == null || probabilities.isEmpty()) {
                return null;
            }

            LOGGER.debug(probabilities.toString());
            com.cybozu.labs.langdetect.Language l = probabilities.get(0);

            return new Language(l.lang, l.prob);
        } catch (LangDetectException e) {
            LOGGER.warn("Cannot detect language because of: " + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }

    }
}
