package org.grobid.trainer;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import java.io.File;

public interface GenericTrainer {
    void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model);
    void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model, boolean incremental);
    String getName();
	public void setEpsilon(double epsilon);
	public void setWindow(int window);
	public double getEpsilon();
	public int getWindow();
	public int getNbMaxIterations();
	public void setNbMaxIterations(int iterations);
	public double getL1();
	public void setL1(double l1);
	public double getL2();
	public void setL2(double l2);
}
