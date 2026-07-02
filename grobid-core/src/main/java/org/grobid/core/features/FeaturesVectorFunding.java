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

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * Class for features used for recognizing funding.
 *
 */
public class FeaturesVectorFunding {
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean containDash = false;
    public boolean knownFunder = false;
    public boolean knownInfrastructure = false;
    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public boolean containPunct = false;

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

        // lexical information (2)
        if (knownFunder)
            res.append(" 1");
        else
            res.append(" 0");

        // lexical information (2)
        if (knownInfrastructure)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (2)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add features for funding recognition.
     */
    public static String addFeatures(List<LayoutToken> tokens, List<String> tags) throws Exception {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        List<OffsetPosition> funderPositions = Lexicon.getInstance().tokenPositionsFunderNames(tokens);
        List<OffsetPosition> infrastructurePositions = Lexicon.getInstance()
                .tokenPositionsResearchInfrastructureNames(tokens);

        String line;
        StringBuilder stringBuilder = new StringBuilder();
        FeaturesVectorFunding features = null;
        boolean newline = true;

        int currentFunderPositions = 0;
        boolean isKnownFunderToken = false;
        int currentInfrastructurePositions = 0;
        boolean isKnownInfrastructureToken = false;
        boolean skipTest;

        for (int n = 0; n < tokens.size(); n++) {
            boolean outputLineStatus = false;
            isKnownFunderToken = false;
            skipTest = false;

            LayoutToken token = tokens.get(n);
            String text = token.getText();
            String tag = null;
            if (tags != null && tags.size() == tokens.size())
                tag = tags.get(n);

            if (text == null || text.length() == 0) {
                continue;
            }

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

            /*boolean filter = false;
            if (text == null) {
                filter = true;
            } else if (text.length() == 0) {
                filter = true;
            } else if (text.startsWith("@IMAGE")) {
                filter = true;
            } else if (text.contains(".pbm")) {
                filter = true;
            } else if (text.contains(".svg")) {
                filter = true;
            } else if (text.contains(".jpg")) {
                filter = true;
            } else if (text.contains(".png")) {
                filter = true;
            }

            if (filter)
                continue;*/

            features = new FeaturesVectorFunding();
            features.string = text;

            // check the position of matches for known funders
            if ((funderPositions != null) && (funderPositions.size() > 0)) {
                if (currentFunderPositions == funderPositions.size() - 1) {
                    if (funderPositions.get(currentFunderPositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentFunderPositions; i < funderPositions.size(); i++) {
                        if ((funderPositions.get(i).start <= n) &&
                                (funderPositions.get(i).end >= n)) {
                            isKnownFunderToken = true;
                            currentFunderPositions = i;
                            break;
                        } else if (funderPositions.get(i).start > n) {
                            isKnownFunderToken = false;
                            currentFunderPositions = i;
                            break;
                        }
                    }
                }
            }

            // check the position of matches for known infrastructures
            if ((infrastructurePositions != null) && (infrastructurePositions.size() > 0)) {
                if (currentInfrastructurePositions == infrastructurePositions.size() - 1) {
                    if (infrastructurePositions.get(currentInfrastructurePositions).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentInfrastructurePositions; i < infrastructurePositions.size(); i++) {
                        if ((infrastructurePositions.get(i).start <= n) &&
                                (infrastructurePositions.get(i).end >= n)) {
                            isKnownInfrastructureToken = true;
                            currentInfrastructurePositions = i;
                            break;
                        } else if (infrastructurePositions.get(i).start > n) {
                            isKnownInfrastructureToken = false;
                            currentInfrastructurePositions = i;
                            break;
                        }
                    }
                }
            }

            if (newline) {
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
            } else if (n == tokens.size() - 1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
                }
            } else {
                // look ahead...
                boolean endline = false;
                int i = 1;
                boolean endloop = false;
                while ((tokens.size() > n + i) && (!endloop)) {
                    LayoutToken newToken = tokens.get(n + i);
                    String newText = newToken.getText();

                    if (newText != null) {
                        if (newText.equals("\n")) {
                            endline = true;
                            if (!outputLineStatus) {
                                features.lineStatus = "LINEEND";
                                outputLineStatus = true;
                            }
                        } else if (newText.equals("@newline")) {
                            endline = true;
                            if (!outputLineStatus) {
                                features.lineStatus = "LINEEND";
                                outputLineStatus = true;
                            }
                        } else {
                            endloop = true;
                        }
                    }

                    if (endline && !outputLineStatus) {
                        features.lineStatus = "LINEEND";
                        outputLineStatus = true;
                    }
                    i++;
                }
            }

            newline = false;
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

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isKnownFunderToken)
                features.knownFunder = true;

            if (isKnownInfrastructureToken)
                features.knownInfrastructure = true;

            if (tag != null)
                features.label = tag;

            stringBuilder.append(features.printVector());
        }

        return stringBuilder.toString();
    }
}
