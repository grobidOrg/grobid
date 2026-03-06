package org.grobid.trainer;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.GrobidModels.Flavor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Training application for training a target model.
 *
 */
public class TrainerRunner {

    private static final List<String> models = Arrays.asList(
        "affiliation",
        "chemical",
        "date",
        "citation",
        "ebook",
        "fulltext",
        "header",
        "header-light",
        "header-light-ref",
        "header-ietf",
        "name-citation",
        "name-header",
        "patent",
        "segmentation",
        "segmentation-light",
        "segmentation-light-ref",
        "segmentation-ietf"
    );
    private static final List<String> options = Arrays.asList("0 - train", "1 - evaluate", "2 - split, train and evaluate", "3 - n-fold evaluation");

    private enum RunType {
        TRAIN, EVAL, SPLIT, EVAL_N_FOLD;

        public static RunType getRunType(int i) {
            for (RunType t : values()) {
                if (t.ordinal() == i) {
                    return t;
                }
            }

            throw new IllegalStateException("Unsupported RunType with ordinal " + i);
        }
    }

    protected static void initProcess(final String path2GbdHome, final String path2GbdProperties) {
        GrobidProperties.getInstance();
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            throw new IllegalStateException(
                "Usage: {" + String.join(", ", options) + "} {" + String.join(", ", models) + "} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -n {[int, num folds for n-fold evaluation, optional]} -epsilon {double, Wapiti epsilon, optional} -w {int, Wapiti window, optional} -maxIter {int, Wapiti max iterations, optional} -modelPath {path, custom output model file, optional}");
        }

        RunType mode = RunType.getRunType(Integer.parseInt(args[0]));
        if ((mode == RunType.SPLIT || mode == RunType.EVAL_N_FOLD) && (args.length < 6)) {
            throw new IllegalStateException(
                "Usage: {" + String.join(", ", options) + "} {" + String.join(", ", models) + "} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -n {[int, num folds for n-fold evaluation, optional]} -epsilon {double, Wapiti epsilon, optional} -w {int, Wapiti window, optional} -maxIter {int, Wapiti max iterations, optional} -modelPath {path, custom output model file, optional}");
        }

