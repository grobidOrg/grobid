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
package org.grobid.service.parser;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Xml2HtmlParser extends DefaultHandler {

    private static final String BR = "<br>";
    private static final String SLASH = "/";
    private static final String ROYAL_BLUE = "RoyalBlue";
    private static final String ORANGE = "Orange";
    private static final String EQUALS = "=";
    private static final String LESS_THAN = "&lt;";
    private static final String GREATER_THAN = "&gt;";
    private static final String SPACE = "&nbsp;";
    private static final String DARK_VIOLET = "DarkViolet";

    StringBuffer htmlOutput;

    int depth;

    boolean inline;

    /**
     * Constructor.
     */
    public Xml2HtmlParser() {
        htmlOutput = new StringBuffer();
        depth = 0;
        inline = false;
    }

    /**
     * Return the xml file formatted to be displayed as html.
     *
     * @return String.
     */
    public String getHTML() {
        return htmlOutput.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(char[] buffer, int start, int length)
            throws SAXException {
        final String value = new String(buffer, start, length);
        if (StringUtils.isBlank(value)) {
        } else if (value.length() <= 40 && StringUtils.isNotBlank(value)) {
            htmlOutput.append(value);
            inline = true;
        } else {
            htmlOutput.append(BR);
            String[] words = value.split("\\p{Space}");
            int cpt = 0;
            for (String currWord : words) {
                if (cpt == 0) {
                    indent();
                    space(3);
                }
                cpt += currWord.length() + 1;
                if (cpt < 100) {
                    htmlOutput.append(currWord);
                    htmlOutput.append(SPACE);
                } else {
                    cpt = 0;
                    htmlOutput.append(currWord);
                    if (!currWord.equals(words[words.length - 1])) {
                        htmlOutput.append(BR);
                    }
                }
            }
            inline = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {
        depth++;
        addTag(qName, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        endTag(qName);
        depth--;
    }

    protected void addTag(String qName, Attributes attributes) {
        htmlOutput.append(BR);
        indent();

        // Tag name
        htmlOutput.append("<font color=" + DARK_VIOLET + ">").append(LESS_THAN);
        htmlOutput.append(qName).append("</font>");

        // Attributes
        String name;
        String value;
        for (int i = 0; i < attributes.getLength(); i++) {
            name = attributes.getQName(i);
            value = attributes.getValue(i);
            htmlOutput.append(SPACE);
            htmlOutput.append("<font color=" + ORANGE + ">");
            htmlOutput.append(name).append(EQUALS).append("</font>");
            htmlOutput.append("<font color=" + ROYAL_BLUE + ">\"")
                    .append(value);
            htmlOutput.append("\"</font>");
        }

        // End of tag
        htmlOutput.append("<font color=" + DARK_VIOLET + ">")
                .append(
                        GREATER_THAN);
        htmlOutput.append("</font>");
    }

    protected void endTag(String qName) {
        if (inline) {
            inline = false;
        } else {
            htmlOutput.append(BR);
            indent();
        }

        htmlOutput.append("<font color=" + DARK_VIOLET + ">")
                .append(
                        LESS_THAN + SLASH);
        htmlOutput.append(qName).append(GREATER_THAN);
        htmlOutput.append("</font>");

    }

    /**
     * Add indentation to xml.
     */
    private void indent() {
        for (int i = 0; i < depth * 3; i++) {
            htmlOutput.append(SPACE);
        }
    }

    /**
     * Add space to xml.
     *
     * @param sapce
     *            number of spaces.
     */
    private void space(int nbSpace) {
        for (int i = 0; i < nbSpace; i++) {
            htmlOutput.append(SPACE);
        }
    }

}
