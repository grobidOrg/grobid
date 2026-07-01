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

import java.io.File;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.LanguageDetector;
import org.grobid.core.lang.LanguageDetectorFactory;
import org.grobid.core.utilities.GrobidProperties;

/**
 * Implementation of a language detector factory with Cybozu language identifier
 */
public class CybozuLanguageDetectorFactory implements LanguageDetectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CybozuLanguageDetectorFactory.class);
    private static volatile LanguageDetector instance = null;

    private static void init() {
        File profilePath = new File(GrobidProperties.getLanguageDetectionResourcePath(), "cybozu/profiles")
                .getAbsoluteFile();
        if (!profilePath.exists() || !profilePath.isDirectory()) {
            throw new IllegalStateException(
                    "Profiles path for cybozu language detection does not exist or not a directory: " + profilePath);
        }

        try {
            DetectorFactory.loadProfile(profilePath);
        } catch (LangDetectException e) {
            throw new IllegalStateException("Cannot read profiles for cybozu language detection from: " + profilePath,
                    e);
        }
    }

    public LanguageDetector getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    init();
                    LOGGER.debug("synchronized getNewInstance");
                    instance = new CybozuLanguageDetector();
                }
            }

        }
        return instance;
    }

}
