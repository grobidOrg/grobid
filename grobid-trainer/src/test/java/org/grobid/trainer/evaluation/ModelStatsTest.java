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
package org.grobid.trainer.evaluation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class ModelStatsTest {

    ModelStats target;

    @Before
    public void setUp() throws Exception {
        target = new ModelStats();
    }

    @Test
    public void test_empty() throws Exception {
        assertThat(target.getInstanceRecall(), is(0.0));
    }

    @Test
    public void testInstantiation_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.3.txt"), StandardCharsets.UTF_8);

        Pair<Integer, Integer> instanceStatistics = target.computeInstanceStatistics(result);

        assertThat(instanceStatistics.getRight(), is(1));
        assertThat(instanceStatistics.getLeft(), is(4));
    }

    @Test
    public void testTokenLevelStats3_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.3.txt"), StandardCharsets.UTF_8);

        Stats fieldStats = target.fieldLevelStats(result);

        TreeMap<String, LabelResult> labelsResults = fieldStats.getLabelsResults();

        assertThat(labelsResults.get("<base>").getSupport(), CoreMatchers.is(4L));
        assertThat(labelsResults.get("<prefix>").getSupport(), CoreMatchers.is(2L));
    }

    @Test
    public void testFieldLevelStats_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.1.txt"), StandardCharsets.UTF_8);

        Stats fieldStats = target.fieldLevelStats(result);
        LabelStat labelstat1 = fieldStats.getLabelStat("<body>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<headnote>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(1));
        assertThat(labelstat2.getObserved(), CoreMatchers.is(2));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(3));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(3));
        assertThat(labelstat1.getFalseNegative(), CoreMatchers.is(2));
        assertThat(labelstat2.getFalseNegative(), CoreMatchers.is(1));
        assertThat(labelstat1.getFalsePositive(), CoreMatchers.is(1));
        assertThat(labelstat2.getFalsePositive(), CoreMatchers.is(0));
    }

    @Test
    public void testFieldLevelStats4_mixed() throws Exception {
        String result = "a I-<1> I-<1>\nb I-<2> <1>\nc <2> I-<2>\nd <2> <2>\ne I-<1> I-<1>\nf <1> <1>\ng I-<2> I-<2>\n";

        Stats fieldStats = target.fieldLevelStats(result);
        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(1));
        assertThat(labelstat2.getObserved(), CoreMatchers.is(1));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(2));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(2));
        assertThat(labelstat1.getFalseNegative(), CoreMatchers.is(1));
        assertThat(labelstat2.getFalseNegative(), CoreMatchers.is(1));
        assertThat(labelstat1.getFalsePositive(), CoreMatchers.is(1));
        assertThat(labelstat2.getFalsePositive(), CoreMatchers.is(1));
    }

    @Test
    public void testFieldLevelStats3_mixed() throws Exception {
        String result = "a I-<1> I-<1>\nb <1> <1>\nc <1> I-<2>\nd <1> I-<1>\ne <1> <1>\n";

        Stats fieldStats = target.fieldLevelStats(result);
        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat2.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(1));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(0));
        assertThat(labelstat1.getFalseNegative(), CoreMatchers.is(1));
        assertThat(labelstat2.getFalseNegative(), CoreMatchers.is(0));
        assertThat(labelstat1.getFalsePositive(), CoreMatchers.is(2));
        assertThat(labelstat2.getFalsePositive(), CoreMatchers.is(1));
    }

    @Test
    public void testFieldLevelStats2_mixed() throws Exception {
        // variant of testMetricsMixed1 where the I- prefix impact the field-level results
        // with field ab correctly found

        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> I-<1>\nd I-<1> <1>\ne <1> <1>\n";
        Stats fieldStats = target.fieldLevelStats(result);

        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(1));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(2));

        assertThat(labelstat2.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(1));
    }

    @Test
    public void testFieldLevelStats_allGood() throws Exception {
        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> I-<2>\nd I-<1> I-<1>\ne <1> <1>\n";

        Stats fieldStats = target.fieldLevelStats(result);

        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(2));
        assertThat(labelstat2.getObserved(), CoreMatchers.is(1));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(2));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(1));

        assertThat(labelstat1.getSupport(), CoreMatchers.is(2L));
        assertThat(labelstat2.getSupport(), CoreMatchers.is(1L));
    }

    @Test
    public void testFieldLevelStats_noMatch() throws Exception {
        String result = "a I-<1> I-<2>\nb <1> <2>\nc <1> I-<2>\nd <1> <2>\ne <1> <2>\n";
        Stats fieldStats = target.fieldLevelStats(result);

        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(1));
        assertThat(labelstat1.getSupport(), CoreMatchers.is(1L));

        assertThat(labelstat2.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(0));
        assertThat(labelstat2.getSupport(), CoreMatchers.is(0L));
    }

    @Test
    public void testFieldLevelStats_mixed() throws Exception {
        // field: precision and recall are 0, because the whole
        // sequence abcde with label <1> does not make sub-field
        // ab and de correctly label with respect to positions
        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> <1>\nd I-<1> <1>\ne <1> <1>\n";
        Stats fieldStats = target.fieldLevelStats(result);

        LabelStat labelstat1 = fieldStats.getLabelStat("<1>");
        LabelStat labelstat2 = fieldStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat1.getExpected(), CoreMatchers.is(2));
        assertThat(labelstat1.getSupport(), CoreMatchers.is(2L));

        assertThat(labelstat2.getObserved(), CoreMatchers.is(0));
        assertThat(labelstat2.getExpected(), CoreMatchers.is(1));
        assertThat(labelstat2.getSupport(), CoreMatchers.is(1L));
    }

}
