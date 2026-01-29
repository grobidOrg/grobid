package org.grobid.core.lang;

/**
 * Interface for language recognition method/library
 */
public interface LanguageDetector {
    /**
     * Detects a language id that must consist of two letter together with a confidence coefficient. If coefficient
     * cannot be provided for some reason, it should be 1.0
     * @param text
     * text to detect a language from
     * @return a language id together with a confidence coefficient
     */
    public Language detect(String text);
}
