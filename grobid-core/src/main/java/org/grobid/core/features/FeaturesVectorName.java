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

import org.apache.commons.collections4.CollectionUtils;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

/**
 * Class for features used for parsing sequence of names.
 *
 */
public class FeaturesVectorName {
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean lastName = false;
    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)

    public boolean isKnownTitle = false;
    public boolean isKnownSuffix = false;

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

        // lexical information (3)
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

        if (isKnownTitle)
            res.append(" 1");
        else
            res.append(" 0");

        if (isKnownSuffix)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        else
            res.append(" 0\n");

        return res.toString();
    }

    /**
     * Add feature for name parsing.
     */
    static public String addFeaturesName(
            List<LayoutToken> tokens,
            List<String> labels,
            List<OffsetPosition> titlePosition,
            List<OffsetPosition> suffixPosition) throws Exception {
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        StringBuffer header = new StringBuffer();
        boolean newline = true;
        String previousTag = null;
        String previousText = null;
        FeaturesVectorName features = null;
        LayoutToken token = null;

        int currentTitlePosition = 0;
        int currentSuffixPosition = 0;

        boolean isTitleToken;
        boolean isSuffixToken;
        boolean skipTest;

        for (int n = 0; n < tokens.size(); n++) {
            boolean outputLineStatus = false;
            isTitleToken = false;
            isSuffixToken = false;
            skipTest = false;

            token = tokens.get(n);

            /*if (line == null) {
                header.append("\n \n");
                newBlock = true;
                newline = true;
                n++;
                continue;
            }
            line = line.trim();
            if (line.length() == 0) {
                header.append("\n \n");
                newBlock = true;
                newline = true;
                n++;
                continue;
            }

            if (line.equals("@newline")) {
                if (newline) {
                    newBlock = true;
                }
                newline = true;
                n++;
                continue;
            }*/

            //int ind = line.indexOf(" ");
            String text = token.getText();
            if (text.equals(" ")) {
                continue;
            }

            newline = false;
            if (text.equals("\n")) {
                newline = true;
                continue;
            }

            // parano normalisation
            text = UnicodeUtil.normaliseTextAndRemoveSpaces(text);
            if (text.trim().length() == 0) {
                continue;
            }

            // check the position of matches for journals
            if ((titlePosition != null) && (titlePosition.size() > 0)) {
                if (currentTitlePosition == titlePosition.size() - 1) {
                    if (titlePosition.get(currentTitlePosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentTitlePosition; i < titlePosition.size(); i++) {
                        if ((titlePosition.get(i).start <= n) &&
                                (titlePosition.get(i).end >= n)) {
                            isTitleToken = true;
                            currentTitlePosition = i;
                            break;
                        } else if (titlePosition.get(i).start > n) {
                            isTitleToken = false;
                            currentTitlePosition = i;
                            break;
                        }
                    }
                }
            }
            // check the position of matches for abbreviated journals
            skipTest = false;
            if (suffixPosition != null) {
                if (currentSuffixPosition == suffixPosition.size() - 1) {
                    if (suffixPosition.get(currentSuffixPosition).end < n) {
                        skipTest = true;
                    }
                }
                if (!skipTest) {
                    for (int i = currentSuffixPosition; i < suffixPosition.size(); i++) {
                        if ((suffixPosition.get(i).start <= n) &&
                                (suffixPosition.get(i).end >= n)) {
                            isSuffixToken = true;
                            currentSuffixPosition = i;
                            break;
                        } else if (suffixPosition.get(i).start > n) {
                            isSuffixToken = false;
                            currentSuffixPosition = i;
                            break;
                        }
                    }
                }
            }

            String tag = null;
            if (!CollectionUtils.isEmpty(labels) && (labels.size() > n)) {
                tag = labels.get(n);
            }

            if (TextUtilities.filterLine(text)) {
                continue;
            }

            features = new FeaturesVectorName();
            features.string = text;

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
            } else if (tokens.size() == n + 1) {
                if (!outputLineStatus) {
                    features.lineStatus = "LINEEND";
                    outputLineStatus = true;
                }
            } else {
                // look ahead...
                boolean endline = false;
                int i = 1;
                boolean endloop = false;
                while ((tokens.size() > n + i) & (!endloop)) {
                    String newLine = tokens.get(n + i).getText();
                    if (newLine != null) {
                        if (newLine.equals("\n")) {
                            endline = true;
                            if (!outputLineStatus) {
                                features.lineStatus = "LINEEND";
                                outputLineStatus = true;
                            }
                            endloop = true;
                        } else if (!newLine.equals(" ")) {
                            endloop = true;
                        }
                    }

                    /*if ((endline) & (!outputLineStatus)) {
                        features.lineStatus = "LINEEND";
                        outputLineStatus = true;
                    }*/
                    i++;
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

            if (features.capitalisation == null)
                features.capitalisation = "NOCAPS";

            if (featureFactory.test_digit(text)) {
                features.digit = "CONTAINSDIGITS";
            }

            if (featureFactory.test_common(text)) {
                features.commonName = true;
            }

            if (featureFactory.test_first_names(text)) {
                features.firstName = true;
            }

            if (featureFactory.test_last_names(text)) {
                features.lastName = true;
            }

            Matcher m = featureFactory.isDigit.matcher(text);
            if (m.find()) {
                features.digit = "ALLDIGIT";
            }

            if (features.digit == null)
                features.digit = "NODIGIT";

            if (features.punctType == null)
                features.punctType = "NOPUNCT";

            if (isTitleToken) {
                features.isKnownTitle = true;
            }

            if (isSuffixToken) {
                features.isKnownSuffix = true;
            }

            features.label = tag;

            header.append(features.printVector());

            previousTag = tag;
            previousText = text;
        }

        return header.toString();
    }

}
