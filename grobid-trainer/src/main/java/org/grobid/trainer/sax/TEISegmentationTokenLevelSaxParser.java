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
package org.grobid.trainer.sax;

import static org.grobid.core.engines.label.TaggingLabels.AVAILABILITY_LABEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.grobid.core.utilities.TextUtilities;

/**
 * Token-level SAX parser for segmentation training data. Unlike the base TEISegmentationSaxParser
 * which outputs only the first token per line, this parser outputs ALL tokens using GROBID's
 * standard delimiters, matching the tokenization produced by pdfalto + GrobidDefaultAnalyzer.
 */
public class TEISegmentationTokenLevelSaxParser extends TEISegmentationSaxParser {

    private static final Logger logger = LoggerFactory.getLogger(TEISegmentationTokenLevelSaxParser.class);

    private StringBuffer accumulator = null;

    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null;
    private boolean inTeiHeader = false;

    public TEISegmentationTokenLevelSaxParser() {
        labeled = new ArrayList<String>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        if (this.inTeiHeader) {
            return;
        }
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        if (accumulator != null) {
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(
            String uri,
            String localName,
            String qName) throws SAXException {
        if (qName.equals("teiHeader")) {
            inTeiHeader = false;
            return;
        }

        if (inTeiHeader) {
            return;
        }

        if ((!qName.equals("lb")) && (!qName.equals("pb"))) {
            writeData(qName, currentTag);
        }
        if (qName.equals("body") ||
                qName.equals("cover") ||
                qName.equals("front") ||
                qName.equals("div") ||
                qName.equals("toc") ||
                qName.equals("other") ||
                qName.equals("listBibl")) {
            currentTag = null;
            upperTag = null;
        } else if (qName.equals("note") ||
                qName.equals("page") ||
                qName.equals("pages") ||
                qName.equals("titlePage")) {
            currentTag = upperTag;
        }
    }

    // startElement is identical to base TEISegmentationSaxParser
    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (inTeiHeader) {
            return;
        }

        if (qName.equals("lb")) {
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else if (qName.equals("teiHeader")) {
            inTeiHeader = true;
        } else {
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(upperQname, upperTag);
                }
            }

            if (qName.equals("front")) {
                currentTag = "<header>";
                upperTag = currentTag;
                upperQname = "front";
            } else if (qName.equals("body")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("titlePage")) {
                currentTag = "<cover>";
            } else if (qName.equals("other")) {
                currentTag = "<other>";
            } else if (qName.equals("toc")) {
                currentTag = "<toc>";
                upperTag = currentTag;
                upperQname = "div";
            } else if (qName.equals("note")) {
                int length = atts.getLength();
                for (int i = 0; i < length; i++) {
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);
                    if (name != null) {
                        if (name.equals("place")) {
                            if (value.equals("footnote") || value.equals("foot")) {
                                currentTag = "<footnote>";
                            } else if (value.equals("headnote") || value.equals("head")) {
                                currentTag = "<headnote>";
                            } else if (value.equals("margin")) {
                                currentTag = "<marginnote>";
                            } else {
                                logger.error("Invalid attribute value for element note: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element note: " + name);
                        }
                    }
                }
            } else if (qName.equals("div")) {
                int length = atts.getLength();
                for (int i = 0; i < length; i++) {
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);
                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("annex")) {
                                currentTag = "<annex>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("funding")) {
                                currentTag = "<funding>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (Arrays.asList("availability", "data_availability", "data-availability")
                                    .contains(value)) {
                                currentTag = AVAILABILITY_LABEL;
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("acknowledgement") || value.equals("acknowledgements")
                                    || value.equals("acknowledgment")
                                    || value.equals("acknowledgments")) {
                                currentTag = "<acknowledgement>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("conflict") || value.equals("conflicts")) {
                                currentTag = "<conflict>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("contribution") || value.equals("contributions")) {
                                currentTag = "<contribution>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else if (value.equals("toc")) {
                                currentTag = "<toc>";
                                upperTag = currentTag;
                                upperQname = "div";
                            } else {
                                logger.error("Invalid attribute value for element div: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element div: " + name);
                        }
                    }
                }
            } else if (qName.equals("page") || qName.equals("pages")) {
                currentTag = "<page>";
            } else if (qName.equals("listBibl")) {
                currentTag = "<references>";
                upperTag = currentTag;
                upperQname = "listBibl";
            } else if (qName.equals("text")) {
                currentTag = "<other>";
                upperTag = null;
                upperQname = null;
            }
        }
    }

    /**
     * Token-level writeData: outputs ALL tokens per line using GROBID delimiters,
     * not just the first token. This matches the tokenization used by the PDF
     * layout analysis pipeline (pdfalto + GrobidDefaultAnalyzer).
     */
    private void writeData(String qName, String surfaceTag) {
        if (qName == null) {
            qName = "other";
            surfaceTag = "<other>";
        }
        if ((qName.equals("front")) || (qName.equals("titlePage")) || (qName.equals("note")) ||
                (qName.equals("page")) || (qName.equals("pages")) || (qName.equals("body")) ||
                (qName.equals("listBibl")) || (qName.equals("div")) ||
                (qName.equals("other")) || (qName.equals("toc"))) {
            String text = getText();
            text = text.replace("\n", " ");
            text = text.replace("\r", " ");
            text = text.replace("  ", " ");
            boolean begin = true;

            // segment the text line by line first
            String[] lines = text.split("\\+L\\+");
            boolean page = false;
            for (int p = 0; p < lines.length; p++) {
                String line = lines[p].trim();
                if (line.length() == 0)
                    continue;
                if (line.equals("\n") || line.equals("\r"))
                    continue;
                if (line.indexOf("+PAGE+") != -1) {
                    line = line.replace("+PAGE+", "");
                    page = true;
                }

                // tokenize using GROBID delimiters (including punctuation as separate tokens)
                StringTokenizer st = new StringTokenizer(line, TextUtilities.delimiters, true);
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken().trim();

                    // skip whitespace-only and empty tokens
                    if (tok.length() == 0)
                        continue;
                    if (tok.equals(" ") || tok.equals("\t") || tok.equals("\n") || tok.equals("\r") ||
                            tok.equals("\u00A0") || tok.equals("\u200C"))
                        continue;

                    if (surfaceTag == null) {
                        surfaceTag = "<other>";
                    }

                    if (begin && (!surfaceTag.equals("<other>"))) {
                        labeled.add(tok + " I-" + surfaceTag + "\n");
                        begin = false;
                    } else {
                        labeled.add(tok + " " + surfaceTag + "\n");
                    }
                }

                if (page) {
                    page = false;
                }
            }
            accumulator.setLength(0);
        }
    }

}
