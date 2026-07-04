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
package org.grobid.core.sax;

import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser for the XML description of country codes in ISO 3166.
 *
 */
public class CountryCodeSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String code = null;
    private String country = null;
    private Map countryCodes = null;
    private Set countries = null;

    private boolean isCode = false;
    private boolean isName = false;

    public CountryCodeSaxParser() {
    }

    public CountryCodeSaxParser(Map cc, Set co) {
        countryCodes = cc;
        countries = co;
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if (qName.equals("row")) {
            code = null;
            country = null;
            isCode = false;
            isName = false;
        } else if (qName.equals("cell")) {
            if (isCode)
                code = getText();
            else if (isName) {
                country = getText();
                if (country != null) {
                    country = country.toLowerCase();
                }
                countryCodes.put(country, code);
                if (!countries.contains(country)) {
                    countries.add(country);
                }
            }
        }
        accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("cell")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("role")) {
                        if (value.equals("a2code")) {
                            isCode = true;
                            isName = false;
                        } else if ((value.equals("name")) | (value.equals("nameAlt"))) {
                            isCode = false;
                            isName = true;
                        } else if (value.equals("a3code")) {
                            isCode = false;
                            isName = false;
                        }
                    }
                }
            }
        }
        accumulator.setLength(0);
    }

}
