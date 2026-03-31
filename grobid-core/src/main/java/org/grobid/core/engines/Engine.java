package org.grobid.core.engines;

import nu.xom.Element;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.lang.Language;
import org.grobid.core.utilities.Consolidation;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.Utilities;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for managing the extraction of bibliographical information from PDF
 * documents or raw text.
 *
 */
public class Engine implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Engine.class);

    private final EngineParsers parsers = new EngineParsers();
    //TODO: when using one instance of Engine in e.g. grobid-service, then make this field not static
    private static CntManager cntManager = CntManagerFactory.getCntManager();

    // The list of accepted languages
    // the languages are encoded in ISO 3166
    // if null, all languages are accepted.
    private List<String> acceptedLanguages = null;

    /**
     * Parse a sequence of authors from a header, i.e. containing possibly
     * reference markers.
     *
     * @param authorSequence - the string corresponding to a raw sequence of names
     * @return the list of structured author object
     */
    public List<Person> processAuthorsHeader(String authorSequence) throws Exception {
        List<Person> result = parsers.getAuthorParser().processingHeader(authorSequence);
        return result;
    }

    /**
     * Parse a sequence of authors from a citation, i.e. containing no reference
     * markers.
     *
     * @param authorSequence - the string corresponding to a raw sequence of names
     * @return the list of structured author object
     */
    public List<Person> processAuthorsCitation(String authorSequence) throws Exception {
        List<Person> result = parsers.getAuthorParser().processingCitation(authorSequence);
        return result;
    }

    /**
     * Parse a list of independent sequences of authors from citations.
     *
     * @param authorSequences - the list of strings corresponding each to a raw sequence of
     *                        names.
     * @return the list of all recognized structured author objects for each
     *         sequence of authors.
     */
    public List<List<Person>> processAuthorsCitationLists(List<String> authorSequences) throws Exception {
        return null;
    }

    /**
     * Parse a text block corresponding to an affiliation+address.
     *
     * @param addressBlock - the string corresponding to a raw affiliation+address
     * @return the list of all recognized structured affiliation objects.
     * @throws IOException
     */
    public List<Affiliation> processAffiliation(String addressBlock) throws IOException {
        return parsers.getAffiliationAddressParser().processing(addressBlock);
    }

    /**
     * Parse a list of text blocks corresponding to an affiliation+address.
     *
     * @param addressBlocks - the list of strings corresponding each to a raw
     *                      affiliation+address.
     * @return the list of all recognized structured affiliation objects for
     *         each sequence of affiliation + address block.
     */
    public List<List<Affiliation>> processAffiliations(List<String> addressBlocks) throws Exception {
        List<List<Affiliation>> results = null;
        for (String addressBlock : addressBlocks) {
            List<Affiliation> localRes = parsers.getAffiliationAddressParser().processing(addressBlock);
            if (results == null) {
                results = new ArrayList<List<Affiliation>>();
            }
            results.add(localRes);
        }
        return results;
    }

    /**
     * Parse a raw string containing dates.
     *
     * @param dateBlock - the string containing raw dates.
     * @return the list of all structured date objects recognized in the string.
     * @throws IOException
     */
    public List<org.grobid.core.data.Date> processDate(String dateBlock) throws IOException {
        List<org.grobid.core.data.Date> result = parsers.getDateParser().process(dateBlock);
        return result;
    }

    /**
     * Parse a list of raw dates.
     *
     * @param dateBlocks - the list of strings each containing raw dates.
     * @return the list of all structured date objects recognized in the string
     *         for each inputed string.
     */
    /*public List<List<org.grobid.core.data.Date>> processDates(List<String> dateBlocks) {
        return null;
    }*/

    /**
     * Apply a parsing model for a given single raw reference string 
     *
     * @param reference   the reference string to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the recognized bibliographical object
     */
    public BiblioItem processRawReference(String reference, int consolidate) {
        if (reference != null) {
            reference = reference.replaceAll("\\\\", "");
        }
        return parsers.getCitationParser().processingString(reference, consolidate);
    }

    /**
     * Apply a parsing model for a set of raw reference text 
     *
     * @param references  the list of raw reference strings to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of recognized bibliographical objects
     */
    public List<BiblioItem> processRawReferences(List<String> references, int consolidate) throws Exception {
        List<BiblioItem> finalResults = new ArrayList<BiblioItem>();
        if (references == null || references.size() == 0)
            return finalResults;

        List<BiblioItem> results = parsers.getCitationParser().processingStringMultiple(references, 0);
        if (results.size() == 0)
            return finalResults;

        // consolidation in a second stage to take advantage of parallel calls
        if (consolidate == 0) {
            return results;
        } else { 
            // prepare for set consolidation
            List<BibDataSet> bibDataSetResults = new ArrayList<BibDataSet>();
            for (BiblioItem bib : results) {
                BibDataSet bds = new BibDataSet();
                bds.setResBib(bib);
                bds.setRawBib(bib.getReference());
                bibDataSetResults.add(bds);
            }

            Consolidation consolidator = Consolidation.getInstance();
            if (consolidator.getCntManager() == null)
                consolidator.setCntManager(cntManager); 
            Map<Integer,BiblioItem> resConsolidation = null;
            try {
                resConsolidation = consolidator.consolidate(bibDataSetResults);
            } catch(Exception e) {
                throw new GrobidException(
                "An exception occured while running consolidation on bibliographical references.", e);
            } 
            if (resConsolidation != null) {
                for(int i=0; i<bibDataSetResults.size(); i++) {
                    BiblioItem resCitation = bibDataSetResults.get(i).getResBib();
                    BiblioItem bibo = resConsolidation.get(Integer.valueOf(i));
                    if (bibo != null) {
                        if (consolidate == 1)
                            BiblioItem.correct(resCitation, bibo);
                        else if (consolidate == 2)
                            BiblioItem.injectIdentifiers(resCitation, bibo);
                    }
                    finalResults.add(resCitation);
                }
            }
        }

        return finalResults;
    }

    /**
     * Constructor for the Grobid engine instance.
     */
    public Engine(boolean loadModels) {
        /*
         * Runtime.getRuntime().addShutdownHook(new Thread() {
		 *
		 * @Override public void run() { try { close(); } catch (IOException e)
		 * { LOGGER.error("Failed to close all resources: " + e); } } });
		 */
        if (loadModels)
            parsers.initAll();
    }

    /**
     * Apply a parsing model to the reference block of a PDF file 
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of parsed references as bibliographical objects enriched
     *         with citation contexts
     */
    public List<BibDataSet> processReferences(File inputFile, int consolidate) {
        return parsers.getCitationParser()
            .processingReferenceSection(inputFile, null, parsers.getReferenceSegmenterParser(), consolidate);
    }

    /**
     * Apply a parsing model to the reference block of a PDF file 
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the PDF file to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of parsed references as bibliographical objects enriched
     *         with citation contexts
     */
    public List<BibDataSet> processReferences(File inputFile, String md5Str, int consolidate) {
        return parsers.getCitationParser()
			.processingReferenceSection(inputFile, md5Str, parsers.getReferenceSegmenterParser(), consolidate);
    }

    /**
     * Download a PDF file.
     *
     * @param url     URL of the PDF to download
     * @param dirName directory where to store the downloaded PDF
     * @param name file name
     */
    public String downloadPDF(String url, String dirName, String name) {
        return Utilities.uploadFile(url, dirName, name);
    }

    /**
     * Give the list of languages for which an extraction is allowed. If null,
     * any languages will be processed
     *
     * @return the list of languages to be processed coded in ISO 3166.
     */
    public List<String> getAcceptedLanguages() {
        return acceptedLanguages;
    }

    /**
     * Add a language to the list of accepted languages.
     *
     * @param lang the language in ISO 3166 to be added
     */
    public void addAcceptedLanguages(String lang) {
        if (acceptedLanguages == null) {
            acceptedLanguages = new ArrayList<String>();
        }
        acceptedLanguages.add(lang);
    }

    /**
     * Perform a language identification
     *
     * @param ext part
     * @return language
     */
    public Language runLanguageId(String filePath, String ext) {
        try {
            // we just skip the 50 first lines and get the next approx. 5000
            // first characters,
            // which should give a close to ~100% accuracy for the supported languages
            String text = "";
            FileInputStream fileIn = new FileInputStream(filePath.substring(0, filePath.length() - 3) + ext);
            InputStreamReader reader = new InputStreamReader(fileIn, "UTF-8");
            BufferedReader bufReader = new BufferedReader(reader);
            String line;
            int nbChar = 0;
            while (((line = bufReader.readLine()) != null) && (nbChar < 5000)) {
                if (line.length() == 0)
                    continue;
                text += " " + line;
                nbChar += line.length();
            }
            bufReader.close();
            LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            return languageUtilities.runLanguageId(text);
        } catch (IOException e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Basic run for language identification, default is on the body of the
     * current document.
     *
     * @return language id
     */
    public Language runLanguageId(String filePath) {
        return runLanguageId(filePath, "body");
    }

    /**
     * Apply a parsing model for the header of a PDF file, using
     * first three pages of the PDF
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param result      bib result
     * @return the TEI representation of the extracted bibliographical
     *         information
     */
    public String processHeader(
        String inputFile,
        int consolidate,
        boolean includeRawAffiliations,
        boolean includeRawCopyrights,
        boolean includeDiscardedText,
        BiblioItem result
    ) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(0)
            .endPage(2)
            .consolidateHeader(consolidate)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCopyrights(includeRawCopyrights)
            .includeRawCopyrights(includeDiscardedText)
            .build();
        return processHeader(inputFile, null, config, result);
    }

    /**
     * Apply a parsing model for the header of a PDF file combined with an extraction and parsing of 
     * funding information (outside the header possibly)
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param consolidateHeader the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param consolidateFunders the consolidation option allows GROBID to exploit Crossref Funder Registry web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param includeRawAffiliations includes the raw affiliation in the output
     * @param includeRawCopyrights includes the raw copyright information in the output
     * @return the TEI representation of the extracted bibliographical
     *         information
     */
    public String processHeaderFunding(
        File inputFile,
        int consolidateHeader,
        int consolidateFunders,
        boolean includeRawAffiliations,
        boolean includeRawCopyrights,
        boolean includeDiscardedText
    ) throws Exception {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateHeader(consolidateHeader)
            .consolidateFunders(consolidateFunders)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCopyrights(includeRawCopyrights)
            .includeDiscardedText(includeDiscardedText)
            .build();
        return processHeaderFunding(inputFile, null, config);
    }

    /**
     * Apply a parsing model for the header of a PDF file, using
     * first three pages of the PDF
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the processed file
     * @param consolidate the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param result      bib result
     * @return the TEI representation of the extracted bibliographical
     *         information
     */
    public String processHeader(
        String inputFile,
        String md5Str,
        int consolidate,
        boolean includeRawAffiliations,
        boolean includeRawCopyrights,
        boolean includeDiscardedText,
        int startPage,
        int endPage,
        BiblioItem result
    ) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .startPage(startPage)
            .endPage(endPage)
            .consolidateHeader(consolidate)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCopyrights(includeRawCopyrights)
            .includeDiscardedText(includeDiscardedText)
            .build();
        return processHeader(inputFile, md5Str, config, result);
    }

    /**
     * Apply a parsing model for the header of a PDF file combined with an extraction and parsing of 
     * funding information (outside the header possibly)
     *
     * @param inputFile   the path of the PDF file to be processed
     * @param md5Str      MD5 digest of the processed file
     * @param consolidateHeader the consolidation option allows GROBID to exploit Crossref web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param consolidateFunders the consolidation option allows GROBID to exploit Crossref Funder Registry web services for improving header
     *                    information. 0 (no consolidation, default value), 1 (consolidate the citation and inject extra
     *                    metadata) or 2 (consolidate the citation and inject DOI only)
     * @param includeRawAffiliations includes the raw affiliation in the output
     * @param includeRawCopyrights includes the raw copyright information in the output
     * @return the TEI representation of the extracted bibliographical
     *         information
     */
    public String processHeaderFunding(
        File inputFile,
        String md5Str,
        int consolidateHeader,
        int consolidateFunders,
        boolean includeRawAffiliations,
        boolean includeRawCopyrights,
        boolean includeDiscardedText
    ) throws Exception {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateHeader(consolidateHeader)
            .consolidateFunders(consolidateFunders)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCopyrights(includeRawCopyrights)
            .includeDiscardedText(includeDiscardedText)
            .build();
        return processHeaderFunding(inputFile, md5Str, config);
    }

    /**
     * Apply a parsing model for the header of a PDF file, using
     * dynamic range of pages as header
     *
     * @param inputFile   : the path of the PDF file to be processed
     * @param result      bib result
     *
     * @return the TEI representation of the extracted bibliographical
     *         information
     */
    public String processHeader(String inputFile, BiblioItem result) {
        return processHeader(inputFile, null, GrobidAnalysisConfig.defaultInstance(), result);
    }

    public String processHeader(String inputFile, GrobidAnalysisConfig config, BiblioItem result) {
        return processHeader(inputFile, null, config, result);
    }

    public String processHeaderFunding(File inputFile, GrobidAnalysisConfig config) throws Exception {
        return processHeaderFunding(inputFile, null, config);
    }

    public String processHeader(String inputFile, String md5Str, GrobidAnalysisConfig config, BiblioItem result) {
        // normally the BiblioItem reference must not be null, but if it is the
        // case, we still continue
        // with a new instance, so that the resulting TEI string is still
        // delivered
        if (result == null) {
            result = new BiblioItem();
        }
        Pair<String, Document> resultTEI = parsers.getHeaderParser().processing(new File(inputFile), md5Str, result, config);
        return resultTEI.getLeft();
    }

    public String processHeaderFunding(File inputFile, String md5Str, GrobidAnalysisConfig config) throws Exception {
        FullTextParser fullTextParser = parsers.getFullTextParser();
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToTEI on " + inputFile);
        long time = System.currentTimeMillis();
        resultDoc = fullTextParser.processingHeaderFunding(inputFile, md5Str, config);
        LOGGER.debug("Ending processing fullTextToTEI on " + inputFile + ". Time to process: "
            + (System.currentTimeMillis() - time) + "ms");
        return resultDoc.getTei();
    }

    /**
     * Create training data for the monograph model based on the application of
     * the current monograph text model on a new PDF
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathRaw      : the path where to put the sequence labeling feature file
     * @param pathTEI      : the path where to put the annotated TEI representation (the
     *                     file to be corrected for gold-level training data)
     * @param id           : an optional ID to be used in the TEI file and the full text
     *                     file, -1 if not used
     */
    public void createTrainingMonograph(File inputFile, String pathRaw, String pathTEI, int id) {
        Document doc = parsers.getMonographParser().createTrainingFromPDF(inputFile, pathRaw, pathTEI, id);
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model. 
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathRaw      : the path where to put the sequence labeling feature file
     * @param pathTEI      : the path where to put the annotated TEI representation (the
     *                     file to be annotated for "from scratch" training data)
     * @param id           : an optional ID to be used in the TEI file and the full text
     *                     file, -1 if not used
     */
    public void createTrainingBlank(File inputFile, String pathRaw, String pathTEI, int id) {
        parsers.getSegmentationParser().createBlankTrainingData(inputFile, pathRaw, pathTEI, id);
    }

    /**
     * Create training data for all models based on the application of
     * the current full text model on a new PDF
     *
     * @param inputFile    : the path of the PDF file to be processed
     * @param pathRaw      : the path where to put the sequence labeling feature file
     * @param pathTEI      : the path where to put the annotated TEI representation (the
     *                       file to be corrected for gold-level training data)
     * @param id           : an optional ID to be used in the TEI file, -1 if not used
     */
    public void createTraining(File inputFile, String pathRaw, String pathTEI, int id, GrobidModels.Flavor flavor) {
        System.out.println(inputFile.getPath());
        Document doc = parsers.getFullTextParser(flavor).createTraining(inputFile, pathRaw, pathTEI, id, flavor);
    }

    /**
     *
     * //TODO: remove invalid JavaDoc once refactoring is done and tested (left for easier reference)
     * Parse and convert the current article into TEI, this method performs the
     * whole parsing and conversion process. If onlyHeader is true, than only
     * the tei header data will be created.
     *
     * @param inputFile            - absolute path to the pdf to be processed
     * @param config               - Grobid config
	 * @return the resulting structured document as a TEI string.
     */
    public String fullTextToTEI(File inputFile,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, null,null, config).getTei();
    }

    public String fullTextToTEI(File inputFile,
                                GrobidModels.Flavor flavor,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, flavor, null, config).getTei();
    }

    /**
     * //TODO: remove invalid JavaDoc once refactoring is done and tested (left for easier reference)
     * Parse and convert the current article into TEI, this method performs the
     * whole parsing and conversion process. If onlyHeader is true, than only
     * the tei header data will be created.
     *
     * @param inputFile            - absolute path to the pdf to be processed
     * @param md5Str               - MD5 digest of the PDF file to be processed
     * @param config               - Grobid config
     * @return the resulting structured document as a TEI string.
     */
    public String fullTextToTEI(File inputFile,
                                GrobidModels.Flavor flavor,
                                String md5Str,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, flavor, md5Str, config).getTei();
    }

    public Document fullTextToTEIDoc(File inputFile,
                                    GrobidModels.Flavor flavor,
                                    String md5Str,
                                     GrobidAnalysisConfig config) throws Exception {
        FullTextParser fullTextParser = parsers.getFullTextParser(flavor);
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToTEI on " + inputFile);
        long time = System.currentTimeMillis();
        resultDoc = fullTextParser.processing(inputFile, flavor, md5Str, config);
        LOGGER.debug("Ending processing fullTextToTEI on " + inputFile + ". Time to process: "
			+ (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    public Document fullTextToTEIDoc(File inputFile,
                                     GrobidAnalysisConfig config) throws Exception {
        return fullTextToTEIDoc(inputFile, null, null, config);
    }

    public Document fullTextToTEIDoc(DocumentSource documentSource,
                                    GrobidModels.Flavor flavor,
                                    GrobidAnalysisConfig config) throws Exception {
        FullTextParser fullTextParser = parsers.getFullTextParser(flavor);
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToTEI on " + documentSource);
        long time = System.currentTimeMillis();
        resultDoc = fullTextParser.processing(documentSource, flavor, config);
        LOGGER.debug("Ending processing fullTextToTEI on " + documentSource + ". Time to process: "
                + (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    /**
     * Process all the PDF in a given directory with a segmentation process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML file
     *                      shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                      identify the training case. This is optional: no identifier
     *                      will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTraining(String directoryPath, String resultPath, int ind, GrobidModels.Flavor flavor) {
        try {
            File path = new File(directoryPath);
            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
			if (ind == -1) {
				// for undefined identifier (value at -1), we initialize it to 0
				n = 1;
			}
            for (final File pdfFile : refFiles) {
                try {
                    createTraining(pdfFile, resultPath, resultPath, ind + n, flavor);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
						+ pdfFile.getPath(), exp);
                }
				if (ind != -1)
					n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Process all the PDF in a given directory with a monograph process and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                        and the sequence labeling feature files shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                        identify the training case. This is optional: no identifier
     *                        will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingMonograph(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File pdfFile : refFiles) {
                try {
                    createTrainingMonograph(pdfFile, resultPath, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + pdfFile.getPath(), exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Process all the PDF in a given directory with a pdf extraction and
     * produce blank training data, i.e. TEI files with text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param directoryPath - the path to the directory containing PDF to be processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                        and default sequence labeling feature files shall be written.
     * @param ind           - identifier integer to be included in the resulting files to
     *                        identify the training case. This is optional: no identifier
     *                        will be included if ind = -1
     * @return the number of processed files.
     */
    public int batchCreateTrainingBlank(String directoryPath, String resultPath, int ind) {
        try {
            File path = new File(directoryPath);
            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            if (ind == -1) {
                // for undefined identifier (value at -1), we initialize it to 0
                n = 1;
            }
            for (final File pdfFile : refFiles) {
                try {
                    createTrainingBlank(pdfFile, resultPath, resultPath, ind + n);
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + pdfFile.getPath(), exp);
                }
                if (ind != -1)
                    n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Get the TEI XML string corresponding to the recognized header text
     */
    public static String header2TEI(BiblioItem resHeader) {
        return resHeader.toTEI(0);
    }

    /**
     * Get the BibTeX string corresponding to the recognized header text
     */
    public static String header2BibTeX(BiblioItem resHeader) {
        return resHeader.toBibTeX();
    }

    /**
     * Get the TEI XML string corresponding to the recognized citation section,
     * with pointers and advanced structuring
     */
    public static String references2TEI(String path, List<BibDataSet> resBib) {
        StringBuilder result = new StringBuilder();
        result.append("<listbibl>\n");

        int p = 0;
        for (BibDataSet bib : resBib) {
            BiblioItem bit = bib.getResBib();
            bit.setPath(path);
            result.append("\n").append(bit.toTEI(p));
            p++;
        }
        result.append("\n</listbibl>\n");
        return result.toString();
    }

    /**
     * Get the BibTeX string corresponding to the recognized citation section
     */
    public String references2BibTeX(String path, List<BibDataSet> resBib) {
        StringBuilder result = new StringBuilder();

        for (BibDataSet bib : resBib) {
            BiblioItem bit = bib.getResBib();
            bit.setPath(path);
            result.append("\n").append(bit.toBibTeX());
        }

        return result.toString();
    }

    /**
     * Get the TEI XML string corresponding to the recognized citation section
     * for a particular citation
     */
    public static String reference2TEI(String path, List<BibDataSet> resBib, int i) {
        StringBuilder result = new StringBuilder();

        if (resBib != null) {
            if (i <= resBib.size()) {
                BibDataSet bib = resBib.get(i);
                BiblioItem bit = bib.getResBib();
                bit.setPath(path);
                result.append(bit.toTEI(i));
            }
        }

        return result.toString();
    }

    /**
     * Get the BibTeX string corresponding to the recognized citation section
     * for a given citation
     */
    public static String reference2BibTeX(String path, List<BibDataSet> resBib, int i) {
        StringBuilder result = new StringBuilder();

        if (resBib != null) {
            if (i <= resBib.size()) {
                BibDataSet bib = resBib.get(i);
                BiblioItem bit = bib.getResBib();
                bit.setPath(path);
                result.append(bit.toBibTeX());
            }
        }

        return result.toString();
    }

    /**
     * Extract and parse both patent and non patent references within a patent text. Result are provided as a BibDataSet
     * with offset position instanciated relative to input text and as PatentItem containing both "WISIWIG" results (the
     * patent reference attributes as they appear in the text) and the attributes in DOCDB format (format according to
     * WIPO and ISO standards). Patent references' offset positions are also given in the PatentItem object.
     *
     * @param text                 the string corresponding to the text body of the patent.
     * @param nplResults           the list of extracted and parsed non patent references as BiblioItem object. This
     *                             list must be instantiated before calling the method for receiving the results.
     * @param patentResults        the list of extracted and parsed patent references as PatentItem object. This list
     *                             must be instantiated before calling the method for receiving the results.
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref web services for improving
     *                             header information. 0 (no consolidation, default value), 1 (consolidate the citation
     *                             and inject extra metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of extracted and parserd patent and non-patent references encoded in TEI.
     */
    public String processAllCitationsInPatent(String text, 
                                            List<BibDataSet> nplResults, 
                                            List<PatentItem> patentResults,
                                            int consolidateCitations, 
                                            boolean includeRawCitations) throws Exception {
        if ((nplResults == null) && (patentResults == null)) {
            return null;
        }
        // we initialize the attribute individually for readability...
        boolean filterDuplicate = false;
        List<String> texts = new ArrayList<>();
        texts.add(text);
        return parsers.getReferenceExtractor().extractAllReferencesString(texts, filterDuplicate,
			consolidateCitations, includeRawCitations, patentResults, nplResults);
    }

    /**
     * Extract and parse both patent and non patent references within a patent in ST.36 format. Result are provided as a
     * BibDataSet with offset position instantiated relative to input text and as PatentItem containing both "WISIWIG"
     * results (the patent reference attributes as they appear in the text) and the attributes in DOCDB format (format
     * according to WIPO and ISO standards). Patent references' offset positions are also given in the PatentItem
     * object.
     *
     * @param nplResults           the list of extracted and parsed non patent references as BiblioItem object. This
     *                             list must be instanciated before calling the method for receiving the results.
     * @param patentResults        the list of extracted and parsed patent references as PatentItem object. This list
     *                             must be instanciated before calling the method for receiving the results.
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref web services for improving
     *                             header information. 0 (no consolidation, default value), 1 (consolidate the citation
     *                             and inject extra metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of extracted and parserd patent and non-patent references encoded in TEI.
     * @throws Exception if sth. went wrong
     */
    public String processAllCitationsInXMLPatent(String xmlPath, List<BibDataSet> nplResults,
                                                 List<PatentItem> patentResults,
                                                 int consolidateCitations, 
                                                 boolean includeRawCitations) throws Exception {
        if ((nplResults == null) && (patentResults == null)) {
            return null;
        }
        // we initialize the attribute individually for readability...
        boolean filterDuplicate = false;
        return parsers.getReferenceExtractor().extractAllReferencesXMLFile(xmlPath, filterDuplicate,
			consolidateCitations, includeRawCitations, patentResults, nplResults);
    }

    /**
     * Extract and parse both patent and non patent references within a patent
     * in PDF format. Result are provided as a BibDataSet with offset position
     * instanciated relative to input text and as PatentItem containing both
     * "WISIWIG" results (the patent reference attributes as they appear in the
     * text) and the attributes in DOCDB format (format according to WIPO and
     * ISO standards). Patent references' offset positions are also given in the
     * PatentItem object.
     *
     * @param pdfPath              pdf path
     * @param nplResults           the list of extracted and parsed non patent references as
     *                             BiblioItem object. This list must be instanciated before
     *                             calling the method for receiving the results.
     * @param patentResults        the list of extracted and parsed patent references as
     *                             PatentItem object. This list must be instanciated before
     *                             calling the method for receiving the results.
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref web services for improving
     *                             header information. 0 (no consolidation, default value), 1 (consolidate the citation
     *                             and inject extra metadata) or 2 (consolidate the citation and inject DOI only)
     * @return the list of extracted and parserd patent and non-patent references
     *         encoded in TEI.
     * @throws Exception if sth. went wrong
     */
    public String processAllCitationsInPDFPatent(String pdfPath, List<BibDataSet> nplResults,
                                                 List<PatentItem> patentResults,
                                                 int consolidateCitations, 
                                                 boolean includeRawCitations) throws Exception {
        if ((nplResults == null) && (patentResults == null)) {
            return null;
        }
        // we initialize the attribute individually for readability...
        boolean filterDuplicate = false;
        return parsers.getReferenceExtractor().extractAllReferencesPDFFile(pdfPath, filterDuplicate,
                consolidateCitations, includeRawCitations, patentResults, nplResults);
    }
	
    /**
     * Extract and parse both patent and non patent references within a patent
     * in PDF format. Results are provided as JSON annotations with coordinates
	 * of the annotations in the orignal PDF and reference informations in DOCDB 
	 * format (format according to WIPO and ISO standards).
     *
     * @param pdfPath              pdf path
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref web services for improving
     *                             header information. 0 (no consolidation, default value), 1 (consolidate the citation
     *                             and inject extra metadata) or 2 (consolidate the citation and inject DOI only)
     *
     * @return JSON annotations with extracted and parsed patent and non-patent references
     *         together with coordinates in the original PDF.
     */
    public String annotateAllCitationsInPDFPatent(String pdfPath, 
                                                  int consolidateCitations, 
                                                  boolean includeRawCitations) throws Exception {
		List<BibDataSet> nplResults = new ArrayList<BibDataSet>();
		List<PatentItem> patentResults = new ArrayList<PatentItem>();
        // we initialize the attribute individually for readability...
        boolean filterDuplicate = false;
        return parsers.getReferenceExtractor().annotateAllReferencesPDFFile(pdfPath, filterDuplicate,
                consolidateCitations, includeRawCitations, patentResults, nplResults);
    }

    /*public void processCitationPatentTEI(String teiPath, String outTeiPath,
                                         int consolidateCitations) throws Exception {
        try {
            InputStream inputStream = new FileInputStream(new File(teiPath));
            OutputStream output = new FileOutputStream(new File(outTeiPath));
            final TeiStAXParser parser = new TeiStAXParser(inputStream, output, false,
				consolidateCitations);
            parser.parse();
            inputStream.close();
            output.close();
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }*/

    /**
     * Process an XML patent document with a patent citation extraction and
     * produce the corresponding training data format files for manual
     * correction. The goal of this method is to help to produce additional
     * traning data based on an existing model.
     *
     * @param pathXML    - the path to the XML patent document to be processed.
     * @param resultPath - the path to the directory where the results as XML files
     *                   shall be written.
     */
    public void createTrainingPatentCitations(String pathXML, String resultPath)
		throws Exception {
        parsers.getReferenceExtractor().generateTrainingData(pathXML, resultPath);
    }


    /**
     * Process all the XML patent documents in a given directory with a patent
     * citation extraction and produce the corresponding training data format
     * files for manual correction. The goal of this method is to help to
     * produce additional traning data based on an existing model.
     *
     * @param directoryPath - the path to the directory containing XML files to be
     *                      processed.
     * @param resultPath    - the path to the directory where the results as XML files
     *                      shall be written.
     * @return the number of processed files.
     */
    public int batchCreateTrainingPatentcitations(String directoryPath, String resultPath)
		throws Exception {
        try {
            File path = new File(directoryPath);
            // we process all xml files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml") || name.endsWith(".XML") ||
                            name.endsWith(".xml.gz") || name.endsWith(".XML.gz");
                }
            });

            if (refFiles == null)
                return 0;

            // System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            for (; n < refFiles.length; n++) {
                File xmlFile = refFiles[n];
                createTrainingPatentCitations(xmlFile.getPath(), resultPath);
            }

            return refFiles.length;
        } catch (Exception e) {
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    /**
     * Extract chemical names from text.
     *
     * @param text - text to be processed.
     * @return List of chemical entites as POJO.
     */
    public List<ChemicalEntity> extractChemicalEntities(String text) throws Exception {
        return parsers.getChemicalParser().extractChemicalEntities(text);
    }

    /**
     * Print the abstract content. Useful for term extraction.
     */
    public String getAbstract(Document doc) throws Exception {
        String abstr = doc.getResHeader().getAbstract();
        abstr = abstr.replace("@BULLET", " • ");
        return abstr;
    }

    /**
     * Process all the .txt in a given directory to generate pre-labeld training data for
     * the citation model. Input file expects one raw reference string per line.
     *
     * @param directoryPath - the path to the directory containing .txt to be processed.
     * @param resultPath    - the path to the directory where the results as XML training files
     *                        shall be written.
     **/
    public int batchCreateTrainingCitation(String directoryPath, String resultPath) {
        try {
            File path = new File(directoryPath);
            // we process all pdf files in the directory
            File[] refFiles = path.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    System.out.println(name);
                    return name.endsWith(".txt");
                }
            });

            if (refFiles == null)
                return 0;

            System.out.println(refFiles.length + " files to be processed.");

            int n = 0;
            for (final File txtFile : refFiles) {
                try {
                    // read file line by line, assuming one reference string per line
                    List<String> allInput = new ArrayList<>();

                    BufferedReader reader;
                    try {
                        reader = new BufferedReader(new FileReader(txtFile));
                        String line = reader.readLine();
                        while (line != null) {
                            allInput.add(line.trim());
                            line = reader.readLine();
                        }
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // process the training generation
                    StringBuilder bufferReference = parsers.getCitationParser().trainingExtraction(allInput);

                    // write the XML training file
                    if (bufferReference != null) {
                        bufferReference.append("\n");

                        Writer writerReference = new OutputStreamWriter(new FileOutputStream(new File(resultPath +
                                File.separator +
                                txtFile.getName().replace(".txt", ".training.references.tei.xml")), false), StandardCharsets.UTF_8);

                        writerReference.write("<?xml version=\"1.0\" ?>\n<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" " +
                                                "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                                                "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");

                        writerReference.write("\t<teiHeader>\n\t\t<fileDesc xml:id=\"_" + n +
                            "\"/>\n\t</teiHeader>\n\t<text>\n\t\t<front/>\n\t\t<body/>\n\t\t<back>\n");
                        
                        writerReference.write("<listBibl>\n");

                        writerReference.write(bufferReference.toString());

                        writerReference.write("\t\t</listBibl>\n\t</back>\n\t</text>\n</TEI>\n");
                        writerReference.close();
                    }
                } catch (final Exception exp) {
                    LOGGER.error("An error occured while processing the following pdf: "
                        + txtFile.getPath(), exp);
                }
                n++;
            }

            return refFiles.length;
        } catch (final Exception exp) {
            throw new GrobidException("An exception occured while running Grobid batch.", exp);
        }
    }

    /**
     * Return all the reference titles. Maybe useful for term extraction.
     */
    public String printRefTitles(List<BibDataSet> resBib) throws Exception {
        StringBuilder accumulated = new StringBuilder();
        for (BibDataSet bib : resBib) {
            BiblioItem bit = bib.getResBib();

            if (bit.getTitle() != null) {
                accumulated.append(bit.getTitle()).append("\n");
            }
        }

        return accumulated.toString();
    }

    /**
     * Process a text corresponding to a funding and/or acknowledgement section 
     * and retun the extracted entities as JSON annotations
     */
    public String processFundingAcknowledgement(String text, GrobidAnalysisConfig config) throws Exception {
        StringBuilder result = new StringBuilder();

        try {
            MutablePair<Element, MutableTriple<List<Funding>,List<Person>,List<Affiliation>>> localResult = 
                parsers.getFundingAcknowledgementParser().processing(text, config);

            if (localResult == null || localResult.getLeft() == null) 
                result.append(text);
            else
                result.append(localResult.getLeft().toXML()); 

        } catch (final Exception exp) {
            throw new GrobidException("An exception occurred while running Grobid funding-acknowledgement model.", exp);
        }

        return result.toString();
    }

    @Override
    public synchronized void close() throws IOException {
        CrossrefClient.getInstance().close();
        parsers.close();
    }

    public static void setCntManager(CntManager cntManager) {
        Engine.cntManager = cntManager;
    }

    public static CntManager getCntManager() {
        return cntManager;
    }

    public EngineParsers getParsers() {
        return parsers;
    }

    /**
     * @return a new engine from GrobidFactory if the execution is parallel,
     *         else return the instance of engine.
     */
    /*public static Engine getEngine(boolean isparallelExec) {
        return isparallelExec ? GrobidPoolingFactory.getEngineFromPool()
                : GrobidFactory.getInstance().getEngine();
    }*/
    public static Engine getEngine(boolean preload) {
        return GrobidPoolingFactory.getEngineFromPool(preload);
    }


    public String fullTextToBlank(File inputFile,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToBlankDoc(inputFile, null, config).getTei();
    }


    public String fullTextToBlank(File inputFile,
                                String md5Str,
                                GrobidAnalysisConfig config) throws Exception {
        return fullTextToBlankDoc(inputFile, md5Str, config).getTei();
    }

    public Document fullTextToBlankDoc(File inputFile,
                                     String md5Str,
                                     GrobidAnalysisConfig config) throws Exception {
        FullTextBlankParser fullTextBlankParser = parsers.getFullTextBlankParser();
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToBlank on " + inputFile);
        long time = System.currentTimeMillis();
        resultDoc = fullTextBlankParser.process(inputFile, md5Str, config);
        LOGGER.debug("Ending processing fullTextToBlank on " + inputFile + ". Time to process: "
            + (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    public Document fullTextToBlankDoc(File inputFile,
                                     GrobidAnalysisConfig config) throws Exception {
        return fullTextToBlankDoc(inputFile, null, config);
    }

    public Document fullTextToBlankDoc(DocumentSource documentSource,
                                     GrobidAnalysisConfig config) throws Exception {
        FullTextBlankParser fullTextBlankParser = parsers.getFullTextBlankParser();
        Document resultDoc;
        LOGGER.debug("Starting processing fullTextToBlank on " + documentSource);
        long time = System.currentTimeMillis();
        resultDoc = fullTextBlankParser.process(documentSource, config);
        LOGGER.debug("Ending processing fullTextToBlank on " + documentSource + ". Time to process: "
            + (System.currentTimeMillis() - time) + "ms");
        return resultDoc;
    }

    public Document getFigures(
        File inputFile, GrobidAnalysisConfig config
    ) throws Exception {
        return parsers.getFigureSegmenterParser().process(inputFile,  config);
    }

}