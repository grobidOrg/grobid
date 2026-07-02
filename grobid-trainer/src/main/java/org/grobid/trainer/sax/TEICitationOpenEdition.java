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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX parser for the TEI format for OpenEdition citation data. Convert the data into Grobid TEI format for inclusion
 * into the training set.
 * <p/>
 *
 * @author Patrice Lopez
 */
public class TEICitationOpenEdition extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private StringBuffer buffer = null;
    private boolean isUrl = false;
    private boolean isIssue = false;

    public TEICitationOpenEdition() {
        buffer = new StringBuffer();
    }

    public String getTEI() {
        return buffer.toString();
    }

    public void cleanBuffer() {
        buffer = new StringBuffer();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString();
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        String text = getText();
        if (text.length() > 0) {
            buffer.append(text);
        }
        accumulator.setLength(0);
        if (qName.equals("bibl")) {
            buffer.append("</bibl>\n");
        } else if (qName.equals("title")) {
            buffer.append("</title>");
        } else if (qName.equals("author")) {
            buffer.append("</author>");
        } else if (qName.equals("editor")) {
            buffer.append("</editor>");
        } else if (qName.equals("date")) {
            buffer.append("</date>");
        } else if (qName.equals("biblScope")) {
            buffer.append("</biblScope>");

            if (isIssue) {
                isIssue = false;
                if (text.indexOf("(") != -1) {
                    System.err.println("warning, issue mixed with volume: " + text);
                }
            }
        } else if (qName.equals("publisher") || qName.equals("distributor") || qName.equals("sponsor")) {
            // for us distributor = publisher
            buffer.append("</publisher>");
        } else if (qName.equals("pubPlace")) {
            buffer.append("</pubPlace>");
        } else if (qName.equals("orgName")) {
            buffer.append("</orgName>");
        } else if (qName.equals("meeting")) {
            // for us a meeting name is encoded as monography title
            buffer.append("</title>");
        } else if (qName.equals("ref")) {
            // the ref element is a little bit difficult to process.
            // in OpenEdition, it seems that plain text url are encoded with <ref> element without any
            // attributes
            // Otherwise, when the url applies to another element, the attribute target is used.
            // A <ref> can apply to different fields, but sometimes no field at all is present, for example:
            // <bibl>CDU : <ref target="http://www.hannover2007.cdu.de/download/071203-beschluss-grundsatzprogramm-4.pdf">
            // <hi rend="italic">Grundsätze für Deutschland</hi></ref> </bibl>
            // -> we would encode "Grundsätze für Deutschland" as <title level="m">.
            if (isUrl) {
                buffer.append("</ptr>");
                isUrl = false;
            }
        } else if (qName.equals("extent")) {
            // so far we also tagged the page extent as usual page scope
            buffer.append("</biblScope>");
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts) throws SAXException {
        String text = getText();
        if (text.length() > 0) {
            buffer.append(text);
        }
        accumulator.setLength(0);

        if (qName.equals("bibl")) {
            buffer.append("/n/t/t<bibl>");
        } else if (qName.equals("title")) {
            int length = atts.getLength();
            buffer.append("<title");
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("level")) {
                        if (value.equals("u")) {
                            // unpublished works in OpenEdition are usually master thesis or reports
                            buffer.append(" level=\"m\">");
                        } else if (value.equals("s")) {
                            // we note series as journals because they are process the same in OpenURL
                            buffer.append(" level=\"j\">");
                        } else
                            buffer.append(" level=\"" + value + "\">");
                    }
                }
            }
            buffer.append(">");
        } else if (qName.equals("author")) {
            buffer.append("<author>");
        } else if (qName.equals("editor")) {
            buffer.append("<editor>");
        } else if (qName.equals("date")) {
            buffer.append("<date>");
        } else if (qName.equals("meeting")) {
            // for us a meeting name is encoded as monography title
            buffer.append("<title level=\"m\">");
        } else if (qName.equals("publisher") || qName.equals("distributor") || qName.equals("sponsor")) {
            // for us distributor = publisher = sponsor
            buffer.append("<publisher>");
        } else if (qName.equals("pubPlace")) {
            buffer.append("<pubPlace>");
        } else if (qName.equals("orgName")) {
            buffer.append("<orgName>");
        } else if (qName.equals("biblScope")) {
            int length = atts.getLength();
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) && (value != null)) {
                    if (name.equals("type")) {
                        if (value.equals("vol")) {
                            buffer.append("<biblScope type=\"vol\">");
                        } else if (value.equals("issue")) {
                            buffer.append("<biblScope type=\"issue\">");
                            // note: combination volume(issue) are often badly encoded as just issue, ex:
                            // <biblScope type="issue">82(3)</biblScope>
                            // which is volume 82 and issue 3
                            // we print a warning in these case to correct this by hand
                            isIssue = true;
                        } else if (value.equals("pp")) {
                            buffer.append("<biblScope type=\"pp\">");
                        } else {
                            System.err.println("warning, unexpected attribute value: " + name + "=" + value);
                        }
                    }
                }
            }
        } else if (qName.equals("extent")) {
            buffer.append("<biblScope type=\"pp\">");
        } else if (qName.equals("lb")) {
            buffer.append(" ");
        } else if (qName.equals("surname") || qName.equals("forename") || qName.equals("c") || qName.equals("edition")
                || qName.equals("abbr") || qName.equals("hi") || qName.equals("nameLink")
                || qName.equals("settlement") || qName.equals("country") || qName.equals("region")) {
            // we do nothing
        } else if (qName.equals("ref")) {
            // Process each attribute
            int length = atts.getLength();
            if (length == 0) {
                buffer.append("<ptr type=\"url\">");
                isUrl = true;
            }
        } else {
            System.err.println("warning, unexpected element value: " + qName);
        }

    }
}
