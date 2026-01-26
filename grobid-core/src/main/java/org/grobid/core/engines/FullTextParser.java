package org.grobid.core.engines;

import com.google.common.collect.Iterables;
import nu.xom.Element;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.*;
import org.grobid.core.document.*;
import org.grobid.core.engines.citations.CalloutAnalyzer;
import org.grobid.core.engines.citations.CalloutAnalyzer.MarkerType;
import org.grobid.core.engines.citations.LabeledReferenceResult;
import org.grobid.core.engines.citations.ReferenceSegmenter;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.counters.CitationParserCounters;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.exceptions.GrobidResourceException;
import org.grobid.core.features.FeatureFactory;
import org.grobid.core.features.FeaturesVectorFulltext;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.*;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.*;
import org.grobid.core.utilities.matching.EntityMatcherException;
import org.grobid.core.utilities.matching.ReferenceMarkerMatcher;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.*;
import static org.grobid.core.GrobidModels.FULLTEXT;
import static org.grobid.core.engines.label.TaggingLabels.FIGURE_LABEL;
import static org.grobid.core.utilities.LabelUtils.postProcessFullTextLabeledText;

public class FullTextParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullTextParser.class);

    protected File tmpPath = null;

    // default bins for relative position
    private static final int NBBINS_POSITION = 12;

    // default bins for inter-block spacing
    private static final int NBBINS_SPACE = 5;

    // default bins for block character density
    private static final int NBBINS_DENSITY = 5;

    // projection scale for line length
    private static final int LINESCALE = 10;

    protected EngineParsers parsers;

    public FullTextParser(EngineParsers parsers) {
        this(parsers, null);
    }

    public FullTextParser(EngineParsers parsers, GrobidModels.Flavor flavor) {
        super(GrobidModels.getModelFlavor(FULLTEXT, flavor));
        this.parsers = parsers;
        tmpPath = GrobidProperties.getTempPath();
    }


    public Document processing(File inputPdf,
                               GrobidAnalysisConfig config) throws Exception {
        return processing(inputPdf, null, config);
    }

    public Document processing(File inputPdf,
                               String md5Str,
                               GrobidAnalysisConfig config) throws Exception {
        return processing(inputPdf, null, md5Str, config);
    }

    public Document processing(File inputPdf,
                               GrobidModels.Flavor flavor,
                               String md5Str,
                               GrobidAnalysisConfig config) throws Exception {
        DocumentSource documentSource =
            DocumentSource.fromPdf(inputPdf, config.getStartPage(), config.getEndPage(),
                config.getPdfAssetPath() != null, true, false);
        documentSource.setMD5(md5Str);
        return processing(documentSource, flavor, config);
    }

    public Document processingHeaderFunding(File inputPdf,
                                            GrobidAnalysisConfig config) throws Exception {
        DocumentSource documentSource =
            DocumentSource.fromPdf(inputPdf, config.getStartPage(), config.getEndPage(),
                config.getPdfAssetPath() != null, true, false);
        return processingHeaderFunding(documentSource, config);
    }

    public Document processingHeaderFunding(File inputPdf,
                                            String md5Str,
                                            GrobidAnalysisConfig config) throws Exception {
        DocumentSource documentSource =
            DocumentSource.fromPdf(inputPdf, config.getStartPage(), config.getEndPage(),
                config.getPdfAssetPath() != null, true, false);
        documentSource.setMD5(md5Str);
        return processingHeaderFunding(documentSource, config);
    }

    /**
     * Machine-learning recognition of the complete fulltext structures.
     *
     * @param documentSource input
     * @param config         config
     * @return the document object with built TEI
     */
    public Document processing(DocumentSource documentSource,
                               GrobidAnalysisConfig config) {
        return processing(documentSource, null, config);
    }

    /**
     * Machine-learning recognition of the complete full text structures.
     *
     * @param documentSource input
     * @param flavor         optional model flavor
     * @param config         config
     * @return the document object with built TEI
     */
    public Document processing(DocumentSource documentSource,
                               GrobidModels.Flavor flavor,
                               GrobidAnalysisConfig config) {
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }

        try {
            // general segmentation
            Document doc = parsers.getSegmentationParser(flavor).processing(documentSource, config);
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(SegmentationLabels.BODY);

            // header processing
            BiblioItem headerResults = new BiblioItem();
            Pair<String, LayoutTokenization> featSeg = null;

            // using the segmentation model to identify the header zones
            parsers.getHeaderParser(flavor).processingHeaderSection(config, doc, headerResults, false);

            // The commented part below makes use of the PDF embedded metadata (the so-called XMP) if available 
            // as fall back to set author and title if they have not been found. 
            // However tests on PMC set 1942 did not improve recognition. This will have to be re-evaluated with
            // another, more diverse, testing set and with further updates of the header model. 

            // ---> DO NOT DELETE !
            
            /*if (isBlank(resHeader.getTitle()) || isBlank(resHeader.getAuthors()) || CollectionUtils.isEmpty(resHeader.getFullAuthors())) {
                // try to exploit PDF embedded metadata (the so-called XMP) if we are still without title/authors
                // this is risky as those metadata are highly unreliable, but as last chance, why not :)
                Metadata metadata = doc.getMetadata();
                if (metadata != null) { 
                    boolean titleUpdated = false;
                    boolean authorsUpdated = false;

                    if (isNotBlank(metadata.getTitle()) && isBlank(resHeader.getTitle())) {
                        if (!endsWithAny(lowerCase(metadata.getTitle()), ".doc", ".pdf", ".tex", ".dvi", ".docx", ".odf", ".odt", ".txt")) {
                            resHeader.setTitle(metadata.getTitle());
                            titleUpdated = true;
                        }
                    }

                    if (isNotBlank(metadata.getAuthor())
                        && (isBlank(resHeader.getAuthors()) || CollectionUtils.isEmpty(resHeader.getFullAuthors()))) {
                        resHeader.setAuthors(metadata.getAuthor());
                        resHeader.setOriginalAuthors(metadata.getAuthor());
                        authorsUpdated = true;
                        List<Person> localAuthors = parsers.getAuthorParser().processingHeader(metadata.getAuthor());
                        if (localAuthors != null) {
                            for (Person pers : localAuthors) {
                                resHeader.addFullAuthor(pers);
                            }
                        }
                    }

                    // if title and author have been updated with embedded PDF metadata, we try to consolidate 
                    // again as required 
                    if ( titleUpdated || authorsUpdated ) {
                        parsers.getHeaderParser().consolidateHeader(resHeader, config.getConsolidateHeader());
                    }
                }
            }*/

            // structure the abstract using the fulltext model
            if (isNotBlank(headerResults.getAbstract())) {
                //List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                List<LayoutToken> abstractTokens = headerResults.getAbstractTokensWorkingCopy();
                if (CollectionUtils.isNotEmpty(abstractTokens)) {
                    abstractTokens = BiblioItem.cleanAbstractLayoutTokens(abstractTokens);
                    Pair<String, List<LayoutToken>> abstractProcessed = processShort(abstractTokens, doc);
                    if (abstractProcessed != null) {
                        // neutralize figure and table annotations (will be considered as paragraphs)
                        String labeledAbstract = abstractProcessed.getLeft();
                        labeledAbstract = postProcessFullTextLabeledText(labeledAbstract);
                        headerResults.setLabeledAbstract(labeledAbstract);
                        headerResults.setLayoutTokensForLabel(abstractProcessed.getRight(), TaggingLabels.HEADER_ABSTRACT);
                    }
                }
            }

            // citation processing
            // consolidation, if selected, is not done individually for each citation but 
            // in a second stage for all citations which is much faster
            List<BibDataSet> resCitations = parsers.getCitationParser().
                processingReferenceSection(doc, parsers.getReferenceSegmenterParser(), 0);

            // consolidate the set
            if (config.getConsolidateCitations() != 0 && resCitations != null) {
                Consolidation consolidator = Consolidation.getInstance();
                if (consolidator.getCntManager() == null)
                    consolidator.setCntManager(Engine.getCntManager());
                try {
                    Map<Integer, BiblioItem> resConsolidation = consolidator.consolidate(resCitations);
                    for (int i = 0; i < resCitations.size(); i++) {
                        BiblioItem resCitation = resCitations.get(i).getResBib();
                        BiblioItem bibo = resConsolidation.get(i);
                        if (bibo != null) {
                            if (config.getConsolidateCitations() == 1)
                                BiblioItem.correct(resCitation, bibo);
                            else if (config.getConsolidateCitations() == 2)
                                BiblioItem.injectIdentifiers(resCitation, bibo);
                        }
                    }
                } catch (Exception e) {
                    throw new GrobidException(
                        "An exception occurred while running consolidation on bibliographical references.", e);
                }
            }
            doc.setBibDataSets(resCitations);

            // full text processing
            featSeg = getBodyTextFeatured(doc, documentBodyParts);
            String bodyResults = null;
            LayoutTokenization bodyTokenization = null;
            List<Figure> bodyFigures = null;
            List<Table> bodyTables = null;
            List<Equation> bodyEquations = null;
            if (featSeg != null && isNotBlank(featSeg.getLeft())) {
                // if featSeg is null, it usually means that the fulltext body is not found in the
                // document segmentation
                String bodyText = featSeg.getLeft();
                bodyTokenization = featSeg.getRight();
                //tokenizationsBody = featSeg.getB().getTokenization();
                //layoutTokensBody = featSeg.getB().getLayoutTokens();

                bodyResults = label(bodyText);
                //Correct subsequent I-<figure> or I-<table>
                bodyResults = LabelUtils.postProcessFulltextFixInvalidTableOrFigure(bodyResults);

                // we apply now the figure and table models based on the fulltext labeled output
                bodyFigures = processFigures(bodyResults, bodyTokenization.getTokenization());
                doc.setFigures(bodyFigures);

                bodyResults = fixFiguresLabellingResults(doc, bodyResults);

                // Figures

                postProcessFigureCaptions(bodyFigures, doc);

                long numberFiguresFulltextModel = Arrays.stream(bodyResults.split("\n"))
                    .filter(r -> r.endsWith("I-" + FIGURE_LABEL))
                    .count();

                List<Figure> badBodyFigures = getBadFigures(bodyFigures);
                bodyResults = revertResultsForBadItems(
                    badBodyFigures,
                    bodyResults,
                    !(bodyFigures.size() > numberFiguresFulltextModel)
                );

                // Filter bad figures and recompute IDs in a single stream operation
                final AtomicInteger figureIndex = new AtomicInteger(0);
                bodyFigures = bodyFigures.stream()
                    .filter(f -> !badBodyFigures.contains(f))
                    .peek(f -> f.setId(String.valueOf(figureIndex.getAndIncrement())))
                    .collect(Collectors.toList());

                doc.setFigures(bodyFigures);

                // Tables
                bodyTables = processTables(bodyResults, bodyTokenization.getTokenization(), doc);

                //We deal with tables considered bad by reverting them as <paragraph>, to reduce the risk them to be
                // dropped later on.

                //TODO: double check the way the tables are validated
                long numberTablesFulltextModel = Arrays.stream(bodyResults.split("\n"))
                    .filter(r -> r.endsWith("I-" + TaggingLabels.TABLE_LABEL))
                .count();

                List<Table> badBodyTables = getBadTables(bodyTables);
                bodyResults = revertResultsForBadItems(badBodyTables, bodyResults, !(bodyTables.size() > numberTablesFulltextModel));

                final AtomicInteger tableIndex = new AtomicInteger(0);
                bodyTables = bodyTables.stream()
                    .filter(t -> !badBodyTables.contains(t))
                    .peek(f -> f.setId(String.valueOf(tableIndex.getAndIncrement())))
                    .collect(Collectors.toList());

                postProcessTableCaptions(bodyTables, doc);
                doc.setTables(bodyTables);

                // Processing equations
                bodyEquations = processEquations(bodyResults, bodyTokenization.getTokenization());
                doc.setEquations(bodyEquations);
            } else {
                LOGGER.debug("Fulltext model: The featured body is empty");
            }

            // possible annexes (view as a piece of full text similar to the body)
            documentBodyParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            featSeg = getBodyTextFeatured(doc, documentBodyParts);
            String annexResults = null;
            List<Figure> annexFigures = null;
            List<Table> annexTables = null;
            List<Equation> annexEquations = null;
            List<LayoutToken> annexTokenization = null;
            if (featSeg != null && isNotEmpty(trim(featSeg.getLeft()))) {
                // if featSeg is null, it usually means that no annex segment is found in the
                // document segmentation
                String annexFeatures = featSeg.getLeft();
                annexTokenization = featSeg.getRight().getTokenization();
                annexResults = label(annexFeatures);
                //System.out.println(rese);

                annexFigures = processFigures(annexResults, annexTokenization, CollectionUtils.size(bodyFigures));

                long numberFiguresInAnnex = Arrays.stream(annexResults.split("\n"))
                    .filter(r -> r.endsWith("I-" + FIGURE_LABEL))
                    .count();

                List<Figure> badAnnexFigures = getBadFigures(annexFigures);

                if (CollectionUtils.isNotEmpty(badAnnexFigures)) {
                    LOGGER.info("Number of figures badly formatted or incomplete we identified in Annex: " + badAnnexFigures.size());
                }
                annexResults = revertResultsForBadItems(
                    badAnnexFigures,
                    annexResults,
                    !(annexFigures.size() > numberFiguresInAnnex)
                );

                annexFigures = annexFigures.stream()
                    .filter(f -> !badAnnexFigures.contains(f))
                    .collect(Collectors.toList());
                postProcessFigureCaptions(annexFigures, doc);

                doc.setAnnexFigures(annexFigures);

                annexTables = processTables(annexResults, annexTokenization, doc, CollectionUtils.size(bodyTables));

                long numberTablesInAnnex = Arrays.stream(annexResults.split("\n"))
                    .filter(r -> r.endsWith("I-" + TaggingLabels.TABLE_LABEL))
                    .count();

                List<Table> badAnnexTables = annexTables.stream()
                    .filter(t -> !(t.isCompleteForTEI() && t.validateTable()))
                    .collect(Collectors.toList());

                if (CollectionUtils.isNotEmpty(badAnnexTables)) {
                    LOGGER.info("Number of tables badly formatted or incomplete we identified in Annex: " + badAnnexTables.size());
                }
                annexResults = revertResultsForBadItems(
                    badAnnexTables,
                    annexResults,
                    !(annexTables.size() > numberTablesInAnnex)
                );

                annexTables = annexTables.stream()
                    .filter(t -> !badAnnexTables.contains(t))
                    .collect(Collectors.toList());

                postProcessTableCaptions(annexTables, doc);
                doc.setAnnexTables(annexTables);

                annexEquations = processEquations(
                    annexResults,
                    annexTokenization,
                    CollectionUtils.size(bodyEquations)
                );
                doc.setAnnexEquations(annexEquations);
            }

            // post-process reference and footnote callout to keep them consistent (e.g. for example avoid that a footnote
            // callout in superscript is by error labeled as a numerical reference callout)
            List<MarkerType> markerTypes = null;

            if (bodyResults != null) {
                markerTypes = postProcessCallout(bodyResults, bodyTokenization);
            }

            // final combination
            toTEI(
                doc, // document
                bodyResults, // labeled data for body
                annexResults, // and annex
                bodyTokenization, // tokenization for body
                annexTokenization, // and annex
                headerResults, // header
                bodyFigures,
                bodyTables,
                bodyEquations,
                annexFigures,
                annexTables,
                annexEquations,
                markerTypes,
                config
            );
            return doc;
        } catch (GrobidException e) {
            throw e;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    @NotNull
    private static List<Table> getBadTables(List<Table> tables) {
        List<Table> badTables = tables.stream()
            .filter(t -> !(t.isCompleteForTEI() && t.validateTable()))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(badTables)) {
            LOGGER.info("Number of tables badly formatted or incomplete we identified: " + badTables.size());
        }
        return badTables;
    }

    @NotNull
    private static List<Figure> getBadFigures(List<Figure> figures) {
        List<Figure> badFigures = figures.stream()
            .filter(f -> !f.isCompleteForTEI())
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(badFigures)) {
            LOGGER.info("Number of figures badly formatted or incomplete we identified: " + badFigures.size());
        }
        return badFigures;
    }

    private void postProcessFigureCaptions(List<Figure> figures, Document doc) {
        // further parse the caption
        for(Figure figure : figures) {
            if (CollectionUtils.isNotEmpty(figure.getCaptionLayoutTokens()) ) {
                Pair<String, List<LayoutToken>> processedCaption = processShort(figure.getCaptionLayoutTokens(), doc);
                figure.setLabeledCaption(processedCaption.getLeft());
                List<LayoutToken> processedCaptionLayoutTokens = processedCaption.getRight();
                if (processedCaptionLayoutTokens.size() != figure.getCaptionLayoutTokens().size()) {
                    // We might have a problem, we might loose some tokens during the processing
                    LOGGER.warn("Changes in the figure caption: \noriginal: "
                        + LayoutTokensUtil.toText(figure.getCaptionLayoutTokens())
                        + "\nmodified: " + LayoutTokensUtil.toText(processedCaptionLayoutTokens));
                }
                figure.setCaptionLayoutTokens(processedCaptionLayoutTokens);
            }
        }
    }

    private static String fixFiguresLabellingResults(Document doc, String bodyResults) {
        List<Triple<Figure, Figure, List<List<LayoutToken>>>> updatedFigures = doc.assignGraphicObjectsToFigures();
        for(Triple<Figure, Figure, List<List<LayoutToken>>> update: updatedFigures) {
            List<List<LayoutToken>> difference = update.getRight();

            long nbDifferences = difference.stream()
                .filter(llt -> !llt.isEmpty())
                .count();

            if (nbDifferences > 0) {
                // In this case we assume they are figures
                String updatedBodyResult = revertDiscardedTokensInMainResults(difference, bodyResults);
                bodyResults = updatedBodyResult;
            }
        }
        return bodyResults;
    }

    static String revertResultsForBadItems(List<? extends Figure> badFiguresOrTables, String resultBody) {
        return revertResultsForBadItems(badFiguresOrTables, resultBody, true);
    }

    /**
     * Revert bad figures or tables (that will likely be dropped later on) to paragraphs.
     * The algorithm first search for candidate index in the resultBody sequence, then it tries to match the sequence
     * of tokens of the bad figure or table with the sequence of tokens in the resultBody. If a match is found, then
     * the figure or table is reverted to a sequence of paragraphs.
     * <p>
     * At the moment the algorithm does not consider possible reference markers in the sequence of tokens.
     */
    static String revertResultsForBadItems(List<? extends Figure> badFiguresOrTables, String resultBody, boolean strict) {
        //LF: we update the resultBody sequence by reverting these figure or tables as <paragraph> elements
        if (CollectionUtils.isNotEmpty(badFiguresOrTables)) {

            List<List<String>> labelledResultsAsList = Arrays.stream(resultBody.split("\n"))
                .map(l -> Arrays.stream(l.split("\t")).collect(Collectors.toList()))
                .collect(Collectors.toList());

            for (Figure badItem : badFiguresOrTables) {
                if (badItem == null) {
                    continue;
                }
                String itemLabel = badItem instanceof Table ? TaggingLabels.TABLE_LABEL : FIGURE_LABEL;
                // Find the index of the first layoutToken of the table in the tokenization
                List<LayoutToken> layoutTokenItem = badItem.getLayoutTokens();
                List<Integer> candidateIndexes = findCandidateIndex(layoutTokenItem, labelledResultsAsList,
                    itemLabel, strict);
                if (candidateIndexes.isEmpty()) {
                    LOGGER.warn("Cannot find the candidate index for fixing the figures/tables. Tokens: " + LayoutTokensUtil.toText(layoutTokenItem));
                    continue;
                }

                // At this point I have more than one candidate, which can be matched if the same first
                // token is repeated in the sequence. The next step is to find the matching figure/table
                // using a large sequence

                List<String> sequenceTokenItemWithoutSpaces = layoutTokenItem.stream()
                    .map(LayoutToken::getText)
                    .map(StringUtils::strip)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());

                //TODO: reduce candidate indexes after matching one sequence
                int resultIndexCandidate = consolidateResultCandidateThroughSequence(candidateIndexes, labelledResultsAsList, sequenceTokenItemWithoutSpaces);

                if (resultIndexCandidate > -1) {
                    boolean first = true;
                    for (int i = resultIndexCandidate; i < Math.min(resultIndexCandidate + sequenceTokenItemWithoutSpaces.size(), labelledResultsAsList.size()); i++) {
                        List<String> line = labelledResultsAsList.get(i);
                        String label = Iterables.getLast(line);
                        if (first) {
                            first = false;
                        } else {
                            if (label.startsWith("I-")) {
                                break;
                            }
                        }
                        line.set(line.size() - 1, label.replace(itemLabel, TaggingLabels.PARAGRAPH_LABEL));
                    }
                } else {
                    LOGGER.warn("Cannot find the result index candidate for fixing the figure/table. Tokens: " + LayoutTokensUtil.toText(layoutTokenItem));
                }
            }

            String updatedResultBody = labelledResultsAsList.stream()
                .map(l -> String.join("\t", l))
                .collect(Collectors.joining("\n"));

            resultBody = updatedResultBody;
        }
        return resultBody;
    }

    static String revertDiscardedTokensInMainResults(List<List<LayoutToken>> layoutTokenPieces, String resultBody) {
        return revertDiscardedTokensInMainResults(layoutTokenPieces, resultBody, FIGURE_LABEL);
    }

    static String revertDiscardedTokensInMainResults(List<List<LayoutToken>> layoutTokenPieces, String resultBody, String itemLabel) {
        //LF: we update the resultBody sequence by reverting the tokens as <paragraph> elements
        if (CollectionUtils.isNotEmpty(layoutTokenPieces)) {
            List<List<String>> labelledResultsAsList = Arrays.stream(resultBody.split("\n"))
                .map(l -> Arrays.stream(l.split("\t")).collect(Collectors.toList()))
                .collect(Collectors.toList());

            for (List<LayoutToken> tokens : layoutTokenPieces) {
                // Find the index of the first layoutToken of the table in the tokenization
                // False because we might have random sequences of tokens within the figures
                List<Integer> candidateIndexes = findCandidateIndex(tokens, labelledResultsAsList, itemLabel, false);
                if (candidateIndexes.isEmpty()) {
                    LOGGER.warn("Cannot find the candidate index for fixing the results. Tokens: " + LayoutTokensUtil.toText(tokens));
                    continue;
                }

                // At this point I have more than one candidate, which can be matched if the same first
                // token is repeated in the sequence. The next step is to find the matching figure/table
                // using a large sequence

                List<String> sequenceTokenItemWithoutSpaces = tokens.stream()
                    .map(LayoutToken::getText)
                    .map(StringUtils::strip)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());

                //TODO: reduce candidate indexes after matching one sequence
                int resultIndexCandidate = consolidateResultCandidateThroughSequence(candidateIndexes, labelledResultsAsList, sequenceTokenItemWithoutSpaces);

                if (resultIndexCandidate > -1) {
                    boolean first = true;
                    for (int i = resultIndexCandidate; i < Math.min(resultIndexCandidate + sequenceTokenItemWithoutSpaces.size(), labelledResultsAsList.size()); i++) {
                        List<String> line = labelledResultsAsList.get(i);
                        String label = Iterables.getLast(line);
                        if (first) {
                            first = false;
                            if (!label.startsWith("I-")) {
                                line.set(line.size() - 1, label.replace(itemLabel, "I-" + TaggingLabels.PARAGRAPH_LABEL));
                                continue;
                            }
                        } else {
                            if (label.startsWith("I-")) {
                                break;
                            }
                        }
                        line.set(line.size() - 1, label.replace(itemLabel, TaggingLabels.PARAGRAPH_LABEL));
                    }
                } else {
                    LOGGER.warn("Cannot find the result index candidate for fixing the results. Tokens " + LayoutTokensUtil.toText(tokens));
                }
            }

            String updatedResultBody = labelledResultsAsList.stream()
                .map(l -> String.join("\t", l))
                .collect(Collectors.joining("\n"));

            resultBody = updatedResultBody;
        }
        return resultBody;
    }


    static int consolidateResultCandidateThroughSequence(List<Integer> candidateIndexes, List<List<String>> splitResult, List<String> tokensNoSpaceItem) {
        int resultIndexCandidate = -1;
        if (candidateIndexes.size() == 1) {
            resultIndexCandidate = candidateIndexes.get(0);
        } else {
            for (int candidateIndex : candidateIndexes) {
                List<String> candidateTable = splitResult.subList(candidateIndex, Math.min(candidateIndex + tokensNoSpaceItem.size(), splitResult.size()))
                    .stream()
                    .map(i -> i.get(0))
                    .collect(Collectors.toList());

                String candidateTableText = String.join("", candidateTable);
                String tokensText = String.join("", tokensNoSpaceItem);

                if (candidateTableText.equals(tokensText)) {
                    resultIndexCandidate = candidateIndex;
                    break;
                }
            }
        }
        return resultIndexCandidate;
    }

    /**
     * Find a set of candidates representing the indexes from the labelledResults which could correspond
     * to the first token of the figure/table
     * <p>
     * strict = True then it will check the items related to I-<table> or I-<figure> first
     * and then the <table> or <figure> only if there are no candidates
     * strict = False is usually necessary if there are more tables than I- token, this because a figure/table could be
     * identified within the sequence initially provided by the fulltext model
     */
    @NotNull
    static List<Integer> findCandidateIndex(List<LayoutToken> layoutTokenItem, List<List<String>> labelledResultsAsList, String itemLabel) {
        return findCandidateIndex(layoutTokenItem, labelledResultsAsList, itemLabel, true);
    }

    @NotNull
    static List<Integer> findCandidateIndex(List<LayoutToken> layoutTokenItem, List<List<String>> labelledResultsAsList, String itemLabel, boolean strict) {
        LayoutToken firstLayoutTokenItem = layoutTokenItem.get(0);

        List<Integer> candidateIndexes = new ArrayList<>();
        if (strict) {
            candidateIndexes = IntStream.range(0, labelledResultsAsList.size())
                .filter(i -> labelledResultsAsList.get(i).get(0).equals(firstLayoutTokenItem.getText())
                    && Iterables.getLast(labelledResultsAsList.get(i)).equals("I-" + itemLabel))
                .boxed()
                .collect(Collectors.toList());
        } else {
            candidateIndexes = IntStream.range(0, labelledResultsAsList.size())
                .filter(i -> labelledResultsAsList.get(i).get(0).equals(firstLayoutTokenItem.getText())
                    && (
                    Iterables.getLast(labelledResultsAsList.get(i)).equals(itemLabel)
                        || Iterables.getLast(labelledResultsAsList.get(i)).equals("I-" + itemLabel))
                )
                .boxed()
                .collect(Collectors.toList());
        }
        return candidateIndexes;
    }


    /**
     * Machine-learning recognition of full text structures limted to header and funding information.
     * This requires however to look at the complete document, but some parts will be skipped
     *
     * @param documentSource input
     * @param config         config
     * @return the document object with built TEI
     */
    public Document processingHeaderFunding(DocumentSource documentSource,
                                            GrobidAnalysisConfig config) {
        if (tmpPath == null) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        }
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        try {
            // general segmentation
            Document doc = parsers.getSegmentationParser().processing(documentSource, config);

            // header processing
            BiblioItem resHeader = new BiblioItem();
            Pair<String, LayoutTokenization> featSeg = null;

            // using the segmentation model to identify the header zones
            parsers.getHeaderParser().processingHeaderSection(config, doc, resHeader, false);

            // structure the abstract using the fulltext model
            if (isNotBlank(resHeader.getAbstract())) {
                //List<LayoutToken> abstractTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_ABSTRACT);
                List<LayoutToken> abstractTokens = resHeader.getAbstractTokensWorkingCopy();
                if (CollectionUtils.isNotEmpty(abstractTokens)) {
                    abstractTokens = BiblioItem.cleanAbstractLayoutTokens(abstractTokens);
                    Pair<String, List<LayoutToken>> abstractProcessed = processShort(abstractTokens, doc);
                    if (abstractProcessed != null) {
                        // neutralize figure and table annotations (will be considered as paragraphs)
                        String labeledAbstract = abstractProcessed.getLeft();
                        labeledAbstract = postProcessFullTextLabeledText(labeledAbstract);
                        resHeader.setLabeledAbstract(labeledAbstract);
                        resHeader.setLayoutTokensForLabel(abstractProcessed.getRight(), TaggingLabels.HEADER_ABSTRACT);
                    }
                }
            }

            // possible annexes (view as a piece of full text similar to the body)
            /*documentBodyParts = doc.getDocumentPart(SegmentationLabels.ANNEX);
            featSeg = getBodyTextFeatured(doc, documentBodyParts);
            String resultAnnex = null;
            List<LayoutToken> tokenizationsBody2 = null;
            if (featSeg != null && isNotEmpty(trim(featSeg.getLeft()))) {
                // if featSeg is null, it usually means that no body segment is found in the
                // document segmentation
                String bodytext = featSeg.getLeft();
                tokenizationsBody2 = featSeg.getRight().getTokenization();
                resultAnnex = label(bodytext);
            }*/

            // final combination
            toTEIHeaderFunding(doc, // document
                resHeader, // header
                config);
            return doc;
        } catch (GrobidException e) {
            throw e;
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
    }

    /**
     * Process a simple segment of layout tokens with the full text model.
     * Return null if provided Layout Tokens is empty or if structuring failed.
     */
    public Pair<String, List<LayoutToken>> processShortNew(List<LayoutToken> tokens, Document doc) {
        if (CollectionUtils.isEmpty(tokens))
            return null;

        SortedSet<DocumentPiece> documentParts = new TreeSet<DocumentPiece>();
        // identify continuous sequence of layout tokens in the abstract
        int posStartPiece = -1;
        int currentOffset = -1;
        int startBlockPtr = -1;
        LayoutToken previousToken = null;
        for (LayoutToken token : tokens) {
            if (currentOffset == -1) {
                posStartPiece = getDocIndexToken(doc, token);
                startBlockPtr = token.getBlockPtr();
            } else if (token.getOffset() != currentOffset + previousToken.getText().length()) {
                // new DocumentPiece to be added 
                DocumentPointer dp1 = new DocumentPointer(doc, startBlockPtr, posStartPiece);
                DocumentPointer dp2 = new DocumentPointer(doc,
                    previousToken.getBlockPtr(),
                    getDocIndexToken(doc, previousToken));
                DocumentPiece piece = new DocumentPiece(dp1, dp2);
                documentParts.add(piece);

                // set index for the next DocumentPiece
                posStartPiece = getDocIndexToken(doc, token);
                startBlockPtr = token.getBlockPtr();
            }
            currentOffset = token.getOffset();
            previousToken = token;
        }
        // we still need to add the last document piece
        // conditional below should always be true because abstract is not null if we reach this part, but paranoia is good when programming 
        if (posStartPiece != -1) {
            DocumentPointer dp1 = new DocumentPointer(doc, startBlockPtr, posStartPiece);
            DocumentPointer dp2 = new DocumentPointer(doc,
                previousToken.getBlockPtr(),
                getDocIndexToken(doc, previousToken));
            DocumentPiece piece = new DocumentPiece(dp1, dp2);
            documentParts.add(piece);
        }

        Pair<String, LayoutTokenization> featSeg = getBodyTextFeatured(doc, documentParts);
        String res = "";
        List<LayoutToken> layoutTokenization = new ArrayList<>();
        if (featSeg != null) {
            String featuredText = featSeg.getLeft();
            LayoutTokenization layouts = featSeg.getRight();
            if (layouts != null)
                layoutTokenization = layouts.getTokenization();
            if (isNotBlank(featuredText)) {
                res = label(featuredText);
            }
        } else
            return null;

        return Pair.of(res, layoutTokenization);
    }

    public Pair<String, List<LayoutToken>> processShort(List<LayoutToken> tokens, Document doc) {
        if (CollectionUtils.isEmpty(tokens))
            return null;

        SortedSet<DocumentPiece> documentParts = new TreeSet<>();

        // we need to identify all the continuous chunks of tokens, and ignore the others
        List<List<LayoutToken>> tokenChunks = new ArrayList<>();
        List<LayoutToken> currentChunk = new ArrayList<>();
        int currentPos = 0;
        for (LayoutToken token : tokens) {
            if (CollectionUtils.isNotEmpty(currentChunk)) {
                int tokenPos = token.getOffset();
                if (currentPos != tokenPos) {
                    // new chunk
                    tokenChunks.add(currentChunk);
                    currentChunk = new ArrayList<LayoutToken>();
                }
            }
            currentChunk.add(token);
            currentPos = token.getOffset() + token.getText().length();
        }
        // add last chunk
        tokenChunks.add(currentChunk);
        for (List<LayoutToken> chunk : tokenChunks) {
            int endInd = chunk.size() - 1;
            int posStartAbstract = getDocIndexToken(doc, chunk.get(0));
            int posEndAbstract = getDocIndexToken(doc, chunk.get(endInd));
            DocumentPointer dp1 = new DocumentPointer(doc, chunk.get(0).getBlockPtr(), posStartAbstract);
            DocumentPointer dp2 = new DocumentPointer(doc, chunk.get(endInd).getBlockPtr(), posEndAbstract);
            DocumentPiece piece = new DocumentPiece(dp1, dp2);
            documentParts.add(piece);
        }
        Pair<String, LayoutTokenization> featSeg = getBodyTextFeatured(doc, documentParts);
        String res = null;
        List<LayoutToken> layoutTokenization = null;
        if (featSeg != null) {
            String featuredText = featSeg.getLeft();
            LayoutTokenization layouts = featSeg.getRight();
            if (layouts != null)
                layoutTokenization = layouts.getTokenization();
            if (StringUtils.isNotBlank(featuredText)) {
                res = label(featuredText);
                res = postProcessFullTextLabeledText(res);
            }
        }

        return Pair.of(res, layoutTokenization);
    }

    static public Pair<String, LayoutTokenization> getBodyTextFeatured(Document doc,
                                                                       SortedSet<DocumentPiece> documentBodyParts) {
        if (CollectionUtils.isEmpty(documentBodyParts)) {
            return null;
        }
        FeatureFactory featureFactory = FeatureFactory.getInstance();
        StringBuilder fulltext = new StringBuilder();
        String currentFont = null;
        int currentFontSize = -1;

        List<Block> blocks = doc.getBlocks();
        if (CollectionUtils.isEmpty(blocks)) {
            return null;
        }

        // vector for features
        FeaturesVectorFulltext features;
        FeaturesVectorFulltext previousFeatures = null;

        ReferenceMarkerMatcher referenceMarkerMatcher = null;

        // if bibliographical references are available from the bibliographical reference section, we look if we have
        // a numbering associated to the bibliographical references (bib. ref. callout will likely be numerical then)
        String bibRefCalloutType = "UNKNOWN";
        List<BibDataSet> bibDataSets = doc.getBibDataSets();
        if (bibDataSets != null) {
            try {
                referenceMarkerMatcher = doc.getReferenceMarkerMatcher();
                // we look at the exising extracted labels in the bibliographical section (if available and if any) and set
                // the value based on the majority of labels
                int nbNumbType = 0;
                int nbAuthorType = 0;
                for (BibDataSet bibDataSet : bibDataSets) {
                    if ((bibDataSet == null) || (bibDataSet.getRefSymbol() == null))
                        continue;
                    boolean isNumb = referenceMarkerMatcher.isNumberedCitationReference(bibDataSet.getRefSymbol());
                    if (isNumb) {
                        nbNumbType++;
                        continue;
                    }
                    boolean isAuthor = referenceMarkerMatcher.isAuthorCitationStyle(bibDataSet.getRefSymbol());
                    if (isAuthor)
                        nbAuthorType++;
                }
                if (nbNumbType > (bibDataSets.size() / 2))
                    bibRefCalloutType = "NUMBER";
                else if (nbAuthorType > (bibDataSets.size() / 2))
                    bibRefCalloutType = "AUTHOR";
            } catch (EntityMatcherException e) {
                LOGGER.info("Could not build the bibliographical matcher", e);
            }
        }
        boolean endblock;
        boolean endPage = true;
        boolean newPage = true;
        //boolean start = true;
        int mm = 0; // page position
        int nn = 0; // document position
        double lineStartX = Double.NaN;
        boolean indented = false;
        int fulltextLength = 0;
        int pageLength = 0; // length of the current page
        double lowestPos = 0.0;
        double spacingPreviousBlock = 0.0;
        int currentPage = 0;

        List<LayoutToken> layoutTokens = new ArrayList<LayoutToken>();
        fulltextLength = getFulltextLength(doc, documentBodyParts, fulltextLength);

//System.out.println("fulltextLength: " + fulltextLength);

        for (DocumentPiece docPiece : documentBodyParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            //int blockPos = dp1.getBlockPtr();
            for (int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
//System.out.println("blockIndex: " + blockIndex);			
                boolean graphicVector = false;
                boolean graphicBitmap = false;
                Block block = blocks.get(blockIndex);
                // length of the page where the current block is
                double pageHeight = block.getPage().getHeight();
                int localPage = block.getPage().getNumber();
                if (localPage != currentPage) {
                    newPage = true;
                    currentPage = localPage;
                    mm = 0;
                    lowestPos = 0.0;
                    spacingPreviousBlock = 0.0;
                }

	            /*if (start) {
	                newPage = true;
	                start = false;
	            }*/

                boolean newline;
                boolean previousNewline = false;
                endblock = false;

	            /*if (endPage) {
	                newPage = true;
	                mm = 0;
					lowestPos = 0.0;
	            }*/

                if (lowestPos > block.getY()) {
                    // we have a vertical shift, which can be due to a change of column or other particular layout formatting 
                    spacingPreviousBlock = doc.getMaxBlockSpacing() / 5.0; // default
                } else
                    spacingPreviousBlock = block.getY() - lowestPos;

                String localText = block.getText();
                if (TextUtilities.filterLine(localText)) {
                    continue;
                }
	            /*if (localText != null) {
	                if (localText.contains("@PAGE")) {
	                    mm = 0;
	                    // pageLength = 0;
	                    endPage = true;
	                    newPage = false;
	                } else {
	                    endPage = false;
	                }
	            }*/

                // character density of the block
                double density = 0.0;
                if ((block.getHeight() != 0.0) && (block.getWidth() != 0.0) &&
                    (localText != null) && (!localText.contains("@PAGE")) &&
                    (!localText.contains("@IMAGE")))
                    density = (double) localText.length() / (block.getHeight() * block.getWidth());

                // check if we have a graphical object connected to the current block
                List<GraphicObject> localImages = Document.getConnectedGraphics(block, doc);
                if (localImages != null) {
                    for (GraphicObject localImage : localImages) {
                        if (localImage.getType() == GraphicObjectType.BITMAP)
                            graphicBitmap = true;
                        if (localImage.getType() == GraphicObjectType.VECTOR || localImage.getType() == GraphicObjectType.VECTOR_BOX)
                            graphicVector = true;
                    }
                }

                List<LayoutToken> tokens = block.getTokens();
                if (tokens == null) {
                    continue;
                }

                int n = 0;// token position in current block
                if (blockIndex == dp1.getBlockPtr()) {
//					n = dp1.getTokenDocPos() - block.getStartToken();
                    n = dp1.getTokenBlockPos();
                }
                int lastPos = tokens.size();
                // if it's a last block from a document piece, it may end earlier
                if (blockIndex == dp2.getBlockPtr()) {
                    lastPos = dp2.getTokenBlockPos() + 1;
                    if (lastPos > tokens.size()) {
                        LOGGER.warn("DocumentPointer for block " + blockIndex + " points to " +
                            dp2.getTokenBlockPos() + " token, but block token size is " +
                            tokens.size());
                        lastPos = tokens.size();
                    }
                }

                boolean isFirstBlockToken = true;
                while (n < lastPos) {
                    if (blockIndex == dp2.getBlockPtr()) {
                        //if (n > block.getEndToken()) {
                        if (n > dp2.getTokenDocPos() - block.getStartToken()) {
                            break;
                        }
                    }

                    LayoutToken token = tokens.get(n);
                    layoutTokens.add(token);

                    features = new FeaturesVectorFulltext();
                    features.token = token;

                    double coordinateLineY = token.getY();

                    String text = token.getText();
                    if ((text == null) || (text.length() == 0)) {
                        n++;
                        //mm++;
                        //nn++;
                        continue;
                    }
                    //text = text.replaceAll("\\s+", "");
                    text = text.replace(" ", "");
                    if (text.length() == 0) {
                        n++;
                        mm++;
                        nn++;
                        continue;
                    }

                    if (text.equals("\n")) {
                        newline = true;
                        previousNewline = true;
                        n++;
                        mm++;
                        nn++;
                        continue;
                    } else
                        newline = false;

                    // final sanitisation and filtering
                    text = text.replaceAll("[ \n]", "");
                    if (TextUtilities.filterLine(text)) {
                        n++;
                        continue;
                    }

                    if (previousNewline) {
                        newline = true;
                        previousNewline = false;
                        if ((token != null) && (previousFeatures != null)) {
                            double previousLineStartX = lineStartX;
                            lineStartX = token.getX();
                            double characterWidth = token.width / text.length();
                            if (!Double.isNaN(previousLineStartX)) {
                                if (previousLineStartX - lineStartX > characterWidth)
                                    indented = false;
                                else if (lineStartX - previousLineStartX > characterWidth)
                                    indented = true;
                                // Indentation ends if line start is > 1 character width to the left of previous line start
                                // Indentation starts if line start is > 1 character width to the right of previous line start
                                // Otherwise indentation is unchanged
                            }
                        }
                    }

                    features.string = text;

                    if (graphicBitmap) {
                        features.bitmapAround = true;
                    }
                    if (graphicVector) {
                        features.vectorAround = true;
                    }

                    if (newline) {
                        features.lineStatus = "LINESTART";
                        if (token != null)
                            lineStartX = token.getX();
                        // be sure that previous token is closing a line, except if it's a starting line
                        if (previousFeatures != null) {
                            if (!previousFeatures.lineStatus.equals("LINESTART"))
                                previousFeatures.lineStatus = "LINEEND";
                        }
                    }
                    Matcher m0 = featureFactory.isPunct.matcher(text);
                    if (m0.find()) {
                        features.punctType = "PUNCT";
                    }
                    if (text.equals("(") || text.equals("[")) {
                        features.punctType = "OPENBRACKET";

                    } else if (text.equals(")") || text.equals("]")) {
                        features.punctType = "ENDBRACKET";

                    } else if (text.equals(".")) {
                        features.punctType = "DOT";

                    } else if (text.equals(",")) {
                        features.punctType = "COMMA";

                    } else if (text.equals("-")) {
                        features.punctType = "HYPHEN";

                    } else if (text.equals("\"") || text.equals("\'") || text.equals("`")) {
                        features.punctType = "QUOTE";
                    }

                    if (indented) {
                        features.alignmentStatus = "LINEINDENT";
                    } else {
                        features.alignmentStatus = "ALIGNEDLEFT";
                    }

                    if (isFirstBlockToken) {
                        features.lineStatus = "LINESTART";
                        // be sure that previous token is closing a line, except if it's a starting line
                        if (previousFeatures != null) {
                            if (!previousFeatures.lineStatus.equals("LINESTART"))
                                previousFeatures.lineStatus = "LINEEND";
                        }
                        if (token != null)
                            lineStartX = token.getX();
                        features.blockStatus = "BLOCKSTART";
                    } else if (n == tokens.size() - 1) {
                        features.lineStatus = "LINEEND";
                        previousNewline = true;
                        features.blockStatus = "BLOCKEND";
                        endblock = true;
                    } else {
                        // look ahead...
                        boolean endline = false;

                        int ii = 1;
                        boolean endloop = false;
                        while ((n + ii < tokens.size()) && (!endloop)) {
                            LayoutToken tok = tokens.get(n + ii);
                            if (tok != null) {
                                String toto = tok.getText();
                                if (toto != null) {
                                    if (toto.equals("\n")) {
                                        endline = true;
                                        endloop = true;
                                    } else {
                                        if ((toto.length() != 0)
                                            && (!(toto.startsWith("@IMAGE")))
                                            && (!(toto.startsWith("@PAGE")))
                                            && (!text.contains(".pbm"))
                                            && (!text.contains(".svg"))
                                            && (!text.contains(".png"))
                                            && (!text.contains(".jpg"))) {
                                            endloop = true;
                                        }
                                    }
                                }
                            }

                            if (n + ii == tokens.size() - 1) {
                                endblock = true;
                                endline = true;
                            }

                            ii++;
                        }

                        if ((!endline) && !(newline)) {
                            features.lineStatus = "LINEIN";
                        } else if (!newline) {
                            features.lineStatus = "LINEEND";
                            previousNewline = true;
                        }

                        if ((!endblock) && (features.blockStatus == null))
                            features.blockStatus = "BLOCKIN";
                        else if (features.blockStatus == null) {
                            features.blockStatus = "BLOCKEND";
                            //endblock = true;
                        }
                    }

                    if (text.length() == 1) {
                        features.singleChar = true;
                    }

                    if (Character.isUpperCase(text.charAt(0))) {
                        features.capitalisation = "INITCAP";
                    }

                    if (featureFactory.test_all_capital(text)) {
                        features.capitalisation = "ALLCAP";
                    }

                    if (featureFactory.test_digit(text)) {
                        features.digit = "CONTAINSDIGITS";
                    }

                    Matcher m = featureFactory.isDigit.matcher(text);
                    if (m.find()) {
                        features.digit = "ALLDIGIT";
                    }

                    if (currentFont == null) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else if (!currentFont.equals(token.getFont())) {
                        currentFont = token.getFont();
                        features.fontStatus = "NEWFONT";
                    } else
                        features.fontStatus = "SAMEFONT";

                    int newFontSize = (int) token.getFontSize();
                    if (currentFontSize == -1) {
                        currentFontSize = newFontSize;
                        features.fontSize = "HIGHERFONT";
                    } else if (currentFontSize == newFontSize) {
                        features.fontSize = "SAMEFONTSIZE";
                    } else if (currentFontSize < newFontSize) {
                        features.fontSize = "HIGHERFONT";
                        currentFontSize = newFontSize;
                    } else if (currentFontSize > newFontSize) {
                        features.fontSize = "LOWERFONT";
                        currentFontSize = newFontSize;
                    }

                    if (token.isBold())
                        features.bold = true;

                    if (token.isItalic())
                        features.italic = true;

                    if (features.capitalisation == null)
                        features.capitalisation = "NOCAPS";

                    if (features.digit == null)
                        features.digit = "NODIGIT";

                    if (features.punctType == null)
                        features.punctType = "NOPUNCT";

                    features.relativeDocumentPosition = featureFactory
                        .linearScaling(nn, fulltextLength, NBBINS_POSITION);
                    // System.out.println(mm + " / " + pageLength);
                    features.relativePagePositionChar = featureFactory
                        .linearScaling(mm, pageLength, NBBINS_POSITION);

                    int pagePos = featureFactory
                        .linearScaling(coordinateLineY, pageHeight, NBBINS_POSITION);
                    if (pagePos > NBBINS_POSITION)
                        pagePos = NBBINS_POSITION;
                    features.relativePagePosition = pagePos;
//System.out.println((coordinateLineY) + " " + (pageHeight) + " " + NBBINS_POSITION + " " + pagePos); 

                    if (spacingPreviousBlock != 0.0) {
                        features.spacingWithPreviousBlock = featureFactory
                            .linearScaling(spacingPreviousBlock - doc.getMinBlockSpacing(),
                                doc.getMaxBlockSpacing() - doc.getMinBlockSpacing(), NBBINS_SPACE);
                    }

                    if (density != -1.0) {
                        features.characterDensity = featureFactory
                            .linearScaling(density - doc.getMinCharacterDensity(), doc.getMaxCharacterDensity() - doc.getMinCharacterDensity(), NBBINS_DENSITY);
//System.out.println((density-doc.getMinCharacterDensity()) + " " + (doc.getMaxCharacterDensity()-doc.getMinCharacterDensity()) + " " + NBBINS_DENSITY + " " + features.characterDensity);
                    }

                    features.calloutType = bibRefCalloutType;

                    // check of the token is a known bib ref label
                    if ((referenceMarkerMatcher != null) && (referenceMarkerMatcher.isKnownLabel(text) || referenceMarkerMatcher.isKnownFirstAuthor(text)))
                        features.calloutKnown = true;

                    if (token.isSuperscript()) {
                        features.superscript = true;
                    }

                    // fulltext.append(features.printVector());
                    if (previousFeatures != null) {
                        if (features.blockStatus.equals("BLOCKSTART") &&
                            previousFeatures.blockStatus.equals("BLOCKIN")) {
                            // this is a post-correction due to the fact that the last character of a block
                            // can be a space or EOL character
                            previousFeatures.blockStatus = "BLOCKEND";
                            previousFeatures.lineStatus = "LINEEND";
                        }
                        fulltext.append(previousFeatures.printVector());
                    }

                    n++;
                    mm += text.length();
                    nn += text.length();
                    previousFeatures = features;
                    isFirstBlockToken = false;
                }
                // lowest position of the block
                lowestPos = block.getY() + block.getHeight();

                //blockPos++;
            }
        }
        if (previousFeatures != null) {
            fulltext.append(previousFeatures.printVector());

        }

        return Pair.of(fulltext.toString(),
            new LayoutTokenization(layoutTokens));
    }

    /**
     * Evaluate the length of the fulltext
     */
    private static int getFulltextLength(Document doc, SortedSet<DocumentPiece> documentBodyParts, int fulltextLength) {
        for (DocumentPiece docPiece : documentBodyParts) {
            DocumentPointer dp1 = docPiece.getLeft();
            DocumentPointer dp2 = docPiece.getRight();

            int tokenStart = dp1.getTokenDocPos();
            int tokenEnd = dp2.getTokenDocPos();
            for (int i = tokenStart; i <= tokenEnd && i < doc.getTokenizations().size(); i++) {
                //tokenizationsBody.add(tokenizations.get(i));
                fulltextLength += doc.getTokenizations().get(i).getText().length();
            }
        }
        return fulltextLength;
    }

    /**
     * Return the index of a token in a document tokenization
     */
    private static int getDocIndexToken(Document doc, LayoutToken token) {
        int blockPtr = token.getBlockPtr();
        Block block = doc.getBlocks().get(blockPtr);
        int startTokenBlockPos = block.getStartToken();
        List<LayoutToken> tokens = doc.getTokenizations();
        int i = startTokenBlockPos;
        for (; i < tokens.size(); i++) {
            int offset = tokens.get(i).getOffset();
            if (offset >= token.getOffset())
                break;
        }
        return i;
    }

    /**
     * Process the specified pdf and format the result as training data for all the models.
     *
     * @param inputFile    input file
     * @param pathFullText path to fulltext
     * @param pathTEI      path to TEI
     * @param id           id
     */
    public Document createTraining(File inputFile,
                                   String pathFullText,
                                   String pathTEI,
                                   int id) {
        return createTraining(inputFile, pathFullText, pathTEI, id, null);
    }

    public Document createTraining(File inputFile,
                                   String pathFullText,
                                   String pathTEI,
                                   int id,
                                   GrobidModels.Flavor flavor) {
        if (tmpPath == null)
            throw new GrobidResourceException("Cannot process pdf file, because temp path is null.");
        if (!tmpPath.exists()) {
            throw new GrobidResourceException("Cannot process pdf file, because temp path '" +
                tmpPath.getAbsolutePath() + "' does not exists.");
        }
        DocumentSource documentSource = null;
        try {
            if (!inputFile.exists()) {
                throw new GrobidResourceException("Cannot train for fulltext, becuase file '" +
                    inputFile.getAbsolutePath() + "' does not exists.");
            }
            String pdfFileName = inputFile.getName();

            // SEGMENTATION MODEL
            documentSource = DocumentSource.fromPdf(inputFile, -1, -1, false, true, true);
            Document doc = new Document(documentSource);
            doc.addTokenizedDocument(GrobidAnalysisConfig.defaultInstance());

            if (doc.getBlocks() == null) {
                throw new Exception("PDF parsing resulted in empty content");
            }
            doc.produceStatistics();

            String fulltext = //getAllTextFeatured(doc, false);
                parsers.getSegmentationParser(flavor).getAllLinesFeatured(doc);
            //List<LayoutToken> tokenizations = doc.getTokenizationsFulltext();
            List<LayoutToken> tokenizations = doc.getTokenizations();

            // we write first the full text untagged (but featurized with segmentation features)
            String outPathFulltext = pathFullText + File.separator + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.segmentation");
            Writer writer = new OutputStreamWriter(new FileOutputStream(outPathFulltext, false), StandardCharsets.UTF_8);
            writer.write(fulltext + "\n");
            writer.close();

            // also write the raw text as seen before segmentation
            StringBuffer rawtxt = new StringBuffer();
            for (LayoutToken txtline : tokenizations) {
                rawtxt.append(txtline.getText());
            }
            String outPathRawtext = pathFullText + File.separator +
                pdfFileName.replaceAll("(?i)\\.pdf$", ".training.segmentation.rawtxt");
            FileUtils.writeStringToFile(new File(outPathRawtext), rawtxt.toString(), StandardCharsets.UTF_8);

            if (isNotBlank(fulltext)) {
                String rese = parsers.getSegmentationParser(flavor).label(fulltext);
                StringBuffer bufferFulltext = parsers.getSegmentationParser(flavor).trainingExtraction(rese, tokenizations, doc);

                // write the TEI file to reflect the extact layout of the text as extracted from the pdf
                writer = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                    File.separator +
                    pdfFileName.replaceAll("(?i)\\.pdf$", ".training.segmentation.tei.xml")), false), StandardCharsets.UTF_8);
                writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                    "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"en\">\n");

                writer.write(bufferFulltext.toString());
                writer.write("\n\t</text>\n</tei>\n");
                writer.close();
            }

            doc = parsers.getSegmentationParser(flavor).processing(documentSource,
                GrobidAnalysisConfig.defaultInstance());

            // REFERENCE SEGMENTER MODEL
            String referencesStr = doc.getDocumentPartText(SegmentationLabels.REFERENCES);
            if (!referencesStr.isEmpty()) {
                //String tei = parsers.getReferenceSegmenterParser().createTrainingData2(referencesStr, id);
                Pair<String, String> result =
                    parsers.getReferenceSegmenterParser().createTrainingData(doc, id);
                String tei = result.getLeft();
                String raw = result.getRight();
                if (tei != null) {
                    String outPath = pathTEI + "/" +
                        pdfFileName.replaceAll("(?i)\\.pdf$", ".training.references.referenceSegmenter.tei.xml");
                    writer = new OutputStreamWriter(new FileOutputStream(new File(outPath), false), StandardCharsets.UTF_8);
                    writer.write(tei + "\n");
                    writer.close();

                    // generate also the raw vector file with the features
                    outPath = pathTEI + "/" + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.references.referenceSegmenter");
                    writer = new OutputStreamWriter(new FileOutputStream(new File(outPath), false), StandardCharsets.UTF_8);
                    writer.write(raw + "\n");
                    writer.close();

                    // also write the raw text as it is before reference segmentation
                    outPathRawtext = pathTEI + "/" + pdfFileName
                        .replace(".pdf", ".training.references.referenceSegmenter.rawtxt");
                    Writer strWriter = new OutputStreamWriter(
                        new FileOutputStream(new File(outPathRawtext), false), StandardCharsets.UTF_8);
                    strWriter.write(referencesStr + "\n");
                    strWriter.close();
                }
            }

            // BIBLIO REFERENCE MODEL
            StringBuilder allBufferReference = new StringBuilder();
            if (!referencesStr.isEmpty()) {
                cntManager.i(CitationParserCounters.NOT_EMPTY_REFERENCES_BLOCKS);
            }
            ReferenceSegmenter referenceSegmenter = parsers.getReferenceSegmenterParser();
            List<LabeledReferenceResult> references = referenceSegmenter.extract(doc);
            List<BibDataSet> resCitations = parsers.getCitationParser().
                processingReferenceSection(doc, referenceSegmenter, 0);
            doc.setBibDataSets(resCitations);

            if (references == null) {
                cntManager.i(CitationParserCounters.NULL_SEGMENTED_REFERENCES_LIST);
            } else {
                cntManager.i(CitationParserCounters.SEGMENTED_REFERENCES, references.size());

                List<String> allInput = new ArrayList<String>();
                for (LabeledReferenceResult ref : references) {
                    allInput.add(ref.getReferenceText());
                }
                StringBuilder bufferReference = parsers.getCitationParser().trainingExtraction(allInput);
                if (bufferReference != null) {
                    bufferReference.append("\n");

                    Writer writerReference = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                        File.separator +
                        pdfFileName.replaceAll("(?i)\\.pdf$", ".training.references.tei.xml")), false), StandardCharsets.UTF_8);

                    writerReference.write("<?xml version=\"1.0\" ?>\n<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" " +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                        "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
                    if (id == -1) {
                        writerReference.write("\t<teiHeader/>\n\t<text>\n\t\t<front/>\n\t\t<body/>\n\t\t<back>\n");
                    } else {
                        writerReference.write("\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                            "\"/>\n\t</teiHeader>\n\t<text>\n\t\t<front/>\n\t\t<body/>\n\t\t<back>\n");
                    }
                    writerReference.write("<listBibl>\n");

                    writerReference.write(bufferReference.toString());

                    writerReference.write("\t\t</listBibl>\n\t</back>\n\t</text>\n</TEI>\n");
                    writerReference.close();

                    // BIBLIO REFERENCE AUTHOR NAMES
                    Writer writerName = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                        File.separator +
                        pdfFileName.replaceAll("(?i)\\.pdf$", ".training.references.authors.tei.xml")), false), StandardCharsets.UTF_8);

                    writerName.write("<?xml version=\"1.0\" ?>\n<TEI xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\" " +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                        "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
                    writerName.write("\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>\n" +
                        "\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n");

                    for (LabeledReferenceResult ref : references) {
                        if (StringUtils.isNotBlank(ref.getReferenceText())) {
                            BiblioItem bib = parsers.getCitationParser().processingString(ref.getReferenceText(), 0);
                            if (bib != null) {
                                String authorSequence = bib.getAuthors();
                                if (StringUtils.isNotBlank(authorSequence)) {
                                    /*List<String> inputs = new ArrayList<String>();
                                    inputs.add(authorSequence);*/
                                    StringBuilder bufferName = parsers.getAuthorParser().trainingExtraction(authorSequence, false);
                                    if ((bufferName != null) && (bufferName.length() > 0)) {
                                        writerName.write("\n\t\t\t\t\t\t<author>");
                                        writerName.write(bufferName.toString());
                                        writerName.write("</author>\n");
                                    }
                                }
                            }
                        }
                    }

                    writerName.write("\n\t\t\t\t\t</analytic>");
                    writerName.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                    writerName.write("\n\t</teiHeader>\n</TEI>\n");
                    writerName.close();
                }
            }

            // FULLTEXT MODEL (body)
            SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(SegmentationLabels.BODY);
            if (documentBodyParts != null) {
                Pair<String, LayoutTokenization> featSeg = getBodyTextFeatured(doc, documentBodyParts);
                if (featSeg != null) {
                    // if no textual body part found, nothing to generate


                    String bodytext = featSeg.getLeft();
                    List<LayoutToken> tokenizationsBody = featSeg.getRight().getTokenization();

                    // we write the full text untagged
                    outPathFulltext = pathFullText + File.separator
                        + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.fulltext");
                    writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFulltext), false), StandardCharsets.UTF_8);
                    writer.write(bodytext + "\n");
                    writer.close();

                    //              StringTokenizer st = new StringTokenizer(fulltext, "\n");
                    String rese = label(bodytext);
                    //System.out.println(rese);
                    StringBuilder bufferFulltext = trainingExtraction(rese, tokenizationsBody);

                    // write the TEI file to reflect the extract layout of the text as extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                        File.separator +
                        pdfFileName.replaceAll("(?i)\\.pdf$", ".training.fulltext.tei.xml")), false), StandardCharsets.UTF_8);
                    if (id == -1) {
                        writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader/>\n\t<text xml:lang=\"en\">\n");
                    } else {
                        writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\"" + id +
                            "\"/>\n\t</teiHeader>\n\t<text xml:lang=\"en\">\n");
                    }
                    writer.write(bufferFulltext.toString());
                    writer.write("\n\t</text>\n</tei>\n");
                    writer.close();

                    // training data for FIGURES
                    Pair<String, String> trainingFigure = processTrainingDataFigures(rese, tokenizationsBody, inputFile.getName());
                    if (trainingFigure.getLeft().trim().length() > 0) {
                        String outPathFigures = pathFullText + File.separator
                            + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.figure");
                        writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFigures), false), StandardCharsets.UTF_8);
                        writer.write(trainingFigure.getRight() + "\n\n");
                        writer.close();

                        String outPathFiguresTEI = pathTEI + File.separator
                            + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.figure.tei.xml");
                        writer = new OutputStreamWriter(new FileOutputStream(new File(outPathFiguresTEI), false), StandardCharsets.UTF_8);
                        writer.write(trainingFigure.getLeft() + "\n");
                        writer.close();
                    }

                    // training data for TABLES
                    Pair<String, String> trainingTable = processTrainingDataTables(rese, tokenizationsBody, inputFile.getName());
                    if (trainingTable.getLeft().trim().length() > 0) {
                        String outPathTables = pathFullText + File.separator
                            + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.table");
                        writer = new OutputStreamWriter(new FileOutputStream(new File(outPathTables), false), StandardCharsets.UTF_8);
                        writer.write(trainingTable.getRight() + "\n\n");
                        writer.close();

                        String outPathTablesTEI = pathTEI + File.separator
                            + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.table.tei.xml");
                        writer = new OutputStreamWriter(new FileOutputStream(new File(outPathTablesTEI), false), StandardCharsets.UTF_8);
                        writer.write(trainingTable.getLeft() + "\n");
                        writer.close();
                    }
                }
            }

            // HEADER MODEL
            SortedSet<DocumentPiece> documentHeaderParts = doc.getDocumentPart(SegmentationLabels.HEADER);
            List<LayoutToken> tokenizationsFull = doc.getTokenizations();
            if (documentHeaderParts != null) {
                List<LayoutToken> headerTokenizations = new ArrayList<LayoutToken>();

                for (DocumentPiece docPiece : documentHeaderParts) {
                    DocumentPointer dp1 = docPiece.getLeft();
                    DocumentPointer dp2 = docPiece.getRight();

                    int tokens = dp1.getTokenDocPos();
                    int tokene = dp2.getTokenDocPos();
                    for (int i = tokens; i < tokene; i++) {
                        headerTokenizations.add(tokenizationsFull.get(i));
                    }
                }
                Pair<String, List<LayoutToken>> featuredHeader = parsers.getHeaderParser(flavor).getSectionHeaderFeatured(doc, documentHeaderParts);
                String header = featuredHeader.getLeft();

                if ((header != null) && (header.trim().length() > 0)) {
                    // we write the header untagged
                    String outPathHeader = pathTEI + File.separator + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header");
                    writer = new OutputStreamWriter(new FileOutputStream(new File(outPathHeader), false), StandardCharsets.UTF_8);
                    writer.write(header + "\n");
                    writer.close();

                    String rese = parsers.getHeaderParser(flavor).label(header);
                    BiblioItem resHeader = new BiblioItem();
                    resHeader = parsers.getHeaderParser(flavor).resultExtraction(rese, headerTokenizations, resHeader);

                    // buffer for the header block
                    StringBuilder bufferHeader = parsers.getHeaderParser(flavor).trainingExtraction(rese, headerTokenizations);
                    Language lang = LanguageUtilities.getInstance().runLanguageId(bufferHeader.toString());
                    if (lang != null) {
                        doc.setLanguage(lang.getLang());
                    }

                    // buffer for the affiliation+address block

                    List<List<LayoutToken>> tokenizationsAffiliation = resHeader.getAffiliationAddresslabeledTokens();
                    //List<LayoutToken> tokenizationsAffiliation = resHeader.getLayoutTokens(TaggingLabels.HEADER_AFFILIATION);
                    List<LayoutToken> tokenizationAffiliation = new ArrayList<>();
                    StringBuilder bufferAffiliation = null;
                    if (tokenizationsAffiliation != null && tokenizationsAffiliation.size() > 0) {
                        for (List<LayoutToken> tokenization : tokenizationsAffiliation) {
                            tokenizationAffiliation.addAll(tokenization);
                        }
                        bufferAffiliation =
                            parsers.getAffiliationAddressParser().trainingExtraction(tokenizationAffiliation);
                    }

                    // buffer for the date block
                    StringBuilder bufferDate = null;
                    // we need to rebuild the found date string as it appears
                    String input = "";
                    int q = 0;
                    StringTokenizer st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < headerTokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = headerTokenizations.get(q).getText();
                        String theTok = headerTokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q > 0) && (q < headerTokenizations.size())) {
                                theTok = headerTokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<date>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.trim().length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferDate = parsers.getDateParser().trainingExtraction(inputs);
                    }

                    // buffer for the name block
                    StringBuilder bufferName = null;
                    // we need to rebuild the found author string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < headerTokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = headerTokenizations.get(q).getText();
                        String theTok = headerTokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q > 0) && (q < headerTokenizations.size())) {
                                theTok = headerTokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<author>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        bufferName = parsers.getAuthorParser().trainingExtraction(input, true);
                    }

                    // buffer for the reference block
                    StringBuilder bufferReference = null;
                    // we need to rebuild the found citation string as it appears
                    input = "";
                    q = 0;
                    st = new StringTokenizer(rese, "\n");
                    while (st.hasMoreTokens() && (q < headerTokenizations.size())) {
                        String line = st.nextToken();
                        String theTotalTok = headerTokenizations.get(q).getText();
                        String theTok = headerTokenizations.get(q).getText();
                        while (theTok.equals(" ") || theTok.equals("\t") || theTok.equals("\n") || theTok.equals("\r")) {
                            q++;
                            if ((q > 0) && (q < headerTokenizations.size())) {
                                theTok = headerTokenizations.get(q).getText();
                                theTotalTok += theTok;
                            }
                        }
                        if (line.endsWith("<reference>")) {
                            input += theTotalTok;
                        }
                        q++;
                    }
                    if (input.length() > 1) {
                        List<String> inputs = new ArrayList<String>();
                        inputs.add(input.trim());
                        bufferReference = parsers.getCitationParser().trainingExtraction(inputs);
                    }

                    // write the training TEI file for header which reflects the extract layout of the text as
                    // extracted from the pdf
                    writer = new OutputStreamWriter(new FileOutputStream(new File(pathTEI + File.separator
                        + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header.tei.xml")), false), StandardCharsets.UTF_8);
                    writer.write("<?xml version=\"1.0\" ?>\n<tei xml:space=\"preserve\">\n\t<teiHeader>\n\t\t<fileDesc xml:id=\""
                        + pdfFileName.replaceAll("(?i)\\.pdf$", "")
                        + "\"/>\n\t</teiHeader>\n\t<text");

                    if (lang != null) {
                        writer.write(" xml:lang=\"" + lang.getLang() + "\"");
                    }
                    writer.write(">\n\t\t<front>\n");

                    writer.write(bufferHeader.toString());
                    writer.write("\n\t\t</front>\n\t</text>\n</tei>\n");
                    writer.close();

                    // AFFILIATION-ADDRESS model
                    if (bufferAffiliation != null) {
                        if (bufferAffiliation.length() > 0) {
                            Writer writerAffiliation = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header.affiliation.tei.xml")), false), StandardCharsets.UTF_8);
                            writerAffiliation.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                            writerAffiliation.write("\n<tei xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\""
                                + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" " + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                            writerAffiliation.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                            writerAffiliation.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\t\t\t\t\t\t<author>\n\n");

                            writerAffiliation.write(bufferAffiliation.toString());

                            writerAffiliation.write("\n\t\t\t\t\t\t</author>\n\t\t\t\t\t</analytic>");
                            writerAffiliation.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                            writerAffiliation.write("\n\t</teiHeader>\n</tei>\n");
                            writerAffiliation.close();
                        }
                    }

                    // DATE MODEL (for dates in header)
                    if (bufferDate != null) {
                        if (bufferDate.length() > 0) {
                            Writer writerDate = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header.date.xml")), false), StandardCharsets.UTF_8);
                            writerDate.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                            writerDate.write("<dates>\n");

                            writerDate.write(bufferDate.toString());

                            writerDate.write("</dates>\n");
                            writerDate.close();
                        }
                    }

                    // HEADER AUTHOR NAME model
                    if (bufferName != null) {
                        if (bufferName.length() > 0) {
                            Writer writerName = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header.authors.tei.xml")), false), StandardCharsets.UTF_8);
                            writerName.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                            writerName.write("\n<tei xml:space=\"preserve\" xmlns=\"http://www.tei-c.org/ns/1.0\"" + " xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                                + "xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">");
                            writerName.write("\n\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<sourceDesc>");
                            writerName.write("\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n\n\t\t\t\t\t\t<author>");
                            writerName.write("\n\t\t\t\t\t\t\t<persName>\n");

                            writerName.write(bufferName.toString());

                            writerName.write("\t\t\t\t\t\t\t</persName>\n");
                            writerName.write("\t\t\t\t\t\t</author>\n\n\t\t\t\t\t</analytic>");
                            writerName.write("\n\t\t\t\t</biblStruct>\n\t\t\t</sourceDesc>\n\t\t</fileDesc>");
                            writerName.write("\n\t</teiHeader>\n</tei>\n");
                            writerName.close();
                        }
                    }

                    // CITATION MODEL (for bibliographical reference in header)
                    if (bufferReference != null) {
                        if (bufferReference.length() > 0) {
                            Writer writerReference = new OutputStreamWriter(new FileOutputStream(new File(pathTEI +
                                File.separator
                                + pdfFileName.replaceAll("(?i)\\.pdf$", ".training.header.reference.xml")), false), StandardCharsets.UTF_8);
                            writerReference.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                            writerReference.write("<citations>\n");

                            writerReference.write(bufferReference.toString());

                            writerReference.write("</citations>\n");
                            writerReference.close();
                        }
                    }
                }
            }

            return doc;

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid training" +
                " data generation for full text.", e);
        } finally {
            DocumentSource.close(documentSource, true, true, true);
        }
    }

    /**
     * Extract results from a labelled full text in the training format without any string modification.
     */
    private StringBuilder trainingExtraction(String result,
                                             List<LayoutToken> tokenizations) {
        // this is the main buffer for the whole full text
        StringBuilder buffer = new StringBuilder();
        try {
            StringTokenizer st = new StringTokenizer(result, "\n");
            String s1 = null;
            String s2 = null;
            String lastTag = null;
            //System.out.println(tokenizations.toString());
            //System.out.println(result);
            // current token position
            int p = 0;
            boolean start = true;
            boolean openFigure = false;
            boolean headFigure = false;
            boolean descFigure = false;
            boolean tableBlock = false;

            while (st.hasMoreTokens()) {
                boolean addSpace = false;
                String tok = st.nextToken().trim();

                if (tok.length() == 0) {
                    continue;
                }
                StringTokenizer stt = new StringTokenizer(tok, " \t");
                List<String> localFeatures = new ArrayList<String>();
                int i = 0;

                boolean newLine = false;
                int ll = stt.countTokens();
                while (stt.hasMoreTokens()) {
                    String s = stt.nextToken().trim();
                    if (i == 0) {
                        s2 = TextUtilities.HTMLEncode(s); // lexical token
                        int p0 = p;
                        boolean strop = false;
                        while ((!strop) && (p < tokenizations.size())) {
                            String tokOriginal = tokenizations.get(p).t();
                            if (tokOriginal.equals(" ")
                                || tokOriginal.equals("\u00A0")) {
                                addSpace = true;
                            } else if (tokOriginal.equals("\n")) {
                                newLine = true;
                            } else if (tokOriginal.equals(s)) {
                                strop = true;
                            }
                            p++;
                        }
                        if (p == tokenizations.size()) {
                            // either we are at the end of the header, or we might have
                            // a problematic token in tokenization for some reasons
                            if ((p - p0) > 2) {
                                // we loose the synchronicity, so we reinit p for the next token
                                p = p0;
                            }
                        }
                    } else if (i == ll - 1) {
                        s1 = s; // current tag
                    } else {
                        if (s.equals("LINESTART"))
                            newLine = true;
                        localFeatures.add(s);
                    }
                    i++;
                }

                if (newLine && !start) {
                    buffer.append("<lb/>");
                }

                String lastTag0 = null;
                if (lastTag != null) {
                    if (lastTag.startsWith("I-")) {
                        lastTag0 = lastTag.substring(2, lastTag.length());
                    } else {
                        lastTag0 = lastTag;
                    }
                }
                String currentTag0 = null;
                if (s1 != null) {
                    if (s1.startsWith("I-")) {
                        currentTag0 = s1.substring(2, s1.length());
                    } else {
                        currentTag0 = s1;
                    }
                }

                boolean closeParagraph = false;
                if (lastTag != null) {
                    closeParagraph = testClosingTag(buffer, currentTag0, lastTag0, s1);
                }

                boolean output;

                //output = writeField(buffer, s1, lastTag0, s2, "<header>", "<front>", addSpace, 3);
                //if (!output) {
                output = writeField(buffer, s1, lastTag0, s2, "<other>",
                    "<note type=\"other\">", addSpace, 3, false);
                //}
                // for paragraph we must distinguish starting and closing tags
                if (!output) {
                    if (closeParagraph) {
                        output = writeFieldBeginEnd(buffer, s1, "", s2, "<paragraph>",
                            "<p>", addSpace, 3, false);
                    } else {
                        output = writeFieldBeginEnd(buffer, s1, lastTag, s2, "<paragraph>",
                            "<p>", addSpace, 3, false);
                    }
                }
                /*if (!output) {
                    if (closeParagraph) {
                        output = writeField(buffer, s1, "", s2, "<reference_marker>", "<label>", addSpace, 3);
                    } else
                        output = writeField(buffer, s1, lastTag0, s2, "<reference_marker>", "<label>", addSpace, 3);
                }*/
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<citation_marker>", "<ref type=\"biblio\">",
                        addSpace, 3, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<table_marker>", "<ref type=\"table\">",
                        addSpace, 3, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<equation_marker>", "<ref type=\"formula\">",
                        addSpace, 3, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<section>",
                        "<head>", addSpace, 3, false);
                }
                /*if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<subsection>", 
						"<head>", addSpace, 3, false);
                }*/
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<equation>",
                        "<formula>", addSpace, 4, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<equation_label>",
                        "<label>", addSpace, 4, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<figure_marker>",
                        "<ref type=\"figure\">", addSpace, 3, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<figure>",
                        "<figure>", addSpace, 3, false);
                }
                if (!output) {
                    output = writeField(buffer, s1, lastTag0, s2, "<table>",
                        "<figure type=\"table\">", addSpace, 3, false);
                }
                // for item we must distinguish starting and closing tags
                if (!output) {
                    output = writeFieldBeginEnd(buffer, s1, lastTag, s2, "<item>",
                        "<item>", addSpace, 3, false);
                }

                lastTag = s1;

                if (!st.hasMoreTokens()) {
                    if (lastTag != null) {
                        testClosingTag(buffer, "", currentTag0, s1);
                    }
                }
                if (start) {
                    start = false;
                }
            }

            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            throw new GrobidException("An exception occured while running Grobid.", e);
        }
    }

    /**
     * TODO some documentation...
     *
     * @param buffer   buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    public static boolean writeField(StringBuilder buffer,
                                     String s1,
                                     String lastTag0,
                                     String s2,
                                     String field,
                                     String outField,
                                     boolean addSpace,
                                     int nbIndent,
                                     boolean generateIDs) {
        boolean result = false;
        if (s1 == null) {
            return result;
        }
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            String divID = null;
            if (generateIDs) {
                divID = KeyGen.getKey().substring(0, 7);
                if (outField.charAt(outField.length() - 2) == '>')
                    outField = outField.substring(0, outField.length() - 2) + " xml:id=\"_" + divID + "\">";
            }
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            }
            /*else if (lastTag0 == null) {
                   for(int i=0; i<nbIndent; i++) {
                       buffer.append("\t");
                   }
                     buffer.append(outField+s2);
               }*/
            else if (field.equals("<citation_marker>")) {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            } else if (field.equals("<figure_marker>")) {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            } else if (field.equals("<table_marker>")) {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            } else if (field.equals("<equation_marker>")) {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            } /*else if (field.equals("<label>")) {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            } */ /*else if (field.equals("<reference_marker>")) {
                if (!lastTag0.equals("<reference>") && !lastTag0.equals("<reference_marker>")) {
                    for (int i = 0; i < nbIndent; i++) {
                        buffer.append("\t");
                    }
                    buffer.append("<bibl>");
                }
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            }*/ else if (lastTag0 == null) {
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(s2);
            } else if (!lastTag0.equals("<citation_marker>")
                && !lastTag0.equals("<figure_marker>")
                && !lastTag0.equals("<equation_marker>")
                //&& !lastTag0.equals("<figure>")
            ) {
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(s2);
            } else {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            }
        }
        return result;
    }

    /**
     * This is for writing fields for fields where begin and end of field matter, like paragraph or item
     *
     * @param buffer
     * @param s1
     * @param lastTag0
     * @param s2
     * @param field
     * @param outField
     * @param addSpace
     * @param nbIndent
     * @return
     */
    public static boolean writeFieldBeginEnd(StringBuilder buffer,
                                             String s1,
                                             String lastTag0,
                                             String s2,
                                             String field,
                                             String outField,
                                             boolean addSpace,
                                             int nbIndent,
                                             boolean generateIDs) {
        boolean result = false;
        if (s1 == null) {
            return false;
        }
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (lastTag0 == null) {
                lastTag0 = "";
            }
            String divID;
            if (generateIDs) {
                divID = KeyGen.getKey().substring(0, 7);
                if (outField.charAt(outField.length() - 2) == '>')
                    outField = outField.substring(0, outField.length() - 2) + " xml:id=\"_" + divID + "\">";
            }
            if (lastTag0.equals("I-" + field)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else if (lastTag0.equals(field) && s1.equals(field)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else if (!lastTag0.endsWith("<citation_marker>") && !lastTag0.endsWith("<figure_marker>")
                && !lastTag0.endsWith("<table_marker>") && !lastTag0.endsWith("<equation_marker>")) {
                for (int i = 0; i < nbIndent; i++) {
                    buffer.append("\t");
                }
                buffer.append(outField).append(s2);
            } else {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            }
        }
        return result;
    }

    /**
     * TODO some documentation
     *
     * @param buffer
     * @param currentTag0
     * @param lastTag0
     * @param currentTag
     * @return
     */
    private static boolean testClosingTag(StringBuilder buffer,
                                          String currentTag0,
                                          String lastTag0,
                                          String currentTag) {
        boolean res = false;
        // reference_marker and citation_marker are two exceptions because they can be embedded

        if (!currentTag0.equals(lastTag0) || currentTag.equals("I-<paragraph>") || currentTag.equals("I-<item>")) {
            if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<equation_marker>") ||
                currentTag0.equals("<figure_marker>") || currentTag0.equals("<table_marker>")) {
                return res;
            }

            res = false;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("</note>\n\n");

            } else if (lastTag0.equals("<paragraph>") &&
                !currentTag0.equals("<citation_marker>") &&
                !currentTag0.equals("<table_marker>") &&
                !currentTag0.equals("<equation_marker>") &&
                !currentTag0.equals("<figure_marker>")
            ) {
                buffer.append("</p>\n\n");
                res = true;

            } else if (lastTag0.equals("<section>")) {
                buffer.append("</head>\n\n");
            } else if (lastTag0.equals("<subsection>")) {
                buffer.append("</head>\n\n");
            } else if (lastTag0.equals("<equation>")) {
                buffer.append("</formula>\n\n");
            } else if (lastTag0.equals("<equation_label>")) {
                buffer.append("</label>\n\n");
            } else if (lastTag0.equals("<table>")) {
                buffer.append("</figure>\n\n");
            } else if (lastTag0.equals("<figure>")) {
                buffer.append("</figure>\n\n");
            } else if (lastTag0.equals("<item>")) {
                buffer.append("</item>\n\n");
            } else if (lastTag0.equals("<citation_marker>") ||
                lastTag0.equals("<figure_marker>") ||
                lastTag0.equals("<table_marker>") ||
                lastTag0.equals("<equation_marker>")) {
                buffer.append("</ref>");

                // Make sure that paragraph is closed when markers are at the end of it
                if (!currentTag0.equals("<paragraph>") &&
                    (!currentTag0.equals("<citation_marker>") ||
                        !currentTag0.equals("<figure_marker>") ||
                        !currentTag0.equals("<table_marker>") ||
                        !currentTag0.equals("<equation_marker>")
                    )
                ) {
                    buffer.append("</p>\n\n");
                }
            } else {
                res = false;
            }

        }
        return res;
    }


    /**
     * Process figures identified by the full text model
     */
    protected List<Figure> processFigures(String rese, List<LayoutToken> layoutTokens) {
        return processFigures(rese, layoutTokens,0);
    }

    protected List<Figure> processFigures(String rese, List<LayoutToken> layoutTokens, int startFigureID) {
        List<Figure> results = new ArrayList<>();

        int figureId = startFigureID;

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(FULLTEXT, rese, layoutTokens, true);

        for (TaggingTokenCluster cluster : Iterables.filter(clusteror.cluster(),
            new TaggingTokenClusteror.LabelTypePredicate(TaggingLabels.FIGURE))) {
            List<LayoutToken> tokenizationFigure = cluster.concatTokens();
            Figure result = this.parsers.getFigureParser().processing(
                tokenizationFigure,
                cluster.getFeatureBlock()
            );
            SortedSet<Integer> blockPtrs = new TreeSet<>();
            for (LayoutToken lt : tokenizationFigure) {
                if (!LayoutTokensUtil.spaceyToken(lt.t()) && !LayoutTokensUtil.newLineToken(lt.t())) {
                    blockPtrs.add(lt.getBlockPtr());
                }
            }
            result.setBlockPtrs(blockPtrs);

            result.setLayoutTokens(tokenizationFigure);

            // the first token could be a space from previous page
            for (LayoutToken lt : tokenizationFigure) {
                if (!LayoutTokensUtil.spaceyToken(lt.t()) && !LayoutTokensUtil.newLineToken(lt.t())) {
                    result.setPage(lt.getPage());
                    break;
                }
            }

            results.add(result);
            result.setId("" + figureId);
            figureId++;
        }

        return results;
    }

    /**
     * Create training data for the figures as identified by the full text model.
     * Return the pair (TEI fragment, sequence labeling raw data).
     */
    protected Pair<String, String> processTrainingDataFigures(String rese,
                                                              List<LayoutToken> tokenizations, String id) {
        StringBuilder tei = new StringBuilder();
        StringBuilder featureVector = new StringBuilder();
        int nb = 0;
        StringTokenizer st1 = new StringTokenizer(rese, "\n");
        boolean openFigure = false;
        StringBuilder figureBlock = new StringBuilder();
        List<LayoutToken> tokenizationsFigure = new ArrayList<>();
        List<LayoutToken> tokenizationsBuffer = null;
        int p = 0; // position in tokenizations
        int i = 0;
        while (st1.hasMoreTokens()) {
            String row = st1.nextToken();
            String[] s = row.split("\t");
            String token = s[0].trim();
            int p0 = p;
            boolean strop = false;
            tokenizationsBuffer = new ArrayList<>();
            while ((!strop) && (p < tokenizations.size())) {
                String tokOriginal = tokenizations.get(p).getText().trim();
                if (openFigure)
                    tokenizationsFigure.add(tokenizations.get(p));
                tokenizationsBuffer.add(tokenizations.get(p));
                if (tokOriginal.equals(token)) {
                    strop = true;
                }
                p++;
            }
            if (p == tokenizations.size()) {
                // either we are at the end of the header, or we might have
                // a problematic token in tokenization for some reasons
                if ((p - p0) > 2) {
                    // we loose the synchronicity, so we reinit p for the next token
                    p = p0;
                    continue;
                }
            }

            int ll = s.length;
            String label = s[ll - 1];
            String plainLabel = GenericTaggerUtils.getPlainLabel(label);
            if (label.equals("<figure>") || ((label.equals("I-<figure>") && !openFigure))) {
                if (!openFigure) {
                    openFigure = true;
                    tokenizationsFigure.addAll(tokenizationsBuffer);
                }
                // we remove the label in the sequence labeling row
                int ind = row.lastIndexOf("\t");
                figureBlock.append(row, 0, ind).append("\n");
            } else if (label.equals("I-<figure>") || openFigure) {
                // remove last tokens
                if (tokenizationsFigure.size() > 0) {
                    int nbToRemove = tokenizationsBuffer.size();
                    for (int q = 0; q < nbToRemove; q++)
                        tokenizationsFigure.remove(tokenizationsFigure.size() - 1);
                }
                // parse the recognized figure area
//System.out.println(tokenizationsFigure.toString());
//System.out.println(figureBlock.toString()); 
                //adjustment
                if ((p != tokenizations.size()) && (tokenizations.get(p).getText().equals("\n") ||
                    tokenizations.get(p).getText().equals("\r") ||
                    tokenizations.get(p).getText().equals(" "))) {
                    tokenizationsFigure.add(tokenizations.get(p));
                    p++;
                }
                while ((tokenizationsFigure.size() > 0) &&
                    (tokenizationsFigure.get(0).getText().equals("\n") ||
                        tokenizationsFigure.get(0).getText().equals(" ")))
                    tokenizationsFigure.remove(0);

                // process the "accumulated" figure
                Pair<String, String> trainingData = parsers.getFigureParser()
                    .createTrainingData(tokenizationsFigure, figureBlock.toString(), "Fig" + nb);
                tokenizationsFigure = new ArrayList<>();
                figureBlock = new StringBuilder();
                if (trainingData != null) {
                    if (tei.length() == 0) {
                        tei.append(parsers.getFigureParser().getTEIHeader(id)).append("\n\n");
                    }
                    if (trainingData.getLeft() != null)
                        tei.append(trainingData.getLeft()).append("\n\n");
                    if (trainingData.getRight() != null)
                        featureVector.append(trainingData.getRight()).append("\n\n");
                }

                if (label.equals("I-<figure>")) {
                    tokenizationsFigure.addAll(tokenizationsBuffer);
                    int ind = row.lastIndexOf("\t");
                    figureBlock.append(row.substring(0, ind)).append("\n");
                } else {
                    openFigure = false;
                }
                nb++;
            } else
                openFigure = false;
        }

        // If there still an open figure
        if (openFigure) {
            while (CollectionUtils.isNotEmpty(tokenizationsFigure) &&
                (tokenizationsFigure.get(0).getText().equals("\n") ||
                    tokenizationsFigure.get(0).getText().equals(" "))
            ) {
                tokenizationsFigure.remove(0);
            }

            // process the "accumulated" figure
            Pair<String, String> trainingData = parsers.getFigureParser()
                .createTrainingData(tokenizationsFigure, figureBlock.toString(), "Fig" + nb);
            if (tei.length() == 0) {
                tei.append(parsers.getFigureParser().getTEIHeader(id)).append("\n\n");
            }
            if (trainingData.getLeft() != null)
                tei.append(trainingData.getLeft()).append("\n\n");
            if (trainingData.getRight() != null)
                featureVector.append(trainingData.getRight()).append("\n\n");
        }

        if (tei.length() != 0) {
            tei.append("\n    </text>\n" +
                "</tei>\n");
        }
        return Pair.of(tei.toString(), featureVector.toString());
    }

    /**
     * Process tables identified by the full text model
     */
    protected List<Table> processTables(
        String rese,
        List<LayoutToken> tokenizations,
        Document doc) {
        return processTables(rese, tokenizations, doc, 0);
    }

    protected List<Table> processTables(String rese,
                                        List<LayoutToken> tokenizations,
                                        Document doc,
                                        int startTableID) {
        List<Table> results = new ArrayList<>();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(FULLTEXT, rese, tokenizations, true);

        int tableId = startTableID;
        for (TaggingTokenCluster cluster : Iterables.filter(clusteror.cluster(),
            new TaggingTokenClusteror.LabelTypePredicate(TaggingLabels.TABLE))) {
            List<LayoutToken> tokenizationTable = cluster.concatTokens();
            List<Table> localResults = parsers.getTableParser().processing(
                tokenizationTable,
                cluster.getFeatureBlock()
            );

            for (Table result : localResults) {
                List<LayoutToken> localTokenizationTable = result.getLayoutTokens();
//                result.setRawLayoutTokens(tokenizationTable);

                // block setting: we restrict to the tokenization of this particular table
                SortedSet<Integer> blockPtrs = new TreeSet<>();
                for (LayoutToken lt : localTokenizationTable) {
                    if (!LayoutTokensUtil.spaceyToken(lt.t()) && !LayoutTokensUtil.newLineToken(lt.t())) {
                        blockPtrs.add(lt.getBlockPtr());
                    }
                }
                result.setBlockPtrs(blockPtrs);

                // page setting: the first token could be a space from previous page
                for (LayoutToken lt : localTokenizationTable) {
                    if (!LayoutTokensUtil.spaceyToken(lt.t()) && !LayoutTokensUtil.newLineToken(lt.t())) {
                        result.setPage(lt.getPage());
                        break;
                    }
                }
                results.add(result);
                result.setId(String.valueOf(tableId));
                tableId++;
            }
        }

        doc.setTables(results);
        doc.postProcessTables();

        return results;
    }

    protected void postProcessTableCaptions(
        List<Table> tables,
        Document doc
    ) {
        // further parse the caption
        for (Table table : tables) {
            if (CollectionUtils.isNotEmpty(table.getCaptionLayoutTokens())) {
                Pair<String, List<LayoutToken>> captionProcess = processShort(table.getCaptionLayoutTokens(), doc);
                table.setLabeledCaption(captionProcess.getLeft());
                table.setCaptionLayoutTokens(captionProcess.getRight());
            }
            if (CollectionUtils.isNotEmpty(table.getNoteLayoutTokens())) {
                Pair<String, List<LayoutToken>> noteProcess = processShort(table.getNoteLayoutTokens(), doc);
                table.setLabeledNote(noteProcess.getLeft());
                table.setNoteLayoutTokens(noteProcess.getRight());
            }
        }
    }

    /**
     * Create training data for the table as identified by the full text model.
     * Return the pair (TEI fragment, sequence labeling raw data).
     */
    protected Pair<String, String> processTrainingDataTables(
        String rese,
        List<LayoutToken> tokenizations,
        String id
    ) {
        StringBuilder tei = new StringBuilder();
        StringBuilder featureVector = new StringBuilder();
        int nb = 0;
        StringTokenizer st1 = new StringTokenizer(rese, "\n");
        boolean openTable = false;
        StringBuilder tableBlock = new StringBuilder();
        List<LayoutToken> tokenizationsTable = new ArrayList<LayoutToken>();
        List<LayoutToken> tokenizationsBuffer = null;
        int p = 0; // position in tokenizations
        int i = 0;
        while (st1.hasMoreTokens()) {
            String row = st1.nextToken();
            String[] s = row.split("\t");
            String token = s[0].trim();
//System.out.println(s0 + "\t" + tokenizations.get(p).getText().trim());
            int p0 = p;
            boolean strop = false;
            tokenizationsBuffer = new ArrayList<LayoutToken>();
            while ((!strop) && (p < tokenizations.size())) {
                String tokOriginal = tokenizations.get(p).getText().trim();
                if (openTable)
                    tokenizationsTable.add(tokenizations.get(p));
                tokenizationsBuffer.add(tokenizations.get(p));
                if (tokOriginal.equals(token)) {
                    strop = true;
                }
                p++;
            }
            if (p == tokenizations.size()) {
                // either we are at the end of the header, or we might have
                // a problematic token in tokenization for some reasons
                if ((p - p0) > 2) {
                    // we loose the synchronicity, so we reinit p for the next token
                    p = p0;
                    continue;
                }
            }

            int ll = s.length;
            String label = s[ll - 1];
            String plainLabel = GenericTaggerUtils.getPlainLabel(label);
            if (label.equals("<table>") || ((label.equals("I-<table>") && !openTable))) {
                if (!openTable) {
                    openTable = true;
                    tokenizationsTable.addAll(tokenizationsBuffer);
                }
                // we remove the label in the sequence labeling row
                int ind = row.lastIndexOf("\t");
                tableBlock.append(row.substring(0, ind)).append("\n");
            } else if (label.equals("I-<table>") || openTable) {
                // remove last tokens
                if (tokenizationsTable.size() > 0) {
                    int nbToRemove = tokenizationsBuffer.size();
                    for (int q = 0; q < nbToRemove; q++)
                        tokenizationsTable.remove(tokenizationsTable.size() - 1);
                }
                // parse the recognized table area
//System.out.println(tokenizationsTable.toString());
//System.out.println(tableBlock.toString()); 
                //adjustment
                if ((p != tokenizations.size()) && (tokenizations.get(p).getText().equals("\n") ||
                    tokenizations.get(p).getText().equals("\r") ||
                    tokenizations.get(p).getText().equals(" "))) {
                    tokenizationsTable.add(tokenizations.get(p));
                    p++;
                }
                while ((tokenizationsTable.size() > 0) &&
                    (tokenizationsTable.get(0).getText().equals("\n") ||
                        tokenizationsTable.get(0).getText().equals(" ")))
                    tokenizationsTable.remove(0);

                // process the "accumulated" table
                Pair<String, String> trainingData = parsers.getTableParser().createTrainingData(tokenizationsTable, tableBlock.toString(), "Fig" + nb);
                tokenizationsTable = new ArrayList<>();
                tableBlock = new StringBuilder();
                if (trainingData != null) {
                    if (tei.length() == 0) {
                        tei.append(parsers.getTableParser().getTEIHeader(id)).append("\n\n");
                    }
                    if (trainingData.getLeft() != null)
                        tei.append(trainingData.getLeft()).append("\n\n");
                    if (trainingData.getRight() != null)
                        featureVector.append(trainingData.getRight()).append("\n\n");
                }
                if (label.equals("I-<table>")) {
                    tokenizationsTable.addAll(tokenizationsBuffer);
                    int ind = row.lastIndexOf("\t");
                    tableBlock.append(row.substring(0, ind)).append("\n");
                } else {
                    openTable = false;
                }
                nb++;
            } else
                openTable = false;
        }

        // If there still an open table
        if (openTable) {
            while ((tokenizationsTable.size() > 0) &&
                (tokenizationsTable.get(0).getText().equals("\n") ||
                    tokenizationsTable.get(0).getText().equals(" ")))
                tokenizationsTable.remove(0);

            // process the "accumulated" figure
            Pair<String, String> trainingData = parsers.getTableParser()
                .createTrainingData(tokenizationsTable, tableBlock.toString(), "Fig" + nb);
            if (tei.length() == 0) {
                tei.append(parsers.getTableParser().getTEIHeader(id)).append("\n\n");
            }
            if (trainingData.getLeft() != null)
                tei.append(trainingData.getLeft()).append("\n\n");
            if (trainingData.getRight() != null)
                featureVector.append(trainingData.getRight()).append("\n\n");
        }

        if (tei.length() != 0) {
            tei.append("\n    </text>\n" +
                "</tei>\n");
        }
        return Pair.of(tei.toString(), featureVector.toString());
    }

    /**
     * Process equations identified by the full text model
     */
    protected List<Equation> processEquations(String rese, List<LayoutToken> layoutTokens) {
        return processEquations(rese, layoutTokens, 0);
    }

    protected List<Equation> processEquations(
        String rese,
        List<LayoutToken> tokenizations,
        int startEquationID
    ) {
        List<Equation> results = new ArrayList<>();
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(FULLTEXT, rese, tokenizations, true);
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        int equationID = startEquationID;

        Equation currentResult = null;
        TaggingLabel lastLabel = null;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);
            if ((clusterLabel != TaggingLabels.EQUATION) && (clusterLabel != TaggingLabels.EQUATION_LABEL)) {
                lastLabel = clusterLabel;
                if (currentResult != null) {
                    results.add(currentResult);
                    currentResult.setId(String.valueOf(equationID));
                    currentResult = null;
                }
                continue;
            }

            List<LayoutToken> tokenizationEquation = cluster.concatTokens();
            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));

            if (currentResult == null)
                currentResult = new Equation();

            if ((!currentResult.getContent().isEmpty()) && (!currentResult.getLabel().isEmpty())) {
                results.add(currentResult);
                currentResult = new Equation();
            }
            if (clusterLabel.equals(TaggingLabels.EQUATION)) {
                if (!currentResult.getContent().isEmpty()) {
                    results.add(currentResult);
                    currentResult = new Equation();
                }
                currentResult.appendContent(clusterContent);
                currentResult.addLayoutTokens(cluster.concatTokens());
            } else if (clusterLabel.equals(TaggingLabels.EQUATION_LABEL)) {
                currentResult.appendLabel(clusterContent);
                currentResult.addLayoutTokens(cluster.concatTokens());
            }

            lastLabel = clusterLabel;
        }

        // add last open result
        if (currentResult != null) {
            results.add(currentResult);
        }

        final AtomicInteger equationsIndex = new AtomicInteger(startEquationID);
        results = results.stream()
                .peek(e -> e.setId(String.valueOf(equationsIndex.getAndIncrement())))
                .collect(Collectors.toList());

        return results;
    }

    /**
     * Ensure consistent use of callouts in the entire document body
     */
    private List<MarkerType> postProcessCallout(String result, LayoutTokenization layoutTokenization) {
        if (layoutTokenization == null)
            return null;

        List<LayoutToken> tokenizations = layoutTokenization.getTokenization();

        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(FULLTEXT, result, tokenizations);
        String tokenLabel = null;
        List<TaggingTokenCluster> clusters = clusteror.cluster();

        MarkerType majorityReferenceMarkerType = MarkerType.UNKNOWN;
        MarkerType majorityFigureMarkerType = MarkerType.UNKNOWN;
        MarkerType majorityTableMarkerType = MarkerType.UNKNOWN;
        MarkerType majorityEquationarkerType = MarkerType.UNKNOWN;

        Map<MarkerType, Integer> referenceMarkerTypeCounts = new HashMap<>();
        Map<MarkerType, Integer> figureMarkerTypeCounts = new HashMap<>();
        Map<MarkerType, Integer> tableMarkerTypeCounts = new HashMap<>();
        Map<MarkerType, Integer> equationMarkerTypeCounts = new HashMap<>();

        List<String> referenceMarkerSeen = new ArrayList<>();
        List<String> figureMarkerSeen = new ArrayList<>();
        List<String> tableMarkerSeen = new ArrayList<>();
        List<String> equationMarkerSeen = new ArrayList<>();

        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            if (TEIFormatter.MARKER_LABELS.contains(clusterLabel)) {
                List<LayoutToken> refTokens = cluster.concatTokens();
                refTokens = LayoutTokensUtil.dehyphenize(refTokens);
                String refText = LayoutTokensUtil.toText(refTokens);
                refText = refText.replace("\n", "");
                refText = refText.replace(" ", "");
                if (refText.trim().length() == 0)
                    continue;

                if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                    if (referenceMarkerSeen.contains(refText)) {
                        // already seen reference marker sequence, we skip it
                        continue;
                    }
                    MarkerType localMarkerType = CalloutAnalyzer.getCalloutType(refTokens);
                    //System.out.println(LayoutTokensUtil.toText(refTokens) + " -> " + localMarkerType);
                    if (referenceMarkerTypeCounts.get(localMarkerType) == null)
                        referenceMarkerTypeCounts.put(localMarkerType, 1);
                    else
                        referenceMarkerTypeCounts.put(localMarkerType, referenceMarkerTypeCounts.get(localMarkerType) + 1);

                    if (!referenceMarkerSeen.contains(refText))
                        referenceMarkerSeen.add(refText);
                } else if (clusterLabel.equals(TaggingLabels.FIGURE_MARKER)) {
                    if (figureMarkerSeen.contains(refText)) {
                        // already seen reference marker sequence, we skip it
                        continue;
                    }
                    MarkerType localMarkerType = CalloutAnalyzer.getCalloutType(refTokens);
                    if (figureMarkerTypeCounts.get(localMarkerType) == null)
                        figureMarkerTypeCounts.put(localMarkerType, 1);
                    else
                        figureMarkerTypeCounts.put(localMarkerType, figureMarkerTypeCounts.get(localMarkerType) + 1);

                    if (!figureMarkerSeen.contains(refText))
                        figureMarkerSeen.add(refText);
                } else if (clusterLabel.equals(TaggingLabels.TABLE_MARKER)) {
                    if (tableMarkerSeen.contains(refText)) {
                        // already seen reference marker sequence, we skip it
                        continue;
                    }
                    MarkerType localMarkerType = CalloutAnalyzer.getCalloutType(refTokens);
                    if (tableMarkerTypeCounts.get(localMarkerType) == null)
                        tableMarkerTypeCounts.put(localMarkerType, 1);
                    else
                        tableMarkerTypeCounts.put(localMarkerType, tableMarkerTypeCounts.get(localMarkerType) + 1);

                    if (!tableMarkerSeen.contains(refText))
                        tableMarkerSeen.add(refText);
                } else if (clusterLabel.equals(TaggingLabels.EQUATION_MARKER)) {
                    if (equationMarkerSeen.contains(refText)) {
                        // already seen reference marker sequence, we skip it
                        continue;
                    }
                    MarkerType localMarkerType = CalloutAnalyzer.getCalloutType(refTokens);
                    if (equationMarkerTypeCounts.get(localMarkerType) == null)
                        equationMarkerTypeCounts.put(localMarkerType, 1);
                    else
                        equationMarkerTypeCounts.put(localMarkerType, equationMarkerTypeCounts.get(localMarkerType) + 1);

                    if (!equationMarkerSeen.contains(refText))
                        equationMarkerSeen.add(refText);
                }
            }
        }

        majorityReferenceMarkerType = getBestType(referenceMarkerTypeCounts);
        majorityFigureMarkerType = getBestType(figureMarkerTypeCounts);
        majorityTableMarkerType = getBestType(tableMarkerTypeCounts);
        majorityEquationarkerType = getBestType(equationMarkerTypeCounts);

