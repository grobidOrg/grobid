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

import java.util.*;
import java.util.function.Supplier;

import org.grobid.core.GrobidModels.Flavor;

/**
 * Single source of truth for model-name to trainer-instance mapping.
 * Used by TrainerRunner (CLI) and GrobidRestProcessTraining (REST API).
 */
public class TrainerRegistry {
    private static final Map<String, Supplier<AbstractTrainer>> REGISTRY;

    static {
        Map<String, Supplier<AbstractTrainer>> map = new LinkedHashMap<>();
        // Simple trainers
        map.put("affiliation", AffiliationAddressTrainer::new);
        map.put("affiliation-address", AffiliationAddressTrainer::new);
        map.put("chemical", ChemicalEntityTrainer::new);
        map.put("citation", CitationTrainer::new);
        map.put("date", DateTrainer::new);
        map.put("figure", FigureTrainer::new);
        map.put("fulltext", FulltextTrainer::new);
        map.put("funding-acknowledgement", FundingAcknowledgementTrainer::new);
        map.put("header", HeaderTrainer::new);
        map.put("monograph", MonographTrainer::new);
        map.put("name-citation", NameCitationTrainer::new);
        map.put("name-header", NameHeaderTrainer::new);
        map.put("patent", PatentParserTrainer::new);
        map.put("patent-citation", PatentParserTrainer::new);
        map.put("reference-segmenter", ReferenceSegmenterTrainer::new);
        map.put("segmentation", SegmentationTrainer::new);
        map.put("shorttext", ShorttextTrainer::new);
        map.put("table", TableTrainer::new);
        // Flavor variants
        map.put("fulltext-light", () -> new FulltextTrainer(Flavor.ARTICLE_LIGHT));
        map.put("fulltext-light-ref", () -> new FulltextTrainer(Flavor.ARTICLE_LIGHT_WITH_REFERENCES));
        map.put("header-ietf", () -> new HeaderTrainer(Flavor.IETF));
        map.put("header-light", () -> new HeaderTrainer(Flavor.ARTICLE_LIGHT));
        map.put("header-light-ref", () -> new HeaderTrainer(Flavor.ARTICLE_LIGHT_WITH_REFERENCES));
        map.put("segmentation-ietf", () -> new SegmentationTrainer(Flavor.IETF));
        map.put("segmentation-light", () -> new SegmentationTrainer(Flavor.ARTICLE_LIGHT));
        map.put("segmentation-light-ref", () -> new SegmentationTrainer(Flavor.ARTICLE_LIGHT_WITH_REFERENCES));
        REGISTRY = Collections.unmodifiableMap(map);
    }

    public static AbstractTrainer getTrainer(String modelName) {
        Supplier<AbstractTrainer> factory = REGISTRY.get(modelName);
        if (factory == null) {
            throw new IllegalStateException("Unknown model: "
                    + modelName
                    +
                    ". Known models: "
                    + String.join(", ", REGISTRY.keySet()));
        }
        return factory.get();
    }

    public static Set<String> getModelNames() {
        return REGISTRY.keySet();
    }
}
