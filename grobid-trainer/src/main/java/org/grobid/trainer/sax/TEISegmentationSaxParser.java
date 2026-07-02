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
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser for the TEI format for the training data for the segmentation model.
 * Normally all training data should be in this unique format.
 * The segmentation of tokens must be identical as the one from pdf2xml files so that
 * training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEISegmentationSaxParser extends DefaultHandler {

    /* TEI -> label mapping (10 labels for this model)
    	cover page (<cover>): titlePage (optionally under front),
    	document header (<header>): front,
    	page footer (<footnote>): note type footnote,
    	page header (<headnote>): note type headnote,
        margin note (<marginnote>): note type margin,
    	document body (<body>): body,
    	bibliographical section (<references>): listbibl,
    	page number (<page>): page,
    	? each bibliographical references in the biblio section (<ref>): bibl
    	annexes (<annex>): div type="annex" (optionally under back)
    	data availability (<availability>): div type="availability"
    	acknowledgement (<acknowledgement>): div type="acknowledgement" (optionally under back)
    	conflict of interest / declaration of interest (<conflict>): div type="conflict"
    	authors contribution (<contribution>): div type="contribution"
    */

    private static final Logger logger = LoggerFactory.getLogger(TEISegmentationSaxParser.class);

    private StringBuffer accumulator = null; // current accumulated text

    private String output = null;
    //private Stack<String> currentTags = null;
    private String currentTag = null;
    private String upperQname = null;
    private String upperTag = null;
    private List<String> labeled = null; // store line by line the labeled data
    private boolean inTeiHeader = false; // flag to track when we're inside teiHeader

    public TEISegmentationSaxParser() {
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
                //currentTags.push("<header>");
                currentTag = "<header>";
                upperTag = currentTag;
                upperQname = "front";
            } else if (qName.equals("body")) {
                //currentTags.push("<other>");
                currentTag = "<body>";
                upperTag = currentTag;
                upperQname = "body";
            } else if (qName.equals("titlePage")) {
                //currentTags.push("<other>");
                currentTag = "<cover>";
                //upperTag = currentTag;
                //upperQname = "titlePage";
            } else if (qName.equals("other")) {
                //currentTags.push("<other>");
                currentTag = "<other>";
            } else if (qName.equals("toc")) {
                // normally valid table of content mark-up should be <div type="toc>", not tag <toc>
                //currentTags.push("<other>");
                currentTag = "<toc>";
                upperTag = currentTag;
                upperQname = "div";
            } else if (qName.equals("note")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
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

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
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
            } /*else {
                logger.error("Invalid element name: " + qName + " - it will be mapped to the label <other>");
                currentTag = "<other>";
              }*/
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

                /*StringTokenizer st = new StringTokenizer(line, TextUtilities.delimiters, true);
                if (!st.hasMoreTokens())
                    continue;
                String tok = st.nextToken().trim();*/

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
                    //labeled.add("@newpage\n");
                    page = false;
                }
                //}
            }
            accumulator.setLength(0);
        }
    }

}
