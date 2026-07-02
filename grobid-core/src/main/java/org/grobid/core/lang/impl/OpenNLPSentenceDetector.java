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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.SentenceUtilities;

/**
 * Implementation of sentence segmentation via OpenNLP
 *
 */
public class OpenNLPSentenceDetector implements SentenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNLPSentenceDetector.class);

    // components for sentence segmentation
    private SentenceModel model = null;

    public OpenNLPSentenceDetector() {
        // Loading sentence detector model
        String openNLPModelFile = GrobidProperties.getGrobidHomePath()
                +
                File.separator
                + "sentence-segmentation"
                + File.separator
                + "openNLP"
                + File.separator
                + "en-sent.bin";
        try (InputStream inputStream = new FileInputStream(openNLPModelFile)) {
            model = new SentenceModel(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Problem when loading the sentence segmenter", e);
        }
    }

    @Override
    public List<OffsetPosition> detect(String text) {
        return detect(text, new Language(Language.EN));
    }

    @Override
    public List<OffsetPosition> detect(String text, Language lang) {
        // unfortunately OpenNLP sentence detector is not thread safe, only the model can be share
        SentenceDetectorME detector = new SentenceDetectorME(model);
        Span spans[] = detector.sentPosDetect(text);
        List<OffsetPosition> result = new ArrayList<>();

        // convert Span to OffsetPosition
        for (int i = 0; i < spans.length; i++) {
            result.add(new OffsetPosition(spans[i].getStart(), spans[i].getEnd()));
        }

        // OpenNLP spans can carry trailing whitespace; normalise so no trailing space or empty span
        // is emitted (consistent with the other segmenters)
        return SentenceUtilities.trimAndFilterSentenceOffsets(text, result);
    }
}
