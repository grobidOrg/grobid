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

import java.util.StringTokenizer;
import java.util.regex.Matcher;

import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for chemical entity identification in raw texts such as scientific articles
 * and patent descriptions.
 *
 */
public class FeaturesVectorChemicalEntity {
    // default bins for relative position, set experimentally
    static private int nbBins = 12;

    public String string = null;     // lexical feature
    public String label = null;     // label if known

    public String capitalisation = null;// one of INITCAP, ALLCAPS, NOCAPS
    public String digit;                  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    // OPENQUOTE, ENDQUOTE

    public boolean isKnownChemicalToken = false;
    public boolean isKnownChemicalNameToken = false;

    public int relativeDocumentPosition = -1;

    public FeaturesVectorChemicalEntity() {
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

        // lexical information (2)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        // chemistry vocabulary information (2)
        if (isKnownChemicalToken)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownChemicalNameToken)
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
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add the features for the chemical entity extraction model.
     */
    static public FeaturesVectorChemicalEntity addFeaturesChemicalEntities(
            String line,
            int totalLength,
            int position,
            boolean isChemicalToken,
            boolean isChemicalNameToken) {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        FeaturesVectorChemicalEntity featuresVector = new FeaturesVectorChemicalEntity();
        StringTokenizer st = new StringTokenizer(line, "\t");
        if (st.hasMoreTokens()) {
            String word = st.nextToken();
            String label = null;
            if (st.hasMoreTokens())
                label = st.nextToken();

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

            featuresVector.relativeDocumentPosition = featureFactory.linearScaling(position, totalLength, nbBins);

            if (isChemicalToken) {
                featuresVector.isKnownChemicalToken = true;
            }

            if (isChemicalNameToken) {
                featuresVector.isKnownChemicalNameToken = true;
            }
        }

        return featuresVector;
    }

}
