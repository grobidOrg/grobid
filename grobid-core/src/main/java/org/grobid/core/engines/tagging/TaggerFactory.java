package org.grobid.core.engines.tagging;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.utilities.GrobidProperties;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for a sequence labelling, aka a tagger, instance. Supported implementations are CRF (CRFPP, Wapiti) and Deep
 * Learning (DeLFT)
 *
 */
public class TaggerFactory {
    public static final Logger LOGGER = LoggerFactory.getLogger(TaggerFactory.class);

    private static Map<GrobidModel, GenericTagger> cache = new HashMap<>();

    private TaggerFactory() {
    }

    public static synchronized GenericTagger getTagger(GrobidModel model) {
        return getTagger(model, GrobidProperties.getGrobidEngine(model), GrobidProperties.getDelftArchitecture(model));
    }

    public static synchronized GenericTagger getTagger(GrobidModel model, GrobidCRFEngine engine) {
        return getTagger(model, engine, GrobidProperties.getDelftArchitecture(model));
    }

    public static synchronized GenericTagger getTagger(GrobidModel model, GrobidCRFEngine engine, String architecture) {
        GenericTagger t = cache.get(model);
        if (t == null) {
            if (model.equals(GrobidModels.DUMMY)) {
                return new DummyTagger(model);
            }

            if (engine != null) {
                switch (engine) {
                    case CRFPP :
                        t = new CRFPPTagger(model);
                        break;
                    case WAPITI :
                        t = new WapitiTagger(model);
                        break;
                    case DELFT :
                        t = new DeLFTTagger(model, architecture);
                        break;
                    default :
                        throw new IllegalStateException(
                                "Unsupported Grobid sequence labelling engine: " + engine.getExt());
                }
                cache.put(model, t);
            } else {
                throw new IllegalStateException(
                        "Unsupported or null Grobid sequence labelling engine: " + engine.getExt());
            }
        }
        return t;
    }
}
