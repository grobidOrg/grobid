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
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for the TEI format for figure and table data encoded for training.
 *
 * @author Patrice Lopez
 */
public class TEIFigureSaxParser extends DefaultHandler {

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;
    private String currentTag = null;

    private boolean figureBlock = false;
    private boolean tableBlock = false;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> allTags = Arrays
            .asList("<figure_head>", "<figDesc>", "<content>", "<label>", "<note>", "<other>");;

    public TEIFigureSaxParser() {
        labeled = new ArrayList<String>();
        currentTags = new Stack<String>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
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
        if ((!qName.equals("lb")) && (!qName.equals("pb"))) {
            if (!currentTags.empty()) {
                currentTag = currentTags.peek();
            }
            writeData(currentTag, true);
        }

        if (qName.equals("figure")) {
            figureBlock = false;
            tableBlock = false;
            labeled.add("");
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(currentTag, false);
                }
            }
            accumulator.setLength(0);

            if (qName.equals("head")) {
                if (figureBlock || tableBlock) {
                    currentTags.push("<figure_head>");
                    currentTag = "<figure_head>";
                }
            } else if (qName.equals("figDesc")) {
                currentTags.push("<figDesc>");
                currentTag = "<figDesc>";
            } else if (qName.equals("table")) {
                currentTags.push("<content>");
                currentTag = "<content>";
            } else if (qName.equals("trash") || qName.equals("content")) {
                currentTags.push("<content>");
                currentTag = "<content>";
            } else if (qName.equals("label")) {
                currentTags.push("<label>");
                currentTag = "<label>";
            } else if (qName.equals("note")) {
                currentTags.push("<note>");
                currentTag = "<note>";
            } else if (qName.equals("figure")) {
                figureBlock = true;
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
                            }
                        }
                    }
                }
                if (tableBlock) {
                    figureBlock = false;
                }

                currentTags.push("<other>");
                currentTag = "<other>";
            } else {
                qName = qName.toLowerCase();
                if (!qName.equals("tei") && !qName.equals("teiheader") && !qName.equals("text")
                        && !qName.equals("filedesc"))
                    System.out.println("Warning, unknown xml tag in training file: " + qName);
            }
        }

    }

    private void writeData(String currentTag, boolean pop) {
        if (currentTag == null) {
            return;
        }
        if (allTags.contains(currentTag)) {

            if (pop) {
                if (!currentTags.empty()) {
                    currentTags.pop();
                }
            }

            String text = getText();
            // we segment the text
            StringTokenizer st = new StringTokenizer(text, " \n\t" + TextUtilities.fullPunctuations, true);
            boolean begin = true;
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                if (tok.length() == 0)
                    continue;

                if (tok.equals("+L+")) {
                    labeled.add("@newline\n");
                } else if (tok.equals("+PAGE+")) {
                    // page break should be a distinct feature
                    labeled.add("@newpage\n");
                } else {
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
        } else {
            System.out.println("Warning, unknown tag in training file: " + currentTag);
        }
    }

}
