package org.grobid.core.tokenization;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.grobid.core.GrobidModel;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
