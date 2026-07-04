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
package org.grobid.core.data;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Element;
import org.apache.commons.lang3.StringUtils;

import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.BoundingBoxCalculator;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;

/**
 * Class for representing an equation.
 *
 */
public class Equation {
    protected StringBuilder content = null;
    protected StringBuilder label = null;
    protected String id = null;
    //protected int start = -1; // start position in the full text tokenization
    //protected int end = -1; // end position in the full text tokenization
    //protected LayoutToken startToken = null; // start layout token
    //protected LayoutToken endToken = null; // end layout token
    private List<BoundingBox> textArea;
    private List<LayoutToken> layoutTokens;

    private List<LayoutToken> contentTokens = new ArrayList<>();
    private List<LayoutToken> labelTokens = new ArrayList<>();

    //private SortedSet<Integer> blockPtrs;

    public Equation() {
        content = new StringBuilder();
        label = new StringBuilder();
    }

    public Element toTEIElement(GrobidAnalysisConfig config) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        Element formulaElement = XmlBuilderUtils.teiElement("formula");
        if (id != null) {
            XmlBuilderUtils.addXmlId(formulaElement, this.getTeiId());
        }

        if ((config.getGenerateTeiCoordinates() != null) && (config.getGenerateTeiCoordinates().contains("formula"))) {
            XmlBuilderUtils.addCoords(formulaElement, LayoutTokensUtil.getCoordsStringForOneBox(getLayoutTokens()));
        }

        formulaElement.appendChild(LayoutTokensUtil.normalizeText(content.toString()).trim());

        if ((label != null) && (label.length() > 0)) {
            Element labelEl = XmlBuilderUtils.teiElement(
                    "label",
                    LayoutTokensUtil.normalizeText(label.toString()));
            formulaElement.appendChild(labelEl);
        }

        return formulaElement;
    }

    public String toTEI(GrobidAnalysisConfig config) {
        Element formulaElement = toTEIElement(config);
        if (formulaElement != null)
            return formulaElement.toXML();
        else
            return null;
    }

    public List<LayoutToken> getContentTokens() {
        return contentTokens;
    }

    public List<LayoutToken> getLabelTokens() {
        return labelTokens;
    }

    public void appendLabel(String lab) {
        label.append(lab);
    }

    public String getLabel() {
        return label.toString();
    }

    public void appendContent(String trash) {
        content.append(trash);
    }

    public String getContent() {
        return content.toString();
    }

    /*public void setStart(int start) {
        this.start = start;
    }*/

    public int getStart() {
        if ((layoutTokens != null) && (layoutTokens.size() > 0))
            return layoutTokens.get(0).getOffset();
        else
            return -1;
    }

    /*public void setEnd(int end) {
        this.end = end;
    }*/

    public int getEnd() {
        if ((layoutTokens != null) && (layoutTokens.size() > 0))
            return layoutTokens.get(layoutTokens.size() - 1).getOffset();
        else
            return -1;
    }

    /*public void setStartToken(LayoutToken start) {
        this.startToken = start;
    }

    public LayoutToken getStartToken() {
        return startToken;
    }

    public void setEndToken(LayoutToken end) {
        this.endToken = end;
    }

    public LayoutToken getEndToken() {
        return endToken;
    }*/

    public void setId() {
        this.id = TextUtilities.cleanField(label.toString(), false);
    }

    public void setId(String theId) {
        this.id = theId;
    }

    public String getId() {
        return this.id;
    }

    public String getTeiId() {
        return "formula_" + this.id;
    }

    /*public void setBlockPtrs(SortedSet<Integer> blockPtrs) {
        this.blockPtrs = blockPtrs;
    }

    public SortedSet<Integer> getBlockPtrs() {
        return blockPtrs;
    }*/

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void addLayoutToken(LayoutToken token) {
        if (token == null)
            return;
        if (layoutTokens == null)
            layoutTokens = new ArrayList<LayoutToken>();
        layoutTokens.add(token);
    }

    public void addLayoutTokens(List<LayoutToken> tokens) {
        if (tokens == null)
            return;
        if (layoutTokens == null)
            layoutTokens = new ArrayList<LayoutToken>();
        for (LayoutToken token : tokens)
            layoutTokens.add(token);
    }

    public List<BoundingBox> getCoordinates() {
        if (layoutTokens == null || layoutTokens.size() == 0)
            return null;
        else {
            BoundingBox oneBox = BoundingBoxCalculator.calculateOneBox(layoutTokens, true);
            List<BoundingBox> result = new ArrayList<BoundingBox>();
            result.add(oneBox);
            return result;
        }
    }
}
