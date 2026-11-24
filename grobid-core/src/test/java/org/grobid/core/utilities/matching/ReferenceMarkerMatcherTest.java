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
package org.grobid.core.utilities.matching;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.engines.label.TaggingLabel;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.tokenization.LabeledTokensContainer;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.utilities.GrobidProperties;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.counters.impl.CntManagerFactory;

public class ReferenceMarkerMatcherTest {

    @BeforeClass
    public static void setInitialContext() throws Exception {
        GrobidProperties.getInstance();
    }

    private static TaggingTokenCluster cluster(TaggingLabel label, String text) {
        TaggingTokenCluster cluster = new TaggingTokenCluster(label);
        cluster.addLabeledTokensContainer(
                new LabeledTokensContainer(
                        GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(text), text, label, true));
        return cluster;
    }

    private static List<TaggingTokenCluster> merge(String left, String dash, TaggingLabel rightLabel, String right) {
        return ReferenceMarkerMatcher.mergeBracketedRanges(
                Arrays.asList(
                        cluster(TaggingLabels.CITATION_MARKER, left),
                        cluster(TaggingLabels.PARAGRAPH, dash),
                        cluster(rightLabel, right)));
    }

    @Test
    public void testMergeBracketedRanges() {
        List<TaggingTokenCluster> merged = merge("[1]", "– ", TaggingLabels.CITATION_MARKER, "[3]");
        assertThat(merged, hasSize(1));
        assertThat(merged.get(0).getTaggingLabel(), is(TaggingLabels.CITATION_MARKER));
        // the tokenizer normalises the en dash to a hyphen
        assertThat(LayoutTokensUtil.toText(merged.get(0).concatTokens()), is("[1]- [3]"));

        // descending, too long to expand, not a lone dash, not a citation marker: left as they are
        assertThat(merge("[3]", "–", TaggingLabels.CITATION_MARKER, "[1]"), hasSize(3));
        assertThat(merge("[1]", "–", TaggingLabels.CITATION_MARKER, "[30]"), hasSize(3));
        assertThat(merge("[1]", " and ", TaggingLabels.CITATION_MARKER, "[3]"), hasSize(3));
        assertThat(merge("[1]", "– see", TaggingLabels.CITATION_MARKER, "[3]"), hasSize(3));
        assertThat(merge("[1]", "–", TaggingLabels.FIGURE_MARKER, "[3]"), hasSize(3));
    }

    @Test
    public void testMatchBracketedRange() throws Exception {
        List<BibDataSet> bibDataSets = new ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            BiblioItem biblio = new BiblioItem();
            biblio.setOrdinal(i - 1);
            BibDataSet bibDataSet = new BibDataSet();
            bibDataSet.setRefSymbol(String.valueOf(i));
            bibDataSet.setResBib(biblio);
            bibDataSets.add(bibDataSet);
        }
        ReferenceMarkerMatcher matcher = new ReferenceMarkerMatcher(bibDataSets, CntManagerFactory.getCntManager());

        assertThat(ordinals(matcher, "[1]–[3]"), contains(0, 1, 2));
        assertThat(ordinals(matcher, "[1–3]"), contains(0, 1, 2));
        // a range that cannot be expanded stays one unresolved marker, so its text is kept
        assertThat(ordinals(matcher, "[1]–[30]"), contains(-1));

        // a range joined across a line break is expanded too
        List<TaggingTokenCluster> merged = ReferenceMarkerMatcher.mergeBracketedRanges(
                Arrays.asList(
                        cluster(TaggingLabels.CITATION_MARKER, "[1] "),
                        cluster(TaggingLabels.PARAGRAPH, "–\n"),
                        cluster(TaggingLabels.CITATION_MARKER, " [3]")));
        assertThat(merged, hasSize(1));
        assertThat(
                matcher.match(merged.get(0).concatTokens())
                        .stream()
                        .map(r -> r.getBibDataSet().getResBib().getOrdinal())
                        .collect(Collectors.toList()),
                contains(0, 1, 2));
    }

    private static List<Integer> ordinals(ReferenceMarkerMatcher matcher, String callout) throws Exception {
        return matcher.match(GrobidAnalyzer.getInstance().tokenizeWithLayoutToken(callout))
                .stream()
                .map(r -> r.getBibDataSet() == null ? -1 : r.getBibDataSet().getResBib().getOrdinal())
                .collect(Collectors.toList());
    }
}
