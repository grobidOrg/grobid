package org.grobid.core.main;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.jni.PythonEnvironmentConfig;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.Utilities;

public class LibraryLoader {

    private static Logger LOGGER = LoggerFactory.getLogger(LibraryLoader.class);

    public static final String CRFPP_NATIVE_LIB_NAME = "libcrfpp";
    public static final String WAPITI_NATIVE_LIB_NAME = "libwapiti";
    public static final String DELFT_NATIVE_LIB_NAME_LINUX = "libjep";
    public static final String DELFT_NATIVE_LIB_NAME = "jep";

    private static boolean loaded = false;

    public static void load() {

        if (!loaded) {
            LOGGER.info("Loading external native sequence labelling library");
            LOGGER.debug(getLibraryFolder());

            Set<GrobidCRFEngine> distinctModels = GrobidProperties.getInstance().getDistinctModels();
            for (GrobidCRFEngine distinctModel : distinctModels) {
                if (distinctModel != GrobidCRFEngine.CRFPP &&
                        distinctModel != GrobidCRFEngine.WAPITI &&
                        distinctModel != GrobidCRFEngine.DELFT) {
                    throw new IllegalStateException("Unsupported sequence labelling engine: " + distinctModel);
                }
            }

            File libraryFolder = new File(getLibraryFolder());
            if (!libraryFolder.exists() || !libraryFolder.isDirectory()) {
                LOGGER.error(
                        "Unable to find a native sequence labelling library: Folder "
                                + libraryFolder
                                + " does not exist");
                throw new RuntimeException(
                        "Unable to find a native sequence labelling library: Folder "
                                + libraryFolder
                                + " does not exist");
            }

            if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.CRFPP))) {
                File[] files = libraryFolder
                        .listFiles(file -> file.getName().toLowerCase().startsWith(CRFPP_NATIVE_LIB_NAME));

                if (ArrayUtils.isEmpty(files)) {
                    LOGGER.error(
                            "Unable to find a native CRF++ library: No files starting with "
                                    + CRFPP_NATIVE_LIB_NAME
                                    + " are in folder "
                                    + libraryFolder);
                    throw new RuntimeException(
                            "Unable to find a native CRF++ library: No files starting with "
                                    + CRFPP_NATIVE_LIB_NAME
                                    + " are in folder "
                                    + libraryFolder);
                }

                if (files.length > 1) {
                    LOGGER.error(
                            "Unable to load a native CRF++ library: More than 1 library exists in " + libraryFolder);
                    throw new RuntimeException(
                            "Unable to load a native CRF++ library: More than 1 library exists in " + libraryFolder);
                }

                String libPath = files[0].getAbsolutePath();
                // finally loading a library

                try {
                    System.load(libPath);
                } catch (Exception e) {
                    LOGGER.error("Unable to load a native CRF++ library, although it was found under path " + libPath);
                    throw new RuntimeException(
                            "Unable to load a native CRF++ library, although it was found under path " + libPath, e);
                }
            }

            if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.WAPITI))) {
                // The Wapiti JNI argument parser splits on whitespace, so any space in
                // the grobid-home path would silently corrupt all model and training paths.
                // Fail fast here with a clear message rather than producing mysterious errors later.
                String grobidHomePath = GrobidProperties.getGrobidHome().getAbsolutePath();
                if (grobidHomePath.contains(" ")) {
                    throw new RuntimeException(
                            "The grobid-home path contains a space character which is not supported "
                                    + "when using the Wapiti sequence labeller. "
                                    + "Please move grobid-home to a path without spaces. "
                                    + "Current path: "
                                    + grobidHomePath);
                }

                File[] wapitiLibFiles = libraryFolder.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(WAPITI_NATIVE_LIB_NAME);
                    }
                });

                if (isEmpty(wapitiLibFiles)) {
                    LOGGER.info("No wapiti library in the Grobid home folder");
                } else {
                    LOGGER.info("Loading Wapiti native library...");
                    if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.DELFT))) {
                        // if DeLFT will be used, we must not load libstdc++, it would create a conflict with tensorflow libstdc++ version
                        // so we temporary rename the lib so that it is not loaded in this case
                        // note that we know that, in this case, the local lib can be ignored because as DeFLT and tensorflow are installed
                        // we are sure that a compatible libstdc++ lib is installed on the system and can be dynamically loaded

                        String libstdcppPath = libraryFolder.getAbsolutePath() + File.separator + "libstdc++.so.6";
                        File libstdcppFile = new File(libstdcppPath);
                        if (libstdcppFile.exists()) {
                            File libstdcppFileNew = new File(libstdcppPath + ".new");
                            libstdcppFile.renameTo(libstdcppFileNew);
                        }

                        String libgccPath = libraryFolder.getAbsolutePath() + File.separator + "libgcc_s.so.1";
                        File libgccFile = new File(libgccPath);
                        if (libgccFile.exists()) {
                            File libgccFileNew = new File(libgccPath + ".new");
                            libgccFile.renameTo(libgccFileNew);
                        }
                    }
                    try {
                        System.load(wapitiLibFiles[0].getAbsolutePath());
                    } finally {
                        if (CollectionUtils.containsAny(distinctModels, Arrays.asList(GrobidCRFEngine.DELFT))) {
                            // restore libstdc++
                            String libstdcppPathNew = libraryFolder.getAbsolutePath()
                                    + File.separator
                                    + "libstdc++.so.6.new";
                            File libstdcppFileNew = new File(libstdcppPathNew);
                            if (libstdcppFileNew.exists()) {
                                File libstdcppFile = new File(
                                        libraryFolder.getAbsolutePath() + File.separator + "libstdc++.so.6");
                                libstdcppFileNew.renameTo(libstdcppFile);
                            }

                            // restore libgcc
                            String libgccPathNew = libraryFolder.getAbsolutePath()
                                    + File.separator
                                    + "libgcc_s.so.1.new";
                            File libgccFileNew = new File(libgccPathNew);
                            if (libgccFileNew.exists()) {
                                File libgccFile = new File(
                                        libraryFolder.getAbsolutePath() + File.separator + "libgcc_s.so.1");
                                libgccFileNew.renameTo(libgccFile);
                            }
                        }
                    }
                }
            }

            if (CollectionUtils.containsAny(distinctModels, Collections.singletonList(GrobidCRFEngine.DELFT))) {
                LOGGER.info("Loading JEP native library for DeLFT... " + libraryFolder.getAbsolutePath());
                // actual loading will be made at JEP initialization, so we just need to add the path in the
                // java.library.path (JEP will anyway try to load from java.library.path, so explicit file
                // loading here will not help)
                try {

                    PythonEnvironmentConfig pythonEnvironmentConfig = PythonEnvironmentConfig.getInstance();
                    if (pythonEnvironmentConfig.isEmpty()) {
                        LOGGER.info("No python environment configured");
                    } else {
                        if (SystemUtils.IS_OS_MAC) {
                            System.loadLibrary("python" + pythonEnvironmentConfig.getPythonVersion());
                            System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                        } else if (SystemUtils.IS_OS_LINUX) {
                            System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                        } else if (SystemUtils.IS_OS_WINDOWS) {
                            throw new UnsupportedOperationException("Delft on Windows is not supported.");
                        }
                    }

                } catch (Exception e) {
                    throw new GrobidException("Loading JEP native library for DeLFT failed", e);
                }
            }

            loaded = true;
            LOGGER.info("Native library for sequence labelling loaded");
        }
    }

    @Deprecated
    public static void addLibraryPath(String pathToAdd) throws Exception {
        Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        String[] paths = (String[]) usrPathsField.get(null);

        for (String path : paths)
            if (path.equals(pathToAdd))
                return;

        String[] newPaths = new String[paths.length + 1];
        System.arraycopy(paths, 0, newPaths, 1, paths.length);
        newPaths[0] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }

    public static String getLibraryFolder() {
        GrobidProperties.getInstance();
        return String.format(
                "%s" + File.separator + "%s",
                GrobidProperties.getNativeLibraryPath().getAbsolutePath(),
                Utilities.getOsNameAndArch());
    }
}
