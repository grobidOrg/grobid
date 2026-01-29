package org.grobid.trainer;

import org.grobid.core.GrobidModel;
import java.io.File;

public interface GenericTrainer {
    void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model);

    void train(
            File template,
            File trainingData,
            File outputModel,
            int numThreads,
            GrobidModel model,
            boolean incremental);

    String getName();

    public void setEpsilon(double epsilon);

    public void setWindow(int window);

    public double getEpsilon();

    public int getWindow();

    public int getNbMaxIterations();

    public void setNbMaxIterations(int iterations);
}
