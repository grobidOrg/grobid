package org.grobid.core.jni;

import java.util.concurrent.*;
import java.util.*;
import java.io.*;
import java.nio.file.Path;

import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.exceptions.GrobidResourceException;

import jep.Jep;
import jep.JepConfig;
import jep.JepException;
import jep.SubInterpreter;
import jep.SharedInterpreter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For using DeLFT deep learning models, we use JEP as JNI CPython interpreter.
 * JEP presents the following constraint: A thread that creates a JEP instance
 * must be reused for all method calls to that JEP instance. For ensuring this,
 * we pool the Jep instances in a singleton class.
 */
public class JEPThreadPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(JEPThreadPool.class);

    private int POOL_SIZE = 1;

    private final ExecutorService executor;
    private final ConcurrentMap<Long, Jep> jepInstances;

    private static volatile JEPThreadPool instance;

    public static JEPThreadPool getInstance() {
        if (instance == null) {
            getNewInstance();
        }
        return instance;
    }

    /**
     * Creates a new instance.
     */
    private static synchronized void getNewInstance() {
        LOGGER.debug("Get new instance of JEPThreadPool");
        instance = new JEPThreadPool();
    }

    /**
     * Hidden constructor
     */
    private JEPThreadPool() {
        // creating a pool of POOL_SIZE threads
        //executor = Executors.newFixedThreadPool(POOL_SIZE); 
        executor = Executors.newSingleThreadExecutor();
        // each of these threads is associated to a JEP instance
        jepInstances = new ConcurrentHashMap<>();

        // Add a shutdown hook to close all JEP instances when the JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("JVM shutdown detected, closing all JEP instances");
            shutdown();
        }));
    }

    private File getAndValidateDelftPath() {
        File delftPath = new File(GrobidProperties.getDeLFTFilePath());
        if (!delftPath.exists()) {
            throw new GrobidResourceException("DeLFT installation path does not exist");
        }
        if (!delftPath.isDirectory()) {
            throw new GrobidResourceException("DeLFT installation path is not a directory");
        }
        return delftPath;
    }

    private JepConfig getJepConfig(File delftPath, Path sitePackagesPath) {
        JepConfig config = new JepConfig();
        config.addIncludePaths(delftPath.getAbsolutePath());
        config.redirectStdout(System.out);
        config.redirectStdErr(System.err);
        if (sitePackagesPath != null) {
            config.addIncludePaths(sitePackagesPath.toString());
        }
        config.setClassLoader(Thread.currentThread().getContextClassLoader());
        return config;
    }

    private void initializeJepInstance(Jep jep, File delftPath) throws JepException {
        // import packages
        jep.eval("import os");
        jep.eval("os.chdir('" + delftPath.getAbsolutePath() + "')");

        // for using legacy Keras 2, and not Keras 3 installed by default by TensorFlow from version 2.16
        jep.eval("os.environ[\"TF_USE_LEGACY_KERAS\"] = \"1\"");
        jep.eval("os.environ[\"KERAS_BACKEND\"] = \"tensorflow\"");
        jep.eval("import tf_keras as keras");

        jep.eval("from delft.utilities.Embeddings import Embeddings");
        jep.eval("import delft.sequenceLabelling");
        jep.eval("from delft.sequenceLabelling import Sequence");
        jep.eval("from delft.sequenceLabelling.reader import load_data_and_labels_crf_file");
        jep.eval("from delft.sequenceLabelling.reader import load_data_crf_string");
        jep.eval("from sklearn.model_selection import train_test_split");
    }

    private Jep createJEPInstance() {
        Jep jep = null;
        boolean success = false;
        try {
            File delftPath = this.getAndValidateDelftPath();
            JepConfig config = this.getJepConfig(
                delftPath,
                PythonEnvironmentConfig.getInstance().getSitePackagesPath()
            );
            //jep = new SubInterpreter(config);
            try {
                SharedInterpreter.setConfig(config);
            } catch(Exception e) {
                LOGGER.info("JEP interpreter already initialized");
            }
            jep = new SharedInterpreter();
            this.initializeJepInstance(jep, delftPath);
            success = true;
            return jep;
        } catch(JepException e) {
            LOGGER.error("JEP initialization failed", e);
            throw new RuntimeException("JEP initialization failed", e);
        } catch(GrobidResourceException e) {
            LOGGER.error("DeLFT installation path invalid, JEP initialization failed", e);
            throw new RuntimeException("DeLFT installation path invalid, JEP initialization failed", e);
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("JEP environment not correctly installed or has incompatible binaries, JEP initialization failed", e);
            throw new RuntimeException("JEP environment not correctly installed or has incompatible binaries, JEP initialization failed", e);
        } finally {
            if (!success) {
                if (jep != null) {
                    try {
                        jep.close();
                    } catch (JepException e) {
                        LOGGER.error("Failed to close JEP instance", e);
                    }
                } else {
                    LOGGER.error("JEP initialisation failed");
                    throw new RuntimeException("JEP initialisation failed");
                }
            }
        }
    }

    /**
     * To be called by the thread executing python commands via JEP.
     * The method will return to the thread its dedicated Jep instance
     * (or create one the first time).
     */
    public synchronized Jep getJEPInstance() {
        long threadId = Thread.currentThread().getId();
        Jep jep = jepInstances.get(threadId);
        if (jep == null) {
            LOGGER.info("Creating JEP instance for thread " + threadId);
            jep = this.createJEPInstance();
            jepInstances.put(threadId, jep);
        }
        try {
            jep.isValidThread();
        } catch (JepException e) {
            LOGGER.warn("JEP instance no longer usable, creating new instance", e);
            jep = this.createJEPInstance();
            jepInstances.put(threadId, jep);
        }
        return jep;
    }

    public void run(Runnable task) throws InterruptedException {
        LOGGER.debug("running thread: " + Thread.currentThread().getId());
        Future future = executor.submit(task);
        // wait until done (in ms)
        while (!future.isDone()) {
            Thread.sleep(1);
        }
    }

    public String call(Callable<String> task) throws InterruptedException, ExecutionException {
        Future<String> future = executor.submit(task);
        // block until done
        return future.get();
    }

    /**
     * Close the JEP instance for the current thread and remove it from the map.
     * This should be called when the thread is done using JEP to free resources.
     */
    public synchronized void closeCurrentJEPInstance() {
        long threadId = Thread.currentThread().getId();
        Jep jep = jepInstances.remove(threadId);
        if (jep != null) {
            try {
                LOGGER.info("Closing JEP instance for thread " + threadId);
                jep.close();
            } catch (JepException e) {
                LOGGER.error("Failed to close JEP instance for thread " + threadId, e);
            }
        }
    }

    /**
     * Close all JEP instances and shutdown the executor.
     * This should be called when the application is shutting down.
     */
    public synchronized void shutdown() {
        LOGGER.info("Shutting down JEPThreadPool");
        // Close all JEP instances
        for (Map.Entry<Long, Jep> entry : jepInstances.entrySet()) {
            try {
                LOGGER.info("Closing JEP instance for thread " + entry.getKey());
                entry.getValue().close();
            } catch (JepException e) {
                LOGGER.error("Failed to close JEP instance for thread " + entry.getKey(), e);
            }
        }
        jepInstances.clear();

        // Shutdown the executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
