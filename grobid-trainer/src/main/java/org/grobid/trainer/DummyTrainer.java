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

import org.grobid.core.GrobidModel;

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
