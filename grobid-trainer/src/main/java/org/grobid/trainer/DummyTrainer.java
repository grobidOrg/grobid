package org.grobid.trainer;

import org.grobid.core.GrobidModel;

import java.io.File;

/**
 * Dummy trainer which won't do anything.
 */
public class DummyTrainer implements GenericTrainer {
    @Override
    public void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model) {

    }

    @Override
    public void train(
            File template,
            File trainingData,
            File outputModel,
            int numThreads,
            GrobidModel model,
            boolean incremental) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setEpsilon(double epsilon) {

    }

    @Override
    public void setWindow(int window) {

    }

    @Override
    public double getEpsilon() {
        return 0;
    }

    @Override
    public int getWindow() {
        return 0;
    }

    @Override
    public int getNbMaxIterations() {
        return 0;
    }

    @Override
    public void setNbMaxIterations(int iterations) {

    }
}
