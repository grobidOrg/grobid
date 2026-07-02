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

import java.io.*;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.exceptions.GrobidException;

/**
 * SAX parser for XML Wikipedia page articles (file .hgw.xml). Grab a definition for the page ID.
 *
 * @author Patrice Lopez
 */
public class WikiTextExtractSaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String PageID = null;
    private String lang = null;
    private Writer writer = null;
    private boolean textBegin = false;
    private int page = 0;
    private String path = null;
    private int fileCount = 0;

    public WikiTextExtractSaxParser() {
    }

    public WikiTextExtractSaxParser(String p) {
        path = p;
    }

    public void characters(char[] buffer, int start, int length) {
        if (textBegin)
            accumulator.append(buffer, start, length);
    }

    //static final String INSERT_PAGEDEF_SQL =
    //	"UPDATE wiki_page SET def = ? WHERE PageID=?";

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if (qName.equals("text")) {
            textBegin = false;

            // we have a big piece of accumulated text, we dump it in the corpus file after some cleaning
            String blabla = accumulator.toString();

            StringTokenizer st = new StringTokenizer(blabla, "\n");
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                //System.out.println(line);
                if (line.length() == 0)
                    continue;

                if (line.startsWith("__")) {
                    continue;
                }

                if (line.startsWith("PMID")) {
                    continue;
                }

                if (line.startsWith("#")) {
                    continue;
                }

                String line0 = "";
                boolean end = false;
                int pos = 0;
                while (!end) {
                    int ind = line.indexOf("[", pos);
                    //System.out.println("ind: " + ind);
                    if (ind != -1) {
                        int inde = line.indexOf(']', pos);
                        //System.out.println("inde: " + inde);
                        if (inde != -1) {
                            line0 += line.substring(pos, ind);
                            pos = inde + 2;
                        } else {
                            line0 += line.substring(pos, ind) + line.substring(inde + 1, line.length());
                            end = true;
                        }
                    } else {
                        //System.out.println("pos: " + pos);
                        if (pos < line.length() - 1)
                            line0 += line.substring(pos, line.length());
                        end = true;
                    }
                }
                line = line0.trim();

                if (line.indexOf("|") != -1)
                    continue;

                if (line.startsWith("poly"))
                    continue;

                for (int i = 0; i < 5; i++) {
                    if ((line.startsWith(".")) | (line.startsWith("*")) | (line.startsWith(":")) |
                            (line.startsWith("\"")) | (line.startsWith(";"))) {
                        line = line.substring(1, line.length());
                        line = line.trim();
                    }
                }

                //System.out.println(line);

                if ((line.length() > 0) & (!line.startsWith("Help")) & (!line.startsWith("NONE"))
                        & (!line.startsWith("beg")) & (!line.startsWith(": See also"))
                        & (!line.startsWith(": \"See also"))
                        & (!line.startsWith(":See also")) & (!line.startsWith("Wiktionary"))
                        & (!line.startsWith("subgroup"))) {
                    // do we need some more cleaning ?
                    try {
                        writer.write(line);
                        writer.write("\n");
                        writer.flush();
                    } catch (Exception e) {
                        //						e.printStackTrace();
                        throw new GrobidException("An exception occurred while running Grobid.", e);
                    }
                }
            }
            // reinit
            PageID = null;
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("page")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("id")) {
                        PageID = value;

                        if (page > 4000) {
                            // reinit
                            page = 0;
                        }

                        if (page == 0) {
                            try {
                                // open a new file
                                if (writer != null)
                                    writer.close();
                                File file = new File(path + "text-" + fileCount + ".txt");
                                System.out.println(path + "text-" + fileCount + ".txt");
                                OutputStream os = new FileOutputStream(file, false);
                                writer = new OutputStreamWriter(os, "UTF-8");
                                fileCount++;
                            } catch (Exception e) {
                                //                   				e.printStackTrace();
                                throw new GrobidException("An exception occurred while running Grobid.", e);
                            }
                        }
                        page++;
                    }
                }
            }
        } else if (qName.equals("text")) {
            textBegin = true;
            accumulator.setLength(0); // we start to buffer text, no need to buffer the rest
        } else {
            // mmm.... nothing else ?
        }
    }

}
