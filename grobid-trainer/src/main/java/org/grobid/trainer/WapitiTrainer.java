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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

import org.grobid.core.GrobidModel;
import org.grobid.core.jni.WapitiModel;

public class WapitiTrainer implements GenericTrainer {

    public static final String WAPITI = "wapiti";

    // default training parameters (only exploited by Wapiti)
    protected double epsilon = 0.00001; // default size of the interval for stopping criterion
    protected int window = 20; // default similar to CRF++
    protected int nbMaxIterations = 2000; // by default maximum of training iterations
    protected int jobSize = 0; // 0 = auto-compute from training data size and thread count
    protected double l1 = 0.5; // L1 regularization (Wapiti default: 0.5)
    protected double l2 = 0.0001; // L2 regularization (Wapiti default: 0.0001)

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
        System.out.println("\tl1: " + l1);
        System.out.println("\tl2: " + l2);

        String incrementalBlock = "";
        if (incremental) {
            String inputModelPath = outputModel.getAbsolutePath();
            if (inputModelPath.endsWith(".new"))
                inputModelPath = inputModelPath.substring(0, inputModelPath.length() - 4);
            System.out.println("\tincremental training from: " + inputModelPath);
            incrementalBlock += " -m " + inputModelPath;
        }

        int effectiveJobSize = jobSize;
        if (effectiveJobSize <= 0) {
            int numSequences = countSequences(trainingData);
            // Aim for at least numThreads batches so all threads stay busy
            effectiveJobSize = Math.max(1, (int) Math.ceil((double) numSequences / numThreads));
            System.out.println("\ttraining sequences: " + numSequences);
        }
        System.out.println("\tjob size: " + effectiveJobSize);

        WapitiModel.train(
                template,
                trainingData,
                outputModel,
                "--nthread "
                        + numThreads
                        + " -j "
                        + effectiveJobSize
                        + " -1 "
                        + BigDecimal.valueOf(l1).toPlainString()
                        + " -2 "
                        + BigDecimal.valueOf(l2).toPlainString()
                        + " -e "
                        + BigDecimal.valueOf(epsilon).toPlainString()
                        + " -w "
                        + window
                        + " -i "
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

    @Override
    public double getL1() {
        return l1;
    }

    @Override
    public void setL1(double l1) {
        this.l1 = l1;
    }

    @Override
    public double getL2() {
        return l2;
    }

    @Override
    public void setL2(double l2) {
        this.l2 = l2;
    }

    /**
     * Count training sequences in a Wapiti training file.
     * Sequences are separated by blank lines.
     */
    private static int countSequences(File trainingData) {
        int count = 0;
        boolean inSequence = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(trainingData))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    if (inSequence) {
                        count++;
                        inSequence = false;
                    }
                } else {
                    inSequence = true;
                }
            }
            if (inSequence) {
                count++;
            }
        } catch (IOException e) {
            System.err.println("Warning: could not count sequences, using default job size");
            return 64; // wapiti default
        }
        return count;
    }
}
