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
package org.grobid.trainer.evaluation;

import static org.grobid.core.engines.tagging.GenericTaggerUtils.getPlainLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;

/**
 * Represent all different evaluation for a specific model
 */
public class ModelStats {
    private int totalInstances;
    private int correctInstance;
    private Stats fieldStats;
    private String rawResults;

    protected ModelStats() {
    }

    public ModelStats(String results) {
        this.fieldStats = fieldLevelStats(results);
        this.rawResults = results;
        Pair<Integer, Integer> doubleDoublePair = computeInstanceStatistics(results);

        this.setTotalInstances(doubleDoublePair.getLeft());
        this.setCorrectInstance(doubleDoublePair.getRight());
    }

    public Pair<Integer, Integer> computeInstanceStatistics(String results) {
        // instance-level: instances are separated by a new line in the result file
        // third pass
        String resultsPost = results.replace("\n\n", "\n \n");
        StringTokenizer stt = new StringTokenizer(resultsPost, "\n");
        boolean allGood = true;
        int correctInstance = 0;
        int totalInstance = 0;
        String line = null;
        while (stt.hasMoreTokens()) {
            line = stt.nextToken();
            if ((line.trim().length() == 0) || (!stt.hasMoreTokens())) {
                // instance done
                totalInstance++;
                if (allGood) {
                    correctInstance++;
                }
                // we reinit for a new instance
                allGood = true;
            } else {
                StringTokenizer st = new StringTokenizer(line, "\t ");
                String obtainedLabel = null;
                String expectedLabel = null;
                while (st.hasMoreTokens()) {
                    obtainedLabel = getPlainLabel(st.nextToken());
                    if (st.hasMoreTokens()) {
                        expectedLabel = obtainedLabel;
                    }
                }

                if (!obtainedLabel.equals(expectedLabel)) {
                    // one error is enough to have the whole instance false, damn!
                    allGood = false;
                }
            }
        }

        return new ImmutablePair<>(totalInstance, correctInstance);
    }

    public void setTotalInstances(int totalInstances) {
        this.totalInstances = totalInstances;
    }

    public int getTotalInstances() {
        return totalInstances;
    }

    public void setCorrectInstance(int correctInstance) {
        this.correctInstance = correctInstance;
    }

    public int getCorrectInstance() {
        return correctInstance;
    }

    public void setFieldStats(Stats fieldStats) {
        this.fieldStats = fieldStats;
    }

    public Stats getFieldStats() {
        return fieldStats;
    }

    public double getInstanceRecall() {
        if (getTotalInstances() <= 0) {
            return 0.0d;
        }
        return (double) getCorrectInstance() / (getTotalInstances());
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean includeRawResults) {
        StringBuilder report = new StringBuilder();

        if (includeRawResults) {
            report.append("=== START RAW RESULTS ===").append("\n");
            report.append(getRawResults()).append("\n");
            report.append("=== END RAw RESULTS ===").append("\n").append("\n");
        }

        Stats fieldStats = getFieldStats();
        report.append("\n===== Field-level results =====\n");
        report.append(
                String.format(
                        "\n%-20s %-12s %-12s %-12s %-12s %-7s\n\n",
                        "label",
                        "accuracy",
                        "precision",
                        "recall",
                        "f1",
                        "support"));

        for (Map.Entry<String, LabelResult> labelResult : fieldStats.getLabelsResults().entrySet()) {
            report.append(labelResult.getValue());
        }

        report.append("\n");

        report.append(
                String.format(
                        "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                        "all (micro avg.)",
                        TextUtilities.formatTwoDecimals(fieldStats.getMicroAverageAccuracy() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMicroAveragePrecision() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMicroAverageRecall() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMicroAverageF1() * 100),
                        String.valueOf(getSupportSum())));

        report.append(
                String.format(
                        "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                        "all (macro avg.)",
                        TextUtilities.formatTwoDecimals(fieldStats.getMacroAverageAccuracy() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMacroAveragePrecision() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMacroAverageRecall() * 100),
                        TextUtilities.formatTwoDecimals(fieldStats.getMacroAverageF1() * 100),
                        String.valueOf(getSupportSum())));

        // instance-level: instances are separated by a new line in the result file
        report.append("\n===== Instance-level results =====\n\n");
        report.append(String.format("%-27s %d\n", "Total expected instances:", getTotalInstances()));
        report.append(String.format("%-27s %d\n", "Correct instances:", getCorrectInstance()));
        report.append(
                String.format(
                        "%-27s %s\n",
                        "Instance-level recall:",
                        TextUtilities.formatTwoDecimals(getInstanceRecall() * 100)));

        return report.toString();
    }

    public long getSupportSum() {
        long supportSum = 0;
        for (LabelResult labelResult : fieldStats.getLabelsResults().values()) {
            supportSum += labelResult.getSupport();
        }
        return supportSum;
    }

    public String getRawResults() {
        return rawResults;
    }

    public void setRawResults(String rawResults) {
        this.rawResults = rawResults;
    }

