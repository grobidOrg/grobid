package org.grobid.core.jni;

import jep.Jep;
import jep.JepException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.GrobidModel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeLFTModel {
    public static final Logger LOGGER = LoggerFactory.getLogger(DeLFTModel.class);

    // Exploit JNI CPython interpreter to execute load and execute a DeLFT deep learning model 
    private String modelName;
    private String architecture;

    public DeLFTModel(GrobidModel model, String architecture) {
        this.modelName = model.getModelName().replace("-", "_");
        this.architecture = architecture;
        try {
            LOGGER.info("Loading DeLFT model for " + model.getModelName() + " with architecture " + architecture + "...");
            JEPThreadPool.getInstance().run(new InitModel(this.modelName, GrobidProperties.getInstance().getModelPath(), architecture));
        } catch (InterruptedException e) {
            LOGGER.error("DeLFT model " + this.modelName + " initialization was interrupted", e);
            Thread.currentThread().interrupt();
            throw new GrobidException("DeLFT model " + this.modelName + " initialization was interrupted", e);
        } catch (RuntimeException e) {
            LOGGER.error("DeLFT model " + this.modelName + " initialization failed", e);
            throw e;
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
            Jep jep = JEPThreadPool.getInstance().getJEPInstance();
            try {
                String fullModelName = this.modelName.replace("_", "-");

                //if (architecture != null && !architecture.equals("BidLSTM_CRF"))
                if (architecture != null)
                    fullModelName += "-" + this.architecture;

                if (GrobidProperties.getInstance().useELMo(this.modelName) && modelName.toLowerCase().indexOf("bert") == -1)
                    fullModelName += "-with_ELMo";

                jep.eval(this.modelName + " = Sequence('" + fullModelName + "')");
                jep.eval(this.modelName + ".load(dir_path='" + modelPath.getAbsolutePath() + "')");

                if (GrobidProperties.getInstance().getDelftRuntimeMaxSequenceLength(this.modelName) != -1) {
                    jep.eval(this.modelName + ".model_config.max_sequence_length=" +
                        GrobidProperties.getInstance().getDelftRuntimeMaxSequenceLength(this.modelName));
                }

                if (GrobidProperties.getInstance().getDelftRuntimeBatchSize(this.modelName) != -1) {
                    jep.eval(this.modelName + ".model_config.batch_size=" +
                        GrobidProperties.getInstance().getDelftRuntimeBatchSize(this.modelName));
                }

            } catch (JepException e) {
                LOGGER.error("DeLFT model initialization failed. ", e);
                throw new GrobidException("DeLFT model initialization failed. ", e);
            }
        }
    }

    private class LabelTask implements Callable<String> {
        private String data;
        private String modelName;
        private String architecture;

        public LabelTask(String modelName, String data, String architecture) {
            //System.out.println("label thread: " + Thread.currentThread().getId());
            this.modelName = modelName;
            this.data = data;
            this.architecture = architecture;
        }

        private void setJepStringValueWithFileFallback(
            Jep jep, String name, String value
        ) throws JepException, IOException {
            try {
                jep.set(name, value);
            } catch (JepException e) {
                File tempFile = IOUtilities.newTempFile(name, ".data");
                LOGGER.debug(
                    "Falling back to file {} due to exception: {}",
                    tempFile, e.toString()
                );
                IOUtilities.writeInFile(tempFile.getAbsolutePath(), value);
                jep.eval("from pathlib import Path");
                jep.eval(
                    name + " = Path('" + tempFile.getAbsolutePath() +
                        "').read_text(encoding='utf-8')"
                );
                tempFile.delete();
            }
        }

        @Override
        public String call() {
            Jep jep = JEPThreadPool.getInstance().getJEPInstance();
            StringBuilder labelledData = new StringBuilder();
            try {
                // load and tag
                this.setJepStringValueWithFileFallback(jep, "input", this.data);
                jep.eval("x_all, f_all = load_data_crf_string(input)");
                Object objectResults = null;
                if (architecture.contains("FEATURE")) {
                    // model is expecting features
                    objectResults = jep.getValue(this.modelName + ".tag(x_all, None, features=f_all)");
                } else {
                    // no features used by the model
                    objectResults = jep.getValue(this.modelName + ".tag(x_all, None)");
                }

                // inject back the labels
                List<List<List<String>>> results = (List<List<List<String>>>) objectResults;
                BufferedReader bufReader = new BufferedReader(new StringReader(data));
                String inputLine;
                int i = 0; // sentence index
                int j = 0; // word index in the sentence
                if (CollectionUtils.isNotEmpty(results)) {
                    List<List<String>> result = results.get(0);
                    while ((inputLine = bufReader.readLine()) != null) {
                        inputLine = inputLine.trim();
                        if (StringUtils.isBlank(inputLine) && j != 0) {
                            j = 0;
                            i++;
                            if (i == results.size())
                                break;
                            result = results.get(i);
                            continue;
                        }

                        if (inputLine.length() == 0) {
                            labelledData.append("\n");
                            continue;
                        }
                        labelledData.append(inputLine);
                        labelledData.append(" ");

                        if (j >= result.size()) {
                            labelledData.append(TaggingLabels.OTHER_LABEL);
                        } else {
                            List<String> pair = result.get(j);
                            // first is the token, second is the label (DeLFT format)
                            String token = pair.get(0);
                            String label = pair.get(1);
                            labelledData.append(DeLFTModel.delft2grobidLabel(label));
                        }
                        labelledData.append("\n");
                        j++;
                    }
                }
                //jep.eval("K.clear_session()");
            } catch (JepException e) {
                LOGGER.error("DeLFT model labelling via JEP failed", e);
            } catch (IOException e) {
                LOGGER.error("DeLFT model labelling failed", e);
            } finally {
                // No need to close the JEP instance here as it's managed by the pool
                // and will be reused for other tasks
                try {
                    jep.eval("del input");
                    jep.eval("del x_all");
                    jep.eval("del f_all");
                } catch (JepException e) {
                    LOGGER.error("DeLFT model labelling via JEP failed during cleanup. ", e);
                }

            }
            //System.out.println(labelledData.toString());
            return labelledData.toString();
        }
    }

    public String label(String data) {
        String result = null;
        try {
            result = JEPThreadPool.getInstance().call(new LabelTask(this.modelName, data, this.architecture));
        } catch (InterruptedException e) {
            LOGGER.error("DeLFT model " + this.modelName + " labelling interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("DeLFT model " + this.modelName + " labelling failed", e);
        }
        // In some areas, GROBID currently expects tabs as feature separators.
        // (Same as in WapitiModel.label)
        if (result != null)
            result = result.replaceAll(" ", "\t");
        return result;
    }

    /**
     * Training via JNI CPython interpreter (JEP). It appears that after some epochs, the JEP thread
     * usually hangs... Possibly issues with IO threads at the level of JEP (output not consumed because
     * of \r and no end of line?).
     */
    public static void trainJNI(String modelName, File trainingData, File outputModel, String architecture, boolean incremental) {
        try {
            LOGGER.info("Train DeLFT model " + modelName + "...");
            JEPThreadPool.getInstance().run(
                new TrainTask(modelName, trainingData, GrobidProperties.getInstance().getModelPath(), architecture, incremental));
        } catch (InterruptedException e) {
            LOGGER.error("Train DeLFT model " + modelName + " task failed", e);
        }
    }

    private static class TrainTask implements Runnable {
        private String modelName;
        private File trainPath;
        private File modelPath;
        private String architecture;
        private boolean incremental;

        public TrainTask(String modelName, File trainPath, File modelPath, String architecture, boolean incremental) {
            //System.out.println("train thread: " + Thread.currentThread().getId());
            this.modelName = modelName;
            this.trainPath = trainPath;
            this.modelPath = modelPath;
            this.architecture = architecture;
            this.incremental = incremental;
        }

        @Override
        public void run() {
            Jep jep = JEPThreadPool.getInstance().getJEPInstance();
            try {
                // load data
                jep.eval("x_all, y_all, f_all = load_data_and_labels_crf_file('" + this.trainPath.getAbsolutePath() + "')");
                jep.eval("x_train, x_valid, y_train, y_valid = train_test_split(x_all, y_all, test_size=0.1)");
                jep.eval("print(len(x_train), 'train sequences')");
                jep.eval("print(len(x_valid), 'validation sequences')");

                String useELMo = "False";
                if (GrobidProperties.getInstance().useELMo(this.modelName) && modelName.toLowerCase().indexOf("bert") == -1) {
                    useELMo = "True";
                }

                String localArgs = "";
                if (GrobidProperties.getInstance().getDelftTrainingMaxSequenceLength(this.modelName) != -1)
                    localArgs += ", max_sequence_length=" +
                        GrobidProperties.getInstance().getDelftTrainingMaxSequenceLength(this.modelName);

                if (GrobidProperties.getInstance().getDelftTrainingBatchSize(this.modelName) != -1)
                    localArgs += ", batch_size=" +
                        GrobidProperties.getInstance().getDelftTrainingBatchSize(this.modelName);

                if (GrobidProperties.getInstance().getDelftTranformer(modelName) != null) {
                    localArgs += ", transformer=" +
                        GrobidProperties.getInstance().getDelftTranformer(modelName);
                }

                // init model to be trained
                if (architecture == null)
                    jep.eval("model = Sequence('" + this.modelName +
                        "', max_epoch=100, recurrent_dropout=0.50, embeddings_name='glove-840B', use_ELMo=" + useELMo + localArgs + ")");
                else
                    jep.eval("model = Sequence('" + this.modelName +
                        "', max_epoch=100, recurrent_dropout=0.50, embeddings_name='glove-840B', use_ELMo=" + useELMo + localArgs +
                        ", architecture='" + architecture + "')");

                // actual training
                //start_time = time.time()
                if (incremental) {
                    // if incremental training, we need to load the existing model
                    if (this.modelPath != null &&
                        this.modelPath.exists() &&
                        this.modelPath.isDirectory()) {
                        jep.eval("model.load('" + this.modelPath.getAbsolutePath() + "')");
                        jep.eval("model.train(x_train, y_train, x_valid, y_valid, incremental=True)");
                    } else {
                        throw new GrobidException("the path to the model to be used for starting incremental training is invalid: " +
                            this.modelPath.getAbsolutePath());
                    }
                } else
                    jep.eval("model.train(x_train, y_train, x_valid, y_valid)");
                //runtime = round(time.time() - start_time, 3)
                //print("training runtime: %s seconds " % (runtime))

                // saving the model
                LOGGER.info("Saving the model at " + this.modelPath.getAbsolutePath());
                jep.eval("model.save('" + this.modelPath.getAbsolutePath() + "')");

            } catch (JepException e) {
                LOGGER.error("DeLFT model training via JEP failed", e);
            } catch (GrobidException e) {
                LOGGER.error("GROBID call to DeLFT training via JEP failed", e);
            } finally {
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
                    LOGGER.error("DeLFT model labelling via JEP failed during cleanup. ", e);
                }
            }
        }
    }

    /**
     * Train with an external process rather than with JNI, this approach appears to be more stable for the
     * training process (JNI approach hangs after a while) and does not raise any runtime/integration issues.
     */
    public static void train(String modelName, File trainingData, File outputModel, String architecture, boolean incremental) {
        try {
            LOGGER.info("Train DeLFT model " + modelName + "...");
            List<String> command = new ArrayList<>();
            List<String> subcommands = Arrays.asList("python3",
                "delft/applications/grobidTagger.py",
                modelName,
                "train",
                "--input", trainingData.getAbsolutePath(),
                "--output", GrobidProperties.getInstance().getModelPath().getAbsolutePath());
            command.addAll(subcommands);
            if (architecture != null) {
                command.add("--architecture");
                command.add(architecture);
            }
            if (GrobidProperties.getInstance().getDelftTranformer(modelName) != null) {
                command.add("--transformer");
                command.add(GrobidProperties.getInstance().getDelftTranformer(modelName));
            }
            if (GrobidProperties.getInstance().useELMo(modelName) && modelName.toLowerCase().indexOf("bert") == -1) {
                command.add("--use-ELMo");
            }
            if (GrobidProperties.getInstance().getDelftTrainingMaxSequenceLength(modelName) != -1) {
                command.add("--max-sequence-length");
                command.add(String.valueOf(GrobidProperties.getInstance().getDelftTrainingMaxSequenceLength(modelName)));
            }
            if (GrobidProperties.getInstance().getDelftTrainingBatchSize(modelName) != -1) {
                command.add("--batch-size");
                command.add(String.valueOf(GrobidProperties.getInstance().getDelftTrainingBatchSize(modelName)));
            }
            if (incremental) {
                command.add("--incremental");

                // if incremental training, we need to load the existing model
                File modelPath = GrobidProperties.getInstance().getModelPath();
                if (modelPath != null &&
                    modelPath.exists() &&
                    modelPath.isDirectory()) {
                    command.add("--input-model");
                    command.add(GrobidProperties.getInstance().getModelPath().getAbsolutePath());
                } else {
                    throw new GrobidException("the path to the model to be used for starting incremental training is invalid: " +
                        GrobidProperties.getInstance().getModelPath().getAbsolutePath());
                }
            }
            ProcessBuilder pb = new ProcessBuilder(command);
            File delftPath = new File(GrobidProperties.getInstance().getDeLFTFilePath());
            pb.directory(delftPath);
            Process process = pb.start();
            //pb.inheritIO();
            CustomStreamGobbler customStreamGobbler =
                new CustomStreamGobbler(process.getInputStream(), System.out);
            Executors.newSingleThreadExecutor().submit(customStreamGobbler);
            SimpleStreamGobbler streamGobbler = new SimpleStreamGobbler(process.getErrorStream(), System.err::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            //assert exitCode == 0;
        } catch (IOException e) {
            LOGGER.error("IO error when training DeLFT model " + modelName, e);
        } catch (InterruptedException e) {
            LOGGER.error("Train DeLFT model " + modelName + " task failed", e);
        } catch (GrobidException e) {
            LOGGER.error("GROBID call to DeLFT training via JEP failed", e);
        }
    }

    public synchronized void close() {
        try {
            LOGGER.info("Close DeLFT model " + this.modelName + "...");
            JEPThreadPool.getInstance().run(new CloseModel(this.modelName));
        } catch (InterruptedException e) {
            LOGGER.error("Close DeLFT model " + this.modelName + " task failed", e);
        }
    }

    private class CloseModel implements Runnable {
        private String modelName;

        public CloseModel(String modelName) {
            this.modelName = modelName;
        }

        @Override
        public void run() {
            Jep jep = JEPThreadPool.getInstance().getJEPInstance();
            try {
                jep.eval("del " + this.modelName);
                // We don't close the JEP instance here because it might be reused by other models
                // The JEP instance will be closed when the application shuts down or when explicitly requested
            } catch (JepException e) {
                LOGGER.error("Closing DeLFT model failed", e);
            }
        }
    }

    private static String delft2grobidLabel(String label) {
        if (label.equals(TaggingLabels.IOB_OTHER_LABEL)) {
            label = TaggingLabels.OTHER_LABEL;
        } else if (label.startsWith(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX)) {
            label = label.replace(TaggingLabels.IOB_START_ENTITY_LABEL_PREFIX, TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX);
        } else if (label.startsWith(TaggingLabels.IOB_INSIDE_LABEL_PREFIX)) {
            label = label.replace(TaggingLabels.IOB_INSIDE_LABEL_PREFIX, TaggingLabels.GROBID_INSIDE_ENTITY_LABEL_PREFIX);
        }
        return label;
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
