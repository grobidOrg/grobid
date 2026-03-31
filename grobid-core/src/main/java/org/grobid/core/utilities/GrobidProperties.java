package org.grobid.core.utilities;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.engines.tagging.GrobidCRFEngine;
import org.grobid.core.exceptions.GrobidPropertyException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.utilities.GrobidConfig.ModelParameters;
import org.grobid.core.utilities.GrobidConfig.DelftModelParameters;
import org.grobid.core.utilities.GrobidConfig.DelftModelParameterSet;
import org.grobid.core.utilities.GrobidConfig.WapitiModelParameters;
import org.grobid.core.main.GrobidHomeFinder;
import org.grobid.core.utilities.Consolidation.GrobidConsolidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.DeserializationFeature;

/**
 * This class provide methods to set/load/access grobid config value from a yaml config file loaded
 * in the class {@link GrobidConfig}.
 *
 * New yaml parameters and former properties should be equivalent via this class. We keep the
 * class name "GrobidProperties" for compatibility with Grobid modules and other Java applications
 * using Grobid as a library.
 *
 * to be done: having parameters that can be overridden by a system property having a compatible name.
 */
public class GrobidProperties {
    public static final Logger LOGGER = LoggerFactory.getLogger(GrobidProperties.class);

    static final String FOLDER_NAME_MODELS = "models";
    static final String FILE_NAME_MODEL = "model";
    private static final String GROBID_VERSION_FILE = "/grobid-version.txt";
    private static final String UNKNOWN_VERSION_STR = "unknown";
    private static final String GROBID_REVISION_FILE = "/grobid-revision.txt";

    // Version
    private static String VERSION = null;
    private static String REVISION = null;

    private static GrobidProperties grobidProperties = null;

    // indicate if GROBID is running in server mode or not
    private static boolean contextExecutionServer = false;

    /**
     * {@link GrobidConfig} object containing all config parameters used by grobid.
     */
    private static GrobidConfig grobidConfig = null;

    /**
     * Map models specified inthe config file to their parameters
     */
    private static Map<String, ModelParameters> modelMap = null;

    /**
     * Path to pdf to xml converter.
     */
    private static File pathToPdfalto = null;

    private static File grobidHome = null;

    /**
     * Path to the yaml config file
     */
    static File GROBID_CONFIG_PATH = null;

    private static String GROBID_VERSION = null;

    /**
     * Returns an instance of {@link GrobidProperties} object. If no one is set, then
     * it creates one
     */
    public static GrobidProperties getInstance() {
        if (grobidProperties == null) {
            return getNewInstance();
        } else {
            return grobidProperties;
        }
    }

    /**
     * Returns an instance of {@link GrobidProperties} object based on a custom grobid-home directory.
     * If no one is set, then it creates one.
     */
    public static GrobidProperties getInstance(GrobidHomeFinder grobidHomeFinder) {
        synchronized (GrobidProperties.class) {
            if (grobidHome == null) {
                grobidHome = grobidHomeFinder.findGrobidHomeOrFail();
            }
        }
        return getInstance();
    }

    /**
     * Reload grobid config
     */
    public static void reload() {
        getNewInstance();
    }

    public static void reset() {
        getNewInstance();
    }

    /**
     * Creates a new {@link GrobidProperties} object, initializes and returns it.
     *
     * @return GrobidProperties
     */
    protected static synchronized GrobidProperties getNewInstance() {
        LOGGER.debug("synchronized getNewInstance");
        grobidProperties = new GrobidProperties();
        return grobidProperties;
    }

    /**
     * Load the path to GROBID_HOME from the env-entry set in web.xml.
     */
    private static void assignGrobidHomePath() {
        if (grobidHome == null) {
            synchronized (GrobidProperties.class) {
                if (grobidHome == null) {
                    grobidHome = new GrobidHomeFinder().findGrobidHomeOrFail();
                }
            }
        }
    }

    /**
     * Return the grobid-home path.
     *
     * @return grobid home path
     */
    public static File getGrobidHome() {
        return grobidHome;
    }

    public static File getGrobidHomePath() {
        return grobidHome;
    }

    /**
     * For back compatibility
     */
    @Deprecated
    public static File get_GROBID_HOME_PATH() {
        return grobidHome;
    }

