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

import static org.grobid.core.engines.label.TaggingLabels.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for the TEI format header data encoded for training. Normally all training data for the header model
 * should be in this unique format (which replaces for instance the CORA format). Segmentation of tokens must be
 * identical as the one from pdf2xml files so that training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIHeaderSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private String fileName = null;
    //public TreeMap<String, String> pdfs = null;
    private String pdfName = null;
    private boolean inTeiHeader = false; // flag to track when we're inside teiHeader

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList(
            "titlePart",
            "note",
            "docAuthor",
            "affiliation",
            "address",
            "email",
            "idno",
            "date",
            "keywords",
            "keyword",
            "reference",
            "ptr",
            "div",
            "editor",
            "meeting");

    private List<String> intermediaryTags = Arrays
            .asList("byline", "front", "lb", "tei", "TEI", "teiHeader", "fileDesc", "text", "byline", "docTitle", "p");

    private List<String> ignoredTags = Arrays
            .asList("location", "version", "web", "degree", "page", "title", "phone", "publisher");

    public TEIHeaderSaxParser() {
        labeled = new ArrayList<>();
    }

    public void characters(char[] buffer, int start, int length) {
        if (inTeiHeader) {
            return; // Skip all character data inside teiHeader
        }
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void setFileName(String name) {
        fileName = name;
    }

    public String getPDFName() {
        if (pdfName != null && pdfName.startsWith("_"))
            return pdfName.substring(1);
        return pdfName;
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(
            String uri,
            String localName,
            String qName) throws SAXException {
        if (qName.equals("teiHeader")) {
            inTeiHeader = false;
            return; // Exit teiHeader and resume normal processing
        }

        if (inTeiHeader) {
            // Skip processing of all other closing tags inside teiHeader
            return;
        }

        if (endTags.contains(qName)) {
            writeData();
            accumulator.setLength(0);
        } else if (qName.equals("front")) {
            // write remaining test as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<other>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
        } else {
            System.out.println(" **** Warning **** Unexpected closing tag " + qName);
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (inTeiHeader) {
            if (qName.equals("fileDesc")) {
                //We need to get the pdf name from the xml:id attribute
                for (int i = 0; i < atts.getLength(); i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);
                    if (StringUtils.equals(name, "xml:id")) {
                        this.pdfName = value;
                        return;
                    }
                }
            }
            return;
        }

        if (qName.equals("teiHeader")) {
            inTeiHeader = true;
            return;
        } else if (qName.equals("lb")) {
            accumulator.append(" ");
            /*} else if (qName.equals("space")) {
            accumulator.append(" ");*/
        } else {
            // add accumulated text as <other>
            String text = getText();
            if (StringUtils.isNotEmpty(text)) {
                currentTag = "<other>";
                writeData();
            }
            accumulator.setLength(0);
        }

        if (qName.equals("div")) {
            int length = atts.getLength();
            currentTag = "<other>";

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("abstract")) {
                            currentTag = "<abstract>";
                        } /*else if (value.equals("intro") || value.equals("introduction")) {
                            currentTag = "<intro>";
                          } else if (value.equals("paragraph")) {
                            currentTag = "<other>";
                          }*/
                        else
                            currentTag = "<other>";
                    }
                }
            }
        } else if (qName.equals("note")) {
            int length = atts.getLength();
            currentTag = "<other>";
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        /*if (value.equals("degree")) {
                            currentTag = "<degree>";
                        } else if (value.equals("dedication")) {
                            currentTag = "<dedication>";
                        } else*/
                        if (value.equals("submission")) {
                            currentTag = "<submission>";
                        } /*else if (value.equals("english-title")) {
                            currentTag = "<entitle>";
                          } else if (value.equals("other")) {
                            currentTag = "<note>";
                          }*/ else if (value.equals("reference")) {
                            currentTag = "<reference>";
                        } else if (value.equals("copyright")) {
                            currentTag = "<copyright>";
                        } else if (value.equals("funding")) {
                            currentTag = FUNDING_LABEL;
                        } else if (value.equals("conflict") || value.equals("conflicts")) {
                            currentTag = CONFLICT_OF_INTEREST_LABEL;
                        } else if (value.equals("contribution") || value.equals("contributions")) {
                            currentTag = AUTHOR_CONTRIBUTION_LABEL;
                        } /*else if (value.equals("acknowledgment")) {
                            currentTag = "<note>";
                          }*/ else if (value.equals("document_type") || value.equals("doctype")
                                || value.equals("docType") ||
                                value.equals("documentType") || value.equals("articleType")) {
                            currentTag = "<doctype>";
                        } /*else if (value.equals("version")) {
                            currentTag = "<version>";
                          } else if (value.equals("release")) {
                            currentTag = "<other>";
                          }*/ else if (value.equals("group")) {
                            currentTag = "<group>";
                        } else if (Arrays.asList("availability", "data_availability", "data-availability")
                                .contains(value)) {
                            currentTag = AVAILABILITY_LABEL;
                        } else
                            currentTag = "<other>";
                    }
                } else
                    currentTag = "<other>";
            }
        } else if (qName.equals("ptr")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("web")) {
                            currentTag = "<web>";
                        }
                    }
                } else
                    currentTag = "<other>";
            }
        } else if (qName.equals("titlePart")) {
            currentTag = "<title>";
        } else if (qName.equals("idno")) {
            currentTag = "<pubnum>";
        } else if (qName.equals("reference")) {
            currentTag = "<reference>";
        } /*else if (qName.equals("degree")) {
            currentTag = "<degree>";
          }*/ else if (qName.equals("docAuthor")) {
            currentTag = "<author>";
        } /*else if (qName.equals("web")) {
            currentTag = "<web>";
          }*/ else if (qName.equals("affiliation")) {
            currentTag = "<affiliation>";
            accumulator.setLength(0);
        } else if (qName.equals("address")) {
            currentTag = "<address>";
            accumulator.setLength(0);
        } else if (qName.equals("email")) {
            currentTag = "<email>";
        } else if (qName.equals("meeting")) {
            currentTag = "<meeting>";
        } /*else if (qName.equals("location")) {
            currentTag = "<location>";
          }*/ else if (qName.equals("editor")) {
            currentTag = "<editor>";
        } else if (qName.equals("date")) {
            currentTag = "<date>";
            /*int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("submission")) {
                            currentTag = "<date-submission>";
                        } else if (value.equals("download")) {
                            currentTag = "<date-download>";
                        }
                    }
                }
            }*/
        } /*else if (qName.equals("p")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("intro") || value.equals("introduction")) {
                            currentTag = "<intro>";
                        }
                    }
                }
            }
          }*/ else if ((qName.equals("keywords")) || (qName.equals("keyword"))) {
            currentTag = "<keyword>";
        } /*else if (qName.equals("title")) {
            // only <title level="j"> for the moment, so don't need to check the attribute value
            currentTag = "<journal>";
          } else if (qName.equals("page")) {
            currentTag = "<page>";
          } else if (qName.equals("phone")) {
            currentTag = "<phone>";
          } else if (qName.equals("publisher")) {
            currentTag = "<publisher>";
          }*/
        else if (qName.equals("fileDesc")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("xml:id")) {
                        pdfName = value;
                    }
                }
            }
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
            currentTag = "<other>";
        } else {
            System.out.println("Warning: Unexpected starting tag " + qName);
            currentTag = "<other>";
        }
    }

    private void writeData() {
        if (currentTag == null) {
            return;
        }

        String text = getText();
        // we segment the text
        StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
        boolean begin = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            if (tok.length() == 0)
                continue;

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
            begin = false;
        }
        accumulator.setLength(0);
    }

}
