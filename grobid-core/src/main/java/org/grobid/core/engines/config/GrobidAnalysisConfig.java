package org.grobid.core.engines.config;

import java.io.File;
import java.util.List;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.Analyzer;

/**
 * A class representing the runtime configuration values needed in the analysis chain
 * TODO: clean up the docs
 * consolidateHeader    - the consolidation option allows GROBID to exploit Crossref or biblio-glutton
 *                             web services for improving header information
 * consolidateCitations - the consolidation option allows GROBID to exploit Crossref or biblio-glutton
 *                             web services for improving citations information
 * consolidateFunders - the consolidation option allows GROBID to exploit Crossref or biblio-glutton
 *                             web services for improving funder information
 * includeRawCitations - the raw bibliographical string is added to parsed results
 * assetPath if not null, the PDF assets (embedded images) will be extracted and
 * saved under the indicated repository path
 * startPage give the starting page to consider in case of segmentation of the
 * PDF, -1 for the first page (default)
 * endPage give the end page to consider in case of segmentation of the
 * PDF, -1 for the last page (default)
 * generateIDs if true, generate random attribute id on the textual elements of
 * the resulting TEI
 * generateTeiCoordinates give the list of TEI elements for which the coordinates
 * of the corresponding element in the original PDF should be included in the 
 * resulting TEI
 * analyzer in case a particular Grobid Analyzer to be used for 
 * tokenizing/filtering text
 */
public class GrobidAnalysisConfig {
    private GrobidAnalysisConfig() {
    }

    // give the starting page to consider in case of segmentation of the
    // PDF, -1 for the first page (default)
    private int startPage = -1;

    // give the end page to consider in case of segmentation of the
    // PDF, -1 for the last page (default)
    private int endPage = -1;

    // if consolidate citations
    private int consolidateCitations = 0;

    // if consolidate header
    private int consolidateHeader = 0;

    // if consolidate funders
    private int consolidateFunders = 0;

    // if the raw affiliation string should be included in the parsed results
    private boolean includeRawAffiliations = false;

    // if the raw bibliographical string should be included in the parsed results
    private boolean includeRawCitations = false;

    // if the raw copyrights/license string should be included in the parsed results
    private boolean includeRawCopyrights = false;

    //if the text marked as <other> in fulltext and header should be retained
    private boolean includeDiscardedText = false;

    /// === TEI-specific settings ==

    // if true, generate random attribute id on the textual elements of
    // the resulting TEI
    private boolean generateTeiIds = false;

    // generates the coordinates in the PDF corresponding
    // to the TEI full text substructures (e.g. reference markers)
    // for the given list of TEI elements
    private List<String> generateTeiCoordinates = null;

    // if true, include image references into TEI
    private boolean generateImageReferences = false;

    private boolean withXslStylesheet = false;

    // if not null, the PDF assets (embedded images) will be extracted
    // and saved under the indicated repository path
    private File pdfAssetPath = null;

    // transform images to PNGs
    private boolean preprocessImages = true;

    private boolean processVectorGraphics = true;

    // a particular Grobid Analyzer to be used for tokenizing/filtering text
    private Analyzer analyzer = null;

    // if true, the TEI text will be segmented into sentences 
    private boolean withSentenceSegmentation = false;

    public boolean isIncludeDiscardedText() {
        return includeDiscardedText;
    }

    public void setIncludeDiscardedText(boolean includeDiscardedText) {
        this.includeDiscardedText = includeDiscardedText;
    }

    private String flavor = null;

    // BUILDER

    public static class GrobidAnalysisConfigBuilder {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig();

        public GrobidAnalysisConfigBuilder() {
        }

        public GrobidAnalysisConfigBuilder(GrobidAnalysisConfig config) {
            // TODO add more properties
            this.config.includeRawAffiliations = config.getIncludeRawAffiliations();
            this.config.includeRawCitations = config.getIncludeRawCitations();
        }

        public GrobidAnalysisConfigBuilder consolidateHeader(int consolidate) {
            config.consolidateHeader = consolidate;
            return this;
        }

        /**
         * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
         *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
         *                    metadata) or 2 (consolidate the citation and inject DOI only)
         */
        public GrobidAnalysisConfigBuilder consolidateCitations(int consolidate) {
            config.consolidateCitations = consolidate;
            return this;
        }

        public GrobidAnalysisConfigBuilder consolidateFunders(int consolidate) {
            config.consolidateFunders = consolidate;
            return this;
        }

        public GrobidAnalysisConfigBuilder includeRawAffiliations(boolean rawAffiliations) {
            config.includeRawAffiliations = rawAffiliations;
            return this;
        }

        public GrobidAnalysisConfigBuilder includeRawCitations(boolean rawCitations) {
            config.includeRawCitations = rawCitations;
            return this;
        }

        public GrobidAnalysisConfigBuilder includeRawCopyrights(boolean rawCopyrights) {
            config.includeRawCopyrights = rawCopyrights;
            return this;
        }

