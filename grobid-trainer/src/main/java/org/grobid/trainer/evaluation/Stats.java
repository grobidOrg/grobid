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

import java.util.Set;
import java.util.TreeMap;

import org.grobid.core.exceptions.*;
import org.grobid.core.utilities.TextUtilities;

/**
 * Contains the single statistic computation for evaluation
 *
 * This class is state full. The statistics needs to be recomputed every time
 * something is changed (the flag requiredToRecomputeMetrics).
 */
public final class Stats {
    private final TreeMap<String, LabelStat> labelStats;

    // State variable to know whether is required to recompute the statistics
    private boolean requiredToRecomputeMetrics = true;

    private double cumulated_tp = 0;
    private double cumulated_fp = 0;
    private double cumulated_tn = 0;
    private double cumulated_fn = 0;
    private double cumulated_f1 = 0.0;
    private double cumulated_accuracy = 0.0;
    private double cumulated_precision = 0.0;
    private double cumulated_recall = 0.0;
    private double cumulated_expected = 0;
    private int totalValidFields = 0;

    public Stats() {
        this.labelStats = new TreeMap<>();
    }

    public Set<String> getLabels() {
        return this.labelStats.keySet();
    }

    public void removeLabel(String label) {
        this.labelStats.remove(label);
    }

    public void incrementFalsePositive(String label) {
        this.incrementFalsePositive(label, 1);
    }

