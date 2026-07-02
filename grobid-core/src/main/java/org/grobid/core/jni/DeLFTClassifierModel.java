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
package org.grobid.core.jni;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jep.Jep;
import jep.JepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;

public class DeLFTClassifierModel {
    public static final Logger LOGGER = LoggerFactory.getLogger(DeLFTClassifierModel.class);

    // Exploit JNI CPython interpreter to execute load and execute a DeLFT deep learning model
    private String modelName;
    private String architecture;

    public DeLFTClassifierModel(String model, String architecture) {
        this.modelName = model;
        this.architecture = architecture;
        try {
            LOGGER.info(
                    "Loading DeLFT classification model for "
                            + this.modelName
                            + " in "
                            + GrobidProperties.getInstance().getModelPath());
            JEPThreadPoolClassifier.getInstance()
                    .run(
                            new InitModel(this.modelName, GrobidProperties.getInstance().getModelPath(),
                                    this.architecture));
        } catch (InterruptedException | RuntimeException e) {
            LOGGER.error("DeLFT model " + this.modelName + " initialization failed", e);
        }
    }

    class InitModel implements Runnable {
        private String modelName;
        private File modelPath;
        private String architecture;

        public InitModel(String modelName, File modelPath, String architecture) {
            this.modelName = modelName;
            this.modelPath = modelPath;
            this.architecture = architecture;
        }

        @Override
        public void run() {
            Jep jep = JEPThreadPoolClassifier.getInstance().getJEPInstance();
            try {
                LOGGER.info("Init DeLFT classifier...");
                // as dash characters are forbidden in python variable name...
                String model_variable = this.modelName.replace("-", "_");
                jep.eval(model_variable + " = Classifier('" + this.modelName + "_" + this.architecture + "')");
                jep.eval(model_variable + ".load(dir_path='" + this.modelPath.getAbsolutePath() + "')");

                GrobidProperties.getInstance();
                if (GrobidProperties.getDelftRuntimeMaxSequenceLength(this.modelName) != -1) {
                    jep.eval(
                            this.modelName
                                    + ".config.max_sequence_length="
                                    +
                                    GrobidProperties.getDelftRuntimeMaxSequenceLength(this.modelName));
                }

                if (GrobidProperties.getDelftRuntimeBatchSize(this.modelName) != -1) {
                    jep.eval(
                            this.modelName
                                    + ".config.batch_size="
                                    +
                                    GrobidProperties.getDelftRuntimeBatchSize(this.modelName));
                }

            } catch (JepException e) {
                LOGGER.error("DeLFT classifier model initialization failed. ", e);
                throw new GrobidException("DeLFT classifier model initialization failed. ", e);
            }
        }
    }

    private class ClassificationTask implements Callable<String> {
        private List<String> data;
        private String modelName;

        public ClassificationTask(String modelName, List<String> data) {
            //System.out.println("label thread: " + Thread.currentThread().getId());
            this.modelName = modelName;
            this.data = data;
        }

        private void setJepStringValueWithFileFallback(
                Jep jep,
                String name,
                List<String> values) throws JepException, IOException {
            try {
                jep.set(name, values);
                // convert PyJList to a normal python list (necessary for Hugging Face transformer tokenizer input)
                jep.eval(name + " = list(" + name + ")");
            } catch (JepException e) {
                // we have normally the Java List as a PyJList in python, which should
                // be equivalent to a normal python list
                File tempFile = IOUtilities.newTempFile(name, ".data");
                LOGGER.debug(
                        "Falling back to file {} due to exception: {}",
                        tempFile,
                        e.toString());
                IOUtilities.writeListInFile(tempFile.getAbsolutePath(), values, "|");
                jep.eval("from pathlib import Path");
                jep.eval(
                        name
                                + " = Path('"
                                + tempFile.getAbsolutePath()
                                +
                                "').read_text(encoding='utf-8').split(\"|\")");
                tempFile.delete();
            }
        }

        @Override
        public String call() {
            Jep jep = JEPThreadPoolClassifier.getInstance().getJEPInstance();
            String results = null;
            try {
                // load and classify, input here is an array of texts to classify
                this.setJepStringValueWithFileFallback(jep, "input", this.data);
                String model_variable = this.modelName.replace("-", "_");
                jep.eval("jsondict = " + model_variable + ".predict(input, 'json', use_main_thread_only=True)");
                //jep.eval("print(json.dumps(jsondict, sort_keys=False, indent=4, ensure_ascii=False))");
                Object objectResult = jep
                        .getValue("json.dumps(jsondict, sort_keys=True, indent=4, ensure_ascii=False)");

                results = (String) objectResult;

            } catch (JepException e) {
                LOGGER.error("DeLFT model classification via JEP failed", e);
            } catch (IOException e) {
                LOGGER.error("DeLFT model classification failed", e);
            } finally {
                // No need to close the JEP instance here as it's managed by the pool
                // and will be reused for other tasks
                // cleaning
                try {
                    jep.eval("del jsondict");
                    jep.eval("del input");
                } catch (JepException e) {
                    LOGGER.error("DeLFT model classification via JEP failed", e);
                }
            }
            //System.out.println(labelledData.toString());
            return results;
        }
    }

