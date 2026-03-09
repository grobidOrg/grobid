package org.grobid.trainer.evaluation;

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import me.tongfei.progressbar.DelegatingProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.evaluation.utilities.FieldSpecification;
import org.grobid.trainer.evaluation.utilities.FieldSpecificationFlavors;
import org.grobid.trainer.evaluation.utilities.NamespaceContextMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import scala.Option;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

//import org.apache.log4j.xml.DOMConfigurator;

/**
 * Evaluation against native XML documents. This is an end-to-end evaluation involving
 * complete document processing, and therefore a complete set of sequence labelling models.
 *
 */
public class EndToEndEvaluation {
    private static String xmlInputPath = null;
    private Engine engine = null;

    public static final int GROBID = 0;
    public static final int PDFX = 1;
    public static final int CERMINE = 2;

    public static final int HEADER = 0;
    public static final int CITATION = 1;
    public static final int FULLTEXT = 2;

    public double fileRatio = 1.0;

    public static final double minLevenshteinDistance = 0.8;
    public static final double minRatcliffObershelpSimilarity = 0.95;

    // the list of labels considered for the evaluation
    private List<String> headerLabels = null;
    private List<String> fulltextLabels = null;
    private List<String> citationsLabels = null;

    // the list of fields considered for the evaluation
    private List<FieldSpecification> headerFields = null;
    private List<FieldSpecification> fulltextFields = null;
    private List<FieldSpecification> citationsFields = null;

    // the type of evaluation XML data - NLM or TEI (obtained via Pub2TEI)
    private String inputType = null;
    private GrobidModels.Flavor flavor = null;

    private class GrobidEndToEndTask implements Callable<Boolean> {
        private final File pdfFile;
        private final GrobidModels.Flavor flavor;

        public GrobidEndToEndTask(File pdfFile, GrobidModels.Flavor flavor) {
            this.pdfFile = pdfFile;
            this.flavor = flavor;
        }

        @Override
        public Boolean call() {
            boolean success = true;
            Engine engine = null;
            try {
                engine = Engine.getEngine(true);
                GrobidAnalysisConfig config =
                    GrobidAnalysisConfig.builder()
                        .consolidateHeader(1)
                        .consolidateCitations(0)
                        .consolidateFunders(0)
                        .withPreprocessImages(true)
                        .withSentenceSegmentation(false)
                        .flavor(flavor)
                        .build();

                String tei = engine.fullTextToTEI(this.pdfFile, flavor, config);
                String replacement = ".fulltext.tei.xml";
                if (flavor != null) {
                    replacement = ".fulltext." + flavor.getPlainLabel() + ".tei.xml";
                }

                // write the result in the same directory
                File resultTEI = Paths.get(
                    pdfFile.getParent(),
                    pdfFile.getName().replace(".pdf", replacement)
                ).toFile();
                FileUtils.writeStringToFile(resultTEI, tei, "UTF-8");

            } catch (NoSuchElementException nseExp) {
                System.out.println("Could not get an engine from the pool within configured time.");
                System.out.println("Could not process: " + this.pdfFile.getPath());
            } catch (IOException e) {
                System.out.println("DeLFT model labelling failed for file " + this.pdfFile.getPath());
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Error when processing: " + this.pdfFile.getPath());
                e.printStackTrace();
                success = false;
            } finally {
                if (engine != null) {
                    GrobidPoolingFactory.returnEngine(engine);
                }
            }

            return success;
        }
    }

    public EndToEndEvaluation(String path, String inType) {
        this(path, inType, null);
    }

    public EndToEndEvaluation(String path, String inType, GrobidModels.Flavor flavor) {
        this.xmlInputPath = path;
        this.inputType = inType;
        this.flavor = flavor;

        File xmlInputFile = new File(path);
        if (!xmlInputFile.exists()) {
            System.out.println("Path to evaluation (gold) XML data is not valid !");
            xmlInputPath = null;
        }

        try {
            GrobidProperties.getInstance();
            System.out.println(">>>>>>>> GROBID_HOME=" + GrobidProperties.getGrobidHome());

            engine = GrobidFactory.getInstance().createEngine();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // initialize the field specifications and label list
        headerFields = new ArrayList<>();
        fulltextFields = new ArrayList<>();
        citationsFields = new ArrayList<>();

        headerLabels = new ArrayList<>();
        fulltextLabels = new ArrayList<>();
        citationsLabels = new ArrayList<>();

        if (flavor == null) {
            FieldSpecification.setUpFields(headerFields, fulltextFields, citationsFields,
                headerLabels, fulltextLabels, citationsLabels);
        } else if (flavor == GrobidModels.Flavor.ARTICLE_LIGHT) {
            FieldSpecificationFlavors.setUpFields(
                headerFields,
                new ArrayList<>(),
                new ArrayList<>(),
                headerLabels,
                new ArrayList<>(),
                new ArrayList<>()
            );
        } else if (flavor == GrobidModels.Flavor.ARTICLE_LIGHT_WITH_REFERENCES) {
            FieldSpecificationFlavors.setUpFields(
                headerFields,
                new ArrayList<>(),
                citationsFields,
                headerLabels,
                new ArrayList<>(),
                citationsLabels
            );
        }
    }

    private static ProgressBar createProgressBar(String taskName, long initialMax) {
        String mode = System.getProperty("grobid.progressbar", "auto");
        boolean useSimple;
        if ("simple".equals(mode) || "false".equals(mode)) {
            useSimple = true;
        } else if ("fancy".equals(mode)) {
            useSimple = false;
        } else {
            // "auto": System.console() is null when not a real TTY
            useSimple = (System.console() == null);
        }

        if (useSimple) {
            return new ProgressBarBuilder()
                .setTaskName(taskName)
                .setInitialMax(initialMax)
                .setUpdateIntervalMillis(30000)
                .setConsumer(new DelegatingProgressBarConsumer(System.err::println))
                .build();
        } else {
            return new ProgressBar(taskName, initialMax);
        }
    }

    public String evaluationGrobid(boolean forceRun, StringBuilder reportMD) throws Exception {
        if (xmlInputPath == null) {
            throw new GrobidResourceException("Path to evaluation (gold) XML data is not correctly set");
        }

        // text report for console
        StringBuilder report = new StringBuilder();

        if (forceRun) {
            // we run Grobid full text extraction on the PubMedCentral data
            File input = new File(xmlInputPath);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles((dir, name) -> {
                File localDir = new File(dir.getAbsolutePath() + File.separator + name);
                return localDir.isDirectory();
            });

            if (refFiles == null) {
                report.append("No file in dataset");
                return report.toString();
            }

            int n = 0;
            long start = System.currentTimeMillis();
            int fails = 0;

            ExecutorService executor = Executors.newFixedThreadPool(GrobidProperties.getInstance().getMaxConcurrency() - 1);
            List<Future<Boolean>> results = new ArrayList<>();

            if (refFiles.length > 0) {
                // this will preload the models, so that the model loading messages don't mess with the progress bar
                engine = Engine.getEngine(true);
            }

            for (File dir : refFiles) {

                // get the PDF file in the directory
                File[] refFiles2 = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".pdf") || name.endsWith(".PDF");
                    }
                });

                if (refFiles2 == null || refFiles2.length == 0) {
                    System.out.println("warning: no PDF found under " + dir.getPath());
                    continue;
                }
                if (refFiles2.length != 1) {
                    System.out.println("warning: more than one PDF found under " + dir.getPath());
                    System.out.println("processing only the first one...");
                }

