package org.grobid.core.lang.impl;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.SentenceDetectorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a sentence segmenter factory with BlingFire
 */
public class BlingFireSentenceDetectorFactory implements SentenceDetectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlingFireSentenceDetectorFactory.class);
    private static volatile SentenceDetector instance = null;

    public SentenceDetector getInstance() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    LOGGER.debug("synchronized getNewInstance");
                    instance = new BlingFireSentenceDetector();
                }
            }
        }
        return instance;
    }
}
