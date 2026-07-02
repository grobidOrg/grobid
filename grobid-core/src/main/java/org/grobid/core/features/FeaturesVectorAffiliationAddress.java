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
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for parsing a block corresponding to affiliation and address.
 *
 */
public class FeaturesVectorAffiliationAddress {
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean locationName = false;
    public boolean countryName = false;
    public String punctType = null;
    public String wordShape = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)

    public String printVector() {
        if (string == null)
            return null;
        if (string.length() == 0)
            return null;
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // lowercase string
        res.append(" " + string.toLowerCase());

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
        res.append(" " + lineStatus);

        // capitalisation (1)
        if (digit.equals("ALLDIGIT"))
            res.append(" NOCAPS");
        else
            res.append(" " + capitalisation);

        // digit information (1)
        res.append(" " + digit);

        // character information (1)
        if (singleChar)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (5)
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

        if (locationName)
            res.append(" 1");
        else
            res.append(" 0");

        if (countryName)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        res.append(" ").append(wordShape);

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add the features for the affiliation+address model.
     */
    static public String addFeaturesAffiliationAddress(
            List<String> lines,
            List<List<LayoutToken>> allTokens,
            List<List<OffsetPosition>> locationPlaces,
            List<List<OffsetPosition>> countriesPositions) throws Exception {
        if (locationPlaces == null) {
            throw new GrobidException("At least one list of gazetter matches positions is null.");
        }
        if (locationPlaces.size() == 0) {
            throw new GrobidException("At least one list of gazetter matches positions is empty.");
        }
        //System.out.println(lines);
        StringBuffer result = new StringBuffer();
        List<String> block = null;
        boolean isPlace = false;
        boolean isCountry = false;
        String lineStatus = "LINESTART";
        int locPlace = 0;
        List<OffsetPosition> currentLocationPlaces = locationPlaces.get(locPlace);
        List<OffsetPosition> currentCountryPlaces = countriesPositions.get(locPlace);
        List<LayoutToken> tokens = allTokens.get(locPlace);
        int currentPosPlaces = 0;
        int currentPosCountries = 0;
        int mm = 0; // position of the token in the current sentence
        String line = null;

        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i);
            isPlace = false;
            isCountry = false;
            if (line.equals("\n")) {
                result.append("\n \n");
                continue;
            }

            while ((tokens != null) && (mm < tokens.size())) {
                LayoutToken token = tokens.get(mm);
                if (token.getText().equals(" ") || token.getText().equals("\n"))
                    mm++;
                else
                    break;
            }

            // check the position of matches for place names
            boolean skipTest = false;
            if ((currentLocationPlaces != null) && (currentLocationPlaces.size() > 0)) {
                if (currentPosPlaces == currentLocationPlaces.size() - 1) {
                    if (currentLocationPlaces.get(currentPosPlaces).end < mm) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int j = currentPosPlaces; j < currentLocationPlaces.size(); j++) {
                        if ((currentLocationPlaces.get(j).start <= mm) &&
                                (currentLocationPlaces.get(j).end >= mm)) {
                            isPlace = true;
                            currentPosPlaces = j;
                            break;
                        } else if (currentLocationPlaces.get(j).start > mm) {
                            isPlace = false;
                            currentPosPlaces = j;
                            break;
                        }
                    }
                }
            }

            // check the position of matches for country names
            skipTest = false;
            if ((currentCountryPlaces != null) && (currentCountryPlaces.size() > 0)) {
                if (currentPosCountries == currentCountryPlaces.size() - 1) {
                    if (currentCountryPlaces.get(currentPosCountries).end < mm) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int j = currentPosCountries; j < currentCountryPlaces.size(); j++) {
                        if ((currentCountryPlaces.get(j).start <= mm) &&
                                (currentCountryPlaces.get(j).end >= mm)) {
                            isCountry = true;
                            currentPosCountries = j;
                            break;
                        } else if (currentCountryPlaces.get(j).start > mm) {
                            isCountry = false;
                            currentPosCountries = j;
                            break;
                        }
                    }
                }
            }

            if (line.trim().contains("@newline")) {
                lineStatus = "LINESTART";
                continue;
            }

            if (line.trim().length() == 0) {
                result.append("\n");
                lineStatus = "LINESTART";
                currentLocationPlaces = locationPlaces.get(locPlace);
                currentCountryPlaces = countriesPositions.get(locPlace);
                tokens = allTokens.get(locPlace);
                currentPosPlaces = 0;
                currentPosCountries = 0;
                locPlace++;
                mm = 0;
            } else {
                // look ahead for line status update
                if (!lineStatus.equals("LINESTART")) {
                    if ((i + 1) < lines.size()) {
                        String nextLine = lines.get(i + 1);
                        if ((nextLine.trim().length() == 0) || (nextLine.trim().contains("@newline"))) {
                            lineStatus = "LINEEND";
                        }
                    } else if ((i + 1) == lines.size()) {
                        lineStatus = "LINEEND";
                    }
                }

                FeaturesVectorAffiliationAddress vector = addFeaturesAffiliationAddress(
                        line,
                        lineStatus,
                        isPlace,
                        isCountry);
                result.append(vector.printVector());

                if (lineStatus.equals("LINESTART")) {
                    lineStatus = "LINEIN";
                } else if (lineStatus.equals("LINEEND")) {
                    lineStatus = "LINESTART";
                }

            }
            mm++;
        }
        //System.out.println(result.toString());
        return result.toString();
    }

    static private FeaturesVectorAffiliationAddress addFeaturesAffiliationAddress(
            String line,
            String lineStatus,
            boolean isPlace,
            boolean isCountry) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        FeaturesVectorAffiliationAddress featuresVector = new FeaturesVectorAffiliationAddress();

        StringTokenizer st = new StringTokenizer(line.trim(), "\t ");
        if (st.hasMoreTokens()) {
            String word = st.nextToken();

            String label = null;
            if (st.hasMoreTokens())
                label = st.nextToken();

            featuresVector.string = word;
            featuresVector.label = label;

            featuresVector.lineStatus = lineStatus;

            if (word.length() == 1) {
                featuresVector.singleChar = true;
            }

            if (featureFactory.test_all_capital(word))
                featuresVector.capitalisation = "ALLCAPS";
            else if (featureFactory.test_first_capital(word))
                featuresVector.capitalisation = "INITCAP";
            else
                featuresVector.capitalisation = "NOCAPS";

            if (featureFactory.test_number(word))
                featuresVector.digit = "ALLDIGIT";
            else if (featureFactory.test_digit(word))
                featuresVector.digit = "CONTAINDIGIT";
            else
                featuresVector.digit = "NODIGIT";

            if (featureFactory.test_common(word))
                featuresVector.commonName = true;

            if (featureFactory.test_names(word))
                featuresVector.properName = true;

            Matcher m0 = featureFactory.isPunct.matcher(word);
            if (m0.find()) {
                featuresVector.punctType = "PUNCT";
            }
            if ((word.equals("(")) | (word.equals("["))) {
                featuresVector.punctType = "OPENBRACKET";
            } else if ((word.equals(")")) | (word.equals("]"))) {
                featuresVector.punctType = "ENDBRACKET";
            } else if (word.equals(".")) {
                featuresVector.punctType = "DOT";
            } else if (word.equals(",")) {
                featuresVector.punctType = "COMMA";
            } else if (word.equals("-")) {
                featuresVector.punctType = "HYPHEN";
            } else if (word.equals("\"") | word.equals("\'") | word.equals("`")) {
                featuresVector.punctType = "QUOTE";
            }

            if (featuresVector.capitalisation == null)
                featuresVector.capitalisation = "NOCAPS";

            if (featuresVector.digit == null)
                featuresVector.digit = "NODIGIT";

            if (featuresVector.punctType == null)
                featuresVector.punctType = "NOPUNCT";

            if (isPlace)
                featuresVector.locationName = true;

            //if (featureFactory.test_country(word))
            if (isCountry)
                featuresVector.countryName = true;

            featuresVector.wordShape = TextUtilities.wordShape(word);
        }

        return featuresVector;
    }
}
