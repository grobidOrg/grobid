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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.exceptions.GrobidException;

/**
 * This SAX parser simply records the chemical class name stand off annotations and their corresponding word
 * identifiers.
 *
 * @author Patrice Lopez
 */
public class ChemicalClassNamesSaxParser extends DefaultHandler {

    private ArrayList<ArrayList<String>> chemicalWords = null;
    private ArrayList<String> localChemicalWords = null;
    private int numberEntities = 0;

    public ArrayList<ArrayList<String>> getChemicalClassNames() {
        return chemicalWords;
    }

    public int getNumberEntities() {
        return numberEntities;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        try {
            if (qName.equals("class-name")) {
                if ((localChemicalWords != null) && (localChemicalWords.size() > 0)) {
                    // we need to keep only the first and last word id for a given sequence
                    // note that the order of the word ids in this file do not respect the original word order
                    String idd1 = null;
                    String idd2 = null;
                    for (String idd : localChemicalWords) {
                        if (idd1 == null) {
                            idd1 = idd;
                        }
                        if (idd2 == null) {
                            idd2 = idd;
                        }
                        if (idd.length() < idd1.length()) {
                            idd1 = idd;
                        } else if (idd.length() > idd2.length()) {
                            idd2 = idd;
                        } else if (idd.compareToIgnoreCase(idd1) < 0) {
                            idd1 = idd;
                        } else if (idd.compareToIgnoreCase(idd2) > 0) {
                            idd2 = idd;
                        }
                    }
                    localChemicalWords = new ArrayList<String>();
                    localChemicalWords.add(idd1);
                    localChemicalWords.add(idd2);
                    chemicalWords.add(localChemicalWords);
                    //System.out.println(localChemicalWords);
                    localChemicalWords = null;
                }
                numberEntities++;
            }
        } catch (Exception e) {
            //		    e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts) throws SAXException {
        try {
            if (qName.equals("nite:root")) {
                chemicalWords = new ArrayList<ArrayList<String>>();
            } else if (qName.equals("class-name")) {
                localChemicalWords = new ArrayList<String>();
            } else if (qName.equals("nite:child")) {
                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if ((name != null) && (value != null)) {
                        if (name.equals("href")) {
                            // there are two notations to handle, one compact with .., one with one child per word
                            int ind = value.indexOf("..");
                            if (ind != -1) {
                                // we have a sequence with a first and last word id
                                int ind1 = value.indexOf("(");
                                int ind2 = value.indexOf(")");
                                String idd1 = value.substring(ind1 + 1, ind2);
                                localChemicalWords.add(idd1);

                                ind1 = value.indexOf("(", ind1 + 1);
                                ind2 = value.indexOf(")", ind2 + 1);
                                String idd2 = value.substring(ind1 + 1, ind2);
                                localChemicalWords.add(idd2);
                            } else {
                                ind = value.indexOf("(");
                                String idd = value.substring(ind + 1, value.length() - 1);
                                localChemicalWords.add(idd);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            //		    e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

}
