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

import org.grobid.core.data.Affiliation;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Person;

/**
 * SAX parser for the NLM XML format - the PubMed XML full text format.
 * This class covers only the header of the NLM file.
 *
 * @author Patrice Lopez
 */
public class NLMHeaderSaxHandler extends DefaultHandler {

    private BiblioItem biblio = null;
    private ArrayList<Person> authors = null;
    private ArrayList<String> editors = null;
    private Person author = null;
    private Affiliation affiliation = null;
    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private String media = null; // print or electronic, for ISSN
    private String current_id = null;

    public boolean journalMetadataBlock = false;
    public boolean journalIssueBlock = false;
    public boolean journalArticleBlock = false;
    public boolean conferencePaperBlock = false;
    public boolean proceedingsMetadataBlock = false;
    public boolean contentItemBlock = false;
    public boolean eventMetadataBlock = false;
    public boolean bookMetadataBlock = false;
    public boolean serieMetadataBlock = false;
    public boolean pubDateMetadataBlock = false;
    public boolean affiliationMetadataBlock = false;
    public boolean online = false;
    public boolean date_accepted = false;
    public boolean date_submitted = false;

    public boolean authorBlock = false;
    public boolean editorBlock = false;
    public boolean firstAuthor = false;

    public NLMHeaderSaxHandler() {
    }

    public NLMHeaderSaxHandler(BiblioItem b) {
        biblio = b;
    }

    public BiblioItem getBiblio() {
        return biblio;
    }

    public void characters(char[] ch, int start, int length) {
        accumulator.append(ch, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if (qName.equals("journal-title")) {
            biblio.setJournal(getText());
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("abbrev-journal-title")) {
            biblio.setJournalAbbrev(getText());
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("issn")) {
            String issn = getText();
            biblio.setItem(BiblioItem.Periodical);
            if (media != null) {
                if (media.equals("print"))
                    biblio.setISSN(issn);
                else
                    biblio.setISSNe(issn);
            } else
                biblio.setISSN(issn);
        } else if (qName.equals("publisher-name")) {
            String publisher = getText();
            biblio.setPublisher(publisher);
        } else if (qName.equals("article-id ")) {
            if (current_id != null) {
                if (current_id.equals("doi")) {
                    String doi = getText();
                    biblio.setDOI(doi);
                    biblio.setError(false);
                }
            }
        } else if (qName.equals("article-title")) {
            biblio.setArticleTitle(getText());
        } else if (qName.equals("contrib")) {
            authorBlock = false;
            editorBlock = false;
            if (authorBlock)
                authors.add(author);
            author = null;
        } else if (qName.equals("given_names")) {
            String sauce = getText();
            if (authorBlock) {
                if (author == null)
                    author = new Person();
                author.setFirstName(sauce);
            }
        } else if (qName.equals("surname")) {
            String sauce = getText();
            if (authorBlock) {
                if (!sauce.equals("Unknown")) {
                    if (author == null)
                        author = new Person();
                    author.setLastName(sauce);
                }
            }
        } else if (qName.equals("volume")) {
            String volume = getText();
            if (volume != null)
                if (volume.length() > 0)
                    biblio.setVolume(volume);
        } else if (qName.equals("issue")) {
            String issue = getText();
            // issue can be of the form 4-5
            if (issue != null) {
                if (issue.length() > 0) {
                    biblio.setNumber(issue);
                    biblio.setIssue(issue);
                }
            }
        } else if (qName.equals("fpage")) {
            String page = getText();
            if (page != null)
                if (page.length() > 0) {
                    if (page.startsWith("L") | page.startsWith("l"))
                        page = page.substring(1, page.length());
                    biblio.setBeginPage(Integer.parseInt(page));
                }
        } else if (qName.equals("pub-date ")) {
            //biblio.setPublicationDate(getText());
            pubDateMetadataBlock = false;
        } else if (qName.equals("year")) {
            String year = getText();
            //if (pubDateMetadataBlock)
            //	biblio.setPublicationDate(year);
            if (online) {
                biblio.setE_Year(year);
            } else if (date_accepted) {
                biblio.setA_Year(year);
            } else {
                biblio.setYear(year);
            }
        } else if (qName.equals("month")) {
            String month = getText();
            if (online) {
                biblio.setE_Month(month);
            } else if (date_accepted) {
                biblio.setA_Month(month);
            } else {
                biblio.setMonth(month);
            }
        } else if (qName.equals("day")) {
            String day = getText();
            if (online) {
                biblio.setE_Day(day);
            } else if (date_accepted) {
                biblio.setA_Day(day);
            } else {
                biblio.setDay(day);
            }
        } else if (qName.equals("p")) {
            String paragraph = getText();
            if (paragraph != null) {
                if (paragraph.length() > 0) {
                    if (biblio.getAbstract() == null)
                        biblio.setAbstract(paragraph);
                    else
                        biblio.setAbstract(biblio.getAbstract() + "\n" + paragraph);
                }
            }
        } else if (qName.equals("aff")) {
            affiliationMetadataBlock = false;
            if (author != null) {
                if (affiliation != null)
                    author.addAffiliation(affiliation);
            }
            affiliation = null;
        } else if (qName.equals("country")) {
            if (affiliationMetadataBlock) {
                if (affiliation == null) {
                    affiliation = new Affiliation();
                }
                affiliation.setCountry(getText());
            }
        } else if (qName.equals("email")) {
            if (affiliationMetadataBlock) {
                if (author != null)
                    author.setEmail(getText());
            }
        } else if (qName.equals("contrib-group")) {
            biblio.setFullAuthors(authors);
        } else if (qName.equals("date")) {
            pubDateMetadataBlock = false;
            date_accepted = false;
            date_submitted = false;
        }

        accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {

        if (qName.equals("article")) {
            int length = atts.getLength();

            if (biblio == null) {
                biblio = new BiblioItem();
            }

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("xml:lang")) {
                        biblio.setLanguage(value);
                    }
                }
            }
        } else if (qName.equals("issn")) {
            int length = atts.getLength();
            biblio.setItem(BiblioItem.Periodical);
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("pub-type")) {
                        if (value.equals("ppub")) {
                            media = "print";
                        } else if (value.equals("epub")) {
                            media = "digital";
                        }
                    }
                }
            }
        } else if (qName.equals("article-id ")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("pub-id-type")) {
                        if (value.equals("doi")) {
                            current_id = "doi";
                        }
                    }
                }
            }
        } else if (qName.equals("contrib-group")) {
            authors = new ArrayList<Person>(0);
            editors = new ArrayList<String>(0);
        } else if (qName.equals("contrib")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("contrib-type")) {
                        if (value.equals("author")) {
                            authorBlock = true;
                        } else if (value.equals("editor")) {
                            editorBlock = true;
                        }
                    }
                }
            }
        } else if (qName.equals("pub-date")) {
            pubDateMetadataBlock = true;
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("pub-type")) {
                        if (value.equals("ppub")) {
                            online = false;
                        } else if (value.equals("epub")) {
                            online = true;
                        }
                    }
                }
            }
        } else if (qName.equals("aff")) {
            affiliationMetadataBlock = true;
        } else if (qName.equals("date")) {
            pubDateMetadataBlock = true;
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("date-type")) {
                        if (value.equals("accepted")) {
                            date_accepted = true;
                        }
                    }
                }
            }
        }

        accumulator.setLength(0);
    }

}
