package org.grobid.core.engines;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.engines.label.TaggingLabels.*;
import static org.grobid.core.layout.VectorGraphicBoxCalculator.mergeBoxes;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import nu.xom.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.*;
import org.grobid.core.data.AnnotatedXMLElement;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorFunding;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.UnicodeUtil;

public class FundingAcknowledgementParser extends AbstractParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(FundingAcknowledgementParser.class);

    protected FundingAcknowledgementParser() {
        super(GrobidModels.FUNDING_ACKNOWLEDGEMENT);
    }

    FundingAcknowledgementParser(GrobidModel model) {
        super(model);
    }

    private MutablePair<List<AnnotatedXMLElement>, FundingAcknowledgmentParse> processing(
            List<LayoutToken> tokenizationFunding,
            GrobidAnalysisConfig config) {
        if (CollectionUtils.isEmpty(tokenizationFunding)) {
            return null;
        }
        String res;
        try {
            String featureVector = FeaturesVectorFunding.addFeatures(tokenizationFunding, null);
            res = labelAndCapture(featureVector, config);

        } catch (Exception e) {
            throw new GrobidException("CRF labeling with table model fails.", e);
        }

        if (res == null) {
            return null;
        }
        return getExtractionResult(tokenizationFunding, res);
    }

    /**
     * For convenience, a processing method taking a raw string as input.
     * Tokenization is done with the default Grobid analyzer triggered by the identified language.
     *
     **/
    public MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> processing(
            String text,
            GrobidAnalysisConfig config) {
        text = UnicodeUtil.normaliseText(text);
        //        List<LayoutToken> tokenizationFunding = GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text);
        //        MutablePair<List<AnnotatedXMLElement>, FundingAcknowledgmentParse> results = processing(tokenizationFunding, config);
        //        MutableTriple<List<Funding>, List<Person>, List<Affiliation>> entities = MutableTriple.of(results.getRight().getFundings(), results.getRight().getPersons(), results.getRight().getAffiliations());
        //        List<AnnotatedXMLElement> annotations = results.getLeft();

        Element outputParagraph = teiElement("p");
        outputParagraph.appendChild(text);

        if (config.isWithSentenceSegmentation()) {
            List<OffsetPosition> theSentences = SentenceUtilities.getInstance().runSentenceDetection(text);

            // update the xml paragraph element
            int pos = 0;
            int posInSentence = 0;
            for (int i = 0; i < theSentences.size(); i++) {
                pos = theSentences.get(i).start;
                posInSentence = 0;
                Element sentenceElement = teiElement("s");

                if (pos + posInSentence <= theSentences.get(i).end) {
                    String localTextChunk = text.substring(pos + posInSentence, theSentences.get(i).end);
                    localTextChunk = XmlBuilderUtils.stripNonValidXMLCharacters(localTextChunk);
                    sentenceElement.appendChild(localTextChunk);
                    outputParagraph.appendChild(sentenceElement);
                }
            }

            for (int i = outputParagraph.getChildCount() - 1; i >= 0; i--) {
                Node theNode = outputParagraph.getChild(i);
                if (theNode instanceof Text) {
                    outputParagraph.removeChild(theNode);
                } else if (theNode instanceof Element) {
                    if (!((Element) theNode).getLocalName().equals("s")) {
                        outputParagraph.removeChild(theNode);
                    }
                }
            }
        }

        return processingXmlFragment(outputParagraph.toXML(), config);
    }

    /**
     * This method takes in input a tokenized text, a set of annotations and a root element and attach a list of nodes
     * under the root where the text is combined with the annotations
     */
    protected static Element injectedAnnotationsInNode(
            List<LayoutToken> tokenizationFunding,
            List<Pair<OffsetPosition, Element>> annotations,
            Element rootElement) {

        int pos = 0;
        for (Pair<OffsetPosition, Element> annotation : annotations) {
            OffsetPosition annotationPosition = annotation.getLeft();
            Element annotationContentElement = annotation.getRight();

            List<LayoutToken> before = tokenizationFunding.subList(pos, annotationPosition.start);
            String clusterContentBefore = LayoutTokensUtil.toText(before);

            if (CollectionUtils.isNotEmpty(before) && before.get(0).getText().equals(" ")) {
                rootElement.appendChild(new Text(" "));
            }

            rootElement.appendChild(clusterContentBefore);

            pos = annotationPosition.end;
            rootElement.appendChild(annotationContentElement);
        }

        // add last chunk of paragraph stuff (or whole paragraph if no note callout matching)
        List<LayoutToken> remaining = tokenizationFunding.subList(pos, tokenizationFunding.size());
        String remainingClusterContent = LayoutTokensUtil.normalizeDehyphenizeText(remaining);

        if (CollectionUtils.isNotEmpty(remaining) && remaining.get(0).getText().equals(" ")) {
            rootElement.appendChild(new Text(" "));
        }

        rootElement.appendChild(remainingClusterContent);

        return rootElement;
    }

    /**
     * For convenience, a processing method taking an TEI XML segment as input - only paragraphs (Element p)
     * will be processed in this segment and paragraph element will be replaced with the processed content.
     * Resulting entities are relative to the whole processed XML segment.
     *
     * Tokenization is done with the default Grobid analyzer triggered by the identified language.
     **/
    public MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> processingXmlFragment(
            String tei,
            GrobidAnalysisConfig config) {
        Builder parser = new Builder();
        MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> globalResult = null;
        try {
            tei = tei.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");

            //System.out.println(tei);
            Document localDoc = parser.build(tei, null);

            // get the paragraphs
            Element rootElementStatement = localDoc.getRootElement();
            Nodes paragraphs = rootElementStatement.query("//p");

            boolean sentenceSegmentation = config.isWithSentenceSegmentation();

            for (Node paragraph : paragraphs) {
                String paragraphText = paragraph.getValue();
                GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();
                List<LayoutToken> tokenizationFunding = analyzer.tokenizeWithLayoutToken(paragraphText);

                MutablePair<List<AnnotatedXMLElement>, FundingAcknowledgmentParse> localResult = processing(
                        tokenizationFunding,
                        config);

                if (localResult == null || CollectionUtils.isEmpty(localResult.left)) {
                    continue;
                }
                List<AnnotatedXMLElement> annotations = localResult.left;
                FundingAcknowledgmentParse localEntities = localResult.right;

                List<OffsetPosition> annotationsPositionTokens = annotations.stream()
                        .map(AnnotatedXMLElement::getOffsetPosition)
                        .collect(Collectors.toList());

                List<OffsetPosition> annotationsPositionText = TextUtilities
                        .matchTokenAndString(tokenizationFunding, paragraphText, annotationsPositionTokens);
                List<AnnotatedXMLElement> annotationsWithPosRefToText = new ArrayList<>();
                for (int i = 0; i < annotationsPositionText.size(); i++) {
                    annotationsWithPosRefToText.add(
                            new AnnotatedXMLElement(annotations.get(i).getAnnotationNode(),
                                    annotationsPositionText.get(i)));
                }

                annotations = annotationsWithPosRefToText;

                if (sentenceSegmentation) {
                    Nodes sentences = paragraph.query(".//s");

                    if (sentences.size() == 0) {
                        // Overly careful - we should never end up here.
                        LOGGER.warn(
                                "While the configuration claim that paragraphs must be segmented, we did not find any sentence. ");
                        updateParagraphNodeWithAnnotations(paragraph, annotations);
                    }
                    mergeSentencesFallingOnAnnotations(sentences, annotations, config);
                    updateSentencesNodesWithAnnotations(sentences, annotations);
                } else {
                    updateParagraphNodeWithAnnotations(paragraph, annotations);
                }

                // update extracted entities
                if (globalResult == null) {
                    globalResult = MutablePair.of(
                            rootElementStatement,
                            MutableTriple.of(
                                    localEntities.getFundings(),
                                    localEntities.getPersons(),
                                    localEntities.getAffiliations()));
                } else {
                    // concatenate members of the local results to the global ones
                    globalResult = aggregateResults(
                            MutableTriple.of(
                                    localEntities.getFundings(),
                                    localEntities.getPersons(),
                                    localEntities.getAffiliations()),
                            globalResult);
                }

            }

            //System.out.println(globalResult.getLeft().toXML());
        } catch (ValidityException exp) {
            LOGGER.warn("Invalid TEI fragment from funding/acknowledgement section", exp);
        } catch (ParsingException exp) {
            LOGGER.warn("Parsing error of the TEI fragment from funding/acknowledgement section", exp);
        } catch (IOException exp) {
            LOGGER.warn("Input TEI fragment invalid from funding/acknowledgement section", exp);
        }

        return globalResult;
    }

    /**
     * This method identify the sentences that should be merged because the annotations are falling on their boundaries.
     * This is necessary when the annotations are extracted from the paragraphs they need to be applied to sentences
     * calculated from the plain text.
     * <b>This method modify the sentences in input</b>
     */
    private static Nodes mergeSentencesFallingOnAnnotations(
            Nodes sentences,
            List<AnnotatedXMLElement> annotations,
            GrobidAnalysisConfig config) {
        // We merge the sentences (including their coordinates) for which the annotations
        // are falling in between two of them or they will be lost later.

        List<OffsetPosition> sentencePositions = getOffsetPositionsFromNodes(sentences);

        // We obtain the corrected coordinates that don't fall over the annotations
        List<OffsetPosition> correctedOffsetPositions = SentenceUtilities.correctSentencePositions(
                sentencePositions,
                annotations
                        .stream()
                        .map(AnnotatedXMLElement::getOffsetPosition)
                        .collect(Collectors.toList()));

        List<Integer> toRemove = new ArrayList<>();
        for (OffsetPosition correctedOffsetPosition : correctedOffsetPositions) {
            List<OffsetPosition> originalSentences = sentencePositions.stream()
                    .filter(a -> a.start >= correctedOffsetPosition.start && a.end <= correctedOffsetPosition.end)
                    .collect(Collectors.toList());

            // if for each "corrected sentences offset" there are more than one original sentence that
            // falls into it, it means we need to merge
            if (originalSentences.size() > 1) {
                List<Integer> toMerge = originalSentences.stream()
                        .map(sentencePositions::indexOf)
                        .collect(Collectors.toList());

                Element destination = (Element) sentences.get(toMerge.get(0));
                boolean needToMergeCoordinates = config.isGenerateTeiCoordinates("s");
                List<BoundingBox> boundingBoxes = new ArrayList<>();
                Attribute destCoordinates = null;

                if (needToMergeCoordinates) {
                    destCoordinates = destination.getAttribute("coords");
                    String coordinates = destCoordinates.getValue();
                    boundingBoxes = Arrays.stream(coordinates.split(";"))
                            .filter(StringUtils::isNotBlank)
                            .map(BoundingBox::fromString)
                            .collect(Collectors.toList());
                    destination.removeAttribute(destCoordinates);
                }

                for (int i = 1; i < toMerge.size(); i++) {
                    Integer sentenceToMergeIndex = toMerge.get(i);
                    Node sentenceToMerge = sentences.get(sentenceToMergeIndex);

                    // Merge coordinates
                    if (needToMergeCoordinates) {
                        Attribute coords = ((Element) sentenceToMerge).getAttribute("coords");
                        String coordinates = coords.getValue();
                        boundingBoxes.addAll(
                                Arrays.stream(coordinates.split(";"))
                                        .filter(StringUtils::isNotBlank)
                                        .map(BoundingBox::fromString)
                                        .collect(Collectors.toList()));

                        // Group by page, then merge
                        List<BoundingBox> postMergeBoxes = new ArrayList<>();
                        Map<Integer, List<BoundingBox>> boundingBoxesByPage = boundingBoxes.stream()
                                .collect(Collectors.groupingBy(BoundingBox::getPage));
                        for (Map.Entry<Integer, List<BoundingBox>> boxesByPages : boundingBoxesByPage.entrySet()) {
                            List<BoundingBox> mergedBoundingBoxes = mergeBoxes(boxesByPages.getValue());
                            postMergeBoxes.addAll(mergedBoundingBoxes);
                        }

                        String coordsAsString = String.join(
                                ";",
                                postMergeBoxes.stream().map(BoundingBox::toString).collect(Collectors.toList()));
                        Attribute newCoords = new Attribute("coords", coordsAsString);
                        destination.addAttribute(newCoords);
                    }

                    // Merge content
                    boolean first = true;
                    Node previous = null;
                    for (int c = 0; c < sentenceToMerge.getChildCount(); c++) {
                        Node child = sentenceToMerge.getChild(c);

                        if (first) {
                            first = false;
                            Node lastNodeDestination = destination.getChild(destination.getChildCount() - 1);
                            previous = lastNodeDestination;
                            //                                        if (lastNodeDestination instanceof Text) {
                            //                                            ((Text) lastNodeDestination).setValue(((Text) lastNodeDestination).getValue() + " ");
                            //                                            previous = lastNodeDestination;
                            //                                        } else {
                            //                                            Text newSpace = new Text(" ");
                            //                                            destination.appendChild(newSpace);
                            //                                            previous = newSpace;
                            //                                        }
                        }

                        if (previous instanceof Text && child instanceof Text) {
                            ((Text) previous).setValue(previous.getValue() + child.getValue());
                        } else {
                            ((Element) sentenceToMerge).replaceChild(child, new Text("placeholder"));
                            child.detach();
                            destination.appendChild(child);
                            previous = child;
                        }
                    }
                    sentenceToMerge.detach();
                    toRemove.add(sentenceToMergeIndex);
                }
            }
        }
        toRemove.stream()
                .sorted(Comparator.reverseOrder())
                .forEach(sentences::remove);

        return sentences;
    }

    private static List<OffsetPosition> getOffsetPositionsFromNodes(Nodes sentences) {
        List<OffsetPosition> sentencePositions = new ArrayList<>();
        int start = 0;
        for (Node sentence : sentences) {
            int end = start + sentence.getValue().length();
            sentencePositions.add(new OffsetPosition(start, end));
            start = end;
        }
        return sentencePositions;
    }

    private static void updateParagraphNodeWithAnnotations(Node paragraph, List<AnnotatedXMLElement> annotations) {
        int pos = 0;
        List<Node> newChildren = new ArrayList<>();
        for (int i = 0; i < paragraph.getChildCount(); i++) {
            //Assumption here is that the structure is flat to maximum one level down
            Node currentNode = paragraph.getChild(i);
            if (currentNode instanceof Text) {
                String text = currentNode.getValue();
                int finalPos = pos;
                List<AnnotatedXMLElement> annotationsInThisChunk = annotations.stream()
                        .filter(
                                a -> a.getOffsetPosition().start >= finalPos
                                        && a.getOffsetPosition().end <= finalPos + text.length())
                        .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(annotationsInThisChunk)) {
                    List<Node> nodes = getNodesAnnotationsInTextNode(currentNode, annotationsInThisChunk, pos);
                    newChildren.addAll(nodes);
                } else {
                    newChildren.add(currentNode);
                }
                pos += text.length();
            } else if (currentNode instanceof Element) {
                newChildren.add(currentNode);
                pos += currentNode.getValue().length();
            }
        }

        for (int i = 0; i < paragraph.getChildCount(); i++) {
            paragraph.getChild(i).detach();
        }
        for (Node node : newChildren) {
            node.detach();
            ((Element) paragraph).appendChild(node);
        }
    }

    private static void updateSentencesNodesWithAnnotations(Nodes sentences, List<AnnotatedXMLElement> annotations) {
        int pos = 0;
        int sentenceStartOffset = 0;
        for (Node sentence : sentences) {
            String sentenceText = sentence.getValue();
            List<Node> newChildren = new ArrayList<>();
            for (int i = 0; i < sentence.getChildCount(); i++) {
                //Assumption here is that the structure is flat to maximum one level down
                Node currentNode = sentence.getChild(i);
                if (currentNode instanceof Text) {
                    String text = currentNode.getValue();
                    int finalPos = pos;
                    List<AnnotatedXMLElement> annotationsInThisChunk = annotations.stream()
                            .filter(
                                    a -> a.getOffsetPosition().start >= finalPos
                                            && a.getOffsetPosition().end <= finalPos + text.length())
                            .collect(Collectors.toList());

                    if (CollectionUtils.isNotEmpty(annotationsInThisChunk)) {
                        List<Node> nodes = getNodesAnnotationsInTextNode(currentNode, annotationsInThisChunk, pos);
                        newChildren.addAll(nodes);
                    } else {
                        newChildren.add(currentNode);
                    }
                    pos += text.length();
                } else if (currentNode instanceof Element) {
                    newChildren.add(currentNode);
                    pos += currentNode.getValue().length();
                } /*else {
                    System.out.println(currentNode);
                  }*/
            }

            for (int i = 0; i < sentence.getChildCount(); i++) {
                sentence.getChild(i).detach();
            }
            for (Node node : newChildren) {
                node.detach();
                ((Element) sentence).appendChild(node);
            }

            sentenceStartOffset += sentenceText.length();
        }
    }

    /**
     * This method return a list of nodes corresponding to the annotations as they are positioned in
     * the text content of the target node. If the node is empty, should be used @see injectedAnnotationsInNode
     * as this method will fail
     */
    protected static List<Node> getNodesAnnotationsInTextNode(Node targetNode, List<AnnotatedXMLElement> annotations) {
        return getNodesAnnotationsInTextNode(targetNode, annotations, 0);
    }

    /**
     * The sentence offset allow to calculate the position relative to the sentence of annotations that
     * have been calculated in relation with the paragraph.
     */
    protected static List<Node> getNodesAnnotationsInTextNode(
            Node targetNode,
            List<AnnotatedXMLElement> annotations,
            int sentenceOffset) {
        String text = targetNode.getValue();

        List<Node> outputNodes = new ArrayList<>();

        int pos = 0;
        for (AnnotatedXMLElement annotation : annotations) {
            OffsetPosition annotationPosition = annotation.getOffsetPosition();
            Element annotationContentElement = annotation.getAnnotationNode();

            String before = text.substring(pos, annotationPosition.start - sentenceOffset);

            //            if (StringUtils.isNotEmpty(before) && before.startsWith(" ")) {
            //                outputNodes.add(new Text(" "));
            //            }

            outputNodes.add(new Text(before));
            pos = annotationPosition.end - sentenceOffset;
            outputNodes.add(annotationContentElement);
        }

        String remaining = text.substring(pos);

        //        if (StringUtils.isNotEmpty(remaining) && remaining.startsWith(" ")) {
        //            outputNodes.add(new Text(" "));
        //        }

        outputNodes.add(new Text(remaining));

        return outputNodes;
    }

    private static MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> aggregateResults(
            MutableTriple<List<Funding>, List<Person>, List<Affiliation>> localEntities,
            MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> globalResult) {
        MutableTriple<List<Funding>, List<Person>, List<Affiliation>> globalEntities = globalResult.getRight();

        List<Funding> localFundings = localEntities.getLeft();
        List<Funding> globalFundings = globalEntities.getLeft();
        globalFundings.addAll(localFundings);
        globalEntities.setLeft(globalFundings);

        List<Person> localPersons = localEntities.getMiddle();
        List<Person> globalPersons = globalEntities.getMiddle();
        globalPersons.addAll(localPersons);
        globalEntities.setMiddle(globalPersons);

        List<Affiliation> localAffiliation = localEntities.getRight();
        List<Affiliation> globalAffiliations = globalEntities.getRight();
        globalAffiliations.addAll(localAffiliation);
        globalEntities.setRight(globalAffiliations);

        globalResult.setRight(globalEntities);

        return globalResult;
    }

    protected static Pair<List<String>, List<OffsetPosition>> extractSentencesAndPositionsFromParagraphElement(
            Element paragraphElement) {
        int offset = 0;
        List<OffsetPosition> sentenceOffsetPositions = new ArrayList<>();

        Nodes sentences = paragraphElement.query("//s");
        List<String> sentencesAsString = new ArrayList<>();
        for (Node sentence : sentences) {
            String sentenceText = sentence.getValue();
            sentenceOffsetPositions.add(new OffsetPosition(offset, offset + sentenceText.length()));
            sentencesAsString.add(sentence.getValue());
            offset += sentence.getValue().length();
        }

        return Pair.of(sentencesAsString, sentenceOffsetPositions);
    }

    /**
     * The processing here is called from the header and/or full text parser in cascade
     * when one of these higher-level model detect a "funding" section, or in case
     * no funding section is found, when a acknowledgements section is detected.
     *
     * Independently from the place this parser is called, it process the input sequence
     * of layout tokens in a context free manner.
     *
     * The expected input here is a paragraph.
     *
     *     // This returns a Element of the annotation and the position where should be injected, relative to the paragraph.
     *     // TODO: make new data objects for the annotations
     *
     * Return an XML fragment with inline annotations of the input text, together with
     * extracted normalized entities. These entities are referenced by the inline
     * annotations with the usual @target attribute pointing to xml:id.
     */
    protected MutablePair<List<AnnotatedXMLElement>, FundingAcknowledgmentParse> getExtractionResult(
            List<LayoutToken> tokensParagraph,
            String labellingResult) {
        List<Funding> fundings = new ArrayList<>();
        List<Person> persons = new ArrayList<>();
        List<Affiliation> affiliations = new ArrayList<>();
        List<Affiliation> institutions = new ArrayList<>();

        FundingAcknowledgmentParse parsedStatement = new FundingAcknowledgmentParse();
        parsedStatement.setFundings(fundings);
        parsedStatement.setPersons(persons);
        parsedStatement.setAffiliations(affiliations);

        // current funding
        Funding funding = new Funding();

        // current person
        Person person = new Person();

        // current organization
        Affiliation affiliation = new Affiliation();
        Affiliation institution = new Affiliation();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FUNDING_ACKNOWLEDGEMENT,
                labellingResult, tokensParagraph);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        TaggingLabel previousLabel = null;

        List<Element> elements = new ArrayList<>();
        List<OffsetPosition> positions = new ArrayList<>();

        int posTokenization = 0;
        int posCharacters = 0;

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            boolean spaceBefore = false;
            if (posTokenization > 0
                    && tokensParagraph.size() >= posTokenization
                    && tokensParagraph.get(posTokenization - 1).getText().equals(" ")) {
                spaceBefore = true;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            List<LayoutToken> tokens = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(tokens));

            if (clusterLabel.equals(FUNDING_OTHER)) {
                posTokenization += tokens.size();
                posCharacters += clusterContent.length();
                continue;
            }

            // We adjust the end position when the entity ends with a space
            int endPosTokenization = posTokenization + tokens.size();
            if (Iterables.getLast(tokens).getText().equals(" ")) {
                endPosTokenization -= 1;
            }

            int endPosCharacters = posCharacters + clusterContent.length();
            if (Iterables.getLast(tokens).getText().equals(" ")) {
                endPosCharacters -= 1;
            }

            if (clusterLabel.equals(FUNDING_FUNDER_NAME)) {
                Funder localFunder = funding.getFunder();
                if (localFunder == null) {
                    localFunder = new Funder();
                    funding.setFunder(localFunder);
                }

                if (StringUtils.isNotBlank(localFunder.getFullName())) {
                    if (funding.isValid()) {
                        fundings.add(funding);
                        // next funding object
                        funding = new Funding();
                        localFunder = new Funder();
                        funding.setFunder(localFunder);
                    }
                }

                localFunder.setFullName(clusterContent);
                localFunder.appendFullNameLayoutTokens(tokens);
                localFunder.addLayoutTokens(tokens);
                funding.addLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "funder"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));
            } else if (clusterLabel.equals(FUNDING_GRANT_NAME)) {
                if (StringUtils.isNotBlank(funding.getGrantName())) {
                    if (funding.isValid()) {
                        fundings.add(funding);
                        // next funding object
                        funding = new Funding();
                    }
                }

                funding.setGrantName(clusterContent);
                funding.appendGrantNameLayoutTokens(tokens);
                funding.addLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "grantName"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_PERSON)) {
                if (StringUtils.isNotBlank(person.getRawName())) {
                    if (person.isValid()) {
                        persons.add(person);
                        // next funding object
                        person = new Person();
                    }
                }

                person.setRawName(clusterContent);
                person.appendLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "person"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_AFFILIATION)) {
                if (StringUtils.isNotBlank(affiliation.getAffiliationString())) {
                    if (affiliation.isNotNull()) {
                        affiliations.add(affiliation);
                        // next funding object
                        affiliation = new Affiliation();
                    }
                }

                affiliation.setRawAffiliationString(clusterContent);
                affiliation.appendLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "affiliation"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_INSTITUTION)) {
                if (StringUtils.isNotBlank(institution.getAffiliationString())) {
                    //if (institution.isNotNull()) {
                    institutions.add(institution);
                    // next funding object
                    institution = new Affiliation();
                    //}
                }

                institution.setAffiliationString(clusterContent);
                institution.appendLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "institution"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_INFRASTRUCTURE)) {
                if (StringUtils.isNotBlank(institution.getAffiliationString())) {
                    //if (institution.isNotNull()) {
                    institutions.add(institution);
                    // next funding object
                    institution = new Affiliation();
                    //}
                }
                institution.setAffiliationString(clusterContent);
                institution.appendLayoutTokens(tokens);
                institution.setInfrastructure(true);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "institution"));
                entity.addAttribute(new Attribute("subtype", "infrastructure"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_GRANT_NUMBER)) {
                Funding previousFounding = null;
                if (StringUtils.isNotBlank(funding.getGrantNumber())) {
                    if (funding.isValid()) {
                        previousFounding = funding;
                        fundings.add(funding);
                        // next funding object
                        funding = new Funding();
                    }
                }

                funding.setGrantNumber(clusterContent);
                funding.appendGrantNumberLayoutTokens(tokens);
                funding.addLayoutTokens(tokens);

                // possibly copy funder from previous funding object (case of "factorization" of grant numbers)
                if (previousFounding != null &&
                        previousFounding.getGrantNumber() != null &&
                        clusterContent.length() == previousFounding.getGrantNumber().length()) {
                    funding.setFunder(previousFounding.getFunder());
                }

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "grantNumber"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_PROGRAM_NAME)) {
                if (StringUtils.isNotBlank(funding.getProgramFullName())) {
                    if (funding.isValid()) {
                        fundings.add(funding);
                        // next funding object
                        funding = new Funding();
                    }
                }

                funding.setProgramFullName(clusterContent);
                funding.appendProgramFullNameLayoutTokens(tokens);
                funding.addLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "programName"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else if (clusterLabel.equals(FUNDING_PROJECT_NAME)) {
                if (StringUtils.isNotBlank(funding.getProjectFullName())) {
                    if (funding.isValid()) {
                        fundings.add(funding);
                        // next funding object
                        funding = new Funding();
                    }
                }

                funding.setProjectFullName(clusterContent);
                funding.appendProjectFullNameLayoutTokens(tokens);
                funding.addLayoutTokens(tokens);

                Element entity = teiElement("rs");
                entity.addAttribute(new Attribute("type", "projectName"));
                entity.appendChild(clusterContent);
                elements.add(entity);

                positions.add(new OffsetPosition(posTokenization, endPosTokenization));

            } else {
                LOGGER.warn("Unexpected funding model label - " + clusterLabel.getLabel() + " for " + clusterContent);
            }

            previousLabel = clusterLabel;
            posTokenization += tokens.size();
            posCharacters += clusterContent.length();
        }

        // last funding, person, institution/affiliation
        if (person.isValid()) {
            persons.add(person);
        }

        if (funding.isValid()) {
            fundings.add(funding);
        }

        if (institution.isNotNull())
            institutions.add(institution);

        if (affiliation.isNotNull())
            affiliations.add(affiliation);

        if (CollectionUtils.isNotEmpty(institutions)) {
            affiliations.addAll(institutions);
        }

        for (Funding localFunding : fundings) {
            localFunding.inferAcronyms();
        }

        List<AnnotatedXMLElement> annotations = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            annotations.add(new AnnotatedXMLElement(elements.get(i), positions.get(i)));
        }

        return MutablePair.of(annotations, parsedStatement);
    }

    /**
     * The training data creation is called from the full text training creation in cascade.
     */
    public Pair<String, String> createTrainingData(
            List<LayoutToken> tokenizations,
            String id) {
        String res = null;
        String featureVector = null;
        try {
            featureVector = FeaturesVectorFunding.addFeatures(tokenizations, null);
            res = label(featureVector);
        } catch (Exception e) {
            LOGGER.error("Sequence labeling in FundingParser fails.", e);
        }
        if (res == null) {
            return Pair.of(null, featureVector);
        }

        List<Pair<String, String>> labeled = GenericTaggerUtils.getTokensAndLabels(res);
        StringBuilder sb = new StringBuilder();

        int tokPtr = 0;
        boolean addSpace = false;
        String lastTag = null;
        boolean fundOpen = false;
        for (Pair<String, String> l : labeled) {
            String tok = l.getLeft();
            String label = l.getRight();

            int tokPtr2 = tokPtr;
            for (; tokPtr2 < tokenizations.size(); tokPtr2++) {
                if (tokenizations.get(tokPtr2).getText().equals(" ")) {
                    addSpace = true;
                } else if (tokenizations.get(tokPtr2).getText().equals("\n") ||
                        tokenizations.get(tokPtr).getText().equals("\r")) {
                    addSpace = true;
                } else {
                    break;
                }
            }
            tokPtr = tokPtr2;

            if (tokPtr >= tokenizations.size()) {
                LOGGER.error("Implementation error: Reached the end of tokenizations, but current token is " + tok);
                // we add a space to avoid concatenated text
                addSpace = true;
            } else {
                String tokenizationToken = tokenizations.get(tokPtr).getText();

                if ((tokPtr != tokenizations.size()) && !tokenizationToken.equals(tok)) {
                    // and we add a space by default to avoid concatenated text
                    addSpace = true;
                    if (!tok.startsWith(tokenizationToken)) {
                        // this is a very exceptional case due to a sequence of accent/diacresis, in this case we skip
                        // a shift in the tokenizations list and continue on the basis of the labeled token
                        // we check one ahead
                        tokPtr++;
                        tokenizationToken = tokenizations.get(tokPtr).getText();
                        if (!tok.equals(tokenizationToken)) {
                            // we try another position forward (second hope!)
                            tokPtr++;
                            tokenizationToken = tokenizations.get(tokPtr).getText();
                            if (!tok.equals(tokenizationToken)) {
                                // we try another position forward (last hope!)
                                tokPtr++;
                                tokenizationToken = tokenizations.get(tokPtr).getText();
                                if (!tok.equals(tokenizationToken)) {
                                    // we return to the initial position
                                    tokPtr = tokPtr - 3;
                                    tokenizationToken = tokenizations.get(tokPtr).getText();
                                    LOGGER.error(
                                            "Implementation error, tokens out of sync: "
                                                    +
                                                    tokenizationToken
                                                    + " != "
                                                    + tok
                                                    + ", at position "
                                                    + tokPtr);
                                }
                            }
                        }
                    }
                    // note: if the above condition is true, this is an exceptional case due to a
                    // sequence of accent/diacresis and we can go on as a full string match
                }
            }

            String plainLabel = GenericTaggerUtils.getPlainLabel(label);

            String output = null;
            if (lastTag != null) {
                testClosingTag(sb, plainLabel, lastTag, addSpace);
            }

            output = writeField(label, lastTag, tok, "<funderFull>", "<fundingAgency>", addSpace, 3);
            String fundingOpening = "\t\t<funding>\n";
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<funderAbbrv>", "<fundingAgency>", addSpace, 3);
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<grantNumber>", "<grantNumber>", addSpace, 3);
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<projectFull>", "<projectName>", addSpace, 3);
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<projectAbbrv>", "<projectName>", addSpace, 3);
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }
            output = writeField(label, lastTag, tok, "<url>", "<url>", addSpace, 3);
            if (output != null) {
                if (!fundOpen) {
                    sb.append(fundingOpening);
                    fundOpen = true;
                }
                sb.append(output);
            }

            lastTag = plainLabel;
            addSpace = false;
            tokPtr++;
        }

        if (fundOpen) {
            testClosingTag(sb, "", lastTag, addSpace);
            sb.append("\t\t</funding>\n");
        }

        return Pair.of(sb.toString(), featureVector);
    }

    public String getTEIHeader(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<tei>\n"
                        +
                        "    <teiHeader>\n"
                        +
                        "        <fileDesc xml:id=\"_"
                        + id
                        + "\"/>\n"
                        +
                        "    </teiHeader>\n"
                        +
                        "    <text xml:lang=\"en\">\n");
        return sb.toString();
    }

    private boolean testClosingTag(
            StringBuilder buffer,
            String currentTag,
            String lastTag,
            boolean addSpace) {
        boolean res = false;
        if (!currentTag.equals(lastTag)) {
            res = true;
            // we close the current tag
            if (lastTag.equals("<funderFull>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("<funderFull>\n");
            } else if (lastTag.equals("<funderAbbrv>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</funderAbbrv>\n");
            } else if (lastTag.equals("<grantNumber>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</grantNumber>\n");
            } else if (lastTag.equals("<projectFull>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</projectFull>\n");
            } else if (lastTag.equals("<projectAbbrv>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</projectAbbrv>\n");
            } else if (lastTag.equals("<url>")) {
                if (addSpace)
                    buffer.append(" ");
                buffer.append("</url>\n");
            } else {
                res = false;
            }
        }
        return res;
    }

    private String writeField(
            String currentTag,
            String lastTag,
            String token,
            String field,
            String outField,
            boolean addSpace,
            int nbIndent) {
        String result = null;
        if (currentTag.endsWith(field)) {
            /*if (currentTag.endsWith("<other>") || currentTag.endsWith("<content>")) {
                result = "";
                if (currentTag.startsWith("I-") || (lastTag == null)) {
                    result += "\n";
                    for (int i = 0; i < nbIndent; i++) {
                        result += "    ";
                    }
                }
                if (addSpace)
                    result += " ";
                result += TextUtilities.HTMLEncode(token);
            }
            else*/
            if ((lastTag != null) && currentTag.endsWith(lastTag)) {
                result = "";
                if (addSpace)
                    result += " ";
                if (currentTag.startsWith("I-"))
                    result += outField;
                result += TextUtilities.HTMLEncode(token);
            } else {
                result = "";
                if (addSpace)
                    result += " ";
                result += "\n";
                if (outField.length() > 0) {
                    for (int i = 0; i < nbIndent; i++) {
                        result += "    ";
                    }
                }

                result += outField + TextUtilities.HTMLEncode(token);
            }
        }
        return result;
    }

}