    /**
     * Set the grobid-home path.
     */
    public static void setGrobidHome(final String pGROBID_HOME_PATH) {
        if (StringUtils.isBlank(pGROBID_HOME_PATH))
            throw new GrobidPropertyException("Cannot set property grobidHome to null or empty.");

        grobidHome = new File(pGROBID_HOME_PATH);
        // exception if prop file does not exist
        if (!grobidHome.exists()) {
            throw new GrobidPropertyException("Could not read GROBID_HOME, the directory '" + pGROBID_HOME_PATH + "' does not exist.");
        }

        try {
            grobidHome = grobidHome.getCanonicalFile();
        } catch (IOException e) {
            throw new GrobidPropertyException("Cannot set grobid home path to the given one '" + pGROBID_HOME_PATH
                + "', because it does not exist.");
        }
    }

    /**
     * Load the path to grobid config yaml from the env-entry set in web.xml.
     */
    static void loadGrobidConfigPath() {
        LOGGER.debug("loading grobid config yaml");
        if (GROBID_CONFIG_PATH == null) {
            synchronized (GrobidProperties.class) {
                if (GROBID_CONFIG_PATH == null) {
                    GROBID_CONFIG_PATH = new GrobidHomeFinder().findGrobidConfigOrFail(grobidHome);
                }
            }
        }
    }

    /**
     * Return the path to the GROBID yaml config file
     *
     * @return grobid properties path
     */
    public static File getGrobidConfigPath() {
        return GROBID_CONFIG_PATH;
    }

    /**
     * Set the GROBID config yaml file path.
     */
    public static void setGrobidConfigPath(final String pGrobidConfigPath) {
        if (StringUtils.isBlank(pGrobidConfigPath))
            throw new GrobidPropertyException("Cannot set GROBID config file to null or empty.");

        File grobidConfigPath = new File(pGrobidConfigPath);
        // exception if config file does not exist
        if (!grobidConfigPath.exists()) {
            throw new GrobidPropertyException("Cannot read GROBID yaml config file, the file '" + pGrobidConfigPath + "' does not exist.");
        }

        try {
            GROBID_CONFIG_PATH = grobidConfigPath.getCanonicalFile();
        } catch (IOException e) {
            throw new GrobidPropertyException("Cannot set grobid yaml config file path to the given one '" + pGrobidConfigPath
                + "', because it does not exist.");
        }
    }

    /**
     * Create a new object and search where to find the grobid-home folder.
     *
     * We check if the system property GrobidPropertyKeys.PROP_GROBID_HOME
     * is set. If not set, the method will search for a folder named
     * grobid-home in the current project.
     *
     * Finally from the found grobid-home, the yaml config file is loaded and
     * the native and data resource paths are initialized.
     */
    public GrobidProperties() {
        assignGrobidHomePath();
        loadGrobidConfigPath();
        setContextExecutionServer(false);

        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            grobidConfig = mapper.readValue(GROBID_CONFIG_PATH, GrobidConfig.class);
        } catch (IOException exp) {
            throw new GrobidPropertyException("Cannot open GROBID config yaml file at location '" + GROBID_CONFIG_PATH.getAbsolutePath()
                + "'", exp);
        } catch (Exception exp) {
            throw new GrobidPropertyException("Cannot open GROBID config yaml file " + getGrobidConfigPath().getAbsolutePath(), exp);
        }

        //Map<String, String> configParametersViaEnvironment = getEnvironmentVariableOverrides(System.getenv());
        //this.setEnvironmentConfigParameter(configParametersViaEnvironment);

