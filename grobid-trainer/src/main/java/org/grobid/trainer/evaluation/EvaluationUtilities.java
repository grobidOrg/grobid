package org.grobid.trainer.evaluation;

import static org.grobid.core.engines.tagging.GenericTaggerUtils.getPlainLabel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.chasen.crfpp.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.exceptions.GrobidException;

/**
 * Generic evaluation of a single-CRF model processing given an expected result.
 *
 * @author Patrice Lopez
 */
public class EvaluationUtilities {
    protected static final Logger logger = LoggerFactory.getLogger(EvaluationUtilities.class);

    /**
     * Method for running a CRF tagger for evaluation purpose (i.e. with
     * expected and actual labels).
     *
     * @param ress   list
     * @param tagger a tagger
     * @return a report
     */
    public static String taggerRun(List<String> ress, Tagger tagger) {
        // clear internal context
        tagger.clear();
        StringBuilder res = new StringBuilder();

        // we have to re-inject the pre-tags because they are removed by the JNI
        // parse method
        ArrayList<String> pretags = new ArrayList<>();
        // add context
        for (String piece : ress) {
            if (piece.trim().length() == 0) {
                // parse and change internal stated as 'parsed'
                if (!tagger.parse()) {
                    // throw an exception
                    throw new RuntimeException("CRF++ parsing failed.");
                }

                for (int i = 0; i < tagger.size(); i++) {
                    for (int j = 0; j < tagger.xsize(); j++) {
                        res.append(tagger.x(i, j)).append("\t");
                    }
                    res.append(pretags.get(i)).append("\t");
                    res.append(tagger.y2(i));
                    res.append("\n");
                }
                res.append(" \n");
                // clear internal context
                tagger.clear();
                pretags = new ArrayList<>();
            } else {
                tagger.add(piece);
                tagger.add("\n");
                // get last tag
                StringTokenizer tokenizer = new StringTokenizer(piece, " \t");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (!tokenizer.hasMoreTokens()) {
                        pretags.add(token);
                    }
                }
            }
        }

        // parse and change internal stated as 'parsed'
        if (!tagger.parse()) {
            // throw an exception
            throw new RuntimeException("CRF++ parsing failed.");
        }

        for (int i = 0; i < tagger.size(); i++) {
            for (int j = 0; j < tagger.xsize(); j++) {
                res.append(tagger.x(i, j)).append("\t");
            }
            res.append(pretags.get(i)).append("\t");
            res.append(tagger.y2(i));
            res.append(System.lineSeparator());
        }
        res.append(System.lineSeparator());

        return res.toString();
    }

    public static ModelStats evaluateStandard(String path, final GenericTagger tagger) {
        return evaluateStandard(path, tagger::label);
    }

    public static ModelStats evaluateStandard(String path, Function<List<String>, String> taggerFunction) {
        String theResult = null;

        try {
            final BufferedReader bufReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));

            String line = null;
            List<String> instance = new ArrayList<>();
            while ((line = bufReader.readLine()) != null) {
                instance.add(line);
            }
            long time = System.currentTimeMillis();
            theResult = taggerFunction.apply(instance);
            bufReader.close();
            System.out.println("Labeling took: " + (System.currentTimeMillis() - time) + " ms");
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while evaluating Grobid.", e);
        }

        return computeStats(theResult);
    }

    /** Computes the stats for a single model, returning a ModelStats object, which
     * ships:
     *  - field level statistics
     *  - instances statistics
     */
    public static ModelStats computeStats(String theResult) {
        return new ModelStats(theResult);
    }

    /**
     * computes the token level results
     */
    @Deprecated
    public static Stats tokenLevelStats(String theResult) {
        Stats wordStats = new Stats();
        String line = null;
        StringTokenizer stt = new StringTokenizer(theResult, System.lineSeparator());
        while (stt.hasMoreTokens()) {
            line = stt.nextToken();

            if (line.trim().length() == 0) {
                continue;
            }
            // the two last tokens, separated by a tabulation, gives the
            // expected label and, last, the resulting label -> for Wapiti
            StringTokenizer st = new StringTokenizer(line, "\t ");
            String obtainedLabel = null;
            String expectedLabel = null;

            while (st.hasMoreTokens()) {
                obtainedLabel = getPlainLabel(st.nextToken());
                if (st.hasMoreTokens()) {
                    expectedLabel = obtainedLabel;
                }
            }

            if ((expectedLabel == null) || (obtainedLabel == null)) {
                continue;
            }

            processCounters(wordStats, obtainedLabel, expectedLabel);
            /*if (!obtainedLabel.equals(expectedLabel)) {
                logger.warn("Disagreement / expected: " + expectedLabel + " / obtained: " + obtainedLabel);
            }*/
        }
        return wordStats;
    }

    private static void processCounters(Stats stats, String obtained, String expected) {
        LabelStat expectedStat = stats.getLabelStat(expected);
        LabelStat obtainedStat = stats.getLabelStat(obtained);

        expectedStat.incrementExpected();

        if (expected.equals(obtained)) {
            expectedStat.incrementObserved();
        } else {
            expectedStat.incrementFalseNegative();
            obtainedStat.incrementFalsePositive();
        }
    }

    public static String computeMetrics(Stats stats) {
        return stats.getTextReport();
    }

    public static String computeMetricsMD(Stats stats) {
        return stats.getMarkDownReport();
    }

}
