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
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import org.grobid.core.GrobidModel;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;

/**
 * Cluster tokens by label
 */
public class TaggingTokenClusteror {
    private final TaggingTokenSynchronizer taggingTokenSynchronizer;

    public static class LabelTypePredicate implements Predicate<TaggingTokenCluster> {
        private TaggingLabel label;

        public LabelTypePredicate(TaggingLabel label) {
            this.label = label;
        }

        @Override
        public boolean apply(TaggingTokenCluster taggingTokenCluster) {
            return taggingTokenCluster.getTaggingLabel() == label;
        }
    }

    public static class LabelTypeExcludePredicate implements Predicate<TaggingTokenCluster> {
        private TaggingLabel[] labels;

        public LabelTypeExcludePredicate(TaggingLabel... labels) {
            this.labels = labels;
        }

        @Override
        public boolean apply(TaggingTokenCluster taggingTokenCluster) {
            for (TaggingLabel label : labels) {
                if (taggingTokenCluster.getTaggingLabel() == label) {
                    return false;
                }
            }
            return true;
        }
    }

    public TaggingTokenClusteror(GrobidModel grobidModel, String result, List<LayoutToken> tokenizations) {
        taggingTokenSynchronizer = new TaggingTokenSynchronizer(grobidModel, result, tokenizations);
    }

    public TaggingTokenClusteror(GrobidModel grobidModel, String result, List<LayoutToken> tokenizations,
            boolean computerFeatureBlock) {
        taggingTokenSynchronizer = new TaggingTokenSynchronizer(grobidModel, result, tokenizations,
                computerFeatureBlock);
    }

    public List<TaggingTokenCluster> cluster() {
        List<TaggingTokenCluster> result = new ArrayList<>();

        PeekingIterator<LabeledTokensContainer> it = Iterators.peekingIterator(taggingTokenSynchronizer);
        if (!it.hasNext() || (it.peek() == null)) {
            return Collections.emptyList();
        }

        // a boolean is introduced to indicate the start of the sequence in the case the label
        // has no beginning indicator (e.g. I-)
        boolean begin = true;
        TaggingTokenCluster curCluster = new TaggingTokenCluster(it.peek().getTaggingLabel());
        while (it.hasNext()) {
            LabeledTokensContainer cont = it.next();
            if (cont == null) {
                // this should not happen, but for the sake of paranoia, we skip
                continue;
            }
            if (begin || cont.isBeginning() || cont.getTaggingLabel() != curCluster.getTaggingLabel()) {
                curCluster = new TaggingTokenCluster(cont.getTaggingLabel());
                result.add(curCluster);
            }
            curCluster.addLabeledTokensContainer(cont);
            if (begin)
                begin = false;
        }

        return result;
    }

}
