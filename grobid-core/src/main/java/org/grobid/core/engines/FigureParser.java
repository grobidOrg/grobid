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

import static org.grobid.core.engines.label.TaggingLabels.*;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Figure;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

class FigureParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FigureParser.class);

    FigureParser() {
        super(GrobidModels.FIGURE);
    }

    /**
     * The processing here is called from the full text parser in cascade.
     * Start and end position in the higher level tokenization are indicated in
     * the resulting Figure object.
     */
    public Figure processing(List<LayoutToken> tokenizationFigure, String featureVector) {
        warnIfDebugUncaptured("FigureParser.processing(List, String)");
        return processing(tokenizationFigure, featureVector, null);
    }

    public Figure processing(
            List<LayoutToken> tokenizationFigure,
            String featureVector,
            org.grobid.core.engines.config.GrobidAnalysisConfig config) {
        String res;
        try {
            res = labelAndCapture(featureVector, config);
        } catch (Exception e) {
            throw new GrobidException("Sequence labeling with figure model fails.", e);
        }
        if (res == null) {
            return null;
        }
        return getExtractionResult(tokenizationFigure, res);
    }

    private Figure getExtractionResult(List<LayoutToken> tokenizations, String result) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FIGURE, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        Figure figure = new Figure();
        figure.setLayoutTokens(tokenizations);

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
            if (clusterLabel.equals(FIG_DESC)) {
                figure.appendCaption(clusterContent);
                figure.appendCaptionLayoutTokens(cluster.concatTokens());
            } else if (clusterLabel.equals(FIG_HEAD)) {
                figure.appendHeader(clusterContent);
            } else if (clusterLabel.equals(FIG_LABEL)) {
                figure.appendLabel(clusterContent);
                //label should also go to head
                figure.appendHeader(" " + clusterContent + " ");
            } else if (clusterLabel.equals(FIG_OTHER)) {
                figure.addDiscardedPieceTokens(cluster.concatTokens());
            } else if (clusterLabel.equals(FIG_CONTENT)) {
                figure.appendContent(clusterContent);
            } else {
                LOGGER.warn("Unexpected figure model label - " + clusterLabel.getLabel() + " for " + clusterContent);
            }
        }
        return figure;
    }

    /**
     * The training data creation is called from the full text training creation in cascade.
     */
    public Pair<String, String> createTrainingData(
            List<LayoutToken> tokenizations,
            String featureVector,
            String id) {
        //System.out.println(tokenizations.toString() + "\n" );
        String res = null;
        try {
            res = label(featureVector);
        } catch (Exception e) {
            LOGGER.error("Sequence labeling in FigureParser fails.", e);
        }
        if (res == null) {
            return Pair.of(null, featureVector);
        }
        //System.out.println(res + "\n" );
        List<Pair<String, String>> labeled = GenericTaggerUtils.getTokensAndLabels(res);
        StringBuilder sb = new StringBuilder();

        int tokPtr = 0;
        boolean addSpace = false;
        boolean addEOL = false;
        String lastTag = null;
        boolean figOpen = false;
        for (Pair<String, String> l : labeled) {
            String tok = l.getLeft();
            String label = l.getRight();

            int tokPtr2 = tokPtr;
            for (; tokPtr2 < tokenizations.size(); tokPtr2++) {
                if (tokenizations.get(tokPtr2).getText().equals(" ")) {
                    addSpace = true;
                } else if (tokenizations.get(tokPtr2).getText().equals("\n") ||
                        tokenizations.get(tokPtr).getText().equals("\r")) {
                    addEOL = true;
                } else {
                    break;
                }
            }
            tokPtr = tokPtr2;

            if (tokPtr >= tokenizations.size()) {
                LOGGER.error("Implementation error: Reached the end of tokenizations, but current token is " + tok);
                // we add a space to avoid concatenated text
                addSpace = true;
            } else {
                String tokenizationToken = tokenizations.get(tokPtr).getText();

                if ((tokPtr != tokenizations.size()) && !tokenizationToken.equals(tok)) {
                    // and we add a space by default to avoid concatenated text
                    addSpace = true;
                    if (!tok.startsWith(tokenizationToken)) {
                        // this is a very exceptional case due to a sequence of accent/diacresis, in this case we skip
                        // a shift in the tokenizations list and continue on the basis of the labeled token
                        // we check one ahead
                        tokPtr++;
                        tokenizationToken = tokenizations.get(tokPtr).getText();
                        if (!tok.equals(tokenizationToken) && (tokenizations.size() > tokPtr + 1)) {
                            // we try another position forward (second hope!)
                            tokPtr++;
                            tokenizationToken = tokenizations.get(tokPtr).getText();
                            if (!tok.equals(tokenizationToken) && (tokenizations.size() > tokPtr + 1)) {
                                // we try another position forward (last hope!)
                                tokPtr++;
                                tokenizationToken = tokenizations.get(tokPtr).getText();
                                if (!tok.equals(tokenizationToken)) {
                                    // we return to the initial position
                                    tokPtr = tokPtr - 3;
                                    tokenizationToken = tokenizations.get(tokPtr).getText();
                                    LOGGER.error(
                                            "Implementation error, tokens out of sync: "
                                                    +
                                                    tokenizationToken
                                                    + " != "
                                                    + tok
                                                    + ", at position "
                                                    + tokPtr);
                                }
                            }
                        }
                    }
                    // note: if the above condition is true, this is an exceptional case due to a
                    // sequence of accent/diacresis and we can go on as a full string match
                }
            }

            String plainLabel = GenericTaggerUtils.getPlainLabel(label);

            String output;
            if (lastTag != null) {
                // A label starting with "I-" marks the beginning of a NEW segment. Force the
                // previous element to close even when the base label is unchanged (e.g. two
                // consecutive <head> regions), otherwise the previous element is never closed
                // while writeField opens a new one, producing unbalanced training XML.
                String closingTrigger = label.startsWith("I-") ? "" : plainLabel;
                testClosingTag(sb, closingTrigger, lastTag, addSpace, addEOL);
            }

            output = writeField(label, lastTag, tok, "<figure_head>", "<head>", addSpace, addEOL, 3);
            String figureOpening = "        <figure>\n";
            if (output != null) {
                if (!figOpen) {
                    sb.append(figureOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<figDesc>", "<figDesc>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(figureOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<label>", "<label>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(figureOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<content>", "", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(figureOpening);
                    figOpen = true;
                }
                sb.append(output);
                //continue;
            }
            output = writeField(label, lastTag, tok, "<other>", "", addSpace, addEOL, 2);
            if (output != null) {
                sb.append(output);
            }

            lastTag = plainLabel;
            addSpace = false;
            addEOL = false;
            tokPtr++;
        }

        if (figOpen) {
            testClosingTag(sb, "", lastTag, addSpace, addEOL);
            sb.append("        </figure>\n");
        }

        return Pair.of(sb.toString(), featureVector);
    }

    public String getTEIHeader(String id) {
        return "<tei>\n"
                +
                "    <teiHeader>\n"
                +
                "        <fileDesc xml:id=\"_"
                + id
                + "\"/>\n"
                +
                "    </teiHeader>\n"
                +
                "    <text xml:lang=\"en\">\n";
    }

    private boolean testClosingTag(
            StringBuilder buffer,
            String currentTag,
            String lastTag,
            boolean addSpace,
            boolean addEOL) {
        boolean res = false;
        if (!currentTag.equals(lastTag)) {
            res = true;
            // we close the current tag
            switch (lastTag) {
                case "<other>" :
                    if (addEOL)
                        buffer.append("<lb/>");
                    if (addSpace)
                        buffer.append(" ");
                    buffer.append("\n");
                    break;
                case "<figure_head>" :
                    if (addEOL)
                        buffer.append("<lb/>");
                    if (addSpace)
                        buffer.append(" ");
                    buffer.append("</head>\n");
                    break;
                case "<figDesc>" :
                    if (addEOL)
                        buffer.append("<lb/>");
                    if (addSpace)
                        buffer.append(" ");
                    buffer.append("</figDesc>\n");
                    break;
                case "<label>" :
                    if (addEOL)
                        buffer.append("<lb/>");
                    if (addSpace)
                        buffer.append(" ");
                    buffer.append("</label>\n");
                    break;
                case "<content>" :
                    // Figure <content> is emitted as bare text (the writeField special-case
                    // below does not open a "<content>" element, and the shipped figure
                    // training corpus has no <content> element at all). Closing with
                    // "</content>" therefore produced an unbalanced tag. Keep it bare like <other>.
                    if (addEOL)
                        buffer.append("<lb/>");
                    if (addSpace)
                        buffer.append(" ");
                    buffer.append("\n");
                    break;
                default :
                    res = false;
                    break;
            }
        }
        return res;
    }

    private String writeField(
            String currentTag,
            String lastTag,
            String token,
            String field,
            String outField,
            boolean addSpace,
            boolean addEOL,
            int nbIndent) {
        String result = null;
        if (currentTag.endsWith(field)) {
            if (currentTag.endsWith("<other>") || currentTag.endsWith("<content>")) {
                result = "";
                if (currentTag.startsWith("I-") || (lastTag == null)) {
                    result += "\n";
                    for (int i = 0; i < nbIndent; i++) {
                        result += "    ";
                    }
                }
                if (addEOL)
                    result += "<lb/>";
                if (addSpace)
                    result += " ";
                result += TextUtilities.HTMLEncode(token);
            } else if ((lastTag != null) && currentTag.endsWith(lastTag)) {
                result = "";
                if (addEOL)
                    result += "<lb/>";
                if (addSpace)
                    result += " ";
                if (currentTag.startsWith("I-"))
                    result += outField;
                result += TextUtilities.HTMLEncode(token);
            } else {
                result = "";
                if (addEOL)
                    result += "<lb/>";
                if (addSpace)
                    result += " ";
                result += "\n";
                if (outField.length() > 0) {
                    for (int i = 0; i < nbIndent; i++) {
                        result += "    ";
                    }
                }

                result += outField + TextUtilities.HTMLEncode(token);
            }
        }
        return result;
    }
}
