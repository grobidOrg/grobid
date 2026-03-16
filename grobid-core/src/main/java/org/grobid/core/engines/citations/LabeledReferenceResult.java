package org.grobid.core.engines.citations;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;

import java.util.List;

public class LabeledReferenceResult {
    private String label = null;
    private final String referenceText;
	private String features; // optionally the vector of features corresponding to the token referenceText
    private List<BoundingBox> coordinates = null;
    private List<LayoutToken> tokens = null;  

    public LabeledReferenceResult(String referenceText) {
        this.referenceText = referenceText;
    }

    public LabeledReferenceResult(String label, String referenceText, 
            List<LayoutToken> referenceTokens, String features, 
            List<BoundingBox> coordinates) {
        this.label = label;
        this.referenceText = referenceText;
		this.tokens = referenceTokens;
        this.features = features;
        this.coordinates = coordinates;
    }

    public String getLabel() {
        return label;
    }

    public String getReferenceText() {
        return referenceText;
    }
	
    public String getFeatures() {
        return features;
    }

    public List<BoundingBox> getCoordinates() {
        return coordinates;
    }

    public List<LayoutToken> getTokens() {
        return this.tokens;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("label", label)
                .append("referenceText", referenceText)
                .toString();
    }
}
