package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for fulltext parsing.
 *
 */
public class FeaturesVectorFulltext {
    public LayoutToken token = null; // not a feature, reference value
    public String string = null; // lexical feature
    public String label = null; // label if known
    public String blockStatus = null; // one of BLOCKSTART, BLOCKIN, BLOCKEND
    public String lineStatus = null; // one of LINESTART, LINEIN, LINEEND
    public String fontStatus = null; // one of NEWFONT, SAMEFONT
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT
    public String alignmentStatus = null; // one of ALIGNEDLEFT, INDENTED, CENTERED - applied to the whole line
    public boolean bold = false;
    public boolean italic = false;
    public String capitalisation = null; // one of INITCAP, ALLCAPS, NOCAPS
    public String digit;  // one of ALLDIGIT, CONTAINDIGIT, NODIGIT
    public boolean singleChar = false;

    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)

    public int relativeDocumentPosition = -1;
    public int relativePagePositionChar = -1;
    public int relativePagePosition = -1;

    // graphic in closed proximity of the current block
    public boolean bitmapAround = false;
    public boolean vectorAround = false;

    // if a graphic is in close proximity of the current block, characteristics of this graphic
    public int closestGraphicHeight = -1;
    public int closestGraphicWidth = -1;
    public int closestGraphicSurface = -1;

    public int spacingWithPreviousBlock = 0; // discretized
    public int characterDensity = 0; // discretized

    // how the reference callouts are expressed, if known
    public String calloutType = null; // one of UNKNOWN, NUMBER, AUTHOR
    public boolean calloutKnown = false; // true if the token match a known reference label
    public boolean superscript = false;

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

        // at this stage, we have written 10 features

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

        // at this stage, we have written 20 features

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // relative document position (1)
        res.append(" " + relativeDocumentPosition);

        // relative page position (1)
        res.append(" " + relativePagePosition);

        // proximity of a graphic to the current block (2)
        if (bitmapAround)
            res.append(" 1");
        else
            res.append(" 0");

        /*if (vectorAround)
            res.append(" 1");
        else
            res.append(" 0");*/

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

        if (calloutType != null)
            res.append(" " + calloutType);
        else
            res.append(" UNKNOWN");

        if (calloutKnown)
            res.append(" 1");
        else
            res.append(" 0");

        if (superscript)
            res.append(" 1");
        else
            res.append(" 0");

        res.append("\n");

        return res.toString();
    }

}
