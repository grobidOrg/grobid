package org.grobid.core.engines.label;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.grobid.core.GrobidModel;

/**
 * Representing label that can be tagged
 */
public class TaggingLabelImpl implements TaggingLabel {

    public static final long serialVersionUID = 1L;

    private final GrobidModel grobidModel;
    private final String label;

    TaggingLabelImpl(GrobidModel grobidModel, String label) {
        this.grobidModel = grobidModel;
        this.label = label;
    }

    public GrobidModel getGrobidModel() {
        return grobidModel;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof TaggingLabelImpl))
            return false;

        TaggingLabelImpl that = (TaggingLabelImpl) o;

        return new EqualsBuilder().append(getGrobidModel(), that.getGrobidModel())
                .append(getLabel(), that.getLabel())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getGrobidModel()).append(getLabel()).toHashCode();
    }

    @Override
    public String getName() {
        final String tmp = getLabel().replaceAll("[<>]", "");
        return StringUtils.upperCase(
                getGrobidModel().getModelName()
                        + "_"
                        + tmp.replace(TaggingLabels.GROBID_START_ENTITY_LABEL_PREFIX, ""));
    }
}
