package org.grobid.core.sax;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;

/**
 * SAX parser initially made for XML CLEF IP data (collection, training and topics),
 * but it works also fine for parsing ST.36 flavors as the formats are similar.
 *
 */
public class ST36SaxParser extends DefaultHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(ST36SaxParser.class);

    private StringBuilder accumulator = new StringBuilder(); // Accumulate parsed text
    private StringBuilder accumulatorRef = new StringBuilder(); // Accumulate parsed text

    private String PatentNumber = null;
    private int PatentID = -1;
    private String PublicDate = null;
    private String PriorityDate = null;
    private String CodeType = null;
    private String PublicationDate = null;
    private String Content = null;
    private List<String> CitedPatentNumber = null;
    private List<Integer> CitationID = null;
    private String Classification = null;

    // working variables
    private String cited_number = null;

    public Map<String, ArrayList<String>> referencesPatent = null;
    public List<String> referencesNPL = null;
    public List<String> citations = null; // search report citations

    private boolean npl = false; // indicate if the current reference is to patent or to a npl
    private boolean ref = false; // are we reading a ref?

    // if a reference has been found in the current considered text segment
    private boolean refFound = false;

    // this boolean keeps track of possible content outside paragraph, that might require some
    // further segmentations
    private boolean outsideParagraph = true;

    private int nbNPLRef = 0;
    private int nbPatentRef = 0;
    public int nbAllRef = 0;

    private int window = -1;  // window of text to be output around the reference strings
    // value at -1 means no window considered - everything will be outputted

    public boolean patentReferences = false;
    public boolean nplReferences = false;

    private String currentFileName = null;

    // this is the accumulated segments to be labeled or with labels for training
    public List<List<LayoutToken>> allAccumulatedTokens = null;
    public List<List<String>> allAccumulatedLabels = null;

    // the current segment to be labeled or with labels for training
    public List<LayoutToken> accumulatedTokens = null;
    public List<String> accumulatedLabels = null;

    private GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();

    public ST36SaxParser() {
    }

    public void setWindow(int n) {
        this.window = n;
    }

    public void characters(char[] buffer, int start, int length) {
        if (ref) {
            accumulatorRef.append(buffer, start, length);
        } else {
            accumulator.append(buffer, start, length);
        }
    }

    public String getText() {
        //System.out.println(accumulator.toString().trim());
        return accumulator.toString().trim();
    }

    public int getNbNPLRef() {
        return nbNPLRef;
    }

    public int getNbPatentRef() {
        return nbPatentRef;
    }

    public String getRefText() {
        //System.out.println(accumulator.toString().trim());
        return accumulatorRef.toString().trim();
    }

    public void setFileName(String name) {
        currentFileName = name;
        if (referencesPatent == null) {
            referencesPatent = new HashMap<String, ArrayList<String>>();
        }
        referencesPatent.put(name, new ArrayList<String>());
    }

    public void endElement(
            java.lang.String uri,
            java.lang.String localName,
            java.lang.String qName) throws SAXException {
        if (qName.equals("date")) {
            accumulator.setLength(0);
        } else if (qName.equals("ref") || qName.equals("bibl")) {
            String refString = getRefText();
            refString = refString.replace("\n", " ");
            refString = refString.replace("\t", " ");
            refString = refString.replace("  ", " ");

            if (npl && ref) {
                if (referencesNPL == null)
                    referencesNPL = new ArrayList<String>();
                referencesNPL.add(refString);
                refFound = true;
                if (nplReferences)
                    nbNPLRef++;
            } else if (ref) {
                if (referencesPatent == null) {
                    referencesPatent = new HashMap<String, ArrayList<String>>();
                }
                ArrayList<String> refss = referencesPatent.get(currentFileName);

                if (refss == null) {
                    refss = new ArrayList<String>();
                }

                refss.add(refString);
                referencesPatent.put(currentFileName, refss);
                refFound = true;
                if (patentReferences) {
                    nbPatentRef++;
                }
            }

            if (refFound) {
                // we tokenize the text
                List<String> tokenizations = new ArrayList<String>();
                try {
                    // TBD: pass a language object to the tokenize method call
                    tokenizations = analyzer.tokenize(refString);
                } catch (Exception e) {
                    LOGGER.debug("Tokenization for XML patent document has failed.");
                }

                int i = 0;
                for (String token : tokenizations) {
                    //token = st.nextToken().trim();
                    if ((token.trim().length() == 0) ||
                            (token.equals(" ")) ||
                            (token.equals("\t")) ||
                            (token.equals("\n")) ||
                            (token.equals("\r"))) {
                        continue;
                    }
                    try {
                        accumulatedTokens.add(new LayoutToken(token));
                        if (npl) {
                            if (nplReferences) {
                                if (i == 0) {
                                    accumulatedLabels.add("I-<refNPL>");
                                } else if (token == null) {
                                    accumulatedLabels.add("E-<refNPL>");
                                } else {
                                    accumulatedLabels.add("<refNPL>");
                                }
                            } else {
                                accumulatedLabels.add("<other>");
                            }
                        } else {
                            if (patentReferences) {
                                if (i == 0) {
                                    accumulatedLabels.add("I-<refPatent>");
                                } else if (token == null) {
                                    accumulatedLabels.add("E-<refPatent>");
                                } else {
                                    accumulatedLabels.add("<refPatent>");
                                }
                            } else {
                                accumulatedLabels.add("<other>");
                            }
                        }
                    } catch (Exception e) {
                        throw new GrobidException("An exception occurred while running Grobid.", e);
                    }
                    i++;
                }
            }
            ref = false;
        } else if (qName.equals("classification-ipcr")) {
            accumulator.setLength(0);
        } else if (qName.equals("classification-symbol")) {
            accumulator.setLength(0);
        } else if (qName.equals("abstract")) {
            accumulator.setLength(0);
        } /*else if (qName.equals("heading")) {
            accumulator.append(" ");
          }*/
        else if (qName.equals("description")) {
            // In case we have no paragraph structures, we will get the whole description in a huge single text block
            // this text needs to be segmented in to paragraph-like blocks to be used by Deep Learning approaches.

        } else if (qName.equals("p") || qName.equals("heading")) {
            accumulator.append("\n");
            List<List<String>> allTokenizations = new ArrayList<>();

            String content = getText();

            // we tokenize the text
            List<String> tokenization = new ArrayList<>();
            try {
                // TBD: pass a language object to the tokenize method call
                tokenization = analyzer.tokenize(content);
            } catch (Exception e) {
                LOGGER.debug("Tokenization for XML patent document has failed.");
            }

            // we could introduce here some further sub-segmentation
            allTokenizations.add(tokenization);

            for (List<String> tokenizations : allTokenizations) {
                int i = 0;
                for (String token : tokenizations) {
                    //token = st.nextToken().trim();
                    if ((token.trim().length() == 0) ||
                            (token.equals(" ")) ||
                            (token.equals("\t")) ||
                            (token.equals("\n")) ||
                            (token.equals("\r"))) {
                        continue;
                    }
                    // we print only a window of N words
                    if ((i > window) && (window != -1)) {
                        token = token.trim();
                        if (token.length() > 0) {
                            accumulatedTokens.add(new LayoutToken(token));
                            accumulatedLabels.add("<ignore>");
                        }
                    } else {
                        try {
                            token = token.trim();
                            if (token.length() > 0) {
                                accumulatedTokens.add(new LayoutToken(token));
                                accumulatedLabels.add("<other>");
                            }
                        } catch (Exception e) {
                            throw new GrobidException("An exception occurred while running Grobid.", e);
                        }
                    }
                    i++;
                }

                allAccumulatedTokens.add(accumulatedTokens);
                allAccumulatedLabels.add(accumulatedLabels);
                accumulatedTokens = new ArrayList<>();
                accumulatedLabels = new ArrayList<>();
            }

            accumulator.setLength(0);
            refFound = false;

            allAccumulatedTokens.add(accumulatedTokens);
            allAccumulatedLabels.add(accumulatedLabels);
            outsideParagraph = true;

        } else if (qName.equals("patcit")) {
            // we register the citation, the citation context will be marked in a later stage
            if (citations == null)
                citations = new ArrayList<String>();
            citations.add(cited_number);
            accumulator.setLength(0);
        } else if (qName.equals("invention-title")) {
            accumulator.setLength(0);
        } else if (qName.equals("applicants")) {
            accumulator.setLength(0);
        } else if (qName.equals("inventors")) {
            accumulator.setLength(0);
        } else if (qName.equals("document-id")) {
            accumulator.setLength(0);
        } else if (qName.equals("legal-status")) {
            accumulator.setLength(0);
        } else if (qName.equals("bibliographic-data")) {
            accumulator.setLength(0);
        } else if (qName.equals("doc-number")) {
            accumulator.setLength(0);
        } else if (qName.equals("country")) {
            accumulator.setLength(0);
        } else if (qName.equals("kind")) {
            accumulator.setLength(0);
        } else if (qName.equals("classification-symbol")) {
            accumulator.setLength(0);
        } else if (qName.equals("classification-ecla")) {
            accumulator.setLength(0);
        } else if (qName.equals("patent-document") || qName.equals("fulltext-document")) {

        } else if (qName.equals("row")) {
            accumulator.append(" ");
        }
    }

    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts) throws SAXException {
        if (qName.equals("patent-document") || qName.equals("fulltext-document")) {
            nbNPLRef = 0;
            nbPatentRef = 0;
            nbAllRef = 0;
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("lang")) {
                        //Global_Language_Code = value.toLowerCase();
                    }
                    if (name.equals("doc-number")) {
                        PatentNumber = "EP" + value;
                    }
                    if (name.equals("kind")) {
                        CodeType = value;
                    }
                    if (name.equals("date")) {
                        PublicDate = value;
                    }
                }
            }

            CitedPatentNumber = new ArrayList<String>();
            accumulator.setLength(0);
            allAccumulatedTokens = new ArrayList<>();
            allAccumulatedLabels = new ArrayList<>();
        } else if (qName.equals("description")) {
            accumulator.setLength(0);
        } else if (qName.equals("p") || qName.equals("heading")) {
            // possible text read outside <p> and <heading>?
            outsideParagraph = false;
            accumulatedTokens = new ArrayList<>();
            accumulatedLabels = new ArrayList<>();
            accumulator.setLength(0);
        } else if (qName.equals("ref") || qName.equals("bibl")) {
            int length = atts.getLength();
            nbAllRef++;
            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("type") || name.equals("typ")) {
                        if (value.equals("npl") || value.equals("book") || value.equals("journal")) {
                            String content = getText();
                            List<List<String>> allTokenizations = new ArrayList<>();

                            // we output what has been read so far in the description

                            // we tokenize the text
                            List<String> tokenization = new ArrayList<String>();
                            try {
                                // TBD: pass a language object to the tokenize method call
                                tokenization = analyzer.tokenize(content);
                            } catch (Exception e) {
                                LOGGER.debug("Tokenization for XML patent document has failed.");
                            }

                            int nbTokens = tokenization.size();

                            // we could introduce here some further sub-segmentation
                            allTokenizations.add(tokenization);

                            //boolean newSegment = false;
                            for (List<String> tokenizations : allTokenizations) {

                                /*if (newSegment) {
                                    allAccumulatedTokens.add(accumulatedTokens);
                                    allAccumulatedLabels.add(accumulatedLabels);
                                    accumulatedTokens = new ArrayList<>();
                                    accumulatedLabels = new ArrayList<>();
                                    newSegment = false;
                                }*/

                                int j = 0;
                                for (String token : tokenizations) {
                                    if ((token.trim().length() == 0) ||
                                            (token.equals(" ")) ||
                                            (token.equals("\t")) ||
                                            (token.equals("\n")) ||
                                            (token.equals("\r"))) {
                                        continue;
                                    }

                                    if (window == -1 ||
                                            ((j > (nbTokens - window) && (window != -1))
                                                    || (refFound && (j < window) && (window != -1)))) {
                                        try {
                                            accumulatedTokens.add(new LayoutToken(token));
                                            accumulatedLabels.add("<other>");
                                        } catch (Exception e) {
                                            throw new GrobidException("An exception occurred while running Grobid.", e);
                                        }
                                    } else {
                                        try {
                                            accumulatedTokens.add(new LayoutToken(token));
                                            accumulatedLabels.add("<ignore>");
                                        } catch (Exception e) {
                                            throw new GrobidException("An exception occurred while running Grobid.", e);
                                        }
                                    }
                                    j++;
                                }
                                //newSegment = true;
                            }

                            accumulator.setLength(0);

                            npl = true;
                            ref = true;
                        } else if (value.equals("patent") || value.equals("pl")) {
                            String content = getText();
                            List<List<String>> allTokenizations = new ArrayList<>();

                            // we output what has been read so far in the description

                            // we tokenize the text
                            List<String> tokenization = new ArrayList<String>();
                            try {
                                // TBD: pass a language object to the tokenize method call
                                tokenization = analyzer.tokenize(content);
                            } catch (Exception e) {
                                LOGGER.debug("Tokenization for XML patent document has failed.");
                            }

                            int nbTokens = tokenization.size();

                            // we could introduce here some further sub-segmentation
                            allTokenizations.add(tokenization);

                            //boolean newSegment = false;
                            for (List<String> tokenizations : allTokenizations) {

                                /*if (newSegment) {
                                    allAccumulatedTokens.add(accumulatedTokens);
                                    allAccumulatedLabels.add(accumulatedLabels);
                                    accumulatedTokens = new ArrayList<>();
                                    accumulatedLabels = new ArrayList<>();
                                    newSegment = false;
                                }*/

                                int j = 0;
                                for (String token : tokenizations) {
                                    if ((token.trim().length() == 0) ||
                                            (token.equals(" ")) ||
                                            (token.equals("\t")) ||
                                            (token.equals("\n")) ||
                                            (token.equals("\r"))) {
                                        continue;
                                    }

                                    if (window == -1 ||
                                            ((j > (nbTokens - window)) || (refFound && (j < window)))) {
                                        try {
                                            accumulatedTokens.add(new LayoutToken(token));
                                            accumulatedLabels.add("<other>");
                                        } catch (Exception e) {
                                            throw new GrobidException("An exception occurred while running Grobid.", e);
                                        }
                                    } else {
                                        try {
                                            accumulatedTokens.add(new LayoutToken(token));
                                            accumulatedLabels.add("<ignore>");
                                        } catch (Exception e) {
                                            throw new GrobidException("An exception occurred while running Grobid.", e);
                                        }
                                    }
                                    j++;
                                }
                                //newSegment = true;
                            }

                            accumulator.setLength(0);
                            npl = false;
                            ref = true;
                        } else {
                            System.out.println("Warning: unknown attribute value for ref or bibl: " + value);
                            ref = false;
                            npl = false;
                        }
                    }
                }
            }

            accumulatorRef.setLength(0);
        } else if (qName.equals("claim")) {
            accumulator.setLength(0);
        } else if (qName.equals("invention-title")) {
            accumulator.setLength(0);
        } else if (qName.equals("patcit")) {
            int length = atts.getLength();

            // Process each attribute
            for (int i = 0; i < length; i++) {
                // Get names and values for each attribute
                String name = atts.getQName(i);
                String value = atts.getValue(i);

                if (name != null) {
                    if (name.equals("ucid")) {
                        cited_number = value;
                        // we might need to normalize a little bit this patent nummer
                    }
                }
            }
        }
    }

}
