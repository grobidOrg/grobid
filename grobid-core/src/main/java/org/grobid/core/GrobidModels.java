package org.grobid.core;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.utilities.GrobidProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.grobid.core.engines.EngineParsers.LOGGER;

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
    COPYRIGHT("copyright"),
    FIGURE_SEGMENTER("figure-segmenter"),
    FIGURE_SEGMENTER_UP("figure-segmenter-up"),
    FIGURE_SEGMENTER_DOWN("figure-segmenter-down");

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
        } else {
            GrobidModel grobidModel = modelFor(model.toString() + "/" + flavor.getLabel().toLowerCase());
            if (!Files.exists(Paths.get(grobidModel.getModelPath()))) {
                LOGGER.info("The requested model flavor " + flavor.getLabel() + " model is not available. Defaulting to the standard model. ");
                return model;
            } else {
                return grobidModel;
            }
        }
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
