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

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.grobid.core.data.BiblioItem;

/**
 * SAX parser for XML crossref DOI metadata descriptions.
 * See http://www.crossref.org/openurl_info.html
 *
 */
public class CrossrefUnixrefSaxParser extends DefaultHandler {

    private BiblioItem biblio = null;
    private BiblioItem biblioParent = null;
    private List<BiblioItem> biblios = null;
    private List<String> authors = null;
    private List<String> editors = null;
    private String author = null;
    private StringBuffer accumulator = new StringBuffer(); // Accumulate parsed text
    private String media = null; // print or electronic, for ISSN

    public CrossrefUnixrefSaxParser() {
    }

    public CrossrefUnixrefSaxParser(BiblioItem b) {
        biblio = b;
    }

    public CrossrefUnixrefSaxParser(List<BiblioItem> b) {
        biblios = b;
    }

    public boolean journalMetadataBlock = false;
    public boolean journalIssueBlock = false;
    public boolean journalArticleBlock = false;
    public boolean conferencePaperBlock = false;
    public boolean proceedingsMetadataBlock = false;
    public boolean contentItemBlock = false;
    public boolean eventMetadataBlock = false;
    public boolean bookMetadataBlock = false;
    public boolean serieMetadataBlock = false;
    public boolean doiDataBlock = false;
    public boolean online = false;

    public boolean authorBlock = false;
    public boolean editorBlock = false;
    public boolean firstAuthor = false;

    public void characters(char[] ch, int start, int length) {
        accumulator.append(ch, start, length);
    }

    public String getText() {
        return accumulator.toString().trim();
    }

