package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.Person;
import org.grobid.core.engines.tagging.GenericTagger;
import org.grobid.core.engines.tagging.TaggerFactory;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorName;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.lang.Language;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorParser {
	private static Logger LOGGER = LoggerFactory.getLogger(AuthorParser.class);
    private final GenericTagger namesHeaderParser;
    private final GenericTagger namesCitationParser;

    private static final Pattern ET_AL_PATTERN = Pattern.compile("et\\.? al\\.?");

    public AuthorParser() {
        namesHeaderParser = TaggerFactory.getTagger(GrobidModels.NAMES_HEADER);
        namesCitationParser = TaggerFactory.getTagger(GrobidModels.NAMES_CITATION);
    }

    /**
     * Processing of authors in citations
     */
    public List<Person> processingCitation(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        input = input.trim();
        Matcher matcher = ET_AL_PATTERN.matcher(input);
        if (matcher.find()) {
            input = input.substring(0, matcher.start()) + " ";
        }

        // set the language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens, null, false);
    }

    public List<Person> processingCitationLayoutTokens(List<LayoutToken> tokens) throws Exception {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        return processing(tokens, null, false);
    }

    /**
     * Processing of authors in authors
     */
    public List<Person> processingHeader(String input) throws Exception {
        if (StringUtils.isEmpty(input)) {
            return null;
        }

        input = input.trim();
        Matcher matcher = ET_AL_PATTERN.matcher(input);
        if (matcher.find()) {
            input = input.substring(0, matcher.start()) + " ";
        }

        // set the language to English for the analyser to avoid any bad surprises
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        return processing(tokens, null, true);
    }
       
    public List<Person> processingHeaderWithLayoutTokens(List<LayoutToken> inputs, List<PDFAnnotation> pdfAnnotations) {
        return processing(inputs, pdfAnnotations, true);
    }

    /**
     * Common processing of authors in header or citation
     *
     * @param tokens list of LayoutToken object to process
     * @param head - if true use the model for header's name, otherwise the model for names in citation
     * @return List of identified Person entites as POJO.
     */
    public List<Person> processing(List<LayoutToken> tokens, List<PDFAnnotation> pdfAnnotations, boolean head) {
        if (CollectionUtils.isEmpty(tokens)) {
            return null;
        }
        List<Person> fullAuthors = null;
        try {
            List<OffsetPosition> titlePositions = Lexicon.getInstance().tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = Lexicon.getInstance().tokenPositionsPersonSuffix(tokens);

            String sequence = FeaturesVectorName.addFeaturesName(tokens, null, 
                titlePositions, suffixPositions);
            if (StringUtils.isEmpty(sequence))
                return null;
            GenericTagger tagger = head ? namesHeaderParser : namesCitationParser;
            String res = tagger.label(sequence);
//System.out.println(res);
            TaggingTokenClusteror clusteror = new TaggingTokenClusteror(head ? GrobidModels.NAMES_HEADER : GrobidModels.NAMES_CITATION, res, tokens);
            org.grobid.core.data.Person aut = new Person();
            boolean newMarker = false;
            String currentMarker = null;
            List<TaggingTokenCluster> clusters = clusteror.cluster();
            for (TaggingTokenCluster cluster : clusters) {
                if (cluster == null) {
                    continue;
                }

                if(pdfAnnotations != null) {
                    for (LayoutToken authorsToken : cluster.concatTokens()) {
                        for (PDFAnnotation pdfAnnotation : pdfAnnotations) {
                            BoundingBox intersectBox = pdfAnnotation.getIntersectionBox(authorsToken);
                            if (intersectBox != null) {
                                BoundingBox authorsBox = BoundingBox.fromLayoutToken(authorsToken);
                                if (intersectBox.equals(authorsBox)) {
                                } else {
                                    double pixPerChar = authorsToken.getWidth() / authorsToken.getText().length();
                                    int charsCovered = (int) ((intersectBox.getWidth() / pixPerChar) + 0.5);
                                    if (StringUtils.isNotBlank(pdfAnnotation.getDestination())) {
                                        Matcher orcidMatcher = TextUtilities.ORCIDPattern.matcher(pdfAnnotation.getDestination());
                                        if (orcidMatcher.find()) {
                                            // !! here we consider the annot is at the tail or end of the names

                                            // LF: sometimes there is no token at the end of the name, and the annotation covers all the name
                                            // Add boundary check to prevent StringIndexOutOfBoundsException
                                            int textLength = authorsToken.getText().length();
                                            if (charsCovered > 0 && charsCovered < textLength) {
                                                String newToken = authorsToken.getText().substring(0, textLength - charsCovered);
                                                if (StringUtils.isNotBlank(newToken)) {
                                                    authorsToken.setText(newToken);
                                                }
                                            }
                                            aut.setORCID(orcidMatcher.group(1) + "-"
                                                + orcidMatcher.group(2) + "-" + orcidMatcher.group(3)+ "-" + orcidMatcher.group(4));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } 

                TaggingLabel clusterLabel = cluster.getTaggingLabel();
                Engine.getCntManager().i(clusterLabel);
                //String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
                String clusterContent = StringUtils.normalizeSpace(LayoutTokensUtil.toText(cluster.concatTokens()));
                if (StringUtils.isBlank(clusterContent)) {
                    continue;
                }

                if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_MARKER)) {
                    // a marker introduces a new author, and the marker could be attached to the previous (usual) 
                    // or following author (rare)
                    currentMarker = clusterContent;
                    newMarker = true;
                    boolean markerAssigned = false;
                    if (aut.notNull()) {
                        if (fullAuthors == null) {
                            fullAuthors = new ArrayList<Person>();
                        } 
                        aut.addMarker(currentMarker);
                        markerAssigned = true;
                        
                        if (!fullAuthors.contains(aut)) {
                            fullAuthors.add(aut);
                            aut = new Person();
                        }
                    } 
                    if (!markerAssigned) {
                        aut.addMarker(currentMarker);
                    }
                } else if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_TITLE) || 
                            clusterLabel.equals(TaggingLabels.NAMES_CITATION_TITLE)) {
                    if (newMarker) {
                        aut.setTitle(clusterContent);
                        newMarker = false;
                    } else if (aut.getTitle() != null) {
                        if (aut.notNull()) {
                            if (fullAuthors == null)
                                fullAuthors = new ArrayList<Person>();
                            fullAuthors.add(aut);
                        }
                        aut = new Person();
                        aut.setTitle(clusterContent);
                    } else {
                        aut.setTitle(clusterContent);
                    }
                    aut.appendLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_FORENAME) || 
                            clusterLabel.equals(TaggingLabels.NAMES_CITATION_FORENAME)) {
                    if (newMarker) {
                        aut.setFirstName(clusterContent);
                        newMarker = false;
                    } else if (aut.getFirstName() != null) {
                        // new author
                        if (aut.notNull()) {
                            if (fullAuthors == null)
                                fullAuthors = new ArrayList<Person>();
                            fullAuthors.add(aut);
                        }
                        aut = new Person();
                        aut.setFirstName(clusterContent);
                    } else {
                        aut.setFirstName(clusterContent);
                    }
                    aut.appendLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_MIDDLENAME) || 
                            clusterLabel.equals(TaggingLabels.NAMES_CITATION_MIDDLENAME)) {
                    if (newMarker) {
                        aut.setMiddleName(clusterContent);
                        newMarker = false;
                    } else if (aut.getMiddleName() != null) {
                        aut.setMiddleName(aut.getMiddleName() + " " + clusterContent);
                    } else {
                        aut.setMiddleName(clusterContent);
                    }
                    aut.appendLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_SURNAME) || 
                            clusterLabel.equals(TaggingLabels.NAMES_CITATION_SURNAME)) {
                    if (newMarker) {
                        aut.setLastName(clusterContent);
                        newMarker = false;
                    } else if (aut.getLastName() != null) {
                        // new author
                        if (aut.notNull()) {
                            if (fullAuthors == null)
                                fullAuthors = new ArrayList<Person>();
                            fullAuthors.add(aut);
                        }
                        aut = new Person();
                        aut.setLastName(clusterContent);
                    } else {
                        aut.setLastName(clusterContent);
                    }
                    aut.appendLayoutTokens(cluster.concatTokens());
                } else if (clusterLabel.equals(TaggingLabels.NAMES_HEADER_SUFFIX) || 
                            clusterLabel.equals(TaggingLabels.NAMES_CITATION_SUFFIX)) {
                    /*if (newMarker) {
                        aut.setSuffix(clusterContent);
                        newMarker = false;
                    } else*/ 
                    if (aut.getSuffix() != null) {
                        aut.setSuffix(aut.getSuffix() + " " + clusterContent);
                    } else {
                        aut.setSuffix(clusterContent);
                    }
                    aut.appendLayoutTokens(cluster.concatTokens());
                }
            }

            // add last built author
            if (aut.notNull()) {
                if (fullAuthors == null) {
                    fullAuthors = new ArrayList<Person>();
                }
                fullAuthors.add(aut);
            }

            // some more person name normalisation
            if (fullAuthors != null) {
                for(Person author : fullAuthors) {
                    author.normalizeName();
                }
            } 

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullAuthors;
    }

    private boolean nameLabel(String label) {
        return label.endsWith("<surname>") || label.endsWith("<forename>") || label.endsWith("<middlename>");
    }

    /**
     * Extract results from a list of name strings in the training format without any string modification.
	 *
	 * @param input - the sequence of author names to be processed as a string.
	 * @param head - if true use the model for header's name, otherwise the model for names in citation
	 * @return the pseudo-TEI training data
	 */
    public StringBuilder trainingExtraction(String input,
                                            boolean head) {
        if (StringUtils.isEmpty(input))
            return null;
        // force analyser with English, to avoid bad surprise
        List<LayoutToken> tokens = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(input, new Language("en", 1.0));
        StringBuilder buffer = new StringBuilder();
        try {
            if (CollectionUtils.isEmpty(tokens)) {
                return null;
            }

            List<OffsetPosition> titlePositions = Lexicon.getInstance().tokenPositionsPersonTitle(tokens);
            List<OffsetPosition> suffixPositions = Lexicon.getInstance().tokenPositionsPersonSuffix(tokens);

            String sequence = FeaturesVectorName.addFeaturesName(tokens, null, titlePositions, suffixPositions);
            if (StringUtils.isEmpty(sequence))
                return null;
            GenericTagger tagger = head ? namesHeaderParser : namesCitationParser;
            String res = tagger.label(sequence);

            // extract results from the processed file
            StringTokenizer st2 = new StringTokenizer(res, "\n");
            String lastTag = null;
            boolean start = true;
            boolean hasMarker = false;
            boolean hasSurname = false;
            boolean hasForename = false;
            boolean tagClosed;
            int q = 0;
            boolean addSpace;
            String lastTag0;
            String currentTag0;
            while (st2.hasMoreTokens()) {
                String line = st2.nextToken();
                addSpace = false;
                if ((line.trim().length() == 0)) {
                    // new author
					if (head)
                    	buffer.append("/t<author>\n");
					else {
						//buffer.append("<author>");
					}
                    continue;
                } else {
                    String theTok = tokens.get(q).getText();
                    while (theTok.equals(" ") || theTok.equals("\n")) {
                        addSpace = true;
                        q++;
                        theTok = tokens.get(q).getText();
                    }
                    q++;
                }

                StringTokenizer st3 = new StringTokenizer(line, "\t");
                int ll = st3.countTokens();
                int i = 0;
                String s1 = null;
                String s2 = null;
                boolean newLine = false;
                List<String> localFeatures = new ArrayList<String>();
                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // string
                    } else if (i == ll - 2) {
                    } else if (i == ll - 1) {
                        s1 = s; // label
                    } else {
                        localFeatures.add(s);
                        if (s.equals("LINESTART") && !start) {
                            newLine = true;
                            start = false;
                        } else if (s.equals("LINESTART")) {
                            start = false;
                        }
                    }
                    i++;
                }

                lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                tagClosed = lastTag0 != null && testClosingTag(buffer, currentTag0, lastTag0, head);

                if (newLine) {
                    if (tagClosed) {
                        buffer.append("\t\t\t\t\t\t\t<lb/>\n");
                    } else {
                        buffer.append("<lb/>");
                    }

                }

                String output = writeField(s1, lastTag0, s2, "<marker>", "<marker>", addSpace, 8, head);
                if (output != null) {
                    if (hasMarker) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        } else {
                            //buffer.append("</author>\n");
                        }
                        hasForename = false;
                        hasSurname = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        } else {
                            //buffer.append("<author>\n");
                        }
                        hasMarker = true;
                    }
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                } else {
                    output = writeField(s1, lastTag0, s2, "<other>", "<other>", addSpace, 8, head);
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<forename>", "<forename>", addSpace, 8, head);
                } else {
                    if (buffer.length() > 0) {
                        if (buffer.charAt(buffer.length() - 1) == '\n') {
                            buffer.deleteCharAt(buffer.length() - 1);
                        }
                    }
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<middlename>", "<middlename>", addSpace, 8, head);
                } else {
                    if (hasForename && !currentTag0.equals(lastTag0)) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        } else {
                            //buffer.append("</author>\n");
                        }
                        hasMarker = false;
                        hasSurname = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        } else {
                            //buffer.append("<author>\n");
                        }
                    }
                    hasForename = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<surname>", "<surname>", addSpace, 8, head);
                } else {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<title>", "<roleName>", addSpace, 8, head);
                } else {
                    if (hasSurname && !currentTag0.equals(lastTag0)) {
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t</persName>\n");
                        } else {
                            //buffer.append("</author>\n");
                        }
                        hasMarker = false;
                        hasForename = false;
                        if (head) {
                            buffer.append("\t\t\t\t\t\t\t<persName>\n");
                        } else {
                            //buffer.append("<author>\n");
                        }
                    }
                    hasSurname = true;
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output == null) {
                    output = writeField(s1, lastTag0, s2, "<suffix>", "<suffix>", addSpace, 8, head);
                } else {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }
                if (output != null) {
                    buffer.append(output);
                    lastTag = s1;
                    continue;
                }

                lastTag = s1;
            }

            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
                currentTag0 = "";
                testClosingTag(buffer, currentTag0, lastTag0, head);
            }
        } catch (Exception e) {
//			e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(String s1,
                              String lastTag0,
                              String s2,
                              String field,
                              String outField,
                              boolean addSpace,
                              int nbIndent, 
							  boolean head) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if ((s1.equals(lastTag0) || s1.equals("I-" + lastTag0))) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
				if (head) {
	                for (int i = 0; i < nbIndent; i++) {
	                    result += "\t";
	                }
				}
				if (addSpace)
					result += " " + outField + s2;
				else		
 					result += outField + s2;
            }
        }
        return result;
    }

    private boolean testClosingTag(StringBuilder buffer,
                                   String currentTag0,
                                   String lastTag0,
								   boolean head) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<forename>")) {
                buffer.append("</forename>");
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<middlename>")) {
                buffer.append("</middlename>");
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<surname>")) {
                buffer.append("</surname>");
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<title>")) {
                buffer.append("</roleName>");
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<suffix>")) {
                buffer.append("</suffix>");
				if (head)
					buffer.append("\n");
            } else if (lastTag0.equals("<marker>")) {
                buffer.append("</marker>");
				if (head)
					buffer.append("\n");
            } else {
                res = false;
            }

        }
        return res;
    }

    public void close() throws IOException {
    }
}