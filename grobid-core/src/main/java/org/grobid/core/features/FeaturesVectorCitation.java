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
package org.grobid.core.features;

import java.util.List;
import java.util.regex.Matcher;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * Class for features used for header parsing.
 *
 */
public class FeaturesVectorCitation {
    // default bins for relative position, set experimentally
    static private int nbBins = 12;

    public String string = null; // lexical feature
    public String label = null; // label if known
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean lastName = false;

    public boolean year = false;
    public boolean month = false;
    public boolean http = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public boolean containPunct = false;
    public int relativePosition = -1;

    // true if the token is part of a predefinied name (single or multi-token)
    public boolean isKnownJournalTitle = false;
    public boolean isKnownAbbrevJournalTitle = false;
    public boolean isKnownConferenceTitle = false;
    public boolean isKnownPublisher = false;
    public boolean isKnownLocation = false;
    public boolean isKnownCollaboration = false;
    public boolean isKnownIdentifier = false;

    public String printVector() {
        if (string == null)
            return null;
        if (string.length() == 0)
            return null;
        StringBuilder res = new StringBuilder();

        // token string (1)
        res.append(string);

        // lowercase string (1)
        res.append(" ").append(string.toLowerCase());

        // prefix (4)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));

        // suffix (4)
        res.append(" " + TextUtilities.suffix(string, 1));
        res.append(" " + TextUtilities.suffix(string, 2));
        res.append(" " + TextUtilities.suffix(string, 3));
        res.append(" " + TextUtilities.suffix(string, 4));

        // line information (1)
        res.append(" ").append(lineStatus);

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" ").append(capitalisation);

        // digit information (1)
        res.append(" ").append(digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (9)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        if (firstName)
            res.append(" 1");
        else
            res.append(" 0");

        if (lastName)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownLocation)
            res.append(" 1");
        else
            res.append(" 0");

        if (year)
            res.append(" 1");
        else
            res.append(" 0");

        if (month)
            res.append(" 1");
        else
            res.append(" 0");

