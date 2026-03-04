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
                if(instance == null) {
                    init();
                    instance = new LinguaLanguageDetector();
                }
            }

        }
        return instance;
    }

}
