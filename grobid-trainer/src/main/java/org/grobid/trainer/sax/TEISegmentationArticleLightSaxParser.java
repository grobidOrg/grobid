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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TEISegmentationArticleLightSaxParser extends TEISegmentationSaxParser {

    private static final Logger logger = LoggerFactory.getLogger(TEISegmentationArticleLightSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null; // store line by line the labeled data
    private boolean inTeiHeader = false; // flag to track when we're inside teiHeader

    public TEISegmentationArticleLightSaxParser() {
        labeled = new ArrayList<String>();
        //currentTags = new Stack<String>();
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
            String uri,
            String localName,
            String qName) throws SAXException {
        if (qName.equals("teiHeader")) {
            inTeiHeader = false;
            return;
        }

        if (inTeiHeader) {
            // Skip processing of all content inside teiHeader
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
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("space")) {
            accumulator.append(" ");
        } else if (qName.equals("teiHeader")) {
            inTeiHeader = true;
        } else {
            // we have to write first what has been accumulated yet with the upper-level tag
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    writeData(upperQname, upperTag);
                }
            }
            //accumulator.setLength(0);

            if (qName.equals("front")) {
                currentTag = "<header>";
                upperTag = currentTag;
                upperQname = "front";
            } else if (qName.equals("body")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("titlePage")) {
                currentTag = "<body>";
            } else if (qName.equals("other")) {
                currentTag = "<other>";
            } else if (qName.equals("toc")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("note")) {
                currentTag = "<body>";
                upperQname = "body";
            } else if (qName.equals("div")) {
                currentTag = "<body>";
                upperQname = "body";
            } else if (qName.equals("page") || qName.equals("pages")) {
                currentTag = "<body>";
            } else if (qName.equals("listBibl")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("text")) {
                currentTag = "<body>";
                upperTag = null;
                upperQname = "body";
            } else if (qName.equals("div")) {
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            }
        }
    }

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

            //System.out.println(text);
            // we segment the text line by line first
            //StringTokenizer st = new StringTokenizer(text, "\n", true);
            String[] tokens = text.split("\\+L\\+");
            //while (st.hasMoreTokens()) {
            boolean page = false;
            for (int p = 0; p < tokens.length; p++) {
                //String line = st.nextToken().trim();
                String line = tokens[p].trim();
                if (line.length() == 0)
                    continue;
                if (line.equals("\n") || line.equals("\r"))
                    continue;
                if (line.indexOf("+PAGE+") != -1) {
                    // page break should be a distinct feature
                    //labeled.add("@newpage\n");
                    line = line.replace("+PAGE+", "");
                    page = true;
                }

                //StringTokenizer st = new StringTokenizer(line, " \t");
                StringTokenizer st = new StringTokenizer(line, " \t\f\u00A0");
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken();

                if (tok.length() == 0)
                    continue;

                if (surfaceTag == null) {
                    // this token belongs to a chunk to ignored
                    //System.out.println("\twarning: surfaceTag is null for token '"+tok+"' - it will be tagged with label <other>");
                    surfaceTag = "<other>";
                }

                if (begin && (!surfaceTag.equals("<other>"))) {
                    labeled.add(tok + " I-" + surfaceTag + "\n");
                    begin = false;
                } else {
                    labeled.add(tok + " " + surfaceTag + "\n");
                }
                if (page) {
                    page = false;
                }
            }
            accumulator.setLength(0);
        }
    }
}