        if (http)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownCollaboration)
            res.append(" 1");
        else
            res.append(" 0");

        // bibliographical information(3)
        if (isKnownJournalTitle || isKnownAbbrevJournalTitle)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownConferenceTitle)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownPublisher)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownIdentifier)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" ").append(punctType); // in case the token is a punctuation (NO otherwise)

        // relative position in the sequence (1)
        res.append(" ").append(relativePosition);

        // label - for training data (1)
        if (label != null)
            res.append(" ").append(label).append("\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add feature for citation parsing.
     */
    static public String addFeaturesCitation(
            List<LayoutToken> tokens,
            List<String> labels,
            List<OffsetPosition> journalPositions,
            List<OffsetPosition> abbrevJournalPositions,
            List<OffsetPosition> conferencePositions,
            List<OffsetPosition> publisherPositions,
            List<OffsetPosition> locationPositions,
            List<OffsetPosition> collaborationPositions,
            List<OffsetPosition> identifierPositions,
            List<OffsetPosition> urlPositions) throws Exception {
        if ((journalPositions == null) ||
                (abbrevJournalPositions == null) ||
                (conferencePositions == null) ||
                (publisherPositions == null) ||
                (locationPositions == null) ||
                (collaborationPositions == null) ||
                (identifierPositions == null) ||
                (urlPositions == null)) {
            throw new GrobidException("At least one list of gazetter matches positions is null.");
        }

        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuilder citation = new StringBuilder();

        int currentJournalPositions = 0;
        int currentAbbrevJournalPositions = 0;
        int currentConferencePositions = 0;
        int currentPublisherPositions = 0;
        int currentLocationPositions = 0;
        int currentCollaborationPositions = 0;
        int currentIdentifierPositions = 0;
        int currentUrlPositions = 0;

        boolean isJournalToken;
        boolean isAbbrevJournalToken;
        boolean isConferenceToken;
        boolean isPublisherToken;
        boolean isLocationToken;
        boolean isCollaborationToken;
        boolean isIdentifierToken;
        boolean isUrlToken;
        boolean skipTest;

        String previousTag = null;
        String previousText = null;
        FeaturesVectorCitation features = null;
        int sentenceLenth = tokens.size(); // length of the current sentence
        for (int n = 0; n < tokens.size(); n++) {
            LayoutToken token = tokens.get(n);
            String tag = null;
            if ((labels != null) && (labels.size() > 0) && (n < labels.size()))
                tag = labels.get(n);

            boolean outputLineStatus = false;
            isJournalToken = false;
            isAbbrevJournalToken = false;
            isConferenceToken = false;
            isPublisherToken = false;
            isLocationToken = false;
            isCollaborationToken = false;
            isIdentifierToken = false;
            isUrlToken = false;
            skipTest = false;

            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            if (text.equals("\n")) {
                // should not be the case for citation model
                continue;
            }

            // parano normalisation
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0) {
                continue;
            }

            // check the position of matches for journals
            if ((journalPositions != null) && (journalPositions.size() > 0)) {
                if (currentJournalPositions == journalPositions.size() - 1) {
                    if (journalPositions.get(currentJournalPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentJournalPositions; i < journalPositions.size(); i++) {
                        if ((journalPositions.get(i).start <= n) &&
                                (journalPositions.get(i).end >= n)) {
                            isJournalToken = true;
                            currentJournalPositions = i;
                            break;
                        } else if (journalPositions.get(i).start > n) {
                            isJournalToken = false;
                            currentJournalPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for abbreviated journals
            skipTest = false;
            if (abbrevJournalPositions != null) {
                if (currentAbbrevJournalPositions == abbrevJournalPositions.size() - 1) {
                    if (abbrevJournalPositions.get(currentAbbrevJournalPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentAbbrevJournalPositions; i < abbrevJournalPositions.size(); i++) {
                        if ((abbrevJournalPositions.get(i).start <= n) &&
                                (abbrevJournalPositions.get(i).end >= n)) {
                            isAbbrevJournalToken = true;
                            currentAbbrevJournalPositions = i;
                            break;
                        } else if (abbrevJournalPositions.get(i).start > n) {
                            isAbbrevJournalToken = false;
                            currentAbbrevJournalPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for conferences
            skipTest = false;
            if (conferencePositions != null) {
                if (currentConferencePositions == conferencePositions.size() - 1) {
                    if (conferencePositions.get(currentConferencePositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentConferencePositions; i < conferencePositions.size(); i++) {
                        if ((conferencePositions.get(i).start <= n) &&
                                (conferencePositions.get(i).end >= n)) {
                            isConferenceToken = true;
                            currentConferencePositions = i;
                            break;
                        } else if (conferencePositions.get(i).start > n) {
                            isConferenceToken = false;
                            currentConferencePositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for publishers
            skipTest = false;
            if (publisherPositions != null) {
                if (currentPublisherPositions == publisherPositions.size() - 1) {
                    if (publisherPositions.get(currentPublisherPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentPublisherPositions; i < publisherPositions.size(); i++) {
                        if ((publisherPositions.get(i).start <= n) &&
                                (publisherPositions.get(i).end >= n)) {
                            isPublisherToken = true;
                            currentPublisherPositions = i;
                            break;
                        } else if (publisherPositions.get(i).start > n) {
                            isPublisherToken = false;
                            currentPublisherPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for locations
            skipTest = false;
            if (locationPositions != null) {
                if (currentLocationPositions == locationPositions.size() - 1) {
                    if (locationPositions.get(currentLocationPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentLocationPositions; i < locationPositions.size(); i++) {
                        if ((locationPositions.get(i).start <= n) &&
                                (locationPositions.get(i).end >= n)) {
                            isLocationToken = true;
                            currentLocationPositions = i;
                            break;
                        } else if (locationPositions.get(i).start > n) {
                            isLocationToken = false;
                            currentLocationPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for collaboration
            skipTest = false;
            if (collaborationPositions != null) {
                if (currentCollaborationPositions == collaborationPositions.size() - 1) {
                    if (collaborationPositions.get(currentCollaborationPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentCollaborationPositions; i < collaborationPositions.size(); i++) {
                        if ((collaborationPositions.get(i).start <= n) &&
                                (collaborationPositions.get(i).end >= n)) {
                            isCollaborationToken = true;
                            currentCollaborationPositions = i;
                            break;
                        } else if (collaborationPositions.get(i).start > n) {
                            isCollaborationToken = false;
                            currentCollaborationPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for identifier
            skipTest = false;
            if (identifierPositions != null) {
                if (currentIdentifierPositions == identifierPositions.size() - 1) {
                    if (identifierPositions.get(currentIdentifierPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentIdentifierPositions; i < identifierPositions.size(); i++) {
                        if ((identifierPositions.get(i).start <= n) &&
                                (identifierPositions.get(i).end >= n)) {
                            isIdentifierToken = true;
                            currentIdentifierPositions = i;
                            break;
                        } else if (identifierPositions.get(i).start > n) {
                            isIdentifierToken = false;
                            currentIdentifierPositions = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for url
            skipTest = false;
            if (urlPositions != null) {
                if (currentUrlPositions == urlPositions.size() - 1) {
                    if (urlPositions.get(currentUrlPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentUrlPositions; i < urlPositions.size(); i++) {
                        if ((urlPositions.get(i).start <= n) &&
                                (urlPositions.get(i).end >= n)) {
                            isUrlToken = true;
                            currentUrlPositions = i;
                            break;
                        } else if (urlPositions.get(i).start > n) {
                            isUrlToken = false;
                            currentUrlPositions = i;
                            break;
                        }
                    }
                }
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorCitation();
            features.string = text;
            features.relativePosition = featureFactory.linearScaling(n, sentenceLenth, nbBins);

            if (n == 0) {
                features.lineStatus = "LINESTART";
                outputLineStatus = true;
            }
            Matcher m0 = featureFactory.isPunct.matcher(text);
            if (m0.find()) {
                features.punctType = "PUNCT";
            }

            if ((text.equals("(")) | (text.equals("["))) {
                features.punctType = "OPENBRACKET";
            } else if ((text.equals(")")) | (text.equals("]"))) {
                features.punctType = "ENDBRACKET";
            } else if (text.equals(".")) {
                features.punctType = "DOT";
            } else if (text.equals(",")) {
                features.punctType = "COMMA";
            } else if (text.equals("-")) {
                features.punctType = "HYPHEN";
            } else if (text.equals("\"") | text.equals("\'") | text.equals("`")) {
                features.punctType = "QUOTE";
            }

            if (n == 0) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINESTART";
                    outputLineStatus = true;
                }
            } else if (tokens.size() == n + 1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
                }
            }

            if (!outputLineStatus) {
                features.lineStatus = "LINEIN";
                outputLineStatus = true;
            }

            if (text.length() == 1) {
                features.singleChar = true;
            }

            if (Character.isUpperCase(text.charAt(0))) {
                features.capitalisation = "INITCAP";
            }

            if (featureFactory.test_all_capital(text)) {
                features.capitalisation = "ALLCAP";
            }

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_names(text)) {
                features.properName = true;
            }

            if (featureFactory.test_month(text)) {
                features.month = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            Matcher m2 = featureFactory.year.matcher(text);
            if (m2.find()) {
                features.year = true;
            }

            if (isCollaborationToken)
                features.isKnownCollaboration = true;

            /*Matcher m5 = featureFactory.ACRONYM.matcher(text);
               if (m5.find()) {
                   features.acronym = true;
               }*/

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isJournalToken) {
                features.isKnownJournalTitle = true;
            }

            if (isAbbrevJournalToken) {
                features.isKnownAbbrevJournalTitle = true;
            }

            if (isConferenceToken) {
                features.isKnownConferenceTitle = true;
            }

            if (isPublisherToken) {
                features.isKnownPublisher = true;
            }

            if (isLocationToken) {
                features.isKnownLocation = true;
            }

            if (isIdentifierToken) {
                features.isKnownIdentifier = true;
            }

            if (isUrlToken) {
                features.http = true;
            }

            features.label = tag;

            citation.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return citation.toString();
    }

}
