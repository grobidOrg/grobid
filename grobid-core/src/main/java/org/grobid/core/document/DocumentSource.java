package org.grobid.core.document;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidExceptionStatus;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.process.ProcessRunner;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.utilities.Utilities;

/**
 * Input document to be processed, which could come from a PDF or directly be an XML file.
 * If from a PDF document, this is the place where pdfalto is called.
 */
public class DocumentSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSource.class);
    //    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int KILLED_DUE_2_TIMEOUT = 143;
    private static final int MISSING_LIBXML2 = 127;
    private static final int MISSING_PDFALTO = 126;
    public static final int PDFALTO_FILES_AMOUNT_LIMIT = 5000;

    private File pdfFile;
    private File xmlFile;
    boolean cleanupXml = false;

    private String md5Str = null;

    private DocumentSource() {
    }

    public static DocumentSource fromPdf(File pdfFile) {
        return fromPdf(pdfFile, -1, -1);
    }

    /**
     * By default the XML extracted from the PDF is without images, to avoid flooding the grobid-home/tmp directory,
     * but with the extra annotation file and with outline
     */
    public static DocumentSource fromPdf(File pdfFile, int startPage, int endPage) {
        return fromPdf(pdfFile, startPage, endPage, false, true, false);
    }

    public static DocumentSource fromPdf(
            File pdfFile,
            int startPage,
            int endPage,
            boolean withImages,
            boolean withAnnotations,
            boolean withOutline) {
        if (!pdfFile.exists() || pdfFile.isDirectory()) {
            throw new GrobidException("Input PDF file " + pdfFile + " does not exist or a directory",
                    GrobidExceptionStatus.BAD_INPUT_DATA);
        }

        DocumentSource source = new DocumentSource();
        source.cleanupXml = true;

        try {
            source.xmlFile = source.pdfalto(
                    null,
                    false,
                    startPage,
                    endPage,
                    pdfFile,
                    GrobidProperties.getTempPath(),
                    withImages,
                    withAnnotations,
                    withOutline);
        } catch (Exception e) {
            source.close(withImages, withAnnotations, withOutline);
            throw e;
        } finally {
        }
        source.pdfFile = pdfFile;
        return source;
    }

    private String getPdfaltoCommand(boolean withImage, boolean withAnnotations, boolean withOutline) {
        StringBuilder pdfToXml = new StringBuilder();
        pdfToXml.append(GrobidProperties.getPdfaltoPath().getAbsolutePath());
        // bat files sets the path env variable for cygwin dll
        if (SystemUtils.IS_OS_WINDOWS) {
            //pdfalto executable are separated to avoid dll conflicts
            pdfToXml.append(File.separator + "pdfalto");
        }
        pdfToXml.append(
                GrobidProperties.isContextExecutionServer() ? File.separator + "pdfalto_server"
                        : File.separator + "pdfalto");

        pdfToXml.append(" -fullFontName -noLineNumbers");

        if (!withImage) {
            pdfToXml.append(" -onlyGraphsCoord ");
        }
        if (withAnnotations) {
            pdfToXml.append(" -annotation ");
        }
        if (withOutline) {
            pdfToXml.append(" -outline ");
        }

        //        pdfToXml.append(" -readingOrder ");
        //        pdfToXml.append(" -ocr ");

        pdfToXml.append(" -filesLimit 2000 ");

        //System.out.println(pdfToXml);
        //pdfToXml.append(" -conf <path to config> ");
        return pdfToXml.toString();
    }

    /**
     * Create an XML representation from a pdf file. If tout is true (default),
     * a timeout is used. If force is true, the xml file is always regenerated,
     * even if already present (default is false, it can save up to 50% overall
     * runtime). If full is true, the extraction covers also images within the
     * pdf, which is relevant for fulltext extraction.
     */
    public File pdfalto(
            Integer timeout,
            boolean force,
            int startPage,
            int endPage,
            File pdfPath,
            File tmpPath,
            boolean withImages,
            boolean withAnnotations,
            boolean withOutline) {
        LOGGER.debug("start pdf to xml sub process");
        long time = System.currentTimeMillis();
        String pdftoxml0;

        pdftoxml0 = getPdfaltoCommand(withImages, withAnnotations, withOutline);

        if (startPage > 0)
            pdftoxml0 += " -f " + startPage + " ";
        if (endPage > 0)
            pdftoxml0 += " -l " + endPage + " ";

        // if the XML representation already exists, no need to redo the
        // conversion,
        // except if the force parameter is set to true
        File tmpPathXML = new File(tmpPath, KeyGen.getKey() + ".lxml");
        xmlFile = tmpPathXML;
        File f = tmpPathXML;

        if ((!f.exists()) || force) {
            List<String> cmd = new ArrayList<>();
            String[] tokens = pdftoxml0.split(" ");
            for (String token : tokens) {
                if (token.trim().length() > 0) {
                    cmd.add(token);
                }
            }
            cmd.add(pdfPath.getAbsolutePath());
            cmd.add(tmpPathXML.getAbsolutePath());
            if (GrobidProperties.isContextExecutionServer()) {
                cmd.add("--timeout");
                cmd.add(String.valueOf(GrobidProperties.getPdfaltoTimeoutS()));
                cmd.add("--ulimit");
                cmd.add(String.valueOf(GrobidProperties.getPdfaltoMemoryLimitMb() * 1024));
                tmpPathXML = processPdfaltoServerMode(pdfPath, tmpPathXML, cmd);
            } else {
                if (!SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_MAC) {
                    cmd = Arrays.asList(
                            "bash",
                            "-c",
                            "ulimit -Sv "
                                    +
                                    GrobidProperties.getPdfaltoMemoryLimitMb() * 1024
                                    + " && "
                                    + pdftoxml0
                                    + " '"
                                    + pdfPath
                                    + "' "
                                    + tmpPathXML);
                }
                LOGGER.debug("Executing command: " + cmd);

                tmpPathXML = processPdfaltoThreadMode(timeout, pdfPath, tmpPathXML, cmd);
            }

            File dataFolder = new File(tmpPathXML.getAbsolutePath() + "_data");
            File[] files = dataFolder.listFiles();
            if (files != null && files.length > PDFALTO_FILES_AMOUNT_LIMIT) {
                //throw new GrobidException("The temp folder " + dataFolder + " contains " + files.length + " files and exceeds the limit",
                //    GrobidExceptionStatus.PARSING_ERROR);
                LOGGER.warn(
                        "The temp folder "
                                + dataFolder
                                + " contains "
                                + files.length
                                +
                                " files and exceeds the limit, only the first "
                                + PDFALTO_FILES_AMOUNT_LIMIT
                                + " asset files will be kept.");
            }
        }
        LOGGER.debug(
                "pdf to xml sub process process finished. Time to process:"
                        + (System.currentTimeMillis() - time)
                        + "ms");
        return tmpPathXML;
    }

    /**
     * Process the conversion of pdfalto format using thread calling native
     * executable.
     * <p>
     * Executed NOT in the server mode
     *
     * @param timeout    in ms.   null, if default
     * @param pdfPath    path to pdf
     * @param tmpPathXML temporary path to save the converted file
     * @param cmd        arguments to call the executable pdfalto
     * @return the path the the converted file.
     */
    private File processPdfaltoThreadMode(
            Integer timeout,
            File pdfPath,
            File tmpPathXML,
            List<String> cmd) {
        LOGGER.debug("Executing: " + cmd.toString());
        ProcessRunner worker = new ProcessRunner(cmd, "pdfalto[" + pdfPath + "]", true);

        worker.start();

        try {
            if (timeout != null) {
                worker.join(timeout);
            } else {
                worker.join(GrobidProperties.getPdfaltoTimeoutMs()); // max 50 second even without predefined
                // timeout
            }
            if (worker.getExitStatus() == null) {
                tmpPathXML = null;
                //killing all child processes harshly
                worker.killProcess();
                close(true, true, true);
                throw new GrobidException("PDF to XML conversion timed out", GrobidExceptionStatus.TIMEOUT);
            }

            if (worker.getExitStatus() != 0) {
                String errorStreamContents = worker.getErrorStreamContents();
                close(true, true, true);
                throw new GrobidException("PDF to XML conversion failed on pdf file "
                        + pdfPath
                        + " "
                        +
                        (StringUtils.isEmpty(errorStreamContents) ? "" : ("due to: " + errorStreamContents)),
                        GrobidExceptionStatus.PDFALTO_CONVERSION_FAILURE);
            }
        } catch (InterruptedException ex) {
            tmpPathXML = null;
            worker.interrupt();
            Thread.currentThread().interrupt();
        } finally {
            worker.interrupt();
        }
        return tmpPathXML;
    }

    /**
     * Process the conversion of pdf to xml format calling native executable. No
     * thread used for the execution.
     *
     * @param pdfPath    path to pdf
     * @param tmpPathXML temporary path to save the converted file
     * @param cmd        arguments to call the executable pdfalto
     * @return the path the the converted file.
     */
    private File processPdfaltoServerMode(File pdfPath, File tmpPathXML, List<String> cmd) {
        LOGGER.debug("Executing: " + cmd.toString());
        Integer exitCode = org.grobid.core.process.ProcessPdfToXml.process(cmd);

        if (exitCode == null) {
            throw new GrobidException("An error occurred while converting pdf " + pdfPath,
                    GrobidExceptionStatus.BAD_INPUT_DATA);
        } else if (exitCode == KILLED_DUE_2_TIMEOUT) {
            throw new GrobidException("PDF to XML conversion timed out", GrobidExceptionStatus.TIMEOUT);
        } else if (exitCode == MISSING_PDFALTO) {
            throw new GrobidException("PDF to XML conversion failed. Cannot find pdfalto executable",
                    GrobidExceptionStatus.PDFALTO_CONVERSION_FAILURE);
        } else if (exitCode == MISSING_LIBXML2) {
            throw new GrobidException(
                    "PDF to XML conversion failed. pdfalto cannot be executed correctly. Has libxml2 been installed in the system? More information can be found in the logs. ",
                    GrobidExceptionStatus.PDFALTO_CONVERSION_FAILURE);
        } else if (exitCode != 0) {
            throw new GrobidException("PDF to XML conversion failed with error code: " + exitCode,
                    GrobidExceptionStatus.BAD_INPUT_DATA);
        }

        return tmpPathXML;
    }

    private boolean cleanXmlFile(File pathToXml, boolean cleanImages, boolean cleanAnnotations, boolean cleanOutline) {
        boolean success = false;

        try {
            if (pathToXml != null) {
                if (pathToXml.exists()) {
                    success = pathToXml.delete();
                    if (!success) {
                        throw new GrobidResourceException("Deletion of a temporary XML file failed for file '"
                                + pathToXml.getAbsolutePath()
                                + "'");
                    }

                    File fff = new File(pathToXml + "_metadata.xml");
                    if (fff.exists()) {
                        success = Utilities.deleteDir(fff);

                        if (!success) {
                            throw new GrobidResourceException(
                                    "Deletion of temporary metadata file failed for file '"
                                            + fff.getAbsolutePath()
                                            + "'");
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof GrobidResourceException) {
                throw (GrobidResourceException) e;
            } else {
                throw new GrobidResourceException(
                        "An exception occurred while deleting an XML file '" + pathToXml + "'.", e);
            }
        }

        // if cleanImages is true, we also remove the corresponding image
        // resources subdirectory
        if (cleanImages) {
            try {
                if (pathToXml != null) {
                    File fff = new File(pathToXml + "_data");
                    if (fff.exists()) {
                        if (fff.isDirectory()) {
                            success = Utilities.deleteDir(fff);

                            if (!success) {
                                throw new GrobidResourceException(
                                        "Deletion of temporary image files failed for file '"
                                                + fff.getAbsolutePath()
                                                + "'");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof GrobidResourceException) {
                    throw (GrobidResourceException) e;
                } else {
                    throw new GrobidResourceException(
                            "An exception occurred while deleting an XML file '" + pathToXml + "'.", e);
                }
            }
        }

        // if cleanAnnotations is true, we also remove the additional annotation file
        if (cleanAnnotations) {
            try {
                if (pathToXml != null) {
                    File fff = new File(pathToXml + "_annot.xml");
                    if (fff.exists()) {
                        success = fff.delete();

                        if (!success) {
                            throw new GrobidResourceException(
                                    "Deletion of temporary annotation file failed for file '"
                                            + fff.getAbsolutePath()
                                            + "'");
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof GrobidResourceException) {
                    throw (GrobidResourceException) e;
                } else {
                    throw new GrobidResourceException(
                            "An exception occurred while deleting an XML file '" + pathToXml + "'.", e);
                }
            }
        }

        // if cleanOutline is true, we also remove the additional outline file
        if (cleanOutline) {
            try {
                if (pathToXml != null) {
                    File fff = new File(pathToXml + "_outline.xml");
                    if (fff.exists()) {
                        success = fff.delete();

                        if (!success) {
                            throw new GrobidResourceException(
                                    "Deletion of temporary outline file failed for file '"
                                            + fff.getAbsolutePath()
                                            + "'");
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof GrobidResourceException) {
                    throw (GrobidResourceException) e;
                } else {
                    throw new GrobidResourceException(
                            "An exception occurred while deleting an XML file '" + pathToXml + "'.", e);
                }
            }
        }

        return success;
    }

    public void close(boolean cleanImages, boolean cleanAnnotations, boolean cleanOutline) {
        try {
            if (cleanupXml) {
                cleanXmlFile(xmlFile, cleanImages, cleanAnnotations, cleanOutline);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot cleanup resources (just printing exception):", e);
        }
    }

    public static void close(
            DocumentSource source,
            boolean cleanImages,
            boolean cleanAnnotations,
            boolean cleanOutline) {
        if (source != null) {
            source.close(cleanImages, cleanAnnotations, cleanOutline);
        }
    }

    public File getPdfFile() {
        return pdfFile;
    }

    public void setPdfFile(File pdfFile) {
        this.pdfFile = pdfFile;
    }

    public File getXmlFile() {
        return xmlFile;
    }

    public void setXmlFile(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    public double getByteSize() {
        if (pdfFile != null)
            return pdfFile.length();
        return 0;
    }

    public String getMD5() {
        return this.md5Str;
    }

    public void setMD5(String md5Str) {
        this.md5Str = md5Str;
    }

}
