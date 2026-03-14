package org.grobid.trainer;

import org.chasen.crfpp.CRFPPTrainer;
import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Note: Usage of CRF++ in GROBID is deprecated.
 *
 * @deprecated use WapitiTrainer or DelftTrainer (requires http://github.com/kermitt2/delft)
 */
@Deprecated
public class CRFPPGenericTrainer implements GenericTrainer {
    public static final Logger LOGGER = LoggerFactory.getLogger(CRFPPGenericTrainer.class);
    public static final String CRF = "crf";
    private final CRFPPTrainer crfppTrainer;

	// default training parameters (not exploited by CRFPP so far, it requires to extend the JNI)
	protected double epsilon = 0.00001; // default size of the interval for stopping criterion
	protected int window = 20; // default similar to CRF++
    protected int nbMaxIterations = 6000;

    public CRFPPGenericTrainer() {
        crfppTrainer = new CRFPPTrainer();
    }

    @Override
    public void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model) {
        train(template, trainingData, outputModel, numThreads, model, false);
    }

    @Override
    public void train(File template, File trainingData, File outputModel, int numThreads, GrobidModel model, boolean incremental) {
        crfppTrainer.train(template.getAbsolutePath(), trainingData.getAbsolutePath(), outputModel.getAbsolutePath(), numThreads);
        if (!crfppTrainer.what().isEmpty()) {
            LOGGER.warn("CRF++ Trainer warnings:\n" + crfppTrainer.what());
        } else {
            LOGGER.info("No CRF++ Trainer warnings!");
        }
    }

    @Override
    public String getName() {
        return CRF;
    }
	
    @Override
    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
	
    @Override
    public void setWindow(int window) {
        this.window = window;
    }
	
    @Override
    public double getEpsilon() {
        return epsilon;
    }
	
    @Override
    public int getWindow() {
        return window;
    }

    @Override
    public void setNbMaxIterations(int interations) {
        this.nbMaxIterations = interations;
    }
    
    @Override
    public int getNbMaxIterations() {
        return nbMaxIterations;
    }

    @Override
    public double getL1() { return 0; }

    @Override
    public void setL1(double l1) { }

    @Override
    public double getL2() { return 0; }

    @Override
    public void setL2(double l2) { }
}
