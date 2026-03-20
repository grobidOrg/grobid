package org.grobid.core.utilities;

import com.rockymadden.stringmetric.similarity.RatcliffObershelpMetric;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Funder;
import org.grobid.core.utilities.counters.CntManager;
import org.grobid.core.utilities.crossref.CrossrefClient;
import org.grobid.core.utilities.crossref.CrossrefRequestListener;
import org.grobid.core.utilities.crossref.FunderDeserializer;
import org.grobid.core.utilities.crossref.WorkDeserializer;
import org.grobid.core.utilities.glutton.GluttonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.grobid.core.data.BiblioItem.cleanDOI;

/**
 * Singleton class for managing the extraction of bibliographical information from pdf documents.
 * When consolidation operations are realized, be sure to call the close() method
 * to ensure that all Executors are terminated.
 *
 */
public class Consolidation {
    private static final Logger LOGGER = LoggerFactory.getLogger(Consolidation.class);

    private static volatile Consolidation instance;

    private CrossrefClient client = null;
    private WorkDeserializer workDeserializer = null;
    private FunderDeserializer funderDeserializer = null;
    private CntManager cntManager = null;

    public static String CONSOLIDATION_STATUS_CONSOLIDATED = "consolidated";
    public static String CONSOLIDATION_STATUS_EXTRACTED = "extracted";


    public enum GrobidConsolidationService {
        CROSSREF("crossref"),
        GLUTTON("glutton");

        private final String ext;

        GrobidConsolidationService(String ext) {
            this.ext = ext;
        }

        public String getExt() {
            return ext;
        }

        public static GrobidConsolidationService get(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name of consolidation service must not be null");
            }

            String n = name.toLowerCase();
            for (GrobidConsolidationService e : values()) {
                if (e.name().toLowerCase().equals(n)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("No consolidation service with name '" + name +
                    "', possible values are: " + Arrays.toString(values()));
        }
    }

    /**
     * Value holder for extracted bibliographic query fields, used to deduplicate
     * field extraction logic across single and batch consolidation methods.
     */
    static class BibQueryFields {
        final String doi;
        final String halId;
        final String author;
        final String title;
        final String journalTitle;
        final String volume;
        final String firstPage;
        final String year;

        BibQueryFields(String doi, String halId, String author, String title,
                       String journalTitle, String volume, String firstPage, String year) {
            this.doi = doi;
            this.halId = halId;
            this.author = author;
            this.title = title;
            this.journalTitle = journalTitle;
            this.volume = volume;
            this.firstPage = firstPage;
            this.year = year;
        }
    }

    /**
     * Extract query fields from a BiblioItem, replacing duplicate extraction blocks
     * in single and batch consolidation methods.
     */
    static BibQueryFields extractFieldsFromBiblioItem(BiblioItem bib) {
        String doi = bib.getDOI();
        if (StringUtils.isNotBlank(doi)) {
            doi = cleanDOI(doi);
        }
        String halId = bib.getHalId();
        String author = bib.getFirstAuthorSurname();
        String title = bib.getTitle();
        String journalTitle = bib.getJournal();

        String volume = bib.getVolume();
        if (StringUtils.isBlank(volume))
            volume = bib.getVolumeBlock();

        String firstPage = null;
        String pageRange = bib.getPageRange();
        int beginPage = bib.getBeginPage();
        if (beginPage != -1) {
            firstPage = "" + beginPage;
        } else if (pageRange != null) {
            StringTokenizer st = new StringTokenizer(pageRange, "--");
            if (st.countTokens() == 2) {
                firstPage = st.nextToken();
            } else if (st.countTokens() == 1)
                firstPage = pageRange;
        }

        String year = null;
        if (bib.getNormalizedPublicationDate() != null) {
            year = "" + bib.getNormalizedPublicationDate().getYear();
        }
        if (year == null)
            year = bib.getYear();

        return new BibQueryFields(doi, halId, author, title, journalTitle, volume, firstPage, year);
    }

