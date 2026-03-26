package org.grobid.service.process;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.PatentItem;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.Engine;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.visualization.BlockVisualizer;
import org.grobid.core.visualization.CitationsVisualizer;
import org.grobid.core.visualization.FigureTableVisualizer;
import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.service.util.BibTexMediaType;
import org.grobid.service.util.ExpectedResponseType;
import org.grobid.service.util.GrobidRestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

/**
 * Web services consuming a file
 */
@Singleton
public class GrobidRestProcessFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestProcessFiles.class);

    @Inject
    public GrobidRestProcessFiles() {

    }

    /**
     * Uploads the origin document which shall be extracted into TEI and
     * extracts only the header data.
     *
     * @param inputStream the data of origin document
     * @param consolidate consolidation parameter for the header extraction
     * @return a response object which contains a TEI representation of the header part
     */
     public Response processStatelessHeaderDocument(
        final InputStream inputStream,
        final int consolidate,
        final boolean includeRawAffiliations,
        final boolean includeRawCopyrights,
        ExpectedResponseType expectedResponseType
    ) {
         return processStatelessHeaderDocument(
             inputStream,
             consolidate,
             includeRawAffiliations,
             includeRawCopyrights,
             false,
             0,
             2,
             expectedResponseType
         );
     }

     public Response processStatelessHeaderDocument(
        final InputStream inputStream,
        final int consolidate,
        final boolean includeRawAffiliations,
        final boolean includeRawCopyrights,
        int startPage,
        int endPage,
        ExpectedResponseType expectedResponseType
    ) {
         return processStatelessHeaderDocument(
             inputStream,
             consolidate,
             includeRawAffiliations,
             includeRawCopyrights,
             false,
             startPage,
             endPage,
             expectedResponseType
         );
     }

    public Response processStatelessHeaderDocument(
        final InputStream inputStream,
        final int consolidate,
        final boolean includeRawAffiliations,
        final boolean includeRawCopyrights,
        final boolean includeDiscardedText,
        int startPage,
        int endPage,
        ExpectedResponseType expectedResponseType
    ) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            BiblioItem result = new BiblioItem();

            // starts conversion process
            retVal = engine.processHeader(
                originFile.getAbsolutePath(),
                md5Str,
                consolidate,
                includeRawAffiliations,
                includeRawCopyrights,
                includeDiscardedText,
                startPage,
                endPage,
                result
            );

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else if (expectedResponseType == ExpectedResponseType.BIBTEX) {
                response = Response.status(Response.Status.OK)
                    .entity(result.toBibTeX(result.generateBibTeXKey()))
                    .header(HttpHeaders.CONTENT_TYPE, BibTexMediaType.MEDIA_TYPE + "; charset=UTF-8")
                    .build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document which shall be extracted into TEI and
     * extracts only the header and funding information, this still requires a full read and segmentation of the document,
     * but non-relevant parts are skipt.
     *
     * @param inputStream the data of origin document
     * @param consolidateHeader consolidation parameter for the header extraction
     * @param consolidateFunders consolidation parameter for the funder extraction
     * @return a response object which contains a TEI representation of the header part
     */
    public Response processStatelessHeaderFundingDocument(
        final InputStream inputStream,
        final int consolidateHeader,
        final int consolidateFunders,
        final boolean includeRawAffiliations,
        final boolean includeRawCopyrights,
        final boolean includeDiscardedText
    ) {
        LOGGER.debug(methodLogIn());
        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();

            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written. ", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            retVal = engine.processHeaderFunding(
                originFile,
                md5Str,
                consolidateHeader,
                consolidateFunders,
                includeRawAffiliations,
                includeRawCopyrights,
                includeDiscardedText
            );

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document which shall be extracted into TEI.
     *
     * @param inputStream          the data of origin document
     * @param consolidateHeader    the consolidation option allows GROBID to exploit Crossref
     *                             for improving header information
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref
     *                             for improving citations information
     * @param startPage            give the starting page to consider in case of segmentation of the
     *                             PDF, -1 for the first page (default)
     * @param endPage              give the end page to consider in case of segmentation of the
     *                             PDF, -1 for the last page (default)
     * @param generateIDs          if true, generate random attribute id on the textual elements of
     *                             the resulting TEI
     * @param segmentSentences     if true, return results with segmented sentences
     * @return a response object mainly contain the TEI representation of the
     * full text
     */
    public Response processFulltextDocument(final InputStream inputStream,
                                        final GrobidModels.Flavor flavor,
                                        final int consolidateHeader,
                                        final int consolidateCitations,
                                        final int consolidateFunders,
                                        final boolean includeRawAffiliations,
                                        final boolean includeRawCitations,
                                        final boolean includeRawCopyrights,
                                        final boolean includeDiscardedText,
                                        final int startPage,
                                        final int endPage,
                                        final boolean generateIDs,
                                        final boolean segmentSentences,
                                        final List<String> teiCoordinates) throws Exception {
        LOGGER.debug(methodLogIn());

        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .consolidateHeader(consolidateHeader)
                    .consolidateCitations(consolidateCitations)
                    .consolidateFunders(consolidateFunders)
                    .includeRawAffiliations(includeRawAffiliations)
                    .includeRawCitations(includeRawCitations)
                    .includeRawCopyrights(includeRawCopyrights)
                    .includeDiscardedText(includeDiscardedText)
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .withSentenceSegmentation(segmentSentences)
                    .flavor(flavor)
                    .build();

            retVal = engine.fullTextToTEI(originFile, flavor, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }

            if (originFile != null)
              IOUtilities.removeTempFile(originFile);
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    public Response processFulltextDocumentBlank(final InputStream inputStream,
                                                 final int startPage,
                                                 final int endPage,
                                                 final boolean generateIDs,
                                                 final boolean segmentSentences,
                                                 final List<String> teiCoordinates) throws Exception {
        LOGGER.debug(methodLogIn());

        String retVal = null;
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md);

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            }

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .withSentenceSegmentation(segmentSentences)
                    .build();

            retVal = engine.fullTextToBlank(originFile, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }

            if (originFile != null)
                IOUtilities.removeTempFile(originFile);
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document which shall be extracted into TEI + assets in a ZIP
     * archive.
     *
     * @param inputStream          the data of origin document
     * @param consolidateHeader    the consolidation option allows GROBID to exploit Crossref
     *                             for improving header information
     * @param consolidateCitations the consolidation option allows GROBID to exploit Crossref
     *                             for improving citations information
     * @param startPage            give the starting page to consider in case of segmentation of the
     *                             PDF, -1 for the first page (default)
     * @param endPage              give the end page to consider in case of segmentation of the
     *                             PDF, -1 for the last page (default)
     * @param generateIDs          if true, generate random attribute id on the textual elements of
     *                             the resulting TEI
     * @param segmentSentences     if true, return results with segmented sentences
     * @return a response object mainly contain the TEI representation of the
     * full text
     */
    public Response processStatelessFulltextAssetDocument(
        final InputStream inputStream,
        final GrobidModels.Flavor flavor,
        final int consolidateHeader,
        final int consolidateCitations,
        final int consolidateFunders,
        final boolean includeRawAffiliations,
        final boolean includeRawCitations,
        final boolean includeRawCopyrights,
        final int startPage,
        final int endPage,
        final boolean generateIDs,
        final boolean segmentSentences,
        final List<String> teiCoordinates
    ) throws Exception {

        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String assetPath = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // set the path for the asset files
            assetPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            GrobidAnalysisConfig config =
                GrobidAnalysisConfig.builder()
                    .consolidateHeader(consolidateHeader)
                    .consolidateCitations(consolidateCitations)
                    .consolidateFunders(consolidateFunders)
                    .includeRawAffiliations(includeRawAffiliations)
                    .includeRawCitations(includeRawCitations)
                    .includeRawCopyrights(includeRawCopyrights)
                    .startPage(startPage)
                    .endPage(endPage)
                    .generateTeiIds(generateIDs)
                    .generateTeiCoordinates(teiCoordinates)
                    .pdfAssetPath(new File(assetPath))
                    .withSentenceSegmentation(segmentSentences)
                    .flavor(flavor)
                    .build();

            retVal = engine.fullTextToTEI(originFile, flavor, md5Str, config);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(outputStream);
                out.putNextEntry(new ZipEntry("tei.xml"));
                out.write(retVal.getBytes(StandardCharsets.UTF_8));
                // put now the assets, i.e. all the files under the asset path
                File assetPathDir = new File(assetPath);
                if (assetPathDir.exists()) {
                    File[] files = assetPathDir.listFiles();
                    if (files != null) {
                        byte[] buffer = new byte[1024];
                        for (final File currFile : files) {
                            if (currFile.getName().toLowerCase().endsWith(".jpg")
                                || currFile.getName().toLowerCase().endsWith(".png")) {
                                try {
                                    ZipEntry ze = new ZipEntry(currFile.getName());
                                    out.putNextEntry(ze);
                                    FileInputStream in = new FileInputStream(currFile);
                                    int len;
                                    while ((len = in.read(buffer)) > 0) {
                                        out.write(buffer, 0, len);
                                    }
                                    in.close();
                                    out.closeEntry();
                                } catch (IOException e) {
                                    throw new GrobidServiceException("IO Exception when zipping", e, Status.INTERNAL_SERVER_ERROR);
                                }
                            }
                        }
                    }
                }
                out.finish();

                response = Response
                    .ok()
                    .type("application/zip")
                    .entity(outputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"result.zip\"")
                    .build();
                out.close();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);
            
            if (assetPath != null) {
                IOUtilities.removeTempDirectory(assetPath);
            }
            
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }


    /**
     * Process a patent document in PDF for extracting and parsing citations in the description body.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response processCitationPatentPDF(final InputStream inputStream,
                                             final int consolidate,
                                             final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            List<PatentItem> patents = new ArrayList<>();
            List<BibDataSet> articles = new ArrayList<>();
            retVal = engine.processAllCitationsInPDFPatent(originFile.getAbsolutePath(),
                                                           articles, patents, consolidate, 
                                                           includeRawCitations);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Process a patent document encoded in ST.36 for extracting and parsing citations in the description body.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response processCitationPatentST36(final InputStream inputStream,
                                              final int consolidate,
                                              final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            List<PatentItem> patents = new ArrayList<>();
            List<BibDataSet> articles = new ArrayList<>();
            retVal = engine.processAllCitationsInXMLPatent(originFile.getAbsolutePath(),
                    articles, patents, consolidate, includeRawCitations);

            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                //response = Response.status(Status.OK).entity(retVal).type(MediaType.APPLICATION_XML).build();
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                    .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);
            
            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin document, extract and parser all its references.
     *
     * @param inputStream          the data of origin document
     * @param consolidate          if the result has to be consolidated with CrossRef access.
     * @param includeRawCitations  determines whether the original citation (called "raw") should be included in the
     *                             output
     * @param expectedResponseType determines whether XML or BibTeX should be returned
     * @return a response object mainly contain the TEI representation of the full text
     */
    public Response processStatelessReferencesDocument(final InputStream inputStream,
                                                       final int consolidate,
                                                       final boolean includeRawCitations,
                                                       ExpectedResponseType expectedResponseType) {
        LOGGER.debug(methodLogIn());
        Response response;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            // starts conversion process
            List<BibDataSet> bibDataSetList = engine.processReferences(originFile, md5Str, consolidate);

            if (bibDataSetList.isEmpty()) {
                response = Response.status(Status.NO_CONTENT).build();
            } else if (expectedResponseType == ExpectedResponseType.BIBTEX) {
                StringBuilder result = new StringBuilder();
                GrobidAnalysisConfig config = new GrobidAnalysisConfig.GrobidAnalysisConfigBuilder().includeRawCitations(includeRawCitations).build();
                for (BibDataSet res : bibDataSetList) {
                    BiblioItem bib = res.getResBib();
                    result.append(bib.toBibTeX(bib.generateBibTeXKey(), config));
                    result.append("\n");
                }
                response = Response.status(Status.OK)
                                   .entity(result.toString())
                                   .header(HttpHeaders.CONTENT_TYPE, BibTexMediaType.MEDIA_TYPE + "; charset=UTF-8")
                                   .build();
            } else {
                StringBuilder result = new StringBuilder();
                // dummy header
                result.append("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" " +
                    "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                    "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
                result.append("\t<teiHeader/>\n\t<text>\n\t\t<front/>\n\t\t" +
                    "<body/>\n\t\t<back>\n\t\t\t<div>\n\t\t\t\t<listBibl>\n");
                int p = 0;
                for (BibDataSet bibDataSet : bibDataSetList) {
                    result.append(bibDataSet.toTEI(p, includeRawCitations));
                    result.append("\n");
                    p++;
                }
                result.append("\t\t\t\t</listBibl>\n\t\t\t</div>\n\t\t</back>\n\t</text>\n</TEI>\n");
                response = Response.status(Status.OK)
                                   .entity(result.toString())
                                   .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML + "; charset=UTF-8")
                                   .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Uploads the origin PDF, process it and return the PDF augmented with annotations.
     *
     * @param inputStream the data of origin PDF
     * @param fileName    the name of origin PDF
     * @param type        gives type of annotation
     * @return a response object containing the annotated PDF
     */
    public Response processPDFAnnotation(final InputStream inputStream,
                                         final String fileName,
                                         final int consolidateHeader,
                                         final int consolidateCitations,
                                         final boolean includeRawAffiliations,
                                         final boolean includeRawCitations,
                                         final GrobidRestUtils.Annotation type) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        PDDocument out = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            out = annotate(
                originFile, type, engine,
                consolidateHeader, consolidateCitations,
                includeRawAffiliations, includeRawCitations
            );
            if (out != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                out.save(outputStream);
                response = Response
                    .ok()
                    .type("application/pdf")
                    .entity(outputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();
            } else {
                response = Response.status(Status.NO_CONTENT).build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            try {
                out.close();
            } catch (IOException e) {
                LOGGER.error("An unexpected exception occurs. ", e);
                response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        LOGGER.debug(methodLogOut());
        return response;
    }


    /**
     * Uploads the origin PDF, process it and return PDF annotations for references in JSON.
     *
     * @param inputStream the data of origin PDF
     * @return a response object containing the JSON annotations
     */
    public Response processPDFReferenceAnnotation(final InputStream inputStream,
                                                  final int consolidateHeader,
                                                  final int consolidateCitations,
                                                  final boolean includeRawCitations,
                                                  final boolean includeFiguresTables) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(inputStream, md); 

            originFile = IOUtilities.writeInputFile(dis);
            byte[] digest = md.digest();
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            String md5Str = DatatypeConverter.printHexBinary(digest).toUpperCase();

            List<String> elementWithCoords = new ArrayList<>();
            elementWithCoords.add("ref");
            elementWithCoords.add("biblStruct");
            GrobidAnalysisConfig config = new GrobidAnalysisConfig
                .GrobidAnalysisConfigBuilder()
                .generateTeiCoordinates(elementWithCoords)
                .consolidateHeader(consolidateHeader)
                .consolidateCitations(consolidateCitations)
                .includeRawCitations(includeRawCitations)
                .build();

            Document teiDoc = engine.fullTextToTEIDoc(originFile, config);
            String json = CitationsVisualizer.getJsonAnnotations(teiDoc, null, includeFiguresTables);

            if (json != null) {
                response = Response
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .entity(json)
                    .build();
            } else {
                response = Response.status(Status.NO_CONTENT).build();
            }
            
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    /**
     * Annotate the citations in a PDF patent document with JSON annotations.
     *
     * @param inputStream the data of origin document
     * @return a response object mainly containing the TEI representation of the
     * citation
     */
    public Response annotateCitationPatentPDF(final InputStream inputStream,
                                              final int consolidate,
                                              final boolean includeRawCitations) throws Exception {
        LOGGER.debug(methodLogIn());
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        try {
            engine = Engine.getEngine(true);
            // conservative check, if no engine is free in the pool a NoSuchElementException is normally thrown
            if (engine == null) {
                throw new GrobidServiceException(
                    "No GROBID engine available", Status.SERVICE_UNAVAILABLE);
            }

            originFile = IOUtilities.writeInputFile(inputStream);
            if (originFile == null) {
                LOGGER.error("The input file cannot be written.");
                throw new GrobidServiceException(
                    "The input file cannot be written.", Status.INTERNAL_SERVER_ERROR);
            } 

            // starts conversion process
            retVal = engine.annotateAllCitationsInPDFPatent(originFile.getAbsolutePath(), consolidate, includeRawCitations);
                    
            if (GrobidRestUtils.isResultNullOrEmpty(retVal)) {
                response = Response.status(Status.NO_CONTENT).build();
            } else {
                response = Response.status(Status.OK)
                    .entity(retVal)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                   .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }
        LOGGER.debug(methodLogOut());
        return response;
    }

    public String methodLogIn() {
        return ">> " + GrobidRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    public String methodLogOut() {
        return "<< " + GrobidRestProcessFiles.class.getName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName();
    }

    protected PDDocument annotate(File originFile, 
                                  final GrobidRestUtils.Annotation type, Engine engine,
                                  final int consolidateHeader,
                                  final int consolidateCitations,
                                  final boolean includeRawAffiliations,
                                  final boolean includeRawCitations) throws Exception {
        // starts conversion process
        PDDocument outputDocument = null;
        // list of TEI elements that should come with coordinates
        List<String> elementWithCoords = new ArrayList<>();
        if (type == GrobidRestUtils.Annotation.CITATION) {
            elementWithCoords.add("ref");
            elementWithCoords.add("biblStruct");
        } else if (type == GrobidRestUtils.Annotation.FIGURE) {
            elementWithCoords.add("figure");
        }

        GrobidAnalysisConfig config = new GrobidAnalysisConfig
            .GrobidAnalysisConfigBuilder()
            .consolidateHeader(consolidateHeader)
            .consolidateCitations(consolidateCitations)
            .includeRawAffiliations(includeRawAffiliations)
            .includeRawCitations(includeRawCitations)
            .generateTeiCoordinates(elementWithCoords)
            .build();

        DocumentSource documentSource = 
            DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage(), true, true, false);

        Document teiDoc = engine.fullTextToTEIDoc(documentSource, null, config);

        documentSource = 
            DocumentSource.fromPdf(originFile, config.getStartPage(), config.getEndPage(), true, true, false);

        PDDocument document = PDDocument.load(originFile);
        //If no pages, skip the document
        if (document.getNumberOfPages() > 0) {
            outputDocument = dispatchProcessing(type, document, documentSource, teiDoc);
        } else {
            throw new RuntimeException("Cannot identify any pages in the input document. " +
                "The document cannot be annotated. Please check whether the document is valid or the logs.");
        }
        
        documentSource.close(true, true, false);

        return outputDocument;
    }

    protected PDDocument dispatchProcessing(GrobidRestUtils.Annotation type, PDDocument document,
                                            DocumentSource documentSource, Document teiDoc
    ) throws Exception {
        PDDocument out = null;
        if (type == GrobidRestUtils.Annotation.CITATION) {
            out = CitationsVisualizer.annotatePdfWithCitations(document, teiDoc, null);
        } else if (type == GrobidRestUtils.Annotation.BLOCK) {
            out = BlockVisualizer.annotateBlocks(document, documentSource.getXmlFile(),
                teiDoc, true, true, false);
        } else if (type == GrobidRestUtils.Annotation.FIGURE) {
            out = FigureTableVisualizer.annotateFigureAndTables(document, documentSource.getXmlFile(),
                teiDoc, true, true, true, false, false);
        } 
        return out;
    }
}
