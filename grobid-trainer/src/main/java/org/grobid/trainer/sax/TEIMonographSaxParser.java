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
import java.util.Stack;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for the TEI format for monograph data. Normally all training data should be in this unique format.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIMonographSaxParser extends DefaultHandler {

    //private Stack<StringBuffer> accumulators = null; // accumulated parsed piece of texts
    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    private Stack<String> currentTags = null;

    //private String fileName = null;
    //private String pdfName = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    public TEIMonographSaxParser() {
        labeled = new ArrayList<String>();
        currentTags = new Stack<String>();
        //accumulators = new Stack<StringBuffer>();
        accumulator = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        //if (accumulator != null)
        accumulator.append(buffer, start, length);
        System.out.println(accumulator.toString());
    }

    public String getText() {
        if (accumulator != null) {
            return accumulator.toString().trim();
        } else {
            return null;
        }
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if ((!qName.equals("lb")) & (!qName.equals("pb"))) {
            writeData(qName, true);
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
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(qName, false);
                }
            }
            accumulator.setLength(0);

            if (qName.equals("header")) {
                currentTags.push("<header>");
            } else if (qName.equals("other")) {
                currentTags.push("<other>");
            } else if (qName.equals("page_header")) {
                currentTags.push("<page_header>");
            } else if (qName.equals("page_footnote")) {
                currentTags.push("<page_footnote>");
            } else if (qName.equals("page") | qName.equals("pages")) {
                currentTags.push("<page>");
            } else if (qName.equals("reference")) {
                currentTags.push("<reference>");
            } else if (qName.equals("toc")) {
                currentTags.push("<toc>");
            } else if (qName.equals("index")) {
                currentTags.push("<index>");
            } else if (qName.equals("section")) {
                currentTags.push("<section>");
            }
        }
    }

    private void writeData(String qName, boolean pop) {
        if ((qName.equals("header")) | (qName.equals("other")) | (qName.equals("page_header")) |
                (qName.equals("page_footnote")) | (qName.equals("page")) | (qName.equals("pages")) |
                (qName.equals("reference")) |
                (qName.equals("toc")) | (qName.equals("index")) | (qName.equals("section"))) {
            String currentTag = null;
            if (pop) {
                currentTag = currentTags.pop();
            } else {
                currentTag = currentTags.peek();
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
        }
    }

}
