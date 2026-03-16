package org.grobid.core.tokenization;

import com.google.common.base.Function;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.layout.LayoutToken;

import java.util.ArrayList;
import java.util.List;

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
                throw new IllegalStateException("This method must be called when feature string is not empty for " +
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
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("taggingLabel", taggingLabel)
                .append("labeledTokensContainers", labeledTokensContainers)
                .toString();
    }

    public LabeledTokensContainer getLastContainer() {
        if (labeledTokensContainers.isEmpty()) {
            return null;
        }

        return labeledTokensContainers.get(labeledTokensContainers.size() - 1);
    }

    public List<LayoutToken> concatTokens() {

        Iterable<LayoutToken> it = Iterables.concat(Iterables.transform(labeledTokensContainers, new Function<LabeledTokensContainer, List<LayoutToken>>() {
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
