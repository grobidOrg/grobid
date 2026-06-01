package org.grobid.trainer;

import java.io.File;

import org.grobid.core.GrobidModel;
import org.grobid.core.jni.DeLFTModel;
import org.grobid.core.utilities.GrobidProperties;

public class DeLFTTrainer implements GenericTrainer {

    public static final String DELFT = "delft";

    @Override
    public void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model) {
        train(template, trainingData, outputModel, numThreads, model, false);
    }

    @Override
    public void train(
            File template,
            File trainingData,
            File outputModel,
            int numThreads,
            GrobidModel model,
            boolean incremental) {
        DeLFTModel.train(
                model.getModelName(),
                trainingData,
                outputModel,
                GrobidProperties.getDelftArchitecture(model),
                incremental);
    }

    @Override
    public String getName() {
        return DELFT;
    }

    /**
     * None of this below is used by DeLFT
     */
    @Override
    public void setEpsilon(double epsilon) {
    }

    @Override
    public void setWindow(int window) {
    }

    @Override
    public double getEpsilon() {
        return 0.0;
    }

    @Override
    public int getWindow() {
        return 0;
    }

    @Override
    public void setNbMaxIterations(int iterations) {
    }

    @Override
    public int getNbMaxIterations() {
        return 0;
    }
}