    public void endElement(java.lang.String uri, java.lang.String localName, java.lang.String qName)
            throws SAXException {
        if (qName.equals("journal_metadata")) {
            journalMetadataBlock = false;
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("journal_issue")) {
            journalIssueBlock = false;
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("journal_article")) {
            journalArticleBlock = false;
            biblio.setItem(BiblioItem.Article);
            //            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("proceedings_metadata")) {
            proceedingsMetadataBlock = false;
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("content_item")) {
            contentItemBlock = false;
        } else if (qName.equals("event_metadata")) {
            eventMetadataBlock = false;
        } else if (qName.equals("conference_paper")) {
            conferencePaperBlock = false;
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("doi_data")) {
            doiDataBlock = false;
        } else if (qName.equals("title")) {
            if (journalArticleBlock || contentItemBlock || conferencePaperBlock) {
                biblio.setArticleTitle(getText());
            } else if (serieMetadataBlock) {
                biblio.setSerieTitle(getText());
            } else {
                biblio.setTitle(getText());
            }
        } else if (qName.equals("full_title")) {
            biblio.setJournal(getText());
        } else if (qName.equals("abbrev_title")) {
            biblio.setJournalAbbrev(getText());
        } else if (qName.equals("issn")) {
            String issn = getText();

            if (media != null) {
                if (media.equals("print"))
                    biblio.setISSN(issn);
                else
                    biblio.setISSNe(issn);
            } else
                biblio.setISSN(issn);
        } else if (qName.equals("isbn")) {
            biblio.setISBN13(getText());
        } else if (qName.equals("volume")) {
            String volume = getText();
            if (volume != null) {
                if (volume.length() > 0) {
                    biblio.setVolume(volume);
                    biblio.setVolumeBlock(volume, true);
                }
            }
        } else if (qName.equals("issue")) {
            String issue = getText();
            // issue can be of the form 4-5
            if (issue != null)
                if (issue.length() > 0) {
                    biblio.setNumber(issue);
                    biblio.setIssue(issue);
                    //biblio.setNumber(Integer.parseInt(issue));
                }
        } else if (qName.equals("year")) {
            String year = getText();
            biblio.setPublicationDate(year);
            if (online)
                biblio.setE_Year(year);
            else
                biblio.setYear(year);
        } else if (qName.equals("month")) {
            String month = getText();
            if (online)
                biblio.setE_Month(month);
            else
                biblio.setMonth(month);
        } else if (qName.equals("day")) {
            String day = getText();
            if (online)
                biblio.setE_Day(day);
            else
                biblio.setDay(day);
        } else if (qName.equals("first_page")) {
            String page = getText();
            if (StringUtils.isNotEmpty(page)) {
                /*if (page.startsWith("L") | page.startsWith("l")) {
                    page = page.substring(1, page.length());
                }*/
                page = cleanPage(page);
                try {
                    biblio.setBeginPage(Integer.parseInt(page));
                } catch (Exception e) {
                    // warning message to be logged here
                }
            }
        } else if (qName.equals("last_page")) {
            String page = getText();
            if (StringUtils.isNotEmpty(page)) {
                page = cleanPage(page);
                try {
                    biblio.setEndPage(Integer.parseInt(page));
                } catch (Exception e) {
                    // warning message to be logged here
                }
            }
        } else if (qName.equals("doi")) {
            String doi = getText();
            if (doiDataBlock)
                biblio.setDOI(doi);
            biblio.setError(false);
        } else if (qName.equals("given_name")) {
            author = getText();
        } else if (qName.equals("surname")) {
            String sauce = getText();
            if (!sauce.equals("Unknown")) {
                if (author == null)
                    author = sauce;
                else
                    author = author + " " + sauce;
                authors.add(author);
                if (authorBlock)
                    biblio.addAuthor(author);
                else if (editorBlock)
                    biblio.addEditor(author);
                author = null;
            }
        } else if (qName.equals("person_name")) {
            firstAuthor = false;
            authorBlock = false;
        } else if (qName.equals("conference_name")) {
            String event = getText();
            biblio.setEvent(event);
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("conference_location")) {
            String location = getText();
            biblio.setLocation(location);
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("conference_acronym")) {
            String acro = getText();
            if (biblio.getEvent() == null)
                biblio.setEvent(acro);
            else
                biblio.setEvent(biblio.getEvent() + ", " + acro);
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("proceedings_title")) {
            String proc = getText();
            if (proc != null)
                proc = proc.replaceAll(" - ", ", ");
            biblio.setBookTitle(proc);
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("doi_record")) {
            if (biblios != null) {
                biblios.add(biblio);
                biblio = null;
            }
        } else if (qName.equals("publisher_name")) {
            String publisher = getText();
            biblio.setPublisher(publisher);
        } else if (qName.equals("publisher_place")) {
            String location = getText();
            biblio.setLocationPublisher(location);
        } else if (qName.equals("series_metadata")) {
            serieMetadataBlock = false;
            biblio.setItem(BiblioItem.InCollection);
        } else if (qName.equals("book_metadata")) {
            bookMetadataBlock = false;
            biblio.setItem(BiblioItem.InBook);
        }
        accumulator.setLength(0);
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts)
            throws SAXException {
        if (qName.equals("journal_metadata")) {
            journalMetadataBlock = true;
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("proceedings_metadata")) {
            proceedingsMetadataBlock = true;
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("book_metadata")) {
            bookMetadataBlock = true;
            biblio.setItem(BiblioItem.InBook);
        } else if (qName.equals("series_metadata")) {
            serieMetadataBlock = true;
            if (bookMetadataBlock)
                biblio.setItem(BiblioItem.InCollection);
        } else if (qName.equals("content_item")) {
            BiblioItem biblio2 = new BiblioItem();
            biblio2.setParentItem(biblio);
            biblio = biblio2;
            contentItemBlock = true;
        } else if (qName.equals("event_metadata")) {
            eventMetadataBlock = true;
        } else if (qName.equals("conference_paper")) {
            conferencePaperBlock = true;
            biblio.setItem(BiblioItem.InProceedings);
        } else if (qName.equals("journal_issue")) {
            journalIssueBlock = true;
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("journal_article")) {
            journalArticleBlock = true;
            biblio.setItem(BiblioItem.Periodical);
        } else if (qName.equals("doi_data")) {
            doiDataBlock = true;
        } else if (qName.equals("contributors")) {
            authors = new ArrayList<>(0);
            editors = new ArrayList<>(0);
        } else if (qName.equals("error")) {
            biblio.setError(true);
        } else if (qName.equals("person_name")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("sequence")) {
                        if (value.equals("firstAuthor"))
                            firstAuthor = true;
                        else
                            firstAuthor = false;
                    }
                    if (name.equals("contributor_role")) {
                        if (value.equals("author")) {
                            authorBlock = true;
                            editorBlock = true;
                        } else if (value.equals("editor")) {
                            authorBlock = false;
                            editorBlock = true;
                        } else {
                            authorBlock = false;
                            editorBlock = false;
                        }
                    }
                }
            }
        } else if (qName.equals("doi_record")) {
            if (biblios != null) {
                biblio = new BiblioItem();
            }
        } else if (qName.equals("publication_date")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("media_type")) {
                        if (value.equals("online"))
                            online = true;
                        else
                            online = false;
                    }
                }
            }
        } else if (qName.equals("issn")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if ((name != null) & (value != null)) {
                    if (name.equals("media_type")) {
                        media = value;
                    }
                }
            }
        }

        accumulator.setLength(0);
    }

    protected static String cleanPage(String page) {
        return StringUtils.stripStart(page, "Ll");
    }

}
