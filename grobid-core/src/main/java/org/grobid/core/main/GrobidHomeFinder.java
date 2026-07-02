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
package org.grobid.core.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.exceptions.GrobidPropertyException;

/**
 * This class is responsible for finding a right grobid home
 */
public class GrobidHomeFinder {
    private static final String PROP_GROBID_HOME = "org.grobid.home";
    private static final String PROP_GROBID_CONFIG = "org.grobid.config";

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidHomeFinder.class);
    private static final List<String> GROBID_FOLDER_POSSIBLE_LOCATIONS = Lists
            .newArrayList("../grobid-home", "grobid-home", "GROBID_HOME");
    private static final int BUFFER_SIZE = 4096;
    private final List<String> grobidHomePossibleLocations;

    public GrobidHomeFinder() {
        this(GROBID_FOLDER_POSSIBLE_LOCATIONS);
    }

    public GrobidHomeFinder(List<String> grobidHomePossibleLocations) {
        if (grobidHomePossibleLocations == null) {
            this.grobidHomePossibleLocations = Collections.emptyList();
        } else {
            this.grobidHomePossibleLocations = grobidHomePossibleLocations;
        }
    }

    public File findGrobidHomeOrFail() {
        File gh = getGrobidHomePathOrLoadFromClasspath();
        LOGGER.info("*** USING GROBID HOME: " + gh.getAbsolutePath());
        if (!gh.exists() || !gh.isDirectory()) {
            fail("Grobid home folder '" + gh.getAbsolutePath() + "' was detected for usage, but does not exist");
        }

        return gh;
    }

    public File findGrobidConfigOrFail(File grobidHome) {
        if (grobidHome == null || !grobidHome.exists() || !grobidHome.isDirectory()) {
            fail("Grobid home folder '" + grobidHome + "' was detected for usage, but does not exist or null");
        }

        String grobidConfig = System.getProperty(PROP_GROBID_CONFIG);
        File grobidConfigFile;
        if (grobidConfig == null) {
            grobidConfigFile = new File(grobidHome, "config/grobid.yaml").getAbsoluteFile();
            LOGGER.info(
                    "Grobid config file location was not explicitly set via '"
                            + PROP_GROBID_CONFIG
                            +
                            "' system variable, defaulting to: "
                            + grobidConfigFile);
        } else {
            grobidConfigFile = new File(grobidConfig).getAbsoluteFile();
        }

        if (!grobidConfigFile.exists() || grobidConfigFile.isDirectory()) {
            fail("Grobid property file '" + grobidConfigFile + "' does not exist or a directory");
        }
        return grobidConfigFile;
    }

    private static void fail(String msg, Throwable e) {
        throw new GrobidPropertyException(msg, e);
    }

    private static void fail(String msg) {
        throw new GrobidPropertyException(msg);
    }

    private File getGrobidHomePathOrLoadFromClasspath() {
        String grobidHomeProperty = System.getProperty(PROP_GROBID_HOME);
        if (grobidHomeProperty != null) {
            try {
                URL url = new URL(grobidHomeProperty);
                if (url.getProtocol().equals("file")) {
                    return new File(grobidHomeProperty);
                } else if (url.getProtocol().equals("http") || url.getProtocol().equals("https")) {
                    // to do, download and cache
                    try {
                        return unzipToTempFile(url, false);
                    } catch (IOException e) {
                        fail("Cannot fetch Grobid home from: " + url, e);
                    }
                }
            } catch (MalformedURLException e) {
                // just normal path, return it
                return new File(grobidHomeProperty);
            }
        } else {
            LOGGER.info("No Grobid property was provided. Attempting to find Grobid home in the current directory...");
            for (String possibleName : grobidHomePossibleLocations) {
                File gh = new File(possibleName);
                if (gh.exists()) {
                    return gh.getAbsoluteFile();
                }
            }

            LOGGER.info("Attempting to find and in the classpath...");

            // TODO: inject a descriptive file into Grobid home
            URL url = GrobidHomeFinder.class.getResource("/grobid-home/lexicon/names/firstname.5k");
            if (url == null) {
                fail("No Grobid home was found in classpath and no Grobid home location was not provided");
            }

            if (url.getProtocol().equals("jar")) {
                final JarURLConnection connection;
                try {
                    connection = (JarURLConnection) url.openConnection();
                    final URL zipUrl = connection.getJarFileURL();
                    return unzipToTempFile(zipUrl, false);
                } catch (IOException e) {
                    fail("Cannot load a Grobid home from classpath", e);
                }
            } else {
                fail("Unsupported protocol for Grobid home at location: " + url);
            }
        }
        fail(
                "Cannot locate Grobid home: add it to classpath or explicitly provide a system property: '-D"
                        + PROP_GROBID_HOME
                        + "'");
        // not reachable code since exception is thrown
        return null;
    }

    private static File unzipToTempFile(URL zipUrl, boolean forceReload) throws IOException {
        String hash = Hashing.md5().hashString(zipUrl.toString(), Charset.defaultCharset()).toString();
        File tempRootDir = new File(System.getProperty("java.io.tmpdir"));

        File grobidHome = new File(tempRootDir, "grobid-home-" + System.getProperty("user.name") + "-" + hash);
        LOGGER.info("Extracting and caching Grobid home to " + grobidHome);

        if (grobidHome.exists()) {
            if (forceReload) {
                FileUtils.deleteDirectory(grobidHome);
            } else {
                LOGGER.warn("Grobid home already cached under: " + grobidHome + "; delete it if you want a new copy");
                return new File(grobidHome, "grobid-home");
            }
        }

        if (!grobidHome.mkdir()) {
            fail("Cannot create folder for Grobid home: " + grobidHome);
        }
        unzip(zipUrl.openStream(), grobidHome);

        return new File(grobidHome, "grobid-home");
    }

    private static List<Path> unzip(InputStream is, File destinationDir) throws IOException {
        List<Path> list = new ArrayList<>();

        ZipInputStream zipIn = new ZipInputStream(is);
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
            File filePath = new File(destinationDir, entry.getName()).toPath().normalize().toFile();
            if (!filePath.toPath().startsWith(destinationDir.toPath())) {
                throw new IOException("Bad zip entry: " + entry.getName());
            }
            try {
                if (!entry.isDirectory()) {
                    String absolutePath = filePath.getAbsolutePath();
                    extractFile(zipIn, absolutePath);
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    filePath.mkdir();
                }
            } finally {
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
        zipIn.close();

        return list;
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Cannot create parent folders: " + file.getParentFile());
            }
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    public static void main(String[] args) throws IOException {
        long t = System.currentTimeMillis();
        File grobidHomePathOrLoadFromClasspath = new GrobidHomeFinder().findGrobidHomeOrFail();
        System.out.println(grobidHomePathOrLoadFromClasspath);

        System.out.println("Took: " + (System.currentTimeMillis() - t));

    }
}