        initializeTmpPath();
        // TBD: tmp to be created
        loadPdfaltoPath();
        createModelMap();
    }

    /**
     * Create a map between model names and associated parameters
     */
    private static void createModelMap() {
        for(ModelParameters modelParameter : grobidConfig.grobid.models) {
            if (modelMap == null)
                modelMap = new TreeMap<>();
            modelMap.put(modelParameter.name, modelParameter);
        }
    }

    /**
     * Add a model with its parameter object in the model map
     */
    public static void addModel(ModelParameters modelParameter) {
        if (modelMap == null)
            modelMap = new TreeMap<>();
        modelMap.put(modelParameter.name, modelParameter);
    }

    /**
     * Create indicated tmp path if it does not exist
     */
    private void initializeTmpPath() {
        File tmpDir = getTempPath();
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                LOGGER.warn("tmp does not exist and unable to create tmp directory: " + tmpDir.getAbsolutePath());
            }
        }
    }

    /**
     * Return the distinct values of all the engines that are specified in the the model map
     */
    public static Set<GrobidCRFEngine> getDistinctModels() {
        Set<GrobidCRFEngine> distinctModels = new HashSet<>();
        for (Map.Entry<String, ModelParameters> entry : modelMap.entrySet()) {
            ModelParameters modelParameter = entry.getValue();

            if (modelParameter.engine == null) {
                // it should not happen normally
                continue;
            }
            GrobidCRFEngine localEngine = GrobidCRFEngine.get(modelParameter.engine);
            if (!distinctModels.contains(localEngine))
                distinctModels.add(localEngine);
        }
        return distinctModels;
    }

    /**
     * Returns the current version of GROBID
     *
     * @return GROBID version
     */
    public static String getVersion() {
        if (VERSION != null) {
            return VERSION;
        }
        synchronized (GrobidProperties.class) {
            if (VERSION == null) {
                VERSION = readFromSystemPropertyOrFromFile("project.version", GROBID_VERSION_FILE);
            }
        }
        return VERSION;
    }

    public static String getRevision() {
        if (REVISION != null) {
            return REVISION;
        }
        synchronized (GrobidProperties.class) {
            if (REVISION == null) {
                REVISION = readFromSystemPropertyOrFromFile("gitRevision", GROBID_REVISION_FILE);
            }
        }
        return REVISION;
    }

    private static String readFromSystemPropertyOrFromFile(String systemPropertyName, String filePath) {
        String grobidVersion = UNKNOWN_VERSION_STR;
        String systemPropertyValue = System.getProperty(systemPropertyName);
        if (systemPropertyValue != null) {
            grobidVersion = systemPropertyValue;
        } else {
            try (InputStream is = GrobidProperties.class.getResourceAsStream(filePath)) {
                String grobidVersionTmp = IOUtils.toString(is, StandardCharsets.UTF_8);
                if (!StringUtils.startsWithIgnoreCase(grobidVersionTmp, "${project_")) {
                    grobidVersion = grobidVersionTmp;
                }
            } catch (IOException e) {
                LOGGER.error("Cannot read the version from resources", e);
            }
        }
        return grobidVersion;
    }

    /**
     * Returns the temprorary path of grobid
     *
     * @return a directory for temp files
     */
    public static File getTempPath() {
        if (grobidConfig.grobid.temp == null)
            return new File(System.getProperty("java.io.tmpdir"));
        else {
            if (!new File(grobidConfig.grobid.temp).isAbsolute()) {
                return new File(grobidHome.getPath(), grobidConfig.grobid.temp);
            } else {
                return new File(grobidConfig.grobid.temp);
            }
        }
    }

    public static void setNativeLibraryPath(final String nativeLibPath) {
        grobidConfig.grobid.nativelibrary = nativeLibPath;
    }

    /**
     * Returns the path to the native libraries as {@link File} object.
     *
     * @return folder that contains native libraries
     */
    public static File getNativeLibraryPath() {
        return new File(grobidHome.getPath(), grobidConfig.grobid.nativelibrary);
    }

    /**
     * Returns the installation path of DeLFT if set, null otherwise. It is required for using
     * a Deep Learning sequence labelling engine.
     *
     * @return path to the folder that contains the local install of DeLFT
     */
    public static String getDeLFTPath() {
        return grobidConfig.grobid.delft.install;
    }

    public static String getDeLFTFilePath() {
        String rawPath = grobidConfig.grobid.delft.install;
        File pathFile = new File(rawPath);
        if (!Files.exists(Paths.get(rawPath).toAbsolutePath())) {
            rawPath = "../" + rawPath;
            pathFile = new File(rawPath);
        }
        return pathFile.getAbsolutePath();
    }

    public static String getGluttonUrl() {
        if (StringUtils.isEmpty(grobidConfig.grobid.consolidation.glutton.url))
            return null;
        else
            return grobidConfig.grobid.consolidation.glutton.url;
    }

    public static void setGluttonUrl(final String theUrl) {
        grobidConfig.grobid.consolidation.glutton.url = theUrl;
    }

    /**
     * Returns the host for a proxy connection, given in the grobid config file.
     *
     * @return proxy host
     */
    public static String getProxyHost() {
        if (grobidConfig.grobid.proxy.host == null || StringUtils.trimToEmpty(grobidConfig.grobid.proxy.host).isEmpty())
            return null;
        else
            return grobidConfig.grobid.proxy.host;
    }

    /**
     * Sets the host a proxy connection, given in the config file.
     *
     * @param host the proxy host to be used
     */
    public static void setProxyHost(final String host) {
        grobidConfig.grobid.proxy.host = host;
        System.setProperty("http.proxyHost", host);
        System.setProperty("https.proxyHost", host);
    }

    /**
     * Returns the port for a proxy connection, given in the grobid config file.
     *
     * @return proxy port
     */
    public static Integer getProxyPort() {
        return grobidConfig.grobid.proxy.port;
    }

    /**
     * Set the "mailto" parameter to be used in the crossref query and in User-Agent
     * header, as recommended by CrossRef REST API documentation.
     *
     * @param mailto email parameter to be used for requesting crossref
     */
    public static void setCrossrefMailto(final String mailto) {
        grobidConfig.grobid.consolidation.crossref.mailto = mailto;
    }

    /**
     * Get the "mailto" parameter to be used in the crossref query and in User-Agent
     * header, as recommended by CrossRef REST API documentation.
     *
     * @return string of the email parameter to be used for requesting crossref
     */
    public static String getCrossrefMailto() {
        if (grobidConfig.grobid.consolidation.crossref.mailto == null || grobidConfig.grobid.consolidation.crossref.mailto.trim().length() == 0)
            return null;
        else
            return grobidConfig.grobid.consolidation.crossref.mailto;
    }

    /**
     * Set the Crossref Metadata Plus authorization token to be used for Crossref
     * requests for the subscribers of this service.  This token will ensure that said
     * requests get directed to a pool of machines that are reserved for "Plus" SLA users.
     *
     * @param token authorization token to be used for requesting crossref
     */
    public static void setCrossrefToken(final String token) {
        grobidConfig.grobid.consolidation.crossref.token = token;
    }

    /**
     * Get the Crossref Metadata Plus authorization token to be used for Crossref
     * requests for the subscribers of this service.  This token will ensure that said
     * requests get directed to a pool of machines that are reserved for "Plus" SLA users.
     *
     * @return authorization token to be used for requesting crossref
     */
    public static String getCrossrefToken() {
        if (grobidConfig.grobid.consolidation.crossref.token == null || grobidConfig.grobid.consolidation.crossref.token.trim().length() == 0)
            return null;
        else
            return grobidConfig.grobid.consolidation.crossref.token;
    }

    /**
     * Sets the port for a proxy connection, given in the grobid config file.
     *
     * @param port the proxy port
     */
    public static void setProxyPort(int port) {
        grobidConfig.grobid.proxy.port = port;
        System.setProperty("http.proxyPort", ""+port);
        System.setProperty("https.proxyPort", ""+port);
    }

    public static Integer getPdfaltoMemoryLimitMb() {
        return grobidConfig.grobid.pdf.pdfalto.memoryLimitMb;
    }

    public static Integer getPdfaltoTimeoutS() {
        return grobidConfig.grobid.pdf.pdfalto.timeoutSec;
    }

    public static Integer getPdfaltoTimeoutMs() {
        return grobidConfig.grobid.pdf.pdfalto.timeoutSec * 1000;
    }

    /*public static Integer getNBThreads() {
        Integer nbThreadsConfig = Integer.valueOf(grobidConfig.grobid.wapiti.nbThreads);
        if (nbThreadsConfig.intValue() == 0) {
            return Integer.valueOf(Runtime.getRuntime().availableProcessors());
        }
        return nbThreadsConfig;
    }*/

    /**
     * Returns the number of threads to be used when training with CRF Wapiti, given in the grobid config file.
     *
     * @return number of threads
     */
    public static Integer getWapitiNbThreads() {
        int nbThreadsConfig = grobidConfig.grobid.wapiti.nbThreads;
        if (nbThreadsConfig == 0) {
            return Runtime.getRuntime().availableProcessors();
        }
        return nbThreadsConfig;
    }

    // PDF with more blocks will be skipped
    public static Integer getPdfBlocksMax() {
        return grobidConfig.grobid.pdf.blocksMax;
    }

    // PDF with more tokens will be skipped
    public static Integer getPdfTokensMax() {
        return grobidConfig.grobid.pdf.tokensMax;
    }

    /**
     * Sets the number of threads for training a Wapiti model, given in the grobid config file.
     *
     * @param nbThreads umber of threads
     */
    /*public static void setNBThreads(int nbThreads) {
        grobidConfig.grobid.wapiti.nbThreads = nbThreads;
    }*/
    public static void setWapitiNbThreads(int nbThreads) {
        grobidConfig.grobid.wapiti.nbThreads = nbThreads;
    }

    public static String getLanguageDetectorFactory() {
        String factoryClassName = grobidConfig.grobid.languageDetectorFactory;
        if (StringUtils.isBlank(factoryClassName)) {
            throw new GrobidPropertyException("Language detection is enabled but a factory class name is not provided");
        }
        return factoryClassName;
    }

    /**
     * Sets if a language id shall be used, given in the grobid-property file.
     */
    /*public static void setUseLanguageId(final String useLanguageId) {
        setPropertyValue(GrobidPropertyKeys.PROP_USE_LANG_ID, useLanguageId);
    }*/

    public static String getSentenceDetectorFactory() {
        String factoryClassName = grobidConfig.grobid.sentenceDetectorFactory;
        if (StringUtils.isBlank(factoryClassName)) {
            throw new GrobidPropertyException("Sentence detection is enabled but a factory class name is not provided");
        }
        return factoryClassName;
    }

    /**
     * Returns the path to the home folder of pdf to xml converter.
     */
    public static void loadPdfaltoPath() {
        LOGGER.debug("loading pdfalto command path");
        String pathName = grobidConfig.grobid.pdf.pdfalto.path;
        pathToPdfalto = new File(grobidHome.getPath(), pathName);
        if (!pathToPdfalto.exists()) {
            throw new GrobidPropertyException(
                "Path to pdfalto doesn't exists. " +
                "Please set the path to pdfalto in the config file");
        }

        pathToPdfalto = new File(pathToPdfalto, Utilities.getOsNameAndArch());

        LOGGER.debug("pdfalto executable home directory set to " + pathToPdfalto.getAbsolutePath());
    }

    /**
     * Returns the path to the home folder of pdfalto program.
     *
     * @return path to pdfalto program
     */
    public static File getPdfaltoPath() {
        return pathToPdfalto;
    }

    public static ModelParameters getGrobidModelParameters(final String modelName) {
        ModelParameters param = modelMap.get(modelName);
        // if we have a flavor of the model, we can fall back to the configuration
        // of the parent model
        String fallBackModelName = modelName;
        while(param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            int ind = fallBackModelName.lastIndexOf("-");
            if (ind != -1) {
                fallBackModelName = modelName.substring(0,ind);
            } else {
                return null;
            }
            param = modelMap.get(fallBackModelName);
        }
        return param;
    }

    public static String getGrobidEngineName(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null)
            return null;
        return param.engine;
    }

    public static GrobidCRFEngine getGrobidEngine(final String modelName) {
        String engineName = getGrobidEngineName(modelName);
        if (engineName == null)
            return null;
        else
            return GrobidCRFEngine.get(engineName);
    }

    public static GrobidCRFEngine getGrobidEngine(final GrobidModel model) {
        return getGrobidEngine(model.getModelName());
    }

    public static File getModelPath(final GrobidModel model) {
        if (modelMap.get(model.getModelName()) == null) {
            // model is either:
            // - a flavor without config, but that should fallback to the parent model config
            //   if no specific config exists. If it is the case, the model path is infered
            //   from the flavor model name
            // - a normal model not specified in the config, so returning null

            if (getGrobidModelParameters(model.getModelName()) == null) {
                return null;
            }
        }
        String extension = getGrobidEngine(model).getExt();
        return new File(getGrobidHome(), FOLDER_NAME_MODELS + File.separator
            + model.getFolderName() + File.separator
            + FILE_NAME_MODEL + "." + extension);
    }

    public static File getModelPath() {
        return new File(getGrobidHome(), FOLDER_NAME_MODELS);
    }

    public static File getTemplatePath(final File resourcesDir, final GrobidModel model) {
        ModelParameters param = getGrobidModelParameters(model.getModelName());
        if (param == null)
            return null;

        File theFile = new File(resourcesDir, "dataset/" + model.getFolderName()
            + "/crfpp-templates/" + model.getTemplateName());
        if (!theFile.exists()) {
            theFile = new File("resources/dataset/" + model.getFolderName()
                + "/crfpp-templates/" + model.getTemplateName());
        }
        return theFile;
    }

    public static File getEvalCorpusPath(final File resourcesDir, final GrobidModel model) {
        File theFile = new File(resourcesDir, "dataset/" + model.getFolderName() + "/evaluation/");
        if (!theFile.exists()) {
            theFile = new File("resources/dataset/" + model.getFolderName() + "/evaluation/");
        }
        return theFile;
    }

    public static File getCorpusPath(final File resourcesDir, final GrobidModel model) {
        File theFile = new File(resourcesDir, "dataset/" + model.getFolderName() + "/corpus");
        if (!theFile.exists()) {
            theFile = new File("resources/dataset/" + model.getFolderName() + "/corpus");
        }
        return theFile;
    }

    public static String getLexiconPath() {
        return new File(getGrobidHome(), "lexicon").getAbsolutePath();
    }

    public static File getLanguageDetectionResourcePath() {
        return new File(getGrobidHome(), "language-detection");
    }

    /**
     * Returns the maximum parallel connections allowed in the pool.
     *
     * @return the number of connections
     */
    public static int getMaxConcurrency() {
        return grobidConfig.grobid.concurrency;
    }

    /**
     * Returns maximum time to wait before timeout when the pool is full.
     *
     * @return time to wait in milliseconds.
     */
    public static int getPoolMaxWait() {
        return grobidConfig.grobid.poolMaxWait * 1000;
    }

    /**
     * Returns the consolidation service to be used.
     *
     * @return the consolidation service to be used
     */
    public static GrobidConsolidationService getConsolidationService() {
        if (grobidConfig.grobid.consolidation.service == null) {
            grobidConfig.grobid.consolidation.service = "crossref";
        }
        return GrobidConsolidationService.get(grobidConfig.grobid.consolidation.service);
    }

    /**
     * Set which consolidation service to use
     */
    public static void setConsolidationService(String service) {
        grobidConfig.grobid.consolidation.service = service;
    }

    /**
     * Get the Crossref timeout in seconds for consolidation service requests.
     * @return timeout in seconds
     */
    public static int getCrossrefConsolidationTimeout() {
        if (grobidConfig.grobid.consolidation.crossref == null) {
            LOGGER.warn("Crossref consolidation configuration is missing. Using default timeout of 60 seconds.");
            return 60;
        }
        return grobidConfig.grobid.consolidation.crossref.timeoutSec;
    }

    /**
     * Get the minimum interval between consecutive CrossRef API request submissions (in milliseconds).
     * Returns -1 (auto-compute from tier) by default. A positive value overrides the tier-based rate.
     * @return minimum interval in milliseconds, or -1 for auto
     */
    public static long getCrossrefMinRequestInterval() {
        if (grobidConfig.grobid.consolidation.crossref == null) {
            return -1;
        }
        return grobidConfig.grobid.consolidation.crossref.minRequestIntervalMs;
    }

    /**
     * Get the Glutton timeout in seconds for consolidation service requests.
     * @return timeout in seconds
     */
    public static int getGluttonConsolidationTimeout() {
        if (grobidConfig.grobid.consolidation.glutton == null) {
            LOGGER.warn("Biblio-glutton consolidation configuration is missing. Using default timeout of 60 seconds.");
            return 60;
        }
        return grobidConfig.grobid.consolidation.glutton.timeoutSec;
    }

    /**
     * Get whether post-validation is enabled for CrossRef consolidation results.
     * When true, GROBID will validate CrossRef results against the source metadata.
     *
     * @return true if post-validation is enabled (default), false otherwise
     */
    public static boolean getCrossrefPostValidation() {
        if (grobidConfig.grobid.consolidation.crossref == null) {
            return true;
        }
        return grobidConfig.grobid.consolidation.crossref.postValidation;
    }

    /**
     * Returns if the execution context is stand alone or server.
     *
     * @return the context of execution. Return false if the property value is
     * null or empty.
     */
    public static boolean isContextExecutionServer() {
        return contextExecutionServer;
    }

    /**
     * Set if the execution context is stand alone or server.
     *
     * @param state true to set the context of execution to server, false else.
     */
    public static void setContextExecutionServer(boolean state) {
        contextExecutionServer = state;
    }

    public static String getPythonVirtualEnv() {
        return grobidConfig.grobid.delft.pythonVirtualEnv;
    }

    public static void setPythonVirtualEnv(String pythonVirtualEnv) {
        grobidConfig.grobid.delft.pythonVirtualEnv = pythonVirtualEnv;
    }

    public static int getWindow(final GrobidModel model) {
        ModelParameters parameters = getGrobidModelParameters(model.getModelName());
        if (parameters != null && parameters.wapiti != null)
            return parameters.wapiti.window;
        else
            return 20;
    }

    public static double getEpsilon(final GrobidModel model) {
        ModelParameters parameters = getGrobidModelParameters(model.getModelName());
        if (parameters != null && parameters.wapiti != null)
            return parameters.wapiti.epsilon;
        else
            return 0.00001;
    }

    public static int getNbMaxIterations(final GrobidModel model) {
        ModelParameters parameters = getGrobidModelParameters(model.getModelName());
        if (parameters != null && parameters.wapiti != null)
            return parameters.wapiti.nbMaxIterations;
        else
            return 2000;
    }

    public static boolean useELMo(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return false;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return false;
        }
        return param.delft.useELMo;
    }

    public static String getDelftArchitecture(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return null;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return null;
        }
        return param.delft.architecture;
    }

    public static String getDelftEmbeddingsName(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return null;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return null;
        }
        return param.delft.embeddings_name;
    }

    public static String getDelftTranformer(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return null;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return null;
        }
        return param.delft.transformer;
    }

    /**
    *  Return -1 if not set in the configuration and the default DeLFT value will be used in this case.
    */
    public static int getDelftTrainingMaxSequenceLength(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return -1;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }
        DelftModelParameterSet delftParamSet = param.delft.training;
        if (delftParamSet == null) {
            LOGGER.debug("No training configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }

        return param.delft.training.max_sequence_length;
    }

    /**
    *  Return -1 if not set in the configuration and the default DeLFT value will be used in this case.
    */
    public static int getDelftRuntimeMaxSequenceLength(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return -1;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }
        DelftModelParameterSet delftParamSet = param.delft.runtime;
        if (delftParamSet == null) {
            LOGGER.debug("No runtime configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }

        return param.delft.runtime.max_sequence_length;
    }

    /**
    *  Return -1 if not set in the configuration and the default DeLFT value will be used in this case.
    */
    public static int getDelftTrainingBatchSize(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return -1;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }
        DelftModelParameterSet delftParamSet = param.delft.training;
        if (delftParamSet == null) {
            LOGGER.debug("No training configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }

        return param.delft.training.batch_size;
    }

    /**
    *  Return -1 if not set in the configuration and the default DeLFT value will be used in this case.
    */
    public static int getDelftRuntimeBatchSize(final String modelName) {
        ModelParameters param = getGrobidModelParameters(modelName);
        if (param == null) {
            LOGGER.debug("No configuration parameter defined for model " + modelName);
            return -1;
        }
        DelftModelParameters delftParam = param.delft;
        if (delftParam == null) {
            LOGGER.debug("No configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }
        DelftModelParameterSet delftParamSet = param.delft.runtime;
        if (delftParamSet == null) {
            LOGGER.debug("No runtime configuration parameter defined for DeLFT engine for model " + modelName);
            return -1;
        }

        return param.delft.runtime.batch_size;
    }

    public static String getDelftArchitecture(final GrobidModel model) {
        return getDelftArchitecture(model.getModelName());
    }   

    /*protected static Map<String, String> getEnvironmentVariableOverrides(Map<String, String> environmentVariablesMap) {
        EnvironmentVariableProperties envParameters = new EnvironmentVariableProperties(environmentVariablesMap, "(grobid__).+");
        return envParameters.getConfigParameters();
    }*/
}
