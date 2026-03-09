package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorFunding;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.trainer.sax.TEIFundingAcknowledgementSaxParser;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.engines.FundingAcknowledgementParser;
import org.grobid.core.features.FeaturesVectorFunding;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class FundingAcknowledgementTrainer extends AbstractTrainer {

    public FundingAcknowledgementTrainer() {
        super(GrobidModels.FUNDING_ACKNOWLEDGEMENT);
    }

    /**
     * Add the selected features to annotated funding statements
     *
     * @param corpusDir
     *            a path where corpus files are located
     * @param trainingOutputPath
     *            path where to store the temporary training data
     * @return the total number of corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir, final File trainingOutputPath) {
        return createCRFPPData(corpusDir, trainingOutputPath, null, 1.0);
    }

    /**
     * Add the selected features to annotated funding statements and acknowledged entities
     *
     * @param corpusDir
     *            a path where corpus files are located
     * @param trainingOutputPath
     *            path where to store the temporary training data
     * @param evalOutputPath
     *            path where to store the temporary evaluation data
     * @param splitRatio
     *            ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return the total number of used corpus items
     */
    @Override
    public int createCRFPPData(final File corpusDir,
                            final File trainingOutputPath,
                            final File evalOutputPath,
                            double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("sourcePathLabel: " + corpusDir);
            if (trainingOutputPath != null)
                System.out.println("outputPath for training data: " + trainingOutputPath);
            if (evalOutputPath != null)
                System.out.println("outputPath for evaluation data: " + evalOutputPath);

            // we convert the tei files into the usual CRF label format
            // we process all tei files in the output directory
            final File[] refFiles = corpusDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".xml");
                }
            });

            if (refFiles == null) {
                throw new IllegalStateException("Folder " + corpusDir.getAbsolutePath()
                        + " does not seem to contain training data. Please check");
            }

            System.out.println(refFiles.length + " tei files");

            // the file for writing the training data
            OutputStream os2 = null;
            Writer writer2 = null;
            if (trainingOutputPath != null) {
                os2 = new FileOutputStream(trainingOutputPath);
                writer2 = new OutputStreamWriter(os2, "UTF8");
            }

            // the file for writing the evaluation data
            OutputStream os3 = null;
            Writer writer3 = null;
            if (evalOutputPath != null) {
                os3 = new FileOutputStream(evalOutputPath);
                writer3 = new OutputStreamWriter(os3, "UTF8");
            }

            // get a factory for SAX parser
            SAXParserFactory spf = SAXParserFactory.newInstance();

            int n = 0;
            for (; n < refFiles.length; n++) {
                final File teifile = refFiles[n];
                String name = teifile.getName();
                //System.out.println(name);

                final TEIFundingAcknowledgementSaxParser parser = new TEIFundingAcknowledgementSaxParser();

                // get a new instance of parser
                final SAXParser p = spf.newSAXParser();
                p.parse(teifile, parser);

                List<List<LayoutToken>> allTokens = parser.getTokensResult();
                List<List<String>> allLabeled = parser.getLabeledResult();

                totalExamples += parser.nbFundings;

                for(int i=0; i<allTokens.size(); i++) {
                    List<LayoutToken> tokens = allTokens.get(i);
                    List<String> labels = allLabeled.get(i);

                    // we can now add the features
                    String fundings = FeaturesVectorFunding.addFeatures(tokens, labels);
                    if ( (writer2 == null) && (writer3 != null) )
                        writer3.write(fundings + "\n \n");
                    else if ( (writer2 != null) && (writer3 == null) )
                        writer2.write(fundings + "\n \n");
                    else {
                        if (Math.random() <= splitRatio)
                            writer2.write(fundings + "\n \n");
                        else
                            writer3.write(fundings + "\n \n");
                    }
                }
            }

            if (writer2 != null) {
                writer2.close();
                os2.close();
            }

            if (writer3 != null) {
                writer3.close();
                os3.close();
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return totalExamples;
    }

    /**
     * Command line execution.
     *
     * @param args
     *            Command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        AbstractTrainer.trainAndEvaluate(FundingAcknowledgementTrainer::new);
    }

}