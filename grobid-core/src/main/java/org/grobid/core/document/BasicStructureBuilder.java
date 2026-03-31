package org.grobid.core.document;

import com.google.common.collect.Iterables;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.Block;
import org.grobid.core.layout.Cluster;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.LayoutTokenization;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.layout.GraphicObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Class for building basic structures in a document item.
 *
 */
public class BasicStructureBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicStructureBuilder.class);

	// note: these regular expressions will disappear as a new CRF model is now covering 
	// the overall document segmentation
    /*static public Pattern introduction =
            Pattern.compile("^\\b*(Introduction?|Einleitung|INTRODUCTION|Acknowledge?ments?|Acknowledge?ment?|Background?|Content?|Contents?|Motivations?|1\\.\\sPROBLEMS?|1\\.(\\n)?\\sIntroduction?|1\\.\\sINTRODUCTION|I\\.(\\s)+Introduction|1\\.\\sProblems?|I\\.\\sEinleitung?|1\\.\\sEinleitung?|1\\sEinleitung?|1\\sIntroduction?)",
                    Pattern.CASE_INSENSITIVE);
    static public Pattern introductionStrict =
            Pattern.compile("^\\b*(1\\.\\sPROBLEMS?|1\\.(\\n)?\\sIntroduction?|1\\.(\\n)?\\sContent?|1\\.\\sINTRODUCTION|I\\.(\\s)+Introduction|1\\.\\sProblems?|I\\.\\sEinleitung?|1\\.\\sEinleitung?|1\\sEinleitung?|1\\sIntroduction?)",
                    Pattern.CASE_INSENSITIVE);
    static public Pattern abstract_ = Pattern.compile("^\\b*\\.?(abstract?|résumé?|summary?|zusammenfassung?)",
            Pattern.CASE_INSENSITIVE);*/
    static public Pattern headerNumbering1 = Pattern.compile("^(\\d+)\\.?\\s");
    static public Pattern headerNumbering2 = Pattern.compile("^((\\d+)\\.)+(\\d+)\\s");
    static public Pattern headerNumbering3 = Pattern.compile("^((\\d+)\\.)+\\s");
    static public Pattern headerNumbering4 = Pattern.compile("^([A-Z](I|V|X)*(\\.(\\d)*)*\\s)");

    private static Pattern startNum = Pattern.compile("^(\\d)+\\s");
    private static Pattern endNum = Pattern.compile("\\s(\\d)+$");

    /**
     * Cluster the blocks following the font, style and size aspects
     *
	 * -> not used at this stage, but could be an interesting feature in the full text model in the future 
	 *
     * @param b   integer
     * @param doc a document
     */
    /*private static void addBlockToCluster(Integer b, Document doc) {
        // get block features
        Block block = doc.getBlocks().get(b);
        String font = block.getFont();
        boolean bold = block.getBold();
        boolean italic = block.getItalic();
        double fontSize = block.getFontSize();
        boolean found = false;

        if (font == null) {
            font = "unknown";
        }
        //System.out.println(font + " " + bold + " " + italic + " " + fontSize );

        if (doc.getClusters() == null) {
            doc.setClusters(new ArrayList<Cluster>());
        } else {
            for (Cluster cluster : doc.getClusters()) {
                String font2 = cluster.getFont();
                if (font2 == null)
                    font2 = "unknown";
                if (font.equals(font2) &&
                        (bold == cluster.getBold()) &
                                (italic == cluster.getItalic()) &
                                (fontSize == cluster.getFontSize())) {
                    cluster.addBlock2(b);
                    found = true;
                }
            }
        }

        if (!found) {
            Cluster cluster = new Cluster();
            cluster.setFont(font);
            cluster.setBold(bold);
            cluster.setItalic(italic);
            cluster.setFontSize(fontSize);
            cluster.addBlock2(b);
            doc.getClusters().add(cluster);
        }
    }*/

    static public Document generalResultSegmentation(Document doc, String labeledResult, List<LayoutToken> documentTokens) {
        List<Pair<String, String>> labeledTokens = GenericTaggerUtils.getTokensAndLabels(labeledResult);

        SortedSetMultimap<String, DocumentPiece> labeledBlocks = TreeMultimap.create();
        doc.setLabeledBlocks(labeledBlocks);

        List<Block> docBlocks = doc.getBlocks();
		int indexLine = 0;		
        int blockIndex = 0;
        int p = 0; // position in the labeled result 
		int currentLineEndPos = 0; // position in the global doc. tokenization of the last 
								// token of the current line
		int currentLineStartPos = 0; // position in the global doc. 
									 // tokenization of the first token of the current line
		String line = null;
		
		//DocumentPointer pointerA = DocumentPointer.START_DOCUMENT_POINTER;
		// the default first block might not contain tokens but only bitmap - in this case we move
		// to the first block containing some LayoutToken objects
        while (docBlocks.get(blockIndex).getTokens() == null ||
            docBlocks.get(blockIndex).getNbTokens() == 0
                //TODO: make things right
//                || docBlocks.get(blockIndex).getStartToken() == -1
                ) {
            blockIndex++;
        }
        DocumentPointer pointerA = new DocumentPointer(doc, blockIndex, docBlocks.get(blockIndex).getStartToken());
		
        DocumentPointer currentPointer = null;
        DocumentPointer lastPointer = null;

        String curLabel;
        String curPlainLabel = null;
        String lastPlainLabel = null;

        int lastTokenInd = -1;
        for (int i = docBlocks.size() - 1; i >=0; i--) {
            int endToken = docBlocks.get(i).getEndToken();
            if (endToken != -1) {
                lastTokenInd = endToken;
                break;
            }
        }

        // we do this concatenation trick so that we don't have to process stuff after the main loop
        // no copying of lists happens because of this, so it's ok to concatenate
        String ignoredLabel = "@IGNORED_LABEL@";
        for (Pair<String, String> labeledTokenPair :
                Iterables.concat(labeledTokens, 
					Collections.singleton(Pair.of("IgnoredToken", ignoredLabel)))) {
            if (labeledTokenPair == null) {
                p++;
                continue;
            }

			// as we process the document segmentation line by line, we don't use the usual 
			// tokenization to rebuild the text flow, but we get each line again from the 
			// text stored in the document blocks (similarly as when generating the features) 
			line = null;
			while( (line == null) && (blockIndex < docBlocks.size()) ) {
				Block block = docBlocks.get(blockIndex);
		        List<LayoutToken> tokens = block.getTokens();
				String localText = block.getText();
		        if ( (tokens == null) || StringUtils.isBlank(localText) ) {
					blockIndex++;
					indexLine = 0;
					if (blockIndex < docBlocks.size()) {
						block = docBlocks.get(blockIndex);
						currentLineStartPos = block.getStartToken();
					}
		            continue;
		        }
				String[] lines = localText.split("[\\n\\r]");
				if ( (lines.length == 0) || (indexLine >= lines.length) || indexLine> 10000) {
					blockIndex++;
					indexLine = 0;
					if (blockIndex < docBlocks.size()) {
						block = docBlocks.get(blockIndex);
						currentLineStartPos = block.getStartToken();
					}
					continue;
				}
				else {
					line = lines[indexLine];
					indexLine++;
					if ( (line.trim().length() == 0) || (TextUtilities.filterLine(line)) ) {
						line = null;
						continue;
					}

					if (currentLineStartPos > lastTokenInd) {
                        break;
                    }
					
					// adjust the start token position in documentTokens to this non trivial line
					// first skip possible space characters and tabs at the beginning of the line
					while( (documentTokens.get(currentLineStartPos).t().equals(" ") ||
							documentTokens.get(currentLineStartPos).t().equals("\t") )
					 	&& (currentLineStartPos != lastTokenInd)) {
					 	currentLineStartPos++;
					}
					if (!labeledTokenPair.getLeft().startsWith(documentTokens.get(currentLineStartPos).getText())) {
						while(currentLineStartPos < block.getEndToken()) {
							if (documentTokens.get(currentLineStartPos).t().equals("\n")
							 || documentTokens.get(currentLineStartPos).t().equals("\r")) {
								 // move to the start of the next line, but ignore space characters and tabs
								 currentLineStartPos++;
								 while( (documentTokens.get(currentLineStartPos).t().equals(" ") ||
									 	documentTokens.get(currentLineStartPos).t().equals("\t") )
								 	&& (currentLineStartPos != lastTokenInd)) {
								 	currentLineStartPos++;
								 }
								 if ((currentLineStartPos != lastTokenInd) && 
								 	labeledTokenPair.getLeft().startsWith(documentTokens.get(currentLineStartPos).getText())) {
									break;
								 }
							 }
							 currentLineStartPos++;
						}
					}

					// what is then the position of the last token of this line?
					currentLineEndPos = currentLineStartPos;
					while(currentLineEndPos < block.getEndToken()) {
						if (documentTokens.get(currentLineEndPos).t().equals("\n")
						 || documentTokens.get(currentLineEndPos).t().equals("\r")) {
							currentLineEndPos--;
							break;
						}
						currentLineEndPos++;
					}
				}
			}
            curLabel = labeledTokenPair.getRight();
            curPlainLabel = GenericTaggerUtils.getPlainLabel(curLabel);
			
			/*System.out.println("-------------------------------");
			System.out.println("block: " + blockIndex);
			System.out.println("line: " + line);
			System.out.println("token: " + labeledTokenPair.a);
			System.out.println("curPlainLabel: " + curPlainLabel);
			System.out.println("lastPlainLabel: " + lastPlainLabel);
			if ((currentLineStartPos < lastTokenInd) && (currentLineStartPos != -1))
				System.out.println("currentLineStartPos: " + currentLineStartPos + 
											" (" + documentTokens.get(currentLineStartPos) + ")");
			if ((currentLineEndPos < lastTokenInd) && (currentLineEndPos != -1))
				System.out.println("currentLineEndPos: " + currentLineEndPos + 
											" (" + documentTokens.get(currentLineEndPos) + ")");*/
			
			if (blockIndex == docBlocks.size()) {
				break;
			}

            currentPointer = new DocumentPointer(doc, blockIndex, currentLineEndPos);

            // either a new entity starts or a new beginning of the same type of entity
			if ((!curPlainLabel.equals(lastPlainLabel)) && (lastPlainLabel != null)) {	
				if ( (pointerA.getTokenDocPos() <= lastPointer.getTokenDocPos()) &&
				   	(pointerA.getTokenDocPos() != -1) ) {
					labeledBlocks.put(lastPlainLabel, new DocumentPiece(pointerA, lastPointer));
				}
                pointerA = new DocumentPointer(doc, blockIndex, currentLineStartPos);
				//System.out.println("add segment for: " + lastPlainLabel + ", until " + (currentLineStartPos-2));
            }

            //updating stuff for next iteration
            lastPlainLabel = curPlainLabel;
            lastPointer = currentPointer;
			currentLineStartPos = currentLineEndPos+2; // one shift for the EOL, one for the next line
            p++;
        }

		if (blockIndex == docBlocks.size()) {
			// the last labelled piece has still to be added
			if (!StringUtils.equals(curPlainLabel, lastPlainLabel) && lastPlainLabel != null) {
				if ( (pointerA.getTokenDocPos() <= lastPointer.getTokenDocPos()) && 
					(pointerA.getTokenDocPos() != -1) ) {
					labeledBlocks.put(lastPlainLabel, new DocumentPiece(pointerA, lastPointer));
					//System.out.println("add segment for: " + lastPlainLabel + ", until " + (currentLineStartPos-2));
				}
			}
		}

        return doc;
    }

    static public Document figureResultSegmentation(Document doc,
                                                    List<GraphicObject> figureAnchors,
                                                    String labelledResultsUp,
                                                    List<LayoutTokenization> theTokenizationsUp,
                                                    String labelledResultsDown,
                                                    List<LayoutTokenization> theTokenizationsDown) {


        return doc;
    }

}