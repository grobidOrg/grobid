package org.grobid.core.sax;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.*;

/**
 * Stupid SAX parser which accumulate the textual content for a patent document.
 * <p/>
 * As an option, it is possible to accumulate only the content under a given element name, for instance "description"
 * for getting the description of a patent XML document.
 *
 */
public class TextSaxParser extends DefaultHandler {

    StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private List<String> filters = null; // the name of elements for getting only the
                                         // corresponding text, this will also be used
    // for possible segmentations if more than one chunk of text is present under the
    // filter element(s)

    private boolean accumule = true;

    public String currentPatentNumber = null;
    public String country = null;

    private List<String> texts = null;

    public TextSaxParser() {
        texts = new ArrayList<>();
    }

    public void characters(char[] buffer, int start, int length) {
        if (accumule) {
            accumulator.append(buffer, start, length);
        }
    }

    public void setFilter(List<String> filt) {
        filters = filt;
        accumule = false;
    }

    public void addFilter(String filt) {
        if (filters == null)
            filters = new ArrayList<>();
        if (!filters.contains(filt))
            filters.add(filt);
        accumule = false;
    }

    public String getText() {
        String text = accumulator.toString().trim();
        // text = text.replace("\n", " ");
        text = text.replace("\t", " ");
        // text = text.replaceAll("\\p{Space}+", " ");
        text = text.replaceAll("( )+", " ");
        return text;
    }

    public List<String> getTexts() {
        return texts;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (filters.contains(qName)) {
            String localText = getText();
            if (localText.trim().length() > 0)
                texts.add(localText);
            accumulator.setLength(0);
            accumule = false;
        }
        if (accumule) {
            if (qName.equals("row") || qName.equals("p") || qName.equals("heading")) {
                accumulator.append(" ");
            }
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("patent-document")) {
            int length = atts.getLength();

            String docID = null;
            String docNumber = null;
            String kindCode = null;
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("country")) {
                        country = value;
                    } else if (name.equals("kind")) {
                        kindCode = value;
                    } else if (name.equals("doc-number") || name.equals("docnumber")) {
                        docNumber = value;
                    } else if (name.equals("id") || name.equals("ID")) {
                        docID = value;
                    }
                }
            }

            if ((country != null) && (docNumber != null)) {
                if (kindCode != null) {
                    currentPatentNumber = country + docNumber + kindCode;
                } else
                    currentPatentNumber = country + docNumber;
            } else if (docID != null) {
                currentPatentNumber = docID;
            }
        }

        if (filters.contains(qName)) {
            accumule = true;
        }
    }

}
