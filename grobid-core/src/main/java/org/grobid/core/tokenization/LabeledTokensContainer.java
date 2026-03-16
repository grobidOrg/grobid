package org.grobid.core.tokenization;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.tagging.GenericTaggerUtils;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

/**
 * Representing labeled tokens and stuff
 */
public class LabeledTokensContainer {

    private List<LayoutToken> layoutTokens;
    private String token;
    private TaggingLabel taggingLabel;
    private boolean beginning;
    private boolean trailingSpace;
    private boolean trailingNewLine;
    private String featureString;

    public LabeledTokensContainer(List<LayoutToken> layoutTokens, String token, TaggingLabel taggingLabel, boolean beginning) {
        this.layoutTokens = layoutTokens;
        this.token = token;
        this.taggingLabel = taggingLabel;
        this.beginning = beginning;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public String getToken() {
        return token;
    }

    public TaggingLabel getTaggingLabel() {
        return taggingLabel;
    }

    public boolean isBeginning() {
        return beginning;
    }

    public String getPlainLabel() {
        return taggingLabel.getLabel();
    }

    public String getFullLabel() {
        return isBeginning() ? TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX + taggingLabel.getLabel()
                : taggingLabel.getLabel();
    }

    public boolean isTrailingSpace() {
        return trailingSpace;
    }

    public boolean isTrailingNewLine() {
        return trailingNewLine;
    }

    public void setTrailingSpace(boolean trailingSpace) {
        this.trailingSpace = trailingSpace;
    }

    public void setTrailingNewLine(boolean trailingNewLine) {
        this.trailingNewLine = trailingNewLine;
    }

    public String getFeatureString() {
        return featureString;
    }

    public void setFeatureString(String featureString) {
        this.featureString = featureString;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("token", token)
                .append("fullLabel", getFullLabel())
                .toString();
    }
}