    public Stats fieldLevelStats(String theResult) {
        Stats fieldStats = new Stats();

        // field: a field is simply a sequence of token with the same label

        // we build first the list of fields in expected and obtained result
        // with offset positions
        List<Pair<String, OffsetPosition>> expectedFields = new ArrayList<>();
        List<Pair<String, OffsetPosition>> obtainedFields = new ArrayList<>();
        StringTokenizer stt = new StringTokenizer(theResult, System.lineSeparator());
        String line = null;
        String previousExpectedLabel = null;
        String previousObtainedLabel = null;
        int pos = 0; // current token index
        OffsetPosition currentObtainedPosition = new OffsetPosition();
        currentObtainedPosition.start = 0;
        OffsetPosition currentExpectedPosition = new OffsetPosition();
        currentExpectedPosition.start = 0;
        String obtainedLabel = null;
        String expectedLabel = null;
        while (stt.hasMoreTokens()) {
            line = stt.nextToken();
            obtainedLabel = null;
            expectedLabel = null;
            StringTokenizer st = new StringTokenizer(line, "\t ");
            while (st.hasMoreTokens()) {
                obtainedLabel = st.nextToken();
                if (st.hasMoreTokens()) {
                    expectedLabel = obtainedLabel;
                }
            }

            if ((obtainedLabel == null) || (expectedLabel == null))
                continue;

            if ((previousObtainedLabel != null) &&
                    (!obtainedLabel.equals(getPlainLabel(previousObtainedLabel)))) {
                // new obtained field
                currentObtainedPosition.end = pos - 1;
                Pair<String, OffsetPosition> theField = new ImmutablePair<>(getPlainLabel(previousObtainedLabel),
                        currentObtainedPosition);
                currentObtainedPosition = new OffsetPosition();
                currentObtainedPosition.start = pos;
                obtainedFields.add(theField);
            }

            if ((previousExpectedLabel != null) &&
                    (!expectedLabel.equals(getPlainLabel(previousExpectedLabel)))) {
                // new expected field
                currentExpectedPosition.end = pos - 1;
                Pair<String, OffsetPosition> theField = new ImmutablePair<>(getPlainLabel(previousExpectedLabel),
                        currentExpectedPosition);
                currentExpectedPosition = new OffsetPosition();
                currentExpectedPosition.start = pos;
                expectedFields.add(theField);
            }

            previousExpectedLabel = expectedLabel;
            previousObtainedLabel = obtainedLabel;
            pos++;
        }
        // last fields of the sequence
        if ((previousObtainedLabel != null)) {
            currentObtainedPosition.end = pos - 1;
            Pair<String, OffsetPosition> theField = new ImmutablePair<>(getPlainLabel(previousObtainedLabel),
                    currentObtainedPosition);
            obtainedFields.add(theField);
        }

        if ((previousExpectedLabel != null)) {
            currentExpectedPosition.end = pos - 1;
            Pair<String, OffsetPosition> theField = new ImmutablePair<>(getPlainLabel(previousExpectedLabel),
                    currentExpectedPosition);
            expectedFields.add(theField);
        }

        // we then simply compared the positions and labels of the two fields and update
        // statistics
        int obtainedFieldIndex = 0;
        List<Pair<String, OffsetPosition>> matchedObtainedFields = new ArrayList<Pair<String, OffsetPosition>>();
        for (Pair<String, OffsetPosition> expectedField : expectedFields) {
            expectedLabel = expectedField.getLeft();
            int expectedStart = expectedField.getRight().start;
            int expectedEnd = expectedField.getRight().end;

            LabelStat labelStat = fieldStats.getLabelStat(getPlainLabel(expectedLabel));
            labelStat.incrementExpected();

            // try to find a match in the obtained fields
            boolean found = false;
            for (int i = obtainedFieldIndex; i < obtainedFields.size(); i++) {
                obtainedLabel = obtainedFields.get(i).getLeft();
                if (!expectedLabel.equals(obtainedLabel))
                    continue;
                if ((expectedStart == obtainedFields.get(i).getRight().start) &&
                        (expectedEnd == obtainedFields.get(i).getRight().end)) {
                    // we have a match
                    labelStat.incrementObserved(); // TP
                    found = true;
                    obtainedFieldIndex = i;
                    matchedObtainedFields.add(obtainedFields.get(i));
                    break;
                }
                // if we went too far, we can stop the pain
                if (expectedEnd < obtainedFields.get(i).getRight().start) {
                    break;
                }
            }
            if (!found) {
                labelStat.incrementFalseNegative();
            }
        }

        // all the obtained fields without match in the expected fields are false positive
        for (Pair<String, OffsetPosition> obtainedField : obtainedFields) {
            if (!matchedObtainedFields.contains(obtainedField)) {
                obtainedLabel = obtainedField.getLeft();
                LabelStat labelStat = fieldStats.getLabelStat(getPlainLabel(obtainedLabel));
                labelStat.incrementFalsePositive();
            }
        }

        return fieldStats;
    }
}
