package org.grobid.core.engines;

import static org.grobid.core.engines.label.TaggingLabels.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Table;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

public class TableParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableParser.class);

    protected TableParser() {
        super(GrobidModels.TABLE);
    }

    /**
     * The processing here is called from the full text parser in cascade.
     * Normally we should find only one table in the sequence to be labelled.
     * But for robustness and recovering error from the higher level, we allow
     * sub-segmenting several tables that appears one after the other.
     */
    public List<Table> processing(List<LayoutToken> tokenizationTable, String featureVector) {
        warnIfDebugUncaptured("TableParser.processing(List, String)");
        return processing(tokenizationTable, featureVector, null);
    }

    public List<Table> processing(
            List<LayoutToken> tokenizationTable,
            String featureVector,
            org.grobid.core.engines.config.GrobidAnalysisConfig config) {
        String res;
        try {
            res = labelAndCapture(featureVector, config);
        } catch (Exception e) {
            throw new GrobidException("Sequence labeling with table model fails.", e);
        }

        if (res == null) {
            return null;
        }
        //        List<Pair<String, String>> labeled = GenericTaggerUtils.getTokensAndLabels(res);
        return getExtractionResult(tokenizationTable, res);
    }

    private List<Table> getExtractionResult(List<LayoutToken> tokenizations, String result) {
        List<Table> tables = new ArrayList<>();

        // first table
        Table table = new Table();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.TABLE, result, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        TaggingLabel previousLabel = null;

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            List<LayoutToken> tokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(tokens));
            if (clusterLabel.equals(TBL_DESC)) {
                table.appendCaption(clusterContent);
                table.appendCaptionLayoutTokens(tokens);
                table.getFullDescriptionTokens().addAll(tokens);
                table.addLayoutTokens(tokens);
            } else if (clusterLabel.equals(TBL_HEAD)) {
                // if we already have a header (it could be via label) and we are not continuing some header/label
                // we consider the non-connected header field as the introduction of a new table
                // TBD: this work fine for header located before the table content, but not sure otherwise
                if (!StringUtils.isEmpty(table.getHeader()) &&
                        previousLabel != null &&
                        (previousLabel.equals(TBL_CONTENT) || previousLabel.equals(TBL_NOTE)
                                || previousLabel.equals(TBL_DESC))) {
                    // we already have a table header, this means that we have a distinct table starting now
                    tables.add(table);
                    table.setTextArea(
                            Collections.singletonList(
                                    BoundingBoxCalculator.calculateOneBox(table.getLayoutTokens(), true)));
                    table = new Table();
                }
                table.appendHeader(clusterContent);
                table.getFullDescriptionTokens().addAll(tokens);
                table.addLayoutTokens(tokens);
            } else if (clusterLabel.equals(TBL_LABEL)) {
                //label should also go to head
                table.appendHeader(" " + clusterContent + " ");
                table.appendLabel(clusterContent);
                table.getFullDescriptionTokens().addAll(tokens);
                table.addLayoutTokens(tokens);
            } else if (clusterLabel.equals(TBL_NOTE)) {
                table.appendNote(clusterContent);
                table.getFullDescriptionTokens().addAll(tokens);
                table.addAllNoteLayoutTokens(tokens);
                table.addLayoutTokens(tokens);
            } else if (clusterLabel.equals(TBL_OTHER)) {
                table.addDiscardedPieceTokens(cluster.concatTokens());
                table.addLayoutTokens(tokens);
            } else if (clusterLabel.equals(TBL_CONTENT)) {
                table.appendContent(clusterContent);
                table.getContentTokens().addAll(tokens);
                table.addLayoutTokens(tokens);
            } else {
                LOGGER.warn("Unexpected table model label - " + clusterLabel.getLabel() + " for " + clusterContent);
            }

            previousLabel = clusterLabel;
        }

        // last table
        table.setTextArea(
                Collections.singletonList(BoundingBoxCalculator.calculateOneBox(table.getLayoutTokens(), true)));
        tables.add(table);

        return tables;
    }

    /**
     * The training data creation is called from the full text training creation in cascade.
     */
    public Pair<String, String> createTrainingData(
            List<LayoutToken> tokenizations,
            String featureVector,
            String id) {
        String res = null;
        try {
            res = label(featureVector);
        } catch (Exception e) {
            LOGGER.error("Sequence labeling in TableParser fails.", e);
        }
        if (res == null) {
            return Pair.of(null, featureVector);
        }

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
                        if (!tok.equals(tokenizationToken)) {
                            // we try another position forward (second hope!)
                            tokPtr++;
                            tokenizationToken = tokenizations.get(tokPtr).getText();
                            if (!tok.equals(tokenizationToken)) {
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

            String output = null;
            if (lastTag != null) {
                // A label starting with "I-" marks the beginning of a NEW segment. Force the
                // previous element to close even when the base label is unchanged (e.g. two
                // consecutive <note> or <head> regions), otherwise the previous element is never
                // closed while writeField opens a new one, producing unbalanced training XML.
                String closingTrigger = label.startsWith("I-") ? "" : plainLabel;
                testClosingTag(sb, closingTrigger, lastTag, addSpace, addEOL);
            }

            output = writeField(label, lastTag, tok, "<figure_head>", "<head>", addSpace, addEOL, 3);
            String tableOpening = "\t\t<figure type=\"table\">\n";
            if (output != null) {
                if (!figOpen) {
                    sb.append(tableOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<figDesc>", "<figDesc>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(tableOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<label>", "<label>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(tableOpening);
                    figOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<content>", "<table>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(tableOpening);
                    figOpen = true;
                }
                sb.append(output);
                //continue;
            }
            output = writeField(label, lastTag, tok, "<note>", "<note>", addSpace, addEOL, 3);
            if (output != null) {
                if (!figOpen) {
                    sb.append(tableOpening);
                    figOpen = true;
                }
                sb.append(output);
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
            sb.append("\t\t</figure>\n");
        }

        return Pair.of(sb.toString(), featureVector);
    }

    public String getTEIHeader(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<tei>\n"
                        +
                        "    <teiHeader>\n"
                        +
                        "        <fileDesc xml:id=\"_"
                        + id
                        + "\"/>\n"
                        +
                        "    </teiHeader>\n"
                        +
                        "    <text xml:lang=\"en\">\n");
        return sb.toString();
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
            if (lastTag.equals("<other>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                // <other> content is emitted as bare text (no wrapping element), so we must
                // not append an opening "<other>" tag here — doing so left a stray, never-closed
                // tag in the generated table training data (unbalanced XML). Mirror FigureParser.
                buffer.append("\n");
            } else if (lastTag.equals("<figure_head>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</head>\n");
            } else if (lastTag.equals("<content>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</table>\n");
            } else if (lastTag.equals("<figDesc>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</figDesc>\n");
            } else if (lastTag.equals("<label>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</label>\n");
            } else if (lastTag.equals("<note>")) {
                if (addEOL)
                    buffer.append("<lb/>");
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</note>\n");
            } else {
                res = false;
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
            /*if (currentTag.endsWith("<other>") || currentTag.endsWith("<content>")) {
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
            }
            else*/
            if ((lastTag != null) && currentTag.endsWith(lastTag)) {
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
