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
package org.grobid.service.process;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.Engine;
import org.grobid.core.factory.GrobidPoolingFactory;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.grobid.core.utilities.KeyGen;
import org.grobid.service.exceptions.GrobidServiceException;
import org.grobid.service.util.GrobidRestUtils;
import org.grobid.trainer.*;

@Singleton
public class GrobidRestProcessTraining {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestProcessTraining.class);

    /**
     * Models (by lowercase name) whose training is currently running, mapped to the token of the
     * training that owns the claim. Used to reject a new training request for a model that is
     * already being trained. The class is a singleton, so this instance field is shared across all
     * requests in the JVM. The map value (the owning token) lets a release be owner-checked, so a
     * stale kill of an old token cannot free a claim that a newer training of the same model holds.
     */
    private final Map<String, String> modelsInTraining = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> trainingsInProgress = new ConcurrentHashMap<>();
    /** Maps token → modelKey so that killTraining can release the per-model claim. */
    private final Map<String, String> tokenModelKey = new ConcurrentHashMap<>();

    @Inject
    public GrobidRestProcessTraining() {
    }

    /**
     * Atomically claim the in-training slot for a model on behalf of {@code token}. Returns
     * {@code false} if a training for this model is already running (the slot was not claimed by
     * this caller).
     */
    boolean tryClaim(String modelKey, String token) {
        return modelsInTraining.putIfAbsent(modelKey, token) == null;
    }

    /**
     * Release the in-training slot for a model so it can be trained again, but only if it is still
     * owned by {@code token}. This owner check (an atomic {@code remove(key, value)}) prevents a
     * kill/cleanup of a finished training from freeing a claim that a newer training of the same
     * model has since acquired.
     */
    void release(String modelKey, String token) {
        modelsInTraining.remove(modelKey, token);
    }

    /**
     * Check if a model name matches an existing GROBID model, as declared in the GrobidModels registry.
     */
    public static boolean containsModel(String targetModel) {
        for (GrobidModels model : GrobidModels.values()) {
            if (model.name().toLowerCase().equals(targetModel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a model given a model name and a target architecture (CRF default, BiLSTM-CRF or BiLSMT-CRF-ELMo).
     * The model is returned in a zip archive (the model being several files in the case of deep learning
     * models)
     *
     * @return a response object containing the zipped model
     */
    public Response getModel(String model, String architecture) {
        LOGGER.debug(">> " + GrobidRestProcessGeneric.class.getName() + ".getModel");
        Response response = null;
        String assetPath = null;
        try {
            // is the model name valid?
            /*if (!containsModel(model)) {
                throw new GrobidServiceException(
                    "The indicated model name " + model + " is invalid or unsupported.",
                    Status.BAD_REQUEST);
            }*/

            GrobidModel theModel = GrobidModels.modelFor(model.toLowerCase().replace("-", "/"));

            File theModelFile = null;
            if (theModel != null) {
                theModelFile = new File(theModel.getModelPath());
            }

            if (architecture == null || architecture.length() == 0) {
                // conservative defaulting of the architecture
                architecture = "crf";
            }

            if (theModel == null) {
                throw new GrobidServiceException(
                        "The indicated model name " + model + " is invalid or unsupported.",
                        Status.BAD_REQUEST);
            } else if (theModelFile == null || !theModelFile.exists()) {
                // model name was valid but no trained model available
                //response = Response.status(Status.NO_CONTENT).build();
                throw new GrobidServiceException(
                        "The indicated model name " + model + " is valid but not trained.",
                        Status.BAD_REQUEST);
            } else {
                ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(ouputStream);

                if (architecture.toLowerCase().equals("crf")) {
                    response = Response.status(Status.OK).type("application/zip").build();

                    out.putNextEntry(new ZipEntry("model.wapiti"));
                    byte[] buffer = new byte[1024];
                    try {
                        FileInputStream in = new FileInputStream(theModelFile);
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        in.close();
                        out.closeEntry();
                    } catch (IOException e) {
                        throw new GrobidServiceException("IO Exception when zipping model file", e,
                                Status.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    System.out.println(theModelFile.getAbsolutePath());

                    // put now the different assets in the case of a Deep Learning model,
                    // i.e. config.json, model_weights.hdf5, preprocessor.pkl
                    File[] files = theModelFile.listFiles();
                    if (files != null) {
                        byte[] buffer = new byte[1024];
                        for (final File currFile : files) {
                            if (currFile.getName().toLowerCase().endsWith(".hdf5")
                                    || currFile.getName().toLowerCase().endsWith(".json")
                                    || currFile.getName().toLowerCase().endsWith(".pkl")
                                    || currFile.getName().toLowerCase().endsWith(".txt")) {
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
                                    throw new GrobidServiceException("IO Exception when zipping", e,
                                            Status.INTERNAL_SERVER_ERROR);
                                }
                            }
                        }
                    }
                }

                out.finish();
                response = Response
                        .ok()
                        .type("application/zip")
                        .entity(ouputStream.toByteArray())
                        .header("Content-Disposition", "attachment; filename=\"model.zip\"")
                        .build();
                out.close();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (GrobidServiceException exp) {
            LOGGER.error("Service cannot be realized: " + exp.getMessage());
            response = Response.status(exp.getResponseCode()).entity(exp.getMessage()).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
        }

        return response;
    }

    /**
     * Start the training of a model based on its name and a target architecture (CRF default, BiLSTM-CRF or
     * BiLSMT-CRF-ELMo) and a training mode. Send back a token to the calling client to retrieve training
     * state and eventually the evaluation metrics via the service /api/resultTraining
     *
     * @return a response object containing the token corresponding to the launched training
     */
    public Response trainModel(
            String model,
            String architecture,
            String type,
            double ratio,
            int n,
            boolean incremental) {
        Response response = null;

        try {
            // is the model name valid?
            /*if (!containsModel(model)) {
                throw new GrobidServiceException(
                    "The indicated model name " + model + " is invalid or unsupported.", Status.BAD_REQUEST);
            }*/

            // create a token for the training
            String token = KeyGen.getKey();
            GrobidProperties.getInstance();

            File home = GrobidProperties.getInstance().getGrobidHomePath();
            AbstractTrainer trainer = getTrainer(model);

            // Reject the request if a training for the same model is already running. Flavor
            // variants (e.g. "header" vs "header-light") write different model files and are
            // distinct models, so they are keyed separately and do not block each other.
            String modelKey = model.toLowerCase();
            if (!tryClaim(modelKey, token)) {
                LOGGER.warn(
                        "Rejected training request for model '{}': a training for this model is already in progress.",
                        model);
                return Response.status(Status.CONFLICT)
                        .entity("{\"error\": \"A training for model '" + model + "' is already in progress.\"}")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }

            try {
                String tokenPath = home.getAbsolutePath() + "/training-history/" + token;
                File tokenDir = new File(tokenPath);
                if (!tokenDir.exists()) {
                    tokenDir.mkdirs();
                }

                ExecutorService executorService = Executors.newFixedThreadPool(1);
                TrainTask trainTask = new TrainTask(trainer, type, token, ratio, n, incremental, modelKey,
                        modelsInTraining, trainingsInProgress, tokenModelKey);
                FileUtils.writeStringToFile(new File(tokenPath + "/status"), "ongoing", StandardCharsets.UTF_8);
                // Step 1 – pre-register a null placeholder BEFORE submit().
                // Without this, a fast-finishing task could call remove() before the real
                // Future is put(), leaving a permanent orphan entry in the map.
                trainingsInProgress.put(token, null);
                tokenModelKey.put(token, modelKey);
                Future<?> trainingFuture = executorService.submit(trainTask);
                // Step 2 – swap the placeholder for the real Future so killTraining can call
                // cancel(). replace() (not put()) is intentional: if the task already finished and
                // its finally-block removed the entry, replace() is a no-op, so a completed Future
                // is not re-inserted as a permanent orphan.
                trainingsInProgress.replace(token, trainingFuture);
                // Orderly shutdown so the worker thread terminates once the submitted training
                // task completes. Without this the FixedThreadPool keeps its core thread alive
                // indefinitely and every request would permanently leak one JVM thread (DoS).
                executorService.shutdown();
            } catch (Exception e) {
                // The worker never took ownership of the claim (it releases it on completion),
                // so release it here to avoid leaving the model permanently blocked.
                release(modelKey, token);
                tokenModelKey.remove(token);
                trainingsInProgress.remove(token);
                throw e;
            }

            if (GrobidRestUtils.isResultNullOrEmpty(token)) {
                // it should never be the case, but let's be conservative!
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                        .entity("{\"token\": \"" + token + "\"}")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (GrobidServiceException exp) {
            LOGGER.error("Service cannot be realized: " + exp.getMessage());
            response = Response.status(exp.getResponseCode()).entity(exp.getMessage()).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
        }

        return response;
    }

    private static AbstractTrainer getTrainer(String model) {
        return TrainerRegistry.getTrainer(model);
    }

    private static class TrainTask implements Runnable {
        private AbstractTrainer trainer;
        private String type;
        private String token;
        private int n = 10;
        private double ratio = 1.0;
        private boolean incremental = false;
        private final String modelKey;
        private final Map<String, String> registry;
        private final Map<String, Future<?>> trainingsInProgress;
        private final Map<String, String> tokenModelKey;

        public TrainTask(AbstractTrainer trainer, String type, String token, double ratio, int n, boolean incremental,
                String modelKey, Map<String, String> registry, Map<String, Future<?>> trainingsInProgress,
                Map<String, String> tokenModelKey) {
            this.trainer = trainer;
            this.type = type;
            this.token = token;
            this.ratio = ratio;
            this.n = n;
            this.incremental = incremental;
            this.modelKey = modelKey;
            this.registry = registry;
            this.trainingsInProgress = trainingsInProgress;
            this.tokenModelKey = tokenModelKey;
        }

        @Override
        public void run() {
            String tokenPath = null;
            try {
                File home = GrobidProperties.getInstance().getGrobidHomePath();
                tokenPath = home.getAbsolutePath() + "/training-history/" + this.token;
                File tokenDir = new File(tokenPath);

                String results = null;
                //PrintStream writeAdvancement = new PrintStream(new FileOutputStream(tokenPath + "/train.txt"));

                //java.lang.System.setErr(writeAdvancement);
                switch (this.type.toLowerCase()) {
                    // possible values are `full`, `holdout`, `split`, `nfold`
                    case "full" :
                        AbstractTrainer.runTraining(this.trainer, this.incremental);
                        break;
                    case "holdout" :
                        AbstractTrainer.runTraining(this.trainer, this.incremental);
                        results = AbstractTrainer.runEvaluation(this.trainer);
                        break;
                    case "split" :
                        results = AbstractTrainer
                                .runSplitTrainingEvaluation(this.trainer, this.ratio, this.incremental);
                        break;
                    case "nfold" :
                        if (n == 0) {
                            throw new IllegalArgumentException("N should be > 0");
                        }
                        results = AbstractTrainer.runNFoldEvaluation(this.trainer, this.n);
                        break;
                    default :
                        throw new IllegalStateException("Invalid training type: " + this.type);
                }
                //java.lang.System.setErr(java.lang.System.err);

                // update status
                FileUtils.writeStringToFile(new File(tokenPath + "/status"), "done", StandardCharsets.UTF_8);

                // write results, if any
                if (results != null) {
                    FileUtils.writeStringToFile(new File(tokenPath + "/report.txt"), results, StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                LOGGER.error("Training failed for token " + token, e);
                if (tokenPath != null) {
                    try {
                        writeTrainingStatus(tokenPath, Thread.currentThread().isInterrupted() ? "killed" : "failed");
                    } catch (IOException ioException) {
                        LOGGER.error("Could not update status for token " + token, ioException);
                    }
                }
            } finally {
                // Release the per-model lock so the same model can be trained again, even if the
                // training above failed (e.g. a runtime exception from the underlying trainer).
                // Owner-checked (remove(key, value)) so we only release a claim we still own.
                registry.remove(this.modelKey, this.token);
                trainingsInProgress.remove(this.token);
                tokenModelKey.remove(this.token);
            }
        }
    }

    public Response allTraining() {
        Response response;
        try {
            File home = GrobidProperties.getInstance().getGrobidHomePath();
            File historyDir = new File(home.getAbsolutePath() + "/training-history");
            List<String> tokens = new ArrayList<>();

            // This scans the filesystem, so it may surface stale "ongoing" entries from crashed
            // runs. Callers should treat returned tokens as advisory; use killTraining to clean up.
            File[] tokenDirectories = historyDir.listFiles(File::isDirectory);
            if (tokenDirectories != null) {
                for (File tokenDirectory : tokenDirectories) {
                    File status = new File(tokenDirectory, "status");
                    if (status.exists()) {
                        String statusString = FileUtils.readFileToString(status, StandardCharsets.UTF_8).trim();
                        if ("ongoing".equals(statusString)) {
                            tokens.add(tokenDirectory.getName());
                        }
                    }
                }
            }

            response = Response.status(Response.Status.OK)
                    .entity(new ObjectMapper().writeValueAsString(Map.of("tokens", tokens)))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        }

        return response;
    }

    public Response killTraining(String token) {
        Response response;
        try {
            validateToken(token);

            File home = GrobidProperties.getInstance().getGrobidHomePath();
            File tokenDirectory = new File(home.getAbsolutePath() + "/training-history/" + token);
            if (!tokenDirectory.exists() || !tokenDirectory.isDirectory()) {
                throw new GrobidServiceException(
                        "The indicated token " + token + " is not matching an existing training.", Status.BAD_REQUEST);
            }

            File statusFile = new File(tokenDirectory, "status");
            String statusString = statusFile.exists()
                    ? FileUtils.readFileToString(statusFile, StandardCharsets.UTF_8).trim()
                    : null;

            Future<?> future = trainingsInProgress.remove(token);
            boolean killed = false;
            if (future != null) {
                // Note: future.cancel(true) only interrupts the JVM thread. Native training
                // processes (Wapiti, DeLFT) typically ignore thread interruption and may keep
                // running; "killed" status here is therefore optimistic.
                killed = future.cancel(true);
            } else if ("ongoing".equals(statusString)) {
                // Stale training state from a previously interrupted process/container — mark as
                // killed so the model can be retrained.
                killed = true;
            }

            if (killed) {
                if ("ongoing".equals(statusString)) {
                    writeTrainingStatus(tokenDirectory.getAbsolutePath(), "killed");
                }
                // Release the per-model claim so the same model can be trained again. The release
                // is owner-checked (keyed by this token), so if the worker already finished and a
                // newer training of the same model has claimed the slot, we do not free its claim.
                String modelKey = tokenModelKey.remove(token);
                if (modelKey != null) {
                    release(modelKey, token);
                }
            }
            String responseStatus = killed ? "killed" : (statusString != null ? statusString : "unknown");

            response = Response.status(Response.Status.OK)
                    .entity("{\"status\": \"" + responseStatus + "\"}")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                    .build();
        } catch (GrobidServiceException exp) {
            LOGGER.error("Service cannot be realized: " + exp.getMessage());
            response = Response.status(exp.getResponseCode()).entity(exp.getMessage()).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        }

        return response;
    }

    /**
     * Validate a training token to prevent directory-traversal attacks and reject blank input.
     *
     * @throws GrobidServiceException (BAD_REQUEST) if the token is null/blank or contains path-separator characters.
     */
    private static void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new GrobidServiceException("Token must not be empty", Status.BAD_REQUEST);
        }
        if (token.contains("..") || token.contains("/") || token.contains("\\")) {
            throw new GrobidServiceException("Invalid token", Status.BAD_REQUEST);
        }
    }

    private static void writeTrainingStatus(String tokenPath, String status) throws IOException {
        FileUtils.writeStringToFile(new File(tokenPath + "/status"), status, StandardCharsets.UTF_8);
    }

    /**
     * Given a training token delivered by the service `modelTraining`, this service gives the possibility
     * of following the advancement of the training and eventually get back the associated evaluation.
     * Depending on the state of the training, the service will returns:
     * - if the training is ongoing, an indication of advancement as a string
     * - it the training is completed, evaluation statistics depending on the selected type of training
     *
     * @return a response object containing information on the training corresponding to the token
     */
    public Response resultTraining(String token) {
        Response response = null;
        try {
            validateToken(token);

            // access report file under token subdirectory
            File home = GrobidProperties.getInstance().getGrobidHomePath();
            String tokenPath = home.getAbsolutePath() + "/training-history/" + token;

            File tokenDirectory = new File(tokenPath);
            if (!tokenDirectory.exists() || !tokenDirectory.isDirectory()) {
                throw new GrobidServiceException(
                        "The indicated token " + token + " is not matching an existing training.", Status.BAD_REQUEST);
            }

            // try to get the status
            File status = new File(tokenDirectory.getAbsolutePath() + "/status");
            String statusString = null;
            if (!status.exists()) {
                LOGGER.warn("Status file is missing in the training history corresponding to token " + token);
            } else {
                statusString = FileUtils.readFileToString(status, StandardCharsets.UTF_8).trim();
            }

            if (statusString != null && statusString.equals("ongoing")) {
                response = Response.status(Response.Status.OK)
                        .entity("{\"status\": \"" + statusString + "\"}")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            } else {
                // try to get the evaluation report
                File report = new File(tokenDirectory.getAbsolutePath() + "/report.txt");
                if (!report.exists()) {
                    throw new GrobidServiceException(
                            "The indicated token "
                                    + token
                                    + " is not matching an existing ongoing or completed training.",
                            Status.BAD_REQUEST);
                } else {
                    String reportStr = FileUtils.readFileToString(report, StandardCharsets.UTF_8);

                    response = Response.status(Response.Status.OK)
                            .entity(
                                    "{\"status\": \""
                                            + statusString
                                            + "\", \"report\": "
                                            + new ObjectMapper().writeValueAsString(reportStr)
                                            + "}")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                            .header("Access-Control-Allow-Origin", "*")
                            .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                            .build();
                }
            }

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (GrobidServiceException exp) {
            LOGGER.error("Service cannot be realized: " + exp.getMessage());
            response = Response.status(exp.getResponseCode()).entity(exp.getMessage()).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
        }

        return response;
    }

    public Response createTraining(
            final InputStream inputStream,
            final String filename,
            final GrobidModels.Flavor flavor) {
        Response response = null;
        String retVal = null;
        File originFile = null;
        Engine engine = null;
        String outputPath = null;
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
            outputPath = GrobidProperties.getTempPath().getPath() + File.separator + KeyGen.getKey();

            Files.createDirectories(Path.of(outputPath));
            engine.createTraining(originFile, outputPath, outputPath, -1, flavor);

            // Rename all the generated output files with the original filename as suffix
            File[] outputFileList = new File(outputPath).listFiles();
            if (ArrayUtils.isNotEmpty(outputFileList)) {
                String[] split = outputFileList[0].getName().split(".training");
                String trainingDataBaseName = split[0];
                String inputFileBaseName = FilenameUtils.getBaseName(filename);
                for (File file : outputFileList) {
                    if (file.isFile() && file.getName().startsWith(trainingDataBaseName)) {
                        String newFileName = file.getName().replace(trainingDataBaseName, inputFileBaseName);
                        File newFile = new File(outputPath, newFileName);
                        Files.move(file.toPath(), newFile.toPath());
                    }
                }
            } else {
                LOGGER.warn("No training files generated.");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ZipOutputStream out = new ZipOutputStream(outputStream);

            File outputPathDir = new File(outputPath);
            if (outputPathDir.exists()) {
                File[] files = outputPathDir.listFiles();
                if (files != null) {
                    byte[] buffer = new byte[1024];
                    for (final File currFile : files) {
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
                            throw new GrobidServiceException("IO Exception when zipping", e,
                                    Status.INTERNAL_SERVER_ERROR);
                        }

                    }
                }
            }
            out.finish();

            String outputFilename = StringUtils.replaceIgnoreCase(filename, "pdf", "zip");

            response = Response
                    .ok()
                    .type("application/zip")
                    .entity(outputStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"" + outputFilename + "\"")
                    .build();
            out.close();

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
            if (originFile != null)
                IOUtilities.removeTempFile(originFile);

            if (outputPath != null) {
                IOUtilities.removeTempDirectory(outputPath);
            }

            if (engine != null) {
                GrobidPoolingFactory.returnEngine(engine);
            }
        }

        return response;
    }
    //        GrobidMainArgs pGbdArgs = new GrobidMainArgs();
    //        pGbdArgs.setPath2Input(inputPath);
    //
    //        try(ProcessEngine processEngine = new ProcessEngine()) {
    //            processEngine.createTraining(pGbdArgs);
    //        }
    //    }
}
