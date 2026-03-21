package org.grobid.trainer;

import net.sf.saxon.lib.SaxonOutputKeys;
import org.grobid.core.GrobidModels;
import org.grobid.core.GrobidModels.Flavor;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEISegmentationArticleLightRefSaxParser;
import org.grobid.trainer.sax.TEISegmentationArticleLightSaxParser;
import org.grobid.trainer.sax.TEISegmentationSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class SegmentationTrainer extends AbstractTrainer {

    private final GrobidModels.Flavor flavor;

    public SegmentationTrainer() {
        super(GrobidModels.SEGMENTATION);
        flavor = null;
    }

    public SegmentationTrainer(GrobidModels.Flavor modelFlavor) {
        super(GrobidModels.getModelFlavor(GrobidModels.SEGMENTATION, modelFlavor));
        flavor = modelFlavor;
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesSegmentation(corpusPath.getAbsolutePath() + "/tei",
                corpusPath.getAbsolutePath() + "/raw",
                outputFile, null, 1.0);
    }

    /**
     * Add the selected features for the segmentation model
     *
     * @param corpusDir          path where corpus files are located
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                               final File trainingOutputPath,
                               final File evalOutputPath,
                               double splitRatio) {
        return addFeaturesSegmentation(corpusDir.getAbsolutePath() + "/tei",
                corpusDir.getAbsolutePath() + "/raw",
                trainingOutputPath,
                evalOutputPath,
                splitRatio);
    }

    /**
     * Add the selected features for the segmentation model
     *
     * @param sourceTEIPathLabel path to corpus TEI files
     * @param sourceRawPathLabel path to corpus raw files
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return number of examples
     */
    public int addFeaturesSegmentation(String sourceTEIPathLabel,
                                       String sourceRawPathLabel,
                                       final File trainingOutputPath,
                                       final File evalOutputPath,
                                       double splitRatio) {
        int totalExamples = 0;
        try {
            LOGGER.info("sourceTEIPathLabel: " + sourceTEIPathLabel);
            LOGGER.info("sourceRawPathLabel: " + sourceRawPathLabel);
            LOGGER.info("trainingOutputPath: " + trainingOutputPath);
            LOGGER.info("evalOutputPath: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles((dir, name) ->
                    name.endsWith(".tei.xml") || name.endsWith(".tei"));

            if (refFiles == null) {
                return 0;
            }

            LOGGER.info(refFiles.length + " tei files");

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, StandardCharsets.UTF_8);
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, StandardCharsets.UTF_8);
            }

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            for (File tf : refFiles) {
                String name = tf.getName();
                LOGGER.info("Processing: " + name);

                TEISegmentationSaxParser parser;
                if (flavor == Flavor.ARTICLE_LIGHT) {
                    parser = new TEISegmentationArticleLightSaxParser();
                } else if (flavor == Flavor.ARTICLE_LIGHT_WITH_REFERENCES) {
                    parser = new TEISegmentationArticleLightRefSaxParser();
                } else {
                    parser = new TEISegmentationSaxParser();
                }

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser);

                List<String> labeled = parser.getLabeledResult();

                // For dh-law-footnotes flavor, remap low-support labels
                if (flavor == Flavor.ARTICLE_DH_LAW_FOOTNOTES) {
                    labeled = remapLowSupportLabels(labeled);
                }

                // we can now add the features
                // we open the featured file
                try {
                    File theRawFile = new File(sourceRawPathLabel + File.separator + name.replace(".tei.xml", ""));
                    if (!theRawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + theRawFile.getPath());
                        continue;
                    }

                    int q = 0;
                    StringBuilder segmentation = new StringBuilder();
                    String previousTag = null;
                    int nbInvalid = 0;
                    int lineNumber = 0;
                    try (BufferedReader bis = new BufferedReader(
                            new InputStreamReader(new FileInputStream(theRawFile), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = bis.readLine()) != null) {
                            lineNumber++;
                            int ii = line.indexOf(' ');
                            String token = null;
                            if (ii != -1) {
                                token = line.substring(0, ii);
                                // unicode normalisation of the token - it should not be necessary if the training data
                                // has been generated by a recent version of grobid
                                token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                            }
                            // we get the label in the labelled data file for the same token
                            for (int pp = q; pp < labeled.size(); pp++) {
                                String localLine = labeled.get(pp);
                                StringTokenizer st = new StringTokenizer(localLine, " \t");
                                if (st.hasMoreTokens()) {
                                    String localToken = st.nextToken();
                                    localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                                    if (localToken.equals(token)) {
                                        String tag = st.nextToken();
                                        segmentation.append(line).append(" ").append(tag);
                                        previousTag = tag;
                                        q = pp + 1;
                                        nbInvalid = 0;
                                        break;
                                    }
                                }
                                if (pp - q > 5) {
                                    nbInvalid++;
                                    LOGGER.debug("{} / TEI and raw file unsynchronized at raw line {}, token '{}' not found in TEI", name, lineNumber, token);
                                    // let's reuse the latest tag
                                    if (previousTag != null)
                                        segmentation.append(line).append(" ").append(previousTag);
                                    break;
                                }
                            }
                            if (nbInvalid > 20) {
                                // too many consecutive synchronization issues
                                break;
                            }
                        }
                    }

                    if (nbInvalid < 10) {
                        String output = segmentation + "\n";
                        if ((writer2 == null) && (writer3 != null)) {
                            writer3.write(output);
                        } else if ((writer2 != null) && (writer3 == null)) {
                            writer2.write(output);
                        } else if (writer2 != null && writer3 != null) {
                            if (Math.random() <= splitRatio)
                                writer2.write(output);
                            else
                                writer3.write(output);
                        }
                        totalExamples++;
                        if (nbInvalid > 0) {
                            LOGGER.warn(name + " / found "+nbInvalid+" synchronization mismatches, however the file is anyway accepted!");
                        }
                    } else {
                        LOGGER.error("{} / too many synchronization issues, file not used in training data and to be fixed!", name);
                    }
                } catch (Exception e) {
                   LOGGER.error("Fail to open or process raw file", e);
                }
            }

            if (writer2 != null) {
                writer2.close();
                if (os2 != null) {
                    os2.close();
                }
            }

            if (writer3 != null) {
                writer3.close();
                if (os3 != null) {
                    os3.close();
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return totalExamples;
    }


    /**
     * Remap low-support segmentation labels to parent labels for the dh-law-footnotes flavor.
     * <acknowledgement>/<annex>/<funding>/<conflict> → <body>
     */
    private static List<String> remapLowSupportLabels(List<String> labeled) {
        List<String> remapped = new ArrayList<>(labeled.size());
        for (String line : labeled) {
            line = line.replace("I-<acknowledgement>", "I-<body>")
                       .replace("<acknowledgement>", "<body>")
                       .replace("I-<annex>", "I-<body>")
                       .replace("<annex>", "<body>")
                       .replace("I-<funding>", "I-<body>")
                       .replace("<funding>", "<body>")
                       .replace("I-<conflict>", "I-<body>")
                       .replace("<conflict>", "<body>")
                       .replace("I-<toc>", "I-<body>")
                       .replace("<toc>", "<body>")
                       .replace("I-<titlePage>", "I-<header>")
                       .replace("<titlePage>", "<header>")
                       .replace("I-<cover>", "I-<header>")
                       .replace("<cover>", "<header>");
            remapped.add(line);
        }
        return remapped;
    }

    public static void main(String[] args) throws Exception {
        // if we have a parameter, it gives the flavor refinement to consider
        Flavor theFlavor = null;
        if (args.length > 0) {
            String flavor = args[0];
            theFlavor = Flavor.fromLabel(flavor);
            if (theFlavor == null) {
                System.out.println("Warning, the flavor is not recognized, " +
                    "must one one of "+ Flavor.getLabels() +", " +
                    "defaulting training with no flavor...");
            }
        }

        GrobidProperties.getInstance();
        if (theFlavor == null) {
            AbstractTrainer.runTraining(new SegmentationTrainer());
            System.out.println(AbstractTrainer.runEvaluation(new SegmentationTrainer()));
        } else {
            AbstractTrainer.runTraining(new SegmentationTrainer(theFlavor));
            System.out.println(AbstractTrainer.runEvaluation(new SegmentationTrainer(theFlavor)));
        }
        System.exit(0);
    }
}