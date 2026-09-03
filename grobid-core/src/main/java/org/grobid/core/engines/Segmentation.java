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
package org.grobid.core.engines;

import static org.apache.commons.lang3.StringUtils.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

import eugfc.imageio.plugins.PNMRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.GrobidModels.Flavor;
import org.grobid.core.document.BasicStructureBuilder;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorSegmentation;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.TextUtilities;

// for image conversion we're using an ImageIO plugin for PPM format support
// see https://github.com/eug/imageio-pnm
// the jar for this plugin is located in the local repository

/**
 * Realise a high level segmentation of a document into cover page, document header, page footer,
 * page header, document body, bibliographical section, each bibliographical references in
 * the biblio section and finally the possible annexes.
 *
 */
public class Segmentation extends AbstractParser {

    /*
        13 labels for this model:
     		cover page <cover>,
    		document header <header>,
    		page footer <footnote>,
    		page header <headnote>,
            note in margin <marginnote>,
    		document body <body>,
    		bibliographical section <references>,
    		page number <page>,
    		annexes <annex>,
    	    acknowledgement <acknowledgement>,
    	   	availability <availability>,
    	   	funding <funding>,
    	   	conflict of interest / declaration of interest <conflict>,
    	   	author contribution <contribution>,
            other <other>,
    	    toc <toc> -> not yet used because not yet training data for this
    */

    private static final Logger LOGGER = LoggerFactory.getLogger(Segmentation.class);

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
    private FeatureFactory featureFactory = FeatureFactory.getInstance();
    private Flavor flavor = null;

    /**
     * TODO some documentation...
     */
    public Segmentation() {
        super(GrobidModels.SEGMENTATION);
    }

    public Segmentation(Flavor flavor) {
        super(GrobidModels.getModelFlavor(GrobidModels.SEGMENTATION, flavor));
        this.flavor = flavor;
    }

    /**
     * Segment a PDF document into high level zones: cover page, document header,
     * page footer, page header, body, page numbers, biblio section and annexes.
     *
     * @param documentSource     document source
     * @return Document object with segmentation information
     */
    public Document processing(DocumentSource documentSource, GrobidAnalysisConfig config) {
        try {
            Document doc = new Document(documentSource);
            if (config.getAnalyzer() != null)
                doc.setAnalyzer(config.getAnalyzer());
            doc.addTokenizedDocument(config);
            doc = prepareDocument(doc, config);

            // if assets is true, the images are still there under directory pathXML+"_data"
            // we copy them to the assetPath directory

            File assetFile = config.getPdfAssetPath();
            if (assetFile != null) {
                dealWithImages(documentSource, doc, assetFile, config);
            }
            return doc;
        } finally {
            // keep it clean when leaving...
            /*if (config.getPdfAssetPath() == null) {
                // remove the pdfalto tmp file
                DocumentSource.close(documentSource, false, true, true);
            } else*/ {
                // remove the pdfalto tmp files, including the sub-directories
                DocumentSource.close(documentSource, true, true, true);
            }
        }
    }

    public Document processing(String text) {
        Document doc = Document.createFromText(text);
        return prepareDocument(doc);
    }

    public Document prepareDocument(Document doc) {
        warnIfDebugUncaptured("Segmentation.prepareDocument(Document)");
        return prepareDocument(doc, null);
    }

    public Document prepareDocument(Document doc, GrobidAnalysisConfig config) {

        List<LayoutToken> tokenizations = doc.getTokenizations();
        if (tokenizations.size() > GrobidProperties.getPdfTokensMax()) {
            throw new GrobidException(
                    "The document has "
                            + tokenizations.size()
                            + " tokens, but the limit is "
                            + GrobidProperties.getPdfTokensMax(),
                    GrobidExceptionStatus.TOO_MANY_TOKENS);
        }

        doc.produceStatistics();
        String content = getAllFeatured(doc);
        if (isNotEmpty(trim(content))) {
            String labelledResult = labelAndCapture(content, config);
            // set the different sections of the Document object
            if (flavor == Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN) {
                doc = BasicStructureBuilder.generalResultSegmentationTokenLevel(doc, labelledResult, tokenizations);
            } else {
                doc = BasicStructureBuilder.generalResultSegmentation(doc, labelledResult, tokenizations);
            }
        }
        return doc;
    }

    private void dealWithImages(
            DocumentSource documentSource,
            Document doc,
            File assetFile,
            GrobidAnalysisConfig config) {
        if (assetFile != null) {
            // copy the files under the directory pathXML+"_data" (the asset files) into the path specified by assetPath

            if (!assetFile.exists()) {
                // we create it
                if (assetFile.mkdir()) {
                    LOGGER.debug("Directory created: " + assetFile.getPath());
                } else {
                    LOGGER.error("Failed to create directory: " + assetFile.getPath());
                }
            }
            PNMRegistry.registerAllServicesProviders();

            // filter all .jpg and .png files
            File directoryPath = new File(documentSource.getXmlFile().getAbsolutePath() + "_data");
            if (directoryPath.exists()) {
                File[] files = directoryPath.listFiles();
                if (files != null) {
                    int nbFiles = 0;
                    for (final File currFile : files) {
                        if (nbFiles > DocumentSource.PDFALTO_FILES_AMOUNT_LIMIT)
                            break;

                        String toLowerCaseName = currFile.getName().toLowerCase();
                        if (toLowerCaseName.endsWith(".png") || !config.isPreprocessImages()) {
                            try {
                                if (toLowerCaseName.endsWith(".svg")) {
                                    continue;
                                }
                                FileUtils.copyFileToDirectory(currFile, assetFile);
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error(
                                        "Cannot copy file "
                                                + currFile.getAbsolutePath()
                                                + " to "
                                                + assetFile.getAbsolutePath(),
                                        e);
                            }
                        } else if (toLowerCaseName.endsWith(".jpg")
                                || toLowerCaseName.endsWith(".ppm")
                        //	|| currFile.getName().toLowerCase().endsWith(".pbm")
                        ) {

                            String outputFilePath = "";
                            try {
                                final BufferedImage bi = ImageIO.read(currFile);

                                if (toLowerCaseName.endsWith(".jpg")) {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                            toLowerCaseName.replace(".jpg", ".png");
                                }
                                /*else if (currFile.getName().toLowerCase().endsWith(".pbm")) {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                         currFile.getName().toLowerCase().replace(".pbm",".png");
                                }*/
                                else {
                                    outputFilePath = assetFile.getPath() + File.separator +
                                            toLowerCaseName.replace(".ppm", ".png");
                                }
                                ImageIO.write(bi, "png", new File(outputFilePath));
                                nbFiles++;
                            } catch (IOException e) {
                                LOGGER.error(
                                        "Cannot convert file " + currFile.getAbsolutePath() + " to " + outputFilePath,
                                        e);
                            }
                        }
                    }
                }
            }
            // update the path of the image description stored in Document
            if (config.isPreprocessImages()) {
                List<GraphicObject> images = doc.getImages();
                if (images != null) {
                    String subPath = assetFile.getPath();
                    int ind = subPath.lastIndexOf("/");
                    if (ind != -1)
                        subPath = subPath.substring(ind + 1, subPath.length());
                    for (GraphicObject image : images) {
                        String fileImage = image.getFilePath();
                        if (fileImage == null) {
                            continue;
                        }
                        fileImage = fileImage.replace(".ppm", ".png")
                                .replace(".jpg", ".png");
                        ind = fileImage.indexOf("/");
                        image.setFilePath(subPath + fileImage.substring(ind, fileImage.length()));
                    }
                }
            }
        }
    }