    /**
     * Classify an array of string in batch. The result is a json array giving
     * for each text the classification results.
     */
    public String classify(List<String> data) {
        String result = null;
        try {
            result = JEPThreadPoolClassifier.getInstance().call(new ClassificationTask(this.modelName, data));
        } catch (InterruptedException e) {
            LOGGER.error("DeLFT model " + this.modelName + " classification interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("DeLFT model " + this.modelName + " classification failed", e);
        }
        return result;
    }

    /**
     * Training via JNI CPython interpreter (JEP). It appears that after some epochs, the JEP thread
     * usually hangs... Possibly issues with IO threads at the level of JEP (output not consumed because
     * of \r and no end of line?).
     */
    public static void trainJNI(String modelName, File trainingData, File outputModel) {
        try {
            LOGGER.info("Train DeLFT classification model " + modelName + "...");
            GrobidProperties.getInstance();
            JEPThreadPoolClassifier.getInstance()
                    .run(
                            new TrainTask(modelName, trainingData, GrobidProperties.getModelPath()));
        } catch (InterruptedException e) {
            LOGGER.error("Train DeLFT classification model " + modelName + " task failed", e);
        }
    }

    private static class TrainTask implements Runnable {
        private String modelName;
        private File trainPath;
        private File modelPath;
        private String architecture;
        private boolean incremental;

        public TrainTask(String modelName, File trainPath, File modelPath) {
            //public TrainTask(String modelName, File trainPath, File modelPath, String architecture, boolean incremental) {
            //System.out.println("train thread: " + Thread.currentThread().getId());
            this.modelName = modelName;
            this.trainPath = trainPath;
            this.modelPath = modelPath;
            this.architecture = null;
            this.incremental = false;
        }

        @Override
        public void run() {
            Jep jep = JEPThreadPoolClassifier.getInstance().getJEPInstance();
            try {
                // load data
                // to be reviewed for classification
                jep.eval(
                        "x_all, y_all, f_all = load_data_and_labels_crf_file('"
                                + this.trainPath.getAbsolutePath()
                                + "')");
                jep.eval("x_train, x_valid, y_train, y_valid = train_test_split(x_all, y_all, test_size=0.1)");
                jep.eval("print(len(x_train), 'train sequences')");
                jep.eval("print(len(x_valid), 'validation sequences')");

                GrobidProperties.getInstance();
                String useELMo = "False";
                if (GrobidProperties.useELMo(this.modelName)) {
                    useELMo = "True";
                }

                String localArgs = "";
                if (GrobidProperties.getDelftTrainingMaxSequenceLength(this.modelName) != -1)
                    localArgs += ", maxlen="
                            +
                            GrobidProperties.getDelftTrainingMaxSequenceLength(this.modelName);

                if (GrobidProperties.getDelftTrainingBatchSize(this.modelName) != -1)
                    localArgs += ", batch_size="
                            +
                            GrobidProperties.getDelftTrainingBatchSize(this.modelName);

                if (GrobidProperties.getDelftTranformer(modelName) != null) {
                    localArgs += ", transformer="
                            +
                            GrobidProperties.getDelftTranformer(modelName);
                }

                // init model to be trained
                if (this.architecture == null) {
                    jep.eval(
                            "model = Classifier('"
                                    + this.modelName
                                    +
                                    "', max_epoch=100, recurrent_dropout=0.50, embeddings_name='glove-840B', use_ELMo="
                                    + useELMo
                                    + localArgs
                                    + ")");
                } else {
                    jep.eval(
                            "model = Classifier('"
                                    + this.modelName
                                    +
                                    "', max_epoch=100, recurrent_dropout=0.50, embeddings_name='glove-840B', use_ELMo="
                                    + useELMo
                                    + localArgs
                                    +
                                    ", architecture='"
                                    + architecture
                                    + "')");
                }

                // actual training
                if (incremental) {
                    // if incremental training, we need to load the existing model
                    if (this.modelPath != null &&
                            this.modelPath.exists() &&
                            !this.modelPath.isDirectory()) {
                        jep.eval("model.load('" + this.modelPath.getAbsolutePath() + "')");
                        jep.eval("model.train(x_train, y_train, x_valid, y_valid, incremental=True)");
                    } else {
                        throw new GrobidException(
                                "the path to the model to be used for starting incremental training is invalid: "
                                        +
                                        this.modelPath.getAbsolutePath());
                    }
                } else
                    jep.eval("model.train(x_train, y_train, x_valid, y_valid)");

                // saving the model
                LOGGER.info("Saving the model at " + this.modelPath.getAbsolutePath());
                jep.eval("model.save('" + this.modelPath.getAbsolutePath() + "')");

            } catch (JepException e) {
                LOGGER.error("DeLFT classification model training via JEP failed", e);
            } catch (GrobidException e) {
                LOGGER.error("GROBID call to DeLFT training via JEP failed", e);
            } finally {
                // cleaning
                try {
                    jep.eval("del x_all");
                    jep.eval("del y_all");
                    jep.eval("del f_all");
                    jep.eval("del x_train");
                    jep.eval("del x_valid");
                    jep.eval("del y_train");
                    jep.eval("del y_valid");
                    jep.eval("del model");
                } catch (JepException e) {
                    LOGGER.error("DeLFT classification model training via JEP failed during cleanup.", e);
                }
            }
        }
    }

    /**
     * Train with an external process rather than with JNI, this approach appears to be more stable for the
     * training process (JNI approach hangs after a while) and does not raise any runtime/integration issues.
     */
    public static void train(String modelName, File trainingData, File outputModel) {
        try {
            LOGGER.info("Train DeLFT model " + modelName + "...");
            List<String> command = Arrays.asList(
                    "python3",
                    "dataseerClassifier.py",
                    modelName,
                    "train",
                    "--input",
                    trainingData.getAbsolutePath(),
                    "--output",
                    GrobidProperties.getInstance().getModelPath().getAbsolutePath());
            if (GrobidProperties.getInstance().useELMo(modelName)) {
                command.add("--use-ELMo");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            File delftPath = new File(GrobidProperties.getInstance().getDeLFTFilePath());
            pb.directory(delftPath);
            Process process = pb.start();
            //pb.inheritIO();
            CustomStreamGobbler customStreamGobbler = new CustomStreamGobbler(process.getInputStream(), System.out);
            Executors.newSingleThreadExecutor().submit(customStreamGobbler);
            SimpleStreamGobbler streamGobbler = new SimpleStreamGobbler(process.getErrorStream(), System.err::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            //assert exitCode == 0;
        } catch (IOException e) {
            LOGGER.error("IO error when training DeLFT classification model " + modelName, e);
        } catch (InterruptedException e) {
            LOGGER.error("Train DeLFT classification model " + modelName + " task failed", e);
        } catch (GrobidException e) {
            LOGGER.error("GROBID call to DeLFT training via JEP failed", e);
        }
    }

    public synchronized void close() {
        try {
            LOGGER.info("Close DeLFT classification model " + this.modelName + "...");
            JEPThreadPoolClassifier.getInstance().run(new CloseModel(this.modelName));
        } catch (InterruptedException e) {
            LOGGER.error("Close DeLFT classification model " + this.modelName + " task failed", e);
        }
    }

    private class CloseModel implements Runnable {
        private String modelName;

        public CloseModel(String modelName) {
            this.modelName = modelName;
        }

        @Override
        public void run() {
            Jep jep = JEPThreadPoolClassifier.getInstance().getJEPInstance();
            String model_variable = null;
            try {
                model_variable = this.modelName.replace("-", "_");
                // We don't close the JEP instance here because it might be reused by other models
                // The JEP instance will be closed when the application shuts down or when explicitly requested
            } catch (JepException e) {
                LOGGER.error("Closing DeLFT classification model failed", e);
            } finally {
                if (model_variable != null) {
                    try {
                        jep.eval("del " + model_variable);
                    } catch (JepException e) {
                        LOGGER.error("Deleting DeLFT classification model variable failed during cleanup.", e);
                    }
                }
            }
        }
    }

    private static class SimpleStreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public SimpleStreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

    /**
     * This is a custom gobbler that reproduces correctly the Keras training progress bar
     * by injecting a \r for progress line updates.
     */
    private static class CustomStreamGobbler implements Runnable {
        public static final Logger LOGGER = LoggerFactory.getLogger(CustomStreamGobbler.class);

        private final InputStream is;
        private final PrintStream os;
        private Pattern pattern = Pattern.compile("\\d/\\d+ \\[");

        public CustomStreamGobbler(InputStream is, PrintStream os) {
            this.is = is;
            this.os = os;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(this.is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        os.print("\r" + line);
                        os.flush();
                    } else {
                        os.println(line);
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("IO error between embedded python and java process", e);
            }
        }
    }

}
