package org.grobid.core.utilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.exceptions.GrobidResourceException;

/**
 * Utilities related to file and directory management.
 */
public class IOUtilities {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtilities.class);

    /**
     * Creates a file and writes some string content in it.
     *
     * @param file    The file to write in.
     * @param content the content to write
     * @throws IOException
     */
    public static void writeInFile(String file, String content)
            throws IOException {
        FileWriter filew = new FileWriter(new File(file));
        BufferedWriter buffw = new BufferedWriter(filew);
        buffw.write(content);
        buffw.close();
    }

    /**
     * Creates a file and writes a list of string in it separated by a given separator.
     *
     * @param file    The file to write in.
     * @param content the list of string to write
     * @param sep separator to used for the list elements
     * @throws IOException
     */
    public static void writeListInFile(String file, List<String> content, String sep)
            throws IOException {
        FileWriter filew = new FileWriter(new File(file));
        BufferedWriter buffw = new BufferedWriter(filew);
        boolean start = true;
        for (String cont : content) {
            if (start) {
                buffw.write(cont);
                start = false;
            } else
                buffw.write(sep + cont);
        }
        buffw.close();
    }

    /**
     * Read a file and return the content.
     *
     * @param pPathToFile path to file to read.
     * @return String contained in the document.
     * @throws IOException
     */
    public static String readFile(String pPathToFile) throws IOException {
        StringBuffer out = new StringBuffer();
        FileInputStream inputStrem = new FileInputStream(new File(pPathToFile));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int len;
        while ((len = inputStrem.read(buf)) > 0) {
            outStream.write(buf, 0, len);
            out.append(outStream.toString());
        }
        IOUtils.closeQuietly(inputStrem);
        IOUtils.closeQuietly(outStream);

        return out.toString();
    }

    /**
     * Write an input stream in temp directory.
     */
    public static File writeInputFile(InputStream inputStream) {
        LOGGER.debug(">> set origin document for stateless service'...");

        File originFile = null;
        OutputStream out = null;
        try {
            originFile = newTempFile("origin", ".pdf");

            out = new FileOutputStream(originFile);

            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            LOGGER.error(
                    "An internal error occurs, while writing to disk (file to write '"
                            + originFile
                            + "').",
                    e);
            originFile = null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LOGGER.error(
                        "An internal error occurs, while writing to disk (file to write '"
                                + originFile
                                + "').",
                        e);
                originFile = null;
            }
        }
        return originFile;
    }

    /**
     * Creates a new not used temporary file and returns it.
     */
    public static File newTempFile(String fileName, String extension) {
        try {
            return File.createTempFile(fileName, extension, GrobidProperties.getTempPath());
        } catch (IOException e) {
            throw new GrobidResourceException(
                    "Could not create temporary file, '"
                            + fileName
                            + "."
                            +
                            extension
                            + "' under path '"
                            + GrobidProperties.getTempPath()
                            + "'.",
                    e);
        }
    }

    /**
     * From JDK 1.7, creates a new system temporary file and returns the file
     */
    public static File newSystemTempFile(String extension) {
        try {
            Path newFile = Files.createTempFile("grobid", extension);
            return newFile.toFile();
        } catch (IOException e) {
            throw new GrobidResourceException(
                    "Could not create temporary file, with extension '" + extension + "' under path tmp system path.",
                    e);
        }
    }

    /**
     * Delete a temporary file
     */
    public static void removeTempFile(final File file) {

        try {
            // sanity cleaning
            deleteOldies(GrobidProperties.getTempPath(), 300);
            LOGGER.debug("Removing " + file.getAbsolutePath());
            file.delete();
        } catch (Exception exp) {
            LOGGER.error("Error while deleting the temporary file: ", exp);
        }
    }

    /**
     * Delete a system temporary file
     */
    public static void removeSystemTempFile(final File file) {

        try {
            // sanity cleaning
            deleteSystemOldies(300);
            LOGGER.debug("Removing " + file.getAbsolutePath());
            file.delete();
        } catch (Exception exp) {
            LOGGER.error("Error while deleting the temporary file: ", exp);
        }
    }

    /**
     * Delete temporary directory
     */
    public static void removeTempDirectory(final String path) {

        try {
            LOGGER.debug("Removing " + path);
            File theDirectory = new File(path);
            if (theDirectory.exists()) {
                theDirectory.delete();
            }
        } catch (Exception exp) {
            LOGGER.error("Error while deleting the temporary directory: ", exp);
        }
    }

    /**
     * Deletes all files and subdirectories under dir if they are older than a given
     * amount of seconds. Returns true if all deletions were successful. If a deletion
     * fails, the method stops attempting to delete and returns false.
     */
    public static boolean deleteOldies(File dir, int maxLifeInSeconds) {
        return deleteOldies(dir, maxLifeInSeconds, "", true);
    }

    public static boolean deleteOldies(File dir, int maxLifeInSeconds, String prefix, boolean root) {
        Date currentDate = new Date();
        long currentDateMillisec = currentDate.getTime();
        boolean empty = true;
        boolean success = true;
        long threasholdMillisec = currentDateMillisec - (maxLifeInSeconds * 1000);
        if (dir.isDirectory() && (StringUtils.isEmpty(prefix) || dir.getName().startsWith(prefix))) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if ((StringUtils.isEmpty(prefix) || children[i].getName().startsWith(prefix))) {
                        long millisec = children[i].lastModified();
                        if (millisec < threasholdMillisec) {
                            success = deleteOldies(children[i], maxLifeInSeconds, prefix, false);
                            if (!success)
                                return false;
                        } else
                            empty = false;
                    }
                }
            }
        }
        // if the dir is a file or if the directory is empty and it is no the root dir, we delete it
        if (!root && (empty || (!dir.isDirectory()))) {
            if (StringUtils.isEmpty(prefix) || dir.getName().startsWith(prefix))
                success = dir.delete();
        }
        return success;
    }

    /**
     * Deletes all files and subdirectories under the system temporary folder if they are older than
     * a given amount of seconds. Returns true if all deletions were successful. If a deletion
     * fails, the method stops attempting to delete and returns false.
     * The grobid system temporary files and folders are all identified with a grobid prefix.
     */
    public static boolean deleteSystemOldies(int maxLifeInSeconds) {
        String defaultBaseDir = System.getProperty("java.io.tmpdir");
        return deleteOldies(new File(defaultBaseDir), maxLifeInSeconds, "grobid", true);
    }

}
