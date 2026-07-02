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
package org.grobid.core;

import static org.grobid.core.engines.EngineParsers.LOGGER;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.utilities.GrobidProperties;

/**
 * This enum class acts as a registry for all Grobid models.
 */
public enum GrobidModels implements GrobidModel {

    // models are declared with a enumerated unique name associated to a **folder name** for the model
    // the folder name is where we will find the model implementation and its resources under grobid-home

    AFFILIATION_ADDRESS("affiliation-address"),
    SEGMENTATION("segmentation"),
    SEGMENTATION_ARTICLE_LIGHT("segmentation/article/light"),
    SEGMENTATION_ARTICLE_LIGHT_REF("segmentation/article/light-ref"),
    SEGMENTATION_SDO_IETF("segmentation/sdo/ietf"),
    SEGMENTATION_SDO_3GPP("segmentation/sdo/3gpp"),
    CITATION("citation"),
    REFERENCE_SEGMENTER("reference-segmenter"),
    DATE("date"),
    DICTIONARIES_LEXICAL_ENTRIES("dictionaries-lexical-entries"),
    DICTIONARIES_SENSE("dictionaries-sense"),
    MONOGRAPH("monograph"),
    ENTITIES_CHEMISTRY("entities/chemistry"),
    //	ENTITIES_CHEMISTRY("chemistry"),
    FULLTEXT("fulltext"),
    FULLTEXT_ARTICLE_LIGHT_REF("fulltext"),
    FULLTEXT_ARTICLE_LIGHT("fulltext"),
    SHORTTEXT("shorttext"),
    FIGURE("figure"),
    TABLE("table"),
    HEADER("header"),
    HEADER_ARTICLE_LIGHT("header/article/light"),
    HEADER_ARTICLE_LIGHT_REF("header/article/light-ref"),
    HEADER_SDO_3GPP("header/sdo/3gpp"),
    HEADER_SDO_IETF("header/sdo/ietf"),
    NAMES_CITATION("name/citation"),
    NAMES_HEADER("name/header"),
    PATENT_PATENT("patent/patent"),
    PATENT_NPL("patent/npl"),
    PATENT_CITATION("patent/citation"),
    PATENT_STRUCTURE("patent/structure"),
    PATENT_EDIT("patent/edit"),
    ENTITIES_NER("ner"),
    ENTITIES_NERFR("nerfr"),
    ENTITIES_NERSense("nersense"),
    //	ENTITIES_BIOTECH("entities/biotech"),
    ENTITIES_BIOTECH("bio"),
    ASTRO("astro"),
    SOFTWARE("software"),
    DATASEER("dataseer"),
    //ACKNOWLEDGEMENT("acknowledgement"),
    FUNDING_ACKNOWLEDGEMENT("funding-acknowledgement"),
    INFRASTRUCTURE("infrastructure"),
    DUMMY("none"),
    LICENSE("license"),
    COPYRIGHT("copyright");

    //I cannot declare it before
    public static final String DUMMY_FOLDER_LABEL = "none";

    // Flavors are dedicated models variant, but using the same base parser.
    // This is used in particular for scientific or technical documents like standards (SDO)
    // which have a particular overall zoning and/or header, while the rest of the content
    // is similar to other general technical and scientific document
    public enum Flavor {
        BLANK("blank"),
        ARTICLE_LIGHT("article/light"),
        ARTICLE_LIGHT_WITH_REFERENCES("article/light-ref"),
        _3GPP("sdo/3gpp"),
        IETF("sdo/ietf");

        public final String label;

