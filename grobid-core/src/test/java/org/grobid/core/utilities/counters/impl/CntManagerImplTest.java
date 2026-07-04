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
package org.grobid.core.utilities.counters.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.grobid.core.engines.counters.CitationParserCounters;
import org.grobid.core.engines.counters.Countable;
import org.grobid.core.engines.counters.FigureCounters;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.utilities.GrobidProperties;

public class CntManagerImplTest {

    CntManagerImpl target;

    @Before
    public void setUp() throws Exception {
        GrobidProperties.getInstance();
        target = new CntManagerImpl();
    }

    @Test
    public void testCountSingleGroup() throws Exception {
        target.i(TaggingLabels.EQUATION);

        assertThat(target.getCounter(TaggingLabels.EQUATION).cnt(), is(1l));
    }

    @Test
    public void testGetAllCounters() throws Exception {
        target.i(TaggingLabels.ITEM);
        target.i(TaggingLabels.OTHER);

        final Map<String, Map<String, Long>> allCounters = target.getAllCounters();
        assertThat(allCounters.size(), is(1));
        final Map<String, Long> stringLongMap = allCounters.get("org.grobid.core.engines.label.TaggingLabelImpl");
        assertThat(stringLongMap.size(), is(2));

        assertThat(stringLongMap.get(TaggingLabels.OTHER.getName()), is(1l));
        assertThat(stringLongMap.get(TaggingLabels.ITEM.getName()), is(1l));
    }

    @Test
    public void testGetAllCounters2() throws Exception {
        target.i(TaggingLabels.ITEM);
        target.i(FigureCounters.SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS);

        final Map<String, Map<String, Long>> allCounters = target.getAllCounters();
        assertThat(allCounters.size(), is(2));

        final Map<String, Long> taggingLabelMap = allCounters.get("org.grobid.core.engines.label.TaggingLabelImpl");
        assertThat(taggingLabelMap.size(), is(1));

        final Map<String, Long> countersMap = allCounters.get("org.grobid.core.engines.counters.FigureCounters");
        assertThat(countersMap.size(), is(1));
    }

    @Test
    public void testGetAllCounters_checkGroupName() throws Exception {
        target.i(FigureCounters.TOO_MANY_FIGURES_PER_PAGE);
        target.i(FigureCounters.SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS);
        target.i(CitationParserCounters.EMPTY_REFERENCES_BLOCKS);
        target.i(CitationParserCounters.SEGMENTED_REFERENCES);
        target.i(CitationParserCounters.SEGMENTED_REFERENCES);
        target.i(CitationParserCounters.SEGMENTED_REFERENCES);
        target.i(CitationParserCounters.SEGMENTED_REFERENCES);

        final Map<String, Map<String, Long>> allCounters = target.getAllCounters();
        assertThat(allCounters.size(), is(2));

        final Map<String, Long> taggingLabelMap = allCounters.get("org.grobid.core.engines.label.TaggingLabelImpl");
        assertNull(taggingLabelMap);

        final Map<String, Long> counterFigures = allCounters.get("org.grobid.core.engines.counters.FigureCounters");
        assertThat(counterFigures.size(), is(2));

        final Map<String, Long> counterCitations = allCounters
                .get("org.grobid.core.engines.counters.CitationParserCounters");
        assertThat(counterCitations.size(), is(2));
        assertThat(counterCitations.get("SEGMENTED_REFERENCES"), is(4l));
        assertThat(counterCitations.get("EMPTY_REFERENCES_BLOCKS"), is(1l));
    }

    @Test
    public void testCnt_withClass() throws Exception {
        assertThat(target.cnt(TaggingLabels.ITEM), is(0l));
        target.i(TaggingLabels.ITEM);
        assertThat(target.cnt(TaggingLabels.ITEM), is(1l));

        assertThat(
                target.cnt(FigureCounters.SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS),
                is(0l));
        target.i(FigureCounters.SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS);
        assertThat(
                target.cnt(FigureCounters.SKIPPED_DUE_TO_MISMATCH_OF_CAPTIONS_AND_VECTOR_AND_BITMAP_GRAPHICS),
                is(1l));
    }

    @Test
    public void testCnt_withExplicitValues() throws Exception {
        assertThat(target.cnt("figures", "element"), is(0l));
        target.i("figures", "element");
        assertThat(target.cnt("figures", "element"), is(1l));

        assertThat(target.cnt("figures", "item"), is(0l));
        target.i("figures", "item");
        assertThat(target.cnt("figures", "item"), is(1l));

        assertThat(target.cnt("tables", "item"), is(0l));
        target.i("tables", "item");
        assertThat(target.cnt("tables", "item"), is(1l));

        target.i("figures", "item");
        assertThat(target.cnt("figures", "item"), is(2l));
    }

    @Test
    public void getCounter_shouldWork() throws Exception {
        target.i("figures", "element", 2);
        assertThat(target.getCounter("figures", "element").cnt(), is(2l));

        target.i(TaggingLabels.CITATION_MARKER, 20);
        assertThat(target.getCounter(TaggingLabels.CITATION_MARKER).cnt(), is(20l));
    }

    @Test
    public void getCounters_shouldWork() throws Exception {
        target.i("figures", "element", 2);
        target.i("table", "john", 4);
        target.i("table", "miao", 2);
        assertThat(target.getCounters("figures").size(), is(1));
        assertThat(target.getCounters("table").size(), is(2));
        assertThat(target.getCounter("table", "john").cnt(), is(4l));

        target.i(TaggingLabels.CITATION_MARKER, 20);
        assertThat(target.getCounter(TaggingLabels.CITATION_MARKER).cnt(), is(20l));
        final Class<? extends Countable> countableClass = (Class<? extends Countable>) Class
                .forName(TaggingLabels.CITATION_MARKER.getClass().getName());
        assertThat(target.getCounters(countableClass).size(), is(1));

        final String[] tables = target.getCounters("table").keySet().toArray(new String[0]);
        Arrays.sort(tables);
        assertThat(tables, is(new String[]{"john", "miao"}));
    }

    @Test
    public void getCounterEnclosingClass_NoEnclosingClass_shouldWork() throws Exception {
        assertThat(
                target.getCounterEnclosingName(TaggingLabels.CITATION_MARKER),
                is("org.grobid.core.engines.label.TaggingLabelImpl"));
    }

    @Test
    public void getCounterEnclosingClass_EnclosingClass_sholdWork() throws Exception {
        assertThat(
                target.getCounterEnclosingName(FigureCounters.TOO_MANY_FIGURES_PER_PAGE),
                is("org.grobid.core.engines.counters.FigureCounters"));
    }

}
