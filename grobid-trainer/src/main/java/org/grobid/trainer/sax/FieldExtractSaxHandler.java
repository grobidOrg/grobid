package org.grobid.trainer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

/**
 * Utility SAX parser which extracts all the text content under a given TEI tag name or under a specified xml path.
 *
 * @author Patrice Lopez
 */
public class FieldExtractSaxHandler extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String field = null;

    private String xmlPath = null;

    private ArrayList<String> values = null; // store the content values for each tag occurrence

    public FieldExtractSaxHandler() {
        values = new ArrayList<String>();
    }

    public void setField(String f) {
        field = f;
    }

    public void setXmlPath(String path) {
        xmlPath = path;
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (field != null) {
            if (qName.equals(field)) {
                values.add(getText());
                accumulator.setLength(0);
            }
        }

    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (field != null) {
            if (qName.equals(field)) {
                accumulator.setLength(0);
            }
        }
    }

}
