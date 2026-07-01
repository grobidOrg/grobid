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

import org.grobid.core.GrobidModels;

public class SegmentationLabels extends TaggingLabels {
    /**
     * cover page <cover>,
     * document header <header>,
     * page footer <footnote>,
     * page header <headnote>,
     * note in margin <marginnote>,
     * document body <body>,
     * bibliographical section <references>,
     * page number <page>,
     * annexes <annex>,
     * acknowledgement <acknowledgement>,
     * availability <availability>,
     * funding <funding>,
     * other <other>,
     * toc <toc> -> not yet used because not yet training data for this
     */

    public final static String COVER_LABEL = "<cover>";
    public final static String HEADER_LABEL = "<header>";
    public final static String FOOTNOTE_LABEL = "<footnote>";
    public final static String HEADNOTE_LABEL = "<headnote>";
    public final static String MARGINNOTE_LABEL = "<marginnote>";
    public final static String BODY_LABEL = "<body>";
    public final static String PAGE_NUMBER_LABEL = "<page>";
    public final static String ANNEX_LABEL = "<annex>";
    public final static String REFERENCES_LABEL = "<references>";
    public final static String ACKNOWLEDGEMENT_LABEL = "<acknowledgement>";
    public final static String TOC_LABEL = "<toc>";

    SegmentationLabels() {
        super();
    }

    public static final TaggingLabel COVER = new TaggingLabelImpl(GrobidModels.SEGMENTATION, COVER_LABEL);
    public static final TaggingLabel HEADER = new TaggingLabelImpl(GrobidModels.SEGMENTATION, HEADER_LABEL);
    public static final TaggingLabel FOOTNOTE = new TaggingLabelImpl(GrobidModels.SEGMENTATION, FOOTNOTE_LABEL);
    public static final TaggingLabel HEADNOTE = new TaggingLabelImpl(GrobidModels.SEGMENTATION, HEADNOTE_LABEL);
    public static final TaggingLabel MARGINNOTE = new TaggingLabelImpl(GrobidModels.SEGMENTATION, MARGINNOTE_LABEL);
    public static final TaggingLabel BODY = new TaggingLabelImpl(GrobidModels.SEGMENTATION, BODY_LABEL);
    public static final TaggingLabel PAGE_NUMBER = new TaggingLabelImpl(GrobidModels.SEGMENTATION, PAGE_NUMBER_LABEL);
    public static final TaggingLabel ANNEX = new TaggingLabelImpl(GrobidModels.SEGMENTATION, ANNEX_LABEL);
    public static final TaggingLabel REFERENCES = new TaggingLabelImpl(GrobidModels.SEGMENTATION, REFERENCES_LABEL);
    public static final TaggingLabel ACKNOWLEDGEMENT = new TaggingLabelImpl(GrobidModels.SEGMENTATION,
            ACKNOWLEDGEMENT_LABEL);

    public static final TaggingLabel AVAILABILITY = new TaggingLabelImpl(GrobidModels.SEGMENTATION, AVAILABILITY_LABEL);
    public static final TaggingLabel CONFLICT_OF_INTEREST = new TaggingLabelImpl(GrobidModels.SEGMENTATION,
            CONFLICT_OF_INTEREST_LABEL);
    public static final TaggingLabel AUTHOR_CONTRIBUTION = new TaggingLabelImpl(GrobidModels.SEGMENTATION,
            AUTHOR_CONTRIBUTION_LABEL);
    public static final TaggingLabel FUNDING = new TaggingLabelImpl(GrobidModels.SEGMENTATION, FUNDING_LABEL);
    public static final TaggingLabel TOC = new TaggingLabelImpl(GrobidModels.SEGMENTATION, TOC_LABEL);

    static {
        register(COVER);
        register(HEADER);
        register(FOOTNOTE);
        register(HEADNOTE);
        register(MARGINNOTE);
        register(BODY);
        register(PAGE_NUMBER);
        register(ANNEX);
        register(REFERENCES);
        register(ACKNOWLEDGEMENT);
        register(AVAILABILITY);
        register(FUNDING);
        register(TOC);
        register(CONFLICT_OF_INTEREST);
        register(AUTHOR_CONTRIBUTION);
    }

}
