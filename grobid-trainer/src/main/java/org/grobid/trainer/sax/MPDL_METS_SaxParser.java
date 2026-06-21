package org.grobid.trainer.sax;

import java.io.*;
import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.grobid.core.data.Person;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;

/**
 * SAX parser for the MPDL METS catalogue data. The actual format for the bibliographical metadata is MODS.
 *
 * @author Patrice Lopez
 */
public class MPDL_METS_SaxParser extends DefaultHandler {

    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text

    private ArrayList<Person> authors = null;
    private ArrayList<String> subjects = null;
    private ArrayList<String> files = null;
    private ArrayList<String> keywords = null;

    private String mods_title = null;
    private String mods_identifier = null;
    private String mods_start = null; // start page
    private String mods_end = null; // end page
    private String mods_language = Language.DE;
    private String mods_genre = null; // type of paper/communication
    private String author_family = null;
    private String author_given = null;
    private String mods_displayForm = null;

    private boolean author = false;
    private boolean family = false;
    private boolean given = false;
    private boolean displayForm = false; // name as displayed in the paper

    private boolean outputFile = false;

    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> ids = new ArrayList<String>();

    //private Writer writer = null;
    private String output = null;

    public MPDL_METS_SaxParser() {
    }

    /*public MPDL_METS_SaxParser(Writer writ) {
         writer = writ;
     }*/
    public MPDL_METS_SaxParser(String outp) {
        output = outp;
        outputFile = true;
    }

    public void characters(char[] buffer, int start, int length) {
        accumulator.append(buffer, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void setOutputFile(boolean b) {
        outputFile = b;
    }

    public ArrayList<String> getModsTitles() {
        return titles;
    }

    public ArrayList<String> getIds() {
        return ids;
    }

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if (qName.equals("mods:title")) {
            // we register in the DB the new entry
            mods_title = getText();
            titles.add(mods_title);
            ids.add(mods_identifier);
            mods_title = mods_title.replace("\n", "<lb/>");
            accumulator.setLength(0);
        } else if (qName.equals("mods:identifier")) {
            mods_identifier = getText();
            accumulator.setLength(0);
        } else if (qName.equals("mods:start")) {
            mods_start = getText();
            accumulator.setLength(0);
        } else if (qName.equals("mods:language")) {
            mods_language = getText();
            // the language must be formatted
            if (mods_language.equals("De"))
                mods_language = Language.DE;
            else if (mods_language.equals("En"))
                mods_language = Language.EN;
            else
                mods_language = Language.DE;
            accumulator.setLength(0);
        } else if (qName.equals("mods:end")) {
            mods_end = getText();
            accumulator.setLength(0);
        } else if (qName.equals("mods:name")) {
            // we register the author
            Person aut = new Person();
            aut.setFirstName(author_given);
            aut.setLastName(author_family);
            //aut.setDisplayName(mods_displayForm);
            //authors.add(aut);

            accumulator.setLength(0);
        } else if (qName.equals("mods:namePart")) {
            if (family) {
                author_family = getText();
                family = false;
            } else if (given) {
                author_given = getText();
                given = false;
            }

            accumulator.setLength(0);
        } else if (qName.equals("mods:displayForm")) {
            mods_displayForm = getText();

            accumulator.setLength(0);
        } else if (qName.equals("mods:mods")) {
            // end of bibliographical entry
            try {
                if (outputFile) {
                    File outFile = new File(output + "/" + mods_identifier + "-train.tei");
                    OutputStream os = new FileOutputStream(outFile);
                    Writer writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(
                            "<tei>\n\t<teiHeader>\n\t<fileDesc xml:id=\""
                                    + mods_identifier
                                    +
                                    "\"/>\n\t</teiHeader>\n\t<text xml:lang=\""
                                    + mods_language
                                    + "\">\n");
                    // we can write the title section
                    writer.write("\t\t<front>\n\t\t\t<titlePage>\n\t\t\t\t<docTitle>\n");
                    writer.write("\t\t\t\t\t<titlePart type=\"main\">" + mods_title + "</titlePart>\n");
                    writer.write("\t\t\t\t</docTitle>\n");

                    writer.write("\t\t\t\t<byline><docAuthor>" + mods_displayForm + "</docAuthor><lb/></byline>\n");

                    writer.write("\t\t\t\t<byline><affiliation><lb/></affiliation></byline>\n");

                    writer.write(
                            "\t\t\t\t<docImprint>(<title level=\"j\">Z. Naturforschg.</title> <biblScope type=\"vol\"></biblScope>, <biblScope type=\"pp\"></biblScope> [<date></date>]; <note>eingegangen am</note>)<lb/></docImprint>\n");

                    writer.write("\t\t\t</titlePage>\n");
                    writer.write("\t\t\t<div type=\"abstract\"><lb/></div>\n");
                    writer.write("\t\t\t<div type=\"intro\"></div>\n");
                    writer.write("\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();
                    os.close();
                }
            } catch (Exception e) {
                //        		e.printStackTrace();
                throw new GrobidException("An exception occurred while running Grobid.", e);
            }
            accumulator.setLength(0);
        }
        accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("mods:namePart")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type")) {
                        if (value.equals("family")) {
                            family = true;
                            given = false;
                        } else if (value.equals("given")) {
                            given = true;
                            family = false;
                        }
                    }
                }
            }
            accumulator.setLength(0);
        } else if (qName.equals("mods:mods")) {
            // new bibliographical entry
            // tei file is opened when this tag is closed
        } else {
            accumulator.setLength(0);
        }

    }

}
