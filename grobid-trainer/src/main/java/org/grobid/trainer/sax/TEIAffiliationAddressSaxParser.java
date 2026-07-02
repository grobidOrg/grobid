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

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;

/**
 * SAX parser for affiliation+address sequences encoded in the TEI format data.
 * Segmentation of tokens must be identical as the one from pdf2xml files to that
 * training and online input tokens are identical.
 *
 * @author Patrice Lopez
 */
public class TEIAffiliationAddressSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private StringBuffer allContent = new StringBuffer();

    private String output = null;
    private String currentTag = null;

    private List<String> labeled = null; // store line by line the labeled data
    public List<List<OffsetPosition>> placesPositions = null; // list of offset positions of place names
    public List<List<OffsetPosition>> countriesPositions = null; // list of offset positions of country names
    public List<List<LayoutToken>> allTokens = null;

    //private Writer writerAddress = null; // writer for the address model
    private Writer writerCORA = null; // writer for conversion into TEI header model

    public int n = 0;
    public Lexicon lexicon = Lexicon.getInstance();

    public void setTEIHeaderOutput(Writer writ) {
        writerCORA = writ;
    }

    public TEIAffiliationAddressSaxParser() {
        labeled = new ArrayList<String>();
        placesPositions = new ArrayList<List<OffsetPosition>>();
        countriesPositions = new ArrayList<List<OffsetPosition>>();
        allTokens = new ArrayList<List<LayoutToken>>();
    }

    public void characters(char[] buffer, int start, int length) {
        StringBuffer localBuffer = new StringBuffer();
        localBuffer.append(buffer, start, length);
        String localText = localBuffer.toString();
        localText = localText.replace("\n\t", " ");
        localText = localText.replaceAll("( )+", " ");
        accumulator.append(localText);
        //accumulator.append(buffer, start, length);
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

    public List<List<OffsetPosition>> getPlacesPositions() {
        return placesPositions;
    }

    public List<List<OffsetPosition>> getCountriesPositions() {
        return countriesPositions;
    }

    public List<List<LayoutToken>> getAllTokens() {
        return allTokens;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if (((qName.equals("addrLine")) ||
                (qName.equals("settlement")) ||
                (qName.equals("region")) ||
                (qName.equals("postCode")) ||
                (qName.equals("postBox")) ||
                (qName.equals("marker")) ||
                (qName.equals("country") ||
                        (qName.equals("orgName"))))) {
            String text = getText();
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
            //accumulator.append(" @newline ");
            accumulator.append("\n");
        } else if (qName.equals("affiliation")) {
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
        } else if (qName.equals("author")) {
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
            labeled.add("\n \n");

            String allString = allContent.toString().trim();
            //allString = allString.replace("@newline", "");
            allString = allString.replace("@newline", "\n");
            //List<OffsetPosition> toto = lexicon.tokenPositionsCityNames(allString);
            List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(allString);
            placesPositions.add(lexicon.tokenPositionsLocationNames(tokens));
            countriesPositions.add(lexicon.tokenPositionsCountryNames(tokens));
            allTokens.add(tokens);
            allContent = null;
            allString = null;

            accumulator.setLength(0);
        } else {
            accumulator.setLength(0);
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
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
        //else {
        //	writeField("+++");
        //}

        if (qName.equals("orgName")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        value = value.toLowerCase();
                        if (value.equals("department") || value.equals("departement")) {
                            currentTag = "<department>";
                        } else if (value.equals("institution") || value.equals("institute")) {
                            currentTag = "<institution>";
                        } else if (value.equals("laboratory")) {
                            currentTag = "<laboratory>";
                        } else if (value.equals("consortium")) {
                            currentTag = "<institution>";
                        } else {
                            currentTag = null;
                        }
                    }
                }
            }
        } else if (qName.equals("affiliation")) {
            currentTag = null;
            accumulator.setLength(0);
            n++;
        } else if (qName.equals("addrLine") || qName.equals("addrline")) {
            currentTag = "<addrLine>";
        } else if (qName.equals("settlement")) {
            currentTag = "<settlement>";
        } else if (qName.equals("region")) {
            currentTag = "<region>";
        } else if (qName.equals("postCode") || qName.equals("postcode")) {
            currentTag = "<postCode>";
        } else if (qName.equals("postBox") || qName.equals("postbox")) {
            currentTag = "<postBox>";
        } else if (qName.equals("country")) {
            currentTag = "<country>";
        } else if (qName.equals("marker")) {
            currentTag = "<marker>";
        } else if (qName.equals("author")) {
            accumulator = new StringBuffer();
            allContent = new StringBuffer();
        } else {
            //currentTag = null;
        }
    }

    private void writeField(String text) {
        // we segment the text
        StringTokenizer st = new StringTokenizer(text, " \n\t" + TextUtilities.fullPunctuations, true);
        boolean begin = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("\n")) {
                labeled.add("@newline");
                continue;
            }

            tok = tok.trim();
            if (tok.length() == 0) {
                continue;
            }

            /*if (tok.equals("@newline")) {
                labeled.add("@newline");
            } else if (tok.equals("+PAGE+")) {
                // page break - no influence here
                labeled.add("@newline");
            } else*/
            {
                String content = tok;
                int i = 0;
                if (content.length() > 0) {
                    if (begin) {
                        labeled.add(content + " I-" + currentTag);
                        begin = false;
                    } else {
                        labeled.add(content + " " + currentTag);
                    }
                }
            }
            begin = false;
        }
    }

}
