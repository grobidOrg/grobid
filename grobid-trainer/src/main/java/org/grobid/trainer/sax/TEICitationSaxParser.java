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

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.analyzers.*;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * SAX parser for the XML format for citation data. Normally all training data should be in this unique format which
 * replaces the ugly CORA format. Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 * <p/>
 * This is unfortunately not yet TEI...
 *
 * @author Patrice Lopez
 */
public class TEICitationSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private StringBuffer allContent = new StringBuffer();

    private String output = null;
    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation
    public int nbCitations = 0;

    public TEICitationSaxParser() {
        allTokens = new ArrayList<List<LayoutToken>>();
        allLabeled = new ArrayList<List<String>>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
        if (allContent != null) {
            allContent.append(buffer, start, length);
        }
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public List<List<String>> getLabeledResult() {
        return allLabeled;
    }

    public List<List<LayoutToken>> getTokensResult() {
        return allTokens;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        qName = qName.toLowerCase();

        if ((qName.equals("author")) || (qName.equals("authors")) || (qName.equals("orgname")) ||
                (qName.equals("title")) || (qName.equals("editor")) || (qName.equals("editors")) ||
                (qName.equals("booktitle")) || (qName.equals("date")) || (qName.equals("journal")) ||
                (qName.equals("institution")) || (qName.equals("tech")) || (qName.equals("volume")) ||
                (qName.equals("pages")) || (qName.equals("page")) || (qName.equals("pubplace")) ||
                (qName.equals("note")) || (qName.equals("web")) || (qName.equals("pages")) ||
                (qName.equals("publisher")) || (qName.equals("idno") || qName.equals("issue")) ||
                (qName.equals("pubnum")) || (qName.equals("biblscope")) || (qName.equals("ptr")) ||
                (qName.equals("keyword")) || (qName.equals("keywords"))) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("bibl")) {
            String text = getText();
            currentTag = "<other>";
            if (text.length() > 0) {
                writeField(text);
            }
            nbCitations++;
            allLabeled.add(labeled);
            allTokens.add(tokens);
            allContent = null;
        }

        accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        String text = getText();
        if (text.length() > 0) {
            currentTag = "<other>";
            writeField(text);
        }
        accumulator.setLength(0);

        qName = qName.toLowerCase();
        if (qName.equals("title")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("level")) {
                        if (value.equals("a")) {
                            currentTag = "<title>";
                        } else if (value.equals("j")) {
                            currentTag = "<journal>";
                        } else if (value.equals("m")) {
                            currentTag = "<booktitle>";
                        } else if (value.equals("s")) {
                            currentTag = "<series>";
                        }
                    }
                }
            }
        } else if ((qName.equals("author")) || (qName.equals("authors"))) {
            currentTag = "<author>";
        } else if (qName.equals("editor")) {
            currentTag = "<editor>";
        } else if (qName.equals("date")) {
            currentTag = "<date>";
        } else if ((qName.equals("keywords")) || (qName.equals("keyword"))) {
            currentTag = "<keyword>";
        } else if (qName.equals("orgname")) {
            // check if we have a collaboration
            boolean found = false;
            int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type")) {
                        if (value.equals("collaboration")) {
                            currentTag = "<collaboration>";
                            found = true;
                        }
                    }
                }
            }

            if (!found)
                currentTag = "<institution>";
        } else if (qName.equals("note")) {
            int length = atts.getLength();

            if (length == 0) {
                currentTag = "<note>";
            } else {
                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if ((name != null) && (value != null)) {
                        if (name.equals("type")) {
                            if (value.equals("report")) {
                                currentTag = "<tech>";
                            }
                        }
                    }
                }
            }
        } else if (qName.equals("biblscope")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type") || name.equals("unit")) {
                        if ((value.equals("vol")) || (value.equals("volume"))) {
                            currentTag = "<volume>";
                        } else if ((value.equals("issue")) || (value.equals("number"))) {
                            currentTag = "<issue>";
                        }
                        if (value.equals("pp") || value.equals("page")) {
                            currentTag = "<pages>";
                        }
                    }
                }
            }
        } else if (qName.equals("pubplace")) {
            currentTag = "<location>";
        } else if (qName.equals("publisher")) {
            currentTag = "<publisher>";
        } else if (qName.equals("ptr")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type")) {
                        if (value.equals("web")) {
                            currentTag = "<web>";
                        }
                    }
                }
            }
        } else if (qName.equals("idno") || qName.equals("pubnum")) {
            currentTag = "<pubnum>";
            String idnoType = null;
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type")) {
                        idnoType = value.toLowerCase();
                    }
                }
            }
            // TBD: keep the idno type for further exploitation
        } else if (qName.equals("bibl")) {
            accumulator = new StringBuffer();
            allContent = new StringBuffer();
            labeled = new ArrayList<String>();
            tokens = new ArrayList<LayoutToken>();
        }
        accumulator.setLength(0);
    }

    private void writeField(String text) {
        if (tokens == null) {
            // nothing to do, text must be ignored
            return;
        }

        // we segment the text
        List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);

        if (isEmpty(localTokens)) {
            localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language("en", 1.0));
        }

        if (isEmpty(localTokens)) {
            return;
        }

        boolean begin = true;
        for (LayoutToken token : localTokens) {
            tokens.add(token);
            String content = token.getText();
            if (content.equals(" ") || content.equals("\n")) {
                labeled.add(null);
                continue;
            }

            content = UnicodeUtil.normaliseTextAndRemoveSpaces(content);
            if (content.trim().length() == 0) {
                labeled.add(null);
                continue;
            }

            if (content.length() > 0) {
                if (begin) {
                    labeled.add("I-" + currentTag);
                    begin = false;
                } else {
                    labeled.add(currentTag);
                }
            }
        }
    }

}