                final File pdfFile = refFiles2[0];
                Future<Boolean> future = executor.submit(new GrobidEndToEndTask(pdfFile, this.flavor));
                results.add(future);
                n++;
            }

            //executor.awaitTermination(5, TimeUnit.SECONDS);

            System.out.println("\n");
            try (ProgressBar pb = createProgressBar("PDF processing", refFiles.length)) {
                for (Future<Boolean> result : results) {
                    try {
                        Boolean success = result.get();
                        if (!success)
                            fails++;
                        pb.step();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }

            executor.shutdown();

            System.out.println("\n-------------> GROBID failed on " + fails + " PDF\n");
            double processTime = ((double) System.currentTimeMillis() - start) / 1000.0;

            System.out.println(n + " PDF files processed in " +
                processTime + " seconds, " + ((double) processTime) / n + " seconds per PDF file\n");
        }

        // evaluation of the run
        long start = System.currentTimeMillis();

        if (CollectionUtils.isNotEmpty(headerFields)) {
            report.append("\n======= Header metadata ======= \n");
            reportMD.append("\n## Header metadata \n");
            report.append(evaluationRun(GROBID, HEADER, reportMD));
        }

        if (CollectionUtils.isNotEmpty(citationsFields)) {
            report.append("\n======= Citation metadata ======= \n");
            reportMD.append("\n## Citation metadata \n");
            report.append(evaluationRun(GROBID, CITATION, reportMD));
        }

        if (CollectionUtils.isNotEmpty(fulltextFields)) {
            report.append("\n======= Fulltext structures ======= \n");
            reportMD.append("\n## Fulltext structures \n\n");
            reportMD.append("Fulltext structure contents are complicated to capture from JATS NLM files. They are often normalized and different from the actual PDF content and are can be inconsistent from one document to another. The scores of the following metrics are thus not very meaningful in absolute term, in particular for the strict matching (textual content of the srtructure can be very long). As relative values for comparing different models, they seem however useful.\n\n");
            report.append(evaluationRun(GROBID, FULLTEXT, reportMD));
        }

        System.out.println("Evaluation metrics produced in " +
            (System.currentTimeMillis() - start) / (1000.00) + " seconds");
        reportMD.append("Evaluation metrics produced in " +
            (System.currentTimeMillis() - start) / (1000.00) + " seconds\n");

        return report.toString();
    }

    public String evaluationPDFX(boolean forceRun, StringBuilder reportMD) throws Exception {
        if (xmlInputPath == null) {
            throw new GrobidResourceException("Path to PubMedCentral is not correctly set");
        }

        // text report for console
        StringBuilder report = new StringBuilder();

        if (forceRun) {
            // we run here PDFX online call on the PDF files...
            // TBD
            // ...
        }

        // evaluation of the run
        report.append("\n======= Header metadata ======= \n");
        reportMD.append("\n## Header metadata \n\n");
        report.append(evaluationRun(this.PDFX, this.HEADER, reportMD));

        report.append("\n======= Citation metadata ======= \n");
        reportMD.append("\n## Citation metadata \n\n");
        report.append(evaluationRun(this.PDFX, this.CITATION, reportMD));

        report.append("\n======= Fulltext structures ======= \n");
        reportMD.append("\n## Fulltext structures \n\n");
        report.append(evaluationRun(this.PDFX, this.FULLTEXT, reportMD));

        return report.toString();
    }

    public String evaluationCermine(boolean forceRun, StringBuilder reportMD) throws Exception {
        if (xmlInputPath == null) {
            throw new GrobidResourceException("Path to PubMedCentral is not correctly set");
        }

        // text report for console
        StringBuilder report = new StringBuilder();

        if (forceRun) {
            // we run here CERMINE on the PDF files...
            // TBD
            // ...
        }

        // evaluation of the run
        report.append("\n======= Header metadata ======= \n");
        reportMD.append("\n## Header metadata \n\n");
        report.append(evaluationRun(CERMINE, HEADER, reportMD));

        report.append("\n======= Citation metadata ======= \n");
        reportMD.append("\n## Citation metadata \n\n");
        report.append(evaluationRun(CERMINE, CITATION, reportMD));

        report.append("\n======= Fulltext structures ======= \n");
        reportMD.append("\n## Fulltext structures \n\n");
        report.append(evaluationRun(CERMINE, FULLTEXT, reportMD));

        return report.toString();
    }

    /**
     * This method removes the fields from the evaluation specifications and labels
     * NOTE: This modifies the fieldSpecification and labelSpecification lists
     *
     * @param listFieldNamesToRemove list of fields names to be removed
     * @param fieldSpecification     field specification list where the fields needs to be removed
     * @param labelsSpecification    field specification labels list where the fields needs to be removed
     */
    protected static void removeFieldsFromEvaluation(List<String> listFieldNamesToRemove, List<FieldSpecification> fieldSpecification, List<String> labelsSpecification) {

        for (String fieldNameToRemove : listFieldNamesToRemove) {
            List<FieldSpecification> toRemove = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fieldSpecification)) {
                for (FieldSpecification field : fieldSpecification) {
                    if (listFieldNamesToRemove.contains(field.fieldName)) {
                        toRemove.add(field);
                    }
                }
            }

            if (toRemove.size() > 0) {
                labelsSpecification.remove(fieldNameToRemove);
                for (FieldSpecification fulltextField : toRemove) {
                    fieldSpecification.remove(fulltextField);
                }
            }
        }
    }

    private String evaluationRun(int runType, int sectionType, StringBuilder reportMD) {
        if ((runType != this.GROBID) && (runType != this.PDFX) && (runType != this.CERMINE)) {
            throw new GrobidException("The run type is not valid for evaluation: " + runType);
        }
        if ((sectionType != this.HEADER) && (sectionType != this.CITATION) && (sectionType != this.FULLTEXT)) {
            throw new GrobidException("The section type is not valid for evaluation: " + sectionType);
        }

        // text report for console
        StringBuilder report = new StringBuilder();

        // we introduce four string-matching comparisons variants for different levels of
        // fidelity between observed and expected strings, in line with other evaluations
        // in the literature:
        // - strict, i.e. exact match
        // - soft, matching ignoring punctuations, character case and extra space characters
        // - Levenshtein distance (relative to the max length of fields)
        // - Ratcliff/Obershelp similarity
        // These variants only apply to textual fields, not numerical and dates fields
        // (such as volume, issue, dates).

        Stats strictStats = new Stats();
        Stats softStats = new Stats();
        Stats levenshteinStats = new Stats();
        Stats ratcliffObershelpStats = new Stats();

        Stats documentLevelStatementsRatioStat = new Stats();

        List<String> labels = null;
        List<FieldSpecification> fields = null;

        int totalExpectedInstances = 0;
        int totalObservedInstances = 0;
        int totalCorrectInstancesStrict = 0;
        int totalCorrectInstancesSoft = 0;
        int totalCorrectInstancesLevenshtein = 0;
        int totalCorrectInstancesRatcliffObershelp = 0;

        int totalExpectedReferences = 0;
        int totalObservedReferences = 0;

        int totalExpectedCitations = 0;
        int totalObservedCitations = 0;
        int totalCorrectObservedCitations = 0;
        int totalWrongObservedCitations = 0;

        if (sectionType == HEADER) {
            fields = headerFields;
            labels = headerLabels;
        } else if (sectionType == CITATION) {
            fields = citationsFields;
            labels = citationsLabels;
        } else if (sectionType == FULLTEXT) {
            fields = fulltextFields;
            labels = fulltextLabels;
        }

        // statics about citation matching
        int match1 = 0;
        int match2 = 0;
        int match3 = 0;
        int match4 = 0;

        if (StringUtils.containsAnyIgnoreCase(xmlInputPath, "pmc", "plos", "elife")) {
            // for PMC files, we further specify the NLM type: some fields might be encoded but not in the document (like PMID, DOI)
            removeFieldsFromEvaluation(Arrays.asList("doi", "pmid", "pmcid"), citationsFields, citationsLabels);
        }

        if (StringUtils.containsIgnoreCase(xmlInputPath, "elife")) {
            // keywords are present in the eLife XML, but not in the PDF !
            removeFieldsFromEvaluation(Arrays.asList("keywords"), headerFields, headerLabels);

            // Contributions in eLife are not text elements, but a combination between the author and some text elements,
            //  since we cannot easily put them together it's better to ignore them for the time being.
            removeFieldsFromEvaluation(Arrays.asList("contribution_stmt", "conflict_stmt"), fulltextFields, fulltextLabels);
        }

        if (StringUtils.containsIgnoreCase(xmlInputPath, "pmc")) {
            // remove availability and funding statements from PMC (not covered, and it would make metrics not comparable over time)
            removeFieldsFromEvaluation(Arrays.asList("availability_stmt", "funding_stmt", "conflict_stmt", "contribution_stmt"), fulltextFields, fulltextLabels);
        }

        if (StringUtils.containsIgnoreCase(xmlInputPath, "plos")) {
            // Contributions in PLOS are not text elements but attributes in the author list.
            removeFieldsFromEvaluation(Arrays.asList("contribution_stmt"), fulltextFields, fulltextLabels);
        }

        File input = new File(xmlInputPath);
        // we process all tei files in the output directory
        File[] refFiles = input.listFiles((dir, name) -> {
            File localDir = new File(dir.getAbsolutePath() + File.separator + name);
            return localDir.isDirectory();
        });

        if (refFiles == null) {
            report.append("No file in dataset");
            return report.toString();
        }

        // get a factory for SAX parsers
        SAXParserFactory spf = SAXParserFactory.newInstance();
        Random rand = new Random();
        int nbFile = 0;

        String typeEval = "";
        if (sectionType == HEADER)
            typeEval = "header";
        if (sectionType == FULLTEXT)
            typeEval = "full text";
        if (sectionType == CITATION)
            typeEval = "citation";

        System.out.println("\n");
        try (ProgressBar pb = createProgressBar("Evaluation " + typeEval, refFiles.length)) {

            for (File dir : refFiles) {
                pb.step();

                if (!dir.isDirectory())
                    continue;

                // file ratio filtering
                double random = rand.nextDouble();
                if (random > fileRatio) {
                    continue;
                }

                // get the gold file in the directory
                File[] refFiles2 = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".nxml") || name.endsWith(".pub2tei.tei.xml");
                    }
                });

                if (refFiles2 == null || refFiles2.length == 0) {
                    // in the case of a bioRxiv NLM/JATS file, we have an .xml extension
                    refFiles2 = dir.listFiles(new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".xml") && !name.endsWith(".tei.xml");
                        }
                    });

                    if (refFiles2 == null || refFiles2.length == 0) {
                        System.out.println("warning: no evaluation (gold) XML data file found under " + dir.getPath());
                        continue;
                    }
                }

                if (refFiles2.length != 1) {
                    System.out.println("warning: more than one evaluation (gold) XML data files found under " + dir.getPath());
                    for (int m = 0; m < refFiles2.length; m++) {
                        System.out.println(refFiles2[m].getPath());
                    }
                    System.out.println("processing only the first one...");
                }

                File goldFile = refFiles2[0];

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setValidating(false);

                try {
                    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                    docBuilder.setEntityResolver((publicId, systemId) -> new InputSource(
                        new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()))); // swap in a dummy resolver to neutralise the online DTD
                    Document gold = docBuilder.parse(goldFile);

                    // get the results of the evaluated tool for this file
                    if (runType == GROBID) {
                        final String fileSuffix;
                        if (this.flavor != null) {
                            fileSuffix = ".fulltext." + flavor.getPlainLabel() + ".tei.xml";
                        } else {
                            fileSuffix = ".fulltext.tei.xml";
                        }

                        // results are produced in a TEI file
                        File[] refFiles3 = dir.listFiles((dir1, name) -> name.endsWith(fileSuffix));

                        if ((refFiles3 == null) || (refFiles3.length == 0)) {
                            System.out.println("warning: no Grobid TEI file found under " + dir.getPath());
                            continue;
                        }

                        if (refFiles3.length != 1) {
                            System.out.println("warning: more than one Grobid TEI files found under " + dir.getPath());
                            System.out.println("processing only the first one...");
                        }

                        File teiFile = refFiles3[0];
                        Document tei = docBuilder.parse(teiFile);

                        XPathFactory xpf = XPathFactory.newInstance();
                        XPath xp = xpf.newXPath();
                        HashMap map = new HashMap();
                        // explicit indication of the default namespace
                        map.put("tei", "http://www.tei-c.org/ns/1.0");

                        Map<String, String> mappings = new HashMap<>();
                        mappings.put("tei", "http://www.tei-c.org/ns/1.0");
                        xp.setNamespaceContext(new NamespaceContextMap(mappings));

                        if (sectionType == CITATION) {
                            // we start by identifying each expected citation
                            // the first FieldSpecification object for the citation is the base path for
                            // each citation structure in the corresponding XML
                            FieldSpecification base = fields.get(0);

                            String path = null;
                            if (inputType.equals("nlm"))
                                path = base.nlmPath.get(0);
                            else
                                path = base.grobidPath.get(0);

                            NodeList nodeList = (NodeList) xp.compile(path).
                                evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                            int nbCitationsGold = nodeList.getLength();
                            totalExpectedInstances += nbCitationsGold;

                            List<Map<String, List<String>>> goldCitations =
                                new ArrayList<Map<String, List<String>>>();

                            // "signature" of the citations for this file
                            // level 1 signature: titre + date
                            List<String> goldCitationSignaturesLevel1 = new ArrayList<>();

                            // level 2 signature: all authors names + date
                            List<String> goldCitationSignaturesLevel2 = new ArrayList<>();

                            // level 3 signature: journal + volume + page
                            List<String> goldCitationSignaturesLevel3 = new ArrayList<>();

                            // level 4 signature:  "fuzzy titre" + date + at least one of auteurs or first page
                            List<String> goldCitationSignaturesLevel4 = new ArrayList<>();

                            // map between citation id from gold and from grobid (if matching between the two citations)
                            Map<String, String> idMap = new HashMap<>();
                            Map<String, String> reverseIdMap = new HashMap<>();
                            List<String> goldIds = new ArrayList<>();

                            for (int i = 0; i < nodeList.getLength(); i++) {
                                // sometimes we just have the raw citation bellow this, so we will have to further
                                // test if we have something structured
                                Map<String, List<String>> fieldsValues = new HashMap<>();
                                Node node = nodeList.item(i);
                                int p = 0;
                                for (FieldSpecification field : fields) {
                                    String fieldName = field.fieldName;
                                    if (fieldName.equals("base")) {
                                        //p++;
                                        continue;
                                    }
                                    List<String> subpaths = null;
                                    if (inputType.equals("nlm")) {
                                        subpaths = field.nlmPath;
                                    } else if (inputType.equals("tei")) {
                                        subpaths = field.grobidPath;
                                    }

                                    if (subpaths == null)
                                        continue;

                                    for (String subpath : subpaths) {
                                        NodeList nodeList2 = (NodeList) xp.compile(subpath).
                                            evaluate(node, XPathConstants.NODESET);

                                        List<String> goldResults = new ArrayList<>();
                                        for (int j = 0; j < nodeList2.getLength(); j++) {
                                            String content = nodeList2.item(j).getNodeValue();
                                            if ((content != null) && (content.trim().length() > 0)) {
                                                if (fieldName.equals("doi") || fieldName.equals("pmid") || fieldName.equals("pmcid")) {
                                                    content = identifierNormalization(content);
                                                }
                                                goldResults.add(content);
                                            }
                                        }

                                        if (goldResults.size() > 0) {
                                            fieldsValues.put(fieldName, goldResults);
                                            if (!fieldName.equals("id")) {
                                                strictStats.incrementExpected(fieldName);
                                                softStats.incrementExpected(fieldName);
                                                levenshteinStats.incrementExpected(fieldName);
                                                ratcliffObershelpStats.incrementExpected(fieldName);
                                            }
                                        }
                                    }

                                    p++;
                                }

                                // signature for this citation
                                String goldTitle = "";
                                List<String> goldResults = fieldsValues.get("title");
                                if (goldResults != null) {
                                    for (String res : goldResults) {
                                        goldTitle += " " + res;
                                    }
                                }
                                goldTitle = basicNormalization(goldTitle);
                                String goldTitleSoft = removeFullPunct(goldTitle);

                                // source title / inTitle information
                                String goldInTitle = "";
                                List<String> inTitleResults = fieldsValues.get("inTitle");
                                if (inTitleResults != null) {
                                    for (String res : inTitleResults) {
                                        goldInTitle += " " + res;
                                    }
                                }
                                goldInTitle = basicNormalization(goldInTitle);
                                String goldInTitleSoft = removeFullPunct(goldInTitle);

                                // first author last name only
                                List<String> authorResults = fieldsValues.get("first_author");
                                String goldAuthor = "";
                                if ((authorResults != null) && (authorResults.size() > 0))
                                    goldAuthor = authorResults.get(0);
                                goldAuthor = basicNormalization(goldAuthor);
                                String goldAuthorSoft = removeFullPunct(goldAuthor);

                                // all authors last names
                                String goldAuthors = "";
                                List<String> authorsResults = fieldsValues.get("authors");
                                if ((authorsResults != null) && (authorsResults.size() > 0)) {
                                    for (String aut : authorsResults)
                                        goldAuthors += aut;
                                }
                                goldAuthors = basicNormalization(goldAuthors);
                                String goldAuthorsSoft = removeFullPunct(goldAuthors);

                                // date of publication
                                List<String> dateResults = fieldsValues.get("date");
                                String goldDate = "";
                                if ((dateResults != null) && (dateResults.size() > 0))
                                    goldDate = dateResults.get(0);
                                goldDate = basicNormalization(goldDate);

                                // volume
                                List<String> volumeResults = fieldsValues.get("volume");
                                String goldVolume = "";
                                if ((volumeResults != null) && (volumeResults.size() > 0))
                                    goldVolume = volumeResults.get(0);
                                goldVolume = basicNormalization(goldVolume);

                                // first page
                                List<String> pageResults = fieldsValues.get("page");
                                String goldPage = "";
                                if ((pageResults != null) && (pageResults.size() > 0))
                                    goldPage = pageResults.get(0);
                                goldPage = basicNormalization(goldPage);

                                // identifier
                                List<String> idResults = fieldsValues.get("id");
                                String goldId = "";
                                if ((idResults != null) && (idResults.size() > 0))
                                    goldId = idResults.get(0);
                                goldId = basicNormalization(goldId);
                                goldIds.add(goldId);
                                /*
                                 * We introduce 4 sequential alignment rules to match an extracted citation with an expected citation.
                                 * If the first rule is not working, we test the second one, and so on until the last one.
                                 * If all rules fail, the extracted citation is considered as false positive for its non-empty fields.
                                 * - first rule: matching of the "soft" title (title ignoring case, punctuation ans space mismatches) and year
                                 * - second rule: matching all of "soft" authors and year
                                 * - third rule: matching of "soft" inTitle (title of Journal or Conference), volume and first page
                                 * - forth rule: matching of first author last name and title, or inTitle if title is empty
                                 */
                                String signature1 = null;
                                if ((goldTitleSoft.length() > 0) && (goldDate.length() > 0)) {
                                    signature1 = goldTitleSoft + goldDate;
                                    //signature1 = signature1.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String signature2 = null;
                                if ((goldAuthorsSoft.length() > 0) && (goldDate.length() > 0)) {
                                    signature2 = goldAuthorsSoft + goldDate;
                                    //signature2 = signature2.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String signature3 = null;
                                if ((goldInTitleSoft.length() > 0) && (goldVolume.length() > 0) && (goldPage.length() > 0)) {
                                    signature3 = goldInTitleSoft + goldVolume + goldPage;
                                    //signature3 = signature3.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String signature4 = null;
                                if (((goldInTitleSoft.length() > 0) || (goldTitleSoft.length() > 0))
                                    && (goldAuthorSoft.length() > 0)) {
                                    if (goldTitleSoft.length() > 0)
                                        signature4 = goldAuthorSoft + goldTitleSoft;
                                    else
                                        signature4 = goldAuthorSoft + goldInTitleSoft;
                                }

                                goldCitationSignaturesLevel1.add(signature1);
                                goldCitationSignaturesLevel2.add(signature2);
                                goldCitationSignaturesLevel3.add(signature3);
                                goldCitationSignaturesLevel4.add(signature4);
                                goldCitations.add(fieldsValues);

                            }

                            // get the Grobid citations
                            path = base.grobidPath.get(0);
                            nodeList = (NodeList) xp.compile(path).
                                evaluate(tei.getDocumentElement(), XPathConstants.NODESET);
                            int nbCitationsGrobid = nodeList.getLength();

//if (nbCitationsGold != nbCitationsGrobid)
//System.out.println(dir.getPath() + " references: " + nbCitationsGold + " (expected) / " + nbCitationsGrobid + " (grobid)");

                            totalObservedInstances += nbCitationsGrobid;
                            List<Map<String, List<String>>> grobidCitations =
                                new ArrayList<Map<String, List<String>>>();
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Map<String, List<String>> fieldsValues = new HashMap<String, List<String>>();
                                Node node = nodeList.item(i);
                                int p = 0;
                                for (FieldSpecification field : fields) {
                                    String fieldName = field.fieldName;
                                    if (fieldName.equals("base")) {
                                        //p++;
                                        continue;
                                    }
                                    for (String subpath : field.grobidPath) {
                                        NodeList nodeList2 = (NodeList) xp.compile(subpath).
                                            evaluate(node, XPathConstants.NODESET);
                                        List<String> grobidResults = new ArrayList<>();
                                        for (int j = 0; j < nodeList2.getLength(); j++) {
                                            String content = nodeList2.item(j).getNodeValue();
                                            if ((content != null) && (content.trim().length() > 0)) {
                                                if (fieldName.equals("doi") || fieldName.equals("pmid") || fieldName.equals("pmcid")) {
                                                    content = identifierNormalization(content);
                                                }
                                                grobidResults.add(content);
                                            }
                                        }
                                        if (grobidResults.size() > 0) {
                                            fieldsValues.put(fieldName, grobidResults);
                                        }
                                    }
                                    p++;
                                }
                                grobidCitations.add(fieldsValues);
                            }

                            for (Map<String, List<String>> grobidCitation : grobidCitations) {
                                String grobidTitle = "";

                                List<String> titleResults = grobidCitation.get("title");
                                if (titleResults != null) {
                                    for (String res : titleResults) {
                                        grobidTitle += " " + res;
                                    }
                                }
                                grobidTitle = basicNormalization(grobidTitle);
                                String grobidTitleSoft = removeFullPunct(grobidTitle);

                                List<String> inTitleResults = grobidCitation.get("inTitle");
                                String grobidInTitle = "";
                                if (inTitleResults != null) {
                                    for (String res : inTitleResults) {
                                        grobidInTitle += " " + res;
                                    }
                                }
                                grobidInTitle = basicNormalization(grobidInTitle);
                                String grobidInTitleSoft = removeFullPunct(grobidInTitle);

                                // first author last name only
                                List<String> authorResults = grobidCitation.get("first_author");
                                String grobidAuthor = "";
                                if ((authorResults != null) && (authorResults.size() > 0))
                                    grobidAuthor = authorResults.get(0);
                                grobidAuthor = basicNormalization(grobidAuthor);
                                String grobidAuthorSoft = removeFullPunct(grobidAuthor);

                                // all authors last names
                                String grobidAuthors = "";
                                List<String> authorsResults = grobidCitation.get("authors");
                                if ((authorsResults != null) && (authorsResults.size() > 0)) {
                                    for (String aut : authorsResults)
                                        grobidAuthors += aut;
                                }
                                grobidAuthors = basicNormalization(grobidAuthors);
                                String grobidAuthorsSoft = removeFullPunct(grobidAuthors);

                                // date of publication
                                List<String> dateResults = grobidCitation.get("date");
                                String grobidDate = "";
                                if ((dateResults != null) && (dateResults.size() > 0))
                                    grobidDate = dateResults.get(0);
                                grobidDate = basicNormalization(grobidDate);

                                // volume
                                List<String> volumeResults = grobidCitation.get("volume");
                                String grobidVolume = "";
                                if ((volumeResults != null) && (volumeResults.size() > 0))
                                    grobidVolume = volumeResults.get(0);
                                grobidVolume = basicNormalization(grobidVolume);

                                // first page
                                List<String> pageResults = grobidCitation.get("page");
                                String grobidPage = "";
                                if ((pageResults != null) && (pageResults.size() > 0))
                                    grobidPage = pageResults.get(0);
                                grobidPage = basicNormalization(grobidPage);

                                // identifier
                                List<String> idResults = grobidCitation.get("id");
                                String grobidId = "";
                                if ((idResults != null) && (idResults.size() > 0))
                                    grobidId = idResults.get(0);
                                grobidId = basicNormalization(grobidId);

                                // DOI
                                List<String> doiResults = grobidCitation.get("doi");
                                String grobidDOI = "";
                                if ((doiResults != null) && (doiResults.size() > 0))
                                    grobidDOI = doiResults.get(0);
                                grobidDOI = identifierNormalization(grobidDOI);

                                // PMID
                                List<String> pmidResults = grobidCitation.get("pmid");
                                String grobidPMID = "";
                                if ((pmidResults != null) && (pmidResults.size() > 0))
                                    grobidPMID = pmidResults.get(0);
                                grobidPMID = identifierNormalization(grobidPMID);

                                // PMCID
                                List<String> pmcidResults = grobidCitation.get("pmcid");
                                String grobidPMCID = "";
                                if ((pmcidResults != null) && (pmcidResults.size() > 0))
                                    grobidPMCID = pmcidResults.get(0);
                                grobidPMCID = identifierNormalization(grobidPMCID);

                                String grobidSignature1 = null;
                                if ((grobidTitleSoft.length() > 0) && (grobidDate.length() > 0)) {
                                    grobidSignature1 = grobidTitleSoft + grobidDate;
                                    //grobidSignature1 = grobidSignature1.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String grobidSignature2 = null;
                                if ((grobidAuthorsSoft.length() > 0) && (grobidDate.length() > 0)) {
                                    grobidSignature2 = grobidAuthorsSoft + grobidDate;
                                    //grobidSignature2 = grobidSignature2.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String grobidSignature3 = null;
                                if ((grobidInTitleSoft.length() > 0) && (grobidVolume.length() > 0)
                                    && (grobidPage.length() > 0)) {
                                    grobidSignature3 = grobidInTitleSoft + grobidVolume + grobidPage;
                                    //grobidSignature3 = grobidSignature3.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                String grobidSignature4 = null;
                                if (((grobidInTitleSoft.length() > 0) || (grobidTitleSoft.length() > 0))
                                    && (grobidAuthorSoft.length() > 0)) {
                                    if (grobidTitleSoft.length() > 0)
                                        grobidSignature4 = grobidAuthorSoft + grobidTitleSoft;
                                    else
                                        grobidSignature4 = grobidAuthorSoft + grobidInTitleSoft;
                                    //grobidSignature4 = grobidSignature4.replaceAll("[^\\x00-\\x7F]", "");
                                }

                                int indexGold = -1;
                                // try to match an expected citation with the signature
                                if (((grobidSignature1 != null) && (grobidSignature1.length() > 0)) ||
                                    ((grobidSignature2 != null) && (grobidSignature2.length() > 0)) ||
                                    ((grobidSignature3 != null) && (grobidSignature3.length() > 0)) ||
                                    ((grobidSignature4 != null) && (grobidSignature4.length() > 0))) {
                                    if ((grobidSignature1 != null) &&
                                        goldCitationSignaturesLevel1.contains(grobidSignature1)) {
                                        // we have a citation-level match and we can evaluate the fields
                                        indexGold = goldCitationSignaturesLevel1.indexOf(grobidSignature1);
                                        match1++;
                                    } else if ((grobidSignature2 != null) &&
                                        goldCitationSignaturesLevel2.contains(grobidSignature2)) {
                                        // we have a citation-level match and we can evaluate the fields
                                        indexGold = goldCitationSignaturesLevel2.indexOf(grobidSignature2);
                                        match2++;
                                    } else if ((grobidSignature3 != null) &&
                                        goldCitationSignaturesLevel3.contains(grobidSignature3)) {
                                        // we have a citation-level match and we can evaluate the fields
                                        indexGold = goldCitationSignaturesLevel3.indexOf(grobidSignature3);
                                        match3++;
                                    } else if ((grobidSignature4 != null) &&
                                        goldCitationSignaturesLevel4.contains(grobidSignature4)) {
                                        // we have a citation-level match and we can evaluate the fields
                                        indexGold = goldCitationSignaturesLevel4.indexOf(grobidSignature4);
                                        match4++;
                                    }

                                    if (indexGold != -1) {
                                        // we have aligned an extracted citation with an expected ones
                                        boolean allGoodStrict = true;
                                        boolean allGoodSoft = true;
                                        boolean allGoodLevenshtein = true;
                                        boolean allGoodRatcliffObershelp = true;
                                        Map<String, List<String>> goldCitation = goldCitations.get(indexGold);
                                        goldCitationSignaturesLevel1.remove(indexGold);
                                        goldCitationSignaturesLevel2.remove(indexGold);
                                        goldCitationSignaturesLevel3.remove(indexGold);
                                        goldCitationSignaturesLevel4.remove(indexGold);
                                        goldCitations.remove(indexGold);

                                        if (goldCitation.get("id") != null && goldCitation.get("id").size() > 0) {

                                            idMap.put(goldCitation.get("id").get(0), grobidId);
                                            reverseIdMap.put(grobidId, goldCitation.get("id").get(0));

                                            int p = 0;
                                            for (FieldSpecification field : fields) {
                                                String label = field.fieldName;
                                                if (label.equals("base") || label.equals("id")) {
                                                    //p++;
                                                    continue;
                                                }

                                                List<String> grobidResults = grobidCitation.get(label);
                                                String grobidResult = "";
                                                if (grobidResults != null) {
                                                    for (String res : grobidResults) {
                                                        grobidResult += " " + res;
                                                    }
                                                }
                                                grobidResult = basicNormalization(grobidResult);

                                                List<String> goldResults = goldCitation.get(label);
                                                String goldResult = "";
                                                if (goldResults != null) {
                                                    for (String res : goldResults) {
                                                        goldResult += " " + res;
                                                    }
                                                }
                                                goldResult = basicNormalization(goldResult);

                                                // strict
                                                if ((goldResult.length() > 0) && (goldResult.equals(grobidResult))) {
                                                    strictStats.incrementObserved(label);
                                                } else {
                                                    if ((grobidResult.length() > 0)) {
                                                        strictStats.incrementFalsePositive(label);
                                                        allGoodStrict = false;
                                                    } else if (goldResult.length() > 0) {
                                                        strictStats.incrementFalseNegative(label);
                                                        allGoodStrict = false;
                                                    }
                                                }

                                                // soft
                                                String goldResultSoft = goldResult;
                                                String grobidResultSoft = grobidResult;
                                                if (field.isTextual) {
                                                    goldResultSoft = removeFullPunct(goldResult);
                                                    grobidResultSoft = removeFullPunct(grobidResult);
                                                }
                                                if ((goldResultSoft.length() > 0) &&
                                                    (goldResultSoft.equals(grobidResultSoft))) {
                                                    softStats.incrementObserved(label);
                                                } else {
                                                    if (grobidResultSoft.length() > 0) {
                                                        softStats.incrementFalsePositive(label);
                                                        allGoodSoft = false;
                                                    } else if (goldResultSoft.length() > 0) {
                                                        softStats.incrementFalseNegative(label);
                                                        allGoodSoft = false;
                                                    }
                                                }

                                                // Levenshtein
                                                double pct = 0.0;
                                                if ((goldResultSoft.length() > 0) && goldResult.equals(grobidResult))
                                                    pct = 1.0;
                                                if (field.isTextual) {
                                                    int distance =
                                                        TextUtilities.getLevenshteinDistance(goldResult, grobidResult);
                                                    // Levenshtein distance is an integer value, not a percentage... however
                                                    // articles usually introduced it as a percentage... so we report it
                                                    // following the straightforward formula:
                                                    int bigger = Math.max(goldResult.length(), grobidResult.length());
                                                    pct = (double) (bigger - distance) / bigger;
                                                }
                                                if ((goldResultSoft.length() > 0) && (pct >= minLevenshteinDistance)) {
                                                    levenshteinStats.incrementObserved(label);
                                                } else {
                                                    if (grobidResultSoft.length() > 0) {
                                                        levenshteinStats.incrementFalsePositive(label);
                                                        allGoodLevenshtein = false;
                                                    } else if (goldResultSoft.length() > 0) {
                                                        levenshteinStats.incrementFalseNegative(label);
                                                        allGoodLevenshtein = false;
                                                    }
                                                }

                                                // RatcliffObershelp
                                                Double similarity = 0.0;
                                                if ((goldResultSoft.length() > 0) && goldResult.equals(grobidResult))
                                                    similarity = 1.0;
                                                if (field.isTextual) {
                                                    if ((goldResult.length() > 0) && (grobidResult.length() > 0)) {
                                                        Option<Object> similarityObject =
                                                            RatcliffObershelpMetric.compare(goldResult, grobidResult);
                                                        if ((similarityObject != null) && (similarityObject.get() != null))
                                                            similarity = (Double) similarityObject.get();
                                                    }
                                                }
                                                if ((goldResultSoft.length() > 0) &&
                                                    (similarity >= minRatcliffObershelpSimilarity)) {
                                                    ratcliffObershelpStats.incrementObserved(label);
                                                } else {
                                                    if (grobidResultSoft.length() > 0) {
                                                        ratcliffObershelpStats.incrementFalsePositive(label);
                                                        allGoodRatcliffObershelp = false;
                                                    } else if (goldResultSoft.length() > 0) {
                                                        ratcliffObershelpStats.incrementFalseNegative(label);
                                                        allGoodRatcliffObershelp = false;
                                                    }
                                                }

                                                p++;
                                            }
                                            if (allGoodStrict) {
                                                totalCorrectInstancesStrict++;
                                            }
                                            if (allGoodSoft) {
                                                totalCorrectInstancesSoft++;
                                            }
                                            if (allGoodLevenshtein) {
                                                totalCorrectInstancesLevenshtein++;
                                            }
                                            if (allGoodRatcliffObershelp) {
                                                totalCorrectInstancesRatcliffObershelp++;
                                            }
                                        }
                                    } else {
                                        // we have a Grobid extracted citation, but no matching with
                                        // expected ones -> false positive for all the present fields
                                        int p = 0;
                                        for (FieldSpecification field : fields) {
                                            String label = field.fieldName;
                                            if (label.equals("base")) {
                                                //p++;
                                                continue;
                                            }

                                            List<String> grobidResults = grobidCitation.get(label);
                                            if ((grobidResults == null) || (grobidResults.size() == 0)) {
                                                p++;
                                                continue;
                                            }

                                            strictStats.incrementFalsePositive(label);
                                            softStats.incrementFalsePositive(label);
                                            levenshteinStats.incrementFalsePositive(label);
                                            ratcliffObershelpStats.incrementFalsePositive(label);

                                            p++;
                                        }
                                    }
                                }
                            }

                            // reference context matching
                            if ((sectionType == this.CITATION) && (runType == this.GROBID)) {
                                // list of identifiers present in the bibliographical references
                                List<String> refBibRefIds = new ArrayList<>();
                                List<String> grobidBibRefIds = new ArrayList<>();

                                String subpath = null;
                                if (inputType.equals("nlm")) {
                                    subpath = FieldSpecification.nlmBibReferenceId;
                                } else if (inputType.equals("tei")) {
                                    subpath = FieldSpecification.grobidBibReferenceId;
                                }

                                // gold
                                nodeList = (NodeList) xp.compile(subpath).
                                    evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                                //System.out.println(path + ": " + nodeList.getLength() + " nodes");
                                int nbgoldResults = nodeList.getLength();
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    refBibRefIds.add(nodeList.item(i).getNodeValue());
                                }
                                totalExpectedReferences += refBibRefIds.size();

                                // grobid
                                nodeList = (NodeList) xp.compile(FieldSpecification.grobidBibReferenceId).
                                    evaluate(tei.getDocumentElement(), XPathConstants.NODESET);
                                //System.out.println(FieldSpecification.grobidBibReferenceId + ": " + nodeList.getLength() + " nodes");
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    grobidBibRefIds.add(nodeList.item(i).getNodeValue());
                                }
                                totalObservedReferences += grobidBibRefIds.size();

                                // Map associating the identifiers present in the reference callout with their number of occurences
                                Map<String, Integer> refCalloutRefIds = new HashMap<>();
                                Map<String, Integer> grobidCalloutRefIds = new HashMap<>();

                                if (inputType.equals("nlm")) {
                                    subpath = FieldSpecification.nlmCitationContextId;
                                } else if (inputType.equals("tei")) {
                                    subpath = FieldSpecification.grobidCitationContextId;
                                }

                                // gold
                                nodeList = (NodeList) xp.compile(subpath).
                                    evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                                nbgoldResults = nodeList.getLength();
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    String localIds = nodeList.item(i).getNodeValue();
                                    if ((localIds != null) && (localIds.length() > 0)) {
                                        // we might have several identifiers, separated by space: e.g.:
                                        // <xref rid="bb0010 bb0090 bb0125 bb0135 bb0150" ref-type="bibr">Beauregard et al., 2008; Jordan and Miller, 2009;
                                        // 			Symer and Boeke, 2010; Tenaillon et al., 2010; Wolf and Goff, 2008</xref>
                                        String[] theIds = localIds.split(" ");
                                        for (int j = 0; j < theIds.length; j++) {
                                            String localId = theIds[j];
                                            localId = localId.replace("#", "");
                                            if (refCalloutRefIds.get(localId) == null)
                                                refCalloutRefIds.put(localId, Integer.valueOf(1));
                                            else {
                                                int val = refCalloutRefIds.get(localId).intValue();
                                                refCalloutRefIds.put(localId, Integer.valueOf(val + 1));
                                            }
                                            totalExpectedCitations++;
                                        }
                                    }
                                }

                                // grobid
                                nodeList = (NodeList) xp.compile(FieldSpecification.grobidCitationContextId).
                                    evaluate(tei.getDocumentElement(), XPathConstants.NODESET);
                                //System.out.println(FieldSpecification.grobidCitationContextId + ": " + nodeList.getLength() + " nodes");
                                for (int i = 0; i < nodeList.getLength(); i++) {
                                    String localId = nodeList.item(i).getNodeValue();
                                    localId = localId.replace("#", "");
                                    if ((localId != null) && (localId.length() > 0)) {
                                        if (grobidCalloutRefIds.get(localId) == null)
                                            grobidCalloutRefIds.put(localId, Integer.valueOf(1));
                                        else {
                                            int val = grobidCalloutRefIds.get(localId).intValue();
                                            grobidCalloutRefIds.put(localId, Integer.valueOf(val + 1));
                                        }
                                        totalObservedCitations++;
                                    }
                                }

                                // simple estimation of correct citation identifications by checking overlaped ids and map
                                int nbCorrect = 0;
                                int nbWrong = 0;
                                for (Map.Entry<String, Integer> entry : grobidCalloutRefIds.entrySet()) {
                                    int nbGrobidId = entry.getValue();
                                    int nbRefId = 0;
                                    if ((refCalloutRefIds != null) && (reverseIdMap.get(entry.getKey()) != null)) {
                                        if (refCalloutRefIds.get(reverseIdMap.get(entry.getKey())) != null) {
                                            nbRefId = refCalloutRefIds.get(reverseIdMap.get(entry.getKey()));
                                        }

                                        if (nbGrobidId > nbRefId) {
                                            nbWrong += nbGrobidId - nbRefId;
                                            nbCorrect += nbRefId;
                                        } else
                                            nbCorrect += nbGrobidId;
                                    } else {
                                        // all wrong matches
                                        nbWrong += nbGrobidId;
                                    }
                                }
                                totalCorrectObservedCitations += nbCorrect;
                                totalWrongObservedCitations += nbWrong;
                            }

                            // cleaning
                            strictStats.removeLabel("id");
                            softStats.removeLabel("id");
                            levenshteinStats.removeLabel("id");
                            ;
                            ratcliffObershelpStats.removeLabel("id");

                        } else if (sectionType == this.HEADER) {
                            // HEADER structures
                            int p = 0;
                            boolean allGoodStrict = true;
                            boolean allGoodSoft = true;
                            boolean allGoodLevenshtein = true;
                            boolean allGoodRatcliffObershelp = true;
                            for (FieldSpecification field : fields) {
                                String fieldName = field.fieldName;

                                List<String> grobidResults = new ArrayList<>();
                                for (String path : field.grobidPath) {
                                    NodeList nodeList = (NodeList) xp.compile(path).
                                        evaluate(tei.getDocumentElement(), XPathConstants.NODESET);
                                    for (int i = 0; i < nodeList.getLength(); i++) {
                                        grobidResults.add((nodeList.item(i).getNodeValue().replaceAll(" +", " ")));
                                    }
                                }
                                //if (!field.hasMultipleValue)
                                {
                                    String grobidResult = "";
                                    for (String res : grobidResults)
                                        grobidResult += " " + res;
                                    // basic normalisation
                                    grobidResult = basicNormalization(grobidResult);
                                    //System.out.println("Grobid: " + fieldName + ":\t" + grobidResult);
                                    grobidResults = new ArrayList<>();
                                    grobidResults.add(grobidResult);
                                }

/*if (fieldName.equals("title") && (grobidResults.size() == 0 || grobidResults.get(0).length() == 0))  
System.out.println(dir.getPath() + " no GROBID title");

if (fieldName.equals("authors") && (grobidResults.size() == 0 || grobidResults.get(0).length() == 0)) 
System.out.println(dir.getPath() + " no authors");

if (fieldName.equals("abstract") && (grobidResults.size() == 0 || grobidResults.get(0).length() == 0)) 
System.out.println(dir.getPath() + " no abstract");
*/
                                List<String> goldResults = new ArrayList<>();
                                int nbGoldResults = 0;
                                List<String> subpaths = null;
                                if (inputType.equals("nlm")) {
                                    subpaths = field.nlmPath;
                                } else if (inputType.equals("tei")) {
                                    subpaths = field.grobidPath;
                                }

                                if (subpaths == null)
                                    continue;

                                for (String path : subpaths) {
                                    NodeList nodeList = (NodeList) xp.compile(path).
                                        evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                                    //System.out.println(path + ": " + nodeList.getLength() + " nodes");
                                    nbGoldResults = nodeList.getLength();
                                    for (int i = 0; i < nodeList.getLength(); i++) {
                                        goldResults.add(nodeList.item(i).getNodeValue().replaceAll(" +", " "));
                                    }
                                }

                                //if (!field.hasMultipleValue)
                                {
                                    String goldResult = "";
                                    for (String res : goldResults)
                                        goldResult += " " + res;
                                    // basic normalisation
                                    goldResult = basicNormalization(goldResult);
                                    if (fieldName.equals("abstract")) {
                                        // some additional cleaning for abstract is required, because PMC and bioRxiv
                                        // tends to put the useless abstract title "Abstract" together with the abstract
                                        if (goldResult.toLowerCase().startsWith("abstract") || goldResult.toLowerCase().startsWith("summary")) {
                                            goldResult = goldResult.replaceAll("(?i)^(abstract)|(summary)(\\n)?( )?", "");
                                        }
                                    }
                                    //System.out.println("gold:  " + fieldName + ":\t" + goldResult);
                                    goldResults = new ArrayList<>();
                                    goldResults.add(goldResult);
                                    nbGoldResults = 1;
                                }

                                int g = 0;
                                for (String goldResult : goldResults) {
                                    String grobidResult = "";
                                    if (g < grobidResults.size())
                                        grobidResult = grobidResults.get(g);

                                    if (goldResult.trim().length() == 0 && grobidResult.trim().length() == 0) {
                                        g++;
                                        continue;
                                    }

                                    // nb expected results
                                    if (goldResult.trim().length() > 0) {
                                        strictStats.incrementExpected(fieldName);
                                        softStats.incrementExpected(fieldName);
                                        levenshteinStats.incrementExpected(fieldName);
                                        ratcliffObershelpStats.incrementExpected(fieldName);
                                    }

                                    // strict
                                    if ((goldResult.trim().length() > 0) && goldResult.equals(grobidResult)) {
                                        strictStats.incrementObserved(fieldName);
                                    } else {
/*System.out.println("gold:  " + fieldName);
System.out.println("gold:   " + goldResult);
System.out.println("grobid: " + grobidResult);*/
                                        if (grobidResult.length() > 0) {
                                            strictStats.incrementFalsePositive(fieldName);
                                            allGoodStrict = false;
                                        } else if (goldResult.length() > 0) {
                                            strictStats.incrementFalseNegative(fieldName);
                                            allGoodStrict = false;
                                        }
                                    }

                                    // soft
                                    String goldResultSoft = goldResult;
                                    String grobidResultSoft = grobidResult;
                                    if (field.isTextual) {
                                        goldResultSoft = removeFullPunct(goldResult);
                                        grobidResultSoft = removeFullPunct(grobidResult);
                                    }

                                    if ((goldResult.trim().length() > 0) && goldResultSoft.equals(grobidResultSoft)) {
                                        softStats.incrementObserved(fieldName);
                                    } else {
//System.out.println("\n" + teiFile.getPath());
//System.out.println("gold:" + fieldName);								
//System.out.println("gold:   " + goldResultSoft);
//System.out.println("grobid: " + grobidResultSoft);
//System.out.println("gold:" + goldResult);
//System.out.println("grobid:" + grobidResult);
                                        if (grobidResultSoft.length() > 0) {
                                            softStats.incrementFalsePositive(fieldName);
                                            allGoodSoft = false;
                                        } else if (goldResultSoft.length() > 0) {
                                            softStats.incrementFalseNegative(fieldName);
                                            allGoodSoft = false;
                                        }
                                    }

                                    // Levenshtein
                                    double pct = 0.0;
                                    if (goldResult.equals(grobidResult))
                                        pct = 1.0;
                                    if (field.isTextual) {
                                        int distance = TextUtilities.getLevenshteinDistance(goldResult, grobidResult);
                                        // Levenshtein distance is an integer value, not a percentage... however
                                        // articles usually introduced it as a percentage... so we report it
                                        // following the straightforward formula:
                                        int bigger = Math.max(goldResult.length(), grobidResult.length());
                                        pct = (double) (bigger - distance) / bigger;
                                    }
                                    if ((goldResult.length() > 0) && (pct >= minLevenshteinDistance)) {
                                        levenshteinStats.incrementObserved(fieldName);
                                    } else {
                                        if (grobidResultSoft.length() > 0) {
                                            levenshteinStats.incrementFalsePositive(fieldName);
                                            allGoodLevenshtein = false;
                                        } else if (goldResultSoft.length() > 0) {
                                            levenshteinStats.incrementFalseNegative(fieldName);
                                            allGoodLevenshtein = false;
                                        }
                                    }

                                    // RatcliffObershelp
                                    Double similarity = 0.0;
                                    if (goldResult.trim().equals(grobidResult.trim()))
                                        similarity = 1.0;
                                    if (field.isTextual) {
                                        if ((goldResult.length() > 0) && (grobidResult.length() > 0)) {
                                            Option<Object> similarityObject =
                                                RatcliffObershelpMetric.compare(goldResult, grobidResult);
                                            if ((similarityObject != null) && (similarityObject.get() != null))
                                                similarity = (Double) similarityObject.get();
                                        }
                                    }
                                    if ((goldResult.length() > 0) && (similarity >= minRatcliffObershelpSimilarity)) {
                                        ratcliffObershelpStats.incrementObserved(fieldName);
                                    } else {
                                        if (grobidResultSoft.length() > 0) {
                                            ratcliffObershelpStats.incrementFalsePositive(fieldName);
                                            allGoodRatcliffObershelp = false;
                                        } else if (goldResultSoft.length() > 0) {
                                            ratcliffObershelpStats.incrementFalseNegative(fieldName);
                                            allGoodRatcliffObershelp = false;
                                        }
                                    }
                                    g++;
                                }
                                p++;
                            }
                            totalExpectedInstances++;
                            if (allGoodStrict) {
                                totalCorrectInstancesStrict++;
                            }
                            if (allGoodSoft) {
                                totalCorrectInstancesSoft++;
                            }
                            if (allGoodLevenshtein) {
                                totalCorrectInstancesLevenshtein++;
                            }
                            if (allGoodRatcliffObershelp) {
                                totalCorrectInstancesRatcliffObershelp++;
                            }
                        } else if (sectionType == this.FULLTEXT) {
                            // full text structures
                            int p = 0;
                            boolean allGoodStrict = true;
                            boolean allGoodSoft = true;
                            boolean allGoodLevenshtein = true;
                            boolean allGoodRatcliffObershelp = true;

                            Map<String, Boolean> grobidDocumentLevelStatements = new HashMap<>();
                            Map<String, Boolean> goldDocumentLevelStatements = new HashMap<>();

                            for (FieldSpecification field : fields) {
                                String fieldName = field.fieldName;

                                List<String> grobidResults = new ArrayList<>();
                                for (String path : field.grobidPath) {
                                    NodeList nodeList = (NodeList) xp.compile(path).
                                        evaluate(tei.getDocumentElement(), XPathConstants.NODESET);
                                    for (int i = 0; i < nodeList.getLength(); i++) {
                                        String normalizedString = basicNormalizationFullText(nodeList.item(i).getNodeValue(), fieldName);
                                        if (normalizedString != null && normalizedString.length() > 0)
                                            grobidResults.add(normalizedString);
                                    }
                                }

							/*boolean first = true;
							System.out.print("\n"+fieldName+" - ");
							System.out.print("\ngrobidResults:\t");
							for(String res : grobidResults) {
								if (!first)
									System.out.print(" | ");
								else 
									first = false;
								System.out.print(res);
							}
							System.out.println("");*/

                                List<String> goldResults = new ArrayList<>();
                                int nbgoldResults = 0;
                                List<String> subpaths = null;
                                if (inputType.equals("nlm")) {
                                    subpaths = field.nlmPath;
                                } else if (inputType.equals("tei")) {
                                    subpaths = field.grobidPath;
                                }

                                for (String path : subpaths) {
                                    NodeList nodeList = (NodeList) xp.compile(path).
                                        evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                                    //System.out.println(path + ": " + nodeList.getLength() + " nodes");
                                    nbgoldResults = nodeList.getLength();
                                    for (int i = 0; i < nodeList.getLength(); i++) {
                                        String normalizedString = basicNormalizationFullText(nodeList.item(i).getNodeValue(), fieldName);
                                        if (normalizedString != null && normalizedString.length() > 0)
                                            goldResults.add(normalizedString);
                                    }
                                }

							/*first = true;
							System.out.print("goldResults:\t");
							for(String res : goldResults) {
								if (!first)
									System.out.print(" | ");
								else 
									first = false;
								System.out.print(res);
							}
							System.out.println("");*/

                                if (field.computeDocumentLevelMetrics) {
                                    if (CollectionUtils.isNotEmpty(goldResults)) {
                                        goldDocumentLevelStatements.put(fieldName, true);
                                    }

                                    if (CollectionUtils.isNotEmpty(grobidResults)) {
                                        grobidDocumentLevelStatements.put(fieldName, true);
                                    }
                                }

                                // Workaround to avoid having two different lists with the same content
                                // Probably to be extended to other fields if does not cause
                                if (field.mergeMultipleValues) {
                                    if (CollectionUtils.isNotEmpty(goldResults)) {
                                        List<String> goldResults2 = new ArrayList<>();
                                        goldResults2.add(String.join(" ", goldResults).replace("  ", " "));
                                        goldResults = goldResults2;
//                                        System.out.print("\n\n---- GOLD ----");
//                                        for (String goldResult : goldResults) {
//                                            System.out.print("\n" + goldResult);
//                                        }
//                                        System.out.print("\n--------");
                                    }

                                    if (CollectionUtils.isNotEmpty(grobidResults)) {
                                        List<String> grobidResults2 = new ArrayList<>();
                                        grobidResults2.add(String.join(" ", grobidResults).replace("  ", " "));
                                        grobidResults = grobidResults2;

//                                        System.out.print("\n---- GROBID ----");
//                                        for (String grobidResult : grobidResults) {
//                                            System.out.print("\n" + grobidResult);
//                                        }
//                                        System.out.print("\n--------");
                                    }
                                    // we compare the two result sets
                                }


                                // prepare first the grobidResult set for soft match
                                List<String> grobidSoftResults = new ArrayList<>();
                                for (String res : grobidResults)
                                    grobidSoftResults.add(removeFullPunct(res));

                                int g = 0;
                                int grobidResultsSize = grobidResults.size();
                                int nbMatchStrict = 0; // number of matched grobid results, strict set
                                int nbMatchSoft = 0;
                                int nbMatchLevenshtein = 0;
                                int nbMatchRatcliffObershelp = 0;
                                for (String goldResult : goldResults) {
                                    // nb expected results
                                    if (goldResult.length() > 0) {
                                        strictStats.incrementExpected(fieldName);
                                        softStats.incrementExpected(fieldName);
                                        levenshteinStats.incrementExpected(fieldName);
                                        ratcliffObershelpStats.incrementExpected(fieldName);
                                    }

                                    double pct = 0.0;
                                    // strict
                                    if ((goldResult.length() > 0) && grobidResults.contains(goldResult)) {
                                        strictStats.incrementObserved(fieldName);
                                        nbMatchStrict++;
                                        pct = 1.0;
                                        grobidResults.remove(goldResult);
                                    } else {
                                        if (goldResult.length() > 0) {
                                            strictStats.incrementFalseNegative(fieldName);
                                            allGoodStrict = false;
                                        }
                                    }

                                    // soft
                                    String goldResultSoft = goldResult;
                                    if (field.isTextual) {
                                        goldResultSoft = removeFullPunct(goldResult);
                                    }
                                    if ((goldResult.length() > 0) && grobidSoftResults.contains(goldResultSoft)) {
                                        softStats.incrementObserved(fieldName);
                                        nbMatchSoft++;
                                        grobidSoftResults.remove(goldResultSoft);
                                    } else {
                                        if (goldResultSoft.length() > 0) {
                                            softStats.incrementFalseNegative(fieldName);
                                            allGoodSoft = false;
                                        }
                                    }
						
								/*StringBuilder goldResultBuilder = new StringBuilder();
								for (String goldResult : goldResults) {
									goldResultBuilder.append(goldResult).append(" ");
								}
								String goldResultString = goldResultBuilder.toString();
								StringBuilder grobidResultBuilder = new StringBuilder();
								for (String grobidResult : grobidResults) {
									grobidResultBuilder.append(grobidResult).append(" ");
								}
								String grobidResultString = grobidResultBuilder.toString();
								
								// Levenshtein
								if (field.isTextual) {
									int distance = TextUtilities.getLevenshteinDistance(goldResultString, grobidResultString);
									// Levenshtein distance is an integer value, not a percentage... however
									// articles usually introduced it as a percentage... so we report it
									// following the straightforward formula:
									int bigger = Math.max(goldResult.length(), grobidResult.length());
									pct = (double)(bigger - distance) / bigger;
								}
								if ((goldResult.length() > 0) && (pct >= minLevenshteinDistance)) {
									Integer count = counterObservedLevenshtein.get(p);
									counterObservedLevenshtein.set(p, count+1);
									nbMatchLevenshtein++;
								}
								else {
									if (goldResult.length() > 0){
										Integer count = counterFalseNegativeLevenshtein.get(p);
										counterFalseNegativeLevenshtein.set(p, count+1);
										allGoodLevenshtein = false;
									}
								}
						
								// RatcliffObershelp
								Double similarity = 0.0;
								if (goldResult.trim().equals(grobidResult.trim()))
									similarity = 1.0;
								if (field.isTextual) {
									if ( (goldResult.length() > 0) && (grobidResult.length() > 0) ) {
										Option<Object> similarityObject = 
											RatcliffObershelpMetric.compare(goldResultString, grobidResultString);
										if ( (similarityObject != null) && (similarityObject.get() != null) )
											 similarity = (Double)similarityObject.get();
									}
								}
								if ((goldResult.length() > 0) && (similarity >= minRatcliffObershelpSimilarity)) {
									Integer count = counterObservedRatcliffObershelp.get(p);
									counterObservedRatcliffObershelp.set(p, count+1);
									nbMatchRatcliffObershelp++;
								}
								else {
									if (grobidResultSoft.length() > 0) {
										Integer count = counterFalsePositiveRatcliffObershelp.get(p);
										counterFalsePositiveRatcliffObershelp.set(p, count+1);
										allGoodRatcliffObershelp = false;
									}
									else if (goldResultSoft.length() > 0){
										Integer count = counterFalseNegativeRatcliffObershelp.get(p);
										counterFalseNegativeRatcliffObershelp.set(p, count+1);
										allGoodRatcliffObershelp = false;
									}
								}*/
                                    g++;
                                }

                                if (nbMatchStrict < grobidResultsSize) {
                                    strictStats.incrementFalsePositive(fieldName, grobidResultsSize - nbMatchStrict);
                                    allGoodStrict = false;
                                }

                                if (nbMatchSoft < grobidResultsSize) {
                                    softStats.incrementFalsePositive(fieldName, grobidResultsSize - nbMatchSoft);
                                    allGoodSoft = false;
                                }

							/*if (nbMatchLevenshtein < grobidResultsSize) {
                                levenshteinStats.incrementFalsePositive(fieldName, grobidResultsSize-nbMatchLevenshtein);
								allGoodLevenshtein= false;
							}

							if (nbMatchRatcliffObershelp < grobidResultsSize) {
                                ratcliffObershelpStats.incrementFalsePositive(fieldName, grobidResultsSize-nbMatchRatcliffObershelp);
								allGoodRatcliffObershelp = false;
							}*/

                                p++;
                            }

                            // document level ratio for statements
                            Set<String> combinedStatements = new HashSet<>();
                            combinedStatements.addAll(grobidDocumentLevelStatements.keySet());
                            combinedStatements.addAll(goldDocumentLevelStatements.keySet());

                            for (String localFieldName : combinedStatements) {
                                boolean grobidStatement = false;
                                boolean goldStatement = false;
                                if (grobidDocumentLevelStatements.containsKey(localFieldName)) {
                                    grobidStatement = grobidDocumentLevelStatements.get(localFieldName);
                                    if (grobidStatement) {
                                        documentLevelStatementsRatioStat.incrementObserved(localFieldName);
                                    }
                                }

                                if (goldDocumentLevelStatements.containsKey(localFieldName)) {
                                    goldStatement = goldDocumentLevelStatements.get(localFieldName);
                                    if (goldStatement) {
                                        documentLevelStatementsRatioStat.incrementExpected(localFieldName);
                                    }
                                }

                                if (grobidStatement && !goldStatement) {
                                    documentLevelStatementsRatioStat.incrementFalsePositive(localFieldName);
                                }


                                if (!grobidStatement && goldStatement)
                                    documentLevelStatementsRatioStat.incrementFalseNegative(localFieldName);

                            }
                        }
                    } else if (runType == this.PDFX) {
                        // TBD
                    } else if (runType == this.CERMINE) {
                        // TBD
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                nbFile++;
            }
        }

        report.append("\nEvaluation on " + nbFile + " random PDF files out of " +
            (refFiles.length - 2) + " PDF (ratio " + fileRatio + ").\n");
        reportMD.append("\nEvaluation on " + nbFile + " random PDF files out of " +
            (refFiles.length - 2) + " PDF (ratio " + fileRatio + ").\n");

        report.append("\n======= Strict Matching ======= (exact matches)\n");
        reportMD.append("\n#### Strict Matching (exact matches)\n");
        report.append("\n===== Field-level results =====\n");
        reportMD.append("\n**Field-level results**\n");
        report.append(EvaluationUtilities.computeMetrics(strictStats));
        reportMD.append(EvaluationUtilities.computeMetricsMD(strictStats));

        report.append("\n\n======== Soft Matching ======== (ignoring punctuation, " +
            "case and space characters mismatches)\n");
        reportMD.append("\n\n#### Soft Matching (ignoring punctuation, case and space characters mismatches)\n");
        report.append("\n===== Field-level results =====\n");
        reportMD.append("\n**Field-level results**\n");
        report.append(EvaluationUtilities.computeMetrics(softStats));
        reportMD.append(EvaluationUtilities.computeMetricsMD(softStats));

        if (sectionType != this.FULLTEXT) {
            report.append("\n\n==== Levenshtein Matching ===== (Minimum Levenshtein distance at " +
                this.minLevenshteinDistance + ")\n");
            reportMD.append("\n\n#### Levenshtein Matching (Minimum Levenshtein distance at " +
                this.minLevenshteinDistance + ")\n");
            report.append("\n===== Field-level results =====\n");
            reportMD.append("\n**Field-level results**\n");
            report.append(EvaluationUtilities.computeMetrics(levenshteinStats));
            reportMD.append(EvaluationUtilities.computeMetricsMD(levenshteinStats));

            report.append("\n\n= Ratcliff/Obershelp Matching = (Minimum Ratcliff/Obershelp similarity at " +
                minRatcliffObershelpSimilarity + ")\n");
            reportMD.append("\n\n#### Ratcliff/Obershelp Matching (Minimum Ratcliff/Obershelp similarity at " +
                minRatcliffObershelpSimilarity + ")\n");
            report.append("\n===== Field-level results =====\n");
            reportMD.append("\n**Field-level results**\n");
            report.append(EvaluationUtilities.computeMetrics(ratcliffObershelpStats));
            reportMD.append(EvaluationUtilities.computeMetricsMD(ratcliffObershelpStats));
        }

        if (sectionType == this.CITATION) {
            report.append("\n===== Instance-level results =====\n\n");
            reportMD.append("\n#### Instance-level results\n\n");

            StringBuilder localReport = new StringBuilder();

            localReport.append("Total expected instances: \t\t").append(totalExpectedInstances).append("\n");
            localReport.append("Total extracted instances: \t\t").append(totalObservedInstances).append("\n");
            localReport.append("Total correct instances: \t\t").append(totalCorrectInstancesStrict)
                .append(" (strict) \n");
            localReport.append("Total correct instances: \t\t").append(totalCorrectInstancesSoft)
                .append(" (soft) \n");
            localReport.append("Total correct instances: \t\t").append(totalCorrectInstancesLevenshtein)
                .append(" (Levenshtein) \n");
            localReport.append("Total correct instances: \t\t").append(totalCorrectInstancesRatcliffObershelp)
                .append(" (RatcliffObershelp) \n");

            double precisionStrict = (double) totalCorrectInstancesStrict / (totalObservedInstances);
            double precisionSoft = (double) totalCorrectInstancesSoft / (totalObservedInstances);
            double precisionLevenshtein = (double) totalCorrectInstancesLevenshtein / (totalObservedInstances);
            double precisionRatcliffObershelp = (double) totalCorrectInstancesRatcliffObershelp /
                (totalObservedInstances);
            localReport.append("\nInstance-level precision:\t")
                .append(TextUtilities.formatTwoDecimals(precisionStrict * 100)).append(" (strict) \n");
            localReport.append("Instance-level precision:\t")
                .append(TextUtilities.formatTwoDecimals(precisionSoft * 100)).append(" (soft) \n");
            localReport.append("Instance-level precision:\t")
                .append(TextUtilities.formatTwoDecimals(precisionLevenshtein * 100))
                .append(" (Levenshtein) \n");
            localReport.append("Instance-level precision:\t")
                .append(TextUtilities.formatTwoDecimals(precisionRatcliffObershelp * 100))
                .append(" (RatcliffObershelp) \n");

            double recallStrict = (double) totalCorrectInstancesStrict / (totalExpectedInstances);
            double recallSoft = (double) totalCorrectInstancesSoft / (totalExpectedInstances);
            double recallLevenshtein = (double) totalCorrectInstancesLevenshtein / (totalExpectedInstances);
            double recallRatcliffObershelp = (double) totalCorrectInstancesRatcliffObershelp /
                (totalExpectedInstances);
            localReport.append("\nInstance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(recallStrict * 100)).append("\t(strict) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(recallSoft * 100)).append("\t(soft) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(recallLevenshtein * 100))
                .append("\t(Levenshtein) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(recallRatcliffObershelp * 100))
                .append("\t(RatcliffObershelp) \n");

            double f0Strict = (2 * precisionStrict * recallStrict) / (precisionStrict + recallStrict);
            double f0Soft = (2 * precisionSoft * recallSoft) / (precisionSoft + recallSoft);
            double f0Levenshtein = (2 * precisionLevenshtein * recallLevenshtein) /
                (precisionLevenshtein + recallLevenshtein);
            double f0RatcliffObershelp = (2 * precisionRatcliffObershelp * recallRatcliffObershelp) /
                (precisionRatcliffObershelp + recallRatcliffObershelp);
            localReport.append("\nInstance-level f-score:\t")
                .append(TextUtilities.formatTwoDecimals(f0Strict * 100)).append(" (strict) \n");
            localReport.append("Instance-level f-score:\t")
                .append(TextUtilities.formatTwoDecimals(f0Soft * 100)).append(" (soft) \n");
            localReport.append("Instance-level f-score:\t")
                .append(TextUtilities.formatTwoDecimals(f0Levenshtein * 100)).append(" (Levenshtein) \n");
            localReport.append("Instance-level f-score:\t")
                .append(TextUtilities.formatTwoDecimals(f0RatcliffObershelp * 100)).append(" (RatcliffObershelp) \n");

            localReport.append("\nMatching 1 :\t").append(match1 + "\n");
            localReport.append("\nMatching 2 :\t").append(match2 + "\n");
            localReport.append("\nMatching 3 :\t").append(match3 + "\n");
            localReport.append("\nMatching 4 :\t").append(match4 + "\n");
            localReport.append("\nTotal matches :\t").append((match1 + match2 + match3 + match4) + "\n");

            report.append(localReport.toString());
            reportMD.append("```\n" + localReport.toString() + "```\n\n");

            report.append("\n======= Citation context resolution ======= \n");
            reportMD.append("\n#### Citation context resolution\n");

            localReport = new StringBuilder();

            localReport.append("\nTotal expected references: \t ").append(totalExpectedReferences)
                .append(" - ").append(TextUtilities.formatTwoDecimals((double) totalExpectedReferences / nbFile)).append(" references per article");
            localReport.append("\nTotal predicted references: \t ").append(totalObservedReferences)
                .append(" - ").append(TextUtilities.formatTwoDecimals((double) totalObservedReferences / nbFile)).append(" references per article");

            //report.append("\nTotal observed references (instance): \t ").append(totalObservedInstances);
            //report.append("\nTotal correct observed references: \t ").append(totalCorrectInstancesRatcliffObershelp);

            localReport.append("\n\nTotal expected citation contexts: \t ").append(totalExpectedCitations)
                .append(" - ").append(TextUtilities.formatTwoDecimals((double) totalExpectedCitations / nbFile)).append(" citation contexts per article");
            localReport.append("\nTotal predicted citation contexts: \t ").append(totalObservedCitations)
                .append(" - ").append(TextUtilities.formatTwoDecimals((double) totalObservedCitations / nbFile)).append(" citation contexts per article");
            localReport.append("\n\nTotal correct predicted citation contexts: \t ").append(totalCorrectObservedCitations)
                .append(" - ").append(TextUtilities.formatTwoDecimals((double) totalCorrectObservedCitations / nbFile)).append(" citation contexts per article");

            localReport.append("\nTotal wrong predicted citation contexts: \t ").append(totalWrongObservedCitations).append(" (wrong callout matching, callout missing in NLM, or matching with a bib. ref. not aligned with a bib.ref. in NLM)");

            double precisionCitationContext = (double) totalCorrectObservedCitations / totalObservedCitations;
            double recallCitationContext = (double) totalCorrectObservedCitations / totalExpectedCitations;
            double fscoreCitationContext = (2 * precisionCitationContext * recallCitationContext) / (precisionCitationContext + recallCitationContext);
            ;

            localReport.append("\n\nPrecision citation contexts: \t ").append(TextUtilities.formatTwoDecimals(precisionCitationContext * 100));
            localReport.append("\nRecall citation contexts: \t ").append(TextUtilities.formatTwoDecimals(recallCitationContext * 100));
            localReport.append("\nfscore citation contexts: \t ").append(TextUtilities.formatTwoDecimals(fscoreCitationContext * 100));
            localReport.append("\n");

            report.append(localReport.toString());
            reportMD.append("```\n" + localReport.toString() + "```\n\n");
        } else if (sectionType == this.HEADER) {
            report.append("\n===== Instance-level results =====\n\n");
            reportMD.append("\n#### Instance-level results\n\n");

            StringBuilder localReport = new StringBuilder();

            localReport.append("Total expected instances: \t").append(totalExpectedInstances).append("\n");
            localReport.append("Total correct instances: \t").append(totalCorrectInstancesStrict)
                .append(" (strict) \n");
            localReport.append("Total correct instances: \t").append(totalCorrectInstancesSoft)
                .append(" (soft) \n");
            localReport.append("Total correct instances: \t").append(totalCorrectInstancesLevenshtein)
                .append(" (Levenshtein) \n");
            localReport.append("Total correct instances: \t").append(totalCorrectInstancesRatcliffObershelp)
                .append(" (ObservedRatcliffObershelp) \n");
            double accuracyStrict = (double) totalCorrectInstancesStrict / (totalExpectedInstances);
            double accuracySoft = (double) totalCorrectInstancesSoft / (totalExpectedInstances);
            double accuracyLevenshtein = (double) totalCorrectInstancesLevenshtein / (totalExpectedInstances);
            double accuracyRatcliffObershelp = (double) totalCorrectInstancesRatcliffObershelp /
                (totalExpectedInstances);
            localReport.append("\nInstance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(accuracyStrict * 100)).append("\t(strict) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(accuracySoft * 100)).append("\t(soft) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(accuracyLevenshtein * 100))
                .append("\t(Levenshtein) \n");
            localReport.append("Instance-level recall:\t")
                .append(TextUtilities.formatTwoDecimals(accuracyRatcliffObershelp * 100))
                .append("\t(RatcliffObershelp) \n");

            report.append(localReport.toString());
            reportMD.append("```\n" + localReport.toString() + "```\n\n");
        }

        if (sectionType == this.FULLTEXT) {
            report.append("\n===== Document-level ratio results =====\n");
            reportMD.append("\n**Document-level ratio results**\n");
            report.append(EvaluationUtilities.computeMetrics(documentLevelStatementsRatioStat));
            reportMD.append(EvaluationUtilities.computeMetricsMD(documentLevelStatementsRatioStat));
        }

        return report.toString();
    }

    private static String basicNormalization(String string) {
        string = string.trim();
        string = string.replace("\n", " ");
        string = string.replace("\t", " ");
        string = string.replaceAll(" ( )*", " ");
        string = string.replace("&apos;", "'");
        return string.trim().toLowerCase();
    }

    private static String identifierNormalization(String string) {
        string = basicNormalization(string);
        if (string.startsWith("pmcpmc")) {
            string = string.replace("pmcpmc", "");
        }
        string = string.replace("pmc", "");
        if (string.startsWith("doi")) {
            string = string.replace("doi", "").trim();
            if (string.startsWith(":")) {
                string = string.substring(1, string.length());
                string = string.trim();
            }
        }
        if (string.startsWith("pmid")) {
            string = string.replace("pmid", "").trim();
            if (string.startsWith(":")) {
                string = string.substring(1, string.length());
                string = string.trim();
            }
        }
        return string.trim().toLowerCase();
    }

    private static String basicNormalizationFullText(String string, String fieldName) {
        string = string.trim();
        string = UnicodeUtil.normaliseText(string);
        string = string.replace("\n", " ");
        string = string.replace("\t", " ");
        string = string.replace("_", " ");
        string = string.replace("\u00A0", " ");
        if (fieldName.equals("reference_figure")) {
            string = string.replace("figure", "").replace("Figure", "").replace("fig.", "").replace("Fig.", "").replace("fig", "").replace("Fig", "");
        }
        if (fieldName.equals("reference_table")) {
            string = string.replace("table", "").replace("Table", "");
        }
        string = string.replaceAll(" ( )*", " ");
        if (string.startsWith("[") || string.startsWith("("))
            string = string.substring(1, string.length());
        while (string.endsWith("]") || string.endsWith(")") || string.endsWith(","))
            string = string.substring(0, string.length() - 1);
        return string.trim();
    }

    private static String removeFullPunct(String string) {
        StringBuilder result = new StringBuilder();
        string = string.toLowerCase();
        String allMismatchToIgnore = TextUtilities.fullPunctuations + "‐ \t\n\r\u00A0" + "\u00B7\u25FC\u25B2\u25BA\u25C6\u25CB\u25C7\u25CF\u25CE\u25FD\u25F8\u25F9\u25FA";//last are placeholders used for to be OCR chars
        for (int i = 0; i < string.length(); i++) {
            if (allMismatchToIgnore.indexOf(string.charAt(i)) == -1) {
                result.append(string.charAt(i));
            }
        }
        return result.toString();
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        //DOMConfigurator is called to force logger to use the xml configuration file
        //DOMConfigurator.configure("src/main/resources/log4j.xml");

        if (args.length > 5 || args.length == 0) {
            System.err.println("usage: command [path to the (gold) evaluation XML dataset] Run[0|1] fileRatio[0.0-1.0]");
            return;
        }

        String inputType = args[0];
        if (StringUtils.isBlank(inputType) || (!inputType.equals("nlm") && !inputType.equals("tei"))) {
            System.err.println("Input type is not correctly set, should be [tei|nlm]");
            return;
        }

        boolean runGrobidVal = true;
        String xmlInputPath = args[1];
        if (StringUtils.isBlank(xmlInputPath)) {
            System.err.println("Path to evaluation (gold) XML data is not correctly set");
            return;
        }

        String runGrobid = args[2];
        if (runGrobid.equals("0")) {
            runGrobidVal = false;
        } else if (runGrobid.equals("1")) {
            runGrobidVal = true;
        } else {
            System.err.println("Invalid value for last argument (run): [0|1]");
            return;
        }

        // optional file ratio for applying the evaluation
        double fileRatio = 1.0;
        if (args.length > 3) {
            String fileRatioString = args[3];
            if (StringUtils.isNotBlank(fileRatioString)) {
                try {
                    fileRatio = Double.parseDouble(fileRatioString);
                } catch (Exception e) {
                    System.err.println("Invalid argument fileRatio, must be a double, e.g. 0.1");
                    return;
                }
            }
        }

        GrobidModels.Flavor parsedFlavor = null;
        if (args.length > 4) {
            String flavor = args[4];
            parsedFlavor = GrobidModels.Flavor.fromLabel(flavor);
            if (parsedFlavor == null) {
                System.out.println("Flavor was not specified, or was empty. Using default Grobid process. ");
            } else {
                System.out.println("Setting flavor to: " + parsedFlavor);
            }
        }

        try {
            File xmlPath = new File(xmlInputPath);
            if (!xmlPath.exists()) {
                System.err.println("Path to evaluation (gold) XML data does not exist");
                return;
            }
            if (!xmlPath.isDirectory()) {
                System.err.println("Path to evaluation (gold) XML data is not a directory");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            EndToEndEvaluation eval = new EndToEndEvaluation(xmlInputPath, inputType, parsedFlavor);
            eval.fileRatio = fileRatio;

            // markdown report
            StringBuilder reportMD = new StringBuilder();

            String report = eval.evaluationGrobid(runGrobidVal, reportMD);

            System.out.println(report);
            System.out.println(Engine.getCntManager());

            // write markdown report
            File fileMarkDown = new File(GrobidProperties.getInstance().getTempPath().getPath() + File.separator + "report.md");
            FileUtils.writeStringToFile(fileMarkDown, reportMD.toString(), "UTF-8");
            System.out.println("\nEvaluation report in markdown format saved under " + fileMarkDown.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // to be sure jvm stops
        System.exit(0);
    }

}