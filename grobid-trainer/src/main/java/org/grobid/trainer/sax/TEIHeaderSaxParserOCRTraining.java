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

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.exceptions.GrobidException;

/**
 * SAX parser for the TEI format header data. Normally all training data should be in this unique format which
 * replace the ugly CORA format. Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * @author Patrice Lopez
 */
public class TEIHeaderSaxParserOCRTraining extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;
    private Writer writer = null;

    // output the training data sets for OCR post corrections
    private Writer writer_affiliations = null;
    private Writer writer_addresses = null;
    private Writer writer_keywords = null;
    private Writer writer_authors = null;
    private Writer writer_notes = null;

    public static String punctuations = ",:;?.!)-\"']";

    public TEIHeaderSaxParserOCRTraining() {
    }

    public TEIHeaderSaxParserOCRTraining(Writer writ1,
            Writer writ2,
            Writer writ3,
            Writer writ4,
            Writer writ5) {
        writer_affiliations = writ1;
        writer_addresses = writ2;
        writer_keywords = writ3;
        writer_authors = writ4;
        writer_notes = writ5;
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if ((qName.equals("titlePart")) | (qName.equals("note")) | (qName.equals("byline")) |
                (qName.equals("affiliation")) | (qName.equals("address")) | (qName.equals("email")) |
                (qName.equals("idno")) | (qName.equals("date")) | (qName.equals("biblScope")) |
                (qName.equals("keywords")) | (qName.equals("ptr")) | (qName.equals("div")) | (qName.equals("title"))) {
            // we register in the DB the new entry
            String text = getText();
            Writer writer = null;

            if (qName.equals("affiliation"))
                writer = writer_affiliations;
            else if (qName.equals("address"))
                writer = writer_addresses;
            else if (qName.equals("keywords"))
                writer = writer_keywords;
            else if (currentTag.equals("<author>"))
                writer = writer_authors;
            else if (qName.equals("note"))
                writer = writer_notes;

            if (writer != null) {
                try {
                    // we segment the text
                    StringTokenizer st = new StringTokenizer(text, " \n\t");
                    while (st.hasMoreTokens()) {
                        String tok = st.nextToken().trim();
                        if (tok.length() == 0)
                            continue;

                        boolean punct1 = false;

                        if (tok.equals("+L+")) {
                            writer.write("\n");
                        } else if (tok.equals("+PAGE+")) {
                            //writer.write("@newpage\n");
                            writer.write("\n"); // page break should be a distinct feature
                        } else {
                            String content = tok;
                            int i = 0;
                            for (; i < punctuations.length(); i++) {
                                if (tok.length() > 0) {
                                    if (tok.charAt(tok.length() - 1) == punctuations.charAt(i)) {
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

                                    writer.write("(" + " ");
                                } else if ((tok.startsWith("[")) & (tok.length() > 1)) {
                                    if (punct1)
                                        content = tok.substring(1, tok.length() - 1);
                                    else
                                        content = tok.substring(1, tok.length());

                                    writer.write("[" + " ");
                                } else if ((tok.startsWith("\"")) & (tok.length() > 1)) {
                                    if (punct1)
                                        content = tok.substring(1, tok.length() - 1);
                                    else
                                        content = tok.substring(1, tok.length());

                                    writer.write("\"" + " ");
                                }
                            }

                            if (content.length() > 0)
                                writer.write(content + " ");

                            if (punct1) {
                                //writer.write(""+ punctuations.charAt(i) + " " + currentTag + "\n");
                                writer.write(tok.charAt(tok.length() - 1) + " ");
                            }
                        }

                    }
                    writer.write("\n");
                } catch (IOException e) {
                    //					e.printStackTrace();
                    throw new GrobidException("An exception occurred while running Grobid.", e);
                }
            }

            accumulator.setLength(0);
        } else if (qName.equals("lb")) {
            // we note a line break
            //try {
            //writer.write("@newline\n");
            accumulator.append(" +L+ ");
            //}
            //catch(IOException e) {
            //	e.printStackTrace();
            //}
            //accumulator.setLength(0);
        } else if (qName.equals("pb")) {
            // we note a page break
            //writer.write("@newpage\n");
            //try {
            //writer.write("@newline\n");
            accumulator.append(" +PAGE+ ");
            //}
            //catch(IOException e) {
            //	e.printStackTrace();
            //}
            //accumulator.setLength(0);
        }

        //accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("div")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("abstract")) {
                            currentTag = "<abstract>";
                        } else if (value.equals("intro")) {
                            currentTag = "<intro>";
                        } else if (value.equals("paragraph")) {
                            currentTag = "<other>";
                        }
                    }
                }
            }
            //accumulator.setLength(0);
        } else if (qName.equals("note")) {
            int length = atts.getLength();
            currentTag = "<note>";
            //accumulator.setLength(0);
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
                }
            }
            //accumulator.setLength(0);
        } else if (qName.equals("biblScope")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("vol")) {
                            currentTag = "<volume>";
                        } else if (value.equals("pp")) {
                            currentTag = "<pages>";
                        }
                    }
                }
            }
            //accumulator.setLength(0);
        } else if (qName.equals("titlePart")) {
            currentTag = "<title>";
            accumulator.setLength(0);
        } else if (qName.equals("idno")) {
            currentTag = "<pubnum>";
            //accumulator.setLength(0);
        } else if (qName.equals("docAuthor")) {
            currentTag = "<author>";
            //accumulator.setLength(0);
        } else if (qName.equals("affiliation")) {
            currentTag = "<affiliation>";
            //accumulator.setLength(0);
        } else if (qName.equals("address")) {
            currentTag = "<address>";
            //accumulator.setLength(0);
        } else if (qName.equals("email")) {
            currentTag = "<email>";
            //accumulator.setLength(0);
        } else if (qName.equals("date")) {
            currentTag = "<date>";
            //accumulator.setLength(0);
        } else if (qName.equals("keywords")) {
            currentTag = "<keyword>";
            //accumulator.setLength(0);
        } else if (qName.equals("title")) {
            currentTag = "<journal>";
            //accumulator.setLength(0);
        }
    }

}
