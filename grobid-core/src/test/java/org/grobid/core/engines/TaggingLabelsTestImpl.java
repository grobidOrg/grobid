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
package org.grobid.core.engines;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.GrobidModel;
import org.grobid.core.GrobidModels;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.utilities.GrobidProperties;

public class TaggingLabelsTestImpl {

    @BeforeClass
    public static void init() {
        GrobidProperties.getInstance();
    }

    @Test
    public void testTaggingLabel_StandardLabelSection() throws Exception {
        TaggingLabel label = TaggingLabels.SECTION;

        assertNotNull(label);

        assertThat(label.getLabel(), is("<section>"));
        assertThat(label.getGrobidModel(), is((GrobidModel) GrobidModels.FULLTEXT));
        assertThat(label.getName(), is("FULLTEXT_SECTION"));
    }

    @Test
    public void testModelFor_StandardLabelStartingSection() throws Exception {

        TaggingLabel label = TaggingLabels.labelFor(GrobidModels.FULLTEXT, "I-<section>");

        assertNotNull(label);

        assertThat(label.getLabel(), is("<section>"));
        assertThat(label.getGrobidModel(), is((GrobidModel) GrobidModels.FULLTEXT));
        assertThat(label.getName(), is("FULLTEXT_SECTION"));
    }

    @Test
    public void testModelFor_LabelNoPresentInCache_shouldRemovePrefix() throws Exception {

        TaggingLabel label = TaggingLabels.labelFor(GrobidModels.FULLTEXT, "I-<sectionsLabel>");

        assertNotNull(label);

        assertThat(label.getLabel(), is("<sectionsLabel>"));
        assertThat(label.getGrobidModel(), is((GrobidModel) GrobidModels.FULLTEXT));
        assertThat(label.getName(), is("FULLTEXT_SECTIONSLABEL"));
    }

    @Test
    public void testModelFor_StandardLabelMiddleSection() throws Exception {

        TaggingLabel label = TaggingLabels.labelFor(GrobidModels.FULLTEXT, "<section>");

        assertNotNull(label);

        assertThat(label.getLabel(), is("<section>"));
        assertThat(label.getGrobidModel(), is((GrobidModel) GrobidModels.FULLTEXT));
        assertThat(label.getName(), is("FULLTEXT_SECTION"));
    }

    @Test
    public void testTaggingLabel_CustomLabel() throws Exception {

        TaggingLabel label = TaggingLabels.labelFor(GrobidModels.DICTIONARIES_LEXICAL_ENTRIES, "<lemma>");

        assertNotNull(label);

        assertThat(label.getLabel(), is("<lemma>"));
        assertThat(label.getGrobidModel(), is((GrobidModel) GrobidModels.DICTIONARIES_LEXICAL_ENTRIES));
        assertThat(label.getName(), is("dictionaries-lexical-entries_LEMMA".toUpperCase()));

    }
}
