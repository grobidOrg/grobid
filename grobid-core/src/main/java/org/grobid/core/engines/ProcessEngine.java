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
package org.grobid.core.engines;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.data.*;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.main.batch.GrobidMainArgs;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.visualization.CitationsVisualizer;

public class ProcessEngine implements Closeable {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngine.class);

    /**
     * The engine.
     */
    private static Engine engine;

    /**
     * @return the engine instance.
     */
    protected Engine getEngine() {
        if (engine == null) {
            engine = GrobidFactory.getInstance().createEngine();
        }
        return engine;
    }

    /**
     * Close engine resources.
     */
    @Override
    public void close() throws IOException {
        if (engine != null) {
            engine.close();
        }
        System.exit(0);
    }

    /**
     * Process the headers using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processHeader(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        File[] files = pdfDirectory.listFiles();
        if (files == null) {
            LOGGER.warn("No files in directory: " + pdfDirectory);
        } else {
            processHeaderDirectory(files, pGbdArgs, pGbdArgs.getPath2Output());
        }
    }

    /**
     * Process the header recursively or not using pGbdArgs parameters.
     *
     * @param files    list of files to be processed
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    private void processHeaderDirectory(File[] files, final GrobidMainArgs pGbdArgs, String outputPath) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            String result;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        result = getEngine().processHeader(currPdf.getAbsolutePath(), null);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdirs();
                        }
                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath())
                                                    .getName()
                                                    .replace(".pdf", ".tei.xml"),
                                    result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath())
                                                    .getName()
                                                    .replace(".PDF", ".tei.xml"),
                                    result.toString());
                        }
                    } else if (recurse && currPdf.isDirectory()) {
                        File[] newFiles = currPdf.listFiles();
                        if (newFiles != null) {
                            String newLevel = currPdf.getName();
                            processHeaderDirectory(
                                    newFiles,
                                    pGbdArgs,
                                    outputPath +
                                            File.separator + newLevel);
                        }
                    }
                } catch (final Exception exp) {
                    LOGGER.error(
                            "An error occurred while processing the file "
                                    + currPdf.getAbsolutePath()
                                    + ". Continuing the process for the other files",
                            exp);
                }
            }
        }
    }

    /**
     * Process the full text using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processFullText(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        File[] files = pdfDirectory.listFiles();
        if (files == null) {
            LOGGER.warn("No files in directory: " + pdfDirectory);
        } else {
            List<String> elementCoordinates = null;
            if (pGbdArgs.getTeiCoordinates()) {
                elementCoordinates = Arrays.asList(
                        "figure",
                        "persName",
                        "ref",
                        "biblStruct",
                        "formula",
                        "s",
                        "note",
                        "title",
                        "head",
                        "affiliation",
                        "p");
            }
            processFullTextDirectory(
                    files,
                    pGbdArgs,
                    pGbdArgs.getPath2Output(),
                    pGbdArgs.getSaveAssets(),
                    elementCoordinates,
                    pGbdArgs.getSegmentSentences(),
                    pGbdArgs.getAddElementId());
            System.out.println(Engine.getCntManager());
        }
    }

    /**
     * Process the full text recursively or not using pGbdArgs parameters.
     *
     * @param files    list of files to be processed
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    private void processFullTextDirectory(
            File[] files,
            final GrobidMainArgs pGbdArgs,
            String outputPath,
            boolean saveAssets,
            List<String> elementCoordinates,
            boolean segmentSentences,
            boolean addElementId) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            String result;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        LOGGER.info("Processing: " + currPdf.getPath());
                        GrobidAnalysisConfig config = null;
                        // path for saving assets
                        if (saveAssets) {
                            String baseName = currPdf.getName().replace(".pdf", "").replace(".PDF", "");
                            String assetPath = outputPath + File.separator + baseName + "_assets";
                            config = GrobidAnalysisConfig.builder()
                                    .pdfAssetPath(new File(assetPath))
                                    .generateTeiCoordinates(elementCoordinates)
                                    .withSentenceSegmentation(segmentSentences)
                                    .generateTeiIds(addElementId)
                                    .build();
                        } else
                            config = GrobidAnalysisConfig.builder()
                                    .generateTeiCoordinates(elementCoordinates)
                                    .withSentenceSegmentation(segmentSentences)
                                    .generateTeiIds(addElementId)
                                    .build();
                        result = getEngine().fullTextToTEI(currPdf, config);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdir();
                        }
                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath())
                                                    .getName()
                                                    .replace(".pdf", ".tei.xml"),
                                    result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath())
                                                    .getName()
                                                    .replace(".PDF", ".tei.xml"),
                                    result.toString());
                        }
                    } else if (recurse && currPdf.isDirectory()) {
                        File[] newFiles = currPdf.listFiles();
                        if (newFiles != null) {
                            String newLevel = currPdf.getName();
                            processFullTextDirectory(
                                    newFiles,
                                    pGbdArgs,
                                    outputPath +
                                            File.separator + newLevel,
                                    saveAssets,
                                    elementCoordinates,
                                    segmentSentences,
                                    addElementId);
                        }
                    }
                } catch (final Exception exp) {
                    LOGGER.error(
                            "An error occurred while processing the file "
                                    + currPdf.getAbsolutePath()
                                    + ". Continuing the process for the other files",
                            exp);
                }
            }
        }
    }

    /**
     * Process the date using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processDate(final GrobidMainArgs pGbdArgs) throws Exception {
        inferOutputPath(pGbdArgs);
        final List<Date> result = getEngine().processDate(pGbdArgs.getInput());
        if (isEmpty(result)) {
            throw new GrobidResourceException("Cannot read the input data for date. Check the documentation. ");
        }
        IOUtilities.writeInFile(pGbdArgs.getPath2Output() + File.separator + "result", result.get(0).toTEI());
    }

    /**
     * Process the author header using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processAuthorsHeader(final GrobidMainArgs pGbdArgs) throws Exception {
        inferOutputPath(pGbdArgs);
        final List<Person> result = getEngine().processAuthorsHeader(pGbdArgs.getInput());
        if (isEmpty(result)) {
            throw new GrobidResourceException(
                    "Cannot read the input data for processAuthorHeader. Check the documentation. ");
        }
        IOUtilities.writeInFile(pGbdArgs.getPath2Output() + File.separator + "result", result.get(0).toTEI(false));
    }

    /**
     * Process the author citation using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters
     * @throws Exception
     */
    public void processAuthorsCitation(final GrobidMainArgs pGbdArgs) throws Exception {
        inferOutputPath(pGbdArgs);
        final List<Person> result = getEngine().processAuthorsCitation(pGbdArgs.getInput());
        if (isEmpty(result)) {
            throw new GrobidResourceException(
                    "Cannot read the input data for authorsCitation. Check the documentation. ");
        }
        IOUtilities.writeInFile(pGbdArgs.getPath2Output() + File.separator + "result", result.get(0).toTEI(false));
    }

    /**
     * Process the affiliation using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processAffiliation(final GrobidMainArgs pGbdArgs) throws Exception {
        inferOutputPath(pGbdArgs);
        final List<Affiliation> result = getEngine().processAffiliation(pGbdArgs.getInput());
        if (isEmpty(result)) {
            throw new GrobidResourceException("Cannot read the input data for affiliations. Check the documentation. ");
        }
        IOUtilities.writeInFile(
                pGbdArgs.getPath2Output() + File.separator + "result",
                Affiliation.toTEI(result.get(0), 0));
    }

    /**
     * Process the raw reference using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processRawReference(final GrobidMainArgs pGbdArgs) throws Exception {
        inferOutputPath(pGbdArgs);
        final BiblioItem result = getEngine().processRawReference(pGbdArgs.getInput(), 0);
        IOUtilities.writeInFile(pGbdArgs.getPath2Output() + File.separator + "result", result.toTEI(-1));
    }

    /**
     * Process all the references using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processReferences(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        File[] files = pdfDirectory.listFiles();
        if (files == null) {
            LOGGER.warn("No files in directory: " + pdfDirectory);
        } else {
            processReferencesDirectory(files, pGbdArgs, pGbdArgs.getPath2Output());
        }
    }

    /**
     * Process the references recursively or not using pGbdArgs parameters.
     *
     * @param files    list of files to be processed
     * @param pGbdArgs The parameters.
     */
    private void processReferencesDirectory(File[] files, final GrobidMainArgs pGbdArgs, String outputPath) {
        if (files != null) {
            boolean recurse = pGbdArgs.isRecursive();
            int id = 0;
            for (final File currPdf : files) {
                try {
                    if (currPdf.getName().toLowerCase().endsWith(".pdf")) {
                        final List<BibDataSet> results = getEngine().processReferences(currPdf, 0);
                        File outputPathFile = new File(outputPath);
                        if (!outputPathFile.exists()) {
                            outputPathFile.mkdir();
                        }

                        StringBuilder result = new StringBuilder();
                        // dummy header
                        result.append(
                                "<?xml version=\"1.0\" ?>\n<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" "
                                        +
                                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                        +
                                        "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");

                        result.append(
                                "\t<teiHeader>\n\t\t<fileDesc xml:id=\"f_"
                                        + id
                                        +
                                        "\"/>\n\t</teiHeader>\n");

                        result.append("\t<text>\n\t\t<front/>\n\t\t<body/>\n\t\t<back>\n\t\t\t<listBibl>\n");
                        for (BibDataSet res : results) {
                            result.append(res.toTEI());
                            result.append("\n");
                        }
                        result.append("\t\t\t</listBibl>\n\t\t</back>\n\t</text>\n</TEI>\n");

                        if (currPdf.getName().endsWith(".pdf")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath()).getName()
                                                    .replace(".pdf", ".references.tei.xml"),
                                    result.toString());
                        } else if (currPdf.getName().endsWith(".PDF")) {
                            IOUtilities.writeInFile(
                                    outputPath + File.separator
                                            + new File(currPdf.getAbsolutePath()).getName()
                                                    .replace(".PDF", ".references.tei.xml"),
                                    result.toString());
                        }
                    } else if (recurse && currPdf.isDirectory()) {
                        File[] newFiles = currPdf.listFiles();
                        if (newFiles != null) {
                            String newLevel = currPdf.getName();
                            processReferencesDirectory(
                                    newFiles,
                                    pGbdArgs,
                                    outputPath +
                                            File.separator + newLevel);
                        }
                    }
                } catch (final Exception exp) {
                    LOGGER.error(
                            "An error occurred while processing the file "
                                    + currPdf.getAbsolutePath()
                                    + ". Continuing the process for the other files",
                            exp);
                }
                id++;
            }
        }
    }

    /**
     * Generate training data for all models
     *
     * @param pGbdArgs The parameters.
     */
    public void createTraining(final GrobidMainArgs pGbdArgs) {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTraining(
                pGbdArgs.getPath2Input(),
                pGbdArgs.getPath2Output(),
                -1,
                pGbdArgs.getModelFlavor());
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate training data for the monograph model from provided directory of PDF documents.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingMonograph(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingMonograph(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate blank training data from provided directory of PDF documents, i.e. where TEI files are text only
     * without tags. This can be used to start from scratch any new model.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingBlank(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingBlank(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output(), -1);
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate training data for citation extraction from patent documents.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingCitationPatent(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine()
                .batchCreateTrainingPatentcitations(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output());
        LOGGER.info(result + " files processed.");
    }

    /**
     * Generate training data from raw reference strings.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void createTrainingCitation(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        int result = getEngine().batchCreateTrainingCitation(pGbdArgs.getPath2Input(), pGbdArgs.getPath2Output());
        LOGGER.info(result + " files processed.");
    }

    /**
     * Process a patent encoded in TEI using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    /*public void processCitationPatentTEI(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);

        final File teiDirectory = new File(pGbdArgs.getPath2Input());
        String result = StringUtils.EMPTY;
        for (final File currTEI : teiDirectory.listFiles()) {
            try {
                if (currTEI.getName().toLowerCase().endsWith(".tei") ||
                        currTEI.getName().toLowerCase().endsWith(".tei.xml")) {
                    getEngine().processCitationPatentTEI(pGbdArgs.getPath2Input() + File.separator + currTEI.getName(),
                            pGbdArgs.getPath2Output() + File.separator + currTEI.getName(), 0);
                }
            } catch (final Exception exp) {
                LOGGER.error("An error occurred while processing the file " + currTEI.getAbsolutePath()
                        + ". Continuing the process for the other files", exp);
            }
        }
    }*/

    /**
     * Process a patent encoded in ST.36 using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processCitationPatentST36(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);

        final File xmlDirectory = new File(pGbdArgs.getPath2Input());
        String result = StringUtils.EMPTY;
        for (final File currXML : xmlDirectory.listFiles()) {
            try {
                if (currXML.getName().toLowerCase().endsWith(".xml") ||
                        currXML.getName().toLowerCase().endsWith(".xml.gz")) {
                    List<BibDataSet> articles = new ArrayList<BibDataSet>();
                    List<PatentItem> patents = new ArrayList<PatentItem>();
                    result = getEngine().processAllCitationsInXMLPatent(
                            pGbdArgs.getPath2Input() + File.separator + currXML.getName(),
                            articles,
                            patents,
                            0,
                            false);
                    if (currXML.getName().endsWith(".gz")) {
                        IOUtilities.writeInFile(
                                pGbdArgs.getPath2Output() + File.separator
                                        + new File(currXML.getAbsolutePath()).getName().replace(".xml.gz", ".tei.xml"),
                                result);
                    } else {
                        IOUtilities.writeInFile(
                                pGbdArgs.getPath2Output() + File.separator
                                        + new File(currXML.getAbsolutePath()).getName().replace(".xml", ".tei.xml"),
                                result);
                    }
                }
            } catch (final Exception exp) {
                LOGGER.error(
                        "An error occurred while processing the file "
                                + currXML.getAbsolutePath()
                                + ". Continuing the process for the other files",
                        exp);
            }
        }
    }

    /**
     * Process a patent in utf-8 text using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processCitationPatentTXT(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);

        final File txtDirectory = new File(pGbdArgs.getPath2Input());
        String result = StringUtils.EMPTY;
        for (final File currTXT : txtDirectory.listFiles()) {
            try {
                if (currTXT.getName().toLowerCase().endsWith(".txt")) {
                    String inputStr = FileUtils.readFileToString(currTXT, "UTF-8");
                    List<BibDataSet> articles = new ArrayList<BibDataSet>();
                    List<PatentItem> patents = new ArrayList<PatentItem>();
                    result = getEngine().processAllCitationsInPatent(inputStr, articles, patents, 0, false);
                    IOUtilities.writeInFile(
                            pGbdArgs.getPath2Output() + File.separator
                                    + new File(currTXT.getAbsolutePath()).getName().replace(".txt", ".tei.xml"),
                            result);
                }
            } catch (final Exception exp) {
                LOGGER.error(
                        "An error occurred while processing the file "
                                + currTXT.getAbsolutePath()
                                + ". Continuing the process for the other files",
                        exp);
            }
        }
    }

    /**
     * Process a patent available in PDF using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processCitationPatentPDF(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);

        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        String result = StringUtils.EMPTY;
        for (final File currPDF : pdfDirectory.listFiles()) {
            try {
                if (currPDF.getName().toLowerCase().endsWith(".pdf")) {
                    List<BibDataSet> articles = new ArrayList<BibDataSet>();
                    List<PatentItem> patents = new ArrayList<PatentItem>();
                    result = getEngine().processAllCitationsInPDFPatent(
                            pGbdArgs.getPath2Input() +
                                    File.separator + currPDF.getName(),
                            articles,
                            patents,
                            0,
                            false);
                    if (currPDF.getName().endsWith(".pdf")) {
                        IOUtilities.writeInFile(
                                pGbdArgs.getPath2Output() + File.separator
                                        + new File(currPDF.getAbsolutePath()).getName().replace(".pdf", ".tei.xml"),
                                result);
                    } else if (currPDF.getName().endsWith(".PDF")) {
                        IOUtilities.writeInFile(
                                pGbdArgs.getPath2Output() + File.separator
                                        + new File(currPDF.getAbsolutePath()).getName().replace(".PDF", ".tei.xml"),
                                result);
                    }
                }
            } catch (final Exception exp) {
                LOGGER.error(
                        "An error occurred while processing the file "
                                + currPDF.getAbsolutePath()
                                + ". Continuing the process for the other files",
                        exp);
            }
        }
    }

    /**
     * Process a patent available in PDF using pGbdArgs parameters.
     *
     * @param pGbdArgs The parameters.
     * @throws Exception
     */
    public void processPDFAnnotation(final GrobidMainArgs pGbdArgs) throws Exception {
        inferPdfInputPath(pGbdArgs);
        inferOutputPath(pGbdArgs);
        final File pdfDirectory = new File(pGbdArgs.getPath2Input());
        final File outDirectory = new File(pGbdArgs.getPath2Output());
        PDDocument out = null;
        PDDocument document = null;
        for (final File currPDF : pdfDirectory.listFiles()) {
            try {
                if (currPDF.getName().toLowerCase().endsWith(".pdf")) {
                    LOGGER.info("Processing: " + currPDF.getName());
                    List<String> elementWithCoords = new ArrayList();
                    elementWithCoords.add("ref");
                    elementWithCoords.add("biblStruct");

                    GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
                            .consolidateCitations(1)
                            .generateTeiCoordinates(elementWithCoords)
                            .build();

                    Document teiDoc = getEngine().fullTextToTEIDoc(currPDF, null, null, config);
                    document = PDDocument.load(currPDF);
                    //If no pages, skip the document
                    if (document.getNumberOfPages() > 0) {
                        DocumentSource documentSource = teiDoc.getDocumentSource();
                        out = CitationsVisualizer.annotatePdfWithCitations(document, teiDoc, null);
                    } else {
                        throw new RuntimeException("Cannot identify any pages in the input document. "
                                +
                                "The document cannot be annotated. Please check whether the document is valid or the logs.");
                    }

                    if (out != null) {
                        File outputFile = null;
                        if (outDirectory.getPath().equals(pdfDirectory.getPath()))
                            outputFile = new File(
                                    outDirectory.getPath() + "/" + currPDF.getName().replace(".pdf", ".grobid.pdf"));
                        else
                            outputFile = new File(outDirectory.getPath() + "/" + currPDF.getName());
                        out.save(outputFile);
                        System.out.println("Saved: " + outputFile.getPath());
                    }
                }
            } catch (final Exception exp) {
                LOGGER.error(
                        "An error occurred while processing the file "
                                + currPDF.getAbsolutePath()
                                + ". Continuing the process for the other files",
                        exp);
            } finally {
                if (document != null)
                    document.close();
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * List the engine methods that can be called.
     *
     * @return List<String> containing the list of the methods.
     */
    public final static List<String> getUsableMethods() {
        final Class<?> pClass = ProcessEngine.class;
        final List<String> availableMethods = new ArrayList<String>();
        for (final Method method : pClass.getMethods()) {
            if (isUsableMethod(method.getName())) {
                availableMethods.add(method.getName());
            }
        }
        return availableMethods;
    }

    /**
     * Check if the method is usable.
     *
     * @param pMethod method name.
     * @return if it is usable
     */
    protected final static boolean isUsableMethod(final String pMethod) {
        boolean isUsable = StringUtils.equals("wait", pMethod);
        isUsable |= StringUtils.equals("equals", pMethod);
        isUsable |= StringUtils.equals("toString", pMethod);
        isUsable |= StringUtils.equals("hashCode", pMethod);
        isUsable |= StringUtils.equals("getClass", pMethod);
        isUsable |= StringUtils.equals("notify", pMethod);
        isUsable |= StringUtils.equals("notifyAll", pMethod);
        isUsable |= StringUtils.equals("isUsableMethod", pMethod);
        isUsable |= StringUtils.equals("getUsableMethods", pMethod);
        isUsable |= StringUtils.equals("inferPdfInputPath", pMethod);
        isUsable |= StringUtils.equals("inferOutputPath", pMethod);
        isUsable |= StringUtils.equals("close", pMethod);
        return !isUsable;
    }

    /**
     * Infer the input path for pdfs if not given in arguments.
     *
     * @param pGbdArgs The GrobidArgs.
     */
    protected final static void inferPdfInputPath(final GrobidMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Input() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the input directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Input(tmpFilePath);
        }
    }

    /**
     * Infer the output path if not given in arguments.
     *
     * @param pGbdArgs The GrobidArgs.
     */
    protected final static void inferOutputPath(final GrobidMainArgs pGbdArgs) {
        String tmpFilePath;
        if (pGbdArgs.getPath2Output() == null) {
            tmpFilePath = new File(".").getAbsolutePath();
            LOGGER.info("No path set for the output directory. Using: " + tmpFilePath);
            pGbdArgs.setPath2Output(tmpFilePath);
        }
    }

}
