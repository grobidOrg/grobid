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
package org.grobid.core.data;

import static org.grobid.core.document.TEIFormatter.generateDiscardedTextNote;
import static org.grobid.core.document.xml.XmlBuilderUtils.addXmlId;
import static org.grobid.core.document.xml.XmlBuilderUtils.textNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.document.Document;
import org.grobid.core.document.TEIFormatter;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.*;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for representing a figure.
 *
 */
public class Figure {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Figure.class);

    public static final Predicate<GraphicObject> GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.BITMAP;
        }
    };

    public static final Predicate<GraphicObject> VECTOR_BOX_GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.VECTOR_BOX;
        }
    };

    public static final Predicate<GraphicObject> BOXED_GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.BITMAP
                    || graphicObject.getType() == GraphicObjectType.VECTOR_BOX;
        }
    };
    protected StringBuilder caption = null;
    protected List<LayoutToken> captionLayoutTokens = new ArrayList<>();
    protected String labeledCaption = null;
    protected StringBuilder header = null;
    protected StringBuilder content = null;
    protected StringBuilder label = null;

    protected String id = null;
    protected URI uri = null;
    protected int start = -1; // start position in the full text tokenization
    protected int end = -1; // end position in the full text tokenization
    protected LayoutToken startToken = null; // start layout token
    protected LayoutToken endToken = null; // end layout token
    private List<BoundingBox> textArea;
    private List<LayoutToken> layoutTokens;

    private List<List<LayoutToken>> discardedPiecesTokens = new ArrayList<>();

    // coordinates
    private int page = -1;
    private double y = 0.0;
    private double x = 0.0;
    private double width = 0.0;
    private double height = 0.0;

    // list of graphic objects corresponding to the figure
    protected List<GraphicObject> graphicObjects = null;
    private SortedSet<Integer> blockPtrs;

    public Figure() {
        caption = new StringBuilder();
        header = new StringBuilder();
        content = new StringBuilder();
        label = new StringBuilder();
    }

    public void appendHeader(String head) {
        header.append(head);
    }

    public String getHeader() {
        return header.toString();
    }

    public void appendCaption(String cap) {
        caption.append(cap);
    }

    public void appendCaptionLayoutTokens(List<LayoutToken> layoutTokens) {
        captionLayoutTokens.addAll(layoutTokens);
    }

    public String getCaption() {
        return caption.toString();
    }

    public List<LayoutToken> getCaptionLayoutTokens() {
        return this.captionLayoutTokens;
    }

    public void setCaptionLayoutTokens(List<LayoutToken> tokens) {
        this.captionLayoutTokens = tokens;
    }

    public void setLabeledCaption(String labeledCaption) {
        this.labeledCaption = labeledCaption;
    }

    public String getLabeledCaption() {
        return this.labeledCaption;
    }

    public void appendLabel(String lab) {
        label.append(lab);
    }

    public String getLabel() {
        return label.toString();
    }

    public void appendContent(String trash) {
        content.append(trash);
    }

    public String getContent() {
        return content.toString();
    }

    public void setURI(URI theURI) {
        uri = theURI;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStart() {
        return start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getEnd() {
        return end;
    }

    public void setStartToken(LayoutToken start) {
        this.startToken = start;
    }

    public LayoutToken getStartToken() {
        return startToken;
    }

    public void setEndToken(LayoutToken end) {
        this.endToken = end;
    }

    public LayoutToken getEndToken() {
        return endToken;
    }

    public void setId() {
        id = TextUtilities.cleanField(label.toString(), false);
    }

    public void setId(String theId) {
        id = theId;
    }

    public String getId() {
        return id;
    }

    public List<GraphicObject> getGraphicObjects() {
        return graphicObjects;
    }

    public List<GraphicObject> getBitmapGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists
                .newArrayList(Iterables.filter(this.graphicObjects, GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }

    public List<GraphicObject> getBoxedGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists
                .newArrayList(Iterables.filter(this.graphicObjects, BOXED_GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }

    public List<GraphicObject> getVectorBoxGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists
                .newArrayList(Iterables.filter(this.graphicObjects, VECTOR_BOX_GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }

    public void addGraphicObject(GraphicObject obj) {
        if (graphicObjects == null)
            graphicObjects = new ArrayList<GraphicObject>();
        graphicObjects.add(obj);
    }

    public void setGraphicObjects(List<GraphicObject> objs) {
        graphicObjects = objs;
    }

    /**
     * Simple block coordinates
     */
    public String getCoordinatesString() {
        return String.format("%d,%.2f,%.2f,%.2f,%.2f", page, x, y, width, height);
    }

    /**
     * Proper bounding boxes
     */
    public List<BoundingBox> getCoordinates() {
        /*if (layoutTokens == null || layoutTokens.size() == 0)
            return null;
        else {
            BoundingBox oneBox = BoundingBoxCalculator.calculateOneBox(layoutTokens, true);
            List<BoundingBox> result = new ArrayList<BoundingBox>();
            result.add(oneBox);
            return result;
        }*/

        List<BoundingBox> theBoxes = null;
        // non graphic elements
        if (CollectionUtils.isNotEmpty(getLayoutTokens())) {
            //theBoxes = BoundingBoxCalculator.calculate(getLayoutTokens());
            BoundingBox oneBox = BoundingBoxCalculator.calculateOneBox(layoutTokens, true);
            List<BoundingBox> result = new ArrayList<BoundingBox>();
            theBoxes = new ArrayList<>();
            theBoxes.add(oneBox);
        }

        // if (getBitmapGraphicObjects() != null && !getBitmapGraphicObjects().isEmpty()) {
        // -> note: this was restricted to the bitmap objects only... the bounding box calculation
        // with vector graphics might need some double check

        // here we bound all figure graphics in one single box (given that we can have hundred graphics
        // in a single figure)
        BoundingBox theGraphicsBox = null;
        if (CollectionUtils.isNotEmpty(graphicObjects)) {
            for (GraphicObject graphicObject : graphicObjects) {
                if (theGraphicsBox == null) {
                    theGraphicsBox = graphicObject.getBoundingBox();
                } else {
                    theGraphicsBox = theGraphicsBox.boundBoxExcludingAnotherPage(graphicObject.getBoundingBox());
                }
            }
        }

        if (theGraphicsBox != null) {
            if (theBoxes == null)
                theBoxes = new ArrayList<>();
            theBoxes.add(theGraphicsBox);
        }

        List<BoundingBox> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(theBoxes)) {
            BoundingBox oneBox = BoundingBoxCalculator.calculateOneBox(layoutTokens, true);
            List<BoundingBox> mergedBox = VectorGraphicBoxCalculator.mergeBoxes(theBoxes);
            result.addAll(mergedBox);
        }

        Collections.sort(result);

        return result;
    }

    public String getTeiId() {
        return "fig_" + this.id;
    }

    public boolean isCompleteForTEI() {
        return (StringUtils.isNotBlank(header) || StringUtils.isNotBlank(caption)
                || CollectionUtils.isNotEmpty(graphicObjects));
    }

    public String toTEI(
            GrobidAnalysisConfig config,
            Document doc,
            TEIFormatter formatter,
            List<MarkerType> markerTypes) {
        if (!isCompleteForTEI()) {
            LOGGER.warn(
                    "Found a figure that is badly formatted but it should have been spotted before. We ignore it now.");
            return null;
        }
        Element figureElement = XmlBuilderUtils.teiElement("figure");
        if (id != null) {
            XmlBuilderUtils.addXmlId(figureElement, "fig_" + id);
        }

        if (config.isGenerateTeiCoordinates("figure")) {
            List<BoundingBox> theBoxes = null;
            // non graphic elements
            if (CollectionUtils.isNotEmpty(getLayoutTokens())) {
                theBoxes = BoundingBoxCalculator.calculate(getLayoutTokens());
            }

            // if (getBitmapGraphicObjects() != null && !getBitmapGraphicObjects().isEmpty()) {
            // -> note: this was restricted to the bitmap objects only... the bounding box calculation
            // with vector graphics might need some double check

            // here we bound all figure graphics in one single box (given that we can have a hundred graphics
            // in a single figure)
            BoundingBox theGraphicsBox = null;
            if (CollectionUtils.isNotEmpty(graphicObjects)) {
                for (GraphicObject graphicObject : graphicObjects) {
                    if (theGraphicsBox == null) {
                        theGraphicsBox = graphicObject.getBoundingBox();
                    } else {
                        theGraphicsBox = theGraphicsBox.boundBoxExcludingAnotherPage(graphicObject.getBoundingBox());
                    }
                }
            }

            if (theGraphicsBox != null) {
                if (theBoxes == null)
                    theBoxes = new ArrayList<>();
                theBoxes.add(theGraphicsBox);
            }

            if (CollectionUtils.isNotEmpty(theBoxes)) {
                String coords = Joiner.on(";").join(theBoxes);
                XmlBuilderUtils.addCoords(figureElement, coords);
            }
        }

        if (StringUtils.isNotBlank(header)) {
            Element head = XmlBuilderUtils.teiElement(
                    "head",
                    LayoutTokensUtil.normalizeText(header.toString()));
            figureElement.appendChild(head);
        }

        if (StringUtils.isNotBlank(label)) {
            Element labelEl = XmlBuilderUtils.teiElement(
                    "label",
                    LayoutTokensUtil.normalizeText(label.toString()));
            figureElement.appendChild(labelEl);
        }
        if (StringUtils.isNotBlank(caption)) {
            Element desc = XmlBuilderUtils.teiElement("figDesc");
            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                addXmlId(desc, "_" + divID);
            }

            // if the segment has been parsed with the full text model we further extract the clusters
            // to get the bibliographical references
            if (StringUtils.isNotBlank(labeledCaption)) {
                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labeledCaption,
                        captionLayoutTokens);
                List<TaggingTokenCluster> clusters = clusteror.cluster();

                MarkerType citationMarkerType = null;
                if (CollectionUtils.isNotEmpty(markerTypes)) {
                    citationMarkerType = markerTypes.get(0);
                }

                for (TaggingTokenCluster cluster : clusters) {
                    if (cluster == null) {
                        continue;
                    }

                    TaggingLabel clusterLabel = cluster.getTaggingLabel();
                    //String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                    String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
                    if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                        try {
                            List<Node> refNodes = formatter.markReferencesTEILuceneBased(
                                    cluster.concatTokens(),
                                    doc.getReferenceMarkerMatcher(),
                                    config.isGenerateTeiCoordinates("ref"),
                                    false,
                                    citationMarkerType);
                            if (refNodes != null) {
                                for (Node n : refNodes) {
                                    desc.appendChild(n);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Problem when serializing TEI fragment for figure caption", e);
                        }
                    } else {
                        desc.appendChild(textNode(clusterContent));
                    }
                }
            } else {
                desc.appendChild(LayoutTokensUtil.normalizeText(caption.toString()).trim());
                //Element desc = XmlBuilderUtils.teiElement("figDesc",
                //    LayoutTokensUtil.normalizeText(caption.toString()));
            }

            if (StringUtils.isNotBlank(desc.getValue()) && config.isWithSentenceSegmentation()) {
                formatter.segmentIntoSentences(
                        desc,
                        this.captionLayoutTokens,
                        config,
                        doc.getLanguage(),
                        doc.getPDFAnnotations());

                // we need a sentence segmentation of the figure caption, for that we need to introduce
                // a <div>, then a <p>
                desc.setLocalName("p");

                Element div = XmlBuilderUtils.teiElement("div");
                div.appendChild(desc);

                Element figDesc = XmlBuilderUtils.teiElement("figDesc");
                figDesc.appendChild(div);

                desc = figDesc;
            }

            figureElement.appendChild(desc);
        }

        if (config.isIncludeDiscardedText() && CollectionUtils.isNotEmpty(discardedPiecesTokens)) {
            for (List<LayoutToken> discardedPieceTokens : discardedPiecesTokens) {
                figureElement.appendChild(
                        generateDiscardedTextNote(discardedPieceTokens, doc, formatter, config));
            }
        }

        if (CollectionUtils.isNotEmpty(graphicObjects)) {
            for (GraphicObject graphicObject : graphicObjects) {
                Element go = XmlBuilderUtils.teiElement("graphic");
                String uri = graphicObject.getURI();
                if (uri != null) {
                    go.addAttribute(new Attribute("url", uri));
                }

                if (graphicObject.getBoundingBox() != null) {
                    go.addAttribute(new Attribute("coords", graphicObject.getBoundingBox().toString()));
                }

                go.addAttribute(new Attribute("type", graphicObject.getType().name().toLowerCase()));
                if (graphicObject.isMask()) {
                    go.addAttribute(new Attribute("mask", "true"));
                }
                figureElement.appendChild(go);
            }
        }
        return figureElement.toXML();
    }

    private String cleanString(String input) {
        return input.replace("\n", " ").replace("  ", " ").trim();
    }

    public int getPage() {
        return page;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public URI getUri() {
        return uri;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<BoundingBox> getTextArea() {
        return textArea;
    }

    public void setTextArea(List<BoundingBox> textArea) {
        this.textArea = textArea;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void addLayoutTokens(List<LayoutToken> layoutTokens) {
        if (this.layoutTokens == null)
            this.layoutTokens = new ArrayList<>();
        this.layoutTokens.addAll(layoutTokens);
    }

    public void setBlockPtrs(SortedSet<Integer> blockPtrs) {
        this.blockPtrs = blockPtrs;
    }

    public SortedSet<Integer> getBlockPtrs() {
        return blockPtrs;
    }

    public void setCaption(StringBuilder caption) {
        this.caption = caption;
    }

    public void setHeader(StringBuilder header) {
        this.header = header;
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }

    public void setLabel(StringBuilder label) {
        this.label = label;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<List<LayoutToken>> getDiscardedPiecesTokens() {
        return discardedPiecesTokens;
    }

    public void setDiscardedPiecesTokens(List<List<LayoutToken>> discardedPiecesTokens) {
        this.discardedPiecesTokens = discardedPiecesTokens;
    }

    public void addDiscardedPieceTokens(List<LayoutToken> pieceToken) {
        this.discardedPiecesTokens.add(pieceToken);
    }
}
