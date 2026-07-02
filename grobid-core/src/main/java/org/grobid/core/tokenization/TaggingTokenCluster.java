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
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;

/**
 * Cluster of related tokens
 */
public class TaggingTokenCluster {
    public static final Function<LabeledTokensContainer, String> CONTAINERS_TO_FEATURE_BLOCK = new Function<LabeledTokensContainer, String>() {
        @Override
        public String apply(LabeledTokensContainer labeledTokensContainer) {
            if (labeledTokensContainer == null) {
                return "\n";
            }

            if (labeledTokensContainer.getFeatureString() == null) {
                throw new IllegalStateException("This method must be called when feature string is not empty for "
                        +
                        "LabeledTokenContainers");
            }
            return labeledTokensContainer.getFeatureString();
        }
    };
    private List<LabeledTokensContainer> labeledTokensContainers = new ArrayList<>();
    private TaggingLabel taggingLabel;

    public TaggingTokenCluster(TaggingLabel taggingLabel) {
        this.taggingLabel = taggingLabel;
    }

    public void addLabeledTokensContainer(LabeledTokensContainer cont) {
        labeledTokensContainers.add(cont);
    }

    public List<LabeledTokensContainer> getLabeledTokensContainers() {
        return labeledTokensContainers;
    }

    public TaggingLabel getTaggingLabel() {
        return taggingLabel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (LabeledTokensContainer c : labeledTokensContainers) {
            sb.append(c).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    public LabeledTokensContainer getLastContainer() {
        if (labeledTokensContainers.isEmpty()) {
            return null;
        }

        return labeledTokensContainers.get(labeledTokensContainers.size() - 1);
    }

    public List<LayoutToken> concatTokens() {

        Iterable<LayoutToken> it = Iterables.concat(
                Iterables.transform(labeledTokensContainers, new Function<LabeledTokensContainer, List<LayoutToken>>() {
                    @Override
                    public List<LayoutToken> apply(LabeledTokensContainer labeledTokensContainer) {
                        return labeledTokensContainer.getLayoutTokens();
                    }
                }));
        return Lists.newArrayList(it);
    }

    public String getFeatureBlock() {
        return Joiner.on("\n").join(Iterables.transform(labeledTokensContainers, CONTAINERS_TO_FEATURE_BLOCK));
    }
}
