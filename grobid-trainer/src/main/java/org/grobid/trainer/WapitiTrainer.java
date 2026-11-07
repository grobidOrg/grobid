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
package org.grobid.trainer;

import java.io.File;
import java.math.BigDecimal;

import org.grobid.core.GrobidModel;
import org.grobid.core.jni.WapitiModel;

public class WapitiTrainer implements GenericTrainer {

    public static final String WAPITI = "wapiti";

    // default training parameters (only exploited by Wapiti)
    protected double epsilon = 0.00001; // default size of the interval for stopping criterion
    protected int window = 20; // default similar to CRF++
    protected int nbMaxIterations = 2000; // by default maximum of training iterations

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
        System.out.println("\tepsilon: " + epsilon);
        System.out.println("\twindow: " + window);
        System.out.println("\tnb max iterations: " + nbMaxIterations);
        System.out.println("\tnb threads: " + numThreads);

        String incrementalBlock = "";
        if (incremental) {
            String inputModelPath = outputModel.getAbsolutePath();
            if (inputModelPath.endsWith(".new"))
                inputModelPath = inputModelPath.substring(0, inputModelPath.length() - 4);
            System.out.println("\tincremental training from: " + inputModelPath);
            incrementalBlock += " -m " + inputModelPath;
        }

        WapitiModel.train(template, trainingData, outputModel, "--nthread " + numThreads +
        //       		" --algo sgd-l1" +
                " -e "
                + BigDecimal.valueOf(epsilon).toPlainString()
                +
                " -w "
                + window
                +
                " -i "
                + nbMaxIterations
                + incrementalBlock);
    }

    @Override
    public String getName() {
        return WAPITI;
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
    public void setNbMaxIterations(int iterations) {
        this.nbMaxIterations = iterations;
    }

    @Override
    public int getNbMaxIterations() {
        return nbMaxIterations;
    }
}
