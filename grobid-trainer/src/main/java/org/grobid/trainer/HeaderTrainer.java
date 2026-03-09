package org.grobid.trainer;

import org.grobid.core.GrobidModels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.trainer.sax.*;
import org.grobid.core.GrobidModels.Flavor;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.List;
import java.util.StringTokenizer;

public class HeaderTrainer extends AbstractTrainer{

    private final GrobidModels.Flavor flavor;

    public HeaderTrainer() {
        super(GrobidModels.HEADER);
        this.flavor = null;
    }

    public HeaderTrainer(Flavor flavor) {
        super(GrobidModels.getModelFlavor(GrobidModels.HEADER, flavor));
        this.flavor = flavor;
    }

    @Override
    public int createCRFPPData(File corpusPath, File trainingOutputPath) {
        return addFeaturesHeaders(
            corpusPath.getAbsolutePath() + "/tei",
            corpusPath.getAbsolutePath() + "/raw",
            trainingOutputPath,
            null,
            1.0
        );
    }

	/**
	 * Add the selected features to a header example set 
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
		return addFeaturesHeaders(corpusDir.getAbsolutePath() + "/tei", 
								corpusDir.getAbsolutePath() + "/raw", 
								trainingOutputPath, 
								evalOutputPath, 
								splitRatio);	
	}

    /**
     * Add the selected features to the header model training
     * @param sourceFile source path
     * @param headerPath header path
     * @param trainingOutputPath output training file
     * @return number of corpus files
     */
    public int addFeaturesHeaders(String sourceFile,
                              	String headerPath,
                               	final File trainingOutputPath,
					  			final File evalOutputPath, 
								double splitRatio) {
        System.out.println(sourceFile);
        System.out.println(headerPath); 
        System.out.println(trainingOutputPath);
		System.out.println(evalOutputPath);
		
		System.out.println("TEI files: " + sourceFile);
		System.out.println("header info files: " + headerPath);
		if (trainingOutputPath != null)
			System.out.println("outputPath for training data: " + trainingOutputPath);
		if (evalOutputPath != null)
			System.out.println("outputPath for evaluation data: " + evalOutputPath);
		
        int nbExamples = 0;
        try {
            File pathh = new File(sourceFile);
            // we process all tei files in the output directory
            File[] refFiles = pathh.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".tei") || name.endsWith(".tei.xml");
                }
            });

            if (refFiles == null)
                return 0;

            nbExamples = refFiles.length;
            System.out.println(nbExamples + " tei files");
 
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

            for (File teifile : refFiles) {
                String name = teifile.getName();
                //System.out.println(name);

                TEIHeaderSaxParser parser;
                if (flavor == Flavor.ARTICLE_LIGHT || flavor == Flavor.ARTICLE_LIGHT_WITH_REFERENCES) {
                    parser = new TEIHeaderArticleLightSaxParser();
                } else {
                    parser = new TEIHeaderSaxParser();
                }

                // get a factory
                SAXParserFactory spf = SAXParserFactory.newInstance();
                //get a new instance of parser
                SAXParser par = spf.newSAXParser();
                par.parse(teifile, parser);

                List<String> labeled = parser.getLabeledResult();

                //System.out.println(labeled);
                //System.out.println(parser2.getPDFName()+"._");

                File refDir2 = new File(headerPath);
                String headerFile = null;
                File[] refFiles2 = refDir2.listFiles();
                for (File aRefFiles2 : refFiles2) {
                    String localFileName = aRefFiles2.getName();
                    if (parser.getPDFName() != null) {
                        if (localFileName.equals(parser.getPDFName() + ".header") ||
                            localFileName.equals(parser.getPDFName() + ".training.header")) {
                            headerFile = localFileName;
                            break;
                        }
                        if ((localFileName.startsWith(parser.getPDFName() + "._")) &&
                                (localFileName.endsWith(".header") || localFileName.endsWith(".training.header") )) {
                            headerFile = localFileName;
                            break;
                        }
                    } 
                    if (headerFile == null) {
                        if (localFileName.equals(name.replace(".tei.xml", ""))) {                            
                            headerFile = localFileName;
                        }
                    }
                }

                if (headerFile == null) {
                    System.out.println("raw header file not found for " + name);
                    continue;
                }

                String pathHeader = headerPath + File.separator + headerFile;
                int p = 0;
                BufferedReader bis = new BufferedReader(
                        new InputStreamReader(new FileInputStream(pathHeader), "UTF8"));

                StringBuilder header = new StringBuilder();

                String line;
                while ((line = bis.readLine()) != null) {
                    header.append(line);
                    int ii = line.indexOf(' ');
                    String token = null;
                    if (ii != -1) {
                        token = line.substring(0, ii);
                        // unicode normalisation of the token - it should not be necessary if the training data
                        // has been gnerated by a recent version of grobid
                        token = UnicodeUtil.normaliseTextAndRemoveSpaces(token);
                    }

                    // we get the label in the labelled data file for the same token
                    for (int pp = p; pp < labeled.size(); pp++) {
                        String localLine = labeled.get(pp);
                        StringTokenizer st = new StringTokenizer(localLine, " ");
                        if (st.hasMoreTokens()) {
                            String localToken = st.nextToken();
                            // unicode normalisation of the token - it should not be necessary if the training data
                            // has been gnerated by a recent version of grobid
                            localToken = UnicodeUtil.normaliseTextAndRemoveSpaces(localToken);

                            if (localToken.equals(token)) {
                                String tag = st.nextToken();
                                header.append(" ").append(tag);
                                p = pp + 1;
                                pp = p + 10;
                            } /*else {
                                System.out.println("feature:"+token + " / tei:" + localToken);
                            }*/
                        }
                        if (pp - p > 5) {
                            break;
                        }
                    }
                    header.append("\n");
                }
                bis.close();

                // post process for ensuring continous labelling
                StringBuilder header2 = new StringBuilder();
                String headerStr = header.toString();
                StringTokenizer sto = new StringTokenizer(headerStr, "\n");
                String lastLabel = null;
                String lastLastLabel = null;
                String previousLine = null;

                while (sto.hasMoreTokens()) {
                    String linee = sto.nextToken();
                    StringTokenizer sto2 = new StringTokenizer(linee, " ");
                    String label = null;
                    while (sto2.hasMoreTokens()) {
                        label = sto2.nextToken();
                    }
                    if (label != null) {
                        if (label.length() > 0) {
                            if (!((label.charAt(0) == '<') | (label.startsWith("I-<")))) {
                                label = null;
                            }
                        }
                    }

                    if (previousLine != null) {
                        if ((label != null) & (lastLabel == null) & (lastLastLabel != null)) {
                            if (label.equals(lastLastLabel)) {
                                lastLabel = label;
                                previousLine += " " + label;
                                header2.append(previousLine);
                                header2.append("\n");
                            } else {
                                //if (lastLabel == null)
                                //	previousLine += " <note>";
                                if (lastLabel != null) {
                                    header2.append(previousLine);
                                    header2.append("\n");
                                }
                            }
                        } else {
                            //if (lastLabel == null)
                            //	previousLine += " <note>";
                            if (lastLabel != null) {
                                header2.append(previousLine);
                                header2.append("\n");
                            }
                        }
                    }

//                    previousPreviousLine = previousLine;
                    previousLine = linee;

                    lastLastLabel = lastLabel;
                    lastLabel = label;
                }

                if (lastLabel != null) {
                    header2.append(previousLine);
                    header2.append("\n");
                }
 
				if ( (writer2 == null) && (writer3 != null) )
					writer3.write(header2.toString() + "\n");
				if ( (writer2 != null) && (writer3 == null) )
					writer2.write(header2.toString() + "\n");
				else {		
					if (Math.random() <= splitRatio)
						writer2.write(header2.toString() + "\n");
					else 
						writer3.write(header2.toString() + "\n");
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
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return nbExamples;
    }

    /**
     * Command line execution.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) throws Exception {
        AbstractTrainer.trainAndEvaluate(args, HeaderTrainer::new, HeaderTrainer::new);
    }
}