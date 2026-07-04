/*
 * Copyright 2008-2026 GROBID contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grobid.core.engines;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.grobid.core.GrobidModels;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Date;
import org.grobid.core.document.Document;
import org.grobid.core.document.DocumentSource;
import org.grobid.core.engines.citations.LabeledReferenceResult;
import org.grobid.core.engines.citations.ReferenceSegmenter;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.counters.CitationParserCounters;
import org.grobid.core.engines.label.SegmentationLabels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorCitation;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.layout.PDFAnnotation;
import org.grobid.core.layout.PDFAnnotation.Type;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.Consolidation;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;
import org.grobid.core.utilities.counters.CntManager;

public class CitationParser extends AbstractParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitationParser.class);

    public Lexicon lexicon = Lexicon.getInstance();
    private EngineParsers parsers;

    public CitationParser(EngineParsers parsers, CntManager cntManager) {
        super(GrobidModels.CITATION, cntManager);
        this.parsers = parsers;
    }

    public CitationParser(EngineParsers parsers) {
        super(GrobidModels.CITATION);
        this.parsers = parsers;
    }

    /**
     * Process one single raw reference string
     */
    public BiblioItem processingString(String input, int consolidate) {
        List<String> inputs = new ArrayList<>();
        input = TextUtilities.removeLeadingAndTrailingChars(input, "[({.,])}: \n", " \n");
        inputs.add(input);
        List<BiblioItem> result = processingStringMultiple(inputs, consolidate);
        if (result != null && result.size() > 0)
            return result.get(0);
        else
            return null;
    }

    /**
     * Process a list of raw reference strings by taking advantage of batch processing
     * when a DeLFT deep learning model is used
     */
    public List<BiblioItem> processingStringMultiple(List<String> inputs, int consolidate) {
        if (inputs == null || inputs.size() == 0)
            return null;
        List<List<LayoutToken>> tokenList = new ArrayList<>();
        for (String input : inputs) {
            // normalize first so non-breaking spaces etc. become regular spaces
            input = UnicodeUtil.normaliseText(input);
            if (StringUtils.isBlank(input)) {
                tokenList.add(new ArrayList<LayoutToken>());
            } else {
                input = TextUtilities.removeLeadingAndTrailingChars(input, "[({.,])}: \n", " \n");
                if (StringUtils.isBlank(input)) {
                    tokenList.add(new ArrayList<LayoutToken>());
                } else {
                    List<LayoutToken> tokens = analyzer.tokenizeWithLayoutToken(input);
                    tokens = analyzer.retokenizeSubdigitsFromLayoutToken(tokens);
                    tokenList.add(tokens);
                }
            }
        }

        List<BiblioItem> results = processingLayoutTokenMultiple(tokenList, consolidate);
        if (results != null && results.size() == inputs.size()) {
            // store original references to enable optional raw output
            int i = 0;
            for (BiblioItem result : results) {
                if (result != null) {
                    String localInput = inputs.get(i);
                    localInput = TextUtilities.removeLeadingAndTrailingChars(localInput, "[({.,])}: \n", " \n");
                    result.setReference(localInput);
                }
                i++;
            }
        }
        return results;
    }

    /**
     * Process one single raw reference string tokenized as layout objects
     */
    public BiblioItem processingLayoutToken(List<LayoutToken> tokens, int consolidate) {
        List<List<LayoutToken>> tokenList = new ArrayList<>();
        tokenList.add(tokens);
        List<BiblioItem> result = processingLayoutTokenMultiple(tokenList, consolidate);
        if (result != null && result.size() > 0)
            return result.get(0);
        else
            return null;
    }

    /**
     * Process a list of raw reference string, each one tokenized as layout objects, and taking advantage
     * of batch processing when a DeLFT deep learning model is used
     */
    public List<BiblioItem> processingLayoutTokenMultiple(List<List<LayoutToken>> tokenList, int consolidate) {
        warnIfDebugUncaptured("CitationParser.processingLayoutTokenMultiple(List, int)");
        return processingLayoutTokenMultiple(tokenList, consolidate, null);
    }

    public List<BiblioItem> processingLayoutTokenMultiple(
            List<List<LayoutToken>> tokenList,
            int consolidate,
            GrobidAnalysisConfig config) {
        if (tokenList == null || tokenList.size() == 0)
            return null;
        List<BiblioItem> results = new ArrayList<>();
        StringBuilder featuredInput = new StringBuilder();

        int p = 0;
        for (List<LayoutToken> tokens : tokenList) {
            tokenList.set(p, analyzer.retokenizeSubdigitsFromLayoutToken(tokens));
            p++;
        }

        for (List<LayoutToken> tokens : tokenList) {
            if (CollectionUtils.isEmpty(tokens))
                continue;

            List<OffsetPosition> journalsPositions = lexicon.tokenPositionsJournalNames(tokens);
            List<OffsetPosition> abbrevJournalsPositions = lexicon.tokenPositionsAbbrevJournalNames(tokens);
            List<OffsetPosition> conferencesPositions = lexicon.tokenPositionsConferenceNames(tokens);
            List<OffsetPosition> publishersPositions = lexicon.tokenPositionsPublisherNames(tokens);
            List<OffsetPosition> locationsPositions = lexicon.tokenPositionsLocationNames(tokens);
            List<OffsetPosition> collaborationsPositions = lexicon.tokenPositionsCollaborationNames(tokens);
            List<OffsetPosition> identifiersPositions = lexicon.tokenPositionsIdentifierPattern(tokens);
            List<OffsetPosition> urlPositions = lexicon.tokenPositionsUrlPattern(tokens);

            try {
                String featuredBlock = FeaturesVectorCitation.addFeaturesCitation(
                        tokens,
                        null,
                        journalsPositions,
                        abbrevJournalsPositions,
                        conferencesPositions,
                        publishersPositions,
                        locationsPositions,
                        collaborationsPositions,
                        identifiersPositions,
                        urlPositions);

                featuredInput.append(featuredBlock);
                featuredInput.append("\n\n");
            } catch (Exception e) {
                LOGGER.error("An exception occurred while adding features for processing a citation.", e);
            }
        }

        if (featuredInput.toString().length() == 0)
            return null;

        String allRes = null;
        try {
            allRes = labelAndCapture(featuredInput.toString(), config);
        } catch (Exception e) {
            LOGGER.error("An exception occurred while labeling a citation.", e);
            throw new GrobidException(
                    "An exception occurred while labeling a citation.", e);
        }

        if (allRes == null || allRes.length() == 0)
            return null;
        String[] resBlocks = allRes.split("\n\n");
        int i = 0;
        for (List<LayoutToken> tokens : tokenList) {
            if (CollectionUtils.isEmpty(tokens))
                results.add(null);
            else {
                String res = resBlocks[i];
                i++;
                BiblioItem resCitation = resultExtractionLayoutTokens(res, true, tokens);

                // post-processing (additional field parsing and cleaning)
                if (resCitation != null) {
                    BiblioItem.cleanTitles(resCitation);

                    resCitation.setOriginalAuthors(resCitation.getAuthors());
                    try {
                        resCitation
                                .setFullAuthors(
                                        parsers.getAuthorParser().processingCitation(resCitation.getAuthors(), config));
                    } catch (Exception e) {
                        LOGGER.error("An exception occurred when processing author names of a citation.", e);
                    }
                    if (resCitation.getPublicationDate() != null) {
                        List<Date> dates = parsers.getDateParser()
                                .process(
                                        resCitation
                                                .getPublicationDate(),
                                        config);
                        if (dates != null) {
                            Date bestDate = null;
                            if (dates.size() > 0) {
                                // we take the earliest most specified date
                                for (Date theDate : dates) {
                                    if (bestDate == null) {
                                        bestDate = theDate;
                                    } else {
                                        if (bestDate.compareTo(theDate) == 1) {
                                            bestDate = theDate;
                                        }
                                    }
                                }
                                if (bestDate != null) {
                                    resCitation.setNormalizedPublicationDate(bestDate);
                                }
                            }
                        }
                    }

                    resCitation.setPageRange(
                            TextUtilities.cleanField(
                                    resCitation.getPageRange(),
                                    true));
                    resCitation.setPublisher(
                            TextUtilities.cleanField(
                                    resCitation.getPublisher(),
                                    true));
                    resCitation.setJournal(
                            TextUtilities.cleanField(
                                    resCitation.getJournal(),
                                    true));
                    resCitation.postProcessPages();

                    // editors (they are human persons in theory)
                    resCitation.setOriginalEditors(resCitation.getEditors());
                    try {
                        resCitation
                                .setFullEditors(
                                        parsers.getAuthorParser().processingCitation(resCitation.getEditors(), config));
                    } catch (Exception e) {
                        LOGGER.error("An exception occurred when processing editor names of a citation.", e);
                    }
                }

                resCitation = consolidateCitation(resCitation, LayoutTokensUtil.toText(tokens), consolidate);
                results.add(resCitation);
            }
        }

        return results;
    }

    public List<BibDataSet> processingReferenceSection(
            String referenceTextBlock,
            ReferenceSegmenter referenceSegmenter) {
        List<LabeledReferenceResult> segm = referenceSegmenter.extract(referenceTextBlock);

        List<BibDataSet> results = new ArrayList<>();
        List<List<LayoutToken>> allRefBlocks = new ArrayList<>();
        if (segm == null || segm.size() == 0)
            return results;
        for (LabeledReferenceResult ref : segm) {
            if (ref.getTokens() == null || ref.getTokens().size() == 0)
                continue;
            List<LayoutToken> localTokens = ref.getTokens();
            localTokens = TextUtilities.removeLeadingAndTrailingCharsLayoutTokens(localTokens, "[({.,])}: \n", " \n");
            allRefBlocks.add(localTokens);
        }

        List<BiblioItem> bibList = processingLayoutTokenMultiple(allRefBlocks, 0);
        if (bibList == null)
            return results;
        int i = 0;
        for (LabeledReferenceResult ref : segm) {
            if (ref.getTokens() == null || ref.getTokens().size() == 0)
                continue;
            BiblioItem bib = bibList.get(i);
            i++;
            if ((bib != null) && !bib.rejectAsReference()) {
                BibDataSet bds = new BibDataSet();
                String localLabel = ref.getLabel();
                if (localLabel != null && localLabel.length() > 0) {
                    // cleaning the label for matching
                    localLabel = TextUtilities.removeLeadingAndTrailingChars(localLabel, "([{<,. \n", ")}]>,.: \n");
                }

                String localRef = ref.getReferenceText();
                localRef = TextUtilities.removeLeadingAndTrailingChars(localRef, "[({.,])}: \n", " \n");
                bds.setRefSymbol(localLabel);
                bib.setReference(localRef);
                bds.setResBib(bib);
                bds.setRawBib(localRef);
                bds.getResBib().setCoordinates(ref.getCoordinates());
                results.add(bds);
            }
        }
        return results;
    }

    public List<BibDataSet> processingReferenceSection(
            Document doc,
            ReferenceSegmenter referenceSegmenter,
            int consolidate) {
        return processingReferenceSection(doc, referenceSegmenter, consolidate, null);
    }

    public List<BibDataSet> processingReferenceSection(
            Document doc,
            ReferenceSegmenter referenceSegmenter,
            int consolidate,
            GrobidAnalysisConfig config) {
        List<BibDataSet> results = new ArrayList<>();

        String referencesStr = doc.getDocumentPartText(SegmentationLabels.REFERENCES);

        if (StringUtils.isEmpty(referencesStr)) {
            cntManager.i(CitationParserCounters.EMPTY_REFERENCES_BLOCKS);
            return results;
        }

        cntManager.i(CitationParserCounters.NOT_EMPTY_REFERENCES_BLOCKS);

        List<LabeledReferenceResult> references = (referenceSegmenter instanceof ReferenceSegmenterParser)
                ? ((ReferenceSegmenterParser) referenceSegmenter).extract(doc, config)
                : referenceSegmenter.extract(doc);

        if (references == null) {
            cntManager.i(CitationParserCounters.NULL_SEGMENTED_REFERENCES_LIST);
            return results;
        } else {
            cntManager.i(CitationParserCounters.SEGMENTED_REFERENCES, references.size());
        }

        // consolidation: if selected, it is NOT done individually for each citation but
        // in a second stage for all citations
        if (references != null) {
            /*List<String> refTexts = new ArrayList<>();
            for (LabeledReferenceResult ref : references) {
                // paranoiac check
                if (ref == null)
                    continue;

                String localRef = ref.getReferenceText();
                localRef = TextUtilities.removeLeadingAndTrailingChars(localRef, "[({.,])}: \n"," \n");
                refTexts.add(localRef);
            }
            List<BiblioItem> bibList = processingStringMultiple(refTexts, 0);*/

            List<List<LayoutToken>> allRefBlocks = new ArrayList<>();
            for (LabeledReferenceResult ref : references) {
                // paranoiac check
                if (ref == null)
                    continue;

                List<LayoutToken> localTokens = ref.getTokens();
                localTokens = TextUtilities
                        .removeLeadingAndTrailingCharsLayoutTokens(localTokens, "[({.,])}: \n", " \n");
                allRefBlocks.add(localTokens);
            }
            List<BiblioItem> bibList = processingLayoutTokenMultiple(allRefBlocks, 0, config);

            if (bibList != null && bibList.size() > 0) {
                int i = 0;
                for (LabeledReferenceResult ref : references) {
                    // paranoiac check
                    if (ref == null)
                        continue;

                    //BiblioItem bib = processingString(ref.getReferenceText(), 0);
                    BiblioItem bib = bibList.get(i);
                    i++;
                    if (bib == null)
                        continue;

                    // check if we have an interesting url annotation over this bib. ref.
                    List<LayoutToken> refTokens = ref.getTokens();
                    if ((refTokens != null) && (refTokens.size() > 0)) {
                        List<Integer> localPages = new ArrayList<Integer>();
                        for (LayoutToken token : refTokens) {
                            if (!localPages.contains(token.getPage())) {
                                localPages.add(token.getPage());
                            }
                        }
                        for (PDFAnnotation annotation : doc.getPDFAnnotations()) {
                            if (annotation.getType() != Type.URI)
                                continue;
                            if (!localPages.contains(annotation.getPageNumber()))
                                continue;
                            for (LayoutToken token : refTokens) {
                                if (annotation.cover(token)) {
                                    // annotation covers tokens, let's look at the href
                                    String uri = annotation.getDestination();
                                    // is it a DOI?
                                    Matcher doiMatcher = TextUtilities.DOIPattern.matcher(uri);
                                    if (doiMatcher.find()) {
                                        // the BiblioItem setter will take care of the prefix and doi cleaninng
                                        bib.setDOI(uri);
                                    }
                                    // TBD: is it something else?
                                }
                            }
                        }
                    }

                    if (!bib.rejectAsReference()) {
                        BibDataSet bds = new BibDataSet();
                        String localLabel = ref.getLabel();
                        if (localLabel != null && localLabel.length() > 0) {
                            // cleaning the label for matching
                            localLabel = TextUtilities
                                    .removeLeadingAndTrailingChars(localLabel, "([{<,. \n", ")}]>,.: \n");
                        }

                        String localRef = ref.getReferenceText();
                        localRef = TextUtilities.removeLeadingAndTrailingChars(localRef, "[({.,])}: \n", " \n");

                        bds.setRefSymbol(localLabel);
                        bds.setResBib(bib);
                        bib.setReference(localRef);
                        bds.setRawBib(localRef);
                        bds.getResBib().setCoordinates(ref.getCoordinates());
                        results.add(bds);
                    }
                }
            }
        }

        // consolidate the set
        if (consolidate != 0) {
            Consolidation consolidator = Consolidation.getInstance();
            if (consolidator.getCntManager() == null)
                consolidator.setCntManager(cntManager);
            Map<Integer, BiblioItem> resConsolidation = null;
            try {
                resConsolidation = consolidator.consolidate(results);
            } catch (Exception e) {
                throw new GrobidException(
                        "An exception occurred while running consolidation on bibliographical references.", e);
            }
            if (resConsolidation != null) {
                for (int i = 0; i < results.size(); i++) {
                    BiblioItem resCitation = results.get(i).getResBib();
                    BiblioItem bibo = resConsolidation.get(Integer.valueOf(i));
                    if (bibo != null) {
                        if (consolidate == 1)
                            BiblioItem.correct(resCitation, bibo);
                        else if (consolidate == 2)
                            BiblioItem.injectIdentifiers(resCitation, bibo);
                    }
                }
            }
        }

        doc.setBibDataSets(results);

        return results;
    }

    public List<BibDataSet> processingReferenceSection(
            File input,
            ReferenceSegmenter referenceSegmenter,
            int consolidate) {
        DocumentSource documentSource = DocumentSource.fromPdf(input);
        return processingReferenceSection(documentSource, referenceSegmenter, consolidate);
    }

    public List<BibDataSet> processingReferenceSection(
            File input,
            String md5Str,
            ReferenceSegmenter referenceSegmenter,
            int consolidate) {
        return processingReferenceSection(input, md5Str, referenceSegmenter, consolidate, null);
    }

    public List<BibDataSet> processingReferenceSection(
            File input,
            String md5Str,
            ReferenceSegmenter referenceSegmenter,
            int consolidate,
            GrobidAnalysisConfig config) {
        DocumentSource documentSource = DocumentSource.fromPdf(input);
        documentSource.setMD5(md5Str);
        return processingReferenceSection(documentSource, referenceSegmenter, consolidate, config);
    }

    public List<BibDataSet> processingReferenceSection(
            DocumentSource documentSource,
            ReferenceSegmenter referenceSegmenter,
            int consolidate) {
        return processingReferenceSection(documentSource, referenceSegmenter, consolidate, null);
    }

    public List<BibDataSet> processingReferenceSection(
            DocumentSource documentSource,
            ReferenceSegmenter referenceSegmenter,
            int consolidate,
            GrobidAnalysisConfig debugConfig) {
        List<BibDataSet> results;
        try {
            // Build a segmentation config that carries any debug collector forward so the
            // segmentation model's labelling is captured even on the references-only endpoint.
            GrobidAnalysisConfig.GrobidAnalysisConfigBuilder builder = GrobidAnalysisConfig.builder()
                    .consolidateCitations(consolidate);
            if (debugConfig != null) {
                builder.debugLabelingCollector(debugConfig.getDebugLabelingCollector());
            }
            Document doc = parsers.getSegmentationParser().processing(documentSource, builder.build());
            results = processingReferenceSection(doc, referenceSegmenter, consolidate, debugConfig);
        } catch (GrobidException e) {
            LOGGER.error("An exception occurred while running Grobid.", e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("An exception occurred while running Grobid.", e);
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }

        return results;
    }

    /**
     * Extract results from a labeled sequence.
     *
     * @param result            labeled sequence
     * @param volumePostProcess whether post process volume
     * @param tokenizations     list of tokens
     * @return biblio item
     */
    public BiblioItem resultExtractionLayoutTokens(
            String result,
            boolean volumePostProcess,
            List<LayoutToken> tokenizations) {
        BiblioItem biblio = new BiblioItem();

        TaggingLabel lastClusterLabel = null;
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.CITATION, result, tokenizations);
        biblio.generalResultMappingReference(result, tokenizations);

        String tokenLabel = null;
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            //String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
            //String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens());
            String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            //String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
            if (clusterLabel.equals(TaggingLabels.CITATION_TITLE)) {
                if (biblio.getTitle() == null)
                    biblio.setTitle(clusterContent);
                else if (biblio.getTitle().length() >= clusterContent.length())
                    biblio.setNoteOrConcatenateIfNotEmpty(clusterContent);
                else {
                    biblio.setNoteOrConcatenateIfNotEmpty(biblio.getTitle());
                    biblio.setTitle(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_AUTHOR)) {
                if (biblio.getAuthors() == null)
                    biblio.setAuthors(clusterContent);
                else
                    biblio.setAuthors(biblio.getAuthors() + " ; " + clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_TECH)) {
                biblio.setBookType(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_LOCATION)) {
                if (biblio.getLocation() != null)
                    biblio.setLocation(biblio.getLocation() + "; " + clusterContent);
                else
                    biblio.setLocation(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_DATE)) {
                if (biblio.getPublicationDate() != null)
                    biblio.setPublicationDate(biblio.getPublicationDate() + ". " + clusterContent);
                else
                    biblio.setPublicationDate(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_BOOKTITLE)) {
                if (biblio.getBookTitle() == null)
                    biblio.setBookTitle(clusterContent);
                else if (biblio.getBookTitle().length() >= clusterContent.length())
                    biblio.setNoteOrConcatenateIfNotEmpty(clusterContent);
                else {
                    biblio.setNoteOrConcatenateIfNotEmpty(biblio.getBookTitle());
                    biblio.setBookTitle(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_SERIES)) {
                if (biblio.getSerieTitle() == null)
                    biblio.setSerieTitle(clusterContent);
                else if (biblio.getSerieTitle().length() >= clusterContent.length()) {
                    biblio.setNoteOrConcatenateIfNotEmpty(clusterContent);
                } else {
                    biblio.setNoteOrConcatenateIfNotEmpty(biblio.getSerieTitle());
                    biblio.setSerieTitle(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_PAGES)) {
                String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
                biblio.setPageRange(clusterNonDehypenizedContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_PUBLISHER)) {
                biblio.setPublisher(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_COLLABORATION)) {
                if (biblio.getCollaboration() != null)
                    biblio.setCollaboration(biblio.getCollaboration() + " ; " + clusterContent);
                else
                    biblio.setCollaboration(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_JOURNAL)) {
                if (biblio.getJournal() == null) {
                    biblio.setJournal(clusterContent);
                } else if (biblio.getJournal().length() >= clusterContent.length()) {
                    biblio.setNoteOrConcatenateIfNotEmpty(clusterContent);
                } else {
                    biblio.setNoteOrConcatenateIfNotEmpty(biblio.getJournal());
                    biblio.setJournal(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_VOLUME)) {
                if (biblio.getVolumeBlock() == null) {
                    biblio.setVolumeBlock(clusterContent, volumePostProcess);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_ISSUE)) {
                if (biblio.getIssue() == null) {
                    biblio.setIssue(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_EDITOR)) {
                biblio.setEditors(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_INSTITUTION)) {
                if (biblio.getInstitution() != null) {
                    biblio.setInstitution(biblio.getInstitution() + " ; " + clusterContent);
                } else {
                    biblio.setInstitution(clusterContent);
                }
            } else if (clusterLabel.equals(TaggingLabels.CITATION_NOTE)) {
                biblio.setNoteOrConcatenateIfNotEmpty(clusterContent);
            } else if (clusterLabel.equals(TaggingLabels.CITATION_PUBNUM)) {
                String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
                biblio.setPubnum(clusterNonDehypenizedContent);
                biblio.checkIdentifier();
            } else if (clusterLabel.equals(TaggingLabels.CITATION_WEB)) {
                String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());
                biblio.setWeb(clusterNonDehypenizedContent);
            }
        }

        return biblio;
    }

    /**
     * Consolidate an existing list of recognized citations based on access to
     * external internet bibliographic databases.
     *
     * @param resCitation citation
     * @return consolidated biblio item
     */
    public BiblioItem consolidateCitation(BiblioItem resCitation, String rawCitation, int consolidate) {
        if (consolidate == 0) {
            // no consolidation
            return resCitation;
        }
        Consolidation consolidator = null;
        try {
            consolidator = Consolidation.getInstance();
            if (consolidator.getCntManager() == null)
                consolidator.setCntManager(cntManager);
            List<BibDataSet> biblios = new ArrayList<BibDataSet>();
            BibDataSet theBib = new BibDataSet();
            theBib.setResBib(resCitation);
            biblios.add(theBib);
            Map<Integer, BiblioItem> bibis = consolidator.consolidate(biblios);

            //BiblioItem bibo = consolidator.consolidate(resCitation, rawCitation);
            BiblioItem bibo = bibis.get(0);
            if (bibo != null) {
                if (consolidate == 1)
                    BiblioItem.correct(resCitation, bibo);
                else if (consolidate == 2)
                    BiblioItem.injectIdentifiers(resCitation, bibo);
            }
        } catch (Exception e) {
            LOGGER.error("An exception occurred while running bibliographical data consolidation.", e);
            throw new GrobidException(
                    "An exception occurred while running bibliographical data consolidation.", e);
        }
        return resCitation;
    }

    /**
     * Extract results from a list of citation strings in the training format
     * without any string modification.
     *
     * @param inputs list of input data
     * @return result
     */
    public StringBuilder trainingExtraction(List<String> inputs) {
        StringBuilder buffer = new StringBuilder();
        try {
            if (inputs == null)
                return null;

            if (inputs.size() == 0)
                return null;

            List<OffsetPosition> journalsPositions = null;
            List<OffsetPosition> abbrevJournalsPositions = null;
            List<OffsetPosition> conferencesPositions = null;
            List<OffsetPosition> publishersPositions = null;
            List<OffsetPosition> locationsPositions = null;
            List<OffsetPosition> collaborationsPositions = null;
            List<OffsetPosition> identifiersPositions = null;
            List<OffsetPosition> urlPositions = null;
            for (String input : inputs) {
                if (input == null)
                    continue;

                List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);
                tokenizations = analyzer.retokenizeSubdigitsFromLayoutToken(tokenizations);

                if (tokenizations.size() == 0)
                    return null;

                journalsPositions = lexicon.tokenPositionsJournalNames(tokenizations);
                abbrevJournalsPositions = lexicon.tokenPositionsAbbrevJournalNames(tokenizations);
                conferencesPositions = lexicon.tokenPositionsConferenceNames(tokenizations);
                publishersPositions = lexicon.tokenPositionsPublisherNames(tokenizations);
                locationsPositions = lexicon.tokenPositionsLocationNames(tokenizations);
                collaborationsPositions = lexicon.tokenPositionsCollaborationNames(tokenizations);
                identifiersPositions = lexicon.tokenPositionsIdentifierPattern(tokenizations);
                urlPositions = lexicon.tokenPositionsUrlPattern(tokenizations);

                String ress = FeaturesVectorCitation.addFeaturesCitation(
                        tokenizations,
                        null,
                        journalsPositions,
                        abbrevJournalsPositions,
                        conferencesPositions,
                        publishersPositions,
                        locationsPositions,
                        collaborationsPositions,
                        identifiersPositions,
                        urlPositions);
                String res = label(ress);

                String lastTag = null;
                String lastTag0;
                String currentTag0 = null;
                boolean start = true;
                String s1 = null;
                String s2 = null;
                int p = 0;

                // extract results from the processed file
                StringTokenizer st = new StringTokenizer(res, "\n");
                while (st.hasMoreTokens()) {
                    boolean addSpace = false;
                    String tok = st.nextToken().trim();

                    if (tok.length() == 0) {
                        // new citation
                        //buffer.append("/t<bibl>\n");
                        start = true;
                        continue;
                    }
                    StringTokenizer stt = new StringTokenizer(tok, "\t");
                    int i = 0;

                    boolean newLine = false;
                    int ll = stt.countTokens();
                    while (stt.hasMoreTokens()) {
                        String s = stt.nextToken().trim();
                        if (i == 0) {
                            s2 = TextUtilities.HTMLEncode(s);
                            //s2 = s;

                            boolean strop = false;
                            while ((!strop) && (p < tokenizations.size())) {
                                String tokOriginal = tokenizations.get(p).t();
                                if (tokOriginal.equals(" ")
                                        || tokOriginal.equals("\u00A0")) {
                                    addSpace = true;
                                } else if (tokOriginal.equals(s)) {
                                    strop = true;
                                }
                                p++;
                            }
                        } else if (i == ll - 1) {
                            s1 = s;
                        }
                        i++;
                    }

                    if (start && (s1 != null)) {
                        buffer.append("\t<bibl>");
                        start = false;
                    }

                    lastTag0 = null;
                    if (lastTag != null) {
                        if (lastTag.startsWith("I-")) {
                            lastTag0 = lastTag.substring(2, lastTag.length());
                        } else {
                            lastTag0 = lastTag;
                        }
                    }
                    if (s1 != null) {
                        if (s1.startsWith("I-")) {
                            currentTag0 = s1.substring(2, s1.length());
                        } else {
                            currentTag0 = s1;
                        }
                    }

                    //tagClosed = lastTag0 != null &&
                    if ((lastTag0 != null) && (currentTag0 != null))
                        testClosingTag(buffer, currentTag0, lastTag0);

                    String output = writeField(s1, lastTag0, s2, "<title>", "<title level=\"a\">", addSpace, 0);
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<other>", "", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<author>", "<author>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<journal>", "<title level=\"j\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<series>", "<title level=\"s\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<booktitle>", "<title level=\"m\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<date>", "<date>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<volume>", "<biblScope unit=\"volume\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<publisher>", "<publisher>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<location>", "<pubPlace>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<editor>", "<editor>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<pages>", "<biblScope unit=\"page\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<tech>", "<note type=\"report\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<issue>", "<biblScope unit=\"issue\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<web>", "<ptr type=\"web\">", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<note>", "<note>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(s1, lastTag0, s2, "<institution>", "<orgName>", addSpace, 0);
                    }
                    if (output == null) {
                        output = writeField(
                                s1,
                                lastTag0,
                                s2,
                                "<collaboration>",
                                "<orgName type=\"collaboration\">",
                                addSpace,
                                0);
                    }
                    if (output == null) {
                        String localTag = null;
                        String cleanS2 = StringUtils.normalizeSpace(s2);
                        cleanS2 = cleanS2.replace(" ", "");

                        Matcher doiMatcher = TextUtilities.DOIPattern.matcher(cleanS2);
                        if (doiMatcher.find())
                            localTag = "<idno type=\"DOI\">";

                        if (localTag == null) {
                            Matcher arxivMatcher = TextUtilities.arXivPattern.matcher(cleanS2);
                            if (arxivMatcher.find())
                                localTag = "<idno type=\"arXiv\">";
                        }

                        if (localTag == null) {
                            Matcher pmidMatcher = TextUtilities.pmidPattern.matcher(cleanS2);
                            if (pmidMatcher.find())
                                localTag = "<idno type=\"PMID\">";
                        }

                        if (localTag == null) {
                            Matcher pmcidMatcher = TextUtilities.pmcidPattern.matcher(cleanS2);
                            if (pmcidMatcher.find())
                                localTag = "<idno type=\"PMC\">";
                        }

                        if (localTag == null) {
                            if (cleanS2.toLowerCase().indexOf("issn") != -1) {
                                localTag = "<idno type=\"ISSN\">";
                            }
                        }

                        if (localTag == null) {
                            if (cleanS2.toLowerCase().indexOf("isbn") != -1) {
                                localTag = "<idno type=\"ISBN\">";
                            }
                        }

                        // TODO: PII

                        if (localTag == null)
                            localTag = "<idno>";

                        output = writeField(s1, lastTag0, s2, "<pubnum>", localTag, addSpace, 0);
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
                    testClosingTag(buffer, currentTag0, lastTag0);
                    buffer.append("</bibl>\n");
                }
            }

        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return buffer;
    }

    private String writeField(
            String s1,
            String lastTag0,
            String s2,
            String field,
            String outField,
            boolean addSpace,
            int nbIndent) {
        String result = null;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                /*for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
                }*/
                if (addSpace) {
                    result += " " + outField + s2;
                } else {
                    result += outField + s2;
                }
            }
        }
        return result;
    }

    private boolean writeField2(
            StringBuilder buffer,
            String s1,
            String lastTag0,
            String s2,
            String field,
            String outField,
            boolean addSpace) {
        boolean result = false;
        if ((s1.equals(field)) || (s1.equals("I-" + field))) {
            result = true;
            if (s1.equals(lastTag0) || (s1).equals("I-" + lastTag0)) {
                if (addSpace)
                    buffer.append(" ").append(s2);
                else
                    buffer.append(s2);
            } else {
                if (addSpace)
                    buffer.append(" ").append(outField).append(s2);
                else
                    buffer.append(outField).append(s2);
            }
        }
        return result;
    }

    private boolean testClosingTag(
            StringBuilder buffer,
            String currentTag0,
            String lastTag0) {
        boolean res = false;
        if (!currentTag0.equals(lastTag0)) {
            res = true;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("");
            } else if (lastTag0.equals("<title>")) {
                buffer.append("</title>");
            } else if (lastTag0.equals("<series>")) {
                buffer.append("</title>");
            } else if (lastTag0.equals("<author>")) {
                buffer.append("</author>");
            } else if (lastTag0.equals("<tech>")) {
                buffer.append("</note>");
            } else if (lastTag0.equals("<location>")) {
                buffer.append("</pubPlace>");
            } else if (lastTag0.equals("<date>")) {
                buffer.append("</date>");
            } else if (lastTag0.equals("<booktitle>")) {
                buffer.append("</title>");
            } else if (lastTag0.equals("<pages>")) {
                buffer.append("</biblScope>");
            } else if (lastTag0.equals("<publisher>")) {
                buffer.append("</publisher>");
            } else if (lastTag0.equals("<journal>")) {
                buffer.append("</title>");
            } else if (lastTag0.equals("<volume>")) {
                buffer.append("</biblScope>");
            } else if (lastTag0.equals("<issue>")) {
                buffer.append("</biblScope>");
            } else if (lastTag0.equals("<editor>")) {
                buffer.append("</editor>");
            } else if (lastTag0.equals("<pubnum>")) {
                buffer.append("</idno>");
            } else if (lastTag0.equals("<web>")) {
                buffer.append("</ptr>");
            } else if (lastTag0.equals("<note>")) {
                buffer.append("</note>");
            } else if (lastTag0.equals("<institution>")) {
                buffer.append("</orgName>");
            } else if (lastTag0.equals("<collaboration>")) {
                buffer.append("</orgName>");
            } else {
                res = false;
            }

        }
        return res;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