        public GrobidAnalysisConfigBuilder includeDiscardedText(boolean includeDiscardedText) {
            config.includeDiscardedText = includeDiscardedText;
            return this;
        }

        public GrobidAnalysisConfigBuilder startPage(int p) {
            config.startPage = p;
            return this;
        }

        public GrobidAnalysisConfigBuilder endPage(int p) {
            config.endPage = p;
            return this;
        }

        public GrobidAnalysisConfigBuilder generateTeiIds(boolean b) {
            config.generateTeiIds = b;
            return this;
        }

        public GrobidAnalysisConfigBuilder pdfAssetPath(File path) {
            config.pdfAssetPath = path;
            return this;
        }

        public GrobidAnalysisConfigBuilder generateTeiCoordinates(List<String> elements) {
            config.generateTeiCoordinates = elements;
            return this;
        }

        public GrobidAnalysisConfigBuilder withXslStylesheet(boolean b) {
            config.withXslStylesheet = b;
            return this;
        }

        public GrobidAnalysisConfigBuilder withPreprocessImages(boolean b) {
            config.preprocessImages = b;
            return this;
        }

        public GrobidAnalysisConfigBuilder withProcessVectorGraphics(boolean b) {
            config.processVectorGraphics = b;
            return this;
        }

        public GrobidAnalysisConfigBuilder withSentenceSegmentation(boolean b) {
            config.withSentenceSegmentation = b;
            return this;
        }

        public GrobidAnalysisConfigBuilder analyzer(Analyzer a) {
            config.analyzer = a;
            return this;
        }

        public GrobidAnalysisConfigBuilder flavor(GrobidModels.Flavor a) {
            if (a != null) {
                config.flavor = a.getLabel();
            }
            return this;
        }


        public GrobidAnalysisConfig build() {
            postProcessAndValidate();
            return config;
        }

        private void postProcessAndValidate() {
            if (config.getPdfAssetPath() != null) {
                config.generateImageReferences = true;
            }

            if (config.generateImageReferences && config.getPdfAssetPath() == null) {
                throw new InvalidGrobidAnalysisConfig("Generating image references is switched on, but no pdf asset path is provided");
            }
        }

    }

    public static GrobidAnalysisConfigBuilder builder() {
        return new GrobidAnalysisConfigBuilder();
    }

    public static GrobidAnalysisConfigBuilder builder(GrobidAnalysisConfig config) {
        return new GrobidAnalysisConfigBuilder(config);
    }

    public static GrobidAnalysisConfig defaultInstance() {
        return new GrobidAnalysisConfig();
    }

    public String toStringTEI() {
        StringBuilder sb = new StringBuilder();

        sb.append("startPage=").append(startPage);
        sb.append(", endPage=").append(endPage);
        sb.append(", consolidateCitations=").append(consolidateCitations);
        sb.append(", consolidateHeader=").append(consolidateHeader);
        sb.append(", consolidateFunders=").append(consolidateFunders);
        sb.append(", includeRawAffiliations=").append(includeRawAffiliations);
        sb.append(", includeRawCitations=").append(includeRawCitations);
        sb.append(", includeRawCopyrights=").append(includeRawCopyrights);
        sb.append(", generateTeiIds=").append(generateTeiIds);
        sb.append(", generateTeiCoordinates=").append(generateTeiCoordinates);
        sb.append(", flavor=").append(flavor);

        return sb.toString();
    }

    public int getStartPage() {
        return startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public int getConsolidateCitations() {
        return consolidateCitations;
    }

    public int getConsolidateHeader() {
        return consolidateHeader;
    }

    public int getConsolidateFunders() {
        return consolidateFunders;
    }

    public boolean getIncludeRawAffiliations() {
        return includeRawAffiliations;
    }

    public boolean getIncludeRawCitations() {
        return includeRawCitations;
    }

    public boolean getIncludeRawCopyrights() {
        return includeRawCopyrights;
    }

    public boolean isGenerateTeiIds() {
        return generateTeiIds;
    }

    public List<String> getGenerateTeiCoordinates() {
        return generateTeiCoordinates;
    }

    public boolean isGenerateTeiCoordinates() {
        return getGenerateTeiCoordinates() != null && getGenerateTeiCoordinates().size()>0;
    }

    public boolean isGenerateTeiCoordinates(String type) {
        return getGenerateTeiCoordinates() != null && getGenerateTeiCoordinates().contains(type);
    }

    public File getPdfAssetPath() {
        return pdfAssetPath;
    }

    public boolean isWithXslStylesheet() {
        return withXslStylesheet;
    }

    public boolean isGenerateImageReferences() {
        return generateImageReferences;
    }

    public boolean isPreprocessImages() {
        return preprocessImages;
    }

    public boolean isProcessVectorGraphics() {
        return processVectorGraphics;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public boolean isWithSentenceSegmentation() {
        return withSentenceSegmentation;
    }

    public String getFlavor() {
        return flavor;
    }
}
