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

import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.PatentItem;
import org.grobid.core.exceptions.GrobidException;

/**
 *  This SAX parser mirror the input XML document, and add as extra annotation identified reference to
 *  patent and NPL. The possible tags within the chunk are removed to avoid hierarchical invalid documents.
 *
 */
public class PatentAnnotationSaxParser extends DefaultHandler {

    StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private Writer writer = null;
    private int offset = 0;
    private boolean counting = false;
    private ArrayList<PatentItem> patents = null;
    private ArrayList<BibDataSet> articles = null;

    // for getting track of the offset walk
    private int currentPatentIndex = 0;
    private int currentArticleIndex = 0;

    //private static String delimiters = " \n\t" + TextUtilities.fullPunctuations;

    public PatentAnnotationSaxParser() {
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public void setPatents(ArrayList<PatentItem> patents) {
        this.patents = patents;
    }

    public void setArticles(ArrayList<BibDataSet> articles) {
        this.articles = articles;
    }

    public String getText() {
        String text = accumulator.toString();
        if (text.trim().length() == 0) {
            return "";
        }
        /*text = text.replace("\n", " ");
        text = text.replace("  ", " ");*/
        if (counting) {
            /*
            StringTokenizer st = new StringTokenizer(text, delimiters, true);
            int count = 0;

            while(st.hasMoreTokens()) {
            	String token = st.nextToken().trim();
            	if (token.length() == 0) {
            		continue;
            	}
            	count++;
            }
            */

            int i = currentPatentIndex;
            int count = text.length();

            while (i < patents.size()) {
                PatentItem currentPatent = patents.get(i);
                if (currentPatent != null) {
                    int startOffset = currentPatent.getOffsetBegin();
                    int endOffset = currentPatent.getOffsetEnd();

                    if ((startOffset >= offset) && (endOffset <= offset + count)) {
                        String context = currentPatent.getContext();

                        /*System.out.println("OFFSET: " + offset);
                        System.out.println("count: " + count);
                        System.out.println("startOffset: " + startOffset);
                        System.out.println("endOffset: " + endOffset);
                        System.out.println("context: " + context);
                        System.out.println("text: " + text);*/

                        String target = "";
                        if (context.charAt(0) == ' ') {
                            target = " <ref type=\"patent\">" + context.substring(1, context.length()) + "</ref>";
                        } else {
                            target = "<ref type=\"patent\">" + context + "</ref>";
                        }

                        text = text.replace(context, target);
                        currentPatentIndex = i;
                    }
                }

                i++;
            }

            //i = currentArticleIndex;
            i = 0;
            while (i < articles.size()) {
                BibDataSet currentArticle = articles.get(i);
                if (currentArticle != null) {
                    List<Integer> offsets = currentArticle.getOffsets();
                    int startOffset = -1;
                    int endOffset = -1;
                    String context = currentArticle.getRawBib().trim();
                    if (offsets.size() > 0) {
                        if (offsets.get(0) != null) {
                            startOffset = offsets.get(0).intValue();
                            /*StringTokenizer stt = new StringTokenizer(context, delimiters, true);
                            int count2 = 0;
                            while(stt.hasMoreTokens()) {
                            	String token2 = stt.nextToken().trim();
                            	if (token2.length() == 0) {
                            		continue;
                            	}
                            	count2++;
                            }*/
                            //endOffset = offsets.get(1).intValue();
                            endOffset = startOffset + context.length();
                        }
                    }

                    //if ( (startOffset >= offset) && (endOffset <= offset+count) ) {
                    if ((startOffset >= offset)) {
                        /*System.out.println("OFFSET: " + offset);
                        System.out.println("count: " + count);
                        System.out.println("startOffset: " + startOffset);
                        System.out.println("endOffset: " + endOffset);
                        System.out.println("context: " + context);
                        System.out.println("text: " + text);*/

                        String target = " <ref type=\"npl\">" + context + "</ref> ";
                        text = text.replace(context, target);
                        currentArticleIndex = i;
                    }
                }

                i++;
            }

            offset += count;
        }

        return text;
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {

        try {
            if (qName.equals("p") || qName.equals("description")) {
                writer.write(getText());
                accumulator.setLength(0);
            }

            if (qName.equals("description")) {
                counting = false;
            }

            if (!counting) {
                writer.write(getText());
                accumulator.setLength(0);
                writer.write("</" + qName + ">\n");
            } else {
                if (qName.equals("row")) {
                    accumulator.append(" ");
                }
                if (qName.equals("p")) {
                    writer.write("\n");
                    accumulator.append(" ");
                }
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
            // we output the remaining text
            if (!counting) {
                writer.write(getText());
                accumulator.setLength(0);
            }
            if (!counting) {
                writer.write("<" + qName);

                int length = atts.getLength();

                // Process each attribute
                for (int i = 0; i < length; i++) {
                    // Get names and values for each attribute
                    String name = atts.getQName(i);
                    String value = atts.getValue(i);

                    if ((name != null) && (value != null)) {
                        writer.write(" " + name + "=\"" + value + "\"");
                    }
                }

                writer.write(">");
            }

            if (qName.equals("description")) {
                offset = 0;
                counting = true;
            } else if (qName.equals("patent-document")) {
                counting = false;
            }
        } catch (Exception e) {
            //		    e.printStackTrace();
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

}
