package org.grobid.core.lang.impl;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import org.grobid.core.lang.Language;
import org.grobid.core.lang.LanguageDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

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
