package org.grobid.trainer.evaluation;

/** 
 *  Model the results for each label 
 */
public final class LabelStat {
    private int falsePositive = 0;
    private int falseNegative = 0;
    private int observed = 0; // this is true positives
    private int expected = 0; // total expected number of items with this label

    private double accuracy = 0.0;
    private int trueNegative;
    private boolean hasChanged = false;

    public void incrementFalseNegative() {
        this.incrementFalseNegative(1);
        hasChanged = true;
    }

    public void incrementFalsePositive() {
        this.incrementFalsePositive(1);
        hasChanged = true;
    }

    public void incrementObserved() {
        this.incrementObserved(1);
        hasChanged = true;
    }

    public void incrementExpected() {
        this.incrementExpected(1);
        hasChanged = true;
    }

    public void incrementFalseNegative(int count) {
        this.falseNegative += count;
        hasChanged = true;
    }

    public void incrementFalsePositive(int count) {
        this.falsePositive += count;
        hasChanged = true;
    }

    public void incrementObserved(int count) {
        this.observed += count;
        hasChanged = true;
    }

    public void incrementExpected(int count) {
        this.expected += count;
        hasChanged = true;
    }

    public int getExpected() {
        return this.expected;
    }

    public int getFalseNegative() {
        return this.falseNegative;
    }

    public int getFalsePositive() {
        return this.falsePositive;
    }

    public int getObserved() {
        return this.observed;
    }

    public int getAll() {
        return observed + falseNegative + falsePositive;
    }

    public void setFalsePositive(int falsePositive) {
        this.falsePositive = falsePositive;
        hasChanged = true;
    }

    public void setFalseNegative(int falseNegative) {
        this.falseNegative = falseNegative;
        hasChanged = true;
    }

    public void setObserved(int observed) {
        this.observed = observed;
        hasChanged = true;
    }

    public void setExpected(int expected) {
        this.expected = expected;
        hasChanged = true;
    }

    public static LabelStat create() {
        return new LabelStat();
    }

    public double getAccuracy() {
        double accuracy = (double) (observed + trueNegative) / (observed + falsePositive + trueNegative + falseNegative);
        if (accuracy < 0.0)
            accuracy = 0.0;

        return accuracy;
    }

    public long getSupport() {
        return expected;
    }

    public double getPrecision() {
        if (observed == 0.0) {
            return 0.0;
        }
        return ((double) observed) / (falsePositive + observed);
    }

    public double getRecall() {
        if (expected == 0.0)
            return 0.0;
        return ((double) observed) / (expected);
    }

    public double getF1Score() {
        double precision = getPrecision();
        double recall = getRecall();

        if ((precision == 0.0) && (recall == 0.0))
            return 0.0;

        return (2.0 * precision * recall) / (precision + recall);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("falsePositive: ").append(falsePositive);
        builder.append("; falseNegative: ").append(falseNegative);
        builder.append("; observed: ").append(observed);
        builder.append("; expected: ").append(expected);
        return builder.toString();
    }

    public void setTrueNegative(int trueNegative) {
        this.trueNegative = trueNegative;
    }

    public boolean hasChanged() {
        boolean oldValue = hasChanged;
        hasChanged = false;
        return oldValue;
    }
}
