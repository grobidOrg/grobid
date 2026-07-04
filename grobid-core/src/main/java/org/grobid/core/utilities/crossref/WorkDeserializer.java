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
package org.grobid.core.utilities.crossref;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Date;
import org.grobid.core.data.Person;

/**
 * Convert a JSON Work model - from a glutton or crossref response - to a BiblioItem
 * (understandable by this stupid GROBID)
 *
 */
public class WorkDeserializer extends CrossrefDeserializer<BiblioItem> {

    @Override
    protected BiblioItem deserializeOneItem(JsonNode item) {
        BiblioItem biblio = null;
        String type = null; // the crossref type of the item, see http://api.crossref.org/types

        if (item.isObject()) {
            biblio = new BiblioItem();
            //System.out.println(item.toString());

            JsonNode doiNode = item.get("DOI");
            if (doiNode != null && (!doiNode.isMissingNode())) {
                String doi = doiNode.asText();
                biblio.setDOI(doi);
            }

            JsonNode halNode = item.get("halId");
            if (halNode != null && (!halNode.isMissingNode())) {
                String halId = halNode.asText();
                biblio.setHalId(halId);
            }

            // the following are usually provided by biblio-glutton which index augmented/aggregated
            // metadata
            JsonNode pmidNode = item.get("pmid");
            if (pmidNode != null && (!pmidNode.isMissingNode())) {
                String pmid = pmidNode.asText();
                biblio.setPMID(pmid);
            }

            JsonNode pmcidNode = item.get("pmcid");
            if (pmcidNode != null && (!pmcidNode.isMissingNode())) {
                String pmcid = pmcidNode.asText();
                biblio.setPMCID(pmcid);
            }

            JsonNode piiNode = item.get("pii");
            if (piiNode != null && (!piiNode.isMissingNode())) {
                String pii = piiNode.asText();
                biblio.setPII(pii);
            }

            JsonNode arkNode = item.get("ark");
            if (arkNode != null && (!arkNode.isMissingNode())) {
                String ark = arkNode.asText();
                biblio.setArk(ark);
            }

            JsonNode istexNode = item.get("istexId");
            if (istexNode != null && (!istexNode.isMissingNode())) {
                String istexId = istexNode.asText();
                biblio.setIstexId(istexId);
            }

            // the open access url - if available, from the glorious UnpayWall dataset provided
            // by biblio-glutton
            JsonNode oaLinkNode = item.get("oaLink");
            if (oaLinkNode != null && (!oaLinkNode.isMissingNode())) {
                String oaLink = oaLinkNode.asText();
                biblio.setOAURL(oaLink);
            }

            // all the following is now pure crossref metadata
            JsonNode typeNode = item.get("type");
            if (typeNode != null && (!typeNode.isMissingNode())) {
                type = typeNode.asText();
            }

            JsonNode titlesNode = item.get("title");
            if (titlesNode != null && (!titlesNode.isMissingNode()) &&
                    titlesNode.isArray() && (((ArrayNode) titlesNode).size() > 0))
                biblio.setTitle(((ArrayNode) titlesNode).get(0).asText());

            JsonNode authorsNode = item.get("author");
            if (authorsNode != null && (!authorsNode.isMissingNode()) &&
                    authorsNode.isArray() && (((ArrayNode) authorsNode).size() > 0)) {
                Iterator<JsonNode> authorIt = ((ArrayNode) authorsNode).elements();
                while (authorIt.hasNext()) {
                    JsonNode authorNode = authorIt.next();

                    Person person = new Person();
                    if (authorNode.get("given") != null && !authorNode.get("given").isMissingNode()) {
                        person.setFirstName(authorNode.get("given").asText());
                        person.normalizeCrossRefFirstName();
                    }
                    if (authorNode.get("family") != null && !authorNode.get("family").isMissingNode()) {
                        person.setLastName(authorNode.get("family").asText());
                    }
                    if (authorNode.get("ORCID") != null && !authorNode.get("ORCID").isMissingNode()) {
                        person.setORCID(authorNode.get("ORCID").asText());
                    }
                    // for cases like JM Smith and for case normalisation
                    person.normalizeName();
                    biblio.addFullAuthor(person);
                }
            }

            JsonNode publisherNode = item.get("publisher");
            if (publisherNode != null && (!publisherNode.isMissingNode()))
                biblio.setPublisher(publisherNode.asText());

            JsonNode pageNode = item.get("page");
            if (pageNode != null && (!pageNode.isMissingNode()))
                biblio.setPageRange(pageNode.asText());

            JsonNode volumeNode = item.get("volume");
            if (volumeNode != null && (!volumeNode.isMissingNode()))
                biblio.setVolumeBlock(volumeNode.asText(), false);

            JsonNode issueNode = item.get("issue");
            if (issueNode != null && (!issueNode.isMissingNode()))
                biblio.setIssue(issueNode.asText());

            JsonNode containerTitlesNode = item.get("container-title");
            if (containerTitlesNode != null && (!containerTitlesNode.isMissingNode()) &&
                    containerTitlesNode.isArray() && (((ArrayNode) containerTitlesNode).size() > 0)) {
                // container title depends on the type of object
                // if journal
                if ((type != null) && (type.equals("journal-article")))
                    biblio.setJournal(((ArrayNode) containerTitlesNode).get(0).asText());

                // if book chapter or proceedings article
                if ((type != null) && (type.equals("book-section") ||
                        type.equals("proceedings-article") || type.equals("book-chapter")))
                    biblio.setBookTitle(((ArrayNode) containerTitlesNode).get(0).asText());
            }

            JsonNode shortContainerTitlesNode = item.get("short-container-title");
            if (shortContainerTitlesNode != null && (!shortContainerTitlesNode.isMissingNode()) &&
                    shortContainerTitlesNode.isArray() && (((ArrayNode) shortContainerTitlesNode).size() > 0)) {
                // container title depends on the type of object
                // if journal
                if ((type != null) && (type.equals("journal-article")))
                    biblio.setJournalAbbrev(((ArrayNode) shortContainerTitlesNode).get(0).asText());
            }

            JsonNode issnTypeNode = item.get("issn-type");
            if (issnTypeNode != null && (!issnTypeNode.isMissingNode()) &&
                    issnTypeNode.isArray() && (((ArrayNode) issnTypeNode).size() > 0)) {
                Iterator<JsonNode> issnIt = ((ArrayNode) issnTypeNode).elements();
                while (issnIt.hasNext()) {
                    JsonNode issnNode = issnIt.next();
                    JsonNode theTypeNode = issnNode.get("type");
                    JsonNode valueNode = issnNode.get("value");

                    if (theTypeNode != null && (!theTypeNode.isMissingNode()) &&
                            valueNode != null && (!valueNode.isMissingNode())) {
                        String theType = theTypeNode.asText();
                        if (theType.equals("print")) {
                            biblio.setISSN(valueNode.asText());
                        } else if (theType.equals("electronic")) {
                            biblio.setISSNe(valueNode.asText());
                        }
                    }
                }
            }

            JsonNode publishPrintNode = item.get("issued");
            if (publishPrintNode == null || publishPrintNode.isMissingNode()) {
                publishPrintNode = item.get("published-online");
            }
            if (publishPrintNode == null || publishPrintNode.isMissingNode()) {
                publishPrintNode = item.get("published-print");
            }
            if (publishPrintNode == null || publishPrintNode.isMissingNode()) {
                publishPrintNode = item.get("published");
            }
            if (publishPrintNode != null && (!publishPrintNode.isMissingNode())) {
                JsonNode datePartNode = publishPrintNode.get("date-parts");
                if (datePartNode != null && (!datePartNode.isMissingNode()) &&
                        datePartNode.isArray() && (((ArrayNode) datePartNode).size() > 0)) {
                    JsonNode firstDatePartNode = ((ArrayNode) datePartNode).get(0);
                    if (firstDatePartNode != null && (!firstDatePartNode.isMissingNode()) &&
                            firstDatePartNode.isArray() && (((ArrayNode) firstDatePartNode).size() > 0)) {

                        // format is [year, month, day], last two optional
                        String year = ((ArrayNode) firstDatePartNode).get(0).asText();
                        String month = null;
                        String day = null;
                        if (((ArrayNode) firstDatePartNode).size() > 1) {
                            month = ((ArrayNode) firstDatePartNode).get(1).asText();
                            if (((ArrayNode) firstDatePartNode).size() > 2) {
                                day = ((ArrayNode) firstDatePartNode).get(2).asText();
                            }
                        }
                        Date date = new Date();
                        date.setYearString(year);
                        int yearInt = -1;
                        try {
                            yearInt = Integer.parseInt(year);
                        } catch (Exception e) {
                            // log something
                        }
                        if (yearInt != -1)
                            date.setYear(yearInt);

                        if (month != null) {
                            date.setMonthString(month);
                            int monthInt = -1;
                            try {
                                monthInt = Integer.parseInt(month);
                            } catch (Exception e) {
                                // log something
                            }
                            if (monthInt != -1)
                                date.setMonth(monthInt);
                        }

                        if (day != null) {
                            date.setDayString(day);
                            int dayInt = -1;
                            try {
                                dayInt = Integer.parseInt(day);
                            } catch (Exception e) {
                                // log something
                            }
                            if (dayInt != -1)
                                date.setDay(dayInt);
                        }
                        biblio.setNormalizedPublicationDate(date);
                    }
                }
            }

            //System.out.println(biblio.toTEI(0));
        }

        return biblio;
    }
}