    /**
     * Build query arguments for work consolidation requests.
     *
     * @param fields extracted fields from the BiblioItem
     * @param rawCitation raw citation string, can be null
     * @param consolidateMode consolidation mode; use -1 for batch mode where query.bibliographic
     *                        is sent for CrossRef when DOI is absent
     * @param service the consolidation service being used
     * @return query arguments map, or null if insufficient data
     */
    static Map<String, String> buildWorkQueryArguments(BibQueryFields fields, String rawCitation,
                                                       int consolidateMode,
                                                       GrobidConsolidationService service) {
        Map<String, String> arguments = new HashMap<>();

        if (StringUtils.isNotBlank(fields.doi)) {
            arguments.put("doi", fields.doi);
        }

        // In batch mode (consolidateMode == -1), CrossRef gets query.bibliographic when no DOI
        if (consolidateMode != -1 && consolidateMode != 3 && StringUtils.isBlank(fields.doi)) {
            // single mode, non-DOI, non-mode-3
            if (StringUtils.isNotBlank(rawCitation) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("query.bibliographic", rawCitation);
            }
            if (StringUtils.isNotBlank(fields.halId) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("halid", fields.halId);
            }
            if (StringUtils.isNotBlank(fields.author) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("query.author", fields.author);
            }
            if (StringUtils.isNotBlank(fields.title) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("query.title", fields.title);
            }
            if (StringUtils.isNotBlank(fields.journalTitle) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("query.container-title", fields.journalTitle);
            }
            if (StringUtils.isNotBlank(fields.volume) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("volume", fields.volume);
            }
            if (StringUtils.isNotBlank(fields.firstPage) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("firstPage", fields.firstPage);
            }
            if (StringUtils.isNotBlank(fields.year) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("year", fields.year);
            }
        } else if (consolidateMode == -1 && StringUtils.isBlank(fields.doi)) {
            // batch mode, no DOI
            if (StringUtils.isNotBlank(fields.halId) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("halid", fields.halId);
            }
            if (StringUtils.isNotBlank(rawCitation)) {
                if (service != GrobidConsolidationService.CROSSREF || StringUtils.isBlank(fields.doi))
                    arguments.put("query.bibliographic", rawCitation);
            }
            if (StringUtils.isNotBlank(fields.title)) {
                if (service != GrobidConsolidationService.CROSSREF ||
                    (StringUtils.isBlank(rawCitation) && StringUtils.isBlank(fields.doi)))
                    arguments.put("query.title", fields.title);
            }
            if (StringUtils.isNotBlank(fields.author)) {
                if (service != GrobidConsolidationService.CROSSREF ||
                    (StringUtils.isBlank(rawCitation) && StringUtils.isBlank(fields.doi)))
                    arguments.put("query.author", fields.author);
            }
            if (StringUtils.isNotBlank(fields.journalTitle) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("query.container-title", fields.journalTitle);
            }
            if (StringUtils.isNotBlank(fields.volume) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("volume", fields.volume);
            }
            if (StringUtils.isNotBlank(fields.firstPage) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("firstPage", fields.firstPage);
            }
            if (StringUtils.isNotBlank(fields.year) && service != GrobidConsolidationService.CROSSREF) {
                arguments.put("year", fields.year);
            }
        }

        if (arguments.isEmpty()) {
            return null;
        }

        // check if there's enough information for CrossRef
        if (service == GrobidConsolidationService.CROSSREF) {
            if (StringUtils.isBlank(fields.doi) && StringUtils.isBlank(rawCitation) &&
                 (StringUtils.isBlank(fields.author) || StringUtils.isBlank(fields.title))) {
                return null;
            }
        }

        if (service == GrobidConsolidationService.CROSSREF) {
            arguments.put("rows", "1");
        } else if (service == GrobidConsolidationService.GLUTTON) {
            arguments.put("parseReference", "false");
        }

        return arguments;
    }

    /**
     * Build query arguments for funder consolidation requests.
     */
    static Map<String, String> buildFunderQueryArguments(Funder funder) {
        String funderNameString = funder.getFullName();
        if (StringUtils.isEmpty(funderNameString))
            return null;

        funderNameString = TextUtilities.removeFieldStopwords(funderNameString);

        Map<String, String> arguments = new HashMap<>();
        arguments.put("query", funderNameString);
        arguments.put("rows", "10");
        return arguments;
    }

    public static Consolidation getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of Consolidation");
        instance = new Consolidation();
    }

    /**
     * Hidden constructor
     */
    private Consolidation() {
        if (GrobidProperties.getInstance().getConsolidationService() == GrobidConsolidationService.GLUTTON) {
            client = GluttonClient.getInstance();
        } else {
            client = CrossrefClient.getInstance();
        }
        workDeserializer = new WorkDeserializer();
        funderDeserializer = new FunderDeserializer();
    }

    public void setCntManager(CntManager cntManager) {
        this.cntManager = cntManager;
    }

    public CntManager getCntManager() {
        return this.cntManager;
    }