    /**
     * Addition of the features at line level for the complete document.
     * <p/>
     * This is an alternative to the token level, where the unit for labeling is the line - so allowing faster
     * processing and involving fewer features.
     * Lexical features becomes line prefix and suffix, the feature text unit is the first 10 characters of the
     * line without space.
     * The dictionary flags are at line level (i.e. the line contains a name mention, a place mention, a year, etc.)
     * Regarding layout features: font, size and style are the one associated to the first token of the line.
     */
    public String getAllFeatured(Document doc) {
        if (flavor == Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN) {
            return getAllTokensFeatured(doc);
        } else {
            return getAllLinesFeatured(doc);
        }
    }

    public String getAllLinesFeatured(Document doc) {

        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        //guaranteeing quality of service. Otherwise, there are some PDF that may contain 300k blocks and thousands of extracted "images" that ruins the performance
        if (blocks.size() > GrobidProperties.getPdfBlocksMax()) {
            throw new GrobidException("Postprocessed document is too big, contains: " + blocks.size(),
                    GrobidExceptionStatus.TOO_MANY_BLOCKS);
        }

        //boolean graphicVector = false;
        //boolean graphicBitmap = false;

        // list of textual patterns at the head and foot of pages which can be re-occur on several pages
        // (typically indicating a publisher foot or head notes)
        Map<String, Integer> patterns = new TreeMap<String, Integer>();
        Map<String, Boolean> firstTimePattern = new TreeMap<String, Boolean>();

        for (Page page : doc.getPages()) {
            // we just look at the two first and last blocks of the page
            if ((page.getBlocks() != null) && (page.getBlocks().size() > 0)) {
                for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        Block block = page.getBlocks().get(blockIndex);
                        String localText = block.getText();
                        if ((localText != null) && (localText.length() > 0)) {
                            String[] lines = localText.split("[\\n\\r]");
                            if (lines.length > 0) {
                                String line = lines[0];
                                String pattern = featureFactory.getPattern(line);
                                if (pattern.length() > 8) {
                                    Integer nb = patterns.get(pattern);
                                    if (nb == null) {
                                        patterns.put(pattern, Integer.valueOf(1));
                                        firstTimePattern.put(pattern, false);
                                    } else
                                        patterns.put(pattern, Integer.valueOf(nb + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        String featuresAsString = getFeatureVectorsAsString(
                doc,
                patterns,
                firstTimePattern);

        return featuresAsString;
    }

    private String getFeatureVectorsAsString(
            Document doc,
            Map<String, Integer> patterns,
            Map<String, Boolean> firstTimePattern) {
        StringBuilder fulltext = new StringBuilder();
        int documentLength = doc.getDocumentLenghtChar();

        String currentFont = null;
        int currentFontSize = -1;

        boolean newPage;
        boolean start = true;
        int mm = 0; // page position
        int nn = 0; // document position
        int pageLength = 0; // length of the current page
        double pageHeight = 0.0;

        // vector for features
        FeaturesVectorSegmentation features;
        FeaturesVectorSegmentation previousFeatures = null;

        for (Page page : doc.getPages()) {
            pageHeight = page.getHeight();
            newPage = true;
            double spacingPreviousBlock = 0.0; // discretized
            double lowestPos = 0.0;
            pageLength = page.getPageLengthChar();
            BoundingBox pageBoundingBox = page.getMainArea();
            mm = 0;
            //endPage = true;

            if (CollectionUtils.isEmpty(page.getBlocks())) {
                continue;
            }

            for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                Block block = page.getBlocks().get(blockIndex);
                /*if (start) {
                    newPage = true;
                    start = false;
                }*/
                boolean graphicVector = false;
                boolean graphicBitmap = false;

                boolean lastPageBlock = false;
                boolean firstPageBlock = false;
                if (blockIndex == page.getBlocks().size() - 1) {
                    lastPageBlock = true;
                }

                if (blockIndex == 0) {
                    firstPageBlock = true;
                }

                //endblock = false;

                /*if (endPage) {
                    newPage = true;
                    mm = 0;
                }*/

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for (GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR
                                || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                if (lowestPos > block.getY()) {
                    // we have a vertical shift, which can be due to a change of column or other particular layout formatting
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0; // default
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (localText == null)
                    continue;

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                        (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                        (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                // is the current block in the main area of the page or not?
                boolean inPageMainArea = true;
                BoundingBox blockBoundingBox = BoundingBox.fromPointAndDimensions(
                        page.getNumber(),
                        block.getX(),
                        block.getY(),
                        block.getWidth(),
                        block.getHeight());
                if (pageBoundingBox == null || (!pageBoundingBox.contains(blockBoundingBox)
                        && !pageBoundingBox.intersect(blockBoundingBox)))
                    inPageMainArea = false;

                String[] lines = localText.split("[\\n\\r]");
                // set the max length of the lines in the block, in number of characters
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }
                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }
                for (int li = 0; li < lines.length; li++) {
                    String line = lines[li];
                    /*boolean firstPageBlock = false;
                    boolean lastPageBlock = false;

                    if (newPage)
                        firstPageBlock = true;
                    if (endPage)
                        lastPageBlock = true;
                    */

                    // for the layout information of the block, we take simply the first layout token
                    LayoutToken token = null;
                    if (tokens.size() > 0)
                        token = tokens.get(0);

                    double coordinateLineY = token.getY();

                    features = new FeaturesVectorSegmentation();
                    features.token = token;
                    features.line = line;

                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        String pattern = featureFactory.getPattern(line);
                        Integer nb = patterns.get(pattern);
                        if ((nb != null) && (nb > 1)) {
                            features.repetitivePattern = true;

                            Boolean firstTimeDone = firstTimePattern.get(pattern);
                            if ((firstTimeDone != null) && !firstTimeDone) {
                                features.firstRepetitivePattern = true;
                                firstTimePattern.put(pattern, true);
                            }
                        }
                    }

                    // we consider the first token of the line as usual lexical token
                    // and the second token of the line as feature
                    StringTokenizer st2 = new StringTokenizer(line, " \t\f\u00A0");
                    // alternatively, use a grobid analyser
                    String text = null;
                    String text2 = null;
                    if (st2.hasMoreTokens())
                        text = st2.nextToken();
                    if (st2.hasMoreTokens())
                        text2 = st2.nextToken();

                    if (text == null)
                        continue;

                    // final sanitization and filtering
                    text = text.replaceAll("[ \n\r]", "");
                    text = text.trim();

                    if ((text.length() == 0) ||
                    //                            (text.equals("\n")) ||
                    //                            (text.equals("\r")) ||
                    //                            (text.equals("\n\r")) ||
                            (TextUtilities.filterLine(line))) {
                        continue;
                    }

                    features.string = text;
                    features.secondString = text2;

                    features.firstPageBlock = firstPageBlock;
                    features.lastPageBlock = lastPageBlock;
                    //features.lineLength = line.length() / LINESCALE;
                    features.lineLength = featureFactory
                            .linearScaling(line.length(), maxLineLength, LINESCALE);

                    features.punctuationProfile = TextUtilities.punctuationProfile(line);

                    if (graphicBitmap) {
                        features.bitmapAround = true;
                    }
                    if (graphicVector) {
                        features.vectorAround = true;
                    }

                    features.lineStatus = null;
                    features.punctType = null;

                    if ((li == 0) ||
                            ((previousFeatures != null) && previousFeatures.blockStatus.equals("BLOCKEND"))) {
                        features.blockStatus = "BLOCKSTART";
                    } else if (li == lines.length - 1) {
                        features.blockStatus = "BLOCKEND";
                        //endblock = true;
                    } else if (features.blockStatus == null) {
                        features.blockStatus = "BLOCKIN";
                    }

                    if (newPage) {
                        features.pageStatus = "PAGESTART";
                        newPage = false;
                        //endPage = false;
                        if (previousFeatures != null)
                            previousFeatures.pageStatus = "PAGEEND";
                    } else {
                        features.pageStatus = "PAGEIN";
                        newPage = false;
                        //endPage = false;
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    if (featureFactory.test_common(text)) {
                        features.commonName = true;
                    }

                    if (featureFactory.test_names(text)) {
                        features.properName = true;
                    }

                    if (featureFactory.test_month(text)) {
                        features.month = true;
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    Matcher m2 = featureFactory.year.matcher(text);
                    if (m2.find()) {
                        features.year = true;
                    }

                    Matcher m3 = featureFactory.email.matcher(text);
                    if (m3.find()) {
                        features.email = true;
                    }

                    Matcher m4 = featureFactory.http.matcher(text);
                    if (m4.find()) {
                        features.http = true;
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    // HERE horizontal information
                    // CENTERED
                    // LEFTAJUSTED
                    // CENTERED

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    //if (features.punctType == null)
                    //    features.punctType = "NOPUNCT";

                    features.relativeDocumentPosition = featureFactory
                            .linearScaling(nn, documentLength, NBBINS_POSITION);
                    //System.out.println(nn + " " + documentLength + " " + NBBINS_POSITION + " " + features.relativeDocumentPosition);
                    features.relativePagePositionChar = featureFactory
                            .linearScaling(mm, pageLength, NBBINS_POSITION);
                    //System.out.println(mm + " " + pageLength + " " + NBBINS_POSITION + " " + features.relativePagePositionChar);
                    int pagePos = featureFactory
                            .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);
                    //System.out.println(coordinateLineY + " " + pageHeight + " " + NBBINS_POSITION + " " + pagePos);
                    if (pagePos > NBBINS_POSITION)
                        pagePos = NBBINS_POSITION;
                    features.relativePagePosition = pagePos;
                    //System.out.println(coordinateLineY + "\t" + pageHeight);

                    if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                                .linearScaling(
                                        spacingPreviousBlock - doc.getMinBlockSpacing(),
                                        doc.getMaxBlockSpacing() - doc.getMinBlockSpacing(),
                                        NBBINS_SPACE);
                    }

                    features.inMainArea = inPageMainArea;

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                                .linearScaling(
                                        density - doc.getMinCharacterDensity(),
                                        doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(),
                                        NBBINS_DENSITY);
                        //System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    // relative horizontal position of the block
                    double pageWidth = page.getWidth();
                    if (pageWidth > 0) {
                        features.relativeBlockHorizontalPosition = featureFactory
                                .linearScaling(block.getX(), pageWidth, NBBINS_POSITION);
                    }

                    // block width ratio relative to main area width (fallback to page width)
                    double referenceWidth = pageWidth;
                    BoundingBox mainArea = page.getMainArea();
                    if (mainArea != null && mainArea.getWidth() > 0) {
                        referenceWidth = mainArea.getWidth();
                    }
                    if (referenceWidth > 0) {
                        features.blockWidthRatio = featureFactory
                                .linearScaling(block.getWidth(), referenceWidth, NBBINS_POSITION);
                    }

                    // additional visual + content features (article/footnotes-refs flavour only)
                    if (flavor == Flavor.ARTICLE_FOOTNOTES_REFS || flavor == Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN) {
                        features.extendedFeatures = true;

                        // relative font size compared to document average
                        double avgFontSize = doc.getAverageFontSize();
                        if (avgFontSize > 0 && newFontSize > 0) {
                            features.relativeFontSize = featureFactory
                                    .linearScaling(newFontSize, (int) (avgFontSize * 2), NBBINS_POSITION);
                        }

                        // distance from page bottom
                        double blockBottom = block.getY() + block.getHeight();
                        double distFromBottom = pageHeight - blockBottom;
                        if (pageHeight > 0 && distFromBottom >= 0) {
                            features.distanceFromPageBottom = featureFactory
                                    .linearScaling(distFromBottom, pageHeight, NBBINS_POSITION);
                        }

                        // parentheses count in line
                        int parenthesesCount = 0;
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) == '(')
                                parenthesesCount++;
                        }
                        features.parenthesesCountInLine = featureFactory
                                .linearScaling(parenthesesCount, 10, NBBINS_DENSITY);

                        // comma count in line
                        int commaCount = 0;
                        for (int i = 0; i < line.length(); i++) {
                            if (line.charAt(i) == ',')
                                commaCount++;
                        }
                        features.commaCountInLine = featureFactory
                                .linearScaling(commaCount, 15, NBBINS_DENSITY);

                        // capitalized word count in line (excluding first word)
                        String[] words = line.split("\\s+");
                        int capitalizedCount = 0;
                        for (int i = 1; i < words.length; i++) {
                            if (words[i].length() > 0 && Character.isUpperCase(words[i].charAt(0))) {
                                capitalizedCount++;
                            }
                        }
                        features.capitalizedWordCountInLine = featureFactory
                                .linearScaling(capitalizedCount, 15, NBBINS_DENSITY);
                    }

                    if (previousFeatures != null) {
                        String vector = previousFeatures.printVector();
                        fulltext.append(vector);
                    }
                    previousFeatures = features;
                }

                //System.out.println((spacingPreviousBlock-doc.getMinBlockSpacing()) + " " + (doc.getMaxBlockSpacing()-doc.getMinBlockSpacing()) + " " + NBBINS_SPACE + " "
                //    + featureFactory.linearScaling(spacingPreviousBlock-doc.getMinBlockSpacing(), doc.getMaxBlockSpacing()-doc.getMinBlockSpacing(), NBBINS_SPACE));

                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();

                // update page-level and document-level positions
                if (tokens != null) {
                    mm += tokens.size();
                    nn += tokens.size();
                }
            }
        }
        if (previousFeatures != null)
            fulltext.append(previousFeatures.printVector());

        return fulltext.toString();
    }

    /**
     * Token-level feature extraction. Same as getAllLinesFeatured() but generates one
     * feature vector per token instead of per line. Each token uses its own LayoutToken's
     * layout properties (font, size, bold, italic, position).
     */
    public String getAllTokensFeatured(Document doc) {
        List<Block> blocks = doc.getBlocks();
        if ((blocks == null) || blocks.size() == 0) {
            return null;
        }

        if (blocks.size() > GrobidProperties.getPdfBlocksMax()) {
            throw new GrobidException("Postprocessed document is too big, contains: " + blocks.size(),
                    GrobidExceptionStatus.TOO_MANY_BLOCKS);
        }

        // repetitive pattern detection (same as line-level)
        Map<String, Integer> patterns = new TreeMap<String, Integer>();
        Map<String, Boolean> firstTimePattern = new TreeMap<String, Boolean>();

        for (Page page : doc.getPages()) {
            if ((page.getBlocks() != null) && (page.getBlocks().size() > 0)) {
                for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                    if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                        Block block = page.getBlocks().get(blockIndex);
                        String localText = block.getText();
                        if ((localText != null) && (localText.length() > 0)) {
                            String[] lines = localText.split("[\\n\\r]");
                            if (lines.length > 0) {
                                String line = lines[0];
                                String pattern = featureFactory.getPattern(line);
                                if (pattern.length() > 8) {
                                    Integer nb = patterns.get(pattern);
                                    if (nb == null) {
                                        patterns.put(pattern, Integer.valueOf(1));
                                        firstTimePattern.put(pattern, false);
                                    } else
                                        patterns.put(pattern, Integer.valueOf(nb + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        return getFeatureVectorsTokenLevelAsString(doc, patterns, firstTimePattern);
    }

    private String getFeatureVectorsTokenLevelAsString(
            Document doc,
            Map<String, Integer> patterns,
            Map<String, Boolean> firstTimePattern) {
        StringBuilder fulltext = new StringBuilder();
        int documentLength = doc.getDocumentLenghtChar();

        String currentFont = null;
        int currentFontSize = -1;

        boolean newPage;
        int mm = 0; // page position
        int nn = 0; // document position
        int pageLength = 0;
        double pageHeight = 0.0;

        FeaturesVectorSegmentation features;
        FeaturesVectorSegmentation previousFeatures = null;

        for (Page page : doc.getPages()) {
            pageHeight = page.getHeight();
            newPage = true;
            double spacingPreviousBlock = 0.0;
            double lowestPos = 0.0;
            pageLength = page.getPageLengthChar();
            BoundingBox pageBoundingBox = page.getMainArea();
            mm = 0;

            if (CollectionUtils.isEmpty(page.getBlocks())) {
                continue;
            }

            for (int blockIndex = 0; blockIndex < page.getBlocks().size(); blockIndex++) {
                Block block = page.getBlocks().get(blockIndex);
                boolean graphicVector = false;
                boolean graphicBitmap = false;

                boolean lastPageBlock = (blockIndex == page.getBlocks().size() - 1);
                boolean firstPageBlock = (blockIndex == 0);

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for (GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR
                                || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                if (lowestPos > block.getY()) {
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0;
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (localText == null)
                    continue;

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                        (block.getText() != null) && (!block.getText().contains("@PAGE")) &&
                        (!block.getText().contains("@IMAGE")))
                    density = (double) block.getText().length() / (block.getHeight() * block.getWidth());

                // is the current block in the main area of the page or not?
                boolean inPageMainArea = true;
                BoundingBox blockBoundingBox = BoundingBox.fromPointAndDimensions(
                        page.getNumber(),
                        block.getX(),
                        block.getY(),
                        block.getWidth(),
                        block.getHeight());
                if (pageBoundingBox == null || (!pageBoundingBox.contains(blockBoundingBox)
                        && !pageBoundingBox.intersect(blockBoundingBox)))
                    inPageMainArea = false;

                // pre-compute line-level features from block text
                String[] lines = localText.split("[\\n\\r]");
                int maxLineLength = 0;
                for (int p = 0; p < lines.length; p++) {
                    if (lines[p].length() > maxLineLength)
                        maxLineLength = lines[p].length();
                }

                List<LayoutToken> tokens = block.getTokens();
                if ((tokens == null) || (tokens.size() == 0)) {
                    continue;
                }

                // group tokens into lines and identify which line each token belongs to
                // also compute per-line features that are shared across tokens
                List<List<LayoutToken>> tokensByLine = new ArrayList<>();
                List<LayoutToken> currentLine = new ArrayList<>();
                for (LayoutToken tok : tokens) {
                    if (tok.getText() != null && tok.getText().equals("\n")) {
                        if (!currentLine.isEmpty()) {
                            tokensByLine.add(currentLine);
                            currentLine = new ArrayList<>();
                        }
                    } else {
                        currentLine.add(tok);
                    }
                }
                if (!currentLine.isEmpty()) {
                    tokensByLine.add(currentLine);
                }

                // determine repetitive pattern (at block level, using first line)
                boolean blockRepetitivePattern = false;
                boolean blockFirstRepetitivePattern = false;
                if ((blockIndex < 2) || (blockIndex > page.getBlocks().size() - 2)) {
                    if (lines.length > 0) {
                        String pattern = featureFactory.getPattern(lines[0]);
                        Integer nb = patterns.get(pattern);
                        if ((nb != null) && (nb > 1)) {
                            blockRepetitivePattern = true;
                            Boolean firstTimeDone = firstTimePattern.get(pattern);
                            if ((firstTimeDone != null) && !firstTimeDone) {
                                blockFirstRepetitivePattern = true;
                                firstTimePattern.put(pattern, true);
                            }
                        }
                    }
                }

                int lineIndex = 0;
                for (List<LayoutToken> lineTokens : tokensByLine) {
                    if (lineTokens.isEmpty())
                        continue;

                    // compute per-line text for shared line-level features
                    StringBuilder lineTextBuilder = new StringBuilder();
                    for (LayoutToken lt : lineTokens) {
                        if (lt.getText() != null)
                            lineTextBuilder.append(lt.getText());
                    }
                    String lineText = lineTextBuilder.toString();

                    if (lineText.trim().length() == 0 || TextUtilities.filterLine(lineText)) {
                        lineIndex++;
                        continue;
                    }

                    // line-level features shared across all tokens on this line
                    String punctuationProfile = TextUtilities.punctuationProfile(lineText);
                    int lineLength = featureFactory.linearScaling(lineText.length(), maxLineLength, LINESCALE);

                    // second token of the line for context (same semantics as line-level)
                    String secondTokenText = null;
                    int nonSpaceCount = 0;
                    for (LayoutToken lt : lineTokens) {
                        String t = lt.getText();
                        if (t != null && t.trim().length() > 0 && !t.equals("\n") && !t.equals("\r")) {
                            nonSpaceCount++;
                            if (nonSpaceCount == 2) {
                                secondTokenText = t.trim();
                                break;
                            }
                        }
                    }

                    // extended features: per-line counts
                    int parenthesesCount = 0;
                    int commaCount = 0;
                    int capitalizedWordCount = 0;
                    if (flavor == Flavor.ARTICLE_FOOTNOTES_REFS || flavor == Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN) {
                        for (int i = 0; i < lineText.length(); i++) {
                            if (lineText.charAt(i) == '(')
                                parenthesesCount++;
                            if (lineText.charAt(i) == ',')
                                commaCount++;
                        }
                        String[] words = lineText.split("\\s+");
                        for (int i = 1; i < words.length; i++) {
                            if (words[i].length() > 0 && Character.isUpperCase(words[i].charAt(0))) {
                                capitalizedWordCount++;
                            }
                        }
                    }

                    // count non-whitespace tokens on this line for lineStatus calculation
                    List<LayoutToken> nonSpaceTokens = new ArrayList<>();
                    for (LayoutToken lt : lineTokens) {
                        String t = lt.getText();
                        if (t != null && t.trim().length() > 0 && !t.equals("\n") && !t.equals("\r")) {
                            nonSpaceTokens.add(lt);
                        }
                    }

                    for (int ti = 0; ti < nonSpaceTokens.size(); ti++) {
                        LayoutToken token = nonSpaceTokens.get(ti);
                        String text = token.getText();
                        if (text == null)
                            continue;
                        text = text.replaceAll("[ \n\r]", "").trim();
                        if (text.length() == 0)
                            continue;

                        features = new FeaturesVectorSegmentation();
                        features.token = token;
                        features.line = lineText;
                        features.string = text;
                        features.secondString = secondTokenText;

                        // line status
                        if (ti == 0)
                            features.lineStatus = "LINESTART";
                        else if (ti == nonSpaceTokens.size() - 1)
                            features.lineStatus = "LINEEND";
                        else
                            features.lineStatus = "LINEIN";

                        // block status
                        if (lineIndex == 0 && ti == 0) {
                            features.blockStatus = "BLOCKSTART";
                        } else if (lineIndex == tokensByLine.size() - 1 && ti == nonSpaceTokens.size() - 1) {
                            features.blockStatus = "BLOCKEND";
                        } else {
                            features.blockStatus = "BLOCKIN";
                        }

                        // page status
                        if (newPage) {
                            features.pageStatus = "PAGESTART";
                            newPage = false;
                            if (previousFeatures != null)
                                previousFeatures.pageStatus = "PAGEEND";
                        } else {
                            features.pageStatus = "PAGEIN";
                        }

                        features.firstPageBlock = firstPageBlock;
                        features.lastPageBlock = lastPageBlock;

                        // line-level features (shared)
                        features.lineLength = lineLength;
                        features.punctuationProfile = punctuationProfile;

                        if (graphicBitmap)
                            features.bitmapAround = true;
                        if (graphicVector)
                            features.vectorAround = true;

                        features.repetitivePattern = blockRepetitivePattern;
                        features.firstRepetitivePattern = blockFirstRepetitivePattern;

                        // per-token lexical features
                        if (text.length() == 1)
                            features.singleChar = true;
                        if (Character.isUpperCase(text.charAt(0)))
                            features.capitalisation = "INITCAP";
                        if (featureFactory.test_all_capital(text))
                            features.capitalisation = "ALLCAP";
                        if (featureFactory.test_digit(text))
                            features.digit = "CONTAINSDIGITS";
                        if (featureFactory.test_common(text))
                            features.commonName = true;
                        if (featureFactory.test_names(text))
                            features.properName = true;
                        if (featureFactory.test_month(text))
                            features.month = true;

                        Matcher m = featureFactory.isDigit.matcher(text);
                        if (m.find())
                            features.digit = "ALLDIGIT";

                        Matcher m2 = featureFactory.year.matcher(text);
                        if (m2.find())
                            features.year = true;

                        Matcher m3 = featureFactory.email.matcher(text);
                        if (m3.find())
                            features.email = true;

                        Matcher m4 = featureFactory.http.matcher(text);
                        if (m4.find())
                            features.http = true;

                        // punctuation type (per-token)
                        Matcher mp = featureFactory.isPunct.matcher(text);
                        if (mp.find())
                            features.punctType = "PUNCT";
                        if (text.equals("(") || text.equals("["))
                            features.punctType = "OPENBRACKET";
                        else if (text.equals(")") || text.equals("]"))
                            features.punctType = "ENDBRACKET";
                        else if (text.equals("."))
                            features.punctType = "DOT";
                        else if (text.equals(","))
                            features.punctType = "COMMA";
                        else if (text.equals("-"))
                            features.punctType = "HYPHEN";
                        else if (text.equals("\"") || text.equals("'") || text.equals("`"))
                            features.punctType = "QUOTE";

                        if (features.punctType == null)
                            features.punctType = "NOPUNCT";

                        // per-token font features (using this token's own LayoutToken)
                        if (currentFont == null) {
                            currentFont = token.getFont();
                            features.fontStatus = "NEWFONT";
                        } else if (!currentFont.equals(token.getFont())) {
                            currentFont = token.getFont();
                            features.fontStatus = "NEWFONT";
                        } else
                            features.fontStatus = "SAMEFONT";

                        int newFontSize = (int) token.getFontSize();
                        if (currentFontSize == -1) {
                            currentFontSize = newFontSize;
                            features.fontSize = "HIGHERFONT";
                        } else if (currentFontSize == newFontSize) {
                            features.fontSize = "SAMEFONTSIZE";
                        } else if (currentFontSize < newFontSize) {
                            features.fontSize = "HIGHERFONT";
                            currentFontSize = newFontSize;
                        } else if (currentFontSize > newFontSize) {
                            features.fontSize = "LOWERFONT";
                            currentFontSize = newFontSize;
                        }

                        if (token.isBold())
                            features.bold = true;
                        if (token.isItalic())
                            features.italic = true;

                        if (features.capitalisation == null)
                            features.capitalisation = "NOCAPS";
                        if (features.digit == null)
                            features.digit = "NODIGIT";

                        // per-token position features
                        features.relativeDocumentPosition = featureFactory
                                .linearScaling(nn, documentLength, NBBINS_POSITION);
                        features.relativePagePositionChar = featureFactory
                                .linearScaling(mm, pageLength, NBBINS_POSITION);

                        double coordinateLineY = token.getY();
                        int pagePos = featureFactory
                                .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);
                        if (pagePos > NBBINS_POSITION)
                            pagePos = NBBINS_POSITION;
                        features.relativePagePosition = pagePos;

                        // block-level features (shared)
                        if (spacingPreviousBlock != 0.0) {
                            features.spacingWithPreviousBlock = featureFactory
                                    .linearScaling(
                                            spacingPreviousBlock - doc.getMinBlockSpacing(),
                                            doc.getMaxBlockSpacing() - doc.getMinBlockSpacing(),
                                            NBBINS_SPACE);
                        }

                        features.inMainArea = inPageMainArea;

                        if (density != -1.0) {
                            features.characterDensity = featureFactory
                                    .linearScaling(
                                            density - doc.getMinCharacterDensity(),
                                            doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(),
                                            NBBINS_DENSITY);
                        }

                        double pageWidth = page.getWidth();
                        if (pageWidth > 0) {
                            features.relativeBlockHorizontalPosition = featureFactory
                                    .linearScaling(block.getX(), pageWidth, NBBINS_POSITION);
                        }

                        double referenceWidth = pageWidth;
                        BoundingBox mainArea = page.getMainArea();
                        if (mainArea != null && mainArea.getWidth() > 0) {
                            referenceWidth = mainArea.getWidth();
                        }
                        if (referenceWidth > 0) {
                            features.blockWidthRatio = featureFactory
                                    .linearScaling(block.getWidth(), referenceWidth, NBBINS_POSITION);
                        }

                        // extended features
                        if (flavor == Flavor.ARTICLE_FOOTNOTES_REFS
                                || flavor == Flavor.ARTICLE_FOOTNOTES_REFS_TOKEN) {
                            features.extendedFeatures = true;

                            double avgFontSize = doc.getAverageFontSize();
                            if (avgFontSize > 0 && newFontSize > 0) {
                                features.relativeFontSize = featureFactory
                                        .linearScaling(newFontSize, (int) (avgFontSize * 2), NBBINS_POSITION);
                            }

                            double blockBottom = block.getY() + block.getHeight();
                            double distFromBottom = pageHeight - blockBottom;
                            if (pageHeight > 0 && distFromBottom >= 0) {
                                features.distanceFromPageBottom = featureFactory
                                        .linearScaling(distFromBottom, pageHeight, NBBINS_POSITION);
                            }

                            features.parenthesesCountInLine = featureFactory
                                    .linearScaling(parenthesesCount, 10, NBBINS_DENSITY);
                            features.commaCountInLine = featureFactory
                                    .linearScaling(commaCount, 15, NBBINS_DENSITY);
                            features.capitalizedWordCountInLine = featureFactory
                                    .linearScaling(capitalizedWordCount, 15, NBBINS_DENSITY);
                        }

                        if (previousFeatures != null) {
                            String vector = previousFeatures.printVectorTokenLevel();
                            if (vector != null)
                                fulltext.append(vector);
                        }
                        previousFeatures = features;

                        // update positions per token
                        nn++;
                        mm++;
                    }

                    lineIndex++;
                }

                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();
            }
        }
        if (previousFeatures != null) {
            String vector = previousFeatures.printVectorTokenLevel();
            if (vector != null)
                fulltext.append(vector);
        }

        return fulltext.toString();
    }

    /**
     * Process the content of the specified pdf and format the result as training data.
     *
     * @param inputFile    input file
     * @param pathFullText path to fulltext
     * @param pathTEI      path to TEI
     */
    public void createTrainingSegmentation(
            String inputFile,
            String pathFullText,
            String pathTEI) {
        DocumentSource documentSource = null;
        try {
            File file = new File(inputFile);

            //documentSource = DocumentSource.fromPdf(file);
            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            // the sanitized base name is used for the training file names, while the xml:id
            // carries an additional leading '_' when required to be a valid NCName
            String baseName = TextUtilities.sanitizeFileName(file.getName().replaceAll("(?i)\\.pdf$", ""));
            String xmlId = TextUtilities.sanitizeXmlId(baseName);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = getAllFeatured(doc);
            //List<LayoutToken> tokenizations = doc.getTokenizationsFulltext();
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write the full text untagged (but featurized)
            String outPathFulltext = pathFullText + File.separator + baseName + ".training.segmentation";
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false),
                    StandardCharsets.UTF_8)) {
                writer.write(fulltext + "\n");
            }

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(txtline.getText());
            }
            String outPathRawtext = pathFullText + File.separator + baseName + ".training.segmentation.rawtxt";
            FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), StandardCharsets.UTF_8);

            if (isNotBlank(fulltext)) {
                String rese = label(fulltext);
                StringBuffer bufferFulltext = trainingExtraction(rese, tokenizations, doc);

                // detect the actual document language so the training TEI is annotated
                // accordingly instead of always assuming English (issue #671); fall back
                // to "en" when detection is inconclusive
                String lang = detectLanguageOrDefault(rawtxt.toString());

                // write the TEI file to reflect the exact layout of the text as extracted from the pdf
                File teiFile = new File(pathTEI + File.separator + baseName + ".training.segmentation.tei.xml");
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(teiFile, false),
                        StandardCharsets.UTF_8)) {
                    writer.write(
                            "<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                                    + xmlId
                                    + "\"/>\n\t</teiHeader>\n\t<text xml:lang=\""
                                    + lang
                                    + "\">\n");

                    writer.write(bufferFulltext.toString());
                    writer.write("\n\t</text>\n</tei>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training"
                    +
                    " data generation for segmentation model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Detect the language of the given text for annotating generated training data,
     * returning its ISO code or "en" when detection is unavailable or inconclusive
     * (issue #671). Detection failures are non-fatal: training data generation should
     * never abort because the language model is missing.
     */
    private String detectLanguageOrDefault(String text) {
        if (isNotBlank(text)) {
            try {
                Language langID = languageUtilities.runLanguageId(text);
                if (langID != null && isNotBlank(langID.getLang())) {
                    return langID.getLang();
                }
            } catch (Exception e) {
                LOGGER.warn("Language detection failed while generating training data, defaulting to 'en'.", e);
            }
        }
        return "en";
    }

    /**
     * Get the content of the pdf and produce a blank training data TEI file, i.e. a text only TEI file
     * without any tags. This is useful to start from scratch the creation of training data at the same
     * level as the segmentation parser.
     *
     * @param inputFile    input file
     * @param pathFullText path to fulltext
     * @param pathTEI      path to TEI
     */
    public void createBlankTrainingData(
            File file,
            String pathFullText,
            String pathTEI) {
        DocumentSource documentSource = null;
        try {
            //File file = new File(inputFile);

            //documentSource = DocumentSource.fromPdf(file);
            documentSource = DocumentSource.fromPdf(file, -1, -1, true, true, true);
            Document doc = new Document(documentSource);

            // the sanitized base name is used for the training file names, while the xml:id
            // carries an additional leading '_' when required to be a valid NCName
            String baseName = TextUtilities.sanitizeFileName(file.getName().replaceAll("(?i)\\.pdf$", ""));
            String xmlId = TextUtilities.sanitizeXmlId(baseName);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = getAllFeatured(doc);
            //List<LayoutToken> tokenizations = doc.getTokenizationsFulltext();
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write the full text untagged (but featurized)
            String outPathFulltext = pathFullText + File.separator + baseName + ".training.blank";
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false),
                    StandardCharsets.UTF_8)) {
                writer.write(fulltext + "\n");
            }

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(TextUtilities.HTMLEncode(txtline.getText()));
            }

            fulltext = rawtxt.toString();
            if (isNotBlank(fulltext)) {
                // detect the actual document language instead of always assuming English (issue #671)
                String lang = detectLanguageOrDefault(fulltext);

                // write the TEI file to reflect the exact layout of the text as extracted from the pdf
                File teiFile = new File(pathTEI + File.separator + baseName + ".training.blank.tei.xml");
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(teiFile, false),
                        StandardCharsets.UTF_8)) {
                    writer.write(
                            "<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                                    + xmlId
                                    + "\"/>\n\t</teiHeader>\n\t<text xml:lang=\""
                                    + lang
                                    + "\">\n");

                    writer.write(fulltext);
                    writer.write("\n\t</text>\n</tei>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training"
                    +
                    " data generation for segmentation model.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Extract results from a labelled full text in the training format without any string modification.
     *
     * @param result        result
     * @param tokenizations toks
     * @return extraction
     */
    public StringBuffer trainingExtraction(
            String result,
            List<LayoutToken> tokenizations,
            Document doc) {
        // this is the main buffer for the whole full text
        StringBuffer buffer = new StringBuffer();
        try {
            List<Block> blocks = doc.getBlocks();
            int currentBlockIndex = 0;
            int indexLine = 0;

            StringTokenizer st = new StringTokenizer(result, "\n");
            String s1 = null; // current label/tag
            String s2 = null; // current lexical token
            String s3 = null; // current second lexical token
            String lastTag = null;

            // current token position
            int p = 0;
            boolean start = true;

            while (st.hasMoreTokens()) {
                boolean addSpace = false;
                String tok = st.nextToken().trim();
                String line = null; // current line

                if (tok.length() == 0) {
                    continue;
                }
                StringTokenizer stt = new StringTokenizer(tok, " \t");
                List<String> localFeatures = new ArrayList<String>();
                int i = 0;

                boolean newLine = true;
                int ll = stt.countTokens();
                while (stt.hasMoreTokens()) {
                    String s = stt.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // lexical token
                    } else if (i == 1) {
                        s3 = TextUtilities.HTMLEncode(s); // second lexical token
                    } else if (i == ll - 1) {
                        s1 = s; // current label
                    } else {
                        localFeatures.add(s); // we keep the feature values in case they appear useful
                    }
                    i++;
                }

                // as we process the document segmentation line by line, we don't use the usual
                // tokenization to rebuild the text flow, but we get each line again from the
                // text stored in the document blocks (similarly as when generating the features)
                line = null;
                while ((line == null) && (currentBlockIndex < blocks.size())) {
                    Block block = blocks.get(currentBlockIndex);
                    List<LayoutToken> tokens = block.getTokens();
                    if (tokens == null) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    String localText = block.getText();
                    if ((localText == null) || (localText.trim().length() == 0)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    }
                    //String[] lines = localText.split("\n");
                    String[] lines = localText.split("[\\n\\r]");
                    if ((lines.length == 0) || (indexLine >= lines.length)) {
                        currentBlockIndex++;
                        indexLine = 0;
                        continue;
                    } else {
                        line = lines[indexLine];
                        indexLine++;
                        if (line.trim().length() == 0) {
                            line = null;
                            continue;
                        }

                        if (TextUtilities.filterLine(line)) {
                            line = null;
                            continue;
                        }
                    }
                }

                line = TextUtilities.HTMLEncode(line);

                if (newLine && !start) {
                    buffer.append("<lb/>");
                }

                String lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                String currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                //boolean closeParagraph = false;
                if (lastTag != null) {
                    //closeParagraph =
                    testClosingTag(buffer, currentTag0, lastTag0, s1);
                }

                boolean output;

                output = writeField(buffer, line, s1, lastTag0, s2, "<header>", "<front>", addSpace, 3);
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<other>", "", addSpace, 3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<headnote>",
                            "<note place=\"headnote\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<footnote>",
                            "<note place=\"footnote\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<marginnote>",
                            "<note place=\"margin\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<page>", "<page>", addSpace, 3);
                }
                if (!output) {
                    //output = writeFieldBeginEnd(buffer, s1, lastTag0, s2, "<reference>", "<listBibl>", addSpace, 3);
                    output = writeField(buffer, line, s1, lastTag0, s2, "<references>", "<listBibl>", addSpace, 3);
                }
                if (!output) {
                    //output = writeFieldBeginEnd(buffer, s1, lastTag0, s2, "<body>", "<body>", addSpace, 3);
                    output = writeField(buffer, line, s1, lastTag0, s2, "<body>", "<body>", addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<cover>", "<titlePage>", addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<toc>", "<div type=\"toc\">", addSpace, 3);
                }
                if (!output) {
                    output = writeField(buffer, line, s1, lastTag0, s2, "<annex>", "<div type=\"annex\">", addSpace, 3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<acknowledgement>",
                            "<div type=\"acknowledgement\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<availability>",
                            "<div type=\"availability\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<funding>",
                            "<div type=\"funding\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<conflict>",
                            "<div type=\"conflict\">",
                            addSpace,
                            3);
                }
                if (!output) {
                    output = writeField(
                            buffer,
                            line,
                            s1,
                            lastTag0,
                            s2,
                            "<contribution>",
                            "<div type=\"contribution\">",
                            addSpace,
                            3);
                }
                lastTag = s1;

                if (!st.hasMoreTokens()) {
                    if (lastTag != null) {
                        testClosingTag(buffer, "", currentTag0, s1);
                    }
                }
                if (start) {
                    start = false;
                }
            }

            return buffer;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * TODO some documentation...
     *
     * @param buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    private boolean writeField(
            StringBuffer buffer,
            String line,
            String s1,
            String lastTag0,
            String s2,
            String field,
            String outField,
            boolean addSpace,
            int nbIndent) {
        boolean result = false;
        // filter the output path
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            line = line.replace("@BULLET", "\u2022");
            // if previous and current tag are the same, we output the token
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                buffer.append(line);
            } else if (lastTag0 == null) {
                // if previous tagname is null, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            } else {
                // new opening tag, we output the opening xml tag
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(line);
            } /*else {
                // otherwise we continue by outputting the token
                buffer.append(line);
              }*/
        }
        return result;
    }

    /**
     * This is for writing fields for fields where begin and end of field matter, like paragraph or item
     *
     * @param buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    /*private boolean writeFieldBeginEnd(StringBuffer buffer,
                                       String s1,
                                       String lastTag0,
                                       String s2,
                                       String field,
                                       String outField,
                                       boolean addSpace,
                                       int nbIndent) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (lastTag0.equals("I-" + field)) {
                if (addSpace)
                    buffer.append(" " + s2);
                else
                    buffer.append(s2);
            } /*else if (lastTag0.equals(field) && s1.equals(field)) {
                if (addSpace)
                    buffer.append(" " + s2);
                else
                    buffer.append(s2);
            } else if (!lastTag0.equals("<citation_marker>") && !lastTag0.equals("<figure_marker>")
                    && !lastTag0.equals("<figure>") && !lastTag0.equals("<reference_marker>")) {
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField + s2);
            }
    		else {
                if (addSpace)
                    buffer.append(" " + s2);
                else
                    buffer.append(s2);
            }
        }
        return result;
    }*/

    /**
     * TODO some documentation
     *
     * @param buffer
     * @param currentTag0
     * @param lastTag0
     * @param currentTag
     * @return
     */
    private boolean testClosingTag(
            StringBuffer buffer,
            String currentTag0,
            String lastTag0,
            String currentTag) {
        boolean res = false;
        // reference_marker and citation_marker are two exceptions because they can be embedded

        if (!currentTag0.equals(lastTag0)) {
            /*if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
                return res;
            }*/

            res = false;
            // we close the current tag
            if (lastTag0.equals("<header>")) {
                buffer.append("</front>\n\n");
                res = true;
            } else if (lastTag0.equals("<body>")) {
                buffer.append("</body>\n\n");
                res = true;
            } else if (lastTag0.equals("<headnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<footnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<marginnote>")) {
                buffer.append("</note>\n\n");
                res = true;
            } else if (lastTag0.equals("<references>")) {
                buffer.append("</listBibl>\n\n");
                res = true;
            } else if (lastTag0.equals("<page>")) {
                buffer.append("</page>\n\n");
                res = true;
            } else if (lastTag0.equals("<cover>")) {
                buffer.append("</titlePage>\n\n");
                res = true;
            } else if (lastTag0.equals("<toc>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<annex>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<acknowledgement>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<availability>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<conflict>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<contribution>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<funding>")) {
                buffer.append("</div>\n\n");
                res = true;
            } else if (lastTag0.equals("<other>")) {
                buffer.append("\n\n");
            } else {
                res = false;
            }

        }
        return res;
    }

    @Override
    public void close() throws IOException {
        super.close();
        // ...
    }

}
