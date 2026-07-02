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

import java.util.regex.Matcher;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for reference identification in raw texts such as patent descriptions.
 * It covers references to scholar works and to patent publications.
 *
 */
public class FeaturesVectorReference {
    // default bins for relative position, set experimentally
    static private int nbBins = 12;

    public String string = null; // lexical feature
    public String label = null; // label if known

    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean locationName = false;
    public boolean year = false;
    public boolean month = false;
    public boolean http = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT,
    // COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE
    public boolean isKnownJournalTitle = false;
    public boolean isKnownAbbrevJournalTitle = false;
    public boolean isKnownConferenceTitle = false;
    public boolean isKnownPublisher = false;

    public boolean isCountryCode = false;
    public boolean isKindCode = false;

    public int relativeDocumentPosition = -1;

    public FeaturesVectorReference() {
    }

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

        // lexical information (7)
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

        // bibliographical information(4)
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

        if (isCountryCode)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKindCode)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // token length
        res.append(" " + string.length());

        // relative document position
        res.append(" " + relativeDocumentPosition);

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");

        //else
        //    res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add the features for the patent reference extraction model.
     */
    public static FeaturesVectorReference addFeaturesPatentReferences(
            LayoutToken token,
            String localLabel,
            int totalLength,
            int position,
            boolean isJournalToken,
            boolean isAbbrevJournalToken,
            boolean isConferenceToken,
            boolean isPublisherToken) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorReference featuresVector = new FeaturesVectorReference();
        //StringTokenizer st = new StringTokenizer(line, "\t");
        //if (st.hasMoreTokens()) {

        String word = token.getText();
        String label = localLabel;
        /*if (st.hasMoreTokens())
            label = st.nextToken();*/

        featuresVector.string = word;
        featuresVector.label = label;

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

        if (featureFactory.test_month(word))
            featuresVector.month = true;

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

        Matcher m2 = featureFactory.year.matcher(word);
        if (m2.find()) {
            featuresVector.year = true;
        }

        Matcher m4 = featureFactory.http.matcher(word);
        if (m4.find()) {
            featuresVector.http = true;
        }

        if (featureFactory.test_city(word)) {
            featuresVector.locationName = true;
        }

        if (featuresVector.capitalisation == null)
            featuresVector.capitalisation = "NOCAPS";

        if (featuresVector.digit == null)
            featuresVector.digit = "NODIGIT";

        if (featuresVector.punctType == null)
            featuresVector.punctType = "NOPUNCT";

        if (featureFactory.test_country_codes(word))
            featuresVector.isCountryCode = true;

        if (featureFactory.test_kind_codes(word))
            featuresVector.isKindCode = true;

        featuresVector.relativeDocumentPosition = featureFactory.linearScaling(position, totalLength, nbBins);

        if (isJournalToken) {
            featuresVector.isKnownJournalTitle = true;
        }

        if (isAbbrevJournalToken) {
            featuresVector.isKnownAbbrevJournalTitle = true;
        }

        if (isConferenceToken) {
            featuresVector.isKnownConferenceTitle = true;
        }

        if (isPublisherToken) {
            featuresVector.isKnownPublisher = true;
        }
        //}

        return featuresVector;
    }

}