/*System.out.println("majorityReferenceMarkerType: " + majorityReferenceMarkerType);
System.out.println("majorityFigureMarkerType: " + majorityFigureMarkerType);
System.out.println("majorityTableMarkerType: " + majorityTableMarkerType);
System.out.println("majorityEquationarkerType: " + majorityEquationarkerType);*/

        return Arrays.asList(majorityReferenceMarkerType, majorityFigureMarkerType, majorityTableMarkerType, majorityEquationarkerType);
    }

    private static MarkerType getBestType(Map<MarkerType, Integer> markerTypeCount) {
        MarkerType bestType = MarkerType.UNKNOWN;
        int maxCount = 0;
        for (Map.Entry<MarkerType, Integer> entry : markerTypeCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                bestType = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        return bestType;
    }

    /**
     * Create the TEI representation for a document based on the parsed header, references
     * and body sections.
     */
    private void toTEI(Document doc,
                       String bodyLabellingResult,
                       String annexLabellingResult,
                       LayoutTokenization layoutTokenization,
                       List<LayoutToken> tokenizationsAnnex,
                       BiblioItem resHeader,
                       List<Figure> bodyFigures,
                       List<Table> bodyTables,
                       List<Equation> bodyEquations,
                       List<Figure> annexFigures,
                       List<Table> annexTables,
                       List<Equation> annexEquations,
                       List<MarkerType> markerTypes,
                       GrobidAnalysisConfig config) {
        if (doc.getBlocks() == null) {
            return;
        }
        List<BibDataSet> resCitations = doc.getBibDataSets();
        TEIFormatter teiFormatter = new TEIFormatter(doc, this);
        StringBuilder tei = new StringBuilder();
        try {
            List<Funding> fundings = new ArrayList<>();
            List<Affiliation> affiliations = new ArrayList<>();

            List<String> annexStatements = new ArrayList<>();

            // acknowledgement is in the back
            StringBuilder acknowledgmentStmt = getSectionAsTEI("acknowledgement", "\t\t\t", doc, SegmentationLabels.ACKNOWLEDGEMENT,
                teiFormatter, resCitations, config);

            if (acknowledgmentStmt.length() > 0) {
                MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                    parsers.getFundingAcknowledgementParser().processingXmlFragment(acknowledgmentStmt.toString(), config);

                if (localResult != null && localResult.getLeft() != null) {
                    String localTei = localResult.getLeft().toXML();
                    localTei = localTei.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    annexStatements.add(localTei);
                } else {
                    annexStatements.add(acknowledgmentStmt.toString());
                }

                if (localResult != null && localResult.getRight() != null) {
                    if (localResult.getRight().getLeft() != null) {
                        List<Funding> localFundings = localResult.getRight().getLeft();
                        if (CollectionUtils.isNotEmpty(localFundings)) {
                            fundings.addAll(localFundings);
                        }
                    }

                    if (localResult.getRight().getRight() != null) {
                        List<Affiliation> localAffiliations = localResult.getRight().getRight();
                        if (CollectionUtils.isNotEmpty(localAffiliations)) {
                            affiliations.addAll(localAffiliations);
                        }
                    }
                }
            }

            // funding in header
            StringBuilder fundingStmt = new StringBuilder();
            if (StringUtils.isNotBlank(resHeader.getFunding())) {
                List<LayoutToken> headerFundingTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_FUNDING);

                Pair<String, List<LayoutToken>> headerFundingProcessed = processShort(headerFundingTokens, doc);
                if (headerFundingProcessed != null) {
                    fundingStmt = teiFormatter.processTEIDivSection("funding",
                        "\t\t\t",
                        headerFundingProcessed.getLeft(),
                        headerFundingProcessed.getRight(),
                        resCitations,
                        config);
                }
                if (StringUtils.isNotBlank(fundingStmt)) {
                    MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                        parsers.getFundingAcknowledgementParser().processingXmlFragment(fundingStmt.toString(), config);

                    if (localResult != null && localResult.getLeft() != null) {
                        String localTEI = localResult.getLeft().toXML();
                        localTEI = localTEI.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                        annexStatements.add(localTEI);
                    } else {
                        annexStatements.add(fundingStmt.toString());
                    }

                    if (localResult != null && localResult.getRight() != null) {
                        if (localResult.getRight().getLeft() != null) {
                            List<Funding> localFundings = localResult.getRight().getLeft();
                            if (CollectionUtils.isNotEmpty(localFundings)) {
                                fundings.addAll(localFundings);
                            }
                        }

                        if (localResult.getRight().getRight() != null) {
                            List<Affiliation> localAffiliations = localResult.getRight().getRight();
                            if (CollectionUtils.isNotEmpty(localAffiliations)) {
                                affiliations.addAll(localAffiliations);
                            }
                        }
                    }
                }
            }

            // funding statements in non-header part
            fundingStmt = getSectionAsTEI("funding",
                "\t\t\t",
                doc,
                SegmentationLabels.FUNDING,
                teiFormatter,
                resCitations,
                config);
            if (fundingStmt.length() > 0) {
                MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                    parsers.getFundingAcknowledgementParser().processingXmlFragment(fundingStmt.toString(), config);

                if (localResult != null && localResult.getLeft() != null) {
                    String localTEI = localResult.getLeft().toXML();
                    localTEI = localTEI.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    annexStatements.add(localTEI);
                } else {
                    annexStatements.add(fundingStmt.toString());
                }

                if (localResult != null && localResult.getRight() != null) {
                    if (localResult.getRight().getLeft() != null) {
                        List<Funding> localFundings = localResult.getRight().getLeft();
                        if (CollectionUtils.isNotEmpty(localFundings)) {
                            fundings.addAll(localFundings);
                        }
                    }

                    if (localResult.getRight().getRight() != null) {
                        List<Affiliation> localAffiliations = localResult.getRight().getRight();
                        if (CollectionUtils.isNotEmpty(localAffiliations)) {
                            affiliations.addAll(localAffiliations);
                        }
                    }
                }
            }

            tei.append(teiFormatter.toTEIHeader(
                resHeader,
                null,
                resCitations,
                markerTypes,
                fundings,
                config)
            );

            tei = teiFormatter.toTEIBody(
                tei,
                bodyLabellingResult,
                resHeader,
                resCitations,
                layoutTokenization,
                bodyFigures,
                bodyTables,
                bodyEquations,
                markerTypes,
                doc,
                config);

            tei.append("\t\t<back>\n");

            for (String annexStatement : annexStatements) {
                tei.append("\n\t\t\t");
                tei.append(annexStatement);
            }

            if (CollectionUtils.isNotEmpty(fundings)) {
                tei.append("\n\t\t\t<listOrg type=\"funding\">\n");
                for (Funding funding : fundings) {
                    if (funding.isNonEmptyFunding())
                        tei.append(funding.toTEI(4));
                }
                tei.append("\t\t\t</listOrg>\n");
            }

            if (CollectionUtils.isNotEmpty(affiliations)) {

                // check if we have at least one acknowledged research infrastructure here
                List<Affiliation> filteredInfrastructures = new ArrayList<>();
                for (Affiliation affiliation : affiliations) {
                    if (affiliation.getAffiliationString() != null && affiliation.getAffiliationString().length() > 0 && affiliation.isInfrastructure())
                        filteredInfrastructures.add(affiliation);
                    else if (affiliation.getAffiliationString() != null && affiliation.getAffiliationString().length() > 0) {
                        // check if this organization is a known infrastructure
                        List<Lexicon.OrganizationRecord> localOrganizationNamings =
                            Lexicon.getInstance().getOrganizationNamingInfo(affiliation.getAffiliationString());
                        if (localOrganizationNamings != null && localOrganizationNamings.size() > 0) {
                            filteredInfrastructures.add(affiliation);
                        }
                    }
                }

                // serialize acknowledged research infrastructure, if any
                if (filteredInfrastructures.size() > 0) {
                    tei.append("\n\t\t\t<listOrg type=\"infrastructure\">\n");
                    for (Affiliation affiliation : filteredInfrastructures) {
                        List<Lexicon.OrganizationRecord> localOrganizationNamings =
                            Lexicon.getInstance().getOrganizationNamingInfo(affiliation.getAffiliationString());
                        tei.append("\t\t\t\t<org type=\"infrastructure\">");
                        tei.append("\t\t\t\t\t<orgName type=\"extracted\">");
                        tei.append(TextUtilities.HTMLEncode(affiliation.getAffiliationString()));
                        tei.append("</orgName>\n");
                        if (localOrganizationNamings != null && localOrganizationNamings.size() > 0) {
                            for (Lexicon.OrganizationRecord orgRecord : localOrganizationNamings) {
                                if (isNotBlank(orgRecord.fullName)) {
                                    tei.append("\t\t\t\t\t<orgName type=\"full\"");
                                    if (isNotBlank(orgRecord.lang))
                                        tei.append(" lang=\"" + orgRecord.lang + "\"");
                                    tei.append(">");
                                    tei.append(TextUtilities.HTMLEncode(orgRecord.fullName));
                                    tei.append("</orgName>\n");
                                }
                            }
                        }
                        tei.append("\t\t\t\t</org>\n");
                    }

                    tei.append("\t\t\t</listOrg>\n");
                }
            }

            // availability statements in header
            StringBuilder availabilityStmt = new StringBuilder();
            if (StringUtils.isNotBlank(resHeader.getAvailabilityStmt())) {
                List<LayoutToken> headerAvailabilityStatementTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_AVAILABILITY);
                Pair<String, List<LayoutToken>> headerAvailabilityProcessed = processShort(headerAvailabilityStatementTokens, doc);
                if (headerAvailabilityProcessed != null) {
                    availabilityStmt = teiFormatter.processTEIDivSection("availability",
                        "\t\t\t",
                        headerAvailabilityProcessed.getLeft(),
                        headerAvailabilityProcessed.getRight(),
                        resCitations,
                        config);
                }
                if (availabilityStmt.length() > 0) {
                    tei.append(availabilityStmt.toString());
                }
            }

            // availability statements in non-header part
            availabilityStmt = getSectionAsTEI("availability",
                "\t\t\t",
                doc,
                SegmentationLabels.AVAILABILITY,
                teiFormatter,
                resCitations,
                config);
            if (availabilityStmt.length() > 0) {
                tei.append(availabilityStmt.toString());
            }

            tei = teiFormatter.toTEIAnnex(
                tei, annexLabellingResult, resHeader, resCitations,
                tokenizationsAnnex, annexFigures, annexTables, annexEquations, markerTypes, doc, config
            );

            tei = teiFormatter.toTEIReferences(tei, resCitations, config);
            doc.calculateTeiIdToBibDataSets();

            tei.append("\t\t</back>\n");

            tei.append("\t</text>\n");
            tei.append("</TEI>\n");
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        doc.setTei(tei.toString());

        //TODO: reevaluate
//		doc.setTei(
//				XmlBuilderUtils.toPrettyXml(
//						XmlBuilderUtils.fromString(tei.toString())
//				)
//		);
    }

    /**
     * Create the TEI representation for a document based on the parsed header and funding only.
     */
    private void toTEIHeaderFunding(Document doc,
                                    BiblioItem resHeader,
                                    GrobidAnalysisConfig config) {
        if (doc.getBlocks() == null) {
            return;
        }
        TEIFormatter teiFormatter = new TEIFormatter(doc, this);
        StringBuilder tei = new StringBuilder();
        try {
            List<Funding> fundings = new ArrayList<>();
            List<Affiliation> affiliations = new ArrayList<>();

            List<String> annexStatements = new ArrayList<>();

            // acknowledgement is in the back
            StringBuilder acknowledgmentStmt = getSectionAsTEI("acknowledgement", "\t\t\t", doc, SegmentationLabels.ACKNOWLEDGEMENT,
                teiFormatter, null, config);

            if (acknowledgmentStmt.length() > 0) {
                MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                    parsers.getFundingAcknowledgementParser().processingXmlFragment(acknowledgmentStmt.toString(), config);

                if (localResult != null && localResult.getLeft() != null) {
                    String local_tei = localResult.getLeft().toXML();
                    local_tei = local_tei.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    annexStatements.add(local_tei);
                } else {
                    annexStatements.add(acknowledgmentStmt.toString());
                }

                if (localResult != null && localResult.getRight() != null) {
                    if (localResult.getRight().getLeft() != null) {
                        List<Funding> localFundings = localResult.getRight().getLeft();
                        if (localFundings.size() > 0) {
                            fundings.addAll(localFundings);
                        }
                    }

                    if (localResult.getRight().getRight() != null) {
                        List<Affiliation> localAffiliations = localResult.getRight().getRight();
                        if (localAffiliations.size() > 0) {
                            affiliations.addAll(localAffiliations);
                        }
                    }
                }
            }

            // funding in header
            StringBuilder fundingStmt = new StringBuilder();
            if (StringUtils.isNotBlank(resHeader.getFunding())) {
                List<LayoutToken> headerFundingTokens = resHeader.getLayoutTokens(TaggingLabels.HEADER_FUNDING);

                Pair<String, List<LayoutToken>> headerFundingProcessed = processShort(headerFundingTokens, doc);
                if (headerFundingProcessed != null) {
                    fundingStmt = teiFormatter.processTEIDivSection("funding",
                        "\t\t\t",
                        headerFundingProcessed.getLeft(),
                        headerFundingProcessed.getRight(),
                        null,
                        config);
                }
                if (fundingStmt.length() > 0) {
                    MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                        parsers.getFundingAcknowledgementParser().processingXmlFragment(fundingStmt.toString(), config);

                    if (localResult != null && localResult.getLeft() != null) {
                        String local_tei = localResult.getLeft().toXML();
                        local_tei = local_tei.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                        annexStatements.add(local_tei);
                    } else {
                        annexStatements.add(fundingStmt.toString());
                    }

                    if (localResult != null && localResult.getRight() != null) {
                        if (localResult.getRight().getLeft() != null) {
                            List<Funding> localFundings = localResult.getRight().getLeft();
                            if (localFundings.size() > 0) {
                                fundings.addAll(localFundings);
                            }
                        }

                        if (localResult.getRight().getRight() != null) {
                            List<Affiliation> localAffiliations = localResult.getRight().getRight();
                            if (localAffiliations.size() > 0) {
                                affiliations.addAll(localAffiliations);
                            }
                        }
                    }
                }
            }

            // funding statements in non-header part
            fundingStmt = getSectionAsTEI("funding",
                "\t\t\t",
                doc,
                SegmentationLabels.FUNDING,
                teiFormatter,
                null,
                config);
            if (fundingStmt.length() > 0) {
                MutablePair<Element, MutableTriple<List<Funding>, List<Person>, List<Affiliation>>> localResult =
                    parsers.getFundingAcknowledgementParser().processingXmlFragment(fundingStmt.toString(), config);

                if (localResult != null && localResult.getLeft() != null) {
                    String local_tei = localResult.getLeft().toXML();
                    local_tei = local_tei.replace(" xmlns=\"http://www.tei-c.org/ns/1.0\"", "");
                    annexStatements.add(local_tei);
                } else {
                    annexStatements.add(fundingStmt.toString());
                }

                if (localResult != null && localResult.getRight() != null) {
                    if (localResult.getRight().getLeft() != null) {
                        List<Funding> localFundings = localResult.getRight().getLeft();
                        if (localFundings.size() > 0) {
                            fundings.addAll(localFundings);
                        }
                    }

                    if (localResult.getRight().getRight() != null) {
                        List<Affiliation> localAffiliations = localResult.getRight().getRight();
                        if (localAffiliations.size() > 0) {
                            affiliations.addAll(localAffiliations);
                        }
                    }
                }
            }

            tei.append(teiFormatter.toTEIHeader(resHeader, null, null, null, fundings, config));
            tei.append("\t\t<back>");

            for (String annexStatement : annexStatements) {
                tei.append("\n\t\t\t");
                tei.append(annexStatement);
            }

            if (CollectionUtils.isNotEmpty(fundings)) {
                tei.append("\n\t\t\t<listOrg type=\"funding\">\n");
                for (Funding funding : fundings) {
                    if (funding.isNonEmptyFunding())
                        tei.append(funding.toTEI(4));
                }
                tei.append("\t\t\t</listOrg>\n");
            }

            if (CollectionUtils.isNotEmpty(affiliations)) {

                // check if we have at least one acknowledged research infrastructure here
                List<Affiliation> filteredInfrastructures = new ArrayList<>();
                for (Affiliation affiliation : affiliations) {
                    if (affiliation.getAffiliationString() != null && affiliation.getAffiliationString().length() > 0 && affiliation.isInfrastructure())
                        filteredInfrastructures.add(affiliation);
                    else if (affiliation.getAffiliationString() != null && affiliation.getAffiliationString().length() > 0) {
                        // check if this organization is a known infrastructure
                        List<Lexicon.OrganizationRecord> localOrganizationNamings =
                            Lexicon.getInstance().getOrganizationNamingInfo(affiliation.getAffiliationString());
                        if (localOrganizationNamings != null && localOrganizationNamings.size() > 0) {
                            filteredInfrastructures.add(affiliation);
                        }
                    }
                }

                // serialize acknowledged research infrastructure, if any
                if (filteredInfrastructures.size() > 0) {
                    tei.append("\n\t\t\t<listOrg type=\"infrastructure\">\n");
                    for (Affiliation affiliation : filteredInfrastructures) {
                        List<Lexicon.OrganizationRecord> localOrganizationNamings =
                            Lexicon.getInstance().getOrganizationNamingInfo(affiliation.getAffiliationString());
                        tei.append("\t\t\t\t<org type=\"infrastructure\">");
                        tei.append("\t\t\t\t\t<orgName type=\"extracted\">");
                        tei.append(TextUtilities.HTMLEncode(affiliation.getAffiliationString()));
                        tei.append("</orgName>\n");
                        if (localOrganizationNamings != null && localOrganizationNamings.size() > 0) {
                            for (Lexicon.OrganizationRecord orgRecord : localOrganizationNamings) {
                                if (isNotBlank(orgRecord.fullName)) {
                                    tei.append("\t\t\t\t\t<orgName type=\"full\"");
                                    if (isNotBlank(orgRecord.lang))
                                        tei.append(" lang=\"" + orgRecord.lang + "\"");
                                    tei.append(">");
                                    tei.append(TextUtilities.HTMLEncode(orgRecord.fullName));
                                    tei.append("</orgName>\n");
                                }
                            }
                        }
                        tei.append("\t\t\t\t</org>\n");
                    }

                    tei.append("\t\t\t</listOrg>\n");
                }
            }

            tei.append("\t\t</back>\n");

            tei.append("\t</text>\n");
            tei.append("</TEI>\n");
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        doc.setTei(tei.toString());
    }

    private StringBuilder getSectionAsTEI(String xmlType,
                                          String indentation,
                                          Document doc,
                                          TaggingLabel taggingLabel,
                                          TEIFormatter teiFormatter,
                                          List<BibDataSet> resCitations,
                                          GrobidAnalysisConfig config) throws Exception {
        StringBuilder output = new StringBuilder();
        SortedSet<DocumentPiece> sectionPart = doc.getDocumentPart(taggingLabel);

        if (CollectionUtils.isNotEmpty(sectionPart)) {
            Pair<String, LayoutTokenization> sectionTokenisation = getBodyTextFeatured(doc, sectionPart);
            if (sectionTokenisation != null) {
                // if featSeg is null, it usually means that no body segment is found in the
                // document segmentation
                String text = sectionTokenisation.getLeft();
                List<LayoutToken> tokens = sectionTokenisation.getRight().getTokenization();
                String resultLabelling = null;
                if (StringUtils.isNotBlank(text)) {
                    resultLabelling = label(text);
                    resultLabelling = postProcessFullTextLabeledText(resultLabelling);
                }
                output = teiFormatter.processTEIDivSection(xmlType, indentation, resultLabelling, tokens, resCitations, config);
            }
        }
        return output;
    }

    private static List<TaggingLabel> inlineFullTextLabels = Arrays.asList(TaggingLabels.CITATION_MARKER, TaggingLabels.TABLE_MARKER,
        TaggingLabels.FIGURE_MARKER, TaggingLabels.EQUATION_LABEL);

    public static List<LayoutTokenization> getDocumentFullTextTokens(List<TaggingLabel> labels, String labeledResult, List<LayoutToken> tokenizations) {
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(FULLTEXT, labeledResult, tokenizations);
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        List<LayoutTokenization> labeledTokenSequences = new ArrayList<LayoutTokenization>();
        LayoutTokenization currentTokenization = null;
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            List<LayoutToken> clusterTokens = cluster.concatTokens();

            if (inlineFullTextLabels.contains(clusterLabel)) {
                // sequence is not interrupted
                if (currentTokenization == null)
                    currentTokenization = new LayoutTokenization();

            } else {
                // we have an independent sequence
                if ((currentTokenization != null) && (currentTokenization.size() > 0)) {
                    labeledTokenSequences.add(currentTokenization);
                    currentTokenization = new LayoutTokenization();
                }
            }
            if (labels.contains(clusterLabel)) {
                if (currentTokenization == null)
                    currentTokenization = new LayoutTokenization();
                currentTokenization.addTokens(clusterTokens);
            }
        }

        if ((currentTokenization != null) && (currentTokenization.size() > 0))
            labeledTokenSequences.add(currentTokenization);

        return labeledTokenSequences;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}