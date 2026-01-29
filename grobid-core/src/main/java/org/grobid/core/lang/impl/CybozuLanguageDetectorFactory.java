package org.grobid.core.lang.impl;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import org.grobid.core.lang.LanguageDetector;
import org.grobid.core.lang.LanguageDetectorFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

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
