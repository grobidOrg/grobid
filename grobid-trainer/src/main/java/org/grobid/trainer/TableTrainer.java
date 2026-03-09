package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.TEIFigureSaxParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;
import java.util.StringTokenizer;

public class TableTrainer extends AbstractTrainer {

    public TableTrainer() {
        super(GrobidModels.TABLE);
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesTable(corpusPath.getAbsolutePath() + "/tei",
                corpusPath.getAbsolutePath() + "/raw",
                outputFile, null, 1.0);
    }

    /**
     * Add the selected features for the table model
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
        return addFeaturesTable(corpusDir.getAbsolutePath() + "/tei",
                corpusDir.getAbsolutePath() + "/raw",
                trainingOutputPath,
                evalOutputPath,
                splitRatio);
    }

    /**
     * Add the selected features for the table model
     *
     * @param sourceTEIPathLabel path to corpus TEI files
     * @param sourceRawPathLabel path to corpus raw files
     * @param trainingOutputPath path where to store the temporary training data
     * @param evalOutputPath     path where to store the temporary evaluation data
     * @param splitRatio         ratio to consider for separating training and evaluation data, e.g. 0.8 for 80%
     * @return number of examples
     */
    public int addFeaturesTable(String sourceTEIPathLabel,
                                String sourceRawPathLabel,
                                final File trainingOutputPath,
                                final File evalOutputPath,
                                double splitRatio) {
        int totalExamples = 0;
        try {
            System.out.println("sourceTEIPathLabel: " + sourceTEIPathLabel);
            System.out.println("sourceRawPathLabel: " + sourceRawPathLabel);
            System.out.println("trainingOutputPath: " + trainingOutputPath);
            System.out.println("evalOutputPath: " + evalOutputPath);

            // we need first to generate the labeled files from the TEI annotated files
            File input = new File(sourceTEIPathLabel);
            // we process all tei files in the output directory
            File[] refFiles = input.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei.xml") || name.endsWith(".tei");
                }
            });

            if (refFiles == null) {
                return 0;
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

            for (File tf : refFiles) {
                String name = tf.getName();
                System.out.println(name);

                // the full text SAX parser can be reused for the tables
                TEIFigureSaxParser parser2 = new TEIFigureSaxParser();
                //parser2.setMode(TEIFulltextSaxParser.TABLE);

                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser2);

                List<String> labeled = parser2.getLabeledResult();
                //totalExamples += parser2.n;

                // we can now add the features
                // we open the featured file
                File theRawFile = new File(sourceRawPathLabel + File.separator + name.replace(".tei.xml", ""));
                if (!theRawFile.exists()) {
                    System.out.println("Raw file " + theRawFile + " does not exist. Please have a look!");
                    continue;
                }

                int q = 0;
                BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(
                                sourceRawPathLabel + File.separator + name.replace(".tei.xml", "")), "UTF8"));

                StringBuilder table = new StringBuilder();
                String line;
                while ((line = bis.readLine()) != null) {
                    if (line.trim().length() < 2) {
                        table.append("\n");
                    }
                    int ii = line.indexOf('\t');
                    if (ii == -1) {
                        ii = line.indexOf(' ');
                    }
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii);
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been gnerated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }
//                    boolean found = false;
                    // we get the label in the labelled data file for the same token
                    for (int pp = q; pp < labeled.size(); pp++) {
                        String localLine = labeled.get(pp);
                        if (localLine.length() == 0) {
                            q = pp + 1;
                            continue;
                        }
                        StringTokenizer st = new StringTokenizer(localLine, " \t");
                        if (st.hasMoreTokens()) {
                            String localToken = st.nextToken();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);
                            if (localToken.equals(token)) {
                                String tag = st.nextToken();
                                line = line.replace("\t", " ").replace("  ", " ");
                                table.append(line).append(" ").append(tag);
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

                if ((writer2 == null) && (writer3 != null))
                    writer3.write(table.toString() + "\n");
                if ((writer2 != null) && (writer3 == null))
                    writer2.write(table.toString() + "\n");
                else {
                    if (Math.random() <= splitRatio)
                        writer2.write(table.toString() + "\n");
                    else
                        writer3.write(table.toString() + "\n");
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
            throw new GrobidException("An exception occured while running training for the table model.", e);
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
        AbstractTrainer.trainAndEvaluate(TableTrainer::new);
    }
}