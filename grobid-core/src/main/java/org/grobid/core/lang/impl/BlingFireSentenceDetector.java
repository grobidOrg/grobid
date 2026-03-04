package org.grobid.core.lang.impl;

import com.github.lfoppiano.blingfire.BlingFire;

import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.GrobidProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of sentence segmentation via Microsoft BlingFire.
 * BlingFire's sbd.bin model is language-agnostic.
 */
public class BlingFireSentenceDetector implements SentenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlingFireSentenceDetector.class);

    private final BlingFire.Model model;

    public BlingFireSentenceDetector() {
        this(GrobidProperties.getGrobidHomePath() + File.separator
            + "sentence-segmentation" + File.separator + "blingfire" + File.separator + "sbd.bin");
    }

    BlingFireSentenceDetector(String modelPath) {
        LOGGER.info("Loading BlingFire sentence segmentation model from: " + modelPath);
        model = new BlingFire.Model(modelPath);
    }

    @Override
    public List<OffsetPosition> detect(String text) {
        return detect(text, new Language(Language.EN));
    }

    @Override
    public List<OffsetPosition> detect(String text, Language lang) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<BlingFire.TokenWithOffset> sentencesWithOffsets = model.textToSentencesWithOffsets(text);
        List<OffsetPosition> result = new ArrayList<>();

        for (BlingFire.TokenWithOffset token : sentencesWithOffsets) {
            // BlingFire returns inclusive end offset, convert to exclusive to match OffsetPosition convention
            int end = Math.min(token.getEndOffset() + 1, text.length());
            result.add(new OffsetPosition(token.getStartOffset(), end));
        }

        return result;
    }
}
