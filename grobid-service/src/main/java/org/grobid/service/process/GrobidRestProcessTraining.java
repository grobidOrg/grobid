package org.grobid.service.process;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Singleton
public class GrobidRestProcessTraining {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidRestProcessTraining.class);

    @Inject
    public GrobidRestProcessTraining() {
    }

    /**
     * Cehck if a model name matches an existing GROBID model, as declared in the GrobidModels registry.
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
            } else  {
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
        } catch(GrobidServiceException exp) {
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
    public Response trainModel(String model, String architecture, String type, double ratio, int n, boolean incremental) {
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

            String tokenPath = home.getAbsolutePath() + "/training-history/" + token;
            File tokenDir = new File(tokenPath);
            if (!tokenDir.exists()) {
                tokenDir.mkdirs();
            }

            ExecutorService executorService = Executors.newFixedThreadPool(1);
            TrainTask trainTask = new TrainTask(trainer, type, token, ratio, n, incremental);
            FileUtils.writeStringToFile(new File(tokenPath + "/status"), "ongoing", "UTF-8");
            executorService.submit(trainTask);

            if (GrobidRestUtils.isResultNullOrEmpty(token)) {
                // it should never be the case, but let's be conservative!
                response = Response.status(Response.Status.NO_CONTENT).build();
            } else {
                response = Response.status(Response.Status.OK)
                        .entity("{\"token\": \""+ token + "\"}")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
            }
        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch(GrobidServiceException exp) {
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

        public TrainTask(AbstractTrainer trainer, String type, String token, double ratio, int n, boolean incremental) {
            this.trainer = trainer;
            this.type = type;
            this.token = token;
            this.ratio = ratio;
            this.n = n;
            this.incremental = incremental;
        }

        @Override
        public void run() {
            try {
                File home = GrobidProperties.getInstance().getGrobidHomePath();
                String tokenPath = home.getAbsolutePath() + "/training-history/" + this.token;
                File tokenDir = new File(tokenPath);

                String results = null;
                //PrintStream writeAdvancement = new PrintStream(new FileOutputStream(tokenPath + "/train.txt")); 

                //java.lang.System.setErr(writeAdvancement);
                switch (this.type.toLowerCase()) {
                    // possible values are `full`, `holdout`, `split`, `nfold`
                    case "full":
                        AbstractTrainer.runTraining(this.trainer, this.incremental);
                        break;
                    case "holdout":
                        AbstractTrainer.runTraining(this.trainer, this.incremental);
                        results = AbstractTrainer.runEvaluation(this.trainer);
                        break;
                    case "split":
                        results = AbstractTrainer.runSplitTrainingEvaluation(this.trainer, this.ratio, this.incremental);
                        break;
                    case "nfold":
                        if (n == 0) {
                            throw new IllegalArgumentException("N should be > 0");
                        }
                        results = AbstractTrainer.runNFoldEvaluation(this.trainer, this.n);
                        break;
                    default:
                        throw new IllegalStateException("Invalid training type: " + this.type);
                }
                //java.lang.System.setErr(java.lang.System.err);

                // update status
                FileUtils.writeStringToFile(new File(tokenPath + "/status"), "done", "UTF-8");

                // write results, if any
                if (results != null) {
                    FileUtils.writeStringToFile(new File(tokenPath + "/report.txt"), results, "UTF-8");
                }
            } catch(IOException e) {
                LOGGER.error("Failed to write training results for token " + token, e);
            }
        }
    }

    /**
     * Given a training token delivered by the service `modelTraining`, this service gives the possibility
     * of following the advancement of the training and eventually get back the associated evaluation.
     * Depending on the state of the training, the service will returns:
     * - if the training is ongoing, an indication of advancement as a string
     * - it the training is completed, evaluation statistics dependeing on the selected type of training
     *
     * @return a response object containing information on the training corresponding to the token
     */
    public Response resultTraining(String token) {
        Response response = null;
        try {
            // Validate the token to prevent directory traversal
            if (token.contains("..") || token.contains("/") || token.contains("\\")) {
                throw new GrobidServiceException("Invalid token", Status.BAD_REQUEST);
            }

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
                statusString = FileUtils.readFileToString(status, "UTF-8");
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
                        "The indicated token " + token + " is not matching an existing ongoing or completed training.",
                        Status.BAD_REQUEST);
                } else {
                    String reportStr = FileUtils.readFileToString(report, "UTF-8");

                    response = Response.status(Response.Status.OK)
                        .entity("{\"status\": \"" + statusString + "\", \"report\": " + new ObjectMapper().writeValueAsString(reportStr) + "}")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON + "; charset=UTF-8")
                        .header("Access-Control-Allow-Origin", "*")
                        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                        .build();
                }
            }

        } catch (NoSuchElementException nseExp) {
            LOGGER.error("Could not get an engine from the pool within configured time. Sending service unavailable.");
            response = Response.status(Status.SERVICE_UNAVAILABLE).build();
        } catch(GrobidServiceException exp) {
            LOGGER.error("Service cannot be realized: " + exp.getMessage());
            response = Response.status(exp.getResponseCode()).entity(exp.getMessage()).build();
        } catch (Exception exp) {
            LOGGER.error("An unexpected exception occurs. ", exp);
            response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(exp.getMessage()).build();
        } finally {
        }

        return response;
    }

    public Response createTraining(final InputStream inputStream, final String filename, final GrobidModels.Flavor flavor) {
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
                            throw new GrobidServiceException("IO Exception when zipping", e, Status.INTERNAL_SERVER_ERROR);
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
                .header("Content-Disposition", "attachment; filename=\""+ outputFilename +"\"")
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

