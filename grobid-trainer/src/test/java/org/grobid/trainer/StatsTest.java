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
package org.grobid.trainer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.grobid.trainer.evaluation.LabelStat;
import org.grobid.trainer.evaluation.Stats;

public class StatsTest {
    Stats target;

    @Before
    public void setUp() throws Exception {
        target = new Stats();
    }

    @Test
    public void testPrecision_fullMatch() throws Exception {
        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);
        assertThat(target.getLabelStat("BAO").getPrecision(), is(1.0));
    }

    @Test
    public void testPrecision_noMatch() throws Exception {
        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);
        assertThat(target.getLabelStat("MIAO").getPrecision(), is(0.0));
        assertThat(target.getLabelStat("MIAO").getSupport(), is(4L));
    }

    @Test
    public void testPrecision_missingMatch() throws Exception {
        // The precision stays at 1.0 because none of the observed
        // is wrong (no false positives)
        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);
        assertThat(target.getLabelStat("CIAO").getPrecision(), is(1.0));
    }

    @Test
    public void testPrecision_2wronglyRecognised() throws Exception {
        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(1);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        assertThat(target.getLabelStat("ZIAO").getPrecision(), is(0.3333333333333333));
    }

    @Test
    public void testRecall_fullMatch() throws Exception {
        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);
        assertThat(target.getLabelStat("BAO").getRecall(), is(1.0));
    }

    @Test
    public void testRecall_noMatch() throws Exception {
        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);
        assertThat(target.getLabelStat("MIAO").getRecall(), is(0.0));
    }

    @Test
    public void testRecall_oneOverFour() throws Exception {
        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);
        assertThat(target.getLabelStat("CIAO").getRecall(), is(0.25));

    }

    @Test
    public void testRecall_partialMatch() throws Exception {
        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(3);
        target.getLabelStat("ZIAO").setFalsePositive(1);

        assertThat(target.getLabelStat("ZIAO").getPrecision(), is(0.75));
    }

    // Average measures

    @Test
    public void testMicroAvgPrecision_shouldWork() throws Exception {
        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        assertThat(target.getMicroAveragePrecision(), is(((double) 4 + 0 + 1 + 0) / (4 + 0 + 3 + 1 + 0 + 2)));
    }

    @Test
    public void testMacroAvgPrecision_shouldWork() throws Exception {

        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        final double precisionBao = target.getLabelStat("BAO").getPrecision();
        final double precisionMiao = target.getLabelStat("MIAO").getPrecision();
        final double precisionCiao = target.getLabelStat("CIAO").getPrecision();
        final double precisionZiao = target.getLabelStat("ZIAO").getPrecision();
        assertThat(
                target.getMacroAveragePrecision(),
                is((precisionBao + precisionMiao + precisionCiao + precisionZiao) / (4)));
    }

    @Test
    public void testMicroAvgRecall_shouldWork() throws Exception {

        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);     //TP

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        assertThat(target.getMicroAverageRecall(), is(((double) 4 + 0 + 1 + 0) / (4 + 4 + 4 + 4)));
    }

    @Test
    public void testMacroAvgRecall_shouldWork() throws Exception {

        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        final double recallBao = target.getLabelStat("BAO").getRecall();
        final double recallMiao = target.getLabelStat("MIAO").getRecall();
        final double recallCiao = target.getLabelStat("CIAO").getRecall();
        final double recallZiao = target.getLabelStat("ZIAO").getRecall();
        assertThat(
                target.getMacroAverageRecall(),
                is((recallBao + recallMiao + recallCiao + recallZiao) / (4)));
    }

    @Test
    public void testMicroAvgF0_shouldWork() throws Exception {

        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);     //TP

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        assertThat(
                target.getMicroAverageF1(),
                is(
                        ((double) 2 * target.getMicroAveragePrecision()
                                * target.getMicroAverageRecall())
                                / (target.getMicroAveragePrecision() + target.getMicroAverageRecall())));
    }

    @Test
    public void testMacroAvgF0_shouldWork() throws Exception {

        target.getLabelStat("BAO").setExpected(4);
        target.getLabelStat("BAO").setObserved(4);

        target.getLabelStat("MIAO").setExpected(4);
        target.getLabelStat("MIAO").setObserved(0);
        target.getLabelStat("MIAO").setFalsePositive(3);
        target.getLabelStat("MIAO").setFalseNegative(1);

        target.getLabelStat("CIAO").setExpected(4);
        target.getLabelStat("CIAO").setObserved(1);
        target.getLabelStat("CIAO").setFalseNegative(3);

        target.getLabelStat("ZIAO").setExpected(4);
        target.getLabelStat("ZIAO").setObserved(0);
        target.getLabelStat("ZIAO").setFalsePositive(2);

        final double f1Bao = target.getLabelStat("BAO").getF1Score();
        final double f1Miao = target.getLabelStat("MIAO").getF1Score();
        final double f1Ciao = target.getLabelStat("CIAO").getF1Score();
        final double f1Ziao = target.getLabelStat("ZIAO").getRecall();

        assertThat(
                target.getMacroAverageF1(),
                is((f1Bao + f1Miao + f1Ciao + f1Ziao) / (4)));
    }

    @Ignore("Not really useful")
    @Test
    public void testMicroMacroAveragePrecision() throws Exception {
        final LabelStat conceptual = target.getLabelStat("CONCEPTUAL");
        conceptual.setFalsePositive(1);
        conceptual.setFalseNegative(1);
        conceptual.setObserved(2);
        conceptual.setExpected(3);

        final LabelStat location = target.getLabelStat("LOCATION");
        location.setFalsePositive(0);
        location.setFalseNegative(0);
        location.setObserved(2);
        location.setExpected(2);

        final LabelStat media = target.getLabelStat("MEDIA");
        media.setFalsePositive(0);
        media.setFalseNegative(0);
        media.setObserved(7);
        media.setExpected(7);

        final LabelStat national = target.getLabelStat("NATIONAL");
        national.setFalsePositive(1);
        national.setFalseNegative(0);
        national.setObserved(0);
        national.setExpected(0);

        final LabelStat other = target.getLabelStat("O");
        other.setFalsePositive(0);
        other.setFalseNegative(1);
        other.setObserved(33);
        other.setExpected(34);

        final LabelStat organisation = target.getLabelStat("ORGANISATION");
        organisation.setFalsePositive(0);
        organisation.setFalseNegative(0);
        organisation.setObserved(2);
        organisation.setExpected(2);

        final LabelStat period = target.getLabelStat("PERIOD");
        period.setFalsePositive(0);
        period.setFalseNegative(0);
        period.setObserved(8);
        period.setExpected(8);

        final LabelStat person = target.getLabelStat("PERSON");
        person.setFalsePositive(1);
        person.setFalseNegative(0);
        person.setObserved(0);
        person.setExpected(0);

        final LabelStat personType = target.getLabelStat("PERSON_TYPE");
        personType.setFalsePositive(0);
        personType.setFalseNegative(1);
        personType.setObserved(0);
        personType.setExpected(1);

        for (String label : target.getLabels()) {
            System.out.println(label + " precision --> " + target.getLabelStat(label).getPrecision());
            System.out.println(label + " recall --> " + target.getLabelStat(label).getRecall());
        }

        System.out.println(target.getMacroAveragePrecision());
        System.out.println(target.getMicroAveragePrecision());
    }

    @Test
    public void testMicroMacroAverageMeasures_realTest() throws Exception {
        LabelStat otherLabelStats = target.getLabelStat("O");
        otherLabelStats.setExpected(33);
        otherLabelStats.setObserved(33);
        otherLabelStats.setFalseNegative(1);

        assertThat(otherLabelStats.getPrecision(), is(1.0));
        assertThat(otherLabelStats.getRecall(), is(1.0));

        LabelStat conceptualLabelStats = target.getLabelStat("CONCEPTUAL");
        conceptualLabelStats.setObserved(2);
        conceptualLabelStats.setExpected(3);
        conceptualLabelStats.setFalseNegative(1);
        conceptualLabelStats.setFalsePositive(1);

        assertThat(conceptualLabelStats.getPrecision(), is(0.6666666666666666));
        assertThat(conceptualLabelStats.getRecall(), is(0.6666666666666666));

        LabelStat periodLabelStats = target.getLabelStat("PERIOD");
        periodLabelStats.setObserved(8);
        periodLabelStats.setExpected(8);

        assertThat(periodLabelStats.getPrecision(), is(1.0));
        assertThat(periodLabelStats.getRecall(), is(1.0));

        LabelStat mediaLabelStats = target.getLabelStat("MEDIA");
        mediaLabelStats.setObserved(7);
        mediaLabelStats.setExpected(7);

        assertThat(mediaLabelStats.getPrecision(), is(1.0));
        assertThat(mediaLabelStats.getRecall(), is(1.0));

        LabelStat personTypeLabelStats = target.getLabelStat("PERSON_TYPE");
        personTypeLabelStats.setObserved(0);
        personTypeLabelStats.setExpected(1);
        personTypeLabelStats.setFalseNegative(1);

        assertThat(personTypeLabelStats.getPrecision(), is(0.0));
        assertThat(personTypeLabelStats.getRecall(), is(0.0));

        LabelStat locationTypeLabelStats = target.getLabelStat("LOCATION");
        locationTypeLabelStats.setObserved(2);
        locationTypeLabelStats.setExpected(2);

        assertThat(locationTypeLabelStats.getPrecision(), is(1.0));
        assertThat(locationTypeLabelStats.getRecall(), is(1.0));

        LabelStat organisationTypeLabelStats = target.getLabelStat("ORGANISATION");
        organisationTypeLabelStats.setObserved(2);
        organisationTypeLabelStats.setExpected(2);

        assertThat(locationTypeLabelStats.getPrecision(), is(1.0));
        assertThat(locationTypeLabelStats.getRecall(), is(1.0));

        LabelStat personLabelStats = target.getLabelStat("PERSON");
        personLabelStats.setFalsePositive(1);

        assertThat(personLabelStats.getPrecision(), is(0.0));
        assertThat(personLabelStats.getRecall(), is(0.0));

        // 2+8+2+2+7 / (2+8+2+2+7+1)
        assertThat(target.getMicroAverageRecall(), is(0.9130434782608695)); //91.3

        // 2+8+2+2+7 / (3+8+7+2+2+1)
        assertThat(target.getMicroAveragePrecision(), is(0.9545454545454546)); //95.45

        // 0.66 + 1.0 + 1.0 + 0.0 + 1.0 + 1.0 / 6
        assertThat(target.getMacroAverageRecall(), is(0.7777777777777777)); //77.78

        // same as above
        assertThat(target.getMacroAveragePrecision(), is(0.7777777777777777)); //77.78
    }

}
