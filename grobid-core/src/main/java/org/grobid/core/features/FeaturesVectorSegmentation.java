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

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for high level segmentation of document.
 *
 */
public class FeaturesVectorSegmentation {
    public LayoutToken token = null; // not a feature, reference value
    public String line = null; // not a feature, the complete processed line

    public String string = null; // first lexical feature
    public String secondString = null; // second lexical feature
    public String label = null; // label if known
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public String pageStatus = null; // one of PAGESTART, PAGEIN, PAGEEND
    public String alignmentStatus = null; // one of ALIGNEDLEFT, INDENT, CENTERED
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    public boolean properName = false;
    public boolean commonName = false;
    public boolean firstName = false;
    public boolean locationName = false;
    public boolean year = false;
    public boolean month = false;
    public boolean email = false;
    public boolean http = false;
    //public boolean acronym = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public int relativeDocumentPosition = -1;
    public int relativePagePosition = -1;
    public int relativePagePositionChar = -1; // not used
    public String punctuationProfile = null; // the punctuations of the current line of the token
    public boolean firstPageBlock = false;
    public boolean lastPageBlock = false;
    public int lineLength = 0;
    public boolean bitmapAround = false;
    public boolean vectorAround = false;
    public boolean inMainArea = true;

    public boolean repetitivePattern = false; // if true, the textual pattern is repeated at the same position on other pages
    public boolean firstRepetitivePattern = false; // if true, this is a repetitive textual pattern and this is its first occurrence in the doc

    public int spacingWithPreviousBlock = 0; // discretized
    public int characterDensity = 0; // discretized

    public String printVector() {
        if (string == null)
            return null;
        if (string.length() == 0)
            return null;
        StringBuffer res = new StringBuffer();

        // token string (1)
        res.append(string);

        // second token string
        if (secondString != null)
            res.append(" " + secondString);
        else
            res.append(" " + string);

        // lowercase string
        res.append(" " + string.toLowerCase());

        // prefix (4)
        res.append(" " + TextUtilities.prefix(string, 1));
        res.append(" " + TextUtilities.prefix(string, 2));
        res.append(" " + TextUtilities.prefix(string, 3));
        res.append(" " + TextUtilities.prefix(string, 4));

        // block information (1)
        if (blockStatus != null)
            res.append(" " + blockStatus);
        //res.append(" 0");

        // line information (1)
        if (lineStatus != null)
            res.append(" " + lineStatus);

        // line alignment/indentation information (1)
        //res.append(" " + alignmentStatus);

        // page information (1)
        res.append(" " + pageStatus);

        // font information (1)
        res.append(" " + fontStatus);

        // font size information (1)
        res.append(" " + fontSize);

        // string type information (3)
        if (bold)
            res.append(" 1");
        else
            res.append(" 0");

        if (italic)
            res.append(" 1");
        else
            res.append(" 0");

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

        // lexical information (9)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        /* TODO: to review, never set! */
        if (firstName)
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

        if (email)
            res.append(" 1");
        else
            res.append(" 0");

        if (http)
            res.append(" 1");
        else
            res.append(" 0");

        // punctuation information (1)
        if (punctType != null)
            res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // relative document position (1)
        res.append(" " + relativeDocumentPosition);

        // relative page position coordinate (1)
        //res.append(" " + relativePagePosition);

        // relative page position characters (1)
        res.append(" " + relativePagePositionChar);

        // punctuation profile
        if ((punctuationProfile == null) || (punctuationProfile.length() == 0)) {
            // string profile
            res.append(" no");
            // number of punctuation symbols in the line
            res.append(" 0");
        } else {
            // string profile
            res.append(" " + punctuationProfile);
            // number of punctuation symbols in the line
            res.append(" " + punctuationProfile.length());
        }

        // current line length on a predefined scale and relative to the longest line of the current block
        res.append(" " + lineLength);

        if (bitmapAround) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        if (vectorAround) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        if (repetitivePattern) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        if (firstRepetitivePattern) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        // if the block is in the page main area (1)
        if (inMainArea) {
            res.append(" 1");
        } else {
            res.append(" 0");
        }

        // space with previous block, discretised (1)
        //res.append(" " + spacingWithPreviousBlock);
        //res.append(" " + 0);

        // character density of the previous block, discretised (1)
        //res.append(" " + characterDensity);
        //res.append(" " + 0);

        // label - for training data (1)
        /*if (label != null)
              res.append(" " + label + "\n");
          else
              res.append(" 0\n");
          */

        res.append("\n");

        return res.toString();
    }

}
