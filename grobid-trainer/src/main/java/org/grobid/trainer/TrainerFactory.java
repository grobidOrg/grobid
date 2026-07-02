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

import org.grobid.core.GrobidModel;
import org.grobid.core.utilities.GrobidProperties;

public class TrainerFactory {
    public static GenericTrainer getTrainer(GrobidModel model) {

        // System.out.println(model.getModelName());
        // System.out.println(model.getModelPath());
        // System.out.println(GrobidProperties.getGrobidEngine(model));

        switch (GrobidProperties.getGrobidEngine(model)) {
            case CRFPP :
                return new CRFPPGenericTrainer();
            case WAPITI :
                return new WapitiTrainer();
            case DELFT :
                return new DeLFTTrainer();
            case DUMMY :
                return new DummyTrainer();
            default :
                throw new IllegalStateException(
                        "Unsupported GROBID sequence labelling engine: " + GrobidProperties.getGrobidEngine(model));
        }
    }
}
