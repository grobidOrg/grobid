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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.grobid.trainer.evaluation.utilities.FieldSpecification;

public class EndToEndEvaluationTest {

    @Test
    public void testRemoveFieldsFromEvaluation_shouldRemove() throws Exception {

        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        FieldSpecification field1 = new FieldSpecification();
        field1.fieldName = "bao";
        fieldSpecification.add(field1);

        FieldSpecification field2 = new FieldSpecification();
        field2.fieldName = "miao";
        fieldSpecification.add(field2);

        List<String> labelSpecification = new ArrayList<>();
        labelSpecification.add("bao");
        labelSpecification.add("miao");

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("bao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(1));
        assertThat(labelSpecification, hasSize(1));

        assertThat(fieldSpecification.get(0).fieldName, is("miao"));
        assertThat(labelSpecification.get(0), is("miao"));
    }

    @Test
    public void testRemoveFieldsFromEvaluation() throws Exception {

        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        FieldSpecification field1 = new FieldSpecification();
        field1.fieldName = "bao";
        fieldSpecification.add(field1);

        FieldSpecification field2 = new FieldSpecification();
        field2.fieldName = "miao";
        fieldSpecification.add(field2);

        List<String> labelSpecification = new ArrayList<>();
        labelSpecification.add("bao");
        labelSpecification.add("miao");

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("zao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(2));
        assertThat(labelSpecification, hasSize(2));

        assertThat(fieldSpecification.get(0).fieldName, is("bao"));
        assertThat(labelSpecification.get(0), is("bao"));

        assertThat(fieldSpecification.get(1).fieldName, is("miao"));
        assertThat(labelSpecification.get(1), is("miao"));
    }

    @Test
    public void testRemoveFieldsFromEvaluationEmpty_ShouldNotFail() throws Exception {
        List<FieldSpecification> fieldSpecification = new ArrayList<>();
        List<String> labelSpecification = new ArrayList<>();

        EndToEndEvaluation.removeFieldsFromEvaluation(Arrays.asList("bao"), fieldSpecification, labelSpecification);

        assertThat(fieldSpecification, hasSize(0));
        assertThat(labelSpecification, hasSize(0));
    }

    @Test
    public void testDocumentEvaluationResultMerge_shouldCombineCountsAndStats() throws Exception {
        EndToEndEvaluation.DocumentEvaluationResult target = new EndToEndEvaluation.DocumentEvaluationResult();
        target.nbFile = 1;
        target.totalExpectedInstances = 10;
        target.totalObservedInstances = 8;
        target.totalCorrectInstancesStrict = 5;
        target.totalCorrectInstancesSoft = 6;
        target.totalCorrectInstancesLevenshtein = 7;
        target.totalCorrectInstancesRatcliffObershelp = 7;
        target.totalExpectedReferences = 20;
        target.totalObservedReferences = 18;
        target.totalExpectedCitations = 15;
        target.totalObservedCitations = 14;
        target.totalCorrectObservedCitations = 12;
        target.totalWrongObservedCitations = 2;
        target.match1 = 1;
        target.match2 = 2;
        target.match3 = 3;
        target.match4 = 4;
        target.strictStats.getLabelStat("FOO").setExpected(4);
        target.strictStats.getLabelStat("FOO").setObserved(3);

        EndToEndEvaluation.DocumentEvaluationResult other = new EndToEndEvaluation.DocumentEvaluationResult();
        other.nbFile = 2;
        other.totalExpectedInstances = 5;
        other.totalObservedInstances = 4;
        other.totalCorrectInstancesStrict = 3;
        other.totalCorrectInstancesSoft = 3;
        other.totalCorrectInstancesLevenshtein = 4;
        other.totalCorrectInstancesRatcliffObershelp = 4;
        other.totalExpectedReferences = 10;
        other.totalObservedReferences = 9;
        other.totalExpectedCitations = 8;
        other.totalObservedCitations = 7;
        other.totalCorrectObservedCitations = 6;
        other.totalWrongObservedCitations = 1;
        other.match1 = 10;
        other.match2 = 20;
        other.match3 = 30;
        other.match4 = 40;
        other.strictStats.getLabelStat("FOO").setExpected(2);
        other.strictStats.getLabelStat("FOO").setObserved(1);
        other.strictStats.getLabelStat("BAR").setExpected(3);

        target.merge(other);

        assertThat(target.nbFile, is(3));
        assertThat(target.totalExpectedInstances, is(15));
        assertThat(target.totalObservedInstances, is(12));
        assertThat(target.totalCorrectInstancesStrict, is(8));
        assertThat(target.totalCorrectInstancesSoft, is(9));
        assertThat(target.totalCorrectInstancesLevenshtein, is(11));
        assertThat(target.totalCorrectInstancesRatcliffObershelp, is(11));
        assertThat(target.totalExpectedReferences, is(30));
        assertThat(target.totalObservedReferences, is(27));
        assertThat(target.totalExpectedCitations, is(23));
        assertThat(target.totalObservedCitations, is(21));
        assertThat(target.totalCorrectObservedCitations, is(18));
        assertThat(target.totalWrongObservedCitations, is(3));
        assertThat(target.match1, is(11));
        assertThat(target.match2, is(22));
        assertThat(target.match3, is(33));
        assertThat(target.match4, is(44));
        assertThat(target.strictStats.getLabelStat("FOO").getExpected(), is(6));
        assertThat(target.strictStats.getLabelStat("FOO").getObserved(), is(4));
        assertThat(target.strictStats.getLabelStat("BAR").getExpected(), is(3));
    }

    @Test
    public void testDocumentEvaluationResultMerge_nullShouldNotChange() throws Exception {
        EndToEndEvaluation.DocumentEvaluationResult target = new EndToEndEvaluation.DocumentEvaluationResult();
        target.nbFile = 1;
        target.totalExpectedInstances = 10;
        target.strictStats.getLabelStat("FOO").setExpected(4);

        target.merge(null);

        assertThat(target.nbFile, is(1));
        assertThat(target.totalExpectedInstances, is(10));
        assertThat(target.strictStats.getLabelStat("FOO").getExpected(), is(4));
    }

}
