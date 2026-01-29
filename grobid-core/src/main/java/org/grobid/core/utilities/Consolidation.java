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

import java.util.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.grobid.core.data.BiblioItem.cleanDOI;

/**
 * Singleton class for managing the extraction of bibliographical information from pdf documents. When consolidation
 * operations are realized, be sure to call the close() method to ensure that all Executors are terminated.
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
        CROSSREF("crossref"), GLUTTON("glutton");

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
            throw new IllegalArgumentException("No consolidation service with name '"
                    + name
                    + "', possible values are: "
                    + Arrays.toString(values()));
        }
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
     * After consolidation operations, this need to be called to ensure that all involved Executors are shut down
     * immediatly, otherwise non terminated thread could prevent the JVM from exiting
     */
    public void close() {
        // client.close();
    }

    /**
     * Try to consolidate one bibliographical object with crossref metadata lookup web services based on core metadata.
     * In practice, this method is used for consolidating header metadata.
     */
    public BiblioItem consolidate(BiblioItem bib, String rawCitation, int consolidateMode) throws Exception {
        final List<BiblioItem> results = new ArrayList<>();

        String theDOI = bib.getDOI();
        if (StringUtils.isNotBlank(theDOI)) {
            theDOI = cleanDOI(theDOI);
        }
        final String doi = theDOI;
        String halId = bib.getHalId();
        String aut = bib.getFirstAuthorSurname();
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

        if (cntManager != null)
            cntManager.i(ConsolidationCounters.CONSOLIDATION);

        long threadId = Thread.currentThread().getId();
        Map<String, String> arguments = new HashMap<>();

        GrobidConsolidationService consolidationService = GrobidProperties.getInstance().getConsolidationService();
        if (StringUtils.isNotBlank(doi)) {
            // call based on the identified DOI
            arguments.put("doi", doi);
        } else if (consolidateMode != 3) {
            if (StringUtils.isNotBlank(rawCitation)) {
                // call with full raw string
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("query.bibliographic", rawCitation);
                }
            }
            if (StringUtils.isNotBlank(halId)) {
                // call based on the identified HAL ID
                if (consolidationService != GrobidConsolidationService.CROSSREF)
                    arguments.put("halid", halId);
            }
            if (StringUtils.isNotBlank(aut)) {
                // call based on partial metadata
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("query.author", aut);
                }
            }
            if (StringUtils.isNotBlank(title)) {
                // call based on partial metadata
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("query.title", title);
                }
            }
            if (StringUtils.isNotBlank(journalTitle)) {
                // call based on partial metadata
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("query.container-title", journalTitle);
                }
            }
            if (StringUtils.isNotBlank(volume)) {
                // call based on partial metadata
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("volume", volume);
                }
            }
            if (StringUtils.isNotBlank(firstPage)) {
                // call based on partial metadata
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("firstPage", firstPage);
                }
            }
            if (StringUtils.isNotBlank(year)) {
                // publication year metadata, CrossRef has no year query field, they are supported by the
                // query.bibliographic
                // field and filter
                if (consolidationService != GrobidConsolidationService.CROSSREF) {
                    arguments.put("year", year);
                }
            }
        }
        if (MapUtils.isEmpty(arguments)) {
            return null;
        }

        if (consolidationService == GrobidConsolidationService.CROSSREF) {
            if (StringUtils.isBlank(doi) && StringUtils.isBlank(rawCitation)
                    && (StringUtils.isBlank(aut) || StringUtils.isBlank(title))) {
                // there's not enough information for a crossref request, which might always return a result
                return null;
            }
        }

        if (consolidationService == GrobidConsolidationService.CROSSREF) {
            arguments.put("rows", "1"); // we just request the top-one result
        } else if (consolidationService == GrobidConsolidationService.GLUTTON) {
            // GROBID has already parsed the reference, so no need to redo this in glutton
            arguments.put("parseReference", "false");
        }

        final boolean doiQuery;
        try {
            // CrossrefRequestListener<BiblioItem> requestListener = new CrossrefRequestListener<BiblioItem>();
            if (cntManager != null) {
                cntManager.i(ConsolidationCounters.CONSOLIDATION);
            }

            if (StringUtils.isNotBlank(doi) && (cntManager != null)) {
                cntManager.i(ConsolidationCounters.CONSOLIDATION_PER_DOI);
                doiQuery = true;
            } else {
                doiQuery = false;
            }

            client.pushRequest("works", arguments, workDeserializer, threadId, new CrossrefRequestListener<>(0) {

                @Override
                public void onSuccess(List<BiblioItem> res) {
                    if (CollectionUtils.isNotEmpty(res)) {
                        // we need here to post-check that the found item corresponds
                        // correctly to the one requested in order to avoid false positive
                        for (BiblioItem oneRes : res) {
                            /*
                             * Glutton integrates its own post-validation, so we can skip post-validation in GROBID when
                             * it is used as consolidation service.
                             *
                             * In case of crossref REST API, for single bib. ref. consolidation (this case comes only
                             * for header extraction), having an extracted DOI matching is considered safe enough, and
                             * we don't require further post-validation.
                             *
                             * For all the other case of matching with CrossRef, we require a post-validation.
                             */
                            if ((consolidationService == GrobidConsolidationService.GLUTTON)
                                    || ((consolidationService == GrobidConsolidationService.CROSSREF) && doiQuery)
                                    || ((consolidationService == GrobidConsolidationService.CROSSREF)
                                            && postValidation(bib, oneRes))) {
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
                    LOGGER.info("Consolidation service returns error (" + status + ") : " + message, exception);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Consolidation error - ", e);
        }

        client.finish(threadId);
        if (results.size() == 0)
            return null;
        else
            return results.get(0);
    }

    /**
     * Try tp consolidate a list of bibliographical objects in one operation with consolidation services. In practice
     * this method is used for consolidating the metadata of all the extracted bibliographical references.
     */
    public Map<Integer, BiblioItem> consolidate(List<BibDataSet> biblios) {
        if (CollectionUtils.isEmpty(biblios))
            return null;
        final Map<Integer, BiblioItem> results = new HashMap<>();
        // init the results
        int n = 0;
        for (n = 0; n < biblios.size(); n++) {
            results.put(n, null);
        }
        n = 0;
        long threadId = Thread.currentThread().getId();
        for (BibDataSet bibDataSet : biblios) {
            final BiblioItem theBiblio = bibDataSet.getResBib();

            if (cntManager != null)
                cntManager.i(ConsolidationCounters.TOTAL_BIB_REF);

            // first we get the exploitable metadata
            String doi = theBiblio.getDOI();
            if (StringUtils.isNotBlank(doi)) {
                doi = BiblioItem.cleanDOI(doi);
            }
            // first we get the exploitable metadata
            String halId = theBiblio.getHalId();
            String aut = theBiblio.getFirstAuthorSurname();
            String title = theBiblio.getTitle();
            String journalTitle = theBiblio.getJournal();

            // and the row string
            String rawCitation = bibDataSet.getRawBib();

            Map<String, String> arguments = null;

            String volume = theBiblio.getVolume();
            if (StringUtils.isBlank(volume))
                volume = theBiblio.getVolumeBlock();

            String firstPage = null;
            String pageRange = theBiblio.getPageRange();
            int beginPage = theBiblio.getBeginPage();
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
            if (theBiblio.getNormalizedPublicationDate() != null) {
                year = "" + theBiblio.getNormalizedPublicationDate().getYear();
            }
            if (year == null)
                year = theBiblio.getYear();

            /*
             * if (aut != null) { aut = TextUtilities.removeAccents(aut); } if (title != null) { title =
             * TextUtilities.removeAccents(title); } if (journalTitle != null) { journalTitle =
             * TextUtilities.removeAccents(journalTitle); }
             */

            if (StringUtils.isNotBlank(doi)) {
                // call based on the identified DOI
                arguments = new HashMap<String, String>();
                arguments.put("doi", doi);
            }
            if (StringUtils.isNotBlank(halId)) {
                // call based on the identified HAL ID
                if (arguments == null)
                    arguments = new HashMap<String, String>();
                if (GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF)
                    arguments.put("halid", halId);
            }
            if (StringUtils.isNotBlank(rawCitation)) {
                // call with full raw string
                if (arguments == null)
                    arguments = new HashMap<String, String>();
                if ((GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF)
                        || StringUtils.isBlank(doi))
                    arguments.put("query.bibliographic", rawCitation);
            }
            if (StringUtils.isNotBlank(title)) {
                // call based on partial metadata
                if (arguments == null)
                    arguments = new HashMap<String, String>();
                if ((GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF)
                        || (StringUtils.isBlank(rawCitation) && StringUtils.isBlank(doi)))
                    arguments.put("query.title", title);
            }
            if (StringUtils.isNotBlank(aut)) {
                // call based on partial metadata
                if (arguments == null)
                    arguments = new HashMap<String, String>();
                if ((GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF)
                        || (StringUtils.isBlank(rawCitation) && StringUtils.isBlank(doi)))
                    arguments.put("query.author", aut);
            }
            if (StringUtils.isNotBlank(journalTitle)) {
                // call based on partial metadata
                if (GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF) {
                    if (arguments == null)
                        arguments = new HashMap<String, String>();
                    arguments.put("query.container-title", journalTitle);
                }
            }
            if (StringUtils.isNotBlank(volume)) {
                // call based on partial metadata
                if (GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF) {
                    if (arguments == null)
                        arguments = new HashMap<String, String>();
                    arguments.put("volume", volume);
                }
            }
            if (StringUtils.isNotBlank(firstPage)) {
                // call based on partial metadata
                if (GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF) {
                    if (arguments == null)
                        arguments = new HashMap<String, String>();
                    arguments.put("firstPage", firstPage);
                }
            }
            if (StringUtils.isNotBlank(year)) {
                // publication year metadata, CrossRef has no year query field, they are supported by the
                // query.bibliographic
                // field and filter
                if (GrobidProperties.getInstance().getConsolidationService() != GrobidConsolidationService.CROSSREF) {
                    if (arguments == null)
                        arguments = new HashMap<String, String>();
                    arguments.put("year", year);
                }
            }

            if (arguments == null || arguments.size() == 0) {
                n++;
                continue;
            }

            if (GrobidProperties.getInstance().getConsolidationService() == GrobidConsolidationService.CROSSREF) {
                if (StringUtils.isBlank(doi) && StringUtils.isBlank(rawCitation)
                        && (StringUtils.isBlank(aut) || StringUtils.isBlank(title))) {
                    // there's not enough information for a crossref request, which might always return a result
                    n++;
                    continue;
                }
            }

            if (GrobidProperties.getInstance().getConsolidationService() == GrobidConsolidationService.CROSSREF)
                arguments.put("rows", "1"); // we just request the top-one result
            else if (GrobidProperties.getInstance().getConsolidationService() == GrobidConsolidationService.GLUTTON) {
                // GROBID has already parsed the reference, so no need to redo this in glutton
                arguments.put("parseReference", "false");
            }

            final boolean doiQuery;
            try {
                // CrossrefRequestListener<BiblioItem> requestListener = new CrossrefRequestListener<BiblioItem>();
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
                        if (CollectionUtils.isNotEmpty(res)) {
                            // for CrossRef API we need here to post-validate if the found item corresponds
                            // to the one requested in order to avoid false positive
                            // Glutton has its own validation mechanisms
                            for (BiblioItem oneRes : res) {
                                if (GrobidProperties.getInstance()
                                        .getConsolidationService() == GrobidConsolidationService.GLUTTON
                                        || postValidation(theBiblio, oneRes)) {
                                    oneRes.setLabeledTokens(theBiblio.getLabeledTokens());
                                    oneRes.setStatus(CONSOLIDATION_STATUS_CONSOLIDATED);
                                    // We set the consolidation here
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
                        LOGGER.info("Consolidation service returns error (" + status + ") : " + message);
                    }
                });
            } catch (Exception e) {
                LOGGER.info("Consolidation error - ", e);
            }
            n++;
        }
        client.finish(threadId);

        return results;
    }

    /**
     * The public CrossRef API is a search API, and thus returns many false positives. It is necessary to validate
     * return results against the (incomplete) source bibliographic item to block inconsistent results.
     */
    private boolean postValidation(BiblioItem source, BiblioItem result) {
        boolean valid = true;

        // check main metadata available in source with fuzzy matching
        /*
         * if (!StringUtils.isBlank(source.getTitle()) && !StringUtils.isBlank(source.getTitle())) {
         * //System.out.println(source.getTitle() + " / " + result.getTitle() + " = " +
         * ratcliffObershelpDistance(source.getTitle(), result.getTitle(), false)); if
         * (ratcliffObershelpDistance(source.getTitle(), result.getTitle(), false) < 0.8) return false; }
         */

        if (!StringUtils.isBlank(source.getFirstAuthorSurname())
                && !StringUtils.isBlank(result.getFirstAuthorSurname())) {
            // System.out.println(source.getFirstAuthorSurname() + " / " + result.getFirstAuthorSurname() + " = " +
            // ratcliffObershelpDistance(source.getFirstAuthorSurname(), result.getFirstAuthorSurname(), false));
            if (ratcliffObershelpDistance(source.getFirstAuthorSurname(), result.getFirstAuthorSurname(), false) < 0.8)
                return false;
        }

        /*
         * if (!StringUtils.isBlank(source.getPublicationDate()) && !StringUtils.isBlank(result.getPublicationDate())) {
         * if (!source.getPublicationDate().equals(result.getPublicationDate())) valid = false; }
         */

        return valid;
    }

    private double ratcliffObershelpDistance(String string1, String string2, boolean caseDependent) {
        if (StringUtils.isBlank(string1) || StringUtils.isBlank(string2))
            return 0.0;
        Double similarity = 0.0;
        if (!caseDependent) {
            string1 = string1.toLowerCase();
            string2 = string2.toLowerCase();
        }
        if (string1.equals(string2)) {
            similarity = 1.0;
        }

        if (isNotEmpty(string1) && isNotEmpty(string2)) {
            Option<Object> similarityObject = RatcliffObershelpMetric.compare(string1, string2);
            if (similarityObject.isDefined()) {
                similarity = (Double) similarityObject.get();
            }
        }

        return similarity;
    }

    public Funder consolidateFunder(Funder funder) {
        final List<Funder> results = new ArrayList<>();

        Map<String, String> arguments = new HashMap<String, String>();

        // CrossRef does not manage stopwords in funder search and has no usable term frequency, so we need
        // to remove basic stopwords in the query to have something manageable from CrossRef
        String funderNameString = funder.getFullName();
        if (StringUtils.isEmpty(funderNameString))
            return null;

        funderNameString = TextUtilities.removeFieldStopwords(funderNameString);

        arguments.put("query", funderNameString);
        arguments.put("rows", "10"); // we request the top-10 results, because there are a lot of noise
        // and we need many candidates in the pairwise comparison step

        long threadId = Thread.currentThread().getId();

        try {
            client.pushRequest(
                    "funders",
                    arguments,
                    funderDeserializer,
                    threadId,
                    new CrossrefRequestListener<Funder>(0) {
                        @Override
                        public void onSuccess(List<Funder> res) {
                            if ((res != null) && (res.size() > 0)) {
                                // we need here to post-check the candidates in a pairwise comparison
                                // in order to avoid false positive
                                for (Funder oneRes : res) {
                                    /*
                                     * Glutton integrates its own post-validation, so we can skip post-validation in
                                     * GROBID when it is used as consolidation service. However, with CrossRef,
                                     * post-validation is mandatory to control false positives.
                                     */
                                    if (oneRes.getFullName() != null) {
                                        String localFullName = oneRes.getFullName();
                                        localFullName = TextUtilities.removeFieldStopwords(localFullName);
                                        // if (localFullName.toLowerCase().equals(arguments.get("query").toLowerCase()))
                                        // {
                                        if (ratcliffObershelpDistance(
                                                localFullName,
                                                arguments.get("query"),
                                                false) > 0.9) {
                                            results.add(oneRes);
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(int status, String message, Exception exception) {
                            LOGGER.info(
                                    "Funder consolidation service returns error (" + status + ") : " + message,
                                    exception);
                        }
                    });
        } catch (Exception e) {
            LOGGER.info("Funder consolidation error - ", e);
        }
        client.finish(threadId);
        if (results.size() == 0)
            return null;
        else
            return results.get(0);
    }

    public Map<Integer, Funder> consolidateFunders(List<Funder> funders) {
        if (CollectionUtils.isEmpty(funders))
            return null;
        final Map<Integer, Funder> results = new HashMap<>();
        // init the results
        int n = 0;
        for (n = 0; n < funders.size(); n++) {
            results.put(n, null);
        }
        n = 0;
        long threadId = Thread.currentThread().getId();
        for (Funder funder : funders) {
            // final List<Funder> results = new ArrayList<>();

            Map<String, String> arguments = new HashMap<String, String>();

            // CrossRef does not manage stopwords in funder search and has no usable term frequency, so we need
            // to remove basic stopwords in the query to have something manageable from CrossRef
            String funderNameString = funder.getFullName();
            if (StringUtils.isEmpty(funderNameString))
                return null;

            funderNameString = TextUtilities.removeFieldStopwords(funderNameString);

            arguments.put("query", funderNameString);
            arguments.put("rows", "10"); // we request the top-10 results, because there are a lot of noise
            // and we need many candidates in the pairwise comparison step

            try {
                client.pushRequest(
                        "funders",
                        arguments,
                        funderDeserializer,
                        threadId,
                        new CrossrefRequestListener<Funder>(n) {
                            @Override
                            public void onSuccess(List<Funder> res) {
                                List<Funder> localResults = new ArrayList<>();
                                if (CollectionUtils.isNotEmpty(res)) {
                                    // we need here to post-check the candidates in a pairwise comparison
                                    // in order to avoid false positive
                                    for (Funder oneRes : res) {
                                        /*
                                         * Glutton integrates its own post-validation, so we can skip post-validation in
                                         * GROBID when it is used as consolidation service. However, with CrossRef,
                                         * post-validation is mandatory to control false positives.
                                         */
                                        if (oneRes.getFullName() != null) {
                                            String localFullName = oneRes.getFullName();
                                            localFullName = TextUtilities.removeFieldStopwords(localFullName);
                                            if (localFullName.equalsIgnoreCase(arguments.get("query"))) {
                                                localResults.add(oneRes);
                                                break;
                                            } else if (ratcliffObershelpDistance(
                                                    localFullName,
                                                    arguments.get("query"),
                                                    false) > 0.9) {
                                                localResults.add(oneRes);
                                            }
                                        }
                                    }

                                    if (localResults.size() > 0)
                                        results.put(Integer.valueOf(getRank()), localResults.get(0));
                                }
                            }

                            @Override
                            public void onError(int status, String message, Exception exception) {
                                LOGGER.info(
                                        "Funder consolidation service returns error (" + status + ") : " + message,
                                        exception);
                            }
                        });
            } catch (Exception e) {
                LOGGER.info("Funder consolidation error - ", e);
            }
            n++;
        }

        client.finish(threadId);
        return results;
    }

}
