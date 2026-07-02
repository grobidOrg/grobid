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
package org.grobid.core.tokenization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import org.grobid.core.GrobidModel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.Triple;

/**
 * Synchronize tagging result and layout tokens
 */
public class TaggingTokenSynchronizer implements Iterator<LabeledTokensContainer>, Iterable<LabeledTokensContainer> {
    private final GrobidModel grobidModel;
    private final Iterator<Triple<String, String, String>> tokensAndLabelsIt;
    private final PeekingIterator<LayoutToken> tokenizationsIt;
    private int tokensAndLabelsPtr;
    private int tokenizationsPtr;
    private List<Triple<String, String, String>> tokensAndLabels;
    private List<LayoutToken> tokenizations;

    public TaggingTokenSynchronizer(GrobidModel grobidModel, String result, List<LayoutToken> tokenizations) {
        this(grobidModel, result, tokenizations, false);
    }

    public TaggingTokenSynchronizer(GrobidModel grobidModel, String result, List<LayoutToken> tokenizations,
            boolean addFeatureStrings) {
        this(grobidModel, GenericTaggerUtils.getTokensWithLabelsAndFeatures(result, addFeatureStrings), tokenizations);
    }

    public TaggingTokenSynchronizer(GrobidModel grobidModel, List<Triple<String, String, String>> tokensAndLabels,
            List<LayoutToken> tokenizations) {
        this.grobidModel = grobidModel;
        this.tokensAndLabels = tokensAndLabels;
        tokensAndLabelsIt = this.tokensAndLabels.iterator();
        this.tokenizations = tokenizations;
        tokenizationsIt = Iterators.peekingIterator(this.tokenizations.iterator());
    }

    @Override
    public boolean hasNext() {
        return tokensAndLabelsIt.hasNext();
    }

    @Override
    //null value indicates an empty line in a tagging result
    public LabeledTokensContainer next() {
        Triple<String, String, String> p = tokensAndLabelsIt.next();

        if (p == null) {
            return null;
        }

        String resultToken = p.getA();
        String label = p.getB();
        String featureString = p.getC();

        List<LayoutToken> layoutTokenBuffer = new ArrayList<>();
        boolean stop = false;
        boolean addSpace = false;
        boolean newLine = false;
        int preTokenizationPtr = tokenizationsPtr;

        while ((!stop) && (tokenizationsIt.hasNext())) {
            LayoutToken layoutToken = tokenizationsIt.next();

            layoutToken.addLabel(TaggingLabels.labelFor(grobidModel, label));

            layoutTokenBuffer.add(layoutToken);
            String tokOriginal = layoutToken.t();

            if (LayoutTokensUtil.newLineToken(tokOriginal)) {
                newLine = true;
            } else if (LayoutTokensUtil.spaceyToken(tokOriginal)) {
                addSpace = true;
            } else if (tokOriginal.replaceAll("[ \n]", "").equals(resultToken)) {
                stop = true;
            } else if (tokOriginal.isEmpty()) {
                // no op
            } else {
                throw new IllegalStateException(prepareErrorMessage(preTokenizationPtr));
            }
            tokenizationsPtr++;
        }

        //filling spaces to the end, instead of appending spaces to the next container
        while (tokenizationsIt.hasNext()) {
            LayoutToken nextToken = tokenizationsIt.peek();
            if (LayoutTokensUtil.spaceyToken(nextToken.t()) || LayoutTokensUtil.newLineToken(nextToken.t())) {
                LayoutToken layoutToken = tokenizationsIt.next();
                layoutTokenBuffer.add(layoutToken);
                tokenizationsPtr++;
                if (LayoutTokensUtil.newLineToken(layoutToken.t())) {
                    newLine = true;
                } else if (LayoutTokensUtil.spaceyToken(layoutToken.t())) {
                    addSpace = true;
                }
            } else {
                break;
            }
        }

        //resultToken = LayoutTokensUtil.removeSpecialVariables(resultToken);

        tokensAndLabelsPtr++;
        LabeledTokensContainer labeledTokensContainer = new LabeledTokensContainer(layoutTokenBuffer, resultToken,
                TaggingLabels.labelFor(grobidModel, label),
                GenericTaggerUtils.isBeginningOfEntity(label));

        labeledTokensContainer.setFeatureString(featureString);
        labeledTokensContainer.setTrailingSpace(addSpace);
        labeledTokensContainer.setTrailingNewLine(newLine);

        return labeledTokensContainer;
    }

    private String prepareErrorMessage(int preTokenizationPtr) {
        int limit = 5;
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, tokensAndLabelsPtr - limit); i < Math
                .min(tokensAndLabelsPtr + limit, tokensAndLabels.size()); i++) {
            Triple<String, String, String> s = tokensAndLabels.get(i);
            if (s != null) {
                String str = i == tokensAndLabelsPtr ? "-->\t'" + s.getA() + "'" : "\t'" + s.getA() + "'";
                sb.append(str).append("\n");
            }
        }

        StringBuilder sb2 = new StringBuilder();
        for (int i = Math.max(0, preTokenizationPtr - limit * 2); i < Math
                .min(preTokenizationPtr + limit * 2, tokenizations.size()); i++) {
            LayoutToken s = tokenizations.get(i);
            String str = i == preTokenizationPtr ? "-->\t'" + s.t() + "'" : "\t'" + s.t() + "'";
            sb2.append(str).append("\n");
        }

        return "IMPLEMENTATION ERROR: "
                +
                "tokens (at pos: "
                + tokensAndLabelsPtr
                + ") got dissynchronized with tokenizations (at pos: "
                + tokenizationsPtr
                + " )\n"
                +
                "labelsAndTokens +-: \n"
                + sb.toString()
                +
                "\n"
                + "tokenizations +-: "
                + sb2;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<LabeledTokensContainer> iterator() {
        return this;
    }
}
