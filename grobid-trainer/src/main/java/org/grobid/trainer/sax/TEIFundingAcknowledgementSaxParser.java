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

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * SAX parser for funding and acknowledgement sequences encoded in the TEI format data.
 *
 * @author Patrice Lopez
 */
public class TEIFundingAcknowledgementSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation
    public int nbFundings = 0;

    public TEIFundingAcknowledgementSaxParser() {
        allTokens = new ArrayList<List<LayoutToken>>();
        allLabeled = new ArrayList<List<String>>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
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
        if (((qName.equals("funder")) || (qName.equals("grantName")) || (qName.equals("grantNumber"))
                || (qName.equals("projectName")) ||
                (qName.equals("programName")) || (qName.equals("individual")) || (qName.equals("institution"))
                || (qName.equals("affiliation")))
                && (currentTag != null)) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("funding") || qName.equals("acknowledgment")) {
            String text = getText();
            currentTag = "<other>";
            if (text.length() > 0) {
                writeField(text);
            }

            allLabeled.add(labeled);
            allTokens.add(tokens);
            nbFundings++;
        } else {
            System.out.println("Unsupported label: " + qName);
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

        if (qName.equals("funder")) {
            currentTag = "<funderName>";
        } else if (qName.equals("grantNumber")) {
            currentTag = "<grantNumber>";
        } else if (qName.equals("grantName")) {
            currentTag = "<grantName>";
        } else if (qName.equals("programName")) {
            currentTag = "<programName>";
        } else if (qName.equals("projectName")) {
            currentTag = "<projectName>";
        } else if (qName.equals("url")) {
            currentTag = "<url>";
        } else if (qName.equals("individual")) {
            currentTag = "<person>";
        } else if (qName.equals("institution")) {
            // default
            currentTag = "<institution>";
            // check the @type attribute for infrastructure
            int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type")) {
                        if (value.equals("infrastructure")) {
                            currentTag = "<infrastructure>";
                            break;
                        }
                    }
                }
            }
        } else if (qName.equals("affiliation")) {
            currentTag = "<affiliation>";
        } else if (qName.equals("funding") || qName.equals("acknowledgment")) {
            accumulator = new StringBuffer();
            labeled = new ArrayList<String>();
            tokens = new ArrayList<LayoutToken>();
        } else {
            currentTag = "<other>";
        }
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
