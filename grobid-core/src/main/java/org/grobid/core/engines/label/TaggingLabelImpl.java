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

        return new EqualsBuilder()
                .append(getGrobidModel(), that.getGrobidModel())
                .append(getLabel(), that.getLabel())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getGrobidModel())
                .append(getLabel())
                .toHashCode();
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
