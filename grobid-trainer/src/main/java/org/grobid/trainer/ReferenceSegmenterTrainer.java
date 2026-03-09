package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorReferenceSegmenter;
import org.grobid.trainer.sax.TEIReferenceSegmenterSaxParser;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;
import java.util.StringTokenizer;

public class ReferenceSegmenterTrainer extends AbstractTrainer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ReferenceSegmenterTrainer.class);

    public ReferenceSegmenterTrainer() {
        super(GrobidModels.REFERENCE_SEGMENTER);
    }

    @Override
    public int createCRFPPData(File corpusPath, File trainingOutputPath) {
        return createCRFPPData(corpusPath, trainingOutputPath, null, 1.0);
    }

    @Override
    public int createCRFPPData(File corpusDir, File trainingOutputPath, File evaluationOutputPath, double splitRatio) {
        int totalExamples = 0;
        try {
            LOGGER.info("Corpus directory: " + corpusDir);
            if (trainingOutputPath != null) {
                LOGGER.info("output path for training data: " + trainingOutputPath);
            }
            if (evaluationOutputPath != null) {
                LOGGER.info("output path for evaluation data: " + evaluationOutputPath);
            }

			File teiCorpusDir = new File(corpusDir.getAbsolutePath() + "/tei/");
			if (!teiCorpusDir.exists()) {
                throw new IllegalStateException("Folder " + corpusDir.getAbsolutePath() +
                        " does not exist. Please have a look!");
			}
			
            // we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            final File[] refFiles = teiCorpusDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".xml") || name.endsWith(".tei"));
                }
            });

            if (refFiles == null) {
                throw new IllegalStateException("Folder " + teiCorpusDir.getAbsolutePath() +
                        " does not seem to contain training data. Please check");
            }

            LOGGER.info("Processing " + refFiles.length + " tei files");

            // the file for writing the training data
            OutputStream trainingOS = null;
            Writer trainingWriter = null;
            if (trainingOutputPath != null) {
                trainingOS = new FileOutputStream(trainingOutputPath);
                trainingWriter = new OutputStreamWriter(trainingOS, "UTF8");
            }

            // the file for writing the evaluation data
            OutputStream evaluationOS = null;
            Writer evaluationWriter = null;
            if (evaluationOutputPath != null) {
                evaluationOS = new FileOutputStream(evaluationOutputPath);
                evaluationWriter = new OutputStreamWriter(evaluationOS, "UTF8");
            }

			System.out.println("training data under: " + trainingOutputPath);
			System.out.println("evaluation data under: " + evaluationOutputPath);

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();
//            List<List<OffsetPosition>> placesPositions;

            int n = 0;
            for (; n < refFiles.length; n++) {
                final File teifile = refFiles[n];
                final TEIReferenceSegmenterSaxParser saxParser = new TEIReferenceSegmenterSaxParser();

				String name = teifile.getName();

                // get a new instance of parser
                final SAXParser p = spf.newSAXParser();
                p.parse(teifile, saxParser);

                final List<String> labeled = saxParser.getLabeledResult();
                totalExamples += saxParser.getTotalReferences();

                // we can now add the features
                // we open the featured file
				File rawCorpusDir = new File(corpusDir.getAbsolutePath() + "/raw/");
				if (!rawCorpusDir.exists()) {
	                throw new IllegalStateException("Folder " + rawCorpusDir.getAbsolutePath() +
	                        " does not exist. Please have a look!");
				}
				
				File theRawFile = new File(rawCorpusDir.getAbsolutePath() + File.separator + 
					name.replace(".tei.xml", ""));
				if (!theRawFile.exists()) {
	                System.out.println("Raw file " + theRawFile +
	                        " does not exist. Please have a look!");
					continue;
				}
				
                int q = 0;
                BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                rawCorpusDir.getAbsolutePath() + File.separator + 
									name.replace(".tei.xml", "")), "UTF8"));

                StringBuilder referenceText = new StringBuilder();

                String line;
                while ((line = bis.readLine()) != null) {
                    int ii = line.indexOf(' ');
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii);
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been gnerated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }
                    // we get the label in the labelled data file for the same token
                    for (int pp = q; pp < labeled.size(); pp++) {
                        String localLine = labeled.get(pp);
                        StringTokenizer st = new StringTokenizer(localLine, " ");
                        if (st.hasMoreTokens()) {
                            String localToken = st.nextToken();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);

                            if (localToken.equals(token)) {
                                String tag = st.nextToken();
                                referenceText.append(line).append(" ").append(tag).append("\n");
//                                lastTag = tag;
//                                found = true;
                                q = pp + 1;
                                pp = q + 10;
                            }
                        }
                        if (pp - q > 5) {
                            break;
                        }
                    }
                }
                bis.close();

                // format with features for sequence tagging...
                //writer2.write(referenceText.toString() + "\n");

                // we can now add the features
                //String featureVector = FeaturesVectorReferenceSegmenter.addFeaturesReferenceSegmenter(labeled);

                // format with features for sequence tagging...
                // given the split ratio we write either in the training file or the evaluation file
                //affAdd = affAdd.replace("\n \n", "\n \n");

                //String[] chunks = featureVector.split("\n\n");

                //for (String chunk : chunks) 
				{
                    if ((trainingWriter == null) && (evaluationWriter != null))
                        evaluationWriter.write(referenceText.toString() + "\n \n");
                    if ((trainingWriter != null) && (evaluationWriter == null))
                        trainingWriter.write(referenceText.toString() + "\n \n");
                    else {
                        if (Math.random() <= splitRatio && trainingWriter != null) {
                            trainingWriter.write(referenceText.toString() + "\n \n");
                        } else if (evaluationWriter != null) {
                            evaluationWriter.write(referenceText.toString() + "\n \n");
                        }
                    }
                }
            }

            if (trainingWriter != null) {
                trainingWriter.close();
                trainingOS.close();
            }

            if (evaluationWriter != null) {
                evaluationWriter.close();
                evaluationOS.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while trainining/evaluating reference segmenter model.", e);
        }
        return totalExamples;
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        AbstractTrainer.trainAndEvaluate(ReferenceSegmenterTrainer::new);
    }
}
