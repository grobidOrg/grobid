package org.grobid.service;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.AbstractEngineFactory;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.service.data.ServiceInfo;
import org.grobid.service.process.GrobidRestProcessFiles;
import org.grobid.service.process.GrobidRestProcessGeneric;
import org.grobid.service.process.GrobidRestProcessString;
import org.grobid.service.process.GrobidRestProcessTraining;
import org.grobid.service.util.BibTexMediaType;
import org.grobid.service.util.ExpectedResponseType;
import org.grobid.service.util.GrobidRestUtils;
import org.grobid.service.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.grobid.core.GrobidModels.Flavor.BLANK;


/**
 * RESTful service for the GROBID system.
 *
 */
@Timed
@Singleton
@Path(GrobidPaths.PATH_GROBID)
public class GrobidRestService implements GrobidPaths {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestService.class);

    private static volatile boolean initialized = false;
    private static volatile String initializationError = null;

    public static boolean isInitialized() {
        return initialized;
    }

    public static String getInitializationError() {
        return initializationError;
    }

    private static final String NAMES = "names";
    private static final String DATE = "date";
    private static final String AFFILIATIONS = "affiliations";
    public static final String CITATION = "citations";
    private static final String TEXT = "text";
    private static final String SHA1 = "sha1";
    private static final String XML = "xml";
    public static final String INPUT = "input";
    public static final String FLAVOR = "flavor";
    public static final String CONSOLIDATE_CITATIONS = "consolidateCitations";
    public static final String CONSOLIDATE_HEADER = "consolidateHeader";
    public static final String CONSOLIDATE_FUNDERS = "consolidateFunders";
    public static final String INCLUDE_RAW_AFFILIATIONS = "includeRawAffiliations";
    public static final String INCLUDE_RAW_CITATIONS = "includeRawCitations";
    public static final String INCLUDE_DISCARDED_TEXT = "includeDiscardedText";
    public static final String INCLUDE_RAW_COPYRIGHTS = "includeRawCopyrights";
    public static final String INCLUDE_FIGURES_TABLES = "includeFiguresTables";

    @Inject
    private GrobidRestProcessFiles restProcessFiles;

    @Inject
    private GrobidRestProcessGeneric restProcessGeneric;

    @Inject
    private GrobidRestProcessString restProcessString;

    @Inject
    private GrobidRestProcessTraining restProcessTraining;

    @Inject
    public GrobidRestService(GrobidServiceConfiguration configuration) {
        GrobidProperties.setGrobidHome(new File(configuration.getGrobid().getGrobidHome()).getAbsolutePath());
        /*if (configuration.getGrobid().getGrobidProperties() != null) {
            GrobidProperties.setGrobidPropertiesPath(new File(configuration.getGrobid().getGrobidProperties()).getAbsolutePath());
        } else {
            GrobidProperties.setGrobidPropertiesPath(new File(configuration.getGrobid().getGrobidHome(), "/config/grobid.properties").getAbsolutePath());
        }*/
        GrobidProperties.getInstance();
        GrobidProperties.setContextExecutionServer(true);
        LOGGER.info("Initiating Servlet GrobidRestService");
        AbstractEngineFactory.init();
        Engine engine = null;
        try {
            // this will init or not all the models in memory
            engine = Engine.getEngine(configuration.getGrobid().getModelPreload());
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time.");
            initializationError = "Could not get an engine from the pool within configured time.";
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs when initiating the grobid engine. ", exp);
            initializationError = "An unexpected exception occurs when initiating the grobid engine: " + exp.getMessage();
        } finally {
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        if (initializationError == null && TaggerFactory.hasFailures()) {
            Map<String, String> failed = TaggerFactory.getFailedModels();
            initializationError = "Models failed to load: " + String.join(", ", failed.keySet());
        }
        if (initializationError == null) {
            initialized = true;
        }
        LOGGER.info("Initiating of Servlet GrobidRestService finished.");
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessGeneric#isAlive()
     */
    @Path(GrobidPaths.PATH_IS_ALIVE)
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public Response isAlive() {
        return restProcessGeneric.isAlive();
    }

    @Path(GrobidPaths.PATH_GET_VERSION)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public ServiceInfo getVersion() {
        return new ServiceInfo(GrobidProperties.getVersion(), GrobidProperties.getRevision());
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessGeneric#getDescription_html(UriInfo)
     */
    @Produces(MediaType.TEXT_HTML)
    @GET
    @Path("grobid")
    public Response getDescription_html(@Context UriInfo uriInfo) {
        return restProcessGeneric.getDescription_html(uriInfo);
    }

    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processHeaderDocumentReturnXml_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_DISCARDED_TEXT) String includeDiscardedText,
        @DefaultValue("0") @FormDataParam("start") int startPage,
        @DefaultValue("2") @FormDataParam("end") int endPage) {
        int consol = validateConsolidationParam(consolidate);
        return restProcessFiles.processStatelessHeaderDocument(
            inputStream, consol,
            validateIncludeRawParam(includeRawAffiliations),
            validateIncludeRawParam(includeRawCopyrights),
            validateIncludeRawParam(includeDiscardedText),
            startPage,
            endPage,
            ExpectedResponseType.XML
        );
    }

    @Path(PATH_HEADER_FUNDING)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processHeaderFundingDocumentReturnXml_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_FUNDERS) String consolidateFunders,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_DISCARDED_TEXT) String includeDiscardedText) {
        int consolHeader = validateConsolidationParam(consolidateHeader);
        int consolFunders = validateConsolidationParam(consolidateFunders);
        return restProcessFiles.processStatelessHeaderFundingDocument(
            inputStream, consolHeader, consolFunders,
            validateIncludeRawParam(includeRawAffiliations),
            validateIncludeRawParam(includeRawCopyrights),
            validateIncludeRawParam(includeDiscardedText)
        );
    }


    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processStatelessHeaderDocumentReturnXml(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_DISCARDED_TEXT) String includeDiscardedText,
        @DefaultValue("0") @FormDataParam("start") int startPage,
        @DefaultValue("2") @FormDataParam("end") int endPage) {
        return processHeaderDocumentReturnXml_post(
            inputStream,
            consolidate,
            includeRawAffiliations,
            includeRawCopyrights,
            includeDiscardedText,
            startPage,
            endPage
        );
    }

    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @POST
    public Response processHeaderDocumentReturnBibTeX_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam("start") int startPage,
        @DefaultValue("2") @FormDataParam("end") int endPage) {
        int consol = validateConsolidationParam(consolidate);
        return restProcessFiles.processStatelessHeaderDocument(
            inputStream,
            consol,
            validateIncludeRawParam(includeRawAffiliations),
            validateIncludeRawParam(includeRawCopyrights),
            ExpectedResponseType.BIBTEX
        );
    }

    @Path(PATH_HEADER)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @PUT
    public Response processStatelessHeaderDocumentReturnBibTeX(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam("start") int startPage,
        @DefaultValue("2") @FormDataParam("end") int endPage) {
        return processHeaderDocumentReturnBibTeX_post(
            inputStream,
            consolidate,
            includeRawAffiliations,
            includeRawCopyrights,
            startPage,
            endPage
        );
    }

    @Path(PATH_FULL_TEXT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processFulltextDocument_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(FLAVOR) String flavor,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_FUNDERS) String consolidateFunders,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @DefaultValue("0") @FormDataParam(INCLUDE_DISCARDED_TEXT) String includeDiscardedText,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates
    ) throws Exception {
        return processFulltext(
            inputStream, flavor, consolidateHeader, consolidateCitations, consolidateFunders,
            includeRawAffiliations, includeRawCitations, includeRawCopyrights, includeDiscardedText,
            startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    @Path(PATH_FULL_TEXT)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processFulltextDocument(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(FLAVOR) String flavor,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_FUNDERS) String consolidateFunders,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @DefaultValue("0") @FormDataParam(INCLUDE_DISCARDED_TEXT) String includeDiscardedText,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates
    ) throws Exception {
        return processFulltext(
            inputStream, flavor, consolidateHeader, consolidateCitations, consolidateFunders,
            includeRawAffiliations, includeRawCitations, includeRawCopyrights, includeDiscardedText,
            startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    private Response processFulltext(InputStream inputStream,
                                     String flavor,
                                     String consolidateHeader,
                                     String consolidateCitations,
                                     String consolidateFunders,
                                     String includeRawAffiliations,
                                     String includeRawCitations,
                                     String includeRawCopyrights,
                                     String includeDiscardedText,
                                     int startPage,
                                     int endPage,
                                     String generateIDs,
                                     String segmentSentences,
                                     List<FormDataBodyPart> coordinates
    ) throws Exception {
        int consolHeader = validateConsolidationParam(consolidateHeader);
        int consolCitations = validateConsolidationParam(consolidateCitations);
        int consolFunders = validateConsolidationParam(consolidateFunders);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        boolean generate = validateGenerateIdParam(generateIDs);
        boolean segment = validateGenerateIdParam(segmentSentences);
        GrobidModels.Flavor flavorValidated = validateModelFlavor(flavor);

        List<String> teiCoordinates = collectCoordinates(coordinates);

        if (flavorValidated == BLANK) {
            return restProcessFiles.processFulltextDocumentBlank(
                inputStream,
                startPage,
                endPage,
                false,
                false,
                new ArrayList<>()
            );
        }

        return restProcessFiles.processFulltextDocument(
            inputStream, flavorValidated, consolHeader, consolCitations, consolFunders,
            validateIncludeRawParam(includeRawAffiliations),
            includeRaw,
            validateIncludeRawParam(includeRawCopyrights),
            validateIncludeRawParam(includeDiscardedText),
            startPage, endPage, generate, segment, teiCoordinates
        );
    }

    private GrobidModels.Flavor validateModelFlavor(String flavor) {
        return GrobidModels.Flavor.fromLabel(flavor);
    }

    private List<String> collectCoordinates(List<FormDataBodyPart> coordinates) {
        List<String> teiCoordinates = new ArrayList<>();
        if (coordinates != null) {
            for (FormDataBodyPart coordinate : coordinates) {
                String v = coordinate.getValueAs(String.class);
                teiCoordinates.add(v);
            }
        }
        return teiCoordinates;
    }

    private boolean validateGenerateIdParam(String generateIDs) {
        boolean generate = false;
        if ((generateIDs != null) && (generateIDs.equals("1") || generateIDs.equals("true"))) {
            generate = true;
        }
        return generate;
    }

    private boolean validateIncludeRawParam(String includeRaw) {
        return ((includeRaw != null) && (includeRaw.equals("1") || includeRaw.equals("true")));
    }

    private int validateConsolidationParam(String consolidate) {
        int consol = 0;
        if (consolidate != null) {
            try {
                consol = Integer.parseInt(consolidate);
            } catch (Exception e) {
                LOGGER.warn("Invalid consolidation parameter (should be an integer): " + consolidate, e);
            }
        }
        return consol;
    }

    @Path(PATH_FULL_TEXT_ASSET)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/zip")
    @POST
    public Response processFulltextAssetDocument_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(FLAVOR) String flavor,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_FUNDERS) String consolidateFunders,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates) throws Exception {
        return processStatelessFulltextAssetHelper(
            inputStream, flavor, consolidateHeader, consolidateCitations, consolidateFunders,
            includeRawAffiliations, includeRawCitations, includeRawCopyrights,
            startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    @Path(PATH_FULL_TEXT_ASSET)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/zip")
    @PUT
    public Response processStatelessFulltextAssetDocument(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(FLAVOR) String flavor,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_FUNDERS) String consolidateFunders,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_COPYRIGHTS) String includeRawCopyrights,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @DefaultValue("-1") @FormDataParam("start") int startPage,
        @DefaultValue("-1") @FormDataParam("end") int endPage,
        @FormDataParam("generateIDs") String generateIDs,
        @FormDataParam("segmentSentences") String segmentSentences,
        @FormDataParam("teiCoordinates") List<FormDataBodyPart> coordinates) throws Exception {
        return processStatelessFulltextAssetHelper(
            inputStream, flavor, consolidateHeader, consolidateCitations, consolidateFunders,
            includeRawAffiliations, includeRawCitations, includeRawCopyrights,
            startPage, endPage, generateIDs, segmentSentences, coordinates
        );
    }

    private Response processStatelessFulltextAssetHelper(InputStream inputStream,
                                                         String flavor,
                                                         String consolidateHeader,
                                                         String consolidateCitations,
                                                         String consolidateFunders,
                                                         String includeRawAffiliations,
                                                         String includeRawCopyrights,
                                                         String includeRawCitations,
                                                         int startPage,
                                                         int endPage,
                                                         String generateIDs,
                                                         String segmentSentences,
                                                         List<FormDataBodyPart> coordinates) throws Exception {
        int consolHeader = validateConsolidationParam(consolidateHeader);
        int consolCitations = validateConsolidationParam(consolidateCitations);
        int consolFunders = validateConsolidationParam(consolidateFunders);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        boolean generate = validateGenerateIdParam(generateIDs);
        boolean segment = validateGenerateIdParam(segmentSentences);
        List<String> teiCoordinates = collectCoordinates(coordinates);
        GrobidModels.Flavor validatedModelFlavor = validateModelFlavor(flavor);

        return restProcessFiles.processStatelessFulltextAssetDocument(
            inputStream, validatedModelFlavor, consolHeader, consolCitations, consolFunders,
            validateIncludeRawParam(includeRawAffiliations),
            includeRaw, validateIncludeRawParam(includeRawCopyrights),
            startPage, endPage, generate, segment, teiCoordinates
        );
    }

    /*@Path(PATH_CITATION_PATENT_TEI)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public StreamingOutput processCitationPatentTEI(@FormDataParam(INPUT) InputStream pInputStream,
                                                    @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String consolidate) throws Exception {
        int consol = validateConsolidationParam(consolidate);
        return restProcessFiles.processCitationPatentTEI(pInputStream, consol);
    }*/

    @Path(PATH_CITATION_PATENT_ST36)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processCitationPatentST36(
        @FormDataParam(INPUT) InputStream pInputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) throws Exception {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);

        pInputStream = ZipUtils.decompressStream(pInputStream);

        return restProcessFiles.processCitationPatentST36(pInputStream, consol, includeRaw);
    }

    @Path(PATH_CITATION_PATENT_PDF)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processCitationPatentPDF(
        @FormDataParam(INPUT) InputStream pInputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) throws Exception {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        return restProcessFiles.processCitationPatentPDF(pInputStream, consol, includeRaw);
    }

    @Path(PATH_CITATION_PATENT_TXT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processCitationPatentTXT_post(
        @FormParam(INPUT) String text,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        return restProcessString.processCitationPatentTXT(text, consol, includeRaw);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processDate(String)
     */
    @Path(PATH_DATE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processDate_post(@FormParam(DATE) String date) {
        return restProcessString.processDate(date);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processDate(String)
     */
    @Path(PATH_DATE)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processDate(@FormParam(DATE) String date) {
        return restProcessString.processDate(date);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processNamesHeader(String)
     */
    @Path(PATH_HEADER_NAMES)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processNamesHeader_post(@FormParam(NAMES) String names) {
        return restProcessString.processNamesHeader(names);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processNamesHeader(String)
     */
    @Path(PATH_HEADER_NAMES)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processNamesHeader(@FormParam(NAMES) String names) {
        return restProcessString.processNamesHeader(names);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processNamesCitation(String)
     */
    @Path(PATH_CITE_NAMES)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processNamesCitation_post(@FormParam(NAMES) String names) {
        return restProcessString.processNamesCitation(names);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processNamesCitation(String)
     */
    @Path(PATH_CITE_NAMES)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processNamesCitation(@FormParam(NAMES) String names) {
        return restProcessString.processNamesCitation(names);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processAffiliations(String)
     */
    @Path(PATH_AFFILIATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processAffiliations_post(@FormParam(AFFILIATIONS) String affiliations) {
        return restProcessString.processAffiliations(affiliations);
    }

    /**
     * @see org.grobid.service.process.GrobidRestProcessString#processAffiliations(String)
     */
    @Path(PATH_AFFILIATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public Response processAffiliations(@FormParam(AFFILIATIONS) String affiliation) {
        return restProcessString.processAffiliations(affiliation);
    }

    @Path(PATH_CITATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processCitationReturnXml_post(
        @FormParam(CITATION) String citation,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateCitations(validateConsolidationParam(consolidate))
            .includeRawCitations(validateIncludeRawParam(includeRawCitations))
            .build();
        return restProcessString.processCitation(citation, config, ExpectedResponseType.XML);
    }

    @Path(PATH_CITATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processCitationReturnXml(
        @FormParam(CITATION) String citation,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        return processCitationReturnXml_post(citation, consolidate, includeRawCitations);
    }

    @Path(PATH_CITATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @POST
    public Response processCitationReturnBibTeX_post(
        @FormParam(CITATION) String citation,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateCitations(validateConsolidationParam(consolidate))
            .includeRawCitations(validateIncludeRawParam(includeRawCitations))
            .build();
        return restProcessString.processCitation(citation, config, ExpectedResponseType.BIBTEX);
    }

    @Path(PATH_CITATION)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @PUT
    public Response processCitationReturnBibTeX(
        @FormParam(CITATION) String citation,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        return processCitationReturnBibTeX_post(citation, consolidate, includeRawCitations);
    }

    @Path(PATH_CITATION_LIST)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processCitationListReturnXml_post(
        @FormParam(CITATION) List<String> citations,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateCitations(validateConsolidationParam(consolidate))
            .includeRawCitations(validateIncludeRawParam(includeRawCitations))
            .build();
        return restProcessString.processCitationList(citations, config, ExpectedResponseType.XML);
    }

    @Path(PATH_CITATION_LIST)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @POST
    public Response processCitationListReturnBibTeX_post(
        @FormParam(CITATION) List<String> citations,
        @DefaultValue("0") @FormParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder()
            .consolidateCitations(validateConsolidationParam(consolidate))
            .includeRawCitations(validateIncludeRawParam(includeRawCitations))
            .build();
        return restProcessString.processCitationList(citations, config, ExpectedResponseType.BIBTEX);
    }

    @Path(PATH_REFERENCES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public Response processStatelessReferencesDocumentReturnXml_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        return restProcessFiles.processStatelessReferencesDocument(inputStream, consol, includeRaw, ExpectedResponseType.XML);
    }

    @Path(PATH_REFERENCES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_XML)
    @PUT
    public Response processStatelessReferencesDocumentReturnXml(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        return processStatelessReferencesDocumentReturnXml_post(inputStream, consolidate, includeRawCitations);
    }

    @Path(PATH_REFERENCES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @POST
    public Response processStatelessReferencesDocumentReturnBibTeX_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        return restProcessFiles.processStatelessReferencesDocument(inputStream, consol, includeRaw, ExpectedResponseType.BIBTEX);
    }

    @Path(PATH_REFERENCES)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(BibTexMediaType.MEDIA_TYPE)
    @PUT
    public Response processStatelessReferencesDocumentReturnBibTeX(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) {
        return processStatelessReferencesDocumentReturnBibTeX_post(inputStream, consolidate, includeRawCitations);
    }

    @Path(PATH_PDF_ANNOTATION)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/pdf")
    @POST
    public Response processAnnotatePDF(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(INPUT) FormDataBodyPart inputBodyPart,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_AFFILIATIONS) String includeRawAffiliations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @FormDataParam("type") int type) throws Exception {
        String fileName = inputBodyPart.getFormDataContentDisposition().getFileName();
        int consolHeader = validateConsolidationParam(consolidateHeader);
        int consolCitations = validateConsolidationParam(consolidateCitations);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);

        return restProcessFiles.processPDFAnnotation(
            inputStream, fileName, consolHeader, consolCitations,
            validateIncludeRawParam(includeRawAffiliations),
            includeRaw,
            GrobidRestUtils.getAnnotationFor(type)
        );
    }

    @Path(PATH_REFERENCES_PDF_ANNOTATION)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response processPDFReferenceAnnotation(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_HEADER) String consolidateHeader,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidateCitations,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations,
        @DefaultValue("0") @FormDataParam(INCLUDE_FIGURES_TABLES) String includeFiguresTables) throws Exception {
        int consolHeader = validateConsolidationParam(consolidateHeader);
        int consolCitations = validateConsolidationParam(consolidateCitations);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        boolean includeFig = validateIncludeRawParam(includeFiguresTables);
        return restProcessFiles.processPDFReferenceAnnotation(inputStream, consolHeader, consolCitations, includeRaw, includeFig);
    }

    @Path(PATH_CITATIONS_PATENT_PDF_ANNOTATION)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response annotatePDFPatentCitation(
        @FormDataParam(INPUT) InputStream inputStream,
        @DefaultValue("0") @FormDataParam(CONSOLIDATE_CITATIONS) String consolidate,
        @DefaultValue("0") @FormDataParam(INCLUDE_RAW_CITATIONS) String includeRawCitations) throws Exception {
        int consol = validateConsolidationParam(consolidate);
        boolean includeRaw = validateIncludeRawParam(includeRawCitations);
        return restProcessFiles.annotateCitationPatentPDF(inputStream, consol, includeRaw);
    }

    public void setRestProcessFiles(GrobidRestProcessFiles restProcessFiles) {
        this.restProcessFiles = restProcessFiles;
    }

    public void setRestProcessGeneric(GrobidRestProcessGeneric restProcessGeneric) {
        this.restProcessGeneric = restProcessGeneric;
    }

    public void setRestProcessString(GrobidRestProcessString restProcessString) {
        this.restProcessString = restProcessString;
    }

    @Path(PATH_FUNDING_ACKNOWLEDGEMENT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    @POST
    public Response processFundingAcknowledgementPost(@FormParam(TEXT) String text,
                                                      @DefaultValue("0") @FormParam("generateIDs") String generateIDs,
                                                      @DefaultValue("0") @FormParam("segmentSentences") String segmentSentences) {
        boolean generate = validateGenerateIdParam(generateIDs);
        boolean segment = validateGenerateIdParam(segmentSentences);
        return restProcessString.processFundingAcknowledgement(text, generate, segment);
    }

    // API for training

    @Path(PATH_MODEL_TRAINING)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response trainModel(@FormParam("model") String model,
                               @DefaultValue("crf") @FormParam("architecture") String architecture,
                               @DefaultValue("split") @FormParam("type") String type,
                               @DefaultValue("0.9") @FormParam("ratio") double ratio,
                               @DefaultValue("10") @FormParam("n") int n,
                               @DefaultValue("0") @FormParam("incremental") String incremental) {
        boolean incrementalVal = validateIncludeRawParam(incremental);
        return restProcessTraining.trainModel(model, architecture, type, ratio, n, incrementalVal);
    }

    @Path(PATH_TRAINING_RESULT)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response resultTraining(@FormParam("token") String token) {
        return restProcessTraining.resultTraining(token);
    }

    @Path(PATH_MODEL)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/zip")
    @GET
    public Response getModel(@QueryParam("model") String model,
                             @QueryParam("architecture") String architecture) {
        return restProcessTraining.getModel(model, architecture);
    }

    @Path(PATH_MODEL)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/zip")
    @POST
    public Response getModel_post(@FormParam("model") String model,
                                  @FormParam("architecture") String architecture) {
        return restProcessTraining.getModel(model, architecture);
    }

    @Path(PATH_CREATE_TRAINING)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/zip")
    @POST
    public Response createTraining_post(
        @FormDataParam(INPUT) InputStream inputStream,
        @FormDataParam(INPUT) FormDataBodyPart inputBodyPart,
        @FormDataParam(FLAVOR) String flavor
    ) {
        GrobidModels.Flavor validatedModelFlavor = validateModelFlavor(flavor);
        String fileName = inputBodyPart.getFormDataContentDisposition().getFileName();
        return restProcessTraining.createTraining(
            inputStream, fileName, validatedModelFlavor
        );
    }

}
