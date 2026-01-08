package org.grobid.core.features;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for header parsing.
 *
 */
public class FeaturesVectorHeader {
    public LayoutToken token = null; // not a feature, reference value
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String alignmentStatus = null; // one of ALIGNEDLEFT, INDENTED, CENTERED - applied to the whole line
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    
    public boolean bold = false;
    public boolean italic = false;
    public boolean rotation = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;
    //public boolean containDash = false;
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
    //public boolean containPunct = false;

    public String punctuationProfile = null; // the punctuations of the current line of the token

    public int spacingWithPreviousBlock = 0; // discretized 
    public int characterDensity = 0; // discretized 

    // font size related
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public boolean largestFont = false;
    public boolean smallestFont = false;
    public boolean largerThanAverageFont = false;
    //public boolean superscript = false;

    public static FeaturesVectorHeader fromLayoutToken(LayoutToken token) {
        FeaturesVectorHeader features = new FeaturesVectorHeader();
        FeatureFactory featureFactory = FeatureFactory.getInstance();

        String text = token.getText();
        features.token = token;
        features.string = text;

        Matcher m0 = featureFactory.isPunct.matcher(text);

        if (m0.find()) {
            features.punctType = "PUNCT";
        }
        if (text.equals("(") || text.equals("[")) {
            features.punctType = "OPENBRACKET";

        } else if (text.equals(")") || text.equals("]")) {
            features.punctType = "ENDBRACKET";

        } else if (text.equals(".")) {
            features.punctType = "DOT";

        } else if (text.equals(",")) {
            features.punctType = "COMMA";

        } else if (text.equals("-")) {
            features.punctType = "HYPHEN";

        } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
            features.punctType = "QUOTE";
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

        Matcher m = featureFactory.isDigit.matcher(text);
        if (m.find()) {
            features.digit = "ALLDIGIT";
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

        Matcher m2 = featureFactory.year.matcher(text);
        if (m2.find()) {
            features.year = true;
        }

        if (token.isBold())
            features.bold = true;

        if (token.isItalic())
            features.italic = true;

        if (features.capitalisation == null)
            features.capitalisation = "NOCAPS";

        if (features.digit == null)
            features.digit = "NODIGIT";

        if (features.punctType == null)
            features.punctType = "NOPUNCT";

        return features;
    }

    public String printVector() {
        if (string == null) return null;
        if (string.length() == 0) return null;
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

        // 10 first features written at this stage

        // block information (1)
        res.append(" " + blockStatus);

        // line information (1)
        res.append(" " + lineStatus);
		
		// line position/indentation (1)
        res.append(" " + alignmentStatus);

        // font information (1)
        res.append(" " + fontStatus);

        // font size information (1)
        res.append(" " + fontSize);

        // string type information (2)
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

        // 20 first features written at this stage

        // lexical information (7)
        if (properName)
            res.append(" 1");
        else
            res.append(" 0");

        if (commonName)
            res.append(" 1");
        else
            res.append(" 0");

        /*if (firstName)
            res.append(" 1");
        else
            res.append(" 0");*/

        if (year)
            res.append(" 1");
        else
            res.append(" 0");

        if (month)
            res.append(" 1");
        else
            res.append(" 0");

        if (locationName)
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
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // 28 features written at this point

        // space with previous block, discretised (1)
        //res.append(" " + spacingWithPreviousBlock);
        //res.append(" " + 0);

        // character density of the previous block, discretised (1)
        //res.append(" " + characterDensity);
        //res.append(" " + 0);

        if (largestFont)
            res.append(" 1");
        else
            res.append(" 0");

        if (smallestFont)
            res.append(" 1");
        else
            res.append(" 0");

        if (largerThanAverageFont)
            res.append(" 1");
        else
            res.append(" 0");

        /*if (superscript)
            res.append(" 1");
        else
            res.append(" 0");*/

        // 30 features written at this point

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        /*else
            res.append("\n");*/
        else
            res.append(" 0\n");

        return res.toString();
    }

}