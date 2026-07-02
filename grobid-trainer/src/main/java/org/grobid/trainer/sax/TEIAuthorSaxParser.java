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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.analyzers.*;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * SAX parser for author sequences encoded in the TEI format data.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * @author Patrice Lopez
 */
public class TEIAuthorSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private List<String> labeled = null; // store token by token the labels
    private List<List<String>> allLabeled = null; // list of labels
    private List<LayoutToken> tokens = null;
    private List<List<LayoutToken>> allTokens = null; // list of LayoutToken segmentation

    private String title = null;
    private String affiliation = null;
    private String address = null;
    private String note = null;
    private String keywords = null;

    public int n = 0;

    public TEIAuthorSaxParser() {
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
        if ((qName.equals("firstname") || qName.equals("forename") || qName.equals("middlename")
                || qName.equals("title") ||
                qName.equals("suffix") || qName.equals("surname") || qName.equals("lastname") || qName.equals("marker")
                ||
                qName.equals("roleName")) & (currentTag != null)) {
            String text = getText();
            writeField(text);
        } else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } else if (qName.equals("author")) {
            String text = getText();
            if (text.length() > 0) {
                currentTag = "<other>";
                writeField(text);
            }
            allLabeled.add(labeled);
            allTokens.add(tokens);
            n++;
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

        if (qName.equals("title") | qName.equals("roleName")) {
            currentTag = "<title>";
        } else if (qName.equals("marker")) {
            currentTag = "<marker>";
        } else if (qName.equals("surname") || qName.equals("lastname")) {
            currentTag = "<surname>";
        } else if (qName.equals("middlename")) {
            currentTag = "<middlename>";
        } else if (qName.equals("forename") || qName.equals("firstname")) {
            currentTag = "<forename>";
        } else if (qName.equals("suffix")) {
            currentTag = "<suffix>";
        } else if (qName.equals("author")) {
            accumulator = new StringBuffer();
            labeled = new ArrayList<String>();
            tokens = new ArrayList<LayoutToken>();
        } else if (!qName.equals("analytic") && !qName.equals("biblStruct") &&
                !qName.equals("sourceDesc") && !qName.equals("fileDesc") &&
                !qName.equals("teiHeader") && !qName.equals("TEI") &&
                !qName.equals("persName") && !qName.equals("tei") && !qName.equals("lb")) {
            System.out.println("Warning, invalid tag: <" + qName + ">");
        }
    }

    private void writeField(String text) {
        // we segment the text
        //List<String> tokens = TextUtilities.segment(text, TextUtilities.punctuations);
        List<LayoutToken> localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        if ((localTokens == null) || (localTokens.size() == 0))
            localTokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text, new Language("en", 1.0));
        if ((localTokens == null) || (localTokens.size() == 0))
            return;

        boolean begin = true;
        for (LayoutToken token : localTokens) {
            if (tokens == null) {
                // should not be the case, it can indicate a structure problem in the training XML file
                tokens = new ArrayList<LayoutToken>();
                System.out.println("Warning: list of LayoutToken not initialized properly, parsing continue... ");
            }
            if (labeled == null) {
                // should not be the case, it can indicate a structure problem in the training XML file
                labeled = new ArrayList<String>();
                System.out.println("Warning: list of labels not initialized properly, parsing continue... ");
            }
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
