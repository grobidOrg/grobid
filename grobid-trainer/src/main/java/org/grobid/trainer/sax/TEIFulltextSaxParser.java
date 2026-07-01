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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for the TEI format for fulltext data encoded for training. Normally all training data should
 * be in this unique format for the fulltext model.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIFulltextSaxParser extends DefaultHandler {
    private static final Logger logger = LoggerFactory.getLogger(TEIFulltextSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;
    private String currentTag = null;

    private boolean figureBlock = false;
    private boolean tableBlock = false;
    private boolean inTeiHeader = false; // flag to track when we're inside teiHeader

    private ArrayList<String> labeled = null; // store line by line the labeled data

    public TEIFulltextSaxParser() {
        labeled = new ArrayList<>();
        currentTags = new Stack<>();
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
            //System.out.println(accumulator.toString().trim());
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if (qName.equals("teiHeader")) {
            inTeiHeader = false;
            return;
        }

        if (inTeiHeader) {
            // Skip processing of all content inside teiHeader
            return;
        }

        if ((!qName.equals("lb")) && (!qName.equals("pb")) && (!qName.equals("space"))) {
            writeData(qName, true);
            if (!currentTags.empty()) {
                currentTag = currentTags.peek();
            }
        }

        if (qName.equals("figure") || qName.equals("table")) {
            figureBlock = false;
            tableBlock = false;
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (inTeiHeader) {
            // Skip processing of all elements inside teiHeader
            return;
        }

        if (qName.equals("lb")) {
            //accumulator.append(" +LINE+ ");
            accumulator.append(" ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else if (qName.equals("teiHeader")) {
            inTeiHeader = true;
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(qName, false);
                }
            }
            accumulator.setLength(0);

            if (qName.equals("div")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("paragraph")) {
                                currentTags.push("<paragraph>");
                                currentTag = "<paragraph>";
                            } else {
                                logger.error("Invalid attribute value for element div: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element div: " + name);
                        }
                    }
                }
            } else if (qName.equals("p")) {
                currentTags.push("<paragraph>");
                currentTag = "<paragraph>";
            } else if (qName.equals("ref")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("biblio")) {
                                currentTags.push("<citation_marker>");
                                currentTag = "<citation_marker>";
                            } else if (value.equals("figure")) {
                                currentTags.push("<figure_marker>");
                                currentTag = "<figure_marker>";
                            } else if (value.equals("table")) {
                                currentTags.push("<table_marker>");
                                currentTag = "<table_marker>";
                            } else if (value.equals("formula") || value.equals("equation")) {
                                currentTags.push("<equation_marker>");
                                currentTag = "<equation_marker>";
                            } else if (value.equals("section")) {
                                currentTags.push("<section_marker>");
                                currentTag = "<section_marker>";
                            } else {
                                logger.error("Invalid attribute value for element ref: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element ref: " + name);
                        }
                    }
                }
            } else if (qName.equals("formula")) {
                currentTags.push("<equation>");
                currentTag = "<equation>";
            } else if (qName.equals("label")) {
                currentTags.push("<equation_label>");
                currentTag = "<equation_label>";
            } else if (qName.equals("head")) {
                {
                    currentTags.push("<section>");
                    currentTag = "<section>";
                }
            } else if (qName.equals("table")) {
                currentTags.push("<table>");
                currentTag = "<table>";
                tableBlock = true;
                figureBlock = false;
            } else if (qName.equals("item")) {
                currentTags.push("<paragraph>");
                currentTag = "<paragraph>";
                //currentTags.push("<item>");
                //currentTag = "<item>";
            } else if (qName.equals("figure")) {
                figureBlock = true;
                tableBlock = false;
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if (name != null) {
                        if (name.equals("type")) {
                            if (value.equals("table")) {
                                tableBlock = true;
                            } else {
                                logger.error("Invalid attribute value for element figure: " + name + "=" + value);
                            }
                        } else {
                            logger.error("Invalid attribute name for element figure: " + name);
                        }
                    }
                }
                if (tableBlock) {
                    figureBlock = false;
                    currentTags.push("<table>");
                    currentTag = "<table>";
                } else {
                    currentTags.push("<figure>");
                    currentTag = "<figure>";
                }
            } else if (qName.equals("other")) {
                currentTags.push("<other>");
                currentTag = "<other>";
            } else if (qName.equals("text")) {
                currentTags.push("<other>");
                currentTag = "<other>";
            } else {
                if (!StringUtils.equalsIgnoreCase(qName, "tei") && !qName.equals("teiHeader")
                        && !qName.equals("fileDesc") && !qName.equals("list")) {
                    logger.error("Invalid element name: " + qName + " - it will be mapped to the label <other>");
                    currentTags.push("<other>");
                    currentTag = "<other>";
                }
            }
        }

    }

    private void writeData(String qName, boolean pop) {
        if ((qName.equals("other")) || (qName.equals("p")) ||
                (qName.equals("ref")) || (qName.equals("head")) || (qName.equals("figure")) ||
                (qName.equals("paragraph")) ||
                (qName.equals("div")) || //(qName.equals("figDesc")) ||
                (qName.equals("table")) || //(qName.equals("trash")) ||
                (qName.equals("formula")) || (qName.equals("item")) || (qName.equals("label"))) {
            if (currentTag == null) {
                return;
            }

            if (pop) {
                if (!currentTags.empty()) {
                    currentTags.pop();
                }
            }

            // adjust tag (conservative)
            if (tableBlock) {
                currentTag = "<table>";
            } else if (figureBlock) {
                currentTag = "<figure>";
            }

            String text = getText();
            // we segment the text
            StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
            boolean begin = true;
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                if (tok.length() == 0)
                    continue;

                /*if (tok.equals("+LINE+")) {
                    labeled.add("@newline\n");
                } else*/ {
                    String content = tok;
                    int i = 0;
                    if (content.length() > 0) {
                        if (begin) {
                            labeled.add(content + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add(content + " " + currentTag + "\n");
                        }
                    }
                }
                begin = false;
            }
            accumulator.setLength(0);
        }
    }

}