    public void incrementFalsePositive(String label, int count) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        labelStat.incrementFalsePositive(count);
        requiredToRecomputeMetrics = true;
    }

    public void incrementFalseNegative(String label) {
        this.incrementFalseNegative(label, 1);
    }

    public void incrementFalseNegative(String label, int count) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        labelStat.incrementFalseNegative(count);
        requiredToRecomputeMetrics = true;
    }

    public void incrementObserved(String label) {
        this.incrementObserved(label, 1);
    }

    public void incrementObserved(String label, int count) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        labelStat.incrementObserved(count);
        requiredToRecomputeMetrics = true;
    }

    public void incrementExpected(String label) {
        this.incrementExpected(label, 1);
    }

    public void incrementExpected(String label, int count) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        labelStat.incrementExpected(count);
        requiredToRecomputeMetrics = true;
    }

    public LabelStat getLabelStat(String label) {
        if (this.labelStats.containsKey(label)) {
            return this.labelStats.get(label);
        }

        LabelStat labelStat = LabelStat.create();
        this.labelStats.put(label, labelStat);
        requiredToRecomputeMetrics = true;

        return labelStat;
    }

    public int size() {
        return this.labelStats.size();
    }

    public double getPrecision(String label) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        return labelStat.getPrecision();
    }

    public double getRecall(String label) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        return labelStat.getRecall();
    }

    public double getF1Score(String label) {
        LabelStat labelStat = this.getLabelStat(label);
        if (labelStat == null)
            throw new GrobidException("Unknown label: " + label);
        return labelStat.getF1Score();
    }

    /**
     * In order to compute metrics in an efficient way, they are computed all at the same time.
     * Since the state of the object is important in this case, it's required to have a flag that
     * allow the recompute of the metrics when one is required.
     */
    public void computeMetrics() {
        for (String label : getLabels()) {
            if (getLabelStat(label).hasChanged()) {
                requiredToRecomputeMetrics = true;
                break;
            }
        }

        if (!requiredToRecomputeMetrics)
            return;

        int totalFields = 0;
        for (String label : getLabels()) {
            LabelStat labelStat = getLabelStat(label);
            totalFields += labelStat.getObserved();
            totalFields += labelStat.getFalseNegative();
            totalFields += labelStat.getFalsePositive();
        }

        for (String label : getLabels()) {
            if (label.equals("<other>") || label.equals("base") || label.equals("O")) {
                continue;
            }

            LabelStat labelStat = getLabelStat(label);
            int tp = labelStat.getObserved(); // true positives
            int fp = labelStat.getFalsePositive(); // false positives
            int fn = labelStat.getFalseNegative(); // false negative
            int tn = totalFields - tp - (fp + fn); // true negatives
            labelStat.setTrueNegative(tn);
            int expected = labelStat.getExpected(); // all expected

            if (expected != 0) {
                totalValidFields++;
            }

            if (expected != 0) {
                cumulated_tp += tp;
                cumulated_fp += fp;
                cumulated_tn += tn;
                cumulated_fn += fn;

                cumulated_expected += expected;
                cumulated_f1 += labelStat.getF1Score();
                cumulated_accuracy += labelStat.getAccuracy();
                cumulated_precision += labelStat.getPrecision();
                cumulated_recall += labelStat.getRecall();
            }
        }

        requiredToRecomputeMetrics = false;
    }

    public TreeMap<String, LabelResult> getLabelsResults() {
        computeMetrics();

        TreeMap<String, LabelResult> result = new TreeMap<>();

        for (String label : getLabels()) {
            if (label.equals("<other>") || label.equals("base") || label.equals("O")) {
                continue;
            }

            LabelStat labelStat = getLabelStat(label);
            LabelResult labelResult = new LabelResult(label);
            labelResult.setAccuracy(labelStat.getAccuracy());
            labelResult.setPrecision(labelStat.getPrecision());
            labelResult.setRecall(labelStat.getRecall());
            labelResult.setF1Score(labelStat.getF1Score());
            labelResult.setSupport(labelStat.getSupport());

            result.put(label, labelResult);
        }

        return result;
    }

    public double getMicroAverageAccuracy() {
        computeMetrics();

        // macro average over measures
        if (totalValidFields == 0)
            return 0.0;
        else
            return Math.min(1.0, cumulated_accuracy / totalValidFields);
    }

    public double getMacroAverageAccuracy() {
        computeMetrics();

        double accuracy = 0.0;
        if (cumulated_tp + cumulated_fp + cumulated_tn + cumulated_fn != 0.0)
            accuracy = ((double) cumulated_tp + cumulated_tn)
                    / (cumulated_tp + cumulated_fp + cumulated_tn + cumulated_fn);

        return Math.min(1.0, accuracy);
    }

    public double getMicroAveragePrecision() {
        computeMetrics();

        double precision = 0.0;
        if (cumulated_tp + cumulated_fp != 0)
            precision = cumulated_tp / (cumulated_tp + cumulated_fp);

        return Math.min(1.0, precision);
    }

    public double getMacroAveragePrecision() {
        computeMetrics();

        if (totalValidFields == 0)
            return 0.0;

        return Math.min(1.0, cumulated_precision / totalValidFields);
    }

    public double getMicroAverageRecall() {
        computeMetrics();

        double recall = 0.0;
        if (cumulated_expected != 0.0)
            recall = cumulated_tp / cumulated_expected;

        return Math.min(1.0, recall);
    }

    public double getMacroAverageRecall() {
        computeMetrics();

        if (totalValidFields == 0)
            return 0.0;

        return Math.min(1.0, cumulated_recall / totalValidFields);
    }

    public int getTotalValidFields() {
        computeMetrics();
        return totalValidFields;
    }

    public double getMicroAverageF1() {
        double precision = getMicroAveragePrecision();
        double recall = getMicroAverageRecall();

        double f1 = 0.0;
        if (precision + recall != 0.0)
            f1 = (2 * precision * recall) / (precision + recall);

        return f1;
    }

    public double getMacroAverageF1() {
        computeMetrics();

        if (totalValidFields == 0)
            return 0.0;

        return Math.min(1.0, cumulated_f1 / totalValidFields);
    }

    public String getTextReport() {
        computeMetrics();

        StringBuilder report = new StringBuilder();
        report.append(
                String.format(
                        "\n%-20s %-12s %-12s %-12s %-12s %-7s\n\n",
                        "label",
                        "accuracy",
                        "precision",
                        "recall",
                        "f1",
                        "support"));

        long supportSum = 0;

        for (String label : getLabels()) {
            if (label.equals("<other>") || label.equals("base") || label.equals("O")) {
                continue;
            }

            LabelStat labelStat = getLabelStat(label);

            long support = labelStat.getSupport();
            report.append(
                    String.format(
                            "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                            label,
                            TextUtilities.formatTwoDecimals(labelStat.getAccuracy() * 100),
                            TextUtilities.formatTwoDecimals(labelStat.getPrecision() * 100),
                            TextUtilities.formatTwoDecimals(labelStat.getRecall() * 100),
                            TextUtilities.formatTwoDecimals(labelStat.getF1Score() * 100),
                            String.valueOf(support)));

            supportSum += support;
        }

        report.append("\n");

        report.append(
                String.format(
                        "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                        "all (micro avg.)",
                        TextUtilities.formatTwoDecimals(getMicroAverageAccuracy() * 100),
                        TextUtilities.formatTwoDecimals(getMicroAveragePrecision() * 100),
                        TextUtilities.formatTwoDecimals(getMicroAverageRecall() * 100),
                        TextUtilities.formatTwoDecimals(getMicroAverageF1() * 100),
                        String.valueOf(supportSum)));

        report.append(
                String.format(
                        "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                        "all (macro avg.)",
                        TextUtilities.formatTwoDecimals(getMacroAverageAccuracy() * 100),
                        TextUtilities.formatTwoDecimals(getMacroAveragePrecision() * 100),
                        TextUtilities.formatTwoDecimals(getMacroAverageRecall() * 100),
                        TextUtilities.formatTwoDecimals(getMacroAverageF1() * 100),
                        String.valueOf(supportSum)));

        return report.toString();
    }

    public String getMarkDownReport() {
        computeMetrics();

        StringBuilder report = new StringBuilder();
        report.append("\n| label            |  precision |   recall  |     f1     | support |\n");
        report.append("|---               |---         |---        |---         |---      |\n");

        long supportSum = 0;

        for (String label : getLabels()) {
            if (label.equals("<other>") || label.equals("base") || label.equals("O")) {
                continue;
            }

            LabelStat labelStat = getLabelStat(label);
            long support = labelStat.getSupport();
            report.append(
                    "| "
                            + label
                            + " | "
                            +
                            TextUtilities.formatTwoDecimals(labelStat.getPrecision() * 100)
                            + " | "
                            +
                            TextUtilities.formatTwoDecimals(labelStat.getRecall() * 100)
                            + " | "
                            +
                            TextUtilities.formatTwoDecimals(labelStat.getF1Score() * 100)
                            + " | "
                            +
                            String.valueOf(support)
                            + " |\n");
            supportSum += support;
        }

        report.append("|                  |            |           |            |         |\n");

        report.append(
                "| **all fields (micro avg.)** | **"
                        +
                        TextUtilities.formatTwoDecimals(getMicroAveragePrecision() * 100)
                        + "** | **"
                        +
                        TextUtilities.formatTwoDecimals(getMicroAverageRecall() * 100)
                        + "** | **"
                        +
                        TextUtilities.formatTwoDecimals(getMicroAverageF1() * 100)
                        + "** | "
                        +
                        String.valueOf(supportSum)
                        + " |\n");

        report.append(
                "| all fields (macro avg.) | "
                        +
                        TextUtilities.formatTwoDecimals(getMacroAveragePrecision() * 100)
                        + " | "
                        +
                        TextUtilities.formatTwoDecimals(getMacroAverageRecall() * 100)
                        + " | "
                        +
                        TextUtilities.formatTwoDecimals(getMacroAverageF1() * 100)
                        + " | "
                        +
                        String.valueOf(supportSum)
                        + " |\n\n");

        return report.toString();
    }
}
