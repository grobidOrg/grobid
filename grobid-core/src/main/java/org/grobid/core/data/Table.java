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
import static org.grobid.core.document.TEIFormatter.isNewParagraph;
import static org.grobid.core.document.xml.XmlBuilderUtils.*;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.table.Cell;
import org.grobid.core.data.table.Line;
import org.grobid.core.data.table.LinePart;
import org.grobid.core.data.table.Row;
import org.grobid.core.document.Document;
import org.grobid.core.document.TEIFormatter;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.counters.TableRejectionCounters;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.counters.CntManager;

/**
 * Class for representing a table.
 *
 */
public class Table extends Figure {
    private List<LayoutToken> contentTokens = new ArrayList<>();
    private List<LayoutToken> fullDescriptionTokens = new ArrayList<>();

    private boolean goodTable = true;

    private StringBuilder note = null;
    private List<LayoutToken> noteLayoutTokens = null;
    private String labeledNote = null;

    private List<List<LayoutToken>> discardedPiecesTokens = new ArrayList<>();

    public void setGoodTable(boolean goodTable) {
        this.goodTable = goodTable;
    }

    public Table() {
        caption = new StringBuilder();
        header = new StringBuilder();
        content = new StringBuilder();
        label = new StringBuilder();
        note = new StringBuilder();
    }

    public boolean isCompleteForTEI() {
        return (StringUtils.isNotEmpty(header) && StringUtils.isNotEmpty(caption));
    }

