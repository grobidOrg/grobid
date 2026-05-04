package org.grobid.core.lang.impl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.lang.Language;
import org.grobid.core.lang.SentenceDetector;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.OffsetPosition;

/**
 * Implementation of sentence segmentation via the Pragmatic Segmenter
 *
 */
public class PragmaticSentenceDetector implements SentenceDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PragmaticSentenceDetector.class);

    private ScriptingContainer instance = null;

    public PragmaticSentenceDetector() {
        String segmenterRbFile = GrobidProperties.getGrobidHomePath()
                + File.separator
                + "sentence-segmentation"
                +
                File.separator
                + "pragmatic_segmenter"
                + File.separator
                + "segmenter.rb";
        String segmenterLoadPath = GrobidProperties.getGrobidHomePath() + File.separator + "sentence-segmentation";
        /*String unicodeLoadPath = GrobidProperties.getGrobidHomePath() + File.separator + "sentence-segmentation" +
            File.separator + "pragmatic_segmenter" + File.separator + "gem" + File.separator + "gems" +
            File.separator + "unicode-0.4.4.4-java" + File.separator + "lib";*/
        String unicodeLoadPath = GrobidProperties.getGrobidHomePath()
                + File.separator
                + "sentence-segmentation"
                +
                File.separator
                + "pragmatic_segmenter"
                + File.separator
                + "lib";
        //System.out.println(vendorLoadPath);

        List<String> loadPaths = new ArrayList();
        loadPaths.add(segmenterLoadPath);
        loadPaths.add(unicodeLoadPath);

        instance = new ScriptingContainer(LocalContextScope.CONCURRENT, LocalVariableBehavior.PERSISTENT);
        instance.setClassLoader(instance.getClass().getClassLoader());
        instance.setLoadPaths(loadPaths);
        instance.runScriptlet(PathType.ABSOLUTE, segmenterRbFile);
    }

    @Override
    public List<OffsetPosition> detect(String text) {
        return detect(text, new Language(Language.EN));
    }

    @Override
    public List<OffsetPosition> detect(String text, Language lang) {
        instance.put("text", text);
        String script = null;
        if (lang == null || "en".equals(lang.getLang()))
            script = "ps = PragmaticSegmenter::Segmenter.new(text: text, clean: false)\nps.segment";
        else
            script = "ps = PragmaticSegmenter::Segmenter.new(text: text, language: '"
                    + lang.getLang()
                    + "', clean: false)\nps.segment";
        Object ret = instance.runScriptlet(script);

        //System.out.println(text);
        //System.out.println(ret.toString());

        // build offset positions from the string chunks
        List<OffsetPosition> result = new ArrayList<>();
        int pos = 0;
        int previousEnd = 0;
        // indicate when the sentence as provided by the Pragmatic Segmented does not match the original string
        // and we had to "massage" the string to identify/approximate offsets in the original string
        boolean recovered = false;
        List<String> retList = (List<String>) ret;
        for (int i = 0; i < retList.size(); i++) {
            String chunk = retList.get(i);
            recovered = false;
            int start = text.indexOf(chunk, pos);
            if (start == -1) {
                LOGGER.warn("Extracted sentence does not match original text - " + chunk);

                // Unfortunately the pragmatic segmenter can modify the string when it gives back the array of sentences as string.
                // it usually concerns removed white space, which then make it hard to locate exactly the offsets.
                // we take as first fallback the previous end of sentence and move it to the next non space character
                // next heuristics is to use the next sentence matching to re-synchronize to the original text

                // note: the white space removal can be avoided by commenting out @language::ExtraWhiteSpaceRule:
                // see https://github.com/echan00/pragmatic_segmenter/commit/e5e4244bacd0bd12e65b560b648d331980fc1ce4
                // but it requires then a modified version of the tool (which is OK :)

                // but it can also be much more ugly/unmanageable when input is more noisy:
                // "The dissolved oxygen concentration in the sediment was measured in the lab with an OX-500 micro electrode (Unisense, Aarhus, Denmark) and was below detection limit (\0.01 mg l -1 )."
                // -> ["The dissolved oxygen concentration in the sediment was measured in the lab with an OX-500 micro electrode (Unisense, Aarhus, Denmark) and was below detection limit (((((((((\\0.01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 ).01 mg l -1 )."]
                // original full paragraph: Nonylphenol polluted sediment was collected in June 2005 from the Spanish Huerva River in Zaragoza (41°37 0 23 00 N, 0°54 0 28 00 W), which is a tributary of the Ebro River. At the moment of sampling, the river water had a temperature of 25.1°C, a redox potential of 525 mV and a pH of 7.82. The water contained 3.8 mg l -1 dissolved oxygen. The dissolved oxygen concentration in the sediment was measured in the lab with an OX-500 micro electrode (Unisense, Aarhus, Denmark) and was below detection limit (\0.01 mg l -1 ). The redox potential, temperature and pH were not determined in the sediment for practical reasons. Sediment was taken anaerobically with stainless steel cores, and transported on ice to the laboratory. Cores were opened in an anaerobic glove box with ±1% H 2 -gas and ±99% N 2 -gas to maintain anaerobic conditions, and the sediment was put in a glass jar. The glass jar was stored at 4°C in an anaerobic box that was flushed with N 2 -gas. The sediment contained a mixture of tNP isomers (20 mg kg -1 dry weight), but 4-n-NP was not present in the sediment. The chromatogram of the gas chromatography-mass spectrometry (GC-MS) of the mixture of tNP isomers present in the sediment was comparable to the chromatogram of the tNP technical mixture ordered from Merck. The individual branched isomers were not identified. The total organic carbon fraction of the sediment was 3.5% and contained mainly clay particles with a diameter size \ 32 lM.
                // it's less frequent that white space removal, but can happen hundred of times when processing thousand PDF
                // -> note it might be related to jruby sharing of the string and encoding/escaping

                if (previousEnd != pos) {
                    // previous sentence was "recovered", which means we are unsure about its end offset
                    start = text.indexOf(chunk, previousEnd);
                    if (start != -1) {
                        // apparently the current sentence match a bit before the end offset of the previous sentence, which mean that
                        // the previous sentence was modified by the segmenter and is longer than "real" (see example above).
                        // we need to correct the previous sentence end offset given the start of the current sentence
                        if (result.size() > 0) {
                            int newPreviousEnd = start;
                            while (newPreviousEnd >= 1 && text.charAt(newPreviousEnd - 1) == ' ') {
                                newPreviousEnd--;
                                if (start - newPreviousEnd > 10) {
                                    // this is a break to avoid going too far
                                    newPreviousEnd = start;
                                    // but look back previous character to cover general case
                                    if (newPreviousEnd >= 1 && text.charAt(newPreviousEnd - 1) == ' ') {
                                        newPreviousEnd--;
                                    }
                                }
                            }
                            result.get(result.size() - 1).end = newPreviousEnd;
                        }
                    }
                }

                // still no start, the provided sentence has been modified by the segmenter and it is really not matching the original string
                // we approximate the start of the non-matching sentence based on the end of the previous sentence
                if (start == -1) {
                    start = previousEnd;
                    while (text.charAt(start) == ' ') {
                        start++;
                        if (start - previousEnd > 10) {
                            // this is a break to avoid going too far
                            start = previousEnd + 1;
                        }
                    }
                    recovered = true;
                }
            }

            int end = start + chunk.length();

            // in case the last sentence is modified
            if (end > text.length() && i == retList.size() - 1)
                end = text.length();

            result.add(new OffsetPosition(start, end));
            pos = start + chunk.length();
            if (recovered)
                previousEnd += 1;
            else
                previousEnd = pos;
        }

        return result;
    }
}
