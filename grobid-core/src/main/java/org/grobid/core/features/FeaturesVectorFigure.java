package org.grobid.core.features;

import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for features used for figure/table parsing.
 *
 */
public class FeaturesVectorFigure {
    public LayoutToken token = null; // not a feature, reference value
    public String string = null; // lexical feature
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

    public String punctType = null;
    // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)

    public int relativeDocumentPosition = -1;
    public int relativePagePosition = -1;

    public boolean bitmapAround = false;
    public boolean vectorAround = false;
    public int closestGraphicHeight = -1;
    public int closestGraphicWidth = -1;
    public int closestGraphicSurface = -1;

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

        // block information (1)
        res.append(" " + blockStatus);
        //res.append(" 0");

        // line information (1)
        res.append(" " + lineStatus);

        // page information (1)
        res.append(" " + pageStatus);

        // alignment/horizontal position information (1)
        //res.append(" " + alignmentStatus);

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

        // punctuation information (2)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // relative document position (1)
        res.append(" " + relativeDocumentPosition);

        // relative page position (1)
        res.append(" " + relativePagePosition);

        if (bitmapAround)
            res.append(" 1");
        else
            res.append(" 0");

        if (vectorAround)
            res.append(" 1");
        else
            res.append(" 0");

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
