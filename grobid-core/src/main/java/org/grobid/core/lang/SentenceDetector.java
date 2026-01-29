package org.grobid.core.lang;

import org.grobid.core.utilities.OffsetPosition;

import java.util.List;

/**
 * Interface for sentence recognition method/library
 */
public interface SentenceDetector {
    /**
     * Detects sentence boundaries
     * @param text
     * text to detect sentence boundaries
     * @return a list of offset positions indicating start and end character position of the recognized sentence in the
     * text
     */
    public List<OffsetPosition> detect(String text);

    /**
     * Detects sentence boundaries using a specified language
     * @param text
     * text to detect sentence boundaries
     * @param lang
     * language to be used for detecting sentence boundaries
     * @return a list of offset positions indicating start and end character position of the recognized sentence in the
     * text
     */
    public List<OffsetPosition> detect(String text, Language lang);
}
