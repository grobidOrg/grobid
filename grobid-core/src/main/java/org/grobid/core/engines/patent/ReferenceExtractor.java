package org.grobid.core.engines.patent;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.analyzers.GrobidDefaultAnalyzer;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.PatentItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.document.OPSService;
import org.grobid.core.document.PatentDocument;
import org.grobid.core.engines.EngineParsers;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeaturesVectorReference;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.sax.PatentAnnotationSaxParser;
import org.grobid.core.sax.TextSaxParser;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.Consolidation;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;

/**
 * Extraction of patent and NPL references from the content body of patent document with sequence labeling.
 *
 */
public class ReferenceExtractor implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceExtractor.class);

    private GenericTagger taggerAll = null;
    private PatentRefParser patentParser = null;
    private Consolidation consolidator = null;

    private String tmpPath = null;

    public boolean debug = false;

    public Lexicon lexicon = Lexicon.getInstance();
    public String currentPatentNumber = null;
    public OPSService ops = null;
    private List<String> descriptionSegments = null;

    public ArrayList<org.grobid.core.data.BibDataSet> resBib = null; // identified current parsed
    // bibliographical items and related information

    private String path = null;     // path where the patent file is stored
    private EngineParsers parsers;

    private GrobidAnalyzer analyzer = null;
    private LanguageUtilities languageUtilities = LanguageUtilities.getInstance();

    public void setDocumentPath(String dirName) {
        path = dirName;
    }

    public ReferenceExtractor() {
        this(new EngineParsers());
    }

    // constructors
    public ReferenceExtractor(EngineParsers parsers) {
        this.parsers = parsers;
        taggerAll = TaggerFactory.getTagger(GrobidModels.PATENT_CITATION);
        analyzer = GrobidAnalyzer.getInstance();
    }

    /**
     * Extract all reference from the full text retrieve via OPS.
     */
    public String extractAllReferencesOPS(
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        try {
            if (descriptionSegments != null && descriptionSegments.size() > 0) {
                return extractAllReferencesString(
                        descriptionSegments,
                        filterDuplicate,
                        consolidate,
                        includeRawCitations,
                        patents,
                        articles);
            }
        } catch (Exception e) {
            throw new GrobidException(e);
        }
        return null;
    }

    /**
     * Extract all reference from a patent in XML ST.36 like.
     */
    public String extractPatentReferencesXMLFile(
            String pathXML,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents) {
        return extractAllReferencesXMLFile(
                pathXML,
                filterDuplicate,
                consolidate,
                includeRawCitations,
                patents,
                null);
    }

    /**
     * Extract all reference from an XML file in ST.36 or MAREC format.
     */
    public String extractAllReferencesXMLFile(
            String pathXML,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        try {
            if (patents == null) {
                System.out.println("Warning patents List is null!");
            }

            TextSaxParser sax = new TextSaxParser();
            sax.addFilter("description");
            sax.addFilter("p");
            sax.addFilter("heading");
            sax.addFilter("head");
            // get a factory
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setFeature("http://xml.org/sax/features/namespaces", false);
            spf.setFeature("http://xml.org/sax/features/validation", false);
            //get a new instance of parser
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(
                            new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
                }
            });
            reader.setContentHandler(sax);

            InputSource input = null;

            if (pathXML.endsWith(".gz")) {
                InputStream dataInputStream = new FileInputStream(pathXML);
                GZIPInputStream gzip = new GZIPInputStream(dataInputStream);
                DataInputStream tmp = new DataInputStream(gzip);
                dataInputStream = tmp;
                input = new InputSource(dataInputStream);
            } else {
                input = new InputSource(pathXML);
            }
            input.setEncoding("UTF-8");

            reader.parse(input);

            List<String> descriptionSegments = sax.getTexts();

            /*for(String text : descriptionSegments){
                System.out.println(text);
            }*/

            currentPatentNumber = sax.currentPatentNumber;
            consolidate = 0;
            filterDuplicate = true;

            if (descriptionSegments != null && descriptionSegments.size() > 0) {
                return extractAllReferencesString(
                        descriptionSegments,
                        filterDuplicate,
                        consolidate,
                        includeRawCitations,
                        patents,
                        articles);
            } else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Extract all reference from the PDF file of a patent publication.
     */
    public String extractAllReferencesPDFFile(
            String inputFile,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        DocumentSource documentSource = null;
        String result = null;
        try {
            documentSource = DocumentSource.fromPdf(new File(inputFile));
            PatentDocument doc = new PatentDocument(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                return result;
            }
            String description = doc.getAllBlocksClean(25, -1);
            if (description != null) {
                List<String> descriptions = new ArrayList<>();
                descriptions.add(description);
                result = extractAllReferencesString(
                        descriptions,
                        filterDuplicate,
                        consolidate,
                        includeRawCitations,
                        patents,
                        articles);
            }

            return result;

        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * JSON annotations for all reference from the PDF file of a patent publication.
     */
    public String annotateAllReferencesPDFFile(
            String inputFile,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        DocumentSource documentSource = null;
        try {
            documentSource = DocumentSource.fromPdf(new File(inputFile));
            PatentDocument doc = new PatentDocument(documentSource);

            List<LayoutToken> tokenizations = doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new GrobidException("PDF parsing resulted in empty content");
            }
            if ((tokenizations != null) && (tokenizations.size() > 0)) {
                return annotateAllReferences(
                        doc,
                        tokenizations,
                        filterDuplicate,
                        consolidate,
                        includeRawCitations,
                        patents,
                        articles);
            } else {
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error in extractAllReferencesPDFFile", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
        return null;
    }

    /**
     * Extract all reference from a simple piece of text or a list of text segments, and return results in an XML document.
     */
    /*public String extractAllReferencesString(String text,
                                          boolean filterDuplicate,
                                          int consolidate,
                                          boolean includeRawCitations,
                                          List<PatentItem> patents,
                                          List<BibDataSet> articles) {
        List<String> texts = new ArrayList<>();
        texts.add(text);
        return extractAllReferencesString(texts, filterDuplicate, consolidate, includeRawCitations, patents, articles);
    }*/

    public String extractAllReferencesString(
            List<String> texts,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        List<List<LayoutToken>> allTokenizations = new ArrayList<>();

        // list of references by index of tokenized text segments
        Map<Integer, List<PatentItem>> patentsBySegment = new HashMap<>();
        Map<Integer, List<BibDataSet>> articlesBySegment = new HashMap<>();

        // sub-segment texts if a DL model will be applied. Use the max sequence length for size limit
        if (GrobidProperties.getGrobidEngineName("patent-citation").equals("delft")) {
            List<String> newTexts = new ArrayList<>();
            int maxSequence = GrobidProperties.getDelftTrainingMaxSequenceLength("patent-citation");
            for (String text : texts) {
                List<String> tokenizations = GrobidDefaultAnalyzer.getInstance().tokenize(text);
                if (tokenizations.size() > maxSequence) {
                    //System.out.println(maxSequence + " vs " + tokenizations.size());
                    String[] subtexts = text.split("\n\n");
                    for (int i = 0; i < subtexts.length; i++) {
                        List<String> subtokenizations = GrobidDefaultAnalyzer.getInstance().tokenize(subtexts[i]);
                        if (subtokenizations.size() > maxSequence) {
                            String[] subsubtexts = subtexts[i].split(".\n");
                            for (int j = 0; j < subsubtexts.length; j++) {
                                List<String> subsubtokenizations = GrobidDefaultAnalyzer.getInstance()
                                        .tokenize(subsubtexts[j]);
                                newTexts.add(subsubtexts[j]);
                            }
                        } else {
                            newTexts.add(subtexts[i]);
                        }
                    }
                } else {
                    newTexts.add(text);
                }
            }
            texts = newTexts;
        }

        try {
            // if parameters are null, these lists will only be valid in the method
            if (patents == null) {
                patents = new ArrayList<>();
            }

            if (articles == null) {
                articles = new ArrayList<>();
            }

            // parser for patent and non patent references
            if (patentParser == null) {
                patentParser = new PatentRefParser();
            }

            // identify the language of the patent document, we use only the first 500 characters
            // which is enough normally for a very safe language prediction
            // the text here is the patent description, so normally strictly monolingual

            // create text buffer
            StringBuilder localText = new StringBuilder();
            for (String text : texts) {
                localText.append(text);
                if (localText.length() > 500)
                    break;
            }
            if (localText.length() == 0)
                return null;
            Language lang = languageUtilities.runLanguageId(localText.toString(), 500);
            List<String> allPatentBlocks = new ArrayList<>();

            for (String text : texts) {
                //text = TextUtilities.dehyphenize(text); // to be reviewed!

                // tokenisation according to the language (except for Korean, which will require retraining)
                List<LayoutToken> tokenizations;
                if (lang.getLang().equals(Language.KO)) {
                    tokenizations = GrobidDefaultAnalyzer.getInstance().tokenizeWithLayoutToken(text);
                } else {
                    tokenizations = analyzer.tokenizeWithLayoutToken(text, lang);
                    // to be sure to sub-tokenize based on standard punctuations:
                    tokenizations = GrobidDefaultAnalyzer.getInstance().retokenizeFromLayoutToken(tokenizations);
                }

                if (tokenizations.size() == 0) {
                    continue;
                }
                allTokenizations.add(tokenizations);

                StringBuilder patentBlocks = new StringBuilder();

                List<OffsetPosition> journalPositions = null;
                List<OffsetPosition> abbrevJournalPositions = null;
                List<OffsetPosition> conferencePositions = null;
                List<OffsetPosition> publisherPositions = null;

                journalPositions = lexicon.tokenPositionsJournalNames(tokenizations);
                abbrevJournalPositions = lexicon.tokenPositionsAbbrevJournalNames(tokenizations);
                conferencePositions = lexicon.tokenPositionsConferenceNames(tokenizations);
                publisherPositions = lexicon.tokenPositionsPublisherNames(tokenizations);

                boolean isJournalToken = false;
                boolean isAbbrevJournalToken = false;
                boolean isConferenceToken = false;
                boolean isPublisherToken = false;
                int currentJournalPositions = 0;
                int currentAbbrevJournalPositions = 0;
                int currentConferencePositions = 0;
                int currentPublisherPositions = 0;
                boolean skipTest = false;
                int posit = 0;
                for (LayoutToken token : tokenizations) {
                    String tok = token.getText();
                    isJournalToken = false;
                    isAbbrevJournalToken = false;
                    isConferenceToken = false;
                    isPublisherToken = false;
                    skipTest = false;
                    if ((tok.trim().length() == 0) ||
                            (tok.equals(" ")) ||
                            (tok.equals("\t")) ||
                            (tok.equals("\n")) ||
                            (tok.equals("\r"))) {
                        posit++;
                        continue;
                    }

                    // check the position of matches for journals
                    if (journalPositions != null) {
                        if (currentJournalPositions == journalPositions.size() - 1) {
                            if (journalPositions.get(currentJournalPositions).end < posit) {
                                skipTest = true;
                            }
                        }
                        if (!skipTest) {
                            for (int i = currentJournalPositions; i < journalPositions.size(); i++) {
                                if ((journalPositions.get(i).start <= posit) &&
                                        (journalPositions.get(i).end >= posit)) {
                                    isJournalToken = true;
                                    currentJournalPositions = i;
                                    break;
                                } else if (journalPositions.get(i).start > posit) {
                                    isJournalToken = false;
                                    currentJournalPositions = i;
                                    break;
                                }
                            }
                        }
                    }

                    // check the position of matches for abbreviated journals
                    skipTest = false;
                    if (abbrevJournalPositions != null) {
                        if (currentAbbrevJournalPositions == abbrevJournalPositions.size() - 1) {
                            if (abbrevJournalPositions.get(currentAbbrevJournalPositions).end < posit) {
                                skipTest = true;
                            }
                        }
                        if (!skipTest) {
                            for (int i = currentAbbrevJournalPositions; i < abbrevJournalPositions.size(); i++) {
                                if ((abbrevJournalPositions.get(i).start <= posit) &&
                                        (abbrevJournalPositions.get(i).end >= posit)) {
                                    isAbbrevJournalToken = true;
                                    currentAbbrevJournalPositions = i;
                                    break;
                                } else if (abbrevJournalPositions.get(i).start > posit) {
                                    isAbbrevJournalToken = false;
                                    currentAbbrevJournalPositions = i;
                                    break;
                                }
                            }
                        }
                    }

                    // check the position of matches for conference names
                    skipTest = false;
                    if (conferencePositions != null) {
                        if (currentConferencePositions == conferencePositions.size() - 1) {
                            if (conferencePositions.get(currentConferencePositions).end < posit) {
                                skipTest = true;
                            }
                        }
                        if (!skipTest) {
                            for (int i = currentConferencePositions; i < conferencePositions.size(); i++) {
                                if ((conferencePositions.get(i).start <= posit) &&
                                        (conferencePositions.get(i).end >= posit)) {
                                    isConferenceToken = true;
                                    currentConferencePositions = i;
                                    break;
                                } else if (conferencePositions.get(i).start > posit) {
                                    isConferenceToken = false;
                                    currentConferencePositions = i;
                                    break;
                                }
                            }
                        }
                    }

                    // check the position of matches for publisher names
                    skipTest = false;
                    if (publisherPositions != null) {
                        if (currentPublisherPositions == publisherPositions.size() - 1) {
                            if (publisherPositions.get(currentPublisherPositions).end < posit) {
                                skipTest = true;
                            }
                        }
                        if (!skipTest) {
                            for (int i = currentPublisherPositions; i < publisherPositions.size(); i++) {
                                if ((publisherPositions.get(i).start <= posit) &&
                                        (publisherPositions.get(i).end >= posit)) {
                                    isPublisherToken = true;
                                    currentPublisherPositions = i;
                                    break;
                                } else if (publisherPositions.get(i).start > posit) {
                                    isPublisherToken = false;
                                    currentPublisherPositions = i;
                                    break;
                                }
                            }
                        }
                    }

                    FeaturesVectorReference featureVector = FeaturesVectorReference.addFeaturesPatentReferences(
                            new LayoutToken(tok),
                            null,
                            tokenizations.size(),
                            posit,
                            isJournalToken,
                            isAbbrevJournalToken,
                            isConferenceToken,
                            isPublisherToken);
                    patentBlocks.append(featureVector.printVector());
                    patentBlocks.append("\n");
                    posit++;
                }

                patentBlocks.append("\n\n");
                allPatentBlocks.add(patentBlocks.toString());
            }

            String theResults = taggerAll.label(allPatentBlocks);
            //System.out.println(theResults);
            String[] theSegmentedResults = theResults.split("\n\n");

            List<String> allReferencesPatent = new ArrayList<>();
            List<String> allReferencesNPL = new ArrayList<>();
            List<Integer> allOffsetsNPL = new ArrayList<>();
            List<Double> allProbNPL = new ArrayList<>();
            List<Integer> localIndexSegmentNPL = new ArrayList<>();

            //int offset = 0;
            for (int index = 0; index < theSegmentedResults.length; index++) {
                String theResult = theSegmentedResults[index];
                StringTokenizer stt = new StringTokenizer(theResult, "\n");
                List<LayoutToken> tokenizations = allTokenizations.get(index);

                List<String> referencesPatent = new ArrayList<>();
                List<String> referencesNPL = new ArrayList<>();

                List<Integer> offsetsPatent = new ArrayList<>();
                List<Integer> offsetsNPL = new ArrayList<>();

                List<Double> probPatent = new ArrayList<>();
                List<Double> probNPL = new ArrayList<>();

                boolean currentPatent = true; // type of current reference
                String reference = null;
                double currentProb = 0.0;
                int offset = 0;
                int currentOffset = 0;
                int addedOffset = 0;
                String label = null; // label
                String actual = null; // token
                int p = 0; // iterator for the tokenizations for restauring the original tokenization with
                // respect to spaces

                while (stt.hasMoreTokens()) {
                    String line = stt.nextToken();
                    if (line.trim().length() == 0) {
                        continue;
                    }

                    StringTokenizer st2 = new StringTokenizer(line, "\t ");
                    boolean start = true;
                    String separator = "";
                    label = null;
                    actual = null;
                    while (st2.hasMoreTokens()) {
                        if (start) {
                            actual = st2.nextToken().trim();
                            start = false;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).getText();
                                addedOffset += tokOriginal.length();
                                if (tokOriginal.equals(" ")) {
                                    separator += tokOriginal;
                                } else if (tokOriginal.equals(actual)) {
                                    strop = true;
                                }
                                p++;
                            }
                        } else {
                            label = st2.nextToken().trim();
                        }
                    }

                    if (label == null) {
                        offset += addedOffset;
                        addedOffset = 0;
                        continue;
                    }

                    double prob = 0.0;
                    int segProb = label.lastIndexOf("/");
                    if (segProb != -1) {
                        String probString = label.substring(segProb + 1, label.length());
                        //System.out.println("given prob: " + probString);
                        try {
                            prob = Double.parseDouble(probString);
                            //System.out.println("given prob: " + probString + ", parsed: " + prob);
                        } catch (Exception e) {
                            LOGGER.debug(probString + " cannot be parsed.");
                        }
                        label = label.substring(0, segProb);
                    }

                    // TBD: use TaggingTokenClusteror and TaggingLabel as for the other parsers
                    if (actual != null) {
                        if (label.endsWith("<refPatent>")) {
                            if (reference == null) {
                                reference = separator + actual;
                                currentOffset = offset;
                                currentPatent = true;
                                currentProb = prob;
                            } else {
                                if (currentPatent) {
                                    if (label.equals("I-<refPatent>")) {
                                        referencesPatent.add(reference);
                                        offsetsPatent.add(currentOffset);

                                        probPatent.add(Double.valueOf(currentProb));

                                        currentPatent = true;
                                        reference = separator + actual;
                                        currentOffset = offset;
                                        currentProb = prob;
                                    } else {
                                        reference += separator + actual;
                                        if (prob > currentProb) {
                                            currentProb = prob;
                                        }
                                    }
                                } else {
                                    referencesNPL.add(reference);
                                    offsetsNPL.add(currentOffset);
                                    probNPL.add(Double.valueOf(currentProb));

                                    currentPatent = true;
                                    reference = separator + actual;
                                    currentOffset = offset;
                                    currentProb = prob;
                                }
                            }
                        } else if (label.endsWith("<refNPL>")) {
                            if (reference == null) {
                                reference = separator + actual;
                                currentOffset = offset;
                                currentPatent = false;
                                currentProb = prob;
                            } else {
                                if (currentPatent) {
                                    referencesPatent.add(reference);
                                    offsetsPatent.add(currentOffset);
                                    probPatent.add(Double.valueOf(currentProb));

                                    currentPatent = false;
                                    reference = separator + actual;
                                    currentOffset = offset;
                                    currentProb = prob;
                                } else {
                                    if (label.equals("I-<refNPL>")) {
                                        referencesNPL.add(reference);
                                        offsetsNPL.add(currentOffset);
                                        probNPL.add(Double.valueOf(currentProb));

                                        currentPatent = false;
                                        reference = separator + actual;
                                        currentOffset = offset;
                                        currentProb = prob;
                                    } else {
                                        reference += separator + actual;
                                        if (prob > currentProb) {
                                            currentProb = prob;
                                        }
                                    }
                                }
                            }
                        } else if (label.equals("<other>")) {
                            if (reference != null) {
                                if (currentPatent) {
                                    referencesPatent.add(reference);
                                    offsetsPatent.add(currentOffset);
                                    probPatent.add(Double.valueOf(currentProb));
                                } else {
                                    referencesNPL.add(reference);
                                    offsetsNPL.add(currentOffset);
                                    probNPL.add(Double.valueOf(currentProb));
                                }
                                currentPatent = false;
                            }
                            reference = null;
                            currentProb = 0.0;
                        }
                    }
                    offset += addedOffset;
                    addedOffset = 0;
                }

                // run reference patent parser in isolation, and produce some traces
                int j = 0;
                for (String ref : referencesPatent) {
                    patentParser.setRawRefText(ref);
                    patentParser.setRawRefTextOffset(offsetsPatent.get(j).intValue());
                    List<PatentItem> patents0 = patentParser.processRawRefText();
                    for (PatentItem pat : patents0) {
                        pat.setContext(ref);
                        pat.setConf(probPatent.get(j).doubleValue());
                        patents.add(pat);
                        //allIndexSegmentPatent.add(index);

                        List<PatentItem> localList = patentsBySegment.get(index);
                        if (localList == null) {
                            localList = new ArrayList<>();
                        }
                        localList.add(pat);
                        patentsBySegment.put(index, localList);

                        /*if (pat.getApplication()) {
                            if (pat.getProvisional()) {
                                if (debug) {
                                    System.out.println(pat.getAuthority() + " " + pat.getNumber()
                                            + " P application " + pat.getOffsetBegin()
                                            + ":" + pat.getOffsetEnd() + "\n");
                                }
                            } else {
                                if (debug) {
                                    System.out.println(pat.getAuthority() + " " + pat.getNumber()
                                            + " application " + pat.getOffsetBegin()
                                            + ":" + pat.getOffsetEnd() + "\n");
                                }
                            }
                        } else if (pat.getReissued()) {
                            if (pat.getAuthority().equals("US")) {
                                if (debug) {
                                    System.out.println(pat.getAuthority() + "RE" + pat.getNumber() + " E "
                                            + pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                                }
                            }
                        } else if (pat.getPlant()) {
                            if (pat.getAuthority().equals("US")) {
                                if (debug)
                                    System.out.println(pat.getAuthority() + "PP" + pat.getNumber() + " " +
                                            pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                            }
                        } else {
                            if (debug) {
                                if (pat.getKindCode() != null) {
                                    System.out.println(pat.getAuthority() + " " + pat.getNumber() + " "
                                            + pat.getKindCode() + " "
                                            + pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                                } else {
                                    System.out.println(pat.getAuthority() + " " + pat.getNumber() + " " +
                                            pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                                }
                                System.out.println(pat.getContext());
                            }
                        }*/
                    }
                    j++;
                }

                if (referencesNPL.size() > 0) {
                    allReferencesNPL.addAll(referencesNPL);
                    allOffsetsNPL.addAll(offsetsNPL);
                    allProbNPL.addAll(probNPL);
                    for (String ref : referencesNPL) {
                        localIndexSegmentNPL.add(index);
                    }
                }
            }

            // list for filtering duplicates, if we want to ignore the duplicate numbers
            /*List<String> numberListe = new ArrayList<String>();
            if (filterDuplicate) {
                // list for filtering duplicates, if we want to ignore the duplicate numbers
                List<PatentItem> toRemove = new ArrayList<PatentItem>();
                for (PatentItem pat : patents) {
                    if (!numberListe.contains(pat.getNumberEpoDoc())) {
                        numberListe.add(pat.getNumberEpoDoc());
                    } else {
                        toRemove.add(pat);
                    }
                }

                for (PatentItem pat : toRemove) {
                    patents.remove(pat);
                }
            }*/

            if (articles != null && allReferencesNPL != null && allReferencesNPL.size() > 0) {
                int k = 0;
                List<BiblioItem> bibResults = parsers.getCitationParser()
                        .processingStringMultiple(allReferencesNPL, consolidate);
                for (String ref : allReferencesNPL) {
                    BiblioItem result = bibResults.get(k);
                    if (result == null) {
                        k++;
                        continue;
                    }
                    BibDataSet bds = new BibDataSet();
                    result.setReference(ref);
                    bds.setResBib(result);
                    bds.setRawBib(ref);
                    bds.addOffset(allOffsetsNPL.get(k).intValue());
                    bds.setConfidence(allProbNPL.get(k).doubleValue());
                    articles.add(bds);
                    //allIndexSegmentNPL.add(localIndexSegmentNPL.get(k));

                    List<BibDataSet> localList = articlesBySegment.get(localIndexSegmentNPL.get(k));
                    if (localList == null) {
                        localList = new ArrayList<>();
                    }
                    localList.add(bds);
                    articlesBySegment.put(localIndexSegmentNPL.get(k), localList);

                    k++;
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }

        StringBuilder resultTEI = new StringBuilder();
        resultTEI.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        +
                        "<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" "
                        +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n");

        resultTEI.append("\t<teiHeader/>\n");
        resultTEI.append("\t<text>\n");

        int index = 0; // index in tokenized text segment list
        int positionInIndexPatent = 0;
        int positionInIndexNPL = 0;

        for (List<LayoutToken> tokens : allTokenizations) {
            // do we have a reference in this text segment ?
            List<PatentItem> localPatentsBySegment = patentsBySegment.get(index);
            List<BibDataSet> localArticlesBySegment = articlesBySegment.get(index);

            if ((localPatentsBySegment != null && localPatentsBySegment.size() > 0) ||
                    (localArticlesBySegment != null && localArticlesBySegment.size() > 0)) {
                // output text
                String divID = KeyGen.getKey().substring(0, 7);
                resultTEI.append("\t\t<div>\n");
                resultTEI.append("\t\t\t<p id=\"_" + divID + "\">");
                String text = LayoutTokensUtil.toText(tokens);
                // not affecting offsets:
                text = text.replace("\n", " ").replace("\t", " ");
                resultTEI.append(TextUtilities.HTMLEncode(text));
                resultTEI.append("</p>\n");
                resultTEI.append("\t\t\t<div type=\"references\">\n");
                if (localPatentsBySegment != null && localPatentsBySegment.size() > 0) {
                    for (PatentItem patentCitation : localPatentsBySegment) {
                        resultTEI.append(patentCitation.toTEI(true, divID) + "\n"); // true here means with offsets
                    }
                }
                if (localArticlesBySegment != null && localArticlesBySegment.size() > 0) {
                    for (BibDataSet articleCitation : localArticlesBySegment) {
                        resultTEI.append(articleCitation.toTEI(includeRawCitations) + "\n");
                    }
                }
                resultTEI.append("\t\t\t</div>\n");
                resultTEI.append("\t\t</div>\n");
            }

            index++;
        }

        resultTEI.append("\t</text>\n");
        resultTEI.append("</TEI>");

        return resultTEI.toString();
    }

    /**
     * Annotate all reference from a list of layout tokens and return annotation results in a JSON document.
     */
    public String annotateAllReferences(
            Document doc,
            List<LayoutToken> tokenizations,
            boolean filterDuplicate,
            int consolidate,
            boolean includeRawCitations,
            List<PatentItem> patents,
            List<BibDataSet> articles) {
        try {
            if (tokenizations.size() == 0) {
                return null;
            }

            // if parameters are null, these lists will only be valid in the method
            if (patents == null) {
                patents = new ArrayList<PatentItem>();
            }

            if (articles == null) {
                articles = new ArrayList<BibDataSet>();
            }

            // parser for patent references
            if (patentParser == null) {
                patentParser = new PatentRefParser();
            }
            // parser for non patent references

            // tokenisation for the CRF parser (with punctuation as tokens)
            ArrayList<String> patentBlocks = new ArrayList<String>();

            // identify the language of the patent document, we use only the last 500 characters
            // which is enough normally for a very safe language prediction
            // the text here is the patent description, so strictly monolingual
            StringBuilder textBuffer = new StringBuilder();
            int accumulated = 0;
            for (int n = tokenizations.size() - 1; n > 0; n--) {
                LayoutToken token = tokenizations.get(n);
                if ((token != null) && (token.getText() != null)) {
                    textBuffer.insert(0, token.getText());
                    accumulated += token.getText().length();
                }
                if (accumulated > 500)
                    break;
            }
            String text = textBuffer.toString();
            text = text.replace("\n", " ").replace("\t", " ");
            Language lang = languageUtilities.runLanguageId(text);
            //List<String> tokenizations = analyzer.tokenize(lang, text);
            int offset = 0;

            List<OffsetPosition> journalPositions = null;
            List<OffsetPosition> abbrevJournalPositions = null;
            List<OffsetPosition> conferencePositions = null;
            List<OffsetPosition> publisherPositions = null;

            //if (articles != null)
            {
                journalPositions = lexicon.tokenPositionsJournalNames(text);
                abbrevJournalPositions = lexicon.tokenPositionsAbbrevJournalNames(text);
                conferencePositions = lexicon.tokenPositionsConferenceNames(text);
                publisherPositions = lexicon.tokenPositionsPublisherNames(text);
            }

            boolean isJournalToken = false;
            boolean isAbbrevJournalToken = false;
            boolean isConferenceToken = false;
            boolean isPublisherToken = false;
            int currentJournalPositions = 0;
            int currentAbbrevJournalPositions = 0;
            int currentConferencePositions = 0;
            int currentPublisherPositions = 0;
            boolean skipTest = false;
            //st = new StringTokenizer(text, " (["+ TextUtilities.punctuations, true);
            //st = new StringTokenizer(text, delimiters, true);
            int posit = 0;
            //while (st.hasMoreTokens()) {
            for (LayoutToken token : tokenizations) {
                String tok = token.getText();
                isJournalToken = false;
                isAbbrevJournalToken = false;
                isConferenceToken = false;
                isPublisherToken = false;
                skipTest = false;
                //String tok = st.nextToken();
                if ((tok.trim().length() == 0) ||
                        (tok.equals(" ")) ||
                        (tok.equals("\t")) ||
                        (tok.equals("\n")) ||
                        (tok.equals("\r"))) {
                    posit++;
                    continue;
                }

                // check the position of matches for journals
                if (journalPositions != null) {
                    if (currentJournalPositions == journalPositions.size() - 1) {
                        if (journalPositions.get(currentJournalPositions).end < posit) {
                            skipTest = true;
                        }
                    }
                    if (!skipTest) {
                        for (int i = currentJournalPositions; i < journalPositions.size(); i++) {
                            if ((journalPositions.get(i).start <= posit) &&
                                    (journalPositions.get(i).end >= posit)) {
                                isJournalToken = true;
                                currentJournalPositions = i;
                                break;
                            } else if (journalPositions.get(i).start > posit) {
                                isJournalToken = false;
                                currentJournalPositions = i;
                                break;
                            }
                        }
                    }
                }

                // check the position of matches for abbreviated journals
                skipTest = false;
                if (abbrevJournalPositions != null) {
                    if (currentAbbrevJournalPositions == abbrevJournalPositions.size() - 1) {
                        if (abbrevJournalPositions.get(currentAbbrevJournalPositions).end < posit) {
                            skipTest = true;
                        }
                    }
                    if (!skipTest) {
                        for (int i = currentAbbrevJournalPositions; i < abbrevJournalPositions.size(); i++) {
                            if ((abbrevJournalPositions.get(i).start <= posit) &&
                                    (abbrevJournalPositions.get(i).end >= posit)) {
                                isAbbrevJournalToken = true;
                                currentAbbrevJournalPositions = i;
                                break;
                            } else if (abbrevJournalPositions.get(i).start > posit) {
                                isAbbrevJournalToken = false;
                                currentAbbrevJournalPositions = i;
                                break;
                            }
                        }
                    }
                }

                // check the position of matches for conference names
                skipTest = false;
                if (conferencePositions != null) {
                    if (currentConferencePositions == conferencePositions.size() - 1) {
                        if (conferencePositions.get(currentConferencePositions).end < posit) {
                            skipTest = true;
                        }
                    }
                    if (!skipTest) {
                        for (int i = currentConferencePositions; i < conferencePositions.size(); i++) {
                            if ((conferencePositions.get(i).start <= posit) &&
                                    (conferencePositions.get(i).end >= posit)) {
                                isConferenceToken = true;
                                currentConferencePositions = i;
                                break;
                            } else if (conferencePositions.get(i).start > posit) {
                                isConferenceToken = false;
                                currentConferencePositions = i;
                                break;
                            }
                        }
                    }
                }

                // check the position of matches for publisher names
                skipTest = false;
                if (publisherPositions != null) {
                    if (currentPublisherPositions == publisherPositions.size() - 1) {
                        if (publisherPositions.get(currentPublisherPositions).end < posit) {
                            skipTest = true;
                        }
                    }
                    if (!skipTest) {
                        for (int i = currentPublisherPositions; i < publisherPositions.size(); i++) {
                            if ((publisherPositions.get(i).start <= posit) &&
                                    (publisherPositions.get(i).end >= posit)) {
                                isPublisherToken = true;
                                currentPublisherPositions = i;
                                break;
                            } else if (publisherPositions.get(i).start > posit) {
                                isPublisherToken = false;
                                currentPublisherPositions = i;
                                break;
                            }
                        }
                    }
                }

                FeaturesVectorReference featureVector = FeaturesVectorReference.addFeaturesPatentReferences(
                        new LayoutToken(tok),
                        null,
                        tokenizations.size(),
                        posit,
                        isJournalToken,
                        isAbbrevJournalToken,
                        isConferenceToken,
                        isPublisherToken);
                patentBlocks.add(featureVector.printVector());
                posit++;
            }

            patentBlocks.add("\n");

            String theResult = null;
            theResult = taggerAll.label(patentBlocks);
            //System.out.println(theResult);

            StringTokenizer stt = new StringTokenizer(theResult, "\n");

            List<String> referencesPatent = new ArrayList<String>();
            List<String> referencesNPL = new ArrayList<String>();
            List<Integer> offsets_patent = new ArrayList<Integer>();
            List<Integer> offsets_NPL = new ArrayList<Integer>();
            List<Double> probPatent = new ArrayList<Double>();
            List<Double> probNPL = new ArrayList<Double>();

            boolean currentPatent = true; // type of current reference
            String reference = null;
            double currentProb = 0.0;
            offset = 0;
            int currentOffset = 0;
            int addedOffset = 0;
            String label = null; // label
            String actual = null; // token
            int p = 0; // iterator for the tokenizations for restauring the original tokenization with
            // respect to spaces

            while (stt.hasMoreTokens()) {
                String line = stt.nextToken();
                if (line.trim().length() == 0) {
                    continue;
                }

                StringTokenizer st2 = new StringTokenizer(line, "\t ");
                boolean start = true;
                String separator = "";
                label = null;
                actual = null;
                while (st2.hasMoreTokens()) {
                    if (start) {
                        actual = st2.nextToken().trim();
                        start = false;

                        boolean strop = false;
                        while ((!strop) && (p < tokenizations.size())) {
                            LayoutToken tokenOriginal = tokenizations.get(p);
                            if ((tokenOriginal == null) || (tokenOriginal.getText() == null))
                                continue;
                            String tokOriginal = tokenOriginal.getText();

                            addedOffset += tokOriginal.length();
                            if (tokOriginal.equals(" ")) {
                                separator += tokOriginal;
                            } else if (tokOriginal.equals(actual)) {
                                strop = true;
                            }
                            p++;
                        }
                    } else {
                        label = st2.nextToken().trim();
                    }
                }

                if (label == null) {
                    offset += addedOffset;
                    addedOffset = 0;
                    continue;
                }

                double prob = 0.0;
                int segProb = label.lastIndexOf("/");
                if (segProb != -1) {
                    String probString = label.substring(segProb + 1, label.length());
                    //System.out.println("given prob: " + probString);
                    try {
                        prob = Double.parseDouble(probString);
                        //System.out.println("given prob: " + probString + ", parsed: " + prob);
                    } catch (Exception e) {
                        LOGGER.debug(probString + " cannot be parsed.");
                    }
                    label = label.substring(0, segProb);
                }

                if (actual != null) {
                    if (label.endsWith("<refPatent>")) {
                        if (reference == null) {
                            reference = separator + actual;
                            currentOffset = offset;
                            currentPatent = true;
                            currentProb = prob;
                        } else {
                            if (currentPatent) {
                                if (label.equals("I-<refPatent>")) {
                                    referencesPatent.add(reference);
                                    offsets_patent.add(currentOffset);

                                    probPatent.add(Double.valueOf(currentProb));

                                    currentPatent = true;
                                    reference = separator + actual;
                                    currentOffset = offset;
                                    currentProb = prob;
                                } else {
                                    reference += separator + actual;
                                    if (prob > currentProb) {
                                        currentProb = prob;
                                    }
                                }
                            } else {
                                referencesNPL.add(reference);
                                offsets_NPL.add(currentOffset);
                                probNPL.add(Double.valueOf(currentProb));

                                currentPatent = true;
                                reference = separator + actual;
                                currentOffset = offset;
                                currentProb = prob;
                            }
                        }
                    } else if (label.endsWith("<refNPL>")) {
                        if (reference == null) {
                            reference = separator + actual;
                            currentOffset = offset;
                            currentPatent = false;
                            currentProb = prob;
                        } else {
                            if (currentPatent) {
                                referencesPatent.add(reference);
                                offsets_patent.add(currentOffset);
                                probPatent.add(Double.valueOf(currentProb));

                                currentPatent = false;
                                reference = separator + actual;
                                currentOffset = offset;
                                currentProb = prob;
                            } else {
                                if (label.equals("I-<refNPL>")) {
                                    referencesNPL.add(reference);
                                    offsets_NPL.add(currentOffset);
                                    probNPL.add(Double.valueOf(currentProb));

                                    currentPatent = false;
                                    reference = separator + actual;
                                    currentOffset = offset;
                                    currentProb = prob;
                                } else {
                                    reference += separator + actual;
                                    if (prob > currentProb) {
                                        currentProb = prob;
                                    }
                                }
                            }
                        }
                    } else if (label.equals("<other>")) {
                        if (reference != null) {
                            if (currentPatent) {
                                referencesPatent.add(reference);
                                offsets_patent.add(currentOffset);
                                probPatent.add(Double.valueOf(currentProb));
                            } else {
                                referencesNPL.add(reference);
                                offsets_NPL.add(currentOffset);
                                probNPL.add(Double.valueOf(currentProb));
                            }
                            currentPatent = false;
                        }
                        reference = null;
                        currentProb = 0.0;
                    }
                }
                offset += addedOffset;
                addedOffset = 0;
            }

            // run reference patent parser in isolation, and produce some traces
            int j = 0;
            for (String ref : referencesPatent) {
                patentParser.setRawRefText(ref);
                patentParser.setRawRefTextOffset(offsets_patent.get(j).intValue());
                List<PatentItem> patents0 = patentParser.processRawRefText();
                for (PatentItem pat : patents0) {
                    pat.setContext(ref);
                    pat.setConf(probPatent.get(j).doubleValue());
                    patents.add(pat);

                    // get the list of LayoutToken corresponding to the offset positions
                    List<LayoutToken> localTokens = Document.getTokens(
                            tokenizations,
                            pat.getOffsetBegin(),
                            pat.getOffsetEnd());
                    // associate the corresponding bounding box
                    if ((localTokens != null) && (localTokens.size() > 0))
                        pat.setCoordinates(BoundingBoxCalculator.calculate(localTokens));

                    /*if (pat.getApplication()) {
                        if (pat.getProvisional()) {
                            if (debug) {
                                System.out.println(pat.getAuthority() + " " + pat.getNumber()
                                        + " P application " + pat.getOffsetBegin()
                                        + ":" + pat.getOffsetEnd() + "\n");
                            }
                        } else {
                            if (debug) {
                                System.out.println(pat.getAuthority() + " " + pat.getNumber()
                                        + " application " + pat.getOffsetBegin()
                                        + ":" + pat.getOffsetEnd() + "\n");
                            }
                        }
                    } else if (pat.getReissued()) {
                        if (pat.getAuthority().equals("US")) {
                            if (debug) {
                                System.out.println(pat.getAuthority() + "RE" + pat.getNumber() + " E "
                                        + pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                            }
                        }
                    } else if (pat.getPlant()) {
                        if (pat.getAuthority().equals("US")) {
                            if (debug)
                                System.out.println(pat.getAuthority() + "PP" + pat.getNumber() + " " +
                                        pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                        }
                    } else {
                        if (debug) {
                            if (pat.getKindCode() != null) {
                                System.out.println(pat.getAuthority() + " " + pat.getNumber() + " "
                                        + pat.getKindCode() + " "
                                        + pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                            } else {
                                System.out.println(pat.getAuthority() + " " + pat.getNumber() + " " +
                                        pat.getOffsetBegin() + ":" + pat.getOffsetEnd() + "\n");
                            }
                            System.out.println(pat.getContext());
                        }
                    }*/
                }
                j++;
            }

            // list for filtering duplicates, if we want to ignore the duplicate numbers
            List<String> numberListe = new ArrayList<String>();
            if (filterDuplicate) {
                // list for filtering duplicates, if we want to ignore the duplicate numbers
                List<PatentItem> toRemove = new ArrayList<PatentItem>();
                for (PatentItem pat : patents) {
                    if (!numberListe.contains(pat.getNumberEpoDoc())) {
                        numberListe.add(pat.getNumberEpoDoc());
                    } else {
                        toRemove.add(pat);
                    }
                }

                for (PatentItem pat : toRemove) {
                    patents.remove(pat);
                }
            }

            if (articles != null) {
                int k = 0;
                List<BiblioItem> bibResults = parsers.getCitationParser()
                        .processingStringMultiple(referencesNPL, consolidate);
                for (String ref : referencesNPL) {
                    BiblioItem result = bibResults.get(k);
                    if (result == null) {
                        k++;
                        continue;
                    }
                    BibDataSet bds = new BibDataSet();
                    result.setReference(ref);
                    bds.setResBib(result);
                    bds.setRawBib(ref);
                    bds.addOffset(offsets_NPL.get(k).intValue());
                    //bds.setConfidence(probNPL.get(k).doubleValue());
                    articles.add(bds);
                    k++;
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        int nbs = 0;
        if (patents != null) {
            nbs = patents.size();
        }
        if (articles != null)
            nbs += articles.size();

        StringBuilder resultJson = new StringBuilder();
        resultJson.append("{");

        // page height and width
        List<Page> pages = doc.getPages();
        int pageNumber = 1;
        resultJson.append("\"pages\": [");
        for (Page page : pages) {
            if (pageNumber > 1)
                resultJson.append(", ");

            resultJson.append("{\"page_height\":" + page.getHeight());
            resultJson.append(", \"page_width\":" + page.getWidth() + "}");
            pageNumber++;
        }
        resultJson.append("]");

        if (patents != null) {
            resultJson.append(", \"patents\": [");
            boolean first = true;
            for (PatentItem patentCitation : patents) {
                if (first)
                    first = false;
                else
                    resultJson.append(", ");
                resultJson.append(patentCitation.toJson(null, true)); // with coordinates
            }
            resultJson.append("]");
        }

        if (articles != null) {
            resultJson.append(", \"articles\": [");
            boolean first = true;
            for (BibDataSet articleCitation : articles) {
                /*if (first)
                	first = false;
                else
                	resultJson.append(", ");
                resultJSON.append(articleCitation.toJson(); */
            }
            resultJson.append("]");
        }
        resultJson.append("}");

        return resultJson.toString();
    }

    /**
     * Get the TEI XML string corresponding to the recognized citation section for
     * a particular citation
     */
    public String reference2TEI(int i) {
        String result = "";

        if (resBib != null) {
            if (i <= resBib.size()) {
                BibDataSet bib = resBib.get(i);
                BiblioItem bit = bib.getResBib();
                if (path != null) {
                    bit.setPath(path);
                }
                result += bit.toTEI(i);
            }
        }

        return result;
    }

    /**
     * Get the BibTeX string corresponding to the recognized citation section
     */
    public String references2BibTeX() {
        String result = "";

        for (BibDataSet bib : resBib) {
            BiblioItem bit = bib.getResBib();
            if (path != null) {
                bit.setPath(path);
            }
            result += "\n" + bit.toBibTeX();
        }

        return result;
    }

    /**
     * Get the TEI XML string corresponding to the recognized citation section,
     * with pointers and advanced structuring
     */
    public String references2TEI() {
        String result = "<listbibl>\n";

        int p = 0;
        for (BibDataSet bib : resBib) {
            BiblioItem bit = bib.getResBib();
            if (path == null) {
                bit.setPath(path);
            }
            result += "\n" + bit.toTEI(p);
            p++;
        }
        result += "\n</listbibl>\n";
        return result;
    }

    /**
     * Get the BibTeX string corresponding to the recognized citation section
     * for a given citation
     */
    public String reference2BibTeX(int i) {
        String result = "";

        if (resBib != null) {
            if (i <= resBib.size()) {
                BibDataSet bib = resBib.get(i);
                BiblioItem bit = bib.getResBib();
                if (path == null) {
                    bit.setPath(path);
                }
                result += bit.toBibTeX();
            }
        }
        return result;
    }

    /**
     * Annotate XML files with extracted reference results. Not used.
     */
    private void annotate(
            File file,
            ArrayList<PatentItem> patents,
            ArrayList<BibDataSet> articles) {
        try {
            // we simply rewrite lines based on identified reference strings without parsing
            // special care for line breaks in the middle of a reference
            ArrayList<String> sources = new ArrayList<String>();
            ArrayList<String> targets = new ArrayList<String>();
            for (PatentItem pi : patents) {
                String context = pi.getContext();
                String source = context;
                sources.add(source);

                String target = " <patcit>" + context + "</patcit> ";
                targets.add(target);
                System.out.println(source + " -> " + target);
            }

            for (BibDataSet bi : articles) {
                String context = bi.getRawBib();
                // we compile the corresponding regular expression
                String source = context; //.replace(" ", "( |\\n)");
                sources.add(source);

                String target = " <nplcit>" + context + "</nplcit> ";
                targets.add(target);
                System.out.println(source + " -> " + target);
            }

            FileInputStream fileIn = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fileIn, "UTF-8");
            BufferedReader bufReader = new BufferedReader(reader);
            String line;
            StringBuffer content = new StringBuffer();
            content.append("");
            while ((line = bufReader.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }
            bufReader.close();
            reader.close();
            int i = 0;
            String contentString = content.toString();
            for (String source : sources) {
                String target = targets.get(i);
                contentString = contentString.replace(source, target);
                i++;
            }
            System.out.println(contentString);

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Annotate a new XML patent document based on training data format with the current model.
     *
     * @param documentPath    is the path to the file to be processed
     * @param newTrainingPath new training path
     */
    public void generateTrainingData(String documentPath, String newTrainingPath) {
        if (documentPath == null) {
            throw new GrobidResourceException("Cannot process the patent file, because the document path is null.");
        }
        if (!documentPath.endsWith(".xml") && !documentPath.endsWith(".xml.gz")) {
            throw new GrobidResourceException("Only patent XML files (ST.36 or Marec) can be processed to "
                    +
                    "generate training data.");
        }

        File documentFile = new File(documentPath);
        if (!documentFile.exists()) {
            throw new GrobidResourceException("Cannot process the patent file, because path '"
                    +
                    documentFile.getAbsolutePath()
                    + "' does not exists.");
        }

        if (newTrainingPath == null) {
            GrobidProperties.getInstance();
            newTrainingPath = GrobidProperties.getTempPath().getAbsolutePath();
        }

        File newTrainingFile = new File(newTrainingPath);
        if (!newTrainingFile.exists()) {
            throw new GrobidResourceException("Cannot process the patent file, because path '"
                    +
                    newTrainingFile.getAbsolutePath()
                    + "' does not exists.");
        }

        try {
            // first pass: we get the text to be processed
            TextSaxParser sax = new TextSaxParser();
            sax.addFilter("description");
            sax.addFilter("p");
            sax.addFilter("heading");
            sax.addFilter("head");
            // get a factory
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            spf.setFeature("http://xml.org/sax/features/namespaces", false);
            spf.setFeature("http://xml.org/sax/features/validation", false);
            //get a new instance of parser
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(
                            new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
                }
            });
            reader.setContentHandler(sax);

            InputSource input = null;

            if (documentPath.endsWith(".gz")) {
                InputStream dataInputStream = new FileInputStream(documentPath);
                GZIPInputStream gzip = new GZIPInputStream(dataInputStream);
                DataInputStream tmp = new DataInputStream(gzip);
                dataInputStream = tmp;
                input = new InputSource(dataInputStream);
            } else {
                input = new InputSource(documentPath);
            }
            input.setEncoding("UTF-8");

            reader.parse(input);

            List<String> descriptionSegments = sax.getTexts();
            String currentPatentNumber = sax.currentPatentNumber;

            ArrayList<PatentItem> patents = new ArrayList<PatentItem>();
            ArrayList<BibDataSet> articles = new ArrayList<BibDataSet>();

            // we process the patent description
            if (descriptionSegments != null && descriptionSegments.size() > 0) {
                extractAllReferencesString(descriptionSegments, false, 0, false, patents, articles);
                // second pass: we add annotations corresponding to identified citation chunks based on
                // stored offsets
                Writer writer = new OutputStreamWriter(
                        new FileOutputStream(new File(newTrainingPath + "/" + currentPatentNumber + ".training.xml"),
                                false),
                        "UTF-8");

                PatentAnnotationSaxParser saxx = new PatentAnnotationSaxParser();
                saxx.setWriter(writer);
                saxx.setPatents(patents);
                saxx.setArticles(articles);

                spf = SAXParserFactory.newInstance();
                spf.setValidating(false);
                spf.setFeature("http://xml.org/sax/features/namespaces", false);
                spf.setFeature("http://xml.org/sax/features/validation", false);
                //get a new instance of parser
                reader = XMLReaderFactory.createXMLReader();
                reader.setEntityResolver(new EntityResolver() {
                    public InputSource resolveEntity(String publicId, String systemId) {
                        return new InputSource(
                                new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
                    }
                });
                reader.setContentHandler(saxx);

                if (documentPath.endsWith(".gz")) {
                    InputStream dataInputStream = new FileInputStream(documentPath);
                    GZIPInputStream gzip = new GZIPInputStream(dataInputStream);
                    DataInputStream tmp = new DataInputStream(gzip);
                    dataInputStream = tmp;
                    input = new InputSource(dataInputStream);
                } else {
                    input = new InputSource(documentPath);
                }

                input.setEncoding("UTF-8");

                reader.parse(input);

                writer.close();

                // last, we generate the training data corresponding to the parsing of the identified NPL citations

                // buffer for the reference block
                StringBuffer allBufferReference = new StringBuffer();
                ArrayList<String> inputs = new ArrayList<String>();
                for (BibDataSet article : articles) {
                    String refString = article.getRawBib();

                    if (refString.trim().length() > 1) {
                        inputs.add(refString.trim());
                    }
                }

                if (inputs.size() > 0) {
                    for (String inpu : inputs) {
                        ArrayList<String> inpus = new ArrayList<String>();
                        inpus.add(inpu);
                        StringBuilder bufferReference = parsers.getCitationParser().trainingExtraction(inpus);
                        if (bufferReference != null) {
                            allBufferReference.append(bufferReference.toString() + "\n");
                        }
                    }
                }

                if (allBufferReference != null) {
                    if (allBufferReference.length() > 0) {
                        Writer writerReference = new OutputStreamWriter(new FileOutputStream(
                                new File(newTrainingPath
                                        + "/"
                                        + currentPatentNumber
                                        +
                                        ".training.references.xml"),
                                false), "UTF-8");
                        writerReference.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                        writerReference.write("<citations>\n");

                        writerReference.write(allBufferReference.toString());

                        writerReference.write("</citations>\n");
                        writerReference.close();
                    }
                }

            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Get a patent description by its number and OPS
     */
    public boolean getDocOPS(String number) {
        try {
            if (ops == null)
                ops = new OPSService();
            String description = ops.descriptionRetrieval(number);
            if (description == null)
                return false;
            else if (description.length() < 600)
                return false;
            else {
                descriptionSegments = new ArrayList<>();
                descriptionSegments.add(description);
                return true;
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Write the list of extracted references in an XML file
     */
    public void generateXMLReport(
            File file,
            ArrayList<PatentItem> patents,
            ArrayList<BibDataSet> articles) {
        try {
            OutputStream tos = new FileOutputStream(file, false);
            Writer writer = new OutputStreamWriter(tos, "UTF-8");
            StringBuffer content = new StringBuffer();
            // header
            content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

            if ((patents.size() > 0) || (articles.size() > 0))
                content.append("<citations>\n");
            if (patents.size() > 0)
                content.append("<patent-citations>\n");

            int i = 0;
            for (PatentItem pi : patents) {
                String dnum = pi.getAuthority() + pi.getNumberEpoDoc();
                if (pi.getKindCode() != null)
                    dnum += pi.getKindCode();
                content.append(
                        "<patcit if=\"pcit"
                                + i
                                + " dnum=\""
                                + dnum
                                + "\">"
                                +
                                "<text>"
                                + pi.getContext()
                                + "</text></patcit>");
                content.append("\n");
                i++;
            }

            if (patents.size() > 0)
                content.append("</patent-citations>\n");

            if (articles.size() > 0)
                content.append("<npl-citations>\n");

            i = 0;
            for (BibDataSet bds : articles) {
                content.append("<nplcit if=\"ncit" + i + "\">");
                content.append(bds.getResBib().toTEI(i));
                content.append("<text>" + bds.getRawBib() + "</text></nplcit>");
                content.append("\n");
                i++;
            }

            if (articles.size() > 0)
                content.append("</npl-citations>\n");

            if ((patents.size() > 0) || (articles.size() > 0))
                content.append("</citations>\n");

            writer.write(content.toString());

            writer.close();
            tos.close();
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * not used...
     */
    private static boolean checkPositionRange(
            int currentPosition,
            int posit,
            List<OffsetPosition> positions) {
        boolean isInRange = false;
        boolean skipTest = false;
        if (currentPosition == positions.size() - 1) {
            if (positions.get(currentPosition).end < posit) {
                skipTest = true;
            }
        }
        if (!skipTest) {
            for (int i = currentPosition; i < positions.size(); i++) {
                if ((positions.get(i).start <= posit) &&
                        (positions.get(i).end >= posit)) {
                    isInRange = true;
                    currentPosition = i;
                    break;
                } else if (positions.get(i).start > posit) {
                    isInRange = false;
                    currentPosition = i;
                    break;
                }
            }
        }
        return isInRange;
    }

    @Override
    public void close() throws IOException {
        taggerAll.close();
        taggerAll = null;
    }
}
