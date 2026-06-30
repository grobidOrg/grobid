package org.grobid.core.jni;

import java.io.File;

import fr.limsi.wapiti.SWIGTYPE_p_mdl_t;
import fr.limsi.wapiti.Wapiti;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.exceptions.GrobidException;

public class WapitiModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(WapitiModel.class);

    private SWIGTYPE_p_mdl_t model;
    private File modelFile;

    public WapitiModel(File modelFile) {
        this.modelFile = modelFile;
        init();
    }

    public WapitiModel(GrobidModel grobidModel) {
        modelFile = new File(grobidModel.getModelPath());
        init();
    }

    private synchronized void init() {
        if (model != null) {
            return;
        }
        if (!modelFile.exists() || modelFile.isDirectory()) {
            throw new GrobidException("Model file does not exists or is a directory: " + modelFile.getAbsolutePath());
        }
        //LOGGER.info("Loading model: " + modelFile + " (size: " + modelFile.length() + ")");
        model = WapitiWrapper.getModel(modelFile);
    }

    public String label(String data) {
        if (model == null) {
            LOGGER.warn("Model has been already closed, reopening: " + modelFile.getAbsolutePath());
            init();
        }
        String label = WapitiWrapper.label(model, data).trim();
        //TODO: VZ: Grobid currently expects tabs as separators whereas wapiti uses spaces for separating features.
        // for now it is safer to replace, although it does not look nice
        label = label.replaceAll(" ", "\t");
        return label;
    }

    public synchronized void close() {
        if (model != null) {
            Wapiti.freeModel(model);
            model = null;
        }
    }

    public static void train(File template, File trainingData, File outputModel) {
        train(template, trainingData, outputModel, "");
    }

    public static void train(File template, File trainingData, File outputModel, String params) {
        String args = String.format(
                "train " + params + " -p %s %s %s",
                template.getAbsolutePath(),
                trainingData.getAbsolutePath(),
                outputModel.getAbsolutePath());
        //System.out.println("Training with equivalent command line: \n" + "wapiti " + args);
        Wapiti.runWapiti(args);
    }

}