        private Flavor(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getPlainLabel() {
            return label.replace("/", "_");
        }

        public static Flavor fromLabel(String text) {
            for (Flavor f : Flavor.values()) {
                if (f.label.equalsIgnoreCase(text)) {
                    return f;
                }
            }
            return null;
        }

        public String toString() {
            return getLabel();
        }

        public static List<String> getLabels() {
            return Arrays.stream(Flavor.values())
                    .map(Flavor::getLabel)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Absolute path to the model.
     */
    private String modelPath;

    private String folderName;

    private static final ConcurrentMap<String, GrobidModel> models = new ConcurrentHashMap<>();

    GrobidModels(String folderName) {
        if (StringUtils.equals(DUMMY_FOLDER_LABEL, folderName)) {
            modelPath = DUMMY_FOLDER_LABEL;
            this.folderName = DUMMY_FOLDER_LABEL;
            return;
        }

        this.folderName = folderName;
        File path = GrobidProperties.getModelPath(this);
        if (path != null)
            modelPath = path.getAbsolutePath();
    }

    public String getFolderName() {
        return folderName;
    }

    public String getModelPath() {
        if (modelPath == null) {
            File path = GrobidProperties.getModelPath(this);
            if (path != null)
                modelPath = path.getAbsolutePath();
        }
        return modelPath;
    }

    public String getModelName() {
        return folderName.replaceAll("/", "-");
    }

    public String getTemplateName() {
        return StringUtils.substringBefore(folderName, "/") + ".template";
    }

    @Override
    public String toString() {
        return folderName;
    }

    public static GrobidModel getModelFlavor(GrobidModel model, Flavor flavor) {
        if (flavor == null) {
            return model;
        }
        GrobidModel grobidModel = modelFor(model.toString() + "/" + flavor.getLabel().toLowerCase());
        if (flavoredModelExistsOnDisk(grobidModel)) {
            return grobidModel;
        }
        LOGGER.info(
                "The requested flavor "
                        + flavor.getLabel()
                        + " for model "
                        + model.getModelName()
                        + " (resolved engine: "
                        + GrobidProperties.getGrobidEngine(grobidModel)
                        + ") is not available on disk. Defaulting to the standard model. "
                        + "Note: a flavor's engine and architecture are inherited from the "
                        + "base unless explicitly set on the flavor entry.");
        return model;
    }

    /**
     * Returns true when a trained model file/directory for the flavored model exists on disk.
     *
     * The check is engine-aware because Wapiti and DeLFT use different on-disk layouts:
     *   - Wapiti:  <home>/models/<flavor-folder>/model.wapiti          (a file, slash-separated path)
     *   - DeLFT:   <home>/models/<hyphenated-name>-<architecture>/    (a directory, hyphenated name)
     *
     * Both engine and DeLFT architecture are resolved against the flavored model itself.
     * The field-level prefix-fallback in GrobidProperties means each field is inherited
     * from the closest ancestor when the flavor entry omits it, or used as-is when the
     * flavor entry sets it explicitly — so this helper does not need a separate handle
     * on the base model.
     *
     * @param flavoredModel the candidate flavored model whose existence we are testing
     * @return true if a usable model exists on disk for {@code flavoredModel}
     */
    private static boolean flavoredModelExistsOnDisk(GrobidModel flavoredModel) {
        // A flavor that omits `engine:` inherits from the base; a flavor that sets it
        // explicitly overrides — e.g. a Wapiti-only flavor of a DeLFT base must set
        // `engine: "wapiti"` on the flavor entry (see header-sdo-ietf in grobid.yaml).
        GrobidCRFEngine engine = GrobidProperties.getGrobidEngine(flavoredModel);

        if (engine == GrobidCRFEngine.DELFT) {
            String architecture = GrobidProperties.getDelftArchitecture(flavoredModel);
            if (StringUtils.isBlank(architecture)) {
                return false;
            }
            File dlDir = new File(
                    GrobidProperties.getModelPath(),
                    flavoredModel.getModelName() + "-" + architecture);
            return dlDir.isDirectory();
        }

        // Wapiti (and any non-DL engine): file-based check at the flavor folder path.
        String path = flavoredModel.getModelPath();
        return path != null && Files.exists(Paths.get(path));
    }

    public static GrobidModel modelFor(final String name) {
        if (models.isEmpty()) {
            for (GrobidModel model : values())
                models.putIfAbsent(model.getFolderName(), model);
        }

        models.putIfAbsent(name.toString(/* null-check */), new GrobidModel() {
            @Override
            public String getFolderName() {
                return name;
            }

            @Override
            public String getModelPath() {
                File path = GrobidProperties.getModelPath(this);
                if (path == null) {
                    LOGGER.warn("The file path to the " + name + " model is invalid, path is null");
                } else if (!path.exists()) {
                    LOGGER.warn("The file path to the " + name + " model is invalid: " + path.getAbsolutePath());
                }
                if (path == null)
                    return null;
                else
                    return path.getAbsolutePath();
            }

            @Override
            public String getModelName() {
                return getFolderName().replaceAll("/", "-");
            }

            @Override
            public String getTemplateName() {
                return StringUtils.substringBefore(getFolderName(), "/") + ".template";
            }
        });
        return models.get(name);
    }

    public String getName() {
        return name();
    }
}
