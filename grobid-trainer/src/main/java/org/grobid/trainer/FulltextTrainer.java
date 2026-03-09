package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;

public class FulltextTrainer extends AbstractTrainer{

    private final GrobidModels.Flavor flavor;

    public FulltextTrainer() {
        super(GrobidModels.FULLTEXT);
        flavor = null;
    }

    public FulltextTrainer(GrobidModels.Flavor modelFlavor) {
        super(GrobidModels.getModelFlavor(GrobidModels.FULLTEXT, modelFlavor));
        flavor = modelFlavor;
    }

    @Override
    public int createCRFPPData(File corpusPath, File outputFile) {
        return addFeaturesFulltext(
            corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            outputFile,
            null,
            1.0
        );
    }

	/**
	 * Add the selected features to a full text example set 
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
        return addFeaturesFulltext(corpusDir.getAbsolutePath() + "/tei",
                corpusDir.getAbsolutePath() + "/raw",
                trainingOutputPath,
                evalOutputPath,
                splitRatio);
    }

    public int addFeaturesFulltext(String sourceTEIPathLabel,
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
                    return name.endsWith(".tei.xml");
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

                TEIFulltextSaxParser parser;
                if (flavor == GrobidModels.Flavor.ARTICLE_LIGHT || flavor == GrobidModels.Flavor.ARTICLE_LIGHT_WITH_REFERENCES) {
                    parser = new TEIFulltextArticleLightSaxParser();
                } else {
                    parser = new TEIFulltextSaxParser();
                }
            
                //get a new instance of parser
                SAXParser p = spf.newSAXParser();
                p.parse(tf, parser);

                List<String> labeled = parser.getLabeledResult();

                // removing the @newline
                /*List<String> newLabeled = new ArrayList<String>();
                for(String label : labeled) {
                    if (!label.startsWith("@newline"))
                        newLabeled.add(label);
                }
                labeled = newLabeled;*/

/*StringBuilder temp = new StringBuilder();
for(String label : labeled) {
    temp.append(label);
}
FileUtils.writeStringToFile(new File("/tmp/expected-"+name+".txt"), temp.toString());*/

                // we can now (try to) add the features
                // we open the featured file
                try {
                    File rawFile = new File(sourceRawPathLabel + File.separator + 
                                    name.replace(".tei.xml", ""));
                    if (!rawFile.exists()) {
                        LOGGER.error("The raw file does not exist: " + rawFile.getPath());
                        continue;
                    }

                    BufferedReader bis = new BufferedReader(
                            new InputStreamReader(new FileInputStream(
                            rawFile), StandardCharsets.UTF_8));
                    int q = 0; // current position in the TEI labeled list
                    StringBuilder fulltext = new StringBuilder();

                    String line;
                    int l = 0;
                    String previousTag = null;
                    int nbInvalid = 0;
                    while ((line = bis.readLine()) != null) {
                        if (line.trim().length() == 0)
                            continue;
                        // we could apply here some more check on the wellformedness of the line
                        //fulltext.append(line);
                        l++;
                        int ii = line.indexOf(' ');
                        String token = null;
                        if (ii != -1) {
                            token = line.substring(0, ii);
                            // Unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                        }
    //                    boolean found = false;
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
                                    fulltext.append(line).append(" ").append(tag);
                                    previousTag = tag;
                                    q = pp + 1;
                                    nbInvalid = 0;
                                    //pp = q + 10;
                                    break;
                                }
                            }
                            if (pp - q > 5) {
                                LOGGER.warn(name + " / Fulltext trainer: TEI and raw file unsynchronized at raw line " + l + " : " + localLine);
                                nbInvalid++;
                                // let's reuse the latest tag
                                if (previousTag != null)
                                   fulltext.append(line).append(" ").append(previousTag);
                                break;
                            }
                        }
                        if (nbInvalid > 20) {
                            // too many consecutive synchronization issues
                            break;
                        }
                    }
                    bis.close();

                    // format with features for sequence tagging...
                    if (nbInvalid < 10) {
                        if ((writer2 == null) && (writer3 != null))
                            writer3.write(fulltext.toString() + "\n");
                        if ((writer2 != null) && (writer3 == null))
                            writer2.write(fulltext.toString() + "\n");
                        else {
                            if (Math.random() <= splitRatio)
                                writer2.write(fulltext.toString() + "\n");
                            else
                                writer3.write(fulltext.toString() + "\n");
                        }
                        totalExamples++;
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
            LOGGER.error("An exception occurred while running Grobid.", e);
        }
        return totalExamples;					
	}


    public static void main(String[] args) throws Exception {
        AbstractTrainer.trainAndEvaluate(args, FulltextTrainer::new, FulltextTrainer::new);
    }
}