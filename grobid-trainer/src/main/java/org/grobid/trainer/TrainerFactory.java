package org.grobid.trainer;

import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.GrobidModel;

public class TrainerFactory {
    public static GenericTrainer getTrainer(GrobidModel model) {

        System.out.println(model.getModelName());
        System.out.println(model.getModelPath());
        System.out.println(GrobidProperties.getGrobidEngine(model));

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
