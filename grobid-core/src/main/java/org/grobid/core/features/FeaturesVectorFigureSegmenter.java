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
public class FeaturesVectorFigureSegmenter {
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

    //public boolean acronym = false;
    public String punctType = null; // one of NOPUNCT, OPENBRACKET, ENDBRACKET, DOT, COMMA, HYPHEN, QUOTE, PUNCT (default)
    public String punctuationProfile = null; // the punctuations of the current line of the token

    public int spacingWithPreviousBlock = 0; // discretized 
    public int characterDensity = 0; // discretized 
    public int blockSize = 0; // discretized 

    // font size related
    public String fontSize = null; // one of HIGHERFONT, SAMEFONTSIZE, LOWERFONT

    // relative position from the graphic box, discretized and relative to the window size
    public int relativePositionFromGraphicBox = -1;

    // indicate if the token is in the identified graphic box or not
    public boolean inGraphicBox = false;

    public String segmentationLabel = "OTHER"; // the label assigned to the token by the upstream segmentation model

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

        // punctuation information (1)
        res.append(" " + punctType); // in case the token is a punctuation (NO otherwise)

        // punctuation profile
        if ( (punctuationProfile == null) || (punctuationProfile.length() == 0) ) {
            // string profile
            res.append(" no");
            // number of punctuation symbols in the line
            res.append(" 0");
        }
        else {
            // string profile
            res.append(" " + punctuationProfile);
            // number of punctuation symbols in the line
            res.append(" "+punctuationProfile.length());
        }

        // space with previous block, discretised (1)
        res.append(" " + spacingWithPreviousBlock);

        // character density of the block, discretised (1)
        res.append(" " + characterDensity);

        // block size, discretised (1)
        res.append(" " + blockSize);

        // relativePositionFromGraphicBox, discretised (1)
        res.append(" " + relativePositionFromGraphicBox);

        // in graphic box or not (1)
        if (inGraphicBox)
            res.append(" 1");
        else
            res.append(" 0");

        res.append(" "+segmentationLabel);

        // 29 features written at this point

        // label - for training data (1)
        if (label != null)
            res.append(" " + label + "\n");
        else
            res.append("\n");

        return res.toString();
    }

}