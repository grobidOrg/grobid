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

import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for date sequences encoded in the TEI format data.
 *
 * @author Patrice Lopez
 */
public class TEIDateSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String currentTag = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    public int n = 0;

    public TEIDateSaxParser() {
        labeled = new ArrayList<String>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if (((qName.equals("year")) || (qName.equals("month")) || (qName.equals("day"))) & (currentTag != null)) {
            String text = getText();
            writeField(text);
        }
        /*else if (qName.equals("lb")) {
            // we note a line break
            accumulator.append(" +L+ ");
        } else if (qName.equals("pb")) {
            accumulator.append(" +PAGE+ ");
        } */
        else if (qName.equals("date")) {
            String text = getText();
            if (text.length() > 0) {
                currentTag = "<other>";
                writeField(text);
            }
            labeled.add("\n \n");
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

        if (qName.equals("year")) {
            currentTag = "<year>";
        } else if (qName.equals("month")) {
            currentTag = "<month>";
        } else if (qName.equals("day")) {
            currentTag = "<day>";
        } else if (qName.equals("date")) {
            n++;
        }
    }

    private void writeField(String text) {
        // we segment the text
        //StringTokenizer st = new StringTokenizer(text, " \n\t");
        List<String> tokens = TextUtilities.segment(text, TextUtilities.punctuations);

        boolean begin = true;
        for (String tok : tokens) {
            tok = tok.trim();
            if (tok.length() == 0)
                continue;
            boolean punct1 = false;

            if (tok.equals("+L+")) {
                labeled.add("@newline\n");
            } else if (tok.equals("+PAGE+")) {
                // page break not relevant for authors
                labeled.add("@newline\n");
            } else {
                String content = tok;
                int i = 0;
                for (; i < TextUtilities.punctuations.length(); i++) {
                    if (tok.length() > 0) {
                        if (tok.charAt(tok.length() - 1) == TextUtilities.punctuations.charAt(i)) {
                            punct1 = true;
                            content = tok.substring(0, tok.length() - 1);
                            break;
                        }
                    }
                }
                if (tok.length() > 0) {
                    if ((tok.startsWith("(")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("(" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("(" + " " + currentTag + "\n");
                        }
                    } else if ((tok.startsWith("[")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("[" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("[" + " " + currentTag + "\n");
                        }
                    } else if ((tok.startsWith("\"")) & (tok.length() > 1)) {
                        if (punct1)
                            content = tok.substring(1, tok.length() - 1);
                        else
                            content = tok.substring(1, tok.length());
                        if (begin) {
                            labeled.add("\"" + " I-" + currentTag + "\n");
                            begin = false;
                        } else {
                            labeled.add("\"" + " " + currentTag + "\n");
                        }
                    }
                }

                if (content.length() > 0) {
                    if (begin) {
                        labeled.add(content + " I-" + currentTag + "\n");
                        begin = false;
                    } else {
                        labeled.add(content + " " + currentTag + "\n");
                    }
                }

                if (punct1) {
                    if (begin) {
                        labeled.add(tok.charAt(tok.length() - 1) + " I-" + currentTag + "\n");
                        begin = false;
                    } else {
                        labeled.add(tok.charAt(tok.length() - 1) + " " + currentTag + "\n");
                    }
                }
            }

            begin = false;
        }
    }

}