    @Override
    public String toTEI(
            GrobidAnalysisConfig config,
            Document doc,
            TEIFormatter formatter,
            List<MarkerType> markerTypes) {
        if (!isCompleteForTEI()) {
            LOGGER.warn(
                    "Found a table that is badly formatted but it should have been spotted before. We ignore it now.");
            return null;
        }

        Element tableElement = XmlBuilderUtils.teiElement("figure");
        tableElement.addAttribute(new Attribute("type", "table"));
        if (id != null) {
            XmlBuilderUtils.addXmlId(tableElement, "tab_" + id);
        }

        // this is non TEI, to be reviewed
        //tableElement.addAttribute(new Attribute("validated", String.valueOf(isGoodTable())));

        if ((config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("figure"))) {
            XmlBuilderUtils.addCoords(tableElement, LayoutTokensUtil.getCoordsStringForOneBox(getLayoutTokens()));
        }

        Element headEl = XmlBuilderUtils.teiElement(
                "head",
                LayoutTokensUtil.normalizeText(header.toString()));

        Element labelEl = XmlBuilderUtils.teiElement(
                "label",
                LayoutTokensUtil.normalizeText(label.toString()));

        /*Element descEl = XmlBuilderUtils.teiElement("figDesc");
        descEl.appendChild(LayoutTokensUtil.normalizeText(caption.toString()).trim());
        if ((config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("figure"))) {
        	XmlBuilderUtils.addCoords(descEl, LayoutTokensUtil.getCoordsString(getFullDescriptionTokens()));
        }*/

        Element desc = null;
        if (StringUtils.isNotBlank(caption)) {
            // if the segment has been parsed with the full text model we further extract the clusters
            // to get the bibliographical references

            desc = XmlBuilderUtils.teiElement("figDesc");
            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                addXmlId(desc, "_" + divID);
            }

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
                            LOGGER.warn("Problem when serializing TEI fragment for table caption", e);
                        }
                    } else {
                        desc.appendChild(textNode(clusterContent));
                    }

                    if (StringUtils.isNotBlank(desc.getValue()) && config.isWithSentenceSegmentation()) {
                        formatter.segmentIntoSentences(
                                desc,
                                this.captionLayoutTokens,
                                config,
                                doc.getLanguage(),
                                doc.getPDFAnnotations());

                        // we need a sentence segmentation of the table caption, for that we need to introduce
                        // a <div>, then a <p>
                        desc.setLocalName("p");

                        Element div = XmlBuilderUtils.teiElement("div");
                        div.appendChild(desc);

                        Element figDesc = XmlBuilderUtils.teiElement("figDesc");
                        figDesc.appendChild(div);

                        desc = figDesc;
                    }
                }
            } else {
                desc.appendChild(LayoutTokensUtil.normalizeText(caption.toString()).trim());
            }
        }

        Element contentEl = XmlBuilderUtils.teiElement("table");
        processTableContent(contentEl, this.getContentTokens());
        if ((config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("figure"))) {
            XmlBuilderUtils.addCoords(contentEl, LayoutTokensUtil.getCoordsStringForOneBox(getContentTokens()));
        }

        Element noteNode = null;
        if (StringUtils.isNotBlank(note)) {

            noteNode = XmlBuilderUtils.teiElement("note");
            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                addXmlId(noteNode, "_" + divID);
            }

            if (StringUtils.isNotBlank(labeledNote)) {
                Element p = teiElement("p");
                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labeledNote,
                        noteLayoutTokens);
                List<TaggingTokenCluster> clusters = clusteror.cluster();
                for (TaggingTokenCluster cluster : clusters) {
                    if (cluster == null) {
                        continue;
                    }

                    MarkerType citationMarkerType = null;
                    if (CollectionUtils.isNotEmpty(markerTypes)) {
                        citationMarkerType = markerTypes.get(0);
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
                                    p.appendChild(n);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Problem when serializing TEI fragment for table note", e);
                        }
                    } else {
                        if (p.getChildCount() > 0 && isNewParagraph(clusterLabel, p)) {
                            noteNode.appendChild(p);
                            p = teiElement("p");
                        }
                        p.appendChild(textNode(clusterContent));
                    }
                }
                if (p.getChildCount() > 0) {
                    noteNode.appendChild(p);
                }
                if (config.isWithSentenceSegmentation()) {
                    // we need a sentence segmentation of the figure caption
                    formatter.segmentIntoSentences(
                            p,
                            this.noteLayoutTokens,
                            config,
                            doc.getLanguage(),
                            doc.getPDFAnnotations());
                }
            } else {
                Element p = teiElement("p");
                p.appendChild(LayoutTokensUtil.normalizeText(note.toString()).trim());

                if (config.isWithSentenceSegmentation()) {
                    // we need a sentence segmentation of the figure caption
                    formatter.segmentIntoSentences(
                            p,
                            this.noteLayoutTokens,
                            config,
                            doc.getLanguage(),
                            doc.getPDFAnnotations());
                }

                noteNode = XmlBuilderUtils.teiElement("note");
                noteNode.appendChild(p);
            }

            String coords = null;
            if (config.isGenerateTeiCoordinates("note")) {
                coords = LayoutTokensUtil.getCoordsString(noteLayoutTokens);
            }

            if (coords != null) {
                noteNode.addAttribute(new Attribute("coords", coords));
            }
        }

        tableElement.appendChild(headEl);
        tableElement.appendChild(labelEl);
        if (desc != null)
            tableElement.appendChild(desc);
        tableElement.appendChild(contentEl);

        if (noteNode != null) {
            tableElement.appendChild(noteNode);
        }

        if (config.isIncludeDiscardedText() && CollectionUtils.isNotEmpty(discardedPiecesTokens)) {
            for (List<LayoutToken> discardedPieceTokens : discardedPiecesTokens) {
                tableElement.appendChild(
                        generateDiscardedTextNote(discardedPieceTokens, doc, formatter, config));
            }
        }

        return tableElement.toXML();
    }

    /**
     *
     * @param contentEl table element to append parsed rows and cells.
     * @param contentTokens tokens that are used to build cells
     * Line-based algorithm for parsing tables, uses tokens' coordinates to identify lines
     */
    void processTableContent(Element contentEl, List<LayoutToken> contentTokens) {
        // Join Layout Tokens into cell lines originally created by PDFAlto
        List<LinePart> lineParts = Line.extractLineParts(contentTokens);

        // Build lines by comparing borders
        List<Line> lines = Line.extractLines(lineParts);

        // Build rows and cells
        List<Row> rows = Row.extractRows(lines);

        int columnCount = Row.columnCount(rows);

        Row.insertEmptyCells(rows, columnCount);

        Row.mergeMulticolumnCells(rows);

        for (Row row : rows) {
            Element tr = XmlBuilderUtils.teiElement("row");
            contentEl.appendChild(tr);
            List<Cell> cells = row.getContent();
            for (Cell cell : cells) {
                Element td = XmlBuilderUtils.teiElement("cell");
                tr.appendChild(td);
                if (cell.getColspan() > 1) {
                    td.addAttribute(new Attribute("cols", Integer.toString(cell.getColspan())));
                }
                td.appendChild(cell.getText().trim());
            }
        }
    }

    private String cleanString(String input) {
        return input.replace("\n", " ").replace("  ", " ").trim();
    }

    public String getNote() {
        return note.toString();
    }

    public void setNote(StringBuilder note) {
        this.note = note;
    }

    public void appendNote(String noteChunk) {
        note.append(noteChunk);
    }

    // if an extracted table passes some validations rules
    public boolean firstCheck() {
        goodTable = goodTable && validateTable();
        return goodTable;
    }

    public boolean secondCheck() {
        goodTable = goodTable && !badTableAdvancedCheck();
        return goodTable;
    }

    public List<LayoutToken> getNoteLayoutTokens() {
        return noteLayoutTokens;
    }

    public void setNoteLayoutTokens(List<LayoutToken> tokens) {
        this.noteLayoutTokens = tokens;
    }

    public void addNoteLayoutToken(LayoutToken token) {
        if (this.noteLayoutTokens == null)
            this.noteLayoutTokens = new ArrayList<LayoutToken>();
        noteLayoutTokens.add(token);
    }

    public void addAllNoteLayoutTokens(List<LayoutToken> tokens) {
        if (this.noteLayoutTokens == null)
            this.noteLayoutTokens = new ArrayList<LayoutToken>();
        noteLayoutTokens.addAll(tokens);
    }

    public void setLabeledNote(String labeledNote) {
        this.labeledNote = labeledNote;
    }

    public String getLabeledNote() {
        return this.labeledNote;
    }

    /** Check if the table:
     * - has label, header and content
     * - header starts with "tab"
     * - label can be parsed
     */
    public boolean validateTable() {
        CntManager cnt = Engine.getCntManager();
        if (StringUtils.isAnyBlank(label, header, content)) {
            cnt.i(TableRejectionCounters.EMPTY_LABEL_OR_HEADER_OR_CONTENT);
            return false;
        }

        try {
            Integer.valueOf(getLabel().trim(), 10);
        } catch (NumberFormatException e) {
            cnt.i(TableRejectionCounters.CANNOT_PARSE_LABEL_TO_INT);
            return false;
        }
        // tab covers: table, tabelle, tableu, tabella, etc.
        if (!StringUtils.startsWithIgnoreCase(getHeader(), "tab")) {
            cnt.i(TableRejectionCounters.HEADER_NOT_STARTS_WITH_TABLE_WORD);
            return false;
        }
        return true;
    }

    private boolean badTableAdvancedCheck() {
        CntManager cnt = Engine.getCntManager();
        BoundingBox contentBox = BoundingBoxCalculator.calculateOneBox(contentTokens, true);
        BoundingBox descBox = BoundingBoxCalculator.calculateOneBox(fullDescriptionTokens, true);

        if (contentBox.getPage() != descBox.getPage()) {
            cnt.i(TableRejectionCounters.HEADER_AND_CONTENT_DIFFERENT_PAGES);
            return true;
        }

        if (contentBox.intersect(descBox)) {
            cnt.i(TableRejectionCounters.HEADER_AND_CONTENT_INTERSECT);
            return true;
        }

        if (descBox.area() > contentBox.area()) {
            cnt.i(TableRejectionCounters.HEADER_AREA_BIGGER_THAN_CONTENT);
            return true;
        }

        if (contentBox.getHeight() < 40) {
            cnt.i(TableRejectionCounters.CONTENT_SIZE_TOO_SMALL);
            return true;
        }

        if (contentBox.getWidth() < 100) {
            cnt.i(TableRejectionCounters.CONTENT_WIDTH_TOO_SMALL);
            return true;
        }

        if (contentTokens.size() < 10) {
            cnt.i(TableRejectionCounters.FEW_TOKENS_IN_CONTENT);
            return true;
        }

        if (fullDescriptionTokens.size() < 5) {
            cnt.i(TableRejectionCounters.FEW_TOKENS_IN_HEADER);
            return true;
        }
        return false;
    }

    public List<LayoutToken> getContentTokens() {
        return contentTokens;
    }

    public List<LayoutToken> getFullDescriptionTokens() {
        return fullDescriptionTokens;
    }

    public boolean isGoodTable() {
        return goodTable;
    }

    public String getTeiId() {
        return "tab_" + this.id;
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
