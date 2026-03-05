package org.grobid.core.lang.impl;

import com.github.lfoppiano.blingfire.BlingFire;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.OffsetPosition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of sentence segmentation via Microsoft BlingFire.
 * Uses the built-in default model (no external sbd.bin file required).
 * BlingFire's sentence segmentation is language-agnostic.
 */
public class BlingFireSentenceDetector implements SentenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlingFireSentenceDetector.class);

    @Override
    public List<OffsetPosition> detect(String text) {
        return detect(text, new Language(Language.EN));
    }

    @Override
    public List<OffsetPosition> detect(String text, Language lang) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        String[] sentences = BlingFire.textToSentences(text);
        List<OffsetPosition> result = new ArrayList<>();
        int pos = 0;

        for (String sentence : sentences) {
            // BlingFire's native TextToSentences includes a trailing null byte in output;
            // the last sentence may contain a \0 character that prevents matching
            if (!sentence.isEmpty() && sentence.charAt(sentence.length() - 1) == '\0') {
                sentence = sentence.substring(0, sentence.length() - 1);
            }
            if (sentence.isEmpty()) {
                continue;
            }
            int start = text.indexOf(sentence, pos);
            if (start == -1) {
                LOGGER.warn("Extracted sentence does not match original text - " + sentence);
                start = pos;
            }
            int end = start + sentence.length();
            if (end > text.length()) {
                end = text.length();
            }
            result.add(new OffsetPosition(start, end));
            pos = end;
        }

        return result;
    }
}