    /**
     * After consolidation operations, this need to be called to ensure that all
     * involved Executors are shut down immediatly, otherwise non terminated thread
     * could prevent the JVM from exiting
     */
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.warn("Error closing consolidation client", e);
        }
        instance = null;
    }

    /**
     * Try to consolidate one bibliographical object with crossref metadata lookup web services based on
     * core metadata. In practice, this method is used for consolidating header metadata.
     */
    public BiblioItem consolidate(BiblioItem bib, String rawCitation, int consolidateMode) throws Exception {
        final List<BiblioItem> results = new ArrayList<>();

        BibQueryFields fields = extractFieldsFromBiblioItem(bib);
        final GrobidConsolidationService consolidationService = GrobidProperties.getInstance().getConsolidationService();

        Map<String, String> arguments = buildWorkQueryArguments(fields, rawCitation, consolidateMode, consolidationService);
        if (arguments == null) {
            return null;
        }

        if (cntManager != null)
            cntManager.i(ConsolidationCounters.CONSOLIDATION);

        long threadId = Thread.currentThread().getId();

        final boolean doiQuery;
        try {
            if (cntManager != null) {
                cntManager.i(ConsolidationCounters.CONSOLIDATION);
            }

            if (StringUtils.isNotBlank(fields.doi) && (cntManager != null)) {
                cntManager.i(ConsolidationCounters.CONSOLIDATION_PER_DOI);
                doiQuery = true;
            } else {
                doiQuery = false;
            }

            client.pushRequest(
                "works",
                arguments,
                workDeserializer,
                threadId,
                new CrossrefRequestListener<>(0) {

                @Override
                public void onSuccess(List<BiblioItem> res) {
                    if (CollectionUtils.isNotEmpty(res)) {
                        for(BiblioItem oneRes : res) {
                            /*
                              Glutton integrates its own post-validation, so we can skip post-validation in GROBID when it is used as
                              consolidation service.

                              Post-validation for CrossRef is configurable. When disabled, all results are accepted.
                              When enabled (default), DOI-based queries skip post-validation, and other queries require it.
                            */
                            if (consolidationService == GrobidConsolidationService.GLUTTON
                                || !GrobidProperties.getCrossrefPostValidation()
                                || doiQuery
                                || postValidation(bib, oneRes)) {
                                oneRes.setStatus(CONSOLIDATION_STATUS_CONSOLIDATED);
                                oneRes.setConsolidationService(consolidationService.getExt());
                                results.add(oneRes);
                                if (cntManager != null) {
                                    cntManager.i(ConsolidationCounters.CONSOLIDATION_SUCCESS);
                                    if (doiQuery)
                                        cntManager.i(ConsolidationCounters.CONSOLIDATION_PER_DOI_SUCCESS);
                                }
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onError(int status, String message, Exception exception) {
                    LOGGER.info("Consolidation service returns error ("+status+") : "+message, exception);
                }
            });
        } catch(Exception e) {
            LOGGER.info("Consolidation error - ", e);
        }

        client.finish(threadId);
        if (results.size() == 0)
            return null;
        else
            return results.get(0);
    }


    /**
     * Try to consolidate a list of bibliographical objects in one operation with consolidation services.
     * In practice this method is used for consolidating the metadata of all the extracted bibliographical
     * references.
     */
    public Map<Integer,BiblioItem> consolidate(List<BibDataSet> biblios) {
        if (CollectionUtils.isEmpty(biblios))
            return null;
        final Map<Integer,BiblioItem> results = new ConcurrentHashMap<>();
        int n = 0;
        long threadId = Thread.currentThread().getId();
        final GrobidConsolidationService consolidationService = GrobidProperties.getInstance().getConsolidationService();

        for(BibDataSet bibDataSet : biblios) {
            final BiblioItem theBiblio = bibDataSet.getResBib();

            if (cntManager != null)
                cntManager.i(ConsolidationCounters.TOTAL_BIB_REF);

            BibQueryFields fields = extractFieldsFromBiblioItem(theBiblio);
            String rawCitation = bibDataSet.getRawBib();

            Map<String, String> arguments = buildWorkQueryArguments(fields, rawCitation, -1, consolidationService);
            if (arguments == null) {
                n++;
                continue;
            }

            final boolean doiQuery;
            final String doi = fields.doi;
            try {
                if (cntManager != null) {
                    cntManager.i(ConsolidationCounters.CONSOLIDATION);
                }

                if (StringUtils.isNotBlank(doi) && (cntManager != null)) {
                    cntManager.i(ConsolidationCounters.CONSOLIDATION_PER_DOI);
                    doiQuery = true;
                } else {
                    doiQuery = false;
                }

                client.pushRequest("works", arguments, workDeserializer, threadId, new CrossrefRequestListener<>(n) {

                    @Override
                    public void onSuccess(List<BiblioItem> res) {
                        if (CollectionUtils.isNotEmpty(res) ) {
                            for(BiblioItem oneRes : res) {
                                if (consolidationService == GrobidConsolidationService.GLUTTON
                                    || !GrobidProperties.getCrossrefPostValidation()
                                    || postValidation(theBiblio, oneRes)) {
                                    oneRes.setLabeledTokens(theBiblio.getLabeledTokens());
                                    oneRes.setStatus(CONSOLIDATION_STATUS_CONSOLIDATED);
                                    oneRes.setConsolidationService(consolidationService.getExt());
                                    results.put(getRank(), oneRes);
                                    if (cntManager != null) {
                                        cntManager.i(ConsolidationCounters.CONSOLIDATION_SUCCESS);
                                        if (doiQuery)
                                            cntManager.i(ConsolidationCounters.CONSOLIDATION_PER_DOI_SUCCESS);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(int status, String message, Exception exception) {
                        LOGGER.info("Consolidation service returns error ("+status+") : "+message);
                    }
                });
            } catch(Exception e) {
                LOGGER.info("Consolidation error - ", e);
            }
            n++;
        }
        client.finish(threadId);

        return results;
    }

    /**
     * The public CrossRef API is a search API, and thus returns
     * many false positives. It is necessary to validate return results
     * against the (incomplete) source bibliographic item to block
     * inconsistent results.
     */
    private boolean postValidation(BiblioItem source, BiblioItem result) {
        boolean valid = true;

        if (!StringUtils.isBlank(source.getFirstAuthorSurname()) &&
            !StringUtils.isBlank(result.getFirstAuthorSurname())) {
            if (ratcliffObershelpDistance(source.getFirstAuthorSurname(),result.getFirstAuthorSurname(), false) < 0.8)
                return false;
        }

        return valid;
    }

    private double ratcliffObershelpDistance(String string1, String string2, boolean caseDependent) {
        if ( StringUtils.isBlank(string1) || StringUtils.isBlank(string2) )
            return 0.0;
        Double similarity = 0.0;
        if (!caseDependent) {
            string1 = string1.toLowerCase();
            string2 = string2.toLowerCase();
        }
        if (string1.equals(string2)) {
            similarity = 1.0;
        }

        if ( isNotEmpty(string1) && isNotEmpty(string2) ) {
            Option<Object> similarityObject =
                RatcliffObershelpMetric.compare(string1, string2);
            if (similarityObject.isDefined()) {
                similarity = (Double) similarityObject.get();
            }
        }

        return similarity;
    }

    public Funder consolidateFunder(Funder funder) {
        final List<Funder> results = new ArrayList<>();

        Map<String, String> arguments = buildFunderQueryArguments(funder);
        if (arguments == null)
            return null;

        long threadId = Thread.currentThread().getId();

        try {
            client.pushRequest("funders", arguments, funderDeserializer, threadId, new CrossrefRequestListener<Funder>(0) {
                @Override
                public void onSuccess(List<Funder> res) {
                    if ((res != null) && (res.size() > 0) ) {
                        for(Funder oneRes : res) {
                            if (oneRes.getFullName() != null) {
                                String localFullName = oneRes.getFullName();
                                localFullName = TextUtilities.removeFieldStopwords(localFullName);
                                if (ratcliffObershelpDistance(localFullName, arguments.get("query"), false)>0.9) {
                                    results.add(oneRes);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onError(int status, String message, Exception exception) {
                    LOGGER.info("Funder consolidation service returns error ("+status+") : "+message, exception);
                }
            });
        } catch(Exception e) {
            LOGGER.info("Funder consolidation error - ", e);
        }
        client.finish(threadId);
        if (results.size() == 0)
            return null;
        else
            return results.get(0);
    }

    public Map<Integer,Funder> consolidateFunders(List<Funder> funders) {
        if (CollectionUtils.isEmpty(funders))
            return null;
        final Map<Integer,Funder> results = new ConcurrentHashMap<>();
        int n = 0;
        long threadId = Thread.currentThread().getId();
        for(Funder funder : funders) {

            Map<String, String> arguments = buildFunderQueryArguments(funder);
            if (arguments == null) {
                n++;
                continue;
            }

            try {
                client.pushRequest("funders", arguments, funderDeserializer, threadId, new CrossrefRequestListener<Funder>(n) {
                    @Override
                    public void onSuccess(List<Funder> res) {
                        List<Funder> localResults = new ArrayList<>();
                        if (CollectionUtils.isNotEmpty(res) ) {
                            for(Funder oneRes : res) {
                                if (oneRes.getFullName() != null) {
                                    String localFullName = oneRes.getFullName();
                                    localFullName = TextUtilities.removeFieldStopwords(localFullName);
                                    if (localFullName.equalsIgnoreCase(arguments.get("query"))) {
                                        localResults.add(oneRes);
                                        break;
                                    } else if (ratcliffObershelpDistance(localFullName, arguments.get("query"), false)>0.9) {
                                        localResults.add(oneRes);
                                    }
                                }
                            }

                            if (localResults.size() >0)
                                results.put(Integer.valueOf(getRank()), localResults.get(0));
                        }
                    }

                    @Override
                    public void onError(int status, String message, Exception exception) {
                        LOGGER.info("Funder consolidation service returns error ("+status+") : "+message, exception);
                    }
                });
            } catch(Exception e) {
                LOGGER.info("Funder consolidation error - ", e);
            }
            n++;
        }

        client.finish(threadId);
        return results;
    }

}
