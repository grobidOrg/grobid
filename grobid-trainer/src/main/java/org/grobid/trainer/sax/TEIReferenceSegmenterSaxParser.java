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
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for reference strings encoded in the TEI format data for training purposes.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * @author Vyacheslav Zholudev
 */
public class TEIReferenceSegmenterSaxParser extends DefaultHandler {

    private StringBuilder accumulator = new StringBuilder(); // Accumulate parsed text
    private StringBuilder allContent = new StringBuilder();

    private String currentTag = null;

    private List<String> labeled = null; // store line by line the labelled data

    // The new TEI corpus files carry a full <teiHeader> with biblStruct, sourceDesc
    // <bibl>, <label> elements (e.g. "GROBID", "PDF-TEI Editor") and lots of free
    // text — all of which would otherwise be consumed and counted as references.
    private boolean inTeiHeader = false;

    //    public int n = 0;
    public Lexicon lexicon = Lexicon.getInstance();
    private int totalReferences = 0;


    public TEIReferenceSegmenterSaxParser() {
        labeled = new ArrayList<String>();
    }

    public void characters(char[] buffer, int start, int length) {
        if (inTeiHeader) {
            return;
        }
        accumulator.append(buffer, start, length);
        //if (allContent != null) {
        //	allContent.append(buffer, start, length);
        //}
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public List<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equals("teiHeader")) {
            inTeiHeader = false;
            accumulator.setLength(0);
            return;
        }
        if (inTeiHeader) {
            return;
        }

        if (qName.equals("label")) {
            String text = getText();
            writeField(text);
            if (allContent != null) {
                if (allContent.length() != 0) {
                    allContent.append(" ");
                }
                allContent.append(text);
            }
            accumulator.setLength(0);
        } else if (qName.equals("bibl")) {
            String text = getText();
            currentTag = "<reference>";
            writeField(text);
            if (allContent != null) {
                if (allContent.length() != 0) {
                    allContent.append(" ");
                }
                allContent.append(text);
            }
            accumulator.setLength(0);
        } else if (qName.equals("lb") || qName.equals("pb")) {
            // we note a line break
            accumulator.append(" @newline ");
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("teiHeader")) {
            inTeiHeader = true;
            accumulator.setLength(0);
            return;
        }
        if (inTeiHeader) {
            return;
        }
        if (!qName.equals("lb") && !qName.equals("pb")) {
            String text = getText();
            if (text.length() > 0) {
                currentTag = "<other>";
                writeField(text);
                if (allContent != null) {
                    if (allContent.length() != 0) {
                        allContent.append(" ");
                    }
                    allContent.append(text);
                }
            }
            accumulator.setLength(0);
        }
        if (qName.equals("bibl")) {
            currentTag = null;
            accumulator.setLength(0);
            totalReferences++;
        } else if (qName.equals("label")) {
            currentTag = "<label>";
        }
    }

    private void writeField(String text) {
        // Pre-split on the literal "@newline" sentinel: the tokenizer below uses
        // TextUtilities.fullPunctuations (which contains '@') as delimiters and would
        // otherwise shred the marker into "@" and "newline" — both then mislabeled.
        final String marker = "@newline";
        boolean begin = true;
        int start = 0;
        while (true) {
            int idx = text.indexOf(marker, start);
            String segment = (idx < 0) ? text.substring(start) : text.substring(start, idx);

            StringTokenizer st = new StringTokenizer(
                    segment, " \n\t" + TextUtilities.fullPunctuations, true);
            while (st.hasMoreTokens()) {
                String tok = st.nextToken().trim();
                if (tok.length() == 0) {
                    continue;
                }
                if (tok.equals("+PAGE+")) {
                    // page break - no influence here
                    labeled.add("@newline");
                } else if (begin) {
                    labeled.add(tok + " I-" + currentTag);
                    begin = false;
                } else {
                    labeled.add(tok + " " + currentTag);
                }
            }

            if (idx < 0) {
                break;
            }
            labeled.add("@newline");
            start = idx + marker.length();
        }
    }

    public int getTotalReferences() {
        return totalReferences;
    }
}
