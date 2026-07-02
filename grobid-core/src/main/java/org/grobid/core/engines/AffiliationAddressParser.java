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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.data.Affiliation;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.features.FeaturesVectorAffiliationAddress;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.lexicon.Lexicon;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.OffsetPosition;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.utilities.UnicodeUtil;

public class AffiliationAddressParser extends AbstractParser {
    public Lexicon lexicon = Lexicon.getInstance();

    protected AffiliationAddressParser(GrobidModel model) {
        super(model);
    }

    public AffiliationAddressParser() {
        this(GrobidModels.AFFILIATION_ADDRESS);
    }

    public List<Affiliation> processing(String input) {
        warnIfDebugUncaptured("AffiliationAddressParser.processing(String)");
        return processing(input, null);
    }

    public List<Affiliation> processing(String input, GrobidAnalysisConfig config) {
        List<Affiliation> results = null;
        try {
            if ((input == null) || (input.length() == 0)) {
                return null;
            }

            input = UnicodeUtil.normaliseText(input);
            input = input.trim();

            input = TextUtilities.dehyphenize(input);

            // TBD: pass the language object to the tokenizer
            List<LayoutToken> tokenizations = analyzer.tokenizeWithLayoutToken(input);

            List<String> affiliationBlocks = getAffiliationBlocks(tokenizations);
            List<List<OffsetPosition>> placesPositions = new ArrayList<List<OffsetPosition>>();
            List<List<OffsetPosition>> countriesPositions = new ArrayList<List<OffsetPosition>>();
            placesPositions.add(lexicon.tokenPositionsLocationNames(tokenizations));
            countriesPositions.add(lexicon.tokenPositionsCountryNames(tokenizations));
            List<List<LayoutToken>> allTokens = new ArrayList<List<LayoutToken>>();
            allTokens.add(tokenizations);
            String affiliationSequenceWithFeatures = FeaturesVectorAffiliationAddress
                    .addFeaturesAffiliationAddress(affiliationBlocks, allTokens, placesPositions, countriesPositions);

            String res = labelAndCapture(affiliationSequenceWithFeatures, config);

            results = resultExtractionLayoutTokens(res, tokenizations);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return results;
    }

    protected static List<String> getAffiliationBlocks(List<LayoutToken> tokenizations) {
        ArrayList<String> affiliationBlocks = new ArrayList<String>();
        for (LayoutToken tok : tokenizations) {
            if (tok.getText().length() == 0)
                continue;

            if (!tok.getText().equals(" ")) {
                if (tok.getText().equals("\n")) {
                    affiliationBlocks.add("@newline");
                } else
                    affiliationBlocks.add(tok + " <affiliation>");
            }
        }
        return affiliationBlocks;
    }

    /**
     * Separate affiliation blocks, when they appears to be in separate set of offsets.
     */
    protected static List<String> getAffiliationBlocksFromSegments(List<List<LayoutToken>> tokenizations) {
        ArrayList<String> affiliationBlocks = new ArrayList<>();
        int end = 0;
        for (List<LayoutToken> tokenizationSegment : tokenizations) {
            if (CollectionUtils.isEmpty(tokenizationSegment))
                continue;

            // if we have an offset shit, we introduce a segmentation of the affiliation block
            LayoutToken startToken = tokenizationSegment.get(0);
            int start = startToken.getOffset();
            if (start - end > 2 && end > 0)
                affiliationBlocks.add("\n");

            for (LayoutToken tok : tokenizationSegment) {
                if (StringUtils.isEmpty(tok.getText())) {
                    continue;
                }

                if (!tok.getText().equals(" ")) {
                    if (tok.getText().equals("\n")) {
                        affiliationBlocks.add("@newline");
                    } else
                        affiliationBlocks.add(tok + " <affiliation>");
                }
                end = tok.getOffset();
            }
        }
        return affiliationBlocks;
    }

    public List<Affiliation> processingLayoutTokens(List<List<LayoutToken>> tokenizations) {
        warnIfDebugUncaptured("AffiliationAddressParser.processingLayoutTokens(List)");
        return processingLayoutTokens(tokenizations, null);
    }

    public List<Affiliation> processingLayoutTokens(
            List<List<LayoutToken>> tokenizations,
            GrobidAnalysisConfig config) {
        List<Affiliation> results = null;
        try {
            if ((tokenizations == null) || (tokenizations.size() == 0)) {
                return null;
            }

            List<LayoutToken> tokenizationsAffiliation = new ArrayList<>();
            for (List<LayoutToken> tokenization : tokenizations) {
                tokenizationsAffiliation.addAll(tokenization);
            }

            List<String> affiliationBlocks = getAffiliationBlocksFromSegments(tokenizations);

            //System.out.println(affiliationBlocks.toString());

            List<List<OffsetPosition>> placesPositions = new ArrayList<>();
            List<List<OffsetPosition>> countriesPositions = new ArrayList<>();
            placesPositions.add(lexicon.tokenPositionsLocationNames(tokenizationsAffiliation));
            countriesPositions.add(lexicon.tokenPositionsCountryNames(tokenizationsAffiliation));
            List<List<LayoutToken>> allTokens = new ArrayList<>();
            allTokens.add(tokenizationsAffiliation);
            String affiliationSequenceWithFeatures = FeaturesVectorAffiliationAddress
                    .addFeaturesAffiliationAddress(affiliationBlocks, allTokens, placesPositions, countriesPositions);

            String res = labelAndCapture(affiliationSequenceWithFeatures, config);
            results = resultExtractionLayoutTokens(res, tokenizationsAffiliation);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return results;
    }

    /**
     * Extract results from a labeled sequence.
     *
     * @param result            labeled sequence
     * @param tokenizations     list of tokens
     * @return lis of Affiliation objects
     */
    protected List<Affiliation> resultExtractionLayoutTokens(String result, List<LayoutToken> tokenizations) {
        List<Affiliation> affiliations = new ArrayList<>();
        if (result == null)
            return affiliations;

        Affiliation affiliation = new Affiliation();

        //System.out.println(result);

        TaggingLabel lastClusterLabel = null;
        TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.AFFILIATION_ADDRESS, result,
                tokenizations);

        String tokenLabel = null;
        boolean newline = true;
        List<TaggingTokenCluster> clusters = clusteror.cluster();
        for (TaggingTokenCluster cluster : clusters) {
            if (cluster == null) {
                continue;
            }

            TaggingLabel clusterLabel = cluster.getTaggingLabel();
            Engine.getCntManager().i(clusterLabel);

            String clusterContent = LayoutTokensUtil.normalizeText(LayoutTokensUtil.toText(cluster.concatTokens()));
            //String clusterContent = LayoutTokensUtil.toText(cluster.concatTokens());
            //String clusterContent = LayoutTokensUtil.normalizeDehyphenizeText(cluster.concatTokens());
            //String clusterNonDehypenizedContent = LayoutTokensUtil.toText(cluster.concatTokens());

            List<LayoutToken> tokens = cluster.concatTokens();

            if (clusterLabel.equals(TaggingLabels.AFFILIATION_MARKER)) {
                // if an affiliation has already a merker, or if a marker start a line,
                // we introduce a new affiliation
                if (affiliation.getMarker() != null || newline) {
                    if (affiliation.isNotNull()) {
                        affiliations.add(affiliation);
                    }
                    affiliation = new Affiliation();
                }

                affiliation.setMarker(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_MARKER, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_INSTITUTION)) {
                if (affiliation.getInstitutions() != null && affiliation.getInstitutions().size() > 0) {
                    if (affiliation.hasAddress()) {
                        // new affiliation
                        if (affiliation.isNotNull()) {
                            affiliations.add(affiliation);
                        }
                        affiliation = new Affiliation();
                    }
                }
                affiliation.addInstitution(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_INSTITUTION, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_DEPARTMENT)) {
                if (affiliation.getDepartments() != null && affiliation.getDepartments().size() > 0) {
                    if (affiliation.hasAddress()) {
                        // new affiliation
                        if (affiliation.isNotNull()) {
                            affiliations.add(affiliation);
                        }
                        affiliation = new Affiliation();
                    }
                }
                affiliation.addDepartment(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_DEPARTMENT, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_LABORATORY)) {
                if (affiliation.getLaboratories() != null && affiliation.getLaboratories().size() > 0) {
                    if (affiliation.hasAddress()) {
                        // new affiliation
                        if (affiliation.isNotNull()) {
                            affiliations.add(affiliation);
                        }
                        affiliation = new Affiliation();
                    }
                }
                affiliation.addLaboratory(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_LABORATORY, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_COUNTRY)) {
                if (affiliation.getCountry() != null) {
                    if (!affiliation.getCountry().equals(clusterContent))
                        affiliation.setCountry(affiliation.getCountry() + " " + clusterContent);
                } else
                    affiliation.setCountry(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_COUNTRY, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_POSTCODE)) {
                if (affiliation.getPostCode() != null)
                    affiliation.setPostCode(affiliation.getPostCode() + " " + clusterContent);
                else
                    affiliation.setPostCode(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_POSTCODE, tokens);
            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_POSTBOX)) {
                if (affiliation.getPostBox() != null)
                    affiliation.setPostBox(affiliation.getPostBox() + " " + clusterContent);
                else
                    affiliation.setPostBox(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_POSTBOX, tokens);

            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_REGION)) {
                if (affiliation.getRegion() != null)
                    affiliation.setRegion(affiliation.getRegion() + " " + clusterContent);
                else
                    affiliation.setRegion(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_REGION, tokens);

            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_SETTLEMENT)) {
                if (affiliation.getSettlement() != null)
                    affiliation.setSettlement(affiliation.getSettlement() + " " + clusterContent);
                else
                    affiliation.setSettlement(clusterContent);
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_SETTLEMENT, tokens);

            } else if (clusterLabel.equals(TaggingLabels.AFFILIATION_ADDRESSLINE)) {
                if (affiliation.getAddrLine() != null) {
                    affiliation.setAddrLine(affiliation.getAddrLine() + " " + clusterContent);
                } else {
                    affiliation.setAddrLine(clusterContent);
                }
                affiliation.addLabeledResult(TaggingLabels.AFFILIATION_ADDRESSLINE, tokens);
            }

            if (!clusterLabel.equals(TaggingLabels.OTHER) && affiliation.isNotNull()) {
                affiliation.appendLayoutTokens(tokens);
            }

            if (!clusterLabel.equals(TaggingLabels.AFFILIATION_MARKER)) {
                if (affiliation.getRawAffiliationString() == null) {
                    affiliation.setRawAffiliationString(clusterContent);
                } else {
                    affiliation.setRawAffiliationString(affiliation.getRawAffiliationString() + " " + clusterContent);
                }
            }

            newline = false;
            if (tokens.size() > 0) {
                LayoutToken lastToken = tokens.get(tokens.size() - 1);
                if (lastToken.getText() != null && lastToken.getText().equals("\n"))
                    newline = true;
            }
        }

        // last affiliation
        if (affiliation.isNotNull()) {
            affiliations.add(affiliation);
        }

        return affiliations;
    }

    /**
     * DEPRECATED
     **/
    @Deprecated
    protected ArrayList<Affiliation> resultBuilder(
            String result,
            List<LayoutToken> tokenizations,
            boolean usePreLabel) {
        ArrayList<Affiliation> fullAffiliations = null;

        if (result == null) {
            return fullAffiliations;
        }
        result = result.replace("\n\n", "\n \n"); // force empty line between affiliation blocks
        try {
            //System.out.println(tokenizations.toString());
            // extract results from the processed file
            if ((result == null) || (result.length() == 0)) {
                return null;
            }

            StringTokenizer st2 = new StringTokenizer(result, "\n");
            String lastTag = null;
            org.grobid.core.data.Affiliation aff = new Affiliation();
            int lineCount = 0;
            boolean hasInstitution;
            boolean hasDepartment = false;
            boolean hasAddress = false;
            boolean hasLaboratory;
            boolean newMarker = false;
            boolean useMarker = false;
            String currentMarker = null;

            int p = 0;

            while (st2.hasMoreTokens()) {
                boolean addSpace = false;
                String line = st2.nextToken();
                Integer lineCountInt = lineCount;
                if (line.trim().length() == 0) {
                    if (aff.isNotNull()) {
                        if (fullAffiliations == null) {
                            fullAffiliations = new ArrayList<Affiliation>();
                        }
                        fullAffiliations.add(aff);
                        aff = new Affiliation();
                        currentMarker = null;
                    }
                    hasInstitution = false;
                    hasDepartment = false;
                    hasLaboratory = false;
                    hasAddress = false;
                    continue;
                }
                String delimiter = "\t";
                if (line.indexOf(delimiter) == -1)
                    delimiter = " ";
                StringTokenizer st3 = new StringTokenizer(line, delimiter);
                int ll = st3.countTokens();
                int i = 0;
                String s1 = null; // predicted label
                String s2 = null; // lexical token
                String s3 = null; // pre-label
                ArrayList<String> localFeatures = new ArrayList<String>();
                while (st3.hasMoreTokens()) {
                    String s = st3.nextToken().trim();
                    if (i == 0) {
                        s2 = s; // lexical token

                        boolean strop = false;
                        while ((!strop) && (p < tokenizations.size())) {
                            String tokOriginal = tokenizations.get(p).getText();
                            if (tokOriginal.equals(" ")) {
                                addSpace = true;
                            } else if (tokOriginal.equals(s)) {
                                strop = true;
                            }
                            p++;
                        }
                    } else if (i == ll - 2) {
                        s3 = s; // pre-label
                    } else if (i == ll - 1) {
                        s1 = s; // label
                    } else {
                        localFeatures.add(s);
                    }
                    i++;
                }

                if (s1.equals("<marker>")) {
                    if (currentMarker == null)
                        currentMarker = s2;
                    else {
                        if (addSpace) {
                            currentMarker += " " + s2;
                        } else
                            currentMarker += s2;
                    }
                    aff.setMarker(currentMarker);
                    newMarker = false;
                    useMarker = true;
                } else if (s1.equals("I-<marker>")) {
                    currentMarker = s2;
                    newMarker = true;
                    useMarker = true;
                }

                if (newMarker) {
                    if (aff.isNotNull()) {
                        if (fullAffiliations == null)
                            fullAffiliations = new ArrayList<Affiliation>();
                        fullAffiliations.add(aff);
                    }

                    aff = new Affiliation();
                    hasInstitution = false;
                    hasLaboratory = false;
                    hasDepartment = false;
                    hasAddress = false;

                    if (currentMarker != null) {
                        aff.setMarker(currentMarker);
                    }
                    newMarker = false;
                } else if (s1.equals("<institution>") || s1.equals("I-<institution>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        hasInstitution = true;
                        if (aff.getInstitutions() != null) {
                            if (s1.equals("I-<institution>") &&
                                    (localFeatures.contains("LINESTART"))) {
                                // new affiliation
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addInstitution(s2);
                                if (currentMarker != null)
                                    aff.setMarker(currentMarker.trim());
                            } else if (s1.equals("I-<institution>") && hasInstitution && hasAddress &&
                                    (!lastTag.equals("<institution>"))) {
                                // new affiliation
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null) {
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    }
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = true;
                                hasDepartment = false;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addInstitution(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (s1.equals("I-<institution>")) {
                                // we have multiple institutions for this affiliation
                                //aff.addInstitution(aff.institution);
                                aff.addInstitution(s2);
                            } else if (addSpace) {
                                aff.extendLastInstitution(" " + s2);
                            } else {
                                aff.extendLastInstitution(s2);
                            }
                        } else {
                            aff.addInstitution(s2);
                        }
                    } else if ((usePreLabel) && (s3.equals("<address>") || s3.equals("I-<address>"))) {
                        // that's a piece of the address badly labelled according to the model
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + s2);
                            } else {
                                aff.setAddressString(aff.getAddressString() + s2);
                            }
                        } else {
                            aff.setAddressString(s2);
                        }
                    }
                } else if (s1.equals("<addrLine>") || s1.equals("I-<addrLine>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getAddrLine() != null) {
                            if (s1.equals(lastTag) || lastTag.equals("I-<addrLine>")) {
                                if (s1.equals("I-<addrLine>")) {
                                    aff.setAddrLine(aff.getAddrLine() + " ; " + s2);
                                } else if (addSpace) {
                                    aff.setAddrLine(aff.getAddrLine() + " " + s2);
                                } else {
                                    aff.setAddrLine(aff.getAddrLine() + s2);
                                }
                            } else {
                                aff.setAddrLine(aff.getAddrLine() + ", " + s2);
                            }
                        } else {
                            aff.setAddrLine(s2);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (s1.equals(lastTag)) {
                                if (addSpace) {
                                    aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                                } else {
                                    aff.setAffiliationString(aff.getAffiliationString() + s2);
                                }
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + " ; " + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                } else if (s1.equals("<department>") || s1.equals("I-<department>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getDepartments() != null) {
                            /*if (localFeatures.contains("LINESTART"))
                                       aff.department += " " + s2;*/

                            if ((s1.equals("I-<department>")) &&
                                    (localFeatures.contains("LINESTART"))) {
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addDepartment(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if ((s1.equals("I-<department>")) && hasDepartment && hasAddress &&
                                    !lastTag.equals("<department>")) {
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null) {
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    }
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasAddress = false;
                                hasLaboratory = false;
                                aff = new Affiliation();
                                aff.addDepartment(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (s1.equals("I-<department>")) {
                                // we have multiple departments for this affiliation
                                aff.addDepartment(s2);
                                //aff.department = s2;
                            } else if (addSpace) {
                                //aff.extendFirstDepartment(" " + s2);
                                aff.extendLastDepartment(" " + s2);
                            } else {
                                //aff.extendFirstDepartment(s2);
                                aff.extendLastDepartment(s2);
                            }
                        } else if (aff.getInstitutions() != null) {
                            /*if (localFeatures.contains("LINESTART"))
                                       aff.department += " " + s2;*/

                            if ((s1.equals("I-<department>")) && hasAddress &&
                                    (localFeatures.contains("LINESTART"))) {
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasDepartment = true;
                                hasLaboratory = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addDepartment(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else {
                                aff.addDepartment(s2);
                            }
                        } else {
                            aff.addDepartment(s2);
                        }
                    } else if ((usePreLabel) && (s3.equals("<address>") || s3.equals("I-<address>"))) {
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + s2);
                            } else {
                                aff.setAddressString(aff.getAddressString() + s2);
                            }
                        } else {
                            aff.setAddressString(s2);
                        }
                    }
                } else if (s1.equals("<laboratory>") || s1.equals("I-<laboratory>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && (s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        hasLaboratory = true;
                        if (aff.getLaboratories() != null) {
                            if (s1.equals("I-<laboratory>") &&
                                    (localFeatures.contains("LINESTART"))) {
                                // new affiliation
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addLaboratory(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (s1.equals("I-<laboratory>")
                                    && hasLaboratory
                                    && hasAddress
                                    && (!lastTag.equals("<laboratory>"))) {
                                // new affiliation
                                if (aff.isNotNull()) {
                                    if (fullAffiliations == null)
                                        fullAffiliations = new ArrayList<Affiliation>();
                                    fullAffiliations.add(aff);
                                }
                                hasInstitution = false;
                                hasLaboratory = true;
                                hasDepartment = false;
                                hasAddress = false;
                                aff = new Affiliation();
                                aff.addLaboratory(s2);
                                if (currentMarker != null) {
                                    aff.setMarker(currentMarker.trim());
                                }
                            } else if (s1.equals("I-<laboratory>")) {
                                // we have multiple laboratories for this affiliation
                                aff.addLaboratory(s2);
                            } else if (addSpace) {
                                aff.extendLastLaboratory(" " + s2);
                            } else {
                                aff.extendLastLaboratory(s2);
                            }
                        } else {
                            aff.addLaboratory(s2);
                        }
                    } else if ((usePreLabel) && (s3.equals("<address>") || s3.equals("I-<address>"))) {
                        // that's a piece of the address badly labelled
                        if (aff.getAddressString() != null) {
                            if (addSpace) {
                                aff.setAddressString(aff.getAddressString() + " " + s2);
                            } else {
                                aff.setAddressString(aff.getAddressString() + s2);
                            }
                        } else {
                            aff.setAddressString(s2);
                        }
                    }
                } else if (s1.equals("<country>") || s1.equals("I-<country>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getCountry() != null) {
                            if (s1.equals("I-<country>")) {
                                aff.setCountry(aff.getCountry() + ", " + s2);
                            } else if (addSpace) {
                                aff.setCountry(aff.getCountry() + " " + s2);
                            } else {
                                aff.setCountry(aff.getCountry() + s2);
                            }
                        } else {
                            aff.setCountry(s2);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                } else if (s1.equals("<postCode>") || s1.equals("I-<postCode>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getPostCode() != null) {
                            if (s1.equals("I-<postCode>")) {
                                aff.setPostCode(aff.getPostCode() + ", " + s2);
                            } else if (addSpace) {
                                aff.setPostCode(aff.getPostCode() + " " + s2);
                            } else {
                                aff.setPostCode(aff.getPostCode() + s2);
                            }
                        } else {
                            aff.setPostCode(s2);
                        }
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                } else if (s1.equals("<postBox>") || s1.equals("I-<postBox>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getPostBox() != null) {
                            if (s1.equals("I-<postBox>")) {
                                aff.setPostBox(aff.getPostBox() + ", " + s2);
                            } else if (addSpace) {
                                aff.setPostBox(aff.getPostBox() + " " + s2);
                            } else {
                                aff.setPostBox(aff.getPostBox() + s2);
                            }
                        } else {
                            aff.setPostBox(s2);
                        }
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                } else if (s1.equals("<region>") || s1.equals("I-<region>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getRegion() != null) {
                            if (s1.equals("I-<region>")) {
                                aff.setRegion(aff.getRegion() + ", " + s2);
                            } else if (addSpace) {
                                aff.setRegion(aff.getRegion() + " " + s2);
                            } else {
                                aff.setRegion(aff.getRegion() + s2);
                            }
                        } else {
                            aff.setRegion(s2);
                        }
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                } else if (s1.equals("<settlement>") || s1.equals("I-<settlement>")) {
                    if ((!usePreLabel) ||
                            ((usePreLabel) && ((s3.equals("<address>") || s3.equals("I-<address>"))))) {
                        if (aff.getSettlement() != null) {
                            if (s1.equals("I-<settlement>")) {
                                aff.setSettlement(aff.getSettlement() + ", " + s2);
                            } else if (addSpace) {
                                aff.setSettlement(aff.getSettlement() + " " + s2);
                            } else {
                                aff.setSettlement(aff.getSettlement() + s2);
                            }
                        } else {
                            aff.setSettlement(s2);
                        }
                        hasAddress = true;
                    } else if ((usePreLabel) && ((s3.equals("<affiliation>") || s3.equals("I-<affiliation>")))) {
                        if (aff.getAffiliationString() != null) {
                            if (addSpace) {
                                aff.setAffiliationString(aff.getAffiliationString() + " " + s2);
                            } else {
                                aff.setAffiliationString(aff.getAffiliationString() + s2);
                            }
                        } else {
                            aff.setAffiliationString(s2);
                        }
                    }
                }

                if (!s1.endsWith("<marker>")) {
                    if (aff.getRawAffiliationString() == null) {
                        aff.setRawAffiliationString(s2);
                    } else if (addSpace) {
                        aff.setRawAffiliationString(aff.getRawAffiliationString() + " " + s2);
                    } else {
                        aff.setRawAffiliationString(aff.getRawAffiliationString() + s2);
                    }
                }

                lastTag = s1;
                lineCount++;
                newMarker = false;
            }
            if (aff.isNotNull()) {
                if (fullAffiliations == null)
                    fullAffiliations = new ArrayList<Affiliation>();

                fullAffiliations.add(aff);
                hasInstitution = false;
                hasDepartment = false;
                hasAddress = false;
            }

            // we clean a little bit
            if (fullAffiliations != null) {
                for (Affiliation affi : fullAffiliations) {
                    affi.clean();
                }
            }
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }
        return fullAffiliations;
    }

    /**
     * Extract results from a labelled header in the training format without any string modification.
     */
    public StringBuilder trainingExtraction(List<LayoutToken> tokenizationsAffiliation) {
        /*if ((result == null) || (result.length() == 0)) {
            return null;
        }*/

        if (tokenizationsAffiliation == null || tokenizationsAffiliation.size() == 0)
            return null;

        List<String> affiliationBlocks = getAffiliationBlocks(tokenizationsAffiliation);
        List<List<OffsetPosition>> placesPositions = new ArrayList<List<OffsetPosition>>();
        List<List<OffsetPosition>> countriesPositions = new ArrayList<List<OffsetPosition>>();
        placesPositions.add(lexicon.tokenPositionsLocationNames(tokenizationsAffiliation));
        countriesPositions.add(lexicon.tokenPositionsCountryNames(tokenizationsAffiliation));
        List<List<LayoutToken>> allTokens = new ArrayList<List<LayoutToken>>();
        allTokens.add(tokenizationsAffiliation);

        String affiliationSequenceWithFeatures = null;
        try {
            affiliationSequenceWithFeatures = FeaturesVectorAffiliationAddress
                    .addFeaturesAffiliationAddress(affiliationBlocks, allTokens, placesPositions, countriesPositions);
        } catch (Exception e) {
            throw new GrobidException("An exception occurred while running Grobid.", e);
        }

        if (affiliationSequenceWithFeatures == null) {
            return null;
        }

        String resultAffiliation = label(affiliationSequenceWithFeatures);
        StringBuilder bufferAffiliation = new StringBuilder();
        if (resultAffiliation == null) {
            return bufferAffiliation;
        }

        StringTokenizer st = new StringTokenizer(resultAffiliation, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;

        int p = 0;

        String currentTag0 = null;
        String lastTag0 = null;
        boolean hasAddressTag = false;
        boolean hasAffiliationTag = false;
        boolean hasAddress = false;
        boolean hasAffiliation = false;
        boolean start = true;
        boolean tagClosed = false;
        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();

            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, "\t");
            ArrayList<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
                    s2 = TextUtilities.HTMLEncode(s);

                    boolean strop = false;
                    while ((!strop) && (p < tokenizationsAffiliation.size())) {
                        String tokOriginal = tokenizationsAffiliation.get(p).getText();
                        if (tokOriginal.equals(" ")) {
                            addSpace = true;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
                } else if (i == ll - 1) {
                    s1 = s;
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

            if (lastTag != null) {
                tagClosed = testClosingTag(bufferAffiliation, currentTag0, lastTag0);
            } else
                tagClosed = false;

            if (newLine) {
                if (tagClosed) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<lb/>\n");
                } else {
                    bufferAffiliation.append("<lb/>");
                }

            }

            String output = writeField(s1, lastTag0, s2, "<marker>", "<marker>", addSpace, 7);
            if (output != null) {
                // output != null implies writeField matched on s1, so s1 is non-null here; the
                // explicit guard keeps that obvious to static analysis (CodeQL) and at runtime.
                if (s1 != null && s1.startsWith("I-")) {
                    // start of a new marker: it begins a new affiliation block, so close the
                    // previous affiliation/address before opening a fresh <affiliation><marker>.
                    if (hasAddressTag) {
                        bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                        hasAddressTag = false;
                    }
                    if (hasAffiliationTag) {
                        bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                        hasAffiliationTag = false;
                    }
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n" + output);
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                } else {
                    // continuation of the current marker: only append its text, so that a
                    // multi-token marker stays inside the same <marker> element instead of
                    // re-opening the affiliation and crossing the (still open) </marker>.
                    bufferAffiliation.append(output);
                }
                lastTag = s1;
                continue;
            } else {
                output = writeField(s1, lastTag0, s2, "<institution>", "<orgName type=\"institution\">", addSpace, 7);
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<department>", "<orgName type=\"department\">", addSpace, 7);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }

                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                }
                bufferAffiliation.append(output);
                hasAffiliation = true;
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<laboratory>", "<orgName type=\"laboratory\">", addSpace, 7);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }

                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                hasAffiliation = true;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<addrLine>", "<addrLine>", addSpace, 8);
            } else {
                if (hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
                    hasAddressTag = false;
                }
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }

                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                }
                bufferAffiliation.append(output);
                hasAffiliation = true;
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<postCode>", "<postCode>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<postBox>", "<postBox>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<region>", "<region>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<settlement>", "<settlement>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<country>", "<country>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }
                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output == null) {
                output = writeField(s1, lastTag0, s2, "<other>", "<other>", addSpace, 8);
            } else {
                if (hasAddress && hasAffiliation) {
                    bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
                    hasAffiliationTag = false;
                    hasAddress = false;
                    hasAffiliation = false;
                    hasAddressTag = false;
                }
                if (!hasAffiliationTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t<affiliation>\n");
                    hasAffiliationTag = true;
                    hasAddressTag = false;
                }

                if (!hasAddressTag) {
                    bufferAffiliation.append("\t\t\t\t\t\t\t<address>\n");
                    hasAddressTag = true;
                }

                bufferAffiliation.append(output);
                lastTag = s1;
                continue;
            }
            if (output != null) {
                if (bufferAffiliation.length() > 0) {
                    if (bufferAffiliation.charAt(bufferAffiliation.length() - 1) == '\n') {
                        bufferAffiliation.deleteCharAt(bufferAffiliation.length() - 1);
                    }
                }
                bufferAffiliation.append(output);
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
            testClosingTag(bufferAffiliation, currentTag0, lastTag0);
            if (hasAddressTag) {
                bufferAffiliation.append("\t\t\t\t\t\t\t</address>\n");
            }
            bufferAffiliation.append("\t\t\t\t\t\t</affiliation>\n");
        }

        return bufferAffiliation;
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
            if ((s1.equals("<other>") || s1.equals("I-<other>"))) {
                //result = "";
                /*for(int i=0; i<nbIndent; i++) {
                        result += "\t";
                    }*/
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else if (s1.equals(lastTag0) || s1.equals("I-" + lastTag0)) {
                if (addSpace)
                    result = " " + s2;
                else
                    result = s2;
            } else {
                result = "";
                for (int i = 0; i < nbIndent; i++) {
                    result += "\t";
                }
                result += outField + s2;
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
            if (lastTag0.equals("<institution>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<department>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<laboratory>")) {
                buffer.append("</orgName>\n");
            } else if (lastTag0.equals("<addrLine>")) {
                buffer.append("</addrLine>\n");
            } else if (lastTag0.equals("<postCode>")) {
                buffer.append("</postCode>\n");
            } else if (lastTag0.equals("<postBox>")) {
                buffer.append("</postBox>\n");
            } else if (lastTag0.equals("<region>")) {
                buffer.append("</region>\n");
            } else if (lastTag0.equals("<settlement>")) {
                buffer.append("</settlement>\n");
            } else if (lastTag0.equals("<country>")) {
                buffer.append("</country>\n");
            } else if (lastTag0.equals("<marker>")) {
                buffer.append("</marker>\n");
            } else if (lastTag0.equals("<other>")) {
                buffer.append("\n");
            } else {
                res = false;
            }
        }
        return res;
    }
}
