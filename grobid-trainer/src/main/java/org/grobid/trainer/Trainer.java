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
import org.grobid.core.engines.tagging.GenericTagger;

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
