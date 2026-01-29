package org.grobid.trainer.evaluation;

import org.grobid.core.utilities.TextUtilities;

public class LabelResult {

    private final String label;
    private double accuracy;
    private double precision;
    private double recall;
    private double f1Score;
    private long support;

    public LabelResult(String label) {
        this.label = label;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public String getLabel() {
        return label;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getPrecision() {
        return precision;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getRecall() {
        return recall;
    }

    public void setF1Score(double f1Score) {
        this.f1Score = f1Score;
    }

    public double getF1Score() {
        return f1Score;
    }

    public void setSupport(long support) {
        this.support = support;

    }

    public String toString() {
        return String.format(
                "%-20s %-12s %-12s %-12s %-12s %-7s\n",
                label,
                TextUtilities.formatTwoDecimals(getAccuracy() * 100),
                TextUtilities.formatTwoDecimals(getPrecision() * 100),
                TextUtilities.formatTwoDecimals(getRecall() * 100),
                TextUtilities.formatTwoDecimals(getF1Score() * 100),
                String.valueOf(getSupport()));
    }

    public long getSupport() {
        return support;
    }
}
