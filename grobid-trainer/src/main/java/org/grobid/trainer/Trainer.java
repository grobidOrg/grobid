package org.grobid.trainer;

import org.grobid.core.GrobidModel;
import org.grobid.core.engines.tagging.GenericTagger;

import java.io.File;

public interface Trainer {

    int createCRFPPData(File corpusPath, File outputFile);

    int createCRFPPData(File corpusPath, File outputTrainingFile, File outputEvalFile, double splitRatio);

    void train();

    void train(boolean incremental);

    String evaluate();

    String evaluate(boolean includeRawResults);

    String evaluate(GenericTagger tagger, boolean includeRawResults);

    String splitTrainEvaluate(Double split);

    String splitTrainEvaluate(Double split, boolean incremental);

    String nFoldEvaluate(int folds);

    String nFoldEvaluate(int folds, boolean includeRawResults);

    GrobidModel getModel();
}
