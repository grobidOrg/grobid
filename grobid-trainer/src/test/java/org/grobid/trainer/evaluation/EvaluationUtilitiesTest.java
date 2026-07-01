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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class EvaluationUtilitiesTest {

    @Test
    public void testTokenLevelStats_allGood() throws Exception {
        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> I-<2>\nd I-<1> I-<1>\ne <1> <1>\n";
        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(4));
        assertThat(labelstat2.getObserved(), is(1));
        assertThat(labelstat1.getExpected(), is(4));
        assertThat(labelstat2.getExpected(), is(1));

        assertThat(labelstat1.getSupport(), is(4L));
        assertThat(labelstat2.getSupport(), is(1L));
    }

    @Test
    public void testTokenLevelStats_noMatch() throws Exception {
        String result = "a I-<1> I-<2>\nb <1> <2>\nc <1> I-<2>\nd <1> <2>\ne <1> <2>\n";

        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(0));
        assertThat(labelstat1.getFalseNegative(), is(5));
        assertThat(labelstat1.getSupport(), is(5L));

        assertThat(labelstat2.getObserved(), is(0));
        assertThat(labelstat2.getFalsePositive(), is(5));
        assertThat(labelstat2.getSupport(), is(0L));
    }

    @Test
    public void testTokenLevelStats_mixed() throws Exception {
        // label of c is false
        // token 80 precision for label <1>, 0 for label <2>
        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> <1>\nd I-<1> <1>\ne <1> <1>\n";
        //System.out.println(result);
        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(4));
        assertThat(labelstat1.getExpected(), is(4));
        assertThat(labelstat1.getFalseNegative(), is(0));
        assertThat(labelstat1.getFalsePositive(), is(1));
        assertThat(labelstat1.getSupport(), is(4L));

        assertThat(labelstat2.getObserved(), is(0));
        assertThat(labelstat2.getExpected(), is(1));
        assertThat(labelstat2.getFalseNegative(), is(1));
        assertThat(labelstat2.getFalsePositive(), is(0));
        assertThat(labelstat2.getSupport(), is(1L));
    }

    @Test
    public void testTokenLevelStats2_mixed() throws Exception {
        String result = "a I-<1> I-<1>\nb <1> <1>\nc I-<2> I-<1>\nd I-<1> <1>\ne <1> <1>\n";
        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(4));
        assertThat(labelstat1.getExpected(), is(4));
        assertThat(labelstat1.getFalseNegative(), is(0));
        assertThat(labelstat1.getFalsePositive(), is(1));
        assertThat(labelstat1.getSupport(), is(4L));

        assertThat(labelstat2.getObserved(), is(0));
        assertThat(labelstat2.getExpected(), is(1));
        assertThat(labelstat2.getFalseNegative(), is(1));
        assertThat(labelstat2.getFalsePositive(), is(0));
        assertThat(labelstat2.getSupport(), is(1L));
    }

    @Test
    public void testTokenLevelStats3_mixed() throws Exception {
        String result = "a I-<1> I-<1>\nb <1> <1>\nc <1> I-<2>\nd <1> I-<1>\ne <1> <1>\n";
        //System.out.println(result);
        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(4));
        assertThat(labelstat2.getObserved(), is(0));
        assertThat(labelstat1.getExpected(), is(5));
        assertThat(labelstat2.getExpected(), is(0));
        assertThat(labelstat1.getFalseNegative(), is(1));
        assertThat(labelstat2.getFalseNegative(), is(0));
        assertThat(labelstat1.getFalsePositive(), is(0));
        assertThat(labelstat2.getFalsePositive(), is(1));

    }

    @Test
    public void testTokenLevelStats4_mixed() throws Exception {
        String result = "a I-<1> I-<1>\nb I-<2> <1>\nc <2> I-<2>\nd <2> <2>\ne I-<1> I-<1>\nf <1> <1>\ng I-<2> I-<2>\n";

        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<1>");
        LabelStat labelstat2 = wordStats.getLabelStat("<2>");

        assertThat(labelstat1.getObserved(), is(3));
        assertThat(labelstat1.getExpected(), is(3));
        assertThat(labelstat1.getFalseNegative(), is(0));
        assertThat(labelstat1.getFalsePositive(), is(1));

        assertThat(labelstat2.getObserved(), is(3));
        assertThat(labelstat2.getExpected(), is(4));
        assertThat(labelstat2.getFalseNegative(), is(1));
        assertThat(labelstat2.getFalsePositive(), is(0));
    }

    @Test
    public void testTokenLevelStats_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.1.txt"), StandardCharsets.UTF_8);
        result = result.replace(System.lineSeparator(), "\n");

        Stats wordStats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat labelstat1 = wordStats.getLabelStat("<body>");
        LabelStat labelstat2 = wordStats.getLabelStat("<headnote>");

        assertThat(labelstat1.getObserved(), is(378));
        assertThat(labelstat2.getObserved(), is(6));
        assertThat(labelstat1.getExpected(), is(378));
        assertThat(labelstat2.getExpected(), is(9));
        assertThat(labelstat1.getFalseNegative(), is(0));
        assertThat(labelstat2.getFalseNegative(), is(3));
        assertThat(labelstat1.getFalsePositive(), is(3));
        assertThat(labelstat2.getFalsePositive(), is(0));

    }

    @Test
    public void testTokenLevelStats2_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.2.txt"), StandardCharsets.UTF_8);

        Stats stats = EvaluationUtilities.tokenLevelStats(result);

        LabelStat conceptualLabelStats = stats.getLabelStat("CONCEPTUAL");
        assertThat(conceptualLabelStats.getObserved(), is(2));
        assertThat(conceptualLabelStats.getExpected(), is(3));
        assertThat(conceptualLabelStats.getFalseNegative(), is(1));
        assertThat(conceptualLabelStats.getFalsePositive(), is(1));

        LabelStat periodLabelStats = stats.getLabelStat("PERIOD");
        assertThat(periodLabelStats.getObserved(), is(8));
        assertThat(periodLabelStats.getExpected(), is(8));
        assertThat(periodLabelStats.getFalseNegative(), is(0));
        assertThat(periodLabelStats.getFalsePositive(), is(0));

        LabelStat mediaLabelStats = stats.getLabelStat("MEDIA");
        assertThat(mediaLabelStats.getObserved(), is(7));
        assertThat(mediaLabelStats.getExpected(), is(7));
        assertThat(mediaLabelStats.getFalseNegative(), is(0));
        assertThat(mediaLabelStats.getFalsePositive(), is(0));

        LabelStat personTypeLabelStats = stats.getLabelStat("PERSON_TYPE");
        assertThat(personTypeLabelStats.getObserved(), is(0));
        assertThat(personTypeLabelStats.getExpected(), is(1));
        assertThat(personTypeLabelStats.getFalseNegative(), is(1));
        assertThat(personTypeLabelStats.getFalsePositive(), is(0));

        LabelStat locationTypeLabelStats = stats.getLabelStat("LOCATION");
        assertThat(locationTypeLabelStats.getObserved(), is(2));
        assertThat(locationTypeLabelStats.getExpected(), is(2));
        assertThat(locationTypeLabelStats.getFalseNegative(), is(0));
        assertThat(locationTypeLabelStats.getFalsePositive(), is(0));

        LabelStat organisationTypeLabelStats = stats.getLabelStat("ORGANISATION");
        assertThat(organisationTypeLabelStats.getObserved(), is(2));
        assertThat(organisationTypeLabelStats.getExpected(), is(2));
        assertThat(organisationTypeLabelStats.getFalseNegative(), is(0));
        assertThat(organisationTypeLabelStats.getFalsePositive(), is(0));

        LabelStat otherLabelStats = stats.getLabelStat("O");
        assertThat(otherLabelStats.getObserved(), is(33));
        assertThat(otherLabelStats.getExpected(), is(34));
        assertThat(otherLabelStats.getFalseNegative(), is(1));
        assertThat(otherLabelStats.getFalsePositive(), is(0));

        LabelStat personLabelStats = stats.getLabelStat("PERSON");
        assertThat(personLabelStats.getObserved(), is(0));
        assertThat(personLabelStats.getExpected(), is(0));
        assertThat(personLabelStats.getFalseNegative(), is(0));
        assertThat(personLabelStats.getFalsePositive(), is(1));
    }

    @Test
    public void testTokenLevelStats4_realCase() throws Exception {
        String result = IOUtils
                .toString(this.getClass().getResourceAsStream("/sample.wapiti.output.3.txt"), StandardCharsets.UTF_8);

        ModelStats fieldStats = EvaluationUtilities.computeStats(result);

        assertThat(fieldStats.getTotalInstances(), is(4));
        assertThat(fieldStats.getCorrectInstance(), is(1));
        assertThat(fieldStats.getInstanceRecall(), is(1.0 / 4));
        assertThat(fieldStats.getSupportSum(), is(6L));
    }
}
