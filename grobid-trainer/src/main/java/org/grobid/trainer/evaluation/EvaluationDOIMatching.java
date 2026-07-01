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
package org.grobid.trainer.evaluation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.Engine;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.factory.GrobidFactory;
import org.grobid.core.utilities.Consolidation.GrobidConsolidationService;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.trainer.evaluation.utilities.NamespaceContextMap;

/**
 * Evaluation of the DOI matching for the extracted bibliographical references,
 * using PDF+native publisher XML where the DOI or PMID are provided. A typical
 * example of the evaluation data is PubMed Central fulltext resources. A second
 * type of evaluation is PDF+TEI files produced via Pub2TEI fir mainstream
 * publishers.
 *
 */
public class EvaluationDOIMatching {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationDOIMatching.class);

    private static String evaluationFilePath = null;
    private Engine engine = null;

    public static final int BIBLIO_GLUTTON = 0;
    public static final int CROSSREF_API = 1;

    public static final double minRatcliffObershelpSimilarity = 0.5;

    // xpath expressions for nlm
    private static final String path_nlm_ref = "/article/back/ref-list/ref/mixed-citation";
    private static final String path_nlm_doi = "pub-id[@pub-id-type=\"doi\"]/text()";
    private static final String path_nlm_pmid = "pub-id[@pub-id-type=\"pmid\"]/text()";

    private static final String path_nlm_title = "article-title/text()";
    private static final String path_nlm_author = "person-group[@person-group-type=\"author\"]/name/surname/text()";
    private static final String path_nlm_host = "source/text()";
    private static final String path_nlm_first_page = "fpage/text()";
    private static final String path_nlm_volume = "volume/text()";

    // xpath expressions for tei
    private static final String path_tei_ref = "//back/div/listBibl/biblStruct";
    private static final String path_tei_doi = "idno[@type=\"doi\"]/text()";

    public EvaluationDOIMatching(String path) {
        this.evaluationFilePath = path;

        File evaluationFile = new File(path);
        if (!evaluationFile.exists()) {
            System.out.println("Path to evaluation (gold) XML data is not valid !");
            this.evaluationFilePath = null;
        }

        try {
            GrobidProperties.getInstance();
            LOGGER.info(">>>>>>>> GROBID_HOME=" + GrobidProperties.getGrobidHome());

            engine = GrobidFactory.getInstance().createEngine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String evaluation() throws Exception {
        StringBuilder report = new StringBuilder();

        // we run Grobid reference extraction on the PubMedCentral data
        File input = new File(this.evaluationFilePath);
        // we process all json files in the input evaluation directory
        File[] refFiles = input.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".json");
            }
        });

        if (refFiles == null) {
            report.append("No file in dataset");
            return report.toString();
        }

        int nbRef = 0;
        int nbDOIFound = 0;
        int nbDOICorrect = 0;
        long start = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        for (File dir : refFiles) {
            // get the PDF file in the directory
            final File jsonFile = refFiles[0];

            JsonNode rootNode = mapper.readTree(jsonFile);

            Iterator<JsonNode> ite = rootNode.elements();

            List<String> rawRefs = new ArrayList<String>();
            List<String> dois = new ArrayList<String>();
            List<String> pmids = new ArrayList<String>();
            List<String> atitles = new ArrayList<String>();
            List<String> jtitles = new ArrayList<String>();
            List<String> firstAuthors = new ArrayList<String>();
            List<String> volumes = new ArrayList<String>();
            List<String> firstPages = new ArrayList<String>();
            while (ite.hasNext()) {
                //if (nbRef > 1000)
                //    break;
                JsonNode entryNode = ite.next();

                String rawRef = null;
                JsonNode refNode = entryNode.findPath("reference");
                if ((refNode != null) && (!refNode.isMissingNode())) {
                    rawRef = refNode.textValue();
                }
                rawRefs.add(rawRef);

                String doi = null;
                JsonNode doiNode = entryNode.findPath("doi");
                if ((doiNode != null) && (!doiNode.isMissingNode())) {
                    doi = doiNode.textValue();
                }
                dois.add(doi);

                String pmid = null;
                JsonNode pmidNode = entryNode.findPath("pmid");
                if ((pmidNode != null) && (!pmidNode.isMissingNode())) {
                    pmid = pmidNode.textValue();
                }
                pmids.add(pmid);

                String atitle = null;
                JsonNode atitleNode = entryNode.findPath("atitle");
                if ((atitleNode != null) && (!atitleNode.isMissingNode())) {
                    atitle = atitleNode.textValue();
                }
                atitles.add(atitle);

                String jtitle = null;
                JsonNode jtitleNode = entryNode.findPath("jtitle");
                if ((jtitleNode != null) && (!jtitleNode.isMissingNode())) {
                    jtitle = jtitleNode.textValue();
                }
                jtitles.add(jtitle);

                String volume = null;
                JsonNode volumeNode = entryNode.findPath("volume");
                if ((volumeNode != null) && (!volumeNode.isMissingNode())) {
                    volume = volumeNode.textValue();
                }
                volumes.add(volume);

                String firstPage = null;
                JsonNode firstPageNode = entryNode.findPath("firstPage");
                if ((firstPageNode != null) && (!firstPageNode.isMissingNode())) {
                    firstPage = firstPageNode.textValue();
                }
                firstPages.add(firstPage);

                String author = null;
                JsonNode authorNode = entryNode.findPath("author");
                if ((authorNode != null) && (!authorNode.isMissingNode())) {
                    author = authorNode.textValue();
                }
                firstAuthors.add(author);

                nbRef++;
            }
            // run Grobid reference parser on this raw strings
            try {
                List<BiblioItem> biblios = engine.processRawReferences(rawRefs, 2);

                for (int i = 0; i < rawRefs.size(); i++) {
                    BiblioItem biblio = biblios.get(i);
                    String doi = dois.get(i);
                    String pmid = pmids.get(i);

                    //LOGGER.info("\n\tDOI: " + doi);
                    //LOGGER.info("\trawRef: " + rawRefs.get(i));

                    if (biblio.getDOI() != null) {
                        nbDOIFound++;
                        //LOGGER.info("\tfound: "+ biblio.getDOI());

                        // is the DOI correct?
                        if (biblio.getDOI().toLowerCase().equals(doi.toLowerCase()))
                            nbDOICorrect++;
                        else {
                            //LOGGER.info("!!!!!!!!!!!!! Mismatch DOI: " + doi + " / " + biblio.getDOI());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error when processing: " + jsonFile.getPath(), e);
            }
        }

        double processTime = ((double) System.currentTimeMillis() - start) / 1000.0;

        double rate = ((double) processTime) / nbRef;
        System.out.println(
                "\n\n"
                        + nbRef
                        + " bibliographical references processed in "
                        +
                        processTime
                        + " seconds, "
                        +
                        TextUtilities.formatFourDecimals(rate)
                        +
                        " seconds per bibliographical reference.");
        System.out.println("Found " + nbDOIFound + " DOI");

        // evaluation of the run
        start = System.currentTimeMillis();

        report.append("\n======= ");
        if (GrobidProperties.getInstance().getConsolidationService() == GrobidConsolidationService.GLUTTON)
            report.append("GLUTTON");
        else
            report.append("CROSSREF");
        report.append(" API ======= \n");
        double precision = ((double) nbDOICorrect / nbDOIFound);
        report.append("\nprecision:\t");
        report.append(TextUtilities.formatTwoDecimals(precision * 100));
        double recall = ((double) nbDOICorrect / nbRef);
        report.append("\nrecall:\t\t").append(TextUtilities.formatTwoDecimals(recall * 100));
        double f1 = 0.0;
        if (precision + recall != 0.0)
            f1 = (2 * precision * recall) / (precision + recall);
        report.append("\nF1-score:\t").append(TextUtilities.formatTwoDecimals(f1 * 100)).append("\n");

        //report.append("\n======= BIBLIO GLUTTON ======= \n");

        //System.out.println("Evaluation metrics produced in " +
        //       (System.currentTimeMillis() - start) / (1000.00) + " seconds");

        return report.toString();
    }

    /**
     * From PDF and publisher XML (nlm or TEI), we create a dataset of raw bibliographical
     * references extracted from the PDF by GROBID associated with their DOI obtained from
     * the XML. This set will be used for evaluating the DOI matching.
     */
    public void buildEvaluationDataset() throws Exception {
        if (this.evaluationFilePath == null) {
            throw new GrobidResourceException("Path to evaluation (gold) XML data is not correctly set");
        }
        StringBuffer report = new StringBuffer();

        // get a factory for SAX parsers
        SAXParserFactory spf = SAXParserFactory.newInstance();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        HashMap map = new HashMap();
        // explicit indication of the default namespace
        map.put("tei", "http://www.tei-c.org/ns/1.0");

        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("tei", "http://www.tei-c.org/ns/1.0");
        xp.setNamespaceContext(new NamespaceContextMap(mappings));

        File input = new File(this.evaluationFilePath);
        // we process all tei files in the output directory
        File[] refFiles = input.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (dir.isDirectory())
                    return true;
                else
                    return false;
            }
        });

        if (refFiles == null) {
            report.append("No file in dataset");
            return;
        }

        List<BibRefAggregated> allGoldReferences = new ArrayList<BibRefAggregated>();

        int n = 0;
        long start = System.currentTimeMillis();
        int fails = 0;
        for (File dir : refFiles) {
            // get the PDF file in the directory
            File[] refFiles2 = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".pdf") || name.endsWith(".PDF");
                }
            });

            if (refFiles2 == null || refFiles2.length == 0) {
                LOGGER.info("warning: no PDF found under " + dir.getPath());
                continue;
            }
            if (refFiles2.length != 1) {
                LOGGER.warn("warning: more than one PDF found under " + dir.getPath());
                LOGGER.warn("processing only the first one...");
            }

            final File pdfFile = refFiles2[0];
            File nlmFile = null;
            File teiFile = null;

            List<BibRefAggregated> goldReferences = new ArrayList<BibRefAggregated>();
            // get the (gold) reference file corresponding to this pdf
            File[] refFiles3 = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".nxml") || name.endsWith(".xml");
                }
            });
            if ((refFiles3 != null) && (refFiles3.length != 0))
                nlmFile = refFiles3[0];
            refFiles3 = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei");
                }
            });
            if ((refFiles3 != null) && (refFiles3.length != 0))
                teiFile = refFiles3[0];

            if ((nlmFile == null) && (teiFile == null)) {
                LOGGER.warn("warning: no reference NLM or TEI file found under " + dir.getPath());
                continue;
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setValidating(false);

            //System.out.println("\n\nFile: " + pdfFile.getPath());

            try {
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                docBuilder.setEntityResolver(new EntityResolver() {
                    public InputSource resolveEntity(String publicId, String systemId) {
                        return new InputSource(
                                new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes()));
                    }
                }); // swap in a dummy resolver to neutralise the online DTD
                File goldFile = null;
                if (teiFile != null)
                    goldFile = teiFile;
                else
                    goldFile = nlmFile;
                Document gold = docBuilder.parse(goldFile);

                //System.out.println("Reference DOIs in : " + goldFile.getPath());

                // get the DOI or PMID of the bibliographical references ia xpath
                String path_doi = null;
                String path_pmid = null;
                String path_ref = null;
                if (teiFile == null) {
                    // gold DOI are in the nlm file
                    path_ref = path_nlm_ref;
                    path_doi = path_nlm_doi;
                    path_pmid = path_nlm_pmid;
                } else {
                    path_ref = path_tei_ref;
                    path_doi = path_tei_doi;
                }
                NodeList nodeList = (NodeList) xp.compile(path_ref)
                        .evaluate(gold.getDocumentElement(), XPathConstants.NODESET);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    // for each node we have a ref bib
                    BibRefAggregated refBib = new BibRefAggregated();
                    Node ref = nodeList.item(i);
                    refBib.setXML(ref);

                    // get DOI and PMID - if any
                    NodeList nodeListDOI = (NodeList) xp.compile(path_doi).evaluate(ref, XPathConstants.NODESET);
                    if (nodeListDOI.getLength() > 0) {
                        Node nodeDOI = nodeListDOI.item(0);
                        String doi = nodeDOI.getNodeValue();
                        refBib.setDOI(doi);
                    }
                    NodeList nodeListPMID = (NodeList) xp.compile(path_pmid).evaluate(ref, XPathConstants.NODESET);
                    if (nodeListPMID.getLength() > 0) {
                        Node nodePMID = nodeListPMID.item(0);
                        String pmid = nodePMID.getNodeValue();
                        refBib.setPMID(pmid);
                    }
                    goldReferences.add(refBib);
                }

            } catch (Exception e) {
                LOGGER.error("Error when collecting reference citations", e);
            }

            int p = 0; // count the number of citation raw reference string aligned with gold reference
            // run Grobid reference extraction
            try {
                LOGGER.info(n + " - " + pdfFile.getPath());
                List<BibDataSet> bibrefs = engine.processReferences(pdfFile, 0);
                for (BibDataSet bib : bibrefs) {
                    String rawRef = bib.getRawBib();
                    // we remove a DOI possibly in the raw ref, as the whole exercie is about DOI
                    // matching
                    if (rawRef != null) {
                        rawRef = TextUtilities.DOIPattern.matcher(rawRef).replaceAll(" ");
                        // we need to align this raw ref bib string with a gold ref bib
                        for (BibRefAggregated goldReference : goldReferences) {
                            if ((goldReference.getRawRef() == null) &&
                            //(goldReference.getDOI() != null || goldReference.getPMID() != null) ) {
                                    (goldReference.getDOI() != null)) {
                                // check key fields like for alignment
                                Node refNode = goldReference.getXML();
                                if (refNode == null)
                                    continue;
                                // title
                                NodeList nodeList = (NodeList) xp.compile(path_nlm_title)
                                        .evaluate(refNode, XPathConstants.NODESET);
                                String title = null;
                                if ((nodeList != null) && nodeList.getLength() > 0)
                                    title = nodeList.item(0).getNodeValue();

                                // author
                                String author = null;
                                String firstAuthor = null;
                                nodeList = (NodeList) xp.compile(path_nlm_author)
                                        .evaluate(refNode, XPathConstants.NODESET);
                                if ((nodeList != null) && nodeList.getLength() > 0) {
                                    author = nodeList.item(0).getNodeValue();
                                    firstAuthor = author;
                                    for (int i = 1; i < nodeList.getLength(); i++)
                                        author += nodeList.item(i).getNodeValue();
                                }
                                // journal, book or conference (aka source in NLM)
                                String host = null;
                                nodeList = (NodeList) xp.compile(path_nlm_host)
                                        .evaluate(refNode, XPathConstants.NODESET);
                                if ((nodeList != null) && nodeList.getLength() > 0)
                                    host = nodeList.item(0).getNodeValue();

                                // first page
                                String firstPage = null;
                                nodeList = (NodeList) xp.compile(path_nlm_first_page)
                                        .evaluate(refNode, XPathConstants.NODESET);
                                if ((nodeList != null) && nodeList.getLength() > 0)
                                    firstPage = nodeList.item(0).getNodeValue();

                                // volume
                                String volume = null;
                                nodeList = (NodeList) xp.compile(path_nlm_volume)
                                        .evaluate(refNode, XPathConstants.NODESET);
                                if ((nodeList != null) && nodeList.getLength() > 0)
                                    volume = nodeList.item(0).getNodeValue();

                                //System.out.println(title + " " + author + " " + host);
                                if ((title == null) && (author == null) && (host == null)) {
                                    // nlm might contain the raw string but usually not DOI or PMID
                                } else {
                                    String rawRefSignature = this.getSignature(rawRef);
                                    String titleSignature = this.getSignature(title);
                                    String authorSignature = this.getSignature(author);
                                    String hostSignature = this.getSignature(host);
                                    String firstPageSignature = this.getSignature(firstPage);
                                    String volumeSignature = this.getSignature(volume);
                                    int ind1 = -1, ind2 = -1, ind3 = -1, ind4 = -1, ind5 = -1;
                                    if (title != null) {
                                        ind1 = rawRefSignature.indexOf(titleSignature);
                                    }
                                    if (author != null) {
                                        ind2 = rawRefSignature.indexOf(authorSignature);
                                    }
                                    if (host != null) {
                                        ind3 = rawRefSignature.indexOf(hostSignature);
                                    }
                                    if (firstPage != null) {
                                        ind4 = rawRefSignature.indexOf(firstPageSignature);
                                    }
                                    if (volume != null) {
                                        ind5 = rawRefSignature.indexOf(volumeSignature);
                                    }
                                    // soft match for the title using Ratcliff Obershelp string distance
                                    //double similarity = 0.0;
                                    //Option<Object> similarityObject =
                                    //        RatcliffObershelpMetric.compare(title, localRawRef);
                                    //if ( (similarityObject != null) && (similarityObject.get() != null) )
                                    //    similarity = (Double)similarityObject.get();

                                    // intra-document matching
                                    if ((ind1 != -1) ||
                                            (ind2 != -1 && ind3 != -1 && (ind4 != -1 || ind5 != -1))) {
                                        goldReference.setRawRef(rawRef);
                                        goldReference.setFirstPage(firstPage);
                                        goldReference.setVolume(volume);
                                        goldReference.setAtitle(title);
                                        goldReference.setJtitle(host);
                                        goldReference.setFirstAuthor(firstAuthor);
                                        // if we have a pmid but no doi, we can still try to get the DOI from it

                                        p++;
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }

                allGoldReferences.addAll(goldReferences);

                // we get from this the reference strings and matched DOI
                System.out.println("total of " + bibrefs.size() + " ref. bib. found by GROBID");
                System.out.println(goldReferences.size() + " DOI identified in gold");
                System.out.println("and " + p + " original reference strings identified");
            } catch (Exception e) {
                System.out.println("Error when processing: " + pdfFile.getPath());
                e.printStackTrace();
                fails++;
            }
            n++;
        }

        // writing the dataset file
        File jsonFile = new File(this.evaluationFilePath + File.separator + "references-doi-matching.json");
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();
        StringBuilder sb = new StringBuilder();

        sb.append("[\n");
        boolean first = true;
        for (BibRefAggregated goldReference : allGoldReferences) {
            if ((goldReference.getRawRef() != null) &&
                    (goldReference.getDOI() != null || goldReference.getPMID() != null)) {
                if (first)
                    first = false;
                else
                    sb.append(",\n");

                sb.append("{");
                byte[] encodedValueRef = encoder.quoteAsUTF8(goldReference.getRawRef());
                String outputValueRef = new String(encodedValueRef);
                sb.append("\"reference\": \"" + outputValueRef + "\"");

                if (goldReference.getDOI() != null) {
                    byte[] encodedValueDOI = encoder.quoteAsUTF8(goldReference.getDOI());
                    String outputValueDOI = new String(encodedValueDOI);
                    sb.append(", \"doi\": \"" + outputValueDOI + "\"");
                }
                if (goldReference.getPMID() != null) {
                    byte[] encodedValuePMID = encoder.quoteAsUTF8(goldReference.getPMID());
                    String outputValuePMID = new String(encodedValuePMID);
                    sb.append(", \"pmid\": \"" + outputValuePMID + "\"");
                }

                // other metadata
                if (goldReference.getAtitle() != null) {
                    byte[] encodedValueAtitle = encoder.quoteAsUTF8(goldReference.getAtitle());
                    String outputValueAtitle = new String(encodedValueAtitle);
                    sb.append(", \"atitle\": \"" + outputValueAtitle + "\"");
                }
                if (goldReference.getFirstAuthor() != null) {
                    byte[] encodedValueFirstAuthor = encoder.quoteAsUTF8(goldReference.getFirstAuthor());
                    String outputValueFirstAuthor = new String(encodedValueFirstAuthor);
                    sb.append(", \"firstAuthor\": \"" + outputValueFirstAuthor + "\"");
                }
                if (goldReference.getJtitle() != null) {
                    byte[] encodedValueJtitle = encoder.quoteAsUTF8(goldReference.getJtitle());
                    String outputValueJtitle = new String(encodedValueJtitle);
                    sb.append(", \"jtitle\": \"" + outputValueJtitle + "\"");
                }
                if (goldReference.getVolume() != null) {
                    byte[] encodedValueVolume = encoder.quoteAsUTF8(goldReference.getVolume());
                    String outputValueVolume = new String(encodedValueVolume);
                    sb.append(", \"volume\": \"" + outputValueVolume + "\"");
                }
                if (goldReference.getFirstPage() != null) {
                    byte[] encodedValueFirstPage = encoder.quoteAsUTF8(goldReference.getFirstPage());
                    String outputValueFirstPage = new String(encodedValueFirstPage);
                    sb.append(", \"firstPage\": \"" + outputValueFirstPage + "\"");
                }

                sb.append("}");
            }
        }

        sb.append("]");
        try {
            // saving the file
            FileUtils.writeStringToFile(jsonFile, sb.toString(), "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("GROBID failed on " + fails + " PDF");
        double processTime = ((double) System.currentTimeMillis() - start) / 1000.0;

        System.out.println(
                n
                        + " PDF files processed in "
                        +
                        processTime
                        + " seconds, "
                        + ((double) processTime) / n
                        + " seconds per PDF file.");
    }

    private Pattern pattern = Pattern.compile("[^a-zA-Z0-9]+");

    /**
     * Simplify a string for soft matching: lowercasing, ascii folding, remove punctuation
     * and special characters
     */
    private String getSignature(String field) {
        if (field == null)
            return null;
        String string = field.toLowerCase();
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        string = string.replaceAll("[^\\p{ASCII}]", "");
        //string = string.replaceAll("\\p{M}", "");
        string = pattern.matcher(string).replaceAll("");
        return string;
    }

    /**
     * This class represents a bibliographical reference by aggregating information
     * from the publisher XML and the PDF extracted information from GROBID.
     */
    public class BibRefAggregated {
        // raw string of the bib ref
        private String rawRef = null;

        // doi if any
        private String doi = null;

        // pmid if present
        private String pmid = null;

        // xml segment corresponding to the bibliographical reference
        private Node xml = null;

        // other metadata
        private String atitle = null;
        private String jtitle = null;
        private String firstAuthor = null;
        private String volume = null;
        private String firstPage = null;

        public String getRawRef() {
            return this.rawRef;
        }

        public void setRawRef(String raw) {
            this.rawRef = raw;
        }

        public String getDOI() {
            return this.doi;
        }

        public void setDOI(String doi) {
            this.doi = doi;
        }

        public String getPMID() {
            return this.pmid;
        }

        public void setPMID(String pmid) {
            this.pmid = pmid;
        }

        public Node getXML() {
            return this.xml;
        }

        public void setXML(Node xml) {
            this.xml = xml;
        }

        public String getAtitle() {
            return this.atitle;
        }

        public void setAtitle(String atitle) {
            this.atitle = atitle;
        }

        public String getJtitle() {
            return this.jtitle;
        }

        public void setJtitle(String jtitle) {
            this.jtitle = jtitle;
        }

        public String getFirstAuthor() {
            return this.firstAuthor;
        }

        public void setFirstAuthor(String firstAuthor) {
            this.firstAuthor = firstAuthor;
        }

        public String getVolume() {
            return this.volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public String getFirstPage() {
            return this.firstPage;
        }

        public void setFirstPage(String firstPage) {
            this.firstPage = firstPage;
        }
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        if ((args.length > 2) || (args.length == 0)) {
            System.err.println("command parameters: action[data|eval] [path to the (gold) evaluation dataset]");
            return;
        }

        String action = args[0];
        if ((action == null) || (action.length() == 0) || (!action.equals("data")) && (!action.equals("eval"))) {
            System.err.println("Action to be performed not correctly set, should be [data|eval]");
            return;
        }

        String inputPath = args[1];
        if ((inputPath == null) || (inputPath.length() == 0)) {
            System.err.println("Path to evaluation (gold) XML data is not correctly set");
            return;
        }

        try {
            File thePath = new File(inputPath);
            if (!thePath.exists()) {
                System.err.println("Path to evaluation (gold) XML data does not exist");
                return;
            }
            if (!thePath.isDirectory()) {
                System.err.println("Path to evaluation (gold) XML data is not a directory");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (action.equals("data")) {
                EvaluationDOIMatching data = new EvaluationDOIMatching(inputPath);
                data.buildEvaluationDataset();
            } else if (action.equals("eval")) {
                EvaluationDOIMatching eval = new EvaluationDOIMatching(inputPath);
                String report = eval.evaluation();
                System.out.println(report);
            }

            System.out.println(Engine.getCntManager());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // to be sure jvm stops
        System.exit(0);
    }
}