        String path2GbdHome = null;
        double split = 0.0;
        int numFolds = 0;
        String outputFilePath = null;
        boolean incremental = false;
        double epsilon = 0.0;
        int window = 0;
        int nbMaxIterations = 0;
        String outputModelFilePath = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-gH")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing path to Grobid home. ");
                }
                path2GbdHome = args[i + 1];
            } else if (args[i].equals("-s")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing split ratio value. ");
                }
                try {
                    split = Double.parseDouble(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid split value: " + args[i + 1]);
                }

            } else if (args[i].equals("-n")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing number of folds value. ");
                }
                try {
                    numFolds = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid number of folds value: " + args[i + 1]);
                }

            } else if (args[i].equals("-o")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing output file. ");
                }
                outputFilePath = args[i + 1];

            } else if (args[i].equals("-i")) {
                incremental = true;

            } else if (args[i].equals("-epsilon")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing epsilon value. ");
                }
                try {
                    epsilon = Double.parseDouble(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid epsilon value: " + args[i + 1]);
                }

            } else if (args[i].equals("-w")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing window value. ");
                }
                try {
                    window = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid window value: " + args[i + 1]);
                }

            } else if (args[i].equals("-maxIter")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing nbMaxIterations value. ");
                }
                try {
                    nbMaxIterations = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid nbMaxIterations value: " + args[i + 1]);
                }

            } else if (args[i].equals("-modelPath")) {
                if (i + 1 == args.length) {
                    throw new IllegalStateException("Missing model output path value. ");
                }
                outputModelFilePath = args[i + 1];

            }
        }

        if (path2GbdHome == null) {
            throw new IllegalStateException(
                "Grobid-home path not found.\n Usage: {" + String.join(", ", options) + "} {" + String.join(", ", models) + "} -gH /path/to/Grobid/home -s { [0.0 - 1.0] - split ratio, optional} -n {[int, num folds for n-fold evaluation, optional]} -epsilon {double, Wapiti epsilon, optional} -w {int, Wapiti window, optional} -maxIter {int, Wapiti max iterations, optional} -modelPath {path, custom output model file, optional}");
        }

        final String path2GbdProperties = path2GbdHome + File.separator + "config" + File.separator + "grobid.properties";

        System.out.println("path2GbdHome=" + path2GbdHome + "   path2GbdProperties=" + path2GbdProperties);
        initProcess(path2GbdHome, path2GbdProperties);

        String model = args[1];

        AbstractTrainer trainer;

        if (model.equals("affiliation") || model.equals("affiliation-address")) {
            trainer = new AffiliationAddressTrainer();
        } else if (model.equals("chemical")) {
            trainer = new ChemicalEntityTrainer();
        } else if (model.equals("date")) {
            trainer = new DateTrainer();
        } else if (model.equals("citation")) {
            trainer = new CitationTrainer();
        } else if (model.equals("monograph")) {
            trainer = new MonographTrainer();
        } else if (model.equals("fulltext")) {
            trainer = new FulltextTrainer();
        } else if (model.equals("header")) {
            trainer = new HeaderTrainer();
        } else if (model.equals("header-ietf")) {
            trainer = new HeaderTrainer(Flavor.IETF);
        } else if (model.equals("header-light")) {
            trainer = new HeaderTrainer(Flavor.ARTICLE_LIGHT);
        } else if (model.equals("header-light-ref")) {
            trainer = new HeaderTrainer(Flavor.ARTICLE_LIGHT_WITH_REFERENCES);
        } else if (model.equals("name-citation")) {
            trainer = new NameCitationTrainer();
        } else if (model.equals("name-header")) {
            trainer = new NameHeaderTrainer();
        } else if (model.equals("patent-citation")) {
            trainer = new PatentParserTrainer();
        } else if (model.equals("segmentation")) {
            trainer = new SegmentationTrainer();
        } else if (model.equals("segmentation-light")) {
            trainer = new SegmentationTrainer(Flavor.ARTICLE_LIGHT);
        } else if (model.equals("segmentation-light-ref")) {
            trainer = new SegmentationTrainer(Flavor.ARTICLE_LIGHT_WITH_REFERENCES);
        } else if (model.equals("segmentation-ietf")) {
            trainer = new SegmentationTrainer(Flavor.IETF);
        } else if (model.equals("reference-segmenter")) {
            trainer = new ReferenceSegmenterTrainer();
        } else if (model.equals("figure")) {
            trainer = new FigureTrainer();
        } else if (model.equals("table")) {
            trainer = new TableTrainer();
        } else if (model.equals("funding-acknowledgement")) {
            trainer = new FundingAcknowledgementTrainer();
        } else {
            throw new IllegalStateException("The model " + model + " is unknown.");
        }

        if (epsilon != 0.0 || window != 0 || nbMaxIterations != 0) {
            trainer.setParams(epsilon, window, nbMaxIterations);
        }
        if (outputModelFilePath != null) {
            trainer.setOutputModelPath(new File(outputModelFilePath));
        }

        switch (mode) {
            case TRAIN:
                AbstractTrainer.runTraining(trainer, incremental);
                break;
            case EVAL:
                System.out.println(AbstractTrainer.runEvaluation(trainer));
                break;
            case SPLIT:
                System.out.println(AbstractTrainer.runSplitTrainingEvaluation(trainer, split, incremental));
                break;
            case EVAL_N_FOLD:
                if(numFolds == 0) {
                    throw new IllegalArgumentException("N should be > 0");
                }
                if (StringUtils.isNotEmpty(outputFilePath)) {
                    Path outputPath = Paths.get(outputFilePath);
                    if (Files.exists(outputPath)) {
                        System.err.println("Output file exists. ");
                    }
                } else {
                    String results = AbstractTrainer.runNFoldEvaluation(trainer, numFolds, incremental);
                    System.out.println(results);
                }
                break;
            default:
                throw new IllegalStateException("Invalid RunType: " + mode.name());
        }
        System.exit(0);
    }

}