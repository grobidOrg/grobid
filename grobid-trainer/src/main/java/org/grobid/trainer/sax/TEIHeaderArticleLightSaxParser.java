package org.grobid.trainer.sax;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.utilities.TextUtilities;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * SAX parser for the TEI format header data encoded for training. Normally all training data for the header model
 * should be in this unique format (which replaces for instance the CORA format). Segmentation of tokens must be
 * identical as the one from pdf2xml files so that training and online input tokens are aligned.
 *
 * @author Patrice Lopez
 */
public class TEIHeaderArticleLightSaxParser extends TEIHeaderSaxParser {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private String output = null;
    private String currentTag = null;

    private String fileName = null;
    private String pdfName = null;

    private ArrayList<String> labeled = null; // store line by line the labeled data

    private List<String> endTags = Arrays.asList("titlePart", "docAuthor", "date", "idno");
    private List<String> tags = Arrays.asList(
            "titlePart",
            "note",
            "docAuthor",
            "affiliation",
            "address",
            "email",
            "idno",
            "date",
            "keywords",
            "keyword",
            "reference",
            "ptr",
            "div",
            "editor",
            "meeting");

    private List<String> intermediaryTags = Arrays
            .asList("byline", "front", "lb", "tei", "teiHeader", "fileDesc", "text", "byline", "docTitle", "p");

    private List<String> ignoredTags = Arrays
            .asList("location", "version", "web", "degree", "page", "title", "phone", "publisher");

    public TEIHeaderArticleLightSaxParser() {
        labeled = new ArrayList<>();
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void setFileName(String name) {
        fileName = name;
    }

    public String getPDFName() {
        if (pdfName != null && pdfName.startsWith("_"))
            return pdfName.substring(1);
        return pdfName;
    }

    public ArrayList<String> getLabeledResult() {
        return labeled;
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (endTags.contains(qName)) {
            writeData();
            accumulator.setLength(0);
        } else if (qName.equals("front")) {
            // write remaining test as <other>
            String text = getText();
            if (text != null) {
                if (text.length() > 0) {
                    currentTag = "<other>";
                    writeData();
                }
            }
            accumulator.setLength(0);
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
        } else {
            // System.out.println(" **** Warning **** Unexpected closing tag " + qName);
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals("lb")) {
            accumulator.append(" ");
        } else {
            // add accumulated text as <other>
            String text = getText();
            if (StringUtils.isNotEmpty(text)) {
                currentTag = "<other>";
                writeData();
            }
            accumulator.setLength(0);
        }

        if (qName.equals("titlePart")) {
            currentTag = "<title>";
        } else if (qName.equals("idno")) {
            currentTag = "<pubnum>";
        } else if (qName.equals("docAuthor")) {
            currentTag = "<author>";
        } else if (qName.equals("date")) {
            currentTag = "<date>";
        } else if (intermediaryTags.contains(qName)) {
            // do nothing
        } else if (ignoredTags.contains(qName)) {
            // do nothing
            currentTag = "<other>";
        } else {
            // System.out.println("Warning: Unexpected starting tag " + qName);
            currentTag = "<other>";
        }
    }

    private void writeData() {
        if (currentTag == null) {
            return;
        }

        String text = getText();
        // we segment the text
        StringTokenizer st = new StringTokenizer(text, TextUtilities.delimiters, true);
        boolean begin = true;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            if (tok.length() == 0)
                continue;

            String content = tok;
            int i = 0;
            if (content.length() > 0) {
                if (begin) {
                    labeled.add(content + " I-" + currentTag + "\n");
                    begin = false;
                } else {
                    labeled.add(content + " " + currentTag + "\n");
                }
            }
            begin = false;
        }
        accumulator.setLength(0);
    }

}
